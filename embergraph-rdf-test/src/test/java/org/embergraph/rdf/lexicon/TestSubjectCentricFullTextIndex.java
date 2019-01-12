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
 * Created on Dec 19, 2007
 */

package org.embergraph.rdf.lexicon;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import junit.framework.AssertionFailedError;
import org.apache.log4j.Logger;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.ITextIndexer.FullTextQuery;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.spo.TestSPOKeyOrder;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.store.BD;
import org.embergraph.rdf.store.EmbergraphValueIteratorImpl;
import org.embergraph.search.Hit;
import org.embergraph.search.Hiterator;
import org.embergraph.striterator.ChunkedWrappedIterator;
import org.embergraph.striterator.Resolver;
import org.embergraph.striterator.Striterator;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;

/*
 * Test of adding terms with the full text index enabled and of lookup of terms by tokens which
 * appear within those terms.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @deprecated Feature was never completed due to scalability issues. See BZLG-1548, BLZG-563.
 */
@Deprecated
public class TestSubjectCentricFullTextIndex extends AbstractTripleStoreTestCase {

  private static final transient Logger log =
      Logger.getLogger(TestSubjectCentricFullTextIndex.class);

  /** */
  public TestSubjectCentricFullTextIndex() {}

  /** @param name */
  public TestSubjectCentricFullTextIndex(String name) {
    super(name);
  }

  @Override
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    // enable the full text index.
    properties.setProperty(AbstractTripleStore.Options.TEXT_INDEX, "true");
    properties.setProperty(AbstractTripleStore.Options.SUBJECT_CENTRIC_TEXT_INDEX, "true");

    return properties;
  }

  //    /*
  //     * Test helper verifies that the term is not in the lexicon, adds the term
  //     * to the lexicon, verifies that the term can be looked up by its assigned
  //     * term identifier, verifies that the term is now in the lexicon, and
  //     * verifies that adding the term again returns the same term identifier.
  //     *
  //     * @param term
  //     *            The term.
  //     */
  //    protected void doAddTermTest(final AbstractTripleStore store,
  //            final EmbergraphValue term) {
  //
  //        assertEquals(NULL, store.getIV(term));
  //
  //        final IV<?,?> id = store.addTerm(term);
  //
  //        assertNotSame(NULL, id);
  //
  //        assertEquals(id, store.getIV(term));
  //
  //        assertEquals(term, store.getTerm(id));
  //
  //        assertEquals(id, store.addTerm(term));
  //
  //    }

  private void assertExpectedHits(
      final AbstractTripleStore store,
      final String query,
      final String languageCode,
      final EmbergraphValue[] expected) {

    assertExpectedHits(store, query, languageCode, 0f /* minCosine */, expected);
  }

  @SuppressWarnings("unchecked")
  private void assertExpectedHits(
      final AbstractTripleStore store,
      final String query,
      final String languageCode,
      final float minCosine,
      final EmbergraphValue[] expected) {

    final Hiterator hitr =
        store
            .getLexiconRelation()
            .getSubjectCentricSearchEngine()
            .search(
                new FullTextQuery(
                    query,
                    languageCode,
                    false /* prefixMatch */,
                    null, // regex
                    false /* matchAllTerms */,
                    false, // matchExact
                    minCosine,
                    1.0d /* maxCosine */,
                    1 /* minRank */,
                    Integer.MAX_VALUE /* maxRank */,
                    Long.MAX_VALUE, // 2L/* timeout */,
                    TimeUnit.MILLISECONDS // TimeUnit.SECONDS
                    ));

    // assertEquals("#hits", (long) expected.length, itr.size());

    final ICloseableIterator<EmbergraphValue> itr2 =
        new EmbergraphValueIteratorImpl(
            store,
            new ChunkedWrappedIterator<IV>(
                new Striterator(hitr)
                    .addFilter(
                        new Resolver() {
                          private static final long serialVersionUID = 1L;

                          @Override
                          protected Object resolve(Object e) {
                            final Hit hit = (Hit) e;
                            if (log.isDebugEnabled()) {
                              log.debug(hit);
                            }
                            return hit.getDocId();
                          }
                        })));

    try {

      TestSPOKeyOrder.assertSameIteratorAnyOrder(expected, itr2);

    } catch (AssertionFailedError ex) {

      fail(
          "minCosine=" + minCosine + ", expected=" + Arrays.toString(expected) + ", actual=" + hitr,
          ex);

    } finally {

      itr2.close();
    }
  }

  private LiteralImpl getLargeLiteral(final AbstractTripleStore store) {

    final int len = store.getLexiconRelation().getLexiconConfiguration().getBlobsThreshold();

    final StringBuilder sb = new StringBuilder(len);

    final String[] tokens =
        new String[] {
          "apple", "mary", "john", "barley", "mellow", "pudding", "fries", "peal", "gadzooks"
        };

    for (int i = 0; sb.length() < len; i++) {

      sb.append(tokens[(i % tokens.length)]);

      sb.append(" ");
    }

    final String s = sb.toString();

    if (log.isInfoEnabled()) log.info("length(s)=" + s.length());

    return new LiteralImpl(s);
  }

  public void test_SingleSubject() {

    AbstractTripleStore store = getStore();

    try {

      assertNotNull(store.getLexiconRelation().getSearchEngine());

      final EmbergraphValueFactory f = store.getValueFactory();

      final EmbergraphURI s = f.createURI(BD.NAMESPACE + "s");

      final EmbergraphURI p = f.createURI(BD.NAMESPACE + "p");

      final LiteralImpl largeLiteral = getLargeLiteral(store);

      final EmbergraphValue[] terms =
          new EmbergraphValue[] {
            f.createLiteral("abc"),
            f.createLiteral("abc", "en"),
            f.createLiteral("good day", "en"),
            f.createLiteral("gutten tag", "de"),
            f.createLiteral("tag team", "en"),
            f.createLiteral("the first day", "en"), // // 'the' is a stopword.
            f.createURI("http://www.embergraph.org"),
            f.asValue(RDF.TYPE),
            f.asValue(RDFS.SUBCLASSOF),
            f.asValue(XMLSchema.DECIMAL),
            f.createBNode(UUID.randomUUID().toString()),
            f.createBNode("a12"),
            f.asValue(largeLiteral),
          };

      for (EmbergraphValue o : terms) {

        store.addStatement(s, p, o);
      }

      store.commit();

      // build the subject-centric full text index.
      store.getLexiconRelation().buildSubjectCentricTextIndex();

      if (log.isInfoEnabled()) {
        log.info("\n" + store.dumpStore(true, false, false));
      }

      /*
       * Note: the language code is only used when tokenizing literals. It
       * IS NOT applied as a filter to the recovered literals.
       */

      assertExpectedHits(store, "abc", null /* languageCode */, new EmbergraphValue[] {s});

      assertExpectedHits(store, "tag", "en", new EmbergraphValue[] {s});

      assertExpectedHits(store, "tag", "de", new EmbergraphValue[] {s});

      assertExpectedHits(
          store,
          "GOOD DAY",
          "en",
          .0f, // minCosine
          new EmbergraphValue[] {s});

      assertExpectedHits(
          store,
          "GOOD DAY",
          "en",
          .0f, // minCosine
          new EmbergraphValue[] {s});

      assertExpectedHits(
          store,
          "day",
          "en",
          .0f, // minCosine
          new EmbergraphValue[] {s});

      // 'the' is a stopword, so there are no hits.
      assertExpectedHits(store, "the", "en", new EmbergraphValue[] {});

      // BLOB
      assertExpectedHits(
          store,
          largeLiteral.getLabel(),
          null /*lang*/,
          .0f, // minCosine
          new EmbergraphValue[] {s});

      /*
       * re-open the store before search to verify that the data were made
       * restart safe.
       */
      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);
      }

      // re-verify the full text index.
      {
        assertNotNull(store.getLexiconRelation().getSubjectCentricSearchEngine());

        assertExpectedHits(store, "abc", null /* languageCode */, new EmbergraphValue[] {s});

        assertExpectedHits(store, "tag", "en", new EmbergraphValue[] {s});

        assertExpectedHits(store, "tag", "de", new EmbergraphValue[] {s});

        assertExpectedHits(
            store,
            "GOOD DAY",
            "en",
            .0f, // minCosine
            new EmbergraphValue[] {s});

        assertExpectedHits(
            store,
            "GOOD DAY",
            "en",
            .0f, // minCosine
            new EmbergraphValue[] {s});

        assertExpectedHits(
            store,
            "day",
            "en",
            .0f, // minCosine
            new EmbergraphValue[] {s});

        // BLOB
        assertExpectedHits(
            store,
            largeLiteral.getLabel(),
            null /*lang*/,
            .0f, // minCosine
            new EmbergraphValue[] {s});
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }

  public void test_MultiSubject() {

    AbstractTripleStore store = getStore();

    try {

      assertNotNull(store.getLexiconRelation().getSearchEngine());

      final EmbergraphValueFactory f = store.getValueFactory();

      final EmbergraphURI s1 = f.createURI(BD.NAMESPACE + "s1");

      final EmbergraphURI s2 = f.createURI(BD.NAMESPACE + "s2");

      final EmbergraphURI s3 = f.createURI(BD.NAMESPACE + "s3");

      final EmbergraphURI p = f.createURI(BD.NAMESPACE + "p");

      final LiteralImpl largeLiteral = getLargeLiteral(store);

      final EmbergraphValue[] terms =
          new EmbergraphValue[] {
            f.createLiteral("abc"),
            f.createLiteral("abc", "en"),
            f.createLiteral("good day", "en"),
            f.createLiteral("gutten tag", "de"),
            f.createLiteral("tag team", "en"),
            f.createLiteral("the first day", "en"), // // 'the' is a stopword.
            f.createURI("http://www.embergraph.org"),
            f.asValue(RDF.TYPE),
            f.asValue(RDFS.SUBCLASSOF),
            f.asValue(XMLSchema.DECIMAL),
            f.createBNode(UUID.randomUUID().toString()),
            f.createBNode("a12"),
            f.asValue(largeLiteral),
          };

      for (EmbergraphValue o : terms) {

        store.addStatement(s1, p, o);
      }

      for (int i = 0; i < 3; i++) {

        store.addStatement(s2, p, terms[i]);
      }

      for (int i = 3; i < 6; i++) {

        store.addStatement(s3, p, terms[i]);
      }

      store.commit();

      // build the subject-centric full text index.
      store.getLexiconRelation().buildSubjectCentricTextIndex();

      if (log.isInfoEnabled()) {
        log.info("\n" + store.dumpStore(true, false, false));
      }

      /*
       * Note: the language code is only used when tokenizing literals. It
       * IS NOT applied as a filter to the recovered literals.
       */

      assertExpectedHits(store, "abc", null /* languageCode */, new EmbergraphValue[] {s1, s2});

      assertExpectedHits(store, "tag", "en", new EmbergraphValue[] {s1, s3});

      assertExpectedHits(store, "tag", "de", new EmbergraphValue[] {s1, s3});

      assertExpectedHits(
          store,
          "GOOD DAY",
          "en",
          .0f, // minCosine
          new EmbergraphValue[] {s1, s2, s3});

      assertExpectedHits(
          store,
          "day",
          "en",
          .0f, // minCosine
          new EmbergraphValue[] {s1, s2, s3});

      // 'the' is a stopword, so there are no hits.
      assertExpectedHits(store, "the", "en", new EmbergraphValue[] {});

      // BLOB
      assertExpectedHits(
          store,
          largeLiteral.getLabel(),
          null /*lang*/,
          .0f, // minCosine
          new EmbergraphValue[] {s1});

      /*
       * re-open the store before search to verify that the data were made
       * restart safe.
       */
      if (store.isStable()) {

        store.commit();

        store = reopenStore(store);
      }

      // re-verify the full text index.
      {
        assertNotNull(store.getLexiconRelation().getSubjectCentricSearchEngine());

        assertExpectedHits(store, "abc", null /* languageCode */, new EmbergraphValue[] {s1, s2});

        assertExpectedHits(store, "tag", "en", new EmbergraphValue[] {s1, s3});

        assertExpectedHits(store, "tag", "de", new EmbergraphValue[] {s1, s3});

        assertExpectedHits(
            store,
            "GOOD DAY",
            "en",
            .0f, // minCosine
            new EmbergraphValue[] {s1, s2, s3});

        assertExpectedHits(
            store,
            "day",
            "en",
            .0f, // minCosine
            new EmbergraphValue[] {s1, s2, s3});

        // BLOB
        assertExpectedHits(
            store,
            largeLiteral.getLabel(),
            null /*lang*/,
            .0f, // minCosine
            new EmbergraphValue[] {s1});
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
