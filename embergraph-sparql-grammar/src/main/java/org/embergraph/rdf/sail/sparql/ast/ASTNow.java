/* Generated By:JJTree: Do not edit this line. ASTNow.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public
class ASTNow extends SimpleNode {
  public ASTNow(int id) {
    super(id);
  }

  public ASTNow(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=799389af90324356a21278efce0964e4 (do not edit this line) */
