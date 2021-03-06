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
 * Created on Aug 24, 2011
 */

package org.embergraph.rdf.sail.sparql;

import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.LocalTripleStore;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.parser.sparql.manifest.SPARQLSyntaxTest;

/*
 * Embergraph integration for the {@link SPARQLSyntaxTest}. This appears to be a manifest driven
 * test suite for both correct acceptance and correct rejection tests of the SPARQL parser. There is
 * also an Earl report for this test suite which provides a W3C markup for the test results. The
 * Earl report is part of the Sesame compliance packages.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Embergraph2ASTSPARQLSyntaxTest extends SPARQLSyntaxTest {

  /*
   * @param testURI
   * @param name
   * @param queryFileURL
   * @param positiveTest
   */
  public Embergraph2ASTSPARQLSyntaxTest(
      String testURI, String name, String queryFileURL, boolean positiveTest) {

    super(testURI, name, queryFileURL, positiveTest);
  }

  private AbstractTripleStore tripleStore;

  protected Properties getProperties() {

    final Properties properties = new Properties();

    // turn on quads.
    properties.setProperty(AbstractTripleStore.Options.QUADS, "true");

    //        // override the default vocabulary.
    //        properties.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS,
    //                NoVocabulary.class.getName());

    // turn off axioms.
    properties.setProperty(AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // Note: No persistence.
    properties.setProperty(
        org.embergraph.journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    return properties;
  }

  protected void setUp() throws Exception {

    super.setUp();

    tripleStore = getStore(getProperties());
  }

  protected AbstractTripleStore getStore(final Properties properties) {

    final String namespace = "kb";

    // create/re-open journal.
    final Journal journal = new Journal(properties);

    final LocalTripleStore lts =
        new LocalTripleStore(journal, namespace, ITx.UNISOLATED, properties);

    lts.create();

    return lts;
  }

  protected void tearDown() throws Exception {

    if (tripleStore != null) {

      tripleStore.__tearDownUnitTest();

      tripleStore = null;
    }

    super.tearDown();
  }

  /*
   * {@inheritDoc}
   *
   * <p>This uses the {@link Embergraph2ASTSPARQLParser}.
   */
  @Override
  protected void parseQuery(String query, String queryFileURL) throws MalformedQueryException {

    new Embergraph2ASTSPARQLParser().parseQuery(query, queryFileURL);
  }

  public static Test suite() throws Exception {

    final Factory factory =
        (testURI, testName, testAction, positiveTest) -> new Embergraph2ASTSPARQLSyntaxTest(testURI, testName, testAction, positiveTest);

    final TestSuite suite = new TestSuite();

    suite.addTest(SPARQLSyntaxTest.suite(factory));

    return suite;
  }
}
