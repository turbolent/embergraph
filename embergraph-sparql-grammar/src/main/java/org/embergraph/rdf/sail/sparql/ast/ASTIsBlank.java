/* Generated By:JJTree: Do not edit this line. ASTIsBlank.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTIsBlank extends SimpleNode {

  public ASTIsBlank(int id) {
    super(id);
  }

  public ASTIsBlank(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  @Override
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
