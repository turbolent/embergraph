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
 * Created on Dec 17, 2006
 */

package com.bigdata.io.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.log4j.Logger;

import com.bigdata.btree.IndexSegment;
import com.bigdata.io.ByteBufferInputStream;
import com.bigdata.io.ByteBufferOutputStream;

/**
 * Bulk data (de-)compressor used for leaves in {@link IndexSegment}s. The
 * compression and decompression operations of a given {@link RecordCompressor}
 * reuse a shared instance buffer. Any decompression result is valid only until
 * the next compression or decompression operation performed by that
 * {@link RecordCompressor}. When used in a single-threaded context this reduces
 * allocation while maximizing the opportunity for bulk transfers.
 * <p>
 * This class is NOT thread-safe.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RecordCompressor implements Externalizable, IRecordCompressor {

    protected static final Logger log = Logger.getLogger(CompressorRegistry.class);

    /**
     * 
     */
    private static final long serialVersionUID = -2028159717578047153L;

    /**
     * A huge portion of the cost associated with using {@link Deflater} is
     * the initialization of a new instance. Since this code is designed to
     * operate within a single-threaded environment, we just reuse the same
     * instance for each invocation.
     */
    private transient Deflater _deflater;

    final private transient Inflater _inflater = new Inflater();

    /**
     * Reused on each decompression request and reallocated if buffer size would
     * be exceeded. This will achieve a steady state sufficient to decompress
     * any given input in a single pass.
     */
    private transient byte[] _buf = new byte[1024];

    /**
     * The level specified to the ctor.
     */
    private int level;

    public String toString() {
        
        return getClass().getName() + "{level=" + level + "}";
        
    }
    
    /**
     * Create a record compressor.
     * 
     * @param level
     *            The compression level.
     * 
     * @see Deflater#BEST_SPEED
     * @see Deflater#BEST_COMPRESSION
     */
    public RecordCompressor(final int level) {

        _deflater = new Deflater(level);

        this.level = level;
        
    }

    /**
     * De-serialization constructor.
     */
    public RecordCompressor() {
        
    }

	public void compress(ByteBuffer bin, ByteBuffer out) {
		compress(bin, new ByteBufferOutputStream(out));
	}


	public ByteBuffer compress(ByteBuffer bin) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		compress(bin, out);
		
		if (log.isTraceEnabled())
			log.trace("Record compression from " + bin.limit() + " to " + out.size());
		
		return ByteBuffer.wrap(out.toByteArray());
	}

	public void compress(final ByteBuffer bin, final OutputStream os) {

        if (bin.hasArray() && bin.position() == 0
                && bin.limit() == bin.capacity()) {

            /*
             * The source buffer is backed by an array so we delegate using the
             * position() and limit() of the source buffer and the backing
             * array.
             */

            compress(bin.array(), bin.position(), bin.limit(), os);

            // Advance the position to the limit.
            bin.position(bin.limit());

        } else {

            /*
             * Figure out how much data needs to be written.
             */
            final int size = bin.remaining();

            /*
             * If the shared buffer is not large enough then reallocate it as a
             * sufficiently large buffer.
             */
            if (_buf.length < size) {

                _buf = new byte[size];

            }

            /*
             * Copy the data from the ByteBuffer into the shared instance
             * buffer.
             */
            bin.get(_buf, 0, size);

            /*
             * Compress the data onto the output stream.
             */
            compress(_buf, 0, size, os);

        }

    }

    public void compress(final byte[] bytes, final OutputStream os) {

        compress(bytes, 0, bytes.length, os);
        
    }
    
    public void compress(final byte[] bytes, final int off, final int len,
            final OutputStream os) {

        _deflater.reset(); // required w/ instance reuse.

        final DeflaterOutputStream dos = new DeflaterOutputStream(os, _deflater);

        try {

            /*
             * Write onto deflator that writes onto the output stream.
             */
            dos.write(bytes, off, len);

            /*
             * Flush and close the deflator instance.
             * 
             * Note: The caller is unable to do this as they do not have access
             * to the {@link Deflator}. However, if this flushes through to the
             * underlying sink then that could drive IOs without the application
             * being aware that synchronous IO was occurring.
             */
            dos.flush();

            dos.close();

        } catch (IOException ex) {

            throw new RuntimeException(ex);

        }

    }

    public ByteBuffer decompress(final ByteBuffer bin) {

        _inflater.reset(); // reset required by reuse.

        final int size = bin.limit();

        final InflaterInputStream iis = new InflaterInputStream(
                new ByteBufferInputStream(bin), _inflater, size);

        return decompress(iis);

    }

    public ByteBuffer decompress(final byte[] bin) {
     
        _inflater.reset(); // reset required by reuse.

        final int size = bin.length;
        
        final InflaterInputStream iis = new InflaterInputStream(
                new ByteArrayInputStream(bin), _inflater, size);

        return decompress(iis);
        
    }

    /**
     * This decompresses data into a shared instance byte[]. If the byte[] runs
     * out of capacity then a new byte[] is allocated with twice the capacity,
     * the data is copied into new byte[], and decompression continues. The
     * shared instance byte[] is then returned to the caller. This approach is
     * suited to single-threaded processes that achieve a suitable buffer size
     * and then perform zero allocations thereafter.
     * 
     * @return A read-only view onto a shared buffer. The data between
     *         position() and limit() are the decompressed data. The contents of
     *         this buffer are valid only until the next compression or
     *         decompression request. The position will be zero. The limit will
     *         be the #of decompressed bytes.
     */
    protected ByteBuffer decompress(final InflaterInputStream iis) {

        int off = 0;
        
        try {

            while (true) { // use bulk I/O.

                int capacity = _buf.length - off;

                if (capacity == 0) {

                    final byte[] tmp = new byte[_buf.length * 2];

                    System.arraycopy(_buf, 0, tmp, 0, off);

                    _buf = tmp;

                    capacity = _buf.length - off;

                }

                final int nread = iis.read(_buf, off, capacity);

                if (nread == -1)
                    break; // EOF.

                off += nread;

            }

        }

        catch (IOException ex) {

            throw new RuntimeException(ex);

        }
//
//      /*
//      * make an exact fit copy of the uncompressed data and return it to
//      * the caller.
//      */
//
//     byte[] tmp = new byte[off];
//
//     System.arraycopy(_buf, 0, tmp, 0, off);
//
////     return tmp;
//        return ByteBuffer.wrap(tmp, 0, off);

        return ByteBuffer.wrap(_buf, 0, off).asReadOnlyBuffer();

    }

    public void readExternal(final ObjectInput in) throws IOException,
            ClassNotFoundException {

        level = in.readInt();

        _deflater = new Deflater(level);

    }

    public void writeExternal(final ObjectOutput out) throws IOException {

        out.writeInt(level);

    }

}
