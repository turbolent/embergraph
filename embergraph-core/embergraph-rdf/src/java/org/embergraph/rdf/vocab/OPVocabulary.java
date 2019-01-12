package org.embergraph.rdf.vocab;

import org.embergraph.rdf.vocab.decls.DCAllVocabularyDecl;
import org.embergraph.rdf.vocab.decls.OPVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFSVocabularyDecl;
import org.embergraph.rdf.vocab.decls.RDFVocabularyDecl;
import org.embergraph.rdf.vocab.decls.XMLSchemaVocabularyDecl;

public class OPVocabulary extends BaseVocabulary {

  /** De-serialization ctor. */
  public OPVocabulary() {

    super();
  }

  /*
   * Used by {@link AbstractTripleStore#create()}.
   *
   * @param namespace The namespace of the KB instance.
   */
  public OPVocabulary(final String namespace) {

    super(namespace);
  }

  @Override
  protected void addValues() {

    addDecl(new RDFVocabularyDecl());
    addDecl(new RDFSVocabularyDecl());
    addDecl(new DCAllVocabularyDecl());
    addDecl(new XMLSchemaVocabularyDecl());
    addDecl(new OPVocabularyDecl());
  }
}
