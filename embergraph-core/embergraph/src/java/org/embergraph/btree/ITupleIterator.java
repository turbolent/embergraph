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
 * Created on Dec 11, 2006
 */

package org.embergraph.btree;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.embergraph.btree.filter.TupleFilter;

/*
* Interface visits {@link ITuple}s populated with the data and metadata for visited index entries.
 *
 * @see IRangeQuery
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface ITupleIterator<E> extends Iterator<ITuple<E>> {

  /*
   * Advance the iterator and return the {@link ITuple} from which you can extract the data and
   * metadata for next entry.
   *
   * <p>Note: An {@link ITupleIterator}s will generally return the <em>same</em> {@link ITuple}
   * reference on on each invocation of this method. The caller is responsible for copying out any
   * data or metadata of interest before calling {@link #next()} again. See {@link TupleFilter}
   * which is aware of this and can be used to stack filters safely.
   *
   * @return The {@link ITuple} containing the data and metadata for the current index entry.
   * @throws NoSuchElementException if there is no next entry.
   */
  @Override
  ITuple<E> next();
}
