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
package org.embergraph.rdf.sail.tck;

import java.util.Properties;
import java.util.UUID;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.embergraph.EmbergraphStatics;
import org.embergraph.btree.keys.CollatorEnum;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.StrengthEnum;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.Options;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.store.ScaleOutTripleStore;
import org.embergraph.service.AbstractDistributedFederation;
import org.embergraph.service.AbstractScaleOutClient;
import org.embergraph.service.ScaleOutClientFactory;
import org.openrdf.query.Dataset;
import org.openrdf.query.parser.sparql.manifest.ManifestTest;
import org.openrdf.query.parser.sparql.manifest.SPARQL11ManifestTest;
import org.openrdf.query.parser.sparql.manifest.SPARQLQueryTest;
import org.openrdf.repository.Repository;
import org.openrdf.repository.dataset.DatasetRepository;

/*
* Runs the SPARQL test suite against a JiniFederation, which must be already deployed. Each test in
 * the suite is run against a distinct quad store in its own embergraph namespace.
 *
 * <p>To run this test suite, you need to have a deployed federation. You then specify the
 * configuration file for that deployed federation. If sysstat is in a non-default location, then it
 * is convenient (but not necessary) to also specify its path. For example:
 *
 * <pre>
 * -Dembergraph.configuration=/nas/embergraph/benchmark/config/embergraphStandalone.config
 * -Dorg.embergraph.counters.linux.sysstat.path=/usr/local/bin
 * </pre>
 *
 * @author <a href="mailto:dmacgbr@users.sourceforge.net">David MacMillan</a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class EmbergraphFederationSparqlTest extends SPARQLQueryTest {

  /*
   * Skip the dataset tests for now until we can figure out what is wrong with them.
   *
   * <p>FIXME Fix the dataset tests. There is some problem in how the data to be loaded into the
   * fixture is being resolved in these tests.
   */
  public static Test suite() throws Exception {

    return suite(true /*hideDatasetTests*/);
  }

  public static Test suite(final boolean hideDatasetTests) throws Exception {

    TestSuite suite1 = fullSuite();

    // Only run the specified tests?
    if (!EmbergraphSparqlTest.testURIs.isEmpty()) {
      final TestSuite suite = new TestSuite();
      for (String s : EmbergraphSparqlTest.testURIs) {
        suite.addTest(EmbergraphSparqlTest.getSingleTest(suite1, s));
      }
      return suite;
    }

    if (hideDatasetTests) suite1 = EmbergraphSparqlTest.filterOutTests(suite1, "dataset");

    suite1 = EmbergraphSparqlTest.filterOutTests(suite1, EmbergraphSparqlTest.badTests);

    if (!EmbergraphStatics.runKnownBadTests)
      suite1 = EmbergraphSparqlTest.filterOutTests(suite1, EmbergraphSparqlTest.knownBadTests);

    //        suite1 = EmbergraphSparqlTest.filterOutTests(suite1, "property-paths");

    /*
     * BSBM BI use case query 5
     *
     * <p>bsbm-bi-q5
     *
     * <p>We were having a problem with this query which I finally tracked this down to an error in
     * the logic to decide on a merge join. The story is documented at the trac issue below.
     * However, even after all that the predicted result for openrdf differs at the 4th decimal
     * place. I have therefore filtered out this test from the openrdf TCK.
     *
     * <pre>
     * Missing bindings:
     * [product=http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromProducer2/Product63;
     * nrOfReviews="3"^^<http://www.w3.org/2001/XMLSchema#integer>;
     * avgPrice="4207.426"^^<http://www.w3.org/2001/XMLSchema#float>;
     * country=http://downlode.org/rdf/iso-3166/countries#RU]
     * ====================================================
     * Unexpected bindings:
     * [country=http://downlode.org/rdf/iso-3166/countries#RU;
     * product=http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromProducer2/Product63;
     * nrOfReviews="3"^^<http://www.w3.org/2001/XMLSchema#integer>;
     * avgPrice="4207.4263"^^<http://www.w3.org/2001/XMLSchema#float>]
     * </pre>
     *
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/534#comment:2">BSBM BI Q5
     *     Error when using MERGE JOIN </a>
     */
    suite1 = EmbergraphSparqlTest.filterOutTests(suite1, "bsbm");

    return suite1;
  }

  /** Return the entire test suite. */
  public static TestSuite fullSuite() throws Exception {

    final SPARQLQueryTest.Factory factory =
        new SPARQLQueryTest.Factory() {

          public SPARQLQueryTest createSPARQLQueryTest(
              String URI,
              String name,
              String query,
              String results,
              Dataset dataSet,
              boolean laxCardinality) {

            return createSPARQLQueryTest(
                URI, name, query, results, dataSet, laxCardinality, true /* checkOrder */);
          }

          public SPARQLQueryTest createSPARQLQueryTest(
              String URI,
              String name,
              String query,
              String results,
              Dataset dataSet,
              boolean laxCardinality,
              boolean checkOrder) {

            return new EmbergraphFederationSparqlTest(
                URI, name, query, results, dataSet, laxCardinality, checkOrder);
          }
        };

    final TestSuite suite = new TestSuite();

    // SPARQL 1.0
    suite.addTest(ManifestTest.suite(factory));

    // SPARQL 1.1
    suite.addTest(SPARQL11ManifestTest.suite(factory, true, true, false));

    return suite;
  }

  public EmbergraphFederationSparqlTest(
      String URI,
      String name,
      String query,
      String results,
      Dataset dataSet,
      boolean laxCardinality,
      boolean checkOrder) {

    super(URI, name, query, results, dataSet, laxCardinality, checkOrder);
  }

  @Override
  public void runTest() throws Exception {
    _logger.info(String.format(">>>>> Running test: %s", testURI));
    super.runTest();
    _logger.info(String.format(">>>>> Completed test: %s", testURI));
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
    if (_ts != null) {
      _ts.destroy();
      _ts = null;
    }
    if (_fed != null) {
      _fed.shutdownNow();
      _fed = null;
    }
  }

  @Override
  protected Repository newRepository() throws Exception {
    return new DatasetRepository(
        new EmbergraphSailRepository(_sail = new EmbergraphSail(newTripleStore())));
  }

  //    @Override
  //    protected Repository createRepository() throws Exception {
  //        Repository repo = newRepository();
  //        repo.initialize();
  //        return repo;
  //    }

  protected EmbergraphSailRepositoryConnection getQueryConnection(Repository dataRep)
      throws Exception {
    // return dataRep.getConnection();
    final EmbergraphSailRepositoryConnection con =
        new EmbergraphSailRepositoryConnection(
            new EmbergraphSailRepository(_sail), _sail.getReadOnlyConnection());
    //		System.err.println(_sail.getDatabase().dumpStore());
    return con;
  }

  private ScaleOutTripleStore newTripleStore() throws Exception {
    _ts = new ScaleOutTripleStore(getFederation(), newNamespace(), ITx.UNISOLATED, getProperties());
    _ts.create();
    return _ts;
  }

  private AbstractDistributedFederation<Object> getFederation() throws Exception {
    if (null == _fed) {
      AbstractScaleOutClient<?> jc =
          ScaleOutClientFactory.getJiniClient(new String[] {getConfiguration()});
      _fed = (AbstractDistributedFederation<Object>) jc.connect();
    }
    return _fed;
  }

  private static String getConfiguration() throws Exception {
    String c = System.getProperty(CONFIG_PROPERTY);
    if (null == c)
      throw new Exception(
          String.format(
              "Configuration property not set. Specify as: -D%s=<filename or URL>",
              CONFIG_PROPERTY));
    return c;
  }

  private String newNamespace() {
    return "SPARQLTest_" + UUID.randomUUID().toString();
  }

  /*
   * Configuration options for the KB instances used to run the SPARQL compliance test suite.
   *
   * <p>Note: These properties can not be cached across tests since they have to be slightly
   * different for some of the tests to handle things like tests which will fail with inlining
   * enabled versus tests which require Unicode collation strength of IDENTICAL.
   */
  private Properties getProperties() throws Exception {
    final Properties _properties;

    /*
     * Specify / override some triple store properties.
     *
     * Note: You must reference this object in the section for the component
     * which will actually create the KB instance, e.g., either the
     * RDFDataLoadMaster or the LubmGeneratorMaster.
     */
    _properties = new Properties();

    /*
     * Setup for quads.
     */
    _properties.put(EmbergraphSail.Options.QUADS_MODE, "true");
    _properties.put(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    _properties.put(EmbergraphSail.Options.QUERY_TIME_EXPANDER, "false");

    /*
     * The Sesame TCK forces statement level connection auto-commit so we
     * set a flag to permit that here. However, auto-commit and this flag
     * SHOULD NOT be used outside of the test suite as they provide an
     * extreme performance penalty.
     */
    // _properties.put ( EmbergraphSail.Options.ALLOW_AUTO_COMMIT, "true" ) ;

    /*
     * Provide Unicode support for keys with locale-based string collation.
     * This is more expensive in key creation during loading, but allows key
     * comparison and sorting in the specified locale in queries.
     *
     * @see org.embergraph.btree.keys.CollatorEnum
     */
    _properties.put(KeyBuilder.Options.COLLATOR, "ICU");
    _properties.put(KeyBuilder.Options.USER_LANGUAGE, "en");
    _properties.put(KeyBuilder.Options.USER_COUNTRY, "US");
    _properties.put(KeyBuilder.Options.USER_VARIANT, "");

    /*
     * Turn off the full text index (search for literals by keyword).
     */
    _properties.put(EmbergraphSail.Options.TEXT_INDEX, "false");

    /*
     * Turn on bloom filter for the SPO index (good up to ~2M index entries
     * for scale-up -or- for any size index for scale-out). This is a big
     * win for some queries on scale-out indices since we can avoid touching
     * the disk if the bloom filter reports "false" for a key.
     */
    _properties.put(EmbergraphSail.Options.BLOOM_FILTER, "true");

    /*
     * Option may be enabled to store blank nodes such that they are stable
     * (they are not stored by default).
     */
    // new NV(EmbergraphSail.Options.STORE_BLANK_NODES,"true");

    /*
     * Turn inlining on or off depending on _this_ test.
     */
    if (EmbergraphSparqlTest.cannotInlineTests.contains(testURI)) {
      _properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "false");
      _properties.setProperty(Options.INLINE_DATE_TIMES, "false");
    } else {
      _properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "true");
      _properties.setProperty(Options.INLINE_DATE_TIMES, "true");
    }

    if (EmbergraphSparqlTest.unicodeStrengthIdentical.contains(testURI)) {
      // Force identical Unicode comparisons.
      _properties.setProperty(Options.COLLATOR, CollatorEnum.JDK.toString());
      _properties.setProperty(Options.STRENGTH, StrengthEnum.Identical.toString());
    }

    return _properties;
  }

  /** The name of the jini configuration file for the federation. */
  public static final String CONFIG_PROPERTY = "embergraph.configuration";

  private static final Logger _logger = Logger.getLogger(EmbergraphFederationSparqlTest.class);

  /*
   * Instance fields for the current test.
   */

  private AbstractDistributedFederation<Object> _fed = null;
  private ScaleOutTripleStore _ts = null;
  private EmbergraphSail _sail = null;

  //	/*
//	 * Dumps the locators for an index of a relation.
  //	 *
  //	 * @param fed
  //	 * @param namespace
  //	 *            The relation namespace.
  //	 * @param timestamp
  //	 *            The timestamp of the view.
  //	 * @param keyOrder
  //	 *            The index.
  //	 */
  //	private static void dumpMDI(AbstractScaleOutFederation<?> fed,
  //			final String namespace, final long timestamp,
  //			final IKeyOrder<?> keyOrder) {
  //
  //		final String name = namespace + "." + keyOrder.getIndexName();
  //
  //		final Iterator<PartitionLocator> itr = fed
  //				.locatorScan(name, timestamp, new byte[] {}/* fromKey */,
  //						null/* toKey */, false/* reverseScan */);
  //
  //		System.err.println("name=" + name + " @ "
  //				+ TimestampUtility.toString(timestamp));
  //
  //		while (itr.hasNext()) {
  //
  //			System.err.println(itr.next());
  //
  //		}
  //
  //	}
  //
  //	public static void main(String[] args) throws ConfigurationException,
  //			Exception {
  //
  //		String namespace = "SPARQLTest_14e097f5-eba9-4ff8-a250-a5f9f19b9c66.spo";
  //
  //		long timestamp=1287001362979L;
  //
  //		IKeyOrder<?> keyOrder = SPOKeyOrder.SPOC;
  //
  //		JiniClient<Object> jc = new JiniClient<Object>(
  //				new String[] { getConfiguration() });
  //
  //		try {
  //
  //			final JiniFederation<?> fed = jc.connect();
  //
  //			dumpMDI(fed, namespace, timestamp, keyOrder);
  //
  //		} finally {
  //
  //			jc.disconnect(true/* immediate */);
  //
  //		}
  //
  //	}

}
