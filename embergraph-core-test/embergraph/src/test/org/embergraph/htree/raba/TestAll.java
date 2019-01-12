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
package org.embergraph.htree.raba;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
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

  /** Returns a test that will run each of the implementation specific test suites in turn. */
  public static Test suite() {

    final TestSuite suite = new TestSuite("Random Access Byte Arrays (rabas)");

    //        // note: this class is exercised by Leaf.
    //        suite.addTestSuite(TestMutableValuesRaba.class);

    // test mutable key buffer.
    suite.addTestSuite(TestMutableKeyBuffer.class);

    // test mutable values buffer
    suite.addTestSuite(TestMutableValueBuffer.class);

    //        // test key search routines on the key buffer implementations.
    //        suite.addTestSuite(TestKeyBufferSearch.class);
    //
    //        // compressed data implementations.
    //        suite.addTest(org.embergraph.btree.raba.codec.TestAll.suite());

    return suite;
  }
}
