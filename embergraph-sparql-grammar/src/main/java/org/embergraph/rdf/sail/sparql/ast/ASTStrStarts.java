/* Generated By:JJTree: Do not edit this line. ASTStrStarts.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTStrStarts extends SimpleNode {
  public ASTStrStarts(int id) {
    super(id);
  }

  public ASTStrStarts(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=1fb5223e1c01db5ddf53281277415852 (do not edit this line) */