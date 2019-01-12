package org.embergraph.rdf.vocab;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.core.EmbergraphCoreVocabulary_v20151210;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class TestMultiVocabulary extends EmbergraphCoreVocabulary_v20151210 {

  /** De-serialization ctor. */
  public TestMultiVocabulary() {

    super();
  }

  /**
   * Used by {@link AbstractTripleStore#create()}.
   *
   * @param namespace The namespace of the KB instance.
   */
  public TestMultiVocabulary(final String namespace) {

    super(namespace);
  }

  @Override
  protected void addValues() {

    addDecl(new TestVocabularyDecl());

    super.addValues();
  }

  class TestVocabularyDecl implements VocabularyDecl {

    private final URI[] uris = new URI[] {new URIImpl("http://embergraph.org/Data#Position_")};

    public TestVocabularyDecl() {}

    public Iterator<URI> values() {
      return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
    }
  }
}
