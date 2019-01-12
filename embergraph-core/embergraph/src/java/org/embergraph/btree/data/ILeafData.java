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
 * Created on Dec 15, 2006
 */

package org.embergraph.btree.data;

import org.embergraph.btree.raba.IRaba;
import org.embergraph.btree.raba.codec.IRabaCoder;
import org.embergraph.rawstore.IRawStore;

/**
 * Interface for low-level data access for the leaves of a B+-Tree.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface ILeafData extends IAbstractNodeData, IKeysData {

  /**
   * The #of values in the leaf (this MUST be equal to the #of keys for a leaf).
   *
   * @return The #of values in the leaf.
   */
  int getValueCount();

  /**
   * Return the object storing the logical byte[][] containing the values for the leaf. When the
   * leaf maintains delete markers you MUST check whether or not the tuple is deleted before
   * requesting its value.
   *
   * <p>Note: When the leaf maintains raw records you MUST check whether or not the value was
   * written onto a raw record before interpreting the data returned by {@link IRaba#get(int)}. If
   * the length of the value exceeded the configured maximum record length for the index, then the
   * value was written onto a raw record on the backing store and {@link IRaba#get(int)} will return
   * the encoded address of that record rather than its data.
   *
   * @see #hasDeleteMarkers()
   * @see #getDeleteMarker(int)
   */
  IRaba getValues();

  /**
   * The version timestamp for the entry at the specified index.
   *
   * @return The version timestamp for the index entry.
   * @throws IndexOutOfBoundsException unless index is in [0:ntuples-1].
   * @throws UnsupportedOperationException if version timestamps are not being maintained (they are
   *     only required for indices on which transaction processing will be used).
   */
  long getVersionTimestamp(int index);

  /**
   * Return <code>true</code> iff the entry at the specified index is marked as deleted.
   *
   * @throws IndexOutOfBoundsException unless index is in [0:ntuples-1].
   * @throws UnsupportedOperationException if delete markers are not being maintained.
   */
  boolean getDeleteMarker(int index);

  /**
   * Return the address of the raw record on the backing store of the value stored in the tuple
   * having the given index -or- {@link IRawStore#NULL} if the value is the actual <code>byte[]
   * </code> value associated with the key in the leaf. When the value is the address of a raw
   * record, the actual <code>byte[] value</code> should be read from the backing store using the
   * decoded address.
   *
   * <p>Raw record addresses are created transparently when a large <code>byte[]</code> is
   * associated with a key in the leaf. They are materialized transparently when the tuple
   * associated with the leaf is read. They are deleted when the tuple associated with the leaf is
   * deleted.
   *
   * <p>Note: Raw records are managed at the leaf, rather than the {@link IRaba} level, because
   * there is not always a backing store associated with an {@link IRaba} object. This is similar to
   * how deleted tuples are handled. However, {@link IRabaCoder}s are responsible for coding the
   * <code>long</code> address stored in the {@link #getValues() values raba}. Raw records are only
   * used for large byte[] values. Highly specialized {@link IRabaCoder}s can avoid the potential
   * for a conflict with their own coding scheme by ensuring that the index either will not promote
   * large values to raw records or by refraining from inserting large values into the index.
   *
   * @return The address of the raw record -or- {@link IRawStore#NULL}
   * @throws UnsupportedOperationException if raw record markers are not being maintained.
   */
  long getRawRecord(int index);

  /** Return <code>true</code> iff the leaf maintains version timestamps. */
  @Override
  boolean hasVersionTimestamps();

  /** Return <code>true</code> iff the leaf maintains delete markers. */
  boolean hasDeleteMarkers();

  /**
   * Return <code>true</code> iff the leaf promotes large <code>byte[]</code> values to raw records
   * on the backing store.
   */
  boolean hasRawRecords();

  /**
   * Return <code>true</code> if the leaf data record supports encoding of the address of the
   * previous and next leaf in the B+Tree order.
   */
  boolean isDoubleLinked();

  /**
   * The address of the previous leaf in key order, <code>0L</code> if it is known that there is no
   * previous leaf, and <code>-1L</code> if either: (a) it is not known whether there is a previous
   * leaf; or (b) it is known but the address of that leaf is not known to the caller.
   *
   * @throws UnsupportedOperationException if the leaf data record is not double-linked.
   */
  long getPriorAddr();

  /**
   * The address of the next leaf in key order, <code>0L</code> if it is known that there is no next
   * leaf, and <code>-1L</code> if either: (a) it is not known whether there is a next leaf; or (b)
   * it is known but the address of that leaf is not known to the caller.
   *
   * @throws UnsupportedOperationException if the leaf data record is not double-linked.
   */
  long getNextAddr();
}
