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

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.aggregate.AggregateBase;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.constraints.INeedsMaterialization;
import org.embergraph.rdf.internal.constraints.IVValueExpression;
import org.embergraph.rdf.internal.constraints.MathBOp;
import org.embergraph.rdf.internal.constraints.MathBOp.MathOp;
import org.embergraph.rdf.internal.constraints.MathUtility;
import org.embergraph.rdf.internal.impl.literal.NumericIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.openrdf.model.Literal;

/*
* Operator computes the running sum over the presented binding sets for the given variable. A
 * missing value does not contribute towards the sum.
 *
 * @author thompsonbry
 */
public class AVERAGE extends AggregateBase<IV> implements INeedsMaterialization {

  //    private static final transient Logger log = Logger.getLogger(AVERAGE.class);

  /** */
  private static final long serialVersionUID = 1L;

  public AVERAGE(AVERAGE op) {
    super(op);
  }

  public AVERAGE(BOp[] args, Map<String, Object> annotations) {
    super(args, annotations);
  }

  public AVERAGE(boolean distinct, IValueExpression<IV> expr) {

    super(distinct, expr);
  }

  /*
   * The running aggregate value.
   *
   * <p>Note: SUM() returns ZERO if there are no non-error solutions presented. This assumes that
   * the ZERO will be an xsd:int ZERO.
   *
   * <p>Note: This field is guarded by the monitor on the {@link AVERAGE} instance.
   */
  private transient NumericIV aggregated = ZERO;

  /** The #of observed values. */
  private transient long n = 0;

  /** The first error encountered since the last {@link #reset()}. */
  private transient Throwable firstCause = null;

  public synchronized void reset() {

    aggregated = ZERO;

    n = 0;

    firstCause = null;
  }

  public synchronized IV done() {

    if (firstCause != null) {

      throw new RuntimeException(firstCause);
    }

    if (n == 0) return ZERO;

    return MathUtility.literalMath(
        aggregated, new XSDNumericIV<EmbergraphLiteral>(n), MathBOp.MathOp.DIVIDE);
  }

  public synchronized IV get(final IBindingSet bindingSet) {

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

    final IV<?, ?> iv = expr.get(bindingSet);

    if (iv != null) {

      /*
       * Aggregate non-null literal values.
       */

      /*
       * Aggregate non-null literal values.
       */

      final Literal lit = IVValueExpression.asLiteral(iv);

      if (!MathUtility.checkNumericDatatype(lit)) throw new SparqlTypeErrorException();

      aggregated = MathUtility.literalMath(aggregated, lit, MathOp.PLUS);

      //        	if (!iv.isLiteral())
      //        		throw new SparqlTypeErrorException();
      //
      //            if (iv.isInline()) {
      //
      //                // Two IVs.
      //                aggregated = IVUtility.numericalMath(iv, aggregated,
      //                        MathOp.PLUS);
      //
      //            } else {
      //
      //                // One IV and one Literal.
      //                final EmbergraphValue val1 = iv.getValue();
      //
      //                if (val1 == null)
      //                    throw new NotMaterializedException();
      //
      ////                if (!(val1 instanceof Literal))
      ////                    throw new SparqlTypeErrorException();
      //
      //                // Only numeric value can be used in math expressions
      //                final URI dt1 = ((Literal)val1).getDatatype();
      //                if (dt1 == null || !XMLDatatypeUtil.isNumericDatatype(dt1))
      //                    throw new SparqlTypeErrorException();
      //
      //                aggregated = IVUtility.numericalMath((Literal) val1,
      //                        aggregated, MathOp.PLUS);
      //
      //            }

      n++;
    }

    return aggregated;
  }

  /*
   * Note: {@link AVERAGE} only works on numerics. If they are inline, then that is great. Otherwise
   * it will handle a materialized numeric literal and do type promotion, which always results in a
   * signed inline number IV and then operate on that.
   *
   * <p>FIXME MikeP: What is the right return value here?
   */
  public Requirement getRequirement() {

    return INeedsMaterialization.Requirement.ALWAYS;
  }
}
