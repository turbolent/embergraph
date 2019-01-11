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
 * Created on Jun 5, 2009
 */

package org.embergraph.relation.accesspath;

import java.util.concurrent.Future;

/**
 * An {@link IBuffer} that may be closed. Instances of this interface are
 * normally drained by a worker thread.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRunnableBuffer<E> extends IBuffer<E> {

    /**
     * Add an element to the buffer.
     * <p>
     * Note: This method is constrained to throw the specified exception if the
     * buffer has been {@link #close()}d.
     * 
     * @param e
     *            The element
     * 
     * @throws BufferClosedException
     *             if the buffer has been {@link #close()}d.
     */
    public void add(E e);
    
    /**
     * Return <code>true</code> if the buffer is open.
     */
    public boolean isOpen();
    
    /**
     * Signal that no more data will be written on this buffer (this is required
     * in order for the iterator to know when no more data will be made
     * available).
     */
    public void close();

    /**
     * Signal abnormal termination of the process writing on the buffer. The
     * buffer will be closed. The iterator will report the <i>cause</i> via a
     * wrapped exception the next time any method on its interface is invoked.
     * The internal queue may be cleared once this method is invoked.
     * 
     * @param cause
     *            The exception thrown by the processing writing on the buffer.
     */
    public void abort(Throwable cause);
    
    /**
     * The {@link Future} for the worker task.
     * 
     * @return The {@link Future} -or- <code>null</code> if no {@link Future}
     *         has been set.
     * 
     * @todo There should be a generic type for this.
     */
    public Future getFuture();

}
