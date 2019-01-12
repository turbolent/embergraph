/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General License for more details.

You should have received a copy of the GNU General License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Sep 5, 2010
 */

package org.embergraph.bop.engine;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Map;
import java.util.UUID;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IQueryContext;
import org.embergraph.journal.IIndexManager;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.util.concurrent.IHaltable;

/*
 * Non-Remote interface exposing a limited set of the state of an executing query.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRunningQuery extends IHaltable<Void>, IQueryContext {

  /** The query. */
  BOp getQuery();

  /** The unique identifier for this query. */
  @Override
  UUID getQueryId();

  /*
   * The {@link IEmbergraphFederation} IFF the operator is being evaluated on an {@link
   * IEmbergraphFederation}. When evaluating operations against an {@link IEmbergraphFederation},
   * this reference provides access to the scale-out view of the indices and to other embergraph
   * services.
   */
  IEmbergraphFederation<?> getFederation();

  /*
   * The <strong>local</strong> {@link IIndexManager}. Query evaluation occurs against the local
   * indices. In scale-out, query evaluation proceeds shard wise and this {@link IIndexManager} MUST
   * be able to read on the {@link ILocalBTreeView}.
   */
  IIndexManager getLocalIndexManager();

  /** The query engine class executing the query on this node. */
  QueryEngine getQueryEngine();

  /*
   * The client coordinate the evaluation of this query (aka the query controller). For a standalone
   * database, this will be the {@link QueryEngine}.
   *
   * <p>For scale-out, this will be the RMI proxy for the {@link QueryEngine} instance to which the
   * query was submitted for evaluation by the application. The proxy is primarily for light weight
   * RMI messages used to coordinate the distributed query evaluation. Ideally, all large objects
   * will be transfered among the nodes of the cluster using NIO buffers.
   */
  IQueryClient getQueryController();

  /*
   * Return an unmodifiable index from {@link BOp.Annotations#BOP_ID} to {@link BOp}. This index may
   * contain operators which are not part of the pipeline evaluation, such as {@link IPredicate}s.
   */
  Map<Integer /*bopId*/, BOp> getBOpIndex();

  /*
   * Return an unmodifiable map exposing the statistics for the operators in the query and <code>
   * null</code> unless this is the query controller. There will be a single entry in the map for
   * each distinct {@link PipelineOp}. Entries might not appear until that operator has either begun
   * or completed at least one evaluation phase. This index only contains operators which are
   * actually part of the pipeline evaluation.
   */
  Map<Integer /* bopId */, BOpStats> getStats();

  /** Set the static analysis stats associated with this query. */
  void setStaticAnalysisStats(StaticAnalysisStats saStats);

  /** Return statistics associated with the static analysis phase of this query. */
  StaticAnalysisStats getStaticAnalysisStats();

  /*
   * Return the query deadline in milliseconds (the time at which it will terminate regardless of
   * its run state).
   *
   * @return The query deadline (milliseconds since the epoch) and {@link Long#MAX_VALUE} if no
   *     explicit deadline was specified.
   */
  long getDeadline();

  /** The timestamp (ms) when the query began execution. */
  long getStartTime();

  /** The timestamp (ms) when the query was done and ZERO (0) if the query is not yet done. */
  long getDoneTime();

  /*
   * The elapsed time (ms) for the query. This will be updated for each call until the query is done
   * executing.
   */
  long getElapsed();

  //	/*
  //	 * Return <code>true</code> if there are no operators which could
  //	 * (re-)trigger the specified operator.
  //	 * <p>
  //	 * Note: This is intended to be invoked synchronously from within the
  //	 * evaluation of the operator in order to determine whether or not the
  //	 * operator can be invoked again for this running query.
  //	 *
  //	 * @param bopId
  //	 *            The specified operator.
  //	 * @param nconsumed
  //	 *            The #of {@link IChunkMessage} consumed by the operator during
  //	 *            its current invocation.
  //	 *
  //	 * @return <code>true</code> iff it is not possible for the specified
  //	 *         operator to be retriggered.
  //	 */
  //    boolean isLastInvocation(final int bopId,final int nconsumed);

  //    /*
  //     * Cancel the running query (normal termination).
  //     * <p>
  //     * Note: This method provides a means for an operator to indicate that the
  //     * query should halt immediately for reasons other than abnormal
  //     * termination.
  //     * <p>
  //     * Note: For abnormal termination of a query, just throw an exception out of
  //     * the query operator implementation.
  //     */
  //    void halt();
  //
  //    /*
  //     * Cancel the query (abnormal termination).
  //     *
  //     * @param t
  //     *            The cause.
  //     *
  //     * @return The argument.
  //     *
  //     * @throws IllegalArgumentException
  //     *             if the argument is <code>null</code>.
  //     */
  //    Throwable halt(final Throwable t);
  //
  //    /*
  //     * Return the cause if the query was terminated by an exception.
  //     * @return
  //     */
  //    Throwable getCause();

  /*
   * Return an iterator which will drain the solutions from the query. The query will be cancelled
   * if the iterator is {@link ICloseableIterator#close() closed}.
   *
   * @throws UnsupportedOperationException if this is not the query controller.
   */
  ICloseableIterator<IBindingSet[]> iterator();
}
