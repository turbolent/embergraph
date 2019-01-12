package org.embergraph.rdf.internal;

import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase2;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.rdf.internal.impl.uri.FullyInlineURIIV;
import org.embergraph.rdf.lexicon.BlobsIndexHelper;
import org.embergraph.rdf.model.EmbergraphURI;
import org.openrdf.model.impl.URIImpl;

/** Test suite for {@link FullyInlineURIIV}. */
public class TestFullyInlineURIIV extends TestCase2 {

  public TestFullyInlineURIIV() {}

  public TestFullyInlineURIIV(String name) {
    super(name);
  }

  public void test_InlineURIIV() {

    doTest(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org")));
    doTest(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/")));
    doTest(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/foo")));
    doTest(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org:80/foo")));
  }

  private void doTest(final FullyInlineURIIV<EmbergraphURI> iv) {

    assertEquals(VTE.URI, iv.getVTE());

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

    final List<IV<?, ?>> ivs = new LinkedList<IV<?, ?>>();
    {
      ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org")));
      ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/")));
      ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org/foo")));
      ivs.add(new FullyInlineURIIV<EmbergraphURI>(new URIImpl("http://www.embergraph.org:80/foo")));
    }

    final IV<?, ?>[] e = ivs.toArray(new IV[0]);

    AbstractEncodeDecodeKeysTestCase.doEncodeDecodeTest(e);

    AbstractEncodeDecodeKeysTestCase.doComparatorTest(e);
  }
}
