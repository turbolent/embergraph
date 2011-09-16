/*

Copyright (C) SYSTAP, LLC 2006-2011.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

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
package com.bigdata.rdf.internal.constraints;

import java.util.Map;

import com.bigdata.bop.BOp;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IVariable;
import com.bigdata.rdf.error.SparqlTypeErrorException;
import com.bigdata.rdf.internal.IV;

/**
 * Imposes the constraint <code>bound(x)</code> for the variable x.
 */
public class IsBoundBOp extends XSDBooleanIVValueExpression {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7408654639183330874L;

    public IsBoundBOp(final IVariable<IV> x) {

        this(new BOp[] { x }, null/*annotations*/);

    }

    /**
     * Required shallow copy constructor.
     */
    public IsBoundBOp(final BOp[] args, final Map<String, Object> anns) {
    	
        super(args, anns);
        
        if (args.length != 1 || args[0] == null)
            throw new IllegalArgumentException();

    }

    /**
     * Required deep copy constructor.
     */
    public IsBoundBOp(final IsBoundBOp op) {
        super(op);
    }

    /**
     * <pre>
     * Returns true if var is bound to a value. Returns false otherwise.
     * Variables with the value NaN or INF are considered bound.
     * </pre>
     * 
     * @see http://www.w3.org/TR/sparql11-query/#func-bound
     */
    public boolean accept(final IBindingSet bs) {

        try {
            return get(0).get(bs) != null;
        } catch (SparqlTypeErrorException ex) {
            // Not bound.
            return false;
        }

    }
    
}
