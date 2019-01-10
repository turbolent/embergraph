/**

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
 * Created on Sep 28, 2010
 */

package org.embergraph.bop.ap.filter;

import java.util.Iterator;
import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.Tuple;
import org.embergraph.btree.filter.TupleFilter;
import org.embergraph.btree.filter.TupleFilter.TupleFilterator;

/**
 * <p>
 * Filter supporting {@link ITupleIterator}s.
 * </p>
 * <p>
 * <strong>Warning: Unlike {@link BOpFilter}, this class correctly uses a second
 * {@link Tuple} instance to perform filtering.<strong> This is necessary since
 * the {@link Tuple} instance for the base {@link ITupleIterator}
 * implementations for the {@link AbstractBTree} is reused by next() on each
 * call and the {@link TupleFilter} uses one-step lookahead. Failure to use a
 * second {@link Tuple} instance will result in <em>overwrite</em> of the
 * current {@link Tuple} with data from the lookahead {@link Tuple}.
 * </p>
 * <p>
 * Note: You must specify {@link IRangeQuery#KEYS} and/or
 * {@link IRangeQuery#VALS} in order to filter on the keys and/or values
 * associated with the visited tuples.
 * </p>
 * <p>
 * Note: YOu must specify {@link IRangeQuery#CURSOR} to enabled
 * {@link Iterator#remove()} for a <em>local</em> {@link BTree}
 * </p>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class BOpTupleFilter<E> extends BOpFilterBase {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

//    /**
//     * Deserialization.
//     */
//    public BOpFilter() {
//        super();
//    }
    
    /**
     * Deep copy.
     * 
     * @param op
     */
    public BOpTupleFilter(BOpTupleFilter op) {
        super(op);
    }

    /**
     * Shallow copy.
     * 
     * @param args
     * @param annotations
     */
    public BOpTupleFilter(BOp[] args, Map<String, Object> annotations) {
        super(args, annotations);
    }

    @Override
    final protected Iterator filterOnce(Iterator src, final Object context) {

        return new TupleFilterator((ITupleIterator<E>) src, context,
                new FilterImpl());

    }

    /**
     * Return <code>true</code> iff the object should be accepted.
     * 
     * @param obj
     *            The object.
     */
    protected abstract boolean isValid(ITuple<E> obj);

    private class FilterImpl extends TupleFilter<E> {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean isValid(ITuple<E> tuple) {

            return BOpTupleFilter.this.isValid(tuple);

        }

    }

}
