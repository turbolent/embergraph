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
import org.apache.log4j.Logger;
import org.embergraph.journal.BufferMode;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailTupleQuery;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

/**
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
public class TestMillisecondPrecisionForInlineDateTimes extends QuadsTestCase {

  private static final Logger log =
      Logger.getLogger(TestMillisecondPrecisionForInlineDateTimes.class);

  /**
   * Please set your database properties here, except for your journal file, please DO NOT SPECIFY A
   * JOURNAL FILE.
   */
  @Override
  public Properties getProperties() {

    final Properties props = super.getProperties();

    /*
     * For example, here is a set of five properties that turns off
     * inference, truth maintenance, and the free text index.
     */
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");
    props.setProperty(EmbergraphSail.Options.INLINE_DATE_TIMES, "true");
    props.setProperty(EmbergraphSail.Options.INLINE_DATE_TIMES_TIMEZONE, "GMT");

    // No disk file.
    props.setProperty(org.embergraph.journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    return props;
  }

  public TestMillisecondPrecisionForInlineDateTimes() {}

  public TestMillisecondPrecisionForInlineDateTimes(String arg0) {
    super(arg0);
  }

  public void testBug() throws Exception {

    /*
     * We use an in-memory Sesame store as our point of reference.  This
     * will supply the "correct" answer to the query (below).
     */
    Sail sesameSail = null;

    /*
     * The embergraph store, backed by a temporary journal file.
     */
    EmbergraphSail embergraphSail = null;

    try {

      sesameSail = new MemoryStore();

      embergraphSail = getSail();

      /*
       * Data file containing the data demonstrating your bug.
       */
      final String data = "TestMillisecondPrecisionForInlineDateTimes.ttl";
      final String baseURI = "";
      final RDFFormat format = RDFFormat.TURTLE;

      /*
       * Query(ies) demonstrating your bug.
       */
      final String query =
          //	          "prefix bd: <"+BD.NAMESPACE+"> " +
          "prefix rdf: <"
              + RDF.NAMESPACE
              + "> "
              + "prefix rdfs: <"
              + RDFS.NAMESPACE
              + "> "
              + "SELECT DISTINCT ?ar WHERE {\n"
              + "  FILTER(?datePub>=\"2011-03-08T08:48:27.003\"^^<http://www.w3.org/2001/XMLSchema#dateTime>).\n"
              + "  ?ar <os:prop/analysis/datePublished> ?datePub\n"
              + "} ORDER BY DESC(?datePub)";

      sesameSail.initialize();
      embergraphSail.initialize();

      final Repository sesameRepo = new SailRepository(sesameSail);
      final EmbergraphSailRepository embergraphRepo = new EmbergraphSailRepository(embergraphSail);

      { // load the data into the Sesame store
        final RepositoryConnection cxn = sesameRepo.getConnection();
        try {
          cxn.setAutoCommit(false);
          cxn.add(getClass().getResourceAsStream(data), baseURI, format);
          cxn.commit();
        } finally {
          cxn.close();
        }
      }

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

      final Collection<BindingSet> answer = new LinkedList<BindingSet>();

      /*
                   * Here is how you manually build the answer set, but please make
                   * sure you answer truly is correct if you choose to do it this way.

      //            answer.add(createBindingSet(
      //            		new BindingImpl("neType", vf.createURI("http://example/class/Location"))
      //            		));
      //            answer.add(createBindingSet(
      //            		new BindingImpl("neType", vf.createURI("http://example/class/Person"))
      //            		));

                   */

      /*
       * Run the problem query using the Sesame store to gather the
       * correct results.
       */
      {
        final RepositoryConnection cxn = sesameRepo.getConnection();
        try {
          final SailTupleQuery tupleQuery =
              (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
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
            if (log.isInfoEnabled()) log.info(bs);
          }
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
        final SailTupleQuery tupleQuery =
            (SailTupleQuery) cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);
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

      if (sesameSail != null) sesameSail.shutDown();

      if (embergraphSail != null) embergraphSail.__tearDownUnitTest();
    }
  }
}
