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
 * Created on Sep 4, 2008
 */

package org.embergraph.rdf.sail;

import java.util.Properties;
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.embergraph.rdf.sail.EmbergraphSail.Options;
import org.embergraph.rdf.sail.sparql.TestVerifyAggregates;
import org.embergraph.rdf.sail.tck.EmbergraphComplexSparqlQueryTest;
import org.embergraph.rdf.sail.tck.EmbergraphConnectionTest;
import org.embergraph.rdf.sail.tck.EmbergraphSPARQLUpdateConformanceTest;
import org.embergraph.rdf.sail.tck.EmbergraphSparqlFullRWTxTest;
import org.embergraph.rdf.sail.tck.EmbergraphSparqlTest;
import org.embergraph.rdf.sail.tck.EmbergraphStoreTest;

/*
* Test suite for the {@link EmbergraphSail} with quads enabled. The provenance mode is disabled.
 * Inference is disabled. This version of the test suite uses the pipeline join algorithm.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestEmbergraphSailWithQuads extends AbstractEmbergraphSailTestCase {

  /** */
  public TestEmbergraphSailWithQuads() {}

  public TestEmbergraphSailWithQuads(final String name) {

    super(name);
  }

  public static Test suite() {

    final TestEmbergraphSailWithQuads delegate =
        new TestEmbergraphSailWithQuads(); // !!!! THIS CLASS !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    final ProxyTestSuite suite = new ProxyTestSuite(delegate, "SAIL with Quads (pipeline joins)");

    // test rewrite of RDF Value => EmbergraphValue for binding set and tuple expr.
    suite.addTestSuite(TestEmbergraphValueReplacer.class);

    // test pruning of variables not required for downstream processing.
    suite.addTestSuite(TestPruneBindingSets.class);

    // misc named graph API stuff.
    suite.addTestSuite(TestQuadsAPI.class);

    // Note: Ported to data driven test.
    //        // SPARQL named graphs tests.
    //        suite.addTestSuite(TestNamedGraphs.class);

    // test suite for optionals handling (left joins).
    suite.addTestSuite(TestOptionals.class);

    // test of the search magic predicate
    suite.addTestSuite(TestSearchQuery.class);

    // test of high-level query on a graph with statements about statements.
    suite.addTestSuite(TestProvenanceQuery.class);

    suite.addTestSuite(TestOrderBy.class);

    suite.addTestSuite(TestUnions.class);

    suite.addTestSuite(TestMultiGraphs.class);

    suite.addTestSuite(TestInlineValues.class);

    // Validation logic for aggregation operators.
    suite.addTestSuite(TestVerifyAggregates.class);

    suite.addTestSuite(TestConcurrentKBCreate.TestWithGroupCommit.class);
    suite.addTestSuite(TestConcurrentKBCreate.TestWithoutGroupCommit.class);

    suite.addTestSuite(TestTxCreate.class);
    suite.addTestSuite(TestCnxnCreate.class);

    suite.addTestSuite(TestChangeSets.class);

    // test suite for the history index.
    suite.addTestSuite(TestHistoryIndex.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestRollbacks.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestRollbacksTx.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestMROWTransactionsNoHistory.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestMROWTransactionsWithHistory.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestMillisecondPrecisionForInlineDateTimes.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket275.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket276.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket348.class);
    //        suite.addTestSuite(org.embergraph.rdf.sail.TestTicket352.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket353.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket355.class);
    //      suite.addTestSuite(org.embergraph.rdf.sail.TestTicket361.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket422.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1747.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1753.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1755.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1785.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1788.class);

    suite.addTestSuite(org.embergraph.rdf.sail.DavidsTestBOps.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestLexJoinOps.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestMaterialization.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket632.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket669.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket1889.class);
    suite.addTestSuite(org.embergraph.rdf.sail.TestTicket4249.class);

    suite.addTestSuite(org.embergraph.rdf.sail.TestNoExceptions.class);

    // The Sesame TCK, including the SPARQL test suite.
    {
      final TestSuite tckSuite = new TestSuite("Sesame 2.x TCK");

      // Sesame Sail test.
      tckSuite.addTestSuite(EmbergraphStoreTest.class);

      // Sesame SailConnection test.
      tckSuite.addTestSuite(EmbergraphConnectionTest.class);

      try {

      /*
       * suite() will call suiteLTSWithPipelineJoins() and then
         * filter out the dataset tests, which we don't need right now
         */
        //                tckSuite.addTest(EmbergraphSparqlTest.suiteLTSWithPipelineJoins());
        tckSuite.addTest(EmbergraphSparqlTest.suite()); // w/ unisolated connection.
        tckSuite.addTest(EmbergraphSparqlFullRWTxTest.suite()); // w/ full read/write tx.

      } catch (Exception ex) {

        throw new RuntimeException(ex);
      }

      suite.addTest(tckSuite);

      /*
       * SPARQL 1.1 test suite for things which do not fit in with the
       * manifest test design.
       *
       * FIXME This should be run for full r/w tx, the embedded federation
       * and scale-out, not just quads.
       */
      tckSuite.addTestSuite(EmbergraphComplexSparqlQueryTest.class);

      /*
       * Note: The SPARQL 1.1 update test suite is run from
       * org.embergraph.rdf.sparql.ast.eval.update.TestAll.
       */
      //            tckSuite.addTestSuite(EmbergraphSPARQLUpdateTest.class);
      try {
        tckSuite.addTest(EmbergraphSPARQLUpdateConformanceTest.suite());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return suite;
  }

  private Properties properties = null;

  @Override
  protected void tearDown(final ProxyEmbergraphSailTestCase testCase) throws Exception {

    super.tearDown(testCase);

    properties = null;
  }

  @Override
  protected EmbergraphSail getSail(final Properties properties) {

    this.properties = properties;

    return new EmbergraphSail(properties);
  }

  @Override
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());
    /*
            properties.setProperty(Options.STATEMENT_IDENTIFIERS, "false");

            properties.setProperty(Options.QUADS, "true");

            properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());
    */
    properties.setProperty(Options.QUADS_MODE, "true");

    properties.setProperty(Options.TRUTH_MAINTENANCE, "false");

    //        properties.setProperty(AbstractResource.Options.NESTED_SUBQUERY, "false");

    return properties;
  }

  @Override
  protected EmbergraphSail reopenSail(final EmbergraphSail sail) {

    //        final Properties properties = sail.getProperties();

    if (sail.isOpen()) {

      try {

        sail.shutDown();

      } catch (Exception ex) {

        throw new RuntimeException(ex);
      }
    }

    return getSail(properties);
  }
}
