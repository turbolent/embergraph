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
 * Created on Aug 29, 2011
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.ASTFulltextSearchOptimizer;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.service.fts.FTS;
import org.openrdf.query.algebra.StatementPattern.Scope;

/**
 * Test suite for {@link ASTFulltextSearchOptimizer}.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestASTFulltextSearchOptimizer extends AbstractASTEvaluationTestCase {

  /** */
  public TestASTFulltextSearchOptimizer() {}

  /** @param name */
  public TestASTFulltextSearchOptimizer(String name) {
    super(name);
  }

  /** Test rewriting of {@link FTS} magiv predicates into SERVICE node. */
  public void test_searchServiceOptimizer_01() {

    /*
     * Note: DO NOT share structures in this test!!!!
     */

    /** Magic vocabulary URIs */
    @SuppressWarnings("rawtypes")
    final IV searchIV = makeIV(FTS.SEARCH);

    @SuppressWarnings("rawtypes")
    final IV endpointIV = makeIV(FTS.ENDPOINT);

    @SuppressWarnings("rawtypes")
    final IV endpointTypeIV = makeIV(FTS.ENDPOINT_TYPE);

    @SuppressWarnings("rawtypes")
    final IV paramsIV = makeIV(FTS.PARAMS);

    @SuppressWarnings("rawtypes")
    final IV searchResultType = makeIV(FTS.SEARCH_RESULT_TYPE);

    @SuppressWarnings("rawtypes")
    final IV scoreIV = makeIV(FTS.SCORE);

    @SuppressWarnings("rawtypes")
    final IV snippetIV = makeIV(FTS.SNIPPET);

    /** Query specific literals */
    @SuppressWarnings("rawtypes")
    final IV searchValueIV = makeIV(store.getValueFactory().createLiteral("blue"));

    @SuppressWarnings("rawtypes")
    final IV endpointValueIV =
        makeIV(store.getValueFactory().createLiteral("http://my.external.solr.endpoint:5656"));

    @SuppressWarnings("rawtypes")
    final IV endpointTypeValueIV = makeIV(store.getValueFactory().createLiteral("Solr"));

    @SuppressWarnings("rawtypes")
    final IV paramsValueIV = makeIV(store.getValueFactory().createLiteral("bf=uses^50"));

    @SuppressWarnings("rawtypes")
    final IV searchResultTypeValueIV = makeIV(store.getValueFactory().createLiteral("URI"));

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    /**
     * The source AST.
     *
     * <pre>
     * PREFIX fts: <http://www.embergraph.org/rdf/fts#>
     * SELECT ?res ?score ?snippet ?p ?o WHERE {
     *   ?res fts:search "blue".
     *   ?res fts:endpoint  "http://my.external.solr.endpoint:5656" .
     *   ?res fts:endpointType  "Solr" .
     *   ?res fts:params "bf=uses^50" .
     *   ?res fts:searchResultType  "URI" .
     *   ?res fts:score ?score .
     *   ?res fts:snippet ?snippet .
     *   ?res ?p ?o
     * }
     * </pre>
     */
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("res"));
      projection.addProjectionVar(new VarNode("score"));
      projection.addProjectionVar(new VarNode("snippet"));
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(searchIV),
              new ConstantNode(searchValueIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(endpointIV),
              new ConstantNode(endpointValueIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(endpointTypeIV),
              new ConstantNode(endpointTypeValueIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(paramsIV),
              new ConstantNode(paramsValueIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(searchResultType),
              new ConstantNode(searchResultTypeValueIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(scoreIV),
              new VarNode("score"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new ConstantNode(snippetIV),
              new VarNode("snippet"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
    }

    /**
     * The expected AST after the rewrite
     *
     * <pre>
     * PREFIX bd: <http://www.embergraph.org/rdf/fts#>
     * QueryType: SELECT
     * SELECT ( VarNode(res) AS VarNode(res) ) ( VarNode(score) AS VarNode(score) ) ( VarNode(snippet) AS VarNode(snippet) ) ( VarNode(p) AS VarNode(p) ) ( VarNode(o) AS VarNode(o) )
     *     JoinGroupNode {
     *       SERVICE <ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#search])> {
     *         JoinGroupNode {
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#search]), ConstantNode(TermId(0L)[blue]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#endpoint]), ConstantNode(TermId(0L)[http://my.external.solr.endpoint:5656]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#endpointType]), ConstantNode(TermId(0L)[bf=uses^50]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#params]), ConstantNode(TermId(0L)[blue]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#searchResultType]), ConstantNode(TermId(0L)[URI]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#score]), VarNode(score), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(res), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/fts#snippet]), VarNode(snippet), DEFAULT_CONTEXTS)
     *         }
     *       }
     *       StatementPatternNode(VarNode(res), VarNode(p), VarNode(o), DEFAULT_CONTEXTS)
     *     }
     * }
     * </pre>
     */
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("res"));
      projection.addProjectionVar(new VarNode("score"));
      projection.addProjectionVar(new VarNode("snippet"));
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("res"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      {
        final JoinGroupNode serviceGraphPattern = new JoinGroupNode();

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(searchIV),
                new ConstantNode(searchValueIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(endpointIV),
                new ConstantNode(endpointValueIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(endpointTypeIV),
                new ConstantNode(endpointTypeValueIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(paramsIV),
                new ConstantNode(paramsValueIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(searchResultType),
                new ConstantNode(searchResultTypeValueIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(scoreIV),
                new VarNode("score"),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("res"),
                new ConstantNode(snippetIV),
                new VarNode("snippet"),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        final ServiceNode serviceNode =
            new ServiceNode(new ConstantNode(searchIV), serviceGraphPattern);

        whereClause.addChild(serviceNode);
      }
    }

    final IASTOptimizer rewriter = new ASTFulltextSearchOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }
}
