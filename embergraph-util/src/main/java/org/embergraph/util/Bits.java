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
package org.embergraph.util;

import it.unimi.dsi.bits.BitVector;
import java.nio.ByteBuffer;

/*
 * Simple helper class to work with bits inside a byte. Useful for classes that have a lot of
 * boolean properties or pointers to enums that can be more compactly represented as a series of bit
 * flags. See SPO.
 *
 * @author mikepersonick
 */
public class Bits {

  /*
   * Set a bit inside a byte.
   *
   * @param bits the original byte
   * @param i the bit index (0 through 7)
   * @param bit the bit value
   * @return the new byte
   */
  public static byte set(final byte bits, final int i, final boolean bit) {

    // check to see if bits[i] == bit already, if so, nothing to do
    // also does range check on i
    if (get(bits, i) == bit) return bits;

    byte b = bits;
    if (bit) {
      b = (byte) (b | (0x1 << i));
    } else {
      b = (byte) (b & ~(0x1 << i));
    }
    return b;
  }

  /*
   * Get a bit from inside a byte.
   *
   * @param bits the byte
   * @param i the bit index (0 through 7)
   * @return the bit value
   */
  public static boolean get(final byte bits, final int i) {

    if (i < 0 || i > 7) {
      throw new IndexOutOfBoundsException();
    }

    return (bits & (0x1 << i)) != 0;
  }

  /*
   * Get a new byte, masking off all but the bits specified by m.
   *
   * @param bits the original byte
   * @param m the bits to keep, all others will be masked
   * @return the new byte
   */
  public static byte mask(final byte bits, final int... m) {

    byte b = 0;

    for (int i1 : m) {

      if (i1 < 0 || i1 > 7) {
        throw new IndexOutOfBoundsException();
      }

      b |= (0x1 << i1);
    }

    b &= bits;

    return b;
  }

  /*
   * Useful for debugging.
   *
   * @param bits the byte
   * @return the unsigned binary string representation
   */
  public static String toString(final byte bits) {

    final byte[] d = new byte[] {bits};
    final ByteBuffer b = ByteBuffer.wrap(d);
    final BitVector v = new ByteBufferBitVector(b);
    return v.toString();
  }
}
