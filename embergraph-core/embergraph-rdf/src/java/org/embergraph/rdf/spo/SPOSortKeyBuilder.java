package org.embergraph.rdf.spo;

import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.ISortKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.util.Bytes;

/*
* Class produces unsigned byte[] sort keys for {@link ISPO}s. This implementation is NOT
 * thread-safe.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SPOSortKeyBuilder implements ISortKeyBuilder<ISPO> {

  private final int arity;
  private final IKeyBuilder keyBuilder;

  public SPOSortKeyBuilder(final int arity) {
    assert arity == 3 || arity == 4;
    this.arity = arity;
    this.keyBuilder = new KeyBuilder(Bytes.SIZEOF_LONG * arity);
  }

  /** Distinct iff the {s:p:o} are distinct. */
  public byte[] getSortKey(final ISPO spo) {

    keyBuilder.reset();

    spo.s().encode(keyBuilder);
    spo.p().encode(keyBuilder);
    spo.o().encode(keyBuilder);

    if (arity == 4) {

      spo.c().encode(keyBuilder);
    }

    return keyBuilder.getKey();
  }
}
