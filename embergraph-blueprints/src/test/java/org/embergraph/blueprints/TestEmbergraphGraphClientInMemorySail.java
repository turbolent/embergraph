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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TestSuite;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.GraphTest;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.webapp.EmbergraphSailNSSWrapper;

/** Blueprints test suite for a client communicating with the server over the REST API. */
public class TestEmbergraphGraphClientInMemorySail extends AbstractTestEmbergraphGraph {

  private static final transient Logger log =
      Logger.getLogger(TestEmbergraphGraphClientInMemorySail.class);

  /** */
  public TestEmbergraphGraphClientInMemorySail() {}

  /** @param name */
  public TestEmbergraphGraphClientInMemorySail(String name) {
    super(name);
  }

  @Override
  protected GraphTest newEmbergraphGraphTest() {
    return new EmbergraphGraphTest();
  }
  /*
  //Currently there is not transaction support in the remote client.
   public void testTransactionalGraphTestSuite() throws Exception {
       final GraphTest test = newEmbergraphGraphTest();
       test.stopWatch();
       test.doTestSuite(new TransactionalGraphTestSuite(test));
       GraphTest.printTestPerformance("TransactionalGraphTestSuite",
               test.stopWatch());
   }
  */
  //    public void testAddVertexProperties() throws Exception {
  //        final EmbergraphGraphTest test = new EmbergraphGraphTest();
  //        test.stopWatch();
  //        final EmbergraphTestSuite testSuite = new EmbergraphTestSuite(test);
  //        try {
  //            testSuite.testVertexEquality();
  //        } finally {
  //            test.shutdown();
  //        }
  //
  //    }
  //
  //    private static class EmbergraphTestSuite extends TestSuite {
  //
  //        public EmbergraphTestSuite(final EmbergraphGraphTest graphTest) {
  //            super(graphTest);
  //        }
  //
  //        public void testVertexEquality() {
  //            Graph graph = graphTest.generateGraph();
  //
  //            if (!graph.getFeatures().ignoresSuppliedIds) {
  //                Vertex v = graph.addVertex(graphTest.convertId("1"));
  //                Vertex u = graph.getVertex(graphTest.convertId("1"));
  //                assertEquals(v, u);
  //            }
  //
  //            this.stopWatch();
  //            Vertex v = graph.addVertex(null);
  //            assertNotNull(v);
  //            Vertex u = graph.getVertex(v.getId());
  //            assertNotNull(u);
  //            assertEquals(v, u);
  //            printPerformance(graph.toString(), 1, "vertex added and retrieved",
  // this.stopWatch());
  //
  //            assertEquals(graph.getVertex(u.getId()), graph.getVertex(u.getId()));
  //            assertEquals(graph.getVertex(v.getId()), graph.getVertex(u.getId()));
  //            assertEquals(graph.getVertex(v.getId()), graph.getVertex(v.getId()));
  //
  //            graph.shutdown();
  //        }
  //    }

  private class EmbergraphGraphTest extends GraphTest {

    @Override
    public void doTestSuite(TestSuite testSuite) throws Exception {
      for (Method method : testSuite.getClass().getDeclaredMethods()) {
        if (method.getName().startsWith("test")) {
          log.warn("Testing " + method.getName() + "...");
          try {
            method.invoke(testSuite);
          } catch (Exception ex) {
            ex.getCause().printStackTrace();
            throw ex;
          } finally {
            shutdown();
          }
        }
      }
    }

    private Map<String, EmbergraphSailNSSWrapper> testSails =
        new LinkedHashMap<>();

    @Override
    public Graph generateGraph(final String key) {

      try {
        if (testSails.containsKey(key) == false) {
          final EmbergraphSail testSail = getSail();
          testSail.initialize();
          final EmbergraphSailNSSWrapper nss = new EmbergraphSailNSSWrapper(testSail);
          nss.init();
          testSails.put(key, nss);
        }

        final EmbergraphSailNSSWrapper nss = testSails.get(key);
        final EmbergraphGraph graph =
            new EmbergraphGraphClient(nss.m_repo.getRepositoryForDefaultNamespace());

        return graph;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public Graph generateGraph() {

      return generateGraph(null);
    }

    public void shutdown() {
      for (EmbergraphSailNSSWrapper wrapper : testSails.values()) {
        try {
          wrapper.shutdown();
          wrapper.getSail().__tearDownUnitTest();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
      testSails.clear();
    }
  }

  public static void main(final String[] args) {

    final String url = "http://localhost:9999/embergraph/sparql";

    final EmbergraphGraph graph = EmbergraphGraphFactory.connect(url);

    for (Vertex v : graph.getVertices()) {

      if (log.isInfoEnabled()) log.info(v);
    }

    for (Edge e : graph.getEdges()) {

      if (log.isInfoEnabled()) log.info(e);
    }
  }
}
