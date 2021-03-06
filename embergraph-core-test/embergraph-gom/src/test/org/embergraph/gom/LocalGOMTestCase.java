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
package org.embergraph.gom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.embergraph.EmbergraphStatics;
import org.embergraph.gom.om.IObjectManager;
import org.embergraph.gom.om.ObjectManager;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal.Options;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

public class LocalGOMTestCase extends TestCase implements IGOMProxy {

  private static final Logger log = Logger.getLogger(LocalGOMTestCase.class);

  protected EmbergraphSailRepository m_repo;
  protected EmbergraphSail m_sail;
  protected ValueFactory m_vf;
  protected IObjectManager om;

  public LocalGOMTestCase() {}

  public LocalGOMTestCase(String name) {
    super(name);
  }

  public static Test suite() {

    final LocalGOMTestCase delegate = new LocalGOMTestCase(); // !!!! THIS CLASS
    // !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    final ProxyTestSuite suite = new ProxyTestSuite(delegate, "Local GOM tests");

    suite.addTestSuite(TestGPO.class);
    suite.addTestSuite(TestGOM.class);
    suite.addTestSuite(TestOwlGOM.class);

    return suite;
  }

  /*
   * List any non-proxied tests (typically bootstrapping tests).
   */

  protected Properties getProperties() throws Exception {

    final Properties properties = new Properties();

    // create a backing file for the database
    final File journal = File.createTempFile("embergraph", ".jnl");
    properties.setProperty(EmbergraphSail.Options.FILE, journal.getAbsolutePath());
    properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskRW.toString());
    properties.setProperty(AbstractTripleStore.Options.TEXT_INDEX, "false");
    //        properties.setProperty(IndexMetadata.Options.WRITE_RETENTION_QUEUE_CAPACITY, "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.spo.SPO.org.embergraph.btree.BTree.branchingFactor", "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.spo.POS.org.embergraph.btree.BTree.branchingFactor", "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.spo.OSP.org.embergraph.btree.BTree.branchingFactor", "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.spo.BLOBS.org.embergraph.btree.BTree.branchingFactor", "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.lex.TERM2ID.org.embergraph.btree.BTree.branchingFactor", "200");
    //
    // properties.setProperty("org.embergraph.namespace.kb.lex.ID2TERM.org.embergraph.btree.BTree.branchingFactor", "200");
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    return properties;
  }

  protected void setUp() throws Exception {

    // instantiate a sail and a Sesame repository
    m_sail = new EmbergraphSail(getProperties());
    m_repo = new EmbergraphSailRepository(m_sail);
    m_repo.initialize();
    m_vf = m_sail.getValueFactory();
    // Note: This uses a mock endpoint URL.
    om =
        new ObjectManager(
            "http://localhost" + EmbergraphStatics.getContextPath() + "/sparql", m_repo);
  }

  protected void tearDown() {
    //        try {
    //            final long start = System.currentTimeMillis();
    // m_repo.close();
    m_sail.__tearDownUnitTest();
    m_sail = null;
    m_repo = null;
    m_vf = null;
    if (om != null) {
      om.close();
      om = null;
    }
    //            final long dur = System.currentTimeMillis() - start;
    //            if (log.isInfoEnabled())
    //                log.info("Sail shutdown: " + dur + "ms");
    //        } catch (SailException e) {
    //            e.printStackTrace();
    //        }
  }

  protected void print(final URL n3) throws IOException {
    if (log.isInfoEnabled()) {
      final InputStream in = n3.openConnection().getInputStream();
      final Reader reader = new InputStreamReader(in);
      try {
        final char[] buf = new char[256];
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

  /** Utility to load n3 statements from a resource */
  public void load(final URL n3, final RDFFormat rdfFormat)
      throws IOException, RDFParseException, RepositoryException {

    final InputStream in = n3.openConnection().getInputStream();
    try {
      final Reader reader = new InputStreamReader(in);
      try {

        final EmbergraphSailRepositoryConnection cxn = m_repo.getConnection();
        try {
          cxn.setAutoCommit(false);
          cxn.add(reader, "", rdfFormat);
          cxn.commit();
        } finally {
          cxn.close();
        }
      } finally {
        reader.close();
      }
    } finally {
      in.close();
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
