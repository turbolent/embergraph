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
 * Created on Feb 28, 2012
 */

package org.embergraph.rdf.rio;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * Test suite for RIO extensions but NOT the integration tests.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll_RIO extends TestCase {

  /** */
  public TestAll_RIO() {}

  /** @param name */
  public TestAll_RIO(String name) {
    super(name);
  }

  /** Returns a test that will run each of the implementation specific test suites in turn. */
  public static Test suite() {

    final TestSuite suite = new TestSuite("RIO Extensions");

    suite.addTest(org.embergraph.rdf.rio.ntriples.TestAll.suite());

    return suite;
  }
}
