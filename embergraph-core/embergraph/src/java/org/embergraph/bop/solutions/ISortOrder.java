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
 * Created on Sep 24, 2008
 */

package org.embergraph.bop.solutions;

import java.io.Serializable;

import org.embergraph.bop.IBind;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IVariable;

/**
 * A value expression and a direction flag for the ordering of the computed
 * values.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface ISortOrder<E> extends Serializable {

    /**
     * An {@link IValueExpression} to be evaluated for each input solution. The
     * {@link IValueExpression} should be either a bare {@link IVariable}, a
     * {@link IConstant} or an {@link IBind} associating the computed value of
     * an {@link IValueExpression} with an {@link IVariable} bound on the
     * solution as a side-effect.
     */
    IValueExpression<E> getExpr();

    /**
     * <code>true</code> iff the values will be placed into an ascending sort
     * and <code>false</code> if the values will be placed into a descending
     * sort.
     */
    boolean isAscending();
    
}
