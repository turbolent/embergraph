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
 * Created on Oct 18, 2007
 */

package org.embergraph.rdf.store;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import org.embergraph.journal.ITx;
import org.embergraph.service.DataService;
import org.embergraph.service.EmbeddedClient;
import org.embergraph.service.EmbeddedFederation;

/*
 * Proxy test suite for {@link ScaleOutTripleStore} running against an {@link EmbeddedFederation}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestScaleOutTripleStoreWithEmbeddedFederation extends AbstractTestCase {

  /** */
  public TestScaleOutTripleStoreWithEmbeddedFederation() {}

  public TestScaleOutTripleStoreWithEmbeddedFederation(String name) {
    super(name);
  }

  public static Test suite() {

    final TestScaleOutTripleStoreWithEmbeddedFederation delegate =
        new TestScaleOutTripleStoreWithEmbeddedFederation(); // !!!! THIS CLASS !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    ProxyTestSuite suite =
        new ProxyTestSuite(delegate, "Scale-Out Triple Store Test Suite (embedded federation)");

    /*
     * List any non-proxied tests (typically bootstrapping tests).
     */

    //        // writes on the term:id and id:term indices.
    //        suite.addTestSuite(TestTermAndIdsIndex.class);
    //
    //        // writes on the statement indices.
    //        suite.addTestSuite(TestStatementIndex.class);

    /*
     * Proxied test suite for use only with the LocalTripleStore.
     *
     * @todo test unisolated operation semantics.
     */

    //        suite.addTestSuite(TestFullTextIndex.class);

    //        suite.addTestSuite(TestLocalTripleStoreTransactionSemantics.class);

    /*
     * Pickup the basic triple store test suite. This is a proxied test
     * suite, so all the tests will run with the configuration specified in
     * this test class and its optional .properties file.
     */

    // basic test suite.
    suite.addTest(TestTripleStoreBasics.suite());

    // rules, inference, and truth maintenance test suite.
    suite.addTest(org.embergraph.rdf.rules.TestAll.suite());

    return suite;
  }

  /** Properties used by tests in the file and in this proxy suite. */
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    //         Note: this reduces the disk usage at the expense of memory usage.
    //        properties.setProperty(EmbeddedEmbergraphFederation.Options.BUFFER_MODE,
    //                BufferMode.Transient.toString());

    //        properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

    //        properties.setProperty(Options.CREATE_TEMP_FILE,"true");

    //        properties.setProperty(Options.DELETE_ON_EXIT,"true");

    properties.setProperty(DataService.Options.OVERFLOW_ENABLED, "false");

    // disable platform statistics collection.
    properties.setProperty(EmbeddedClient.Options.COLLECT_PLATFORM_STATISTICS, "false");

    /*
     * Note: there are also properties to control the #of data services
     * created in the embedded federation.
     */

    return properties;
  }

  /** An embedded federation is setup and torn down per unit test. */
  EmbeddedClient client;

  /*
   * Data files are placed into a directory named by the test. If the directory exists, then it is
   * removed before the federation is set up.
   */
  public void setUp(final ProxyTestCase testCase) throws Exception {

    super.setUp(testCase);

    final File dataDir = new File(testCase.getName());

    if (dataDir.exists() && dataDir.isDirectory()) {

      recursiveDelete(dataDir);
    }

    final Properties properties = new Properties(getProperties());

    //        // Note: directory named for the unit test (name is available from the
    //        // proxy test case).
    //        properties.setProperty(EmbeddedClient.Options.DATA_DIR, testCase
    //                .getName());

    // new client
    client = new EmbeddedClient(properties);

    // connect.
    client.connect();
  }

  public void tearDown(final ProxyTestCase testCase) throws Exception {

    if (client != null) {

      if (client.isConnected()) {

        // destroy the federation under test.
        client.getFederation().destroy();
      }

      /*
       * Note: Must clear the reference or junit will cause the federation
       * to be retained.
       */
      client = null;
    }

    super.tearDown();
  }

  private AtomicInteger inc = new AtomicInteger();

  protected AbstractTripleStore getStore(final Properties properties) {

    // Note: distinct namespace for each triple store created on the federation.
    final String namespace = "test" + inc.incrementAndGet();

    final AbstractTripleStore store =
        new ScaleOutTripleStore(client.getFederation(), namespace, ITx.UNISOLATED, properties);

    store.create();

    return store;
  }

  /*
   * Re-open the same backing store.
   *
   * @param store the existing store.
   * @return A new store.
   * @exception Throwable if the existing store is closed, or if the store can not be re-opened,
   *     e.g., from failure to obtain a file lock, etc.
   */
  protected AbstractTripleStore reopenStore(final AbstractTripleStore store) {

    final String namespace = store.getNamespace();

    // Note: properties we need to re-start the client.
    final Properties properties = new Properties(client.getProperties());

    // Note: also shutdown the embedded federation.
    client.disconnect(true /*immediateShutdown*/);

    // Turn this off now since we want to re-open the same store.
    properties.setProperty(org.embergraph.journal.Options.CREATE_TEMP_FILE, "false");

    // The data directory for the embedded federation.
    final File file = ((EmbeddedFederation) store.getIndexManager()).getDataDir();

    // Set the file property explicitly.
    properties.setProperty(EmbeddedClient.Options.DATA_DIR, file.toString());

    // new client.
    client = new EmbeddedClient(properties);

    // connect.
    client.connect();

    // Obtain view on the triple store.
    return new ScaleOutTripleStore(
            client.getFederation(), namespace, ITx.UNISOLATED, store.getProperties()
            //                client.getProperties()
            )
        .init();
  }
}
