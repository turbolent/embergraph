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
 * Created on Aug 1, 2012
 */
package org.embergraph.btree;

import cutthecrap.utils.striterators.ICloseableIterator;
import org.embergraph.rawstore.IRawStore;

/*
 * Generic data access methods defined for all persistence capable data structures.
 *
 * <p>TODO There should be a high level method to insert objects into the index (index "entries" not
 * tuples - the index will need to compute the appropriate key, etc. in an implementation dependent
 * manner).
 */
public interface ISimpleIndexAccess {

  /** The backing store. */
  IRawStore getStore();

  /*
   * Return the #of entries in the index.
   *
   * <p>Note: If the index supports deletion markers then the range count will be an upper bound and
   * may double count tuples which have been overwritten, including the special case where the
   * overwrite is a delete.
   *
   * @return The #of tuples in the index.
   * @see IRangeQuery#rangeCount()
   */
  long rangeCount();

  /*
   * Visit all entries in the index in the natural order of the index (dereferencing visited tuples
   * to the application objects stored within those tuples).
   */
  ICloseableIterator<?> scan();

  /** Remove all entries in the index. */
  void removeAll();
}
