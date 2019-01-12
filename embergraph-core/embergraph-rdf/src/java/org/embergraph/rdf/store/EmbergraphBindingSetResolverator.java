package org.embergraph.rdf.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.engine.SolutionsLog;
import org.embergraph.bop.rdf.join.ChunkedMaterializationOp;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.striterator.AbstractChunkedResolverator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.openrdf.model.Value;

/**
 * Efficiently resolve term identifiers in Embergraph {@link IBindingSet}s to RDF {@link
 * EmbergraphValue}s.
 *
 * @see ChunkedMaterializationOp
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: EmbergraphSolutionResolverator.java 3448 2010-08-18 20:55:58Z thompsonbry $
 */
public class EmbergraphBindingSetResolverator
    extends AbstractChunkedResolverator<IBindingSet, IBindingSet, AbstractTripleStore> {

  private static final Logger log = Logger.getLogger(EmbergraphBindingSetResolverator.class);

  private final UUID queryId;

  @SuppressWarnings("rawtypes")
  private final IVariable[] required;

  private final int termsChunkSize;
  private final int blobsChunkSize;

  /**
   * @param db Used to resolve term identifiers to {@link Value} objects.
   * @param src The source iterator (will be closed when this iterator is closed).
   * @param queryId The query {@link UUID} (for logging on the {@link SolutionsLog}).
   * @param required The variables to be resolved (optional). When <code>null</code>, all variables
   *     will be resolved.
   *     <p>FIXME must accept reverse bnodes map (from term identifier to blank nodes) for
   *     resolution of blank nodes within a Sesame connection context.
   */
  public EmbergraphBindingSetResolverator(
      final AbstractTripleStore db,
      final IChunkedOrderedIterator<IBindingSet> src,
      final UUID queryId,
      final IVariable[] required,
      final int chunkOfChunksCapacity,
      final int chunkCapacity,
      final long chunkTimeout,
      final int termsChunkSize,
      final int blobsChunkSize) {

    super(
        db,
        src,
        new BlockingBuffer<IBindingSet[]>(
            chunkOfChunksCapacity, chunkCapacity, chunkTimeout, TimeUnit.MILLISECONDS));

    this.queryId = queryId;
    this.required = required;
    this.termsChunkSize = termsChunkSize;
    this.blobsChunkSize = blobsChunkSize;

    //        System.err.println("required: " + (required != null ? Arrays.toString(required) :
    // "null"));

  }

  /** Strengthens the return type. */
  public EmbergraphBindingSetResolverator start(final ExecutorService service) {

    return (EmbergraphBindingSetResolverator) super.start(service);
  }

  /**
   * Resolve a chunk of {@link IBindingSet}s into a chunk of {@link IBindingSet}s in which term
   * identifiers have been resolved to {@link EmbergraphValue}s.
   */
  @Override
  protected IBindingSet[] resolveChunk(final IBindingSet[] chunk) {

    return resolveChunk(/*required, */ state.getLexiconRelation(), chunk);
  }

  /**
   * Resolve a chunk of {@link IBindingSet}s into a chunk of {@link IBindingSet}s in which term
   * identifiers have been resolved to {@link EmbergraphValue}s.
   *
   * @param required The variable(s) to be materialized. When <code>null</code>, everything will be
   *     materialized. If a zero length array is given, then NOTHING will be materialized and the
   *     outputs solutions will be empty.
   * @param lex The lexicon reference.
   * @param chunk The chunk of solutions whose variables will be materialized.
   * @return A new chunk of solutions in which those variables have been materialized.
   */
  private IBindingSet[] resolveChunk( // final IVariable<?>[] required,
      final LexiconRelation lex, final IBindingSet[] chunk) {

    return resolveChunk(queryId, lex, chunk, required, termsChunkSize, blobsChunkSize);
  }

  /**
   * Public entry point for batch resolution.
   *
   * @param queryId The query {@link UUID} (for logging on the {@link SolutionsLog}).
   * @param lex The {@link LexiconRelation}.
   * @param chunk The {@link IBindingSet}[] chunk.
   * @param required The variables which need to be materialized.
   * @param termsChunkSize The chunk size for materialization of {@link TermId}s.
   * @param blobsChunkSize The chunk size for materialization of {@link BlobIV}s.
   * @return The resolved {@link IBindingSet}[] chunk.
   */
  /*
   * Note: I've made this static to support chunked resolution outside of the
   * producer/consumer pattern, but there never seems to be a use case for it.
   * Each time it turns out that the EmbergraphValueReplacer is the right thing
   * to use.
   */
  private static IBindingSet[] resolveChunk(
      final UUID queryId,
      final LexiconRelation lex,
      final IBindingSet[] chunk,
      final IVariable<?>[] required,
      final int termsChunkSize,
      final int blobsChunkSize) {

    final long begin = System.currentTimeMillis();

    if (log.isDebugEnabled())
      log.debug("Fetched chunk: size=" + chunk.length + ", chunk=" + Arrays.toString(chunk));

    /*
     * Create a collection of the distinct term identifiers used in this
     * chunk.
     */

    /*
     * Estimate the capacity of the hash map based on the #of variables to
     * materialize per solution and the #of solutions.
     */
    final int initialCapacity =
        required == null
            ? chunk.length
            : ((required.length == 0) ? 1 : chunk.length * required.length);

    final Collection<IV<?, ?>> ids = new HashSet<IV<?, ?>>(initialCapacity);

    for (IBindingSet solution : chunk) {

      final IBindingSet bindingSet = solution;

      //            System.err.println(solution);

      assert bindingSet != null;

      if (required == null) {

        @SuppressWarnings("rawtypes")
        final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

        while (itr.hasNext()) {

          @SuppressWarnings("rawtypes")
          final Map.Entry<IVariable, IConstant> entry = itr.next();

          final IV<?, ?> iv = (IV<?, ?>) entry.getValue().get();

          if (iv == null) {

            throw new RuntimeException("NULL? : var=" + entry.getKey() + ", " + bindingSet);
          }

          handleIV(iv, ids);

          //	                if (iv.hasValue())
          //	                	continue;
          //
          //	                ids.add(iv);

        }

      } else {

        for (IVariable<?> v : required) {

          final IConstant<?> c = bindingSet.get(v);

          if (c == null) {
            continue;
          }

          final IV<?, ?> iv = (IV<?, ?>) c.get();

          if (iv == null) {

            throw new RuntimeException("NULL? : var=" + v + ", " + bindingSet);
          }

          //	                if (iv.hasValue())
          //	                	continue;
          //
          //	                ids.add(iv);

          handleIV(iv, ids);
        }
      }
    }

    //        System.err.println("resolving: " + Arrays.toString(ids.toArray()));

    if (log.isDebugEnabled())
      log.debug("Resolving " + ids.size() + " IVs, required=" + Arrays.toString(required));

    // batch resolve term identifiers to terms.
    final Map<IV<?, ?>, EmbergraphValue> terms = lex.getTerms(ids, termsChunkSize, blobsChunkSize);

    /*
     * Assemble a chunk of resolved elements.
     */
    {
      final IBindingSet[] chunk2 = new IBindingSet[chunk.length];
      int i = 0;
      for (IBindingSet e : chunk) {

        final IBindingSet f = getBindingSet(e, required, terms);

        chunk2[i++] = f;
      }

      if (SolutionsLog.INFO) {

        SolutionsLog.log(queryId, null /* bop */, -1 /* bopId */, -1 /* partitionId */, chunk2);
      }

      final long elapsed = System.currentTimeMillis() - begin;

      if (log.isDebugEnabled())
        log.debug("Resolved chunk: size=" + chunk2.length + ", chunk=" + Arrays.toString(chunk2));

      if (log.isInfoEnabled())
        log.info("Resolved chunk: size=" + chunk2.length + ", elapsed=" + elapsed);

      // return the chunk of resolved elements.
      return chunk2;
    }
  }

  /**
   * Add the IV to the list of terms to materialize, and also delegate to {@link #handleSid(SidIV,
   * Collection, boolean)} if it's a SidIV.
   */
  private static void handleIV(final IV<?, ?> iv, final Collection<IV<?, ?>> ids) {

    if (iv instanceof SidIV) {

      handleSid((SidIV<?>) iv, ids);
    }

    ids.add(iv);
  }

  /**
   * Sids need to be handled specially because their individual ISPO components might need
   * materialization as well.
   */
  private static void handleSid(final SidIV<?> sid, final Collection<IV<?, ?>> ids) {

    final ISPO spo = sid.getInlineValue();

    handleIV(spo.s(), ids);

    handleIV(spo.p(), ids);

    handleIV(spo.o(), ids);

    if (spo.c() != null) {

      handleIV(spo.c(), ids);
    }
  }

  /**
   * Resolve the term identifiers in the {@link IBindingSet} using the map populated when we fetched
   * the current chunk and return the {@link IBindingSet} for that solution in which term
   * identifiers have been resolved to their corresponding {@link EmbergraphValue}s.
   *
   * @param solution A solution whose {@link Long}s will be interpreted as term identifiers and
   *     resolved to the corresponding {@link EmbergraphValue}s.
   * @return The corresponding {@link IBindingSet} in which the term identifiers have been resolved
   *     to {@link EmbergraphValue}s.
   * @throws IllegalStateException if the {@link IBindingSet} was not materialized with the {@link
   *     IBindingSet}.
   */
  private static IBindingSet getBindingSet(
      final IBindingSet solution,
      final IVariable<?>[] required,
      final Map<IV<?, ?>, EmbergraphValue> terms) {

    if (solution == null) throw new IllegalArgumentException();

    if (terms == null) throw new IllegalArgumentException();

    final IBindingSet bindingSet;
    if (required == null) {
      bindingSet = solution;
    } else {
      bindingSet = solution.copy(required);
    }

    @SuppressWarnings("rawtypes")
    final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

    while (itr.hasNext()) {

      @SuppressWarnings("rawtypes")
      final Map.Entry<IVariable, IConstant> entry = itr.next();

      final Object boundValue = entry.getValue().get();

      if (!(boundValue instanceof IV<?, ?>)) {

        continue;
      }

      final IV<?, ?> iv = (IV<?, ?>) boundValue;

      if (iv.hasValue()) continue;

      final EmbergraphValue value = terms.get(iv);

      if (value == null) {

        throw new RuntimeException("Could not resolve: iv=" + iv);
      }

      /*
       * Replace the binding.
       *
       * FIXME This probably needs to strip out the EmbergraphSail#NULL_GRAPH
       * since that should not become bound.
       */
      bindingSet.set(entry.getKey(), new Constant<EmbergraphValue>(value));
    }

    return bindingSet;
  }
}
