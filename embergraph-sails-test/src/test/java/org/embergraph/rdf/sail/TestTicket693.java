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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.rio.RDFFormat;

/*
 * Unit test template for use in submission of bugs.
 *
 * <p>This test case will delegate to an underlying backing store. You can specify this store via a
 * JVM property as follows: <code>-DtestClass=org.embergraph.rdf.sail.TestEmbergraphSailWithQuads
 * </code>
 *
 * <p>There are three possible configurations for the testClass:
 *
 * <ul>
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithQuads (quads mode)
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithoutSids (triples mode)
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithSids (SIDs mode)
 * </ul>
 *
 * <p>The default for triples and SIDs mode is for inference with truth maintenance to be on. If you
 * would like to turn off inference, make sure to do so in {@link #getProperties()}.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestTicket693 extends QuadsTestCase {

  protected static final Logger log = Logger.getLogger(TestTicket693.class);

  /*
   * Please set your database properties here, except for your journal file, please DO NOT SPECIFY A
   * JOURNAL FILE.
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
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  public TestTicket693() {}

  public TestTicket693(String arg0) {
    super(arg0);
  }

  public void testBug1() throws Exception {

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    final EmbergraphSail embergraphSail = getSail();

    /*
     * Data file containing the data demonstrating your bug.
     */
    final String data = "property_paths.owl";
    final String baseURI = "";
    final RDFFormat format = RDFFormat.RDFXML;

    //	  	final String update =
    //  			"INSERT DATA " +
    //			"{ " +
    //			"<http://example.com/book1> a <http://example.com/Book> . " +
    //			"<http://example.com/book2> a <http://example.com/Book> . " +
    //			"<http://example.com/book3> a <http://example.com/Book> . " +
    //			"}";
    //
    /*
    embergraph results:
    [s=http://example.org/A;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Class]
    [s=http://example.org/B;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Class]
    [s=http://example.org/C;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Class]
    [s=http://example.org/D;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Class]
    [s=http://example.org/E;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Class]
    [s=http://example.org/B;p=http://www.w3.org/2000/01/rdf-schema#subClassOf;o=http://example.org/A]
    [s=http://example.org/C;p=http://www.w3.org/2000/01/rdf-schema#subClassOf;o=http://example.org/B]
    [s=http://example.org/D;p=http://www.w3.org/2000/01/rdf-schema#subClassOf;o=http://example.org/C]
    [s=http://example.org/E;p=http://www.w3.org/2000/01/rdf-schema#subClassOf;o=http://example.org/D]
    [s=http://www.semanticweb.org/ontologies/2013/5/untitled-ontology-287;p=http://www.w3.org/1999/02/22-rdf-syntax-ns#type;o=http://www.w3.org/2002/07/owl#Ontology]
    10 results.
    	  	 */

    /*
     * Query(ies) demonstrating your bug.
     */
    final String withPath = IOUtils.toString(getClass().getResourceAsStream("ticket693.txt"));

    final String dumpStore = "select * where { ?s ?p ?o . }";

    try {

      embergraphSail.initialize();

      final EmbergraphSailRepository embergraphRepo = new EmbergraphSailRepository(embergraphSail);

      { // load the data into the embergraph store
        final RepositoryConnection cxn = embergraphRepo.getConnection();
        try {
          cxn.setAutoCommit(false);
          cxn.add(getClass().getResourceAsStream(data), baseURI, format);
          //	  				cxn.add(data);
          cxn.commit();
        } finally {
          cxn.close();
        }
      }

      {
        final Collection<BindingSet> answer = new LinkedList<>();
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/B"))));

        final String query = "SELECT * WHERE { ?sub rdfs:subClassOf <http://example.org/A> . } ";

        if (log.isInfoEnabled()) {
          log.info("running query:\n" + query);
        }

        /*
         * Run the problem query using the embergraph store and then compare
         * the answer.
         */
        final RepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
        try {

          final SailTupleQuery tupleQuery =
              (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
          tupleQuery.setIncludeInferred(false /* includeInferred */);

          final TupleQueryResult result = tupleQuery.evaluate();
          compare(result, answer);

        } finally {
          cxn.close();
        }
      }

      {
        final Collection<BindingSet> answer = new LinkedList<>();
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/A"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/B"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/C"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/D"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/E"))));

        final String query = "SELECT * WHERE { ?sub rdfs:subClassOf* <http://example.org/A> . } ";

        if (log.isInfoEnabled()) {
          log.info("running query:\n" + query);
        }

        /*
         * Run the problem query using the embergraph store and then compare
         * the answer.
         */
        final RepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
        try {

          final SailTupleQuery tupleQuery =
              (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
          tupleQuery.setIncludeInferred(false /* includeInferred */);

          final TupleQueryResult result = tupleQuery.evaluate();
          compare(result, answer);

        } finally {
          cxn.close();
        }
      }

      {
        final Collection<BindingSet> answer = new LinkedList<>();
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/A"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/B"))));

        final String query = "SELECT * WHERE { ?sub rdfs:subClassOf? <http://example.org/A> . } ";

        if (log.isInfoEnabled()) {
          log.info("running query:\n" + query);
        }

        /*
         * Run the problem query using the embergraph store and then compare
         * the answer.
         */
        final RepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
        try {

          final SailTupleQuery tupleQuery =
              (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
          tupleQuery.setIncludeInferred(false /* includeInferred */);

          final TupleQueryResult result = tupleQuery.evaluate();
          compare(result, answer);

        } finally {
          cxn.close();
        }
      }

      {
        final Collection<BindingSet> answer = new LinkedList<>();
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/B"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/C"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/D"))));
        answer.add(createBindingSet(new BindingImpl("sub", new URIImpl("http://example.org/E"))));

        final String query = "SELECT * WHERE { ?sub rdfs:subClassOf+ <http://example.org/A> . } ";

        if (log.isInfoEnabled()) {
          log.info("running query:\n" + query);
        }

        /*
         * Run the problem query using the embergraph store and then compare
         * the answer.
         */
        final RepositoryConnection cxn = embergraphRepo.getReadOnlyConnection();
        try {

          final SailTupleQuery tupleQuery =
              (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
          tupleQuery.setIncludeInferred(false /* includeInferred */);

          final TupleQueryResult result = tupleQuery.evaluate();
          compare(result, answer);

        } finally {
          cxn.close();
        }
      }

    } finally {

      embergraphSail.__tearDownUnitTest();
    }
  }
}
