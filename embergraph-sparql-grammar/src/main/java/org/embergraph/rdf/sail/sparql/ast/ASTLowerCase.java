/* Generated By:JJTree: Do not edit this line. ASTLowerCase.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTLowerCase extends SimpleNode {
  public ASTLowerCase(int id) {
    super(id);
  }

  public ASTLowerCase(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=bd4c93f9709149c89aa4f195ee4ce02a (do not edit this line) */