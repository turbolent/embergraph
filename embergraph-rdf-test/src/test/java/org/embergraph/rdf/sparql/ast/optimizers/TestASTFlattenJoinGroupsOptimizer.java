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

package org.embergraph.rdf.sparql.ast.optimizers;

import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.DEFAULT_CONTEXTS;
import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.DISTINCT;
import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.NAMED_CONTEXTS;
import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.ZERO_OR_ONE;

public class TestASTFlattenJoinGroupsOptimizer extends AbstractOptimizerTestCase {

  public TestASTFlattenJoinGroupsOptimizer(String name) {
    super(name);
  }

  public TestASTFlattenJoinGroupsOptimizer() {}

  @Override
  IASTOptimizer newOptimizer() {
    return new ASTFlattenJoinGroupsOptimizer();
  }

  public void testBasicFlattening() {

    new Helper() {
      {
        given =
            select(
                varNode(z),
                where(
                    joinGroupNode(
                        statementPatternNode(varNode(x), constantNode(c), constantNode(d)),
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(e), varNode(z))),
                        joinGroupNode(
                            statementPatternNode(varNode(x), constantNode(f), varNode(z))))),
                DISTINCT);

        expected =
            select(
                varNode(z),
                where(
                    statementPatternNode(varNode(x), constantNode(c), constantNode(d)),
                    statementPatternNode(varNode(x), constantNode(e), varNode(z)),
                    statementPatternNode(varNode(x), constantNode(f), varNode(z))),
                DISTINCT);
      }
    }.test();
  }

  public void testContextChange() {

    new Helper() {
      {
        given =
            select(
                varNode(z),
                where(
                    joinGroupNode(
                        statementPatternNode(varNode(x), constantNode(c), constantNode(d)),
                        joinGroupNode(
                            varNode(w),
                            statementPatternNode(
                                varNode(x),
                                constantNode(e),
                                varNode(z),
                                varNode(w),
                                NAMED_CONTEXTS)),
                        joinGroupNode(
                            statementPatternNode(
                                varNode(x), constantNode(f), varNode(z), DEFAULT_CONTEXTS)))),
                DISTINCT);

        expected =
            select(
                varNode(z),
                where(
                    statementPatternNode(varNode(x), constantNode(c), constantNode(d)),
                    statementPatternNode(
                        varNode(x), constantNode(e), varNode(z), varNode(w), NAMED_CONTEXTS),
                    statementPatternNode(
                        varNode(x), constantNode(f), varNode(z), DEFAULT_CONTEXTS)),
                DISTINCT);
      }
    }.test();
  }

  public void testSingleALPP() {

    new Helper() {
      {
        given =
            select(
                varNode(z),
                where(
                    joinGroupNode(
                        arbitartyLengthPropertyPath(
                            varNode(x),
                            varNode(y),
                            ZERO_OR_ONE,
                            joinGroupNode(
                                statementPatternNode(leftVar(), constantNode(c), rightVar()))))));

        varCount = 0;

        expected =
            select(
                varNode(z),
                where(
                    arbitartyLengthPropertyPath(
                        varNode(x),
                        varNode(y),
                        ZERO_OR_ONE,
                        joinGroupNode(
                            statementPatternNode(leftVar(), constantNode(c), rightVar())))));
      }
    }.test();
  }
}
