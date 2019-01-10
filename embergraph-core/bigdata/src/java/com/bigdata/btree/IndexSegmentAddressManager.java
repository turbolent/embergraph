package com.bigdata.btree;

import com.bigdata.rawstore.WormAddressManager;

/**
 * <p>
 * Address manager supporting offsets that are encoded for one of several
 * regions in an {@link IndexSegmentStore}. The regions are identified by a
 * {@link IndexSegmentRegion}, which gets encoded into the offset component of
 * the address. The offsets are relative to the start of the identified regions.
 * The {@link IndexSegmentCheckpoint} record gives the start of each region.
 * </p>
 * <p>
 * Together with {@link IndexSegmentRegion}, this class class provides a
 * workaround for node offsets (which are relative to the start of the nodes
 * block) in contrast to leaf offsets (which are relative to a known offset from
 * the start of the index segment file). This condition arises as a side effect
 * of serializing nodes at the same time that the {@link IndexSegmentBuilder} is
 * serializing leaves such that we can not group the nodes and leaves into
 * distinct regions and know the absolute offset to each node or leaf as it is
 * serialized.
 * </p>
 * <p>
 * The offsets for blobs are likewise relative to the start of a
 * {@link IndexSegmentRegion#BLOB} region. The requirement for a blob region
 * arises in a similar manner: blobs are serialized during the
 * {@link IndexSegmentBuilder} operation onto a buffer and then bulk copied onto
 * the output file. This means that only the relative offset into the blob
 * region is available at the time that the blob's address is written in an
 * index entry's value.
 * </p>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class IndexSegmentAddressManager extends WormAddressManager {

    /**
     * The offset within the file of the start of the leaves region. All leaves
     * are written densely on the file beginning at this offset.
     */
    protected final long offsetLeaves;
    
    /**
     * #of bytes in the leaves region.
     */
    protected final long extentLeaves;

    /**
     * The offset within the file of the start of the node region. All nodes
     * are written densely on the file beginning at this offset. The child
     * addresses for a node are relative to this offset and are
     * automatically adjusted during decoding by this class.
     */
    protected final long offsetNodes;
    
    /**
     * #of bytes in the nodes region.
     */
    protected final long extentNodes;
    
    /**
     * The offset within the file of the start of the blob region. All blob
     * records are written densely on the file beginning at this offset. The
     * blob addresses stored in a leaf are relative to this offset and are
     * automatically adjusted during decoding by this class.
     */
    protected final long offsetBlobs;
    
    /**
     * #of bytes in the blobs region.
     */
    protected final long extentBlobs;
    
    /**
     * The maximum offset (aka the #of bytes in the file).
     */
    protected final long maxOffset;
    
    /**
     * @param checkpoint
     */
    public IndexSegmentAddressManager(final IndexSegmentCheckpoint checkpoint) {

        super(checkpoint.offsetBits);

        this.offsetLeaves = checkpoint.offsetLeaves;
        
        this.extentLeaves = checkpoint.extentLeaves;
        
        this.offsetNodes = checkpoint.offsetNodes;

        this.extentNodes = checkpoint.extentNodes;

        this.offsetBlobs = checkpoint.offsetBlobs;

        this.extentBlobs = checkpoint.extentBlobs;

        this.maxOffset = checkpoint.length;
        
    }
    
    /**
     * Return the region relative to which this address was encoded.
     * <p>
     * Note: ANY address MAY be encoded relative to the
     * {@link IndexSegmentRegion#BASE} region. However, choosing
     * {@link IndexSegmentRegion#NODE} or {@link IndexSegmentRegion#BLOB}
     * regions does restrict the address to referencing a node (or blob)
     * respectively.
     * 
     * @param addr
     *            The address.
     * 
     * @return The region relative to which this address was encoded.
     */
    final public IndexSegmentRegion getRegion(final long addr) {

        // the encoded offset (the region is encoded in this value).
        final long encodedOffset = super.getOffset(addr);
        
        // the region.
        final IndexSegmentRegion region = IndexSegmentRegion.decodeRegion(encodedOffset);
        
        return region;
        
    }
    
    /**
     * Decodes the offset to extract the {@link IndexSegmentRegion} and then
     * applies the appropriate offset for that region in order to convert the
     * offset into an absolute offset into the store.
     */
    final public long getOffset(final long addr) {
    
        if (addr == 0L)
            return 0L;
        
        // the encoded offset (the region is encoded in this value).
        final long encodedOffset = super.getOffset(addr);
        
        // the region.
        final IndexSegmentRegion region = IndexSegmentRegion.decodeRegion(encodedOffset);
        
        // #of bytes in the addressed record.
        final int nbytes = getByteCount(addr);
        
        // the decoded offset (relative to the region).
        final long decodedOffset = IndexSegmentRegion.decodeOffset(encodedOffset);
        
        final long offset;
        
        switch (region) {
        
        case BASE:

            offset = decodedOffset; // + 0L (offsetBase);

            // range check address.
            if ((decodedOffset + nbytes) > maxOffset)
                throw new AssertionError("Region=" + region + ", addr="
                        + toString(addr) + ", offset=" + offset
                        + ", byteCount=" + nbytes + ", maxOffset=" + maxOffset);

            break;

        case NODE:

            // adjust offset.
            offset = decodedOffset + offsetNodes;

            // range check address.
            if ((decodedOffset + nbytes) > extentNodes)
                throw new AssertionError("Region=" + region + ", addr="
                        + toString(addr) + ", offset=" + offset
                        + ", byteCount=" + nbytes + ", sizeNodes="
                        + extentNodes);

            break;

        case BLOB:

            // adjust offset.
            offset = decodedOffset + offsetBlobs;

            // range check address.
            if ((decodedOffset + nbytes) > extentBlobs)
                throw new AssertionError("Region=" + region + ", addr="
                        + toString(addr) + ", offset=" + offset
                        + ", byteCount=" + nbytes + ", sizeBlobs="
                        + extentBlobs);

            break;
            
        default:
            
            throw new IllegalArgumentException("Could not decode: addr=" + addr);
        
        }
        
        return offset;
        
    }

    /**
     * Returns a representation of the address with the decoded offset and
     * the region to which that offset is relative.
     */
    public String toString(long addr) {
        
        if (addr == 0L) return _NULL_;
        
        final long encodedOffset = super.getOffset(addr);
        
        final int nbytes = getByteCount(addr);
        
        return "{region=" + IndexSegmentRegion.decodeRegion(encodedOffset)
                + ",off=" + IndexSegmentRegion.decodeOffset(encodedOffset)
                + ",len=" + nbytes + "}";

//        return "{nbytes=" + nbytes + ",offset="
//                + IndexSegmentRegion.decodeOffset(encodedOffset) + ",region="
//                + IndexSegmentRegion.decodeRegion(encodedOffset) + "}";

    }

    /**
     * Return <code>true</code> IFF the starting address lies entirely within
     * the region dedicated to the B+Tree nodes.
     */
    public boolean isNodeAddr(long addr) {
       
        // abs. offset of the record in the file.
        final long offset = getOffset(addr);

        // length of the record.
        final int length = getByteCount(addr);

        final boolean isNodeAddr = offset >= offsetNodes
                && (offset + length) <= (offsetNodes + extentNodes);

        return isNodeAddr;

    }
    
    /**
     * Return <code>true</code> IFF the starting address lies entirely within
     * the region dedicated to the B+Tree leaves.
     */
    public boolean isLeafAddr(long addr) {
       
        // abs. offset of the record in the file.
        final long offset = getOffset(addr);

        // length of the record.
        final int length = getByteCount(addr);

        final boolean isNodeAddr = offset >= offsetLeaves
                && (offset + length) <= (offsetLeaves + extentLeaves);

        return isNodeAddr;

    }
    
}
