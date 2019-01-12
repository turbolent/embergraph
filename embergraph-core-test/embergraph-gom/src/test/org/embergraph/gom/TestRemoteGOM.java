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
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Server;
import org.embergraph.EmbergraphStatics;
import org.embergraph.gom.gpo.IGPO;
import org.embergraph.gom.gpo.ILinkSet;
import org.embergraph.gom.om.IObjectManager;
import org.embergraph.gom.om.NanoSparqlObjectManager;
import org.embergraph.gom.om.ObjectMgrModel;
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
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/**
 * Similar to TestGOM but is setup to connect to the NanoSparqlServer using a RemoteRepository
 *
 * @author Martyn Cutcher
 */
public class TestRemoteGOM extends TestCase {

  private static final Logger log = Logger.getLogger(TestRemoteGOM.class);

  private Server m_server;

  private HttpClient m_client;

  private RemoteRepositoryManager m_repo;

  private String m_serviceURL;

  private IIndexManager m_indexManager;

  private String m_namespace;

  private EmbergraphSailRepository repo;

  //	private EmbergraphSailRepositoryConnection m_cxn;

  public void setUp() throws Exception {

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

    // instantiate a sail and a Sesame repository
    final EmbergraphSail sail = new EmbergraphSail(properties);
    repo = new EmbergraphSailRepository(sail);
    repo.initialize();

    // m_cxn = repo.getConnection();
    // m_cxn.setAutoCommit(false);

    m_namespace = EmbergraphSail.Options.DEFAULT_NAMESPACE;

    final Map<String, String> initParams = new LinkedHashMap<String, String>();
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
  }

  // FIXME This is probably not tearing down the backing file for the journal!
  public void tearDown() throws Exception {

    if (log.isInfoEnabled()) log.info("tearing down test: " + getName());

    if (m_server != null) {

      m_server.stop();

      m_server = null;
    }

    if (m_repo != null) {
      m_repo.close();

      m_repo = null;
    }

    if (m_client != null) {
      m_client.stop();

      m_client = null;
    }

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

    m_indexManager = null;

    m_namespace = null;

    super.tearDown();

    log.info("tear down done");
  }

  public void testSimpleDirectData() throws Exception {
    final IObjectManager om =
        new NanoSparqlObjectManager(m_repo.getRepositoryForDefaultNamespace(), m_namespace);
    try {
      final ValueFactory vf = om.getValueFactory();

      final URL n3 = TestGOM.class.getResource("testgom.n3");

      print(n3);
      load(n3, RDFFormat.N3);

      final URI s = vf.createURI("gpo:#root");

      final URI rootAttr = vf.createURI("attr:/root");
      om.getGPO(s).getValue(rootAttr);
      final URI rootId = (URI) om.getGPO(s).getValue(rootAttr);

      final IGPO rootGPO = om.getGPO(rootId);

      if (log.isInfoEnabled()) {
        log.info("--------");
        log.info(rootGPO.pp());
        log.info(rootGPO.getType().pp());
        log.info(rootGPO.getType().getStatements());
      }

      final URI typeName = vf.createURI("attr:/type#name");
      assertTrue("Company".equals(rootGPO.getType().getValue(typeName).stringValue()));

      // find set of workers for the Company
      final URI worksFor = vf.createURI("attr:/employee#worksFor");
      final ILinkSet linksIn = rootGPO.getLinksIn(worksFor);
      final Iterator<IGPO> workers = linksIn.iterator();
      while (workers.hasNext()) {
        final IGPO tmp = workers.next();
        log.info("Returned: " + tmp.pp());
      }
    } finally {
      om.close();
    }
  }

  public void testSimpleCreate() throws RepositoryException, IOException {
    final NanoSparqlObjectManager om =
        new NanoSparqlObjectManager(m_repo.getRepositoryForDefaultNamespace(), m_namespace);
    final ValueFactory vf = om.getValueFactory();

    final URI keyname = vf.createURI("attr:/test#name");
    final Resource id = vf.createURI("gpo:test#1");
    //		om.checkValue(id);
    final int transCounter = om.beginNativeTransaction();
    try {
      IGPO gpo = om.getGPO(id);

      gpo.setValue(keyname, vf.createLiteral("Martyn"));

      om.commitNativeTransaction(transCounter);
    } catch (Throwable t) {
      om.rollbackNativeTransaction();

      throw new RuntimeException(t);
    }

    // clear cached data
    ((ObjectMgrModel) om).clearCache();

    IGPO gpo = om.getGPO(id); // reads from backing journal

    assertTrue("Martyn".equals(gpo.getValue(keyname).stringValue()));
  }

  /** Throughput test for updates. */
  public void testUpdateThroughput() throws RepositoryException, IOException {

    final IObjectManager om =
        new NanoSparqlObjectManager(m_repo.getRepositoryForDefaultNamespace(), m_namespace);
    final ValueFactory vf = om.getValueFactory();

    final int transCounter = om.beginNativeTransaction();
    final URI name = vf.createURI("attr:/test#name");
    final URI ni = vf.createURI("attr:/test#ni");
    final URI age = vf.createURI("attr:/test#age");
    final URI mob = vf.createURI("attr:/test#mobile");
    final URI gender = vf.createURI("attr:/test#mail");
    try {
      // warmup
      for (int i = 0; i < 10000; i++) {
        final IGPO tst = om.createGPO();
        tst.setValue(name, vf.createLiteral("Test" + i));
      }

      // go for it
      final long start = System.currentTimeMillis();

      final int creates = 20000;
      for (int i = 0; i < creates; i++) {
        final IGPO tst = om.createGPO();
        tst.setValue(name, vf.createLiteral("Name" + i));
        tst.setValue(ni, vf.createLiteral("NI" + i));
        tst.setValue(age, vf.createLiteral(i));
        tst.setValue(mob, vf.createLiteral("0123-" + i));
        tst.setValue(gender, vf.createLiteral(1 % 3 == 0));
      }

      om.commitNativeTransaction(transCounter);

      final long duration = (System.currentTimeMillis() - start);

      /*
       * Note that this is a conservative estimate for statements per
       * second since there is only one per object, requiring the object
       * URI and the Value to be added.
       */
      if (log.isInfoEnabled()) {
        log.info("Creation rate of " + (creates * 1000 / duration) + " objects per second");
        log.info("Creation rate of " + (creates * 5 * 1000 / duration) + " statements per second");
      }
    } catch (Throwable t) {
      t.printStackTrace();

      om.rollbackNativeTransaction();

      throw new RuntimeException(t);
    }
  }

  public void testSimpleJSON() throws RepositoryException, IOException {
    final NanoSparqlObjectManager om =
        new NanoSparqlObjectManager(m_repo.getRepositoryForDefaultNamespace(), m_namespace);
    final ValueFactory vf = om.getValueFactory();

    final URI keyname = vf.createURI("attr:/test#name");
    final Resource id = vf.createURI("gpo:test#1");
    //		om.checkValue(id);
    final int transCounter = om.beginNativeTransaction();
    try {
      IGPO gpo = om.getGPO(id);

      gpo.setValue(keyname, vf.createLiteral("Martyn"));

      om.commitNativeTransaction(transCounter);
    } catch (Throwable t) {
      om.rollbackNativeTransaction();

      throw new RuntimeException(t);
    }

    // Now let's read the data as JSON by connecting directly with the serviceurl
    URL url = new URL(m_serviceURL);
    URLConnection server = url.openConnection();
    try {
      // server.setRequestProperty("Accept", TupleQueryResultFormat.JSON.toString());
      server.setDoOutput(true);
      server.connect();
      PrintWriter out = new PrintWriter(server.getOutputStream());
      out.print("query=SELECT ?p ?v WHERE {<" + id.stringValue() + "> ?p ?v}");
      out.close();
      InputStream inst = server.getInputStream();
      byte[] buf = new byte[2048];
      int curs = 0;
      while (true) {
        int len = inst.read(buf, curs, buf.length - curs);
        if (len == -1) {
          break;
        }
        curs += len;
      }
      if (log.isInfoEnabled()) log.info("Read in " + curs + " - " + new String(buf, 0, curs));
    } catch (Exception e) {
      e.printStackTrace();
    }

    // clear cached data
    ((ObjectMgrModel) om).clearCache();

    IGPO gpo = om.getGPO(id); // reads from backing journal

    assertTrue("Martyn".equals(gpo.getValue(keyname).stringValue()));
  }

  /** Utility to load statements from a resource */
  private void load(final URL n3, final RDFFormat rdfFormat)
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

  void print(final URL n3) throws IOException {
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
}
