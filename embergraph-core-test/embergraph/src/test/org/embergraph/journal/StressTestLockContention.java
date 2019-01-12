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
 * Created on Oct 15, 2007
 */

package org.embergraph.journal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Lock contention results when unisolated writers seek conflicting locks. In all cases lock
 * contention reduces the possible parallelism. However, in the extreme case, lock contention forces
 * the serialization of unisolated writers.
 *
 * <p>This test suite may be used to examine the responsiveness of the {@link WriteExecutorService}
 * under lock contention. Performance will be less than that for the equivilent tasks without lock
 * contention, but group commit should still block many serialized writer tasks together for good
 * throughput.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class StressTestLockContention extends ProxyTestCase<Journal> {

  /** */
  public StressTestLockContention() {
    super();
  }

  /** @param name */
  public StressTestLockContention(final String name) {
    super(name);
  }

  /**
   * Test that no tasks are failed when a large set of <strong>writer</strong> tasks that attempt to
   * lock the same resource(s) are submitted at once (write tasks use the lock system to control
   * access to the unisolated indices).
   *
   * <p>Note: Tasks will be serialized since they are contending for the same resources.
   *
   * @throws InterruptedException
   * @todo I will sometimes see errors reported by this test when running as part of the total test
   *     suite but never when run by itself.
   * @todo if the [backoff] property for the {@link ConcurrencyManager} is disabled (and it does not
   *     work with cached thread pools) then you may see a {@link RejectedExecutionException} from
   *     this test.
   */
  public void test_lockContention() throws InterruptedException {

    final int ntasks = 500;

    final List<Future<Object>> futures;

    {
      final Properties properties = getProperties();

      final Journal journal = new Journal(properties);

      try {

        final String[] resource = new String[] {"foo", "bar", "baz"};

        final Collection<AbstractTask<Object>> tasks = new HashSet<AbstractTask<Object>>(ntasks);

        for (int i = 0; i < ntasks; i++) {

          tasks.add(
              new AbstractTask<Object>(journal, ITx.UNISOLATED, resource) {

                @Override
                protected Object doTask() throws Exception {

                  return null;
                }
              });
        }

        /*
         * Submit all tasks. Tasks can begin executing right away. If the
         * write service is using a blocking queue with a limited capacity
         * then some or all of the tasks may complete before this method
         * returns.
         */

        futures = (List<Future<Object>>) journal.invokeAll(tasks, 20, TimeUnit.SECONDS);

      } finally {

        /*
         * Shutdown the journal.
         *
         * Note: It is possible for shutdownNow() to close the store before
         * all worker threads have been canceled, in which case you may see
         * some strange errors being thrown.
         *
         * Note: The simplest way to fix this is to adjust the #of tasks
         * that are submitted concurrently and the timeout in
         * invokeAll(tasks,....) above.
         *
         * Note: 500 tasks that will be serialized due to their lock
         * requirements executes in ~8 seconds on my laptop.
         *
         * Note: This test is not CPU intensive. The delay comes from the
         * commits. Since the resource locks cause the tasks to be
         * serialized, there is one commit per task.
         */

        journal.destroy();
      }
    }

    final Iterator<Future<Object>> itr = futures.iterator();

    int ncancelled = 0;
    int ncomplete = 0;
    int ninterrupt = 0;
    int nretry = 0;
    int nerror = 0;

    while (itr.hasNext()) {

      final Future<? extends Object> future = itr.next();

      if (future.isCancelled()) {

        ncancelled++;

      } else if (future.isDone()) {

        try {

          future.get();

          ncomplete++;

        } catch (ExecutionException ex) {

          //                    if( isInnerCause(ex, RetryException.class)) {
          //
          //                        log.warn("RetryException: "+ex);
          //
          //                        nretry++;
          //
          //                        continue;
          //
          //                    } else
          if (isInnerCause(ex, InterruptedException.class)) {

            log.warn("Interrupted: " + ex);

            ninterrupt++;

            continue;
          }

          nerror++;

          log.warn("Not expecting: " + ex, ex);
        }
      }
    }

    final String msg =
        "#tasks="
            + ntasks
            + " : ncancelled="
            + ncancelled
            + ", ncomplete="
            + ncomplete
            + ", ninterrupt="
            + ninterrupt
            + ", nretry="
            + nretry
            + ", nerror="
            + nerror;

    if (log.isInfoEnabled()) log.info(msg);

    /*
     * No errors are allowed, but some tasks may never start due to the high
     * lock contention.
     */

    assertEquals(msg, 0, nerror);
  }
}
