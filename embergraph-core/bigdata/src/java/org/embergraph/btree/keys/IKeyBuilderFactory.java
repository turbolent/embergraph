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
 * Created on Jul 3, 2008
 */

package org.embergraph.btree.keys;

/**
 * A factory for pre-configured {@link IKeyBuilder} instances.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IKeyBuilderFactory {

    /**
     * Return an instance of the configured {@link IKeyBuilder}.
     */
    public IKeyBuilder getKeyBuilder();
    
    /**
     * Return an instance of the configured {@link IKeyBuilder} that has been
     * overridden to have {@link StrengthEnum#Primary} collation strength. This
     * may be used to form successors for Unicode prefix scans without having
     * the secondary sort ordering characteristics mucking things up.
     * 
     * @see <a href="http://trac.blazegraph.com/ticket/974" >
     *      Name2Addr.indexNameScan(prefix) uses scan + filter </a>
     */
    public IKeyBuilder getPrimaryKeyBuilder();
    
}
