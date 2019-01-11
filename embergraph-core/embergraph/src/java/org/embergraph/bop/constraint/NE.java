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
package org.embergraph.bop.constraint;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;

/**
 * Imposes the constraint <code>x != y</code>.
 */
public class NE extends BOpBase implements BooleanValueExpression {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2511708022205706034L;

	public NE(final IVariable<?> x, final IVariable<?> y) {

        this(new BOp[] { x, y }, null/* annotations */);

    }
    
    /**
     * Required shallow copy constructor.
     */
    public NE(final BOp[] args, final Map<String, Object> annotations) {
        super(args, annotations);

        if (args.length != 2 || args[0] == null || args[1] == null)
            throw new IllegalArgumentException();

        if (args[0] == args[1])
            throw new IllegalArgumentException();
        
    }

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public NE(final NE op) {
        super(op);
    }
    
    public Boolean get(final IBindingSet s) {
        
        // get binding for "x".
        final IConstant<?> x = s.get((IVariable<?>) get(0)/* x */);

        if (x == null)
            return true; // not yet bound.

        // get binding for "y".
        final IConstant<?> y = s.get((IVariable<?>) get(1)/* y */);

        if (y == null)
            return true; // not yet bound.
        
        return !x.equals(y); 

   }

}
