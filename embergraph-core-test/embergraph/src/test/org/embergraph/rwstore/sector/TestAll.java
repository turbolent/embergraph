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

package org.embergraph.rwstore.sector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Runs all tests for all journal implementations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestAll.java 4069 2011-01-09 20:58:02Z thompsonbry $
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

    final TestSuite suite = new TestSuite("memory manager");

    suite.addTestSuite(TestMemoryManager.class);

    suite.addTestSuite(TestMemoryManager2.class);

    suite.addTestSuite(TestMemoryManagerStreams.class);

    suite.addTest(TestMemStore.suite());

    return suite;
  }
}
