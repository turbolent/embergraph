/* Generated By:JJTree: Do not edit this line. ASTPathNegatedPropertySet.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

public
class ASTPathNegatedPropertySet extends SimpleNode {
  public ASTPathNegatedPropertySet(int id) {
    super(id);
  }

  public ASTPathNegatedPropertySet(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=117fe334ed7b39f8534b8fb1b3bc638c (do not edit this line) */
