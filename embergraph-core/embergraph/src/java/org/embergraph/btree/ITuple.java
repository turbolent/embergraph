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
 * Created on Dec 21, 2007
 */

package org.embergraph.btree;

import org.embergraph.io.ByteArrayBuffer;
import org.embergraph.io.DataInputBuffer;
import org.embergraph.rawstore.IBlock;

/*
 * Interface exposes more direct access to keys and values visited by an {@link ITupleIterator}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param E The generic type of the de-serialized objects stored in the values of the index.
 * @todo consider only the {@link #getKeyStream()} and {@link #getValueStream()} vs the {@link
 *     #getKeyBuffer()} and {@link #getValueBuffer()} methods. do we need both?
 */
public interface ITuple<E> {

  /*
   * The {@link IRangeQuery} flags
   *
   * <pre>
   *
   * if ((flags &amp; IRangeQuery.KEYS) != 0) {
   *
   *     // keys requested.
   *
   * }
   *
   * </pre>
   *
   * Note: the {@link IRangeQuery#DELETED} flag state is a property of the iterator NOT the tuple.
   * Whether or not a tuple is deleted is detected using {@link ITuple#isDeletedVersion()}.
   */
  int flags();

  /** True iff {@link IRangeQuery#KEYS} was specified. */
  boolean getKeysRequested();

  /** True iff {@link IRangeQuery#VALS} was specified. */
  boolean getValuesRequested();

  /*
   * The index of the source from which the tuple was read. This is zero (0) if there is only a
   * single source, e.g., a {@link BTree} or {@link IndexSegment}. When reading on a {@link
   * FusedView} this is the index of the element of the view which reported the tuple.
   *
   * @throws IllegalStateException if nothing has been visited.
   * @see <a href="http://sourceforge.net/apps/trac/bigdata/ticket/682">AtomicRowFilter
   *     UnsupportedOperationException </a>
   */
  int getSourceIndex();

  /*
   * The #of entries that have been visited so far and ZERO (0) until the first entry has been
   * visited.
   */
  long getVisitCount();

  /*
   * Returns a copy of the current key.
   *
   * <p>Note: This can cause a heap allocation depending on how the keys are buffered. See {@link
   * #getKeyBuffer()} to avoid that allocation.
   *
   * @throws UnsupportedOperationException if keys are not being materialized.
   */
  byte[] getKey();

  /*
   * The buffer into which the keys are being copied.
   *
   * @return The buffer.
   * @throws UnsupportedOperationException if keys are not being materialized.
   */
  ByteArrayBuffer getKeyBuffer();

  /*
   * Return a stream from which the key may be read.
   *
   * @throws UnsupportedOperationException if the keys were not requested.
   */
  DataInputBuffer getKeyStream();

  /** <code>true</code> iff the value stored under the index entry is <code>null</code>. */
  boolean isNull();

  /*
   * The value in the index under the key.
   *
   * <p>Note: This causes a heap allocation. See {@link #getValueBuffer()} to avoid that allocation.
   *
   * @return The value in the index under the key -or- <code>null</code> if version metadata is
   *     being maintained and the the index entry is marked as deleted.
   * @throws UnsupportedOperationException if values are not being materialized.
   */
  byte[] getValue();

  /*
   * The buffer into which the values are being copied.
   *
   * <p>Note: If the index supports delete markers then you MUST test {@link #isDeletedVersion()} in
   * order to determine whether or not the value buffer contains data for the current index entry.
   *
   * @return The buffer.
   * @throws UnsupportedOperationException if values are not being materialized.
   * @throws UnsupportedOperationException if the value is <code>null</code>.
   * @throws UnsupportedOperationException if the index entry is <code>deleted</code>.
   */
  ByteArrayBuffer getValueBuffer();

  /*
   * Return a stream from which the value may be read. Callers SHOULD prefer {@link
   * #getValueStream()} to {@link #getValue()} as it can avoid some heap churn.
   *
   * @throws UnsupportedOperationException if the values were not requested.
   * @throws UnsupportedOperationException if the value is <code>null</code>.
   * @throws UnsupportedOperationException if the index entry is <code>deleted</code>.
   */
  DataInputBuffer getValueStream();

  /*
   * De-serializes the object from the key and/or value.
   *
   * @return The de-serialized object.
   * @throws UnsupportedOperationException if {@link IRangeQuery#KEYS} and/or {@link
   *     IRangeQuery#VALS} are required to de-serialize the object but were not specified when the
   *     {@link ITupleIterator} was created.
   * @see ITupleSerializer#deserialize(ITuple)
   */
  E getObject();

  /*
   * Return an object that may be used to perform a streaming read of a large record from the {@link
   * IRawStore} that provided this tuple.
   *
   * @param addr The address of the record.
   * @return The object that may be used to read that record.
   */
  IBlock readBlock(long addr);

  /*
   * Return the timestamp associated with the index entry -or- <code>0L</code> IFF the index does
   * not support transactional isolation.
   */
  long getVersionTimestamp();

  /*
   * Return <code>true</code> iff the index entry was marked as deleted.
   *
   * <p>Note: If the index does not support deletion markers then this method MUST return <code>
   * false</code>.
   *
   * <p>Note: the {@link IRangeQuery#DELETED} flag state is a property of the iterator NOT the
   * tuple. Whether or not a tuple is deleted is detected using {@link ITuple#isDeletedVersion()}.
   */
  boolean isDeletedVersion();

  /** Return the object that can be used to de-serialize the tuple. */
  ITupleSerializer getTupleSerializer();
}
