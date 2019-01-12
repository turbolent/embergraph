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
 * Created on Aug 26, 2009
 */

package org.embergraph.btree.raba.codec;

import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Iterator;
import org.embergraph.btree.raba.AbstractRaba;
import org.embergraph.btree.raba.IRaba;
import org.embergraph.io.AbstractFixedByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;

/*
* Useful when a B+Tree uses keys but not values. The coder maintains the {@link IRaba#size()}, but
 * any <code>byte[]</code> values stored under the B+Tree will be <strong>discarded</strong> by this
 * {@link IRabaCoder}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class EmptyRabaValueCoder implements IRabaCoder, Externalizable {

  /** */
  private static final long serialVersionUID = -8011456562258609162L;

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    // NOP

  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {

    // NOP

  }

  public static final transient EmptyRabaValueCoder INSTANCE = new EmptyRabaValueCoder();

  public EmptyRabaValueCoder() {}

  /** No. Keys can not be constrained to be empty. */
  @Override
  public final boolean isKeyCoder() {

    return false;
  }

  /** Yes. */
  @Override
  public final boolean isValueCoder() {

    return true;
  }

  @Override
  public boolean isDuplicateKeys() {

    return false;
  }

  @Override
  public ICodedRaba encodeLive(final IRaba raba, final DataOutputBuffer buf) {

    if (raba == null) throw new IllegalArgumentException();

    if (raba.isKeys()) {

      // not allowed for B+Tree keys.
      throw new UnsupportedOperationException();
    }

    final int O_origin = buf.pos();

    final int size = raba.size();

    buf.putInt(size);

    return new EmptyCodedRaba(buf.slice(O_origin, buf.pos() - O_origin), size);
  }

  /*
   * <strong>Any data in the {@link IRaba} will be discarded!</strong> Only the {@link IRaba#size()}
   * is maintained.
   */
  @Override
  public AbstractFixedByteArrayBuffer encode(final IRaba raba, final DataOutputBuffer buf) {

    if (raba == null) throw new IllegalArgumentException();

    if (raba.isKeys()) {

      // not allowed for B+Tree keys.
      throw new UnsupportedOperationException();
    }

    final int O_origin = buf.pos();

    buf.putInt(raba.size());

    return buf.slice(O_origin, buf.pos() - O_origin);
  }

  @Override
  public ICodedRaba decode(final AbstractFixedByteArrayBuffer data) {

    return new EmptyCodedRaba(data);
  }

  /*
   * An {@link ICodedRaba} for use when the encoded logical byte[][] was empty.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  private static class EmptyCodedRaba implements ICodedRaba {

    private final AbstractFixedByteArrayBuffer data;

    private final int size;

    public EmptyCodedRaba(final AbstractFixedByteArrayBuffer data) {

      if (data == null) throw new IllegalArgumentException();

      this.data = data;

      size = data.getInt(0);
    }

    public EmptyCodedRaba(final AbstractFixedByteArrayBuffer data, final int size) {

      if (data == null) throw new IllegalArgumentException();

      this.data = data;

      this.size = size;
    }

    @Override
    public final AbstractFixedByteArrayBuffer data() {

      return data;
    }

    /** Yes. */
    @Override
    public final boolean isReadOnly() {

      return true;
    }

    @Override
    public boolean isKeys() {

      return false;
    }

    @Override
    public final int capacity() {

      return size;
    }

    @Override
    public final int size() {

      return size;
    }

    @Override
    public final boolean isEmpty() {

      return size == 0;
    }

    @Override
    public final boolean isFull() {

      return true;
    }

    @Override
    public final boolean isNull(final int index) {

      if (index < 0 || index >= size) throw new IndexOutOfBoundsException();

      return true;
    }

    @Override
    public final int length(final int index) {

      if (index < 0 || index >= size) throw new IndexOutOfBoundsException();

      throw new NullPointerException();
    }

    @Override
    public final byte[] get(final int index) {

      if (index < 0 || index >= size) throw new IndexOutOfBoundsException();

      return null;
    }

    @Override
    public final int copy(final int index, final OutputStream os) {

      if (index < 0 || index >= size) throw new IndexOutOfBoundsException();

      throw new NullPointerException();
    }

    @Override
    public final Iterator<byte[]> iterator() {

      return new Iterator<byte[]>() {

        int i = 0;

        @Override
        public boolean hasNext() {

          return i < size;
        }

        @Override
        public byte[] next() {

          i++;

          return null;
        }

        @Override
        public void remove() {

          throw new UnsupportedOperationException();
        }
      };
    }

    /*
     * If the {@link IRaba} represents B+Tree keys then returns <code>-1</code> as the insertion
     * point.
     *
     * @throws UnsupportedOperationException unless the {@link IRaba} represents B+Tree keys.
     */
    @Override
    public final int search(final byte[] searchKey) {

      if (isKeys()) return -1;

      throw new UnsupportedOperationException();
    }

    /*
     * Mutation API is not supported.
     */

    @Override
    public final int add(byte[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final int add(byte[] value, int off, int len) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final int add(DataInput in, int len) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    public final void set(int index, byte[] a) {
      throw new UnsupportedOperationException();
    }

    @Override
    public final String toString() {

      return AbstractRaba.toString(this);
    }
  }
}
