package org.embergraph.rdf.internal;

import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase2;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.lexicon.BlobsIndexHelper;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/** Test suite for {@link FullyInlineTypedLiteralIV}. */
public class TestFullyInlineTypedLiteralIV extends TestCase2 {

  public TestFullyInlineTypedLiteralIV() {}

  public TestFullyInlineTypedLiteralIV(String name) {
    super(name);
  }

  public void test_InlineLiteralIV_plain() {

    doTest(new FullyInlineTypedLiteralIV<>(""));
    doTest(new FullyInlineTypedLiteralIV<>(" "));
    doTest(new FullyInlineTypedLiteralIV<>("1"));
    doTest(new FullyInlineTypedLiteralIV<>("12"));
    doTest(new FullyInlineTypedLiteralIV<>("123"));
  }

  // Removed in backport.    Not used in pre-RDF 1.1 versions
  //	public void test_InlineLiteralIV_languageCode() {
  //
  //        doTest(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("","en",null/*datatype*/));
  //        doTest(new FullyInlineTypedLiteralIV<EmbergraphLiteral>(" ","en",null/*datatype*/));
  //        doTest(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("1","en",null/*datatype*/));
  //        doTest(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("12","fr",null/*datatype*/));
  //        doTest(new FullyInlineTypedLiteralIV<EmbergraphLiteral>("123","de",null/*datatype*/));
  //
  //	}

  public void test_InlineLiteralIV_datatypeURI() {

    final URI datatype = new URIImpl("http://www.embergraph.org");

    doTest(new FullyInlineTypedLiteralIV<>("", null, datatype));
    doTest(new FullyInlineTypedLiteralIV<>(" ", null, datatype));
    doTest(new FullyInlineTypedLiteralIV<>("1", null, datatype));
    doTest(new FullyInlineTypedLiteralIV<>("12", null, datatype));
    doTest(new FullyInlineTypedLiteralIV<>("123", null, datatype));
  }

  private void doTest(final FullyInlineTypedLiteralIV<EmbergraphLiteral> iv) {

    assertEquals(VTE.LITERAL, iv.getVTE());

    assertTrue(iv.isInline());

    assertFalse(iv.isExtension());

    assertEquals(DTE.XSDString, iv.getDTE());

    final BlobsIndexHelper h = new BlobsIndexHelper();

    final IKeyBuilder keyBuilder = h.newKeyBuilder();

    final byte[] key = IVUtility.encode(keyBuilder, iv).getKey();

    final IV<?, ?> actual = IVUtility.decode(key);

    assertEquals(iv, actual);

    assertEquals(key.length, iv.byteLength());

    assertEquals(key.length, actual.byteLength());
  }

  public void test_encodeDecode_comparator() {

    final List<IV<?, ?>> ivs = new LinkedList<>();
    {
      final URI datatype = new URIImpl("http://www.embergraph.org");

      ivs.add(new FullyInlineTypedLiteralIV<>(""));
      ivs.add(new FullyInlineTypedLiteralIV<>(" "));
      ivs.add(new FullyInlineTypedLiteralIV<>("1"));
      ivs.add(new FullyInlineTypedLiteralIV<>("12"));
      ivs.add(new FullyInlineTypedLiteralIV<>("123"));

      ivs.add(new FullyInlineTypedLiteralIV<>("", "en", null /*datatype*/));
      ivs.add(new FullyInlineTypedLiteralIV<>(" ", "en", null /*datatype*/));
      ivs.add(new FullyInlineTypedLiteralIV<>("1", "en", null /*datatype*/));
      ivs.add(new FullyInlineTypedLiteralIV<>("12", "fr", null /*datatype*/));
      ivs.add(new FullyInlineTypedLiteralIV<>("123", "de", null /*datatype*/));

      ivs.add(new FullyInlineTypedLiteralIV<>("", null, datatype));
      ivs.add(new FullyInlineTypedLiteralIV<>(" ", null, datatype));
      ivs.add(new FullyInlineTypedLiteralIV<>("1", null, datatype));
      ivs.add(new FullyInlineTypedLiteralIV<>("12", null, datatype));
      ivs.add(new FullyInlineTypedLiteralIV<>("123", null, datatype));
    }

    final IV<?, ?>[] e = ivs.toArray(new IV[0]);

    AbstractEncodeDecodeKeysTestCase.doEncodeDecodeTest(e);

    AbstractEncodeDecodeKeysTestCase.doComparatorTest(e);
  }
}
