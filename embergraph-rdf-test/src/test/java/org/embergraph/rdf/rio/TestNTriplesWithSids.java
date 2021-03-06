package org.embergraph.rdf.rio;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.ServiceProviderHook;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.rio.ntriples.EmbergraphNTriplesParser;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.store.DataLoader;
import org.embergraph.rdf.store.EmbergraphStatementIterator;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFParserRegistry;

/*
 * Test suite for SIDS support with NTRIPLES data.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestNTriplesWithSids extends AbstractTripleStoreTestCase {

  protected static final transient Logger log = Logger.getLogger(TestNTriplesWithSids.class);

  public TestNTriplesWithSids() {}

  public TestNTriplesWithSids(String name) {
    super(name);
  }

  @Override
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    return properties;
  }

  /*
   * The "terse" syntax:
   *
   * <pre>
   * <<_:alice foaf:mbox <mailto:alice@work>>> <http://purl.org/dc/terms/source> <http://hr.example.com/employees#bob> .
   * <<_:alice foaf:mbox <mailto:alice@work>>> <http://purl.org/dc/terms/created>  "2012-02-05T12:34:00Z"^^xsd:dateTime .
   * </pre>
   *
   * is equivalent to the expanded syntax.
   *
   * <pre>
   * @prefix dc:          .
   *
   * _:s1 rdf:subject _:alice .
   * _:s1 rdf:predicate foaf:mbox .
   * _:s1 rdf:object <mailto:alice@work> .
   * _:s1 rdf:type rdf:Statement .
   * _:s1 dc:source   <http://hr.example.com/employees#bob> ;
   *      dc:created  "2012-02-05T12:34:00Z"^^xsd:dateTime .
   * </pre>
   * @throws IOException
   * @throws RDFHandlerException
   * @throws RDFParseException
   */
  public void test_ntriples_sids_00() throws IOException {

    final String data =
        ""
            + "_:alice <http://xmlns.com/foaf/0.1/mbox> <mailto:alice@work> .\n"
            + "_:s1 <"
            + RDF.SUBJECT
            + "> _:alice .\n"
            + "_:s1 <"
            + RDF.PREDICATE
            + "> <http://xmlns.com/foaf/0.1/mbox> .\n"
            + "_:s1 <"
            + RDF.OBJECT
            + "> <mailto:alice@work> .\n"
            + "_:s1 <http://purl.org/dc/terms/source> <http://hr.example.com/employees#bob> .\n"
            + "<<_:alice <http://xmlns.com/foaf/0.1/mbox> <mailto:alice@work>>> <http://purl.org/dc/terms/created>  \"2012-02-05T12:34:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .\n";

    final AbstractTripleStore store = getStore();

    try {

      if (!store.getStatementIdentifiers()) {

        log.warn("Statement identifiers not enabled - skipping test");

        return;
      }

      // Verify that the correct parser will be used.
      assertEquals(
          "NTriplesParserClass",
          EmbergraphNTriplesParser.class.getName(),
          RDFParserRegistry.getInstance()
              .get(ServiceProviderHook.NTRIPLES_RDR)
              .getParser()
              .getClass()
              .getName());

      final DataLoader dataLoader = store.getDataLoader();

      final LoadStats loadStats =
          dataLoader.loadData(
              new StringReader(data), getName() /* baseURL */, ServiceProviderHook.NTRIPLES_RDR);

      if (log.isInfoEnabled()) log.info(store.dumpStore());

      assertEquals("toldTriples", 3L, store.getStatementCount()); // loadStats.toldTriples.get());

      final EmbergraphStatementIterator it = store.getStatements(null, null, null);
      while (it.hasNext()) System.err.println(it.next());

      final EmbergraphURI dcSource =
          store.getValueFactory().createURI("http://purl.org/dc/terms/source");

      final EmbergraphURI dcCreated =
          store.getValueFactory().createURI("http://purl.org/dc/terms/created");

      final EmbergraphURI bobSource =
          store.getValueFactory().createURI("http://hr.example.com/employees#bob");

      assertEquals(
          1, store.getAccessPath(null /* s */, dcSource, bobSource).rangeCount(true /* exact */));

      assertEquals(
          1, store.getAccessPath(null /* s */, dcCreated, null /*o*/).rangeCount(true /* exact */));

      // Verify subject is a Statement.
      {
        int n = 0;

        final EmbergraphStatementIterator itr =
            store.getStatements(null /* s */, dcSource, bobSource);

        try {

          while (itr.hasNext()) {

            final EmbergraphStatement st = itr.next();

            assertTrue(st.getSubject().getIV().isStatement());

            n++;
          }

        } finally {

          itr.close();
        }

        assertEquals(1, n);
      }

      // Verify subject is a Statement.
      {
        int n = 0;

        final EmbergraphStatementIterator itr =
            store.getStatements(null /* s */, dcCreated, null /* o */);

        try {

          while (itr.hasNext()) {

            final EmbergraphStatement st = itr.next();

            assertTrue(st.getSubject().getIV().isStatement());

            n++;
          }

        } finally {

          itr.close();
        }

        assertEquals(1, n);
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * The "terse" syntax:
   *
   * <pre>
   * <<_:alice foaf:mbox <mailto:alice@work>>> <http://purl.org/dc/terms/source> <http://hr.example.com/employees#bob> .
   * <<_:alice foaf:mbox <mailto:alice@work>>> <http://purl.org/dc/terms/created>  "2012-02-05T12:34:00Z"^^xsd:dateTime .
   * </pre>
   *
   * is equivalent to the expanded syntax.
   *
   * <pre>
   * @prefix dc:          .
   *
   * _:s1 rdf:subject _:alice .
   * _:s1 rdf:predicate foaf:mbox .
   * _:s1 rdf:object <mailto:alice@work> .
   * _:s1 rdf:type rdf:Statement .
   * _:s1 dc:source   <http://hr.example.com/employees#bob> ;
   *      dc:created  "2012-02-05T12:34:00Z"^^xsd:dateTime .
   * </pre>
   * @throws IOException
   * @throws RDFHandlerException
   * @throws RDFParseException
   */
  public void test_ntriples_sids_01() throws IOException {

    final String data =
        ""
            + "_:alice <http://xmlns.com/foaf/0.1/mbox> <mailto:alice@work> .\n"
            + "<< _:alice <http://xmlns.com/foaf/0.1/mbox> <mailto:alice@work> >> <http://purl.org/dc/terms/source>   <http://hr.example.com/employees#bob> .\n"
            + "<<_:alice <http://xmlns.com/foaf/0.1/mbox> <mailto:alice@work>>> <http://purl.org/dc/terms/created>  \"2012-02-05T12:34:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .\n";

    final AbstractTripleStore store = getStore();

    try {

      if (!store.getStatementIdentifiers()) {

        log.warn("Statement identifiers not enabled - skipping test");

        return;
      }

      // Verify that the correct parser will be used.
      assertEquals(
          "NTriplesParserClass",
          EmbergraphNTriplesParser.class.getName(),
          RDFParserRegistry.getInstance()
              .get(ServiceProviderHook.NTRIPLES_RDR)
              .getParser()
              .getClass()
              .getName());

      final DataLoader dataLoader = store.getDataLoader();

      final LoadStats loadStats =
          dataLoader.loadData(
              new StringReader(data), getName() /* baseURL */, ServiceProviderHook.NTRIPLES_RDR);

      if (log.isInfoEnabled()) log.info(store.dumpStore());

      assertEquals("toldTriples", 3L, store.getStatementCount()); // loadStats.toldTriples.get());

      final EmbergraphURI dcSource =
          store.getValueFactory().createURI("http://purl.org/dc/terms/source");

      final EmbergraphURI dcCreated =
          store.getValueFactory().createURI("http://purl.org/dc/terms/created");

      final EmbergraphURI bobSource =
          store.getValueFactory().createURI("http://hr.example.com/employees#bob");

      assertEquals(
          1, store.getAccessPath(null /* s */, dcSource, bobSource).rangeCount(true /* exact */));

      assertEquals(
          1, store.getAccessPath(null /* s */, dcCreated, null /*o*/).rangeCount(true /* exact */));

      // Verify subject is a Statement.
      {
        int n = 0;

        final EmbergraphStatementIterator itr =
            store.getStatements(null /* s */, dcSource, bobSource);

        try {

          while (itr.hasNext()) {

            final EmbergraphStatement st = itr.next();

            assertTrue(st.getSubject().getIV().isStatement());

            n++;
          }

        } finally {

          itr.close();
        }

        assertEquals(1, n);
      }

      // Verify subject is a Statement.
      {
        int n = 0;

        final EmbergraphStatementIterator itr =
            store.getStatements(null /* s */, dcCreated, null /* o */);

        try {

          while (itr.hasNext()) {

            final EmbergraphStatement st = itr.next();

            assertTrue(st.getSubject().getIV().isStatement());

            n++;
          }

        } finally {

          itr.close();
        }

        assertEquals(1, n);
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
