package org.embergraph.rdf.inf;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOFilter;
import org.embergraph.rdf.vocab.Vocabulary;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
 * Filter matches <code>(x rdf:type rdfs:Resource).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RdfTypeRdfsResourceFilter<E extends ISPO> extends SPOFilter<E>
    implements Externalizable {

  /** */
  private static final long serialVersionUID = -2157234197316632000L;

  private IV rdfType;
  private IV rdfsResource;

  /** De-serialization ctor. */
  public RdfTypeRdfsResourceFilter() {}

  /** @param vocab */
  public RdfTypeRdfsResourceFilter(final Vocabulary vocab) {

    this.rdfType = vocab.get(RDF.TYPE);

    this.rdfsResource = vocab.get(RDFS.RESOURCE);
  }

  public boolean isValid(final Object o) {

    if (!canAccept(o)) {

      return true;
    }

    return accept((ISPO) o);
  }

  private boolean accept(final ISPO spo) {

    // reject (?x, rdf:type, rdfs:Resource )
    return spo.p().equals(rdfType) && spo.o().equals(rdfsResource);

    // Accept everything else.

  }

  /** The initial version. */
  private static final transient short VERSION0 = 0;

  /** The current version. */
  private static final transient short VERSION = VERSION0;

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    final short version = in.readShort();

    switch (version) {
      case VERSION0:
        break;
      default:
        throw new UnsupportedOperationException("Unknown version: " + version);
    }

    //        rdfType = LongPacker.unpackLong(in);
    //
    //        rdfsResource = LongPacker.unpackLong(in);

    rdfType = (IV) in.readObject();

    rdfsResource = (IV) in.readObject();
  }

  public void writeExternal(ObjectOutput out) throws IOException {

    out.writeShort(VERSION);

    //        LongPacker.packLong(out,rdfType);
    //
    //        LongPacker.packLong(out,rdfsResource);

    out.writeObject(rdfType);

    out.writeObject(rdfsResource);
  }
}
