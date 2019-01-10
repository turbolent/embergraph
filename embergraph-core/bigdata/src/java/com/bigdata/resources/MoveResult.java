package com.bigdata.resources;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.bigdata.btree.IndexMetadata;
import com.bigdata.mdi.PartitionLocator;
import com.bigdata.service.DataService;

/**
 * The object returned by {@link MoveIndexPartitionTask}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MoveResult extends AbstractResult {

    final public UUID targetDataServiceUUID;

    final public int newPartitionId;

    /**
     * The name of the new index partition on the target data service.
     * <p>
     * Note: {@link AbstractResult#name} is the name of the source index
     * partition that was moved to the target data service.
     */
    final public String targetIndexName;
    
    final public PartitionLocator oldLocator;
    final public PartitionLocator newLocator;

    /**
     * <code>false</code> until the target index partition is registered in
     * the MDS. This is used to decide whether or not we need to rollback a
     * change in the MDS if the atomic update task fails during the commit.
     */
    final protected AtomicBoolean registeredInMDS = new AtomicBoolean(false);
    
    /**
     * @param name
     *            The name of the source index partition.
     * @param indexMetadata
     *            The index metadata object for the source index partition.
     * @param targetDataServiceUUID
     *            The data service {@link UUID} for the target data service.
     * @param newPartitionId
     *            The partition identifier assigned to the target index
     *            partition.
     * @param oldLocator
     *            The locator for the source index partition.
     * @param newLocator
     *            The locator for the new index partition.
     */
    public MoveResult(final String name,// 
            final IndexMetadata indexMetadata,//
            final UUID targetDataServiceUUID, //
            final int newPartitionId,//
            final PartitionLocator oldLocator,//
            final PartitionLocator newLocator//
    ) {

        super(name, indexMetadata);

        if (targetDataServiceUUID == null)
            throw new IllegalArgumentException();

        if (oldLocator == null)
            throw new IllegalArgumentException();
        
        if (newLocator == null)
            throw new IllegalArgumentException();

        this.targetDataServiceUUID = targetDataServiceUUID;

        this.newPartitionId = newPartitionId;

        this.targetIndexName = DataService.getIndexPartitionName(indexMetadata
                .getName(), newPartitionId);
        
        this.oldLocator = oldLocator;

        this.newLocator = newLocator;

    }

    public String toString() {

        final StringBuilder sb = new StringBuilder();

        sb.append("MoveResult");
        sb.append("{ name=" + name);
        sb.append(", newPartitionId=" + newPartitionId);
        sb.append(", targetDataService=" + targetDataServiceUUID);
        sb.append(", oldLocator=" + oldLocator);
        sb.append(", newLocator=" + newLocator);
        sb.append(" }");

        return sb.toString();
        
    }

}
