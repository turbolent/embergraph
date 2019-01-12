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
 * Created on Aug 11, 2009
 */

package org.embergraph.btree.raba;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.embergraph.util.BytesUtil;

/**
 * Abstract base class implements mutation operators and search. A concrete subclass need only
 * indicate if it is mutable, searchable, or allows nulls by overriding the appropriate methods.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractRaba implements IRaba {

  /** The inclusive lower bound of the view. */
  protected final int fromIndex;

  /**
   * The exclusive upper bound of the view.
   *
   * <p>Note: This field is NOT final since it is modified by a subclass which permits mutation.
   */
  protected int toIndex;

  /** The maximum #of elements in the view of the backing array. */
  protected final int capacity;

  /** The backing array. */
  protected final byte[][] a;

  /**
   * Create a view of a byte[][]. All elements in the array are visible in the view.
   *
   * @param a The backing byte[][].
   */
  public AbstractRaba(final byte[][] a) {

    this(0 /* fromIndex */, a.length /* toIndex */, a.length /* capacity */, a);
  }

  /**
   * Create a view from a slice of a byte[][].
   *
   * @param fromIndex The index of the first element in the byte[][] which is visible in the view
   *     (inclusive lower bound).
   * @param toIndex The index of the first element in the byte[][] beyond the view (exclusive upper
   *     bound).
   * @param capacity The #of elements which may be used in the view.
   * @param a The backing byte[][].
   */
  public AbstractRaba(
      final int fromIndex, final int toIndex, final int capacity, final byte[][] a) {

    if (a == null) throw new IllegalArgumentException();

    if (fromIndex < 0) throw new IllegalArgumentException();

    if (fromIndex > toIndex) throw new IllegalArgumentException();

    if (toIndex > a.length) throw new IllegalArgumentException();

    if (capacity < toIndex - fromIndex) throw new IllegalArgumentException();

    this.fromIndex = fromIndex;

    this.toIndex = toIndex;

    this.capacity = capacity;

    this.a = a;
  }

  @Override
  public final int size() {

    return (toIndex - fromIndex);
  }

  @Override
  public final boolean isEmpty() {

    return toIndex == fromIndex;
  }

  @Override
  public final boolean isFull() {

    return size() == capacity();
  }

  @Override
  public final int capacity() {

    return capacity;
  }

  protected final boolean rangeCheck(final int index) throws IndexOutOfBoundsException {

    if (index < 0 || index >= (toIndex - fromIndex)) {

      throw new IndexOutOfBoundsException(
          "index=" + index + ", fromIndex=" + fromIndex + ", toIndex=" + toIndex);
    }

    return true;
  }

  @Override
  public final byte[] get(final int index) {

    assert rangeCheck(index);

    return a[fromIndex + index];
  }

  @Override
  public final int length(final int index) {

    assert rangeCheck(index);

    final byte[] tmp = a[fromIndex + index];

    if (tmp == null) throw new NullPointerException();

    return tmp.length;
  }

  @Override
  public final boolean isNull(final int index) {

    assert rangeCheck(index);

    return a[fromIndex + index] == null;
  }

  @Override
  public final int copy(final int index, final OutputStream out) {

    assert rangeCheck(index);

    final byte[] tmp = a[fromIndex + index];

    if (tmp == null) throw new NullPointerException();

    try {

      out.write(tmp, 0, tmp.length);

    } catch (IOException ex) {

      throw new RuntimeException(ex);
    }

    return tmp.length;
  }

  @Override
  public final Iterator<byte[]> iterator() {

    return new Iterator<byte[]>() {

      private int i = fromIndex;

      @Override
      public boolean hasNext() {

        return i < toIndex;
      }

      @Override
      public byte[] next() {

        if (!hasNext()) throw new NoSuchElementException();

        return a[i++];
      }

      @Override
      public void remove() {

        if (isReadOnly()) throw new UnsupportedOperationException();

        // @todo support remove on the iterator when mutable.
        throw new UnsupportedOperationException();
      }
    };
  }

  /** @throws UnsupportedOperationException if the view is read-only. */
  protected void assertNotReadOnly() {

    if (isReadOnly()) throw new UnsupportedOperationException();
  }

  /** @throws IllegalStateException unless there is room to store another value. */
  protected void assertNotFull() {

    if (toIndex >= fromIndex + a.length) {

      throw new IllegalStateException();
    }
  }

  /**
   * @param a A byte[] to be inserted or set on the {@link IRaba}.
   * @throws IllegalArgumentException if the <code>byte[]</code> is <code>null</code> and the
   *     implementation does not permit <code>null</code>s to be stored.
   */
  protected void assertNullAllowed(final byte[] a) {

    if (a == null && isKeys()) {

      throw new IllegalArgumentException();
    }
  }

  @Override
  public void set(final int index, final byte[] key) {

    assertNotReadOnly();

    assert rangeCheck(index);

    assertNullAllowed(key);

    a[fromIndex + index] = key;
  }

  @Override
  public int add(final byte[] key) {

    assertNotReadOnly();

    assertNotFull();

    assertNullAllowed(key);

    assert toIndex < fromIndex + capacity;

    a[toIndex++] = key;

    return (toIndex - fromIndex);
  }

  @Override
  public int add(final byte[] key, final int off, final int len) {

    assertNotReadOnly();

    assertNotFull();

    assertNullAllowed(key);

    final byte[] b = new byte[len];

    System.arraycopy(key, off, b, 0, len);

    //        for (int i = 0; i < len; i++) {
    //
    //            b[i] = key[off + i];
    //
    //        }

    a[toIndex++] = b;

    return (toIndex - fromIndex);
  }

  @Override
  public int add(final DataInput in, final int len) throws IOException {

    assertNotReadOnly();

    assertNotFull();

    final byte[] b = new byte[len];

    in.readFully(b, 0, len);

    a[toIndex++] = b;

    return (toIndex - fromIndex);
  }

  @Override
  public int search(final byte[] searchKey) {

    if (!isKeys()) {

      throw new UnsupportedOperationException();
    }

    return BytesUtil.binarySearch(a, 0 /* base */, size() /* nmem */, searchKey);
  }

  @Override
  public String toString() {

    return toString(this);
  }

  /**
   * If {@link IRaba#isKeys()} is <code>true</code> then represents the elements as <code>
   * unsigned byte[]</code>s. Otherwise represents the elements as <code>signed byte[]</code>s.
   */
  public static String toString(final IRaba raba) {

    final StringBuilder sb = new StringBuilder();

    final boolean isKeys = raba.isKeys();

    sb.append(raba.getClass().getName());
    sb.append("{ capacity=" + raba.capacity());
    sb.append(", size=" + raba.size());
    sb.append(", isKeys=" + isKeys);
    sb.append(", isReadOnly=" + raba.isReadOnly());
    sb.append(", [\n");

    int i = 0;
    for (byte[] a : raba) {

      if (i > 0) sb.append(",\n");

      if (a == null) {

        sb.append("null");

      } else {

        if (isKeys) {

          // representation as unsigned byte[].
          sb.append(BytesUtil.toString(a));

        } else {

          // representation as signed byte[].
          sb.append(Arrays.toString(a));
        }
      }

      i++;
    }

    sb.append("]}");

    return sb.toString();
  }

  /**
   * Resize the buffer, copying up to <i>n</i> references to the existing data into a new view
   * backed by a new byte[][]. <code>fromIndex</code> will be zero in the new view.
   *
   * <p>This method requires a public constructor with the following signature:
   *
   * <pre>
   * ctor(byte[][])
   * </pre>
   *
   * @param n The size of the new buffer.
   * @return The new view, backed by a new byte[][].
   * @throws IllegalArgumentException if <i>n</i> is negative.
   * @throws RuntimeException if the {@link AbstractRaba} instance does not declare an appropriate
   *     ctor.
   * @todo redefine as slice() (view onto the same data) and copy(int n)?
   */
  public AbstractRaba resize(final int n) {

    assertNotReadOnly();

    if (n < 0) throw new IllegalArgumentException();

    // #of entries in the source.
    final int m = size();

    // #of entries to be copied into the new buffer.
    final int p = Math.min(m, n);

    // new backing array sized to [n].
    final byte[][] b = new byte[n][];

    // copy references to the new buffer.
    System.arraycopy(a, fromIndex, b, 0, p);

    try {

      final Constructor<? extends AbstractRaba> ctor =
          getClass().getConstructor(byte[].class);

      return ctor.newInstance(new Object[] {a});

    } catch (Exception ex) {

      throw new RuntimeException(ex);
    }
  }
}
