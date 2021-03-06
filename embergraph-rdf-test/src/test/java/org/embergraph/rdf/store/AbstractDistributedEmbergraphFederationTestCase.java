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

import junit.framework.TestCase2;
import org.embergraph.service.AbstractScaleOutClient;

/*
 * An abstract test harness that sets up (and tears down) the metadata and data services required
 * for a embergraph federation using JINI to handle service discovery.
 *
 * <p>Note: The configuration options for the (meta)data services are set in their respective <code>
 * properties</code> files NOT by the System properties!
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractDistributedEmbergraphFederationTestCase extends TestCase2 {

  public AbstractDistributedEmbergraphFederationTestCase() {
    super();
  }

  public AbstractDistributedEmbergraphFederationTestCase(String name) {
    super(name);
  }

  // BLZG-1370 removing unused test classes for embergraph-jini
  // private JiniServicesHelper helper = new JiniServicesHelper();

  protected AbstractScaleOutClient client;

  protected void setUp() throws Exception {

    // start services.
    // BLZG-1370 removing unused test classes for embergraph-jini
    //        helper.start();

    // expose to subclasses.
    // BLZG-1370 removing unused test classes for embergraph-jini
    //        client = helper.client;

  }

  protected void tearDown() throws Exception {

    // BLZG-1370 removing unused test classes for embergraph-jini
    //        helper.destroy();

  }
}
