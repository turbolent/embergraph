package org.embergraph.rdf.sparql.ast;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;

/**
 * Used to represent a variable or constant in the AST (a term in a statement
 * pattern for example). Unlike {@link FunctionNode}s, a {@link TermNode} models
 * the {@link IValueExpression} directly as its sole child argument. This
 * facilitates various rewrite patterns in which variables are renamed, etc.
 * 
 * @author mikepersonick
 */
public abstract class TermNode extends ValueExpressionNode {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2050144811725774174L;

    /**
     * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
     */
    public TermNode(TermNode op) {
        super(op);
    }
    
    /**
     * Required shallow copy constructor.
     */
	public TermNode(final BOp[] args, final Map<String, Object> anns) {
		
		super(args, anns);
		
	}

	/**
	 * Strengthen the return type.
	 */
	@Override
	public IVariableOrConstant<IV> getValueExpression() {
		
		return (IVariableOrConstant<IV>) get(0);
		
	}

    /**
     * Operation is not supported (could be modeled by changing args[0] if we
     * still want to do this).
     */
    @Override
    public void setValueExpression(IValueExpression<? extends IV> ve) {

        throw new UnsupportedOperationException();
        
    }

    /**
     * NOP.
     */
    @Override
    final public void invalidate() {
        // NOP
    }

    final public EmbergraphValue getValue() {
	
    	final IVariableOrConstant<IV> val = getValueExpression();
    	
    	if (val != null && val instanceof IConstant) {
    		
    		final IV iv = val.get();
    		
    		if (iv.hasValue()) {
    			
    			return iv.getValue();
    			
    		}
    		
    	}
    	
    	return null;

    }

	@Override
	public String toString(int i) {
		return toShortString();
	}
    
}
