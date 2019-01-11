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
 * Created on Feb 17, 2007
 */

package org.embergraph.journal;

import org.embergraph.btree.IIndex;
import org.embergraph.counters.ICounterSetAccess;

/**
 * Interface for managing named indices.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IIndexManager extends IIndexStore, IGISTManager, ICounterSetAccess {

    /**
     * Return a view of the named index as of the specified timestamp.
     * 
     * @param name
     *            The index name.
     * @param timestamp
     *            A timestamp which represents either a possible commit time on
     *            the store or a read-only transaction identifier.
     * 
     * @return The index or <code>null</code> iff there is no index registered
     *         with that name for that timestamp.
     * 
     * @see IBTreeManager#getIndexLocal(String, long)
     */
    IIndex getIndex(String name, long timestamp); // non-GIST

    /**
     * Return true if the index manager supports group commit semantics.
     * 
     * @see #566 (NSS GROUP COMMIT)
     */
    boolean isGroupCommit();
    
}
