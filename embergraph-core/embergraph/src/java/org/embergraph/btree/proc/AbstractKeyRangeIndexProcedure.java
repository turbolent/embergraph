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
 * Created on Jan 28, 2008
 */

package org.embergraph.btree.proc;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.embergraph.mdi.ISeparatorKeys;
import org.embergraph.util.BytesUtil;

/** @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a> */
public abstract class AbstractKeyRangeIndexProcedure<T> extends AbstractIndexProcedure<T>
    implements IKeyRangeIndexProcedure<T>, Externalizable {

  protected byte[] fromKey;

  protected byte[] toKey;

  @Override
  public byte[] getFromKey() {

    return fromKey;
  }

  @Override
  public byte[] getToKey() {

    return toKey;
  }

  /** De-serialization ctor. */
  public AbstractKeyRangeIndexProcedure() {}

  public AbstractKeyRangeIndexProcedure(final byte[] fromKey, final byte[] toKey) {

    this.fromKey = fromKey;

    this.toKey = toKey;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {

    readKeys(in);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {

    writeKeys(out);
  }

  private static final short VERSION0 = 0x0;

  protected void readKeys(final ObjectInput in) throws IOException, ClassNotFoundException {

    final short version = in.readShort();

    if (version != VERSION0) {

      throw new IOException("Unknown version: " + version);
    }

    // fromKey
    {
      final int len = in.readInt();

      if (len > 0) {

        fromKey = new byte[len - 1];

        in.readFully(fromKey);
      }
    }

    // toKey
    {
      final int len = in.readInt();

      if (len > 0) {

        toKey = new byte[len - 1];

        in.readFully(toKey);
      }
    }
  }

  protected void writeKeys(final ObjectOutput out) throws IOException {

    out.writeShort(VERSION0);

    /*
     * Note: 0 indicates a null reference. Otherwise the length of the
     * byte[] is written as (len + 1).
     */

    out.writeInt(fromKey == null ? 0 : fromKey.length + 1);

    if (fromKey != null) {

      out.write(fromKey);
    }

    out.writeInt(toKey == null ? 0 : toKey.length + 1);

    if (toKey != null) {

      out.write(toKey);
    }
  }

  /*
   * Constrain the fromKey to lie within the index partition.
   *
   * @param fromKey The fromKey.
   * @param pmd The index partition metadata (MAY be null).
   * @return The <i>fromKey</i> -or- the leftSeparator key of the index partition IFF <i>pmd</i> is
   *     non-<code>null</code> AND the <i>fromKey</i> is LT the leftSeparator key.
   */
  public static byte[] constrainFromKey(byte[] fromKey, final ISeparatorKeys pmd) {

    if (pmd == null) return fromKey;

    if (fromKey != null) {

      /*
       * Choose the left separator key if the fromKey is strictly less
       * than the left separator. This has the effect of constraining the
       * fromKey to be within the index partition key range.
       */

      final int ret = BytesUtil.compareBytes(fromKey, pmd.getLeftSeparatorKey());

      if (ret < 0) {

        fromKey = pmd.getLeftSeparatorKey();
      }

    } else {

      /*
       * There is no lower bound, so accept the lower bound of the index
       * partition.
       */

      fromKey = pmd.getLeftSeparatorKey();
    }

    return fromKey;
  }

  /*
   * Constrain the toKey to lie within the index partition.
   *
   * @param toKey The toKey.
   * @param pmd The index partition metadata (MAY be null).
   * @return The <i>toKey</i> -or- the rightSeparator key of the index partition IFF <i>pmd</i> is
   *     non-<code>null</code> AND the <i>toKey</i> is GT the rightSeparator key.
   */
  public static byte[] constrainToKey(byte[] toKey, final ISeparatorKeys pmd) {

    if (pmd == null) return toKey;

    if (toKey != null) {

      if (pmd.getRightSeparatorKey() != null) {

        /*
         * Choose the right separator key if the toKey is strictly
         * greater than the right separator key. This has the effect of
         * constraining the toKey to be within the index partition key
         * range.
         */

        final int ret = BytesUtil.compareBytes(toKey, pmd.getRightSeparatorKey());

        if (ret > 0) {

          toKey = pmd.getRightSeparatorKey();
        }
      }

    } else {

      /*
       * If the toKey was unconstrained, then use the upper bound of the
       * index partition. If this is the last index partition for an
       * index, then the index partition will also have an unconstrained
       * upper bound.
       */

      toKey = pmd.getRightSeparatorKey();
    }

    return toKey;
  }
}
