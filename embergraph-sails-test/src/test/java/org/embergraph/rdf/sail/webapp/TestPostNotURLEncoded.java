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

/*
 * See trac 711 for discussion.
 *
 * @author jeremycarroll
 */
public class TestPostNotURLEncoded extends AbstractProtocolTest {

  public static Test suite() {
    return ProxySuiteHelper.suiteWhenStandalone(
        TestPostNotURLEncoded.class, "test.*", TestMode.quads, TestMode.sids, TestMode.triples);
  }

  public TestPostNotURLEncoded(String name) {
    super(name);
  }

  public void testSelectPostXML() throws IOException {
    setMethodisPost("application/sparql-query", AbstractProtocolTest.SELECT);
    assertTrue(serviceRequest().contains("</sparql>"));
    assertEquals(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_XML, getResponseContentType());
  }

  public void testSelectPostJSON() throws IOException {
    setAccept(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_JSON);
    setMethodisPost("application/sparql-query", AbstractProtocolTest.SELECT);
    assertTrue(serviceRequest().contains("results"));
    assertEquals(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_JSON, getResponseContentType());
  }

  public void testAskPostXML() throws IOException {
    setMethodisPost("application/sparql-query", AbstractProtocolTest.ASK);
    assertTrue(serviceRequest().contains("</sparql>"));
    assertEquals(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_XML, getResponseContentType());
  }

  public void testAskPostJSON() throws IOException {
    setAccept(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_JSON);
    setMethodisPost("application/sparql-query", AbstractProtocolTest.ASK);
    String response = serviceRequest("query", AbstractProtocolTest.ASK);
    assertTrue("Bad response: " + response, response.contains("boolean"));
    assertEquals(EmbergraphRDFServlet.MIME_SPARQL_RESULTS_JSON, getResponseContentType());
  }

  public void testUpdatePost() throws IOException {
    checkUpdate(false);
    setMethodisPost("application/sparql-update", update);
    serviceRequest();
    checkUpdate(true);
  }
}
