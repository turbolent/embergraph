/* Generated By:JJTree: Do not edit this line. ASTExistsFunc.java Version 4.1 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package org.embergraph.rdf.sail.sparql.ast;

public class ASTExistsFunc extends SimpleNode {

	public ASTExistsFunc(int id) {
		super(id);
	}

	public ASTExistsFunc(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}
}
/* JavaCC - OriginalChecksum=e2ce7a71379b9555eb4e74a92f1f98e0 (do not edit this line) */
