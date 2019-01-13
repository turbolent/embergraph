/*
 * Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
 * Copyright (C) Embergraph contributors 2019. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.embergraph.rdf.sparql.ast.optimizers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.aggregate.AggregateBase;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.GraphPatternGroup;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.IValueExpressionNode;
import org.embergraph.rdf.sparql.ast.NamedSubqueriesNode;
import org.embergraph.rdf.sparql.ast.NamedSubqueryRoot;
import org.embergraph.rdf.sparql.ast.OrderByExpr;
import org.embergraph.rdf.sparql.ast.OrderByNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryBase;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.SubqueryBase;
import org.embergraph.rdf.sparql.ast.SubqueryRoot;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;

/*
 * This compulsory AST transformation (not an optional optimizer!) enforces the correct treatment of
 * aggregates in ORDER BY clauses, according to the SPARQL semantic, under the assumption that
 * {@link org.embergraph.bop.solutions.MemorySortOp} does not have to deal with aggregates. In a
 * nutshell, this is done by introducing aliases for the aggregate expressions and thus pushing the
 * computation of the aggregates to where they can already be processed.
 *
 * <p>Simple example.
 *
 * <p>Consider this query (sparql11-order-02.rq):
 *
 * <p>SELECT ?type WHERE { ?subj a ?type } GROUP BY ?type ORDER BY (count(?subj))
 *
 * <p>It contains aggregate count(?subj) in ORDER BY. The idea is to rewrite it into this query:
 *
 * <p>SELECT ?type (count(?subj) as ?cnt)[excludeFromProjection] WHERE { ?subj a ?type } GROUP BY
 * ?type ORDER BY ?cnt
 *
 * <p>Here ?cnt is an auxiliary alias, i.e, a fresh variable (UUIDs are used as fresh variable names
 * in the implementation). This query can be computed even if the sorting does not support
 * aggregates.
 *
 * <p>Note that the rewritten query is not completely equivalent to the original one: it will assign
 * ?cnt, which does not occur in the original query. To rectify this, some variables in a projection
 * are designated as excluded from the projection outputs: see the label "[excludeFromProjection]"
 * by ?cnt in the rewritten query (pseudo-SPARQL).
 *
 * <p>More complex example.
 *
 * <p>The original query is:
 *
 * <p>PREFIX ex: <http://example.org/> SELECT ?x ?y WHERE { ?x ex:r ?y . ?y ex:q ?z } GROUP BY ?x ?y
 * ORDER BY DESC(max(?z)) ?x (count(?z)) DESC(?y)
 *
 * <p>The rewritten query is:
 *
 * <p>PREFIX ex: <http://example.org/> SELECT ?x ?y (max(?z) AS ?maxz)[excludeFromProjection]
 * (count(?z) AS ?cntz)[excludeFromProjection] WHERE { ?x ex:r ?y . ?y ex:q ?z } GROUP BY ?x ?y
 * ORDER BY DESC(?maxz) ?x ?cntz DESC(?y)
 *
 * <p>Here ?maxz and ?cntz are the introduced auxiliary aliases for the aggregates.
 *
 * @author <a href="mailto:ariazanov@blazegraph.com">Alexandre Riazanov</a>
 */
public class ASTOrderByAggregateFlatteningOptimizer implements IASTOptimizer {

  @Override
  public QueryNodeWithBindingSet optimize(
      final AST2BOpContext context, final QueryNodeWithBindingSet input) {

    final IQueryNode queryNode = input.getQueryNode();
    final IBindingSet[] bindingSets = input.getBindingSets();

    final QueryRoot queryRoot = (QueryRoot) queryNode;

    // First, process any pre-existing named subqueries.
    {
      final NamedSubqueriesNode namedSubqueries = queryRoot.getNamedSubqueries();

      if (namedSubqueries != null) {

        // Note: works around concurrent modification error.
        final List<NamedSubqueryRoot> list =
            BOpUtility.toList(namedSubqueries, NamedSubqueryRoot.class);

        for (NamedSubqueryRoot namedSubquery : list) {

          // Rewrite the named sub-select
          doSelectQuery(context, namedSubquery);
        }
      }
    }

    // rewrite the top-level select
    doSelectQuery(context, (QueryRoot) queryNode);

    return new QueryNodeWithBindingSet(queryNode, bindingSets);
  } // optimize(final AST2BOpContext context,..)

  private void doSelectQuery(final AST2BOpContext context, final QueryBase queryBase) {

    // recursion first.
    doRecursiveRewrite(context, queryBase.getWhereClause());

    if (queryBase.getQueryType() != QueryType.SELECT) {
      return;
    }

    final ProjectionNode projection = queryBase.getProjection();

    final OrderByNode orderBy = queryBase.getOrderBy();

    final OrderByNode newOrderBy = new OrderByNode();

    boolean aggregatesPresent = false;

    if (orderBy == null) {
      // The transformation is not applicable here.
      return;
    }

    final Set<IVariable<?>> varsToExcludeFromProjection = new HashSet<>();

    for (OrderByExpr orderByExpr : orderBy) {
      IValueExpression<? extends IV> ve = orderByExpr.getValueExpression();
      IValueExpressionNode ven = orderByExpr.getValueExpressionNode();

      if (ve instanceof AggregateBase) {

        aggregatesPresent = true;

        final Var freshVar = Var.var();

        final IValueExpressionNode newVEN = new VarNode(freshVar);

        final OrderByExpr newOrderByExpr = new OrderByExpr(newVEN, orderByExpr.isAscending());

        newOrderBy.addExpr(newOrderByExpr);

        // E.g., COUNT(?subj) AS ?cnt
        // or    MAX(?obj) AS ?mx
        final AssignmentNode replacementAlias = new AssignmentNode((VarNode) newVEN, ven);
        projection.addProjectionExpression(replacementAlias);
        varsToExcludeFromProjection.add(freshVar);

      } else {
        newOrderBy.addExpr(orderByExpr);
      }
    } // for (OrderByExpr orderByExpr : orderBy)

    projection.setVarsToExcludeFromProjection(varsToExcludeFromProjection);

    if (!aggregatesPresent) {
      // The transformation is not applicable here.
      return;
    }

    queryBase.setOrderBy(newOrderBy);
  } // doSelectQuery(final AST2BOpContext context, final QueryBase queryBase)

  /*
   * @param context
   * @param group possibly null, eg, when the enclosing query is a DESCRIBE
   */
  private void doRecursiveRewrite(
      final AST2BOpContext context, final GraphPatternGroup<IGroupMemberNode> group) {

    if (group == null) {
      return;
    }

    final int arity = group.arity();

    for (int i = 0; i < arity; i++) {

      final BOp child = group.get(i);

      if (child instanceof GraphPatternGroup<?>) {

        // Recursion into groups.
        doRecursiveRewrite(context, ((GraphPatternGroup<IGroupMemberNode>) child));

      } else if (child instanceof SubqueryRoot) {

        // Recursion into subqueries.
        final SubqueryRoot subqueryRoot = (SubqueryRoot) child;
        doRecursiveRewrite(context, subqueryRoot.getWhereClause());

        // rewrite the sub-select
        doSelectQuery(context, (SubqueryBase) child);

      } else if (child instanceof ServiceNode) {

        // Do not rewrite things inside of a SERVICE node.
        continue;
      }
    }
  } // doRecursiveRewrite(final AST2BOpContext context,..
} // class ASTDummyOptimizer
