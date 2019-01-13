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
 * Created on June 20, 2015
 */
package org.embergraph.rdf.sparql.ast.optimizers;

import cutthecrap.utils.striterators.Filter;
import cutthecrap.utils.striterators.IStriterator;
import cutthecrap.utils.striterators.Striterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.BindingsClause;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.sparql.ast.HavingNode;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.IValueExpressionNode;
import org.embergraph.rdf.sparql.ast.IValueExpressionNodeContainer;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.PropertyPathNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.RangeNode;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;
import org.embergraph.rdf.sparql.ast.SubqueryRoot;
import org.embergraph.rdf.sparql.ast.UnionNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpUtility;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.store.BDS;
import org.embergraph.service.fts.FTS;

/*
 * Test case helper class in the style of {@link Helper} exposing additional methods for
 * constructing ASTs with different properties.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 */
@SuppressWarnings({"rawtypes"})
public abstract class AbstractOptimizerTestCaseWithUtilityMethods
    extends AbstractOptimizerTestCase {

  public AbstractOptimizerTestCaseWithUtilityMethods() {}

  public AbstractOptimizerTestCaseWithUtilityMethods(String name) {
    super(name);
  }

  /** Returns a fresh statement pattern with the specified variable bound. */
  StatementPatternNode stmtPatternWithVar(final String varName) {
    return stmtPatternWithVar(varName, false);
  }

  /** Returns a fresh statement pattern with the specified variables bound. */
  StatementPatternNode stmtPatternWithVars(final String varName1, final String varName2) {

    final StatementPatternNode spn =
        (StatementPatternNode)
            new Helper() {
              {
                tmp = statementPatternNode(varNode(varName1), constantNode(a), varNode(varName2));
              }
            }.getTmp();

    return spn;
  }

  /** Returns a fresh statement pattern with the specified variables bound. */
  StatementPatternNode stmtPatternWithVarsOptional(final String varName1, final String varName2) {

    final StatementPatternNode spn = stmtPatternWithVars(varName1, varName2);
    spn.setOptional(true);

    return spn;
  }

  /** Returns a fresh optional statement pattern with the specified variable bound. */
  StatementPatternNode stmtPatternWithVarOptional(final String varName) {
    return stmtPatternWithVar(varName, true);
  }

  /*
   * Returns a fresh statement pattern with the specified variable bound that is either optional or
   * not, as specified by the second parameter.
   */
  StatementPatternNode stmtPatternWithVar(final String varName, final boolean optional) {

    final StatementPatternNode spn =
        (StatementPatternNode)
            new Helper() {
              {
                tmp = statementPatternNode(varNode(varName), constantNode(a), constantNode(b));
              }
            }.getTmp();
    spn.setOptional(optional);

    return spn;
  }

  /*
   * For each variable, a fresh statement pattern with the variable is created and the list of
   * statement patterns is returned.
   */
  StatementPatternNode[] stmtPatternsWithVars(final String... varNames) {

    final StatementPatternNode[] statementPatterns = new StatementPatternNode[varNames.length];
    for (int i = 0; i < varNames.length; i++) {
      statementPatterns[i] = stmtPatternWithVar(varNames[i], false);
    }

    return statementPatterns;
  }

  /*
   * Returns a fresh filter node with the specified variable in their {@link ValueExpressionNode}.
   * The filter is not of type EXISTS or NOT EXISTS.
   */
  FilterNode filterWithVar(final String varName) {

    final FilterNode fn =
        (FilterNode)
            new Helper() {
              {
                tmp = filter(FunctionNode.EQ(varNode(varName), constantNode(a)));
              }
            }.getTmp();

    return (FilterNode) resolveVEs(fn);
  }

  /*
   * Returns a fresh filter node with the specified variables in their {@link ValueExpressionNode}.
   * The filter is not of type EXISTS or NOT EXISTS.
   */
  FilterNode filterWithVars(final String varName1, final String varName2) {

    final FilterNode fn =
        (FilterNode)
            new Helper() {
              {
                tmp = filter(FunctionNode.NE(varNode(varName1), varNode(varName2)));
              }
            }.getTmp();

    return (FilterNode) resolveVEs(fn);
  }

  /** Returns a fresh FILTER NOT EXISTS node with the specified variables in their body. */
  FilterNode filterExistsWithVars(final String anonymousVar, final String... varNames) {

    final StatementPatternNode[] statementPatterns = stmtPatternsWithVars(varNames);

    final VarNode askVar = new VarNode(anonymousVar);
    askVar.setAnonymous(true);

    final FilterNode fn =
        (FilterNode)
            new Helper() {
              {
                tmp = filter(notExists(askVar, joinGroupNode((Object[]) statementPatterns)));
              }
            }.getTmp();

    return (FilterNode) resolveVEs(fn);
  }

  /** Returns a fresh FILTER EXISTS node with the specified variables in their body. */
  FilterNode filterNotExistsWithVars(final String anonymousVar, final String... varNames) {

    final StatementPatternNode[] statementPatterns = stmtPatternsWithVars(varNames);

    final VarNode askVar = new VarNode(anonymousVar);
    askVar.setAnonymous(true);

    final FilterNode fn =
        (FilterNode)
            new Helper() {
              {
                tmp = filter(exists(askVar, joinGroupNode((Object[]) statementPatterns)));
              }
            }.getTmp();

    return (FilterNode) resolveVEs(fn);
  }

  SubqueryRoot filterExistsOrNotExistsSubqueryWithVars(
      final String anonymousVar, final String... varNames) {

    final SubqueryRoot sqr = new SubqueryRoot(QueryType.ASK);

    /** Projection contains all variables */
    final ProjectionNode projection = new ProjectionNode();
    sqr.setProjection(projection);
    projection.addProjectionExpression(
        new AssignmentNode(new VarNode(anonymousVar), new VarNode(anonymousVar)));
    for (final String varName : varNames) {
      projection.addProjectionExpression(
          new AssignmentNode(new VarNode(varName), new VarNode(varName)));
    }

    /** Set the anonymous variable */
    final VarNode askVar = new VarNode(anonymousVar);
    askVar.setAnonymous(true);
    sqr.setAskVar(askVar.getValueExpression());

    /** Set the WHERE clause */
    sqr.setWhereClause(joinGroupWithVars(varNames));

    /** And the MODE */
    sqr.setFilterExistsMode(QueryHints.DEFAULT_FILTER_EXISTS);

    return sqr;
  }

  /** Returns a {@link JoinGroupNode} binding the specified vars. */
  JoinGroupNode joinGroupWithVars(final String... varNames) {

    final StatementPatternNode[] statementPatterns = stmtPatternsWithVars(varNames);

    final JoinGroupNode jgn =
        (JoinGroupNode)
            new Helper() {
              {
                tmp = joinGroupNode((Object[]) statementPatterns);
              }
            }.getTmp();

    return jgn;
  }

  /*
   * Returns a {@link UnionNode} binding varNames in their body, where left variable is the (one and
   * only) in the left pattern, and rights are the variables in the right union pattern.
   */
  UnionNode unionWithVars(final String varLeft, final String... varsRight) {

    final StatementPatternNode[] statementPatternsRight = stmtPatternsWithVars(varsRight);

    final UnionNode unionNode =
        (UnionNode)
            new Helper() {
              {
                tmp =
                    unionNode(
                        joinGroupNode(stmtPatternWithVar(varLeft, false)),
                        joinGroupNode((Object[]) statementPatternsRight));
              }
            }.getTmp();

    return unionNode;
  }

  /** Returns an {@link AssignmentNode} binding varNames in their body to the specified var. */
  AssignmentNode assignmentWithConst(final String boundVar) {

    final AssignmentNode an =
        (AssignmentNode)
            new Helper() {
              {
                tmp = bind(constantNode(a), varNode(boundVar));
              }
            }.getTmp();

    return an;
  }

  /** Returns an {@link AssignmentNode} binding varNames in their body to the specified var. */
  AssignmentNode assignmentWithVar(final String boundVar, final String usedVar) {

    final AssignmentNode an =
        (AssignmentNode)
            new Helper() {
              {
                tmp = bind(varNode(usedVar), varNode(boundVar));
              }
            }.getTmp();

    return an;
  }

  /*
   * Returns an {@link AssignmentNode} binding the vars to themselves (useful for projection nodes).
   */
  AssignmentNode[] assignmentWithVars(final String... varNames) {

    AssignmentNode[] assignmentNodes = new AssignmentNode[varNames.length];

    for (int i = 0; i < varNames.length; i++) {
      assignmentNodes[i] = assignmentWithVar(varNames[i], varNames[i]);
    }

    return assignmentNodes;
  }

  /** Returns a {@link BindingsClause} binding the specified vars. */
  BindingsClause bindingsClauseWithVars(final String... varNames) {

    final LinkedHashSet<IVariable<?>> declaredVars = new LinkedHashSet<>();
    for (String varName : varNames) {
      declaredVars.add(Var.var(varName));
    }

    final List<IBindingSet> bindingSets = new ArrayList<>();
    IBindingSet bs = new ListBindingSet();
    for (String varName : varNames) {
      bs.set(Var.var(varName), new Constant<>(TermId.mockIV(VTE.URI)));
    }

    return new BindingsClause(declaredVars, bindingSets);
  }

  /** Returns a {@link SubqueryRoot} projecting the specified vars. */
  SubqueryRoot subqueryWithVars(final String... varNames) {

    final AssignmentNode[] assignments = assignmentWithVars(varNames);
    final StatementPatternNode[] statementPatterns = stmtPatternsWithVars(varNames);

    final SubqueryRoot subquery =
        (SubqueryRoot)
            new Helper() {
              {
                tmp =
                    selectSubQuery(
                        projection(assignments),
                        where(joinGroupNode((Object[]) statementPatterns)));
              }
            }.getTmp();

    return subquery;
  }

  /*
   * Returns an {@link ArbitraryLengthPathNode} binding varNames in their body, where the first
   * parameter specified the {@link ArbitraryLengthPathNode}s internal variable.
   */
  PropertyPathNode alpNodeWithVars(final String varName1, final String varName2) {

    final PropertyPathNode alpNode =
        (PropertyPathNode)
            new Helper() {
              {
                tmp = propertyPathNode(varNode(varName1), "c*", varNode(varName2));
              }
            }.getTmp();

    return alpNode;
  }

  /*
   * Returns a SPARQL 1.1 {@link ServiceNode} using the specified vars in its body, with a constant
   * endpoint.
   */
  ServiceNode serviceSparql11WithConstant(final String... varNames) {

    final JoinGroupNode jgn = joinGroupWithVars(varNames);

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI serviceEndpoint = f.createURI("http://custom.endpoint");
    final IV serviceEndpointIV = makeIV(serviceEndpoint);

    final EmbergraphValue[] values = new EmbergraphValue[] {serviceEndpoint};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp = service(constantNode(serviceEndpointIV), jgn);
              }
            }.getTmp();

    return serviceNode;
  }

  /*
   * Returns a SPARQL 1.1 {@link ServiceNode} using the specified vars in its body, with an endpoint
   * specified through endpointVar.
   */
  ServiceNode serviceSparql11WithVariable(final String endpointVar, final String... varNames) {

    final JoinGroupNode jgn = joinGroupWithVars(varNames);

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp = service(varNode(endpointVar), jgn);
              }
            }.getTmp();

    return serviceNode;
  }

  /** Returns a BDS type {@link ServiceNode} using the specified vars in its body. */
  ServiceNode serviceBDSWithVariable(final String inputVar) {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI bdsSearch = f.createURI(BDS.NAMESPACE + "search");
    final EmbergraphURI predSearch = f.createURI(BDS.SEARCH.toString());
    final EmbergraphURI predSearchTimeout = f.createURI(BDS.SEARCH_TIMEOUT.toString());
    final EmbergraphURI predMatchExact = f.createURI(BDS.MATCH_EXACT.toString());

    final EmbergraphValue[] values =
        new EmbergraphValue[] {bdsSearch, predSearch, predSearchTimeout, predMatchExact};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp =
                    service(
                        constantNode(makeIV(bdsSearch)),
                        joinGroupNode(
                            statementPatternNode(
                                varNode(inputVar),
                                constantNode(makeIV(predSearch)),
                                constantNode("search")),
                            statementPatternNode(
                                varNode(inputVar),
                                constantNode(makeIV(predSearchTimeout)),
                                constantNode("1000")),
                            statementPatternNode(
                                varNode(inputVar),
                                constantNode(makeIV(predMatchExact)),
                                constantNode("false"))));
              }
            }.getTmp();

    return serviceNode;
  }

  /** Return a FTS type {@link ServiceNode} using the specified variables. */
  ServiceNode serviceFTSWithVariable(
      // output variables:
      final String resultVar,
      final String scoreVar,
      final String snippetVar,
      // input variables:
      final String searchVar,
      final String endpointVar,
      final String paramsVar) {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI ftsSearch = f.createURI(FTS.NAMESPACE + "search");
    final EmbergraphURI predSearch = f.createURI(FTS.SEARCH.toString());
    final EmbergraphURI predEndpoint = f.createURI(FTS.ENDPOINT.toString());
    final EmbergraphURI predParams = f.createURI(FTS.PARAMS.toString());

    final EmbergraphValue[] values =
        new EmbergraphValue[] {ftsSearch, predSearch, predEndpoint, predParams};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp =
                    service(
                        constantNode(makeIV(ftsSearch)),
                        joinGroupNode(
                            statementPatternNode(
                                varNode(resultVar),
                                constantNode(makeIV(predSearch)),
                                varNode(searchVar)),
                            statementPatternNode(
                                varNode(resultVar),
                                constantNode(makeIV(predEndpoint)),
                                varNode(endpointVar)),
                            statementPatternNode(
                                varNode(resultVar),
                                constantNode(makeIV(predParams)),
                                varNode(paramsVar))));
              }
            }.getTmp();

    return serviceNode;
  }

  public Set<IVariable<?>> varSet(String... varNames) {
    final Set<IVariable<?>> varSet = new HashSet<>();
    for (String varName : varNames) {
      varSet.add(Var.var(varName));
    }
    return varSet;
  }

  /** Properly resolves the value expressions in the {@link IGroupMemberNode}. */
  IGroupMemberNode resolveVEs(final IGroupMemberNode groupNode) {

    final QueryRoot query = new QueryRoot(QueryType.SELECT);
    final JoinGroupNode jgn = new JoinGroupNode(groupNode);
    query.setWhereClause(jgn);

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(query), store);
    final GlobalAnnotations globals =
        new GlobalAnnotations(context.getLexiconNamespace(), context.getTimestamp());

    /*
     * Visit nodes that require modification.
     */
    final IStriterator it =
        new Striterator(BOpUtility.preOrderIteratorWithAnnotations(groupNode))
            .addFilter(
                new Filter() {

                  private static final long serialVersionUID = 1L;

                  @Override
                  public boolean isValid(Object obj) {

                    if (obj instanceof IValueExpressionNodeContainer) return true;
                    if (obj instanceof HavingNode) return true;
                    return obj instanceof StatementPatternNode;
                  }
                });

    while (it.hasNext()) {

      final Object op = it.next();

      if (op instanceof IValueExpressionNodeContainer) {

        // AssignmentNode, FilterNode, OrderByExpr
        AST2BOpUtility.toVE(
            getBOpContext(),
            globals,
            ((IValueExpressionNodeContainer) op).getValueExpressionNode());

      } else if (op instanceof HavingNode) {

        final HavingNode havingNode = (HavingNode) op;

        for (IValueExpressionNode node : havingNode) {

          AST2BOpUtility.toVE(getBOpContext(), globals, node);
        }

      } else if (op instanceof StatementPatternNode) {

        final StatementPatternNode sp = (StatementPatternNode) op;

        final RangeNode range = sp.getRange();

        if (range != null) {

          if (range.from() != null) AST2BOpUtility.toVE(getBOpContext(), globals, range.from());

          if (range.to() != null) AST2BOpUtility.toVE(getBOpContext(), globals, range.to());
        }
      }
    }

    return groupNode;
  }

  /*
   * Creates a {@link StaticAnalysis} object for the nodes by setting up a SELECT query with a
   * single {@link JoinGroupNode} comprising the nodes in its body to retrieve the {@link
   * StaticAnalysis} object.
   */
  StaticAnalysis statisAnalysisForNodes(List<IGroupMemberNode> nodes) {

    final IGroupMemberNode[] nodesArr = nodes.toArray(new IGroupMemberNode[0]);
    final JoinGroupNode jgn =
        (JoinGroupNode)
            new Helper() {
              {
                tmp = joinGroupNode((Object[]) nodesArr);
              }
            }.getTmp();

    final QueryRoot queryRoot = new QueryRoot(QueryType.SELECT);
    queryRoot.setWhereClause(jgn);

    return new StaticAnalysis(queryRoot, new AST2BOpContext(new ASTContainer(queryRoot), store));
  }
}
