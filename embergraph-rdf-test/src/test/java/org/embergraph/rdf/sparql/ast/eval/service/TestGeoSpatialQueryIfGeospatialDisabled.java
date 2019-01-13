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
 * Created on March 20, 2016
 */
package org.embergraph.rdf.sparql.ast.eval.service;

import java.util.Properties;
import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sparql.ast.eval.AbstractDataDrivenSPARQLTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.service.geospatial.GeoSpatialSearchException;

/*
 * Test case asserting that geospatial SERVICE query fails with proper exception if geospatial
 * submodue is turned off.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestGeoSpatialQueryIfGeospatialDisabled extends AbstractDataDrivenSPARQLTestCase {

  /** */
  public TestGeoSpatialQueryIfGeospatialDisabled() {}

  /** @param name */
  public TestGeoSpatialQueryIfGeospatialDisabled(String name) {
    super(name);
  }

  /*
   * Submit a query and make sure it fails with proper exception if geospatial SERVCIE is not
   * enabled.
   */
  public void testDocumentationBuiltin01() {

    try {
      new TestHelper(
              "geo-documentation-builtin01",
              "geo-documentation-builtin01.rq",
              "geo-documentation.ttl",
              "geo-documentation-builtin01.srx")
          .runTest();
    } catch (Exception e) {
      assertTrue(e.toString().contains(GeoSpatialSearchException.class.getName()));
      return; // expected
    }

    throw new RuntimeException("Expected to run into exception.");
  }

  @Override
  public Properties getProperties() {

    // Note: clone to avoid modifying!!!
    final Properties properties = (Properties) super.getProperties().clone();

    // turn on quads.
    properties.setProperty(AbstractTripleStore.Options.QUADS, "false");

    // TM not available with quads.
    properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

    // turn off axioms.
    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // no persistence.
    properties.setProperty(
        org.embergraph.journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    // enable GeoSpatial index
    properties.setProperty(
        org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL, "false");

    return properties;
  }
}
