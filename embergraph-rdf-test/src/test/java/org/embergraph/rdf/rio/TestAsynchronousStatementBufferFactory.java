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
 * Created on Apr 18, 2009
 */

package org.embergraph.rdf.rio;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.journal.ConcurrencyManager;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.lexicon.EmbergraphValueCentricFullTextIndex;
import org.embergraph.rdf.lexicon.LexiconKeyOrder;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.ScaleOutTripleStore;
import org.embergraph.rdf.store.TestScaleOutTripleStoreWithEmbeddedFederation;
import org.embergraph.rdf.util.DumpLexicon;
import org.embergraph.service.AbstractScaleOutFederation;
import org.embergraph.service.EmbeddedClient;
import org.embergraph.service.IEmbergraphClient;
import org.openrdf.rio.RDFFormat;

/**
 * Test suite for {@link AsynchronousStatementBufferFactory}. To run this test by itself specify
 * <code>-DtestClass=org.embergraph.rdf.store.TestScaleOutTripleStoreWithEmbeddedFederation</code> .
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 *     <p>FIXME variant to test w/ and w/o the full text index (with lookup by tokens).
 *     <p>FIXME variant to test async w/ sids (once written).
 * @todo The async API is only defined at this time for scale-out index views, so maybe move this
 *     into the scale-out proxy test suite.
 * @see TestScaleOutTripleStoreWithEmbeddedFederation
 */
public class TestAsynchronousStatementBufferFactory extends AbstractRIOTestCase {

  /** */
  public TestAsynchronousStatementBufferFactory() {}

  /** @param name */
  public TestAsynchronousStatementBufferFactory(String name) {
    super(name);
  }

  private static final int chunkSize = 20000;
  private static final int valuesInitialCapacity = 10000;
  private static final int bnodesInitialCapacity = 16;
  private static final long unbufferedStatementThreshold = 5000L; // Long.MAX_VALUE;
  private static final long rejectedExecutionDelay = 250L; // milliseconds.

  /**
   * SHOULD be <code>true</code> since the whole point of this is higher concurrency. If you set
   * this to <code>false</code> to explore some issue, then change it back to <code>true</code> when
   * you are done!
   */
  private static final boolean parallel = true;

  //    protected AbstractTripleStore getStore() {
  //
  //        return getStore(getProperties());
  //
  //    }

  /** Note: This is overridden to turn off features not supported by this loader. */
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    // Disable reporting.
    properties.setProperty(IEmbergraphClient.Options.REPORT_DELAY, "0");
    properties.setProperty(IEmbergraphClient.Options.COLLECT_QUEUE_STATISTICS, "false");
    properties.setProperty(IEmbergraphClient.Options.COLLECT_PLATFORM_STATISTICS, "false");

    // One DS is enough.
    properties.setProperty(EmbeddedClient.Options.NDATA_SERVICES, "1");

    // Minimize the #of threads so things are simpler to debug.
    properties.setProperty(ConcurrencyManager.Options.DEFAULT_WRITE_SERVICE_CORE_POOL_SIZE, "0");

    properties.setProperty(AbstractTripleStore.Options.TEXT_INDEX, "true");

    properties.setProperty(AbstractTripleStore.Options.STATEMENT_IDENTIFIERS, "false");

    //        properties.setProperty(AbstractTripleStore.Options.QUADS, "true");

    // no closure so we don't need the axioms either.
    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // enable a vocabulary so some things will be inlined.
    properties.setProperty(
        AbstractTripleStore.Options.VOCABULARY_CLASS,
        AbstractTripleStore.Options.DEFAULT_VOCABULARY_CLASS);

    {

      /*
       * FIXME We MUST specify the KB namespace so we can override this
       * property. [Another approach is to override the idle timeout and
       * have it be less than the chunk timeout such that the sink is
       * closed if it becomes idle (no new chunks appearing) but continues
       * to combine chunks as long as they are appearing before the idle
       * timeout.
       */
      final String namespace = "test1";

      {
        final String pname =
            org.embergraph.config.Configuration.getOverrideProperty(
                namespace
                    + "."
                    + LexiconRelation.NAME_LEXICON_RELATION
                    + "."
                    + LexiconKeyOrder.TERM2ID,
                IndexMetadata.Options.SINK_IDLE_TIMEOUT_NANOS);

        final String pval = "" + TimeUnit.SECONDS.toNanos(1);

        if (log.isInfoEnabled()) log.info("Override: " + pname + "=" + pval);

        // Put an idle timeout on the sink of 1s.
        properties.setProperty(pname, pval);
      }

      {
        final String pname =
            org.embergraph.config.Configuration.getOverrideProperty(
                namespace
                    + "."
                    + LexiconRelation.NAME_LEXICON_RELATION
                    + "."
                    + LexiconKeyOrder.BLOBS,
                IndexMetadata.Options.SINK_IDLE_TIMEOUT_NANOS);

        final String pval = "" + TimeUnit.SECONDS.toNanos(1);

        if (log.isInfoEnabled()) log.info("Override: " + pname + "=" + pval);

        // Put an idle timeout on the sink of 1s.
        properties.setProperty(pname, pval);
      }
    }

    // @todo comment out or will fail during verify.
    //        properties.setProperty(AbstractTripleStore.Options.ONE_ACCESS_PATH, "true");

    return properties;
  }

  /** Test with the "small.rdf" data set. */
  public void test_loadAndVerify_small() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/small.rdf";

    doLoadAndVerifyTest(resource, getProperties());
  }

  /** Test with the "small.rdf" data set in quads mode. */
  public void test_loadAndVerify_small_quadsMode() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/small.rdf";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(resource, p);
  }

  /**
   * Test with the "little.ttl" data set in quads mode (triples data loaded into a quads mode kb).
   */
  public void test_loadAndVerify_little_ttl_quadsMode() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/little.ttl";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(resource, p);
  }

  /** Test with the "little.trig" data set in quads mode (quads data loaded into a quads mode kb) */
  public void test_loadAndVerify_little_trig_quadsMode() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/little.trig";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(resource, p);
  }

  /** Test with the "smallWithBlobs.rdf" data set. */
  public void test_loadAndVerify_smallWithBlobs() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/smallWithBlobs.rdf";

    doLoadAndVerifyTest(resource, getProperties());
  }

  /** Test with the "smallWithBlobs.rdf" data set in quads mode. */
  public void test_loadAndVerify_smallWithBlobs_quadsMode() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/smallWithBlobs.rdf";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(resource, p);
  }

  /**
   * Test with the "broken.rdf" data set (does not contain valid RDF). This tests that the factory
   * will shutdown correctly if there are processing errors.
   *
   * @throws Exception
   */
  public void test_loadFails() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/broken.rdf";

    final AbstractTripleStore store = getStore();
    try {

      if (!(store.getIndexManager() instanceof AbstractScaleOutFederation)) {

        log.warn("Test requires scale-out index views.");

        return;
      }

      if (store.isQuads()) {

        log.warn("Quads not supported yet.");

        return;
      }

      // only do load since we expect an error to be reported.
      final AsynchronousStatementBufferFactory<EmbergraphStatement, File> factory =
          doLoad2(store, new File(resource), parallel);

      assertEquals("errorCount", 1, factory.getDocumentErrorCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /** Test with the "sample data.rdf" data set. */
  public void test_loadAndVerify_sampleData() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/sample data.rdf";

    doLoadAndVerifyTest(resource, getProperties());
  }

  /** Test with the "sample data.rdf" data set in quads mode. */
  public void test_loadAndVerify_sampleData_quadsMode() throws Exception {

    final String resource = "/org/embergraph/rdf/rio/sample data.rdf";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(resource, p);
  }

  /** Uses a modest file (~40k statements). This is BSBM data so it has some BLOBs in it. */
  public void test_loadAndVerify_bsbm_pc100() throws Exception {

    final String file = "/data/bsbm/dataset_pc100.nt";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(file, p);
  }

  /**
   * Uses a modest file (~40k statements). This is BSBM data so it has some BLOBs in it. This loads
   * the data in quads mode.
   */
  public void test_loadAndVerify_bsbm_pc100_quadsMode() throws Exception {

    final String file = "/data/bsbm/dataset_pc100.nt";

    final Properties p = getProperties();

    p.setProperty(AbstractTripleStore.Options.QUADS, "true");

    doLoadAndVerifyTest(file, p);
  }

  //	/**
  //	 * LUBM U(1).
  //	 * <p>
  //	 * Note: This unit test can hang under JDK 1.6.0_17 if you have been running
  //	 * the entire test suite and you do not specify <code>-XX:+UseMembar</code>
  //	 * to the JVM. This is a JVM bug. The <code>-XX:+UseMembar</code> option is
  //	 * the workaround. [This is also very slow to run, especially with the lexicon
  //   * validation.]
  //	 */
  //    public void test_loadAndVerify_U1() throws Exception {
  //
  //        final String file = "/data/lehigh/U1";
  //
  //        doLoadAndVerifyTest(file, getProperties());
  //
  //    }

  //    /**
  //     * Do not leave this unit test in -- it takes too long to validate the
  //     * loaded data: LUBM U(10)
  //     */
  //    public void test_loadAndVerify_U10() throws Exception {
  //
  //        final String file = "../rdf-data/lehigh/U10";
  //
  //        doLoadAndVerifyTest(file);
  //
  //    }

  /**
   * Test loads an RDF/XML resource into a database and then verifies by re-parse that all expected
   * statements were made persistent in the database.
   *
   * @param resource
   * @throws Exception
   */
  protected void doLoadAndVerifyTest(final String resource, final Properties properties)
      throws Exception {

    final AbstractTripleStore store = getStore(properties);

    try {

      if (!(store.getIndexManager() instanceof AbstractScaleOutFederation)) {

        log.warn("Test requires scale-out index views.");

        return;
      }

      doLoad(store, resource, parallel);

      if (log.isDebugEnabled()) {

        log.debug("dumping store...");

        log.debug("LEXICON:\n" + DumpLexicon.dump(store.getLexiconRelation()));

        if (store.getLexiconRelation().isTextIndex()) {

          // Full text index.

          final ITupleIterator<?> itr =
              ((EmbergraphValueCentricFullTextIndex) store.getLexiconRelation().getSearchEngine())
                  .getIndex()
                  .rangeIterator();

          while (itr.hasNext()) {

            log.debug(itr.next().getObject());
          }
        }

        // raw statement indices.
        {
          final Iterator<SPOKeyOrder> itr =
              store.isQuads()
                  ? SPOKeyOrder.quadStoreKeyOrderIterator()
                  : SPOKeyOrder.tripleStoreKeyOrderIterator();

          while (itr.hasNext()) {

            final SPOKeyOrder keyOrder = itr.next();

            log.debug("\n---" + keyOrder + "---\n" + store.getSPORelation().dump(keyOrder));
          }
        }

        // resolved statement indices.
        {
          final Iterator<SPOKeyOrder> itr =
              store.isQuads()
                  ? SPOKeyOrder.quadStoreKeyOrderIterator()
                  : SPOKeyOrder.tripleStoreKeyOrderIterator();

          while (itr.hasNext()) {

            final SPOKeyOrder keyOrder = itr.next();

            log.debug("\n" + keyOrder + "\n" + store.getSPORelation().dump(keyOrder));

            log.debug(
                "\n---"
                    + keyOrder
                    + "---\n"
                    + store.dumpStore(
                        store /* resolveTerms */,
                        true /* explicit */,
                        true /* inferred */,
                        true /* axioms */,
                        true /* history */,
                        true /* justifications */,
                        true /* sids */,
                        keyOrder));
          }
        }
      }

      doVerify(store, resource, parallel);

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /** Load using {@link AsynchronousStatementBufferWithoutSids2}. */
  protected void doLoad(
      final AbstractTripleStore store, final String resource, final boolean parallel)
      throws Exception {

    doLoad2(store, new File(resource), parallel);
  }

  /** Load using {@link AsynchronousStatementBufferFactory}. */
  protected AsynchronousStatementBufferFactory<EmbergraphStatement, File> doLoad2(
      final AbstractTripleStore store, final File resource, final boolean parallel)
      throws Exception {

    final RDFParserOptions parserOptions = new RDFParserOptions();
    parserOptions.setVerifyData(false);

    final AsynchronousStatementBufferFactory<EmbergraphStatement, File> statementBufferFactory =
        new AsynchronousStatementBufferFactory<EmbergraphStatement, File>(
            (ScaleOutTripleStore) store,
            chunkSize,
            valuesInitialCapacity,
            bnodesInitialCapacity,
            RDFFormat.RDFXML, // defaultFormat
            null, // defaultGraph
            parserOptions, //
            false, // deleteAfter
            parallel ? 5 : 1, // parserPoolSize,
            20, // parserQueueCapacity
            parallel ? 5 : 1, // term2IdWriterPoolSize,
            parallel ? 5 : 1, // otherWriterPoolSize
            parallel ? 5 : 1, // notifyPoolSize
            unbufferedStatementThreshold);

    //        final AsynchronousWriteBufferFactoryWithoutSids2<EmbergraphStatement, File>
    // statementBufferFactory = new AsynchronousWriteBufferFactoryWithoutSids2<EmbergraphStatement,
    // File>(
    //                (ScaleOutTripleStore) store, chunkSize, valuesInitialCapacity,
    //                bnodesInitialCapacity);

    try {

      // tasks to load the resource or file(s)
      if (resource.isDirectory()) {

        statementBufferFactory.submitAll(
            resource, new org.embergraph.rdf.load.RDFFilenameFilter(), rejectedExecutionDelay);

      } else {

        statementBufferFactory.submitOne(resource);
      }

      // wait for the async writes to complete.
      statementBufferFactory.awaitAll();

      // dump write statistics for indices used by kb.
      //            System.err.println(((AbstractFederation) store.getIndexManager())
      //                    .getServiceCounterSet().getPath("Indices").toString());

      // dump factory specific counters.
      System.err.println(statementBufferFactory.getCounters().toString());

    } catch (Throwable t) {

      statementBufferFactory.cancelAll(true /* mayInterruptIfRunning */);

      // rethrow
      throw new RuntimeException(t);
    }

    return statementBufferFactory;
  }
}
