package org.embergraph.rdf.sparql.ast;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;

/*
 * A special kind of AST node that represents the SPARQL 1.1 zero length path operator.
 *
 * @deprecated Does not work - leads to cardinality problems and can be removed. Zero Length Paths
 *     are integrated into the ALP node / ArbitraryLengthPathOp now.
 */
public class ZeroLengthPathNode extends GroupMemberNodeBase<ZeroLengthPathNode>
    implements IBindingProducerNode {

  /** */
  private static final long serialVersionUID = 1L;

  interface Annotations extends GroupNodeBase.Annotations {

    /** The left side of the zero-length path. */
    String LEFT = "left";

    /** The right side of the zero-length path. */
    String RIGHT = "right";
  }

  /** Constructor required for {@link org.embergraph.bop.BOpUtility#deepCopy(FilterNode)}. */
  public ZeroLengthPathNode(ZeroLengthPathNode op) {

    super(op);
  }

  /** Required shallow copy constructor. */
  public ZeroLengthPathNode(BOp[] args, Map<String, Object> anns) {

    super(args, anns);
  }

  public ZeroLengthPathNode() {
    this(BOp.NOARGS, new LinkedHashMap<>());
  }

  public ZeroLengthPathNode(final TermNode left, final TermNode right) {
    this(BOp.NOARGS, NV.asMap(new NV(Annotations.LEFT, left), new NV(Annotations.RIGHT, right)));
  }

  /** Returns the left term. */
  public TermNode left() {
    return (TermNode) super.getRequiredProperty(Annotations.LEFT);
  }

  /** Sets the left term. */
  public void setLeft(final TermNode left) {
    super.setProperty(Annotations.LEFT, left);
  }

  /** Returns the right term. */
  public TermNode right() {
    return (TermNode) super.getRequiredProperty(Annotations.RIGHT);
  }

  /** Sets the right term. */
  public void setRight(final TermNode right) {
    super.setProperty(Annotations.RIGHT, right);
  }

  /** Return the variables used by the path - i.e. what this node will attempt to bind when run. */
  public Set<IVariable<?>> getProducedBindings() {

    final Set<IVariable<?>> producedBindings = new LinkedHashSet<>();

    addProducedBindings(left(), producedBindings);
    addProducedBindings(right(), producedBindings);

    return producedBindings;
  }

  /*
   * This handles the special case where we've wrapped a Var with a Constant because we know it's
   * bound, perhaps by the exogenous bindings. If we don't handle this case then we get the join
   * vars wrong.
   *
   * @see StaticAnalysis._getJoinVars
   */
  private void addProducedBindings(final TermNode t, final Set<IVariable<?>> producedBindings) {

    if (t instanceof VarNode) {

      producedBindings.add(((VarNode) t).getValueExpression());

    } else if (t instanceof ConstantNode) {

      final ConstantNode cNode = (ConstantNode) t;
      final Constant<?> c = (Constant<?>) cNode.getValueExpression();
      final IVariable<?> var = c.getVar();
      if (var != null) {
        producedBindings.add(var);
      }
    }
  }

  @Override
  public String toString(int indent) {

    final String s = indent(indent);

    String sb = "\n"
        + s + getClass().getSimpleName()
        + "(left=" + left() + ", right=" + right() + ")";
    return sb;
  }

  @Override
  public Set<IVariable<?>> getRequiredBound(StaticAnalysis sa) {
    return new HashSet<>();
  }

  @Override
  public Set<IVariable<?>> getDesiredBound(StaticAnalysis sa) {
    return sa.getSpannedVariables(this, true, new HashSet<>());
  }
}
