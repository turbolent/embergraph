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

package org.embergraph.rdf.sail.webapp;

import java.io.IOException;
import junit.framework.Test;

/**
 * This class tests whether its superclass is working (at least a little) The superclass provides
 * capability to check the request/response protocol, without actually starting a server.
 *
 * @author jeremycarroll
 */
public class ExampleProtocolTest extends AbstractProtocolTest {

  public ExampleProtocolTest(String name) {
    super(name);
  }

  public void test101() throws IOException {
    assertTrue(serviceRequest("query", "SELECT ( true AS ?t ) {}").contains("</sparql>"));
    assertEquals("application/sparql-results+xml", getResponseContentType());
  }

  public static Test suite() {
    return ProxySuiteHelper.suiteWhenStandalone(
        ExampleProtocolTest.class, "test.*", TestMode.quads, TestMode.sids, TestMode.triples);
  }
}
