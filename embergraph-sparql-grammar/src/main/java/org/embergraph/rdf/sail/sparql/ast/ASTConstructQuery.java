/* Generated By:JJTree: Do not edit this line. ASTConstructQuery.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTConstructQuery extends ASTQuery {

  public ASTConstructQuery(int id) {
    super(id);
  }

  public ASTConstructQuery(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  @Override
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }

  public ASTConstruct getConstruct() {
    return jjtGetChild(ASTConstruct.class);
  }
}
