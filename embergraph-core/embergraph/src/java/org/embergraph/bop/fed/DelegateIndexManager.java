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
package org.embergraph.bop.fed;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.embergraph.bfs.EmbergraphFileSystem;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.counters.CounterSet;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IIndexStore;
import org.embergraph.journal.IResourceLockService;
import org.embergraph.journal.TemporaryStore;
import org.embergraph.relation.locator.IResourceLocator;
import org.embergraph.relation.rule.eval.pipeline.DistributedJoinTask;
import org.embergraph.relation.rule.eval.pipeline.JoinTaskFactoryTask;
import org.embergraph.resources.IndexManager;
import org.embergraph.resources.StoreManager.ManagedJournal;
import org.embergraph.service.DataService;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.sparse.SparseRowStore;

/**
 * The index view that we need for the {@link DistributedJoinTask} is on the {@link IndexManager}
 * class, not the live {@link ManagedJournal}. Looking on the live journal we will only see the
 * mutable {@link BTree} and not the entire index partition view. However, {@link IndexManager} does
 * not implement {@link IIndexManager} or even {@link IIndexStore}. Therefore this class was
 * introduced. It passes most of the methods on to the {@link IEmbergraphFederation} but {@link
 * #getIndex(String, long)} is delegated to {@link IndexManager#getIndex(String, long)} which is the
 * method that knows how to create the index partition view.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @todo While this class solves our problem I do not know whether or not this class should this
 *     class have more visibility? The downside is that it is a bit incoherent how it passes along
 *     one method to the {@link IndexManager}, most methods to the {@link IEmbergraphFederation} and
 *     disallows {@link #dropIndex(String)} and {@link #registerIndex(IndexMetadata)} in an attempt
 *     to stay out of trouble. That may be enough reason to keep it private.
 *     <p>TODO Is this an exact functional duplicate of the class by the same name in the {@link
 *     JoinTaskFactoryTask}?
 */
class DelegateIndexManager implements IIndexManager {

  private final DataService dataService;

  public DelegateIndexManager(final DataService dataService) {

    if (dataService == null) throw new IllegalArgumentException();

    this.dataService = dataService;
  }

  /**
   * Delegates to the {@link IndexManager}.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public IIndex getIndex(final String name, final long timestamp) {

    return dataService.getResourceManager().getIndex(name, timestamp);
  }

  /**
   * Not allowed.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void dropIndex(final String name) {

    throw new UnsupportedOperationException();
  }

  /** Not allowed. */
  @Override
  public void registerIndex(IndexMetadata indexMetadata) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void destroy() {

    throw new UnsupportedOperationException();
  }

  @Override
  public ExecutorService getExecutorService() {

    return dataService.getFederation().getExecutorService();
  }

  @Override
  public EmbergraphFileSystem getGlobalFileSystem() {

    return dataService.getFederation().getGlobalFileSystem();
  }

  @Override
  public SparseRowStore getGlobalRowStore() {

    return dataService.getFederation().getGlobalRowStore();
  }

  @Override
  public SparseRowStore getGlobalRowStore(final long timestamp) {

    return dataService.getFederation().getGlobalRowStore(timestamp);
  }

  @Override
  public long getLastCommitTime() {

    return dataService.getFederation().getLastCommitTime();
  }

  @Override
  public IResourceLocator getResourceLocator() {

    return dataService.getFederation().getResourceLocator();
  }

  @Override
  public IResourceLockService getResourceLockService() {

    return dataService.getFederation().getResourceLockService();
  }

  @Override
  public TemporaryStore getTempStore() {

    return dataService.getFederation().getTempStore();
  }

  @Override
  public ScheduledFuture<?> addScheduledTask(
      Runnable task, long initialDelay, long delay, TimeUnit unit) {
    return dataService.getFederation().addScheduledTask(task, initialDelay, delay, unit);
  }

  @Override
  public boolean getCollectPlatformStatistics() {
    return dataService.getFederation().getCollectPlatformStatistics();
  }

  @Override
  public boolean getCollectQueueStatistics() {
    return dataService.getFederation().getCollectQueueStatistics();
  }

  @Override
  public int getHttpdPort() {
    return dataService.getFederation().getHttpdPort();
  }

  //	public ConcurrencyManager getConcurrencyManager() {
  //
  //	    return dataService.getConcurrencyManager();
  //
  //	}

  @Override
  public CounterSet getCounters() {
    return dataService.getFederation().getCounters();
  }

  @Override
  public String toString() {

    return super.toString() + "{dataServiceUUID=" + dataService.getServiceUUID() + "}";
  }

  /**
   * {@inheritDoc}
   *
   * <p>TODO Implement. Probably delegate to the local DS n2a index so this does a DS local n2a
   * scan.
   */
  @Override
  public Iterator<String> indexNameScan(String prefix, long timestamp) {
    throw new UnsupportedOperationException();
  }

  /** The data service does support group commit. */
  @Override
  public boolean isGroupCommit() {
    return true;
  }
}
