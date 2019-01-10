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
package org.embergraph.mdi;

import java.io.Serializable;
import java.util.UUID;

import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IndexSegment;
import org.embergraph.journal.AbstractJournal;
import org.embergraph.journal.Journal;
import org.embergraph.resources.ResourceManager;

/**
 * Interface for metadata about a {@link Journal} or {@link IndexSegment}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IResourceMetadata extends Serializable, Cloneable {

    /**
     * True iff this resource is an {@link IndexSegment}. Each
     * {@link IndexSegment} contains historical read-only data for exactly one
     * partition of a scale-out index.
     */
    public boolean isIndexSegment();
    
    /**
     * True iff this resource is a {@link Journal}. When the resource is a
     * {@link Journal}, there will be a named mutable btree on the journal that
     * is absorbing writes for one or more index partition of a scale-out index.
     */
    public boolean isJournal();
    
    /**
     * The name of the file containing the resource (this is always relative to
     * some local data directory).
     * <p>
     * Note: This property is primarily used for debugging. It is NOT used by
     * the {@link ResourceManager}. Instead, the {@link ResourceManager} builds
     * up the mapping from resource {@link UUID} to local filename during
     * startup.
     */
    public String getFile();
    
    /*
     * Note: size() was removed since (a) required a new instance of the
     * resource metadata object for the ManagedJournal each time we obtained an
     * iterator reading on a data service; and (b) the resources are the same
     * regardless of their size.  it is the file and UUID that identify them.
     * the file in the file system and the UUID in our code.
     */
//    /**
//     * The #of bytes in the store file.
//     */
//    public long size();

    /**
     * The unique identifier for the resource.
     * 
     * @see IRootBlockView#getUUID(), the UUID for an {@link AbstractJournal}.
     * 
     * @see IndexSegmentCheckpoint#segmentUUID, the UUID for an
     *      {@link IndexSegment}.
     */
    public UUID getUUID();
    
    /**
     * The commit time associated with the creation of this resource. When the
     * index is an {@link IndexSegment} this is the commit time of the view from
     * which that {@link IndexSegment} was generated. When the resource is a
     * {@link Journal}, the create time is the commit time associated with the
     * journal creation, which is generally an overflow operation. Regardless,
     * the create time MUST be assigned by the same time source that is used to
     * assign commit timestamps.
     */
    public long getCreateTime();

    /**
     * The commit time of the view from which the caller should read. For an
     * {@link IndexSegment}, this is always the same as {@link #getCreateTime()}
     * . For a {@link Journal}, this may be a specific commit time for a source
     * in an {@link ILocalBTreeView}. A value of <code>0L</code> indicates that
     * no specific commit time is indicated. For historical journals, this
     * implies a read from the lastCommitTime on the journal in order to
     * constitute the view. For the current journal, this implies a read at
     * whatever timestamp the caller desires.
     * 
     * @see IndexManager
     */
    public long getCommitTime();
    
    /**
     * The hash code of the {@link #getUUID() resource UUID}.
     */
    public int hashCode();

    /**
     * Compares two resource metadata objects for consistent state.
     *
     * @param o
     * 
     * @return
     */
    public boolean equals(IResourceMetadata o);

}
