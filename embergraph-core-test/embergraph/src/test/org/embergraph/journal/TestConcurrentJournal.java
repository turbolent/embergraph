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
 * Created on Oct 3, 2007
 */

package org.embergraph.journal;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.journal.AbstractInterruptsTestCase.InterruptMyselfTask;
import org.embergraph.journal.AbstractTask.ResubmitException;
import org.embergraph.journal.ConcurrencyManager.Options;
import org.embergraph.util.DaemonThreadFactory;

/*
 * Test suite for the {@link IConcurrencyManager} interface on the {@link Journal}.
 *
 * @todo write test cases that submit various kinds of operations and verify the correctness of
 *     those individual operations. refactor the services package to do this, including things such
 *     as the {@link DataServiceTupleIterator}. this will help to isolate the correctness of the
 *     data service "api", including concurrency of operations, from the {@link DataService}.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestConcurrentJournal extends ProxyTestCase<Journal> {

  public TestConcurrentJournal() {
    super();
  }

  public TestConcurrentJournal(String name) {
    super(name);
  }

  /*
   * Test ability to create a {@link Journal} and then shut it down (in particular this is testing
   * shutdown of the thread pool on the {@link ConcurrencyManager}).
   */
  public void test_shutdown() {

    final Journal journal = new Journal(getProperties());

    try {

      journal.shutdown();

    } finally {

      journal.destroy();
    }
  }

  public void test_shutdownNow() {

    final Journal journal = new Journal(getProperties());

    try {

      journal.shutdownNow();

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits an unisolated task to the read service and verifies that it executes.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_readService_01() throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final String resource = "foo";

      final AtomicBoolean ran = new AtomicBoolean(false);

      final Future<String> future =
          journal.submit(
              new AbstractTask<String>(journal, ITx.READ_COMMITTED, resource) {

                /*
                 * The task just sets a boolean value and returns the name of the sole resource. It
                 * does not actually read anything.
                 */
                @Override
                protected String doTask() {

                  ran.compareAndSet(false, true);

                  return getOnlyResource();
                }
              });

      // the test task returns the resource as its value.
      assertEquals("result", resource, future.get());

      /*
       * make sure that the flag was set (not reliably set until we get()
       * the future).
       */
      assertTrue("ran", ran.get());

      /*
       * Verify that a commit was NOT performed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits an unisolated task to the write service and verifies that it executes.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_writeService_01() throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final String resource = "foo";

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final Future<String> future =
          journal.submit(
              new AbstractTask<String>(journal, ITx.UNISOLATED, resource) {

                /*
                 * The task just sets a boolean value and returns the name of the sole resource. It
                 * does not actually read or write on anything.
                 */
                @Override
                protected String doTask() {

                  ran.compareAndSet(false, true);

                  return getOnlyResource();
                }
              });

      // the test task returns the resource as its value.
      assertEquals("result", resource, future.get());

      /*
       * make sure that the flag was set (not reliably set until we get()
       * the future).
       */
      assertTrue("ran", ran.get());

      /*
       * Verify that the commit counter was changed.
       */
      assertEquals(
          "commit counter unchanged?",
          commitCounterBefore + 1,
          journal.getRootBlockView().getCommitCounter());

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits an read-only task to the transaction service and verifies that it executes.
   *
   * @todo this test is somewhat odd since there are no commits on the journal when we request a
   *     read-only transaction. Potentially that should be illegal since there is nothing available
   *     to read. Certainly the returned transaction identifier MUST NOT be ZERO (0) since that
   *     would indicate an unisolated operation!
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_txService_readOnly_01() throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final String resource = "foo";

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final long tx = journal.newTx(ITx.READ_COMMITTED);

      assertNotSame(ITx.UNISOLATED, tx);

      final Future<String> future =
          journal.submit(
              new AbstractTask<String>(journal, tx, resource) {

                /*
                 * The task just sets a boolean value and returns the name of the sole resource. It
                 * does not actually read or write on anything.
                 */
                @Override
                protected String doTask() {

                  ran.compareAndSet(false, true);

                  return getOnlyResource();
                }
              });

      // the test task returns the resource as its value.
      assertEquals("result", resource, future.get());

      /*
       * make sure that the flag was set (not reliably set until we get()
       * the future).
       */
      assertTrue("ran", ran.get());

      /*
       * Verify that the commit counter was NOT changed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

      // commit of a read-only tx returns commitTime of ZERO(0L).
      assertEquals(0L, journal.commit(tx));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits a read-committed task to the transaction service and verifies that it executes.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_txService_readCommitted_01()
      throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final String resource = "foo";

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final long tx = ITx.READ_COMMITTED;
      // final long tx = journal.newTx(IsolationEnum.ReadCommitted);
      // assertNotSame(ITx.UNISOLATED, tx);

      final Future<String> future =
          journal.submit(
              new AbstractTask<String>(journal, tx, resource) {

                /*
                 * The task just sets a boolean value and returns the name of the sole resource. It
                 * does not actually read or write on anything.
                 */
                @Override
                protected String doTask() {

                  ran.compareAndSet(false, true);

                  return getOnlyResource();
                }
              });

      // the test task returns the resource as its value.
      assertEquals("result", resource, future.get());

      /*
       * make sure that the flag was set (not reliably set until we get()
       * the future).
       */
      assertTrue("ran", ran.get());

      /*
       * Verify that the commit counter was NOT changed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

      // // commit of a readCommitted tx returns commitTime of ZERO(0L).
      // assertEquals(0L,journal.commit(tx));
      // should be illegal since this is not a full transaction.
      try {
        journal.abort(tx);
        fail("Expecting: " + IllegalStateException.class);
      } catch (IllegalStateException ex) {
        log.info("Ignoring expected exception: " + ex);
      }

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits a read-write task with an empty write set to the transaction service and verifies that
   * it executes.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_txService_readWrite_01() throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final String resource = "foo";

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final long tx = journal.newTx(ITx.UNISOLATED);

      assertNotSame(ITx.UNISOLATED, tx);

      final Future<String> future =
          journal.submit(
              new AbstractTask<String>(journal, tx, resource) {

                /*
                 * The task just sets a boolean value and returns the name of the sole resource. It
                 * does not actually read or write on anything.
                 */
                @Override
                protected String doTask() {

                  ran.compareAndSet(false, true);

                  return getOnlyResource();
                }
              });

      // the test task returns the resource as its value.
      assertEquals("result", resource, future.get());

      /*
       * make sure that the flag was set (not reliably set until we get()
       * the future).
       */
      assertTrue("ran", ran.get());

      /*
       * Verify that the commit counter was NOT changed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

      // commit of a readWrite tx with an empty result set returns
      // commitTime
      // of ZERO(0L).
      assertEquals(0L, journal.commit(tx));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits an unisolated task to the write service. The task just sleeps. We then verify that we
   * can interrupt that task using {@link Future#cancel(boolean)} with <code>
   * mayInterruptWhileRunning := true</code> and that an appropriate exception is thrown in the main
   * thread.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_interrupt01() throws InterruptedException, ExecutionException {

    final Properties properties = getProperties();

    final Journal journal = new Journal(properties);

    try {

      // Note:
      properties.setProperty(Options.SHUTDOWN_TIMEOUT, "500");

      final String[] resource = new String[] {"foo"};

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final Future<Void> future =
          journal.submit(
              new AbstractTask<Void>(journal, ITx.UNISOLATED, resource) {

                /** The task just sets a boolean value and then sleeps. */
                @Override
                protected Void doTask() throws Exception {

                  ran.compareAndSet(false, true);

                  while (true) {

                    if (Thread.interrupted()) {

                      if (log.isInfoEnabled()) log.info("Interrupted.");

                      /*
                       * Note: If you simply continue processing rather
                       * than throwing an exception then the interrupt is
                       * _ignored_.
                       */

                      throw new InterruptedException("Task was interrupted");
                    }

                    /*
                     * Note: this will notice if the Thread is interrupted.
                     */

                    Thread.sleep(Long.MAX_VALUE);
                  }
                }
              });

      // wait until the task starts executing.

      while (!ran.get()) {

        Thread.sleep(100);
      }

      // interrupts and cancels the task.
      assertTrue(future.cancel(true /* mayInterruptWhileRunning */));

      // the task was cancelled.
      assertTrue(future.isCancelled());

      // verify that get() throws the expected exception.
      try {

        future.get();

        fail("Expecting: " + CancellationException.class);

      } catch (CancellationException ex) {

        if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
      }

      /*
       * Verify that the commit counter was changed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

    } finally {

      journal.destroy();
    }
  }

  /*
   * Submits an unisolated task to the write service. The task just sleeps. We then verify that we
   * can terminate that task using {@link Future#cancel(boolean)} with <code>
   * mayInterruptWhileRunning := false</code> and that an appropriate exception is thrown in the
   * main thread when we {@link Future#get()} the result of the task.
   *
   * <p>Note: {@link FutureTask#cancel(boolean)} is able to return control to the caller without
   * being allowed to interrupt the task. However, it does NOT terminate the task - the worker
   * thread is still running. Once the main thread reaches {@link Journal#shutdown()} it awaits the
   * termination of the {@link WriteExecutorService}. However that service does NOT terminate
   * because that worker thread is still running an infinite loop.
   *
   * <p>Eventually, we interrupt the thread directly using a reference to it we obtained when
   * setting up the task. This allows the journal to terminate and the test to complete.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_submit_interrupt02() throws InterruptedException, ExecutionException {

    final Properties properties = getProperties();

    properties.setProperty(Options.SHUTDOWN_TIMEOUT, "500");

    final Journal journal = new Journal(properties);

    // the thread that we need to eventually interrupt.
    final AtomicReference<Thread> t = new AtomicReference<>(null);

    try {

      final String[] resource = new String[] {"foo"};

      final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();

      final AtomicBoolean ran = new AtomicBoolean(false);

      final Future<Void> future =
          journal.submit(
              new AbstractTask<Void>(journal, ITx.UNISOLATED, resource) {

                /*
                 * The task just sets a boolean value and then runs an infinite loop,
                 * <strong>ignoring interrupts</strong>.
                 */
                @Override
                protected Void doTask() {

                  t.set(Thread.currentThread());

                  ran.compareAndSet(false, true);

                  while (true) {
                    try {
                      Thread.sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ex) {
                      /* ignore */
                      if (log.isInfoEnabled()) log.info("Ignoring interrupt: " + ex);

                      break;
                    }
                  }

                  return null;
                }
              });

      // wait until the task starts executing.

      while (!ran.get()) {

        Thread.sleep(100);
      }

      // we have a reference to the thread running the task.
      assertNotNull(t.get());

      // this terminates the task, but does NOT interrupt it.
      assertTrue(future.cancel(false /* mayInterruptWhileRunning */));

      // the task was cancelled.
      assertTrue(future.isCancelled());

      // verify that get() throws the expected exception.
      try {

        future.get();

        fail("Expecting: " + CancellationException.class);

      } catch (CancellationException ex) {

        if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
      }

      /*
       * Verify that the commit counter was NOT changed.
       */
      assertEquals(
          "commit counter changed?",
          commitCounterBefore,
          journal.getRootBlockView().getCommitCounter());

      // Verify that the thread was not interupted.
      assertFalse(t.get().isInterrupted());

      // Sleep a bit
      Thread.sleep(250);

      // Verify that the thread is not interrupted.
      assertFalse(t.get().isInterrupted());

      // Interrupt the thread so the Journal will terminate.
      t.get().interrupt();

    } finally {

      journal.destroy();
    }
  }

  /// *
  // * Note: This test no longer matches new semantics for the
  // * {@link WriteExecutorService}. A failed unisolated task will now return as
  // * soon as we rollback the indices to their last checkpoint (which we do just by
  // * closing the index).
  // *
  // */
  //    /*
  //     * This task verifies that an abort will wait until all running tasks
  //     * complete (ie, join the "abort group") before aborting.
  //     * <p>
  //     * If the abort occurs while task(s) are still running then actions by those
  //     * tasks can break the concurrency control mechanisms. For example, writes
  //     * on unisolated indices by tasks in the abort group could be made restart
  //     * safe with the next commit. However, the lock system SHOULD still keep new
  //     * tasks from writing on resources locked by tasks that have not yet
  //     * terminated.
  //     */
  //    public void test_submit_interrupt03() throws InterruptedException, ExecutionException {
  //
  //        Properties properties = getProperties();
  //
  //        Journal journal = new Journal(properties);
  //
  //        try {
  //
  //            final String[] resource = new String[] { "foo" };
  //
  //            {
  //                journal.registerIndex(resource[0]);
  //                journal.commit();
  //            }
  //
  //            final long commitCounterBefore = journal.getRootBlockView()
  //                    .getCommitCounter();
  //
  //            /*
  //             * 4 - done.
  //             */
  //            final AtomicInteger runState = new AtomicInteger(0);
  //
  //            final Lock lock = new ReentrantLock();
  //
  //            /*
  //             * Used to force an abort.
  //             */
  //            class PrivateException extends RuntimeException {
  //
  //                private static final long serialVersionUID = 1L;
  //
  //                PrivateException(String msg) {
  //                    super(msg);
  //                }
  //
  //            }
  //
  //            Future<Object> f1 = journal.submit(new AbstractTask(journal,
  //                    ITx.UNISOLATED, resource) {
  //
  //                /*
  //                 */
  //                protected Object doTask() throws Exception {
  //
  //                    runState.compareAndSet(0, 1);
  //
  //                    // low-level write so there is some data to be committed.
  //                    getJournal().write(getRandomData());
  //
  //                    // wait more before exiting.
  //                    {
  //                        final long begin = System.currentTimeMillis();
  //                        final long timeout = 5000; // ms
  //                        log.warn("Sleeping " + timeout + "ms");
  //                        try {
  //                            final long elapsed = System.currentTimeMillis()
  //                                    - begin;
  //                            Thread.sleep(timeout - elapsed/* ms */);
  //                        } catch (InterruptedException ex) {
  //                            log.warn(ex);
  //                        }
  //                    }
  //
  //                    lock.lock();
  //                    try {
  //                        runState.compareAndSet(1, 4);
  //                        log.warn("Done");
  //                    } finally {
  //                        lock.unlock();
  //                    }
  //
  //                    return null;
  //
  //                }
  //
  //            });
  //
  //            // force async abort of the commit group.
  //
  //            Future<Object> f2 = journal.submit(new AbstractTask(journal,
  //                    ITx.UNISOLATED, new String[] {}) {
  //
  //                protected Object doTask() throws Exception {
  //
  //                    log
  //                            .warn("Running task that will force abort of the commit group.");
  //
  //                    // low-level write.
  //                    getJournal().write(getRandomData());
  //
  //                    throw new PrivateException(
  //                            "Forcing abort of the commit group.");
  //
  //                }
  //
  //            });
  //
  //            // Verify that the commit counter was NOT changed.
  //            assertEquals("commit counter changed?", commitCounterBefore,
  //                    journal.getRootBlockView().getCommitCounter());
  //
  //            /*
  //             * Verify that the write service waits for the interrupted task to
  //             * join the abort group.
  //             */
  //            log.warn("Waiting for 1st task to finish");
  //            while (runState.get() < 4) {
  //
  //                lock.lock();
  //                try {
  //                    if (runState.get() < 4) {
  //                        // No abort yet.
  //                        assertEquals("Not expecting abort", 0, journal
  //                                .getConcurrencyManager().writeService
  //                                .getAbortCount());
  //                    }
  //                } finally {
  //                    lock.unlock();
  //                }
  //                Thread.sleep(20);
  //
  //            }
  //            log.warn("Reached runState=" + runState);
  //
  //            // wait for the abort or a timeout.
  //            {
  //                log.warn("Waiting for the abort.");
  //                final long begin = System.currentTimeMillis();
  //                while ((begin - System.currentTimeMillis()) < 1000) {
  //                    if (journal.getConcurrencyManager().writeService
  //                            .getAbortCount() > 0) {
  //                        log.warn("Noticed abort");
  //                        break;
  //                    } else {
  //                        // Verify that the commit counter was NOT changed.
  //                        assertEquals("commit counter changed?",
  //                                commitCounterBefore, journal.getRootBlockView()
  //                                        .getCommitCounter());
  //                    }
  //                }
  //            }
  //
  //            // verify did abort.
  //            assertEquals("Expecting abort", 1,
  //                    journal.getConcurrencyManager().writeService
  //                            .getAbortCount());
  //
  //            /*
  //             * Verify that both futures are complete and that both throw
  //             * exceptions since neither task was allowed to commit.
  //             *
  //             * Note: f1 completed successfully but the commit group was
  //             * discarded when f2 threw an exception. Therefore f1 sees an inner
  //             * RetryException while f2 sees the exception that it threw as its
  //             * inner exception.
  //             */
  //            {
  //
  //                try {
  //                    f1.get();
  //                    fail("Expecting exception.");
  //                } catch (ExecutionException ex) {
  //                    assertTrue(isInnerCause(ex, RetryException.class));
  //                    log.warn("Expected exception: " + ex, ex);
  //                }
  //                try {
  //                    f2.get();
  //                    fail("Expecting exception.");
  //                } catch (ExecutionException ex) {
  //                    log.warn("Expected exception: " + ex, ex);
  //                    assertTrue(isInnerCause(ex, PrivateException.class));
  //                }
  //            }
  //
  //        } finally {
  //
  //            journal.shutdown();
  //
  //            journal.deleteResources();
  //
  //        }
  //
  //    }

  /*
   * Verify that an {@link AbstractTask} correctly rejects an attempt to submit the same instance
   * twice. This is important since the base class has various items of state that are not
   * thread-safe and are not designed to be reusable.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_tasksAreNotThreadSafe() throws InterruptedException, ExecutionException {

    final Journal journal = new Journal(getProperties());

    try {

      final String[] resource = new String[] {"foo"};

      /*
       * Note: this task is stateless
       */
      final AbstractTask<Void> task =
          new AbstractTask<Void>(journal, ITx.UNISOLATED, resource) {

            @Override
            protected Void doTask() {

              return null;
            }
          };

      /*
       * Note: We have to request the result of the task before
       * re-submitting the task again since the duplicate instance is
       * being silently dropped otherwise - I expect that there is a hash
       * set involved somewhere such that duplicates can not exist in the
       * queue.
       */
      journal.submit(task).get();

      /*
       * Submit the task again - it will fail.
       */
      try {

        journal.submit(task).get();

        fail("Expecting: " + ResubmitException.class);

      } catch (ExecutionException ex) {

        if (ex.getCause() instanceof ResubmitException) {

          if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);

        } else {

          fail("Expecting: " + ResubmitException.class);
        }
      }

    } finally {

      journal.destroy();
    }
  }

  /*
   * @todo revisit this unit test.  It's semantics appear to have aged.
   */
  //    /*
  //     * Test verifies that an {@link ITx#UNISOLATED} task failure does not cause
  //     * concurrent writers to abort. The test also verifies that the
  //     * {@link Checkpoint} record for the named index is NOT updated since none
  //     * of the tasks write anything on the index.
  //     *
  //     * @todo The assumptions for this test may have been invalidated by the
  //     *       recent (4/29) changes to the group commit and task commit protocol
  //     *       and this test might need to be reworked or rewritten.
  //     */
  //    public void test_writeService001() throws Exception {
  //
  //        final Journal journal = new Journal(getProperties());
  //
  //        try {
  //
  //            final String name = "test";
  //
  //            // Note: checkpoint for the newly registered index.
  //            final long checkpointAddr0;
  //            {
  //
  //                journal.registerIndex(name,new IndexMetadata(name,UUID.randomUUID()));
  //
  //                journal.commit();
  //
  //                checkpointAddr0 = journal.getIndex(name).getCheckpoint()
  //                        .getCheckpointAddr();
  //
  //            }
  //
  //            // the list of tasks to be run.
  //            final List<AbstractTask<Object>> tasks = new LinkedList<AbstractTask<Object>>();
  //
  //            // NOP
  //            tasks.add(new AbstractTask(journal, ITx.UNISOLATED, name) {
  //                protected String getTaskName() {
  //                    return "a";
  //                }
  //                protected Object doTask() throws Exception {
  //                    assertEquals(checkpointAddr0, ((BTree) getIndex(name))
  //                            .getCheckpoint().getCheckpointAddr());
  //                    return null;
  //                }
  //            });
  //
  //            // throws exception.
  //            tasks.add(new AbstractTask(journal, ITx.UNISOLATED, name) {
  //                protected String getTaskName() {
  //                    return "b";
  //                }
  //                protected Object doTask() throws Exception {
  //                    assertEquals(checkpointAddr0, ((BTree) getIndex(name))
  //                            .getCheckpoint().getCheckpointAddr());
  //                    throw new ForcedAbortException();
  //                }
  //            });
  //
  //            // NOP
  //            tasks.add(new AbstractTask(journal, ITx.UNISOLATED, name) {
  //                protected String getTaskName() {
  //                    return "c";
  //                }
  //                protected Object doTask() throws Exception {
  //                    assertEquals(checkpointAddr0, ((BTree) getIndex(name))
  //                            .getCheckpoint().getCheckpointAddr());
  //                    return null;
  //                }
  //            });
  //
  //            // the commit counter before we submit the tasks.
  //            final long commitCounter0 = journal.getRootBlockView()
  //                    .getCommitCounter();
  //
  //            // the write service on which the tasks execute.
  //            final WriteExecutorService writeService = journal
  //                    .getConcurrencyManager().getWriteService();
  //
  //            // the group commit count before we submit the tasks.
  //            final long groupCommitCount0 = writeService.getGroupCommitCount();
  //
  //            // the abort count before we submit the tasks.
  //            final long abortCount0 = writeService.getAbortCount();
  //
  //            // the #of failed tasks before we submit the tasks.
  //            final long failedTaskCount0 = writeService.getTaskFailedCount();
  //
  //            // the #of successfully tasks before we submit the tasks.
  //            final long successTaskCount0 = writeService.getTaskSuccessCount();
  //
  //            // the #of successfully committed tasks before we submit the tasks.
  //            final long committedTaskCount0 = writeService.getTaskCommittedCount();
  //
  //            // submit the tasks and await their completion.
  //            final List<Future<Object>> futures = journal.invokeAll( tasks );
  //
  //            /*
  //             * verify the #of commits on the journal is unchanged since nothing
  //             * is written by any of these tasks.
  //             *
  //             * The expectation is that the tasks that succeed make it into the
  //             * same commit group while the task that throws an exception does
  //             * not cause the commit group to be aborted.
  //             *
  //             * Note: The tasks will make it into the same commit group iff the
  //             * first task that completes is willing to wait for the others to
  //             * join the commit group.
  //             *
  //             * Note: The tasks have a dependency on the same resource so they
  //             * will be serialized (executed in a strict sequence).
  //             */
  //            assertEquals("commitCounter", commitCounter0, journal
  //                    .getRootBlockView().getCommitCounter());
  //
  //            // however, a group commit SHOULD have been performed.
  //            assertEquals("groupCommitCount", groupCommitCount0 + 1, writeService
  //                    .getGroupCommitCount());
  //
  //            // NO aborts should have been performed.
  //            assertEquals("aboutCount", abortCount0, writeService.getAbortCount());
  //
  //            // ONE(1) tasks SHOULD have failed.
  //            assertEquals("failedTaskCount", failedTaskCount0 + 1, writeService.
  //                    getTaskFailedCount());
  //
  //            // TWO(2) tasks SHOULD have succeeded.
  //            assertEquals("successTaskCount", successTaskCount0 + 2, writeService
  //                    .getTaskSuccessCount());
  //
  //            // TWO(2) successfull tasks SHOULD have been committed.
  //            assertEquals("committedTaskCount", committedTaskCount0 + 2, writeService
  //                    .getTaskCommittedCount());
  //
  //            assertEquals( 3, futures.size());
  //
  //            // tasks[0]
  //            {
  //
  //                Future f = futures.get(0);
  //
  //                assertTrue(f.isDone());
  //
  //                f.get(); // No exception expected.
  //
  //            }
  //
  //            // tasks[2]
  //            {
  //
  //                Future f = futures.get(2);
  //
  //                assertTrue(f.isDone());
  //
  //                f.get(); // No exception expected.
  //
  //            }
  //
  //            // tasks[1]
  //            {
  //
  //                Future f = futures.get(1);
  //
  //                assertTrue(f.isDone());
  //
  //                try {
  //                    f.get();
  //                    fail("Expecting exception");
  //                } catch(ExecutionException ex) {
  //                    assertTrue(InnerCause.isInnerCause(ex, ForcedAbortException.class));
  //                }
  //
  //            }
  //
  //            assertEquals(checkpointAddr0, journal.getIndex(name)
  //                    .getCheckpoint().getCheckpointAddr());
  //
  //        } finally {
  //
  //            journal.destroy();
  //
  //        }
  //
  //    }

  /*
   * Test verifies that a write on an index will cause the index to be checkpointed when the task
   * completes.
   */
  public void test_writeServiceCheckpointDirtyIndex() throws Exception {

    final Journal journal = new Journal(getProperties());

    try {

      final String name = "test";

      // Note: checkpoint for the newly registered index.
      final long checkpointAddr0;
      {
        journal.registerIndex(new IndexMetadata(name, UUID.randomUUID()));

        journal.commit();

        checkpointAddr0 = journal.getIndex(name).getCheckpoint().getCheckpointAddr();
      }

      // Submit task that writes on the index and wait for the commit.
      journal
          .submit(
              new AbstractTask<Void>(journal, ITx.UNISOLATED, name) {

                @Override
                protected Void doTask() {

                  final BTree ndx = (BTree) getIndex(name);

                  // verify checkpoint unchanged.
                  assertEquals(checkpointAddr0, ndx.getCheckpoint().getCheckpointAddr());

                  // write on the index.
                  ndx.insert(new byte[] {1}, new byte[] {1});

                  return null;
                }
              })
          .get(); // wait for the commit.

      // verify checkpoint was updated.
      assertNotSame(checkpointAddr0, journal.getIndex(name).getCheckpoint().getCheckpointAddr());

    } finally {

      journal.destroy();
    }
  }

  /*
   * @todo revisit this unit test.  It's semantics appear to have aged.
   */
  //    /*
  //     * Test verifies that a task failure causes accessed indices to be rolled
  //     * back to their last checkpoint.
  //     *
  //     * FIXME write test where a task registers an index and then throws an
  //     * exception. This will cause the index to have a checkpoint record that
  //     * does not agree with {@link Name2Addr} for the last commit point. Verify
  //     * that the index is not in fact available to another task that is executed
  //     * after the failed task (it will be if we merely close the index and then
  //     * re-open it since it will reopen from the last checkpoint NOT from the
  //     * last commit point).
  //     *
  //     * FIXME write test where a tasks (a), (b) and (c) are submitted with
  //     * invokeAll() in that order and require a lock on the same index. Task (a)
  //     * writes on an existing index and completes normally. The index SHOULD be
  //     * checkpointed and task (b) SHOULD be able to read the data written in task
  //     * (a) and SHOULD be run in the same commit group. Task (b) then throws an
  //     * exception. Verify that the index is rolledback to the checkpoint for (a)
  //     * (vs the last commit point) using task (c) which will read on the same
  //     * index looking for the correct checkpoint record and data in the index.
  //     * This test will fail if (b) is not reading from the checkpoint written by
  //     * (a) or if (c) reads from the last commit point rather than the checkpoint
  //     * written by (a).
  //     *
  //     * FIXME write tests to verify that an {@link #abort()} causes all running
  //     * tasks to be interrupted and have their write sets discarded (should it?
  //     * Should an abort just be an shutdownNow() in response to some truely nasty
  //     * problem?)
  //     */
  //    public void test_writeService002()throws Exception {
  //
  //        final Properties properties = new Properties(getProperties());
  //
  //        /*
  //         * Note: restricting the thread pool size does not give us the control
  //         * that we need because it results in each task running as its own
  //         * commit group.
  //         */
  ////        /*
  ////         * Note: Force the write service to be single threaded so that we can
  ////         * control the order in which the tasks start by the order in which they
  ////         * are submitted.
  ////         */
  ////        properties.setProperty(Options.WRITE_SERVICE_CORE_POOL_SIZE,"1");
  ////        properties.setProperty(Options.WRITE_SERVICE_MAXIMUM_POOL_SIZE,"1");
  //
  //        final Journal journal = new Journal(properties);
  //
  //        try {
  //
  //            final String name = "test";
  //
  //            // Note: checkpoint for the newly registered index.
  //            final long checkpointAddr0;
  //            {
  //
  //                // register
  //                journal.registerIndex(name);
  //
  //                // commit.
  //                journal.commit();
  //
  //                // note checkpoint for index.
  //                checkpointAddr0 = journal.getIndex(name).getCheckpoint()
  //                        .getCheckpointAddr();
  //
  //            }
  //
  //            // Note: commit counter before we invoke the tasks.
  //            final long commitCounter = journal.getRootBlockView()
  //                    .getCommitCounter();
  //
  //            final WriteExecutorService writeService = journal
  //            .getConcurrencyManager().getWriteService();
  //
  //            // Note: group commit counter before we invoke the tasks.
  //            final long groupCommitCount0 = writeService.getGroupCommitCount();
  //
  //            // Note: #of failed tasks before we submit the tasks.
  //            final long failedTaskCount0 = writeService.getTaskFailedCount();
  //            final long successTaskCount0 = writeService.getTaskSuccessCount();
  //            final long committedTaskCount0 = writeService.getTaskCommittedCount();
  //
  //            // Note: set by one of the tasks below.
  //            final AtomicLong checkpointAddr2 = new AtomicLong(0L);
  //
  //            final AtomicReference<Future<? extends Object>> futureB = new
  // AtomicReference<Future<? extends Object>>();
  //            final AtomicReference<Future<? extends Object>> futureC = new
  // AtomicReference<Future<? extends Object>>();
  //            final AtomicReference<Future<? extends Object>> futureD = new
  // AtomicReference<Future<? extends Object>>();
  //
  //            /*
  //             * Note: the setup for this test is a PITA. In order to exert full
  //             * control over the order in which the tasks begin to execute we
  //             * need to have each task submit the next itself. This is because it
  //             * is possible for any of these tasks to be the first one to grab
  //             * the exclusive lock on the necessary resource [name]. We can't
  //             * solve this problem by restricting the #of threads that can run
  //             * the tasks since that limits the size of the commit group. So we
  //             * are stuck imposing serial execution using the behavior of the
  //             * tasks themselves.
  //             *
  //             * Create the task objects in the reverse order of their execution.
  //             */
  //
  //            // task (d) verifies expected rollback checkpoint was restored.
  //            final AbstractTask d = new AbstractTask(journal,ITx.UNISOLATED,name){
  //                protected String getTaskName() {return "d";}
  //                protected Object doTask() throws Exception {
  //                    // commit counter unchanged.
  //                    assertEquals("commitCounter", commitCounter, getJournal()
  //                            .getRootBlockView().getCommitCounter());
  //                    if(checkpointAddr2.get()==0L) {
  //                        fail("checkpointAddr2 was not set");
  //                    }
  //                    // lookup index.
  //                    BTree ndx = (BTree)getIndex(name);
  //                    final long newCheckpointAddr =ndx.getCheckpoint().getCheckpointAddr();
  //                    // verify checkpoint != last committed checkpoint.
  //                    assertNotSame(checkpointAddr0,newCheckpointAddr);
  //                    // verify checkpoint == last rollback checkpoint.
  //                    assertEquals(checkpointAddr2.get(),newCheckpointAddr);
  //                    return null;
  //                }
  //            };
  //
  //            /*
  //             * task (c) notes the last checkpoint, writes on the index, and then
  //             * fails. This is designed to trigger rollback of the index to the
  //             * last checkpoint, which is the checkpoint that we note at the
  //             * start of this task.
  //             */
  //            final AbstractTask c = new AbstractTask(journal,ITx.UNISOLATED,name){
  //                protected String getTaskName() {return "c";}
  //                protected Object doTask() throws Exception {
  //                    // commit counter unchanged.
  //                    assertEquals("commitCounter", commitCounter, getJournal()
  //                            .getRootBlockView().getCommitCounter());
  //                    // lookup index.
  //                    BTree ndx = (BTree)getIndex(name);
  //                    // note the last checkpoint written.
  //                    final long newCheckpointAddr = ndx.getCheckpoint().getCheckpointAddr();
  //                    assertNotSame(0L,newCheckpointAddr);
  //                    assertNotSame(checkpointAddr0,newCheckpointAddr);
  //                    // make note of the checkpoint before we force an abort.
  //                    assertTrue("checkpointAddr2 already set?",checkpointAddr2.compareAndSet(0L,
  // newCheckpointAddr));
  //                    // write another record on the index.
  //                    ndx.insert(new byte[]{3}, new byte[]{3});
  //                    // run task (d) next.
  //                    assertTrue(futureD.compareAndSet(null,journal.submit(d)));
  //                    // force task to about with dirty index.
  //                    throw new ForcedAbortException();
  //                }
  //            };
  //
  //            // task (b) writes another record on the index.
  //            final AbstractTask b = new AbstractTask(journal,ITx.UNISOLATED,name){
  //                protected String getTaskName() {return "b";}
  //                protected Object doTask() throws Exception {
  //                    // commit counter unchanged.
  //                    assertEquals("commitCounter", commitCounter, getJournal()
  //                            .getRootBlockView().getCommitCounter());
  //                    // lookup index.
  //                    BTree ndx = (BTree)getIndex(name);
  //                    // verify checkpoint was updated.
  //                    assertNotSame(checkpointAddr0,ndx.getCheckpoint().getCheckpointAddr());
  //                    // write another record on the index.
  //                    ndx.insert(new byte[]{2}, new byte[]{2});
  //                    // run task (c) next.
  //                    assertTrue(futureC.compareAndSet(null,journal.submit(c)));
  //                    return null;
  //                }
  //            };
  //
  //            // task (a) writes on index.
  //            final AbstractTask a = new AbstractTask(journal,ITx.UNISOLATED,name){
  //                protected String getTaskName() {return "a";}
  //                protected Object doTask() throws Exception {
  //                    // commit counter unchanged.
  //                    assertEquals("commitCounter", commitCounter, getJournal()
  //                            .getRootBlockView().getCommitCounter());
  //                    // group commit counter unchanged.
  //                    assertEquals("groupCommitCounter", groupCommitCount0,
  //                            writeService.getGroupCommitCount());
  //                    // lookup index.
  //                    BTree ndx = (BTree)getIndex(name);
  //                    // verify same checkpoint.
  //                    assertEquals(checkpointAddr0,ndx.getCheckpoint().getCheckpointAddr());
  //                    // write record on the index.
  //                    ndx.insert(new byte[]{1}, new byte[]{1});
  //                    // run task (b) next.
  //                    assertTrue(futureB.compareAndSet(null,journal.submit(b)));
  //                    return null;
  //                }
  //            };
  //
  ////            final List<AbstractTask> tasks = Arrays.asList(new AbstractTask[] {
  ////                    a,b,c,d
  ////            });
  ////
  ////            final List<Future<Object>> futures = journal.invokeAll( tasks );
  //
  //            final Future<? extends Object> futureA = journal.submit( a );
  //
  //            /*
  //             * wait for (a). if all tasks are in the same commit group then all
  //             * tasks will be done once we have the future for (a).
  //             */
  //            futureA.get(); // task (a)
  //
  //            /*
  //             * The expectation is that the tasks that succeed make it into the
  //             * same commit group while the task that throws an exception does
  //             * not cause the commit group to be aborted. Therefore there should
  //             * be ONE (1) commit more than when we submitted the tasks.
  //             *
  //             * Note: The tasks will make it into the same commit group iff the
  //             * first task that completes is willing to wait for the others to
  //             * join the commit group.
  //             *
  //             * Note: The tasks have a dependency on the same resource so they
  //             * will be serialized (executed in a strict sequence).
  //             */
  //            assertEquals("failedTaskCount", failedTaskCount0 + 1,
  //                    writeService.getTaskFailedCount());
  //            assertEquals("successTaskCount", successTaskCount0 + 3,
  //                    writeService.getTaskSuccessCount());
  //            assertEquals("committedTaskCount", committedTaskCount0 + 3,
  //                    writeService.getTaskCommittedCount());
  //            assertEquals("groupCommitCount", groupCommitCount0 + 1,
  //                    writeService.getGroupCommitCount());
  //            assertEquals("commitCounter", commitCounter + 1, journal
  //                    .getRootBlockView().getCommitCounter());
  //
  ////            assertEquals( 4, futures.size());
  //
  //            futureB.get().get(); // task (b)
  //            {
  //                // task (c) did the abort.
  //                Future f = futureC.get();
  //                try {f.get(); fail("Expecting exception");}
  //                catch(ExecutionException ex) {
  //                    if(!InnerCause.isInnerCause(ex, ForcedAbortException.class)) {
  //                        fail("Expecting "+ForcedAbortException.class+", not "+ex, ex);
  //                    }
  //                }
  //            }
  //            futureD.get().get(); // task (d)
  //
  //        } finally {
  //
  //            journal.destroy();
  //
  //        }
  //
  //    }

  /*
   * A class used to force aborts on tasks and then recognize the abort by the {@link
   * ForcedAbortException} from the uni
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  @SuppressWarnings("unused")
  private class ForcedAbortException extends RuntimeException {

    private static final long serialVersionUID = 1L;
  }

  //    /*
  //     * Correctness test of task retry when another task causes a commit group to
  //     * be discarded.
  //     *
  //     * @todo implement retry and maxLatencyFromSubmit and move the tests for
  //     *       those features into their own test suites.
  //     *
  //     * @todo Test retry of tasks (read only or read write) when they are part of
  //     *       a commit group in which some other task fails so they get
  //     *       interrupted and have to abort.
  //     *
  //     * @todo also test specifically with isolated tasks to make sure that the
  //     *       isolated indices are being rolled back to the last commit when the
  //     *       task is aborted.
  //     */
  //    public void test_retry_readService() {
  //
  //        fail("write this test");
  //
  //    }
  //
  //    public void test_retry_writeService() {
  //
  //        fail("write this test");
  //
  //    }
  //
  //    public void test_retry_txService_readOnly() {
  //
  //        fail("write this test");
  //
  //    }
  //
  //    /*
  //     * note: the difference between readOnly and readCommitted is that the
  //     * latter MUST read from whatever the committed state of the index is at the
  //     * time that it executes (or re-retries?) while the formed always reads from
  //     * the state of the index as of the transaction start time.
  //     */
  //    public void test_retry_txService_readCommitted() {
  //
  //        fail("write this test");
  //
  //    }
  //
  //    public void test_retry_txService_readWrite() {
  //
  //        fail("write this test");
  //
  //    }

  //     Note: I can not think of any way to write this test.
  //
  //    /*
  //     * This test verifies that unisolated reads are against the last committed
  //     * state of the index(s), that they do NOT permit writes, and that
  //     * concurrent writers on the same named index(s) do NOT conflict.
  //     */
  //    public void test_submit_readService_isolation01() throws InterruptedException,
  // ExecutionException {
  //
  //        Properties properties = getProperties();
  //
  //        Journal journal = new Journal(properties);
  //
  //        final long commitCounterBefore = journal.getRootBlockView().getCommitCounter();
  //
  //        final String resource = "foo";
  //
  //        final AtomicBoolean ran = new AtomicBoolean(false);
  //
  //        final UUID indexUUID = UUID.randomUUID();
  //
  //        // create the index (and commit).
  //        assertEquals("indexUUID", indexUUID, journal.submit(
  //                new RegisterIndexTask(journal, resource, new UnisolatedBTree(
  //                        journal, indexUUID))).get());
  //
  //        // verify commit.
  //        assertEquals("commit counter unchanged?",
  //                commitCounterBefore+1, journal.getRootBlockView()
  //                        .getCommitCounter());
  //
  //        // write some data on the index (and commit).
  //        final long metadataAddr = (Long) journal.submit(
  //                new AbstractIndexTask(journal, ITx.UNISOLATED,
  //                        false/*readOnly*/, resource) {
  //
  //            protected Object doTask() throws Exception {
  //
  //                IIndex ndx = getIndex(getOnlyResource());
  //
  //                // Note: the metadata address before any writes on the index.
  //                final long metadataAddr = ((BTree)ndx).getMetadata().getMetadataAddr();
  //
  //                // write on the index.
  //                ndx.insert(new byte[]{1,2,3},new byte[]{2,2,3});
  //
  //                return metadataAddr;
  //
  //            }
  //
  //        }).get();
  //
  //        // verify another commit.
  //        assertEquals("commit counter unchanged?",
  //                commitCounterBefore+2, journal.getRootBlockView()
  //                        .getCommitCounter());
  //
  //        Future<Object> future = journal.submit(new AbstractIndexTask(journal,
  //                ITx.UNISOLATED, true/*readOnly*/, resource) {
  //
  //            /*
  //             * The task just sets a boolean value and returns the name of the
  //             * sole resource. It does not actually read anything.
  //             */
  //            protected Object doTask() throws Exception {
  //
  //                ran.compareAndSet(false, true);
  //
  //                return getOnlyResource();
  //
  //            }
  //        });
  //
  //        // the test task returns the resource as its value.
  //        assertEquals("result",resource,future.get());
  //
  //        /*
  //         * make sure that the flag was set (not reliably set until we get() the
  //         * future).
  //         */
  //        assertTrue("ran",ran.get());
  //
  //        /*
  //         * Verify that a commit was NOT performed.
  //         */
  //        assertEquals("commit counter changed?",
  //                commitCounterBefore, journal.getRootBlockView()
  //                        .getCommitCounter());
  //
  //        journal.shutdown();
  //
  //        journal.delete();
  //
  //    }

  /*
   * A stress test that runs concurrent {@link ITx#READ_COMMITTED} readers and {@link
   * ITx#UNISOLATED} writers and verifies that readers are able to transparently continue to read
   * against the named indices if the backing {@link FileChannel} is closed by an interrupt noticed
   * during an IO on that {@link FileChannel}. In order for this test to succeed the backing {@link
   * FileChannel} must be transparently re-opened in a thread-safe manner if it is closed
   * asynchronously (i.e., closed while the buffer strategy is still open).
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public void test_concurrentReadersAreOk() throws Throwable {

    // Note: clone so that we do not modify!!!
    final Properties properties = new Properties(getProperties());

    // Mostly serialize the readers.
    properties.setProperty(Options.READ_SERVICE_CORE_POOL_SIZE, "2");

    // Completely serialize the writers.
    properties.setProperty(Options.WRITE_SERVICE_CORE_POOL_SIZE, "1");
    properties.setProperty(Options.WRITE_SERVICE_MAXIMUM_POOL_SIZE, "1");

    final Journal journal = new Journal(properties);
    //        final IBufferStrategy bufferStrategy = journal.getBufferStrategy();
    //        if (bufferStrategy instanceof RWStrategy) {
    //            ((RWStrategy)bufferStrategy).getRWStore().activateTx();
    //        }

    try {

      // Note: This test requires a journal backed by stable storage.

      if (journal.isStable()) {

        // register and populate the indices and commit.
        final int NRESOURCES = 10;
        final int NWRITES = 10000;
        final String[] resource = new String[NRESOURCES];
        {
          final KeyBuilder keyBuilder = new KeyBuilder(4);
          for (int i = 0; i < resource.length; i++) {
            resource[i] = "index#" + i;
            final IIndex ndx =
                (IIndex)
                    journal.register(
                        resource[i], new IndexMetadata(resource[i], UUID.randomUUID()));
            for (int j = 0; j < NWRITES; j++) {
              final byte[] val = (resource[i] + "#" + j).getBytes();
              ndx.insert(keyBuilder.reset().append(j).getKey(), val);
            }
          }
          journal.commit();
        }
        log.warn(
            "Registered and populated "
                + resource.length
                + " named indices with "
                + NWRITES
                + " records each");

        /*
         * Does an {@link ITx#READ_COMMITTED} index scan on a named index using the last committed
         * state of the named index.
         *
         * <p>Note: The expectation is that the read tasks WILL NOT throw exceptions related to the
         * asynchronous close of the {@link FileChannel} in response to a writer interrupted during
         * a FileChannel IO since we are expecting transparent re-opening of the store. In practice
         * this means that the buffer strategy implementation must notice when the store was closed
         * asynchronously, obtain a lock, re-open the store, and then re-try the operation.
         *
         * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
         */
        class ReadTask extends AbstractTask<Void> {

          protected ReadTask(IConcurrencyManager concurrencyManager, String resource) {

            super(concurrencyManager, ITx.READ_COMMITTED, resource);
          }

          @Override
          protected Void doTask() {

            final IIndex ndx = getIndex(getOnlyResource());

            // verify writes not allowed.
            try {
              ndx.insert(new byte[] {}, new byte[] {});
              fail("Expecting: " + UnsupportedOperationException.class);
            } catch (UnsupportedOperationException ex) {
              log.info("Ingoring expected exception: " + ex);
            }
            //                    assertTrue(ndx instanceof ReadOnlyIndex);

            final ITupleIterator<?> itr = ndx.rangeIterator(null, null);

            int n = 0;

            while (itr.hasNext()) {

              itr.next();

              n++;
            }

            assertEquals("#entries", n, NWRITES);

            return null;
          }
        }

        /*
         * Runs a sequence of write operations that interrupt themselves.
         */
        final ExecutorService writerService =
            Executors.newSingleThreadExecutor(new DaemonThreadFactory());

        /*
         * Submit tasks to the single threaded service that will in turn
         * feed them on by one to the journal's writeService. When run on
         * the journal's writeService the tasks will interrupt themselves
         * and, depending on whether or not the interrupt occurs during a
         * FileChannel IO, provoke the JDK to close the FileChannel backing
         * the journal.
         */
        for (int i = 0; i < 10; i++) {
          final String theResource = resource[i % resource.length];
          writerService.submit(
              () -> {
                journal.submit(new InterruptMyselfTask(journal, ITx.UNISOLATED, theResource));
                // pause between submits
                Thread.sleep(20);
                return null;
              });
        }

        /*
         * Submit concurrent reader tasks and wait for them to run for a
         * while.
         */
        {
          final Collection<AbstractTask<Void>> tasks = new LinkedList<>();
          for (int i = 0; i < NRESOURCES * 10; i++) {

            tasks.add(new ReadTask(journal, resource[i % resource.length]));
          }

          // await futures.
          final List<Future<Void>> futures = journal.invokeAll(tasks, 10, TimeUnit.SECONDS);

          for (Future<Void> f : futures) {

            if (f.isDone() && !f.isCancelled()) {
              // all tasks that complete should have done so without error.
              f.get();
            }
          }
        }

        writerService.shutdownNow();

        log.warn("End of test");
      }

    } finally {

      //            if (bufferStrategy instanceof RWStrategy) {
      //                ((RWStrategy)bufferStrategy).getRWStore().deactivateTx();
      //            }

      journal.destroy();
    }
  }
}
