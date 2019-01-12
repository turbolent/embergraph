/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.embergraph.rdf.rio.ntriples;

import org.embergraph.rdf.ServiceProviderHook;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;

/**
 * An RDR-aware {@link RDFParserFactory} for N-Triples parsers.
 *
 * @author Arjohn Kampman
 * @openrdf
 * @see http://wiki.blazegraph.com/wiki/index.php/Reification_Done_Right
 */
public class EmbergraphNTriplesParserFactory implements RDFParserFactory {

  /** Returns {@link ServiceProviderHook#NTRIPLES_RDR}. */
  @Override
  public RDFFormat getRDFFormat() {
    return ServiceProviderHook.NTRIPLES_RDR;
  }

  /** Returns a new instance of EmbergraphNTriplesParser. */
  @Override
  public RDFParser getParser() {
    return new EmbergraphNTriplesParser();
  }
}
