/* Generated By:JJTree: Do not edit this line. ASTSameTerm.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTSameTerm extends SimpleNode {

	public ASTSameTerm(int id) {
		super(id);
	}

	public ASTSameTerm(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
}
