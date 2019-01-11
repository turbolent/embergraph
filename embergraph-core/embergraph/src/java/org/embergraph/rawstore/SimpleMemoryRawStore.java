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
 * Created on Jan 31, 2007
 */

package org.embergraph.rawstore;

import java.io.File;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.embergraph.counters.CounterSet;
import org.embergraph.journal.TemporaryRawStore;
import org.embergraph.mdi.IResourceMetadata;

/**
 * A purely transient append-only implementation useful when data need to be
 * buffered in memory. The writes are stored in an {@link ArrayList}.
 * <p>
 * Note: it is safe to NOT call {@link #close()} on this implementation. The
 * implementation does not contain things like {@link ExecutorService}s that
 * would hang around unless explicitly shutdown.
 * 
 * @see {@link TemporaryRawStore}, which provides a more scalable solution for
 *      temporary data.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SimpleMemoryRawStore extends AbstractRawWormStore {

    private boolean open = true;
    
    private final UUID uuid = UUID.randomUUID();
    
    /**
     * The #of bytes written so far. This is used to generate the address values
     * returned by {@link #write(ByteBuffer)}. This is necessary in order for
     * this implementation to assign addresses in the same manner as they would
     * be assigned by an implementation using an append only byte[] or file.
     */
    protected int nextOffset = 0;
    
    /**
     * Maps an address onto the index in {@link #records} at which the data for
     * that address was written.
     */
    private final Map<Long,Integer> addrs;
    
    /**
     * The buffered records in the order written. If a record is deleted then
     * that element in the list will be a [null] value.
     */
    protected final ArrayList<byte[]> records;
    
    /**
     * Uses an initial capacity of 1000 records.
     */
    public SimpleMemoryRawStore() {

        this(1000);
        
    }
    
    /**
     * 
     * @param capacity
     *            The #of records that you expect to store (non-negative). If
     *            the capacity is exceeded then the internal {@link ArrayList}
     *            will be grown as necessary.
     */
    public SimpleMemoryRawStore(int capacity) {
        
        /*
         * Note: The #of offset bits is restricted to 31 since we can not
         * address more an array having more than 31 unsigned bits of length in
         * Java's memory model.
         */
        super(31);
        
        if (capacity < 0)
            throw new IllegalArgumentException("capacity is negative");
        
        records = new ArrayList<byte[]>(capacity);
        
        // estimate hash table capacity to avoid resizing.
        addrs = new HashMap<Long,Integer>((int)(capacity*1.25));

    }
    
    @Override
    public boolean isOpen() {

        return open;
        
    }

    @Override
    public boolean isReadOnly() {

        if (!open)
            throw new IllegalArgumentException();

        return false;
        
    }

    @Override
    public boolean isStable() {
        
        return false;
        
    }

    @Override
    public boolean isFullyBuffered() {
        
        return true;
        
    }
    
    @Override
    public UUID getUUID() {
        
        return uuid;
        
    }
    
    @Override
    public IResourceMetadata getResourceMetadata() {

        return new ResourceMetadata(uuid);
        
    }

    /**
     * Static class since must be {@link Serializable}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    private static class ResourceMetadata implements IResourceMetadata {
        
        /**
         * 
         */
        private static final long serialVersionUID = -8333003625527191826L;

        private final UUID uuid;
        
        public ResourceMetadata(UUID uuid) {
            
            this.uuid = uuid;
            
        }
        
        @Override
        public boolean equals(IResourceMetadata o) {

            return this == o;

        }

        @Override
        public long getCreateTime() {

            // does not support commit
            return 0L;

        }

        @Override
        public long getCommitTime() {

            // does not support commit
            return 0L;

        }

        @Override
        public String getFile() {

            // no backing file.
            return null;

        }

        @Override
        public UUID getUUID() {

            return uuid;

        }

        @Override
        public boolean isIndexSegment() {

            // not index segment.
            return false;

        }

        @Override
        public boolean isJournal() {

            // not journal.
            return false;

        }

//        public long size() {
//
//            // #of bytes not available.
//            return 0L;
//
//        }

    }
    
    /**
	 * {@inheritDoc}
	 * <P>
	 * This always returns <code>null</code>.
	 */
    @Override
    public File getFile() {
        
        return null;
        
    }
    
    @Override
    public void close() {
        
        if( !open ) throw new IllegalStateException();

        open = false;
        
        // discard all the records.
        records.clear();
        
    }

    @Override
    public void deleteResources() {
        
        if(open) throw new IllegalStateException();
        
//        if (LRUNexus.INSTANCE != null) {
//
//            LRUNexus.INSTANCE.deleteCache(getUUID());
//
//        }
        
    }
    
    @Override
    public void destroy() {
        
        if(isOpen()) close();
        
        deleteResources();
        
    }

    @Override
    public ByteBuffer read(final long addr) {

        if (addr == 0L)
            throw new IllegalArgumentException("Address is 0L");
        
//        final int offset = (int)getOffset(addr);
        
        final int nbytes = getByteCount(addr);

        if(nbytes==0) {
            
            throw new IllegalArgumentException(
                    "Address encodes record length of zero");
            
        }
        
        Integer index = addrs.get(addr);
        
        if(index==null) {
            
            throw new IllegalArgumentException("Address never written.");

        }

        final byte[] b = records.get(index);

        if(b == null) {
            
            throw new IllegalArgumentException("Record was deleted");
            
        }
        
        if(b.length != nbytes) {
            
            throw new RuntimeException("Bad address / data");
            
        }
        
        // return a read-only view onto the data in the store.
            
        return ByteBuffer.wrap(b).asReadOnlyBuffer();
        
    }

    @Override
    public long write(final ByteBuffer data) {

        if (data == null)
            throw new IllegalArgumentException("Buffer is null");

        // #of bytes to store.
        final int nbytes = data.remaining();

        if (nbytes == 0)
            throw new IllegalArgumentException("No bytes remaining in buffer");

        // allocate a new array that is an exact fit for the data.
        final byte[] b = new byte[nbytes];

        // copy the data into the array.
        data.get(b);
        
        // the next offset.
        final int offset = nextOffset;
        
        // increment by the #of bytes written.
        nextOffset += nbytes;

        // the position in the records[] where this record is stored.
        final int index = records.size();
        
        // add the record to the records array.
        records.add(b);
        
        // formulate the address that can be used to recover that record.
        long addr = toAddr(nbytes, offset);
        
        // save the mapping from the addr to the records[].
        addrs.put(addr,index);
        
        return addr;
        
    }

//    public void delete(long addr) {
//
//        if(addr==0L) throw new IllegalArgumentException("Address is 0L");
//        
////        final long offset = getOffset(addr);
//        
//        final int nbytes = getByteCount(addr);
//
//        if(nbytes==0) {
//            
//            throw new IllegalArgumentException(
//                    "Address encodes record length of zero");
//            
//        }
//        
//        Integer index = addrs.get(addr);
//        
//        if(index==null) {
//         
//            throw new IllegalArgumentException("Address never written.");
//
//        }
//        
//        byte[] b = records.get(index);
//
//        if(b == null) {
//            
//            throw new IllegalArgumentException("Record was deleted");
//            
//        }
//
//        if(b.length != nbytes) {
//            
//            throw new RuntimeException("Bad address / data");
//            
//        }
//        
//        // release that record.
//        records.set(index, null);
//        
//    }

    @Override
    public void force(boolean metadata) {
        
        // NOP.
        
    }
    
    @Override
    public long size() {
        
        return nextOffset;
        
    }
    
    @Override
    public CounterSet getCounters() {
    	return new CounterSet();
    }

}
