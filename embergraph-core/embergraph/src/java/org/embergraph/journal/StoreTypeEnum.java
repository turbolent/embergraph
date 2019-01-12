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
 * Created on Jan 28, 2010
 */

package org.embergraph.journal;

/**
 * The type of store (read/write vs worm).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public enum StoreTypeEnum {

  /**
   * Indicates that the store is a WORM (Write Once, Read Many) aka a journal or a log-structured
   * store.
   */
  WORM((byte) 0),

  /**
   * Indicate that the store is a read/write store. For the read/write store, records are allocated
   * from allocation blocks and can be reused sometime after they have been deleted.
   */
  RW((byte) 1);

  StoreTypeEnum(byte b) {
    this.type = b;
  }

  private final byte type;

  public byte getType() {
    return type;
  }

  public static StoreTypeEnum valueOf(final byte type) {
    switch (type) {
      case 0:
        return WORM;
      case 1:
        return RW;
      default:
        throw new IllegalArgumentException();
    }
  }
}
