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
 * Created on Oct 3, 2008
 */

package org.embergraph.service;

import cutthecrap.utils.striterators.IFilter;
import org.apache.log4j.Logger;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.MetadataIndex;
import org.embergraph.mdi.MetadataIndex.MetadataIndexMetadata;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.service.ndx.RawDataServiceTupleIterator;

/*
* Implementation caches all locators but does not allow stale locators. This is useful for
 * read-historical index views since locators can not become stale for a historical view.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class CacheOnceMetadataIndex implements IMetadataIndex {

  protected static final Logger log = Logger.getLogger(CacheOnceMetadataIndex.class);

  /** The federation. */
  protected final AbstractScaleOutFederation<?> fed;

  /** Name of the scale-out index. */
  protected final String name;

  /** Timestamp of the view. */
  protected final long timestamp;

  /** Cached metadata record for the metadata index. */
  protected final MetadataIndexMetadata mdmd;

  /** Local copy of the remote {@link MetadataIndex}. */
  private final MetadataIndex mdi;

  @Override
  public String toString() {

    return super.toString()
        + "{name="
        + name
        + ",timestamp="
        + TimestampUtility.toString(timestamp)
        + "}";
  }

  /*
   * Caches the index partition locators.
   *
   * @param name The name of the scale-out index.
   */
  public CacheOnceMetadataIndex(
      final AbstractScaleOutFederation<?> fed,
      final String name,
      final long timestamp,
      final MetadataIndexMetadata mdmd) {

    if (fed == null) throw new IllegalArgumentException();

    if (name == null) throw new IllegalArgumentException();

    if (mdmd == null) throw new IllegalArgumentException();

    this.fed = fed;

    this.name = name;

    this.timestamp = timestamp;

    /*
     * Allocate a cache for the defined index partitions.
     */
    this.mdi =
        MetadataIndex.create(
            fed.getTempStore(),
            mdmd.getIndexUUID(), // UUID of the metadata index.
            mdmd.getManagedIndexMetadata() // the managed index's metadata.
            );

    this.mdmd = mdmd;

    // cache all the locators.
    cacheLocators(null /* fromKey */, null /* toKey */);
  }

  /*
   * Bulk copy the partition definitions for the scale-out index into the client.
   *
   * <p>Note: This assumes that the metadata index is NOT partitioned and DOES NOT support delete
   * markers.
   */
  protected void cacheLocators(final byte[] fromKey, final byte[] toKey) {

    long n = 0;

    /*
     * Note: before we can update the cache we need to delete any locators
     * in the specified key range. This is a NOP of course if we are just
     * caching everything, but there is a subclass that updates key-ranges
     * of the cache in response to stale locator notices, so we have to
     * first wipe out the old locator(s) before reading in the updated
     * locator(s)
     */
    mdi.rangeIterator(fromKey, toKey, 0 /* capacity */, IRangeQuery.REMOVEALL, null /*filter*/);

    /*
     * Read the locators from the remote metadata service.
     */
    final ITupleIterator<?> itr =
        new RawDataServiceTupleIterator(
            fed.getMetadataService(),
            MetadataService.getMetadataIndexName(name),
            timestamp,
            true, // readConsistent
            fromKey,
            toKey,
            0, // capacity
            IRangeQuery.KEYS | IRangeQuery.VALS | IRangeQuery.READONLY,
            null // filter
            );

    while (itr.hasNext()) {

      final ITuple<?> tuple = itr.next();

      final byte[] key = tuple.getKey();

      final byte[] val = tuple.getValue();

      mdi.insert(key, val);

      n++;
    }

    if (log.isInfoEnabled()) {

      log.info("Copied " + n + " locator records: name=" + name);
    }
  }

  /*
   * @throws UnsupportedOperationException stale locators should not occur for read-historical
   *     views!
   */
  public void staleLocator(final PartitionLocator locator) {

    throw new UnsupportedOperationException();
  }

  public final MetadataIndexMetadata getIndexMetadata() {

    return mdmd;
  }

  public final IndexMetadata getScaleOutIndexMetadata() {

    return getIndexMetadata().getManagedIndexMetadata();
  }

  public PartitionLocator get(final byte[] key) {

    return mdi.get(key);
  }

  public PartitionLocator find(final byte[] key) {

    return mdi.find(key);
  }

  public long rangeCount() {

    return mdi.rangeCount();
  }

  public long rangeCount(final byte[] fromKey, final byte[] toKey) {

    return mdi.rangeCount(fromKey, toKey);
  }

  public long rangeCountExact(final byte[] fromKey, final byte[] toKey) {

    return mdi.rangeCountExact(fromKey, toKey);
  }

  public long rangeCountExactWithDeleted(final byte[] fromKey, final byte[] toKey) {

    return mdi.rangeCountExactWithDeleted(fromKey, toKey);
  }

  public ITupleIterator rangeIterator() {

    return mdi.rangeIterator();
  }

  public ITupleIterator rangeIterator(
      final byte[] fromKey,
      final byte[] toKey,
      final int capacity,
      final int flags,
      final IFilter filter) {

    return mdi.rangeIterator(fromKey, toKey, capacity, flags, filter);
  }

  public ITupleIterator rangeIterator(final byte[] fromKey, final byte[] toKey) {

    return mdi.rangeIterator(fromKey, toKey);
  }
}
