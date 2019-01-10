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
 * Created on Nov 15, 2008
 */

package org.embergraph.service.proxy;

import java.io.IOException;
import java.rmi.Remote;

import com.bigdata.relation.accesspath.IBuffer;

/**
 * A helper object that provides the API of {@link IBuffer} but whose methods
 * throw {@link IOException} and are therefore compatible with {@link Remote}
 * and {@link Exporter}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RemoteBufferImpl<E> implements RemoteBuffer<E> {

    private final IBuffer<E> buffer;
    
    public RemoteBufferImpl(final IBuffer<E> buffer) {

        if (buffer == null)
            throw new IllegalArgumentException();
        
        this.buffer = buffer;
        
    }

    public void add(E e) throws IOException {

        buffer.add(e);
        
    }

    public long flush() throws IOException {

        return buffer.flush();
        
    }

    public boolean isEmpty() throws IOException {

        return buffer.isEmpty();
        
    }

    public void reset() throws IOException {

        buffer.reset();
        
    }

    public int size() throws IOException {
        
        return buffer.size();
        
    }
    
}
