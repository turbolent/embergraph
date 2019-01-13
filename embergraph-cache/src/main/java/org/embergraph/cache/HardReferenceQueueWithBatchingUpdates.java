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
 * Created on Jan 11, 2010
 */

package org.embergraph.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * A variant relying on thread-local {@link IHardReferenceQueue}s to batch updates and thus minimize
 * thread contention for the lock required to synchronize calls to {@link #add(Object)}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo This class was created to improve concurrency for the read-only {@link
 *     org.embergraph.btree.BTree} by batching updates from each thread using a thread-local queue.
 *     While SOME of the methods from the base class have been modified to preserve the {@link
 *     Queue} or {@link IHardReferenceQueue} semantics across both the thread-local and the backing
 *     shared queue, many methods have not been so modified and care MUST be taken if you wish to
 *     reuse this class for another purpose.
 */
public class HardReferenceQueueWithBatchingUpdates<T> implements IHardReferenceQueue<T> {

  /*
   * Thread local buffer stuff.
   */

  /** The concurrency level of the cache. */
  private final int concurrencyLevel;

  /*
   * When <code>true</code> true thread-local buffers will be used. Otherwise, striped locks will be
   * used and each lock will protect its own buffer.
   *
   * @see #add(Object)
   */
  private final boolean threadLocalBuffers;

  /*
   * Used iff striped locks are used.
   */
  /** The striped locks and <code>null</code> if per-thread {@link BatchQueue}s are being used. */
  //    private final Semaphore[] permits;
  private final Lock[] permits;

  /*
   * The {@link BatchQueue}s protected by the striped locks and <code>null</code> if per-thread
   * {@link BatchQueue}s are being used.
   */
  private final BatchQueue<T>[] buffers;

  /*
   * Used iff true thread-local buffers are used.
   */

  /** The thread-local queues. */
  private final ConcurrentHashMap<Thread, BatchQueue<T>> threadLocalQueues;

  /** Scanning is done on the thread-local {@link BatchQueue}. */
  private final int threadLocalQueueNScan;

  /** The capacity of the thread-local {@link BatchQueue}. */
  private final int threadLocalQueueCapacity;

  /*
   * When our inner queue has this many entries we will invoke tryLock() and batch the updates if we
   * can barge in on the lock.
   */
  private final int threadLocalTryLockSize;

  /** Optional listener is notified when updates are batched through. */
  private final IBatchedUpdateListener<T> batchedUpdatedListener;

  /** Lock used to synchronize operations on the {@link #sharedQueue}. */
  private final ReentrantLock lock = new ReentrantLock(false /* fair */);

  /** The shared queue. Touches are batched onto this queue by the {@link #threadLocalQueues}. */
  private final IHardReferenceQueue<T> sharedQueue;

  /*
   * The listener to which cache eviction notices are reported by the thread- local queues. This
   * listener is responsible for adding the evicted reference to the {@link #sharedQueue}.
   */
  private final HardReferenceQueueEvictionListener<T> threadLocalQueueEvictionListener;

  //    /*
  //     * @param listener
  //     *            The eviction listener (sees only evictions from the outer
  //     *            class).
  //     * @param capacity
  //     *            The capacity of this cache (does not include the capacity of
  //     *            the thread-local caches).
  //     */
  //    public HardReferenceQueueWithBatchingUpdates(
  //            final HardReferenceQueueEvictionListener<T> listener,
  //            final int capacity) {
  //
  //            this(
  //                    new HardReferenceQueue<T>(listener, capacity, 0/* nscan */),
  ////                    listener, capacity,
  //                IHardReferenceQueue.DEFAULT_NSCAN,// threadLocalNScan
  //                64,// threadLocalCapacity
  //                32, // threadLocalTryLockSize
  //                null // batched update listener
  //                );
  //
  //    }

  /*
   * Designated constructor.
   *
   * @param sharedQueue The backing {@link IHardReferenceQueue}.
   * @param threadLocalQueueNScan The #of references to scan on the thread-local queue.
   * @param threadLocalQueueCapacity The capacity of the thread-local queues in which the updates
   *     are gathered before they are batched into the shared queue. This must be at least
   * @param threadLocalTryLockSize Once the thread-local queue is this full an attempt will be made
   *     to barge in on the lock and batch the updates to the shared queue. This feature may be
   *     disabled by passing ZERO (0).
   */
  public HardReferenceQueueWithBatchingUpdates(
      final IHardReferenceQueue<T> sharedQueue,
      //            final HardReferenceQueueEvictionListener<T> listener,
      //            final int capacity,
      final int threadLocalQueueNScan,
      final int threadLocalQueueCapacity,
      final int threadLocalTryLockSize,
      final IBatchedUpdateListener<T> batchedUpdateListener) {
    this(
        true /* threadLocalBuffers */,
        16 /* concurrencyLevel */,
        sharedQueue,
        threadLocalQueueNScan,
        threadLocalQueueCapacity,
        threadLocalTryLockSize,
        batchedUpdateListener);
  }

  public HardReferenceQueueWithBatchingUpdates(
      final boolean threadLocalBuffers,
      final int concurrencyLevel,
      final IHardReferenceQueue<T> sharedQueue,
      //            final HardReferenceQueueEvictionListener<T> listener,
      //            final int capacity,
      final int threadLocalQueueNScan,
      final int threadLocalQueueCapacity,
      final int threadLocalTryLockSize,
      final IBatchedUpdateListener<T> batchedUpdateListener) {

    if (sharedQueue == null) throw new IllegalArgumentException();

    this.sharedQueue = sharedQueue;
    //        sharedQueue = new HardReferenceQueue<T>(listener, capacity, 0/* nscan */);

    if (threadLocalQueueCapacity <= 0) throw new IllegalArgumentException();

    if (threadLocalQueueNScan < 0 || threadLocalQueueNScan > threadLocalQueueCapacity)
      throw new IllegalArgumentException();

    if (threadLocalTryLockSize < 0 || threadLocalTryLockSize > threadLocalQueueCapacity)
      throw new IllegalArgumentException();

    this.threadLocalQueueNScan = threadLocalQueueNScan;

    this.threadLocalQueueCapacity = threadLocalQueueCapacity;

    this.threadLocalTryLockSize = threadLocalQueueCapacity;

    this.batchedUpdatedListener = batchedUpdateListener;

    /*
     * Add the reference to the backing queue for the outer class. The caller MUST hold the
     * outer class lock.
     */
    this.threadLocalQueueEvictionListener =
        (cache, ref) -> {

          // Note: invokes add() on the shared inner queue.
          sharedQueue.add(ref);
        };

    this.threadLocalBuffers = threadLocalBuffers;
    this.concurrencyLevel = concurrencyLevel;
    if (threadLocalBuffers) {
      /*
       * Per-thread buffers.
       */
      permits = null;
      buffers = null;
      threadLocalQueues =
          new ConcurrentHashMap<>(
              16, // initialCapacity
              0.75f, // load factor (default is .75f)
              concurrencyLevel);
    } else {
      /*
       * Striped locks.
       */
      //            permits = new Semaphore[concurrencyLevel];
      permits = new Lock[concurrencyLevel];
      buffers = new BatchQueue[concurrencyLevel];
      threadLocalQueues = null;
      for (int i = 0; i < concurrencyLevel; i++) {
        //                permits[i] = new Semaphore(1, false/* fair */);
        permits[i] = new ReentrantLock(false /*fair*/);
        buffers[i] =
            new BatchQueue<>(
                i, // id
                threadLocalQueueNScan,
                threadLocalQueueCapacity,
                threadLocalTryLockSize,
                lock,
                threadLocalQueueEvictionListener,
                batchedUpdatedListener);
      }
    }
  }

  /*
   * Return a thread-local queue which may be used to batch updates to this {@link
   * HardReferenceQueueWithBatchingUpdates}. The returned queue will combine calls to {@link
   * IHardReferenceQueue#add(Object)} in a thread-local array, batching updates from the array to
   * <i>this</i> queue periodically. This can substantially reduce contention for the lock required
   * to synchronize before invoking {@link #add(Object)}.
   *
   * <p>Note: The returned queue handles synchronization for {@link #add(Object)} internally using
   * {@link Lock#tryLock()} and {@link Lock#lock()}.
   *
   * <p>Note: {@link IHardReferenceQueue#add(Object)} for the returned reference will report <code>
   * true</code> iff the object is already on the thread-local queue and DOES NOT consider whether
   * the object is already on <i>this</i> queue. Therefore {@link #getThreadLocalQueue()} MUST NOT
   * be used if the value returned by {@link #add(Object)} is significant (for example, do not use
   * the thread-local batch queue for the mutable {@link org.embergraph.btree.BTree}!).
   *
   * @return The thread-local queue used to batch updates to <i>this</i> queue.
   */
  private final BatchQueue<T> getThreadLocalQueue() {

    final Thread t = Thread.currentThread();

    BatchQueue<T> tmp = threadLocalQueues.get(t); // id);

    if (tmp == null) {

      if (threadLocalQueues.put(
              t,
              tmp =
                  new BatchQueue<>(
                      0 /* idIsIgnored */,
                      threadLocalQueueNScan,
                      threadLocalQueueCapacity,
                      threadLocalTryLockSize,
                      lock,
                      threadLocalQueueEvictionListener,
                      batchedUpdatedListener))
          != null) {

        /*
         * Note: Since the key is the thread it is not possible for there to
         * be a concurrent put of an entry under the same key so we do not
         * have to use putIfAbsent().
         */

        throw new AssertionError();
      }
    }

    return tmp;
  }

  //    /*
  //     * Acquire a {@link BatchQueue} from an internal array of {@link BatchQueue}
  //     * instances using a striped lock pattern.
  //     */
  //    private BatchQueue<T> acquire() throws InterruptedException {
  //
  //        // Note: Thread.getId() is a positive integer.
  //        final int i = (int) (Thread.currentThread().getId() % concurrencyLevel);
  //
  ////        permits[i].acquire();
  //        permits[i].lock();
  //
  //        return buffers[i];
  //
  //    }
  //
  //    /*
  //     * Release a {@link BatchQueue} obtained using {@link #acquire()}.
  //     *
  //     * @param b
  //     *            The {@link BatchQueue}.
  //     */
  //    private void release(final BatchQueue<T> b) {
  //
  ////        permits[b.id].release();
  //        permits[b.id].unlock();
  //
  //    }

  /*
   * IHardReferenceQueue
   */

  /** The size of the shared queue (approximate). */
  public int size() {
    return sharedQueue.size();
  }
  //    /*
  //     * Reports the combined size of the thread-local queue plus the shared
  //     * queue.
  //     */
  //    public int size() {
  //
  //        return getThreadLocalQueue().size + size;
  //
  //    }

  /** The capacity of the shared queue. */
  public int capacity() {
    return sharedQueue.capacity();
  }

  /** The nscan value of the shared queue. */
  public int nscan() {
    return sharedQueue.nscan();
  }

  /** Not supported. */
  public boolean evict() {
    throw new UnsupportedOperationException();
  }

  /** Not supported. */
  public void evictAll(boolean clearRefs) {
    throw new UnsupportedOperationException();
  }

  /** Not supported. */
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
    //        return innerQueue.isEmpty();
  }

  /** Not supported. */
  public boolean isFull() {
    throw new UnsupportedOperationException();
    //        return innerQueue.isFull();
  }

  /** Not supported. */
  public T peek() {
    throw new UnsupportedOperationException();
  }

  /*
   * Adds the reference to the thread-local queue, returning <code>true</code> iff the queue was
   * modified as a result.
   *
   * <p>When using true thread-local buffers, this is non-blocking unless the thread-local queue is
   * full. If the thread-local queue is full, the existing references will be batched first onto the
   * shared queue.
   *
   * <p>Contention can arise when using striped locks. For the synthetic test (on a 2 core laptop
   * with 8 threads), implementing using per-thread {@link BatchQueue}s scores <code>6,984,896
   * </code> ops/sec whereas implementing using striped locks the performance score is only <code>
   * 4,654,673</code>. One thread on the laptop has a throughput of <code>4,856,814</code>, the
   * maximum possible throughput for 2 threads is ~ 9M. The actual performance of the striped locks
   * approach depends on the degree of collision in the {@link Thread#getId()} values and the #of
   * {@link BatchQueue} instances in the array.
   *
   * <p>While striped locks clearly have less throughput when compared to thread- local {@link
   * BatchQueue}s, the striped lock performance can be significantly better than implementations
   * without lock amortization strategies and we do not have to worry about references on {@link
   * BatchQueue}s "escaping" when we rarely see requests for some threads (which is basically a
   * memory leak).
   *
   * @return The {@link BatchQueue}.
   * @throws InterruptedException
   *     <p>TODO Actually, using the CHM (7M ops/sec) rather than acquiring a permit (4.7M ops/sec)
   *     is MUCH less expensive even when using only one thread. The permit is clearly costing us.
   *     Striped locks might do as well as thread-local locks if we could replace the permit with a
   *     less expensive lock.
   */
  public final boolean add(final T ref) {

    if (threadLocalBuffers) {

      /*
       * Per-thread buffers.
       */
      return getThreadLocalQueue().add(ref);

    } else {

      /*
       * Striped locks.
       */

      // Note: Thread.getId() is a positive integer.
      //            final int i = (int) (Thread.currentThread().getId() % concurrencyLevel);
      final int i = hash(ref);

      BatchQueue<T> t = null;
      try {

        //                permits[i].acquire();
        //              t = acquire();
        permits[i].lockInterruptibly();

        t = buffers[i];

        return t.add(ref);

      } catch (InterruptedException ex) {

        throw new RuntimeException(ex);

      } finally {

        if (t != null) {
          //                    release(t);
          permits[i].unlock();
        }
      }
    }
  }

  private int hash(final T ref) {

    int h;

    // Note: Thread.getId() is a positive integer.
    h = (int) (Thread.currentThread().getId() % concurrencyLevel);
    //        h = ((int) Thread.currentThread().getId()) % concurrencyLevel;

    //      h = ref.hashCode();
    //
    //        h = h > 0 ? h % concurrencyLevel : -(h % concurrencyLevel);

    return h;
  }

  /*
   * Offers the reference to the thread-local queue, returning <code>true</code> iff the queue was
   * modified as a result. This is non-blocking unless the thread-local queue is full. If the
   * thread-local queue is full, the existing references will be batched first onto the shared
   * queue.
   */
  public final boolean offer(final T ref) {

    throw new UnsupportedOperationException();
    //        return getThreadLocalQueue().offer(ref);

  }

  /*
   * Discards the thread-local buffers and clears the backing ring buffer.
   *
   * <p>Note: This method can have side-effects from asynchronous operations if the queue is still
   * in use.
   */
  public final void clear(final boolean clearRefs) {

    lock.lock();

    try {

      if (threadLocalBuffers) {

        for (BatchQueue<T> q : threadLocalQueues.values()) {

          // clear the thread local queues.
          q.clear(clearRefs);
        }

        // discard map entries.
        threadLocalQueues.clear();

      } else {

        for (BatchQueue<T> q : buffers) {

          // clear the thread local queues.
          q.clear(clearRefs);
        }
      }

      // clear the shared backing queue.
      sharedQueue.clear(true /* clearRefs */);

    } finally {

      lock.unlock();
    }
  }

  /** Not supported. */
  //    Tests the thread-local buffer first, then the shared buffer.
  public final boolean contains(final Object ref) {

    throw new UnsupportedOperationException();

    //        if (getThreadLocalQueue().contains(ref)) {
    //
    //            // found in the thread-local queue.
    //            return true;
    //
    //        }
    //
    //        // test the shared queue.
    //        lock.lock();
    //
    //        try {
    //
    //            return sharedQueue.contains(ref);
    //
    //        } finally {
    //
    //            lock.unlock();
    //
    //        }

  }

  /*
   * Inner class provides thread-local batching of updates to the outer {@link HardReferenceQueue}.
   * This can substantially decrease the contention for the lock required to provide safe access to
   * the outer {@link HardReferenceQueue} during updates.
   *
   * @param <T>
   */
  private static class BatchQueue<T> extends RingBuffer<T> implements IHardReferenceQueue<T> {

    private final int id;
    private final int nscan;
    private final int tryLockSize;
    private final Lock lock;
    private final HardReferenceQueueEvictionListener<T> listener;
    private final IBatchedUpdateListener<T> batchedUpdatedListener;

    /*
     * @param id The identifier for this instance (used with striped locks).
     * @param nscan
     * @param capacity
     * @param tryLockSize
     * @param lock
     * @param listener
     * @param batchedUpdateListener
     */
    public BatchQueue(
        final int id,
        final int nscan,
        final int capacity,
        final int tryLockSize,
        final ReentrantLock lock,
        final HardReferenceQueueEvictionListener<T> listener,
        final IBatchedUpdateListener<T> batchedUpdateListener) {

      super(capacity);

      this.id = id;

      this.nscan = nscan;

      this.tryLockSize = tryLockSize;

      this.lock = lock;

      this.listener = listener;

      this.batchedUpdatedListener = batchedUpdateListener;
    }

    /** Return the value on the outer class. */
    public int nscan() {

      return nscan;
    }

    /*
     * Add a reference to the cache. If the reference was recently added to the cache then this is a
     * NOP. Otherwise the reference is appended to the cache. If a reference is appended to the
     * cache and then cache is at capacity, then the LRU reference is first evicted from the cache.
     *
     * @param ref The reference to be added.
     * @return True iff the reference was added to the cache and false iff the reference was found
     *     in a scan of the nscan MRU cache entries.
     */
    @Override
    public boolean add(final T ref) {

      /*
       * Scan the last nscan references for this reference. If found,
       * return immediately.
       */
      if (nscan > 0 && scanHead(nscan, ref)) {

        return false;
      }

      // add to the thread-local queue.
      return super.add(ref);
    }

    @Override
    public boolean offer(final T ref) {

      /*
       * Scan the last nscan references for this reference. If found,
       * return immediately.
       */
      if (nscan > 0 && scanHead(nscan, ref)) {

        return false;
      }

      // offer to the thread-local queue.
      return super.offer(ref);
    }

    /*
     * Extended to batch the updates to the base class for the outer class when the inner queue is
     * half full (tryLock()) and when the inner queue is full (lock()).
     */
    @Override
    protected void beforeOffer(final T ref) {

      //            assert size <= capacity : "size=" + size + ", capacity=" + capacity;

      if (tryLockSize != 0 && size == tryLockSize) {

        if (lock.tryLock()) {

          /*
           * Batch evict all references to the outer class's queue.
           */

          try {

            evictAll(true /* clearRefs */);

            //                        assert size == 0 : "size=" + size;

            if (batchedUpdatedListener != null) {

              batchedUpdatedListener.didBatchUpdates();
            }

          } finally {

            lock.unlock();
          }
        }

        return;
      }

      // @todo why does this fail if written as (size == capacity)???
      if (size + 1 == capacity) {

        /*
         * If at capacity, batch evict all references to the outer
         * class's queue.
         */

        lock.lock();

        try {

          evictAll(true /* clearRefs */);

          //                    assert size == 0 : "size=" + size;

          if (batchedUpdatedListener != null) {

            batchedUpdatedListener.didBatchUpdates();
          }

        } finally {

          lock.unlock();
        }
      }

      //            assert size < capacity : "size=" + size + ", capacity=" + capacity;

    }

    public boolean evict() {

      //            assert lock.isHeldByCurrentThread();

      final T ref = poll();

      if (ref == null) {

        // buffer is empty.
        return false;
      }

      if (listener != null) {

        // report eviction notice to listener.
        listener.evicted(this, ref);
      }

      return true;
    }

    /*
     * Evict all references, starting with the LRU reference and proceeding to the MRU reference.
     *
     * @param clearRefs When true, the reference are actually cleared from the cache. This may be
     *     false to force persistence of the references in the cache without actually clearing the
     *     cache.
     */
    //        synchronized
    public final void evictAll(final boolean clearRefs) {
      // System.err.println((clearRefs?'T':'F')+" : "+Thread.currentThread());
      //            assert lock.isHeldByCurrentThread();
      if (clearRefs) {

        /*
         * Evict all references, clearing each as we go.
         */

        while (!isEmpty()) { // count > 0 ) {

          evict();
        }

      } else {

        /*
         * Generate eviction notices in LRU to MRU order but do NOT clear
         * the references.
         */

        final int size = size();

        for (int n = 0; n < size; n++) {

          final T ref = get(n);

          if (listener != null) {

            // report eviction notice to listener.
            listener.evicted(this, ref);
          }
        }
      }

      //            assert size() == 0 : "size=" + size();

    }
  }
}
