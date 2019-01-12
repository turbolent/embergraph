/* Generated By:JJTree: Do not edit this line. ASTUpdateContainer.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTUpdateContainer extends ASTOperationContainer {

  //  private String sourceString;

  public ASTUpdateContainer(int id) {
    super(id);
  }

  public ASTUpdateContainer(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  /** Accept the visitor. * */
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }

  @Override
  public void setSourceString(String source) {
    throw new RuntimeException(
        "should use setSourceString on parent node of type ASTUpdateSequence instead");
  }

  @Override
  public String getSourceString() {
    ASTUpdateSequence sequence = (ASTUpdateSequence) parent;
    return sequence.getSourceString();
  }

  public ASTUpdate getUpdate() {
    return this.jjtGetChild(ASTUpdate.class);
  }
}
/* JavaCC - OriginalChecksum=fc3044232a8f28a530abb0172b429242 (do not edit this line) */
