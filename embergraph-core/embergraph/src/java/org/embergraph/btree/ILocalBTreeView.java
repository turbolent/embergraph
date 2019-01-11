/*

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
 * Created on May 30, 2008
 */

package org.embergraph.btree;

import org.embergraph.btree.view.FusedView;

/**
 * Interface indicates that the index is local rather than remote. A local index
 * may consistent of either an {@link AbstractBTree} or a {@link FusedView} of
 * {@link AbstractBTree}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface ILocalBTreeView extends IIndex {

    /**
	 * The #of {@link AbstractBTree}s sources for the view. This will be ONE (1)
	 * if the view is a {@link BTree}. 
	 */
    public int getSourceCount();

    /**
     * An array containing the ordered sources in the view. Changes to the array
     * DO NOT affect the view. If the view is an {@link AbstractBTree} then the
     * array will contain a single element which is that {@link AbstractBTree}.
     */
    public AbstractBTree[] getSources();

    /**
     * The {@link BTree} that is absorbing writes for the view.
     * 
     * @throws UnsupportedOperationException
     *             if the index is not mutable.
     */
    public BTree getMutableBTree();

    /**
     * Return the bloom filter.
     * 
     * @return The bloom filter if one exists and otherwise <code>null</code>.
     */
    public IBloomFilter getBloomFilter();
    
}
