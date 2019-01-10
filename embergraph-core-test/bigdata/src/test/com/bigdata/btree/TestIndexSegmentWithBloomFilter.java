/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Dec 21, 2006
 */

package com.bigdata.btree;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import com.bigdata.btree.keys.TestKeyBuilder;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.Options;
import com.bigdata.rawstore.IRawStore;
import com.bigdata.rawstore.SimpleMemoryRawStore;
import com.bigdata.util.BytesUtil;

/**
 * Test build trees on the journal, evicts them into an {@link IndexSegment},
 * and then compares the performance and correctness of index point tests with
 * and without the use of the bloom filter.
 * 
 * @todo compare performance with and without the bloom filter.
 * 
 * @todo test points that will not be in the index as well as those that are.
 * 
 * @todo report on the cost to construct the filter and its serialized size and
 *       runtime space.
 * 
 * @todo verify the target error rate.
 * 
 * @todo explore different error rates, including Fast.mostSignificantBit( n ) +
 *       1 which would provide an expectation of no false positives.
 * 
 * @todo Compare for each build algorithm, just like
 *       {@link TestIndexSegmentBuilderWithLargeTrees}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestIndexSegmentWithBloomFilter extends AbstractBTreeTestCase {

    public TestIndexSegmentWithBloomFilter() {
    }

    public TestIndexSegmentWithBloomFilter(String name) {
        super(name);
    }

    private static final boolean bufferNodes = true;
    
    public Properties getProperties() {

        if (properties == null) {

            properties = super.getProperties();

            properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk
                    .toString());

            properties.setProperty(Options.CREATE_TEMP_FILE, "true");

        }

        return properties;

    }

    private Properties properties;

    /**
     * Return a btree backed by a journal with the indicated branching factor.
     * The serializer requires that values in leaves are {@link SimpleEntry}
     * objects.
     * 
     * @param branchingFactor
     *            The branching factor.
     * 
     * @return The btree.
     */
    public BTree getBTree(int branchingFactor, BloomFilterFactory bloomFilterFactory) {

        IRawStore store = new SimpleMemoryRawStore(); 

        IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
        
        metadata.setBranchingFactor(branchingFactor);
        
        metadata.setBloomFilterFactory(bloomFilterFactory);
        
        BTree btree = BTree.create(store, metadata);

        return btree;

    }
    
    /**
     * Branching factors for the source btree that is then used to build an
     * {@link IndexSegment}. This parameter indirectly determines both the #of
     * leaves and the #of entries in the source btree.
     * 
     * Note: Regardless of the branching factor in the source btree, the same
     * {@link IndexSegment} should be build for a given set of entries
     * (key-value pairs) and a given output branching factor for the
     * {@link IndexSegment}. However, input trees of different heights also
     * stress different parts of the algorithm.
     */
    final int[] branchingFactors = new int[]{3,4,5,10,13};//64};//128};//,512};
    
    /**
     * A stress test for building {@link IndexSegment}s. A variety of
     * {@link BTree}s are built from dense random keys in [1:n] using a variety
     * of branching factors. For each {@link BTree}, a variety of
     * {@link IndexSegment}s are built using a variety of output branching
     * factors. For each {@link IndexSegment}, we then compare it against its
     * source {@link BTree} for the same total ordering.
     */
    public void test_randomDenseKeys() throws Exception {

        final double p = 1/64d;// error rate
        final double maxP = p*10; // max error rate

        for(int i=0; i<branchingFactors.length; i++) {
            
            final int m = branchingFactors[i];
            
            doBuildIndexSegmentAndCompare(doSplitWithRandomDenseKeySequence(
                    getBTree(m, new BloomFilterFactory(m/* n */, p, maxP)), m,
                    m));

            doBuildIndexSegmentAndCompare(doSplitWithRandomDenseKeySequence(
                    getBTree(m, new BloomFilterFactory(m * m/* n */, p, maxP)),
                    m, m * m));

            doBuildIndexSegmentAndCompare(doSplitWithRandomDenseKeySequence(
                    getBTree(m, new BloomFilterFactory(m * m * m/* n */, p,
                            maxP)), m, m * m * m));

            // @todo overflows the initial journal extent.
//            doBuildIndexSegmentAndCompare( doSplitWithRandomDenseKeySequence( getBTree(m,errorRate), m, m*m*m*m ) );

        }
        
    }
    
    /**
     * A stress test for building {@link IndexSegment}s. A variety of
     * {@link BTree}s are built from spase random keys using a variety of
     * branching factors. For each {@link BTree}, a variety of
     * {@link IndexSegment}s are built using a variety of output branching
     * factors. For each {@link IndexSegment}, we then compare it against its
     * source {@link BTree} for the same total ordering.
     */
    public void test_randomSparseKeys() throws Exception {

        int trace = 0;
        
        for(int i=0; i<branchingFactors.length; i++) {
            
            int m = branchingFactors[i];

            doBuildIndexSegmentAndCompare(doInsertRandomSparseKeySequenceTest(
                    getBTree(m), m, trace));

            doBuildIndexSegmentAndCompare(doInsertRandomSparseKeySequenceTest(
                    getBTree(m), m * m, trace));

            doBuildIndexSegmentAndCompare(doInsertRandomSparseKeySequenceTest(
                    getBTree(m), m * m * m, trace));

            //@todo overflows the initial journal extent.
//            doBuildIndexSegmentAndCompare( doInsertRandomSparseKeySequenceTest(m,m*m*m*m,trace) );

        }
    
    }

    /**
     * Test when the input tree is a root leaf with three values.  The output
     * tree will also be a root leaf.
     * 
     * @throws IOException
     */
    public void test_rootLeaf() throws Exception {

        final int m = 3; // for input and output trees.
        
        final BTree btree = getBTree(m, new BloomFilterFactory(100, 1 / 64d,
                1 / 32d));
        
        SimpleEntry v3 = new SimpleEntry(3);
        SimpleEntry v5 = new SimpleEntry(5);
        SimpleEntry v7 = new SimpleEntry(7);

        btree.insert(TestKeyBuilder.asSortKey(3), v3);
        btree.insert(TestKeyBuilder.asSortKey(5), v5);
        btree.insert(TestKeyBuilder.asSortKey(7), v7);
       
        final File outFile2 = new File(getName()+"_m"+m+ "_bloom.seg");

        if( outFile2.exists() && ! outFile2.delete() ) {
            fail("Could not delete old index segment: "+outFile2.getAbsoluteFile());
        }
        
        final File tmpDir = outFile2.getAbsoluteFile().getParentFile(); 
        
        /*
         * Build the index segment with a bloom filter.
         */
		if (log.isInfoEnabled())
			log.info("Building index segment (w/ bloom): in(m="
                + btree.getBranchingFactor() + ", nentries=" + btree.getEntryCount()
                + "), out(m=" + m + ")");

        final long commitTime = System.currentTimeMillis();
        
        final IndexSegmentBuilder builder2 = IndexSegmentBuilder.newInstance(
                outFile2, tmpDir, btree.getEntryCount(), btree.rangeIterator(),
                m, btree.getIndexMetadata(), commitTime,
                true/* compactingMerge */, bufferNodes);

        @SuppressWarnings("unused")
		final IndexSegmentCheckpoint checkpoint = builder2.call();
        
//      @see BLZG-1501 (remove LRUNexus)        
//        if (LRUNexus.INSTANCE != null) {
//
//            /*
//             * Clear the records for the index segment from the cache so we will
//             * read directly from the file. This is necessary to ensure that the
//             * data on the file is good rather than just the data in the cache.
//             */
//            
//            LRUNexus.INSTANCE.deleteCache(checkpoint.segmentUUID);
//
//        }

//        IndexSegmentBuilder builder2 = new IndexSegmentBuilder(outFile2,
//                tmpDir, btree, m, 1/64.);

        // the bloom filter instance before serialization.
        IBloomFilter bloomFilter = builder2.bloomFilter;
        
        // false positive tests (should succeed with reasonable errorRate).
        assertTrue("3",bloomFilter.contains(i2k(3)));
        assertTrue("5",bloomFilter.contains(i2k(5)));
        assertTrue("7",bloomFilter.contains(i2k(7)));
        // correct rejections (must succeed)
        assertFalse("4",bloomFilter.contains(i2k(4)));
        assertFalse("9",bloomFilter.contains(i2k(9)));

        /*
         * Verify can load the index file and that the metadata
         * associated with the index file is correct (we are only
         * checking those aspects that are easily defined by the test
         * case and not, for example, those aspects that depend on the
         * specifics of the length of serialized nodes or leaves).
         */
		if (log.isInfoEnabled())
			log.info("Opening index segment w/ bloom filter.");
        final IndexSegment seg2 = new IndexSegmentStore(outFile2).loadIndexSegment();
        try {
        
        /*
         * Verify the total index order.
         */
		if (log.isInfoEnabled())
			log.info("Verifying index segments.");
        assertSameBTree(btree, seg2);

        // the bloom filter instance that was de-serialized.
        bloomFilter = seg2.getBloomFilter();
        
        // false positive tests (should succeed with resonable errorRate).
        assertTrue("3",bloomFilter.contains(i2k(3)));
        assertTrue("5",bloomFilter.contains(i2k(5)));
        assertTrue("7",bloomFilter.contains(i2k(7)));
        // correct rejections (must succeed)
        assertFalse("4",bloomFilter.contains(i2k(4)));
        assertFalse("9",bloomFilter.contains(i2k(9)));
        
        // Note: this is a very small index (3 keys) so the cast is safe.
        byte[][] keys = new byte[(int)btree.getEntryCount()][];
        byte[][] vals = new byte[(int)btree.getEntryCount()][];

        getKeysAndValues(btree,keys,vals);

        doRandomLookupTest("btree", btree, keys, vals);
        doRandomLookupTest("w/ bloom", seg2, keys, vals);
        
        } finally {
			if (log.isInfoEnabled())
				log.info("Closing index segments.");
			seg2.close();
        
        }

        if (!outFile2.delete()) {

            log.warn("Could not delete index segment: " + outFile2);

        }
        
    }
    
    /**
     * Test helper builds an index segment from the btree using several
     * different branching factors and each time compares the resulting total
     * ordering to the original btree.
     * 
     * @param btree The source btree.
     */
    public void doBuildIndexSegmentAndCompare(final BTree btree)
            throws Exception {

    	try {
		if (btree.getEntryCount() > Integer.MAX_VALUE) {
			/*
			 * This code can not validate a B+Tree with more than MAX_INT keys
			 * since it relies on materialization of the data in RAM within
			 * arrays and Java does not support int64 array indices.
			 */
			throw new RuntimeException();
    	}
    	
        // branching factors used for the index segment.
        final int branchingFactors[] = new int[] { 3, 4, 5, 10, 20, 60, 100,
                256, 1024, 4096, 8192 };
        
        for( int i=0; i<branchingFactors.length; i++ ) {
        
            int m = branchingFactors[i];

            final File outFile = new File(getName()+"_m"+m+ ".seg");
            final File outFile2 = new File(getName()+"_m"+m+ "_bloom.seg");

            if( outFile.exists() && ! outFile.delete() ) {
                fail("Could not delete old index segment: "+outFile.getAbsoluteFile());
            }
            
            if( outFile2.exists() && ! outFile2.delete() ) {
                fail("Could not delete old index segment: "+outFile2.getAbsoluteFile());
            }
            
            final File tmpDir = outFile.getAbsoluteFile().getParentFile(); 
            
            /*
             * Build the index segment.
             */
            
            final long commitTime = System.currentTimeMillis();
            
            {
                
    			if (log.isInfoEnabled())
    				log.info("Building index segment (w/o bloom): in(m="
                        + btree.getBranchingFactor() + ", nentries=" + btree.getEntryCount()
                        + "), out(m=" + m + ")");
                
                IndexMetadata metadata = btree.getIndexMetadata().clone();
                
                metadata.setBloomFilterFactory(null/*disable*/);

                IndexSegmentBuilder.newInstance(outFile, tmpDir, btree.getEntryCount(),
                        btree.rangeIterator(), m, metadata, commitTime,
                        true/*compactingMerge*/,bufferNodes).call();
                
//              new IndexSegmentBuilder(outFile, tmpDir, btree, m, 0.);
                
            }
            
            final IndexSegmentBuilder builder2;
            {

    			if (log.isInfoEnabled())
    				log.info("Building index segment (w/ bloom): in(m="
                        + btree.getBranchingFactor() + ", nentries=" + btree.getEntryCount()
                        + "), out(m=" + m + ")");
            
                final IndexMetadata metadata = btree.getIndexMetadata().clone();
                
                /*
                 * Note: Since we know the exact #of index entries in an index
                 * segment, both [n] and [maxP] will be ignored when it comes
                 * time to create the bloom filter for the index segment.
                 */
                metadata.setBloomFilterFactory(new BloomFilterFactory(
                        1/* n */, 1 / 64d/* p */, 1 / 32d/* maxP */));

                builder2 = IndexSegmentBuilder.newInstance(outFile2, tmpDir,
                        btree.getEntryCount(), btree.rangeIterator(), m,
                        metadata, commitTime, true/* compactingMerge */,
                        bufferNodes);

                builder2.call();
            
//            IndexSegmentBuilder builder2 = new IndexSegmentBuilder(outFile2,
//                    tmpDir, btree, m, 1/64.);
                
            }

            /*
             * Verify can load the index file and that the metadata
             * associated with the index file is correct (we are only
             * checking those aspects that are easily defined by the test
             * case and not, for example, those aspects that depend on the
             * specifics of the length of serialized nodes or leaves).
             */
			if (log.isInfoEnabled())
				log.info("Opening index segment w/o bloom filter.");
            final IndexSegment seg = new IndexSegmentStore(outFile).loadIndexSegment();

            /*
             * Verify can load the index file and that the metadata
             * associated with the index file is correct (we are only
             * checking those aspects that are easily defined by the test
             * case and not, for example, those aspects that depend on the
             * specifics of the length of serialized nodes or leaves).
             */
			if (log.isInfoEnabled())
				log.info("Opening index segment w/ bloom filter.");
            final IndexSegment seg2 = new IndexSegmentStore(outFile2).loadIndexSegment();

            /*
             * Explicitly test the bloom filter against ground truth. 
             */
            
            // Note: cast is safe - we check entryCount above.
            final byte[][] keys = new byte[(int)btree.getEntryCount()][];
            final byte[][] vals = new byte[(int)btree.getEntryCount()][];

            getKeysAndValues(btree,keys,vals);

            /*
             * vet the bloom filter on the index segment builder
             * (pre-serialization).
             */
            doBloomFilterTest("pre-serialization", builder2.bloomFilter, keys);
            
            /*
             * vet the bloom filter on the loaded index segment
             * (post-serialization).
             */
            doBloomFilterTest("pre-serialization", seg2.getBloomFilter(), keys);

            /*
             * Verify index segments against the source btree and against one
             * another.
             */
			if (log.isInfoEnabled())
				log.info("Verifying index segments.");
            assertSameBTree(btree, seg);
            assertSameBTree(btree, seg2);
            seg2.close(); // close seg w/ bloom filter and the verify with implicit reopen.
            assertSameBTree(seg, seg2);

			if (log.isInfoEnabled())
				log.info("Closing index segments.");
            seg.close();
            seg2.close();

            if (!outFile.delete()) {

                log.warn("Could not delete index segment: " + outFile);

            }

            if (!outFile2.delete()) {

                log.warn("Could not delete index segment: " + outFile2);

            }

        } // build index segment with the next branching factor.

    	} finally {
            /*
             * Closing the journal.
             */
    		if (log.isInfoEnabled())
    			log.info("Closing journal.");
    		btree.getStore().destroy();        
    	}
    }

    /**
     * Test the bloom filter for false negatives given the ground truth set of
     * keys. if it reports that a key was not in the bloom filter then that is a
     * false negative. bloom filters are not supposed to have false negative.
     * 
     * @param keys
     *            The ground truth keys that were inserted into the bloom
     *            filter.
     */
    protected void doBloomFilterTest(String label, IBloomFilter bloomFilter, byte[][] keys) {
        
        /*
         * Closing the journal.
         */
		if (log.isInfoEnabled())
			log.info("\ncondition: "+label);//+", size="+bloomFilter.size());

        final int[] order = getRandomOrder(keys.length);

        for (int i = 0; i < order.length; i++) {

            final byte[] key = keys[order[i]];

            final boolean found = bloomFilter.contains(key);

            assertTrue("false negative: i=" + i + ", key="
                    + BytesUtil.toString(key), found);
            
        }
 
    }
}
