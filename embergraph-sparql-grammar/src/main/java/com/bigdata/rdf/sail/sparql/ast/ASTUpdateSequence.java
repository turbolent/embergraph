/* Generated By:JJTree: Do not edit this line. ASTUpdateSequence.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.bigdata.rdf.sail.sparql.ast;

import java.util.List;

import org.embergraph.rdf.sail.sparql.ast.ASTUpdateContainer;
import org.embergraph.rdf.sail.sparql.ast.SimpleNode;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilder;
import org.embergraph.rdf.sail.sparql.ast.SyntaxTreeBuilderVisitor;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;

public class ASTUpdateSequence extends SimpleNode {

    private String source;

    public ASTUpdateSequence(int id) {
        super(id);
    }

    public ASTUpdateSequence(SyntaxTreeBuilder p, int id) {
        super(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
        throws VisitorException
    {
        return visitor.visit(this, data);
    }

    public void setSourceString(String source) {
        this.source = source;
    }
    
    public String getSourceString() {
        return source;
    }
    
    public List<ASTUpdateContainer> getUpdateContainers() {
        
        final List<ASTUpdateContainer> result = jjtGetChildren(ASTUpdateContainer.class);
        final ASTUpdateSequence seq = jjtGetChild(ASTUpdateSequence.class);
        
        if (seq != null) {
            result.addAll(seq.getUpdateContainers());
        }
        
        return result;
    }
}
/* JavaCC - OriginalChecksum=e4b13eef2d0d6dbe36d25df3ab1d11da (do not edit this line) */
