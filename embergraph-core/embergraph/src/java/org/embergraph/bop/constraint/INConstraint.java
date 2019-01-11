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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.constraint;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IVariable;
import org.embergraph.rdf.spo.InGraphBinarySearchFilter;
import org.embergraph.rdf.spo.InGraphHashSetFilter;

/**
 * Abstract base class for "IN" {@link IConstraint} implementations.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 *          FIXME Reconcile this with {@link InGraphBinarySearchFilter} and
 *          {@link InGraphHashSetFilter} and also with the use of an in-memory
 *          join against the incoming binding sets to handle SPARQL data sets.
 */
abstract public class INConstraint<T> extends BOpBase 
		implements BooleanValueExpression {

    /**
	 * 
	 */
	private static final long serialVersionUID = -774833617971700165L;

	public interface Annotations extends BOpBase.Annotations {

        /**
         * The variable against which the constraint is applied.
         */
        String VARIABLE = (INConstraint.class.getName() + ".variable").intern();

        /**
         * The set of allowed values for that variable.
         * 
         * @todo allow large sets to be specified by reference to a resource
         *       which is then materialized on demand during evaluation.
         */
        String SET = (INConstraint.class.getName() + ".set").intern();
        
    }
    
    /**
     * @param op
     */
    public INConstraint(final INConstraint<T> op) {
        super(op);
    }

    /**
     * @param args
     * @param annotations
     */
    public INConstraint(BOp[] args, Map<String, Object> annotations) {

        super(args, annotations);

        final IVariable<T> var = getVariable();

        if (var == null)
            throw new IllegalArgumentException();

        final IConstant<T>[] set = getSet();

        if (set == null)
            throw new IllegalArgumentException();

        if (set.length == 0)
            throw new IllegalArgumentException();

    }

    /**
     * @see Annotations#VARIABLE
     */
    @SuppressWarnings("unchecked")
    public IVariable<T> getVariable() {
        
        return (IVariable<T>) getProperty(Annotations.VARIABLE);
        
    }

    /**
     * @see Annotations#SET
     */
    @SuppressWarnings("unchecked")
    public IConstant<T>[] getSet() {
        
        return (IConstant<T>[]) getProperty(Annotations.SET);
        
    }

}
