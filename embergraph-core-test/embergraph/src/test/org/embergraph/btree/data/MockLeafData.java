package org.embergraph.btree.data;

import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.raba.IRaba;
import org.embergraph.io.AbstractFixedByteArrayBuffer;
import org.embergraph.rawstore.IRawStore;

/**
 * Mock object for {@link ILeafData} used for unit tests.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MockLeafData extends AbstractMockNodeData implements ILeafData {

  private final IRaba vals;

  private final boolean[] deleteMarkers;

  private final long[] versionTimestamps;

  private final long minVersionTimestamp, maxVersionTimestamp;

  private final boolean[] rawRecords;

  public final IRaba getValues() {

    return vals;
  }

  public final byte[] getValue(final int index) {

    return vals.get(index);
  }

  //    final public int getSpannedTupleCount() {
  //
  //        return vals.size();
  //
  //    }

  public final int getValueCount() {

    return vals.size();
  }

  public final boolean isLeaf() {

    return true;
  }

  public final boolean isReadOnly() {

    return true;
  }

  /** No. */
  public final boolean isCoded() {

    return false;
  }

  public final AbstractFixedByteArrayBuffer data() {

    throw new UnsupportedOperationException();
  }

  public final boolean getDeleteMarker(final int index) {

    if (deleteMarkers == null) throw new UnsupportedOperationException();

    return deleteMarkers[index];
  }

  public final long getVersionTimestamp(final int index) {

    if (versionTimestamps == null) throw new UnsupportedOperationException();

    return versionTimestamps[index];
  }

  public final long getRawRecord(final int index) {

    if (rawRecords == null) throw new UnsupportedOperationException();

    if (!rawRecords[index]) return IRawStore.NULL;

    final byte[] b = vals.get(index);

    final long addr = AbstractBTree.decodeRecordAddr(b);

    return addr;
  }

  public final boolean hasDeleteMarkers() {

    return deleteMarkers != null;
  }

  public final boolean hasVersionTimestamps() {

    return versionTimestamps != null;
  }

  public boolean hasRawRecords() {

    return rawRecords != null;
  }

  //    public MockLeafData(final IRaba keys, final IRaba vals) {
  //
  //		this(keys, vals, null/* deleteMarkers */, null/* versionTimestamps */,
  //				null/* rawRecords */);
  //
  //    }

  public MockLeafData(
      final IRaba keys,
      final IRaba vals,
      final boolean[] deleteMarkers,
      final long[] versionTimestamps,
      final boolean[] rawRecords) {

    super(keys);

    assert vals != null;
    assert !vals.isKeys();
    assert vals.size() == keys.size();
    assert vals.capacity() == keys.capacity();

    if (deleteMarkers != null) assert deleteMarkers.length == vals.capacity();

    if (versionTimestamps != null) assert versionTimestamps.length == vals.capacity();

    if (rawRecords != null) assert rawRecords.length == vals.capacity();

    this.vals = vals;

    this.deleteMarkers = deleteMarkers;

    this.rawRecords = rawRecords;

    this.versionTimestamps = versionTimestamps;

    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    if (versionTimestamps != null) {

      final int nkeys = keys.size();

      for (int i = 0; i < nkeys; i++) {

        final long t = versionTimestamps[i];

        if (t < min) min = t;

        if (t > max) max = t;
      }
    }

    this.minVersionTimestamp = min;

    this.maxVersionTimestamp = max;
  }

  public boolean isDoubleLinked() {
    return false;
  }

  public long getNextAddr() {
    throw new UnsupportedOperationException();
  }

  public long getPriorAddr() {
    throw new UnsupportedOperationException();
  }

  public long getMaximumVersionTimestamp() {

    if (!hasVersionTimestamps()) throw new UnsupportedOperationException();

    return maxVersionTimestamp;
  }

  public long getMinimumVersionTimestamp() {

    if (!hasVersionTimestamps()) throw new UnsupportedOperationException();

    return minVersionTimestamp;
  }

  public String toString() {

    final StringBuilder sb = new StringBuilder();

    sb.append(super.toString());

    sb.append("{");

    DefaultLeafCoder.toString(this, sb);

    sb.append("}");

    return sb.toString();
  }
}
