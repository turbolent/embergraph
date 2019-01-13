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

package org.embergraph.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.embergraph.cache.ConcurrentWeakValueCache;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.Instrument;

/*
 * This class coordinates a schedule among concurrent operations requiring exclusive access to
 * shared resources. Whenever possible, the result is a concurrent schedule - that is, operations
 * having non-overlapping lock requirements run concurrently while operations that have lock
 * contentions are queued behind operations that currently have locks on the relevant resources. A
 * {@link ResourceQueue} is created for each resource and used to block operations that are awaiting
 * a lock. When locks are not being pre-declared, a {@link TxDag WAITS_FOR} graph is additionally
 * used to detect deadlocks.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param R The type of the object that identifies a resource for the purposes of the locking
 *     system. This is typically the name of an index.
 * @todo Support escalation of operation priority based on time and scheduling of higher priority
 *     operations. the latter is done by queueing lock requests in front of pending requests for
 *     each resource on which an operation attempt to gain a lock. The former is just a dynamic
 *     adjustment of the position of the operation in the resource queue where it is awaiting a lock
 *     (an operation never awaits more than one lock at a time). This facility could be used to give
 *     priority to distributed transactions over local unisolated operations and to priviledge
 *     certain operations that have low latency requirements. This is not quite a "real-time"
 *     guarentee since the VM is not (normally) providing real-time guarentees and since we are not
 *     otherwise attempting to ensure anything except lower latency when compared to other
 *     operations awaiting their own locks.
 * @deprecated This implementation manages locks in terms of threads. A thread is required in order
 *     for a task to contend for its locks. This places a strain on the thread scheduler.
 */
public class LockManager</*T,*/ R extends Comparable<R>> {

  protected static final Logger log = Logger.getLogger(LockManager.class);

  /** True iff the {@link #log} level is INFO or less. */
  protected final boolean INFO = log.isInfoEnabled();

  /** True iff the {@link #log} level is DEBUG or less. */
  protected final boolean DEBUG = log.isDebugEnabled();

  /*
   * Each resource that can be locked has an associated {@link ResourceQueue}.
   *
   * <p>Note: This is a concurrent collection since new resources may be added while concurrent
   * operations resolve resources to their queues. Stale {@link ResourceQueue}s are purged after
   * they become only weakly reachable.
   *
   * @todo could also use timeout to purge stale resource queues, but it should not matter since the
   *     {@link ResourceQueue} does not have a reference to the resource itself - just to its name.
   */
  private final ConcurrentWeakValueCache<R, ResourceQueue<R, Thread>> resourceQueues =
      new ConcurrentWeakValueCache<>(1000 /* nresources */);

  /** The set of locks held by each transaction. */
  private final ConcurrentHashMap<Thread, Collection<ResourceQueue<R, Thread>>> lockedResources;

  /*
   * True iff locks MUST be predeclared by the operation - this is a special case of 2PL (two-phrase
   * locking) that allows significant optimizations and avoids the possibility of deadlock
   * altogether.
   */
  private final boolean predeclareLocks;

  /*
   * When true, the resources in a lock request are sorted before the lock requests are made to the
   * various resource queues. This option is ONLY turned off for testing purposes as it ALWAYS
   * reduces the chance of deadlocks and eliminates it entirely when locks are also predeclared.
   */
  private final boolean sortLockRequests;

  /** Used to track dependencies among transactions. */
  private final TxDag waitsFor;

  /*
   * counters
   */

  public synchronized CounterSet getCounters() {

    if (root == null) {

      root = new CounterSet();

      root.addCounter(
          "nstarted",
          new Instrument<Long>() {
            public void sample() {
              setValue(nstarted.get());
            }
          });

      root.addCounter(
          "nended",
          new Instrument<Long>() {
            public void sample() {
              setValue(nended.get());
            }
          });

      root.addCounter(
          "nerror",
          new Instrument<Long>() {
            public void sample() {
              setValue(nerror.get());
            }
          });

      root.addCounter(
          "ndeadlock",
          new Instrument<Long>() {
            public void sample() {
              setValue(ndeadlock.get());
            }
          });

      root.addCounter(
          "ntimeout",
          new Instrument<Long>() {
            public void sample() {
              setValue(ntimeout.get());
            }
          });

      // Note: #that are seeking to acquire or waiting on their locks.
      root.addCounter(
          "nwaiting",
          new Instrument<Long>() {
            public void sample() {
              setValue(nwaiting.get());
            }
          });

      // Note: #that have acquired locks are executing concurrently.
      root.addCounter(
          "nrunning",
          new Instrument<Long>() {
            public void sample() {
              setValue(nrunning.get());
            }
          });

      // the maximum observed value for [nrunning].
      root.addCounter(
          "maxRunning",
          new Instrument<Long>() {
            public void sample() {
              setValue(maxrunning.get());
            }
          });
    }

    return root;
  }

  private CounterSet root;

  /*
   * The #of tasks that start execution (enter {@link LockManagerTask#call()}). This counter is
   * incremented BEFORE the task attempts to acquire its resource lock(s).
   */
  final AtomicLong nstarted = new AtomicLong(0);

  /** The #of tasks that end execution (exit {@link LockManagerTask#call()}). */
  final AtomicLong nended = new AtomicLong(0);

  /** The #of tasks that had an error condition. */
  final AtomicLong nerror = new AtomicLong(0);

  /*
   * The #of tasks that deadlocked when they attempted to acquire their locks. Note that a task MAY
   * retry lock acquisition and this counter will be incremented each time it does so and then
   * deadlocks.
   */
  final AtomicLong ndeadlock = new AtomicLong(0);

  /*
   * The #of tasks that timed out when they attempted to acquire their locks. Note that a task MAY
   * retry lock acquisition and this counter will be incremented each time it does so and then times
   * out.
   */
  final AtomicLong ntimeout = new AtomicLong(0);

  /** #of tasks that are either waiting on locks or attempting to acquire their locks. */
  final AtomicLong nwaiting = new AtomicLong(0);

  /** #of tasks that have acquired their locks and are concurrently executing. */
  final AtomicLong nrunning = new AtomicLong(0);

  /** The maximum observed value of {@link #nrunning}. */
  final AtomicLong maxrunning = new AtomicLong(0);

  /*
   * Create a lock manager for resources and concurrent operations.
   *
   * <p>Note that there is no concurrency limit imposed by the {@link LockManager} when
   * predeclareLocks is true as deadlocks are impossible and we do not maintain a WAITS_FOR graph.
   *
   * @param maxConcurrency The maximum multi-programming level (ignored if predeclareLocks is true).
   * @param predeclareLocks When true operations MUST declare all locks before they begin to
   *     execute. This makes possible several efficiencies and by sorting the resources in each lock
   *     request into a common order we are able to avoid deadlocks entirely.
   */
  public LockManager(final int maxConcurrency, final boolean predeclareLocks) {

    this(maxConcurrency, predeclareLocks, true /* sortLockRequests */);
  }

  /*
   * Create a lock manager for resources and concurrent operations.
   *
   * <p>Note that there is no concurrency limit imposed by the {@link LockManager} when
   * <i>predeclareLocks</i> is <code>true</code> as deadlocks are impossible and we do not maintain
   * a <code>WAITS_FOR</code> graph.
   *
   * @param maxConcurrency The maximum multi-programming level (ignored if predeclareLocks is true).
   * @param predeclareLocks When true operations MUST declare all locks before they begin to
   *     execute. This makes possible several efficiencies and by sorting the resources in each lock
   *     request into a common order we are able to avoid deadlocks entirely.
   * @param sortLockRequests This option indicates whether or not the resources in a lock request
   *     will be sorted before attempting to acquire the locks for those resources. Normally <code>
   *     true</code> this option MAY be disabled for testing purposes. It is an error to disable
   *     this option if <i>predeclareLocks</i> is <code>false</code>.
   */
  LockManager(
      final int maxConcurrency, final boolean predeclareLocks, final boolean sortLockRequests) {

    if (maxConcurrency < 2 && !predeclareLocks) {

      throw new IllegalArgumentException(
          "maxConcurrency: must be 2+ unless you are predeclaring locks, not " + maxConcurrency);
    }

    if (predeclareLocks && !sortLockRequests) {

      /*
       * This is required since we do not maintain TxDag when locks
       * are predeclare and therefore can not detect deadlocks.
       * Sorting with predeclared locks avoids the possibility of
       * deadlocks so we do not need the TxDag (effectively, it means
       * that all locks that can be requested by an operation are
       * sorted since they are predeclared and acquired in one go).
       */

      throw new IllegalArgumentException(
          "Sorting of lock requests MUST be enabled when locks are being predeclared.");
    }

    this.predeclareLocks = predeclareLocks;

    this.sortLockRequests = sortLockRequests;

    lockedResources =
        new ConcurrentHashMap<>(maxConcurrency);

    if (predeclareLocks) {

      /*
       * Note: waitsFor is NOT required if we will acquire all locks
       * at once for a given operation since we can simply sort the
       * lock requests for each operation into a common order, thereby
       * making deadlock impossible!
       *
       * Note: waitsFor is also NOT required if we are using only a
       * single threaded system.
       *
       * Note: if you allocate waitsFor here anyway then you can
       * measure the cost of deadlock detection. As far as I can tell
       * it is essentially zero when locks are predeclared.
       */

      waitsFor = null;

      //                waitsFor = new TxDag(maxConcurrency);

    } else {

      /*
       * Construct the directed graph used to detect deadlock cycles.
       */

      waitsFor = new TxDag(maxConcurrency);
    }
  }

  /*
   * Add if absent and return a {@link ResourceQueue} for the named resource.
   *
   * @param resource The resource.
   * @return The {@link ResourceQueue}.
   */
  private ResourceQueue<R, Thread> declareResource(final R resource) {

    // test 1st to avoid creating a new ResourceQueue if it already exists.
    ResourceQueue<R, Thread> resourceQueue = resourceQueues.get(resource);

    // not found, so create a new ResourceQueue for that resource.
    resourceQueue = new ResourceQueue<>(resource, waitsFor);

    // put if absent.
    final ResourceQueue<R, Thread> oldval = resourceQueues.putIfAbsent(resource, resourceQueue);

    if (oldval != null) {

      // concurrent insert, so use the winner's resource queue.
      return oldval;
    }

    // we were the winner, so return the our new resource queue.
    return resourceQueue;
  }

  /*
   * Drop a resource.
   *
   * <p>The caller must have lock on the resource. All tasks blocked waiting for that resource will
   * be aborted.
   *
   * @param resource The resource.
   * @throws IllegalArgumentException if the resource does not exist.
   * @throws IllegalStateException if the caller does not have a lock on the resource.
   */
  void dropResource(final R resource) {

    final Thread tx = Thread.currentThread();

    // synchronize before possible modification.
    synchronized (resourceQueues) {
      final ResourceQueue<R, Thread> resourceQueue = resourceQueues.get(resource);

      if (resourceQueue == null) {

        throw new IllegalArgumentException("No such resource: " + resource);
      }

      /*
       * If the caller has the lock then aborts anyone waiting on that
       * resource and releases the lock; otherwise throws an
       * exception.
       */
      resourceQueue.clear(tx);

      resourceQueues.remove(resource);
    }
  }

  /*
   * Lock resource(s).
   *
   * <p>Note: If you can not obtain the required lock(s) then you MUST use {@link #releaseLocks()}
   * to make sure that you release any locks that you might have obtained.
   *
   * @param resource The resource(s) to be locked.
   * @param timeout The lock timeout -or- 0L to wait forever.
   * @throws InterruptedException If the operation is interrupted while awaiting a lock.
   * @throws DeadlockException If the lock request would cause a deadlock.
   * @throws TimeoutException If the lock request times out.
   * @throws IllegalStateException If locks are being predeclared and there are already locks held
   *     by the operation.
   */
  void lock(R[] resource, final long timeout)
      throws InterruptedException, DeadlockException, TimeoutException {

    if (resource == null) {

      throw new NullPointerException();
    }

    for (int i = 0; i < resource.length; i++) {

      if (resource[i] == null) {

        throw new NullPointerException();
      }
    }

    if (timeout < 0) throw new IllegalArgumentException();

    if (resource.length == 0) return; // NOP.

    final Thread t = Thread.currentThread();

    if (predeclareLocks) {

      // verify that no locks are held for this operation.
      final Collection<ResourceQueue<R, Thread>> resources = lockedResources.get(t);

      if (resources != null) {

        /*
         * The operation has already declared some locks. Since
         * [predeclareLocks] is true it is not permitted to grow the set
         * of declared locks, so we throw an exception.
         */

        throw new IllegalStateException("Operation already has lock(s): " + t);
      }
    }

    if (resource.length > 1 && sortLockRequests) {

      /*
       * Sort the resources in the lock request.
       *
       * Note: Sorting the resources reduces the chance of a deadlock and
       * excludes it entirely when predeclaration of locks is also used.
       *
       * Note: We clone the resources to avoid side-effects on the caller.
       *
       * Note: This will throw an exception if the "resource" does not
       * implement Comparable.
       */

      resource = resource.clone();

      Arrays.sort(resource);
    }

    if (INFO) {

      log.info("Acquiring lock(s): " + Arrays.toString(resource));
    }

    if (lockedResources.get(t) == null) {

      final int initialCapacity = resource.length > 16 ? resource.length : 16;

      lockedResources.put(t, new LinkedHashSet<>(initialCapacity));
    }

    for (int i = 0; i < resource.length; i++) {

      lock(t, resource[i], timeout);
    }

    if (INFO) {

      log.info("Acquired lock(s): " + Arrays.toString(resource));
    }
  }

  /*
   * Obtain a lock on a resource.
   *
   * @param resource The resource to be locked.
   * @param timeout The lock timeout -or- 0L to wait forever.
   * @throws InterruptedException
   */
  private void lock(final Thread t, final R resource, final long timeout)
      throws InterruptedException {

    // make sure queue exists for this resource.
    final ResourceQueue<R, Thread> resourceQueue = declareResource(resource);

    // acquire the lock.
    resourceQueue.lock(t, timeout);

    // add queue to the set of queues whose locks are held by this task.
    final Collection<ResourceQueue<R, Thread>> tmp = lockedResources.get(t);

    if (tmp == null) {

      /*
       * Note: The caller should have created this collection first.
       */

      throw new AssertionError();
    }

    tmp.add(resourceQueue);
  }

  /*
   * Release all locks.
   *
   * @param waiting <code>false</code> iff the operation was <strong>known</strong> to be running.
   *     Otherwise <code>true</code> to indicate that the operation is awaiting a lock. An
   *     optimization is used to update the {@link TxDag} when the operation is NOT waiting. Since
   *     that optimization is invalid when the operation is waiting, always specify <code>true
   *     </code> if you are not sure and the less efficient technique will be used to update the
   *     {@link TxDag}.
   * @todo The [waiting] flag is not being used to optimize the removal of edges from the WAITS_FOR
   *     graph. Fixing this will require us to remove the operation from each {@link ResourceQueue}
   *     without updating the {@link TxDag} and then update the {@link TxDag} using {@link
   *     TxDag#removeEdges(Object, boolean)} and specifying "false" for "waiting". Since this
   *     operation cuts across multiple queues at once additional synchronization MAY be required.
   */
  void releaseLocks(final boolean waiting) {

    if (INFO) log.info("Releasing locks");

    //            resourceManagementLock.lock();

    final Thread t = Thread.currentThread();

    try {

      final Collection<ResourceQueue<R, Thread>> resources = lockedResources.remove(t);

      if (resources == null) {

        if (INFO) log.info("No locks: " + t);

        return;
      }

      /*
       * Note: The way this is written releasing locks is not atomic.
       * This means that blocked operations can start as soon as a
       * resource becomes available rather than waiting until the
       * operation has released all of its locks. I don't think that
       * there are any negative consequences to this.
       */

      if (INFO) log.info("Releasing resource locks: resources=" + resources);

      final Iterator<ResourceQueue<R, Thread>> itr = resources.iterator();

      while (itr.hasNext()) {

        final ResourceQueue<R, Thread> resourceQueue = itr.next();

        final R resource = resourceQueue.getResource();

        //                final ResourceQueue<R, Thread> resourceQueue = resourceQueues
        //                        .get(resource);

        if (!resourceQueues.containsKey(resource)) {

          /*
           * Note: This would indicate a failure of the mechanisms
           * which keep the resource queues around while there are
           * tasks seeking or holding locks for those queues.
           */

          throw new IllegalStateException("No queue for resource: " + resource);
        }

        try {

          // release a lock on a resource.

          resourceQueue.unlock(t);

        } catch (Throwable ex) {

          log.warn("Could not release lock", ex);

          // Note: release the rest of the locks anyway.

          continue;
        }

        if (INFO) log.info("Released lock: " + resource);
      }

      if (INFO) log.info("Released resource locks: resources=" + resources);

    } catch (Throwable ex) {

      log.error("Could not release locks: " + ex, ex);

    } finally {

      /*
       * Release the vertex (if any) in the WAITS_FOR graph.
       *
       * Note: A vertex is created iff a dependency chain is
       * established. Therefore it is possible for a transaction to
       * obtain a lock without a vertex begin created for that
       * tranasaction. Hence it is Ok if this method returns [false].
       */

      if (waitsFor != null) {

        waitsFor.releaseVertex(t);
      }

      //                resourceManagementLock.unlock();

    }
  }

  /*
   * Invoked when a task begins to run.
   *
   * @param task
   */
  void didStart(final Callable task) {

    nstarted.incrementAndGet();

    if (INFO) log.info("Started: nstarted=" + nstarted);
  }

  /** Invoked on successful task completion. */
  void didSucceed(final Callable task) {

    nended.incrementAndGet();

    try {

      /*
       * Force release of locks (if any) and removal of the vertex (if
       * any) from the WAITS_FOR graph.
       *
       * Note: An operation that completes successfully is by definition
       * NOT awaiting a lock.
       */

      final boolean waiting = false;

      releaseLocks(waiting);

    } catch (Throwable t) {

      log.warn("Problem(s) releasing locks: " + t, t);
    }

    if (INFO) log.info("Ended: nended=" + nended);
  }

  /*
   * Invoke if a task aborted.
   *
   * @param task
   * @param t
   * @param waiting <code>false</code> iff the operation was <strong>known</strong> to be running.
   *     Otherwise <code>true</code> to indicate that the operation is awaiting a lock. An
   *     optimization is used to update the {@link TxDag} when the operation is NOT waiting. Since
   *     that optimization is invalid when the operation is waiting, always specify <code>true
   *     </code> if you are not sure and the less efficient technique will be used to update the
   *     {@link TxDag}.
   */
  void didAbort(final Callable task, final Throwable t, final boolean waiting) {

    if (INFO) log.info("Begin: nended=" + nended);

    nerror.incrementAndGet();

    try {

      /*
       * Force release of locks (if any) and removal of the vertex (if
       * any) from the WAITS_FOR graph.
       */

      releaseLocks(waiting);

    } catch (Throwable t2) {

      log.warn("Problem(s) releasing locks: " + t2, t2);
    }

    if (INFO) log.info("Ended: nended=" + nended);
  }

  public String toString() {

    return getCounters().toString();
  }
}
