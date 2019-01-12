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
package org.embergraph.bop.rdf.aggregate;

import java.math.BigInteger;
import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.aggregate.AggregateBase;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.constraints.INeedsMaterialization;
import org.embergraph.rdf.internal.constraints.INeedsMaterialization.Requirement;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.model.EmbergraphLiteral;

/**
 * Operator computes the number of non-null values over the presented binding
 * sets for the given variable.
 * <p>
 * Note: COUNT(*) is the cardinality of the solution multiset. COUNT(DISTINCT *)
 * is the cardinality of the distinct solutions in the solution multiset. These
 * semantics are not directly handled by this class. It relies on the
 * aggregation operator to compute those values.
 *
 * @author thompsonbry
 */
public class COUNT extends AggregateBase<IV> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public COUNT(COUNT op) {
		super(op);
	}

	public COUNT(BOp[] args, Map<String, Object> annotations) {
		super(args, annotations);
	}

    public COUNT(final boolean distinct, IValueExpression<IV> expr) {
        super(/*FunctionCode.COUNT,*/ distinct, expr);
    }

	/**
	 * The running aggregate value.
	 * <p>
	 * Note: This field is guarded by the monitor on the {@link COUNT} instance.
	 */
    private transient long aggregated = 0L;

    /**
     * The first error encountered since the last {@link #reset()}.
     */
    private transient Throwable firstCause = null;

    /**
     * {@inheritDoc}
     * <p>
     * Note: COUNT() returns ZERO if there are no non-error solutions presented.
     * This assumes that the ZERO will be an xsd:long.
     */
    synchronized public IV get(final IBindingSet bindingSet) {

        try {

            return doGet(bindingSet);

        } catch (Throwable t) {

            if (firstCause == null) {

                firstCause = t;

            }

            throw new RuntimeException(t);

        }

    }

    private IV doGet(final IBindingSet bindingSet) {

        final IValueExpression<IV> expr = (IValueExpression<IV>) get(0);

        if (expr instanceof IVariable<?> && ((IVariable<?>) expr).isWildcard()) {
            // Do not attempt to evaluate "*".
            aggregated++;
            return null;
        }

        // evaluate the expression (typically just a variable, but who knows).
        final IV<?,?> val = expr.get(bindingSet);

        if (val != null) {

            // aggregate non-null values.
            aggregated++;

        }

        // No intermediate value is returned to minimize churn.
        return null;

    }

    synchronized public void reset() {

        aggregated = 0L;

        firstCause = null;

    }

    synchronized public IV done() {

        if (firstCause != null) {

            throw new RuntimeException(firstCause);

        }

        return new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(aggregated));
        //return new XSDNumericIV<EmbergraphLiteral>(aggregated);

    }

    /**
     * COUNT does not need to actually see the materialized values, or even the
     * IVs. COUNT(DISTINCT) does need to see the IVs, but they still do not need
     * to be materialized.
     */
    public Requirement getRequirement() {

        return INeedsMaterialization.Requirement.NEVER;

    }

    // /**
    // * Overridden to allow <code>COUNT(*)</code>.
    // */
    // @Override
    // final public boolean isWildcardAllowed() {

    // return true;

    // }

}
