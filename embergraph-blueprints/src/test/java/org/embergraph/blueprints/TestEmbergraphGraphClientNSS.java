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
package org.embergraph.blueprints;

/**
 * Test class for EmbergraphGraphClient against an embedded NSS provided for test suite coverage of
 * clients.
 *
 * @author beebs
 */
public class TestEmbergraphGraphClientNSS extends AbstractTestNSSBlueprintsClient {

  public TestEmbergraphGraphClientNSS() {
    super();
  }

  public void setUp() throws Exception {
    super.setUp();
  }

  /** This test validates that connecting to a getServiceURL() does not work. */
  public void testEmbergraphGraphConnectServiceURL() {

    final String testURL = getServiceURL() + "/";

    testPrint("Connecting to Remote Repository at " + testURL);

    EmbergraphGraph testGraph = new EmbergraphGraphClient(testURL);

    boolean hadException = false;

    try {
      testEmbergraphGraph(testGraph);
    } catch (Exception e) {
      hadException = true;
    }

    if (!hadException) fail("This test should not work.");
  }

  public void testEmbergraphGraphConnectSparqlEndpoint() {

    final String testURL = getServiceURL() + "/sparql";

    testPrint("Connecting to Remote Repository at " + testURL);

    EmbergraphGraph testGraph = new EmbergraphGraphClient(testURL);

    try {
      testEmbergraphGraph(testGraph);
    } catch (Exception e) {
      fail(e.toString());
    }
  }

  public void testEmbergraphGraphConnectSparqlEndpointWithNamespace() {

    final String testURL = getServiceURL() + "/namespace/" + super.getNamespace() + "/sparql";

    testPrint("Connecting to Remote Repository at " + testURL);

    EmbergraphGraph testGraph = new EmbergraphGraphClient(testURL);

    try {
      testEmbergraphGraph(testGraph);
    } catch (Exception e) {

      fail(e.toString());
    }
  }

  @Override
  protected EmbergraphGraph getNewGraph(String file) throws Exception {

    final String testURL = getServiceURL() + "/sparql";

    testPrint("Connecting to Remote Repository at " + testURL);

    return new EmbergraphGraphClient(testURL);
  }
}
