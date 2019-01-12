package org.embergraph.rdf.lexicon;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import org.embergraph.io.SerializerUtil;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.NotMaterializedException;
import org.embergraph.rdf.internal.impl.AbstractIV;
import org.embergraph.rdf.internal.impl.bnode.NumericBNodeIV;
import org.embergraph.rdf.internal.impl.bnode.UUIDBNodeIV;
import org.embergraph.rdf.internal.impl.literal.UUIDLiteralIV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.internal.impl.literal.XSDDecimalIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;

/*
 * Test suite for {@link IV#getValue()}, which provides a cache on the {@link IV} for a materialized
 * {@link EmbergraphValue}.
 *
 * @author thompsonbry
 */
public class TestIVCache extends AbstractTripleStoreTestCase {

  public TestIVCache() {}

  public TestIVCache(String name) {
    super(name);
  }

  /** Unit test for {@link IV#getValue()} and friends. */
  public void test_getValue() {

    final AbstractTripleStore store = getStore(getProperties());

    try {

      final LexiconRelation lex = store.getLexiconRelation();

      final EmbergraphValueFactory f = lex.getValueFactory();

      final EmbergraphURI uri = f.createURI("http://www.embergraph.org");
      final EmbergraphBNode bnd = f.createBNode(); // "12");
      final EmbergraphLiteral lit = f.createLiteral("embergraph");

      final EmbergraphValue[] a = new EmbergraphValue[] {uri, bnd, lit};

      // insert some terms.
      lex.addTerms(a, a.length, false /*readOnly*/);

      doTest(lex, uri.getIV(), uri);
      doTest(lex, bnd.getIV(), bnd);
      doTest(lex, lit.getIV(), lit);

      doTest(lex, new XSDBooleanIV<EmbergraphLiteral>(true));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>((byte) 1));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>((short) 1));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>(1));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>(1L));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>(1f));
      doTest(lex, new XSDNumericIV<EmbergraphLiteral>(1d));
      doTest(lex, new XSDIntegerIV<EmbergraphLiteral>(BigInteger.valueOf(1L)));
      doTest(lex, new XSDDecimalIV<EmbergraphLiteral>(BigDecimal.valueOf(1d)));

      doTest(lex, new UUIDBNodeIV<EmbergraphBNode>(UUID.randomUUID()));
      doTest(lex, new NumericBNodeIV<EmbergraphBNode>(1));

      doTest(lex, new UUIDLiteralIV<EmbergraphLiteral>(UUID.randomUUID()));

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Variant used *except* for {@link BlobIV}s.
   *
   * @param lex
   * @param iv
   */
  private void doTest(final LexiconRelation lex, final IV iv) {

    doTest(lex, iv, null /*given*/);
  }

  /*
   * Core impl.
   *
   * @param lex
   * @param iv
   * @param given
   */
  @SuppressWarnings("unchecked")
  private void doTest(final LexiconRelation lex, final IV iv, final EmbergraphValue given) {

    // not found in the cache.
    try {
      iv.getValue();
      fail("Expecting: " + NotMaterializedException.class);
    } catch (NotMaterializedException e) {
      // ignore.
    }

    /*
     * Set on the cache (TermId) or materialize in the cache (everthing
     * else).
     */
    final EmbergraphValue val;
    if (!iv.isInline()) {
      ((AbstractIV<EmbergraphValue, ?>) iv).setValue(val = given);
    } else {
      val = iv.asValue(lex);
    }

    // found in the cache.
    assertTrue(val == iv.getValue());

    // round-trip (de-)serialization.
    final IV<?, ?> iv2 = (IV<?, ?>) SerializerUtil.deserialize(SerializerUtil.serialize(iv));

    // this is a distinct IV instance.
    assertTrue(iv != iv2);

    /* Note: All IVs currently send their cached value across the wire.
     *
     * @see https://sourceforge.net/apps/trac/bigdata/ticket/337
     */
    {
      //		if (iv.isInline()) {
      //
      //			/*
      //			 * For an inline IV, we drop the cached value when it is serialized
      //			 * in order to keep down the serialized object size since it is
      //			 * basically free to re-materialize the Value from the IV.
      //			 */
      //			// not found in the cache.
      //			try {
      //				iv2.getValue();
      //				fail("Expecting: " + NotMaterializedException.class);
      //			} catch (NotMaterializedException e) {
      //				// ignore.
      //			}
      //
      //		} else {

      /*
       * For a non-inline IV, the value is found in the cache, even though
       * it is not the same EmbergraphValue.
       */

      final EmbergraphValue val2 = iv2.getValue();
      // found in the cache.
      assertNotNull(val2);
      // but distinct EmbergraphValue
      assertTrue(val != val2);
      // but same value factory.
      assertTrue(val.getValueFactory() == val2.getValueFactory());
      // and compares as "equals".
      assertTrue(val.equals(val2));
    }

    if (iv.isInline()) {
      // Verify the cache is unchanged.
      assertTrue(val == iv.asValue(lex));
      assertTrue(val == iv.getValue());
    }

    //		/*
    //		 * Drop the value and verify that it is no longer found in the cache.
    //		 */
    //		iv.dropValue();
    //
    //		// not found in the cache.
    //		try {
    //			iv.getValue();
    //			fail("Expecting: " + NotMaterializedException.class);
    //		} catch (NotMaterializedException e) {
    //			// ignore.
    //		}

  }
}
