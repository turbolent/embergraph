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
package org.embergraph.rdf.internal.constraints;

import java.util.Map;

import org.apache.log4j.Logger;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

/**
 * Conditional if BOp
 */
public class IfBOp extends IVValueExpression<IV> implements IPassesMaterialization {

	/**
	 *
	 */
	private static final long serialVersionUID = 7391999162162545704L;

	private static final transient Logger log = Logger.getLogger(IfBOp.class);

    @Override
    protected boolean areGlobalsRequired() {
     
        return false;
        
    }
    
    public IfBOp(final IValueExpression<? extends IV> conditional,//
            final IValueExpression<? extends IV> expression1,//
            final IValueExpression<? extends IV> expression2) {

        this(new BOp[] { conditional,expression1,expression2 }, BOp.NOANNS);

    }

    /**
     * Required shallow copy constructor.
     */
    public IfBOp(final BOp[] args, final Map<String, Object> anns) {

    	super(args, anns);

        if (args.length != 3 || args[0] == null|| args[1] == null|| args[2] == null)
            throw new IllegalArgumentException();
    }

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public IfBOp(final IfBOp op) {
        super(op);
    }

    public IV get(final IBindingSet bs) {

        final IV iv = get(0).get(bs);
        if(iv==null||!( iv instanceof XSDBooleanIV)){
            throw new SparqlTypeErrorException();
        }

        if (((XSDBooleanIV) iv).getInlineValue()) {
            final IV exp1 = get(1).get(bs);
            if(exp1==null){
                throw new SparqlTypeErrorException();
            }
            return exp1;
        }else{
            final IV exp2 = get(2).get(bs);
            if(exp2==null){
                throw new SparqlTypeErrorException();
            }
            return exp2;
        }


    }

}
