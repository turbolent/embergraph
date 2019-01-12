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
 * Created on Jun 5, 2009
 */

package org.embergraph.service.proxy;

import java.io.IOException;
import java.rmi.Remote;
import java.util.concurrent.Future;
import org.embergraph.relation.accesspath.IRunnableBuffer;

/*
* {@link Remote} interface declaring the API of {@link IRunnableBuffer} but also declaring that
 * each methods throws {@link IOException} in order to be compatible with {@link Remote} and
 * Exporter. Of course, this means that this interface can not extend {@link IRunnableBuffer}!
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E> The generic type of the elements in the buffer.
 * @param <V> The generic type of the result of the method call.
 */
public interface RemoteRunnableBuffer<E, V> extends RemoteBuffer<E> {

  void add(E e) throws IOException;

  boolean isOpen() throws IOException;

  void close() throws IOException;

  void abort(Throwable cause) throws IOException;

  Future<V> getFuture() throws IOException;
}
