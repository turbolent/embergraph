/* Generated By:JJTree: Do not edit this line. ASTNamedSubquery.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public
class ASTNamedSubquery extends SimpleNode {
  public ASTNamedSubquery(int id) {
    super(id);
  }

  public ASTNamedSubquery(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. **/
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }
  
  private String name;
  public void setName(String name) {
      this.name = name;
  }
  
  public String getName() {
      return name;
  }
  
}
/* JavaCC - OriginalChecksum=e27f64af0fab78a6988e958a9ed7f1a9 (do not edit this line) */
