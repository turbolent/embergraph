package org.embergraph.samples;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class LUBM {

  static final String NS = "http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#";

  static final URI FULL_PROFESSOR = new URIImpl(NS + "FullProfessor");

  static final URI PROFESSOR = new URIImpl(NS + "Professor");

  static final URI WORKS_FOR = new URIImpl(NS + "worksFor");

  static final URI NAME = new URIImpl(NS + "name");

  static final URI EMAIL_ADDRESS = new URIImpl(NS + "emailAddress");

  static final URI TELEPHONE = new URIImpl(NS + "telephone");
}
