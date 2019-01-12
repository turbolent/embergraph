/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.embergraph.rdf.rio.ntriples;

import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.openrdf.rio.RDFParser;

/**
 * JUnit test for the N-Triples parser.
 *
 * @author Arjohn Kampman
 */
public class EmbergraphNTriplesParserTest extends EmbergraphNTriplesParserTestCase {

  private EmbergraphValueFactory valueFactory;

  protected void setUp() throws Exception {
    super.setUp();
    valueFactory = EmbergraphValueFactoryImpl.getInstance(getName());
  }

  protected void tearDown() throws Exception {
    if (valueFactory != null) {
      valueFactory.remove();
      valueFactory = null;
    }
    super.tearDown();
  }

  @Override
  protected RDFParser createRDFParser() {
    /*
     * Note: Requires the EmbergraphValueFactory for SIDs support.
     */
    return new EmbergraphNTriplesParser(valueFactory);
  }
}
