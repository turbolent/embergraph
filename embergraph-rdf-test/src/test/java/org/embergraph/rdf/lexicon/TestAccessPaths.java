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
import java.util.Iterator;
import java.util.Properties;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStore.Options;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.striterator.IKeyOrder;
import org.openrdf.model.vocabulary.RDF;

/*
 * Test suite for {@link LexiconRelation#newAccessPath(IIndexManager, IPredicate, IKeyOrder)}.
 *
 * <p>Note: If you query with {@link IV} or {@link EmbergraphValue} already cached (on one another
 * or in the termsCache) then the cached value will be returned.
 *
 * <p>Note: Blank nodes will not unify with themselves unless you are using told blank node
 * semantics.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAccessPaths extends AbstractTripleStoreTestCase {

  /** */
  public TestAccessPaths() {
    super();
  }

  /** @param name */
  public TestAccessPaths(String name) {
    super(name);
  }

  /** */
  public void test_TERMS_accessPaths() {

    final Properties properties = getProperties();

    // test w/o predefined vocab.
    properties.setProperty(Options.VOCABULARY_CLASS, NoVocabulary.class.getName());

    // test w/o the full text index.
    properties.setProperty(Options.TEXT_INDEX, "false");

    final AbstractTripleStore store = getStore(properties);

    try {

      final Collection<EmbergraphValue> terms = new HashSet<EmbergraphValue>();

      // lookup/add some values.
      final EmbergraphValueFactory f = store.getValueFactory();

      final EmbergraphValue rdfType;
      final EmbergraphValue largeLiteral;
      terms.add(rdfType = f.asValue(RDF.TYPE));
      terms.add(f.asValue(RDF.PROPERTY));
      terms.add(f.createLiteral("test"));
      terms.add(f.createLiteral("test", "en"));
      terms.add(f.createLiteral("10", f.createURI("http://www.w3.org/2001/XMLSchema#int")));

      terms.add(f.createLiteral("12", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.0", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(f.createLiteral("12.00", f.createURI("http://www.w3.org/2001/XMLSchema#float")));

      terms.add(largeLiteral = f.createLiteral(TestAddTerms.getVeryLargeLiteral()));

      if (store.getLexiconRelation().isStoreBlankNodes()) {
        /*
         * Note: Blank nodes will not round trip through the lexicon
         * unless the "told bnodes" is enabled.
         */
        terms.add(f.createBNode());
        terms.add(f.createBNode("a"));
      }

      final int size = terms.size();

      final EmbergraphValue[] a = terms.toArray(new EmbergraphValue[size]);

      // resolve term ids.
      store.getLexiconRelation().addTerms(a, size, false /* readOnly */);

      // populate map w/ the assigned term identifiers.
      final Collection<IV> ids = new ArrayList<IV>();

      for (EmbergraphValue t : a) {

        ids.add(t.getIV());
      }

      // Test id2terms for reverse lookup.

      doAccessPathTest(rdfType, LexiconKeyOrder.ID2TERM, store);

      doAccessPathTest(largeLiteral, LexiconKeyOrder.BLOBS, store);

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Test the access path.
   *
   * @param expected The {@link EmbergraphValue} with its {@link IV}.
   * @param expectedKeyOrder The keyorder to be used.
   * @param store The store.
   */
  private void doAccessPathTest(
      final EmbergraphValue expected,
      final IKeyOrder<EmbergraphValue> expectedKeyOrder,
      final AbstractTripleStore store) {

    assertNotNull(expected.getIV());

    final LexiconRelation r = store.getLexiconRelation();

    @SuppressWarnings("unchecked")
    final IVariable<EmbergraphValue> termvar = Var.var("termvar");

    final IPredicate<EmbergraphValue> predicate =
        LexPredicate.reverseInstance(
            r.getNamespace(), ITx.UNISOLATED, termvar, new Constant<IV>(expected.getIV()));

    final IKeyOrder<EmbergraphValue> keyOrder = r.getKeyOrder(predicate);

    assertEquals(expectedKeyOrder, keyOrder);

    final IAccessPath<EmbergraphValue> ap =
        r.newAccessPath(store.getIndexManager(), predicate, keyOrder);

    assertEquals(1, ap.rangeCount(false /* exact */));
    assertEquals(1, ap.rangeCount(true /* exact */));

    final Iterator<EmbergraphValue> itr = ap.iterator();
    assertTrue(itr.hasNext());
    final EmbergraphValue actual = itr.next();
    assertFalse(itr.hasNext());

    assertEquals(expected, actual);

    assertEquals(expected.getIV(), actual.getIV());
  }
}
