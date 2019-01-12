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
 * Created on Jun 24, 2008
 */

package org.embergraph.relation.rule.eval;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.Journal;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.relation.accesspath.ChunkConsumerIterator;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.rule.IProgram;
import org.embergraph.relation.rule.IStep;
import org.embergraph.service.DataService;
import org.embergraph.service.DataServiceCallable;
import org.embergraph.service.IEmbergraphFederation;

/*
 * Task for executing a program when all of the indices for the relation are co-located on the same
 * {@link DataService}.
 *
 * @todo Named result sets. This would provide a means to run a IRuleTask and cache the output for
 *     further evaluation as a named result set. The backing store should be a temporary resource.
 *     for scale-out it needs to be visible to the federation (since the rule executing against that
 *     data may be distributed across the federation based on the access path for the SPORelation)
 *     so it would have to be registered on some data service (any) in the federation and dropped in
 *     a finally {} clause.
 *     <p>When the sets are large then they may need a backing store, e.g., EmbergraphSet<Long>
 *     (specialized so that it does not store anything under the key since we can decode the Long
 *     from the key - do utility versions EmbergraphLongSet(), but the same code can serve float,
 *     double, and int as well. Avoid override for duplicate keys to reduce IO.
 * @todo it should be possible to have a different action associated with each rule in the program,
 *     and to have a different target relation for the head of each rule on which we will write
 *     (mutation). Different query or mutation count results could be handled by an extension with
 *     "nextResultSet" style semantics. However, for now, all rules MUST write on the same buffer.
 *     Query results will therefore be multiplexed as will mutations counts.
 * @todo foreign key joins: it should be possible to handle different relation classes in the same
 *     rules, e.g., RDF and non-RDF relations. Or even the SPO and lexicon relation for the RDF DB
 *     -- the latter will be useful for materializing externalized statements efficiently.
 * @todo could make the return type a generic for {@link AbstractStepTask} and make this class a
 *     concrete implementation of that one.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class ProgramTask extends DataServiceCallable<Object> implements IProgramTask {

  /** */
  private static final long serialVersionUID = -7047397038429305180L;

  protected static final transient Logger log = Logger.getLogger(ProgramTask.class);

  private final ActionEnum action;

  private final IStep step;

  /*
   * Serializable field specified when the {@link ProgramTask} will be submitted (via RMI or not) to
   * a {@link DataService}. A new {@link IJoinNexus} is instantiated in the execution context on the
   * {@link DataService} from this field.
   */
  private final IJoinNexusFactory joinNexusFactory;

  /*
   * Note: NOT serialized! The {@link IIndexManager} will be set by {@link
   * #setDataService(DataService)} if this object submitted using {@link
   * DataService#submit(Callable)}.
   */
  private transient IIndexManager indexManager;

  //    /*
  //     * Note: NOT serialized!
  //     */
  //    private transient DataService dataService;

  @Override
  public void setDataService(final DataService dataService) {

    super.setDataService(dataService);

    this.indexManager = dataService.getFederation();
  }

  /*
   * Variant when the task will be submitted using {@link IDataService#submit(Callable)} (efficient
   * since all indices will be local, but the indices must not be partitioned and must all exist on
   * the target {@link DataService}).
   *
   * <p>Note: the caller MUST submit the {@link ProgramTask} using {@link
   * DataService#submit(Callable)} in which case {@link #dataService} field will be set (after the
   * ctor) by the {@link DataService} itself. The {@link DataService} will be used to identify an
   * {@link ExecutorService} and the {@link IJoinNexusFactory} will be used to establish access to
   * indices, relations, etc. in the context of the {@link AbstractTask} - see {@link
   * AbstractStepTask#submit()}.
   *
   * @param action
   * @param step
   * @param joinNexus
   */
  public ProgramTask(
      final ActionEnum action, final IStep step, final IJoinNexusFactory joinNexusFactory) {

    if (action == null) throw new IllegalArgumentException();

    if (step == null) throw new IllegalArgumentException();

    if (joinNexusFactory == null) throw new IllegalArgumentException();

    this.action = action;

    this.step = step;

    this.joinNexusFactory = joinNexusFactory;

    this.indexManager = null;
  }

  /*
   * Variant when the task will be executed directly by the caller.
   *
   * @param action
   * @param step
   * @param joinNexusFactory
   * @param indexManager
   * @throws IllegalArgumentException if any parameter is <code>null</code>.
   */
  public ProgramTask(
      final ActionEnum action,
      final IStep step,
      final IJoinNexusFactory joinNexusFactory,
      final IIndexManager indexManager) {

    if (action == null) throw new IllegalArgumentException();

    if (step == null) throw new IllegalArgumentException();

    if (joinNexusFactory == null) throw new IllegalArgumentException();

    if (indexManager == null) throw new IllegalArgumentException();

    this.action = action;

    this.step = step;

    this.joinNexusFactory = joinNexusFactory;

    this.indexManager = indexManager;
  }

  /*
   * Execute the program.
   *
   * <p>Note: There is no natural order for high-level query. Also, unless stable evaluation is
   * requested, the results can be produced by parallel threads and the order of the materialized
   * solution is therefore not even stable. The only way to have a natural order is for a sort to be
   * imposed on the {@link ISolution}s.
   *
   * @throws Exception
   */
  public Object call() throws Exception {

    if (log.isDebugEnabled()) {

      log.debug("begin: program=" + step.getName() + ", action=" + action);
    }

    try {

      final ProgramUtility util = new ProgramUtility();

      if (action.isMutation()) {

        final RuleStats totals;

        if (!step.isRule() && ((IProgram) step).isClosure()) {

          /*
           * Compute closure of a flat set of rules.
           */

          totals = executeClosure((IProgram) step);

        } else if (util.isClosureProgram(step)) {

          /*
           * Compute closure of a program that embedded closure
           * operations.
           */

          totals = executeProgramWithEmbeddedClosure((IProgram) step);

        } else {

          /*
           * Execute a mutation operation that does not use closure.
           */

          totals = executeMutation(step);
        }

        RuleLog.log(totals);

        return totals.mutationCount.get();

      } else {

        if ((!step.isRule() && ((IProgram) step).isClosure()) || util.isClosureProgram(step)) {

          /*
           * The step is either a closure program or embeds a closure
           * program.
           */

          throw new UnsupportedOperationException("Closure only allowed for mutation.");
        }

        /*
         * Execute a query.
         */
        return new ChunkConsumerIterator<ISolution>(executeQuery(step));
      }

    } finally {

      if (log.isDebugEnabled()) log.debug("bye");
    }
  }

  /*
   * Execute the {@link IStep} as a query.
   *
   * @param step The {@link IStep}.
   * @return The {@link IChunkedOrderedIterator} that will drain the {@link ISolution}s generated by
   *     the {@link IStep}. Execution will be cancelled if the iterator is {@link
   *     ICloseableIterator#close() closed}. If execution results in an error, then the iterator
   *     will throw a {@link RuntimeException} whose cause is the error.
   * @throws RuntimeException
   */
  protected IAsynchronousIterator<ISolution[]> executeQuery(final IStep step) {

    if (step == null) throw new IllegalArgumentException();

    if (log.isDebugEnabled()) log.debug("program=" + step.getName());

    // buffer shared by all rules run in this query.
    final IBlockingBuffer<ISolution[]> buffer =
        joinNexusFactory.newInstance(indexManager).newQueryBuffer();

    // the task to execute.
    final QueryTask queryTask =
        new QueryTask(
            step,
            joinNexusFactory,
            buffer,
            indexManager,
            isDataService() ? getDataService() : null);

    Future<RuleStats> future = null;

    try {

      /*
       * Note: We do NOT get() this Future. This task will run
       * asynchronously.
       *
       * The Future is canceled IF (hopefully WHEN) the iterator is
       * closed.
       *
       * If the task itself throws an error, then it will use
       * buffer#abort(cause) to notify the buffer of the cause (it will be
       * passed along to the iterator) and to close the buffer (the
       * iterator will notice that the buffer has been closed as well as
       * that the cause was set on the buffer).
       *
       * @todo if the #of results is small and they are available with
       * little latency then return the results inline using a fully
       * buffered iterator.
       *
       * Note: hack pattern to ensure Future is cancelled if we exit by
       * any code path before the future has been set on the BlockingBuffer.
       *
       * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/707">
       *      BlockingBuffer.close() does not unblock threads </a>
       */
      try {
        // run the task.
        future = queryTask.submit();

        // set the future on the BlockingBuffer.
        buffer.setFuture(future);
      } finally {
        if (future != null && buffer.getFuture() == null) {
          // Future exists but not set on BlockingBuffer.
          future.cancel(true /* mayInterruptIfRunning */);
        }
      }

      if (log.isDebugEnabled()) log.debug("Returning iterator reading on async query task");

      // return the async iterator.
      return buffer.iterator();

    } catch (Throwable ex) {

      try {

        log.error(ex, ex);

        throw new RuntimeException(ex);

      } finally {

        buffer.close();

        if (future != null) {

          future.cancel(true /* mayInterruptIfRunning */);
        }
      }
    }
  }

  /*
   * Run a mutation {@link IStep}. The {@link IStep} may consist of many sub-{@link IStep}s.
   *
   * <p>Note: If you specify {@link ITx#READ_COMMITTED} for mutation operations when using a
   * federation then concurrent split/join/move can cause the operation to fail. It is safer to use
   * read-consistent semantics by specifying {@link IIndexStore#getLastCommitTime()} instead.
   *
   * @param step The {@link IStep}.
   * @return Metadata about the program execution, including the required {@link
   *     RuleStats#mutationCount}.
   * @throws InterruptedException
   * @throws ExecutionException
   */
  protected RuleStats executeMutation(final IStep step)
      throws InterruptedException, ExecutionException {

    if (step == null) throw new IllegalArgumentException();

    if (!action.isMutation()) throw new IllegalArgumentException();

    long tx = 0L;
    try {

      /*
       * Note: The WORM Journal reads and writes against the unisolated
       * view for mutation operations. This works because it never
       * releases any written storage. For the RWStore we have to do
       * something different to ensure that the writes driven by the
       * mutation operation do not cause allocation slots to be read
       * against which we need to read. For the federation, we need to
       * protect against the release of the journal and index segment
       * resources against which we need to read. Both of those cases (RW
       * and federation) are handled using a read-only transaction to
       * protect the historical view against which the mutation rule will
       * read.
       */

      if (indexManager instanceof IEmbergraphFederation<?>) {

        /*
         * Advance the read-consistent timestamp so that any writes from
         * the previous rules or the last round are now visible.
         *
         * Note: The federation has shard-wise autoCommit semantics
         * which ensure that there will be a suitable commit point for
         * us to read against (as long as the caller flushed the
         * buffered writes before invoking closure, which they do).
         */

        final long lastCommitTime = indexManager.getLastCommitTime();

        try {
          /*
           * A read-only tx reading from the lastCommitTime.
           *
           * Note: This provides a read-lock on the commit time from
           * which the mutation task will read.
           *
           * @todo we could use the [tx] as the readTimestamp and we
           * could use ITx.READ_COMMITTED rather that explicitly
           * looking up the lastCommitTime.
           */
          tx =
              ((IEmbergraphFederation<?>) indexManager)
                  .getTransactionService()
                  .newTx(lastCommitTime);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }

        // the timestamp that we will read on for this step.
        joinNexusFactory.setReadTimestamp(TimestampUtility.asHistoricalRead(lastCommitTime));

      } else if (false
          && indexManager instanceof Journal
          && ((Journal) indexManager).getBufferStrategy().getBufferMode() == BufferMode.DiskRW) {

        /*
         * Do a commit and then advance the read-consistent timestamp so
         * that any writes from the previous rules or the last round are
         * now visible.
         *
         * Note: The RWStore needs a commit point and a read-only tx
         * against that commit point in order to protect from the reuse
         * of allocation slots during unisolated writes on the journal.
         *
         * @todo This could be captured with the notion of an checkpoint
         * on the journal without introducing a full commit IF the txs
         * was able to make sure of that checkpoint.
         */

        final Journal jnl = (Journal) indexManager;

        // force a commit before running the mutation rule.
        final long lastCommitTime = jnl.commit();

        /*
         * A read-only tx reading from the commit point we just
         * introduced.
         *
         * Note: This provides a read-lock on the commit time from which
         * the mutation task will read.
         */
        tx = jnl.newTx(lastCommitTime);

        // the timestamp that we will read on for this step.
        joinNexusFactory.setReadTimestamp(TimestampUtility.asHistoricalRead(lastCommitTime));
      }

      final MutationTask mutationTask =
          new MutationTask(
              action,
              joinNexusFactory,
              step,
              indexManager,
              isDataService() ? getDataService() : null);

      if (log.isDebugEnabled())
        log.debug(
            "begin: action=" + action + ", program=" + step.getName() + ", task=" + mutationTask);

      /*
       * Submit task and await completion, returning the result.
       *
       * Note: The task is responsible for computing the aggregate
       * mutation count.
       */
      return mutationTask.submit().get();

    } finally {

      if (tx != 0L) {
        /*
         * Terminate the read-only tx (releases the read-lock).
         */
        if (indexManager instanceof IEmbergraphFederation<?>) {
          try {
            ((IEmbergraphFederation<?>) indexManager).getTransactionService().abort(tx);
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        } else if (indexManager instanceof Journal) {
          ((Journal) indexManager).abort(tx);
        }
      }
    }
  }

  /*
   * Computes the closure of a set of {@link IRule}s until the relation(s) on which they are writing
   * reach a "fixed point".
   *
   * <p>The general approach is a series of rounds in which each rule is applied in turn (either
   * sequentially or in parallel, depending on the program). Solutions computed for each rule in
   * each round written onto the relation for the head of that rule. The process halts when no new
   * solutions are computed in a given round.
   *
   * <p>Note: When we are running the program on a {@link ConcurrencyManager}, each round of the
   * closure is submitted as a single {@link AbstractTask}. This allows us to do historical reads
   * during the round and to update the read-behind timestamp before each round. During the round,
   * the program will write on buffers that are flushed at the end of the round. Those buffers will
   * use unisolated writes onto the appropriate relations.
   *
   * <h2>mutation counts</h2>
   *
   * In order to detect the fixed point we MUST know whether or not any mutations were made to the
   * relation during the round. The design does NOT rely on the relation count before and after the
   * round since it would have to use an _exact_ range count for the relation (otherwise it is
   * possible that a deleted tuple would be overwritten by a computed entailment but that the count
   * would not change). However, the exact range count is relatively expensive which is why the
   * design insists on getting back the #of elements actually written on the index from each rule in
   * each round. If no rules in a given round caused an element to be written, then we are at the
   * fixed point.
   *
   * <p>Note: This assumes that you are following the {@link IMutableRelation} contract -- you MUST
   * NOT overwrite tuples with the same key and value, or at least you must not report such "do
   * nothing" overwrites in the mutation count!!!
   *
   * @param action The action (must be a mutation operation).
   * @param program The program to be executed.
   * @throws ExecutionException
   * @throws InterruptedException
   */
  protected RuleStats executeClosure(final IProgram program)
      throws InterruptedException, ExecutionException {

    if (program == null) throw new IllegalArgumentException();

    if (!program.isClosure()) throw new IllegalArgumentException();

    final long begin = System.currentTimeMillis();

    final RuleStats totals =
        joinNexusFactory.newInstance(indexManager).getRuleStatisticsFactory().newInstance(program);

    int round = 1;

    long mutationCount = 0L;
    while (true) {

      // mutationCount before this round.
      final long mutationCount0 = totals.mutationCount.get();

      if (log.isDebugEnabled())
        log.debug("round=" + round + ", mutationCount(before)=" + mutationCount0);

      //            if (round > 1) {
      //
      //                /*
      //                 * Advance the read-consistent timestamp so that any writes from
      //                 * the previous rules or the last round are now visible.
      //                 */
      //
      //                joinNexusFactory.setReadTimestamp(TimestampUtility
      //                        .asHistoricalRead(indexManager.getLastCommitTime()));
      //
      //            }

      // execute the program.
      final RuleStats tmp = executeMutation(program);

      /*
       * This is the #of mutations from executing this round.
       *
       * Note: each round has its own mutation buffer so this is just the
       * #of mutations in the round. This is because executeMutation()
       * builds a new execution context for each round.
       */
      final long mutationDelta = tmp.mutationCount.get();

      // Total mutation count so far.
      final long mutationCount1 = mutationCount = mutationCount0 + tmp.mutationCount.get();

      // set the round identifier.
      tmp.closureRound = round;

      // Aggregate the rule statistics, but not mutationCount.
      totals.add(tmp);

      if (log.isDebugEnabled()) {

        log.debug(
            "round# "
                + round
                + ", mutationCount(before="
                + mutationCount0
                + ", after="
                + mutationCount1
                + ", delta="
                + mutationDelta
                + "):"
                + totals);
      }

      if (mutationDelta == 0L) break;

      round++;
    }

    final long elapsed = System.currentTimeMillis() - begin;

    if (!totals.mutationCount.compareAndSet(0L, mutationCount)) {

      throw new AssertionError("mutationCount=" + totals.mutationCount);
    }

    if (log.isInfoEnabled()) {

      log.info(
          "\nComputed fixed point: program="
              + program.getName()
              + ", rounds="
              + round
              + ", elapsed="
              + elapsed
              + "ms");
    }

    return totals;
  }

  /*
   * Execute an {@link IProgram} containing one or more sub-{@link IProgram} that are closure
   * operations. The top-level program must not be a closure operation. All steps above the closure
   * operations will be run in a sequence. The closure operations themselves will be executed using
   * {@link #executeClosure(IProgram)}.
   *
   * <p>Note: Any program that embeds a closure operation must be sequential (this is enforced by
   * the Program class).
   *
   * <p>Note: Programs that use closure operations are constrained to either (a) a fix point of a
   * (normally parallel) program consisting solely of {@link IRule}s; or (b) a sequential program
   * containing some steps that are the fix point of a (normally parallel) program consisting solely
   * of {@link IRule}s.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   * @todo this will not correctly handle programs use closure in a sub-sub-program.
   * @throws IllegalArgumentException if <i>program</i> is <code>null</code>
   * @throws IllegalArgumentException if <i>program</i> is <em>itself</em> a closure operation.
   * @throws IllegalStateException unless the {@link ActionEnum} is a mutation operation.
   */
  protected RuleStats executeProgramWithEmbeddedClosure(final IProgram program)
      throws InterruptedException, ExecutionException {

    if (program == null) throw new IllegalArgumentException();

    if (program.isClosure()) throw new IllegalArgumentException();

    if (!action.isMutation()) {
      throw new IllegalStateException();
    }

    if (log.isInfoEnabled()) log.info("program embeds closure operations");

    final RuleStats totals =
        joinNexusFactory.newInstance(indexManager).getRuleStatisticsFactory().newInstance(program);

    final Iterator<? extends IStep> itr = (program).steps();

    long mutationCount = 0L;
    while (itr.hasNext()) {

      final IStep step = itr.next();

      final RuleStats stats;

      if (!step.isRule() && ((IProgram) step).isClosure()) {

        // A closure step.
        stats = executeClosure((IProgram) step);

      } else {

        // A non-closure step.
        stats = executeMutation(step);
      }

      totals.add(stats);

      /*
       * Note: both a executeClosure() and executeMutation() will run with
       * their own buffers so flush() reporting is not carried forward
       * beyond those methods. Hence we have to aggregate the
       * mutationCount ourselves for each step that we run.
       */
      mutationCount += stats.mutationCount.get();
    }

    // transfer the final mutation count onto the total.
    if (!totals.mutationCount.compareAndSet(0L, mutationCount)) {

      throw new AssertionError("mutationCount=" + totals.mutationCount);
    }

    return totals;
  }
}
