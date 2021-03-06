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
 * Created on June 15, 2015
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.OPTIONAL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import org.embergraph.rdf.sparql.ast.ArbitraryLengthPathNode;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.BindingsClause;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.FunctionNode;
import org.embergraph.rdf.sparql.ast.GlobalAnnotations;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.NamedSubqueryInclude;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.StaticAnalysis;
import org.embergraph.rdf.sparql.ast.SubqueryRoot;
import org.embergraph.rdf.sparql.ast.UnionNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpUtility;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.store.BDS;
import org.embergraph.service.fts.FTS;

/*
 * Test implementation of {@link IVariableBindingRequirements} interface.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 */
public class TestIVariableBindingRequirements extends AbstractOptimizerTestCase {

  public TestIVariableBindingRequirements() {}

  public TestIVariableBindingRequirements(String name) {
    super(name);
  }

  @Override
  IASTOptimizer newOptimizer() {

    /*
     * This test case is not optimizer specific, but we want to reuse the Helper factory methods
     * here and that's the (only) reason why we extend the AbstractOptimizerTestCase class.
     */
    throw new RuntimeException("Not optimizer specific, don't call this method.");
  }

  /** Test interface implementation for statement patterns nodes. */
  @SuppressWarnings("serial")
  public void testStatementPatternNode() {

    final StatementPatternNode spn1 =
        store.isQuads()
            ? (StatementPatternNode)
                new Helper() {
                  {
                    tmp = statementPatternNode(varNode(x), varNode(y), varNode(z), varNode(w));
                  }
                }.getTmp()
            : (StatementPatternNode)
                new Helper() {
                  {
                    tmp = statementPatternNode(varNode(x), varNode(y), varNode(z));
                  }
                }.getTmp();

    final StatementPatternNode spn2 =
        (StatementPatternNode)
            new Helper() {
              {
                tmp = statementPatternNode(constantNode(a), constantNode(b), constantNode(c));
              }
            }.getTmp();

    final StatementPatternNode spn3 =
        (StatementPatternNode)
            new Helper() {
              {
                tmp = statementPatternNode(varNode(x), constantNode(b), varNode(z));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBoundSpn123 = new HashSet<>();
    final Set<IVariable<?>> desiredBoundSpn1 =
        store.isQuads()
            ? new HashSet<IVariable<?>>() {
              {
                add(Var.var("x"));
                add(Var.var("y"));
                add(Var.var("z"));
                add(Var.var("w"));
              }
            }
            : new HashSet<IVariable<?>>() {
              {
                add(Var.var("x"));
                add(Var.var("y"));
                add(Var.var("z"));
              }
            };

    final Set<IVariable<?>> desiredBoundSpn2 = new HashSet<>();
    final Set<IVariable<?>> desiredBoundSpn3 =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("z"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBoundSpn123, spn1.getRequiredBound(sa));
    assertEquals(desiredBoundSpn1, spn1.getDesiredBound(sa));

    assertEquals(requiredBoundSpn123, spn2.getRequiredBound(sa));
    assertEquals(desiredBoundSpn2, spn2.getDesiredBound(sa));

    assertEquals(requiredBoundSpn123, spn3.getRequiredBound(sa));
    assertEquals(desiredBoundSpn3, spn3.getDesiredBound(sa));
  }

  /** Test interface implementation for assignment nodes. */
  @SuppressWarnings("serial")
  public void testAssignmentNode() {

    final AssignmentNode an1 =
        (AssignmentNode)
            new Helper() {
              {
                tmp = bind(constantNode(a), varNode(x));
              }
            }.getTmp();

    final AssignmentNode an2 =
        (AssignmentNode)
            new Helper() {
              {
                tmp = bind(varNode(y), varNode(x));
              }
            }.getTmp();

    final AssignmentNode an3 =
        (AssignmentNode)
            new Helper() {
              {
                tmp =
                    bind(
                        FunctionNode.AND(
                            FunctionNode.OR(
                                FunctionNode.EQ(varNode(x), varNode(y)), constantNode(z)),
                            varNode(z)),
                        varNode(w));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBoundAn1 = new HashSet<>();
    final Set<IVariable<?>> requiredBoundAn2 =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("y"));
          }
        };
    final Set<IVariable<?>> requiredBoundAn3 =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
            add(Var.var("z"));
          }
        };

    final Set<IVariable<?>> desiredBoundAn1 = new HashSet<>();
    final Set<IVariable<?>> desiredBoundAn2 = new HashSet<>();
    final Set<IVariable<?>> desiredBoundAn3 = new HashSet<>();

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBoundAn1, an1.getRequiredBound(sa));
    assertEquals(desiredBoundAn1, an1.getDesiredBound(sa));

    assertEquals(requiredBoundAn2, an2.getRequiredBound(sa));
    assertEquals(desiredBoundAn2, an2.getDesiredBound(sa));

    assertEquals(requiredBoundAn3, an3.getRequiredBound(sa));
    assertEquals(desiredBoundAn3, an3.getDesiredBound(sa));
  }

  /** Test interface implementation for assignment nodes. */
  @SuppressWarnings("serial")
  public void testBindingsClause() {

    final LinkedHashSet<IVariable<?>> declaredVars =
        new LinkedHashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
          }
        };

    final List<IBindingSet> bindingSets =
        new ArrayList<IBindingSet>() {
          {
            add(
                new ListBindingSet() {
                  {
                    set(Var.var("x"), new Constant<>(TermId.mockIV(VTE.URI)));
                    set(Var.var("y"), new Constant<>(TermId.mockIV(VTE.URI)));
                  }
                });
          }
        };

    final BindingsClause bc = new BindingsClause(declaredVars, bindingSets);

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    // bindings clauses requires no variables to be bound before, as they
    // introduce constant values only; nothing is forbidden and nothing
    // desired here...
    assertEquals(new HashSet<IVariable<?>>(), bc.getRequiredBound(sa));
    assertEquals(new HashSet<IVariable<?>>(), bc.getDesiredBound(sa));
  }

  /** Test interface implementation for UNION nodes. */
  @SuppressWarnings("serial")
  public void testUnion() {

    final UnionNode un1 =
        (UnionNode)
            new Helper() {
              {
                tmp =
                    unionNode(
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(c), varNode(y))),
                        joinGroupNode(bind(varNode(y), varNode(x))));
              }
            }.getTmp();

    final UnionNode un2 =
        (UnionNode)
            new Helper() {
              {
                tmp =
                    unionNode(
                        joinGroupNode(bind(varNode(y), varNode(x))),
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(c), varNode(y))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("y"));
          }
        };
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, un1.getRequiredBound(sa));
    assertEquals(desiredBound, un1.getDesiredBound(sa));

    assertEquals(requiredBound, un2.getRequiredBound(sa));
    assertEquals(desiredBound, un2.getDesiredBound(sa));
  }

  /** Test interface implementation for UNION nodes. */
  @SuppressWarnings("serial")
  public void testSubquery() {

    final SubqueryRoot subquery =
        (SubqueryRoot)
            new Helper() {
              {
                tmp =
                    selectSubQuery(
                        projection(bind(varNode(s), varNode(s))),
                        where(statementPatternNode(varNode(s), constantNode(c), varNode(o))));
              }
            }.getTmp();

    final QueryRoot query =
        (QueryRoot)
            new Helper() {
              {
                tmp = select(projection(wildcard()), where(subquery));
              }
            }.getTmp();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(query), store);
    final StaticAnalysis sa = new StaticAnalysis(query, context);

    final Set<IVariable<?>> requiredBound = new HashSet<>();
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("s"));
          }
        };

    assertEquals(requiredBound, subquery.getRequiredBound(sa));
    assertEquals(desiredBound, subquery.getDesiredBound(sa));
  }

  @SuppressWarnings("serial")
  public void testNamedSubquery() {

    final NamedSubqueryInclude nsi =
        (NamedSubqueryInclude)
            new Helper() {
              {
                tmp = namedSubQueryInclude("_set1");
              }
            }.getTmp();

    final QueryRoot query =
        (QueryRoot)
            new Helper() {
              {
                tmp =
                    select(
                        varNodes(x, y, y),
                        namedSubQuery(
                            "_set1",
                            varNode(x),
                            where(
                                statementPatternNode(
                                    varNode(x), constantNode(a), constantNode(b), 1))),
                        where(
                            nsi,
                            statementPatternNode(
                                varNode(x), constantNode(c), varNode(y), 1, OPTIONAL)));
              }
            }.getTmp();

    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(query), store);
    final StaticAnalysis sa = new StaticAnalysis(query, context);

    final Set<IVariable<?>> requiredBound = new HashSet<>();
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
          }
        };

    assertEquals(requiredBound, nsi.getRequiredBound(sa));
    assertEquals(desiredBound, nsi.getDesiredBound(sa));
  }

  /** Test interface implementation for ALP nodes. */
  @SuppressWarnings("serial")
  public void testSimpleALPNode() {

    final ArbitraryLengthPathNode alpNode =
        (ArbitraryLengthPathNode)
            new Helper() {
              {
                tmp =
                    arbitartyLengthPropertyPath(
                        varNode(x),
                        constantNode(b),
                        HelperFlag.ZERO_OR_MORE,
                        joinGroupNode(
                            statementPatternNode(leftVar(), constantNode(c), rightVar(), 26)));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBoundAlpNode = new HashSet<>();
    final Set<IVariable<?>> desiredBoundAlpNode =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBoundAlpNode, alpNode.getRequiredBound(sa));
    assertEquals(desiredBoundAlpNode, alpNode.getDesiredBound(sa));
  }

  @SuppressWarnings("serial")
  public void testComplexALPNode() {

    final ArbitraryLengthPathNode alpNode =
        (ArbitraryLengthPathNode)
            new Helper() {
              {
                tmp =
                    arbitartyLengthPropertyPath(
                        varNode(x),
                        constantNode(b),
                        HelperFlag.ZERO_OR_MORE,
                        joinGroupNode(
                            statementPatternNode(leftVar(), constantNode(c), varNode(y)),
                            statementPatternNode(varNode(y), constantNode(d), rightVar())));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBoundAlpNode = new HashSet<>();
    final Set<IVariable<?>> desiredBoundAlpNode =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBoundAlpNode, alpNode.getRequiredBound(sa));
    assertEquals(desiredBoundAlpNode, alpNode.getDesiredBound(sa));
  }

  /** Test interface implementation for FILTER nodes (simple). */
  @SuppressWarnings("serial")
  public void testFilter() {

    final FilterNode fn =
        (FilterNode)
            new Helper() {
              {
                tmp =
                    filter(
                        FunctionNode.AND(
                            varNode(w),
                            FunctionNode.OR(
                                FunctionNode.EQ(constantNode(w), varNode(y)),
                                FunctionNode.LT(varNode(x), varNode(z)))));
              }
            }.getTmp();

    final QueryRoot query = new QueryRoot(QueryType.SELECT);
    final JoinGroupNode jgn = new JoinGroupNode(fn);
    query.setWhereClause(jgn);

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("w"));
            add(Var.var("x"));
            add(Var.var("y"));
            add(Var.var("z"));
          }
        };
    final Set<IVariable<?>> desiredBound = new HashSet<>();

    /*
     * We need to convert the filter node. Usually (in the optimization chain) this is done through
     * the ASTSetValueExpressionOptimizer.
     */
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(query), store);
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), context);
    final GlobalAnnotations globals =
        new GlobalAnnotations(context.getLexiconNamespace(), context.getTimestamp());
    AST2BOpUtility.toVE(getBOpContext(), globals, fn.getValueExpressionNode());

    assertEquals(requiredBound, fn.getRequiredBound(sa));
    assertEquals(desiredBound, fn.getDesiredBound(sa));
  }

  /** Test interface implementation for FILTER EXISTS and FILTER NOT EXISTS nodes. */
  @SuppressWarnings("serial")
  public void testFilterExistsAndNotExists() {

    final FilterNode exists =
        (FilterNode)
            new Helper() {
              {
                tmp =
                    filter(
                        exists(
                            varNode(x),
                            joinGroupNode(
                                statementPatternNode(constantNode(a), constantNode(b), varNode(y)),
                                statementPatternNode(
                                    constantNode(a), constantNode(b), varNode(z)))));
              }
            }.getTmp();

    final FilterNode notExists =
        (FilterNode)
            new Helper() {
              {
                tmp =
                    filter(
                        exists(
                            varNode(x),
                            joinGroupNode(
                                statementPatternNode(constantNode(a), constantNode(b), varNode(y)),
                                statementPatternNode(
                                    constantNode(a), constantNode(b), varNode(z)))));
              }
            }.getTmp();

    final QueryRoot query = new QueryRoot(QueryType.SELECT);
    final JoinGroupNode jgn = new JoinGroupNode();
    jgn.addChild(exists);
    jgn.addChild(notExists);
    query.setWhereClause(jgn);

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
          }
        };
    final Set<IVariable<?>> desiredBound = new HashSet<>();

    /*
     * We need to convert the filter nodes. Usually (in the optimization chain) this is done through
     * the ASTSetValueExpressionOptimizer.
     */
    final AST2BOpContext context = new AST2BOpContext(new ASTContainer(query), store);
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), context);
    final GlobalAnnotations globals =
        new GlobalAnnotations(context.getLexiconNamespace(), context.getTimestamp());
    AST2BOpUtility.toVE(getBOpContext(), globals, exists.getValueExpressionNode());
    AST2BOpUtility.toVE(getBOpContext(), globals, notExists.getValueExpressionNode());

    assertEquals(requiredBound, exists.getRequiredBound(sa));
    assertEquals(desiredBound, exists.getDesiredBound(sa));

    assertEquals(requiredBound, notExists.getRequiredBound(sa));
    assertEquals(desiredBound, notExists.getDesiredBound(sa));
  }

  /** Test interface implementation for simple statement pattern only join groups. */
  @SuppressWarnings("serial")
  public void testSimpleJoinGroup() {

    final JoinGroupNode jgn =
        (JoinGroupNode)
            new Helper() {
              {
                tmp =
                    joinGroupNode(
                        statementPatternNode(varNode(x), constantNode(b), constantNode(c)),
                        statementPatternNode(constantNode(a), varNode(y), constantNode(c)),
                        statementPatternNode(
                            constantNode(a),
                            varNode(y),
                            varNode(z),
                            OPTIONAL /* doesn't change anything here */));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound = new HashSet<>();
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
            add(Var.var("z"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, jgn.getRequiredBound(sa));
    assertEquals(desiredBound, jgn.getDesiredBound(sa));
  }

  /** Test interface implementation for more complex join groups. */
  @SuppressWarnings("serial")
  public void testComplexJoinGroup01() {

    final JoinGroupNode jgn =
        (JoinGroupNode)
            new Helper() {
              {
                tmp =
                    joinGroupNode(
                        statementPatternNode(varNode(x), constantNode(b), constantNode(c)),
                        bind(varNode(y), varNode(x)),
                        selectSubQuery(
                            projection(bind(varNode(z), varNode(z))),
                            where(statementPatternNode(varNode(z), constantNode(c), varNode(o)))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("y"));
          }
        };
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("z"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, jgn.getRequiredBound(sa));
    assertEquals(desiredBound, jgn.getDesiredBound(sa));
  }

  /** Test interface implementation for more complex join groups. */
  @SuppressWarnings("serial")
  public void testComplexJoinGroup02() {

    final JoinGroupNode jgn =
        (JoinGroupNode)
            new Helper() {
              {
                tmp =
                    joinGroupNode(
                        statementPatternNode(varNode(y), constantNode(b), constantNode(c)),
                        bind(varNode(y), varNode(x)),
                        selectSubQuery(
                            projection(bind(varNode(z), varNode(z))),
                            where(statementPatternNode(varNode(z), constantNode(c), varNode(o)))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("y"));
          }
        };
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("y"));
            add(Var.var("z"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, jgn.getRequiredBound(sa));
    assertEquals(desiredBound, jgn.getDesiredBound(sa));
  }

  /*
   * Test interface implementation for SPARQL 1.1 SERVICE with constant specifying service endpoint.
   */
  @SuppressWarnings({"rawtypes", "serial"})
  public void testServiceSparql11Constant() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI serviceEndpoint = f.createURI("http://custom.endpoint");
    final IV serviceEndpointIV = makeIV(serviceEndpoint);

    final EmbergraphValue[] values = new EmbergraphValue[] {serviceEndpoint};
    store.getLexiconRelation().addTerms(values, values.length, false /* readOnly */);

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp =
                    service(
                        constantNode(serviceEndpointIV),
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(a), varNode(y))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound = new HashSet<>();
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, serviceNode.getRequiredBound(sa));
    assertEquals(desiredBound, serviceNode.getDesiredBound(sa));
  }

  /*
   * Test interface implementation for SPARQL 1.1 SERVICE with variable specifying service endpoint.
   */
  @SuppressWarnings("serial")
  public void testServiceSparql11Variable() {

    final ServiceNode serviceNode =
        (ServiceNode)
            new Helper() {
              {
                tmp =
                    service(
                        varNode(z),
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(a), varNode(y))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("z"));
          }
        };
    final Set<IVariable<?>> desiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("x"));
            add(Var.var("y"));
          }
        };

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, serviceNode.getRequiredBound(sa));
    assertEquals(desiredBound, serviceNode.getDesiredBound(sa));
  }

  /*
   * Test interface implementation for internal {@link BDS} service. Note that, at the time being,
   * the BDS service does not allow the injection of variables (in contrast to, e.g., the FTS
   * service). Therefore, this service imposes only a not bound constraint on the outgoing variable.
   */
  public void testServiceBDS() {

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
                                varNode("res"),
                                constantNode(makeIV(predSearch)),
                                constantNode("search")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predSearchTimeout)),
                                constantNode("1000")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predMatchExact)),
                                constantNode("false"))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound = new HashSet<>();
    final Set<IVariable<?>> desiredBound = new HashSet<>();

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, serviceNode.getRequiredBound(sa));
    assertEquals(desiredBound, serviceNode.getDesiredBound(sa));
  }

  /** Test interface implementation for internal {@link FTS} service. */
  @SuppressWarnings("serial")
  public void testServiceFTS() {

    final EmbergraphValueFactory f = store.getValueFactory();
    final EmbergraphURI ftsSearch = f.createURI(FTS.NAMESPACE + "search");
    final EmbergraphURI predSearch = f.createURI(FTS.SEARCH.toString());
    final EmbergraphURI predEndpoint = f.createURI(FTS.ENDPOINT.toString());
    final EmbergraphURI predEndpointType = f.createURI(FTS.ENDPOINT_TYPE.toString());
    final EmbergraphURI predParams = f.createURI(FTS.PARAMS.toString());
    final EmbergraphURI predScore = f.createURI(FTS.SCORE.toString());
    final EmbergraphURI predScoreField = f.createURI(FTS.SCORE_FIELD.toString());
    final EmbergraphURI predSearchField = f.createURI(FTS.SEARCH_FIELD.toString());
    final EmbergraphURI predSnippet = f.createURI(FTS.SNIPPET.toString());
    final EmbergraphURI predSnippetField = f.createURI(FTS.SNIPPET_FIELD.toString());
    final EmbergraphURI predTimeout = f.createURI(FTS.TIMEOUT.toString());
    final EmbergraphURI predSearchResultType = f.createURI(FTS.SEARCH_RESULT_TYPE.toString());

    final EmbergraphValue[] values =
        new EmbergraphValue[] {
          ftsSearch,
          predSearch,
          predEndpoint,
          predEndpointType,
          predParams,
          predScore,
          predScoreField,
          predSearchField,
          predSnippet,
          predSnippetField,
          predTimeout,
          predSearchResultType
        };
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
                                varNode("res"),
                                constantNode(makeIV(predSearch)),
                                varNode("search")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predEndpoint)),
                                varNode("endpoint")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predEndpointType)),
                                varNode("endpointType")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predParams)),
                                varNode("params")),
                            statementPatternNode(
                                varNode("res"), constantNode(makeIV(predScore)), varNode("score")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predScoreField)),
                                varNode("scoreField")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predSearchField)),
                                varNode("searchField")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predSnippet)),
                                varNode("snippet")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predSnippetField)),
                                varNode("snippetField")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predTimeout)),
                                varNode("timeout")),
                            statementPatternNode(
                                varNode("res"),
                                constantNode(makeIV(predSearchResultType)),
                                varNode("searchResultType"))));
              }
            }.getTmp();

    final Set<IVariable<?>> requiredBound =
        new HashSet<IVariable<?>>() {
          {
            add(Var.var("search"));
            add(Var.var("endpoint"));
            add(Var.var("endpointType"));
            add(Var.var("params"));
            add(Var.var("scoreField"));
            add(Var.var("searchField"));
            add(Var.var("snippetField"));
            add(Var.var("timeout"));
            add(Var.var("searchResultType"));
          }
        };
    final Set<IVariable<?>> desiredBound = new HashSet<>();

    // dummy sa object
    final StaticAnalysis sa = new StaticAnalysis(new QueryRoot(QueryType.SELECT), null);

    assertEquals(requiredBound, serviceNode.getRequiredBound(sa));
    assertEquals(desiredBound, serviceNode.getDesiredBound(sa));
  }
}
