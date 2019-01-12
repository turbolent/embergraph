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
 * Created on Nov 8, 2006
 */

package org.embergraph.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A unsynchronized fixed capacity ring buffer. The buffer does not accept <code>null</code>
 * elements. If you want a thread-safe {@link Queue} consider {@link ArrayBlockingQueue} instead.
 *
 * <p>Note: The "head" of the ring buffer is the insertion point, NOT the MRU element which is
 * located at the previous "head" index. The "tail" of the ring buffer is the LRU position.
 * Unfortunately, these labels are exactly the reverse of the labels used to describe the {@link
 * Queue} semantics.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T> The reference type stored in the buffer.
 */
public class RingBuffer<T> implements Queue<T> {

  /** The capacity of the buffer. */
  protected final int capacity;

  /**
   * The internal fixed capacity buffer.
   *
   * <p>References are inserted at the {@link #head}, which is then post-incremented using <code>
   * (head + 1) % capacity</code>. So {@link #head} is always the next position at which a reference
   * would be inserted. See {@link #offer(Object)}
   *
   * <p>References are removed from the {@link #tail}, which is then post-incremented using <code>
   * (tail + 1) % capacity</code>. So {@link #tail} is always the next element to take from the
   * buffer IFF the buffer is non-empty. See {@link #poll()}
   *
   * <p>The buffer is empty IFF {@link #size()} EQ ZERO (0).
   *
   * <p>The buffer is full IFF {@link #size()} EQ {@link #capacity()}.
   *
   * <p>{@link #head} EQ {@link #tail} implies that the buffer is either full or empty. That
   * condition is preserved by {@link #remove()}, which must adjust the {@link #head} (the insertion
   * point) so that it does not "loose" capacity when elements are removed from the buffer by random
   * access.
   */
  private final T[] refs;

  /** The head (the insertion point for the next reference). */
  private int head = 0;

  /** The tail (the LRU position and the eviction point). */
  private int tail = 0;

  /**
   * The #of elements in the buffer. The buffer is empty when this field is zero. The buffer is full
   * when this field equals the {@link #capacity}.
   *
   * <p>Note: Exposed to {@link HardReferenceQueue} as an optimization for {@link #isFull()}.
   */
  protected int size = 0;

  /**
   * Ctor.
   *
   * @param capacity The capacity of the buffer.
   */
  @SuppressWarnings("unchecked")
  public RingBuffer(final int capacity) {

    if (capacity <= 0) throw new IllegalArgumentException();

    this.capacity = capacity;

    this.refs = (T[]) new Object[capacity];
  }

  /** The capacity of the buffer as specified to the constructor. */
  public final int capacity() {

    return capacity;
  }

  public final int size() {

    return size;
  }

  public final boolean isEmpty() {

    return size == 0;
  }

  /** True iff the buffer is full. */
  public final boolean isFull() {

    return size == capacity;
  }

  public boolean add(final T ref) throws IllegalStateException {

    if (ref == null) throw new NullPointerException();

    beforeOffer(ref);

    if (size == capacity /* isFull() inlined */) throw new IllegalStateException();

    refs[head] = ref;

    head = (head + 1) % capacity;

    size++;

    return true;
    //        if (!offer(ref))
    //            throw new IllegalStateException();
    //
    //        return true;

  }

  public boolean offer(final T ref) {

    if (ref == null) throw new NullPointerException();

    beforeOffer(ref);

    if (size == capacity /* isFull() inlined */) return false;

    refs[head] = ref;

    head = (head + 1) % capacity;

    size++;

    return true;
  }

  /**
   * All attempts to add an element to the buffer invoke this hook before checking the remaining
   * capacity in the buffer.
   *
   * <p>This hook provides an opportunity to realize an eviction protocol and is used for that
   * purpose by {@link HardReferenceQueue}. It is also used to realize the the stale reference
   * protocol in {@link SynchronizedHardReferenceQueueWithTimeout}.
   *
   * @todo it can be used to realize the chunk combiner on add protocol as well.
   */
  protected void beforeOffer(final T ref) {

    // NOP

  }

  public boolean addAll(final Collection<? extends T> c) {

    boolean modified = false;

    for (T e : c) {

      add(e);

      modified = true;
    }

    return modified;
  }

  /** Clears the buffer, setting the references in the buffer to <code>null</code>. */
  public void clear() {

    clear(true /* clearRefs */);
  }

  /**
   * Clears the buffer (sets the index of the head and the tail to zero and sets the count to zero).
   *
   * @param clearRefs When <code>true</code> the references are explicitly set to <code>null</code>
   *     which can facilitate garbage collection.
   */
  public final void clear(final boolean clearRefs) {

    if (clearRefs) {

      /*
       * Evict all references, clearing each as we go.
       */

      while (size > 0) {

        refs[tail] = null; // drop LRU reference.

        size--; // update #of references.

        tail = (tail + 1) % capacity; // update tail.
      }
    }

    // reset to initial conditions.

    head = tail = size = 0;
  }

  /*
   * package private methods used to write the unit tests.
   */

  /** The head index (the MRU position / insertion point). */
  final int getHeadIndex() {

    return head;
  }

  /** The tail index (the LRU position / eviction point). */
  final int getTailIndex() {

    return tail;
  }

  /** Return the buffer elements in MRU (head) to LRU (tail) order. */
  public Object[] toArray() {

    return toArray(new Object[size]);
  }

  /** Return the buffer elements in MRU (head) to LRU (tail) order. */
  @SuppressWarnings("unchecked")
  public <TX> TX[] toArray(final TX[] a) {

    final TX[] r =
        a.length >= size
            ? a
            : (TX[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

    int n = 0;
    for (int i = tail; n < size; n++) {

      final T ref = refs[i];

      assert ref != null;

      r[n] = (TX) ref;

      i = (i + 1) % capacity; // update index.
    }

    if (n < r.length) {

      // flag end of array w/ null iff over capacity @todo unit test.
      r[n] = null;
    }

    return r;
  }

  /**
   * Return the n<i>th</i> element in the buffer. The index positions are counted from the MRU (the
   * insertion point), which has an index of ZERO (0), to the LRU position (the eviction point),
   * which as an index of {@link #size()}-1.
   *
   * @param index The index of the desired element.
   * @return The element at that index.
   * @throws IllegalArgumentException if the index is less than ZERO (0).
   * @throws IllegalArgumentException if the index is greater than or equal to the {@link #size()}.
   */
  public final T get(final int index) {

    if (index < 0 || index >= size) throw new IllegalArgumentException();

    // the effective index.
    final int i = (tail + index) % capacity;

    // the element at that index.
    return refs[i];
  }

  /**
   * Return the reference at the specified index in the backing array.
   *
   * <p>Note: Unlike {@link #get(int)}, this method does not adjust by the index of the <code>tail
   * </code>. It is intended for use by subclasses which require low level access into the backing
   * array.
   *
   * @param i The index into the backing array.
   * @return The reference at that index.
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/465#comment:2">Too many GRS
   *     reads</a>
   */
  protected final T _get(final int i) {

    return refs[i];
  }

  /**
   * Remove the element at the specified index in the buffer. The index positions are counted from
   * the MRU (the insertion point), which has an index of ZERO (0), to the LRU position (the
   * eviction point), which as an index of {@link #size()}-1.
   *
   * @param index The index of the element to be removed.
   * @return The element at that index.
   * @throws IllegalArgumentException if the index is less than ZERO (0).
   * @throws IllegalArgumentException if the index is greater than or equal to the {@link #size()}.
   */
  T remove(final int index) {

    if (index < 0 || index >= size) throw new IllegalArgumentException();

    //        if (index + 1 == size) {
    //
    //            // remove the LRU position.
    //            return remove();
    //
    //        }

    /*
     * Otherwise we are removing some non-LRU element.
     */

    // the effective index.
    int i = (tail + index) % capacity;

    // the element at that index.
    final T ref = refs[i];

    // clear reference from the buffer.
    refs[i] = null;

    for (; ; ) {

      final int nexti = (i + 1) % capacity; // update index.

      if (nexti != head) {

        refs[i] = refs[nexti];

        i = nexti;

      } else {

        refs[i] = null;

        head = i;

        break;
      }
    }

    // there is one less element in the buffer.
    size--;

    return ref;
  }

  public final T element() throws NoSuchElementException {

    if (size == 0) throw new NoSuchElementException();

    return peek();
  }

  /** Note: This is the reference at the "tail" of the {@link RingBuffer} (the LRU position). */
  public final T peek() {

    if (size == 0) {

      // buffer is empty.
      return null;
    }

    return refs[tail];
  }

  public final T poll() {

    // The buffer must not be empty.
    if (size <= 0) return null;

    final T ref = refs[tail]; // LRU reference.
    refs[tail] = null; // drop reference.
    size--; // update #of references.
    tail = (tail + 1) % capacity; // update tail.

    return ref;
  }

  /**
   * Scan the last nscan references for this reference. If found, return immediately.
   *
   * @param nscan The #of positions to scan, starting with the most recently added reference.
   * @param ref The reference for which we are scanning.
   * @return True iff we found <i>ref</i> in the scanned queue positions.
   */
  public boolean scanHead(final int nscan, final T ref) {

    if (nscan <= 0) throw new IllegalArgumentException();

    if (ref == null) throw new IllegalArgumentException();

    /*
     * Note: This loop goes backwards from the head.  Since the head is the
     * insertion point, we decrement the head position before testing the
     * reference.  If the head is zero, then we wrap around.  This carries
     * the head back to the last index in the array (capacity-1).
     *
     * Note: This uses local variables to shadow the instance variables
     * so that we do not modify the state of the cache as a side effect.
     */

    int head = this.head;

    int count = this.size;

    for (int i = 0; i < nscan && count > 0; i++) {

      head = (head == 0 ? capacity - 1 : head - 1); // update head.

      count--; // update #of references.

      if (refs[head] == ref) {

        // Found a match.

        return true;
      }
    }

    return false;
  }

  /**
   * Return true iff the reference is found in the first N positions scanning backwards from the
   * tail of the queue.
   *
   * @param nscan The #of positions to be scanned. When one (1) only the tail of the queue is
   *     scanned.
   * @param ref The reference to scan for.
   * @return True iff the reference is found in the last N queue positions counting backwards from
   *     the tail.
   * @todo Write unit tests for this method.
   */
  public final boolean scanTail(final int nscan, final T ref) {

    if (nscan <= 0) throw new IllegalArgumentException();

    if (ref == null) throw new IllegalArgumentException();

    for (int n = 0, i = tail; n < nscan; n++) {

      if (ref == refs[i]) return true;

      i = (i + 1) % capacity; // update index.
    }

    return false;
  }

  public T remove() throws NoSuchElementException {

    final T ref = poll();

    if (ref == null) throw new NoSuchElementException();

    return ref;
  }

  public boolean contains(final Object ref) {

    if (ref == null) throw new NullPointerException();

    // MRU to LRU scan.
    for (int n = 0, i = tail; n < size; n++) {

      if (ref == refs[i]) return true;

      i = (i + 1) % capacity; // update index.
    }

    return false;
  }

  public boolean containsAll(final Collection<?> c) {

    if (c == null) throw new NullPointerException();

    if (c == this) return true;
    //            throw new IllegalArgumentException();

    for (Object e : c) {

      if (!contains(e)) {

        // something is missing.
        return false;
      }
    }

    return true;
  }

  /**
   * Return an iterator over the buffer visiting elements in LRU to MRU order (the order in which
   * those elements would be read by {@link #poll()}). The iterator supports {@link
   * Iterator#remove()}. The iterator is NOT thread-safe.
   */
  public Iterator<T> iterator() {

    return new MyIterator();
  }

  /**
   * Iterator (not thread-safe).
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  private class MyIterator implements Iterator<T> {

    private int next = 0;
    private int current = -1;

    private MyIterator() {}

    public boolean hasNext() {
      return next < size;
    }

    public T next() {

      if (!hasNext()) throw new NoSuchElementException();

      final T ref = get(next);

      current = next;

      next++;

      return ref;
    }

    public void remove() {

      final int i = current;

      if (current == -1) throw new IllegalStateException();

      current = -1;

      final int tmp = tail;

      RingBuffer.this.remove(i);

      // re-position index into buffer.
      next = (i == tmp) ? tail : i;
    }
  }

  public boolean remove(final Object ref) {

    final Iterator<T> itr = iterator();

    while (itr.hasNext()) {

      if (ref == itr.next()) {

        itr.remove();

        return true;
      }
    }

    return false;
  }

  public boolean removeAll(final Collection<?> c) {

    boolean modified = false;

    final Iterator<?> itr = iterator();

    while (itr.hasNext()) {

      if (c.contains(itr.next())) {

        itr.remove();

        modified = true;
      }
    }

    return modified;
  }

  public boolean retainAll(final Collection<?> c) {

    boolean modified = false;

    final Iterator<T> itr = iterator();

    while (itr.hasNext()) {

      if (!c.contains(itr.next())) {

        itr.remove();

        modified = true;
      }
    }

    return modified;
  }
}
