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
 * Created on Dec 18, 2008
 */

package org.embergraph.journal;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.embergraph.service.AbstractFederation;
import org.embergraph.service.AbstractHATransactionService;

/*
 * Implementation for a standalone journal using single-phase commits.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public abstract class JournalTransactionService extends AbstractHATransactionService {

  private final Journal journal;

  /** @param properties */
  public JournalTransactionService(final Properties properties, final Journal journal) {

    super(properties);

    this.journal = journal;
  }

  @Override
  public JournalTransactionService start() {

    super.start();

    return this;
  }

  /** Extended to register the new tx in the {@link AbstractLocalTransactionManager}. */
  @Override
  protected void activateTx(final TxState state) {

    super.activateTx(state);

    //        if (TimestampUtility.isReadWriteTx(state.tx))
    {

      /*
       * Register transaction with the local transaction manager.
       */

      new Tx(journal.getLocalTransactionManager(), journal, state.tx, state.getReadsOnCommitTime());
    }
  }

  @Override
  protected void deactivateTx(final TxState state) {

    super.deactivateTx(state);

    //        if (TimestampUtility.isReadWriteTx(state.tx))
    {

      /*
       * Unregister transactions.
       */

      final Tx localState = journal.getLocalTransactionManager().getTx(state.tx);

      if (localState != null) {

        journal.getLocalTransactionManager().deactivateTx(localState);
      }
    }
  }

  @Override
  protected long findCommitTime(final long timestamp) {

    final ICommitRecord commitRecord = journal.getCommitRecord(timestamp);

    if (commitRecord == null) {

      return -1L;
    }

    return commitRecord.getTimestamp();
  }

  @Override
  protected long findNextCommitTime(final long commitTime) {

    /*
     * Note: The following code did not obtain the appropriate lock to
     * access the CommitRecordIndex. It was replaced by the
     * getCommitRecordStrictlyGreaterThan() call, which does take the
     * necessary lock and does the same thing.
     */
    //      final ICommitRecord commitRecord = journal.getCommitRecordIndex()
    //      .findNext(commitTime);

    final ICommitRecord commitRecord = journal.getCommitRecordStrictlyGreaterThan(commitTime);

    if (commitRecord == null) {

      return -1L;
    }

    return commitRecord.getTimestamp();
  }

  @Override
  protected void abortImpl(final TxState state) {

    if (state.isReadOnly()) {

      /*
       * A read-only transaction.
       *
       * Note: We do not maintain state on the client for read-only
       * transactions. The state for a read-only transaction is captured
       * by its transaction identifier and by state on the transaction
       * service, which maintains a read lock.
       */

      state.setRunState(RunState.Aborted);

      return;
    }

    try {

      /*
       * The local (client-side) state for this tx.
       */
      final Tx localState = journal.getLocalTransactionManager().getTx(state.tx);

      if (localState == null) {

        /*
         * The client should maintain the local state of the transaction
         * until the transaction service either commits or aborts the
         * tx.
         */

        throw new AssertionError("Local tx state not found: tx=" + state);
      }

      /*
       * Update the local state of the tx to indicate that it is aborted.
       *
       * Note: We do not need to issue an abort to the journal since
       * nothing is written by the transaction on the unisolated indices
       * until it has validated - and the validate/merge task is an
       * unisolated write operation, so the task's write set will be
       * automatically discarded if it fails.
       */

      localState.lock.lock();

      try {

        localState.setRunState(RunState.Aborted);

      } finally {

        localState.lock.unlock();
      }

    } finally {

      state.setRunState(RunState.Aborted);
    }
  }

  @Override
  protected long commitImpl(final TxState state) {

    if (state.isReadOnly()) {

      /*
       * A read-only transaction.
       *
       * Note: We do not maintain state on the client for read-only
       * transactions. The state for a read-only transaction is captured
       * by its transaction identifier and by state on the transaction
       * service, which maintains a read lock.
       */

      state.setRunState(RunState.Committed);

      return 0L;
    }

    final Tx localState = journal.getLocalTransactionManager().getTx(state.tx);

    if (localState == null) {

      throw new AssertionError("Not in local tables: " + state);
    }

    /*
     * Note: This code is shared (copy by value) by the DataService
     * singlePhaseCommit.
     */
    {

      /*
       * A transaction with an empty write set can commit immediately
       * since validation and commit are basically NOPs (this is the same
       * as the read-only case.)
       *
       * Note: We lock out other operations on this tx so that this
       * decision will be atomic.
       */

      localState.lock.lock();

      try {

        if (localState.isEmptyWriteSet()) {

          /*
           * Sort of a NOP commit.
           */

          localState.setRunState(RunState.Committed);

          journal.getLocalTransactionManager().deactivateTx(localState);

          state.setRunState(RunState.Committed);

          return 0L;
        }

      } finally {

        localState.lock.unlock();
      }
    }

    final IConcurrencyManager concurrencyManager = journal.getConcurrencyManager();

    final AbstractTask<Void> task =
        new SinglePhaseCommit(concurrencyManager, journal.getLocalTransactionManager(), localState);

    try {

      /*
       * Submit the task and wait for the result.
       *
       * Note: This task MUST go through the ConcurrencyManager to obtain
       * its locks.
       */
      concurrencyManager./* getWriteService(). */ submit(task).get();

      /*
       * FIXME The state changes for the local tx should be atomic across
       * this operation. In order to do that we have to make those changes
       * inside of SinglePhaseCommit while it is holding the lock, but after
       * it has committed. Perhaps the best way to do this is with a pre-
       * and post- call() API since we can not hold the lock across the
       * task otherwise (it will deadlock).
       */

      localState.lock.lock();

      try {

        localState.setRunState(RunState.Committed);

        journal.getLocalTransactionManager().deactivateTx(localState);

        state.setRunState(RunState.Committed);

      } finally {

        localState.lock.unlock();
      }

    } catch (Throwable t) {

      //            log.error(t.getMessage(), t);

      localState.lock.lock();

      try {

        localState.setRunState(RunState.Aborted);

        journal.getLocalTransactionManager().deactivateTx(localState);

        state.setRunState(RunState.Aborted);

        throw new RuntimeException(t);

      } finally {

        localState.lock.unlock();
      }
    }

    /*
     * Note: This is returning the commitTime set on the task when it was
     * committed as part of a group commit.
     */

    //        log.warn("\n" + state + "\n" + localState);

    return task.getCommitTime();
  }

  /*
   * This task is an UNISOLATED operation that validates and commits a transaction known to have
   * non-empty write sets.
   *
   * <p>Note: DO NOT {@link Tx#lock} while you submit this task as it could cause a deadlock if
   * there is a task ahead of you in the queue for the same tx!
   *
   * <p>Note: DO NOT use this task for a distributed transaction (one with writes on more than one
   * {@link DataService}) since it will fail to obtain a coherent commit time for the transaction as
   * a whole.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class SinglePhaseCommit extends AbstractTask<Void> {

    /** The transaction that is being committed. */
    private final Tx state;

    private final ILocalTransactionManager localTransactionManager;

    public SinglePhaseCommit(
        final IConcurrencyManager concurrencyManager,
        final ILocalTransactionManager localTransactionManager,
        final Tx state) {

      super(concurrencyManager, ITx.UNISOLATED, state.getDirtyResource());

      if (localTransactionManager == null) throw new IllegalArgumentException();

      this.localTransactionManager = localTransactionManager;

      this.state = state;
    }

    @Override
    public Void doTask() throws Exception {

      /*
       * Note: In this case the [revisionTime] will be LT the
       * [commitTime]. That's Ok as long as the issued revision times are
       * strictly increasing, which they are.
       */
      final long revisionTime = localTransactionManager.nextTimestamp();

      /*
       * Lock out other operations on this tx.
       */

      state.lock.lockInterruptibly();

      try {

        // Note: throws ValidationError.
        state.prepare(revisionTime);

      } finally {

        state.lock.unlock();
      }

      return null;
    }
  }

  /*
   * This task is an UNISOLATED operation that validates a transaction known to have non-empty write
   * sets.
   *
   * <p>Note: DO NOT {@link Tx#lock} while you submit this task as it could cause a deadlock if
   * there is a task ahead of you in the queue for the same tx!
   *
   * <p>Note: DO NOT use this task for a distributed transaction (one with writes on more than one
   * {@link DataService}) since it will fail to obtain a coherent commit time for the transaction as
   * a whole.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class ValidateWriteSetTask extends AbstractTask<Boolean> {

    /** The transaction that is being validated. */
    private final Tx state;

    public ValidateWriteSetTask(
        final IConcurrencyManager concurrencyManager,
        final ILocalTransactionManager localTransactionManager,
        final Tx state) {

      super(concurrencyManager, ITx.UNISOLATED, state.getDirtyResource());

      if (localTransactionManager == null) throw new IllegalArgumentException();

      this.state = state;
    }

    @Override
    public Boolean doTask() throws Exception {

      /*
       * Lock out other operations on this tx.
       */

      state.lock.lockInterruptibly();

      try {

        // Note: throws ValidationError.
        return state.validateWriteSets();

      } finally {

        state.lock.unlock();
      }
    }
  }

  /** The last commit time from the current root block. */
  @Override
  public final long getLastCommitTime() {

    return journal.getRootBlockView().getLastCommitTime();
  }

  /* @todo This is only true for the WORM.  For the RWStore, the release time
   * will advance normally and things can get aged out of the store.
   */
  //    /*
  //     * Ignored since the {@link Journal} records the last commit time
  //     * in its root blocks.
  //     */
  //    public void notifyCommit(long commitTime) {
  //
  //        // NOP
  //
  //    }

  /* @todo This is only true for the WORM.  For the RWStore, the release time
   * will advance normally and things can get aged out of the store.
   */
  //    /*
  //     * Always returns ZERO (0L) since history can not be released on the
  //     * {@link Journal}.
  //     */
  //    @Override
  //    public long getReleaseTime() {
  //
  //        return 0L;
  //
  //    }

  //    /*
  //     * Throws exception since distributed transactions are not used for a single
  //     * {@link Journal}.
  //     */
  //    @Override
  //    public long prepared(long tx, UUID dataService) throws IOException {
  //
  //        throw new UnsupportedOperationException();
  //
  //    }
  //
  //    /*
  //     * Throws exception since distributed transactions are not used for a single
  //     * {@link Journal}.
  //     */
  //    @Override
  //    public boolean committed(long tx, UUID dataService) throws IOException {
  //
  //        throw new UnsupportedOperationException();
  //
  //    }

  /*
   * Throws exception.
   *
   * @throws UnsupportedOperationException always.
   */
  @Override
  public AbstractFederation<?> getFederation() {

    throw new UnsupportedOperationException();
  }

  //	/*
  //	 * Invoke a method with the {@link AbstractTransactionService}'s lock held.
  //	 *
  //	 * @param <T>
  //	 * @param callable
  //	 * @return
  //	 * @throws Exception
  //	 */
  //	public <T> T callWithLock(final Callable<T> callable) throws Exception {
  //		lock.lock();
  //		try {
  //			return callable.call();
  //		} finally {
  //			lock.unlock();
  //		}
  //	}
  //
  //	/*
  //	 * Invoke a method with the {@link AbstractTransactionService}'s lock held.
  //	 *
  //	 * But throw immediate exception if try fails.
  //	 *
  //	 * @param <T>
  //	 * @param callable
  //	 * @return
  //	 * @throws Exception
  //	 */
  //	public <T> T tryCallWithLock(final Callable<T> callable, long waitFor, TimeUnit unit) throws
  // Exception {
  //		if (!lock.tryLock(waitFor,unit)) {
  //			throw new RuntimeException("Lock not available");
  //		}
  //		try {
  //			return callable.call();
  //		} finally {
  //			lock.unlock();
  //		}
  //	}

}
