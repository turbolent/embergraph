package org.embergraph.btree.isolation;

import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;

/*
 * Does not resolve any conflicts.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public final class NoConflictResolver implements IConflictResolver {

  /** */
  private static final long serialVersionUID = 4873027180161852127L;

  public boolean resolveConflict(IIndex writeSet, ITuple txTuple, ITuple currentTuple) {

    return false;
  }
}
