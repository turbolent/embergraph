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
package org.embergraph.journal;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.embergraph.io.FileChannelUtility;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.rawstore.AbstractRawWormStore;
import org.embergraph.resources.ResourceManager;
import org.embergraph.util.Bytes;

/*
 * Abstract base class for {@link IBufferStrategy} implementation.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractBufferStrategy extends AbstractRawWormStore
    implements IBufferStrategy {

  /** Log for buffer operations. */
  protected static final Logger log = Logger.getLogger(AbstractBufferStrategy.class);

  protected static final boolean WARN = log.getEffectiveLevel().toInt() <= Level.WARN.toInt();

  /*
   * Text of the error message used when a {@link ByteBuffer} with zero bytes {@link
   * ByteBuffer#remaining()} is passed to {@link #write(ByteBuffer)}.
   */
  public static final String ERR_BUFFER_EMPTY = "Zero bytes remaining in buffer";

  /*
   * Text of the error message used when a <code>null</code> reference is provided for a {@link
   * ByteBuffer}.
   */
  public static final String ERR_BUFFER_NULL = "Buffer is null";

  /*
   * Text of the error message used when an address is given has never been written. Since the
   * journal is an append-only store, an address whose offset plus record length exceeds the {@link
   * #nextOffset} on which data would be written may be easily detected.
   */
  public static final String ERR_ADDRESS_NOT_WRITTEN = "Address never written.";

  /*
   * Text of the error message used when a ZERO (0L) is passed as an address to {@link
   * IRawStore#read(long)} or similar methods. This value 0L is reserved to indicate a persistent
   * null reference and may never be read.
   */
  public static final String ERR_ADDRESS_IS_NULL = "Address is 0L";

  /*
   * Text of the error message used when an address provided to {@link IRawStore#read(long)} or a
   * similar method encodes a record length of zero (0). Empty records are not permitted on write
   * and addresses with a zero length are rejected on read.
   */
  public static final String ERR_RECORD_LENGTH_ZERO = "Record length is zero";

  /*
   * Text of the error message used when a write operation would exceed the maximum extent for a
   * backing store.
   */
  public static final String ERR_MAX_EXTENT = "Would exceed maximum extent.";

  /*
   * Text of the error message used when {@link IBufferStrategy#truncate(long)} would truncate data
   * that has already been written.
   */
  public static final String ERR_TRUNCATE = "Would truncate written data.";

  /** Error message used when the writes are not allowed. */
  public static final String ERR_READ_ONLY = "Read only";

  /*
   * Error message used when the record size is invalid (e.g., negative).
   *
   * @todo There is some overlap with {@link #ERR_RECORD_LENGTH_ZERO} and {@link #ERR_BUFFER_EMPTY}.
   */
  public static final String ERR_BAD_RECORD_SIZE = "Bad record size";

  /*
   * Error message used when the store is closed but the operation requires that the store is open.
   */
  public static final String ERR_NOT_OPEN = "Not open";

  /*
   * Error message used when the store is open by the operation requires that the store is closed.
   */
  public static final String ERR_OPEN = "Open";

  /*
   * Error message used when an operation would write more data than would be permitted onto a
   * buffer.
   */
  public static final String ERR_BUFFER_OVERRUN = "Would overrun buffer";

  /** <code>true</code> iff the {@link IBufferStrategy} is open. */
  private volatile boolean open = false;

  /** <code>true</code> iff the {@link IBufferStrategy} is read-only. */
  private boolean readOnly;

  //    private final UUID storeUUID;

  protected final long initialExtent;
  protected final long maximumExtent;

  /** The buffer strategy implemented by this class. */
  protected final BufferMode bufferMode;

  /*
   * The next offset at which a data item would be written on the store as an offset into the
   * <em>user extent</em> (offset zero(0) addresses the first byte after the root blocks). This is
   * updated each time a new record is written on the store. On restart, the value is initialized
   * from the current root block. The current value is written as part of the new root block during
   * each commit.
   *
   * <p>Note: It is NOT safe to reload the current root block and therefore reset this to an earlier
   * offset unless all transactions are discarded. The reason is that transactions may use objects
   * (btrees) to provide isolation. Those objects write on the store but do not register as {@link
   * ICommitter}s and therefore never make themselves restart safe. However, you can not discard the
   * writes of those objects unless the entire store is being restarted, e.g., after a shutdown or a
   * crash.
   *
   * <p>Note: An {@link AtomicLong} is used to provide an object on which we can lock when assigning
   * the next record's address and synchronously updating the counter value. It also ensures that
   * threads can not see a stale value for the counter.
   */
  protected final AtomicLong nextOffset;

  /** The WORM address of the last committed allocation. */
  protected final AtomicLong commitOffset;

  static final NumberFormat cf;

  static {
    cf = NumberFormat.getIntegerInstance();

    cf.setGroupingUsed(true);
  }

  //    final public UUID getUUID() {
  //
  //        return storeUUID;
  //
  //    }

  public final long getInitialExtent() {

    return initialExtent;
  }

  public final long getMaximumExtent() {

    return maximumExtent;
  }

  /** The minimum amount to extend the backing storage when it overflows. */
  protected long getMinimumExtension() {

    return Bytes.megabyte * 32;
  }

  public final BufferMode getBufferMode() {

    return bufferMode;
  }

  public final long getNextOffset() {

    return nextOffset.get();
  }

  /*
   * (Re-)open a buffer.
   *
   * @param storeUUID The UUID that identifies the owning {@link IRawStore}.
   * @param initialExtent - as defined by {@link #getInitialExtent()}
   * @param maximumExtent - as defined by {@link #getMaximumExtent()}.
   * @param offsetBits The #of bits that will be used to represent the byte offset in the 64-bit
   *     long integer addresses for the store. See {@link WormAddressManager}.
   * @param nextOffset The next offset within the buffer on which a record will be written. Note
   *     that the buffer begins _after_ the root blocks and offset zero is always the first byte in
   *     the buffer.
   * @param bufferMode The {@link BufferMode}.
   */
  AbstractBufferStrategy(
      // UUID storeUUID,
      long initialExtent,
      long maximumExtent,
      int offsetBits,
      long nextOffset,
      BufferMode bufferMode,
      boolean readOnly) {

    super(offsetBits);

    assert nextOffset >= 0;

    if (bufferMode == null) throw new IllegalArgumentException();

    //        this.storeUUID = storeUUID;

    this.initialExtent = initialExtent;

    this.maximumExtent = maximumExtent; // MAY be zero!

    this.nextOffset = new AtomicLong(nextOffset);

    this.commitOffset = new AtomicLong(nextOffset);

    this.bufferMode = bufferMode;

    this.open = true;

    this.readOnly = readOnly;
  }

  public final long size() {

    return nextOffset.get();
  }

  protected final void assertOpen() {

    if (!open) throw new IllegalStateException(ERR_NOT_OPEN);
  }

  public boolean isOpen() {

    return open;
  }

  public boolean isReadOnly() {

    assertOpen();

    return readOnly;
  }

  /** Manages the {@link #open} flag state. */
  public void close() {

    if (!open) throw new IllegalStateException();

    open = false;
  }

  public final void destroy() {

    if (open) close();

    deleteResources();
  }

  /*
   * Invoked if the store would exceed its current extent by {@link #write(ByteBuffer)}. The default
   * behavior extends the capacity of the buffer by the at least the requested amount and a maximum
   * of 32M or the {@link Options#INITIAL_EXTENT}.
   *
   * <p>If the data are fully buffered, then the maximum store size is limited to int32 bytes which
   * is the maximum #of bytes that can be addressed in RAM (the pragmatic maximum is slightly less
   * than 2G due to the limits of the JVM to address system memory).
   *
   * @return true if the capacity of the store was extended and the write operation should be
   *     retried.
   */
  public final boolean overflow(final long needed) {

    final long userExtent = getUserExtent();

    final long required = userExtent + needed;

    if (required > bufferMode.getMaxExtent()) {

      /*
       * Would overflow int32 bytes and data are buffered in RAM.
       */

      log.error(ERR_MAX_EXTENT);

      return false;
    }

    if (maximumExtent != 0L && required > maximumExtent) {

      /*
       * Would exceed the maximum extent (iff a hard limit).
       *
       * Note: this will show up for transactions that whose write set
       * overflows the in-memory buffer onto the disk.
       */

      if (WARN) log.warn("Would exceed maximumExtent=" + maximumExtent);

      return false;
    }

    /*
     * Increase by the initial extent or by 32M, whichever is greater, but
     * by no less that the requested amount.
     */
    long newExtent = userExtent + Math.max(needed, Math.max(initialExtent, getMinimumExtension()));

    if (newExtent > bufferMode.getMaxExtent()) {

      /*
       * Do not allocate more than the maximum extent.
       */

      newExtent = bufferMode.getMaxExtent();

      if (newExtent - userExtent < needed) {

        /*
         * Not enough room for the requested extension.
         */

        log.error(ERR_MAX_EXTENT);

        return false;
      }
    }

    /*
     * Extend the capacity.
     */
    truncate(newExtent);

    // report event.
    ResourceManager.extendJournal(getFile() == null ? null : getFile().toString(), newExtent);

    // Retry the write operation.
    return true;
  }

  /*
   * Helper method used by {@link DiskBackedBufferStrategy} and {@link DiskOnlyStrategy} to
   * implement {@link IBufferStrategy#transferTo(RandomAccessFile)} using a {@link FileChannel} to
   * {@link FileChannel} transfer.
   *
   * @param src The source.
   * @param out The output file.
   * @return The #of bytes transferred.
   * @throws IOException
   */
  protected static long transferFromDiskTo(final IDiskBasedStrategy src, final RandomAccessFile out)
      throws IOException {

    // We want everything after the file header.
    final long fromPosition = src.getHeaderSize();

    // #of bytes to transfer (everything in the user extent).
    final long count = src.getNextOffset();

    // the source channel.
    final FileChannel srcChannel = src.getChannel();

    // the output channel.
    final FileChannel outChannel = out.getChannel();

    // the current file position on the output channel.
    final long outPosition = outChannel.position();

    //        final long outSize = outChannel.size();
    //
    //        final long outRemaining = outSize - outPosition;

    /*
     * Transfer the user extent from the source channel onto the output
     * channel starting at its current file position. The output channel
     * will be transparently extended if necessary.
     *
     * Note: this has a side-effect on the position for both the source and
     * output channels.
     *
     * Note: If the last record on the source was allocated but not written
     * then the data transfer operation will fail since the source channel
     * will not have enough data.
     */
    FileChannelUtility.transferAll(srcChannel, fromPosition, count, out, outPosition);

    return count;

    //        final long begin = System.currentTimeMillis();
    //
    //        // the output channel.
    //        final FileChannel outChannel = out.getChannel();
    //
    //        // current position on the output channel.
    //        final long toPosition = outChannel.position();
    //
    //        /*
    //         * Transfer data from channel to channel.
    //         */
    //
    //        /*
    //         * Extend the output file. This is required at least for some
    //         * circumstances.
    //         */
    //        out.setLength(toPosition+count);
    //
    //        /*
    //         * Transfer the data. It is possible that this will take multiple
    //         * writes for at least some implementations.
    //         */
    //
    //        if (log.isInfoEnabled())
    //            log.info("fromPosition="+tmpChannel.position()+", toPosition="+toPosition+",
    // count="+count);
    //
    //        int nwrites = 0; // #of write operations.
    //
    //        {
    //
    //            long n = count;
    //
    //            long to = toPosition;
    //
    //            while (n > 0) {
    //
    //                if (log.isInfoEnabled())
    //                    log.info("to=" + toPosition+", remaining="+n+", nwrites="+nwrites);
    //
    //                long nxfer = outChannel.transferFrom(tmpChannel, to, n);
    //
    //                to += nxfer;
    //
    //                n -= nxfer;
    //
    //                nwrites++;
    //
    ////        // Verify transfer is complete.
    ////            if (nxfer != count) {
    //
    ////                throw new IOException("Expected to transfer " + count
    ////                        + ", but transferred " + nxfer);
    //
    ////            }
    //
    //            }
    //
    //        }
    //
    //        /*
    //         * Update the position on the output channel since transferFrom does
    //         * NOT do this itself.
    //         */
    //        outChannel.position(toPosition+count);
    //
    //        final long elapsed = System.currentTimeMillis() - begin;
    //
    //        log.warn("Transferred " + count
    //                + " bytes from disk channel to disk channel (offset="
    //                + toPosition + ") in " + nwrites + " writes and " + elapsed
    //                + "ms");
    //
    //        return count;

  }

  /*
   * Not supported - this is available on the {@link AbstractJournal}.
   *
   * @throws UnsupportedOperationException always
   */
  public UUID getUUID() {

    throw new UnsupportedOperationException();
  }

  /*
   * Not supported - this is available on the {@link AbstractJournal}.
   *
   * @throws UnsupportedOperationException always
   */
  public IResourceMetadata getResourceMetadata() {

    throw new UnsupportedOperationException();
  }

  /*
   * Sets the <code>readOnly</code> flag.
   *
   * <p>Note: This method SHOULD be extended to release write caches, etc.
   */
  public void closeForWrites() {

    if (isReadOnly()) {

      throw new IllegalStateException();
    }

    readOnly = true;
  }

  /*
   * These are default implementations of methods defined for the R/W store
   * which are NOPs for the WORM store.
   */

  /** The default is a NOP. */
  @Override
  public void delete(long addr) {

    // NOP for WORM.

  }

  /*
   * {@inheritDoc}
   *
   * <p>This implementation checks the current allocation offset with that in the rootBlock
   *
   * @return true if store has been modified since last commit()
   */
  @Override
  public boolean isDirty() {

    return commitOffset.get() != nextOffset.get();
  }

  @Override
  public void commit() {

    // remember offset at commit
    commitOffset.set(nextOffset.get());
  }

  @Override
  public void abort() {

    // restore the last committed value for nextOffset.
    nextOffset.set(commitOffset.get());
  }

  public long getMetaBitsAddr() {

    // NOP for WORM.
    return 0;
  }

  public long getMetaStartAddr() {
    // NOP for WORM.
    return 0;
  }

  public boolean requiresCommit(IRootBlockView block) {
    return getNextOffset() > block.getNextOffset();
  }

  /*
   * The maximum size of a record for the address manager less 4 bytes iff checksums are enabled.
   */
  public int getMaxRecordSize() {

    return getAddressManager().getMaxByteCount() - (useChecksums() ? 4 : 0);
  }

  /*
   * <code>false</code> by default since these were added for HA with the {@link WORMStrategy} and
   * the {@link RWStrategy}.
   */
  public boolean useChecksums() {
    return false;
  }

  //
  //    /*
  //     * {@inheritDoc}
  //     * <p>
  //     * Note: By default there is no WriteCache to buffer any writes
  //     *
  //     * @return <code>true</code> unless overridden.
  //     */
  //	public boolean isFlushed() {
  //		return true;
  //	}
}
