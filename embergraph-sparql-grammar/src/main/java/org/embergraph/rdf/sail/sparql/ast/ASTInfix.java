/* Generated By:JJTree: Do not edit this line. ASTInfix.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTInfix extends SimpleNode {
  public ASTInfix(int id) {
    super(id);
  }

  public ASTInfix(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=07e57eccf0c9e12d1089ccc2413a2ae2 (do not edit this line) */
