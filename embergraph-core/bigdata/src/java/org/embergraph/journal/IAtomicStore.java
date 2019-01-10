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
 * Created on Feb 3, 2007
 */

package org.embergraph.journal;

import org.embergraph.rawstore.IRawStore;

/**
 * Interface for low-level operations on a store supporting an atomic commit.
 * Persistent implementations of this interface are restart-safe.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IAtomicStore extends IRawStore {

    /**
     * Abandon the current write set (synchronous).
     */
    public void abort();
    
    /**
     * Atomic commit (synchronous).
     * <p>
     * Note: if the commit fails (by throwing any kind of exception) then you
     * MUST invoke {@link #abort()} to abandon the current write set.
     * 
     * @return The timestamp assigned to the {@link ICommitRecord} -or- 0L if
     *         there were no data to commit.
     * 
     * @exception IllegalStateException
     *                if the store is not open.
     * @exception IllegalStateException
     *                if the store is not writable.
     */
    public long commit();

    /**
     * Return <code>true</code> if the store has been modified since the last
     * {@link #commit()} or {@link #abort()}.
     * 
     * @return true if store has been modified since last {@link #commit()} or
     *         {@link #abort()}.
     */
    public boolean isDirty();
    
    /**
     * Set a persistence capable data structure for callback during the commit
     * protocol.
     * <p>
     * Note: the committers must be reset after restart or whenever
     * 
     * @param index
     *            The slot in the root block where the address of the
     *            {@link ICommitter} will be recorded.
     * 
     * @param committer
     *            The committer.
     */
    public void setCommitter(int index, ICommitter committer);

    /**
     * The last address stored in the specified root slot as of the last
     * committed state of the store.
     * 
     * @param index
     *            The index of the root address to be retrieved.
     * 
     * @return The address stored at that index.
     * 
     * @exception IndexOutOfBoundsException
     *                if the index is negative or too large.
     */
    public long getRootAddr(int index);

    /**
     * Return a read-only view of the current root block.
     * 
     * @return The current root block.
     */
    public IRootBlockView getRootBlockView();

    /**
     * Return the {@link ICommitRecord} for the most recent committed state
     * whose commit timestamp is less than or equal to <i>timestamp</i>. This
     * is used by a {@link Tx transaction} to locate the committed state that is
     * the basis for its operations.
     * 
     * @param timestamp
     *            The timestamp of interest.
     * 
     * @return The {@link ICommitRecord} for the most recent committed state
     *         whose commit timestamp is less than or equal to <i>timestamp</i>
     *         -or- <code>null</code> iff there are no {@link ICommitRecord}s
     *         that satisfy the probe.
     */
    public ICommitRecord getCommitRecord(long timestamp);

    /*
     * These methods have been removed from the public interface. They were only
     * used by the test suite. Further, there were problems with the
     * implementations.
     */
    
//	/**
//	 * Return the root block view associated with the commitRecord for the
//	 * provided commit time.  This requires accessing the next commit record
//	 * since it is the previous root block that is referenced from each record.
//	 * 
//	 * @param commitTime
//	 *            A commit time.
//	 * 
//	 * @return The root block view -or- <code>null</code> if there is no commit
//	 *         record for that commitTime.
//	 */
//	public IRootBlockView getRootBlock(final long commitTime);
//	
//	/**
//	 * 
//	 * @param startTime from which to begin iteration
//	 * 
//	 * @return an iterator over the committed root blocks
//	 */
//	public Iterator<IRootBlockView> getRootBlocks(final long startTime);

}
