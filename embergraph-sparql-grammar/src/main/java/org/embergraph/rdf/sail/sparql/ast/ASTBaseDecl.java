/* Generated By:JJTree: Do not edit this line. ASTBaseDecl.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTBaseDecl extends SimpleNode {

	private String iri;

	public ASTBaseDecl(int id) {
		super(id);
	}

	public ASTBaseDecl(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public String getIRI() {
		return iri;
	}

	public void setIRI(String iri) {
		this.iri = iri;
	}

	@Override
	public String toString()
	{
		return super.toString() + " (" + iri + ")";
	}
}
