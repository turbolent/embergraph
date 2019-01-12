/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Nov 3, 2011
 */

package org.embergraph.bop.rdf.join;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.LexiconConfiguration;
import org.embergraph.rdf.internal.impl.literal.NumericIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.relation.accesspath.IBlockingBuffer;

/**
 * A vectored operator that resolves variables bound to mocked terms in binding sets through a
 * dictionary join. This may be necessary when having {@link AssignmentNode} inside queries; these
 * construct fresh values, which are mocked in a first step. Whenever these values are used later in
 * the query, e.g. by joining such a mock URI with a statement pattern, they need to be resolved to
 * their real IVs.Ø
 *
 * @see <a href="http://trac.blazegraph.com/ticket/1007">Ticket 1007: Using bound variables to refer
 *     to a graph</a>
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 */
public class MockTermResolverOp extends PipelineOp {

  private static final Logger log = Logger.getLogger(MockTermResolverOp.class);

  private static final long serialVersionUID = 1L;

  public interface Annotations extends PipelineOp.Annotations {

    /**
     * The {@link IVariable}[] identifying the variables for which the referenced values in binding
     * sets are joined with the dictionary (whenever they are bound to mocked IDs). be materialized.
     * When <code>null</code> or not specified, ALL variables will be materialized. This may not be
     * an empty array as that would imply that there is no need to use this operator.
     */
    String VARS = MockTermResolverOp.class.getName() + ".vars";

    String RELATION_NAME = Predicate.Annotations.RELATION_NAME;

    String TIMESTAMP = Predicate.Annotations.TIMESTAMP;
  }

  /**
   * @param args
   * @param annotations
   */
  public MockTermResolverOp(final BOp[] args, final Map<String, Object> annotations) {

    super(args, annotations);

    final IVariable<?>[] vars = getVars();

    if (vars != null && vars.length == 0) throw new IllegalArgumentException();

    getRequiredProperty(Annotations.RELATION_NAME);
    getRequiredProperty(Annotations.TIMESTAMP);
  }

  /** @param op */
  public MockTermResolverOp(final MockTermResolverOp op) {

    super(op);
  }

  public MockTermResolverOp(final BOp[] args, final NV... annotations) {

    this(args, NV.asMap(annotations));
  }

  /**
   * @param vars The variables for which to resolve mocked IVs. Resolving is only attempted for
   *     those variables which are actually bound in given solution.
   * @param namespace The namespace of the {@link LexiconRelation}.
   * @param timestamp The timestamp against which to read.
   */
  public MockTermResolverOp(
      final BOp[] args, final IVariable<?>[] vars, final String namespace, final long timestamp) {
    this(
        args,
        new NV(Annotations.VARS, vars),
        new NV(Annotations.RELATION_NAME, new String[] {namespace}),
        new NV(Annotations.TIMESTAMP, timestamp));
  }

  /**
   * Return the variables for which mocked IVs are resolved.
   *
   * @return The variables for which mocked IDs shall be resolved -or- <code>null</code> iff all
   *     variables should resolved.
   * @see Annotations#VARS
   */
  public IVariable<?>[] getVars() {

    return (IVariable<?>[]) getProperty(Annotations.VARS);
  }

  @Override
  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<Void>(new ChunkTask(this, context));
  }

  /** Task executing on the node. */
  private static class ChunkTask implements Callable<Void> {

    private final BOpContext<IBindingSet> context;

    /** The variables to be materialized. */
    private final IVariable<?>[] vars;

    private final String namespace;

    private final long timestamp;

    ChunkTask(final MockTermResolverOp op, final BOpContext<IBindingSet> context) {

      this.context = context;

      this.vars = op.getVars();

      namespace = ((String[]) op.getProperty(Annotations.RELATION_NAME))[0];

      timestamp = (Long) op.getProperty(Annotations.TIMESTAMP);
    }

    @Override
    public Void call() throws Exception {

      final BOpStats stats = context.getStats();

      final ICloseableIterator<IBindingSet[]> itr = context.getSource();

      final IBlockingBuffer<IBindingSet[]> sink = context.getSink();

      try {

        final LexiconRelation lex = (LexiconRelation) context.getResource(namespace, timestamp);

        while (itr.hasNext()) {

          final IBindingSet[] a = itr.next();

          stats.chunksIn.increment();
          stats.unitsIn.add(a.length);

          handleChunk(vars, lex, a);

          sink.add(a);
        }

        sink.flush();

        // done.
        return null;

      } finally {

        sink.close();
      }
    }
  } // ChunkTask

  /**
   * Resolve a chunk of {@link IBindingSet}s into a chunk of {@link IBindingSet}s in which {@link
   * IV}s have been resolved to {@link EmbergraphValue}s.
   *
   * @param required The variable(s) to be materialized or <code>null</code> to materialize all
   *     variable bindings.
   * @param lex The lexicon reference.
   * @param chunk The chunk of solutions whose variables will be materialized.
   */
  private static void handleChunk(
      final IVariable<?>[] required, final LexiconRelation lex, final IBindingSet[] chunk) {

    if (log.isInfoEnabled())
      log.info("Fetched chunk: size=" + chunk.length + ", chunk=" + Arrays.toString(chunk));

    /*
     * Estimate hash map capacity based on #variables and #solutions.
     */
    final int initialCapacity =
        required == null
            ? chunk.length
            : ((required.length == 0) ? 1 : chunk.length * required.length);

    /**
     * Collected affected IVs, storing them in a map pointing from the IV to the associated
     * EmbergraphValue.
     */
    final Map<IV<?, ?>, EmbergraphValue> ivMap =
        new LinkedHashMap<IV<?, ?>, EmbergraphValue>(initialCapacity);

    for (IBindingSet solution : chunk) {

      final IBindingSet bindingSet = solution;

      assert bindingSet != null;

      if (required == null) { // all variables

        // Materialize all variable bindings.
        @SuppressWarnings("rawtypes")
        final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

        while (itr.hasNext()) {

          @SuppressWarnings("rawtypes")
          final Map.Entry<IVariable, IConstant> entry = itr.next();

          final IV<?, ?> iv = (IV<?, ?>) entry.getValue().get();

          if (iv == null) {

            throw new RuntimeException("NULL? : var=" + entry.getKey() + ", " + bindingSet);
          }

          /**
           * We are interested only in ivs whose value resolves to a EmbergraphValue that is mocked.
           * As a side effect, the internal cache of the mocked value is cleared.
           */
          collectIVsToResolve(iv, ivMap, lex);
        }

      } else {

        // Materialize the specified variable bindings.
        for (IVariable<?> v : required) {

          final IConstant<?> c = bindingSet.get(v);

          if (c == null) {
            continue;
          }

          final IV<?, ?> iv = (IV<?, ?>) c.get();

          if (iv == null) {

            throw new RuntimeException("NULL? : var=" + v + ", " + bindingSet);
          }

          /**
           * We are interested only in ivs whose value resolves to a EmbergraphValue that is mocked.
           * As a side effect, the internal cache of the mocked value is cleared.
           */
          collectIVsToResolve(iv, ivMap, lex);
        }
      }
    }

    if (log.isInfoEnabled())
      log.info("Processing " + ivMap.size() + " IVs, required=" + Arrays.toString(required));

    /**
     * In order to make lex.addTerms take effect, we need to clear the mocked internal values of the
     * mocked EmbergraphValues that we want to resolve.
     */
    final Collection<EmbergraphValue> ivVals = ivMap.values();

    final EmbergraphValue[] ivValsArr = ivVals.toArray(new EmbergraphValue[ivVals.size()]);

    for (EmbergraphValue ivVal : ivValsArr) {

      // case 1: null IVs need to be resolved
      if (ivVal.getIV() != null && ivVal.getIV().isNullIV()) {
        ivVal.clearInternalValue();
      }

      // case 2: literals that have not been inlined need to be resolved
      if (!lex.isInlineLiterals() && ivVal.getIV() != null && ivVal.getIV().isLiteral()) {
        ivVal.clearInternalValue();
      }
    }

    /** Join with the dictionary and cache the resolved EmbergraphValues on IVs */
    lex.addTerms(ivValsArr, ivValsArr.length, true);
    for (EmbergraphValue ivVal : ivValsArr) {

      final IV<EmbergraphValue, ?> iv = ivVal.getIV();
      if (iv != null) {
        iv.setValue(ivVal);
      }
    }

    /*
     * Replace the IVs in the binding set
     */
    for (IBindingSet e : chunk) {

      replaceInBindingSet(required, e, ivMap);
    }
  } // handleChunk

  /**
   * Collect IVs that need to be resolved against the dictionary. There are actually two cases that
   * we need to consider here:
   *
   * <p>(i) MockedIVs representing (thus far) unresolved URIs need to be resolved against the
   * dictionary. (ii) If inlining of literals is disabled (which is, for instance, the case for the
   * GPU), we also need to make sure that literals that have been created (by default, the math
   * engine creates inlined literals when adding up values) are resolved properly.
   *
   * <p>The method fills the ivMap, mapping the original iv to its EmbergraphValue.
   *
   * @param iv the iv to process
   * @param ivMap mapping from IVs to be resolved to their {@link EmbergraphValue}
   * @param lex the {@link LexiconConfiguration}
   */
  private static void collectIVsToResolve(
      final IV<?, ?> iv, final Map<IV<?, ?>, EmbergraphValue> ivMap, final LexiconRelation lex) {

    if (iv.isNullIV() && iv.hasValue() && !iv.isInline()) {

      final Object ivVal = iv.getValue();
      if (ivVal instanceof EmbergraphValue) {
        final EmbergraphValue bdVal = (EmbergraphValue) ivVal;
        if (!bdVal.isRealIV()) {
          ivMap.put(iv, bdVal);
        }
      }

      // if literals are not inlined, we need to resolve them
    } else if (!lex.isInlineLiterals() && iv.isLiteral()) {

      ivMap.put(iv, ((NumericIV<?, ?>) iv).asValue(lex));
    }
  }

  /**
   * Replaces the constant referenced in the {@link IBindingSet} using the map populated when we
   * fetched the current chunk.
   *
   * @param required The variables to be resolved -or- <code>null</code> if all variables should
   *     have been resolved.
   * @param bindingSet A solution whose constants will be resolved to the corresponding constants
   *     with the resolved IVs included according to the <terms>ivMap<terms> ivs.
   * @param ivMap A map from {@link IV}s to resolved {@link EmbergraphValue}s.
   */
  private static void replaceInBindingSet(
      final IVariable<?>[] required,
      final IBindingSet bindingSet,
      final Map<IV<?, ?>, EmbergraphValue> ivMap) {

    if (bindingSet == null) throw new IllegalArgumentException();

    if (ivMap == null) throw new IllegalArgumentException();

    if (required != null) {

      /*
       * Only the specified variables.
       */

      for (IVariable<?> var : required) {

        @SuppressWarnings("unchecked")
        final IConstant<IV<?, ?>> c = bindingSet.get(var);

        if (c == null) {
          // Variable is not bound in this solution.
          continue;
        }

        final IV<?, ?> iv = c.get();

        if (iv == null) {

          continue;
        }

        final EmbergraphValue value = ivMap.get(iv);

        /**
         * Note: constants are immutable, so we can't execute a c.set(), but instead we need to
         * construct a fresh constant with the resolved value.
         */
        if (value != null && value.getIV() != null) {
          bindingSet.set(var, new Constant<IV<EmbergraphValue, ?>>(value.getIV()));
        } // otherwise: nothing to be done
      }

    } else {

      /*
       * Everything in the binding set.
       */

      @SuppressWarnings("rawtypes")
      final Iterator<Map.Entry<IVariable, IConstant>> itr = bindingSet.iterator();

      while (itr.hasNext()) {

        @SuppressWarnings("rawtypes")
        final Map.Entry<IVariable, IConstant> entry = itr.next();

        final Object boundValue = entry.getValue().get();

        if (!(boundValue instanceof IV)) {

          continue;
        }

        final IV<?, ?> iv = (IV<?, ?>) boundValue;

        final EmbergraphValue value = ivMap.get(iv);

        /**
         * Note: constants are immutable, so we can't execute a c.set(), but instead we need to
         * construct a fresh constant with the resolved value.
         */
        bindingSet.set(entry.getKey(), new Constant<IV<EmbergraphValue, ?>>(value.getIV()));
      }
    }
  }
}
