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
 * Created on Sep 28, 2010
 */

package com.bigdata.bop;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Annotations for an operator using an internal hash map.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface HashMapAnnotations {

    /**
     * The initial capacity of the {@link ConcurrentHashMap} used to impose the
     * distinct constraint.
     * 
     * @see #DEFAULT_INITIAL_CAPACITY
     */
    String INITIAL_CAPACITY = HashMapAnnotations.class.getName()
            + ".initialCapacity";

    int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The load factor of the {@link ConcurrentHashMap} used to impose the
     * distinct constraint.
     * 
     * @see #DEFAULT_LOAD_FACTOR
     */
    String LOAD_FACTOR = HashMapAnnotations.class.getName() + ".loadFactor";

    float DEFAULT_LOAD_FACTOR = .75f;

}
