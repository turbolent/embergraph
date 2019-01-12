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
import org.embergraph.bop.ImmutableBOp;
import org.embergraph.bop.NV;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

/**
 * A math expression involving a left and right IValueExpression operand. The operation to be
 * applied to the operands is specified by the {@link Annotations#OP} annotation.
 */
public class NumericBOp extends IVValueExpression<IV> implements INeedsMaterialization {

  private static final long serialVersionUID = 9136864442064392445L;

  private static final transient Logger log = Logger.getLogger(NumericBOp.class);

  public interface Annotations extends ImmutableBOp.Annotations {
    String OP = NumericBOp.class.getName() + ".op";
  }

  public enum NumericOp {
    ABS,
    ROUND,
    CEIL,
    FLOOR;
  }

  @Override
  protected boolean areGlobalsRequired() {

    return false;
  }

  /**
   * @param left The left operand.
   * @param right The right operand.
   * @param op The annotation specifying the operation to be performed on those operands.
   */
  @SuppressWarnings("rawtypes")
  public NumericBOp(final IValueExpression<? extends IV> left, final NumericOp op) {

    this(new BOp[] {left}, NV.asMap(Annotations.OP, op));
  }

  /**
   * Required shallow copy constructor.
   *
   * @param args The operands.
   * @param op The operation.
   */
  public NumericBOp(final BOp[] args, Map<String, Object> anns) {

    super(args, anns);

    if (args.length != 1 || args[0] == null || getProperty(Annotations.OP) == null) {

      throw new IllegalArgumentException();
    }
  }

  /**
   * Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}.
   *
   * @param op
   */
  public NumericBOp(final NumericBOp op) {

    super(op);
  }

  @SuppressWarnings("rawtypes")
  public IValueExpression<? extends IV> left() {
    return get(0);
  }

  public NumericOp op() {
    return (NumericOp) getRequiredProperty(Annotations.OP);
  }

  @SuppressWarnings("rawtypes")
  public IV get(final IBindingSet bs) {

    final Literal lit = super.getAndCheckLiteralValue(0, bs);

    final URI dt = lit.getDatatype();

    final NumericOp op = op();

    if ((dt.equals(XMLSchema.INT) || dt.equals(XMLSchema.INTEGER))
        && (op == NumericOp.CEIL || op == NumericOp.FLOOR || op == NumericOp.ROUND)) {

      return get(0).get(bs);
    }

    if (log.isDebugEnabled()) log.debug(lit);

    return MathUtility.numericalFunc(lit, op());
  }

  public String toString() {

    final StringBuilder sb = new StringBuilder();
    sb.append(op());
    sb.append("(").append(left()).append(")");
    return sb.toString();
  }

  /**
   * The MathBOp can work on inline numerics. It is only when the operands evaluate to non-inline
   * numerics that this bop needs materialization.
   */
  @Override
  public Requirement getRequirement() {
    return INeedsMaterialization.Requirement.SOMETIMES;
  }
}
