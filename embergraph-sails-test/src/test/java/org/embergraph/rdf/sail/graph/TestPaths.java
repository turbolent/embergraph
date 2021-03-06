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

package org.embergraph.rdf.sail.graph;

import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.ProxyEmbergraphSailTestCase;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.rio.RDFFormat;

public class TestPaths extends ProxyEmbergraphSailTestCase {

  protected static final Logger log = Logger.getLogger(TestPaths.class);

  protected static final boolean INFO = log.isInfoEnabled();

  @Override
  public Properties getProperties() {

    Properties props = super.getProperties();

    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /** */
  public TestPaths() {}

  /** @param arg0 */
  public TestPaths(String arg0) {
    super(arg0);
  }

  protected void load(final EmbergraphSailRepositoryConnection cxn, final String resource)
      throws Exception {

    final InputStream is = getClass().getResourceAsStream(resource);

    cxn.add(is, "", RDFFormat.TURTLE);
  }

  protected InputStream open(final String resource) {

    return getClass().getResourceAsStream(resource);
  }

  //    public void testSimpleBFS() throws Exception {
  //
  //    	final EmbergraphSail sail = getSail();
  //    	sail.initialize();
  //    	final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
  //
  //    	final EmbergraphSailRepositoryConnection cxn = repo.getConnection();
  //        cxn.setAutoCommit(false);
  //
  //        try {
  //
  //        	cxn.add(open("paths1.ttl"), "", RDFFormat.TURTLE);
  //        	cxn.commit();
  //
  //        	log.trace("\n"+sail.getDatabase().dumpStore());
  //
  //        	final String query = IOUtils.toString(open("paths1.rq"));
  //
  //        	log.trace("\n"+query);
  //
  //        	final TupleQuery tqr = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
  //
  //        	final TupleQueryResult result = tqr.evaluate();
  //
  //        	while (result.hasNext()) {
  //
  //        		log.trace(result.next());
  //
  //        	}
  //
  //        	result.close();
  //
  //        } finally {
  //            cxn.close();
  //            sail.__tearDownUnitTest();
  //        }
  //
  //    }
  //
  public void testSimpleSSSP() throws Exception {

    final EmbergraphSail sail = getSail();
    sail.initialize();
    final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

    final EmbergraphSailRepositoryConnection cxn = repo.getConnection();
    cxn.setAutoCommit(false);

    try {

      cxn.add(open("sssp.ttl"), "", RDFFormat.TURTLE);
      cxn.commit();

      log.trace("\n" + cxn.getTripleStore().dumpStore());

      final String query = IOUtils.toString(open("sssp.rq"));

      log.trace("\n" + query);

      final TupleQuery tqr = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);

      final TupleQueryResult result = tqr.evaluate();

      while (result.hasNext()) {

        log.trace(result.next());
      }

      result.close();

    } finally {
      cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  //    public void testPaths() throws Exception {
  //
  //    	final EmbergraphSail sail = getSail();
  //    	sail.initialize();
  //    	final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
  //
  //    	final EmbergraphSailRepositoryConnection cxn = repo.getConnection();
  //        cxn.setAutoCommit(false);
  //
  //        try {
  //
  //        	cxn.add(open("paths4.ttl"), "", RDFFormat.TURTLE);
  //        	cxn.commit();
  //
  //        	log.trace("\n"+sail.getDatabase().dumpStore());
  //
  //        	final String query = IOUtils.toString(open("paths4.rq"));
  //
  //        	log.trace("\n"+query);
  //
  //        	final TupleQuery tqr = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
  //
  //        	final TupleQueryResult result = tqr.evaluate();
  //
  //        	while (result.hasNext()) {
  //
  //        		log.trace(result.next());
  //
  //        	}
  //
  //        	result.close();
  //
  //        } finally {
  //            cxn.close();
  //            sail.__tearDownUnitTest();
  //        }
  //
  //    }

}
