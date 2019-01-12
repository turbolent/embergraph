package org.embergraph.rdf.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.striterator.AbstractChunkedResolverator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.openrdf.model.Value;

/**
 * Efficiently resolve term identifiers in Embergraph {@link ISolution}s to RDF {@link
 * EmbergraphValue}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphSolutionResolverator
    extends AbstractChunkedResolverator<ISolution, IBindingSet, AbstractTripleStore> {

  private static final Logger log = Logger.getLogger(EmbergraphSolutionResolverator.class);

  /**
   * @param db Used to resolve term identifiers to {@link Value} objects.
   * @param src The source iterator (will be closed when this iterator is closed).
   *     <p>FIXME must accept reverse bnodes map (from term identifier to blank nodes) for
   *     resolution of blank nodes within a Sesame connection context.
   */
  public EmbergraphSolutionResolverator(
      final AbstractTripleStore db, final IChunkedOrderedIterator<ISolution> src) {

    super(
        db,
        src,
        new BlockingBuffer<IBindingSet[]>(
            db.getChunkOfChunksCapacity(),
            db.getChunkCapacity(),
            db.getChunkTimeout(),
            TimeUnit.MILLISECONDS));
  }

  /** Strengthens the return type. */
  public EmbergraphSolutionResolverator start(ExecutorService service) {

    return (EmbergraphSolutionResolverator) super.start(service);
  }

  /**
   * Resolve a chunk of {@link ISolution}s into a chunk of {@link IBindingSet}s in which term
   * identifiers have been resolved to {@link EmbergraphValue}s.
   */
  protected IBindingSet[] resolveChunk(final ISolution[] chunk) {

    if (log.isInfoEnabled()) log.info("Fetched chunk: size=" + chunk.length);

    /*
     * Create a collection of the distinct term identifiers used in this
     * chunk.
     */

    final Collection<IV<?, ?>> ids = new HashSet<IV<?, ?>>(chunk.length * state.getSPOKeyArity());

    for (ISolution solution : chunk) {

      final IBindingSet bindingSet = solution.getBindingSet();

      assert bindingSet != null;

      final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

      while (itr.hasNext()) {

        final Map.Entry<IVariable, IConstant> entry = itr.next();

        final IV<?, ?> iv = (IV<?, ?>) entry.getValue().get();

        if (iv == null) {

          throw new RuntimeException("NULL? : var=" + entry.getKey() + ", " + bindingSet);
        }

        ids.add(iv);
      }
    }

    if (log.isInfoEnabled()) log.info("Resolving " + ids.size() + " term identifiers");

    // batch resolve term identifiers to terms.
    final Map<IV<?, ?>, EmbergraphValue> terms = state.getLexiconRelation().getTerms(ids);

    /*
     * Assemble a chunk of resolved elements.
     */
    {
      final IBindingSet[] chunk2 = new IBindingSet[chunk.length];
      int i = 0;
      for (ISolution e : chunk) {

        final IBindingSet f = getBindingSet(e, terms);

        chunk2[i++] = f;
      }

      // return the chunk of resolved elements.
      return chunk2;
    }
  }

  /**
   * Resolve the term identifiers in the {@link ISolution} using the map populated when we fetched
   * the current chunk and return the {@link IBindingSet} for that solution in which term
   * identifiers have been resolved to their corresponding {@link EmbergraphValue}s.
   *
   * @param solution A solution whose {@link Long}s will be interpreted as term identifiers and
   *     resolved to the corresponding {@link EmbergraphValue}s.
   * @return The corresponding {@link IBindingSet} in which the term identifiers have been resolved
   *     to {@link EmbergraphValue}s.
   * @throws IllegalStateException if the {@link IBindingSet} was not materialized with the {@link
   *     ISolution}.
   */
  private IBindingSet getBindingSet(
      final ISolution solution, final Map<IV<?, ?>, EmbergraphValue> terms) {

    if (solution == null) throw new IllegalArgumentException();

    if (terms == null) throw new IllegalArgumentException();

    final IBindingSet bindingSet = solution.getBindingSet();

    if (bindingSet == null) {

      throw new IllegalStateException("BindingSet was not materialized");
    }

    final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

    while (itr.hasNext()) {

      final Map.Entry<IVariable, IConstant> entry = itr.next();

      final Object boundValue = entry.getValue().get();

      if (!(boundValue instanceof IV<?, ?>)) {

        continue;
      }

      final IV<?, ?> iv = (IV<?, ?>) boundValue;

      final EmbergraphValue value = terms.get(iv);

      if (value == null) {

        throw new RuntimeException("Could not resolve termId=" + iv);
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
