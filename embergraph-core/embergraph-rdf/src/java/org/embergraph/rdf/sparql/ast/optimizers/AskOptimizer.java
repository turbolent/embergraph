/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

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
 * Created on Aug 24, 2011
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import org.embergraph.bop.IBindingSet;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.SliceNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/**
 * ASK queries have a LIMIT of ONE imposed unless they are aggregations.
 * 
 * TODO ASK style aggregations should always succeed (unless they encounter an
 * error) and could be refactored into a subquery with a limit of ONE and an
 * aggregation to ensure that they do the minimum possible work.
 */
public class AskOptimizer implements IASTOptimizer {

	public QueryNodeWithBindingSet optimize(
	      final AST2BOpContext context, final QueryNodeWithBindingSet input) {
	   
      final IQueryNode queryNode = input.getQueryNode();
      final IBindingSet[] bindingSet = input.getBindingSets();
      
		final QueryRoot queryRoot = (QueryRoot) queryNode;
		
		if (queryRoot.getQueryType() != QueryType.ASK) {
			
		    // Not a query that we will rewrite.
		    return new QueryNodeWithBindingSet(queryRoot, bindingSet);
		    
		}

        if (queryRoot.getGroupBy() != null && !queryRoot.getGroupBy().isEmpty()) {
            
            // Do not modify an aggregation query.
            return new QueryNodeWithBindingSet(queryRoot, bindingSet);
            
        }
        
        if (queryRoot.getHaving() != null && !queryRoot.getHaving().isEmpty()) {
            
            // Do not modify an aggregation query.
            return new QueryNodeWithBindingSet(queryRoot, bindingSet);
            
        }

        SliceNode slice = queryRoot.getSlice();
        
        if(slice == null) {
            
            queryRoot.setSlice(slice = new SliceNode());
        
        }
        
        if (slice.getLimit() > 1L) {

            slice.setLimit(1L);
            
        }        
        
		return new QueryNodeWithBindingSet(queryRoot, bindingSet);
		
	}
	
}
