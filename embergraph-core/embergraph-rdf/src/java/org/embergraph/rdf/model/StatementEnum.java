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
 * Created on Oct 19, 2007
 */

package org.embergraph.rdf.model;

/**
 * The basic statement types are: axioms, explicit, inferred.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public enum StatementEnum {

  /** A statement that was inserted into the database explicitly by the application. */
  Explicit((byte) 0),

  /** Something that is directly entailed by the appropriate model theory. */
  Axiom((byte) 1),

  /** A statement that was inferred from the explicit statements by the appropriate model theory. */
  Inferred((byte) 2),

  /**
   * An explicit statement that was deleted but is being maintained in the statement indices for
   * history.
   */
  History((byte) 3);

  private final byte code;

  private StatementEnum(final byte code) {

    this.code = code;
  }

  public byte code() {

    return code;
  }

  /**
   * Max returns the value that is first in the total order
   *
   * <ul>
   *   <li>Explicit
   *   <li>Axiom
   *   <li>Inferred
   *   <li>Deleted
   * </ul>
   *
   * @param a
   * @param b
   * @return
   */
  public static StatementEnum max(final StatementEnum a, final StatementEnum b) {

    if (a.code < b.code) {

      return a;

    } else {

      return b;
    }
  }

  /**
   * Decode a byte into a {@link StatementEnum}.
   *
   * <p>Note: The override bit is masked off during this operation.
   *
   * @param b The byte.
   * @return The {@link StatementEnum} value.
   */
  public static StatementEnum decode(final byte b) {

    switch (b & ~MASK_OVERRIDE & ~MASK_USER_FLAG) {
      case 0:
        return Explicit;

      case 1:
        return Axiom;

      case 2:
        return Inferred;

      case 3:
        return History;

      default:
        throw new RuntimeException("Unexpected byte: " + b);
    }
  }

  //    static public StatementEnum deserialize(DataInputBuffer in) {
  //
  //        try {
  //
  //            return decode(in.readByte());
  //
  //        } catch(IOException ex) {
  //
  //            throw new UnsupportedOperationException();
  //
  //        }
  //
  //    }

  public static StatementEnum deserialize(final byte[] val) {

    if (val.length != 1) {

      throw new RuntimeException("Expecting one byte, not " + val.length);
    }

    return decode(val[0]);
  }

  public byte[] serialize() {

    return new byte[] {code};
  }

  /**
   * A bit mask used to isolate the bit that indicates that the existing statement type should be
   * overridden thereby allowing the downgrade of a statement from explicit to inferred.
   */
  public static final int MASK_OVERRIDE = 0x1 << 3;

  /** A user bit mask used by applications to flag statements. */
  public static final int MASK_USER_FLAG = 0x1 << 2;

  /**
   * Return <code>true</code> iff the user bit is set.
   *
   * @param b The byte.
   */
  public static boolean isUserFlag(final byte b) {

    return (b & StatementEnum.MASK_USER_FLAG) != 0;
  }

  /**
   * Return <code>true</code> iff the override bit is set.
   *
   * @param b The byte.
   */
  public static boolean isOverride(final byte b) {

    return (b & StatementEnum.MASK_OVERRIDE) != 0;
  }
}
