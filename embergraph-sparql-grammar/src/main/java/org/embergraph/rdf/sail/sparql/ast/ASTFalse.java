/* Generated By:JJTree: Do not edit this line. ASTFalse.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTFalse extends ASTRDFValue {

  public ASTFalse(int id) {
    super(id);
  }

  public ASTFalse(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  @Override
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
