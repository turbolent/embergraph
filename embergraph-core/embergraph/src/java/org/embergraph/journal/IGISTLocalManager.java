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
 * Created on July 17, 2014
 */
package org.embergraph.journal;

import org.embergraph.btree.Checkpoint;
import org.embergraph.btree.ICheckpointProtocol;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.rawstore.IRawStore;

/**
 * Interface for managing local (non-distributed) generalized search trees (GiST).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/585" > GIST </a>
 */
public interface IGISTLocalManager extends IGISTManager {

  /**
   * Method creates and registers a named persistence capable data structure.
   *
   * @param name The name of the index.
   * @param metadata The metadata that describes the data structure to be created.
   * @return The persistence capable data structure.
   * @see IGISTManager#registerIndex(IndexMetadata)
   * @see Checkpoint#create(IRawStore, IndexMetadata)
   */
  ICheckpointProtocol register(String name, IndexMetadata metadata); // GIST

  /**
   * Return the mutable view of the named persistence capable data structure (aka the "live" or
   * {@link ITx#UNISOLATED} view).
   *
   * <p>Note: {@link IIndexManager#getIndex(String)} delegates to this method and then casts the
   * result to an {@link IIndex}. This is the core implementation to access an existing named index.
   *
   * @return The mutable view of the persistence capable data structure.
   * @see IIndexManager#getIndex(String)
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/585" > GIST </a>
   */
  ICheckpointProtocol getUnisolatedIndex(String name); // GIST

  /**
   * Core implementation for access to historical index views.
   *
   * <p>Note: Transactions should pass in the timestamp against which they are reading rather than
   * the transaction identifier (aka startTime). By providing the timestamp of the commit point, the
   * transaction will hit the index cache on the journal. If the transaction passes the startTime
   * instead, then all startTimes will be different and the cache will be defeated.
   *
   * @throws UnsupportedOperationException If you pass in {@link ITx#UNISOLATED}, {@link
   *     ITx#READ_COMMITTED}, or a timestamp that corresponds to a read-write transaction since
   *     those are not "commit times".
   * @see IIndexStore#getIndex(String, long)
   * @see <a href="http://sourceforge.net/apps/trac/bigdata/ticket/546" > Add cache for access to
   *     historical index views on the Journal by name and commitTime. </a>
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/585" > GIST </a>
   *     <p>FIXME GIST : Reconcile with {@link IResourceManager#getIndex(String, long)}. They are
   *     returning types that do not overlap ({@link ICheckpointProtocol} and {@link
   *     ILocalBTreeView}). This is blocking the support of GIST in {@link AbstractTask}.
   */
  ICheckpointProtocol getIndexLocal(String name, long commitTime); // GIST
}
