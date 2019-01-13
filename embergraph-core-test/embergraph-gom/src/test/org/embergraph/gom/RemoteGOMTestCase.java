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
 * Created on Mar 19, 2012
 */
package org.embergraph.gom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.embergraph.EmbergraphStatics;
import org.embergraph.gom.om.IObjectManager;
import org.embergraph.gom.om.NanoSparqlObjectManager;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal.Options;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.webapp.ConfigParams;
import org.embergraph.rdf.sail.webapp.NanoSparqlServer;
import org.embergraph.rdf.sail.webapp.client.HttpClientConfigurator;
import org.embergraph.rdf.sail.webapp.client.RemoteRepositoryManager;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.util.config.NicUtil;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/*
 * Similar to TestGOM but is setup to connect to the NanoSparqlServer using a RemoteRepository
 *
 * @author Martyn Cutcher
 */
public class RemoteGOMTestCase extends TestCase implements IGOMProxy {

  private static final Logger log = Logger.getLogger(RemoteGOMTestCase.class);

  protected Server m_server;

  protected HttpClient m_client;
  protected RemoteRepositoryManager m_repo;

  protected String m_serviceURL;

  protected IIndexManager m_indexManager;

  protected String m_namespace;

  protected EmbergraphSailRepository repo;

  protected ValueFactory m_vf;
  protected IObjectManager om;

  public static Test suite() {

    final RemoteGOMTestCase delegate = new RemoteGOMTestCase(); // !!!! THIS CLASS
    // !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    final ProxyTestSuite suite = new ProxyTestSuite(delegate, "Remote GOM tests");

    suite.addTestSuite(TestGPO.class);
    suite.addTestSuite(TestGOM.class);
    suite.addTestSuite(TestOwlGOM.class);

    return suite;
  }

  //	protected EmbergraphSailRepositoryConnection m_cxn;

  protected Properties getProperties() throws Exception {

    final Properties properties = new Properties();

    // create a backing file for the database
    final File journal = File.createTempFile("embergraph", ".jnl");
    properties.setProperty(EmbergraphSail.Options.FILE, journal.getAbsolutePath());
    properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskRW.toString());
    properties.setProperty(AbstractTripleStore.Options.TEXT_INDEX, "false");
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    //        properties.setProperty(
    //                IndexMetadata.Options.WRITE_RETENTION_QUEUE_CAPACITY, "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.spo.SPO.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.spo.POS.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.spo.OSP.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.spo.BLOBS.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.lex.TERM2ID.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");
    //        properties
    //                .setProperty(
    //
    // "org.embergraph.namespace.kb.lex.ID2TERM.org.embergraph.btree.BTree.branchingFactor",
    //                        "200");

    return properties;
  }

  @Override
  public void setUp() throws Exception {

    // instantiate a sail and a Sesame repository
    final EmbergraphSail sail = new EmbergraphSail(getProperties());
    repo = new EmbergraphSailRepository(sail);
    repo.initialize();

    // m_cxn = repo.getConnection();
    // m_cxn.setAutoCommit(false);

    m_namespace = EmbergraphSail.Options.DEFAULT_NAMESPACE;

    final Map<String, String> initParams = new LinkedHashMap<>();
    {
      initParams.put(ConfigParams.NAMESPACE, m_namespace);

      initParams.put(ConfigParams.CREATE, "false");
    }

    m_indexManager = repo.getSail().getIndexManager();
    m_server = NanoSparqlServer.newInstance(0 /* port */, m_indexManager, initParams);

    m_server.start();

    final int port = NanoSparqlServer.getLocalPort(m_server);

    final String hostAddr = NicUtil.getIpAddress("default.nic", "default", true /* loopbackOk */);

    if (hostAddr == null) {

      fail("Could not identify network address for this host.");
    }

    m_serviceURL =
        new URL("http", hostAddr, port, EmbergraphStatics.getContextPath() /* file */)
            // EmbergraphStatics.getContextPath() + "/sparql"/* file */)
            .toExternalForm();

    // final HttpClient httpClient = new DefaultHttpClient();

    // m_cm = httpClient.getConnectionManager();
    m_client = HttpClientConfigurator.getInstance().newInstance();

    m_repo =
        new RemoteRepositoryManager(m_serviceURL, m_client, m_indexManager.getExecutorService());

    om = new NanoSparqlObjectManager(m_repo.getRepositoryForDefaultNamespace(), m_namespace);
  }

  // FIXME This is probably not tearing down the backing file for the journal!
  @Override
  public void tearDown() throws Exception {

    if (log.isInfoEnabled()) log.info("tearing down test: " + getName());

    if (om != null) {
      om.close();
      om = null;
    }

    if (m_server != null) {

      m_server.stop();

      m_server = null;
    }

    m_repo.close();

    m_repo = null;

    m_client.stop();
    m_client = null;

    m_serviceURL = null;

    if (m_indexManager != null && m_namespace != null) {

      final AbstractTripleStore tripleStore =
          (AbstractTripleStore)
              m_indexManager.getResourceLocator().locate(m_namespace, ITx.UNISOLATED);

      if (tripleStore != null) {

        if (log.isInfoEnabled()) log.info("Destroying: " + m_namespace);

        tripleStore.destroy();
      }
    }

    if (m_indexManager != null) {
      m_indexManager.destroy();
    }

    m_indexManager = null;

    m_namespace = null;

    super.tearDown();

    log.info("tear down done");
  }

  /** Utility to load statements from a resource */
  @Override
  public void load(final URL n3, final RDFFormat rdfFormat)
      throws IOException, RDFParseException, RepositoryException {
    final InputStream in = n3.openConnection().getInputStream();
    try {
      final Reader reader = new InputStreamReader(in);

      // FIXME: Loads into server directly, should change later to load
      // view ObjectManager
      final EmbergraphSailRepositoryConnection m_cxn = repo.getConnection();
      try {
        m_cxn.setAutoCommit(false);
        m_cxn.add(reader, "kb", rdfFormat);
        m_cxn.commit();
      } finally {
        m_cxn.close();
      }
    } finally {
      in.close();
    }
  }

  protected void print(final URL n3) throws IOException {
    if (log.isInfoEnabled()) {
      InputStream in = n3.openConnection().getInputStream();
      Reader reader = new InputStreamReader(in);
      try {
        char[] buf = new char[256];
        int rdlen = 0;
        while ((rdlen = reader.read(buf)) > -1) {
          if (rdlen == 256) System.out.print(buf);
          else System.out.print(new String(buf, 0, rdlen));
        }
      } finally {
        reader.close();
      }
    }
  }

  @Override
  public IObjectManager getObjectManager() {
    return om;
  }

  @Override
  public ValueFactory getValueFactory() {
    return m_vf;
  }

  @Override
  public void proxySetup() throws Exception {
    setUp();
  }

  @Override
  public void proxyTearDown() throws Exception {
    tearDown();
  }
}
