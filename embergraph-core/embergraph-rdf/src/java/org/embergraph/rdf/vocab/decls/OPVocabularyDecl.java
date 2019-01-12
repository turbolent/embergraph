package org.embergraph.rdf.vocab.decls;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import org.embergraph.rdf.vocab.VocabularyDecl;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class OPVocabularyDecl implements VocabularyDecl {

  private static final URI[] uris =
      new URI[] {
        // two named graphs
        new URIImpl("file:///home/OPS/develop/openphacts/datasets/chem2bio2rdf/chembl.nt"),
        new URIImpl("http://linkedlifedata.com/resource/drugbank"),
      };

  public OPVocabularyDecl() {}

  public Iterator<URI> values() {

    return Collections.unmodifiableList(Arrays.asList(uris)).iterator();
  }
}
