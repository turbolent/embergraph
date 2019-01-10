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
package org.embergraph.btree;

import org.embergraph.cache.HardReferenceQueue;
import org.embergraph.cache.HardReferenceQueueEvictionListener;

/**
 * Wraps the basic implementation and exposes a protected method that we
 * need to write the tests in this suite.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T>
 */
public class MyHardReferenceQueue<T> extends HardReferenceQueue<T> {

    public MyHardReferenceQueue(HardReferenceQueueEvictionListener<T> listener, int capacity) {
        super(listener, capacity);
    }
    
    public MyHardReferenceQueue(HardReferenceQueueEvictionListener<T> listener, int capacity, int nscan) {
        super(listener, capacity,nscan);
    }
    
}
