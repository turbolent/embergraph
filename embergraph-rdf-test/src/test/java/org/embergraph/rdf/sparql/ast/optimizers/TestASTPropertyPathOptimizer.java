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

import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.SUBGROUP_OF_ALP;
import static org.embergraph.rdf.sparql.ast.optimizers.AbstractOptimizerTestCase.HelperFlag.ZERO_OR_MORE;

/*
 * Test suite for {@link ASTUnionFiltersOptimizer}.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id: TestASTEmptyGroupOptimizer.java 5302 2011-10-07 14:28:03Z thompsonbry $
 */
public class TestASTPropertyPathOptimizer extends AbstractOptimizerTestCase {

  /** */
  public TestASTPropertyPathOptimizer() {}

  /** @param name */
  public TestASTPropertyPathOptimizer(String name) {
    super(name);
  }

  @Override
  IASTOptimizer newOptimizer() {
    return new ASTPropertyPathOptimizerInTest();
  }

  /*
   * This is (nearly) the same as {@link TestALPPinTrac773#testSimpleALPP()
   */
  public void test_basic_star() {
    new Helper() {
      {
        given =
            select(
                varNode(x),
                where(joinGroupNode(propertyPathNode(varNode(x), "c*", constantNode(b)))));

        expected =
            select(
                varNode(x),
                where(
                    joinGroupNode(
                        arbitartyLengthPropertyPath(
                            varNode(x),
                            constantNode(b),
                            ZERO_OR_MORE,
                            joinGroupNode(
                                statementPatternNode(leftVar(), constantNode(c), rightVar()),
                                SUBGROUP_OF_ALP)))));
      }
    }.test();
  }

  public void test_filter_star() {
    new Helper() {
      {
        given =
            select(
                varNode(x),
                where(
                    filter(
                        exists(
                            varNode(y),
                            joinGroupNode(propertyPathNode(varNode(x), "c*", constantNode(b)))))));

        expected =
            select(
                varNode(x),
                where(
                    filter(
                        exists(
                            varNode(y),
                            joinGroupNode(
                                arbitartyLengthPropertyPath(
                                    varNode(x),
                                    constantNode(b),
                                    ZERO_OR_MORE,
                                    joinGroupNode(
                                        statementPatternNode(
                                            leftVar(), constantNode(c), rightVar()),
                                        SUBGROUP_OF_ALP)))))));
      }
    }.test();
  }

  public void test_filter_or_star() {
    new Helper() {
      {
        given =
            select(
                varNode(x),
                where(
                    filter(
                        or(
                            constantNode(a),
                            exists(
                                varNode(y),
                                joinGroupNode(
                                    propertyPathNode(varNode(x), "c*", constantNode(b))))))));

        expected =
            select(
                varNode(x),
                where(
                    filter(
                        or(
                            constantNode(a),
                            exists(
                                varNode(y),
                                joinGroupNode(
                                    arbitartyLengthPropertyPath(
                                        varNode(x),
                                        constantNode(b),
                                        ZERO_OR_MORE,
                                        joinGroupNode(
                                            statementPatternNode(
                                                leftVar(), constantNode(c), rightVar()),
                                            SUBGROUP_OF_ALP))))))));
      }
    }.test();
  }
}
