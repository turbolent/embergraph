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
 * Created on Nov 18, 2006
 */

package com.bigdata.htree;

import java.util.UUID;

import com.bigdata.btree.AbstractNode;
import com.bigdata.btree.Checkpoint;
import com.bigdata.btree.DefaultTupleSerializer;
import com.bigdata.btree.HTreeIndexMetadata;
import com.bigdata.btree.ITupleSerializer;
import com.bigdata.btree.IndexMetadata;
import com.bigdata.btree.MyHardReferenceQueue;
import com.bigdata.btree.PO;
import com.bigdata.btree.keys.ASCIIKeyBuilderFactory;
import com.bigdata.btree.raba.codec.FrontCodedRabaCoderDupKeys;
import com.bigdata.btree.raba.codec.SimpleRabaCoder;
import com.bigdata.cache.HardReferenceQueue;
import com.bigdata.rawstore.IRawStore;
import com.bigdata.rawstore.SimpleMemoryRawStore;
import com.bigdata.util.Bytes;

/**
 * Test suite for the logic performing incremental writes of nodes and leaves
 * onto the store. The actual timing of evictions from the
 * {@link HardReferenceQueue} is essentially unpredictable since evictions are
 * driven by {@link AbstractHTree#touch(AbstractNode)} and nodes and leaves are
 * both touched frequently and in a data and code path dependent manner.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 *          TODO The 2nd two tests in this file both rely on structural
 *          assumptions about the B+Tree. They will have to be carefully
 *          recrafted to work for the HTree.
 * 
 *          TODO There is an apparent problem with the persistence backed stress
 *          tests where incremental eviction can cause a parent directory page
 *          to become immutable during handling of addLevel(). The tests in this
 *          suite do not have the data scale necessary to trigger this issue.
 * 
 *          TODO This focuses on the reference counts and reference changes
 *          while {@link TestDirtyIterators} focuses on structural changes and
 *          the visitation patterns for children and post-order traversal both
 *          for all pages and for only dirty pages.
 * 
 * @see https://sourceforge.net/apps/trac/bigdata/ticket/203#comment:29
 */
public class TestIncrementalWrite extends AbstractHTreeTestCase {

    /**
     * 
     */
    public TestIncrementalWrite() {
    }

    /**
     * @param name
     */
    public TestIncrementalWrite(String name) {
        super(name);
    }
       
	protected HTree getHTree(final IRawStore store, final int addressBits,
			final int queueCapacity, final int queueScan) {
        
        final HTreeIndexMetadata md = new HTreeIndexMetadata(UUID.randomUUID());
        
        md.setAddressBits(addressBits);

		final ITupleSerializer<?,?> tupleSer = new DefaultTupleSerializer(
				new ASCIIKeyBuilderFactory(Bytes.SIZEOF_INT),
				FrontCodedRabaCoderDupKeys.INSTANCE,//
				new SimpleRabaCoder() // vals
				);
		
		md.setTupleSerializer(tupleSer);

		/*
		 * Note: This jumps through hoops to create the HTree instance with the
		 * appropriate parameterization of the hard reference queue.
		 */

        md.write(store);
        
        final Checkpoint checkpoint = md.firstCheckpoint();
        
        checkpoint.write(store);
        
        HTree btree = new TestHTree(store, checkpoint, md, false/*readOnly*/) {

            @Override
            int getQueueCapacity() {
                return queueCapacity;
            }

            @Override
            int getQueueScan() {
                return queueScan;
            }
            
        };
        
        return btree;
        
    }

    /**
     * Custom hard reference queue.
     *  
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private abstract static class TestHTree extends HTree {

        abstract int getQueueCapacity();
        
        abstract int getQueueScan();
        
        /**
         * @param store
         * @param checkpoint
         * @param metadata
         */
        public TestHTree(IRawStore store, Checkpoint checkpoint, IndexMetadata metadata, boolean readOnly) {
         
            super(store, checkpoint, metadata, readOnly);
            
        }
        
        protected HardReferenceQueue<PO> newWriteRetentionQueue(final boolean readOnly) {

            return new MyHardReferenceQueue<PO>(//
                    new DefaultEvictionListener(),//
                    getQueueCapacity(),//
                    getQueueScan()//
            );

        }
        
    }

	/**
	 * Test verifies that an incremental write of the root leaf may be
	 * performed.
	 */
	public void test_incrementalWrite() {

		final IRawStore store = new SimpleMemoryRawStore();
		try {
		
			/*
			 * setup the tree. it uses a queue capacity of two since that is the
			 * minimum allowed. it uses scanning to ensure that one a single
			 * reference to the root leaf actually enters the queue. that way
			 * when we request an incremental write it occurs since the
			 * reference counter for the root leaf will be one (1) since there
			 * is only one reference to that leaf on the queue.
			 */
			final HTree btree = getHTree(store, 2/* addressBits */,
					2/* queueCapacity */, 1/* queueScan */);

            final byte[] k1 = new byte[]{0x01};
            final byte[] v1 = new byte[]{0x01};

			/*
			 * insert some keys into the htree.
			 */
			final DirectoryPage a = btree.getRoot();
			btree.insert(k1, v1);

			/*
			 * do an incremental write of the tree.
			 */
			assertFalse(a.isPersistent());
			((HardReferenceQueue<PO>) btree.writeRetentionQueue)
					.getListener()
					.evicted(
							((HardReferenceQueue<PO>) btree.writeRetentionQueue),
							btree.getRoot());
			assertTrue(a.isPersistent());
		} finally {
			store.destroy();
		}

    }

//    /**
//     * Test verifies that an incremental write of a leaf may be performed, that
//     * identity is assigned to the written leaf, and that the childKey[] on the
//     * parent node is updated to reflect the identity assigned to the leaf.
//     */
//    public void test_incrementalWrite02() {
//
//    	final IRawStore store = new SimpleMemoryRawStore();
//
//    	try {
//
//			/*
//			 * setup the tree with a most queue capacity but set the scan
//			 * parameter such that we never allow more than a single reference
//			 * to a node onto the queue.
//			 */
//			final HTree btree = getHTree(store, 2/* addressBits */,
//					20/* queueCapacity */, 20/* queueScan */);
//
//			/*
//			 * insert keys into the root and cause it to split.
//			 */
//			
//			final byte[] k3 = new byte[]{3};
//			final byte[] k5 = new byte[]{5};
//			final byte[] k7 = new byte[]{7};
//			final byte[] k9 = new byte[]{9};
//			
//			final byte[] v3 = new byte[]{3};
//			final byte[] v5 = new byte[]{5};
//			final byte[] v7 = new byte[]{7};
//			final byte[] v9 = new byte[]{9};
//			
//			final DirectoryPage a = btree.getRoot();
//			btree.insert(k3, v3);
//			btree.insert(k5, v5);
//			btree.insert(k7, v7);
//			btree.insert(k9, v9);
//			assertNotSame(a, btree.getRoot());
//			final Node c = (Node) btree.getRoot();
//			assertKeys(new int[] { 7 }, c);
//			assertEquals(a, c.getChild(0));
//			final Leaf b = (Leaf) c.getChild(1);
//			assertKeys(new int[] { 3, 5 }, a);
//			assertValues(new Object[] { v3, v5 }, a);
//			assertKeys(new int[] { 7, 9 }, b);
//			assertValues(new Object[] { v7, v9 }, b);
//
//			/*
//			 * verify reference counters.
//			 */
//
//			assertEquals(1, a.referenceCount);
//			assertEquals(1, b.referenceCount);
//			assertEquals(1, c.referenceCount);
//
//			/*
//			 * verify that all nodes are NOT persistent.
//			 */
//
//			assertFalse(a.isPersistent());
//			assertFalse(b.isPersistent());
//			assertFalse(c.isPersistent());
//
//			/*
//			 * verify the queue order. we know the queue order since no node is
//			 * allowed into the queue more than once (because the scan parameter
//			 * is equal to the queue capacity) and because we know the node
//			 * creation order (a is created when the tree is created; b is
//			 * created when a is split; and c is created after the split when we
//			 * discover that there is no parent of a and that we need to create
//			 * one).
//			 */
//
//			assertEquals(new PO[] { a, b, c },
//					((MyHardReferenceQueue<PO>) btree.writeRetentionQueue)
//							.toArray(new PO[0]));
//
//			/*
//			 * force (b) to be evicted. since its reference count is one(1) it
//			 * will be made persistent.
//			 * 
//			 * Note: this causes the reference counter for (b) to be reduced to
//			 * zero(0) even through (b) is on the queue. This is not a legal
//			 * state so we can not continue with operation that would touch the
//			 * queue.
//			 */
//
//			((HardReferenceQueue<PO>) btree.writeRetentionQueue)
//					.getListener()
//					.evicted(
//							((HardReferenceQueue<PO>) btree.writeRetentionQueue),
//							b);
//
//			// verify that b is now persistent.
//			assertTrue(b.isPersistent());
//
//			/*
//			 * verify that we set the identity of b on its parent so that it can
//			 * be recovered from the store if necessary.
//			 */
//			assertEquals(b.getIdentity(), c.getChildAddr(1));
//
//		} finally {
//
//			store.destroy();
//
//    	}
//        
//    }
//    
//
//    /**
//     * Test verifies that an incremental write of a node may be performed, that
//     * identity is assigned to the written node, and that the childKey[] on the
//     * node are updated to reflect the identity assigned to its children (the
//     * dirty children are written out when the node is evicted so that the 
//     * persistent node knows the persistent identity of each child).
//     */
//    public void test_incrementalWrite03() {
//    	
//    	final IRawStore store = new SimpleMemoryRawStore();
//
//    	try {
//
//			/*
//			 * setup the tree with a most queue capacity but set the scan
//			 * parameter such that we never allow more than a single reference
//			 * to a node onto the queue.
//			 */
//			final HTree btree = getHTree(store, 3/* addressBits */,
//					20/* queueCapacity */, 20/* queueScan */);
//
//			/*
//			 * insert keys into the root and cause it to split.
//			 */
//			final byte[] k3 = new byte[]{3};
//			final byte[] k5 = new byte[]{5};
//			final byte[] k7 = new byte[]{7};
//			final byte[] k9 = new byte[]{9};
//			
//			final byte[] v3 = new byte[]{3};
//			final byte[] v5 = new byte[]{5};
//			final byte[] v7 = new byte[]{7};
//			final byte[] v9 = new byte[]{9};
//			
//			final DirectoryPage a = (DirectoryPage) btree.getRoot();
//			btree.insert(k3, v3);
//			btree.insert(k5, v5);
//			btree.insert(k7, v7);
//			btree.insert(k9, v9);
//			assertNotSame(a, btree.getRoot());
//			final Node c = (Node) btree.getRoot();
//			assertKeys(new int[] { 7 }, c);
//			assertEquals(a, c.getChild(0));
//			final Leaf b = (Leaf) c.getChild(1);
//			assertKeys(new int[] { 3, 5 }, a);
//			assertValues(new Object[] { v3, v5 }, a);
//			assertKeys(new int[] { 7, 9 }, b);
//			assertValues(new Object[] { v7, v9 }, b);
//
//			/*
//			 * verify reference counters.
//			 */
//
//			assertEquals(1, a.referenceCount);
//			assertEquals(1, b.referenceCount);
//			assertEquals(1, c.referenceCount);
//
//			/*
//			 * verify that all nodes are NOT persistent.
//			 */
//
//			assertFalse(a.isPersistent());
//			assertFalse(b.isPersistent());
//			assertFalse(c.isPersistent());
//
//			/*
//			 * verify the queue order. we know the queue order since no node is
//			 * allowed into the queue more than once (because the scan parameter
//			 * is equal to the queue capacity) and because we know the node
//			 * creation order (a is created when the tree is created; b is
//			 * created when a is split; and c is created after the split when we
//			 * discover that there is no parent of a and that we need to create
//			 * one).
//			 */
//
//			assertEquals(new PO[] { a, b, c },
//					((MyHardReferenceQueue<PO>) btree.writeRetentionQueue)
//							.toArray(new PO[0]));
//
//			/*
//			 * force (c) to be evicted. since its reference count is one(1) it
//			 * will be made persistent.
//			 * 
//			 * Note: this causes the reference counter for (c) to be reduced to
//			 * zero(0) even through (c) is on the queue. This is not a legal
//			 * state so we can not continue with operations that would touch the
//			 * queue.
//			 */
//
//			((HardReferenceQueue<PO>) btree.writeRetentionQueue)
//					.getListener()
//					.evicted(
//							((HardReferenceQueue<PO>) btree.writeRetentionQueue),
//							c);
//
//			// verify that c and its children (a,b) are now persistent.
//			assertTrue(c.isPersistent());
//			assertTrue(a.isPersistent());
//			assertTrue(b.isPersistent());
//
//			// verify that we set the identity of (a,b) on their parent (c).
//			assertEquals(a.getIdentity(), c.getChildAddr(0));
//			assertEquals(b.getIdentity(), c.getChildAddr(1));
//
//		} finally {
//
//			store.destroy();
//
//    	}
//        
//    }

}
