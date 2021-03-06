/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.embergraph.rdf.rio.turtle;

import java.io.OutputStream;
import java.io.Writer;
import org.embergraph.rdf.ServiceProviderHook;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;

/*
 * An RDR-aware {@link RDFWriterFactory} for Turtle writers.
 *
 * @author Arjohn Kampman
 * @openrdf
 * @see http://wiki.blazegraph.com/wiki/index.php/Reification_Done_Right
 */
public class EmbergraphTurtleWriterFactory implements RDFWriterFactory {

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

  /** Returns a new instance of {@link EmbergraphTurtleWriter}. */
  @Override
  public RDFWriter getWriter(final OutputStream out) {
    return new EmbergraphTurtleWriter(out);
  }

  /** Returns a new instance of {@link EmbergraphTurtleWriter}. */
  @Override
  public RDFWriter getWriter(final Writer writer) {
    return new EmbergraphTurtleWriter(writer);
  }
}
