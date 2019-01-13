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
 * Created on Sep 10, 2011
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import java.util.Collection;
import java.util.LinkedList;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IBindingSet;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.GraphPatternGroup;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.NamedSubqueriesNode;
import org.embergraph.rdf.sparql.ast.NamedSubqueryRoot;
import org.embergraph.rdf.sparql.ast.QueryBase;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;
import org.embergraph.rdf.sparql.ast.UnionNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.IEvaluationContext;

public class ASTUnionFiltersOptimizer implements IASTOptimizer {

  @Override
  public QueryNodeWithBindingSet optimize(
      final AST2BOpContext context, final QueryNodeWithBindingSet input) {

    final IQueryNode queryNode = input.getQueryNode();
    final IBindingSet[] bindingSets = input.getBindingSets();

    if (!(queryNode instanceof QueryRoot))
      return new QueryNodeWithBindingSet(queryNode, bindingSets);

    final QueryRoot queryRoot = (QueryRoot) queryNode;

    final StaticAnalysis sa = new StaticAnalysis(queryRoot, context);

    // Main WHERE clause
    {
      @SuppressWarnings("unchecked")
      final GraphPatternGroup<IGroupMemberNode> whereClause =
          (GraphPatternGroup<IGroupMemberNode>) queryRoot.getWhereClause();

      if (whereClause != null) {

        optimize(context, sa, whereClause);
      }
    }

    // Named subqueries
    if (queryRoot.getNamedSubqueries() != null) {

      final NamedSubqueriesNode namedSubqueries = queryRoot.getNamedSubqueries();

      /*
       * Note: This loop uses the current size() and get(i) to avoid
       * problems with concurrent modification during visitation.
       */
      for (NamedSubqueryRoot namedSubquery : namedSubqueries) {

        @SuppressWarnings("unchecked")
        final GraphPatternGroup<IGroupMemberNode> whereClause =
            (GraphPatternGroup<IGroupMemberNode>) namedSubquery.getWhereClause();

        if (whereClause != null) {

          optimize(context, sa, whereClause);
        }
      }
    }

    // log.error("\nafter rewrite:\n" + queryNode);

    return new QueryNodeWithBindingSet(queryNode, bindingSets);
  }

  /*
   * Look for a join group that has only one union and some filters. Lift the filters into all
   * children of the union and remove the filters from the group.
   */
  private void optimize(
      final IEvaluationContext ctx, final StaticAnalysis sa, final GraphPatternGroup<?> op) {

    if (op instanceof JoinGroupNode) {

      final JoinGroupNode joinGroup = (JoinGroupNode) op;

      UnionNode union = null;

      Collection<FilterNode> filters = null;

      boolean canOptimize = false;

      for (IGroupMemberNode child : joinGroup) {

        if (child instanceof UnionNode) {

          // more than one union
          if (union != null) {

            canOptimize = false;

            break;

          } else {

            union = (UnionNode) child;

            canOptimize = true;
          }

        } else if (child instanceof FilterNode) {

          if (filters == null) {

            filters = new LinkedList<>();
          }

          filters.add((FilterNode) child);

        } else {

          // something else in the group other than a union and filters
          canOptimize = false;

          break;
        }
      }

      if (canOptimize && filters != null) {

        for (JoinGroupNode child : union) {

          for (FilterNode filter : filters) {

            child.addChild(BOpUtility.deepCopy(filter));
          }
        }

        for (FilterNode filter : filters) {

          joinGroup.removeChild(filter);
        }
      }
    }

    /*
     * Recursion, but only into group nodes (including within subqueries).
     */
    for (int i = 0; i < op.arity(); i++) {

      final BOp child = op.get(i);

      if (child instanceof GraphPatternGroup<?>) {

        @SuppressWarnings("unchecked")
        final GraphPatternGroup<IGroupMemberNode> childGroup =
            (GraphPatternGroup<IGroupMemberNode>) child;

        optimize(ctx, sa, childGroup);

      } else if (child instanceof QueryBase) {

        final QueryBase subquery = (QueryBase) child;

        @SuppressWarnings("unchecked")
        final GraphPatternGroup<IGroupMemberNode> childGroup =
            (GraphPatternGroup<IGroupMemberNode>) subquery.getWhereClause();

        optimize(ctx, sa, childGroup);
      }
    }
  }
}
