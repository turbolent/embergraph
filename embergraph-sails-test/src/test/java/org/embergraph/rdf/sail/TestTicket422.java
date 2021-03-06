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
 * Created on Dec 6, 2011
 */

package org.embergraph.rdf.sail;

import info.aduna.iteration.CloseableIteration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.TempTripleStore;
import org.openrdf.model.Statement;
import org.openrdf.sail.SailException;

/*
 * Test suite for wrapping a {@link TempTripleStore} as a {@link EmbergraphSail}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestTicket422 extends ProxyEmbergraphSailTestCase {

  /** */
  public TestTicket422() {}

  /** @param name */
  public TestTicket422(String name) {
    super(name);
  }

  public void test_wrapTempTripleStore()
      throws SailException, ExecutionException, InterruptedException {

    final EmbergraphSail sail = getSail();

    try {

      sail.initialize();

      final String namespace = sail.getNamespace();

      final EmbergraphSailConnection mainConn = sail.getUnisolatedConnection();

      try {

        final AbstractTripleStore mainTripleStore = mainConn.getTripleStore();

        if (mainTripleStore == null) throw new UnsupportedOperationException();

        final TempTripleStore tempStore =
            new TempTripleStore(
                sail.getIndexManager().getTempStore(), mainConn.getProperties(), mainTripleStore);

        try {

          // Note: The namespace of the tempSail MUST be distinct from the namespace of the main
          // Sail.
          final EmbergraphSail tempSail =
              new EmbergraphSail(
                  namespace + "-" + UUID.randomUUID(),
                  tempStore.getIndexManager(),
                  mainTripleStore.getIndexManager());

          try {

            tempSail.initialize();

            tempSail.create(new Properties());

            final EmbergraphSailConnection con = tempSail.getConnection();

            try {

              final CloseableIteration<? extends Statement, SailException> itr =
                  con.getStatements(null, null, null, null);

              try {

                while (itr.hasNext()) {

                  itr.next();
                }

              } finally {

                itr.close();
              }

            } finally {

              con.close();
            }

          } finally {

            tempSail.shutDown();
          }

        } finally {

          tempStore.close();
        }

      } finally {

        mainConn.close();
      }

    } finally {

      sail.__tearDownUnitTest();
    }
  }
}
