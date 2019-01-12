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
 * Created on Feb 20, 2008
 */

package org.embergraph.mdi;

import org.apache.log4j.Logger;
import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.DelegateIndex;
import org.embergraph.btree.ILinearList;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.cache.LRUCache;
import org.embergraph.mdi.MetadataIndex.MetadataIndexMetadata;

/*
* The extension semantics for the {@link IMetadataIndex} are implemented by this class.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MetadataIndexView extends DelegateIndex implements IMetadataIndex {

  protected static final Logger log = Logger.getLogger(MetadataIndexView.class);

  //    protected static final boolean INFO = log.isInfoEnabled();
  //    protected static final boolean DEBUG = log.isDebugEnabled();

  private final AbstractBTree delegate;

  /*
   * <code>true</code> iff this is a read-only view. this is used to conditionally enable caching of
   * de-serialized objects in {@link #getLocatorAtIndex(int)}. We can't cache those objects if the
   * view is mutable!
   */
  private final boolean readOnly;

  public MetadataIndexView(final AbstractBTree delegate) {

    super(delegate);

    this.delegate = delegate;

    this.readOnly = delegate.isReadOnly();
  }

  public MetadataIndexMetadata getIndexMetadata() {

    return (MetadataIndexMetadata) super.getIndexMetadata();
  }

  public IndexMetadata getScaleOutIndexMetadata() {

    return getIndexMetadata().getManagedIndexMetadata();
  }

  public PartitionLocator get(byte[] key) {

    /*
     * Note: The cast of the key to Object triggers automatic
     * de-serialization using the ITupleSerializer.
     */

    return (PartitionLocator) delegate.lookup((Object) key);
  }

  /*
   * The method is used to discover the locator for the index partition within which the <i>key</i>
   * would be found.
   */
  public PartitionLocator find(byte[] key) {

    return find_with_indexOf(key);
    //        return find_with_iterator(key);

  }

  /*
   * The implementation uses an iterator with a capacity of ONE (1) and a {@link
   * IRangeQuery#REVERSE} scan. This approach can be used with a key-range partitioned metadata
   * index.
   *
   * @todo test this variant and keep it on hand for the key-range partitioned metadata index (it
   *     does not quite work yet).
   */
  private PartitionLocator find_with_iterator(byte[] key) {

    final ITupleIterator<PartitionLocator> itr =
        delegate.rangeIterator(
            null /* fromKey */,
            key /* toKey */,
            1 /* capacity */,
            IRangeQuery.VALS | IRangeQuery.REVERSE,
            null /* filter */);

    if (!itr.hasNext()) {

      /*
       * There are no index partitions defined since none span the key.
       *
       * Note: This is never a good sign. There should always be at least
       * one index partition having an empty byte[] as its left separator
       * and a null as its right separator.
       */
      log.warn("No index partitions defined? name=" + getIndexMetadata().getName());

      return null;
    }

    return itr.next().getObject();
  }

  /*
   * This implementation depends on the {@link ILinearList} API and therefore can not be used with a
   * key-range partitioned metadata index.
   */
  private PartitionLocator find_with_indexOf(final byte[] key) {

    final long index;

    if (key == null) {

      // use the index of the last partition.
      index = delegate.getEntryCount() - 1;

    } else {

      // locate the index partition for that key.
      index = findIndexOf(key);
    }

    if (index == -1) {

      return null;
    }

    if (readOnly && index < Integer.MAX_VALUE) return getAndCacheLocatorAtIndex((int) index);
    else return getLocatorAtIndex(index);
  }

  /*
   * Uses a cache to reduce {@link PartitionLocator} de-serialization costs.
   *
   * @param index
   * @return
   */
  private PartitionLocator getAndCacheLocatorAtIndex(final int index) {

    final Integer key = Integer.valueOf(index);

    synchronized (locatorCache) {
      PartitionLocator locator = locatorCache.get(key);

      if (locator == null) {

        locator = getLocatorAtIndex(index);

        locatorCache.put(key, locator, false /* dirty */);
      }

      return locator;
    }
  }

  /*
   * {@link #find(byte[])} was a hot spot with the costs being primarily the the de-serialization of
   * the {@link PartitionLocator} from the {@link ITuple} so I setup this {@link LRUCache}. The keys
   * are the index position in the B+Tree. The values are de-serialized {@link PartitionLocator}s.
   * That seems to do the trick for now, but a different approach may be required if {@link
   * #find(byte[])} is changed to use an iterator.
   *
   * <p>Note: This exploits the fact that the indexOf the tuple in the metadata index is also its
   * partition identifier. This is true because the partition identifier is based on a one-up index
   * local counter. This means that the index partition tuples are created in strict order.
   */
  private LRUCache<Integer, PartitionLocator> locatorCache =
      new LRUCache<Integer, PartitionLocator>(1000);

  /*
   * Remove the locator from the {@link #locatorCache}. It will be re-read on demand from the {@link
   * #delegate}.
   */
  public void staleLocator(PartitionLocator locator) {

    locatorCache.remove(locator.getPartitionId());
  }

  /*
   * Looks up and de-serializes the {@link PartitionLocator} at the given index.
   *
   * @param index
   * @return
   */
  private PartitionLocator getLocatorAtIndex(final long index) {

    final ITuple<PartitionLocator> tuple = delegate.valueAt(index, delegate.getLookupTuple());

    return tuple.getObject();
  }

  /*
   * Find the index of the partition spanning the given key. It is only used by {@link
   * #find_with_indexOf(byte[])} and does not scale-out because of a dependency on the {@link
   * ILinearList} API.
   *
   * @return The index of the partition spanning the given key or <code>-1</code> iff there are no
   *     partitions defined.
   * @exception IllegalStateException if there are partitions defined but no partition spans the
   *     key. In this case the {@link MetadataIndex} lacks an entry for the key <code>new byte[]{}
   *     </code>.
   */
  private long findIndexOf(final byte[] key) {

    long pos = delegate.indexOf(key);

    if (pos < 0) {

      /*
       * the key lies between the partition separators and represents the
       * insert position.  we convert it to an index and subtract one to
       * get the index of the partition that spans this key.
       */

      pos = -(pos + 1);

      if (pos == 0) {

        if (delegate.getEntryCount() != 0) {

          throw new IllegalStateException("Partition not defined for empty key.");
        }

        return -1;
      }

      pos--;

      return pos;

    } else {

      /*
       * exact hit on a partition separator, so we choose the entry with
       * that key.
       */

      return pos;
    }
  }
}
