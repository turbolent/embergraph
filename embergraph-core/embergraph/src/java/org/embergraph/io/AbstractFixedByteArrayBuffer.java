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
 * Created on Dec 23, 2007
 */

package org.embergraph.io;

import it.unimi.dsi.io.InputBitStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.embergraph.util.BytesUtil;

/*
 * Efficient absolute get/put operations on a slice of a byte[]. This class is not thread-safe under
 * mutation because the operations are not atomic. Concurrent operations on the same region of the
 * slice can reveal partial updates. This class is abstract. A concrete implementation need only
 * implement {@link #array()} and an appropriate constructor. This allows for use cases where the
 * backing byte[] is extensible. E.g., a fixed slice onto an extensible {@link ByteArrayBuffer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractFixedByteArrayBuffer implements IFixedDataRecord {

  //    protected static final Logger log = Logger.getLogger(AbstractFixedByteArrayBuffer.class);
  //
  //    protected static final boolean INFO = log.isInfoEnabled();

  /** The start of the slice in the {@link #array()}. */
  private final int off;

  /** The length of the slice in the {@link #array()}. */
  private final int len;

  public final int off() {

    return off;
  }

  public final int len() {

    return len;
  }

  /*
   * A slice wrapping the entire array.
   *
   * @param array The array.
   */
  public static FixedByteArrayBuffer wrap(final byte[] array) {

    return new FixedByteArrayBuffer(array, 0 /* off */, array.length /* len */);
  }

  /*
   * Protected constructor used to create a slice. The caller is responsible for verifying that the
   * slice is valid for the backing byte[] buffer.
   *
   * @param off The offset of the start of the slice.
   * @param len The length of the slice.
   */
  protected AbstractFixedByteArrayBuffer(final int off, final int len) {

    if (off < 0) throw new IllegalArgumentException("off<0");

    if (len < 0) throw new IllegalArgumentException("len<0");

    this.off = off;

    this.len = len;
  }

  /*
   * Absolute get/put operations.
   */

  /*
   * Verify that an operation starting at the specified offset into the slice and having the
   * specified length is valid against the slice.
   *
   * @param aoff The offset into the slice.
   * @param alen The #of bytes to be addressed starting from that offset.
   * @return <code>true</code>.
   * @throws IllegalArgumentException if the operation is not valid.
   */
  protected boolean rangeCheck(final int aoff, final int alen) {

    if (aoff < 0) throw new IndexOutOfBoundsException();

    if (alen < 0) throw new IndexOutOfBoundsException();

    if ((aoff + alen) > len) {

      /*
       * The operation run length at that offset would extend beyond the
       * end of the slice.
       */

      throw new IndexOutOfBoundsException();
    }

    return true;
  }

  public final void put(final int pos, final byte[] b) {

    put(pos, b, 0, b.length);
  }

  public final void put(final int dstoff, final byte[] src, final int srcoff, final int srclen) {

    assert rangeCheck(dstoff, srclen);

    System.arraycopy(src, srcoff, array(), off + dstoff, srclen);
  }

  public final void get(final int srcoff, final byte[] dst) {

    get(srcoff, dst, 0 /* dstoff */, dst.length);
  }

  public final void get(final int srcoff, final byte[] dst, final int dstoff, final int dstlen) {

    assert rangeCheck(srcoff, dstlen);

    System.arraycopy(array(), off + srcoff, dst, dstoff, dstlen);
  }

  public final void putByte(final int pos, final byte v) {

    assert rangeCheck(pos, 1);

    // adjust by the offset.
    array()[off + pos] = v;
  }

  public final byte getByte(final int pos) {

    assert rangeCheck(pos, 1);

    // adjust by the offset.
    return array()[off + pos];
  }

  public final void putShort(int pos, final short v) {

    assert rangeCheck(pos, 2);

    // adjust by the offset.
    pos += off;

    // big-endian
    array()[pos++] = (byte) (v >>> 8);
    array()[pos] = (byte) (v >>> 0);
  }

  public final short getShort(int pos) {

    assert rangeCheck(pos, 2);

    // adjust by the offset.
    pos += off;

    short v = 0;

    // big-endian.
    v += (0xff & array()[pos++]) << 8;
    v += (0xff & array()[pos]) << 0;

    return v;
  }

  public final void putInt(int pos, final int v) {

    assert rangeCheck(pos, 4);

    // adjust by the offset.
    pos += off;

    array()[pos++] = (byte) (v >>> 24);
    array()[pos++] = (byte) (v >>> 16);
    array()[pos++] = (byte) (v >>> 8);
    array()[pos] = (byte) (v >>> 0);
  }

  public final int getInt(int pos) {

    assert rangeCheck(pos, 4);

    // adjust by the offset.
    pos += off;

    int v = 0;

    // big-endian.
    v += (0xff & array()[pos++]) << 24;
    v += (0xff & array()[pos++]) << 16;
    v += (0xff & array()[pos++]) << 8;
    v += (0xff & array()[pos]) << 0;

    return v;
  }

  public final void putFloat(final int pos, final float f) {

    putInt(pos, Float.floatToIntBits(f));
  }

  public final float getFloat(final int pos) {

    return Float.intBitsToFloat(getInt(pos));
  }

  public final void putLong(int pos, final long v) {

    assert rangeCheck(pos, 8);

    // adjust by the offset.
    pos += off;

    // big-endian.
    array()[pos++] = (byte) (v >>> 56);
    array()[pos++] = (byte) (v >>> 48);
    array()[pos++] = (byte) (v >>> 40);
    array()[pos++] = (byte) (v >>> 32);
    array()[pos++] = (byte) (v >>> 24);
    array()[pos++] = (byte) (v >>> 16);
    array()[pos++] = (byte) (v >>> 8);
    array()[pos] = (byte) (v >>> 0);
  }

  public final void putDouble(final int pos, final double d) {

    putLong(pos, Double.doubleToLongBits(d));
  }

  public final long getLong(int pos) {

    assert rangeCheck(pos, 8);

    // adjust by the offset.
    pos += off;

    long v = 0L;

    // big-endian.
    v += (0xffL & array()[pos++]) << 56;
    v += (0xffL & array()[pos++]) << 48;
    v += (0xffL & array()[pos++]) << 40;
    v += (0xffL & array()[pos++]) << 32;
    v += (0xffL & array()[pos++]) << 24;
    v += (0xffL & array()[pos++]) << 16;
    v += (0xffL & array()[pos++]) << 8;
    v += (0xffL & array()[pos]) << 0;

    return v;
  }

  public final double getDouble(final int pos) {

    return Double.longBitsToDouble(getLong(pos));
  }

  public final boolean getBit(final long bitIndex) {

    assert rangeCheck(BytesUtil.byteIndexForBit(bitIndex), 1);

    // convert off() to a bit offset and then address the bit at the
    // caller's index
    return BytesUtil.getBit(array(), (off << 3) + bitIndex);
  }

  public final boolean setBit(final long bitIndex, final boolean value) {

    assert rangeCheck(BytesUtil.byteIndexForBit(bitIndex), 1);

    // convert off() to a bit offset and then address the bit at the
    // caller's index
    return BytesUtil.setBit(array(), (off << 3) + bitIndex, value);
  }

  public final byte[] toByteArray() {

    final byte[] tmp = new byte[len];

    System.arraycopy(array(), off /* srcPos */, tmp /* dst */, 0 /* destPos */, len);

    return tmp;
  }

  public final ByteBuffer asByteBuffer() {

    return ByteBuffer.wrap(array(), off, len);
  }

  public AbstractFixedByteArrayBuffer slice(final int aoff, final int alen) {

    assert rangeCheck(aoff, alen);

    return new AbstractFixedByteArrayBuffer(off() + aoff, alen) {

      public byte[] array() {

        return AbstractFixedByteArrayBuffer.this.array();
      }
    };
  }

  /*
   * IFixedDataRecord
   */

  public DataInputBuffer getDataInput() {

    return new DataInputBuffer(array(), off, len);
  }

  public InputBitStream getInputBitStream() {

    //        /*
    //         * We have to double-wrap the buffer to ensure that it reads from just
    //         * the slice since InputBitStream does not have a constructor which
    //         * accepts a slice of the form (byte[], off, len). [It would be nice if
    //         * InputBitStream handled the slice natively since should be faster per
    //         * its own javadoc.]
    //         *
    //         * Note: The reflection test semantics are not quite what I would want.
    //         * If you specify [false] then the code does not even test for the
    //         * RepositionableStream interface. Ideally, it would always do that but
    //         * skip the reflection on the getChannel() method when it was false.
    //         */
    //        return new InputBitStream(getDataInput(), 0/* unbuffered */, true/* reflectionTest
    // */);

    /*
     * This directly wraps the slice.  This is much faster.
     */
    return new InputBitStream(array(), off, len);
  }

  public final void writeOn(final OutputStream os) throws IOException {

    os.write(array(), off, len);
  }

  public final void writeOn(final DataOutput out) throws IOException {

    out.write(array(), off, len);
  }

  public final void writeOn(final OutputStream os, final int aoff, final int alen)
      throws IOException {

    if (aoff < 0) // check starting pos.
    throw new IllegalArgumentException();

    if (aoff + alen > this.len) // check run length.
    throw new IllegalArgumentException();

    os.write(array(), off + aoff, alen);
  }
}
