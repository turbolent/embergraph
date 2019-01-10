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
 * Created on Dec 12, 2006
 */

package com.bigdata.btree;

import java.util.UUID;

import com.bigdata.btree.keys.TestKeyBuilder;
import com.bigdata.cache.HardReferenceQueue;
import com.bigdata.rawstore.IRawStore;
import com.bigdata.rawstore.SimpleMemoryRawStore;

/**
 * Test suite for {@link BTree#touch(AbstractNode)}. None of these tests cause
 * an evicted node to be made persistent, but they do verify the correct
 * tracking of the {@link AbstractNode#referenceCount} and the contract for
 * touching a node.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestTouch extends AbstractBTreeTestCase {

    /**
     * 
     */
    public TestTouch() {
    }

    /**
     * @param name
     */
    public TestTouch(String name) {
        super(name);
    }

    /**
     * Test verifies that the reference counter is incremented when a node is
     * appended to the hard reference queue (the scan of the tail of the queue
     * is disabled for this test). Finally, verify that we can force the node to
     * be evicted from the queue but that its non-zero reference counter means
     * that it is not made persistent when it is evicted.
     */
    public void test_touch01() {

        /*
         * setup the btree with a queue having two entries and no scanning. The
         * listener initially disallows any evictions and we have to explicitly
         * notify the listener when it should expect an eviction.
         */
        final int branchingFactor = 3;
        final MyEvictionListener listener = new MyEvictionListener();
        final int queueCapacity = 2;
        final int queueScan = 0;
        final MyHardReferenceQueue<PO> leafQueue = new MyHardReferenceQueue<PO>(
                listener, queueCapacity, queueScan);
        assertEquals(queueCapacity,leafQueue.capacity());
        assertEquals(queueScan,leafQueue.nscan());
        assertEquals(listener,leafQueue.getListener());
        
        /*
         * Setup the B+Tree directly so that we can override the hard reference
         * queue.
         */
        final BTree btree; 
        {
            
            IRawStore store = new SimpleMemoryRawStore();
            
            IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
            
            metadata.setBranchingFactor(branchingFactor);
            
            metadata.write(store);
            
            Checkpoint checkpoint = metadata.firstCheckpoint();
            
            checkpoint.write(store);

            btree = new BTree(store,checkpoint,metadata,false/*readOnly*/) {
              
                @Override
                protected HardReferenceQueue<PO> newWriteRetentionQueue(final boolean readOnly) {
                    
                    return leafQueue;
                    
                }
                
            };
            
        }
        // The btree.
//        final BTree btree = new BTree(
//                new SimpleMemoryRawStore(),
//                branchingFactor,
//                UUID.randomUUID(),
//                false,//isolatable
//                null,//conflictResolver
//                leafQueue,
//                KeyBufferSerializer.INSTANCE,
//                ByteArrayValueSerializer.INSTANCE,
//                null // no record compressor
//                );
        
        /*
         * verify the initial conditions - the root leaf is on the queue and
         * its reference counter is one (1).
         */
        final Leaf a = (Leaf)btree.getRoot();

        assertEquals(1,a.referenceCount);
        
        assertEquals(new PO[]{a}, leafQueue.toArray(new PO[0]));
        
        /*
         * touch the leaf. since we are not scanning the queue, another
         * reference to the leaf will be added to the queue and the reference
         * counter will be incremented.
         */
        btree.touch(a);
        
        assertEquals(2,a.referenceCount);
        
        assertEquals(new PO[]{a,a}, leafQueue.toArray(new PO[0]));
        
        /*
         * touch the leaf. since the queue is at capacity, the leaf is evicted.
         * We verify that leaf has a non-zero reference counter when it is
         * evicted, which means that it will not be made persistent since other
         * references to the leaf remain on the queue.
         */
        
        listener.setExpectedRef(a);
        
        btree.touch(a);
        
        assertEquals(2,a.referenceCount);
        
        assertEquals(new PO[]{a,a}, leafQueue.toArray(new PO[0]));

        assertFalse(a.isPersistent());
        
    }

    /**
     * Test verifies that the reference counter is unchanged across
     * {@link BTree#touch(AbstractNode)} if a node is already on the hard
     * reference queue.
     */
    public void test_touch02() {

        /*
         * setup the btree with a queue having two entries and scanning. The
         * listener initially disallows any evictions and we have to explicitly
         * notify the listener when it should expect an eviction.
         */
        final int branchingFactor = 3;
        final MyEvictionListener listener = new MyEvictionListener();
        final int queueCapacity = 2;
        final int queueScan = 1;
        final MyHardReferenceQueue<PO> leafQueue = new MyHardReferenceQueue<PO>(
                listener, queueCapacity, queueScan);
        assertEquals(queueCapacity,leafQueue.capacity());
        assertEquals(queueScan,leafQueue.nscan());
        assertEquals(listener,leafQueue.getListener());
        
        /*
         * Setup the B+Tree directly so that we can override the hard reference
         * queue.
         */
        final BTree btree; 
        {
            
            IRawStore store = new SimpleMemoryRawStore();
            
            IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
            
            metadata.setBranchingFactor(branchingFactor);
            
            metadata.write(store);
            
            Checkpoint checkpoint = metadata.firstCheckpoint();
            
            checkpoint.write(store);
            
            btree = new BTree(store,checkpoint,metadata, false/*readOnly*/) {
                
                @Override
                protected HardReferenceQueue<PO> newWriteRetentionQueue(final boolean readOnly) {
                    
                    return leafQueue;
                    
                }
                
            };

        }
//        // The btree.
//        final BTree btree = new BTree(
//                new SimpleMemoryRawStore(),
//                branchingFactor,
//                UUID.randomUUID(),
//                false,//isolatable
//                null,//conflictResolver
//                leafQueue,
//                KeyBufferSerializer.INSTANCE,
//                ByteArrayValueSerializer.INSTANCE,
//                null // no record compressor
//        );
        
        /*
         * verify the initial conditions - the root leaf is on the queue and
         * its reference counter is one (1).
         */
        final Leaf a = (Leaf)btree.getRoot();

        assertEquals(1,a.referenceCount);
        
        assertEquals(new PO[]{a}, leafQueue.toArray(new PO[0]));
        
        /*
         * touch the leaf. since we are scanning the queue, this does NOT cause
         * another reference to the leaf to be added to the queue and the
         * reference counter MUST NOT be incremented across the method call.
         * Nothing is evicted and the leaf is not made persistent.
         */
        btree.touch(a);
        
        assertEquals(1,a.referenceCount);
        
        assertEquals(new PO[]{a}, leafQueue.toArray(new PO[0]));

        assertFalse(a.isPersistent());

    }

    /**
     * Test verifies that touching a node when the queue is full and the node is
     * the next reference to be evicted from the queue does NOT cause the node
     * to be made persistent. The test is setup using a queue of capacity one
     * (1) and NO scanning. The root leaf is already on the queue when the btree
     * is created. The test verifies that merely touching the root leaf causes a
     * reference to the leaf to be evicted from the queue, but does NOT cause
     * the leaf to be made persistent. {@link BTree#touch(AbstractNode)} handles
     * this condition by incrementing the reference counter before appending the
     * node to the queue and therefore ensuring that the reference counter for
     * the node that touched is not zero if the node is also selected for
     * eviction. The test also verifies that the reference counter is correctly
     * maintained across the touch. Since the counter was one before the touch
     * and since the root was itself evicted, the counter after the touch is
     * <code>1+1-1 = 1</code>.
     * 
     * FIXME This test needs to use a tree with nodes and leaves or fake another
     * root leaf since the minimum cache size is (2)
     */
    public void test_touch03() {

        /*
         * setup the btree with a queue with surplus capacity and no scanning.
         * The listener initially disallows any evictions and we have to
         * explicitly notify the listener when it should expect an eviction.
         */
        final int branchingFactor = 3;
        final MyEvictionListener listener = new MyEvictionListener();
        final int queueCapacity = 20;
        final int queueScan = 0;
        final MyHardReferenceQueue<PO> leafQueue = new MyHardReferenceQueue<PO>(
                listener, queueCapacity, queueScan);
        assertEquals(queueCapacity,leafQueue.capacity());
        assertEquals(queueScan,leafQueue.nscan());
        assertEquals(listener,leafQueue.getListener());
        
        /*
         * Setup the B+Tree directly so that we can override the hard reference
         * queue.
         */
        final BTree btree; 
        {
            
            IRawStore store = new SimpleMemoryRawStore();
            
            IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
            
            metadata.setBranchingFactor(branchingFactor);
            
            metadata.write(store);
            
            Checkpoint checkpoint = metadata.firstCheckpoint();
            
            checkpoint.write(store);
            
            btree = new BTree(store, checkpoint, metadata, false/* readOnly */) {
                
                @Override
                protected HardReferenceQueue<PO> newWriteRetentionQueue(final boolean readOnly) {
                    
                    return leafQueue;
                    
                }
                
            };
            
        }
//        // The btree.
//        final BTree btree = new BTree(
//                new SimpleMemoryRawStore(),
//                branchingFactor,
//                UUID.randomUUID(),
//                false,//isolatable
//                null,//conflictResolver
//                leafQueue,
//                KeyBufferSerializer.INSTANCE,
//                ByteArrayValueSerializer.INSTANCE,
//                null // no record compressor
//        ); 
        
        /*
         * verify the initial conditions - the root leaf is on the queue and
         * its reference counter is one (1).
         */
        final Leaf a = (Leaf)btree.getRoot();

        assertEquals(1,a.referenceCount);
        
        assertEquals(new PO[]{a}, leafQueue.toArray(new PO[0]));

        assertFalse(a.isPersistent());

        /*
         * insert keys into the root and cause it to split.
         */
        final SimpleEntry v3 = new SimpleEntry(3);
        final SimpleEntry v5 = new SimpleEntry(5);
        final SimpleEntry v7 = new SimpleEntry(7);
        final SimpleEntry v9 = new SimpleEntry(9);
        btree.insert(TestKeyBuilder.asSortKey(3),v3);
        btree.insert(TestKeyBuilder.asSortKey(5),v5);
        btree.insert(TestKeyBuilder.asSortKey(7),v7);
        btree.insert(TestKeyBuilder.asSortKey(9),v9);
        assertNotSame(a,btree.getRoot());
        final Node c = (Node) btree.getRoot();
        assertKeys(new int[]{7},c);
        assertEquals(a,c.getChild(0));
        final Leaf b = (Leaf) c.getChild(1);
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5}, a);
        assertKeys(new int[]{7,9},b);
        assertValues(new Object[]{v7,v9}, b);

        /*
         * bring the queue up to its capacity without causing it to overflow.
         */
        while(leafQueue.size()<leafQueue.capacity()) {
            
            // touch a node - which one does not really matter.
            btree.touch(a);
            
        }
        
        /*
         * examine the queue state and figure out which node or leaf we want to
         * evict. we continue to append a specific node (a) until the reference
         * that would be evicted next has a reference count of one (1). it does
         * not matter which node this is. It will be either (b) or (c) depending
         * on the code paths when we setup the test tree.
         */

        AbstractNode ref;
        
        while(true) {

            ref = (AbstractNode) leafQueue.peek();

            if(ref.referenceCount == 1 ) break;
            
            listener.setExpectedRef(ref);
            
            btree.touch(a);

        } 

        /*
         * touch the node or leaf that is poised for eviction from the queue and
         * which would be made immutable if it were evicted since its
         * pre-eviction reference count is one (1). since we are not scanning
         * the queue, another reference to the node or leaf will be added to the
         * queue. Since the queue is at capacity, the reference on the queue
         * will be evicted. However, since the reference counter is non-zero in
         * the eviction handler, the leaf will not be made persistent.
         */
        
        assertEquals(1,ref.referenceCount);
        
        listener.setExpectedRef(ref);
        
        btree.touch(ref);
        
        assertEquals(1,ref.referenceCount);
        
        assertFalse(ref.isPersistent());
        
    }

}
