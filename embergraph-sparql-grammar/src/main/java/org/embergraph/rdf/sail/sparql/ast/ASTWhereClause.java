/* Generated By:JJTree: Do not edit this line. ASTWhereClause.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTWhereClause extends SimpleNode {

  public ASTWhereClause(int id) {
    super(id);
  }

  public ASTWhereClause(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  @Override
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }

  public ASTGraphPatternGroup getGraphPatternGroup() {
    return jjtGetChild(ASTGraphPatternGroup.class);
  }
}
