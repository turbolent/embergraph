/*

Copyright (C) SYSTAP, LLC 2006-2015.  All rights reserved.

Contact:
     SYSTAP, LLC
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@systap.com

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
 * Created on Jan 12, 2009
 */

package com.bigdata.zookeeper;

/**
 * Flags for the {@link HierarchicalZNodeWatcher}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface HierarchicalZNodeWatcherFlags {

    /**
     * No watches.
     */
    public static final int NONE = 0;
    
    /**
     * Watch znode create/destroy.
     */
    public static final int EXISTS = 1 << 0;

    /**
     * Watch znode data.
     */
    public static final int DATA = 1 << 1;

    /**
     * Watch znode children.
     */
    public static final int CHILDREN = (1 << 2);

    /**
     * Shorthand for [{@link #EXISTS}, {@link #DATA}, {@link #CHILDREN}].
     */
    public static final int ALL = (EXISTS | DATA | CHILDREN);

}