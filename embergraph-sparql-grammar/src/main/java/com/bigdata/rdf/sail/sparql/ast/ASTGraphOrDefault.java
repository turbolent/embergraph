/* Generated By:JJTree: Do not edit this line. ASTGraphOrDefault.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

public
class ASTGraphOrDefault extends SimpleNode {
  public ASTGraphOrDefault(int id) {
    super(id);
  }

  public ASTGraphOrDefault(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=74b78659852a809b64a306636a7ac2ac (do not edit this line) */
