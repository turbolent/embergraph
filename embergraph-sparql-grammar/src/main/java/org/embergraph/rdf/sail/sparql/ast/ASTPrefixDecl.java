/* Generated By:JJTree: Do not edit this line. ASTPrefixDecl.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTPrefixDecl extends SimpleNode {

  private String prefix;

  public ASTPrefixDecl(int id) {
    super(id);
  }

  public ASTPrefixDecl(SyntaxTreeBuilder p, int id) {
    super(p, id);
  }

  @Override
  public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
    return visitor.visit(this, data);
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public ASTIRI getIRI() {
    return jjtGetChild(ASTIRI.class);
  }

  @Override
  public String toString() {
    return super.toString() + " (prefix=" + prefix + ")";
  }
}
