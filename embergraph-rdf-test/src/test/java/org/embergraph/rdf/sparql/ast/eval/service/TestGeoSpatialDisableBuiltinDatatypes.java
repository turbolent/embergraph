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
 * Created on March 21, 2016
 */
package org.embergraph.rdf.sparql.ast.eval.service;

import java.util.Properties;

import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sparql.ast.eval.AbstractDataDrivenSPARQLTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.service.geospatial.GeoSpatialSearchException;

/**
 * Data driven test suite testing configurability of GeoSpatial service.
 * The query set is a subset of queries from {@link TestGeoSpatialServiceEvaluation},
 * with possible modifications to account for the changed configuration.
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestGeoSpatialDisableBuiltinDatatypes extends AbstractDataDrivenSPARQLTestCase {

    /**
     * 
     */
    public TestGeoSpatialDisableBuiltinDatatypes() {
    }

    /**
     * @param name
     */ 
    public TestGeoSpatialDisableBuiltinDatatypes(String name) {
        super(name);
    }

    /**
     * Verify that built-in lat+lon datatype is disabled
     */    
    public void testDisableBuiltin01() throws Exception {
       
       try {
           
           new TestHelper(
              "geo-disable-builtin01",
              "geo-disable-builtin01.rq", 
              "geo-disable-builtin.nt",
              "geo-disable-builtin01.srx").runTest();
           

       } catch (Throwable e) {
           
           // check for wrapped exception
           assertTrue(e.toString().contains(GeoSpatialSearchException.class.getName()));
           
           return; // expected
       }
       
       throw new RuntimeException("Expected to run into exception");
    }
    
    /**
     * Verify that built-in lat+lon+time datatype is disabled
     */    
    public void testDisableBuiltin02() throws Exception {
       
       try {
           
           new TestHelper(
              "geo-disable-builtin02",
              "geo-disable-builtin02.rq", 
              "geo-disable-builtin.nt",
              "geo-disable-builtin02.srx").runTest();
           

       } catch (Throwable e) {
           
           // check for wrapped exception
           assertTrue(e.toString().contains(GeoSpatialSearchException.class.getName()));
           
           return; // expected
       }
       
       throw new RuntimeException("Expected to run into exception");
    }
    
    /**
     * Verify that specifically registered datatypes is working properly.
     */    
    public void testDisableBuiltin03() throws Exception {
       
       new TestHelper(
          "geo-disable-builtin03",
          "geo-disable-builtin03.rq", 
          "geo-disable-builtin.nt",
          "geo-disable-builtin03.srx").runTest();
       
    }
    
    @Override
    public Properties getProperties() {

        // Note: clone to avoid modifying!!!
        final Properties properties = (Properties) super.getProperties().clone();

        // turn on quads.
        properties.setProperty(AbstractTripleStore.Options.QUADS, "false");

        // TM not available with quads.
        properties.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE,"false");

        // turn off axioms.
        properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS,
                NoAxioms.class.getName());

        // no persistence.
        properties.setProperty(org.embergraph.journal.Options.BUFFER_MODE,
                BufferMode.Transient.toString());

        // enable GeoSpatial index
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL, "true");

        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_DEFAULT_DATATYPE,
           "http://www.embergraph.org/rdf/geospatial#geoSpatialLiteral");

        // NOTE: this is the crux of the test: we disable the built-in datatypes
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_INCLUDE_BUILTIN_DATATYPES,
           "false");

        // instead, we register a single, non built-in datatype
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_DATATYPE_CONFIG + ".0",
           "{\"config\": "
           + "{ \"uri\": \"http://www.embergraph.org/rdf/geospatial#geoSpatialLiteral\", "
           + "\"fields\": [ "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"0\", \"multiplier\": \"1000000\", \"serviceMapping\": \"LATITUDE\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"0\", \"multiplier\": \"100000\", \"serviceMapping\": \"LONGITUDE\" }, "
           + "{ \"valueType\": \"LONG\", \"minVal\" : \"0\", \"multiplier\": \"1\", \"serviceMapping\" : \"TIME\"  } "
           + "]}}");

        return properties;

    }
}
