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

import org.embergraph.bop.IBindingSet;

/** A sample produced by a cutoff join. */
public class EdgeSample extends SampleBase {

  // private static final transient Logger log = Logger
  // .getLogger(EdgeSample.class);

  /** The source sample used to compute the cutoff join. */
  public final SampleBase sourceSample;

  /** The #of binding sets out of the source sample vertex sample which were consumed. */
  public final int inputCount;

  /** The #of tuples read from the access path when processing the cutoff join. */
  public final long tuplesRead;

  /**
   * The #of binding sets generated before the join was cutoff.
   *
   * <p>Note: If the outputCount is zero then this is a good indicator that there is an error in the
   * query such that the join will not select anything. This is not 100%, merely indicative.
   */
  public final long outputCount;

  /**
   * The adjusted cardinality estimate for the cutoff join (this is {@link #outputCount} as adjusted
   * for a variety of edge conditions).
   */
  public final long adjCard;

  /**
   * The ratio of the #of input samples consumed to the #of output samples generated (the join hit
   * ratio or scale factor).
   */
  public final double f;

  /**
   * The sum of the fast range count for each access path tested.
   *
   * <p>Note: We use pipeline joins to sample cutoff joins so there will be one access path read for
   * each solution in. However, a hash join could be used when the operator is fully executed. The
   * hash join will have one access path on which we read for all input solutions and the range
   * count of the access path will be larger since the access path will be less constrained.
   */
  public final long sumRangeCount;

  /**
   * Estimated tuples read if the operator were fully executed. This is in contrast to {@link
   * SampleBase#estCard}, which is the estimated output cardinality if the operator were fully
   * executed.
   *
   * <p>TODO The actual IOs depend on the join type (hash join versus pipeline join) and whether or
   * not the file has index order (segment versus journal). A hash join will read once on the AP. A
   * pipeline join will read once per input solution. A key-range read on a segment uses multi-block
   * IO while a key-range read on a journal uses random IO. Also, remote access path reads are more
   * expensive than sharded or hash partitioned access path reads in scale-out.
   */
  public final long estRead;

  /**
   * Create an object which encapsulates a sample of an edge.
   *
   * @param sourceSample The input sample.
   * @param limit The limit used to sample the edge (this is the limit on the #of solutions
   *     generated by the cutoff join used when this sample was taken).
   * @param inputCount The #of binding sets out of the source sample vertex sample which were
   *     consumed.
   * @param tuplesRead The #of tuples read from the access path when processing the cutoff join.
   * @param outputCount The #of binding sets generated before the join was cutoff.
   * @param adjustedCard The adjusted cardinality estimate for the cutoff join (this is
   *     <i>outputCount</i> as adjusted for a variety of edge conditions).
   */
  public EdgeSample(
      final SampleBase sourceSample,
      final int inputCount,
      final long tuplesRead,
      final long sumRangeCount,
      final long outputCount,
      final long adjustedCard,
      final double f,
      // args to SampleBase
      final long estCard,
      final long estRead,
      final int limit,
      final EstimateEnum estimateEnum,
      final IBindingSet[] sample) {

    super(estCard, limit, estimateEnum, sample);

    if (sourceSample == null) throw new IllegalArgumentException();

    this.sourceSample = sourceSample;

    this.inputCount = inputCount;

    this.tuplesRead = tuplesRead;

    this.sumRangeCount = sumRangeCount;

    this.outputCount = outputCount;

    this.adjCard = adjustedCard;

    this.f = f;

    this.estRead = estRead;
  }

  @Override
  protected void toString(final StringBuilder sb) {
    sb.append(", sourceEstCard=" + sourceSample.estCard);
    sb.append(", sourceEstimateEnum=" + sourceSample.estimateEnum);
    sb.append(", inputCount=" + inputCount);
    sb.append(", tuplesRead=" + tuplesRead);
    sb.append(", sumRangeCount=" + sumRangeCount);
    sb.append(", outputCount=" + outputCount);
    sb.append(", adjustedCard=" + adjCard);
    sb.append(", f=" + f);
    sb.append(", estRead=" + estRead);
  }
}
