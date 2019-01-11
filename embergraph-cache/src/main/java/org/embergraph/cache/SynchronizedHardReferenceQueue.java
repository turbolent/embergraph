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
 * Created on Feb 9, 2009
 */

package org.embergraph.cache;


/**
 * Thread-safe version.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SynchronizedHardReferenceQueue<T> implements IHardReferenceQueue<T> {

    /**
     * Note: Synchronization for the inner {@link #queue} is realized using the
     * <strong>outer</strong> reference!
     */
    protected final HardReferenceQueue<T> queue;

    /**
     * Defaults the #of references to scan on append requests to 10.
     * 
     * @param listener
     *            The listener on which cache evictions are reported.
     * @param capacity
     *            The maximum #of references that can be stored on the cache.
     *            There is no guarantee that all stored references are distinct.
     */
    public SynchronizedHardReferenceQueue(
            final HardReferenceQueueEvictionListener<T> listener,
            final int capacity) {

        this(listener, capacity, DEFAULT_NSCAN);

    }

    /**
     * Core impl.
     * 
     * @param listener
     *            The listener on which cache evictions are reported (optional).
     * @param capacity
     *            The maximum #of references that can be stored on the cache.
     *            There is no guarantee that all stored references are distinct.
     * @param nscan
     *            The #of references to scan from the MRU position before
     *            appended a reference to the cache. Scanning is used to reduce
     *            the chance that references that are touched several times in
     *            near succession from entering the cache more than once. The
     *            #of reference tests trades off against the latency of adding a
     *            reference to the cache.
     */
    public SynchronizedHardReferenceQueue(
            final HardReferenceQueueEvictionListener<T> listener,
            final int capacity, final int nscan) {

        this.queue = new InnerHardReferenceQueue(listener, capacity,
                DEFAULT_NSCAN);

    }

    /**
     * All attempts to add an element to the buffer invoke this hook before
     * checking the remaining capacity in the buffer. The caller will be
     * synchronized on <i>this</i> when this method is invoked.
     */
    protected void beforeOffer(final T t) {

        // NOP
        
    }

    /**
     * Inner class delegates {@link #beforeOffer(Object)} to the outer class.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
     *         Thompson</a>
     * @version $Id$
     * @param <T>
     */
    private final class InnerHardReferenceQueue extends HardReferenceQueue<T> {

        /**
         * 
         * @param listener
         * @param capacity
         * @param nscan
         */
        public InnerHardReferenceQueue(
                final HardReferenceQueueEvictionListener<T> listener,
                final int capacity, final int nscan) {

            super(listener, capacity, nscan);

        }

        @Override
        protected final void beforeOffer(final T ref) {

            // delegate to the outer class.
            SynchronizedHardReferenceQueue.this.beforeOffer(ref);

            // delegate to the super class.
            super.beforeOffer(ref);

        }

    }

    /*
     * Methods which DO NOT require synchronization.
     */
    final public int capacity() {
        return queue.capacity();
    }

    public HardReferenceQueueEvictionListener<T> getListener() {
        return queue.getListener();
    }

    public int nscan() {
        return queue.nscan();
    }

    /*
     * Methods which DO require synchronization.
     */
    
    synchronized public boolean add(T ref) {
        return queue.add(ref);
    }

    synchronized public void clear(boolean clearRefs) {
        queue.clear(clearRefs);
    }

    synchronized public boolean evict() {
        return queue.evict();
    }

    synchronized public void evictAll(boolean clearRefs) {
        queue.evictAll(clearRefs);
    }

    synchronized public T peek() {
        return queue.peek();
    }

    synchronized public boolean isEmpty() {
        return queue.isEmpty();
    }

    synchronized public boolean isFull() {
        return queue.isFull();
    }

    synchronized public boolean scanHead(int nscan, T ref) {
        return queue.scanHead(nscan, ref);
    }

    synchronized public boolean scanTail(int nscan, T ref) {
        return queue.scanTail(nscan, ref);
    }

    synchronized public int size() {
        return queue.size();
    }

}
