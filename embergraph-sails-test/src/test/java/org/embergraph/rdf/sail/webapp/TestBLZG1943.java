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
 * Created on June 08, 2016
 */
package org.embergraph.rdf.sail.webapp;

import java.util.Properties;

import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;

import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.impl.extensions.GeoSpatialLiteralExtension;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.ProxyEmbergraphSailTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;

/**
 * Test case for ticket BLZG-1943: thread safety issues related to reuse of
 * {@link GeoSpatialLiteralExtension}'s internal key builder.
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestBLZG1943 extends ProxyEmbergraphSailTestCase {

    /**
     * 
     */
    public TestBLZG1943() {
    }

    /**
     * @param name
     */ 
    public TestBLZG1943(String name) {
        super(name);
    }

    public void testTicketBlzg1943() throws Exception {

        
        final EmbergraphSail sail = getSail();
        final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

        try {

            repo.initialize();
            
            EmbergraphSailRepositoryConnection conn = repo.getConnection();
            
            // init repository
            conn.setAutoCommit(false);
            for (int i=0; i<500; i++) {
                final Statement s1 = new StatementImpl(new URIImpl("http://s" + i), new URIImpl("http://lat"), new LiteralImpl("10.24884"));
                final Statement s2 = new StatementImpl(new URIImpl("http://s" + i), new URIImpl("http://lon"), new LiteralImpl("73.24884"));
                
                conn.add(s1);
                conn.add(s2);
            }
            conn.commit();

            // validate data has been loaded
            final TupleQuery validate = conn.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o }");
            final TupleQueryResult res = validate.evaluate();
            int numTriples = 0;
            while (res.hasNext()) { res.next(); numTriples++; }
            assertEquals(numTriples, 1000);
            res.close();
            conn.close();

            // submit update query inducing geospatial literal construction
            conn = repo.getConnection();
            for (int i=0; i<100; i++) { // repeat a couple of times to increase the chance for thread collisions
                // send update
                final StringBuilder sb = new StringBuilder();
                sb.append("PREFIX ogc: <http://www.opengis.net/ont/geosparql#> ");
                sb.append("insert { ?s ogc:asWKT ?wkt } where { "
                        + "    ?s <http://lat> ?lat. ?s <http://lon> ?long. "
                        + "    bind(strdt(concat(\"POINT(\",str(?long),\" \",str(?lat),\")\"),ogc:wktLiteral) as ?wkt) }");
    
                final Update update = conn.prepareUpdate(QueryLanguage.SPARQL, sb.toString());
                update.execute();
            }
            conn.close();


        } catch (Exception e) {
            
            System.err.println("EXCEPTION: " + e.getMessage());
            throw(e);
            
        } finally {        

            repo.shutDown();
            
        }
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
            org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_INCLUDE_BUILTIN_DATATYPES, "false");

        // set GeoSpatial configuration: use a higher precision and range shifts; 
        // the test accounts for this higher precision (and assert that range shifts
        // actually do not harm the evaluation process)
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_DATATYPE_CONFIG + ".0",
           "{\"config\": "
           + "{ \"uri\": \"http://www.opengis.net/ont/geosparql#wktLiteral\", "
           + "\"literalSerializer\": \"org.embergraph.rdf.sparql.ast.eval.service.BLZG1943LiteralSerializer\",  "
           + "\"fields\": [ "
           + "{ \"valueType\": \"DOUBLE\", \"multiplier\": \"1000000\", \"serviceMapping\": \"LONGITUDE\" }, "
           + "{ \"valueType\": \"DOUBLE\", \"multiplier\": \"1000000\", \"serviceMapping\": \"LATITUDE\" } "
           + "]}}");   
        
        // make our dummy WKT datatype default to ease querying
        properties.setProperty(
            org.embergraph.rdf.store.AbstractLocalTripleStore.Options.GEO_SPATIAL_DEFAULT_DATATYPE,
            "http://www.opengis.net/ont/geosparql#wktLiteral");
        
        properties.setProperty(
           org.embergraph.rdf.store.AbstractLocalTripleStore.Options.VOCABULARY_CLASS,
           "org.embergraph.rdf.sparql.ast.eval.service.GeoSpatialTestVocabulary");
        
        return properties;

    }
}
