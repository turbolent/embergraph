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
 * Created on May 3, 2010
 */

package org.embergraph.rdf.internal;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
 * Value Type Enumeration (IVTE) is a class with methods for interpreting and setting the bit flags
 * used to identify the type of an RDF Value (URI, Literal, Blank Node, SID, etc).
 */
public enum VTE {

  /** A URI. */
  URI((byte) 0x00),

  /** A blank node. */
  BNODE((byte) 0x01),

  /** A literal. */
  LITERAL((byte) 0x02),

  /** A statement identifier. */
  STATEMENT((byte) 0x03);

  VTE(final byte flags) {

    this.v = flags;
  }

  /** The code for the value type. */
  final byte v;

  /** Return the code for the value type. */
  public final byte v() {
    return v;
  }

  /*
   * Return the {@link VTE} identified by the LOW TWO (2) bits in the caller's value.
   *
   * @param b The bit flags.
   * @return The corresponding {@link VTE}.
   * @see AbstractIV#getVTE(byte)
   */
  public static VTE valueOf(final byte b) {
    /*
     * Note: Java does not permit the construction URI.v in the cases of the
     * switch (it is not interpreted as a constant). Therefore the switch
     * cases are hard wired to the values specified for each of the 4 RDF
     * Value types above.
     */
    switch (b & 0x03) { // mask off everything but the low 2 bits.
      case 0x00:
        return URI;
      case 0x01:
        return BNODE;
      case 0x02:
        return LITERAL;
      case 0x03:
        return STATEMENT;
      default:
        throw new AssertionError();
    }
  }

  /*
   * Return the one character code for this RDF Value type (U, L, B, or S). This is used in various
   * internal toString() implementations.
   */
  public final char getCharCode() {
    if (v == URI.v) return 'U';
    else if (v == LITERAL.v) return 'L';
    else if (v == BNODE.v) return 'B';
    else if (v == STATEMENT.v) return 'S';
    throw new AssertionError();
  }

  public static VTE valueOf(char c) {
    switch (c) {
      case 'U':
        return URI;
      case 'L':
        return LITERAL;
      case 'B':
        return BNODE;
      case 'S':
        return STATEMENT;
      default:
        throw new IllegalArgumentException();
    }
  }

  /** Determine the VTE based on the type of Value - URI, Literal, or BNode. */
  public static VTE valueOf(final Value v) {
    if (v instanceof URI) return URI;
    if (v instanceof Literal) return LITERAL;
    if (v instanceof BNode) return BNODE;

    throw new IllegalArgumentException();
  }
}
