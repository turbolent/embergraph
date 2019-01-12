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
 * Created on Feb 11, 2008
 */

package org.embergraph.mdi;

import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.mdi.MetadataIndex.MetadataIndexMetadata;

/*
 * Interface for a metadata index. The metadata index stores the {@link PartitionLocator}s that
 * specify which {@link IDataService} has data for each index partition in a scale-out index.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IMetadataIndex extends IRangeQuery { // extends IIndex {

  /*
   * The metadata for the metadata index itself. Note that the {@link
   * MetadataIndexMetadata#getManagedIndexMetadata()} returns the template {@link IndexMetadata} for
   * the scale-out index partitions.
   *
   * @see #getScaleOutIndexMetadata()
   */
  MetadataIndexMetadata getIndexMetadata();

  /** The metadata template for the scale-out index managed by this metadata index. */
  IndexMetadata getScaleOutIndexMetadata();

  /*
   * The partition with that separator key or <code>null</code> (exact match on the separator key).
   *
   * @param key The separator key (the first key that would go into that partition).
   * @return The partition with that separator key or <code>null</code>.
   */
  PartitionLocator get(byte[] key);

  /*
   * Find and return the partition spanning the given key.
   *
   * @param key A key (optional). When <code>null</code> the locator for the last index partition
   *     will be returned.
   * @return The partition spanning the given key or <code>null</code> if there are no partitions
   *     defined.
   */
  PartitionLocator find(byte[] key);

  /*
   * Notification that a locator is stale. Caching implementations of this interface will use this
   * notice to update their state from the authoritative metadata index. Non-caching and
   * authoritative implementations just ignore this message.
   *
   * @param locator The locator.
   */
  void staleLocator(PartitionLocator locator);
}
