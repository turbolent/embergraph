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
 * https://jira.blazegraph.com/browse/BLZG-1862 and https://jira.blazegraph.com/browse/BLZG-856 take
 * this optimizer into an infinite loop.
 *
 * <p>BLZG-856 SELECT * WITH { SELECT * { INCLUDE %sub_record }} AS %foo { INCLUDE %foo } and
 * BLZG-1862 select * { select ?product { INCLUDE %solutionSet1 } OFFSET 1 LIMIT 1 }
 *
 * <p>BLZG-1862 is:
 *
 * <p>from WITH {
 *
 * <p>QueryType: SELECT
 *
 * <p>SELECT ( VarNode(p) AS VarNode(p) )
 *
 * <p>JoinGroupNode {
 *
 * <p>INCLUDE %sx
 *
 * <p>}
 *
 * <p>slice(offset=1,limit=1)
 *
 * <p>} AS -subSelect-1
 *
 * <p>QueryType: SELECT
 *
 * <p>includeInferred=true
 *
 * <p>SELECT ( VarNode(p) AS VarNode(p) )
 *
 * <p>JoinGroupNode {
 *
 * <p>INCLUDE -subSelect-1
 *
 * <p>}
 *
 * <p>we should get:
 *
 * <p>WITH {
 *
 * <p>QueryType: SELECT
 *
 * <p>SELECT ( VarNode(p) AS VarNode(p) )
 *
 * <p>JoinGroupNode {
 *
 * <p>INCLUDE %sx
 *
 * <p>}
 *
 * <p>slice(offset=1,limit=1)
 *
 * <p>} AS -subSelect-1 JOIN ON () DEPENDS ON (%sx)
 *
 * <p>QueryType: SELECT
 *
 * <p>includeInferred=true
 *
 * <p>SELECT ( VarNode(p) AS VarNode(p) )
 *
 * <p>JoinGroupNode {
 *
 * <p>INCLUDE -subSelect-1 JOIN ON ()
 *
 * <p>}
 */
package org.embergraph.rdf.sparql.ast.optimizers;

import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.ISolutionSetStats;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.SolutionSetStats;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

public class TestASTNamedSubqueryOptimizer2 extends AbstractOptimizerTestCaseWithUtilityMethods {

  /*
   * Modify the inherited Helper class to have a named solution set.
   *
   * @author jeremycarroll
   */
  private class Helper extends AbstractOptimizerTestCase.Helper {
    IBindingSet bs = new ListBindingSet();

    {
      bs.set(toValueExpression(varNode(p)), toValueExpression((ConstantNode) constantNode(a)));
    }

    @SuppressWarnings("deprecation")
    SolutionSetStats sss = new SolutionSetStats(new IBindingSet[] {bs, bs});

    AST2BOpContext getAST2BOpContext(QueryRoot given) {
      return new AST2BOpContext(new ASTContainer(given), store) {
        @Override
        public ISolutionSetStats getSolutionSetStats(final String localName) {
          if ("solutionSet".equals(localName)) {
            return sss;
          }
          return super.getSolutionSetStats(localName);
        }
      };
    }
  }

  @Override
  IASTOptimizer newOptimizer() {
    return new ASTNamedSubqueryOptimizer();
  }
  /** See https://jira.blazegraph.com/browse/BLZG-856 */
  public void testNamedSolutionSetInsideNamedSubQuery() {
    new Helper() {
      {
        given =
            select(
                varNodes(p),
                namedSubQuery(
                    "foo", varNode(p), where(joinGroupNode(namedSubQueryInclude("solutionSet")))),
                where(joinGroupNode(namedSubQueryInclude("foo"))));
        expected =
            select(
                varNodes(p),
                namedSubQuery(
                    "foo",
                    varNode(p),
                    where(joinGroupNode(namedSubQueryInclude("solutionSet"))),
                    joinOn(varNodes()),
                    dependsOn("solutionSet")),
                where(joinGroupNode(namedSubQueryInclude("foo", joinOn(varNodes())))));
      }
    }.test();
  }
  /** See https://jira.blazegraph.com/browse/BLZG-1862 */
  public void testNamedSolutionSetLimit() {
    new Helper() {
      {
        given =
            select(
                varNodes(p),
                namedSubQuery(
                    "subQuery",
                    varNode(p),
                    where(joinGroupNode(namedSubQueryInclude("solutionSet"))),
                    slice(1, 1)),
                where(joinGroupNode(namedSubQueryInclude("subQuery"))));
        expected =
            select(
                varNodes(p),
                namedSubQuery(
                    "subQuery",
                    varNode(p),
                    where(joinGroupNode(namedSubQueryInclude("solutionSet"))),
                    slice(1, 1),
                    joinOn(varNodes()),
                    dependsOn("solutionSet")),
                where(joinGroupNode(namedSubQueryInclude("subQuery", joinOn(varNodes())))));
      }
    }.test();
  }
}
