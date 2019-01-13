/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on May 21, 2007
 */

package org.embergraph.rdf.store;

import java.util.Properties;
import org.embergraph.btree.BTree;
import org.embergraph.btree.BTreeCounters;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.ITx;

/*
 * Abstract base class for both transient and persistent {@link ITripleStore} implementations using
 * local storage.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractLocalTripleStore extends AbstractTripleStore {

  /*
   * @param indexManager
   * @param namespace
   * @param timestamp
   * @param properties
   */
  protected AbstractLocalTripleStore(
      IIndexManager indexManager, String namespace, Long timestamp, Properties properties) {

    super(indexManager, namespace, timestamp, properties);
  }

  /*
   * Reports the bytes written on each of the {@link SPORelation} indices and on each of the {@link
   * LexiconRelation} indices. These performance data are not restart safe. However, they are help
   * by a hard reference from the {@link BTree}, and the {@link BTree} instances for these indices
   * are held by hard references from the {@link SPORelation} and the {@link LexiconRelation} so the
   * data will remain valid across the life cycle of a {@link LocalTripleStore} instance, e.g.,
   * between restarts.
   *
   * @param sb The caller's buffer.
   * @return The caller's buffer.
   */
  public StringBuilder getLocalBTreeBytesWritten(final StringBuilder sb) {

    boolean first = true;

    for (String fqn : getLexiconRelation().getIndexNames()) {

      /*
       * Note: This tunnels to the unisolated index. This is the one with
       * the performance counters. Since we are only going to access the
       * performance counters, this is safe (no concurrent modification).
       */
      final BTreeCounters btreeCounters =
          ((BTree) getIndexManager().getIndex(fqn, ITx.UNISOLATED)).getBtreeCounters();

      //            final int leavesSplit = btreeCounters.leavesSplit;
      final long nodesWritten = btreeCounters.getNodesWritten();
      final long leavesWritten = btreeCounters.getLeavesWritten();
      final long bytesWritten = btreeCounters.getBytesWritten();
      final long totalWritten = (nodesWritten + leavesWritten);
      final long bytesPerRecord =
          totalWritten == 0 ? 0 : bytesWritten / (nodesWritten + leavesWritten);

      sb.append(first ? "" : ", ").append(fqn).append("{nodes=").append(nodesWritten)
          .append(",leaves=").append(leavesWritten).append(", bytes=").append(bytesWritten)
          .append(", averageBytesPerRecord=").append(bytesPerRecord).append("}");

      first = false;
    }

    for (String fqn : getSPORelation().getIndexNames()) {

      /*
       * Note: This tunnels to the unisolated index. This is the one with
       * the performance counters. Since we are only going to access the
       * performance counters, this is safe (no concurrent modification).
       */
      final BTreeCounters btreeCounters =
          ((BTree) getIndexManager().getIndex(fqn, ITx.UNISOLATED)).getBtreeCounters();

      //            final int leavesSplit = btreeCounters.leavesSplit;
      final long nodesWritten = btreeCounters.getNodesWritten();
      final long leavesWritten = btreeCounters.getLeavesWritten();
      final long bytesWritten = btreeCounters.getBytesWritten();
      final long totalWritten = (nodesWritten + leavesWritten);
      final long bytesPerRecord =
          totalWritten == 0 ? 0 : bytesWritten / (nodesWritten + leavesWritten);

      sb.append(first ? "" : ", ").append(fqn).append("{nodes=").append(nodesWritten)
          .append(",leaves=").append(leavesWritten).append(", bytes=").append(bytesWritten)
          .append(", averageBytesPerRecord=").append(bytesPerRecord).append("}");

      first = false;
    }

    return sb;
  }
}
