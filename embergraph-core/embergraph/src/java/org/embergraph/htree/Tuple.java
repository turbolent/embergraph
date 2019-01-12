package org.embergraph.htree;

import org.embergraph.btree.AbstractTuple;
import org.embergraph.btree.ITupleSerializer;

/** A key-value pair used to facilitate some iterator constructs. */
class Tuple<E> extends AbstractTuple<E> {

  /*
   * @param htree
   * @param flags
   */
  public Tuple(final AbstractHTree htree, final int flags) {

    super(flags);

    if (htree == null) throw new IllegalArgumentException();

    tupleSer = htree.getIndexMetadata().getTupleSerializer();
  }

  public int getSourceIndex() {

    return 0;
  }

  private final ITupleSerializer tupleSer;

  public ITupleSerializer getTupleSerializer() {

    return tupleSer;
  }
}
