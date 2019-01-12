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
package org.embergraph.rdf.task;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import org.embergraph.counters.CAT;
import org.embergraph.journal.IConcurrencyManager;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IReadOnly;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.changesets.IChangeRecord;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.webapp.DatasetNotFoundException;
import org.embergraph.resources.IndexManager;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.sparse.GlobalRowStoreHelper;
import org.openrdf.repository.RepositoryException;
import org.openrdf.sail.SailException;

/*
* Base class for task-oriented concurrency. Directly derived classes are suitable for internal
 * tasks (stored queries, stored procedures, etc) while REST API tasks are based on a specialized
 * subclass that also provides for access to the HTTP request and response.
 *
 * <p><strong>CAUTION: Instances of this class that perform mutations MUST throw an exception if
 * they do not want to join a commit group. Failure to follow this guideline can break the ACID
 * contract.</strong>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="http://trac.blazegraph.com/ticket/566" > Concurrent unisolated operations against
 *     multiple KBs </a>
 */
public abstract class AbstractApiTask<T> implements IApiTask<T>, IReadOnly {

  /** The reference to the {@link IIndexManager} is set before the task is executed. */
  private final AtomicReference<IIndexManager> indexManagerRef =
      new AtomicReference<IIndexManager>();

  /** The namespace of the target KB instance. */
  protected final String namespace;

  /** The timestamp of the view of that KB instance. */
  protected final long timestamp;

  /** The GRS is required for create/destroy of a relation (triple/quad store, etc). */
  private final boolean isGRSRequired;

  private final CAT mutationCount = new CAT();

  @Override
  public abstract boolean isReadOnly();

  @Override
  public final String getNamespace() {
    return namespace;
  }

  @Override
  public final long getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean isGRSRequired() {
    return isGRSRequired;
  }

  @Override
  public String toString() {

    return getClass().getName()
        + "{namespace="
        + getNamespace()
        + ",timestamp="
        + getTimestamp()
        + ", isGRSRequired="
        + isGRSRequired
        + "}";
  }

  /*
   * @param namespace The namespace of the target KB instance.
   * @param timestamp The timestamp of the view of that KB instance.
   */
  protected AbstractApiTask(final String namespace, final long timestamp) {

    this(namespace, timestamp, false /* requiresGRS */);
  }

  /*
   * @param namespace The namespace of the target KB instance.
   * @param timestamp The timestamp of the view of that KB instance.
   * @param isGRSRequired True iff a lock must be obtain on the Global Row Store (GRS). For example,
   *     the GRS is required for create/destroy of a relation (triple/quad store, etc).
   */
  protected AbstractApiTask(
      final String namespace, final long timestamp, final boolean isGRSRequired) {
    this.namespace = namespace;
    this.timestamp = timestamp;
    this.isGRSRequired = isGRSRequired;
  }

  @Override
  public void setIndexManager(final IIndexManager indexManager) {

    indexManagerRef.set(indexManager);
  }

  @Override
  public IIndexManager getIndexManager() {

    final IIndexManager tmp = indexManagerRef.get();

    if (tmp == null) throw new IllegalStateException();

    return tmp;
  }

  //    /*
//    * Return a view of the {@link AbstractTripleStore} for the namespace and
  //    * timestamp associated with this task.
  //    *
  //    * @return The {@link AbstractTripleStore} -or- <code>null</code> if none is
  //    *         found for that namespace and timestamp.
  //    */
  //   protected AbstractTripleStore getTripleStore() {
  //
  //      return getTripleStore(namespace, timestamp);
  //
  //   }
  //
  //    /*
//     * Return a view of the {@link AbstractTripleStore} for the given namespace
  //     * that will read on the commit point associated with the given timestamp.
  //     *
  //     * @param namespace
  //     *            The namespace.
  //     * @param timestamp
  //     *            The timestamp or {@link ITx#UNISOLATED} to obtain a read/write
  //     *            view of the index.
  //     *
  //     * @return The {@link AbstractTripleStore} -or- <code>null</code> if none is
  //     *         found for that namespace and timestamp.
  //     */
  //    protected AbstractTripleStore getTripleStore(final String namespace,
  //            final long timestamp) {
  //
  //        // resolve the default namespace.
  //        final AbstractTripleStore tripleStore = (AbstractTripleStore) getIndexManager()
  //                .getResourceLocator().locate(namespace, timestamp);
  //
  //        return tripleStore;
  //
  //    }

  /*
   * Return a connection transaction, which may be either read-only or support mutation depending on
   * the timestamp associated with the task's view. When the timestamp is associated with a
   * historical commit point, this will be a read-only connection. When it is associated with the
   * {@link ITx#UNISOLATED} view or a read-write transaction, this will be a mutable connection.
   *
   * <p>This version uses the namespace and timestamp associated with the HTTP request.
   *
   * @throws RepositoryException
   * @throws DatasetNotFoundException
   */
  protected EmbergraphSailRepositoryConnection getQueryConnection() throws RepositoryException {

    /*
     * Note: [timestamp] will be a read-only tx view of the triple store if
     * a READ_LOCK was specified when the NanoSparqlServer was started
     * (unless the query explicitly overrides the timestamp of the view on
     * which it will operate).
     */
    return getQueryConnection(namespace, timestamp);
  }

  /*
   * This version uses the namespace and timestamp provided by the caller.
   *
   * @param namespace
   * @param timestamp
   * @return
   * @throws RepositoryException
   * @throws DatasetNotFoundException
   */
  protected EmbergraphSailRepositoryConnection getQueryConnection(
      final String namespace, final long timestamp) throws RepositoryException {

    // Wrap with SAIL.
    final EmbergraphSail sail = new EmbergraphSail(namespace, getIndexManager());

    final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

    repo.initialize();

    if (TimestampUtility.isReadOnly(timestamp)) {

      return repo.getReadOnlyConnection(timestamp);
    }

    // Read-write connection.
    final EmbergraphSailRepositoryConnection conn = repo.getConnection();

    conn.setAutoCommit(false);

    return conn;
  }

  protected EmbergraphSailConnection getUnisolatedSailConnection()
      throws SailException, InterruptedException {

    // Wrap with SAIL.
    final EmbergraphSail sail = new EmbergraphSail(namespace, getIndexManager());

    sail.initialize();

    final EmbergraphSailConnection conn = sail.getUnisolatedConnection();

    // Setup a change listener. It will notice the #of mutations.
    conn.addChangeLog(new SailChangeLog());

    return conn;
  }

  /*
   * Return a connection for the namespace. If the task is associated with either a read/write
   * transaction or an {@link ITx#UNISOLATED} view of the indices, the connection may be used to
   * read or write on the namespace. Otherwise the connection will be read-only.
   *
   * @return The connection.
   * @throws SailException
   * @throws RepositoryException
   * @throws DatasetNotFoundException if the specified namespace does not exist.
   */
  protected EmbergraphSailRepositoryConnection getConnection()
      throws SailException, RepositoryException {

    // Wrap with SAIL.
    final EmbergraphSail sail = new EmbergraphSail(namespace, getIndexManager());

    final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

    repo.initialize();

    final EmbergraphSailRepositoryConnection conn = repo.getConnection();

    conn.setAutoCommit(false);

    // Setup a change listener. It will notice the #of mutations.
    conn.addChangeLog(new SailChangeLog());

    return conn;
  }

  private class SailChangeLog implements IChangeLog {

    @Override
    public final void changeEvent(final IChangeRecord record) {
      mutationCount.increment();
    }

    @Override
    public void transactionBegin() {}

    @Override
    public void transactionPrepare() {}

    @Override
    public void transactionCommited(long commitTime) {}

    @Override
    public void transactionAborted() {}

    @Override
    public void close() {}
  }

  /*
   * Submit a task and return a {@link Future} for that task. The task will be run on the
   * appropriate executor service depending on the nature of the backing database and the view
   * required by the task.
   *
   * <p><strong> This method returns a {@link Future}. Remember to do {@link Future#get()} on the
   * returned {@link Future} to await the group commit.</strong>
   *
   * @param indexManager The {@link IndexManager}.
   * @param task The task.
   * @return The {@link Future} for that task.
   * @see <a href="http://trac.blazegraph.com/ticket/753" > HA doLocalAbort() should interrupt NSS
   *     requests and AbstractTasks </a>
   * @see <a href="http://trac.blazegraph.com/ticket/566" > Concurrent unisolated operations against
   *     multiple KBs </a>
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> FutureTask<T> submitApiTask(
      final IIndexManager indexManager, final IApiTask<T> task) {

    final String namespace = task.getNamespace();

    final long timestamp = task.getTimestamp();

    if (!indexManager.isGroupCommit()
        || indexManager instanceof IEmbergraphFederation
        || TimestampUtility.isReadOnly(timestamp)) {

      /*
       * Execute the REST API task.
       *
       * Note: For scale-out, the operation will be applied using client-side
       * global views of the indices. This means that there will not be any
       * globally consistent views of the indices and that updates will be
       * shard-wise local (even through scale-out uses group commit, we do
       * not submit tasks on the client via the group commit API).
       *
       * Note: This can be used for operations on read-only views (even on a
       * Journal). This is helpful since we can avoid some overhead
       * associated the AbstractTask lock declarations and the overhead
       * associated with an isolated TemporaryStore per read-only or
       * read-write tx AbstractTask instance.
       */
      // Wrap Callable.
      final FutureTask<T> ft = new FutureTask<T>(new ApiTaskForIndexManager(indexManager, task));

      //         /*
      //          * Caller runs (synchronous execution)
      //          *
      //          * Note: By having the caller run the task here we avoid consuming
      //          * another thread.
      //          */
      //         ft.run();
      /*
       * Submit to an executor.
       *
       * Note: The code was changed to submit to an executor so the caller
       * does not block while inside of submitApiTask(). This makes it
       * possible to support the StatusServlet's ability to list the
       * running tasks.
       *
       * @see <a href="http://trac.bigdata.com/ticket/1254" > All REST API
       * operations should be cancelable from both REST API and workbench
       * </a>
       */
      indexManager.getExecutorService().execute(ft);

      return ft;

    } else {

      /*
       * Run on the ConcurrencyManager of the Journal.
       *
       * <p>Mutation operations will be scheduled based on the pre-declared locks and will have
       * exclusive access to the resources guarded by those locks when they run.
       *
       * <p>FIXME GIST: The hierarchical locking mechanisms will fail on durable named solution sets
       * because they use either HTree or Stream and AbstractTask does not yet support those durable
       * data structures (it is still being refactored to support the ICheckpointProtocol rather
       * than the BTree in its Name2Addr isolation logic).
       */

      // Obtain the names of the necessary locks for R/W access to indices.
      final String[] locks = getLocksForKB((Journal) indexManager, namespace, task.isGRSRequired());

      final IConcurrencyManager cc = ((Journal) indexManager).getConcurrencyManager();

      /*
       * Submit task to ConcurrencyManager.
       *
       * Task will (eventually) acquire locks and run.
       *
       * Note: The Future of that task is returned to the caller.
       *
       * Note: ConcurrencyManager.submit() requires an AbstractTask. This
       * makes it quite difficult for us to return a FutureTask here. Making
       * the change there touches the lock manager and write executor service
       * but maybe it should be done since it is otherwise difficult to
       * convert a Future into a FutureTask or RunnableFuture.
       *
       * TODO Could pass through timeout for submitted task here.
       */
      final FutureTask<T> ft =
          cc.submit(new ApiTaskForJournal(cc, task.getTimestamp(), locks, task));

      return ft;
    }
  }

  /*
   * Return the set of locks that the task must acquire in order to operate on the specified
   * namespace.
   *
   * @param indexManager The {@link Journal}.
   * @param namespace The namespace of the KB instance.
   * @param requiresGRS GRS is required for create/destroy of a relation (triple/quad store, etc).
   * @return The locks for the named indices associated with that KB instance.
   */
  private static String[] getLocksForKB(
      final Journal indexManager, final String namespace, final boolean requiresGRS) {

    /*
     * This uses hierarchical locking, so it just returns the namespace. This
     * is implicitly used to contend with any other unisolated operations on
     * the same namespace. Thus we do not need to enumerate the indices under
     * that namespace.
     */
    if (requiresGRS) {
      /*
       * The GRS is required for create/destroy of a relation (triple/quad store, etc).
       */
      return new String[] {GlobalRowStoreHelper.GLOBAL_ROW_STORE_INDEX, namespace};
    } else {
      return new String[] {namespace};
    }
  }

  public long getMutationCount() {
    return this.mutationCount.get();
  }
}
