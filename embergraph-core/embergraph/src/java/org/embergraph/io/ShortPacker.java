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
 * Created on Oct 24, 2005
 */
package org.embergraph.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/*
 * Packing utility for non-negative <code>short</code> values.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class ShortPacker {

  public ShortPacker() {
    super();
  }

  /*
   * Packs a non-negative short value into one or two bytes and writes them on <i>os </i>. A short
   * in [0:127] is packed into one byte. Larger values are packed into two bytes. The high bit of
   * the first byte is set if the value was packed into two bytes. If the bit is set, clear the high
   * bit, read the next byte, and interpret the two bytes as a short value. Otherwise interpret the
   * byte as a short value.
   *
   * @return The #of bytes into which the value was packed.
   */
  public static int packShort(final DataOutput os, final short v) throws IOException {

    /*
     * You can only pack non-negative values with this method.
     */
    if (v < 0) {
      throw new IllegalArgumentException("negative value: v=" + v);
    }
    if (v > 127) {
      // the value requires two bytes.
      os.write((byte) ((0xff & (v >> 8)) | 0x80)); // note: set the high bit.
      os.write((byte) (0xff & v));
      return 2;
    } else {
      // the value fits in one byte.
      os.write((byte) (0xff & v));
      return 1;
    }
  }

  /*
   * Unpack a non-negative short value from the input stream.
   *
   * @param is The input stream.
   * @return The short value.
   * @throws IOException
   */
  public static short unpackShort(final DataInput is) throws IOException {
    short b = (short) is.readByte();
    short v;
    if ((b & 0x80) != 0) {
      // high bit is set.
      v = (short) ((b & 0x7f) << 8); // clear the high bit and shift over one byte.
      b = is.readByte(); // read the next byte.
      v |= (b & 0xff); // and combine it together with the high byte.
    } else {
      // high bit is clear.
      v = b; // interpret the byte as a short value.
    }
    return v;
  }

  /*
   * Unpack a non-negative short value from the input stream.
   *
   * @param is The input stream.
   * @return The short value.
   * @throws IOException
   */
  public static short unpackShort(final InputStream is) throws IOException {
    short b = (short) readByte(is);
    short v;
    if ((b & 0x80) != 0) {
      // high bit is set.
      v = (short) ((b & 0x7f) << 8); // clear the high bit and shift over one byte.
      b = readByte(is); // read the next byte.
      v |= (b & 0xff); // and combine it together with the high byte.
    } else {
      // high bit is clear.
      v = b; // interpret the byte as a short value.
    }
    return v;
  }

  /*
   * Read a byte from an {@link InputStream} ala {@link DataInput#readByte()}
   *
   * @param is The input stream.
   * @return The byte.
   * @throws IOException
   * @throws EOFException if the end of the file has been reached.
   */
  private static byte readByte(final InputStream is) throws IOException {

    final int v = is.read();

    if (v == -1) throw new EOFException();

    return (byte) (v & 0xff);
  }

  /*
   * Returns the #of bytes into which a short value was packed based on the first byte.
   *
   * @param firstByte The first byte.
   * @return The #of bytes (either one (1) or two (2)).
   */
  public static int getNBytes(byte firstByte) {
    if ((firstByte & 0x80) != 0) {
      return 2;
    } else {
      return 1;
    }
  }
}
