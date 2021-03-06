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
import com.tinkerpop.blueprints.Vertex;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.embergraph.EmbergraphStatics;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.sail.CreateKBTask;
import org.embergraph.rdf.sail.DestroyKBTask;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.webapp.ConfigParams;
import org.embergraph.rdf.sail.webapp.NanoSparqlServer;
import org.embergraph.rdf.sail.webapp.client.HttpClientConfigurator;
import org.embergraph.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.embergraph.rdf.task.AbstractApiTask;
import org.embergraph.util.config.NicUtil;

/*
 * Unit tests for the {@link NanoSparqlServer} with a focus on the ability to override the init
 * parameters, the default http port, etc. This test suite is written without the proxy mechanisms
 * to make this easier to debug.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public abstract class AbstractTestNSSBlueprintsClient extends AbstractTestEmbergraphGraphFactory {

  private static final transient Logger log =
      Logger.getLogger(AbstractTestNSSBlueprintsClient.class);

  private Server m_fixture;
  protected String namespace;
  protected Journal m_indexManager;
  private String m_rootURL;
  private String m_serviceURL;
  private RemoteRepositoryManager m_repo;
  private HttpClient m_client;
  private int m_port;

  public String getServiceURL() {
    return m_serviceURL;
  }

  public String getNamespace() {
    return namespace;
  }

  public int getPort() {
    return m_port;
  }

  protected Properties getTripleStoreProperties() {
    final Properties tripleStoreProperties = new Properties();
    {
      tripleStoreProperties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

      tripleStoreProperties.setProperty(EmbergraphSail.Options.TRIPLES_MODE, "true");
    }

    return tripleStoreProperties;
  }

  @Override
  public void setUp() throws Exception {

    if (log.isInfoEnabled()) log.info("Setting up test:" + getName());

    final Properties journalProperties = new Properties();
    {
      journalProperties.setProperty(Journal.Options.BUFFER_MODE, BufferMode.MemStore.name());
    }

    // guaranteed distinct namespace for the KB instance.
    namespace = getName() + UUID.randomUUID();

    m_indexManager = new Journal(journalProperties);

    // Properties for the KB instance.
    final Properties tripleStoreProperties = this.getTripleStoreProperties();
    // Create the triple store instance.
    AbstractApiTask.submitApiTask(
            m_indexManager, new CreateKBTask(namespace, tripleStoreProperties))
        .get();

    // Override namespace.  Do not create the default KB.
    final Map<String, String> initParams = new LinkedHashMap<>();
    {
      initParams.put(ConfigParams.NAMESPACE, namespace);

      initParams.put(ConfigParams.CREATE, "true");
    }

    // Start server for that kb instance.
    m_fixture = NanoSparqlServer.newInstance(0 /* port */, m_indexManager, initParams);

    m_fixture.start();

    m_port = NanoSparqlServer.getLocalPort(m_fixture);

    final String hostAddr = NicUtil.getIpAddress("default.nic", "default", true /* loopbackOk */);

    if (hostAddr == null) {

      fail("Could not identify network address for this host.");
    }

    m_rootURL = new URL("http", hostAddr, m_port, "" /* contextPath */).toExternalForm();

    m_serviceURL =
        new URL("http", hostAddr, m_port, EmbergraphStatics.getContextPath()).toExternalForm();

    if (log.isInfoEnabled())
      log.info(
          "Setup done: \nname="
              + getName()
              + "\nnamespace="
              + namespace
              + "\nrootURL="
              + m_rootURL
              + "\nserviceURL="
              + m_serviceURL);

    m_client = HttpClientConfigurator.getInstance().newInstance();

    m_repo =
        new RemoteRepositoryManager(m_serviceURL, m_client, m_indexManager.getExecutorService());
  }

  @Override
  public void tearDown() throws Exception {

    if (log.isInfoEnabled()) log.info("tearing down test: " + getName());

    if (m_fixture != null) {

      m_fixture.stop();

      m_fixture = null;
    }

    if (m_indexManager != null && namespace != null) {

      AbstractApiTask.submitApiTask(m_indexManager, new DestroyKBTask(namespace)).get();

      m_indexManager = null;
    }

    namespace = null;

    m_rootURL = null;
    m_serviceURL = null;

    m_repo.close();

    m_client.stop();

    if (log.isInfoEnabled()) log.info("tear down done");

    super.tearDown();
  }

  protected void testEmbergraphGraph(EmbergraphGraph testGraph) throws Exception {

    loadTestGraph(testGraph, testData);

    for (Vertex v : testGraph.getVertices()) {
      testPrint(v);
    }
    for (Edge e : testGraph.getEdges()) {
      testPrint(e);
    }

    testGraph.shutdown();
  }

  protected abstract EmbergraphGraph getNewGraph(String file);

  @Override
  protected EmbergraphGraph loadGraph(String file) throws Exception {

    return getNewGraph(file);
  }
}
