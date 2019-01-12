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
 * Created on Sep 15, 2008
 */

package org.embergraph.btree.keys;

/**
 * Delegation pattern for {@link ISortKeyBuilder} that is useful when you need to {@link
 * #resolve(Object)} one type to another before applying the delegate to generate the sort key.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E> The generic type of the object to which this builder will be applied.
 * @param <F> The generic type of the object whose sort keys the delegate can generate.
 */
public abstract class DelegateSortKeyBuilder<E, F> implements ISortKeyBuilder<E> {

  private final ISortKeyBuilder<F> delegate;

  public DelegateSortKeyBuilder(final ISortKeyBuilder<F> delegate) {

    if (delegate == null) throw new IllegalArgumentException();

    this.delegate = delegate;
  }

  /**
   * Resolve one generic type to another.
   *
   * @param e An element.
   * @return The resolved element.
   */
  protected abstract F resolve(E e);

  public byte[] getSortKey(E e) {

    return delegate.getSortKey(resolve(e));
  }
}
