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

package org.embergraph.rdf.sail;

import java.util.Arrays;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.OwlAxioms;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;

/*
* To run this test case, specify the following JVM property: <code>
 * -DtestClass=org.embergraph.rdf.sail.TestEmbergraphSailWithoutSids</code>
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestTicket610 extends ProxyEmbergraphSailTestCase {

  protected static final Logger log = Logger.getLogger(TestTicket610.class);

  @Override
  public Properties getProperties() {

    Properties props = super.getProperties();

    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, OwlAxioms.class.getName());

    return props;
  }

  public TestTicket610() {}

  public TestTicket610(String arg0) {
    super(arg0);
  }

  public void testBug() throws Exception {

    final URI a = new URIImpl(":a");
    final URI b = new URIImpl(":b");

    final Model data =
        new LinkedHashModel(
            Arrays.asList(
                new StatementImpl(a, RDF.TYPE, OWL.TRANSITIVEPROPERTY),
                new StatementImpl(b, RDFS.SUBPROPERTYOF, a)));

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final EmbergraphSail sail = getSail();

    try {

      sail.initialize();

      final EmbergraphSailRepository embergraphRepo = new EmbergraphSailRepository(sail);

      { // load the data into the embergraph store
        final RepositoryConnection cxn = embergraphRepo.getConnection();
        try {
          cxn.setAutoCommit(false);
          cxn.add(data);
          cxn.commit();
        } finally {
          cxn.close();
        }
      }

      { // check the closure
        final EmbergraphSailRepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
        try {

          final AbstractTripleStore store = cxn.getTripleStore();

          if (log.isDebugEnabled()) {
            log.info(store.dumpStore(true, true, false));
          }

          assertFalse(
              "should not have the (<b> rdf:type owl:TransitiveProperty) inference",
              store.hasStatement(b, RDF.TYPE, OWL.TRANSITIVEPROPERTY));

        } finally {
          cxn.close();
        }
      }

    } finally {
      sail.__tearDownUnitTest();
    }
  }
}
