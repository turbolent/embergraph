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
 * Created on Aug 7, 2008
 */

package org.embergraph.striterator;

import java.util.Enumeration;
import java.util.Iterator;

import cutthecrap.utils.striterators.ICloseableIterator;

/**
 * Streaming iterator pattern ala Martyn Cutcher with generics.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <I>
 * @param <E>
 * 
 * @todo appender and excluder patterns. These are just filters so we only
 *       really need a single {@link #addFilter(IFilter)} method.
 */
public interface IStriterator<I extends Iterator<E>, E> extends
        ICloseableIterator<E>, Enumeration<E> {

    /**
     * Stack a filter on the source iterator.
     * 
     * @param filter
     *            The filter.
     * 
     * @return The filtered iterator.
     */
    public IStriterator<I, E> addFilter(IFilter<I, ?, E> filter);

    /**
     * Visits elements that are instances of the specified class.
     * 
     * @param cls
     *            The class.
     *            
     * @return The filtered iterator.
     */
    public IStriterator<I,E> addInstanceOfFilter(Class<E> cls);

    /**
     * Appends the given iterator when this iterator is exhausted.
     * 
     * @param src
     *            Another iterator.
     *            
     * @return The combined iterator.
     */
    public IStriterator<I,E> append(I src);

//    public IStriterator<I,E> exclude(Object object);
//
//    public IStriterator<I,E> makeUnique();
//
//    public IStriterator<I,E> map(Object client, Method method);

}
