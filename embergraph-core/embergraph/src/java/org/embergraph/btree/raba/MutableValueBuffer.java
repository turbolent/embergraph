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
package org.embergraph.btree.raba;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/*
* A flyweight mutable implementation exposing the backing byte[][], permitting <code>null</code>s
 * and not supporting search. It is assumed that caller maintains a dense byte[][] in the sense that
 * all entries in [0:nvalues] are defined, even if some of entries are null. The implementation is
 * NOT thread-safe for mutation.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MutableValueBuffer implements IRaba {

  /** The #of entries with valid data. */
  public int nvalues;

  /** The backing array. */
  public final byte[][] values;

  /** Mutable. */
  public final boolean isReadOnly() {

    return false;
  }

  /** For B+Tree values. */
  @Override
  public final boolean isKeys() {

    return false;
  }

  /*
   * Create a new, empty byte[][] of the specified capacity.
   *
   * @param nvalues The capacity of the byte[][].
   */
  public MutableValueBuffer(final int nvalues) {

    this(0 /* size */, new byte[nvalues][]);
  }

  /*
   * Create a view of a byte[][]. All elements in the array are visible in the view.
   *
   * @param nvalues The #of entries in the array with valid data.
   * @param values The backing byte[][].
   */
  public MutableValueBuffer(final int nvalues, final byte[][] values) {

    if (values == null) throw new IllegalArgumentException();

    if (nvalues < 0 || nvalues >= values.length) throw new IllegalArgumentException();

    this.nvalues = nvalues;

    this.values = values;
  }

  /*
   * Builds a mutable value buffer.
   *
   * @param capacity The capacity of the new instance (this is based on the branching factor for the
   *     B+Tree).
   * @param src The source data.
   * @throws IllegalArgumentException if the capacity is LT the {@link IRaba#size()} of the
   *     <i>src</i>.
   * @throws IllegalArgumentException if the source is <code>null</code>.
   */
  public MutableValueBuffer(final int capacity, final IRaba src) {

    if (src == null) throw new IllegalArgumentException();

    if (capacity < src.capacity()) throw new IllegalArgumentException();

    nvalues = src.size();

    assert nvalues >= 0; // allows deficient root.

    values = new byte[capacity][];

    int i = 0;
    for (byte[] a : src) {

      values[i++] = a;
    }
  }

  @Override
  public final int size() {

    return nvalues;
  }

  @Override
  public final boolean isEmpty() {

    return nvalues == 0;
  }

  @Override
  public final boolean isFull() {

    return nvalues == values.length;
  }

  @Override
  public final int capacity() {

    return values.length;
  }

  protected final boolean rangeCheck(final int index) throws IndexOutOfBoundsException {

    if (index < 0 || index >= nvalues) {

      throw new IndexOutOfBoundsException("index=" + index + ", capacity=" + nvalues);
    }

    return true;
  }

  public final byte[] get(final int index) {

    assert rangeCheck(index);

    return values[index];
  }

  @Override
  public final int length(final int index) {

    assert rangeCheck(index);

    final byte[] tmp = values[index];

    if (tmp == null) throw new NullPointerException();

    return tmp.length;
  }

  @Override
  public final boolean isNull(final int index) {

    assert rangeCheck(index);

    return values[index] == null;
  }

  @Override
  public final int copy(final int index, final OutputStream out) {

    assert rangeCheck(index);

    final byte[] tmp = values[index];

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

      int i = 0;

      @Override
      public boolean hasNext() {

        return i < nvalues;
      }

      @Override
      public byte[] next() {

        if (!hasNext()) throw new NoSuchElementException();

        return values[i++];
      }

      @Override
      public void remove() {

        if (isReadOnly()) throw new UnsupportedOperationException();

        // @todo support remove on the iterator when mutable.
        throw new UnsupportedOperationException();
      }
    };
  }

  /** @throws IllegalStateException unless there is room to store another value. */
  protected void assertNotFull() {

    if (nvalues >= values.length) {

      throw new IllegalStateException();
    }
  }

  @Override
  public void set(final int index, final byte[] key) {

    assert rangeCheck(index);

    values[index] = key;
  }

  @Override
  public int add(final byte[] key) {

    assertNotFull();

    values[nvalues++] = key;

    return nvalues;
  }

  @Override
  public int add(final byte[] key, final int off, final int len) {

    assertNotFull();

    final byte[] b = new byte[len];

    System.arraycopy(key, off, b, 0, len);

    values[nvalues++] = b;

    return nvalues;
  }

  @Override
  public int add(final DataInput in, final int len) throws IOException {

    assertNotFull();

    final byte[] b = new byte[len];

    in.readFully(b, 0, len);

    values[nvalues++] = b;

    return nvalues;
  }

  @Override
  public final int search(final byte[] searchKey) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {

    return AbstractRaba.toString(this);
  }
}
