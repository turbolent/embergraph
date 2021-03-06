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
 * Created on Apr 13, 2011
 */
package org.embergraph.rdf.sail;

import org.apache.log4j.Logger;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sail.webapp.DatasetNotFoundException;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.task.AbstractApiTask;

/*
 * Task destroys a KB for the given namespace. The correct use of this class is as follows:
 *
 * <pre>
 * AbstractApiTask.submitApiTask(indexManager, new DestroyKBTask(namespace)).get();
 * </pre>
 *
 * @see CreateKBTask
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class DestroyKBTask extends AbstractApiTask<Void> {

  //   private static final transient Logger log = Logger.getLogger(DestroyKBTask.class);

  private static final transient Logger txLog = Logger.getLogger("org.embergraph.txLog");

  public DestroyKBTask(final String namespace) {

    super(namespace, ITx.UNISOLATED, true /* isGRSRequired */);
  }

  @Override
  public final boolean isReadOnly() {
    return false;
  }

  /** @throws DatasetNotFoundException if the namespace does not exist. */
  @Override
  public Void call() throws Exception {

    boolean ok = false;

    final EmbergraphSailConnection con = getUnisolatedSailConnection();

    try {

      final AbstractTripleStore tripleStore = con.getTripleStore();

      //            if (tripleStore == null) {
      //
      //              throw new DatasetNotFoundException("Not found: namespace="
      //                    + namespace + ", timestamp="
      //                    + TimestampUtility.toString(timestamp));
      //
      //           }

      // Destroy the KB instance.
      tripleStore.destroy();

      // Note: This is the commit point only if group commit is NOT enabled.
      tripleStore.commit();

      ok = true;

      if (txLog.isInfoEnabled()) txLog.info("SAIL-DESTROY-NAMESPACE: namespace=" + namespace);

      return null;

    } finally {

      if (!ok) {

        con.rollback();
      }

      con.close();
    }

    //      /*
    //       * Note: Unless group commit is enabled, we need to make this operation
    //       * mutually exclusive with KB level writers in order to avoid the
    //       * possibility of a triggering a commit during the middle of a
    //       * EmbergraphSailConnection level operation (or visa versa).
    //       *
    //       * Note: When group commit is not enabled, the indexManager will be a
    //       * Journal class. When it is enabled, it will merely implement the
    //       * IJournal interface.
    //       *
    //       * @see #1143 (Isolation broken in NSS when groupCommit disabled)
    //       */
    //      final IIndexManager indexManager = getIndexManager();
    //      final boolean isGroupCommit = indexManager.isGroupCommit();
    //      boolean acquiredConnection = false;
    //      try {
    //
    //         if (!isGroupCommit) {
    //            try {
    //               // acquire the unisolated connection permit.
    //               ((Journal) indexManager).acquireUnisolatedConnection();
    //               acquiredConnection = true;
    //            } catch (InterruptedException e) {
    //               throw new RuntimeException(e);
    //            }
    //         }
    //
    //         final boolean exists = getIndexManager().getResourceLocator().locate(namespace,
    // ITx.UNISOLATED) != null;
    //
    //         if (!exists) {
    //
    //            throw new DatasetNotFoundException("Not found: namespace="
    //                  + namespace + ", timestamp="
    //                  + TimestampUtility.toString(timestamp));
    //
    //         }
    //
    //         // Destroy the KB instance.
    //         tripleStore.destroy();
    //
    //         // Note: This is the commit point only if group commit is NOT enabled.
    //         tripleStore.commit();
    //
    //         if (log.isInfoEnabled())
    //            log.info("Destroyed: namespace=" + namespace);
    //
    //         return null;
    //
    //      } finally {
    //
    //         if (!isGroupCommit && acquiredConnection) {
    //
    //            /*
    //             * When group commit is not enabled, we need to release the
    //             * unisolated connection.
    //             *
    //             * @see #1143 (Isolation broken in NSS when groupCommit disabled)
    //             */
    //            ((Journal) indexManager).releaseUnisolatedConnection();
    //
    //         }
    //
    //      }

  }
}
