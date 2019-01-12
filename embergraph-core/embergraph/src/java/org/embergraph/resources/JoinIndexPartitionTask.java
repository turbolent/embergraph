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
 * Created on Feb 29, 2008
 */

package org.embergraph.resources;

import java.util.Arrays;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.journal.AbstractTask;
import org.embergraph.journal.ITx;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.IndexPartitionCause;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.mdi.MetadataIndex;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.service.DataService;
import org.embergraph.service.Event;
import org.embergraph.service.EventResource;
import org.embergraph.util.BytesUtil;

/**
 * Task joins one or more index partitions and should be invoked when their is strong evidence that
 * the index partitions have shrunk enough to warrant their being combined into a single index
 * partition. The index partitions MUST be partitions of the same scale-out index, MUST be siblings
 * (their left and right separators must cover a continuous interval), and MUST reside on the same
 * {@link DataService}.
 *
 * <p>The task reads from the lastCommitTime of the old journal and builds a single {@link BTree}
 * from the merged read of the source index partitions as of that timestamp and returns a {@link
 * JoinResult}. The task automatically submits, and awaits the completion of, an {@link
 * AtomicUpdateJoinIndexPartition}, which performs the atomic update of the view definitions on the
 * live journal and the {@link MetadataIndex}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class JoinIndexPartitionTask extends AbstractPrepareTask<JoinResult> {

  protected final ViewMetadata[] vmd;

  /**
   * @param resourceManager
   * @param lastCommitTime
   * @param resource The names of the index partitions to be joined. These names MUST be given in
   *     natural ordering of the left separator keys for those index partitions.
   */
  protected JoinIndexPartitionTask(
      ResourceManager resourceManager,
      long lastCommitTime,
      String[] resources,
      ViewMetadata[] vmd) {

    super(resourceManager, TimestampUtility.asHistoricalRead(lastCommitTime), resources);

    if (vmd == null) throw new IllegalArgumentException();

    if (vmd.length != resources.length) throw new IllegalArgumentException();

    this.vmd = vmd;
  }

  @Override
  protected void clearRefs() {

    for (ViewMetadata t : vmd) {

      t.clearRef();
    }
  }

  /** FIXME Improve error handling for this task. See the build and split tasks for examples. */
  @Override
  protected JoinResult doTask() throws Exception {

    final Event e =
        new Event(
                resourceManager.getFederation(),
                // Note: using the leftSibling for the event resource.
                new EventResource(vmd[0].indexMetadata),
                OverflowActionEnum.Join)
            .start();

    e.addDetail("summary", OverflowActionEnum.Join + "(" + Arrays.toString(getResource()) + ")");

    try {

      final long checkpointAddr;
      final JoinResult result;
      final String summary;

      try {

        if (resourceManager.isOverflowAllowed()) throw new IllegalStateException();

        final String[] resources = getResource();

        // _clone_ the index metadata for the first of the siblings.
        final IndexMetadata newMetadata = getIndex(resources[0]).getIndexMetadata().clone();

        if (newMetadata.getPartitionMetadata() == null) {

          throw new RuntimeException("Not an index partition: " + resources[0]);
        }

        if (newMetadata.getPartitionMetadata().getSourcePartitionId() != -1) {

          throw new IllegalStateException(
              "Join not allowed during move: sourcePartitionId="
                  + newMetadata.getPartitionMetadata().getSourcePartitionId());
        }

        /*
         * Make a note of the expected left separator for the next
         * partition. The first time through the loop this is just the
         * left separator for the 1st index partition that to be joined.
         *
         * Note: Do this _before_ we clear the partition metadata.
         */
        byte[] leftSeparator = newMetadata.getPartitionMetadata().getLeftSeparatorKey();

        /*
         * clear the partition metadata before we create the index so
         * that it will not report range check errors on the data that
         * we copy in.
         */
        newMetadata.setPartitionMetadata(null);

        /*
         * Create B+Tree on which all data will be merged. This B+Tree
         * is created on the _live_ journal. It will be inaccessible to
         * anyone until it is registered. Until then we will just pass
         * along the checkpoint address (obtained below).
         *
         * Note: the lower 32-bits of the counter will be zero. The high
         * 32-bits will be the partition identifier assigned to the new
         * index partition.
         */
        final BTree btree = BTree.create(resourceManager.getLiveJournal(), newMetadata);

        // the partition metadata for each partition that is being
        // merged.
        final LocalPartitionMetadata[] oldpmd = new LocalPartitionMetadata[resources.length];

        // consider each resource in order.
        for (int i = 0; i < resources.length; i++) {

          final String name = resources[i];

          final IIndex src = getIndex(name);

          /*
           * Validate partition of same index
           */

          final IndexMetadata sourceIndexMetadata = src.getIndexMetadata();

          if (!newMetadata.getIndexUUID().equals(sourceIndexMetadata.getIndexUUID())) {

            throw new RuntimeException(
                "Partition for the wrong index? : names=" + Arrays.toString(resources));
          }

          final LocalPartitionMetadata pmd = sourceIndexMetadata.getPartitionMetadata();

          if (pmd == null) {

            throw new RuntimeException("Not an index partition: " + resources[i]);
          }

          /*
           * Validate that this is a rightSibling by checking the left
           * separator of the index partition to be joined against the
           * expected left separator.
           */
          if (!BytesUtil.bytesEqual(leftSeparator, pmd.getLeftSeparatorKey())) {

            throw new RuntimeException(
                "Partitions out of order: names="
                    + Arrays.toString(resources)
                    + ", have="
                    + Arrays.toString(oldpmd)
                    + ", found="
                    + pmd);
          }

          oldpmd[i] = pmd;

          /*
           * Copy all data into the new btree. Since we are copying
           * from the old journal onto the new journal [overflow :=
           * true] so that any referenced raw records are copied as
           * well.
           */

          final long ncopied = btree.rangeCopy(src, null, null, true /* overflow */);

          if (INFO) log.info("Copied " + ncopied + " index entries from " + name);

          // the new left separator.
          leftSeparator = pmd.getRightSeparatorKey();
        }

        /*
         * Set index partition.
         *
         * Note: A new index partitionId is assigned by the metadata
         * server.
         *
         * Note: The leftSeparator is the leftSeparator of the first
         * joined index partition and the rightSeparator is the
         * rightSeparator of the last joined index partition.
         *
         * Note: All data for the new index partition is in this B+Tree
         * which we just created. Therefore only the journal itself gets
         * listed as a resource for the index partition view.
         */

        final String scaleOutIndexName = newMetadata.getName();

        final int partitionId =
            resourceManager.getFederation().getMetadataService().nextPartitionId(scaleOutIndexName);

        // used for the history and also for event reporting.
        summary =
            OverflowActionEnum.Join + "(" + Arrays.toString(resources) + "->" + partitionId + ")";

        newMetadata.setPartitionMetadata(
            new LocalPartitionMetadata(
                partitionId,
                -1, // Note: join not allowed during move.
                oldpmd[0].getLeftSeparatorKey(),
                oldpmd[resources.length - 1].getRightSeparatorKey(),
                new IResourceMetadata[] {
                  // Note: the live journal.
                  getJournal().getResourceMetadata()
                },
                IndexPartitionCause.join(resourceManager)
                //                        // new history line.
                //                        , summary+" "
                ));

        /*
         * Set the updated index metadata on the btree (required for it
         * to be available on reload).
         */

        btree.setIndexMetadata(newMetadata.clone());

        /*
         * Explicitly checkpoint the B+Tree.
         *
         * Note: The atomic update task will re-load the BTree from this
         * checkpoint address. This is necessary since the BTree is NOT
         * been registered under a name on the journal yet.
         */
        checkpointAddr = btree.writeCheckpoint();

        result =
            new JoinResult(
                DataService.getIndexPartitionName(scaleOutIndexName, partitionId),
                newMetadata,
                checkpointAddr,
                resources);

      } finally {

        /*
         * Now that the JOIN is done we can clear our references for the
         * source index partitions views.
         */

        clearRefs();
      }

      {

        /*
         * The array of index names on which we will need an exclusive
         * lock.
         *
         * Note: We pass in the name of the new index partition (while
         * this has not been registered yet we MUST name it as a
         * resource for the lock manager to ensure that we are holding a
         * lock on it once it IS registered otherwise a task could see
         * the MDI update and obtain concurrent access to the unisolated
         * index) and the names of the old index partitions. We will
         * need an exclusive lock on all of those resources so that we
         * can register the former and drop the latter in an atomic
         * operation. We will update the metadata index within that
         * atomic operation so that the total change over is atomic.
         */
        final String[] names2 = new String[result.oldnames.length + 1];

        names2[0] = result.name;

        System.arraycopy(result.oldnames, 0, names2, 1, result.oldnames.length);

        /*
         * The task to make the atomic updates on the live journal and the
         * metadata index.
         */
        final AbstractTask<Void> task =
            new AtomicUpdateJoinIndexPartition(
                resourceManager, names2, result, e.newSubEvent(OverflowSubtaskEnum.AtomicUpdate));

        // submit task and wait for it to complete
        concurrencyManager.submit(task).get();
      }

      return result;

    } finally {

      e.end();
    }
  }

  /**
   * Task performs an atomic update of the index partition view definitions on the live journal and
   * the {@link MetadataIndex}, thereby putting into effect the changes made by a {@link
   * JoinIndexPartitionTask}.
   *
   * <p>This task obtains an exclusive lock on the new index partition and on all of the index
   * partitons on the live journal that are being joined. It then copies all writes absorbed by the
   * index partitions that are being since the overflow onto the new index partition and atomically
   * (a) drops the old index partitions; (b) registers the new index partition; and (c) updates the
   * metadata index to reflect the join.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  protected static class AtomicUpdateJoinIndexPartition extends AbstractAtomicUpdateTask<Void> {

    private final JoinResult result;

    private final Event updateEvent;

    /**
     * @param resourceManager
     * @param startTime
     * @param resource All resources (both the new index partition arising from the join and the old
     *     index partitions which have continued to receive writes that need to be copied into the
     *     new index partition and then dropped).
     * @param result
     */
    public AtomicUpdateJoinIndexPartition(
        ResourceManager resourceManager, String[] resource, JoinResult result, Event updateEvent) {

      super(resourceManager, ITx.UNISOLATED, resource);

      if (result == null) throw new IllegalArgumentException();

      if (updateEvent == null) throw new IllegalArgumentException();

      this.result = result;

      this.updateEvent = updateEvent;
    }

    @Override
    protected Void doTask() throws Exception {

      updateEvent.start();

      try {

        if (resourceManager.isOverflowAllowed()) throw new IllegalStateException();

        /*
         * Load the btree from the live journal that already contains
         * all data from the source index partitions to the merge as of
         * the lastCommitTime of the old journal.
         *
         * Note: At this point the btree exists on the journal but has
         * not been registered and is therefore NOT visible to
         * concurrent tasks.
         *
         * In order to make this btree complete we will now copy in any
         * writes absorbed by those index partitions now that we have an
         * exclusive lock on everyone on the new journal.
         */
        final BTree btree =
            BTree.load(
                resourceManager.getLiveJournal(), result.checkpointAddr, false /* readOnly */);
        // resourceManager.getLiveJournal().getIndex(result.checkpointAddr);

        assert btree != null;

        assert !btree.isReadOnly();

        final String scaleOutIndexName = btree.getIndexMetadata().getName();

        final int njoined = result.oldnames.length;

        final PartitionLocator[] oldLocators = new PartitionLocator[njoined];

        for (int i = 0; i < njoined; i++) {

          final String name = result.oldnames[i];

          final IIndex src = getIndex(name);

          assert src != null;

          // same scale-out index.
          if (!btree
              .getIndexMetadata()
              .getIndexUUID()
              .equals(src.getIndexMetadata().getIndexUUID())) {

            throw new AssertionError();
          }

          final LocalPartitionMetadata pmd = src.getIndexMetadata().getPartitionMetadata();

          oldLocators[i] =
              new PartitionLocator(
                  pmd.getPartitionId(),
                  resourceManager.getDataServiceUUID(),
                  pmd.getLeftSeparatorKey(),
                  pmd.getRightSeparatorKey());

          /*
           * Copy in all data.
           *
           * Note: [overflow := false] since the btrees are on the
           * same backing store.
           */
          btree.rangeCopy(src, null, null, false /* overflow */);

          // drop the old index partition
          getJournal().dropIndex(name);
        }

        // register the new index partition
        getJournal().registerIndex(result.name, btree);

        final LocalPartitionMetadata pmd = btree.getIndexMetadata().getPartitionMetadata();

        assert pmd != null;

        final PartitionLocator newLocator =
            new PartitionLocator(
                pmd.getPartitionId(),
                resourceManager.getDataServiceUUID(),
                pmd.getLeftSeparatorKey(),
                pmd.getRightSeparatorKey());

        resourceManager
            .getFederation()
            .getMetadataService()
            .joinIndexPartition(scaleOutIndexName, oldLocators, newLocator);

        for (String name : result.oldnames) {

          // will notify tasks that the index partition was joined.
          resourceManager.setIndexPartitionGone(name, StaleLocatorReason.Join);
        }

        // notify successful index partition join.
        resourceManager.overflowCounters.indexPartitionJoinCounter.incrementAndGet();

        return null;

      } finally {

        updateEvent.end();
      }
    } // doTask()
  } // class AtomicUpdate
}
