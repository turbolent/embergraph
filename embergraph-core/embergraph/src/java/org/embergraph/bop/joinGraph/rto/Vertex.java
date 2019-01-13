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
package org.embergraph.bop.joinGraph.rto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.BOpContextBase;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IElement;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.NV;
import org.embergraph.bop.ap.SampleIndex;
import org.embergraph.bop.ap.SampleIndex.SampleType;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.relation.IRelation;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.striterator.IChunkedIterator;

/*
 * A vertex of the join graph is an annotated relation (this corresponds to an {@link IPredicate}
 * with additional annotations to support the adaptive query optimization algorithm).
 *
 * <p>The unique identifier for a {@link Vertex} (within a given join graph) is the {@link
 * BOp.Annotations#BOP_ID} decorating its {@link IPredicate}. {@link #hashCode()} is defined in
 * terms of this unique identifier so we can readily detect when a {@link Set} already contains a
 * given {@link Vertex}.
 */
public class Vertex implements Serializable {

  private static final transient Logger log = Logger.getLogger(Vertex.class);

  private static final long serialVersionUID = 1L;

  /*
   * The {@link IPredicate} associated with the {@link Vertex}. This basically provides the
   * information necessary to select an appropriate access path.
   */
  public final IPredicate<?> pred;

  /** The most recently taken sample of the {@link Vertex}. */
  transient VertexSample sample = null;

  Vertex(final IPredicate<?> pred) {

    if (pred == null) throw new IllegalArgumentException();

    this.pred = pred;
  }

  @Override
  public String toString() {

    return "Vertex{pred=" + pred + ",sample=" + sample + "}";
  }

  /** Equals is based on a reference test. */
  @Override
  public boolean equals(Object o) {
    return this == o;
  }

  /*
   * The hash code is just the {@link BOp.Annotations#BOP_ID} of the associated {@link IPredicate}.
   */
  @Override
  public int hashCode() {
    return pred.getId();
  }

  /*
   * Take a sample of the vertex, updating {@link #sample} as a side-effect. If the sample is
   * already exact, then this is a NOP. If the vertex was already sampled to that limit, then this
   * is a NOP (you have to raise the limit to re-sample the vertex).
   *
   * @param limit The sample cutoff.
   */
  @SuppressWarnings("unchecked")
  public void sample(final QueryEngine queryEngine, final int limit, final SampleType sampleType) {

    if (queryEngine == null) throw new IllegalArgumentException();

    if (limit <= 0) throw new IllegalArgumentException();

    if (sampleType == null) throw new IllegalArgumentException();

    final VertexSample oldSample = this.sample;

    if (oldSample != null && oldSample.estimateEnum == EstimateEnum.Exact) {

      /*
       * The old sample is already the full materialization of the vertex.
       */

      return;
    }

    if (oldSample != null && oldSample.limit >= limit) {

      /*
       * The vertex was already sampled to this limit.
       */

      return;
    }

    /*
     * FIXME RTO: AST2BOpJoins is responsible for constructing the
     * appropriate access path. Under some cases it can emit a DataSetJoin
     * followed by a join against the access path. Under other cases, it
     * will use a SCAN+FILTER pattern and attach a filter. The code below
     * does not benefit from any of this because the vertex created from the
     * [pred] before we invoke AST2BOpJoin#join() and hence lacks all of
     * these interesting and critical annotations. When generating the join
     * graph, the RTO needs to emit a set of vertices and filters that is
     * sufficient for joins for named graphs and default graphs. It also
     * needs to emit a set of predicates and filters that is sufficient for
     * triples mode joins.
     *
     * Some possible approaches:
     *
     * - For the RTO, always do a DataSetJoin + SP. We would need to support
     * the DataSetJoin as a Predicate (it does not get modeled that way
     * right now). The SP would need to have the DISTINCT SPO filter
     * attached for a default graph join. This might even be a DISTINCT
     * FILTER that gets into the plan and winds up attached to either the
     * DataSetJoin or the SP, depending on which runs first. This would give
     * us two APs plus a visible FILTER rather than ONE AP with some hidden
     * filters. The DataSetJoin would need to be associated with an AP that
     * binds the (hidden) graph variable. This could be an opporunity to
     * generalize for storing those data on the native heap / htree / etc. /
     * named solution set as well.
     *
     * Basically, this amounts to saying that we will sample both the set of
     * graphs that are in the named graphs or default graphs data set and
     * the unconstrained triple pattern AP.
     *
     * - If C is bound, then we should just wind up with a FILTER that is
     * imposing the DISTINCT SPO (for default graph APs) and do not need to
     * do anything (for named graph AP)s.
     */
    final BOpContextBase context = new BOpContextBase(queryEngine);

    final IRelation r = context.getRelation(pred);

    final IAccessPath ap = context.getAccessPath(r, pred);

    final long rangeCount =
        oldSample == null ? ap.rangeCount(false /* exact */) : oldSample.estCard;

    if (rangeCount <= limit) {

      /*
       * Materialize the access path.
       *
       * TODO This could be more efficient if we raised it onto the AP or
       * if we overrode CHUNK_CAPACITY and the fully buffered iterator
       * threshold such that everything was materialized as a single
       * chunk.
       */

      final List<Object> tmp = new ArrayList<>((int) rangeCount);

      final IChunkedIterator<Object> itr = ap.iterator();

      try {

        while (itr.hasNext()) {

          tmp.add(itr.next());
        }

      } finally {

        itr.close();
      }

      sample =
          new VertexSample(
              rangeCount,
              limit,
              EstimateEnum.Exact,
              elementsToBindingSets(pred, tmp.toArray(new Object[0])));

    } else {

      /*
       * Materialize a sample from the access path.
       */

      final SampleIndex<?> sampleOp =
          new SampleIndex(
              new BOp[] {},
              NV.asMap(
                  new NV(SampleIndex.Annotations.PREDICATE, pred),
                  new NV(SampleIndex.Annotations.LIMIT, limit),
                  new NV(SampleIndex.Annotations.SAMPLE_TYPE, sampleType.name())));

      sample =
          new VertexSample(
              rangeCount,
              limit,
              EstimateEnum.Normal,
              elementsToBindingSets(pred, sampleOp.eval(context)));
    }

    if (log.isTraceEnabled()) log.trace("Sampled: id=" + pred.getId() + ", sample=" + sample);

  }

  /*
   * Convert the source sample into an IBindingSet[].
   *
   * @param pred The {@link IPredicate}, which tells us the variables which need to become bound.
   * @param elements The sampled elements as materialized from the index.
   *     <p>FIXME Replace with inline access path based on {@link IBindingSetAccessPath}. The data
   *     can be stored on an {@link HTree}.
   */
  private static IBindingSet[] elementsToBindingSets(
      final IPredicate<?> pred, final Object[] elements) {

    final IBindingSet[] sourceSample = new IBindingSet[elements.length];

    for (int i = 0; i < sourceSample.length; i++) {

      final IBindingSet bset = new ListBindingSet();

      /*
       * TODO Make this method package private once we convert to using an
       * inline access path.
       */
      BOpContext.copyValues((IElement) elements[i], pred, bset);

      sourceSample[i] = bset;
    }

    return sourceSample;
  }
}
