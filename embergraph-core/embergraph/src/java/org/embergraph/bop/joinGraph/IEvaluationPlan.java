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
 * Created on Jun 26, 2008
 */

package org.embergraph.bop.joinGraph;

import org.embergraph.relation.rule.IRule;

/**
 * Interface for evaluation orders.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IEvaluationPlan {

    /**
     * Return the evaluation order.
     * 
     * @return The evaluation order. This is an array of indices into the
     *         predicates in the tail of the rule. Each tail predicate must be
     *         used exactly once. The elements in this array are the order in
     *         which each tail predicate will be evaluated. The index into the
     *         array is the index of the tail predicate whose evaluation order
     *         you want. So <code>[2,0,1]</code> says that the predicates will
     *         be evaluated in the order tail[2], then tail[0], then tail[1].
     */
    public int[] getOrder();

    /**
     * <code>true</code> iff the {@link IRule} was proven to be empty based on
     * range counts or other data.
     */
    public boolean isEmpty();

    /**
     * The range count for the predicate.
     * 
     * @param tailIndex
     *            The index of the predicate in the tail of the rule.
     *            
     * @return The range count for that predicate.
     * 
     * @see IRangeCountFactory
     */
    public long rangeCount(int tailIndex);
    
}
