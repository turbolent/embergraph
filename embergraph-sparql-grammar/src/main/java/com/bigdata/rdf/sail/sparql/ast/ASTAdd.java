/* Generated By:JJTree: Do not edit this line. ASTAdd.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import org.embergraph.rdf.sail.sparql.ast.ASTUpdate;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

public class ASTAdd extends ASTUpdate {

	private boolean silent;

	public ASTAdd(int id) {
		super(id);
	}

	public ASTAdd(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	/** Accept the visitor. **/
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
	}

	public boolean isSilent() {
		return this.silent;
	}
}
/* JavaCC - OriginalChecksum=0beafdd82b983011c7a68f5d763b3d15 (do not edit this line) */
