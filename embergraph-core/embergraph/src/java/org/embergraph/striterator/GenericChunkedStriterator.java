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
 * Created on Aug 10, 2008
 */

package org.embergraph.striterator;

import java.util.Iterator;

/*
 * Chunked streaming iterator class that supresses generic types. Striterator patterns are often
 * used to convert the type of the elements visited by the underlying iterator. That and the
 * covarying generics combine to make code using generics and striterators rather ugly.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class GenericChunkedStriterator<E> extends ChunkedStriterator<IChunkedIterator<E>, E> {

  /** @param src */
  //    @SuppressWarnings("unchecked")
  public GenericChunkedStriterator(final IChunkedIterator<E> src) {

    super(src);
  }

  /** @param src */
  //    @SuppressWarnings("unchecked")
  public GenericChunkedStriterator(final Iterator<E> src) {

    super(src);
  }

  /*
   * @param chunkSize
   * @param src
   */
  //    @SuppressWarnings("unchecked")
  public GenericChunkedStriterator(final int chunkSize, final Iterator<E> src) {

    super(chunkSize, src);
  }
}
