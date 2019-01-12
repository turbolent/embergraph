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
 * Created on Dec 27, 2007
 */

package org.embergraph.io;

import java.util.Random;
import junit.framework.TestCase2;
import org.embergraph.util.Bytes;
import org.embergraph.util.BytesUtil;

/*
 * Test suite for {@link ByteArrayBuffer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestByteArrayBuffer extends TestCase2 {

  /** */
  public TestByteArrayBuffer() {}

  /** @param arg0 */
  public TestByteArrayBuffer(String arg0) {
    super(arg0);
  }

  /*
   * ctor tests.
   */

  /** ctor tests, including correct rejection. */
  public void test_ctor() {

    {
      final ByteArrayBuffer buf = new ByteArrayBuffer();
      assertNotNull(buf.array());
      assertEquals(0, buf.pos());
      assertEquals(0, buf.limit());
      assertEquals(ByteArrayBuffer.DEFAULT_INITIAL_CAPACITY, buf.array().length);
      assertEquals(ByteArrayBuffer.DEFAULT_INITIAL_CAPACITY, buf.capacity());
    }

    {
      final ByteArrayBuffer buf = new ByteArrayBuffer(0);
      assertNotNull(buf.array());
      assertEquals(0, buf.pos());
      assertEquals(0, buf.limit());
      assertEquals(0, buf.array().length);
      assertEquals(0, buf.capacity());
    }

    {
      final ByteArrayBuffer buf = new ByteArrayBuffer(20);
      assertNotNull(buf.array());
      assertEquals(0, buf.pos());
      assertEquals(0, buf.limit());
      assertEquals(20, buf.array().length);
      assertEquals(20, buf.capacity());
    }
  }

  /** correct rejection tests. */
  public void test_ctor_correctRejection() {

    try {
      new ByteArrayBuffer(-1);
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /*
   * buffer extension tests.
   */

  public void test_ensureCapacity() {

    final ByteArrayBuffer buf = new ByteArrayBuffer(0);

    //        assertEquals(0, buf.len);
    assertNotNull(buf.array());
    assertEquals(0, buf.array().length);

    final byte[] originalBuffer = buf.array();

    // correct rejection.
    try {
      buf.ensureCapacity(-1);
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
    assertTrue(originalBuffer == buf.array()); // same buffer.

    // no change.
    buf.ensureCapacity(0);
    //        assertEquals(0, buf.len);
    assertNotNull(buf.array());
    assertEquals(0, buf.array().length);
    assertTrue(originalBuffer == buf.array()); // same buffer.
  }

  public void test_ensureCapacity02() {

    ByteArrayBuffer buf = new ByteArrayBuffer(0);

    //        assertEquals(0, buf.len);
    assertNotNull(buf.array());
    assertEquals(0, buf.array().length);

    final byte[] originalBuffer = buf.array();

    // extends buffer.
    buf.ensureCapacity(100);
    //        assertEquals(0, buf.len);
    assertNotNull(buf.array());
    assertEquals(100, buf.array().length);
    assertTrue(originalBuffer != buf.array()); // same buffer.
  }

  /** verify that existing data is preserved if the capacity is extended. */
  public void test_ensureCapacity03() {

    final byte[] expected = new byte[20];
    r.nextBytes(expected);

    final ByteArrayBuffer buf = new ByteArrayBuffer(20);

    // copy in random data.
    buf.put(0, expected);

    // verify buffer state.
    {
      assertEquals(
          0,
          BytesUtil.compareBytesWithLenAndOffset(
              0, expected.length, expected, 0, expected.length, buf.array()));

      //        assertEquals(20, buf.len);
      assertNotNull(buf.array());
      assertTrue(expected != buf.array()); // data was copied.
    }

    // extend capacity.
    buf.ensureCapacity(30);

    // verify buffer state.
    {
      //        assertEquals(20, buf.len);
      assertTrue(buf.array().length >= 30);

      assertEquals(
          0,
          BytesUtil.compareBytesWithLenAndOffset(
              0, expected.length, expected, 0, expected.length, buf.array()));

      for (int i = 21; i < 30; i++) {
        assertEquals(0, buf.array()[i]);
      }
    }
  }

  public void test_trim() {

    // note: will be extended.
    final int initialCapacity = Bytes.SIZEOF_INT * 2;

    final ByteArrayBuffer buf = new ByteArrayBuffer(initialCapacity);

    buf.putInt(1);
    buf.putInt(3);
    buf.putInt(5);

    assertEquals(Bytes.SIZEOF_INT * 3, buf.limit());

    final int currentCapacity = buf.capacity();

    assertTrue(currentCapacity > initialCapacity);

    final byte[] old = buf.trim();

    assertEquals(currentCapacity, old.length);

    assertTrue(old != buf.array());

    assertEquals(Bytes.SIZEOF_INT * 3, buf.limit());

    assertEquals(Bytes.SIZEOF_INT * 3, buf.array().length);

    assertEquals(1, buf.getInt(0));
    assertEquals(3, buf.getInt(Bytes.SIZEOF_INT));
    assertEquals(5, buf.getInt(Bytes.SIZEOF_INT * 2));
  }

  /*
   * get/put
   *
   * @todo verify all methods are tested.
   *
   * @todo verify transparent extension for all methods.
   */

  Random r = new Random();

  final int LIMIT = 1000;

  public void test_get_correctRejection() {

    final int capacity = 20;

    final ByteArrayBuffer buf = new ByteArrayBuffer(capacity);

    assertEquals((byte) 0, buf.getByte(0));

    try {
      buf.getByte(-1);
      fail("Expecting: " + IndexOutOfBoundsException.class);
    } catch (IndexOutOfBoundsException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    try {
      buf.getByte(capacity);
      fail("Expecting: " + IndexOutOfBoundsException.class);
    } catch (IndexOutOfBoundsException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    buf.getByte(capacity - 1);
  }

  /** Test bulk get/put byte[] methods. */
  public void test_getPutByteArray() {

    final int capacity = 200;

    final ByteArrayBuffer buf = new ByteArrayBuffer(capacity);

    assertEquals((byte) 0, buf.getByte(0));
    assertEquals((byte) 0, buf.getByte(capacity - 1));

    final int pos = 1;

    for (int i = 0; i < LIMIT; i++) {

      final byte[] expected = new byte[r.nextInt(capacity - 2)];

      r.nextBytes(expected);

      buf.put(pos, expected);

      assertEquals(
          0,
          BytesUtil.compareBytesWithLenAndOffset(
              0, expected.length, expected, pos, expected.length, buf.array()));

      final byte[] actual = new byte[expected.length];

      buf.get(pos, actual);

      assertTrue(BytesUtil.bytesEqual(expected, actual));
    }

    assertEquals((byte) 0, buf.getByte(0));

    assertEquals((byte) 0, buf.getByte(pos + capacity - 2));
  }

  /** Test bulk get/put byte[] methods with offset and length. */
  public void test_getPutByteArrayWithOffsetAndLength() {

    final int capacity = 200;

    final ByteArrayBuffer buf = new ByteArrayBuffer(capacity);

    assertEquals((byte) 0, buf.getByte(0));
    assertEquals((byte) 0, buf.getByte(capacity - 1));

    final int pos = 1;

    for (int i = 0; i < LIMIT; i++) {

      final byte[] expected = new byte[r.nextInt(capacity - 2)];

      final int off = (expected.length / 2 == 0 ? 0 : r.nextInt(expected.length / 2));

      final int len = (expected.length == 0 ? 0 : r.nextInt(expected.length - off));

      r.nextBytes(expected);

      buf.put(pos, expected, off, len);

      assertEquals(
          0, BytesUtil.compareBytesWithLenAndOffset(off, len, expected, pos, len, buf.array()));

      final int dstoff = r.nextInt(10);

      final byte[] actual = new byte[expected.length + dstoff];

      buf.get(pos, actual, dstoff, expected.length);

      assertEquals(
          0, BytesUtil.compareBytesWithLenAndOffset(off, len, expected, dstoff, len, actual));
    }

    assertEquals((byte) 0, buf.getByte(0));

    assertEquals((byte) 0, buf.getByte(pos + capacity - 2));
  }

  public void test_getByte_putByte() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_BYTE * 3);

    final int pos = Bytes.SIZEOF_BYTE;

    assertEquals((byte) 0, buf.getByte(pos));

    final byte[] tmp = new byte[1];

    for (int i = 0; i < LIMIT; i++) {

      r.nextBytes(tmp);

      final byte expected = tmp[0];

      buf.putByte(pos, expected);

      assertEquals(expected, buf.getByte(pos));
    }

    assertEquals((byte) 0, buf.getByte(pos - Bytes.SIZEOF_BYTE));

    assertEquals((byte) 0, buf.getByte(pos + Bytes.SIZEOF_BYTE));
  }

  public void test_getShort_putShort() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_SHORT * 3);

    final int pos = Bytes.SIZEOF_SHORT;

    assertEquals((short) 0, buf.getShort(pos));

    for (int i = 0; i < LIMIT; i++) {

      final short expected = (short) r.nextInt();

      buf.putShort(pos, expected);

      assertEquals(expected, buf.getShort(pos));
    }

    assertEquals((short) 0, buf.getShort(pos - Bytes.SIZEOF_SHORT));

    assertEquals((short) 0, buf.getShort(pos + Bytes.SIZEOF_SHORT));
  }

  public void test_getInt_putInt() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_INT * 3);

    final int pos = Bytes.SIZEOF_INT;

    assertEquals(0, buf.getInt(pos));

    for (int i = 0; i < LIMIT; i++) {

      final int expected = r.nextInt();

      buf.putInt(pos, expected);

      assertEquals(expected, buf.getInt(pos));
    }

    assertEquals(0, buf.getInt(pos - Bytes.SIZEOF_INT));

    assertEquals(0, buf.getInt(pos + Bytes.SIZEOF_INT));
  }

  public void test_getFloat_putFloat() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_FLOAT * 3);

    final int pos = Bytes.SIZEOF_FLOAT;

    assertEquals(0f, buf.getFloat(pos));

    for (int i = 0; i < LIMIT; i++) {

      final float expected = r.nextFloat();

      buf.putFloat(pos, expected);

      assertEquals(expected, buf.getFloat(pos));
    }

    assertEquals(0f, buf.getFloat(pos - Bytes.SIZEOF_FLOAT));

    assertEquals(0f, buf.getFloat(pos + Bytes.SIZEOF_FLOAT));
  }

  public void test_getLong_putLong() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_LONG * 3);

    final int pos = Bytes.SIZEOF_LONG;

    assertEquals(0L, buf.getLong(pos));

    for (int i = 0; i < LIMIT; i++) {

      final long expected = r.nextLong();

      buf.putLong(pos, expected);

      assertEquals(expected, buf.getLong(pos));
    }

    assertEquals(0L, buf.getLong(pos - Bytes.SIZEOF_LONG));

    assertEquals(0L, buf.getLong(pos + Bytes.SIZEOF_LONG));
  }

  public void test_getDouble_putDouble() {

    ByteArrayBuffer buf = new ByteArrayBuffer(Bytes.SIZEOF_DOUBLE * 3);

    final int pos = Bytes.SIZEOF_DOUBLE;

    assertEquals(0d, buf.getDouble(pos));

    for (int i = 0; i < LIMIT; i++) {

      final double expected = r.nextDouble();

      buf.putDouble(pos, expected);

      assertEquals(expected, buf.getDouble(pos));
    }

    assertEquals(0d, buf.getDouble(pos - Bytes.SIZEOF_DOUBLE));

    assertEquals(0d, buf.getDouble(pos + Bytes.SIZEOF_DOUBLE));
  }

  /** Test of reading past the limit on the buffer. */
  public void test_readPastLimit() {

    ByteArrayBuffer buf = new ByteArrayBuffer(2);

    assertEquals(0, buf.pos());
    assertEquals(0, buf.limit());
    assertEquals(0, buf.remaining());
    assertEquals(2, buf.capacity());

    byte expected = (byte) 12;

    buf.putByte(expected);

    assertEquals(1, buf.pos());
    assertEquals(1, buf.limit());
    assertEquals(0, buf.remaining());
    assertEquals(2, buf.capacity());

    // resets the position, but leaves the read limit alone.
    buf.flip();

    assertEquals(0, buf.pos());
    assertEquals(1, buf.limit());
    assertEquals(1, buf.remaining());
    assertEquals(2, buf.capacity());

    // read expected byte @ pos := 0.
    assertEquals(expected, buf.getByte());

    assertEquals(1, buf.pos());
    assertEquals(1, buf.limit());
    assertEquals(0, buf.remaining());
    assertEquals(2, buf.capacity());

    try {
      buf.getByte(expected);
      fail("Expecting exception.");
    } catch (RuntimeException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  public void test_readPastLimit2() {

    final int capacity = 10;

    ByteArrayBuffer buf = new ByteArrayBuffer(capacity);

    for (int i = 0; i < 5; i++) {

      buf.putByte((byte) i);
    }

    // reset the position, leaving the read limit alone.
    buf.flip();

    assert buf.pos() == 0;

    assert buf.limit() == 5;

    for (int i = 0; i < 5; i++) {

      assertEquals(5, buf.limit()); // unchanged.

      assertEquals(i, buf.pos());

      assertEquals((byte) i, buf.getByte());
    }

    assertEquals(5, buf.limit()); // unchanged.
    assertEquals(buf.pos(), buf.limit()); // position is at the read limit.
  }
}
