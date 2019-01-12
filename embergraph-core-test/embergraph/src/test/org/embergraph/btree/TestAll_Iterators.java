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
 * Created on Jan 31, 2009
 */

package org.embergraph.btree;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * Aggregates the unit tests for the {@link IndexSegment} and its related classes, all of which are
 * in the same package as the {@link BTree}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll_Iterators extends TestCase {

  public TestAll_Iterators() {}

  public TestAll_Iterators(String arg0) {
    super(arg0);
  }

  /** Returns a test that will run each of the implementation specific test suites in turn. */
  public static Test suite() {

    final TestSuite suite = new TestSuite("Iterators");

    // test leaf traversal cursors.
    suite.addTestSuite(TestBTreeLeafCursors.class);

    // test suite for B+Tree iterators (vs cursors).
    suite.addTestSuite(TestIterators.class);

    // test cursors for a read-only B+Tree.
    suite.addTestSuite(TestReadOnlyBTreeCursors.class);

    // test cursors for a read-write B+Tree.
    suite.addTestSuite(TestMutableBTreeCursors.class);

    // test stackable tuple filters
    suite.addTest(org.embergraph.btree.filter.TestAll.suite());

    // test chunked iterators.
    suite.addTestSuite(TestChunkedIterators.class);

    return suite;
  }
}
