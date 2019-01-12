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
 * Created on Aug 27, 2008
 */

package org.embergraph.service.proxy;

import java.io.IOException;
import java.rmi.Remote;
import org.embergraph.striterator.IChunkedIterator;

/*
* Interface for objects proxying for asynchronous chunked iterators. This is used to export
 * iterators. We wrap an {@link IChunkedIterator} with an object that implements this interface, and
 * then export a proxy for that object. On the client, we wrap the proxy so as to hide the {@link
 * IOException}s and regain our original interface signature.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E> The generic type of the elements visited by the source iterator.
 */
public interface IRemoteChunkedIterator<E> extends Remote {

  /*
   * Close the remote iterator.
   *
   * @throws IOException
   */
  void close() throws IOException;

  /*
   * Return the next "chunk" from the iterator.
   *
   * @return The next {@link IRemoteChunk}.
   */
  IRemoteChunk<E> nextChunk() throws IOException;
}
