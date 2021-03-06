package org.embergraph.rdf.internal.constraints;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.constraints.MathBOp.MathOp;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;

/** Interface for handling math operations on specific data type. */
public interface IMathOpHandler {

  /*
   * Check if this utility applies for the argument types.
   *
   * @param args
   * @return
   */
  boolean canInvokeMathOp(final Literal... args);

  /*
   * Perform the operation on arguments.
   *
   * @param l1
   * @param iv1
   * @param l2
   * @param iv2
   * @param op
   * @param vf
   * @return
   */
  IV doMathOp(
      final Literal l1,
      final IV iv1,
      final Literal l2,
      final IV iv2,
      final MathOp op,
      final EmbergraphValueFactory vf);
}
