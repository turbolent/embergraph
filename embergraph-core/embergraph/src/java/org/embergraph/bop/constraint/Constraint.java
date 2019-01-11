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
package org.embergraph.bop.constraint;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IValueExpressionConstraint;

/**
 * {@link Constraint} wraps a {@link BooleanValueExpression}.
 */
public class Constraint<X> extends BOpBase implements
        IValueExpressionConstraint<X> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9144690224260823279L;

//	protected static final Logger log = Logger.getLogger(Constraint.class);
	
	/**
	 * Convenience method to generate a constraint from a value expression.
	 */
	@SuppressWarnings("rawtypes")
    public static IConstraint wrap(final BooleanValueExpression ve) {
		
	    return new Constraint(ve);
	    
	}
	
	
	public Constraint(final BooleanValueExpression x) {
		
        this(new BOp[] { x }, null/*annotations*/);

    }

    /**
     * Required shallow copy constructor.
     */
    public Constraint(final BOp[] args, final Map<String, Object> anns) {

        super(args, anns);
        
        if (args.length != 1 || args[0] == null)
            throw new IllegalArgumentException();

    }

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public Constraint(final Constraint<X> op) {
        super(op);
    }

    @Override
    @SuppressWarnings("unchecked")
    public IValueExpression<X> getValueExpression() {
     
        return (IValueExpression<X>) get(0);
        
    }

    @Override
    public boolean accept(final IBindingSet bs) {
    	
//    	try {
    		
    		// evaluate the BVE operator
    		return ((BooleanValueExpression) get(0)).get(bs);

//		} catch (Throwable t) {
//
//			if (InnerCause.isInnerCause(t, SparqlTypeErrorException.class)) {
//
//				// trap the type error and filter out the solution
//				if (log.isInfoEnabled())
//					log.info("discarding solution due to type error: " + bs
//							+ " : " + t);
//				
//				return false;
//
//			}
//
//			throw new RuntimeException(t);
//
//    	}
    	
    }
    
}
