/* Generated By:JJTree: Do not edit this line. ASTSeconds.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

public
class ASTSeconds extends SimpleNode {
  public ASTSeconds(int id) {
    super(id);
  }

  public ASTSeconds(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=93fda6f4d664de4164295dbd5e3dc9bf (do not edit this line) */
