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
 * Created on Feb 18, 2007
 */

package org.embergraph.journal;

import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.testutil.ExperimentDriver;
import org.embergraph.testutil.ExperimentDriver.IComparisonTest;
import org.embergraph.testutil.ExperimentDriver.Result;
import org.embergraph.util.Bytes;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.NV;

/*
 * Stress tests for concurrent transaction processing.
 *
 * <p>Note: For short transactions, TPS is basically constant for a given combination of the buffer
 * mode and whether or not commits are forced to disk. This means that the #of clients is not a
 * strong influence on performance. The big wins are Transient and Force := No since neither
 * conditions syncs to disk. This suggests that the big win for TPS throughput is going to be group
 * commit followed by either the use of SDD for the journal or pipelining writes to secondary
 * journals on failover hosts.
 *
 * <p>FIXME Finish support for transactions in {@link AbstractTask}.
 *
 * @todo run tests of transaction throughput using a number of models. E.g., a few large indices
 *     with a lot of small transactions vs a lot of small indices with small transactions vs a few
 *     large indices with moderate to large transactions. Also look out for transactions where the
 *     validation and merge on the unisolated index takes more than one group commit cycle and see
 *     how that effects the application.
 * @todo Verify proper partial ordering over transaction schedules (runs tasks in parallel, uses
 *     exclusive locks for access to the same isolated index within the same transaction, and
 *     schedules their commits using a partial order based on the indices that were actually written
 *     on).
 *     <p>Verify that only the indices that were written on are used to establish the partial order,
 *     not any index from which the tx might have read.
 * @todo test writing on multiple isolated indices in the different transactions and verify that no
 *     concurrency limits are imposed across transactions (only within transactions).
 * @todo test where concurrent operations are executed against the same transaction. this provokes
 *     contention for access to the mutable isolated indices which must be resolved through a lock
 *     manager.
 * @todo show state-based validation for concurrent transactions on the same index that result in
 *     write-write conflicts.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class StressTestConcurrentTx extends ProxyTestCase<Journal> implements IComparisonTest {

  public StressTestConcurrentTx() {}

  public StressTestConcurrentTx(final String name) {

    super(name);
  }

  private Journal journal;

  @Override
  public void setUpComparisonTest(final Properties properties) throws Exception {

    journal = new Journal(properties);
  }

  @Override
  public void tearDownComparisonTest() throws Exception {

    if (journal != null) {

      journal.destroy();
    }
  }

  /** A stress test with a small pool of concurrent clients. */
  public void test_concurrentClients() throws InterruptedException {

    //    	new Float("123.23");

    final Properties properties = getProperties();

    final Journal journal = new Journal(properties);

    try {

      if (false && journal.getBufferStrategy() instanceof MappedBufferStrategy) {

        /*
         * @todo the mapped buffer strategy has become cpu bound w/o
         * termination when used with concurrent clients - this needs to
         * be looked into further.
         */

        fail("Mapped buffer strategy may have problem with tx concurrency");
      }

      doConcurrentClientTest(
          journal,
          30, // timeout
          20, // nclients
          500, // ntrials
          3, // keylen
          100, // nops
          .10 // abortRate
          );

    } finally {

      journal.destroy();
    }
  }

  /*
   * A stress test with a pool of concurrent clients.
   *
   * <p>Note: <i>nclients</i> corresponds to a number of external processes that start transactions,
   * submit {@link AbstractTask}s that operate against those transactions, and finally choose to
   * either commit or abort the transactions. The concurrency with which the {@link AbstractTask}s
   * submitted by the "clients" may run is governed by the {@link ConcurrencyManager.Options}.
   * However, the concurrency is capped by the #of clients since each client manages a single
   * transaction at a time.
   *
   * @param journal The database.
   * @param resource The name of the index on which the transactions will operation.
   * @param timeout The #of seconds before the test will terminate.
   * @param nclients The #of concurrent clients.
   * @param ntrials The #of transactions to execute.
   * @param keyLen The length of the random unsigned byte[] keys used in the operations. The longer
   *     the keys the less likely it is that there will be a write-write conflict (that concurrent
   *     txs will write on the same key).
   * @param nops The #of operations to be performed in each transaction.
   * @param abortRate The #of clients that choose to abort a transaction rather the committing it
   *     [0.0:1.0]. Note that the abort point is always choosen after the client has submitting at
   *     least one write task for the transaction.
   * @todo introduce a parameter to govern the #of named resources from which transactions choose
   *     the indices on which they will write.
   * @todo introduce a parameter to govern the #of named indices on which each transaction will
   *     write (min/max). together these allow us to control the amount of contention among the
   *     transactions for access to the same unisolated index during commit (validate and
   *     mergeDown).
   * @todo Introduce a parameter to govern the #of concurrent operations that a client will submit
   *     against the named resource(s) and the #of named resources on which each isolated task may
   *     write.
   * @todo factor out the operation to be run as a test parameter?
   */
  public static Result doConcurrentClientTest(
      final Journal journal,
      final long timeout,
      final int nclients,
      final int ntrials,
      final int keyLen,
      final int nops,
      final double abortRate)
      throws InterruptedException {

    final String name = "abc";

    { // Setup the named index and commit the journal.
      final IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

      md.setIsolatable(true);

      journal.registerIndex(name, BTree.create(journal, md));

      journal.commit();
    }

    final ExecutorService executorService =
        Executors.newFixedThreadPool(nclients, DaemonThreadFactory.defaultThreadFactory());

    final Collection<Callable<Long>> tasks = new HashSet<>();

    for (int i = 0; i < ntrials; i++) {

      tasks.add(new Task(journal, name, i, keyLen, nops, abortRate));
    }

    /*
     * Run the M transactions on N clients.
     */

    final long begin = System.currentTimeMillis();

    final List<Future<Long>> results = executorService.invokeAll(tasks, timeout, TimeUnit.SECONDS);

    final long elapsed = System.currentTimeMillis() - begin;

    executorService.shutdownNow();

    final Iterator<Future<Long>> itr = results.iterator();

    int ninterrupt = 0; // #of tasks that throw InterruptedException.
    //        int nretry = 0; // #of transactions that were part of a commit group that failed but
    // MAY be retried.
    int nfailed = 0; // #of transactions that failed validation (MUST BE zero if nclients==1).
    int naborted = 0; // #of transactions that choose to abort rather than commit.
    int ncommitted = 0; // #of transactions that successfully committed.
    int nuncommitted = 0; // #of transactions that did not complete in time.

    try {
      while (itr.hasNext()) {

        final Future<Long> future = itr.next();

        if (future.isCancelled()) {

          nuncommitted++;

          continue;
        }

        try {

          if (future.get() == 0L) {

            // Note: Could also be an empty write set.

            naborted++;

          } else {

            ncommitted++;
          }

        } catch (ExecutionException ex) {

          // Validation errors are allowed and counted as aborted txs.

          if (isInnerCause(ex, ValidationError.class)) {

            nfailed++;

            if (log.isInfoEnabled()) log.info(getInnerCause(ex, ValidationError.class));

            //                } else if(isInnerCause(ex,RetryException.class)){
            //
            //                    nretry++;
            //
            //                    log.info(getInnerCause(ex, RetryException.class));

          } else if (isInnerCause(ex, InterruptedException.class)
              || isInnerCause(ex, ClosedByInterruptException.class)) {

            ninterrupt++;

            if (log.isInfoEnabled()) log.info(getInnerCause(ex, InterruptedException.class));

          } else {

            /*
             * Other kinds of exceptions are errors.
             */

            fail("Not expecting: " + ex, ex);
            //                    log.warn("Not expecting: "+ex, ex);

          }
        }
      }

      /*
       * Note: Code was commented out (also see report below) because
       * journal.getRootBlocks() was not implemented correctly.
       */
      //        // Now test rootBlocks
      //        int rootBlockCount = 0;
      //        final Iterator<IRootBlockView> rbvs = journal.getRootBlocks(1);
      //        while (rbvs.hasNext()) {
      //            final IRootBlockView rbv = rbvs.next();
      //            rootBlockCount++;
      //        }
    } finally {
      // immediately terminate any tasks that are still running.
      log.warn("Shutting down now!");
      journal.shutdownNow();
    }
    final Result ret = new Result();

    /*
     * #of bytes written on the backing store (does not count writes on the
     * temporary stores that back the isolated indices).
     */
    final long bytesWritten = journal.getRootBlockView().getNextOffset();

    // these are the results.
    ret.put("ninterupt", "" + ninterrupt);
    //        ret.put("nretry",""+nretry);
    ret.put("nfailed", "" + nfailed);
    ret.put("naborted", "" + naborted);
    ret.put("ncommitted", "" + ncommitted);
    ret.put("nuncommitted", "" + nuncommitted);
    //        ret.put("rootBlocks found", ""+rootBlockCount);
    ret.put("elapsed(ms)", "" + elapsed);
    ret.put("tps", "" + (ncommitted * 1000 / elapsed));
    ret.put("bytesWritten", "" + bytesWritten);
    ret.put("bytesWritten/sec", "" + (int) (bytesWritten * 1000d / elapsed));

    System.err.println(ret.toString(true /*newline*/));

    // Note: This is done by the caller using journal#destroy().
    //        journal.deleteResources();

    return ret;
  }

  /*
   * Run a transaction.
   *
   * <p>Note: defers creation of the tx until it begins to execute! This provides a substantial
   * resource savings and lets transactions begin execution immediately.
   */
  public static class Task implements Callable<Long> {

    private final Journal journal;
    private final String name;
    private final int trial;
    private final int keyLen;
    private final int nops;
    private final double abortRate;

    private final Random r = new Random();

    public Task(
        final Journal journal,
        final String name,
        int trial,
        int keyLen,
        int nops,
        double abortRate) {

      this.journal = journal;

      this.name = name;

      this.trial = trial;

      this.keyLen = keyLen;

      this.nops = nops;

      this.abortRate = abortRate;
    }

    @Override
    public String toString() {

      return super.toString() + "#" + trial;
    }

    /*
     * Executes random operations in the transaction.
     *
     * @return The commit time of the transactions and <code>0L</code> IFF the transaction was
     *     aborted.
     */
    @Override
    public Long call() throws Exception {

      final long tx = journal.newTx(ITx.UNISOLATED);

      /*
       * Now that the transaction is running, submit tasks that are
       * isolated by that transaction to the journal and wait for them to
       * complete.
       */

      journal
          .submit(
              new AbstractTask<Object>(journal, tx, name) {

                @Override
                protected Object doTask() {
                  // Random operations on the named index(s).

                  final IIndex ndx = getIndex(name);

                  for (int i = 0; i < nops; i++) {

                    byte[] key = new byte[keyLen];

                    r.nextBytes(key);

                    if (r.nextInt(100) > 10) {

                      byte[] val = new byte[5];

                      r.nextBytes(val);

                      ndx.insert(key, val);

                    } else {

                      ndx.remove(key);
                    }
                  }

                  return null;
                }
              })
          .get();

      /*
       * The percentage of transactions that will choose to abort rather
       * than commit.
       */

      if (r.nextInt(100) >= abortRate) {

        // commit.

        //                assertFalse("Empty write set?", journal.getTx(tx).isEmptyWriteSet());

        final long commitTime = journal.commit(tx);

        if (commitTime == 0L) {

          /*
           *
           */
          throw new AssertionError("Expecting non-zero commit time");
        }

        return commitTime;

      } else {

        // abort.

        journal.abort(tx);

        return 0L;
      }
    }
  }

  /*
   * Runs a single instance of the test as configured in the code.
   *
   * @todo Try to make this a correctness test since there are lots of little ways in which things
   *     can go wrong. Note that the actual execution execution order is important for transactions.
   * @see ExperimentDriver, which parameterizes the use of this stress test. That information should
   *     be used to limit the #of transactions allowed to start at one time on the server and should
   *     guide a search for thinning down resource consumption, e.g., memory usage by btrees, the
   *     node serializer, etc.
   * @see GenerateExperiment, which may be used to generate a set of conditions to be run by the
   *     {@link ExperimentDriver}.
   */
  public static void main(final String[] args) throws Exception {

    final Properties properties = new Properties();

    //        properties.setProperty(Options.FORCE_ON_COMMIT,ForceEnum.No.toString());

    //        properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient.toString());

    //        properties.setProperty(Options.BUFFER_MODE, BufferMode.Direct.toString());

    //        properties.setProperty(Options.BUFFER_MODE, BufferMode.Mapped.toString());

    properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

    properties.setProperty(Options.CREATE_TEMP_FILE, "true");

    properties.setProperty(TestOptions.TIMEOUT, "60");

    properties.setProperty(TestOptions.NCLIENTS, "20");

    properties.setProperty(TestOptions.NTRIALS, "10000");

    properties.setProperty(TestOptions.KEYLEN, "4");

    properties.setProperty(TestOptions.NOPS, "4");

    properties.setProperty(TestOptions.ABORT_RATE, ".05");

    final IComparisonTest test = new StressTestConcurrentTx();

    test.setUpComparisonTest(properties);

    try {

      test.doComparisonTest(properties);

    } finally {

      try {

        test.tearDownComparisonTest();

      } catch (Throwable t) {

        log.warn("Tear down problem: " + t, t);
      }
    }
  }

  /** Additional properties understood by this test. */
  public interface TestOptions extends ConcurrencyManager.Options {

    /** The timeout for the test. */
    String TIMEOUT = "timeout";

    /** The #of concurrent clients to run. */
    String NCLIENTS = "nclients";

    /** The #of trials (aka transactions) to run. */
    String NTRIALS = "ntrials";

    /*
     * The length of the keys used in the test. This directly impacts the likelyhood of a
     * write-write conflict. Shorter keys mean more conflicts. However, note that conflicts are only
     * possible when there are at least two concurrent clients running.
     */
    String KEYLEN = "keyLen";

    /** The #of operations in each trial. */
    String NOPS = "nops";

    /** The #of clients that choose to abort a transaction rather the committing it [0.0:1.0]. */
    String ABORT_RATE = "abortRate";
  }

  /*
   * Setup and run a test.
   *
   * @param properties There are no "optional" properties - you must make sure that each property
   *     has a defined value.
   */
  @Override
  public Result doComparisonTest(final Properties properties) throws Exception {

    final long timeout = Long.parseLong(properties.getProperty(TestOptions.TIMEOUT));

    final int nclients = Integer.parseInt(properties.getProperty(TestOptions.NCLIENTS));

    final int ntrials = Integer.parseInt(properties.getProperty(TestOptions.NTRIALS));

    final int keyLen = Integer.parseInt(properties.getProperty(TestOptions.KEYLEN));

    final int nops = Integer.parseInt(properties.getProperty(TestOptions.NOPS));

    final double abortRate = Double.parseDouble(properties.getProperty(TestOptions.ABORT_RATE));

    final Result result =
        doConcurrentClientTest(journal, timeout, nclients, ntrials, keyLen, nops, abortRate);

    return result;
  }

  /*
   * Experiment generation utility class.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class GenerateExperiment extends ExperimentDriver {

    /*
     * Generates an XML file that can be run by {@link ExperimentDriver}.
     *
     * <p>FIXME I have seen an out of memory error showing up on the 28th condition (they were
     * randomized so who knows which condition it was) when running the generated experiment. Who is
     * holding onto what ?
     *
     * @param args
     */
    public static void main(final String[] args) throws Exception {

      // this is the test to be run.
      final String className = StressTestConcurrentTx.class.getName();

      /*
       * Set defaults for each condition.
       */

      final Map<String, String> defaultProperties = new HashMap<>();

      // force delete of the files on close of the journal under test.
      defaultProperties.put(Options.CREATE_TEMP_FILE, "true");

      // avoids journal overflow when running out to 60 seconds.
      defaultProperties.put(Options.MAXIMUM_EXTENT, "" + Bytes.megabyte32 * 400);

      defaultProperties.put(Options.BUFFER_MODE, BufferMode.Disk.toString());

      defaultProperties.put(TestOptions.TIMEOUT, "30");

      defaultProperties.put(TestOptions.NTRIALS, "10000");

      defaultProperties.put(TestOptions.KEYLEN, "4");

      defaultProperties.put(TestOptions.ABORT_RATE, ".05");

      /*
       * Build up the conditions.
       */

      List<Condition> conditions = new ArrayList<>();

      conditions.add(new Condition(defaultProperties));

      conditions =
          apply(
              conditions,
              new NV[] {
                new NV(TestOptions.NCLIENTS, "1"),
                new NV(TestOptions.NCLIENTS, "10"),
                new NV(TestOptions.NCLIENTS, "20"),
                new NV(TestOptions.NCLIENTS, "50"),
                new NV(TestOptions.NCLIENTS, "100"),
                new NV(TestOptions.NCLIENTS, "200"),
              });

      conditions =
          apply(
              conditions,
              new NV[] {
                new NV(TestOptions.NOPS, "1"),
                new NV(TestOptions.NOPS, "10"),
                new NV(TestOptions.NOPS, "100"),
                new NV(TestOptions.NOPS, "1000"),
              });

      conditions =
          apply(
              conditions,
              new NV[] {
                new NV(TestOptions.KEYLEN, "4"), new NV(TestOptions.KEYLEN, "8"),
                //                    new NV(TestOptions.KEYLEN,"32"),
                //                    new NV(TestOptions.KEYLEN,"64"),
                //                    new NV(TestOptions.KEYLEN,"128"),
              });

      //            conditions = apply(
      //                    conditions,
      //                    new NV[][] {
      //                            new NV[] { new NV(Options.BUFFER_MODE,
      //                                    BufferMode.Transient), },
      //                            new NV[] { new NV(Options.BUFFER_MODE,
      //                                    BufferMode.Direct), },
      //                            new NV[] {
      //                                    new NV(Options.BUFFER_MODE, BufferMode.Direct),
      //                                    new NV(Options.FORCE_ON_COMMIT, ForceEnum.No
      //                                            .toString()), },
      //                            new NV[] { new NV(Options.BUFFER_MODE, BufferMode.Mapped), },
      //                            new NV[] { new NV(Options.BUFFER_MODE, BufferMode.Disk), },
      //                            new NV[] {
      //                                    new NV(Options.BUFFER_MODE, BufferMode.Disk),
      //                                    new NV(Options.FORCE_ON_COMMIT, ForceEnum.No
      //                                            .toString()), },
      //                    });

      Experiment exp = new Experiment(className, defaultProperties, conditions);

      // copy the output into a file and then you can run it later.
      System.err.println(exp.toXML());
    }
  }
}
