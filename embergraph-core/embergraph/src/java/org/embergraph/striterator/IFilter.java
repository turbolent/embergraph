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
 * Created on Aug 7, 2008
 */

package org.embergraph.striterator;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Stackable filter pattern with generics.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <I>
 *            The generic type of the source iterator.
 * @param <E>
 *            The generic type of the elements visited by the source iterator.
 * @param <F>
 *            The generic type of the elements visited by the filtered iterator.
 */
public interface IFilter<I extends Iterator<E>, E, F> extends Serializable {

    /**
     * Wrap the source iterator with an iterator that applies this filter.
     * 
     * @param src
     *            The source iterator.
     * 
     * @return The filtered iterator.
     */
    public Iterator<F> filter(I src);

}
