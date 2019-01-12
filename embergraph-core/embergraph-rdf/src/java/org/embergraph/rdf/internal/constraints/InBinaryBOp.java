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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;

/**
 * A constraint that a variable may only take on the bindings enumerated by some
 * set.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: INBinarySearch.java 4357 2011-03-31 17:11:26Z thompsonbry $
 */
public class InBinaryBOp extends InBOp {

	private static final long serialVersionUID = 2251370041131847351L;

	/**
     * The value expression to be computed for each solution (cached).
     * <p>
     * Note: This cache is not serialized and is compiled on demand when the
     * operator is used.
     */
    private transient volatile IValueExpression<IV> valueExpr;

    /**
     * The ordered list of constants (cached).
     * <p>
     * Note: This cache is not serialized and is compiled on demand when the
     * operator is used.
     */
    private transient volatile IV[] set;
    
    /**
     * <code>true</code> iff this is NOT IN (cached).
     * <p>
     * Note: This cache is not serialized and is compiled on demand when the
     * operator is used.
     */
    private transient boolean not;

    /**
     * Deep copy constructor.
     */
    public InBinaryBOp(final InBinaryBOp op) {
        super(op);
    }

    /**
     * Shallow copy constructor.
     */
    public InBinaryBOp(final BOp[] args, final Map<String, Object> annotations) {
        
        super(args, annotations);
        
    }

    /**
     * 
     * @param x
     *            Some variable.
     * @param set
     *            A set of legal term identifiers providing a constraint on the
     *            allowable values for that variable.
     */
    @SuppressWarnings("rawtypes")
    public InBinaryBOp(
            final boolean not,
            final IValueExpression<? extends IV> var,
            final IConstant<? extends IV>... set) {
     
        super(not, var, set);

    }

    @SuppressWarnings("rawtypes")
    static private IV[] sort(final IConstant<IV>[] set) {

        final int n = set.length;

        if (n == 0)
            throw new IllegalArgumentException();

//        final IV firstValue = set[0].get();

        // allocate an array of the correct type.
        final IV[] tmp = new IV[n]; 
//                (IV[]) java.lang.reflect.Array.newInstance(
//                firstValue.getClass(), n);

        for (int i = 0; i < n; i++) {

            // dereference the constants to their bound values.
            tmp[i] = set[i].get();

        }

        // sort the bound values.
        Arrays.sort(tmp);

        return tmp;

    }
    
    /**
     * Extends {@link org.embergraph.bop.CoreBaseBOp#mutation() CoreBaseBOp.mutation} method to reflect args changes in cached IVs set.
     */
    @Override
    public void mutation() {
    	super.mutation();
    	if (set != null) {
            synchronized (this) {
                if (set != null) {
                    // clearing cached set is guarded by double-checked locking pattern.
                    set = null;
                }
            }
        }
    }

    private void init() {

        valueExpr = getValueExpression();

        set = sort(getSet());
        not=((Boolean)getProperty(Annotations.NOT)).booleanValue();
    }

    public boolean accept(final IBindingSet bindingSet) {

    	final IV[] _set;
        if (valueExpr == null || set==null) {
            synchronized (this) {
                if (valueExpr == null || set==null) {
                    // init() is guarded by double-checked locking pattern.
                    init();
                }
                // "set" is pulled into a local variable in this double-checked locking pattern
                // to avoid the possibility that set is resolved to non-null
                // and then cleared to null before it is used in accept()
                _set = set;                    
            }
        } else {
        	_set = set;
        }

        // get the as-bound value for that value expression.
        final IV v = valueExpr.get(bindingSet);

        if (v == null)
            throw new SparqlTypeErrorException.UnboundVarException();

        // lookup the bound value in the set of values.
        final int pos = Arrays.binarySearch(_set, v);

        // true iff the bound value was found in the set.
        final boolean found = pos >= 0;

        return not ? !found : found;

    }

}
