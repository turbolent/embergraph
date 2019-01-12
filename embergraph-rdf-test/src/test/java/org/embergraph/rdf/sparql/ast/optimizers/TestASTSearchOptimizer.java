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
import org.embergraph.rdf.sparql.ast.eval.ASTSearchOptimizer;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.store.BDS;
import org.openrdf.query.algebra.StatementPattern.Scope;

/*
* Test suite for {@link ASTSearchOptimizer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestASTSearchOptimizer extends AbstractASTEvaluationTestCase {

  /** */
  public TestASTSearchOptimizer() {}

  /** @param name */
  public TestASTSearchOptimizer(String name) {
    super(name);
  }

  /*
   * Given
   *
   * <pre>
   * PREFIX bd: <http://www.embergraph.org/rdf/search#>
   * SELECT ?subj ?score
   * {
   *    SELECT ?subj ?score
   *     WHERE {
   *       ?lit bd:search "mike" .
   *       ?lit bd:relevance ?score .
   *       ?subj ?p ?lit .
   *       }
   * }
   * </pre>
   *
   * The AST is rewritten as:
   *
   * <pre>
   * PREFIX bd: <http://www.embergraph.org/rdf/search#>
   * QueryType: SELECT
   * SELECT ( VarNode(subj) AS VarNode(subj) ) ( VarNode(score) AS VarNode(score) )
   *     JoinGroupNode {
   *       StatementPatternNode(VarNode(subj), VarNode(p), VarNode(lit), DEFAULT_CONTEXTS)
   *         org.embergraph.rdf.sparql.ast.eval.AST2BOpBase.estimatedCardinality=5
   *         org.embergraph.rdf.sparql.ast.eval.AST2BOpBase.originalIndex=SPOC
   *       SERVICE <ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#search])> {
   *         JoinGroupNode {
   *           StatementPatternNode(VarNode(lit), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#search]), ConstantNode(TermId(0L)[mike]), DEFAULT_CONTEXTS)
   *           StatementPatternNode(VarNode(lit), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#relevance]), VarNode(score), DEFAULT_CONTEXTS)
   *         }
   *       }
   *     }
   * }
   * </pre>
   */
  public void test_searchServiceOptimizer_01() {

    /*
     * Note: DO NOT share structures in this test!!!!
     */
    //        final VarNode s = new VarNode("s");
    //        final VarNode p = new VarNode("p");
    //        final VarNode o = new VarNode("o");
    //
    //        final IConstant const1 = new Constant<IV>(TermId.mockIV(VTE.URI));

    @SuppressWarnings("rawtypes")
    final IV searchIV = makeIV(BDS.SEARCH);

    @SuppressWarnings("rawtypes")
    final IV relevanceIV = makeIV(BDS.RELEVANCE);

    @SuppressWarnings("rawtypes")
    final IV mikeIV = makeIV(store.getValueFactory().createLiteral("mike"));

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    /*
     * The source AST.
     *
     * <pre>
     * PREFIX bd: <http://www.embergraph.org/rdf/search#>
     * SELECT ?subj ?score
     * {
     *    SELECT ?subj ?score
     *     WHERE {
     *       ?lit bd:search "mike" .
     *       ?lit bd:relevance ?score .
     *       ?subj ?p ?lit .
     *       }
     * }
     * </pre>
     */
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("subj"));
      projection.addProjectionVar(new VarNode("score"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("lit"),
              new ConstantNode(searchIV),
              new ConstantNode(mikeIV),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("lit"),
              new ConstantNode(relevanceIV),
              new VarNode("score"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("subj"),
              new VarNode("p"),
              new VarNode("lit"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
    }

    /*
     * The expected AST after the rewrite
     *
     * <pre>
     * PREFIX bd: <http://www.embergraph.org/rdf/search#>
     * QueryType: SELECT
     * SELECT ( VarNode(subj) AS VarNode(subj) ) ( VarNode(score) AS VarNode(score) )
     *     JoinGroupNode {
     *       StatementPatternNode(VarNode(subj), VarNode(p), VarNode(lit), DEFAULT_CONTEXTS)
     *         org.embergraph.rdf.sparql.ast.eval.AST2BOpBase.estimatedCardinality=5
     *         org.embergraph.rdf.sparql.ast.eval.AST2BOpBase.originalIndex=SPOC
     *       SERVICE <ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#search])> {
     *         JoinGroupNode {
     *           StatementPatternNode(VarNode(lit), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#search]), ConstantNode(TermId(0L)[mike]), DEFAULT_CONTEXTS)
     *           StatementPatternNode(VarNode(lit), ConstantNode(TermId(0U)[http://www.embergraph.org/rdf/search#relevance]), VarNode(score), DEFAULT_CONTEXTS)
     *         }
     *       }
     *     }
     * }
     * </pre>
     */
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("subj"));
      projection.addProjectionVar(new VarNode("score"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("subj"),
              new VarNode("p"),
              new VarNode("lit"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      {
        final JoinGroupNode serviceGraphPattern = new JoinGroupNode();

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("lit"),
                new ConstantNode(searchIV),
                new ConstantNode(mikeIV),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        serviceGraphPattern.addChild(
            new StatementPatternNode(
                new VarNode("lit"),
                new ConstantNode(relevanceIV),
                new VarNode("score"),
                null /* c */,
                Scope.DEFAULT_CONTEXTS));

        final ServiceNode serviceNode =
            new ServiceNode(new ConstantNode(searchIV), serviceGraphPattern);

        whereClause.addChild(serviceNode);
      }
    }

    final IASTOptimizer rewriter = new ASTSearchOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }
}
