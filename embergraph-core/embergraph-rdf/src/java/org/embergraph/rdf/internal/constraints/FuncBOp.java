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
import org.openrdf.model.Value;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.NV;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.NotMaterializedException;
import org.embergraph.rdf.model.BigdataValueFactory;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

/**
 * Call one of the Sesame casting functions.
 */
public class FuncBOp extends IVValueExpression<IV> implements
        INeedsMaterialization {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2587499644967260639L;
	
	private static final transient Logger log = Logger.getLogger(FuncBOp.class);

	public interface Annotations extends IVValueExpression.Annotations {

		String FUNCTION = FuncBOp.class.getName() + ".function";

    }
	
    @SuppressWarnings("rawtypes")
    public FuncBOp(final IValueExpression<? extends IV>[] args, 
    		final String func, final GlobalAnnotations globals) {
        
        this(args, anns(globals, new NV(Annotations.FUNCTION, func)));
        
    }
    
    /**
     * Required shallow copy constructor.
     */
    public FuncBOp(final BOp[] args, final Map<String, Object> anns) {

    	super(args, anns);
    	
		if (getProperty(Annotations.NAMESPACE) == null)
			throw new IllegalArgumentException();
		
		if (getProperty(Annotations.FUNCTION) == null)
			throw new IllegalArgumentException();
		
    }

    private Function getFunc() {

        if (funct == null) {

            final String funcName = (String) getRequiredProperty(Annotations.FUNCTION);

            funct = FunctionRegistry.getInstance().get(funcName);

            if (funct == null) {
                throw new RuntimeException("Unknown function '" + funcName
                        + "'");
            }

        }

        return funct;

    }
    private transient volatile Function funct;

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public FuncBOp(final FuncBOp op) {
        super(op);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public IV get(final IBindingSet bs) {
        
    	final Value[] vals = new Value[arity()];
    	
    	for (int i = 0; i < vals.length; i++) {

    		final IV<?,?> iv = getAndCheckBound(i, bs);
    		
            final Value val = asValue(iv);
            
    		if (log.isDebugEnabled()) {
	            log.debug("iv: " + iv + ", class="+iv.getClass());
	            log.debug("val: " + val + ", class="+val.getClass());
    		}
            
            vals[i] = val;
            
    	}
    	
    	final Function func = getFunc();

	    final BigdataValueFactory vf = getValueFactory();
    
	    try {
	    
	    	final Value val = func.evaluate(vf, vals);
	    	
    		final IV iv = asIV(val, bs);
    		
    		if (log.isDebugEnabled()) {
	            log.debug("val: " + val + ", class="+val.getClass());
	            log.debug("iv: " + iv + ", class="+iv.getClass());
    		}
            
            return iv;
	    	
	    } catch (ValueExprEvaluationException ex) {
	    	
	    	throw new SparqlTypeErrorException();
	    	
	    }
        
    }   
    
    /**
     * This bop can only work with materialized terms.  
     */
    public Requirement getRequirement() {
    	
    	return INeedsMaterialization.Requirement.SOMETIMES;
    	
    }
        
}
