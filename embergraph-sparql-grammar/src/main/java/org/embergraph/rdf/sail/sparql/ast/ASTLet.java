/* Generated By:JJTree: Do not edit this line. ASTLet.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTLet extends SimpleNode {
  public ASTLet(int id) {
    super(id);
  }

  public ASTLet(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=5ed096eb094dd6783917e97c1227116d (do not edit this line) */
