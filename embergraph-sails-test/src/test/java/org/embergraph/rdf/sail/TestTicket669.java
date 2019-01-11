/**
Copyright (C) SYSTAP, LLC DBA Blazegraph 2011.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.URI;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.vocab.NoVocabulary;

/**
 * Unit test template for use in submission of bugs.
 * <p>
 * This test case will delegate to an underlying backing store.  You can
 * specify this store via a JVM property as follows:
 * <code>-DtestClass=org.embergraph.rdf.sail.TestBigdataSailWithQuads</code>
 * <p>
 * There are three possible configurations for the testClass:
 * <ul>
 * <li>org.embergraph.rdf.sail.TestBigdataSailWithQuads (quads mode)</li>
 * <li>org.embergraph.rdf.sail.TestBigdataSailWithoutSids (triples mode)</li>
 * <li>org.embergraph.rdf.sail.TestBigdataSailWithSids (SIDs mode)</li>
 * </ul>
 * <p>
 * The default for triples and SIDs mode is for inference with truth maintenance
 * to be on.  If you would like to turn off inference, make sure to do so in
 * {@link #getProperties()}.
 *  
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestTicket669 extends QuadsTestCase {

    protected static final Logger log = Logger.getLogger(TestTicket669.class);

    /**
     * Please set your database properties here, except for your journal file,
     * please DO NOT SPECIFY A JOURNAL FILE. 
     */
    @Override
    public Properties getProperties() {
        
        Properties props = super.getProperties();

        /*
         * For example, here is a set of five properties that turns off
         * inference, truth maintenance, and the free text index.
         */
        props.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(BigdataSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
        props.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(BigdataSail.Options.JUSTIFY, "false");
        props.setProperty(BigdataSail.Options.TEXT_INDEX, "false");
        
        return props;
        
    }

    public TestTicket669() {
    }

    public TestTicket669(String arg0) {
        super(arg0);
    }
    
    public void testBug() throws Exception {
    	
    	/*
    	 * We use an in-memory Sesame store as our point of reference.  This
    	 * will supply the "correct" answer to the query (below).
    	 */
        final Sail sesameSail = new MemoryStore();
        
        /*
         * The bigdata store, backed by a temporary journal file.
         */
	  	final BigdataSail bigdataSail = getSail();
	  	
	  	/*
	  	 * Data file containing the data demonstrating your bug.
	  	 */
//	  	final String data = "data.ttl";
//	  	final String baseURI = "";
//	  	final RDFFormat format = RDFFormat.TURTLE;
	  	
	  	final String update =
  			"INSERT DATA " +
			"{ " +
			"<http://example.com/book1> a <http://example.com/Book> . " +
			"<http://example.com/book2> a <http://example.com/Book> . " +
			"<http://example.com/book3> a <http://example.com/Book> . " +
			"}";
	  	
	  	/*
	  	 * Query(ies) demonstrating your bug.
	  	 */
        final String nested =
			"SELECT ?s WHERE { " +
			"  SELECT ?s WHERE { ?s ?p ?o} LIMIT 1 " +
			"}";
        
        final String doubleNested =
        	"SELECT ?s WHERE { " +
			"  SELECT ?s WHERE { " +
			"    SELECT ?s WHERE { ?s ?p ?o} LIMIT 1 " +
			"  } " +
			"}";
        
        final String query = doubleNested;
	  	
	  	try {
	  	
	  		sesameSail.initialize();
	  		bigdataSail.initialize();
	  		
  			final Repository sesameRepo = new SailRepository(sesameSail);
  			final BigdataSailRepository bigdataRepo = new BigdataSailRepository(bigdataSail);
  			
  			final URI book1 = new URIImpl("http://example.com/book1");
  			final URI book2 = new URIImpl("http://example.com/book2");
  			final URI book3 = new URIImpl("http://example.com/book3");
  			final URI book = new URIImpl("http://example.com/book");
  			final Graph data = new GraphImpl();
  			data.add(book1, RDF.TYPE, book);
  			data.add(book2, RDF.TYPE, book);
  			data.add(book3, RDF.TYPE, book);
  			
	  		{ // load the data into the Sesame store
	  			
	  			final RepositoryConnection cxn = sesameRepo.getConnection();
	  			try {
	  				cxn.setAutoCommit(false);
//	  				cxn.add(getClass().getResourceAsStream(data), baseURI, format);
	  				cxn.add(data);
	  				cxn.commit();
	  			} finally {
	  				cxn.close();
	  			}
	  			
	  		}
	  		
	  		{ // load the data into the bigdata store
	  			
	  			final RepositoryConnection cxn = bigdataRepo.getConnection();
	  			try {
	  				cxn.setAutoCommit(false);
//	  				cxn.add(getClass().getResourceAsStream(data), baseURI, format);
	  				cxn.add(data);
	  				cxn.commit();
	  			} finally {
	  				cxn.close();
	  			}
	  			
	  		}
	  		
            final Collection<BindingSet> answer = new LinkedList<BindingSet>();
            
            /*
             * Here is how you manually build the answer set, but please make
             * sure you answer truly is correct if you choose to do it this way.
             */

//            answer.add(createBindingSet(
//            		new BindingImpl("neType", vf.createURI("http://example/class/Location"))
//            		));
//            answer.add(createBindingSet(
//            		new BindingImpl("neType", vf.createURI("http://example/class/Person"))
//            		));

	  		/*
	  		 * Run the problem query using the Sesame store to gather the
	  		 * correct results.
	  		 */
            { 
	  			final RepositoryConnection cxn = sesameRepo.getConnection();
	  			try {
		            final SailTupleQuery tupleQuery = (SailTupleQuery)
		                cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
		            tupleQuery.setIncludeInferred(false /* includeInferred */);
	            	final TupleQueryResult result = tupleQuery.evaluate();
	            	
	            	if (log.isInfoEnabled()) {
	            		log.info("sesame results:");
	            		if (!result.hasNext()) {
	            			log.info("no results.");
	            		}
	            	}
	            	
	                while (result.hasNext()) {
	                	final BindingSet bs = result.next();
	                	answer.add(bs);
		            	if (log.isInfoEnabled())
		            		log.info(bs);
		            }
	  			} finally {
	  				cxn.close();
	  			}
            }
                
            /*
             * Run the problem query using the bigdata store and then compare
             * the answer.
             */
            final RepositoryConnection cxn = bigdataRepo.getReadOnlyConnection();
  			try {
	            final SailTupleQuery tupleQuery = (SailTupleQuery)
	                cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
	            tupleQuery.setIncludeInferred(false /* includeInferred */);
            	
	            if (log.isInfoEnabled()) {
		            final TupleQueryResult result = tupleQuery.evaluate();
            		log.info("embergraph results:");
            		if (!result.hasNext()) {
            			log.info("no results.");
            		}
	                while (result.hasNext()) {
	            		log.info(result.next());
		            }
	            }
	            
	            final TupleQueryResult result = tupleQuery.evaluate();
            	compare(result, answer);
            	
  			} finally {
  				cxn.close();
  			}
          
        } finally {
        	bigdataSail.__tearDownUnitTest();
        	sesameSail.shutDown();
        }
    	
    }

}
