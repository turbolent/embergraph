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
 * Created on Jul 25, 2007
 */

package org.embergraph.rdf.store;

import java.util.Properties;
import org.embergraph.journal.ITx;

/**
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractEmbeddedTripleStoreTestCase
    extends AbstractEmbeddedEmbergraphFederationTestCase {

  /** */
  public AbstractEmbeddedTripleStoreTestCase() {
    super();
  }

  /** @param arg0 */
  public AbstractEmbeddedTripleStoreTestCase(String arg0) {
    super(arg0);
  }

  /** The triple store under test. */
  ScaleOutTripleStore store;

  protected Properties getProperties() {

    return new Properties(System.getProperties());
  }

  public void setUp() throws Exception {

    super.setUp();

    // connect to the database.
    store =
        new ScaleOutTripleStore(
            client.getFederation(), "test_", ITx.UNISOLATED, client.getProperties());

    store.create();
  }

  public void tearDown() throws Exception {

    super.tearDown();
  }
}
