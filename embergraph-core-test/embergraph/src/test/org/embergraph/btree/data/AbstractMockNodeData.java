package org.embergraph.btree.data;

import java.io.OutputStream;
import org.embergraph.btree.raba.IRaba;

/*
* Abstract base class for mock node and leaf data implementations for unit tests.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract class AbstractMockNodeData implements IAbstractNodeData {

  // mutable.
  private final IRaba keys;

  public final int getKeyCount() {

    return keys.size();
  }

  public final IRaba getKeys() {

    return keys;
  }

  public final byte[] getKey(final int index) {

    return keys.get(index);
  }

  public final void copyKey(final int index, final OutputStream os) {

    keys.copy(index, os);
  }

  protected AbstractMockNodeData(final IRaba keys) {

    if (keys == null) throw new IllegalArgumentException();

    //        Note: The HTree IRaba for keys does NOT report true for isKeys() since
    //        it does not obey any of the contract for the B+Tree keys (unordered,
    //        sparse, allows duplicates and nulls, not searchable).
    //
    //        if (!keys.isKeys())
    //            throw new IllegalArgumentException();

    this.keys = keys;
  }
}
