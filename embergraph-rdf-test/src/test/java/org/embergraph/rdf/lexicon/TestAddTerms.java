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
 * Created on Nov 6, 2007
 */

package org.embergraph.rdf.lexicon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStore.Options;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.vocabulary.RDF;

/**
 * Test suite for adding terms to the lexicon.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAddTerms extends AbstractTripleStoreTestCase {

  /** */
  public TestAddTerms() {
    super();
  }

  /** @param name */
  public TestAddTerms(String name) {
    super(name);
  }

  public void test_addTerms() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // test w/o inlining
    properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      terms.add(f.asValue(RDF.TYPE));
      terms.add(f.asValue(RDF.PROPERTY));
      terms.add(f.createURI(getVeryLargeURI()));

      terms.add(f.createLiteral("test"));
      terms.add(f.createLiteral("test", "en"));
      terms.add(f.createLiteral(getVeryLargeLiteral()));

      terms.add(f.createLiteral("10", f.createURI("http://www.w3.org/2001/XMLSchema#int")));

      terms.add(f.createLiteral("12", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.0", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.00", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      if (store.getLexiconRelation().isStoreBlankNodes()) {
        /*
         * Note: Blank nodes will not round trip through the lexicon unless
         * the "told bnodes" is enabled.
         */
        terms.add(f.createBNode());
        terms.add(f.createBNode("a"));
        terms.add(f.createBNode(getVeryLargeLiteral()));
      }

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids2.entrySet()) {

          final IV<?, ?> id = e.getKey();

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /**
   * The "told bnodes" mode uses the blank node ID as specified rather than assigning one based on a
   * UUID. For this case, we need to store the blank nodes in the reverse index (T2ID) so we can
   * translate a blank node back to a specific identifier.
   */
  public void test_toldBNodes() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // this is the "told bnodes" mode.
    properties.setProperty(Options.STORE_BLANK_NODES, "true");

    AbstractTripleStore store = getStore(properties);

    try {

      if (!store.isStable()) {

        /*
         * We need a restart safe store to test this since otherwise a
         * term cache could give us a false positive.
         */

        return;
      }

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      terms.add(f.createBNode());
      terms.add(f.createBNode("a"));

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids.entrySet()) {

          final IV<?, ?> id = e.getKey();

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /**
   * Unit test for addTerms() when the {@link EmbergraphValue}[] contains multiple instances of a
   * given reference.
   */
  public void test_duplicates_same_reference() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // test w/o xsd inlining
    properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      // Note: List allows duplicates!
      final Collection<EmbergraphValue> terms = new LinkedList<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      // Add two instances of the same reference.
      final EmbergraphURI type = f.asValue(RDF.TYPE);
      terms.add(type);
      terms.add(type);
      assertEquals(2, terms.size());

      // Add two instances of the same reference.
      final EmbergraphURI largeURI = f.createURI(getVeryLargeURI());
      terms.add(largeURI);
      terms.add(largeURI);
      assertEquals(4, terms.size());

      // Add two instances of the same reference.
      final EmbergraphLiteral lit1 = f.createLiteral("test");
      final EmbergraphLiteral lit2 = f.createLiteral("test", "en");
      final EmbergraphLiteral lit3 = f.createLiteral(getVeryLargeLiteral());
      terms.add(lit1);
      terms.add(lit1);
      terms.add(lit2);
      terms.add(lit2);
      terms.add(lit3);
      terms.add(lit3);
      assertEquals(10, terms.size());

      if (store.getLexiconRelation().isStoreBlankNodes()) {

        /*
         * Note: Blank nodes will not round trip through the lexicon unless
         * the "told bnodes" is enabled.
         */
        final EmbergraphBNode bnode1 = f.createBNode();
        final EmbergraphBNode bnode2 = f.createBNode("a");
        final EmbergraphBNode bnode3 = f.createBNode(getVeryLargeLiteral());
        terms.add(bnode1);
        terms.add(bnode1);
        terms.add(bnode2);
        terms.add(bnode2);
        terms.add(bnode3);
        terms.add(bnode3);
        assertEquals(16, terms.size());
      }

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids2.entrySet()) {

          final IV<?, ?> id = e.getKey();

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /**
   * Unit test for addTerms() when the {@link EmbergraphValue}[] contains distinct instances of
   * {@link EmbergraphValue}s which are equals().
   */
  public void test_duplicates_distinct_references() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // test w/o xsd inlining
    properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      // Note: List allows duplicates!
      final Collection<EmbergraphValue> terms = new LinkedList<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      // Add two distinct instances of the same Value.
      terms.add(f.asValue(RDF.TYPE));
      terms.add(f.asValue(RDF.TYPE));
      assertEquals(2, terms.size());

      // Add two distinct instances of the same Value.
      terms.add(f.createURI(getVeryLargeURI()));
      terms.add(f.createURI(getVeryLargeURI()));
      assertEquals(4, terms.size());

      // Add two distinct instances of the same Value.
      terms.add(f.createLiteral("test"));
      terms.add(f.createLiteral("test"));
      terms.add(f.createLiteral("test", "en"));
      terms.add(f.createLiteral("test", "en"));
      terms.add(f.createLiteral(getVeryLargeLiteral()));
      terms.add(f.createLiteral(getVeryLargeLiteral()));
      assertEquals(10, terms.size());

      if (store.getLexiconRelation().isStoreBlankNodes()) {

        /*
         * Note: Blank nodes will not round trip through the lexicon unless
         * the "told bnodes" is enabled.
         */

        // Add two distinct instances of the same Value.
        terms.add(f.createBNode());
        terms.add(f.createBNode());
        terms.add(f.createBNode("a"));
        terms.add(f.createBNode("a"));
        terms.add(f.createBNode(getVeryLargeLiteral()));
        terms.add(f.createBNode(getVeryLargeLiteral()));
        assertEquals(16, terms.size());
      }

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids2.entrySet()) {

          final IV<?, ?> id = e.getKey();

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /**
   * @param store
   * @param terms
   * @return
   */
  private Map<IV<?, ?>, EmbergraphValue> doAddTermsTest(
      final AbstractTripleStore store, final Collection<EmbergraphValue> terms) {

    final int size = terms.size();

    final EmbergraphValue[] a = terms.toArray(new EmbergraphValue[size]);

    // Resolve/add IVs.
    store.getLexiconRelation().addTerms(a, size, false /* readOnly */);

    // Collect the assigned IVs.
    final Collection<IV<?, ?>> ids = new ArrayList<IV<?, ?>>();

    for (EmbergraphValue t : a) ids.add(t.getIV());

    // Resolve assigned IVs against the lexicon.
    final Map<IV<?, ?>, EmbergraphValue> tmp = store.getLexiconRelation().getTerms(ids);

    // Note: This is not true if there are duplicates.
    //        assertEquals(size, tmp.size());

    /*
     * Verify that the lexicon reports the same RDF Values for those term
     * identifiers (they will be "equals()" to the values that we added to
     * the lexicon).
     */
    for (EmbergraphValue expected : a) {

      assertNotNull("Did not assign IV? : " + expected, expected.getIV());

      final EmbergraphValue actual = tmp.get(expected.getIV());

      if (actual == null) {

        fail("Lexicon does not have value: iv=" + expected.getIV() + ", expected=" + expected);
      }

      assertEquals("Id mapped to a different term? iv=" + expected.getIV(), expected, actual);
    }

    return tmp;
  }

  static String getVeryLargeURI() {

    final int len = 1024000;

    final StringBuilder sb = new StringBuilder(len + 20);

    sb.append("http://www.embergraph.org/");

    for (int i = 0; i < len; i++) {

      sb.append(Character.toChars('A' + (i % 26)));
    }

    final String s = sb.toString();

    return s;
  }

  static String getVeryLargeLiteral() {

    final int len = 1024000;

    final StringBuilder sb = new StringBuilder(len);

    for (int i = 0; i < len; i++) {

      sb.append(Character.toChars('A' + (i % 26)));
    }

    final String s = sb.toString();

    return s;
  }
}
