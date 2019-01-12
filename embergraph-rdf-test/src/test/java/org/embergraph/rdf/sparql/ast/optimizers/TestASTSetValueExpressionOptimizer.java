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

import java.util.UUID;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.ContextBindingSet;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IQueryContext;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.BlockingBufferWithStats;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.bop.solutions.MockQuery;
import org.embergraph.bop.solutions.MockQueryContext;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;
import org.openrdf.query.algebra.StatementPattern.Scope;

/**
 * Test suite for {@link ASTSetValueExpressionsOptimizer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestASTSetValueExpressionOptimizer extends AbstractASTEvaluationTestCase {

  /** */
  public TestASTSetValueExpressionOptimizer() {}

  /** @param name */
  public TestASTSetValueExpressionOptimizer(String name) {
    super(name);
  }

  /**
   * Given
   *
   * <pre>
   * SELECT ?s (?o as 12+1) where {?s ?p ?o}
   * </pre>
   *
   * Verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s (?o as 13) where {?s ?p ?o}
   * </pre>
   *
   * TODO unit test with FILTER(sameTerm(var,constExpr)) in {@link ASTBindingAssigner}
   */
  public void test_reduceFunctionToConstant() {

    /*
     * Note: DO NOT share structures in this test!!!!
     */

    @SuppressWarnings("rawtypes")
    final IV c1 = makeIV(store.getValueFactory().createLiteral(1));
    @SuppressWarnings("rawtypes")
    final IV c12 = makeIV(store.getValueFactory().createLiteral(12));
    @SuppressWarnings("rawtypes")
    final IV c13 =
        store
            .getLexiconRelation()
            .getInlineIV(store.getValueFactory().createLiteral("13", XSD.INTEGER));
    store.commit();

    @SuppressWarnings("rawtypes")
    final BOpContext context;
    {
      final BOpStats stats = new BOpStats();
      final PipelineOp mockQuery = new MockQuery();
      final IAsynchronousIterator<IBindingSet[]> source =
          new ThickAsynchronousIterator<IBindingSet[]>(new IBindingSet[][] {});
      final IBlockingBuffer<IBindingSet[]> sink =
          new BlockingBufferWithStats<IBindingSet[]>(mockQuery, stats);
      final UUID queryId = UUID.randomUUID();
      final IQueryContext queryContext = new MockQueryContext(queryId);
      final IRunningQuery runningQuery =
          new MockRunningQuery(
              null /* fed */, store.getIndexManager() /* indexManager */, queryContext);
      context =
          BOpContext.newMock(
              runningQuery,
              null /* fed */,
              store.getIndexManager() /* localIndexManager */,
              -1 /* partitionId */,
              stats,
              mockQuery,
              false /* lastInvocation */,
              source,
              sink,
              null /* sink2 */);
    }

    final IBindingSet[] bsets =
        new IBindingSet[] {
          new ContextBindingSet(
              context,
              new ListBindingSet(
                  //                new IVariable[] { Var.var("p") },
                  //                new IConstant[] { new Constant<IV>(mockIV) }
                  ))
        };

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final FunctionNode fn = FunctionNode.add(new ConstantNode(c12), new ConstantNode(c1));

      projection.addProjectionExpression(new AssignmentNode(new VarNode("o"), fn));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final FunctionNode fn = FunctionNode.add(new ConstantNode(c12), new ConstantNode(c1));

      fn.setValueExpression(new Constant<IV<?, ?>>(c13));

      projection.addProjectionExpression(new AssignmentNode(new VarNode("o"), fn));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
    }

    final IASTOptimizer rewriter = new ASTSetValueExpressionsOptimizer();

    final IQueryNode actual =
        rewriter
            .optimize(
                new AST2BOpContext(new ASTContainer(given), store),
                new QueryNodeWithBindingSet(given, bsets))
            .getQueryNode();

    assertSameAST(expected, actual);
  }
}
