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
package org.embergraph.btree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.embergraph.io.ChecksumUtility;
import org.embergraph.io.FileChannelUtility;
import org.embergraph.io.NOPReopener;
import org.embergraph.journal.Journal;
import org.embergraph.journal.RootBlockException;
import org.embergraph.rawstore.IAddressManager;
import org.embergraph.util.Bytes;

/**
 * The checkpoint record for an {@link IndexSegment}.
 * <p>
 * The checkpoint record for the index segment file is written at the head of
 * the file. It should have identical timestamps at the start and end of the
 * checkpoint record (e.g., it doubles as a root block). Since the file format
 * is immutable it is ok to have what is essentially only a single root block.
 * If the timestamps do not agree then the build was not successfully completed.
 * <p>
 * Similar to the {@link BTree}'s {@link Checkpoint} record, this record
 * contains only data that pertains specifically to the {@link IndexSegment}
 * checkpoint or data otherwise required to bootstrap the load of the
 * {@link IndexSegment} from the file. General purpose metadata is stored in the
 * {@link IndexMetadata} record.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class IndexSegmentCheckpoint implements ICheckpoint {

    /**
     * Logger.
     */
	private static final Logger log = Logger
			.getLogger(IndexSegmentCheckpoint.class);

    /**
     * The file is empty.
     */
    private static final String ERR_EMPTY = "Empty file";

    /**
     * The file is non-empty, but is too small to contain a valid root block.
     */
    private static final String ERR_TOO_SMALL = "Too small for valid root block";
    
    private static final int SIZEOF_MAGIC = Bytes.SIZEOF_INT;
    private static final int SIZEOF_VERSION = Bytes.SIZEOF_INT;
    private static final int SIZEOF_OFFSET_BITS = Bytes.SIZEOF_INT;
    private static final int SIZEOF_BRANCHING_FACTOR = Bytes.SIZEOF_INT;
    private static final int SIZEOF_COUNTS = Bytes.SIZEOF_INT;
    private static final int SIZEOF_NBYTES = Bytes.SIZEOF_INT;
    private static final int SIZEOF_ADDR = Bytes.SIZEOF_LONG;
    private static final int SIZEOF_ERROR_RATE = Bytes.SIZEOF_DOUBLE;
    private static final int SIZEOF_TIMESTAMP = Bytes.SIZEOF_LONG;
    private static final int SIZEOF_CHECKSUM = Bytes.SIZEOF_INT;

	/**
	 * The #of unused bytes in the checkpoint record format for various versions
	 * of the record format. Note that the unused space occurs <em>before</em>
	 * the final timestamp in the record. As the unused bytes are allocated in
	 * new versions the value in this field MUST be adjusted down from its
	 * original value of 256.
	 */
    static final int SIZEOF_UNUSED_VERSION0 = 256;
    static final int SIZEOF_UNUSED_VERSION1 = SIZEOF_UNUSED_VERSION0 -
    ( Bytes.SIZEOF_BYTE // useChecksums
    );
    static final int SIZEOF_UNUSED_VERSION2 = SIZEOF_UNUSED_VERSION1-
    ( Bytes.SIZEOF_LONG // int64 value for [entries]
    );

    /**
     * The #of bytes required by the current {@link IndexSegmentCheckpoint}
     * record format.
     */
    static final int SIZE = //
            SIZEOF_MAGIC + //
            SIZEOF_VERSION + //
            Bytes.SIZEOF_LONG + // timestamp0
            Bytes.SIZEOF_UUID + // segment UUID.
            SIZEOF_OFFSET_BITS + // #of bits used to represent a byte offset.
            SIZEOF_COUNTS * 4 + // height, #leaves, #nodes, (nentries:int32 now unused) 
            SIZEOF_NBYTES + // max record length
            Bytes.SIZEOF_LONG * 6 + // {offset,extent} tuples for the {leaves, nodes, blobs} regions.
            SIZEOF_ADDR * 5 + // address of the {root node/leaf, indexMetadata, bloomFilter, {first,last}Leaf}.
            Bytes.SIZEOF_LONG + // file size in bytes.
            Bytes.SIZEOF_BYTE + // compactingMerge flag (0 | 1).
            Bytes.SIZEOF_BYTE + // useChecksums flag (0 | 1) [VERSION1]
            Bytes.SIZEOF_LONG + // nentries:int64 as of VERSION2
            SIZEOF_UNUSED_VERSION2 + // available bytes for future versions.
            SIZEOF_CHECKSUM+ // the checksum for the proceeding bytes in the checkpoint record.
            Bytes.SIZEOF_LONG // timestamp1
    ;
    
    /**
     * Magic value written at the start of the {@link IndexSegmentCheckpoint}
     * record.
     */
    static transient final public int MAGIC = 0x87ab34f5;
    
    /**
     * Version 0 of the serialization format.
     */
    static transient final public int VERSION0 = 0x0;

    /**
     * Version 1 of the serialization format introduces an option for record
     * level checksums. New fields in this version include:
     * <dl>
     * <dt>{@link #useChecksums}</dt>
     * <dd><code>true</code> iff record level checksums are enabled. The default
     * for earlier versions is <code>false</code>, which provides backward
     * compatibility for existing {@link IndexSegment} files.</dd>
     * </dl>
     */
    static transient final public int VERSION1 = 0x1;

	/**
	 * Version 2 of the serialization format replaced the int32 value for
	 * nentries with an int64 value.
	 */
	static transient final public int VERSION2 = 0x2;

    /**
     * The current serialization version.
     */
    static transient final public int currentVersion = VERSION2;
    
    /**
     * UUID for this {@link IndexSegment} (it is a unique identifier for the
     * index segment resource and is reported as the {@link UUID} of the
     * {@link IndexSegmentStore}).
     */
    final public UUID segmentUUID;

    /**
     * The #of bits in an 64-bit long integer address that are used to represent
     * the byte offset into the {@link IndexSegmentStore}.
     */
    final public int offsetBits;

    /**
     * The {@link IAddressManager} used to interpret addresses in the
     * {@link IndexSegmentStore}.
     */
    final IndexSegmentAddressManager am;
    
    /**
     * Height of the index segment (origin zero, so height := 0 means that
     * there is only a root leaf in the tree).
     */
    final public int height;

	/**
	 * The #of leaves serialized in the file.
	 * <p>
	 * Note: {@link IndexSegmentBuilder} is restricted to MAX_INT leaves in
	 * its build plan.
	 */
    final public int nleaves;

    /**
     * The #of nodes serialized in the file. If zero, then {@link #nleaves} MUST
     * be ONE (1) and the index consists solely of a root leaf.
	 * <p>
	 * Note: {@link IndexSegmentBuilder} is restricted to MAX_INT leaves in
	 * its build plan and there are always more leaves than nodes in a BTree
	 * so this is also an int32 value.
     */
    final public int nnodes;

    /**
     * The #of index entries serialized in the file (non-negative and MAY be
     * zero).
     */
    final public long nentries;

    /**
     * The maximum #of bytes in any node or leaf stored on the
     * {@link IndexSegment}.
     * <p>
     * Note: while this appears to be unused now, it is still of interest and
     * will be retained.
     */
    final public int maxNodeOrLeafLength;
    
    /*
     * begin {offset,size} region extent tuples for the various multi-record
     * regions defined in the store file. These use a long value for the
     * byteCount size each region can span many, many records.
     */
    
    /**
     * The offset of the contiguous region containing the serialized leaves in
     * the file.
     * <p>
     * Note: The offset must be equal to {@link #SIZE} since the leaves are
     * written immediately after the {@link IndexSegmentCheckpoint} record.
     */
    final public long offsetLeaves;
    
    /**
     * The #of bytes in the contiguous region containing the serialized leaves in
     * the file.
     */
    final public long extentLeaves;
    
    /**
     * The offset of the contiguous region containing the serialized nodes in
     * the file or <code>0L</code> iff there are no nodes in the file.
     */
    final public long offsetNodes;
    
    /**
     * The #of bytes in the contiguous region containing the serialized nodes in
     * the file or <code>0L</code> iff there are no nodes in the file.
     */
    final public long extentNodes;
    
    /**
     * The offset of the optional contiguous region containing the raw records
     * to be resolved by blob references or <code>0L</code> iff there are no
     * raw records in this region.
     */
    final public long offsetBlobs;

    /**
     * The #of bytes in the optional contiguous region containing the raw
     * records to be resolved by blob references or <code>0L</code> iff there
     * are no raw records in this region.
     */
    final public long extentBlobs;

    /*
     * begin: addresses for individual records.
     */
    
    /**
     * Address of the root node or leaf in the file.
     */
    final public long addrRoot;

    /**
     * The address of the {@link IndexMetadata} record.
     */
    final public long addrMetadata;
    
    /**
     * Address of the optional bloom filter and 0L iff no bloom filter
     * was constructed.
     */
    final public long addrBloom;

    /**
     * Address of the first leaf in the file.
     */
    final public long addrFirstLeaf;

    /**
     * Address of the last leaf in the file.
     */
    final public long addrLastLeaf;
    
    /*
     * end: addresses for individual records.
     */
    
    /**
     * Length of the file in bytes.
     */
    final public long length;

    /**
     * <code>true</code> iff the caller asserted that the {@link IndexSegment}
     * was a fused view of the source index (partition) as of the specified
     * {@link #commitTime}. <code>false</code> implies that the
     * {@link IndexSegment} is the result of an incremental build. This flag is
     * important when attempting a bottom up reconstruction of a scale-out index
     * from its components on various journals and {@link IndexSegmentStore}s.
     */
    final public boolean compactingMerge;

    /**
     * <code>true</code> iff record level checksums are in use for the
     * {@link IndexSegment}.
     * 
     * @see #VERSION1
     */
    final public boolean useChecksums;
    
    /**
     * The commit time associated with the view from which the
     * {@link IndexSegment} was generated. The {@link IndexSegment} state is
     * equivalent to the state of the view as of that timestamp. However, the
     * {@link IndexSegment} provides a view of only a single commit point in
     * contrast to the many commit points that are typically available on a
     * {@link Journal}.
     * <p>
     * Note: This field is written at the head and tail of the
     * {@link IndexSegmentCheckpoint} record. If the timestamps on that record
     * do not agree then the build operation probably failed while writing the
     * checkpoint record.
     */
    final public long commitTime;
    
    /**
     * The checksum for the serialized representation of the
     * {@link IndexSegmentCheckpoint} record. This is computed when the record
     * is serialized and verified when it is de-serialized.
     */
    private int checksum;

    /**
     * A read-only view of the serialized {@link IndexSegmentCheckpoint} record.
     */
    final private ByteBuffer buf;
    
    /**
     * Reads the {@link IndexSegmentCheckpoint} record for the
     * {@link IndexSegment}. The operation seeks to the start of the file and
     * uses relative reads with the file pointer.
     * 
     * @param raf
     *            The file.
     * 
     * @throws IOException
     *             If there is a IO problem.
     * 
     * @throws RootBlockException
     *             if the {@link IndexSegmentCheckpoint} record is invalid (it
     *             doubles as a root block), including if the total file length
     *             is not large enough to contain an valid
     *             {@link IndexSegmentCheckpoint} record.
     */
    public IndexSegmentCheckpoint(final RandomAccessFile raf) throws IOException {

        if (raf == null)
            throw new IllegalArgumentException();
        
        final long len = raf.length();
        
        if (len == 0L) {

            throw new RootBlockException(ERR_EMPTY);
            
        }
        
        if (raf.length() < SIZE) {

            // File is non-empty, but too small to contain a valid root block.
            throw new RootBlockException(ERR_TOO_SMALL);
            
        }
        
        // allocate buffer for the checkpoint record.
        ByteBuffer buf = ByteBuffer.allocate(SIZE);
        
        // read in the serialized checkpoint record.
        FileChannelUtility.readAll(new NOPReopener(raf), buf, 0L);
        
        // prepare for reading.
        buf.rewind();
        
        // extract the various fields.
        final int magic = buf.getInt();

        if (magic != MAGIC) {

            throw new RootBlockException("MAGIC: expected=" + MAGIC
                    + ", actual=" + magic);

        }

        final int version = buf.getInt();

        if (version < 0 || version > currentVersion) {

            throw new RootBlockException("unknown version=" + version);

        }

        final long timestamp0 = buf.getLong();
        
        segmentUUID = new UUID(buf.getLong()/*MSB*/, buf.getLong()/*LSB*/);

        offsetBits = buf.getInt();
        
        height = buf.getInt();
        
        nleaves = buf.getInt();
        
        nnodes = buf.getInt();

        long nentries = -1;
		if (version < VERSION2) {
			nentries = buf.getInt();
		} else {
			buf.getInt();
        }

        maxNodeOrLeafLength = buf.getInt();
        
        // regions.
        
        offsetLeaves = buf.getLong();
        extentLeaves = buf.getLong();

        offsetNodes = buf.getLong();
        extentNodes = buf.getLong();
        
        offsetBlobs = buf.getLong();
        extentBlobs = buf.getLong();
        
        // simple addresses.
        
        addrRoot = buf.getLong();

        addrMetadata = buf.getLong();

        addrBloom = buf.getLong();
        
        addrFirstLeaf = buf.getLong();

        addrLastLeaf = buf.getLong();
        
        // other data.
        
        length = buf.getLong();
        
        compactingMerge = buf.get() != 0;
        
        if (version >= VERSION1) {
            useChecksums = buf.get() != 0;
        } else {
//            // skip unused byte for prior versions.
//            buf.get();
            // record checksums were not used for prior versions.
            useChecksums = false;
        }

		if (version >= VERSION2) {
			nentries = buf.getLong();
		}
		this.nentries = nentries;
        
        // advance to beyond the end of the unused section.
		switch (version) {
		case VERSION0:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION0);
			break;
		case VERSION1:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION1);
			break;
		case VERSION2:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION2);
			break;
		default:
			throw new AssertionError();
		}
        
        // Note: this sets the instance field to the checksum read from the record!
        checksum = buf.getInt();
        
        final long timestamp1 = buf.getLong();
        
        this.commitTime = timestamp0;
        
        /*
         * Now do the validation steps.
         * 
         * Start with the checksum.
         * 
         * Then the timestamps.
         * 
         * Then the file length, etc.
         */
        
        // compute the checksum of the record.
        final int checksum = new ChecksumUtility().checksum(buf, 0, SIZE
                - (SIZEOF_CHECKSUM + SIZEOF_TIMESTAMP));
            
        if (checksum != this.checksum) {
            
            throw new RootBlockException("Bad checksum: expected="
                    + this.checksum + ", but actual=" + checksum);
                
        }
        
        if (timestamp0 != timestamp1) {

            throw new RootBlockException("Timestamps differ: " + timestamp0
                    + " vs " + timestamp1);
            
        }
        
        if (length != raf.length()) {

            throw new RootBlockException("Length differs: actual="
                    + raf.length() + ", expected=" + length);

        }
        
        am = new IndexSegmentAddressManager(this); 

        // more validation.
        validate();

        // save read-only view of the checkpoint record.
        buf.rewind();
        this.buf = buf.asReadOnlyBuffer();

        if (log.isInfoEnabled())
            log.info(this.toString());

    }

    /**
     * Create a new checkpoint record in preparation for writing it on a file
     * containing a newly constructed {@link IndexSegment}.
     * 
     * @todo javadoc.
     */
    public IndexSegmentCheckpoint(//
            //
            final int offsetBits,//
            // basic checkpoint record.
            final int height, //
            final int nleaves,//
            final int nnodes,//
            final long nentries,//
            //
            final int maxNodeOrLeafLength,//
            // region extents
            final long offsetLeaves, final long extentLeaves,//
            final long offsetNodes, final long extentNodes,// 
            final long offsetBlobs, final long extentBlobs,//
            // simple addresses
            final long addrRoot, //
            final long addrMetadata, //
            final long addrBloom,//
            final long addrFirstLeaf,//
            final long addrLastLeaf,//
            // misc.
            final long length,//
            final boolean compactingMerge,//
            final boolean useChecksums,
            final UUID segmentUUID,//
            final long commitTime//
    ) {
        
        /*
         * Copy the various fields to initialize the checkpoint record.
         */
        
        this.segmentUUID = segmentUUID;

        this.offsetBits = offsetBits;
        
        this.height = height;

        this.nleaves = nleaves;
        
        this.nnodes = nnodes;
        
        this.nentries = nentries;

        this.maxNodeOrLeafLength = maxNodeOrLeafLength;
        
        // region extents.
        
        this.offsetLeaves = offsetLeaves;
        this.extentLeaves = extentLeaves;
        
        this.offsetNodes = offsetNodes;
        this.extentNodes = extentNodes;

        this.offsetBlobs = offsetBlobs;
        this.extentBlobs = extentBlobs;

        // simple addresses

        this.addrRoot = addrRoot;

        this.addrMetadata = addrMetadata;

        this.addrBloom = addrBloom;

        this.addrFirstLeaf = addrFirstLeaf;
        
        this.addrLastLeaf = addrLastLeaf;
        
        // other data
        
        this.length = length;
        
        this.compactingMerge = compactingMerge;
        
        this.useChecksums = useChecksums;
        
        this.commitTime = commitTime;
        
        /*
         * Create the address manager using this checkpoint record (requires
         * that certain fields are initialized on the checkpoint record).
         */
        
        am = new IndexSegmentAddressManager(this);
        
        validate();
        
        buf = createView();
        
        if (log.isInfoEnabled())
            log.info(this.toString());
        
    }

    /**
     * Test validity of the {@link IndexSegmentCheckpoint} record.
     */
    public void validate() {
        
//        assert branchingFactor >= BTree.MIN_BRANCHING_FACTOR;
        
        // height is non-negative.
//        assert height >= 0;
        if (height < 0)
            throw new RootBlockException("height=" + height);
        
        // must be non-negative.
        if (nentries < 0)
            throw new RootBlockException("nentries=" + nentries);
        
//        if (nentries == 0) {
//
//            /*
//             * Empty index segment.
//             */
//
//            if (nleaves != 0)
//                throw new RootBlockException("empty index but nleaves="
//                        + nleaves);
//
//            if (nnodes != 0)
//                throw new RootBlockException("empty index but nnodes="
//                        + nnodes);
//        
//            if (maxNodeOrLeafLength != 0)
//                throw new RootBlockException(
//                        "empty index but maxNodeOrLeafLength="
//                                + maxNodeOrLeafLength);
//            
//            if (extentLeaves != 0L)
//                throw new RootBlockException("empty index but extentLeaves="
//                        + extentLeaves);
//
//            if (offsetLeaves != 0L)
//                throw new RootBlockException("empty index but offsetLeaves="
//                        + offsetLeaves);
//
//            if (extentNodes != 0L)
//                throw new RootBlockException("empty index but extentNodes="
//                        + extentNodes);
//
//            if (offsetNodes != 0L)
//                throw new RootBlockException("empty index but offsetNodes="
//                        + offsetNodes);
//            
//            if (addrFirstLeaf != 0L)
//                throw new RootBlockException("empty index but addrFirstLeaf="
//                        + addrFirstLeaf);
//            
//            if (addrLastLeaf != 0L)
//                throw new RootBlockException("empty index but addrLastLeaf="
//                        + addrLastLeaf);
//            
//        } else {
        {
        
        if (nleaves <= 0)
            throw new RootBlockException("nleaves=" + nleaves);
        
        // zero nodes is Ok - the B+Tree may be just a root leaf.
        if (nnodes < 0)
            throw new RootBlockException("nnodes=" + nnodes);
        
//        // #entries must fit within the tree height.
//        assert nentries <= Math.pow(branchingFactor,height+1);
        
//        assert maxNodeOrLeafLength > 0;
        if (maxNodeOrLeafLength <= 0)
            throw new RootBlockException("maxNodeOrLeafLength="
                    + maxNodeOrLeafLength);
        
        // validate addrLeaves
        {
            // assert addrLeaves != 0L;
            if (extentLeaves == 0L) {

                throw new RootBlockException("extentLeaves=" + extentLeaves);

            }

            // leaves start immediately after the checkpoint record.
            if (offsetLeaves != SIZE) {

                throw new RootBlockException("offsetLeaves=" + offsetLeaves
                        + ", but expecting " + SIZE);

            }
            
            if (offsetLeaves + extentLeaves > length) {

                throw new RootBlockException(
                        "The leaves region extends beyond the end of the file: leaves={extent="
                                + extentLeaves + ", offset=" + offsetLeaves
                                + "}, but length=" + length);
                
            }

            if (addrFirstLeaf == 0L)
                throw new RootBlockException("No address for the first leaf?");

            if (addrLastLeaf == 0L)
                throw new RootBlockException("No address for the first leaf?");

        }

        if(nnodes == 0) {

            /*
             * The root is a leaf. In this case there is only a single root leaf
             * and there are no nodes.
             */
            
            if (offsetNodes != 0L || extentNodes != 0L) {
            
                /*
                 * Since there is are no nodes, nodes offset and extent MUST be
                 * ZERO.
                 */
                
                throw new RootBlockException("nodes={extent=" + extentNodes
                        + ", offset=" + offsetNodes + "}, but expecting zero.");
                
            }

            if (am.getByteCount(addrRoot) != extentLeaves) {

                /*
                 * Since there is only a single root leaf, size of the root leaf
                 * record MUST equal extent of the leaves region.
                 */

                throw new RootBlockException("addrRoot("
                        + am.toString(addrRoot)
                        + ") : size is not equal to extentLeaves("
                        + extentLeaves + ")");
                
            }
            
//            assert am.getOffset(addrRoot) >= am.getOffset(addrLeaves);
//            assert am.getOffset(addrRoot) < length;

        } else {
        
            /*
             * The root is a node.
             */
            
            if (offsetNodes == 0L || extentNodes == 0L) {
            
                // the nodes region MUST exist.
                
                throw new RootBlockException("nodes={extent=" + extentNodes
                        + ", offset=" + offsetNodes + "}");
                
            }
            
            if (offsetNodes + extentNodes > length) {
                
                throw new RootBlockException(
                        "The nodes region extends beyond the end of the file: nodes={extent="
                                + extentNodes + ",offset=" + offsetNodes
                                + "}, but length=" + length);
                
            }

            if (am.getOffset(addrRoot) + am.getByteCount(addrRoot) > length) {
            
                throw new RootBlockException(
                        "The root node record extends beyond the end of the file: addrRoot="
                                + am.toString(addrRoot) + ", but length="
                                + length);
                
            }

//            assert am.getOffset(addrNodes) > am.getOffset(addrLeaves);
//            assert am.getOffset(addrNodes) < length;
//            assert am.getOffset(addrRoot) >= am.getOffset(addrNodes);
//            assert am.getOffset(addrRoot) < length;
        }

        }
        
        /*
         * @todo validate the blob, bloom, and metadata addresses and the
         * first/last leaf addresses as well and the total length of the file.
         */
        
//        if( addrBloom == 0L ) assert errorRate == 0.;
//        
//        if( errorRate != 0.) assert addrBloom != 0L;
        
//        assert commitTime != 0L;
        if (commitTime <= 0L) {

            throw new RootBlockException("commitTime=" + commitTime);
            
        }
        
//        assert segmentUUID != null;
        if (segmentUUID == null) {
            
            throw new RootBlockException("No segment UUID");
            
        }

    }
    
    /**
     * Returns a new view of the read-only {@link ByteBuffer} containing the
     * serialized representation of the {@link IndexSegmentCheckpoint} record.
     */
    public ByteBuffer asReadOnlyBuffer() {
        
        return buf.asReadOnlyBuffer(); // Note: a _new_ view.
        
    }
    
    /**
     * Serialize the {@link IndexSegmentCheckpoint} record onto a read-only
     * {@link ByteBuffer}.
     * 
     * @return The read-only {@link ByteBuffer}.
     */
    private ByteBuffer createView() {

        final ByteBuffer buf = ByteBuffer.allocate(SIZE);
        
        buf.putInt(MAGIC);

        buf.putInt(currentVersion);

        buf.putLong(commitTime);
        
        buf.putLong(segmentUUID.getMostSignificantBits());

        buf.putLong(segmentUUID.getLeastSignificantBits());

        buf.putInt(offsetBits);
                        
        buf.putInt(height);
        
        buf.putInt(nleaves);

        buf.putInt(nnodes);

		if (currentVersion < VERSION2) {
			if (nentries > Integer.MAX_VALUE)
				throw new RuntimeException();
			buf.putInt((int) nentries);
		} else {
			buf.putInt(0/* unused */);
		}

        buf.putInt(maxNodeOrLeafLength);
        
        // region extents.
        
        buf.putLong(offsetLeaves);
        buf.putLong(extentLeaves);
        
        buf.putLong(offsetNodes);
        buf.putLong(extentNodes);

        buf.putLong(offsetBlobs);
        buf.putLong(extentBlobs);

        // simple addresses
        
        buf.putLong(addrRoot);

        buf.putLong(addrMetadata);
        
        buf.putLong(addrBloom);
        
        buf.putLong(addrFirstLeaf);

        buf.putLong(addrLastLeaf);
        
        // other data.
        
        buf.putLong(length);

        buf.put((byte) (compactingMerge ? 1 : 0));

		if (currentVersion >= VERSION1) {
			buf.put((byte) (useChecksums ? 1 : 0));
		}

		if (currentVersion >= VERSION2) {
			buf.putLong(nentries);
        }

		// skip over the unused bytes.
		switch (currentVersion) {
		case VERSION0:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION0);
			break;
		case VERSION1:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION1);
			break;
		case VERSION2:
			buf.position(buf.position() + SIZEOF_UNUSED_VERSION2);
			break;
		default:
			throw new AssertionError();
		}
        
        // Note: this sets the instance field! 
        checksum = new ChecksumUtility().checksum(buf, 0, SIZE
                - (SIZEOF_CHECKSUM + SIZEOF_TIMESTAMP));
        
        buf.putInt(checksum); // checksum of the proceeding bytes.

        buf.putLong(commitTime);

        assert buf.position() == SIZE : "position=" + buf.position()
                + " but checkpoint record should be " + SIZE + " bytes";

        assert buf.limit() == SIZE;

        buf.rewind();

        // read-only view.
        return buf.asReadOnlyBuffer();

    }
    
    /**
     * Write the checkpoint record at the start of the file.
     * 
     * @param raf
     *            The file.
     *            
     * @throws IOException
     */
    public void write(final RandomAccessFile raf) throws IOException {

        FileChannelUtility.writeAll(raf.getChannel(), asReadOnlyBuffer(), 0L);

        if (log.isInfoEnabled()) {

            log.info("wrote checkpoint record: " + this);
            
        }

    }
    
    /**
     * A human readable representation of the {@link IndexSegmentCheckpoint}
     * record.
     */
    public String toString() {
 
        final StringBuilder sb = new StringBuilder();
        
        sb.append("magic="+Integer.toHexString(MAGIC));
        sb.append(", segmentUUID="+segmentUUID);
        sb.append(", offsetBits="+offsetBits);
        sb.append(", height=" + height);
        sb.append(", nleaves=" + nleaves);
        sb.append(", nnodes=" + nnodes);
        sb.append(", nentries=" + nentries);
        sb.append(", maxNodeOrLeafLength=" + maxNodeOrLeafLength);
        sb.append(", leavesRegion={extent=" + extentLeaves+", offset="+offsetLeaves+"}, avgLeafSize="+(nleaves==0?0:(extentLeaves/nleaves)));
        sb.append(", nodesRegion={extent=" + extentNodes+", offset="+offsetNodes+"}, avgNodeSize="+(nnodes==0?0:(extentNodes/nnodes)));
        sb.append(", blobsRegion={extent=" + extentBlobs+", offset="+offsetBlobs+"}");
        sb.append(", addrRoot=" + am.toString(addrRoot));
        sb.append(", addrFirstLeaf=" + am.toString(addrFirstLeaf));
        sb.append(", addrLastLeaf=" + am.toString(addrLastLeaf));
        sb.append(", addrMetadata=" + am.toString(addrMetadata));
        sb.append(", addrBloom=" + am.toString(addrBloom));
        sb.append(", length=" + length);
        sb.append(", compactingMerge=" + compactingMerge);
        sb.append(", useChecksums=" + useChecksums);
        sb.append(", checksum=" + checksum);
        sb.append(", commitTime=" + new Date(commitTime));

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: The checkpoint is assembled from the root block by the constructor.
     * There is no address from which it can be re-read.
     * 
     * @return <code>0L</code>
     */
    @Override
    public long getCheckpointAddr() {
        return 0L;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: The checkpoint is assembled from the root block by the constructor.
     * There is no address from which it can be re-read.
     * 
     * @return <code>false</code>
     */
    @Override
    public boolean hasCheckpointAddr() {
        return false;
    }

    @Override
    public long getMetadataAddr() {
        return addrMetadata;
    }

    @Override
    public long getRootAddr() {
        return addrRoot;
    }

    @Override
    public long getBloomFilterAddr() {
        return addrBloom;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getGlobalDepth() {
        return 0; // ZERO since not HTree.
    }

    @Override
    public long getNodeCount() {
        return nnodes;
    }

    @Override
    public long getLeafCount() {
        return nleaves;
    }

    @Override
    public long getEntryCount() {
        return nentries;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: There is no counter associated with an {@link IndexSegment}. The
     * counter is only available for the {@link BTree}.
     * 
     * @return <code>0L</code>
     */
    @Override
    public long getCounter() {
        return 0;
    }

    @Override
    public long getRecordVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public IndexTypeEnum getIndexType() {
        return IndexTypeEnum.BTree;
    }

}
