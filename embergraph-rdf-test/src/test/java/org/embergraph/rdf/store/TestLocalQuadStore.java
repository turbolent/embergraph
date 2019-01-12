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
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import org.embergraph.journal.Options;
import org.embergraph.rdf.axioms.NoAxioms;

/*
* Proxy test suite for {@link LocalTripleStore} in quad-store mode.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestLocalQuadStore extends AbstractTestCase {

  /** */
  public TestLocalQuadStore() {}

  public TestLocalQuadStore(String name) {
    super(name);
  }

  public static Test suite() {

    final TestLocalQuadStore delegate = new TestLocalQuadStore(); // !!!! THIS CLASS !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    final ProxyTestSuite suite = new ProxyTestSuite(delegate, "Local Quad Store Test Suite");

    /*
     * List any non-proxied tests (typically bootstrapping tests).
     */

    // ...
    //        suite.addTestSuite(TestCompletionScan.class);

    /*
     * Proxied test suite for use only with the LocalTripleStore.
     */

    suite.addTestSuite(TestLocalTripleStoreTransactionSemantics.class);

    /*
     * Pickup the basic triple store test suite. This is a proxied test
     * suite, so all the tests will run with the configuration specified in
     * this test class and its optional .properties file.
     */

    // basic test suite.
    suite.addTest(TestTripleStoreBasics.suite());

    // Note: quad store does not support inference at this time.
    // rules, inference, and truth maintenance test suite.
    //        suite.addTest( org.embergraph.rdf.rules.TestAll.suite() );

    return suite;
  }

  public Properties getProperties() {

    // Note: clone to avoid modifying!!!
    final Properties properties = (Properties) super.getProperties().clone();

    // turn on quads.
    properties.setProperty(AbstractTripleStore.Options.QUADS, "true");

    // turn off axioms.
    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    return properties;
  }

  protected AbstractTripleStore getStore(final Properties properties) {

    return LocalTripleStore.getInstance(properties);
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

    // close the store.
    store.close();

    if (!store.isStable()) {

      throw new UnsupportedOperationException("The backing store is not stable");
    }

    // Note: clone to avoid modifying!!!
    final Properties properties = (Properties) getProperties().clone();

    // Turn this off now since we want to re-open the same store.
    properties.setProperty(Options.CREATE_TEMP_FILE, "false");

    // The backing file that we need to re-open.
    final File file = ((LocalTripleStore) store).getIndexManager().getFile();

    assertNotNull(file);

    // Set the file property explicitly.
    properties.setProperty(Options.FILE, file.toString());

    return LocalTripleStore.getInstance(properties);
  }
}
