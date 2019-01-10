package com.bigdata.htree.data;

import com.bigdata.btree.data.AbstractLeafDataRecordTestCase;
import com.bigdata.btree.data.ILeafData;
import com.bigdata.btree.raba.IRaba;

/**
 * Abstract class for tests of {@link ILeafData} implementations for a hash
 * bucket.
 */
abstract public class AbstractHashBucketDataRecordTestCase extends
		AbstractLeafDataRecordTestCase {

	public AbstractHashBucketDataRecordTestCase() {

		super();

	}

	public AbstractHashBucketDataRecordTestCase(String name) {

		super(name);

	}

	@Override
	protected ILeafData mockLeafFactory(final IRaba keys, final IRaba vals,
			final boolean[] deleteMarkers, final long[] versionTimestamps,
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

		return new MockBucketData(keys, vals, deleteMarkers, versionTimestamps,
				rawRecords);//, lengthMSB, hashCodes);

	}

}
