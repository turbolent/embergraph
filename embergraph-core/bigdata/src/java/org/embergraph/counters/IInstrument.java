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
 * Created on Mar 13, 2008
 */

package org.embergraph.counters;

/**
 * Interface used to construct a counter that reports on an instrumented value.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IInstrument<T> {

    /** Obtain a sample. */
    public T getValue();
    
    /** Obtain the timestamp for the last collected sample. */
    public long lastModified();

    /**
     * Set the current value.
     * 
     * @param value
     *            The sampled value.
     * @param timestamp
     *            The timestamp for that sample.
     * 
     * @throws UnsupportedOperationException
     *             if this operation is not allowed.
     */
    public void setValue(T value,long timestamp);
    
}
