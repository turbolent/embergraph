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
 * Created on Oct 14, 2006
 */

package org.embergraph.service.ndx;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.embergraph.service.TestBasicIndexStuff;
import org.embergraph.service.TestEmbeddedClient;
import org.embergraph.service.TestRangeQuery;

/**
 * Aggregates test suites in increasing dependency order.
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

    final TestSuite suite = new TestSuite("scale-out indices");

    // test splitting client requests against index partition locators.
    suite.addTestSuite(TestSplitter.class);

    // client basics, including static partitioning of indices.
    suite.addTestSuite(TestEmbeddedClient.class);

    // test basic index operations.
    suite.addTestSuite(TestBasicIndexStuff.class);

    // test range iterators (within and across index partitions).
    suite.addTestSuite(TestRangeQuery.class);

    // unit tests for the streaming index write API.
    suite.addTest(org.embergraph.service.ndx.pipeline.TestAll.suite());

    return suite;
  }
}
