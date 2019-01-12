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
 * Created on Apr 16, 2012
 */
package org.embergraph.bop.join;

import junit.framework.TestCase2;

/**
 * Test suite the {@link NestedLoopJoinOp}
 *
 * @author thompsonbry
 *     <p>FIXME Test suite! It would be best to refactor the existing hash join test suites, at
 *     least for the test setup.
 *     <p>FIXME Verify that we are handling SELECT and CONSTRAINTS as well as the operator specific
 *     annotations (NAME, SPARQL_CACHE). Look at the existing test suites for hash joins for
 *     examples that we can setup here.
 *     <p>FIXME Verify that the output of this join operator is order preserving (that could be done
 *     in a data driven unit test at the SPARQL layer for INCLUDE).
 */
public class TestNestedLoopJoinOp extends TestCase2 {

  public TestNestedLoopJoinOp() {}

  public TestNestedLoopJoinOp(String name) {
    super(name);
  }

  /**
   * Note: There are some tests at the data-driven level.
   *
   * @see org.embergraph.rdf.sparql.ast.eval.TestInclude
   *     <p>FIXME Implement test.
   */
  public void test_something() {
    log.error("implement test");
  }
}
