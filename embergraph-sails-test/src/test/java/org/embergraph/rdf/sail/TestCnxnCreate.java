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
 * Created on Mar 7, 2011
 */

package org.embergraph.rdf.sail;

import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.sail.SailException;

/*
 * Test suite for BLZG-2056 EmbergraphSailConnections not always closed by EmbergraphSail.shutdown()
 *
 * @see https://jira.blazegraph.com/browse/BLZG-2056
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestCnxnCreate extends ProxyEmbergraphSailTestCase {

  /** */
  public TestCnxnCreate() {}

  /** @param name */
  public TestCnxnCreate(String name) {
    super(name);
  }

  /*
   * Test whether connections are auto closed when the Sail is shutdown
   *
   * @throws SailException
   * @throws InterruptedException
   * @throws IOException
   */
  public void test_manageConnections() throws SailException, InterruptedException {

    final Properties properties = getProperties();

    // truth maintenance is not compatible with full transactions.
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    properties.setProperty(
        AbstractTripleStore.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    properties.setProperty(EmbergraphSail.Options.ISOLATABLE_INDICES, "true");

    properties.setProperty(AbstractTripleStore.Options.JUSTIFY, "false");

    properties.setProperty(AbstractTripleStore.Options.INLINE_DATE_TIMES, "false");

    final EmbergraphSail sail = new EmbergraphSail(properties);

    try {

      sail.initialize();

      log.info("Sail is initialized.");

      final EmbergraphSailConnection uicnxn = sail.getUnisolatedConnection();

      assertTrue(uicnxn.isOpen());

      final EmbergraphSailConnection rocnxn = sail.getReadOnlyConnection();

      assertTrue(rocnxn.isOpen());

      sail.shutDown();

      log.info("done - sail.shutDown()");

      assertTrue(!uicnxn.isOpen());
      assertTrue(!rocnxn.isOpen());

    } finally {

      log.info("__tearDownUnitTest");
      sail.__tearDownUnitTest();
      log.info("done - __tearDownUnitTest");
    }
  }

  /*
   * Test whether connections are auto closed when the Sail is shutdown
   *
   * @throws SailException
   * @throws InterruptedException
   * @throws IOException
   */
  public void test_manageDefaultConnection()
      throws SailException {

    final Properties properties = getProperties();

    // truth maintenance is not compatible with full transactions.
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    properties.setProperty(
        AbstractTripleStore.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    properties.setProperty(EmbergraphSail.Options.ISOLATABLE_INDICES, "true");

    properties.setProperty(AbstractTripleStore.Options.JUSTIFY, "false");

    properties.setProperty(AbstractTripleStore.Options.INLINE_DATE_TIMES, "false");

    final EmbergraphSail sail = new EmbergraphSail(properties);

    try {

      sail.initialize();

      log.info("Sail is initialized.");

      final EmbergraphSailConnection cnxn = sail.getConnection();

      assertTrue(cnxn.isOpen());

      sail.shutDown();

      log.info("done - sail.shutDown()");

      assertTrue(!cnxn.isOpen());

    } finally {

      log.info("__tearDownUnitTest");
      sail.__tearDownUnitTest();
      log.info("done - __tearDownUnitTest");
    }
  }

  public void test_manageThreadConnections()
      throws SailException, InterruptedException {

    final Properties properties = getProperties();

    // truth maintenance is not compatible with full transactions.
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    properties.setProperty(
        AbstractTripleStore.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    properties.setProperty(EmbergraphSail.Options.ISOLATABLE_INDICES, "true");

    properties.setProperty(AbstractTripleStore.Options.JUSTIFY, "false");

    properties.setProperty(AbstractTripleStore.Options.INLINE_DATE_TIMES, "false");

    final EmbergraphSail sail = new EmbergraphSail(properties);

    try {

      sail.initialize();

      log.info("Sail is initialized.");

      final AtomicReference<EmbergraphSailConnection> uicnxn =
          new AtomicReference<>();
      final Semaphore c1 = new Semaphore(0);

      final Thread t =
          new Thread(() -> {
            System.out.println("Running...");
            uicnxn.set(sail.getUnisolatedConnection());
            System.out.println("Releasing permit");
            c1.release();
          });

      t.start();
      c1.acquire();

      assertTrue(uicnxn.get().isOpen());

      final EmbergraphSailConnection rocnxn = sail.getReadOnlyConnection();

      assertTrue(rocnxn.isOpen());

      //            final EmbergraphSailConnection rwcnxn = sail.getReadWriteConnection();
      //
      //            assertTrue(rwcnxn.isOpen());
      //
      sail.shutDown();

      log.warn("don - sail.shutDown()");

      assertTrue(!uicnxn.get().isOpen());
      assertTrue(!rocnxn.isOpen());
      //            assertTrue(!rwcnxn.isOpen());

    } finally {

      log.warn("__tearDownUnitTest");
      sail.__tearDownUnitTest();
      log.warn("done - __tearDownUnitTest");
    }
  }
}
