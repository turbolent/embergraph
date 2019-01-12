package org.embergraph.rdf.sail.config;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

/*
* Defines constants for the schema which is used by {@link EmbergraphSailFactory} and {@link
 * EmbergraphRepositoryFactory}.
 */
public class EmbergraphConfigSchema {

  /*
   * The embergraph schema namespace (<tt>http://www.embergraph.org/config/sail/embergraph#</tt>).
   */
  public static final String NAMESPACE = "http://www.embergraph.org/config/sail/embergraph#";

  /** <tt>http://www.embergraph.org/config/sail/embergraph#properties</tt> */
  public static final URI PROPERTIES;

  static {
    ValueFactory factory = ValueFactoryImpl.getInstance();
    PROPERTIES = factory.createURI(NAMESPACE, "properties");
  }
}
