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
 * Created on Jul 7, 2008
 */

package org.embergraph.relation.accesspath;

import cutthecrap.utils.striterators.IFilterTest;
import java.io.Serializable;

/**
 * Filter for accepting or rejecting visited elements.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IElementFilter<E> extends IFilterTest, Serializable {

  //    /**
  //     * True iff the argument is matched by the filter.
  //     *
  //     * @param e
  //     *            An element.
  //     *
  //     * @return true iff the element is accepted by the filter.
  //     */
  //    public boolean isValid(E e);

  /**
   * Return true iff this this filter can be used on the specified object (filter on the object
   * class).
   *
   * <p>Note: This was added to make it possible filter out cases where the runtime type system was
   * throwing a {@link ClassCastException} in the {@link #isValid(Object)} implementation.
   *
   * @param o An object of some type.
   * @return <code>true</code> if the element can be inspected by this filter.
   */
  boolean canAccept(Object o);
}
