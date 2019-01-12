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
 * Created on Oct 28, 2005
 */
package org.embergraph.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import junit.framework.TestCase;

/*
 * Test suite for packing and unpacking unsigned short integers using the {@link DataInputBuffer}
 * and the {@link ByteArrayBuffer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestShortPacker extends TestCase {

  /** */
  public TestShortPacker() {
    super();
  }

  /** @param name */
  public TestShortPacker(String name) {
    super(name);
  }

  /*
   * Unpacks a short value.
   *
   * @param expected The expected long value.
   * @param packed The packed byte[].
   * @throws IOException If there was not enough data.
   * @throws junit.framework.AssertionFailedError If there is too much data.
   */
  public void doUnpackTest(short expected, byte[] packed) throws IOException {

    DataInputBuffer dib = new DataInputBuffer(packed);

    short actual = dib.unpackShort();

    assertEquals("value", expected, actual);

    //        assertTrue( "Expecting EOF", dib.read() == -1 );
    try {
      dib.readByte();
      fail("Expecting: " + IOException.class);
    } catch (IOException ex) {
      System.err.println("Ignoring expected exception: " + ex);
    }
  }

  /*
   * A short in [0:127] is packed into one byte. Larger values are packed into
   * two bytes. The high bit of the first byte is set if the value was packed
   * into two bytes. If the bit is set, clear the high bit, read the next
   * byte, and interpret the two bytes as a short value. Otherwise interpret
   * the byte as a short value.
   */
  public void testUnpack() throws IOException {

    doUnpackTest((short) 0x0, new byte[] {(byte) 0x0});
    doUnpackTest((short) 0x1, new byte[] {(byte) 0x1});
    doUnpackTest((short) 0x7, new byte[] {(byte) 0x7});
    doUnpackTest((short) 0xf, new byte[] {(byte) 0xf});

    doUnpackTest((short) 0x70, new byte[] {(byte) 0x70});
    doUnpackTest((short) 0x7f, new byte[] {(byte) 0x7f});

    doUnpackTest((short) 0x80, new byte[] {(byte) 0x80, (byte) 0x80});
    doUnpackTest((short) 0x8e, new byte[] {(byte) 0x80, (byte) 0x8e});
  }

  public void doPackTest(short v, byte[] expected) throws IOException {

    DataOutputBuffer dob = new DataOutputBuffer();

    final int nbytes = dob.packShort(v);

    final byte[] actual = dob.toByteArray();

    assertEquals("nbytes", expected.length, nbytes);
    assertEquals("bytes", expected, actual);
  }

  public void testPack() throws IOException {

    // [0:127] should be packed into one byte.
    doPackTest((short) 0x00, new byte[] {(byte) 0x0});
    doPackTest((short) 0x01, new byte[] {(byte) 0x1});
    doPackTest((short) 0x02, new byte[] {(byte) 0x2});
    doPackTest((short) 0x0e, new byte[] {(byte) 0xe});
    doPackTest((short) 0x0f, new byte[] {(byte) 0xf});
    doPackTest((short) 0x10, new byte[] {(byte) 0x10});
    doPackTest((short) 0x11, new byte[] {(byte) 0x11});
    doPackTest((short) 0x1f, new byte[] {(byte) 0x1f});
    doPackTest((short) 0x20, new byte[] {(byte) 0x20});
    doPackTest((short) 0x70, new byte[] {(byte) 0x70});
    doPackTest((short) 0x7f, new byte[] {(byte) 0x7f});

    // high nibble of the short value is zero.
    doPackTest((short) 0x80, new byte[] {(byte) 0x80, (byte) 0x80});
    doPackTest((short) 0xff, new byte[] {(byte) 0x80, (byte) 0xff});
    doPackTest((short) 0x100, new byte[] {(byte) 0x81, (byte) 0x00});
    doPackTest((short) 0x101, new byte[] {(byte) 0x81, (byte) 0x01});
    doPackTest((short) 0x121, new byte[] {(byte) 0x81, (byte) 0x21});
    doPackTest((short) 0x1ee, new byte[] {(byte) 0x81, (byte) 0xee});
    doPackTest((short) 0x1ff, new byte[] {(byte) 0x81, (byte) 0xff});
    doPackTest((short) 0xfff, new byte[] {(byte) 0x8f, (byte) 0xff});
  }

  public void test_rejectNegatives() throws IOException {

    try {
      doPackTest((short) 0x8fff, new byte[] {});
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      System.err.println("Ignoring expected exception: " + ex);
    }

    try {
      doPackTest((short) 0xffff, new byte[] {});
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      System.err.println("Ignoring expected exception: " + ex);
    }
  }

  public static final long SIGN_MASK = 11 << 15;

  public void testHighBit() {
    assertTrue("sign bit", ((short) -1 & SIGN_MASK) != 0);
    assertFalse("sign bit", ((short) 0 & SIGN_MASK) != 0);
  }

  private interface ShortGenerator {
    short nextShort();
  }

  /*
   * All long values in sequence starting from the given start value and using the given increment.
   *
   * @author thompsonbry
   */
  private static class Sequence implements ShortGenerator {

    short _start, _inc, _next;

    public Sequence(short start, short inc) {
      _start = start;
      _inc = inc;
      _next = start;
    }

    public short nextShort() {
      if (_next == Short.MAX_VALUE) {
        throw new RuntimeException("No more short values.");
      }
      short v = _next;
      _next += _inc;
      return v;
    }
  }

  /*
   * Random short values (16 bits of random short), including negatives, with a uniform
   * distribution.
   *
   * @author thompsonbry
   */
  private static class RandomShort implements ShortGenerator {

    Random _rnd;

    public RandomShort(Random rnd) {
      _rnd = rnd;
    }

    public short nextShort() {
      return (short) _rnd.nextInt(); // truncate.
    }
  }

  /*
   * Run a large #of pack/unpack operations on a sequence of short values to demonstrate correctness
   * in that sequence. The sequence is the short values from -1 to {@link Short#MAX_VALUE} by one
   * (dense coverage).
   *
   * @throws IOException
   */
  public void testStressSequence() throws IOException {

    // dense coverage of the first 1M values.
    doStressTest(Short.MAX_VALUE + 1, new Sequence((short) -1, (short) 1));
  }

  /*
   * Run a large #of random pack/unpack operations to sample the space while showing correctness on
   * those samples.
   *
   * @throws IOException
   */
  public void testStressRandom() throws IOException {

    // test on random long values.
    doStressTest(0xffff, new RandomShort(new Random()));
  }

  /*
   * Returns the #of bytes into which a short value was packed based on the first byte.
   *
   * @param firstByte The first byte.
   * @return The #of bytes (either one (1) or two (2)).
   */
  static int getNBytes(byte firstByte) {

    if ((firstByte & 0x80) != 0) {

      return 2;

    } else {

      return 1;
    }
  }

  /*
   * Run a stress test. Writes some information of possible interest onto System.err.
   *
   * @param ntrials #of trials.
   * @param g Generator for the long values.
   * @throws IOException
   */
  public void doStressTest(int ntrials, ShortGenerator g) throws IOException {

    long nwritten = 0L;

    long packlen = 0L;

    long minv = Short.MAX_VALUE, maxv = Short.MIN_VALUE;

    for (int i = 0; i < ntrials; i++) {

      short expected = g.nextShort();

      if (expected < 0L) {

        DataOutputBuffer dos = new DataOutputBuffer();

        try {

          dos.packShort(expected);

          fail("Expecting rejection of negative value: val=" + expected);

        } catch (IllegalArgumentException ex) {

          //                    System.err.println( "Ingoring expected exception: "+ex );

        }

      } else {

        if (expected > maxv) maxv = expected;
        if (expected < minv) minv = expected;

        DataOutputBuffer dos = new DataOutputBuffer();

        int nbytesActual = dos.packShort(expected);

        byte[] packed = dos.toByteArray();

        final int nbytesExpected = getNBytes(packed[0]);

        DataInputBuffer dis = new DataInputBuffer(packed);

        final short actual = dis.unpackShort();

        assertEquals("trial=" + i, expected, actual);

        assertEquals("trial=" + i + ", v=" + expected + ", nbytes", nbytesExpected, nbytesActual);

        assertEquals("trial=" + i + ", v=" + expected + ", nbytes", nbytesExpected, packed.length);

        packlen += packed.length; // total #of packed bytes.
        nwritten++; // count #of non-negative random values.
      }
    }

    System.err.println("\nWrote " + nwritten + " non-negative long values.");
    System.err.println("minv=" + minv + ", maxv=" + maxv);
    System.err.println("#packed bytes       =" + packlen);
    System.err.println("#bytes if not packed=" + (nwritten * 8));
    long nsaved = (nwritten * 8) - packlen;
    System.err.println("#bytes saved        =" + nsaved);
    System.err.println("%saved by packing   =" + nsaved / (nwritten * 8f) * 100 + "%");
  }

  public static void assertEquals(String msg, byte[] expected, byte[] actual) {
    assertEquals(msg + ": length", expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(msg + ": byte[i=" + i + "]", expected[i], actual[i]);
    }
  }

  /*
   * This test packs the data using the {@link ShortPacker} and unpacks it using a {@link
   * DataInputBuffer}.
   */
  public void test_compatiblity_ShortPacker_DataInputBuffer() throws IOException {

    final int limit = 10000;

    Random r = new Random();

    short[] expected = new short[limit];

    /*
     * Pack a sequence of random non-negative short integers.
     */
    final byte[] serialized;
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(limit * 2);

      DataOutputStream os = new DataOutputStream(baos);

      for (int i = 0; i < limit; i++) {

        /*
         * Random non-negative short integer. 1% of the time we will use
         * 0 instead.
         */

        final short v = (short) ((r.nextInt(100) < 1) ? 0 : r.nextInt(Short.MAX_VALUE));

        assert v >= 0;

        ShortPacker.packShort(os, v);

        expected[i] = v;
      }

      os.flush();

      serialized = baos.toByteArray();
    }

    /*
     * Deserialize, checking the unpacked values.
     */
    {
      DataInputBuffer in = new DataInputBuffer(serialized);

      for (int i = 0; i < limit; i++) {

        short v = in.unpackShort();

        if (v != expected[i]) {

          assertEquals("index=" + i, expected[i], v);
        }
      }
    }
  }

  /*
   * This test packs the data using a {@link DataOutputBuffer} and unpacks it using the {@link
   * ShortPacker}.
   */
  public void test_compatiblity_DataOutputBuffer_ShortPacker() throws IOException {

    final int limit = 10000;

    Random r = new Random();

    short[] expected = new short[limit];

    /*
     * Pack a sequence of random non-negative short integers.
     */
    final byte[] serialized;
    {
      DataOutputBuffer os = new DataOutputBuffer();

      for (int i = 0; i < limit; i++) {

        /*
         * Random non-negative short integer. 1% of the time we will use
         * 0L instead.
         */

        final short v = (short) ((r.nextInt(100) < 1) ? 0L : r.nextInt(Short.MAX_VALUE));

        assert v >= 0;

        os.packShort(v);

        expected[i] = v;
      }

      serialized = os.toByteArray();
    }

    /*
     * Deserialize, checking the unpacked values.
     */
    {
      ByteArrayInputStream bais = new ByteArrayInputStream(serialized);

      DataInput in = new DataInputStream(bais);

      for (int i = 0; i < limit; i++) {

        short v = ShortPacker.unpackShort(in);

        if (v != expected[i]) {

          assertEquals("index=" + i, expected[i], v);
        }
      }
    }
  }
}
