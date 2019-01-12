package org.embergraph.rdf.store;

import org.embergraph.rdf.axioms.Axioms;
import org.embergraph.rdf.vocab.RDFSVocabulary;
import org.embergraph.relation.RelationSchema;

/*
 * Extensions for additional state maintained by the {@link AbstractTripleStore} in the global row
 * store.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TripleStoreSchema extends RelationSchema {

  /** */
  private static final long serialVersionUID = -4981670950510775408L;

  private static final String ns = TripleStoreSchema.class.getPackage().getName() + ".";

  /** The serialized {@link Axioms} as configured for the database. */
  public static final String AXIOMS = ns + "axioms";

  /** The serialized {@link RDFSVocabulary} as configured for the database. */
  public static final String VOCABULARY = ns + "vocabulary";

  /** The serialized {@link GeoSpatialConfig} as configured for the database. */
  public static final String GEO_SPATIAL_CONFIG = ns + "geospatialconfig";

  /** De-serialization ctor. */
  public TripleStoreSchema() {}
}
