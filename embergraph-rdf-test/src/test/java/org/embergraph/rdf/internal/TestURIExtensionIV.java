package org.embergraph.rdf.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import junit.framework.TestCase2;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.uri.URIExtensionIV;
import org.embergraph.rdf.lexicon.BlobsIndexHelper;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.vocab.BaseVocabulary;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDFS;

/** Test suite for {@link URIExtensionIV}. */
public class TestURIExtensionIV extends TestCase2 {

  public TestURIExtensionIV() {}

  public TestURIExtensionIV(String name) {
    super(name);
  }

  /*
   * Must test using Vocabulary to test asValue().
   */

  private String namespace;
  private BaseVocabulary vocab;
  //	private EmbergraphValueFactory valueFactory;

  protected void setUp() throws Exception {

    super.setUp();

    namespace = getName();

    //        valueFactory  = EmbergraphValueFactoryImpl.getInstance(namespace);

    vocab = new MockVocabulary(namespace);

    vocab.init();
  }

  protected void tearDown() throws Exception {

    super.tearDown();

    vocab = null;

    namespace = null;

    //        if (valueFactory != null)
    //            valueFactory.remove();
    //
    //        valueFactory = null;

  }

  private static class MockVocabularyDecl implements VocabularyDecl {

    private static final URI[] uris =
        new URI[] {
          new URIImpl("http://www.embergraph.org/"),
          new URIImpl(RDFS.NAMESPACE),
          new URIImpl("http://www.Department0.University0.edu/"),
        };

    @Override
    public Iterator<URI> values() {
      return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
    }
  }

  public static class MockVocabulary extends BaseVocabulary {

    /** De-serialization. */
    public MockVocabulary() {
      super();
    }

    /** @param namespace */
    public MockVocabulary(String namespace) {
      super(namespace);
    }

    @Override
    protected void addValues() {
      addDecl(new MockVocabularyDecl());
      //            addDecl(new LUBMVocabularyDecl());
    }
  }

  private URIExtensionIV<EmbergraphURI> newFixture(final URI uri) {

    final String namespace = uri.getNamespace();

    final URI namespaceURI = new URIImpl(namespace);

    final IV<?, ?> namespaceIV = vocab.get(namespaceURI);

    if (namespaceIV == null) {

      fail("Not declared by vocabulary: namespace: " + namespace);
    }

    final FullyInlineTypedLiteralIV<EmbergraphLiteral> localNameIV =
        new FullyInlineTypedLiteralIV<EmbergraphLiteral>(uri.getLocalName());

    final URIExtensionIV<EmbergraphURI> iv =
        new URIExtensionIV<EmbergraphURI>(localNameIV, namespaceIV);

    return iv;
  }

  public void test_InlineURIIV() {

    //        doTest(new URIImpl("http://www.embergraph.org"));
    doTest(new URIImpl("http://www.embergraph.org/"));
    doTest(new URIImpl("http://www.embergraph.org/foo"));
    doTest(RDFS.CLASS);
    doTest(RDFS.SUBPROPERTYOF);
    doTest(new URIImpl("http://www.Department0.University0.edu/UndergraduateStudent488"));
    doTest(new URIImpl("http://www.Department0.University0.edu/GraduateStudent15"));
    //        doTest(new URIImpl("http://www.embergraph.org:80/foo"));

  }

  private void doTest(final URI uri) {

    final URIExtensionIV<EmbergraphURI> iv = newFixture(uri);

    assertEquals(VTE.URI, iv.getVTE());

    assertTrue(iv.isInline());

    assertTrue(iv.isExtension());

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

      //            ivs.add(newFixture(new URIImpl("http://www.embergraph.org")));
      ivs.add(newFixture(new URIImpl("http://www.embergraph.org/")));
      ivs.add(newFixture(new URIImpl("http://www.embergraph.org/foo")));
      ivs.add(newFixture(RDFS.CLASS));
      ivs.add(newFixture(RDFS.SUBPROPERTYOF));
      ivs.add(
          newFixture(
              new URIImpl("http://www.Department0.University0.edu/UndergraduateStudent488")));
      ivs.add(newFixture(new URIImpl("http://www.Department0.University0.edu/GraduateStudent15")));
      //            ivs.add(newFixture(new URIImpl("http://www.embergraph.org:80/foo")));

    }

    final IV<?, ?>[] e = ivs.toArray(new IV[0]);

    AbstractEncodeDecodeKeysTestCase.doEncodeDecodeTest(e);

    AbstractEncodeDecodeKeysTestCase.doComparatorTest(e);
  }
}
