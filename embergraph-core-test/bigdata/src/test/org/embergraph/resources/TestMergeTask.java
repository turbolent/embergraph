/*

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
 * Created on Feb 21, 2008
 */

package org.embergraph.resources;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.embergraph.btree.AbstractBTreeTestCase;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.IndexSegment;
import org.embergraph.btree.IndexSegmentStore;
import org.embergraph.btree.keys.TestKeyBuilder;
import org.embergraph.btree.proc.IIndexProcedure;
import org.embergraph.btree.proc.BatchInsert.BatchInsertConstructor;
import org.embergraph.journal.AbstractJournal;
import org.embergraph.journal.ITx;
import org.embergraph.journal.IndexProcedureTask;
import org.embergraph.journal.RegisterIndexTask;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.IndexPartitionCause;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.mdi.MetadataIndex;
import org.embergraph.rawstore.SimpleMemoryRawStore;

/**
 * Basic test of compacting merge for an index partition on overflow.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestMergeTask extends AbstractResourceManagerTestCase {

    /**
     * 
     */
    public TestMergeTask() {
        super();

    }

    /**
     * @param arg0
     */
    public TestMergeTask(String arg0) {
        super(arg0);
    }

    /**
     * Test generates an {@link IndexSegment} from a (typically historical)
     * fused view of an index partition. The resulting {@link IndexSegment} is a
     * complete replacement for the historical view but does not possess any
     * deleted index entries. Typically the {@link IndexSegment} will be used to
     * replace the current index partition definition such that the resources
     * that were the inputs to the view from which the {@link IndexSegment} was
     * built are no longer required to read on that view. This change needs to
     * be recorded in the {@link MetadataIndex} before clients will being
     * reading from the new view using the new {@link IndexSegment}.
     * 
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * 
     * @todo test more complex merges.
     */
    public void test_mergeWithOverflow() throws IOException,
            InterruptedException, ExecutionException {

        /*
         * Register the index.
         */
        final String name = "testIndex";
        final UUID indexUUID = UUID.randomUUID();
        final IndexMetadata indexMetadata = new IndexMetadata(name, indexUUID);
        {

            // must support delete markers
            indexMetadata.setDeleteMarkers(true);

            // must be an index partition.
            indexMetadata.setPartitionMetadata(new LocalPartitionMetadata(
                    0, // partitionId.
                    -1, // not a move.
                    new byte[] {}, // leftSeparator
                    null, // rightSeparator
                    new IResourceMetadata[] {//
                            resourceManager.getLiveJournal().getResourceMetadata(), //
                    }, //
                    IndexPartitionCause.register(resourceManager)
//                    ,"" // history
                    ));

            // submit task to register the index and wait for it to complete.
            concurrencyManager.submit(
                    new RegisterIndexTask(concurrencyManager, name,
                            indexMetadata)).get();

        }

        /*
         * Populate the index with some data.
         */
        final BTree groundTruth = BTree.create(new SimpleMemoryRawStore(),
                new IndexMetadata(indexUUID));
        {

            final int nentries = 10;

            final byte[][] keys = new byte[nentries][];
            final byte[][] vals = new byte[nentries][];

            final Random r = new Random();

            for (int i = 0; i < nentries; i++) {

                keys[i] = TestKeyBuilder.asSortKey(i);

                vals[i] = new byte[4];

                r.nextBytes(vals[i]);

                groundTruth.insert(keys[i], vals[i]);

            }

            final IIndexProcedure proc = BatchInsertConstructor.RETURN_NO_VALUES
                    .newInstance(indexMetadata, 0/* fromIndex */,
                            nentries/*toIndex*/, keys, vals);

            // submit the task and wait for it to complete.
            concurrencyManager.submit(
                    new IndexProcedureTask(concurrencyManager, ITx.UNISOLATED,
                            name, proc)).get();

        }

        /*
         * Force overflow causing an empty btree to be created for that index on
         * a new journal and the view definition in the new btree to be updated.
         */

        // createTime of the old journal.
        final long createTime0 = resourceManager.getLiveJournal()
                .getRootBlockView().getCreateTime();

        // uuid of the old journal.
        final UUID uuid0 = resourceManager.getLiveJournal().getRootBlockView()
                .getUUID();

        // force overflow onto a new journal.
        final OverflowMetadata overflowMetadata = resourceManager
                .doSynchronousOverflow();
        
        // nothing should have been copied to the new journal.
        assertEquals(0, overflowMetadata
                .getActionCount(OverflowActionEnum.Copy));

        // lookup the old journal again using its createTime.
        final AbstractJournal oldJournal = resourceManager
                .getJournal(createTime0);
        assertEquals("uuid", uuid0, oldJournal.getRootBlockView().getUUID());
        assertNotSame("closeTime", 0L, oldJournal.getRootBlockView()
                .getCloseTime());

        // run merge task.
        final BuildResult result;
        {

            /*
             * Note: The task start time is a historical read on the final
             * committed state of the old journal. This means that the generated
             * index segment will have a createTime EQ to the lastCommitTime on
             * the old journal. This also means that it will have been generated
             * from a fused view of all data as of the final commit state of the
             * old journal.
             */
//            final OverflowMetadata omd = new OverflowMetadata(resourceManager);
            
            final ViewMetadata vmd = overflowMetadata.getViewMetadata(name);
            
            // task to run.
            final CompactingMergeTask task = new CompactingMergeTask(vmd);

            try {

                // overflow must be disallowed as a task pre-condition.
                resourceManager.overflowAllowed.compareAndSet(true, false);

                /*
                 * Submit task and await result (metadata describing the new
                 * index segment).
                 */
                result = concurrencyManager.submit(task).get();

            } finally {

                // re-enable overflow processing.
                resourceManager.overflowAllowed.set(true);
                
            }

            final IResourceMetadata segmentMetadata = result.segmentMetadata;

            if (log.isInfoEnabled())
                log.info(segmentMetadata.toString());

            // verify index segment can be opened.
            resourceManager.openStore(segmentMetadata.getUUID());
            
            // Note: this assertion only works if we store the file path vs its basename.
//            assertTrue(new File(segmentMetadata.getFile()).exists());

            // verify createTime == lastCommitTime on the old journal.
            assertEquals("createTime", oldJournal.getRootBlockView()
                    .getLastCommitTime(), segmentMetadata.getCreateTime());

            // verify segment has all data in the groundTruth btree.
            {

                final IndexSegmentStore segStore = (IndexSegmentStore) resourceManager
                        .openStore(segmentMetadata.getUUID());

                final IndexSegment seg = segStore.loadIndexSegment();

                AbstractBTreeTestCase.assertSameBTree(groundTruth, seg);

            }

        }

        /*
         * verify same data from ground truth and the new view (using btree
         * helper classes for this).
         */
        {

            final IIndex actual = resourceManager
                    .getIndex(name, ITx.UNISOLATED);

            AbstractBTreeTestCase.assertSameBTree(groundTruth, actual);

        }

    }

}
