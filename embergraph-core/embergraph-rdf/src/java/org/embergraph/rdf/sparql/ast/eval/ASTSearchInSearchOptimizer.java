/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Sep 8, 2011
 */

package org.embergraph.rdf.sparql.ast.eval;

import java.util.Map;

import org.openrdf.model.Literal;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.NV;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.ITextIndexer;
import org.embergraph.rdf.lexicon.ITextIndexer.FullTextQuery;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.FunctionRegistry;
import org.embergraph.rdf.sparql.ast.FunctionRegistry.InFactory;
import org.embergraph.rdf.sparql.ast.IValueExpressionNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;
import org.embergraph.rdf.sparql.ast.ValueExpressionNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.optimizers.AbstractJoinGroupOptimizer;
import org.embergraph.rdf.store.BDS;
import org.embergraph.search.Hiterator;
import org.embergraph.search.IHit;

/**
 * Converts a {@link BDS#SEARCH_IN_SEARCH} function call (inside a filter) into
 * an IN filter using the full text index to determine the IN set.
 * 
 * Convert:
 * 
 * <pre>
 * filter(<BDS.SEARCH_IN_SEARCH>(?o,"foo")) .
 * </pre>
 * 
 * To:
 * 
 * <pre>
 * filter(?o IN ("foo", "foo bar", "hello foo", ...)) .
 * </pre>
 * 
 * This is a way of using the full text index to filter instead of using regex.
 */
public class ASTSearchInSearchOptimizer extends AbstractJoinGroupOptimizer {

//    private static final Logger log = Logger.getLogger(ASTSearchInSearchOptimizer.class);
    
//    static private long time = 0;
    
    /**
     * Optimize the join group.
     */
	protected void optimizeJoinGroup(final AST2BOpContext ctx, 
    		final StaticAnalysis sa, final IBindingSet[] bSets, final JoinGroupNode group) {

//		final long start = System.currentTimeMillis();
		
    	for (FilterNode node : group.getChildren(FilterNode.class)) {
    		
    		optimize(ctx, sa, group, node);
    		
    	}
    	
//    	time += (System.currentTimeMillis() - start);
    	
//    	System.err.println(time);
    	
    }

    /**
     * Optimize a single FilterNode.  We want to replace a search in search
     * function node with an In function node.
     */
	protected void optimize(final AST2BOpContext ctx, final StaticAnalysis sa, 
			final JoinGroupNode group, final FilterNode filterNode) {

		final IValueExpressionNode veNode = filterNode.getValueExpressionNode();
		
		if (veNode instanceof FunctionNode) {
			
			final FunctionNode funcNode = (FunctionNode) veNode;
			
			if (funcNode.getFunctionURI().equals(BDS.SEARCH_IN_SEARCH)) {
				
				filterNode.setArg(0, convert(ctx, funcNode));
				
			}
			
		}
    	
    }
	
	/**
	 * Perform the conversion from one function (BDS.SEARCH_IN_SEARCH) to
	 * another (IN).
	 */
	protected FunctionNode convert(final AST2BOpContext ctx, 
			final FunctionNode searchInSearch) {
		
		final VarNode var = (VarNode) searchInSearch.get(0);

		final Literal search = (Literal) ((ConstantNode) searchInSearch.get(1)).getValue();
		
		final String match;
		if (searchInSearch.arity() > 2) {

			final Literal l = (Literal) ((ConstantNode) searchInSearch.get(2)).getValue();
			
			match = l.getLabel();
			
		} else {
			
			match = "ANY";
			
		}
		
        final String regex;
        if (searchInSearch.arity() > 3) {
        	
			final Literal l = (Literal) ((ConstantNode) searchInSearch.get(3)).getValue();
            
            regex = l.getLabel();
        	
        } else {
        	
        	regex = null;
        	
        }
        
        final IV[] hits = getHits(ctx, search, match, regex);
        
        final ValueExpressionNode[] args = new ValueExpressionNode[hits.length+1];
        
        args[0] = var;
        
        for (int i = 0; i < hits.length; i++) {
        	args[i+1] = new ConstantNode(new Constant<IV>(hits[i]));
        }
        
        final Map<String, Object> props = NV.asMap(
        		new NV(InFactory.Annotations.ALLOW_LITERALS, true));
        
        final FunctionNode in = new FunctionNode(FunctionRegistry.IN, props, args);
		
		return in;
		
	}
	
	/**
	 * Collect the hits for the IN filter.
	 */
	@SuppressWarnings("unchecked")
	protected IV[] getHits(final AST2BOpContext ctx, 
			final Literal search, final String match, final String regex) {
		
    	final ITextIndexer<IHit> textIndex = (ITextIndexer<IHit>) 
    				ctx.getAbstractTripleStore().getLexiconRelation().getSearchEngine();
    	
        if (textIndex == null)
            throw new UnsupportedOperationException("No free text index?");

        String s = search.getLabel();
        final boolean prefixMatch;
        if (s.indexOf('*') >= 0) {
            prefixMatch = true;
            s = s.replaceAll("\\*", "");
        } else {
            prefixMatch = false;
        }

        final Hiterator<IHit> it = textIndex.search(new FullTextQuery(
    		s,//
            search.getLanguage(),// 
            prefixMatch,//
            regex,
            match != null && match.equalsIgnoreCase("ALL"),
            match != null && match.equalsIgnoreCase("EXACT")
            ));
        
        final IV[] hits = new IV[it.size()];
        
        int i = 0;
        while (it.hasNext()) { 
        	
        	hits[i++] = (IV) it.next().getDocId();

        }
        
		return hits;
		
	}

}
