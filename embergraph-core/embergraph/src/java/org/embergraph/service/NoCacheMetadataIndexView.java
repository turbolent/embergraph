package org.embergraph.service;

import cutthecrap.utils.striterators.IFilter;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.proc.IIndexProcedure;
import org.embergraph.btree.proc.RangeCountProcedure;
import org.embergraph.journal.ITx;
import org.embergraph.journal.WriteExecutorService;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.MetadataIndex.MetadataIndexMetadata;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.service.ndx.RawDataServiceTupleIterator;

/**
 * An implementation that performs NO caching. All methods read through to the remote metadata
 * index. Basically, this hides the RMI requests.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class NoCacheMetadataIndexView implements IMetadataIndex {

  private final AbstractScaleOutFederation fed;

  private final String name;

  private final long timestamp;

  private final MetadataIndexMetadata mdmd;

  public MetadataIndexMetadata getIndexMetadata() {

    return mdmd;
  }

  protected IMetadataService getMetadataService() {

    return fed.getMetadataService();
  }

  /**
   * @param name The name of the scale-out index.
   * @param timestamp
   */
  public NoCacheMetadataIndexView(
      AbstractScaleOutFederation fed, String name, long timestamp, MetadataIndexMetadata mdmd) {

    if (fed == null) throw new IllegalArgumentException();
    if (name == null) throw new IllegalArgumentException();
    if (mdmd == null) throw new IllegalArgumentException();

    this.fed = fed;

    this.name = name;

    this.timestamp = timestamp;

    this.mdmd = mdmd;
  }

  // @todo re-fetch if READ_COMMITTED or UNISOLATED? it's very unlikely to change.
  public IndexMetadata getScaleOutIndexMetadata() {

    return mdmd;
  }

  // this could be cached easily, but its only used by the unit tests.
  public PartitionLocator get(byte[] key) {

    try {

      return getMetadataService().get(name, timestamp, key);

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
  }

  // harder to cache - must look for "gaps"
  public PartitionLocator find(byte[] key) {

    try {

      return getMetadataService().find(name, timestamp, key);

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
  }

  public long rangeCount() {

    return rangeCount(null, null);
  }

  // only used by unit tests.
  public long rangeCount(final byte[] fromKey, final byte[] toKey) {

    final IIndexProcedure proc =
        new RangeCountProcedure(false /* exact */, false /*deleted*/, fromKey, toKey);

    final Long rangeCount;
    try {

      rangeCount =
          (Long)
              getMetadataService()
                  .submit(timestamp, MetadataService.getMetadataIndexName(name), proc)
                  .get();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }

    return rangeCount.longValue();
  }

  public long rangeCountExact(final byte[] fromKey, final byte[] toKey) {

    final IIndexProcedure proc =
        new RangeCountProcedure(true /* exact */, false /*deleted*/, fromKey, toKey);

    final Long rangeCount;
    try {

      rangeCount =
          (Long)
              getMetadataService()
                  .submit(timestamp, MetadataService.getMetadataIndexName(name), proc)
                  .get();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }

    return rangeCount.longValue();
  }

  public long rangeCountExactWithDeleted(final byte[] fromKey, final byte[] toKey) {

    final IIndexProcedure proc =
        new RangeCountProcedure(true /* exact */, true /*deleted*/, fromKey, toKey);

    final Long rangeCount;
    try {

      rangeCount =
          (Long)
              getMetadataService()
                  .submit(timestamp, MetadataService.getMetadataIndexName(name), proc)
                  .get();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }

    return rangeCount.longValue();
  }

  public ITupleIterator rangeIterator() {

    return rangeIterator(null, null);
  }

  public ITupleIterator rangeIterator(final byte[] fromKey, final byte[] toKey) {

    return rangeIterator(fromKey, toKey, 0 /* capacity */, IRangeQuery.DEFAULT, null /* filter */);
  }

  /**
   * Note: Since this view is read-only this method forces the use of {@link ITx#READ_COMMITTED} IFF
   * the timestamp for the view is {@link ITx#UNISOLATED}. This produces the same results on read
   * and reduces contention for the {@link WriteExecutorService}. This is already done automatically
   * for anything that gets run as an index procedure, so we only have to do this explicitly for the
   * range iterator method.
   */
  // not so interesting to cache, but could cache the iterator results on the scale-out index.
  public ITupleIterator rangeIterator(
      byte[] fromKey, byte[] toKey, int capacity, int flags, IFilter filter) {

    return new RawDataServiceTupleIterator(
        getMetadataService(),
        MetadataService.getMetadataIndexName(name),
        (timestamp == ITx.UNISOLATED ? ITx.READ_COMMITTED : timestamp),
        true, // read-consistent semantics.
        fromKey,
        toKey,
        capacity,
        flags,
        filter);
  }

  /** NOP since nothing is cached. */
  public void staleLocator(PartitionLocator locator) {}
}
