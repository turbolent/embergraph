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

import java.util.Properties;

import org.apache.log4j.Logger;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailGraphQuery;
import org.openrdf.rio.RDFFormat;

import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.store.BDS;
import org.embergraph.rdf.vocab.NoVocabulary;

/**
 * Unit test template for use in submission of bugs.
 * <p>
 * This test case will delegate to an underlying backing store.  You can
 * specify this store via a JVM property as follows:
 * <code>-DtestClass=org.embergraph.rdf.sail.TestEmbergraphSailWithQuads</code>
 * <p>
 * There are three possible configurations for the testClass:
 * <ul>
 * <li>org.embergraph.rdf.sail.TestEmbergraphSailWithQuads (quads mode)</li>
 * <li>org.embergraph.rdf.sail.TestEmbergraphSailWithoutSids (triples mode)</li>
 * <li>org.embergraph.rdf.sail.TestEmbergraphSailWithSids (SIDs mode)</li>
 * </ul>
 * <p>
 * The default for triples and SIDs mode is for inference with truth maintenance
 * to be on.  If you would like to turn off inference, make sure to do so in
 * {@link #getProperties()}.
 *  
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestTicket581 extends QuadsTestCase {

    protected static final Logger log = Logger.getLogger(TestTicket581.class);

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
        props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
        props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
        props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
        props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "true");
        
        return props;
        
    }

    public TestTicket581() {
    }

    public TestTicket581(String arg0) {
        super(arg0);
    }
    
    public void testBug() throws Exception {
    	
        /*
         * The embergraph store, backed by a temporary journal file.
         */
	  	final EmbergraphSail embergraphSail = getSail();
	  	
	  	/*
	  	 * Data file containing the data demonstrating your bug.
	  	 */
	  	final String data = "fulltextsearchwithsubselect.ttl";
	  	final String baseURI = "";
	  	final RDFFormat format = RDFFormat.TURTLE;
	  	
	  	/*
	  	 * Query(ies) demonstrating your bug.
	  	 */
        final String query =
        	"CONSTRUCT { ?object ?p ?o . } " +
//            "WITH { " +
//			"		SELECT DISTINCT ?object WHERE { " +
//			"			?object ?sp ?so . ?so <"+BD.SEARCH+"> \"music\" . " +
//			"		} " +
//			"} as %set1 " +
			"WHERE { " +
//			"	?so <"+BD.SEARCH+"> \"music\" . " +
			"   service <"+BDS.SEARCH+"> { " +
			"	  ?so <"+BDS.SEARCH+"> \"music\" . " +
            "   } " +
			"	?object ?p ?so . " +
			"	?object ?p ?o . " +
			"}"; 
//			"	{ " +
//			"		SELECT DISTINCT ?object WHERE { " +
//			"			?object ?sp ?so . ?so <"+BD.SEARCH+"> \"music\" . " +
//			"		} " +
//			"	} " +
////			"   include %set1 . " +
//			"	OPTIONAL { ?object ?p ?o . } " + 
//			"} ORDER BY ?object ?p"
//			;
	  	
	  	try {
	  	
	  		embergraphSail.initialize();
	  		
  			final EmbergraphSailRepository embergraphRepo = new EmbergraphSailRepository(embergraphSail);
  			
	  		{ // load the data into the embergraph store
	  			
	  			final RepositoryConnection cxn = embergraphRepo.getConnection();
	  			try {
	  				cxn.setAutoCommit(false);
	  				cxn.add(getClass().getResourceAsStream(data), baseURI, format);
	  				cxn.commit();
	  			} finally {
	  				cxn.close();
	  			}
	  			
	  		}
	  		
            /*
             * Run the problem query using the embergraph store and then compare
             * the answer.
             */
            final RepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
  			try {
	            final SailGraphQuery graphQuery = (SailGraphQuery)
	                cxn.prepareGraphQuery(QueryLanguage.SPARQL, query);
            	
	            if (log.isInfoEnabled()) {
		            final GraphQueryResult result = graphQuery.evaluate();
            		log.info("embergraph results:");
            		if (!result.hasNext()) {
            			log.info("no results.");
            		}
	                while (result.hasNext()) {
	            		log.info(result.next());
		            }
	            }
	            
  			} finally {
  				cxn.close();
  			}
          
        } finally {
        	embergraphSail.__tearDownUnitTest();
        }
    	
    }

}
