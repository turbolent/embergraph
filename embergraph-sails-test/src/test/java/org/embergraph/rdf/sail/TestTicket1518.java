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
 * Created on Sep 16, 2009
 */

package org.embergraph.rdf.sail;

import java.util.Properties;

import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;

import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sparql.ast.QuadsOperationInTriplesModeException;
import org.embergraph.rdf.vocab.NoVocabulary;
import com.ibm.icu.impl.Assert;

/**
 * Test suite for ticket #1518: tests for quad-mode SPARQL features with triplestore not supporting quads
 */
public class TestTicket1518 extends ProxyEmbergraphSailTestCase {

    public Properties getTriplesNoInference() {
        
        Properties props = super.getProperties();
        
        props.setProperty(EmbergraphSail.Options.QUADS, "false");
        props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");
        props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
        props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
        props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");
        
        return props;
        
    }
    
    /**
     * 
     */
    public TestTicket1518() {
    }

    /**
     * @param arg0
     */
    public TestTicket1518(String arg0) {
        super(arg0);
    }
    
    /**
     * Quads in SPARQL update data block (INSERT clause). 
     */
    
    public void testQuadsInSPARQLInsertBlock() throws Exception {

        EmbergraphSailRepositoryConnection cxn = null;

        final EmbergraphSail sail = getSail(getTriplesNoInference());

        try {

            sail.initialize();
            
            final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
            
            cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();
            
            cxn.begin();
            
            final String update = "INSERT DATA  {" + // 
            		
            		"GRAPH <http://graphA> {<s:s1> <p:p1> <o:o1> .} " +
       			
    			"} ";

            final Update  q = cxn.prepareUpdate(QueryLanguage.SPARQL, update);
            try {
				
				q.execute();
				
				Assert.fail("");
				
			} catch (QuadsOperationInTriplesModeException ex) {
				
				String expectedMessage = "Quads in SPARQL update data block are not supported " + 
										"in triples mode.";
				
				assertEquals(expectedMessage, ex.getMessage());
				
			}
			
        } finally {
        	
        	if (cxn != null)
                cxn.close();
            sail.__tearDownUnitTest();
        }
    }
    
    /**
     * Quads in SPARQL update data block (DELETE clause).
     */
    
    public void testQuadsInSPARQLDeleteDataBlock() throws Exception {

        EmbergraphSailRepositoryConnection cxn = null;

        final EmbergraphSail sail = getSail(getTriplesNoInference());

        try {

            sail.initialize();
            
            final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
            
            cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();
            
            cxn.begin();
         
            final String update = "DELETE DATA  {" + // 
            
						"GRAPH <http://graphA> {<s:s1> <p:p1> <o:o1> .} " +
       			
    			"} ";

            final Update  q = cxn.prepareUpdate(QueryLanguage.SPARQL, update);
            try {
				
				q.execute();
				
				Assert.fail("");
				
			} catch (QuadsOperationInTriplesModeException ex) {
				
				String expectedMessage = "Quads in SPARQL update data block are not supported " + 
										"in triples mode.";
				
				assertEquals(expectedMessage, ex.getMessage());
				
			}
			
        } finally {
        	
        	if (cxn != null)
                cxn.close();
            sail.__tearDownUnitTest();
        }
    }
    
    /**
     * Test named graph referenced through WITH clause.
     */
    
    public void testNamedGraphReferencedThroughWITHClause() throws Exception {

        EmbergraphSailRepositoryConnection cxn = null;

        final EmbergraphSail sail = getSail(getTriplesNoInference());

        try {

            sail.initialize();
            
            final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
            
            cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();
            
            cxn.begin();
           
            final String update = "with <c:graphA> " +
            					 "INSERT { <s:s1> <p:p1> <o:o1> . } where {}";

            final Update  q = cxn.prepareUpdate(QueryLanguage.SPARQL, update);
            try {
				
				q.execute();
				
				Assert.fail("");
				
			} catch (QuadsOperationInTriplesModeException ex) {
				
				String expectedMessage = "Use of WITH and GRAPH constructs in query body is not supported " +
														"in triples mode.";
				
				assertEquals(expectedMessage, ex.getMessage());
				
			}
  			
			
        } finally {
            if (cxn != null)
                cxn.close();
            sail.__tearDownUnitTest();
        }
    }
    
    /**
     * Test using of GRAPH construct in query body.
     */
    
    public void testGRAPHConstructInQueryBody() throws Exception {

        EmbergraphSailRepositoryConnection cxn = null;

        final EmbergraphSail sail = getSail(getTriplesNoInference());

        try {

            sail.initialize();
            
            final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
            
            cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();
            
            cxn.begin();
            
            
           
            final String query = "SELECT ?g ?s ?p ?o" +
								"WHERE { GRAPH ?g { ?s ?p ?o }}";
								

            final TupleQuery q = cxn.prepareTupleQuery(QueryLanguage.SPARQL,
						query);
			
            try {
				
				final TupleQueryResult tqr = q.evaluate();
				
				Assert.fail("");
				
			} catch (QuadsOperationInTriplesModeException ex) {
				
				String expectedMessage = "Use of WITH and GRAPH constructs in query body is not supported " +
                        					"in triples mode.";
				
				assertEquals(expectedMessage, ex.getMessage());
				
			}
  			
			
        } finally {
            if (cxn != null)
                cxn.close();
            sail.__tearDownUnitTest();
        }
    }

}
