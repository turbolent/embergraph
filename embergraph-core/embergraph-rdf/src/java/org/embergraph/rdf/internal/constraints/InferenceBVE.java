package org.embergraph.rdf.internal.constraints;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.constraint.BooleanValueExpression;
import org.embergraph.rdf.error.SparqlTypeErrorException;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;

public class InferenceBVE extends BOpBase implements BooleanValueExpression {

  /** */
  private static final long serialVersionUID = -5713570348190136135L;

  public InferenceBVE(final XSDBooleanIVValueExpression x) {

    this(new BOp[] {x}, null /*annocations*/);
  }

  /** Required shallow copy constructor. */
  public InferenceBVE(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);

    if (args.length != 1 || args[0] == null) throw new IllegalArgumentException();
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public InferenceBVE(final InferenceBVE op) {
    super(op);
  }

  /*
   * For inference rules, we want to trap unbound variable exceptions and allow the solution
   * through.
   */
  public Boolean get(final IBindingSet bs) {

    final XSDBooleanIVValueExpression bop = (XSDBooleanIVValueExpression) get(0);

    try {
      final XSDBooleanIV iv = bop.get(bs);
      return iv.booleanValue();
    } catch (SparqlTypeErrorException ex) {
      // allow unbound variables
      return true;
    }
  }
}
