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
package org.embergraph.journal;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.embergraph.ha.HAGlue;
import org.embergraph.ha.QuorumService;
import org.embergraph.quorum.AsynchronousQuorumCloseException;
import org.embergraph.quorum.Quorum;
import org.embergraph.rawstore.IMRMW;

/*
 * An persistence capable data structure supporting atomic commit, scalable named indices, and
 * transactions.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IJournal extends IMRMW, IAtomicStore, IBTreeManager {

  /** A copy of the properties used to initialize this journal. */
  Properties getProperties();

  /*
   * Shutdown the journal politely. Scheduled operations will run to completion, but no new
   * operations will be scheduled.
   */
  void shutdown();

  /** Immediate shutdown. */
  void shutdownNow();

  /** Return the object providing the local transaction manager for this journal. */
  ILocalTransactionManager getLocalTransactionManager();

  /*
   * The {@link Quorum} for this service -or- <code>null</code> if the service is not running with a
   * quorum.
   */
  Quorum<HAGlue, QuorumService<HAGlue>> getQuorum();

  /*
   * Await the service being ready to partitipate in an HA quorum. The preconditions include:
   *
   * <ol>
   *   <li>receiving notice of the quorum token via {@link #setQuorumToken(long)}
   *   <li>The service is joined with the met quorum for that token
   *   <li>If the service is a follower and it's local root blocks were at <code>commitCounter:=0
   *       </code>, then the root blocks from the leader have been installed on the follower.
   *       <ol>
   *
   * @param timeout The timeout to await this condition.
   * @param units The units for that timeout.
   * @return the quorum token for which the service became HA ready.
   */
  long awaitHAReady(final long timeout, final TimeUnit units)
      throws InterruptedException, TimeoutException, AsynchronousQuorumCloseException;

  /*
   * Convenience method created in BLZG-1370 to factor out embergraph-jini artifact dependencies.
   *
   * <p>This should return true IFF the underlying journal is org.embergraph.jini.ha.HAJournal.
   *
   * @return
   */
  boolean isHAJournal();
}
