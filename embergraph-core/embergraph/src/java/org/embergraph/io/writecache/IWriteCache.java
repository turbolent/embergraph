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
 * Created on Feb 10, 2010
 */

package org.embergraph.io.writecache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.embergraph.journal.IAtomicStore;
import org.embergraph.util.ChecksumError;

/**
 * Interface for a write cache with read back and the capability to update
 * records while they are still in the cache.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IWriteCache {

    /**
     * Write the record on the cache. This interface DOES NOT provide any
     * guarantee about the ordering of writes. Callers who require a specific
     * ordering must coordinate that ordering themselves, e.g., by synchronizing
     * across their writes onto the cache.
     * 
     * @param offset
     *            The file offset of that record in the backing file.
     * @param data
     *            The record. The bytes from the current
     *            {@link ByteBuffer#position()} to the
     *            {@link ByteBuffer#limit()} will be written and the
     *            {@link ByteBuffer#position()} will be advanced to the
     *            {@link ByteBuffer#limit()} . The caller may subsequently
     *            modify the contents of the buffer without changing the state
     *            of the cache (i.e., the data are copied into the cache).
     * @param chk
     *            The checksum of the <i>data</i> (optional). When checksums are
     *            not enabled this should be ZERO (0). When checksums are
     *            enabled, {@link #read(long)} will validate the checksum before
     *            returning <i>data</i>.
     * 
     * @return <code>true</code> iff the caller's record was transferred to the
     *         cache. When <code>false</code>, there is not enough room left in
     *         the write cache for this record.
     * 
     * @throws InterruptedException
     * @throws IllegalStateException
     *             If the buffer is closed.
     * @throws IllegalArgumentException
     *             If the caller's record is larger than the maximum capacity of
     *             cache (the record could not fit within the cache). The caller
     *             should check for this and provide special handling for such
     *             large records. For example, they can be written directly onto
     *             the backing channel.
     */
    public boolean write(final long offset, final ByteBuffer data, final int chk)
            throws InterruptedException;

    /**
     * Read a record from the write cache.
     * 
     * @param offset
     *            The file offset of that record in the backing file.
     * @param nbytes
     *            The length of the record (decoded from the address by the
     *            caller).
     * 
     * @return The data read -or- <code>null</code> iff the record does not lie
     *         within the {@link IWriteCache}. When non-null, this will be a
     *         newly allocated exact fit mutable {@link ByteBuffer} backed by a
     *         Java <code>byte[]</code>. The buffer will be flipped to prepare
     *         for reading (the position will be zero and the limit will be the
     *         #of bytes read). The data DOES NOT include the bytes used to code
     *         checksum even when checksums are enabled.
     * 
     * @throws InterruptedException
     * @throws IllegalStateException
     *             if the buffer is closed.
     * @throws ChecksumError
     *             if checksums are enabled and the checksum for the record
     *             could not be validated.
     */
    public ByteBuffer read(final long offset, final int nbytes) throws InterruptedException,
            ChecksumError;

    /**
     * Flush the writes to the backing channel but does not force anything to
     * the backing channel. The caller is responsible for managing when the
     * channel is forced to the disk (if it is backed by disk) and whether file
     * data or file data and file metadata are forced to the disk.
     * 
     * @throws IOException
     * @throws InterruptedException
     * 
     *             FIXME The [force] parameter is ignored and will be removed
     *             shortly.
     */
    public void flush(final boolean force) throws IOException,
            InterruptedException;

    /**
     * Flush the writes to the backing channel but does not force anything to
     * the backing channel. The caller is responsible for managing when the
     * channel is forced to the disk (if it is backed by disk) and whether file
     * data or file data and file metadata are forced to the disk.
     * 
     * @throws IOException
     * @throws TimeoutException
     * @throws InterruptedException
     * 
     *             FIXME The [force] parameter is ignored and will be removed
     *             shortly.
     */
    public boolean flush(final boolean force, final long timeout,
            final TimeUnit unit) throws IOException, TimeoutException,
            InterruptedException;

    /**
     * Reset the write cache, discarding any writes which have not been written
     * through to the backing channel yet. This method IS NOT responsible for
     * discarding writes which have been written through since those are in
     * general available for reading directly from the backing channel. The
     * abort protocol at the {@link IAtomicStore} level is responsible for
     * ensuring that processes do not see old data after an abort. This is
     * generally handled by re-loading the appropriate root block and
     * reinitializing various things from that root block.
     * 
     * @throws InterruptedException
     */
    public void reset() throws InterruptedException;
    
    /**
     * Permanently take the {@link IWriteCache} out of service. Dirty records
     * are discarded, not flushed.
     * 
     * @throws InterruptedException
     */
    public void close() throws InterruptedException;

}
