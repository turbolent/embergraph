package org.embergraph.rdf.sparql.ast;

import java.util.Map;
import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;

/*
 * The dummy constant node is used to represent constants in the AST that do not actually exist in
 * the database. For example, the pattern and flags arguments to regex, or possibly the right
 * operand to a compare operation whose left operand is a datatype, label, or str function. Also
 * useful for magic predicates such as those used by free text search.
 *
 * @author mikepersonick
 */
public class DummyConstantNode extends ConstantNode {

  /** */
  private static final long serialVersionUID = 1393730362383536411L;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static IV toDummyIV(final EmbergraphValue val) {

    final IV dummy = TermId.mockIV(VTE.valueOf(val));

    dummy.setValue(val);

    val.setIV(dummy);

    return dummy;
  }

  public DummyConstantNode(final EmbergraphValue val) {

    super(new Constant<>(toDummyIV(val)));
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public DummyConstantNode(DummyConstantNode op) {
    super(op);
  }

  /** Required shallow copy constructor. */
  public DummyConstantNode(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);
  }
}
