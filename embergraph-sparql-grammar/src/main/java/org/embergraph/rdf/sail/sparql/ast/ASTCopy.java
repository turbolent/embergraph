/* Generated By:JJTree: Do not edit this line. ASTCopy.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTCopy extends ASTUpdate {
  private boolean silent;

  public ASTCopy(int id) {
    super(id);
  }

  public ASTCopy(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }

  public void setSilent(boolean silent) {
    this.silent = silent;
  }

  public boolean isSilent() {
    return this.silent;
  }
}
/* JavaCC - OriginalChecksum=b7b8d4d51e4e7a55e3ce5faa1a1d721f (do not edit this line) */
