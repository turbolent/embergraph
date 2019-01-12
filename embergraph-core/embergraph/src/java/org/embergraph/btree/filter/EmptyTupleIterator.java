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
 * Created on Dec 12, 2006
 */

package org.embergraph.btree.filter;

import java.util.NoSuchElementException;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;

/*
* Empty iterator.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmptyTupleIterator implements ITupleIterator {

  public static final transient ITupleIterator INSTANCE = new EmptyTupleIterator();

  private EmptyTupleIterator() {}

  public boolean hasNext() {
    return false;
  }

  public ITuple next() {
    throw new NoSuchElementException();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
