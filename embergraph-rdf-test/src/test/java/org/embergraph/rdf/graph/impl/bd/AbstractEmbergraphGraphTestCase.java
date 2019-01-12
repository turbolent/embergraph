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
package org.embergraph.rdf.graph.impl.bd;

import java.util.Properties;

import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;

import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.graph.AbstractGraphTestCase;
import org.embergraph.rdf.graph.util.IGraphFixtureFactory;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;

public class AbstractEmbergraphGraphTestCase extends AbstractGraphTestCase {

    public AbstractEmbergraphGraphTestCase() {
    }

    public AbstractEmbergraphGraphTestCase(String name) {
        super(name);
    }

    @Override
    protected IGraphFixtureFactory getGraphFixtureFactory() {
        final AbstractEmbergraphGraphTestCase testCase = this;
        return () -> new EmbergraphGraphFixture(testCase.getProperties());
    }

    protected Properties getProperties() {
        
        final Properties p = new Properties();

        p.setProperty(Journal.Options.BUFFER_MODE,
                BufferMode.MemStore.toString());
        
        /*
         * TODO Test both triples and quads.
         * 
         * Note: We need to use different data files for quads (trig). If we use
         * trig for a triples mode kb then we get errors (context bound, but not
         * quads mode).
         */
        p.setProperty(EmbergraphSail.Options.TRIPLES_MODE, "true");
//        p.setProperty(EmbergraphSail.Options.QUADS_MODE, "true");
        p.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");

        return p;
        
    }

    @Override
    protected EmbergraphGraphFixture getGraphFixture() {

        return (EmbergraphGraphFixture) super.getGraphFixture();
        
    }

    /**
     * A small FOAF data set relating some of the project contributors (triples
     * mode data).
     * 
     * @see {@value #smallGraph}
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    protected class SmallGraphProblem {

        /**
         * The data file.
         */
        static private final String smallGraph = "src/test/resources/graph/data/smallGraph.ttl";

        private final EmbergraphURI rdfType, foafKnows, foafPerson, mike, bryan,
                martyn;

        public SmallGraphProblem() throws Exception {

            getGraphFixture().loadGraph(smallGraph);
            
            final EmbergraphSailConnection conn = getGraphFixture().getSail().getReadOnlyConnection();
            
            try {
                    
                final ValueFactory vf = conn.getEmbergraphSail().getValueFactory();
    
            rdfType = (EmbergraphURI) vf.createURI(RDF.TYPE.stringValue());
    
            foafKnows = (EmbergraphURI) vf
                    .createURI("http://xmlns.com/foaf/0.1/knows");
    
            foafPerson = (EmbergraphURI) vf
                    .createURI("http://xmlns.com/foaf/0.1/Person");
    
            mike = (EmbergraphURI) vf.createURI("http://www.embergraph.org/Mike");
    
            bryan = (EmbergraphURI) vf.createURI("http://www.embergraph.org/Bryan");
    
            martyn = (EmbergraphURI) vf.createURI("http://www.embergraph.org/Martyn");
    
            final EmbergraphValue[] terms = new EmbergraphValue[] { rdfType,
                    foafKnows, foafPerson, mike, bryan, martyn };
    
            // batch resolve existing IVs.
                conn.getTripleStore().getLexiconRelation()
                    .addTerms(terms, terms.length, true/* readOnly */);
    
            for (EmbergraphValue v : terms) {
                if (v.getIV() == null)
                    fail("Did not resolve: " + v);
            }

            } finally {
                
                conn.close();
                
            }

        }

        @SuppressWarnings("rawtypes")
        public IV getRdfType() {
            return rdfType.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getFoafKnows() {
            return foafKnows.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getFoafPerson() {
            return foafPerson.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getMike() {
            return mike.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getBryan() {
            return bryan.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getMartyn() {
            return martyn.getIV();
        }

    }

    /**
     * Load and setup the {@link SmallGraphProblem}.
     */
    protected SmallGraphProblem setupSmallGraphProblem() throws Exception {

        return new SmallGraphProblem();

    }

    /**
     * A small weighted graph data set.
     * 
     * @see {@value #smallWeightedGraph}
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    protected class SmallWeightedGraphProblem {

        /**
         * The data file.
         */
        static private final String smallWeightedGraph = "src/test/resources/graph/data/smallWeightedGraph.ttlx";

        private final EmbergraphURI foafKnows, linkWeight, v1, v2, v3, v4, v5;

        public SmallWeightedGraphProblem() throws Exception {

            getGraphFixture().loadGraph(smallWeightedGraph);

            final EmbergraphSailConnection conn = getGraphFixture().getSail().getReadOnlyConnection();
            
            try {
                    
                final ValueFactory vf = conn.getEmbergraphSail().getValueFactory();
    
            foafKnows = (EmbergraphURI) vf
                    .createURI("http://xmlns.com/foaf/0.1/knows");
            
            linkWeight = (EmbergraphURI) vf
                    .createURI("http://www.embergraph.org/weight");
    
            v1 = (EmbergraphURI) vf.createURI("http://www.embergraph.org/1");
            v2 = (EmbergraphURI) vf.createURI("http://www.embergraph.org/2");
            v3 = (EmbergraphURI) vf.createURI("http://www.embergraph.org/3");
            v4 = (EmbergraphURI) vf.createURI("http://www.embergraph.org/4");
            v5 = (EmbergraphURI) vf.createURI("http://www.embergraph.org/5");
    
            final EmbergraphValue[] terms = new EmbergraphValue[] { foafKnows,
                    linkWeight, v1, v2, v3, v4, v5 };
    
            // batch resolve existing IVs.
                conn.getTripleStore().getLexiconRelation()
                    .addTerms(terms, terms.length, true/* readOnly */);
    
            for (EmbergraphValue v : terms) {
                if (v.getIV() == null)
                    fail("Did not resolve: " + v);
            }
                
            } finally {
                
                conn.close();
                
            }

        }

        @SuppressWarnings("rawtypes")
        public IV getFoafKnows() {
            return foafKnows.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getLinkWeight() {
            return linkWeight.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getV1() {
            return v1.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getV2() {
            return v2.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getV3() {
            return v3.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getV4() {
            return v4.getIV();
        }

        @SuppressWarnings("rawtypes")
        public IV getV5() {
            return v5.getIV();
        }


    }

    /**
     * Load and setup the {@link SmallWeightedGraphProblem}.
     */
    protected SmallWeightedGraphProblem setupSmallWeightedGraphProblem() throws Exception {

        return new SmallWeightedGraphProblem();

    }

}
