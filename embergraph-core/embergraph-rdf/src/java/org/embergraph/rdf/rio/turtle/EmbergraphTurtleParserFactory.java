/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.embergraph.rdf.rio.turtle;

import org.embergraph.rdf.ServiceProviderHook;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.turtle.TurtleParser;

/*
 * An RDR-aware {@link RDFParserFactory} for Turtle parsers.
 *
 * @author Arjohn Kampman
 * @openrdf
 * @see http://wiki.blazegraph.com/wiki/index.php/Reification_Done_Right
 */
public class EmbergraphTurtleParserFactory implements RDFParserFactory {

  /*
   * Returns {@link ServiceProviderHook#TURTLE_RDR}.
   *
   * @see <a href="http://trac.blazegraph.com/ticket/1038" >RDR RDF parsers not always discovered
   *     </a>
   */
  @Override
  public RDFFormat getRDFFormat() {
    return ServiceProviderHook.TURTLE_RDR;
  }

  /** Returns a new instance of {@link TurtleParser}. */
  @Override
  public RDFParser getParser() {
    return new EmbergraphTurtleParser();
  }
}
