/* Generated By:JJTree: Do not edit this line. ASTLang.java */

package org.embergraph.rdf.sail.sparql.ast;

public class ASTLang extends SimpleNode {

	public ASTLang(int id) {
		super(id);
	}

	public ASTLang(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
}
