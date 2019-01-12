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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.ColorsEnumExtension;
import org.embergraph.rdf.internal.EpochExtension;
import org.embergraph.rdf.internal.IExtension;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.SampleExtensionFactory;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStore.Options;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.vocab.BaseVocabulary;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.openrdf.model.URI;

/*
 * Test suite for adding terms to the lexicon.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestInlining extends AbstractTripleStoreTestCase {

  private static final transient Logger log = Logger.getLogger(TestInlining.class);

  /** */
  public TestInlining() {
    super();
  }

  /** @param name */
  public TestInlining(String name) {
    super(name);
  }

  /** Unsigned numerics should not be inlined at this time. */
  public void test_unsigned() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values, ensure range is beyond max signed values.
      final EmbergraphValueFactory f = store.getValueFactory();

      terms.add(f.createLiteral("135", f.createURI(XSD.UNSIGNED_BYTE.toString())));

      terms.add(
          f.createLiteral(
              "" + (10L + Integer.MAX_VALUE), f.createURI(XSD.UNSIGNED_INT.toString())));

      terms.add(
          f.createLiteral("" + (Short.MAX_VALUE + 10), f.createURI(XSD.UNSIGNED_SHORT.toString())));

      terms.add(
          f.createLiteral(
              BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(10)).toString(),
              f.createURI(XSD.UNSIGNED_LONG.toString())));

      /*
       * Note: Blank nodes will not round trip through the lexicon unless
       * the "told bnodes" is enabled.
       */
      //            terms.add(f.createBNode());
      //            terms.add(f.createBNode("a"));

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

          // Should be inlined
          assertTrue(store.isInlineLiterals() == id.isInline());

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /** Verify inlined unsigned numeric values */
  public void test_verifyunsigned() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values, ensure range is beyond max signed values.
      final EmbergraphValueFactory f = store.getValueFactory();

      assertTrue(
          f.createLiteral("198", f.createURI(XSD.UNSIGNED_BYTE.toString())).intValue() == 198);

      assertTrue(f.createLiteral("0", f.createURI(XSD.UNSIGNED_BYTE.toString())).intValue() == 0);

      assertTrue(f.createLiteral("50", f.createURI(XSD.UNSIGNED_BYTE.toString())).intValue() == 50);

      assertTrue(
          f.createLiteral("" + (Short.MAX_VALUE + 10), f.createURI(XSD.UNSIGNED_SHORT.toString()))
                  .intValue()
              == (Short.MAX_VALUE + 10));

      assertTrue(f.createLiteral("0", f.createURI(XSD.UNSIGNED_SHORT.toString())).intValue() == 0);

      assertTrue(
          f.createLiteral("" + (10L + Integer.MAX_VALUE), f.createURI(XSD.UNSIGNED_INT.toString()))
                  .longValue()
              == (10L + Integer.MAX_VALUE));

      assertTrue(f.createLiteral("0", f.createURI(XSD.UNSIGNED_INT.toString())).longValue() == 0L);

      BigInteger bi = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(10));
      assertTrue(
          f.createLiteral(bi.toString(), f.createURI(XSD.UNSIGNED_LONG.toString()))
              .integerValue()
              .equals(bi));

      BigInteger biz = BigInteger.valueOf(0);
      assertTrue(
          f.createLiteral("0", f.createURI(XSD.UNSIGNED_LONG.toString()))
              .integerValue()
              .equals(biz));
      BigInteger bi100 = BigInteger.valueOf(100);
      assertTrue(
          f.createLiteral("100", f.createURI(XSD.UNSIGNED_LONG.toString()))
              .integerValue()
              .equals(bi100));

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /** Unsigned numerics should not be inlined at this time. */
  public void test_badrangeUnsigned() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values, ensure range is beyond max signed values.
      final EmbergraphValueFactory f = store.getValueFactory();

      // Out of range values cannot be inlined
      terms.add(f.createLiteral("-12", f.createURI(XSD.UNSIGNED_BYTE.toString())));

      terms.add(f.createLiteral("1024", f.createURI(XSD.UNSIGNED_BYTE.toString())));

      terms.add(
          f.createLiteral("" + Integer.MAX_VALUE, f.createURI(XSD.UNSIGNED_SHORT.toString())));

      terms.add(f.createLiteral("" + Long.MAX_VALUE, f.createURI(XSD.UNSIGNED_INT.toString())));

      terms.add(
          f.createLiteral(
              BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(10)).toString(),
              f.createURI(XSD.UNSIGNED_LONG.toString())));

      /*
       * Note: Blank nodes will not round trip through the lexicon unless
       * the "told bnodes" is enabled.
       */
      //            terms.add(f.createBNode());
      //            terms.add(f.createBNode("a"));

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

          // Should be inlined
          assertFalse(id.isInline());

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * The "told bnodes" mode uses the blank node ID as specified rather than assigning one based on a
   * UUID. For this case, we need to store the blank nodes in the reverse index (T2ID) so we can
   * translate a blank node back to a specific identifier.
   */
  public void test_inlineBNodes() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // this is the "told bnodes" mode.
    properties.setProperty(Options.STORE_BLANK_NODES, "true");

    // inline the bnodes
    properties.setProperty(Options.INLINE_BNODES, "true");

    // Do not inline unicode data.
    properties.setProperty(Options.MAX_INLINE_TEXT_LENGTH, "0");

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

      final EmbergraphBNode b1 = f.createBNode("i1");
      final EmbergraphBNode b01 = f.createBNode("i01");
      final EmbergraphBNode b2 = f.createBNode("u" + UUID.randomUUID().toString());
      final EmbergraphBNode b3 = f.createBNode("foo");
      final EmbergraphBNode b4 = f.createBNode("foo12345");
      final EmbergraphBNode b5 = f.createBNode("12345");

      terms.add(b1);
      terms.add(b01);
      terms.add(b2);
      terms.add(b3);
      terms.add(b4);
      terms.add(b5);

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      assertTrue(b1.getIV().isInline());
      assertFalse(b01.getIV().isInline());
      assertTrue(b2.getIV().isInline());
      assertFalse(b3.getIV().isInline());
      assertFalse(b4.getIV().isInline());
      assertFalse(b5.getIV().isInline());

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids.entrySet()) {

          final IV<?, ?> iv = e.getKey();

          if (log.isInfoEnabled()) log.info(iv);

          assertEquals("Id mapped to a different term? : iv=" + iv, ids.get(iv), ids2.get(iv));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  public void test_epoch() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, MyVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // do not inline unicode data.
    properties.setProperty(Options.MAX_INLINE_TEXT_LENGTH, "0");

    // test with the sample extension factory
    properties.setProperty(Options.EXTENSION_FACTORY_CLASS, SampleExtensionFactory.class.getName());

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

      final EmbergraphLiteral l1 = f.createLiteral("1", EpochExtension.EPOCH);
      final EmbergraphLiteral l2 =
          f.createLiteral(String.valueOf(System.currentTimeMillis()), EpochExtension.EPOCH);
      //            final EmbergraphLiteral l3 = f.createLiteral("-100", EpochExtension.EPOCH);
      final EmbergraphURI datatype = f.createURI(EpochExtension.EPOCH.stringValue());

      terms.add(l1);
      terms.add(l2);
      //            terms.add(l3);
      terms.add(datatype);

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      assertTrue(l1.getIV().isInline());
      assertTrue(l2.getIV().isInline());
      //            assertFalse(l3.getIV().isInline());

      final LiteralExtensionIV iv1 = (LiteralExtensionIV) l1.getIV();
      final LiteralExtensionIV iv2 = (LiteralExtensionIV) l2.getIV();

      assertEquals(iv1.getExtensionIV(), datatype.getIV());
      assertEquals(iv2.getExtensionIV(), datatype.getIV());

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids.entrySet()) {

          final IV<?, ?> iv = e.getKey();

          if (log.isInfoEnabled()) log.info(iv);

          assertEquals("Id mapped to a different term? : iv=" + iv, ids.get(iv), ids2.get(iv));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /** Declares URIs to support the {@link IExtension} examples. */
  private static class MyVocabularyDecl implements VocabularyDecl {

    private static final URI[] myVocabURIs =
        new URI[] {ColorsEnumExtension.COLOR, EpochExtension.EPOCH};

    public MyVocabularyDecl() {}

    public Iterator<URI> values() {

      return Collections.unmodifiableList(Arrays.asList(myVocabURIs)).iterator();
    }
  }

  /** Declares URIs to support the {@link IExtension} examples. */
  public static class MyVocabulary extends BaseVocabulary {

    /** De-serialization ctor. */
    public MyVocabulary() {

      super();
    }

    public MyVocabulary(final String namespace) {

      super(namespace);
    }

    @Override
    protected void addValues() {

      addDecl(new MyVocabularyDecl());
    }
  }

  public void test_colorsEnum() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, MyVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // do not inline unicode data.
    properties.setProperty(Options.MAX_INLINE_TEXT_LENGTH, "0");

    // test with the sample extension factory
    properties.setProperty(Options.EXTENSION_FACTORY_CLASS, SampleExtensionFactory.class.getName());

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

      final EmbergraphLiteral l1 = f.createLiteral("Blue", ColorsEnumExtension.COLOR);
      final EmbergraphLiteral l2 = f.createLiteral("Brown", ColorsEnumExtension.COLOR);
      final EmbergraphLiteral l3 = f.createLiteral("Yellow", ColorsEnumExtension.COLOR);
      final EmbergraphLiteral l4 = f.createLiteral("Dog", ColorsEnumExtension.COLOR);
      final EmbergraphLiteral l5 = f.createLiteral("yellow", ColorsEnumExtension.COLOR);
      final EmbergraphURI datatype = f.createURI(ColorsEnumExtension.COLOR.stringValue());

      terms.add(l1);
      terms.add(l2);
      terms.add(l3);
      terms.add(l4);
      terms.add(l5);
      terms.add(datatype);

      final Map<IV<?, ?>, EmbergraphValue> ids = doAddTermsTest(store, terms);

      assertTrue(l1.getIV().isInline());
      assertTrue(l2.getIV().isInline());
      assertTrue(l3.getIV().isInline());
      assertFalse(l4.getIV().isInline());
      assertFalse(l5.getIV().isInline());

      final LiteralExtensionIV iv1 = (LiteralExtensionIV) l1.getIV();
      final LiteralExtensionIV iv2 = (LiteralExtensionIV) l2.getIV();
      final LiteralExtensionIV iv3 = (LiteralExtensionIV) l3.getIV();

      if (log.isInfoEnabled()) log.info(l1.getLabel() + ": " + iv1.getDelegate().byteValue());
      if (log.isInfoEnabled()) log.info(l2.getLabel() + ": " + iv2.getDelegate().byteValue());
      if (log.isInfoEnabled()) log.info(l3.getLabel() + ": " + iv3.getDelegate().byteValue());

      assertEquals(iv1.getExtensionIV(), datatype.getIV());
      assertEquals(iv2.getExtensionIV(), datatype.getIV());
      assertEquals(iv3.getExtensionIV(), datatype.getIV());

      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);

        // verify same reverse mappings.

        final Map<IV<?, ?>, EmbergraphValue> ids2 =
            store.getLexiconRelation().getTerms(ids.keySet());

        assertEquals(ids.size(), ids2.size());

        for (Map.Entry<IV<?, ?>, EmbergraphValue> e : ids.entrySet()) {

          final IV<?, ?> iv = e.getKey();

          if (log.isInfoEnabled()) log.info(iv);

          assertEquals("Id mapped to a different term? : iv=" + iv, ids.get(iv), ids2.get(iv));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * @param store
   * @param terms
   * @return
   */
  private Map<IV<?, ?>, EmbergraphValue> doAddTermsTest(
      final AbstractTripleStore store, final Collection<EmbergraphValue> terms) {

    final int size = terms.size();

    final EmbergraphValue[] a = terms.toArray(new EmbergraphValue[size]);

    // resolve term ids.
    store.getLexiconRelation().addTerms(a, size, false /* readOnly */);

    // populate map w/ the assigned term identifiers.
    final Collection<IV<?, ?>> ids = new ArrayList<IV<?, ?>>();

    for (EmbergraphValue t : a) {

      ids.add(t.getIV());
    }

    /*
     * Resolve the assigned term identifiers against the lexicon.
     */
    final Map<IV<?, ?>, EmbergraphValue> tmp = store.getLexiconRelation().getTerms(ids);

    assertEquals(size, tmp.size());

    /*
     * Verify that the lexicon reports the same RDF Values for those term
     * identifiers (they will be "equals()" to the values that we added to
     * the lexicon).
     */
    for (EmbergraphValue expected : a) {

      assertNotSame("Did not assign internal value? : " + expected, null, expected.getIV());

      final EmbergraphValue actual = tmp.get(expected.getIV());

      if (actual == null) {

        fail("Lexicon does not have value: iv=" + expected.getIV() + ", term=" + expected);
      }

      assertEquals("IV mapped to a different term? iv=" + expected.getIV(), expected, actual);
    }

    return tmp;
  }

  public void test_inlinedatetimes() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o axioms - they imply a predefined vocab.
    properties.setProperty(Options.AXIOMS_CLASS, NoAxioms.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    // test w/o the full text index.
    properties.setProperty(Options.INLINE_DATE_TIMES, "true");

    AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new LinkedHashSet<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      terms.add(f.createLiteral("2008-03-22T00:00:00", f.createURI(XSD.DATETIME.toString())));

      terms.add(f.createLiteral("2007-12-25T00:00:00", f.createURI(XSD.DATETIME.toString())));

      terms.add(f.createLiteral("2008-03-22", f.createURI(XSD.DATE.toString())));

      terms.add(f.createLiteral("2007-12-25", f.createURI(XSD.DATE.toString())));

      terms.add(f.createLiteral("00:00:00", f.createURI(XSD.TIME.toString())));

      terms.add(f.createLiteral("13:15:42", f.createURI(XSD.TIME.toString())));

      terms.add(f.createLiteral("---22", f.createURI(XSD.GDAY.toString())));

      terms.add(f.createLiteral("---25", f.createURI(XSD.GDAY.toString())));

      terms.add(f.createLiteral("--03", f.createURI(XSD.GMONTH.toString())));

      terms.add(f.createLiteral("--12", f.createURI(XSD.GMONTH.toString())));

      terms.add(f.createLiteral("--03-22", f.createURI(XSD.GMONTHDAY.toString())));

      terms.add(f.createLiteral("--12-25", f.createURI(XSD.GMONTHDAY.toString())));

      terms.add(f.createLiteral("2008", f.createURI(XSD.GYEAR.toString())));

      terms.add(f.createLiteral("1976", f.createURI(XSD.GYEAR.toString())));

      terms.add(f.createLiteral("2008-03", f.createURI(XSD.GYEARMONTH.toString())));

      terms.add(f.createLiteral("1976-12", f.createURI(XSD.GYEARMONTH.toString())));

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

          if (log.isInfoEnabled()) log.info(ids.get(id));

          if (log.isInfoEnabled()) log.info(ids2.get(id));

          assertEquals("Id mapped to a different term? : termId=" + id, ids.get(id), ids2.get(id));
        }
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
