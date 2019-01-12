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
package org.embergraph.rdf.spo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * Aggregates test suites into increasing dependency order.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll extends TestCase {

  /** */
  public TestAll() {}

  /** @param arg0 */
  public TestAll(String arg0) {

    super(arg0);
  }

  /*
   * Returns a test that will run each of the implementation specific test suites in turn.
   *
   * <p>Note: Much of the testing of the {@link SPORelation} is performed by the tests in the
   * org.embergraph.rdf.store package.
   *
   * @todo SPO (compareTo, equals, hashCode)
   */
  public static Test suite() {

    final TestSuite suite = new TestSuite("SPORelation");

    suite.addTestSuite(TestSPO.class);

    // test predicate impls.
    suite.addTestSuite(TestSPOPredicate.class);

    // test {inferred, explicit, axiom} enum class.
    suite.addTestSuite(TestStatementEnum.class);

    // @todo test IKeyOrder impl (comparators).
    suite.addTestSuite(TestSPOKeyOrder.class);

    // key/value coders
    suite.addTestSuite(TestSPOKeyCoders.class);
    suite.addTestSuite(TestSPOValueCoders.class);

    // key and value (de-)serialization of SPO tuples for B+Tree.
    suite.addTestSuite(TestSPOTupleSerializer.class);

    // test suite for the access path api.
    suite.addTestSuite(TestSPOAccessPath.class);

    // star joins
    //        suite.addTestSuite(TestSPOStarJoin.class);

    // test for shard split handler for the xxxC indices.
    suite.addTestSuite(TestXXXCShardSplitHandler.class);

    return suite;
  }
}
