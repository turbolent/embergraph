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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.IndexSegment;
import org.embergraph.btree.IndexSegmentStore;
import org.embergraph.btree.proc.IIndexProcedure;
import org.embergraph.journal.AbstractTask;
import org.embergraph.journal.ConcurrencyManager;
import org.embergraph.journal.ITx;
import org.embergraph.journal.NoSuchIndexException;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.IndexPartitionCause;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.mdi.MetadataIndex;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.mdi.SegmentMetadata;
import org.embergraph.service.DataService;
import org.embergraph.service.DataServiceCallable;
import org.embergraph.service.Event;
import org.embergraph.service.EventResource;
import org.embergraph.service.IDataService;
import org.embergraph.service.IMetadataService;
import org.embergraph.service.MetadataService;
import org.embergraph.service.ResourceService;

/**
 * Task moves an index partition to another {@link IDataService}.
 * <p>
 * This task runs as a historical read operation and copy the view of the index
 * partition as of the lastCommitTime of old journal to another
 * {@link IDataService}. Once that historical view has been copied, this task
 * then submits an {@link AtomicUpdateMoveIndexPartitionTask}. The atomic
 * update is an {@link ITx#UNISOLATED} operation. It is responsible copying any
 * writes buffered for the index partition on the live journal to the target
 * {@link IDataService} and then updating the {@link MetadataIndex}. Once the
 * atomic update task is finished, clients will discover that the source index
 * partition does not exist. When they query the {@link MetadataService} they
 * will discover that the key(-range) is now handled by the new index partition
 * on the target {@link IDataService}.
 * <p>
 * Note: This task is run on the target {@link IDataService} and it copies the
 * data from the source {@link IDataService}. This allows us to use standard
 * {@link IRangeQuery} operations to copy the historical view. However, the
 * {@link AtomicUpdateMoveIndexPartitionTask} is run on the source
 * {@link IDataService} since it needs to obtain an exclusive lock on the index
 * partition that is being moved in order to prevent concurrent writes during
 * the atomic cutover. For the same reason, the
 * {@link AtomicUpdateMoveIndexPartitionTask} can not use standard
 * {@link IRangeQuery} operations. Instead, it initiates a series of data
 * transfers while holding onto the exclusive lock until the target
 * {@link IDataService} has the current state of the index partition. At that
 * point it notifies the {@link IMetadataService} to perform the atomic cutover
 * to the new index partition.
 * <p>
 * Note: This task does NOT cause any resources associated with the current view
 * of the index partition to be released on the source {@link IDataService}.
 * The reason is two-fold. First, the {@link IndexSegment}(s) associated with
 * that view MAY be in used by historical views. Second, there MAY be historical
 * commit points for the index partition on the live journal before the atomic
 * cutover to the new {@link IDataService} - those historical commit points MUST
 * be preserved until the release policy for those views has been satisfied.
 * <p>
 * Note: The MOVE task MUST be explicitly coordinated with the target
 * {@link IDataService}. Failure to coordinate the move results in an error
 * message reported by the {@link MetadataService} indicating that the wrong
 * partition locator was found under the key. The cause is a MOVE operation
 * during which the target data service undergoes concurrent synchronous (and
 * then asynchronous) overflow. What happens is the {@link MoveTask} registers
 * the new index partition on the target data service. One registered on the
 * {@link IDataService}, the index partition it is visible during synchronous
 * overflow BEFORE the MOVE is complete and BEFORE the index is registered with
 * the {@link MetadataService} and hence discoverable to clients. If the target
 * {@link IDataService} then undergoes synchronous and asynchronous overflow and
 * chooses an action which would change the index partition definition (split,
 * join, or move) WHILE the index partition is still being moved onto the target
 * {@link IDataService} THEN the MOVE is not atomic and the definition of the
 * index partition in the {@link MetadataService} will not coherently reflect
 * either the MOVE or the action chosen by the target {@link IDataService},
 * depending on which one makes its atomic update first.
 * <p>
 * The target {@link IDataService} MAY undergo both synchronous and asynchronous
 * overflow as {@link IDataService}s are designed to allow continued writes
 * during those operations. Further, it MAY choose to copy, build, or compact
 * the index partition while it is being moved. However, it MUST NOT choose any
 * action (split, join, or move) that would change the index partition
 * definition until the move is complete (whether it ends in success or
 * failure).
 * <p>
 * This issue is addressed by the following protocol:
 * <ol>
 * 
 * <li>The {@link MoveTask} set the <code>sourcePartitionId</code> on the
 * {@link LocalPartitionMetadata} when it registers the index partition on the
 * target {@link IDataService}. When <code>sourcePartitionId != -1</code>.
 * the target {@link IDataService} is restricted to for that index partition to
 * overflows actions which do not change the index partition definition (copy,
 * build, or merge). Further, any index partition found on restart whose by the
 * target {@link IDataService} whose <code>sourcePartitionId != -1</code> is
 * deleted as it was never successfully put into play (this prevents partial
 * moves from accumulating state which could not otherwise be released.)</li>
 * 
 * <li>The atomic update task causes the <code>sourcePartitionId</code> to be
 * set to <code>-1</code> as one of its last actions, thereby allowing the
 * target {@link IDataService} to use operations that could re-define the index
 * partition (split, join, move) and also preventing the target index partition
 * from being deleted on restart. </li>
 * 
 * </ol>
 * 
 * FIXME javadoc
 * <p>
 * Note: There are only two entry points: a simple move and a move where the
 * compacting merge has already been performed, e.g., by a split, and we just
 * need to do the atomic update phase.  
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class MoveTask extends AbstractPrepareTask<MoveResult> {
    
    private final ViewMetadata vmd;
    
    /**
     * {@link UUID} of the target {@link IDataService} (the one to which the index
     * partition will be moved).
     */
    private final UUID targetDataServiceUUID;

    /**
     * The partition identifier for the target index partition that will be
     * created by the move (RMI).
     */
    final int newPartitionId;

    /**
     * The name of the new index partition on the target data service.
     */
    final String targetIndexName; 
    
    /**
     * The summary used for the event description and the partition history
     * record.
     */
    private final String summary;
    
    /**
     * The event corresponding to this action.
     */
    private final Event e;
    
    /**
     * @param vmd
     *            Metadata for the source index partition view.
     * @param targetDataServiceUUID
     *            The UUID for the target data service.
     */
    public MoveTask(//
            final ViewMetadata vmd,//
            final UUID targetDataServiceUUID//
            ) {
        
        super(vmd.resourceManager, TimestampUtility
                .asHistoricalRead(vmd.commitTime), vmd.name);

        if (targetDataServiceUUID == null)
            throw new IllegalArgumentException();

        if (resourceManager.getDataServiceUUID().equals(targetDataServiceUUID)) {

            throw new IllegalArgumentException("Same data service: "
                    + targetDataServiceUUID);

        }

        this.vmd = vmd;

        this.targetDataServiceUUID = targetDataServiceUUID;

        this.newPartitionId = resourceManager.nextPartitionId(vmd.indexMetadata
                .getName());

        this.targetIndexName = DataService.getIndexPartitionName(
                vmd.indexMetadata.getName(), newPartitionId);

        this.summary = OverflowActionEnum.Move + "(" + vmd.name + "->"
                + targetIndexName + ")";

        final Map<String, Object> params = vmd.getParams();

        params.put("summary", summary);

        this.e = new Event(resourceManager.getFederation(), new EventResource(
                vmd.indexMetadata), OverflowActionEnum.Move, params);

    }

    @Override
    protected void clearRefs() {
        
        vmd.clearRef();
        
    }
    
    /**
     * Builds a compact index segment from the historical view as of the last
     * commit time on the old journal and then submits an atomic update
     * operation to move the source index partition to the target data service.
     * 
     * @return A {@link MoveResult} describing the move operation (this is
     *         returned mainly for historical reasons).
     */
    @Override
    protected MoveResult doTask() throws Exception {

        e.start();

        BuildResult historicalWritesBuildResult = null;
        try {

            if (resourceManager.isOverflowAllowed())
                throw new IllegalStateException();
            
            try {

                // view of the source index partition.
                final ILocalBTreeView src = getIndex(vmd.name);
 
                /*
                 * Do a compacting merge of the historical view in order to
                 * obtain a dense index segment.
                 * 
                 * Note: This will also apply the overflow handler, so any blobs
                 * managed by the index partition will be found in the blobs
                 * region of the generated index segment.
                 */

                historicalWritesBuildResult = resourceManager
                        .buildIndexSegment(vmd.name, src,
                                true/* compactingMerge */, vmd.commitTime,
                                null/* fromKey */, null/* toKey */, e);

                if (INFO)
                    log
                            .info("Generated compact index segment from historical view: "
                                    + historicalWritesBuildResult);
                
            } finally {

                /*
                 * While we still need to copy the buffered writes on the live
                 * journal to the target index partition, at this point we no
                 * longer require the source index partition view (the view on
                 * the old journal) so we clear our references for that index.
                 */

                clearRefs();

            }

            /*
             * Atomic move of the index partition.
             */

            final MoveResult moveResult = doAtomicUpdate(resourceManager,
                    vmd.name, historicalWritesBuildResult,
                    targetDataServiceUUID, newPartitionId, e);

            if (INFO)
                log.info("Successfully moved index partition: " + summary);

            return moveResult;

        } finally {

            if (historicalWritesBuildResult != null) {

                /*
                 * At this point the index segment was either MOVEd to the
                 * target data service or there was an error. Either way, we now
                 * remove the index segment store's UUID from the retentionSet
                 * so it will be subject to the release policy of the
                 * StoreManager.
                 */
                resourceManager
                        .retentionSetRemove(historicalWritesBuildResult.segmentMetadata
                                .getUUID());

                /*
                 * Delete the index segment since it is no longer required and
                 * was not incorporated into a view used by this data service.
                 */
                resourceManager.deleteResource(
                        historicalWritesBuildResult.segmentMetadata.getUUID(),
                        false/* isJournal */);

            }
            
            e.end();

        }
        
    }

    /**
     * Submits an {@link AtomicUpdate} and awaits and returns its outcome.
     * 
     * @param resourceManager
     *            The resource manager.
     * @param sourceIndexName
     *            The name of the source index partition.
     * @param historicalWritesBuildResult
     *            An index segment containing all data for the source view as of
     *            the last commit time on the old journal. This index segment
     *            should be generated by a compacting merge or by an index
     *            partition split with the same semantics so that we will move
     *            the minimum amount of data.
     * @param targetDataServiceUUID
     *            The {@link UUID} of the target data service.
     * @param targetIndexPartitionId
     *            The partition identifier assigned to the target index
     *            partition.
     * @param parentEvent
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    static protected MoveResult doAtomicUpdate(
            final ResourceManager resourceManager,//
            final String sourceIndexName,//
            final BuildResult historicalWritesBuildResult,//
            final UUID targetDataServiceUUID, //
            final int targetIndexPartitionId,//
            final Event parentEvent//
            ) throws InterruptedException, ExecutionException {

        return resourceManager.getConcurrencyManager().submit(
                new AtomicUpdate(resourceManager, sourceIndexName,
                        historicalWritesBuildResult, targetDataServiceUUID,
                        targetIndexPartitionId, parentEvent)).get();

    }
    
    /**
     * Moves an index partition from this data service to another data service.
     * <p>
     * This is an "atomic update" operation. It moves an index segment (supplied
     * by the caller) containing the historical view of the source index
     * partition and generates and moves an index segment containing any
     * buffered writes on the live journal for the source index partition to the
     * target data service. Once the target index partition is registered on the
     * target data service and the {@link IMetadataService} has been updated to
     * reflect the move, this task updates the stale locator cache. At that
     * point clients addressing tasks to the source index partition will
     * discover that it has been moved.
     * <p>
     * Note: If the operation fails, then it has no side-effects but the caller
     * is responsible for deleting the <i>historicalWritesBuildResult</i> iff
     * that is deemed necessary (that is, if it is not in use then either put it
     * to use or delete it -- an attractive alternative is to incorporate it
     * into the source index partition view instead.)
     * <p>
     * Tasks executing after this one will discover that the source index
     * partition no longer exists as of the timestamp when this task commits.
     * Clients that submit tasks for the source index partition will be notified
     * that it no longer exists. When the client queries the
     * {@link MetadataService} it will discover that the key range has been
     * assigned to a new index partition - the one on the target data service.
     * <p>
     * Note: This task runs as an {@link ITx#UNISOLATED} operation since it MUST
     * have an exclusive lock in order to ensure that the buffered writes are
     * transferred to the target index partition without allowing concurrent
     * writes on the source index partition.
     * <p>
     * Note: I have placed the "receive" of the historical index partition view
     * within the atomic update task deliberately. It should add at most a few
     * seconds to the execution time of that task and makes it easier to write
     * corrective actions for the atomic update since we can offer a guarantees
     * such that the existence of the target index partition on the target data
     * service is sufficient to determine that the entire operation was
     * successful.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * 
     * @todo optimization to NOT send an empty index segment if there are no
     *       buffered writes on the live journal.
     */
    protected static class AtomicUpdate extends AbstractAtomicUpdateTask<MoveResult> {
        
        private final ResourceManager resourceManager;
        private final String sourceIndexName;
        private final BuildResult historicalWritesBuildResult;
        private final UUID targetDataServiceUUID;
        private final int targetIndexPartitionId;
        private final Event parentEvent;
//        private final InetAddress thisInetAddr;
        
        /**
         * 
         * @param resourceManager
         *            The resource manager.
         * @param sourceIndexName
         *            The name of the source index partition.
         * @param historicalWritesBuildResult
         *            An index segment containing all data for the source view
         *            as of the last commit time on the old journal. This index
         *            segment should be generated by a compacting merge or by an
         *            index partition split with the same semantics so that we
         *            will move the minimum amount of data.
         * @param targetDataServiceUUID
         *            The {@link UUID} of the target data service.
         * @param targetIndexPartitionId
         *            The partition identifier assigned to the target index
         *            partition.
         * @param parentEvent
         */
        protected AtomicUpdate(//
                final ResourceManager resourceManager,//
                final String sourceIndexName,//
                final BuildResult historicalWritesBuildResult,//
                final UUID targetDataServiceUUID,//
                final int targetIndexPartitionId,//
                final Event parentEvent//
                ) {

            super(resourceManager, ITx.UNISOLATED, sourceIndexName);

            if (historicalWritesBuildResult == null)
                throw new IllegalArgumentException();

            if (targetDataServiceUUID == null)
                throw new IllegalArgumentException();

            if (parentEvent == null)
                throw new IllegalArgumentException();

            this.resourceManager = resourceManager;
            this.sourceIndexName = sourceIndexName;
            this.historicalWritesBuildResult = historicalWritesBuildResult;
            this.targetDataServiceUUID = targetDataServiceUUID;
            this.targetIndexPartitionId = targetIndexPartitionId;
            this.parentEvent = parentEvent;

//            try {
//                this.thisInetAddr = InetAddress.getByName(NicUtil.getIpAddress("default.nic", "default", false));
//            } catch(Throwable t) {
//                throw new IllegalArgumentException(t.getMessage(), t);
//            }
        }

        /**
         * Atomic update (move).
         * 
         * @return A {@link MoveResult} describing the operation (this is
         *         returned mainly for historical reasons).
         */
        public MoveResult doTask() throws Exception {

            final Event e = parentEvent.newSubEvent(
                    OverflowSubtaskEnum.AtomicUpdate).start();
            
            BuildResult bufferedWritesBuildResult = null;
            try {

                // Unisolated view of the source index partition.
                final BTree src = getIndex(getOnlyResource()).getMutableBTree();

                // The current index metadata record.
                final IndexMetadata indexMetadata = src.getIndexMetadata();

                // The name of the scale-out index whose index partition is
                // being moved.
                final String scaleOutIndexName = indexMetadata.getName();

                // The name of the target index partition.
                final String targetIndexName = DataService
                        .getIndexPartitionName(scaleOutIndexName,
                                targetIndexPartitionId);

                // The current metadata for the source index partition view.
                final LocalPartitionMetadata pmd = indexMetadata
                        .getPartitionMetadata();

                // The current locator for the source index partition.
                final PartitionLocator oldLocator = new PartitionLocator(//
                        pmd.getPartitionId(),//
                        resourceManager.getDataServiceUUID(),//
                        pmd.getLeftSeparatorKey(),//
                        pmd.getRightSeparatorKey()//
                );

                // The locator for the target index partition.
                final PartitionLocator newLocator = new PartitionLocator(
                        targetIndexPartitionId,//
                        targetDataServiceUUID,//
                        pmd.getLeftSeparatorKey(),//
                        pmd.getRightSeparatorKey()//
                );

                /*
                 * Build an index segment from the buffered writes on the live
                 * journal for the source index partition.
                 * 
                 * Note: DO NOT specify a compacting merge. That presumes that
                 * the entire view is being processed so that deleted tuples may
                 * be removed from the view. That is NOT the case here. There
                 * MAY be deleted tuples in the buffered writes and those MUST
                 * be included in the generated index segment so that the total
                 * view when reconstructed on the target data service will still
                 * report that those tuples are deleted. If you do not do this
                 * then a historical tuple could "reappear" after the move.
                 * 
                 * Note: The [createTime] for the generated index segment store
                 * will reflect the commit point for the last buffered write on
                 * the source index partition.
                 */

                final long sourceCommitTime = src.getLastCommitTime();

                bufferedWritesBuildResult = resourceManager.buildIndexSegment(
                        sourceIndexName, src, false/* compactingMerge */,
                        sourceCommitTime, null/* fromKey */, null/* toKey */,
                        parentEvent);

                {

                    final IDataService targetDataService = resourceManager
                            .getFederation().getDataService(
                                    targetDataServiceUUID);

                    if (targetDataService == null)
                        throw new Exception("No such data service: "
                                + targetDataServiceUUID);

                    /*
                     * Submit task to the target data service that will copy the
                     * index segment store resources onto that data service and
                     * register the target index partition using the given
                     * IndexMetadata and the copied index segment store files.
                     */
                    {

                        final Event receiveIndexPartitionEvent = parentEvent
                                .newSubEvent(
                                        OverflowSubtaskEnum.ReceiveIndexPartition)
                                .start();

                        try {

                            targetDataService
                                    .submit(
                                            new ReceiveIndexPartitionTask(
                                                    indexMetadata,//
                                                    resourceManager
                                                            .getDataServiceUUID(),//
                                                    targetIndexPartitionId,//
                                                    historicalWritesBuildResult.segmentMetadata,//
                                                    bufferedWritesBuildResult.segmentMetadata,//
                                                    resourceManager
                                                            .getResourceService().getAddr()//
                                            )).get();

                        } catch (ExecutionException ex) {

                            // The task failed.
                            rollbackMove(ex, scaleOutIndexName,
                                    targetIndexName, targetDataService,
                                    oldLocator, newLocator);

                        } catch (InterruptedException ex) {

                            // Task was interrupted.
                            rollbackMove(ex, scaleOutIndexName,
                                    targetIndexName, targetDataService,
                                    oldLocator, newLocator);

                        } catch (IOException ex) {

                            // RMI failure submitting task or obtain its
                            // outcome.
                            rollbackMove(ex, scaleOutIndexName,
                                    targetIndexName, targetDataService,
                                    oldLocator, newLocator);

                        } finally {

                            receiveIndexPartitionEvent.end();

                        }

                    }

                    /*
                     * The source index partition has been moved. All we need to
                     * do is drop the source index partition and notify clients
                     * that their locators for that key range are stale.
                     */

                    /*
                     * The index manager will notify tasks that index partition
                     * has moved.
                     * 
                     * Note: At this point, if the commit for this task fails,
                     * then clients will still be notified that the source index
                     * partition was moved. That is Ok since it WAS moved.
                     */
                    resourceManager.setIndexPartitionGone(getOnlyResource(),
                            StaleLocatorReason.Move);

                    /*
                     * Drop the old index partition.
                     * 
                     * Note: This action is rolled back automatically if this
                     * task fails. The consequence of this here is that the
                     * source index partition will remain registered on this
                     * data service. However, clients are being redirected
                     * (using stale locator exceptions) to the target data
                     * service and the metadata index will direct new requests
                     * to the target data service as well. So the consequence of
                     * failure here is that the source index partition becomes a
                     * zombie. It will remain on this data service forever
                     * unless someone explicitly drops it.
                     */
                    getJournal().dropIndex(getOnlyResource());

                    // notify successful index partition move.
                    resourceManager.overflowCounters.indexPartitionMoveCounter.incrementAndGet();

                }
                
                return new MoveResult(scaleOutIndexName,
                        src.getIndexMetadata(), targetDataServiceUUID,
                        targetIndexPartitionId, oldLocator, newLocator);

            } finally {

                if (bufferedWritesBuildResult != null) {

                    /*
                     * At this point the index segment was either MOVEd to the
                     * target data service or there was an error. Either way, we
                     * now remove the index segment store's UUID from the
                     * retentionSet so it will be subject to the release policy
                     * of the StoreManager.
                     */
                    resourceManager
                            .retentionSetRemove(bufferedWritesBuildResult.segmentMetadata
                                    .getUUID());

                    /*
                     * Delete the index segment containing the buffer writes
                     * since it no longer required by this data service.
                     */
                    resourceManager
                            .deleteResource(
                                    bufferedWritesBuildResult.segmentMetadata
                                            .getUUID(), false/* isJournal */);

                }

                e.end();

            }
            
        }

        /**
         * Invoked to rollback a partial move operation.
         * <p>
         * This handles all cases from a completely successful move where only
         * the RMI conveying the outcome failed (the exception is logged as a
         * warning and this method returns normally since the move was in fact
         * successful) to cases where it must rollback the change to the MDS
         * (the commit of the target index partition failed after it had updated
         * the MDS - in this cases some clients may temporarily see the locator
         * for the target index partition, but they will discover that the index
         * partition does not exist), to cases where the move failed before the
         * MDS was updated (no compensating action is required).
         * 
         * @throws Exception
         *             the caller's {@link Throwable}, wrapped iff necessary.
         */
        private void rollbackMove(//
                final Throwable t,//
                final String scaleOutIndexName,//
                final String targetIndexName,//
                final IDataService targetDataService,//
                final PartitionLocator oldLocator,//
                final PartitionLocator newLocator//
                ) throws Exception {

            /*
             * 1. Query the target data service. If the target index partition
             * was registered, then we are done and this method will return
             * normally rather than re-throwing the exception.
             * 
             * Note: It is not possible for the target index partition to be
             * successfully registered on the target data service unless the MDS
             * was also updated successfully - this is guaranteed because the
             * MDS update occurs within the UNISOLATED task which registers the
             * target index partition.
             */
            try {
                
                /*
                 * Figure out whether the target index partition was registered.
                 * 
                 * Note: An UNISOLATED request and a custom IIndexProcedure
                 * (which IS NOT marked as read-only) are used deliberately.
                 * This way, even if the Future was not returned correctly for
                 * the task which we submitted to receive the index partition on
                 * the target data service (due to an RMI error) then we are
                 * guaranteed that our test WILL NOT execute until it can gain a
                 * lock on the target index partition. This prevents us from
                 * attempting to verify the outcome of the task before it has
                 * completed in the odd case where it is running asynchronously
                 * but we lack its Future.
                 */
                final IndexMetadata tmp = (IndexMetadata) targetDataService
                        .submit(ITx.UNISOLATED, targetIndexName,
                                new IsIndexRegistered_UsingWriteService())
                        .get();

                if (tmp == null) {
                
                    /*
                     * This guards against a potential API change where
                     * AbstractTask#getIndex(String) would report [null] rather
                     * than throwing a NoSuchIndexException.
                     */
                    
                    throw new AssertionError("Not expecting [null] return.");
                    
                }
                
                log
                        .error("Move successful - ignoring spurious exception: "
                                + t);
                
                // return normally.
                return;

            } catch (ExecutionException ex) {

                if (ex.getCause() instanceof NoSuchIndexException) {
                
                    /*
                     * The target index partition was not registered.
                     */

                    // fall through
                    
                } else throw ex;
                
            }
            
            /*
             * 2. Query the MDS.
             * 
             * We know that the target index partition was not successfully
             * registered on the target data service. Now we query the MDS. If
             * the target index partition is assigned by the MDS to the key
             * range for the source index partition then the MDS was updated and
             * we rollback that change using a compensating action.
             */
            
            try {
                
                final PartitionLocator current = resourceManager
                        .getFederation().getMetadataService().get(
                                scaleOutIndexName, ITx.UNISOLATED,
                                oldLocator.getLeftSeparatorKey());

                final boolean mdsWasUpdated = current.getPartitionId() != oldLocator
                        .getPartitionId();

                if (mdsWasUpdated) {

                    /*
                     * Rollback the MDS using a compensating action.
                     */
                    try {

                        resourceManager.getFederation().getMetadataService()
                                .moveIndexPartition(scaleOutIndexName,
                                        newLocator, oldLocator);
                        
                    } catch (Throwable t2) {

                        log.error("Problem writing MDS? ", t2);

                    }
                    
                }

            } catch (Throwable t2) {

                log.error("Problem reading MDS? ", t2);
                
            }

            /*
             * In any case, rethrow the original exception since the move was
             * not successful.
             */
            
            if(t instanceof Exception)
                throw (Exception)t;
            
            throw new RuntimeException(t);

        }
        
    }

    /**
     * Method used to test whether or not the target index partition was
     * successfully registered on the target data service. This class explicitly
     * uses the write service in order to guarantee that it can not execute
     * until the "receive" operation is complete.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private static class IsIndexRegistered_UsingWriteService implements
            IIndexProcedure<IndexMetadata> {
        
        /**
         * 
         */
        private static final long serialVersionUID = -6492979226768348981L;

        @Override
        public IndexMetadata apply(final IIndex ndx) {

            return ndx.getIndexMetadata();
            
        }

        /**
         * Note: This procedure is deliberately marked as NOT read-only.
         * This ensures that the procedure will not execute until it
         * has the exclusive write lock for the index.
         */
        public boolean isReadOnly() {

            return false;
            
        }
        
    }
    
    /**
     * Receives an index partition comprised of a historical index segment store
     * and an index segment store containing the buffered writes and registers
     * the index partition on the data service on which this procedure is
     * executed. This class is actually a {@link Serializable} wrapper which
     * submits the {@link InnerReceiveIndexPartitionTask} once it is running on
     * the target data service.
     * 
     * @see InnerReceiveIndexPartitionTask
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    protected static class ReceiveIndexPartitionTask extends DataServiceCallable<Void> {

        /**
         * 
         */
        private static final long serialVersionUID = -4277343552510590741L;
        
        final private IndexMetadata sourceIndexMetadata;
        final private UUID sourceDataServiceUUID;
        final private int targetIndexPartitionId;
        final private SegmentMetadata historyIndexSegmentMetadata;
        final private SegmentMetadata bufferedWritesIndexSegmentMetadata;
        final private InetSocketAddress addr;
//        final private int port;

        /**
         * @param sourceIndexMetadata
         *            The index metadata for the source index partition.
         * @param sourceDataServiceUUID
         * @param targetIndexPartitionId
         *            The index partition identifier assigned to the target
         *            index partition.
         * @param historyIndexSegmentMetadata
         *            Describes the {@link IndexSegmentStore} containing the
         *            historical data for the source index partition.
         * @param bufferedWritesIndexSegmentMetadata
         *            Describes the {@link IndexSegmentStore} containing the
         *            buffered writes from the live journal for the source index
         *            partition.
         * @param addr
         *            The {@link InetSocketAddress} of the
         *            {@link ResourceService} running on the source data service
         *            (the one from which the resources will be copied during
         *            the move).
         */
        ReceiveIndexPartitionTask(//
                final IndexMetadata sourceIndexMetadata,//
                final UUID sourceDataServiceUUID,//
                final int targetIndexPartitionId,//
                final SegmentMetadata historyIndexSegmentMetadata,//
                final SegmentMetadata bufferedWritesIndexSegmentMetadata,//
                final InetSocketAddress addr
                ) {

            this.sourceIndexMetadata = sourceIndexMetadata;
            this.sourceDataServiceUUID = sourceDataServiceUUID;
            this.targetIndexPartitionId = targetIndexPartitionId;
            this.historyIndexSegmentMetadata = historyIndexSegmentMetadata;
            this.bufferedWritesIndexSegmentMetadata = bufferedWritesIndexSegmentMetadata;
            this.addr = addr;
        }
    
//        private transient DataService dataService;
//        
//        public void setDataService(DataService dataService) {
//            
//            this.dataService = dataService;
//            
//        }
//
//        protected DataService getDataService() {
//            
//            if (dataService == null)
//                throw new IllegalArgumentException();
//
//            return dataService;
//            
//        }

        public Void call() throws Exception {
            
            /*
             * The name of the target index partition on the target data
             * service. This is formed using the name of the scale-out index and
             * the partition identifier that was assigned to the new index
             * partition.
             */
            final String targetIndexName = DataService.getIndexPartitionName(
                    sourceIndexMetadata.getName(), targetIndexPartitionId);

            /*
             * Run the inner task on the write service of the target data
             * service.
             */
            final ResourceManager resourceManager = getDataService()
                    .getResourceManager();
            try {

                getDataService().getConcurrencyManager().submit(
                        new InnerReceiveIndexPartitionTask(//
                                resourceManager,//
                                targetIndexName,//
                                sourceIndexMetadata,//
                                sourceDataServiceUUID,//
                                targetIndexPartitionId,//
                                historyIndexSegmentMetadata,//
                                bufferedWritesIndexSegmentMetadata,//
                                addr//
                        )).get();

                // update the index partition receive counter.
                resourceManager.overflowCounters.indexPartitionReceiveCounter.incrementAndGet();

                return null;
                
            } finally {
                
                /*
                 * Regardless of whether the move was successful or not, we now
                 * remove from the received index segment stores from the
                 * retention set so that these resources become releaseable.
                 */

                resourceManager.retentionSetRemove(historyIndexSegmentMetadata
                        .getUUID());
                
                resourceManager
                        .retentionSetRemove(bufferedWritesIndexSegmentMetadata
                                .getUUID());
                
            }
            
        }
        
    }

    /**
     * Task submitted to the {@link ConcurrencyManager} on the target data
     * service handles all the work required to receive the data for the index
     * partition and register the new index partition on the target data service
     * and in the {@link IMetadataService}.
     * <p>
     * The new index partition initially will have three sources in the view:
     * the live journal on the target data service, the buffered writes index
     * segment from the source data service, and the history index segment from
     * source data service.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private static class InnerReceiveIndexPartitionTask extends AbstractTask<Void> {
        
        final private ResourceManager resourceManager;
        final private String scaleOutIndexName;
        final private String sourceIndexName;
        final private String targetIndexName;
        final private IndexMetadata sourceIndexMetadata;
        final private UUID sourceDataServiceUUID;
        final private UUID targetDataServiceUUID;
        final private int sourceIndexPartitionId;
        final private int targetIndexPartitionId;
        final private SegmentMetadata sourceHistorySegmentMetadata;
        final private SegmentMetadata sourceBufferedWritesSegmentMetadata;
        final private Event parentEvent;
        final private String summary;
        final InetSocketAddress addr;
//        final int port;

        /**
         * @param resourceManager
         *            The resource manager (on the data service).
         * @param targeIndexName
         *            The name of the target index partition.
         * @param sourceIndexMetadata
         *            The index metadata for the source index partition.
         * @param sourceDataServiceUUID
         * @param targetIndexPartitionId
         *            The index partition identifier assigned to the target
         *            index partition.
         * @param historyIndexSegmentMetadata
         *            Describes the {@link IndexSegmentStore} containing the
         *            historical data for the source index partition.
         * @param bufferedWritesIndexSegmentMetadata
         *            Describes the {@link IndexSegmentStore} containing the
         *            buffered writes from the live journal for the source index
         *            partition.
         * @param addr
         *            The {@link InetSocketAddress} of the
         *            {@link ResourceService} of the source data service (the
         *            one from which the resources will be copied).
         */
        InnerReceiveIndexPartitionTask(final ResourceManager resourceManager,
                final String targetIndexName,
                final IndexMetadata sourceIndexMetadata,
                final UUID sourceDataServiceUUID,
                final int targetIndexPartitionId,
                final SegmentMetadata historyIndexSegmentMetadata,
                final SegmentMetadata bufferedWritesIndexSegmentMetadata,
                final InetSocketAddress addr
                ) {

            super(resourceManager.getConcurrencyManager(), ITx.UNISOLATED,
                    targetIndexName);

            if (sourceIndexMetadata == null)
                throw new IllegalArgumentException();
            if (sourceDataServiceUUID == null)
                throw new IllegalArgumentException();
            if (historyIndexSegmentMetadata == null)
                throw new IllegalArgumentException();
            if (bufferedWritesIndexSegmentMetadata == null)
                throw new IllegalArgumentException();
            if (addr == null)
                throw new IllegalArgumentException();
            
            this.resourceManager = resourceManager;
            this.scaleOutIndexName = sourceIndexMetadata.getName();
            this.sourceIndexPartitionId = sourceIndexMetadata
                    .getPartitionMetadata().getPartitionId();
            this.sourceIndexName = DataService.getIndexPartitionName(
                    scaleOutIndexName, sourceIndexPartitionId);
            this.targetIndexName = targetIndexName;
            this.sourceIndexMetadata = sourceIndexMetadata;
            this.sourceDataServiceUUID = sourceDataServiceUUID;
            this.targetDataServiceUUID = resourceManager.getDataServiceUUID();
            this.targetIndexPartitionId = targetIndexPartitionId;
            this.sourceHistorySegmentMetadata = historyIndexSegmentMetadata;
            this.sourceBufferedWritesSegmentMetadata = bufferedWritesIndexSegmentMetadata;
            this.addr = addr;

            this.summary = OverflowActionEnum.Move + "(" + sourceIndexName
                    + "->" + targetIndexName + ")";

            this.parentEvent = new Event(resourceManager.getFederation(),
                    new EventResource(sourceIndexMetadata.getName(),
                            sourceIndexPartitionId), OverflowActionEnum.Move);

            this.parentEvent.addDetail("summary", this.summary);

        }

        /**
         * Copies the history and the buffered writes for the source index
         * partition into the local {@link StoreManager}, registers the new
         * index partition using those resources in its view, and notifies the
         * {@link IMetadataService} of the move.
         * <p>
         * The caller still needs to delete the moved resources on their end,
         * delete the source index partition, and update the stale locator cache
         * so that tasks in the write queue will be redirected to the new index
         * partition.
         * <p>
         * If this task throws an exception the caller MUST query the
         * {@link IMetadataService} and determine whether the source index
         * partition or the target index partition is now registered therein. If
         * the target index partition is registered with the
         * {@link IMetadataService}, then the caller needs to rollback that
         * change using a compensating action.
         * 
         * @throws Exception
         */
        public Void doTask() throws Exception {

            SegmentMetadata targetHistorySegmentMetadata = null;
            SegmentMetadata targetBufferedWritesSegmentMetadata = null;
            parentEvent.start();
            try {

                targetHistorySegmentMetadata = receiveIndexSegmentStore(sourceHistorySegmentMetadata);

                targetBufferedWritesSegmentMetadata = receiveIndexSegmentStore(sourceBufferedWritesSegmentMetadata);

                final MoveResult moveResult = registerIndexPartition(
                        targetHistorySegmentMetadata,
                        targetBufferedWritesSegmentMetadata);

                updateMetadataIndex(moveResult);

                return null;

            } catch (Throwable t) {

                if (targetHistorySegmentMetadata != null) {
                    try {
                        resourceManager
                                .retentionSetRemove(targetHistorySegmentMetadata
                                        .getUUID());
                        resourceManager
                                .deleteResource(targetHistorySegmentMetadata
                                        .getUUID(), false/* isJournal */);
                    } catch (Throwable t2) {
                        // ignore
                    }
                }

                if (targetBufferedWritesSegmentMetadata != null) {
                    try {
                        resourceManager
                                .retentionSetRemove(targetBufferedWritesSegmentMetadata
                                        .getUUID());
                        resourceManager.deleteResource(
                                targetBufferedWritesSegmentMetadata.getUUID(),
                                false/* isJournal */);
                    } catch (Throwable t2) {
                        // ignore
                    }
                }

                if (t instanceof Exception)
                    throw (Exception) t;

                throw new Exception(t);

            } finally {
                
                parentEvent.end();
                
            }

        }

        /**
         * This transfers the specified resource into its local data directory
         * and registers it with the local {@link ResourceManager}.
         * <p>
         * Note: Since the file (in this case an index segment) is being
         * transferred from one data service to another the dataDir, the target
         * index partitionId, and the basename of the file will all be
         * different. This code make sure that the file winds up in the correct
         * directory for the scale-out index _partition_ to which it belongs.
         * 
         * @param sourceSegmentMetadata
         *            The {@link SegmentMetadata} for the resource on the source
         *            data service.
         * 
         * @return The {@link SegmentMetadata} for the resource where it resides
         *         on this data service.
         */
        protected SegmentMetadata receiveIndexSegmentStore(
                final SegmentMetadata sourceSegmentMetadata) throws Exception {

            final Event e = parentEvent.newSubEvent(
                    OverflowSubtaskEnum.ReceiveIndexSegment,
                    sourceSegmentMetadata.getParams()).start();

            try {

                if (sourceSegmentMetadata == null)
                    throw new IllegalArgumentException();

                final long begin = System.currentTimeMillis();

                // name for the segFile on this data service.
                final File file = resourceManager.getIndexSegmentFile(
                        scaleOutIndexName, sourceIndexMetadata.getIndexUUID(),
                        targetIndexPartitionId);

                // make sure that the parent directory exists.
                file.getParentFile().mkdirs();

                /*
                 * Construct the metadata describing the index segment that we
                 * are going to receive.
                 */
                final SegmentMetadata targetSegmentMetadata = new SegmentMetadata(
                        file, sourceSegmentMetadata.getUUID(),
                        sourceSegmentMetadata.getCreateTime());

                try {

                    // read the resource, writing onto that file.
                    new ResourceService.ReadResourceTask(addr,
                            sourceSegmentMetadata.getUUID(), file).call();

                } catch (Throwable t) {

                    try {
                        file.delete();
                    } catch (Throwable t2) {
                        // ignore
                    }

                    if (t instanceof Exception)
                        throw (Exception) t;

                    throw new Exception(t);

                }

                // put on the retentionSet first!
                resourceManager.retentionSetAdd(sourceSegmentMetadata.getUUID());
                
                // add the resource to those managed by this service.
                resourceManager.addResource(sourceSegmentMetadata, file);

                if (INFO) {

                    final long elapsed = System.currentTimeMillis() - begin;

                    log.info("Received index segment: " + sourceSegmentMetadata
                            + " in " + elapsed + "ms");

                }

                return targetSegmentMetadata;

            } finally {

                e.end();

            }

        }

        /**
         * Register the target index partition on this (the target) data
         * service.
         * 
         * @param historySegmentMetadata
         *            The metadata for the received index segment containing the
         *            historical writes up to the last commit point of the old
         *            journal for the source index partition.
         * @param bufferedWritesSegmentMetadata
         *            The metadata for the received index segment containing the
         *            buffered writes from the live journal for the source index
         *            partition.
         * 
         * @return {@link MoveResult} which contains the information we need to
         *         update the {@link IMetadataService} when the move is
         *         complete.
         */
        protected MoveResult registerIndexPartition(
                final SegmentMetadata historySegmentMetadata,
                final SegmentMetadata bufferedWritesSegmentMetadata) {

            final Event e = parentEvent.newSubEvent(
                    OverflowSubtaskEnum.RegisterIndex).addDetail(
                    "targetIndexName", targetIndexName).start();

            try {

                // clone metadata.
                final IndexMetadata newMetadata = sourceIndexMetadata.clone();

                // the partition metadata for the source index partition.
                final LocalPartitionMetadata oldpmd = newMetadata
                        .getPartitionMetadata();

                /*
                 * Note: We DO NOT specify the sourcePartitionId on the new
                 * index partition's view since the view will be made live
                 * within this UNISOLATED task. Using this approach does not
                 * allow a gap when the move is in progress since it runs as a
                 * single unisolated task rather than a series of such tasks.
                 */
                newMetadata.setPartitionMetadata(new LocalPartitionMetadata(//
                        targetIndexPartitionId, // the new partition identifier.
                        -1, // The source partition identifier (unused here).
                        oldpmd.getLeftSeparatorKey(),//
                        oldpmd.getRightSeparatorKey(),//
                        /*
                         * Define the view for the target index partition.
                         */
                        new IResourceMetadata[] {//
                            // The live journal (no data for this index partition yet).
                            getJournal().getResourceMetadata(), //
                            // Buffered writes from the live journal of the source DS.
                            bufferedWritesSegmentMetadata,//
                            // Historical writes from the source DS.
                            historySegmentMetadata//
                        },
                        IndexPartitionCause.move(resourceManager)
//                        // history line.
//                        ,oldpmd.getHistory() + summary + " "
                ));
                        
                /*
                 * Create the BTree to aborb writes for the target index
                 * partition. The metadata for this BTree was configured above
                 * and is associated with a view that captures all data received
                 * from the source index partition.
                 */
                final BTree btree = BTree.create(getJournal(), newMetadata);
                
                /*
                 * Register the BTree on this data service.
                 */
                registerIndex(targetIndexName, btree);

                if (INFO)
                    log
                            .info("Registered new index partition on target data service: targetIndexName="
                                    + targetIndexName);

                /*
                 * Formulate a MoveResult containing the information that we
                 * need to update the MDS.
                 */

                final LocalPartitionMetadata pmd = sourceIndexMetadata
                        .getPartitionMetadata();

                final PartitionLocator oldLocator = new PartitionLocator(//
                        sourceIndexPartitionId,//
                        sourceDataServiceUUID,//
                        pmd.getLeftSeparatorKey(),//
                        pmd.getRightSeparatorKey()//
                );

                final PartitionLocator newLocator = new PartitionLocator(
                        targetIndexPartitionId,//
                        targetDataServiceUUID,//
                        pmd.getLeftSeparatorKey(),//
                        pmd.getRightSeparatorKey()//
                );

                return new MoveResult(sourceIndexName, sourceIndexMetadata,
                        targetDataServiceUUID, targetIndexPartitionId, oldLocator,
                        newLocator);

            } finally {

                e.end();

            }

        }
        
        /**
         * Notifies the {@link IMetadataService} that the index partition has
         * been moved. Once the {@link IMetadataService} has been notified, new
         * requests for the key range will be able to discover the target index
         * partition. However, tasks already in the queue on the source data
         * service will not be notified that the index partition has been moved
         * until this task completes and the caller updates the stale locator
         * cache and deletes the source index partition.
         * 
         * @throws ExecutionException
         * @throws InterruptedException
         * @throws IOException
         */
        protected void updateMetadataIndex(final MoveResult moveResult)
                throws IOException, InterruptedException, ExecutionException {

            if (INFO)
                log.info("Updating metadata index: name=" + scaleOutIndexName
                        + ", oldLocator=" + moveResult.oldLocator
                        + ", newLocator=" + moveResult.newLocator);

            // atomic update on the metadata server.
            resourceManager.getFederation().getMetadataService()
                    .moveIndexPartition(scaleOutIndexName,
                            moveResult.oldLocator, moveResult.newLocator);

            /*
             * @todo This flag is unused for this impl since MDS update is done
             * by the target data service - in fact, the flag can probably be
             * discarded if this move procedure works out nicely since it offers
             * better atomicity guarantees.  The OverflowSubtaskEnum can also
             * be pruned since we will no longer use certain subtasks which it
             * declares.
             */
//            moveResult.registeredInMDS.set(true);

        }
    
    }

}
