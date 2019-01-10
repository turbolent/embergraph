package com.bigdata.bop;

import java.io.Serializable;

/**
 * An expression which may be evaluated to a value.
 * 
 * @author mrpersonick
 */
public interface IValueExpression<E> extends BOp, Serializable {

    /**
     * Return the <i>as bound</i> value of the variable, constant, or
     * expression. The <i>as bound</i> value of an {@link IConstant} is the
     * contant's value. The <i>as bound</i> value of an {@link IVariable} is the
     * bound value in the given {@link IBindingSet} -or- <code>null</code> if
     * the variable is not bound in the {@link IBindingSet}.
     * 
     * @param bindingSet
     *            The binding set.
     * 
     * @return The as bound value of the constant, variable, or expression.
     * 
     * @throws IllegalArgumentException
     *             if this is an {@link IVariable} and the <i>bindingSet</i> is
     *             <code>null</code>.
     */
    E get(IBindingSet bindingSet);
    /**
	 * A string representation of a recursive structure with pretty-print indent.
	 * @param indent
	 * @return
	 */
    String toString(int indent);
    
}
