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
package org.embergraph.btree.raba.codec;

import java.io.Serializable;
import org.embergraph.btree.raba.IRaba;
import org.embergraph.io.AbstractFixedByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;

/**
 * Interface for coding (compressing) a logical <code>byte[][]</code> and for accessing the coded
 * data in place.
 *
 * @see IRaba
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IRabaCoder extends Serializable {

  /**
   * Return <code>true</code> if this implementation can code B+Tree keys (supports search on the
   * coded representation). Note that some implementations can code either keys or values.
   */
  boolean isKeyCoder();

  /**
   * Return <code>true</code> if this implementation can code B+Tree values (allows <code>null
   * </code>s). Note that some implementations can code either keys or values.
   */
  boolean isValueCoder();

  /**
   * Return true iff this {@link IRabaCoder} supports duplicate keys.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/763" > Stochastic Results With
   *     Analytic Query Mode </a>
   */
  boolean isDuplicateKeys();

  /**
   * Encode the data, returning an {@link ICodedRaba}. Implementations of this method should be
   * optimized for the very common use case where the caller requires immediate access to the coded
   * data record. In that case, many of the {@link IRabaCoder} implementations can be optimized by
   * passing the underlying decoding object directly into an alternative constructor for the {@link
   * ICodedRaba}. The byte[] slice for the coded data record is available from {@link
   * ICodedRaba#data()}.
   *
   * <p>This method covers the vast major of the use cases for coding data, which is to code B+Tree
   * keys or values for a node or leaf that has been evicted from the {@link AbstractBTree}'s write
   * retention queue. The common use case is to wrap a coded record that was read from an {@link
   * IRawStore}. The {@link IndexSegmentBuilder} is a special case, since the coded record will not
   * be used other than to write it on the disk.
   */
  public ICodedRaba encodeLive(IRaba raba, DataOutputBuffer buf);

  /**
   * Encode the data.
   *
   * <p>Note: Implementations of this method are typically heavy. While it is always valid to {@link
   * #encode(IRaba, DataOutputBuffer)} an {@link IRaba} , DO NOT invoke this <em>arbitrarily</em> on
   * data which may already be coded. The {@link ICodedRaba} interface will always be implemented
   * for coded data.
   *
   * @param raba The data.
   * @param buf A buffer on which the coded data will be written.
   * @return A slice onto the post-condition state of the caller's buffer whose view corresponds to
   *     the coded record. This may be written directly onto an output stream or the slice may be
   *     converted to an exact fit byte[].
   * @throws UnsupportedOperationException if {@link IRaba#isKeys()} is <code>true</code> and this
   *     {@link IRabaCoder} can not code keys.
   * @throws UnsupportedOperationException if {@link IRaba#isKeys()} is <code>false</code> and this
   *     {@link IRabaCoder} can not code values.
   */
  AbstractFixedByteArrayBuffer encode(IRaba raba, DataOutputBuffer buf);

  /**
   * Return an {@link IRaba} which can access the coded data. In general, implementations SHOULD NOT
   * materialize a backing byte[][]. Instead, the implementation should access the data in place
   * within the caller's buffer. Frequently used fields MAY be cached, but the whole point of the
   * {@link IRabaCoder} is to minimize the in-memory footprint for the B+Tree by using a coded (aka
   * compressed) representation of the keys and values whenever possible.
   *
   * @param data The record containing the coded data.
   * @return A view of the coded data.
   */
  ICodedRaba decode(AbstractFixedByteArrayBuffer data);
}
