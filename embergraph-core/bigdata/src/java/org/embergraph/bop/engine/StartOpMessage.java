package org.embergraph.bop.engine;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.embergraph.io.LongPacker;

/**
 * A message sent to the {@link IQueryClient} when an operator begins executing
 * for some chunk of inputs (an operator on a node against a shard for some
 * binding set chunks generated by downstream operator(s)).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class StartOpMessage implements Externalizable, IStartOpMessage {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private UUID queryId;
    private UUID serviceId;
    private int bopId;
    private int partitionId;
    private int messageReadyCount;

    /**
     * De-serialization constructor.
     */
    public StartOpMessage() {
        
    }
    
    public StartOpMessage(final UUID queryId, final int opId,
            final int partitionId, final UUID serviceId, final int nmessages
            ) {

        if (queryId == null)
            throw new IllegalArgumentException();
        
        if (nmessages <= 0)
            throw new IllegalArgumentException();
        
        this.queryId = queryId;
        
        this.bopId = opId;
    
        this.partitionId = partitionId;
        
        this.serviceId = serviceId;
        
        this.messageReadyCount = nmessages;
    
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append("{queryId=" + queryId);
        sb.append(",bopId=" + bopId);
        sb.append(",partitionId=" + partitionId);
        sb.append(",serviceId=" + serviceId);
        sb.append(",nchunks=" + messageReadyCount);
        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public UUID getQueryId() {
        return queryId;
    }

    @Override
    public int getBOpId() {
        return bopId;
    }

    @Override
    public int getPartitionId() {
        return partitionId;
    }

    @Override
    public UUID getServiceId() {
        return serviceId;
    }

    @Override
    public int getChunkMessageCount() {
        return messageReadyCount;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(queryId.getMostSignificantBits());
        out.writeLong(queryId.getLeastSignificantBits());
        out.writeLong(serviceId.getMostSignificantBits());
        out.writeLong(serviceId.getLeastSignificantBits());
        out.writeInt(bopId);
        out.writeInt(partitionId); // Note: partitionId is 32-bits clean
//        out.writeInt(messageReadyCount);
        LongPacker.packLong(out,messageReadyCount);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        queryId = new UUID(in.readLong()/* MSB */, in.readLong()/* LSB */);
        serviceId = new UUID(in.readLong()/* MSB */, in.readLong()/* LSB */);
        bopId = in.readInt();
        partitionId = in.readInt();
        messageReadyCount = LongPacker.unpackInt(in);
//        messageReadyCount = in.readInt();
    }

}
