package org.embergraph.htree.data;

import org.embergraph.btree.data.AbstractLeafDataRecordTestCase;
import org.embergraph.btree.data.ILeafData;
import org.embergraph.btree.raba.IRaba;

/** Abstract class for tests of {@link ILeafData} implementations for a hash bucket. */
public abstract class AbstractHashBucketDataRecordTestCase extends AbstractLeafDataRecordTestCase {

  public AbstractHashBucketDataRecordTestCase() {

    super();
  }

  public AbstractHashBucketDataRecordTestCase(String name) {

    super(name);
  }

  @Override
  protected ILeafData mockLeafFactory(
      final IRaba keys,
      final IRaba vals,
      final boolean[] deleteMarkers,
      final long[] versionTimestamps,
      final boolean[] rawRecords) {

    //		/*
    //		 * Note: This computes the MSB prefix and the hash codes using the
    //		 * standard Java semantics for the hash of a byte[]. In practice, the
    //		 * hash value is normally computed from the key using an application
    //		 * specified hash function.
    //		 */
    //		final int lengthMSB = 0;
    //
    //		final int[] hashCodes = new int[keys.size()];
    //
    //		for (int i = 0; i < hashCodes.length; i++) {
    //
    //			hashCodes[i] = keys.get(i).hashCode();
    //
    //		}

    return new MockBucketData(
        keys, vals, deleteMarkers, versionTimestamps, rawRecords); // , lengthMSB, hashCodes);
  }
}
