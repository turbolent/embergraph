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
 * Created on March 02, 2016
 */
package org.embergraph.rdf.sparql.ast.eval.service;

import java.util.Properties;

import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.BigdataSail;
import org.embergraph.rdf.sparql.ast.eval.AbstractDataDrivenSPARQLTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;

/**
 * Data driven test suite for custom serializer.
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestGeoSpatialCustomSerializer extends AbstractDataDrivenSPARQLTestCase {

    /**
     * 
     */
    public TestGeoSpatialCustomSerializer() {
    }

    /**
     * @param name
     */ 
    public TestGeoSpatialCustomSerializer(String name) {
        super(name);
    }


    public void testSerializerRectangle01() throws Exception {
       
       new TestHelper(
          "geo-serializer-rectangle01",
          "geo-serializer-rectangle01.rq", 
          "geo-serializer.nt",
          "geo-serializer-rectangle01.srx").runTest();
       
    }

    public void testSerializerCircle01() throws Exception {
        
        new TestHelper(
           "geo-serializer-circle01",
           "geo-serializer-circle01.rq", 
           "geo-serializer.nt",
           "geo-serializer-circle01.srx").runTest();
        
     }


    @Override
    public Properties getProperties() {

        // Note: clone to avoid modifying!!!
        final Properties properties = (Properties) super.getProperties().clone();

        // turn on quads.
        properties.setProperty(AbstractTripleStore.Options.QUADS, "false");

        // TM not available with quads.
        properties.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE,"false");

        // turn off axioms.
        properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS,
                NoAxioms.class.getName());

        // no persistence.
        properties.setProperty(org.embergraph.journal.Options.BUFFER_MODE,
                BufferMode.Transient.toString());

        // enable GeoSpatial index
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL, "true");

        // set up a datatype containing everything, including a dummy literal serializer
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_DATATYPE_CONFIG + ".0",
           "{\"config\": "
           + "{ \"uri\": \"http://my.custom.datatype/x-y-z-lat-lon-time-coord\", "
           + "\"literalSerializer\": \"org.embergraph.rdf.sparql.ast.eval.service.GeoSpatialDummyLiteralSerializer\",  "
           + "\"fields\": [ "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"-1000\", \"multiplier\": \"10\", \"serviceMapping\": \"x\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"-10\", \"multiplier\": \"100\", \"serviceMapping\": \"y\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"-2\", \"multiplier\": \"1000\", \"serviceMapping\": \"z\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"0\", \"multiplier\": \"1000000\", \"serviceMapping\": \"LATITUDE\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"minVal\" : \"0\", \"multiplier\": \"100000\", \"serviceMapping\": \"LONGITUDE\" }, "
           + "{ \"valueType\": \"LONG\", \"minVal\" : \"0\", \"multiplier\": \"1\", \"serviceMapping\": \"TIME\" }, "
           + "{ \"valueType\": \"LONG\", \"minVal\" : \"0\", \"multiplier\": \"1\", \"serviceMapping\": \"COORD_SYSTEM\" } "
           + "]}}");
        
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.VOCABULARY_CLASS,
           "org.embergraph.rdf.sparql.ast.eval.service.GeoSpatialTestVocabulary");
        
        return properties;

    }
}
