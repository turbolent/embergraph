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
 * Created on June 2, 2015
 */
package org.embergraph.rdf.sparql.ast.optimizers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.BindingsClause;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.FunctionRegistry;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.PathNode;
import org.embergraph.rdf.sparql.ast.PathNode.PathAlternative;
import org.embergraph.rdf.sparql.ast.PathNode.PathElt;
import org.embergraph.rdf.sparql.ast.PathNode.PathMod;
import org.embergraph.rdf.sparql.ast.PathNode.PathSequence;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.PropertyPathNode;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.SubqueryRoot;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.algebra.StatementPattern.Scope;

/*
 * Test suite for the {@link ASTStaticBindingsOptimizer} class.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestASTStaticBindingsOptimizer extends AbstractASTEvaluationTestCase {

  public TestASTStaticBindingsOptimizer() {}

  public TestASTStaticBindingsOptimizer(String name) {
    super(name);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?s where {?s ?p ?o}
   * </pre>
   *
   * and a binding for <code>?p</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where {?s CONST ?o}
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution.
   *
   * <p>Carried over from {@link TestASTBindingAssigner#test_astBindingAssigner()}.
   */
  public void testInlineFromExogeneousBindings() {

    /*
     * Note: DO NOT share structures in this test!!!!
     */

    final IV mockIV = TermId.mockIV(VTE.URI);

    final IBindingSet[] bsets =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("p")}, new IConstant[] {new Constant<>(mockIV)})
        };

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

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

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), mockIV)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p where {?s ?p ?s}
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p where {CONST ?p CONST}
   * </pre>
   *
   * where CONST is the binding for <code>?s</code> in the input solution.
   *
   * <p>Note: For this unit test, a variable is replaced in more than one location in the AST.
   *
   * <p>Carried over from {@link TestASTBindingAssigner#test_astBindingAssigner2()}.
   */
  public void testMultiInlineFromExogeneousBindings() {

    /*
     * Note: DO NOT SHARE STRUCTURES IN THIS TEST.
     */

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphLiteral c12Lit = f.createLiteral(12);

    final IV c12 = makeIV(c12Lit);

    final EmbergraphValue[] values = new EmbergraphValue[] {c12Lit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("s")}, new IConstant[] {new Constant<>(c12)})
        };

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("s"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      given.setWhereClause(whereClause);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new ConstantNode(new Constant(Var.var("s"), c12)),
              new VarNode("p"),
              new ConstantNode(new Constant(Var.var("s"), c12)),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { BIND(CONST AS ?p) . ?s ?p ?s }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where { ?s CONST ?s }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineFromBind() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
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

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set in a nested setting.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { { BIND(CONST AS ?p) . ?s ?p ?s } }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where { { ?s CONST ?s } }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineFromBindNested1() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      final JoinGroupNode jgn = new JoinGroupNode();
      whereClause.addChild(jgn);
      jgn.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
      jgn.addChild(
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

      final JoinGroupNode whereClause = new JoinGroupNode();
      final JoinGroupNode jgn = new JoinGroupNode();
      whereClause.addChild(jgn);

      jgn.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set in a nested setting.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { { BIND(CONST AS ?p) } ?s ?p ?s }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where { { } ?s CONST ?s }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineFromBindNested2() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      final JoinGroupNode jgn = new JoinGroupNode();
      whereClause.addChild(jgn);
      jgn.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
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

      final JoinGroupNode whereClause = new JoinGroupNode();
      final JoinGroupNode jgn = new JoinGroupNode();
      whereClause.addChild(jgn);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from top-level VALUES clause, including the
   * removal of the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?p where { ?s ?p ?s } VALUES ?p { CONST }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p where { ?s CONST ?s }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineFromTopLevelValues() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      final IVariable<?> var = Var.var("p");
      final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
      declaredVars.add(var);

      final List<IBindingSet> bindingSets = new ArrayList<>();
      IBindingSet bs = new ListBindingSet();
      bs.set(var, new Constant<>(cTest));
      bindingSets.add(bs);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      final BindingsClause bc = new BindingsClause(declaredVars, bindingSets);
      given.setBindingsClause(bc);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from top-level VALUES clause, including the
   * removal of the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?p where { VALUES ?p { CONST } . ?s ?p ?s }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p where { ?s CONST ?s }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineFromValues() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      final IVariable<?> var = Var.var("p");
      final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
      declaredVars.add(var);

      final List<IBindingSet> bindingSets = new ArrayList<>();
      IBindingSet bs = new ListBindingSet();
      bs.set(var, new Constant<>(cTest));
      bindingSets.add(bs);

      final BindingsClause bc = new BindingsClause(declaredVars, bindingSets);
      whereClause.addChild(bc);
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

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from top-level VALUES clause, including the
   * removal of the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?p where { BIND(1 AS ?p) }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p where { }
   * </pre>
   *
   * and the exogeneous mapping is /extended/ by { ?p -> CONST }.
   */
  public void testMergeWithSimpleExogeneousMapping() {

    final EmbergraphLiteral cTrueLit = store.getValueFactory().createLiteral(true);
    final EmbergraphLiteral cFalseLit = store.getValueFactory().createLiteral(false);

    final IV cTrue = makeIV(cTrueLit);
    final IV cFalse = makeIV(cFalseLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTrueLit, cFalseLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("s")}, new IConstant[] {new Constant<>(cFalse)})
        };

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTrue)));

      given.setWhereClause(whereClause);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 2);
    assertTrue(resBs[0].get(Var.var("s")).equals(new Constant<>(cFalse)));
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTrue)));

    assertSameAST(expected, res.getQueryNode());
  }

  public void testMergeWithComplexExogeneousMapping() {}

  /*
   * Assert that even for complex exogeneous mappings, variables that map to the same value in all
   * cases are inlined properly.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?p where {
   *   BIND("b" AS ?b) .
   *   VALUES (?c ?d) { ("c1" "d1") ("c2" "d2") }
   * } VALUES ?e { "e" }
   * </pre>
   *
   * and an exogeneous binding set { { ?a -> "a1", ?c -> "c1" }, { ?a -> "a2", ?c -> "c2" }, { ?a ->
   * "a3", ?c -> "c1" } }
   *
   * <p>in an input solution, verify that the rewritten query is
   *
   * <pre>
   * SELECT ?p where { }
   * </pre>
   *
   * and that the resulting generated exogeneous binding set is:
   *
   * <p>{ { ?a -> "a1", ?b -> "b", ?c -> "c1", ?d -> "d1", ?e -> "e" }, { ?a -> "a2", ?b -> "b", ?c
   * -> "c2", ?d -> "d2", ?e -> "e" } }
   */
  public void testInliningForComplexExogeneousMapping() {

    final EmbergraphLiteral a1LitBD = store.getValueFactory().createLiteral("a1");
    final EmbergraphLiteral a2LitBD = store.getValueFactory().createLiteral("a2");
    final EmbergraphLiteral a3LitBD = store.getValueFactory().createLiteral("a3");
    final EmbergraphLiteral bLitBD = store.getValueFactory().createLiteral("b");
    final EmbergraphLiteral c1LitBD = store.getValueFactory().createLiteral("c1");
    final EmbergraphLiteral c2LitBD = store.getValueFactory().createLiteral("c2");
    final EmbergraphLiteral c3LitBD = store.getValueFactory().createLiteral("c3");
    final EmbergraphLiteral d1LitBD = store.getValueFactory().createLiteral("d1");
    final EmbergraphLiteral d2LitBD = store.getValueFactory().createLiteral("d2");
    final EmbergraphLiteral eLitBD = store.getValueFactory().createLiteral("e");

    final IV a1Lit = makeIV(a1LitBD);
    final IV a2Lit = makeIV(a2LitBD);
    final IV a3Lit = makeIV(a3LitBD);
    final IV bLit = makeIV(bLitBD);
    final IV c1Lit = makeIV(c1LitBD);
    final IV c2Lit = makeIV(c2LitBD);
    final IV c3Lit = makeIV(c3LitBD);
    final IV d1Lit = makeIV(d1LitBD);
    final IV d2Lit = makeIV(d2LitBD);
    final IV eLit = makeIV(eLitBD);

    final EmbergraphValue[] values =
        new EmbergraphValue[] {
          a1LitBD, a2LitBD, a3LitBD, bLitBD, c1LitBD, c2LitBD, c3LitBD, d1LitBD, d2LitBD, eLitBD
        };
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    /*
     * Construct in mapping set: { { ?a -> "a1", ?c -> "c1" }, { ?a -> "a2", ?c -> "c2" }, { ?a ->
     * "a3", ?c -> "c3" } }
     */
    final IBindingSet exogeneousIn1 = new ListBindingSet();
    exogeneousIn1.set(Var.var("a"), new Constant<>(a1Lit));
    exogeneousIn1.set(Var.var("c"), new Constant<>(c1Lit));
    final IBindingSet exogeneousIn2 = new ListBindingSet();
    exogeneousIn2.set(Var.var("a"), new Constant<>(a2Lit));
    exogeneousIn2.set(Var.var("c"), new Constant<>(c2Lit));
    final IBindingSet exogeneousIn3 = new ListBindingSet();
    exogeneousIn3.set(Var.var("a"), new Constant<>(a3Lit));
    exogeneousIn3.set(Var.var("c"), new Constant<>(c3Lit));
    final IBindingSet[] bsetsGiven =
        new IBindingSet[] {exogeneousIn1, exogeneousIn2, exogeneousIn3};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      // BIND("b" AS ?b)
      final AssignmentNode bAss = new AssignmentNode(new VarNode("b"), new ConstantNode(bLit));

      // VALUES ?e { "e" }
      final LinkedHashSet<IVariable<?>> declaredVarsE = new LinkedHashSet<>();
      declaredVarsE.add(Var.var("e"));

      final List<IBindingSet> bindingSetsE = new ArrayList<>();
      IBindingSet bsE = new ListBindingSet();
      bsE.set(Var.var("e"), new Constant<>(eLit));
      bindingSetsE.add(bsE);
      final BindingsClause eBindings = new BindingsClause(declaredVarsE, bindingSetsE);

      // VALUES (?c ?d) { ("c1" "d1") ("c2" "d2") }
      final LinkedHashSet<IVariable<?>> declaredVarsBcd = new LinkedHashSet<>();
      declaredVarsE.add(Var.var("b"));
      declaredVarsE.add(Var.var("c"));
      declaredVarsE.add(Var.var("d"));

      final List<IBindingSet> bindingSetsCd = new ArrayList<>();
      IBindingSet bsCd1 = new ListBindingSet();
      bsCd1.set(Var.var("c"), new Constant<>(c1Lit));
      bsCd1.set(Var.var("d"), new Constant<>(d1Lit));
      bindingSetsCd.add(bsCd1);

      IBindingSet bsCd2 = new ListBindingSet();
      bsCd2.set(Var.var("c"), new Constant<>(c2Lit));
      bsCd2.set(Var.var("d"), new Constant<>(d2Lit));
      bindingSetsCd.add(bsCd2);

      final BindingsClause bcdBindings = new BindingsClause(declaredVarsBcd, bindingSetsCd);

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(bAss);
      whereClause.addChild(bcdBindings);
      given.setBindingsClause(eBindings);

      given.setWhereClause(whereClause);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    /*
     * Verify that the resulting binding set is:
     * { { ?a -> "a1", ?b -> "b", ?c -> "c1", ?d -> "d1", ?e -> "e" },
     *   { ?a -> "a2", ?b -> "b", ?c -> "c2", ?d -> "d2", ?e -> "e" } }
     *
     */

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();

    final IBindingSet bs1 = resBs[0];
    assertTrue(bs1.size() == 5);
    assertTrue(bs1.get(Var.var("a")).equals(new Constant<>(a1Lit)));
    assertTrue(bs1.get(Var.var("b")).equals(new Constant<>(bLit)));
    assertTrue(bs1.get(Var.var("c")).equals(new Constant<>(c1Lit)));
    assertTrue(bs1.get(Var.var("d")).equals(new Constant<>(d1Lit)));
    assertTrue(bs1.get(Var.var("e")).equals(new Constant<>(eLit)));

    final IBindingSet bs2 = resBs[1];
    assertTrue(bs2.size() == 5);
    assertTrue(bs2.get(Var.var("a")).equals(new Constant<>(a2Lit)));
    assertTrue(bs2.get(Var.var("b")).equals(new Constant<>(bLit)));
    assertTrue(bs2.get(Var.var("c")).equals(new Constant<>(c2Lit)));
    assertTrue(bs2.get(Var.var("d")).equals(new Constant<>(d2Lit)));
    assertTrue(bs2.get(Var.var("e")).equals(new Constant<>(eLit)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p ?o where {?s ?p ?o. FILTER( sameTerm(?o,12) ) }
   * </pre>
   *
   * Verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p CONST where {?s ?p CONST . FILTER( sameTerm(CONST,12) ) }
   * </pre>
   *
   * where CONST is the binding for <code>?o</code> given by the FILTER.
   *
   * <p>Note: For this unit test, a variable is replaced in more than one location in the AST.
   *
   * <p>Carried over from {@link
   * TestASTBindingAssigner#test_astBindingAssigner_filter_eq_ConstURI()}
   */
  public void testSimpleSameTermFilter() {

    /*
     * Note: DO NOT SHARE STRUCTURES IN THIS TEST.
     */

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphLiteral c12Lit = f.createLiteral(12);

    final IV c12 = makeIV(c12Lit);

    final EmbergraphValue[] values = new EmbergraphValue[] {c12Lit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.sameTerm(new VarNode("o"), new ConstantNode(c12))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));

      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new ConstantNode(new Constant(Var.var("o"), c12)),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(
              FunctionNode.sameTerm(
                  new ConstantNode(new Constant(Var.var("o"), c12)), new ConstantNode(c12))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p ?o where {?s ?p ?o. FILTER( ?o=CONST_URI) ) }
   * </pre>
   *
   * Verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p ?o where {?s ?p CONST_URI . FILTER( ?o=CONST_URI) ) }
   * </pre>
   *
   * where CONST_URI is a URI binding for <code>?o</code> given by the FILTER.
   *
   * <p>Note: For this unit test, a variable is replaced in more than one location in the AST.
   *
   * <p>Carried over from {@link
   * TestASTBindingAssigner#test_astBindingAssigner_filter_eq_ConstURI()}.
   */
  public void testInlineSimpleFilterEqURI() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI fooLit = f.createURI(":foo");

    final IV foo = makeIV(fooLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {fooLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new VarNode("o"), new ConstantNode(foo))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new ConstantNode(new Constant(Var.var("o"), foo)),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(
              FunctionNode.EQ(
                  new ConstantNode(new Constant(Var.var("o"), foo)), new ConstantNode(foo))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p ?o where {?s ?p ?o. FILTER( ?o IN (CONST_URI) ) }
   * </pre>
   *
   * Verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?p ?o where { ?s ?p CONST_URI . FILTER( ?o IN (CONST_URI) ) }
   *
   * </pre>
   *
   * where CONST_URI is a URI binding for <code>?o</code> given by the FILTER.
   *
   * <p>Note: For this unit test, a variable is replaced in more than one location in the AST.
   *
   * <p>Carried over from {@link
   * TestASTBindingAssigner#test_astBindingAssigner_filter_eq_ConstURI()}.
   */
  public void testInlineSimpleFilterINURI() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI fooLit = f.createURI(":foo");

    final IV foo = makeIV(fooLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {fooLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(
              new FunctionNode(
                  FunctionRegistry.IN,
                  null /* scalarValues */,
                  new VarNode("o"),
                  new ConstantNode(foo))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new ConstantNode(new Constant(Var.var("o"), foo)),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      whereClause.addChild(
          new FilterNode(
              new FunctionNode(
                  FunctionRegistry.IN,
                  null /* scalarValues */,
                  new ConstantNode(new Constant(Var.var("o"), foo)),
                  new ConstantNode(foo))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /** Test testInlineSimpleFilterEqURI with FILTER conditions reversed. */
  public void testInlineSimpleFilterEqURIRev() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI fooLit = f.createURI(":foo");

    final IV foo = makeIV(fooLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {fooLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new ConstantNode(foo), new VarNode("o"))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new ConstantNode(new Constant(Var.var("o"), foo)),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(
              FunctionNode.EQ(
                  new ConstantNode(foo), new ConstantNode(new Constant(Var.var("o"), foo)))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p ?o where {?s ?p ?o. FILTER( ?o=CONST_LIT) ) }
   * </pre>
   *
   * Verify that the AST is not rewritten.
   *
   * Carried over from {@link TestASTBindingAssigner#test_astBindingAssigner_filter_eq_ConstLit()
   */
  public void testNotInlineSimpleFilterEqLiteral() {

    /*
     * Note: DO NOT SHARE STRUCTURES IN THIS TEST.
     */

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphLiteral fooLit = f.createLiteral("foo");

    final IV foo = makeIV(fooLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {fooLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new VarNode("o"), new ConstantNode(foo))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new VarNode("o"), new ConstantNode(foo))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /** Test testNotInlineSimpleFilterEqLiteral with filter reversed. */
  public void testNotInlineSimpleFilterEqLiteralRev() {

    /*
     * Note: DO NOT SHARE STRUCTURES IN THIS TEST.
     */

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphLiteral fooLit = f.createLiteral("foo");

    final IV foo = makeIV(fooLit);

    final EmbergraphValue[] values = new EmbergraphValue[] {fooLit};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsets = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      given.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new ConstantNode(foo), new VarNode("o"))));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));
      expected.setProjection(projection);

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new ConstantNode(foo), new VarNode("o"))));
    }

    final IASTOptimizer rewriter = new ASTStaticBindingsOptimizer();
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final IQueryNode actual =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsets)).getQueryNode();

    assertSameAST(expected, actual);
  }

  /*
   * Given
   *
   * <pre>
   * SELECT ?p ?o where { BIND(<http://p> AS ?p> . FILTER(?o=CONST_LIT) . ?s ?p ?o ) }
   * </pre>
   *
   * Verify that the AST is rewritten as
   *
   * <pre>
   * SELECT ?p ?o where { BIND(<http://p> AS ?p> . FILTER(?o=CONST_LIT) . ?s ?p ?o ) }
   * </pre>
   *
   * Carried over from {@link TestASTBindingAssigner#test_astBindingAssigner_filter_eq_ConstLit()
   */
  public void testFilterAndBindInlinedBindAddedToExogeneous() {}

  /*
   * Assert that even for complex exogeneous mappings, variables that map to the same value in all
   * cases are inlined properly.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?p WHERE {
   *   {
   *     SELECT ?p where {
   *       BIND("b" AS ?b) .
   *       VALUES (?c ?d) { ("c1" "d1") ("c2" "d2") }
   *     } VALUES ?e { "e" }
   *   }
   * } VALUES (?a ?c) { ("a1" "c1") ("a2" "c2") ("a2" "c3" )}
   * </pre>
   *
   * verify that the rewritten query is
   *
   * <pre>
   * SELECT ?p WHERE {
   *   {
   *     SELECT ?p where {
   *     } VALUES (?b ?c ?e) { ("b" "c1" "e" "d1) ("b" c2" "e" "d2") }
   *   }
   * }
   * </pre>
   *
   * and that the resulting generated exogeneous binding is identical to the VALUES clause of the
   * top-level query.
   */
  public void testValuesComplexExogeneousMappingInSubquery() {

    final EmbergraphLiteral a1LitBD = store.getValueFactory().createLiteral("a1");
    final EmbergraphLiteral a2LitBD = store.getValueFactory().createLiteral("a2");
    final EmbergraphLiteral a3LitBD = store.getValueFactory().createLiteral("a3");
    final EmbergraphLiteral bLitBD = store.getValueFactory().createLiteral("b");
    final EmbergraphLiteral c1LitBD = store.getValueFactory().createLiteral("c1");
    final EmbergraphLiteral c2LitBD = store.getValueFactory().createLiteral("c2");
    final EmbergraphLiteral c3LitBD = store.getValueFactory().createLiteral("c3");
    final EmbergraphLiteral d1LitBD = store.getValueFactory().createLiteral("d1");
    final EmbergraphLiteral d2LitBD = store.getValueFactory().createLiteral("d2");
    final EmbergraphLiteral eLitBD = store.getValueFactory().createLiteral("e");

    final IV a1Lit = makeIV(a1LitBD);
    final IV a2Lit = makeIV(a2LitBD);
    final IV a3Lit = makeIV(a3LitBD);
    final IV bLit = makeIV(bLitBD);
    final IV c1Lit = makeIV(c1LitBD);
    final IV c2Lit = makeIV(c2LitBD);
    final IV c3Lit = makeIV(c3LitBD);
    final IV d1Lit = makeIV(d1LitBD);
    final IV d2Lit = makeIV(d2LitBD);
    final IV eLit = makeIV(eLitBD);

    final EmbergraphValue[] values =
        new EmbergraphValue[] {
          a1LitBD, a2LitBD, a3LitBD, bLitBD, c1LitBD, c2LitBD, c3LitBD, d1LitBD, d2LitBD, eLitBD
        };
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    /*
     * Construct in mapping set: { { ?a -> "a1", ?c -> "c1" }, { ?a -> "a2", ?c -> "c2" }, { ?a ->
     * "a3", ?c -> "c3" } }
     */
    final IBindingSet exogeneousIn1 = new ListBindingSet();
    exogeneousIn1.set(Var.var("a"), new Constant<>(a1Lit));
    exogeneousIn1.set(Var.var("c"), new Constant<>(c1Lit));
    final IBindingSet exogeneousIn2 = new ListBindingSet();
    exogeneousIn2.set(Var.var("a"), new Constant<>(a2Lit));
    exogeneousIn2.set(Var.var("c"), new Constant<>(c2Lit));
    final IBindingSet exogeneousIn3 = new ListBindingSet();
    exogeneousIn3.set(Var.var("a"), new Constant<>(a3Lit));
    exogeneousIn3.set(Var.var("c"), new Constant<>(c3Lit));
    final IBindingSet[] bsetsGiven =
        new IBindingSet[] {exogeneousIn1, exogeneousIn2, exogeneousIn3};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      // BIND("b" AS ?b)
      final AssignmentNode bAss = new AssignmentNode(new VarNode("b"), new ConstantNode(bLit));

      // VALUES ?e { "e" }
      final LinkedHashSet<IVariable<?>> declaredVarsE = new LinkedHashSet<>();
      declaredVarsE.add(Var.var("e"));

      final List<IBindingSet> bindingSetsE = new ArrayList<>();
      IBindingSet bsE = new ListBindingSet();
      bsE.set(Var.var("e"), new Constant<>(eLit));
      bindingSetsE.add(bsE);
      final BindingsClause eBindings = new BindingsClause(declaredVarsE, bindingSetsE);

      // VALUES (?c ?d) { ("c1" "d1") ("c2" "d2") }
      final LinkedHashSet<IVariable<?>> declaredVarsBcd = new LinkedHashSet<>();
      declaredVarsE.add(Var.var("b"));
      declaredVarsE.add(Var.var("c"));
      declaredVarsE.add(Var.var("d"));

      final List<IBindingSet> bindingSetsCd = new ArrayList<>();
      IBindingSet bsCd1 = new ListBindingSet();
      bsCd1.set(Var.var("c"), new Constant<>(c1Lit));
      bsCd1.set(Var.var("d"), new Constant<>(d1Lit));
      bindingSetsCd.add(bsCd1);

      IBindingSet bsCd2 = new ListBindingSet();
      bsCd2.set(Var.var("c"), new Constant<>(c2Lit));
      bsCd2.set(Var.var("d"), new Constant<>(d2Lit));
      bindingSetsCd.add(bsCd2);

      final BindingsClause bcdBindings = new BindingsClause(declaredVarsBcd, bindingSetsCd);

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(bAss);
      whereClause.addChild(bcdBindings);
      given.setBindingsClause(eBindings);

      given.setWhereClause(whereClause);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("p"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    /*
     * Verify that the resulting binding set is:
     * { { ?a -> "a1", ?b -> "b", ?c -> "c1", ?d -> "d1", ?e -> "e" },
     *   { ?a -> "a2", ?b -> "b", ?c -> "c2", ?d -> "d2", ?e -> "e" } }
     *
     */

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();

    final IBindingSet bs1 = resBs[0];
    assertTrue(bs1.size() == 5);
    assertTrue(bs1.get(Var.var("a")).equals(new Constant<>(a1Lit)));
    assertTrue(bs1.get(Var.var("b")).equals(new Constant<>(bLit)));
    assertTrue(bs1.get(Var.var("c")).equals(new Constant<>(c1Lit)));
    assertTrue(bs1.get(Var.var("d")).equals(new Constant<>(d1Lit)));
    assertTrue(bs1.get(Var.var("e")).equals(new Constant<>(eLit)));

    final IBindingSet bs2 = resBs[1];
    assertTrue(bs2.size() == 5);
    assertTrue(bs2.get(Var.var("a")).equals(new Constant<>(a2Lit)));
    assertTrue(bs2.get(Var.var("b")).equals(new Constant<>(bLit)));
    assertTrue(bs2.get(Var.var("c")).equals(new Constant<>(c2Lit)));
    assertTrue(bs2.get(Var.var("d")).equals(new Constant<>(d2Lit)));
    assertTrue(bs2.get(Var.var("e")).equals(new Constant<>(eLit)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { FILTER(?p=URI) . BIND(URI AS ?p) } }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where { FILTER(Const(?p -> URI)=URI) . BIND(URI AS ?p) }
   * </pre>
   *
   * with unmodified exogenous mapping set.
   */
  public void testInlineFromBindInFilter() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");

    final IV cTest = makeIV(cTestUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new FilterNode(FunctionNode.EQ(new VarNode("p"), new ConstantNode(cTest))));
      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new FilterNode(
              FunctionNode.EQ(
                  new ConstantNode(new Constant(Var.var("p"), cTest)), new ConstantNode(cTest))));
      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));

      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 0);

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { FILTER(?p=URI && ?p!=URI2) . BIND(URI AS ?p) } }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where { FILTER(Const(?p -> URI)=URI && Const(?p -> URI)!=URI2) . BIND(URI AS ?p) }
   * </pre>
   *
   * with unmodified exogenous mapping set.
   */
  public void testInlineFromBindInComplexFilter() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");
    final EmbergraphURI cTestUri2 = f.createURI("http://www.test2.com");

    final IV cTest = makeIV(cTestUri);
    final IV cTest2 = makeIV(cTestUri2);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri, cTestUri2};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new FilterNode(
              FunctionNode.AND(
                  FunctionNode.EQ(new VarNode("p"), new ConstantNode(cTest)),
                  FunctionNode.NE(new VarNode("p"), new ConstantNode(cTest2)))));
      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new FilterNode(
              FunctionNode.AND(
                  FunctionNode.EQ(
                      new ConstantNode(new Constant(Var.var("p"), cTest)), new ConstantNode(cTest)),
                  FunctionNode.NE(
                      new ConstantNode(new Constant(Var.var("p"), cTest)),
                      new ConstantNode(cTest2)))));
      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));

      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 0);

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where {
   *   BIND(CONST AS ?p) . ?s ?p ?s
   *   SELECT ?s where {
   *     BIND(CONST2 AS ?p) . ?s ?p ?s
   *   }
   * }
   * </pre>
   *
   * and a binding for <code>?s</code> in an input solution, verify that the AST is rewritten as:
   *
   * <pre>
   * SELECT ?s where {
   *   ?s CONST ?s
   *   SELECT ?s where {
   *     ?s CONST2 ?s
   *   } VALUES ?p (CONST2)
   * }
   * </pre>
   *
   * where CONST is the binding for <code>?p</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testInlineWithSubquery() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");
    final EmbergraphURI cTestUri2 = f.createURI("http://www.test2.com");

    final IV cTest = makeIV(cTestUri);
    final IV cTest2 = makeIV(cTestUri2);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri, cTestUri2};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest)));
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      SubqueryRoot sq = new SubqueryRoot(QueryType.SELECT);
      final JoinGroupNode sqWhereClause = new JoinGroupNode();

      sqWhereClause.addChild(new AssignmentNode(new VarNode("p"), new ConstantNode(cTest2)));
      sqWhereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      sq.setWhereClause(sqWhereClause);

      whereClause.addChild(sq);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      /*
       * SELECT ?s where {
       *   ?s CONST ?s
       *   SELECT ?s where {
       *     ?s CONST2 ?s
       *   } VALUES ?p (CONST2)
       * }
       */

      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      expected.setWhereClause(whereClause);

      SubqueryRoot sq = new SubqueryRoot(QueryType.SELECT);
      final JoinGroupNode sqWhereClause = new JoinGroupNode();

      sqWhereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new ConstantNode(new Constant(Var.var("p"), cTest2)),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      sq.setWhereClause(sqWhereClause);

      final IVariable<?> var = Var.var("p");
      final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
      declaredVars.add(var);

      final List<IBindingSet> bindingSets = new ArrayList<>();
      IBindingSet bs = new ListBindingSet();
      bs.set(var, new Constant<>(cTest2));
      bindingSets.add(bs);

      BindingsClause sqBC = new BindingsClause(declaredVars, bindingSets);
      sq.setBindingsClause(sqBC);

      whereClause.addChild(sq);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("p")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause of static bindings from BIND clause, including the removal of
   * the clause and putting the values into the exogeneous mapping set.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where {
   *   ?s ?p ?o
   *   SELECT ?x where {
   *     ?s ?p ?o
   *     BIND(CONST1 AS ?x)
   *   } VALUES ?y (CONST2)
   * }
   * </pre>
   *
   * and an exogenous binding CONST3 for <code>?s</code> in an input solution, verify that the AST
   * is rewritten as:
   *
   * <pre>
   * SELECT ?s where {
   *   CONST3 ?p ?o
   *   SELECT ?x where {
   *     ?s ?p ?o
   *   } VALUES ?x ?y (CONST1 CONST2)
   * }
   * </pre>
   */
  public void testSubqueryWithValues() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI c1BD = f.createURI("http://www.test.com");
    final EmbergraphLiteral c2BD = f.createLiteral("X");
    final EmbergraphLiteral c3BD = f.createLiteral("Y");

    final IV c1 = makeIV(c1BD);
    final IV c2 = makeIV(c2BD);
    final IV c3 = makeIV(c3BD);

    final EmbergraphValue[] values = new EmbergraphValue[] {c1BD, c2BD, c3BD};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {Var.var("s")}, new IConstant[] {new Constant<>(c3)})
        };

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);
      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      SubqueryRoot sq = new SubqueryRoot(QueryType.SELECT);

      final ProjectionNode sqProjection = new ProjectionNode();
      sqProjection.addProjectionVar(new VarNode("x"));
      sq.setProjection(projection);

      final JoinGroupNode sqWhereClause = new JoinGroupNode();

      sqWhereClause.addChild(new AssignmentNode(new VarNode("x"), new ConstantNode(c1)));
      sqWhereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      sq.setWhereClause(sqWhereClause);

      final IVariable<?> var = Var.var("y");
      final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
      declaredVars.add(var);

      final List<IBindingSet> bindingSets = new ArrayList<>();
      IBindingSet bs = new ListBindingSet();
      bs.set(var, new Constant<>(c2));
      bindingSets.add(bs);

      BindingsClause sqBc = new BindingsClause(declaredVars, bindingSets);
      sq.setBindingsClause(sqBc);

      whereClause.addChild(sq);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);
      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new ConstantNode(new Constant(Var.var("s"), c3)),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      SubqueryRoot sq = new SubqueryRoot(QueryType.SELECT);

      final ProjectionNode sqProjection = new ProjectionNode();
      sqProjection.addProjectionVar(new VarNode("x"));
      sq.setProjection(projection);

      final JoinGroupNode sqWhereClause = new JoinGroupNode();

      sqWhereClause.addChild(
          new StatementPatternNode(
              new VarNode("s"),
              new VarNode("p"),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      sq.setWhereClause(sqWhereClause);

      final IVariable<?> varX = Var.var("x");
      final IVariable<?> varY = Var.var("y");
      final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
      declaredVars.add(varX);
      declaredVars.add(varY);

      final List<IBindingSet> bindingSets = new ArrayList<>();
      IBindingSet bs = new ListBindingSet();
      bs.set(varX, new Constant<>(c1));
      bs.set(varY, new Constant<>(c2));
      bindingSets.add(bs);

      BindingsClause sqBc = new BindingsClause(declaredVars, bindingSets);
      sq.setBindingsClause(sqBc);

      whereClause.addChild(sq);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has not been modified
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("s")).equals(new Constant<>(c3)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Tests rewriting of a modified form of ticket #653 (i.e., the corresponding SELECT query). In
   * particular, verify that the query
   *
   * <pre>
   * SELECT ?uri ?p ?o WHERE {
   *   BIND (CONST as ?uri )
   *   ?uri ?p ?o .
   *   OPTIONAL {
   *     ?uri a ?type .
   *     ?type rdfs:label ?typelabel .
   *   }
   * }
   * </pre>
   *
   * is rewritten as
   *
   * <pre>
   * SELECT ?uri ?p ?o WHERE {
   *   CONST ?p ?o .
   *   OPTIONAL {
   *     ?uri a ?type .
   *     ?type rdfs:label ?typelabel .
   *   }
   * }
   * </pre>
   *
   * where { ?uri -> CONST } is added to the exogenous mapping set.
   */
  public void testTicket653() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.yso.fi/onto/ysa/Y141994");
    final EmbergraphURI rdfTypeUri = f.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    final EmbergraphURI rdfsLabelUri = f.createURI("http://www.w3.org/2000/01/rdf-schema#label");

    final IV cTest = makeIV(cTestUri);
    final IV rdfType = makeIV(cTestUri);
    final IV rdfsLabel = makeIV(rdfsLabelUri);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri, rdfTypeUri, rdfsLabelUri};

    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);
      projection.addProjectionVar(new VarNode("uri"));
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(new AssignmentNode(new VarNode("uri"), new ConstantNode(cTest)));
      whereClause.addChild(
          new StatementPatternNode(
              new VarNode("uri"),
              new VarNode("p"),
              new VarNode("type"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      final JoinGroupNode jgn = new JoinGroupNode();
      jgn.addChild(
          new StatementPatternNode(
              new VarNode("uri"),
              new ConstantNode(rdfType),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      jgn.addChild(
          new StatementPatternNode(
              new VarNode("type"),
              new ConstantNode(rdfsLabel),
              new VarNode("typeLabel"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      jgn.setOptional(true);

      whereClause.addChild(jgn);
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);
      projection.addProjectionVar(new VarNode("uri"));
      projection.addProjectionVar(new VarNode("p"));
      projection.addProjectionVar(new VarNode("o"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      expected.setWhereClause(whereClause);

      whereClause.addChild(
          new StatementPatternNode(
              new ConstantNode(new Constant(Var.var("uri"), cTest)),
              new VarNode("p"),
              new VarNode("type"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));

      final JoinGroupNode jgn = new JoinGroupNode();
      jgn.addChild(
          new StatementPatternNode(
              new VarNode("uri"),
              new ConstantNode(rdfType),
              new VarNode("o"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      jgn.addChild(
          new StatementPatternNode(
              new VarNode("type"),
              new ConstantNode(rdfsLabel),
              new VarNode("typeLabel"),
              null /* c */,
              Scope.DEFAULT_CONTEXTS));
      jgn.setOptional(true);

      whereClause.addChild(jgn);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("uri")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }

  /*
   * Test inlining from BIND clause into property paths, inspired by the case reported in
   * https://jira.blazegraph.com/browse/BLZG-2042.
   *
   * <p>Given
   *
   * <pre>
   * SELECT ?s where { BIND(CONST AS ?o) . ?s <http://p1>/<http://p2> ?o }
   * </pre>
   *
   * , verify that the AST is rewritten as
   *
   * <pre>
   * SELECT ?s where { ?s <http://p1>/<http://p2> ?o }
   * </pre>
   *
   * where CONST is the binding for <code>?o</code> in the input solution and the exogeneous mapping
   * { ?p -> CONST } is added.
   */
  public void testTicketBLZG2042() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI cTestUri = f.createURI("http://www.test.com");
    final EmbergraphValue bv1 = store.getValueFactory().asValue(new URIImpl("http://p1"));
    final EmbergraphValue bv2 = store.getValueFactory().asValue(new URIImpl("http://p2"));

    final IV cTest = makeIV(cTestUri);
    final IV iv1 = makeIV(bv1);
    final IV iv2 = makeIV(bv2);

    final EmbergraphValue[] values = new EmbergraphValue[] {cTestUri};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final IBindingSet[] bsetsGiven = new IBindingSet[] {new ListBindingSet()};

    // The source AST.
    final QueryRoot given = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      given.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();
      given.setWhereClause(whereClause);

      whereClause.addChild(new AssignmentNode(new VarNode("o"), new ConstantNode(cTest)));

      final PathElt[] elements = new PathElt[2];
      elements[0] = new PathElt(new ConstantNode(iv1), false, PathMod.ZERO_OR_MORE);
      elements[1] = new PathElt(new ConstantNode(iv2), false, PathMod.ZERO_OR_MORE);

      final PathNode pn = new PathNode(new PathAlternative(new PathSequence(elements)));
      whereClause.addChild(new PropertyPathNode(new VarNode("s"), pn, new VarNode("o")));
    }

    // The expected AST after the rewrite.
    final QueryRoot expected = new QueryRoot(QueryType.SELECT);
    {
      final ProjectionNode projection = new ProjectionNode();
      expected.setProjection(projection);

      projection.addProjectionVar(new VarNode("s"));

      final JoinGroupNode whereClause = new JoinGroupNode();

      final PathElt[] elements = new PathElt[2];
      elements[0] = new PathElt(new ConstantNode(iv1), false, PathMod.ZERO_OR_MORE);
      elements[1] = new PathElt(new ConstantNode(iv2), false, PathMod.ZERO_OR_MORE);

      final PathNode pn = new PathNode(new PathAlternative(new PathSequence(elements)));
      whereClause.addChild(
          new PropertyPathNode(
              new VarNode("s"),
              pn,
              new ConstantNode(new Constant(Var.var("o"), cTest)) /* o inlined! */));

      expected.setWhereClause(whereClause);
    }

    final ASTStaticBindingsOptimizer rewriter = new ASTStaticBindingsOptimizer();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(given), store);

    final QueryNodeWithBindingSet res =
        rewriter.optimize(context, new QueryNodeWithBindingSet(given, bsetsGiven));

    // assert that the bindings set has been modified as expected
    IBindingSet[] resBs = res.getBindingSets();
    assertTrue(resBs.length == 1);
    assertTrue(resBs[0].size() == 1);
    assertTrue(resBs[0].get(Var.var("o")).equals(new Constant<>(cTest)));

    assertSameAST(expected, res.getQueryNode());
  }
}
