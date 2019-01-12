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
import java.nio.ByteBuffer;
import org.embergraph.counters.CounterSet;
import org.embergraph.rawstore.IAddressManager;
import org.embergraph.rawstore.IMRMW;
import org.embergraph.rawstore.IRawStore;

/*
* Interface for implementations of a buffer strategy as identified by a {@link BufferMode}. This
 * interface is designed to encapsulate the specifics of reading and writing slots and performing
 * operations to make an atomic commit.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IBufferStrategy extends IRawStore, IMRMW {

  /*
   * The next offset at which a data item would be written on the store as an offset into the
   * <em>user extent</em>.
   */
  long getNextOffset();

  /*
   * The buffer mode supported by the implementation
   *
   * @return The implemented buffer mode.
   */
  BufferMode getBufferMode();

  /** The initial extent. */
  long getInitialExtent();

  /*
   * The maximum extent allowable before a buffer overflow operation will be rejected.
   *
   * <p>Note: The semantics here differ from those defined by {@link Options#MAXIMUM_EXTENT}. The
   * latter specifies the threshold at which a journal will overflow (onto another journal) while
   * this specifies the maximum size to which a buffer is allowed to grow.
   *
   * <p>Note: This is <em>normally</em> zero (0L), which basically means that the maximum extent is
   * ignored by the {@link IBufferStrategy} but respected by the {@link AbstractJournal}, resulting
   * in a <i>soft limit</i> on journal overflow.
   *
   * @return The maximum extent permitted for the buffer -or- <code>0L</code> iff no limit is
   *     imposed.
   */
  long getMaximumExtent();

  /*
   * The current size of the journal in bytes. When the journal is backed by a disk file this is the
   * actual size on disk of that file. The initial value for this property is set by {@link
   * Options#INITIAL_EXTENT}.
   */
  long getExtent();

  /*
   * The size of the user data extent in bytes.
   *
   * <p>Note: The size of the user extent is always generally smaller than the value reported by
   * {@link #getExtent()} since the latter also reports the space allocated to the journal header
   * and root blocks.
   */
  long getUserExtent();

  /*
   * The size of the journal header, including MAGIC, version, and both root blocks. This is used as
   * an offset when computing the address of a record in an underlying file and is ignored by buffer
   * modes that are not backed by a file (e.g., transient) or that are memory mapped (since the map
   * is setup to skip over the header)
   */
  int getHeaderSize();

  /*
   * Either truncates or extends the journal.
   *
   * <p>Note: Implementations of this method MUST be synchronized so that the operation is atomic
   * with respect to concurrent writers.
   *
   * @param extent The new extent of the journal. This value represent the total extent of the
   *     journal, including any root blocks together with the user extent.
   * @exception IllegalArgumentException The user extent MAY NOT be increased beyond the maximum
   *     offset for which the journal was provisioned by {@link Options#OFFSET_BITS}.
   */
  void truncate(long extent);

  /*
   * Write the root block onto stable storage (ie, flush it through to disk).
   *
   * @param rootBlock The root block. Which root block is indicated by {@link
   *     IRootBlockView#isRootBlock0()}.
   * @param forceOnCommit Governs whether or not the journal is forced to stable storage and whether
   *     or not the file metadata for the journal is forced to stable storage. See {@link
   *     Options#FORCE_ON_COMMIT}.
   */
  void writeRootBlock(IRootBlockView rootBlock, ForceEnum forceOnCommitEnum);

  //    /*
//     * Rolls back the store to the prior commit point by restoring the last
  //     * written root block.
  //     *
  //     * @throws IllegalStateException
  //     *             if the store is not open
  //     * @throws IllegalStateException
  //     *             if no prior root block is on hand to be restored.
  //     *
  //     * @todo Right now the rollback is a single step to the previous root block.
  //     *       However, we could in fact maintain an arbitrary history for
  //     *       rollback by writing the rootblocks onto the store (in the user
  //     *       extent) and saving a reference to the prior root block on the
  //     *       {@link ICommitRecord} before we write the new root block.
  //     */
  //    public void rollback();

  /** Read the specified root block from the backing file. */
  ByteBuffer readRootBlock(boolean rootBlock0);

  /*
   * A block operation that transfers the serialized records (aka the written on portion of the user
   * extent) en mass from the buffer onto an output file. The buffered records are written "in
   * order" starting at the current position on the output file. The file is grown if necessary. The
   * file position is advanced to the last byte written on the file.
   *
   * <p>Note: Implementations of this method MUST be synchronized so that the operation is atomic
   * with respect to concurrent writers.
   *
   * @param out The file to which the buffer contents will be transferred.
   * @return The #of bytes written.
   * @throws IOException
   */
  long transferTo(RandomAccessFile out) throws IOException;

  /*
   * Seals the store against further writes and discards any write caches since they will no longer
   * be used. Buffered writes are NOT forced to the disk so the caller SHOULD be able to guarantee
   * that concurrent writers are NOT running. The method should be implemented such that concurrent
   * readers are NOT disturbed.
   *
   * @throws IllegalStateException if the store is closed.
   * @throws IllegalStateException if the store is read-only.
   */
  void closeForWrites();

  /** Return the performance counter hierarchy. */
  CounterSet getCounters();

  IAddressManager getAddressManager();

  /*
   * A method that removes assumptions of how a specific strategy determines whether a transaction
   * commit is required.
   *
   * @param block The root block held by the client, can be checked against the state of the Buffer
   *     Strategy
   * @return whether any modification has occurred.
   */
  boolean requiresCommit(IRootBlockView block);

  /*
   * A method that removes assumptions of how a specific strategy commits data. For most strategies
   * the action is void since the client WORM DISK strategy writes data as allocated. For the Read
   * Write Strategy more data must be managed as part of the protocol outside of the RootBlock, and
   * this is the method that triggers that management. The caller MUST provide appropriate
   * synchronization.
   */
  void commit();

  /*
   * A method that requires the implementation to discard its buffered write set (if any). The
   * caller is responsible for any necessary synchronization as part of the abort protocol.
   */
  void abort();

  /*
   * Return <code>true</code> if the store has been modified since the last {@link #commit()} or
   * {@link #abort()}.
   *
   * @return true if store has been modified since last {@link #commit()} or {@link #abort()}.
   */
  boolean isDirty();

  /*
   * The RWStrategy requires meta allocation info in the root block, this method is the hook to
   * enable access. The metaStartAddr is the address in the file where the allocation blocks are
   * stored.
   *
   * @return the metaStartAddr for the root block if any
   */
  long getMetaStartAddr();

  /*
   * The RWStrategy requires meta allocation info in the root block, this method is the hook to
   * enable access. The metaBitsAddr is the address in the file where the metaBits that control the
   * allocation of the allocation blocks themselves is stored.
   *
   * @return the metaBitsAddr for the root block if any
   */
  long getMetaBitsAddr();

  /** @return the number of bits available in the address to define offset */
  int getOffsetBits();

  /** @return the maximum record size supported by this strategy */
  int getMaxRecordSize();

  /*
   * Return <code>true</code> if the store uses per-record checksums. When <code>true</code>, an
   * additional 4 bytes are written after the record on the disk. Those bytes contain the checksum
   * of the record.
   */
  boolean useChecksums();

  //    /*
//     * Determines whether there are outstanding writes to the underlying store
  //     */
  //	public boolean isFlushed();

}
