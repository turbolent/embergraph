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
package org.embergraph.blueprints;

import com.tinkerpop.blueprints.Features;
import com.tinkerpop.blueprints.TransactionalGraph;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.embergraph.blueprints.EmbergraphGraphEdit.Action;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.journal.IIndexManager;
import org.embergraph.rdf.changesets.ChangeAction;
import org.embergraph.rdf.changesets.ChangeRecord;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.changesets.IChangeRecord;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.QueryCancellationHelper;
import org.embergraph.rdf.sail.model.RunningQuery;
import org.embergraph.rdf.sail.webapp.EmbergraphRDFContext.AbstractQueryTask;
import org.embergraph.rdf.sail.webapp.StatusServlet;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.EmbergraphStatementIterator;
import org.embergraph.striterator.ChunkedArrayIterator;
import org.openrdf.model.BNode;

/**
 * This is the most basic possible implementation of the Blueprints Graph API. It wraps an embedded
 * {@link EmbergraphSailRepository} and holds open an unisolated connection to the database for the
 * lifespan of the Graph (until {@link #shutdown()} is called.
 *
 * @author mikepersonick
 */
public class EmbergraphGraphEmbedded extends EmbergraphGraph
    implements TransactionalGraph, IChangeLog {

  private static final transient Logger log = Logger.getLogger(EmbergraphGraphEmbedded.class);

  public static interface Options {

    String AUTO_COMMIT_ON_SHUTDOWN =
        EmbergraphGraphEmbedded.class.getName() + ".autoCommitOnShutdown";

    boolean DEFAULT_AUTO_COMMIT_ON_SHUTDOWN = false;
  }

  protected final EmbergraphSailRepository repo;

  protected final boolean autocommitOnShutdown;

  //	transient EmbergraphSailRepositoryConnection cxn;

  final List<EmbergraphGraphListener> listeners =
      Collections.synchronizedList(new LinkedList<EmbergraphGraphListener>());

  /** Create a Blueprints wrapper around a {@link EmbergraphSail} instance. */
  public EmbergraphGraphEmbedded(final EmbergraphSail sail) {
    this(sail, EmbergraphRDFFactory.INSTANCE);
  }

  /**
   * Create a Blueprints wrapper around a {@link EmbergraphSail} instance with a non-standard {@link
   * BlueprintsValueFactory} implementation.
   */
  public EmbergraphGraphEmbedded(final EmbergraphSail sail, final BlueprintsValueFactory factory) {
    this(new EmbergraphSailRepository(sail), factory, new Properties());
  }

  /**
   * Create a Blueprints wrapper around a {@link EmbergraphSail} instance with a non-standard {@link
   * BlueprintsValueFactory} implementation.
   */
  public EmbergraphGraphEmbedded(
      final EmbergraphSail sail, final BlueprintsValueFactory factory, final Properties props) {
    this(new EmbergraphSailRepository(sail), factory, props);
  }

  /** Create a Blueprints wrapper around a {@link EmbergraphSailRepository} instance. */
  public EmbergraphGraphEmbedded(final EmbergraphSailRepository repo) {
    this(repo, EmbergraphRDFFactory.INSTANCE, new Properties());
  }

  /**
   * Create a Blueprints wrapper around a {@link EmbergraphSailRepository} instance with a
   * non-standard {@link BlueprintsValueFactory} implementation.
   */
  public EmbergraphGraphEmbedded(
      final EmbergraphSailRepository repo,
      final BlueprintsValueFactory factory,
      final Properties props) {
    super(factory, props);

    this.repo = (EmbergraphSailRepository) repo;
    this.autocommitOnShutdown =
        Boolean.valueOf(
            props.getProperty(
                Options.AUTO_COMMIT_ON_SHUTDOWN,
                Boolean.toString(Options.DEFAULT_AUTO_COMMIT_ON_SHUTDOWN)));
  }

  public EmbergraphSailRepository getRepository() {
    return repo;
  }

  //    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ReentrantLock lock = new ReentrantLock();

  private transient EmbergraphSailRepositoryConnection cxn = null;

  private transient String heldBy = null;

  public EmbergraphSailRepositoryConnection cxn() throws Exception {
    /*
     * Asking for the connection locks the graph to the current thread
     * until released by commit() or rollback().
     */
    if (!lock.isHeldByCurrentThread()) {
      lock.lock();
    }
    if (cxn == null) {
      cxn = repo.getUnisolatedConnection();
      cxn.addChangeLog(EmbergraphGraphEmbedded.this);
    }
    return cxn;
  }

  public EmbergraphReadOnlyGraph getReadOnlyView() throws Exception {
    if (log.isDebugEnabled()) log.debug("get read-only view: " + Thread.currentThread().getName());

    return new EmbergraphReadOnlyGraph(repo.getReadOnlyConnection(), factory, maxQueryTime);
  }

  @Override
  public void commit() {
    /*
     * Asking for the connection locks the graph to the current thread
     * until released by commit() or rollback().
     */
    if (!lock.isHeldByCurrentThread()) {
      lock.lock();
    }
    try {
      try {
        if (cxn != null) {
          try {
            cxn.commit();
          } finally {
            cxn.close();
          }
        }
      } finally {
        cxn = null;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      /*
       * Release the lock for the next writer thread.
       */
      lock.unlock();
    }
  }

  @Override
  public void rollback() {
    /*
     * Asking for the connection locks the graph to the current thread
     * until released by commit() or rollback().
     */
    if (!lock.isHeldByCurrentThread()) {
      lock.lock();
    }
    try {
      try {
        if (cxn != null) {
          try {
            cxn.rollback();
          } finally {
            cxn.close();
          }
        }
      } finally {
        cxn = null;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      /*
       * Release the lock for the next writer thread.
       */
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    try {
      try {
        if (autocommitOnShutdown) {
          /*
           * Auto-commit on close.
           */
          commit();
        }
      } finally {
        repo.shutDown();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //    private synchronized void lock(final String method) {
  //        if (log.isDebugEnabled())
  //            log.debug("waiting for lock from: " + method + ", thread: " +
  // Thread.currentThread().getName() + ", held by: " + heldBy);
  //
  //        if (!lock.isHeldByCurrentThread()) {
  //            lock.lock();
  //        }
  //        heldBy = Thread.currentThread().getName();
  //
  //        if (log.isDebugEnabled())
  //            log.debug("lock is held: " + Thread.currentThread().getName() + ", holdCount: " +
  // lock.getHoldCount());
  //    }
  //
  //    private synchronized void unlock(final String method) {
  //        heldBy = null;
  //        lock.unlock();
  //
  //        if (log.isDebugEnabled())
  //            log.debug("lock is released by: " + method + ", thread: " +
  // Thread.currentThread().getName());
  //    }

  @Override
  @Deprecated
  public void stopTransaction(Conclusion arg0) {}

  public StringBuilder dumpStore() throws Exception {
    final EmbergraphSailRepositoryConnection cxn = cxn();
    cxn.flush();
    return cxn.getTripleStore().dumpStore();
  }

  protected static final Features FEATURES = new Features();

  @Override
  public Features getFeatures() {

    return FEATURES;
  }

  static {
    FEATURES.supportsSerializableObjectProperty =
        EmbergraphGraph.FEATURES.supportsSerializableObjectProperty;
    FEATURES.supportsBooleanProperty = EmbergraphGraph.FEATURES.supportsBooleanProperty;
    FEATURES.supportsDoubleProperty = EmbergraphGraph.FEATURES.supportsDoubleProperty;
    FEATURES.supportsFloatProperty = EmbergraphGraph.FEATURES.supportsFloatProperty;
    FEATURES.supportsIntegerProperty = EmbergraphGraph.FEATURES.supportsIntegerProperty;
    FEATURES.supportsPrimitiveArrayProperty =
        EmbergraphGraph.FEATURES.supportsPrimitiveArrayProperty;
    FEATURES.supportsUniformListProperty = EmbergraphGraph.FEATURES.supportsUniformListProperty;
    FEATURES.supportsMixedListProperty = EmbergraphGraph.FEATURES.supportsMixedListProperty;
    FEATURES.supportsLongProperty = EmbergraphGraph.FEATURES.supportsLongProperty;
    FEATURES.supportsMapProperty = EmbergraphGraph.FEATURES.supportsMapProperty;
    FEATURES.supportsStringProperty = EmbergraphGraph.FEATURES.supportsStringProperty;
    FEATURES.supportsDuplicateEdges = EmbergraphGraph.FEATURES.supportsDuplicateEdges;
    FEATURES.supportsSelfLoops = EmbergraphGraph.FEATURES.supportsSelfLoops;
    FEATURES.isPersistent = EmbergraphGraph.FEATURES.isPersistent;
    FEATURES.isWrapper = EmbergraphGraph.FEATURES.isWrapper;
    FEATURES.supportsVertexIteration = EmbergraphGraph.FEATURES.supportsVertexIteration;
    FEATURES.supportsEdgeIteration = EmbergraphGraph.FEATURES.supportsEdgeIteration;
    FEATURES.supportsVertexIndex = EmbergraphGraph.FEATURES.supportsVertexIndex;
    FEATURES.supportsEdgeIndex = EmbergraphGraph.FEATURES.supportsEdgeIndex;
    FEATURES.ignoresSuppliedIds = EmbergraphGraph.FEATURES.ignoresSuppliedIds;
    //        FEATURES.supportsTransactions = EmbergraphGraph.FEATURES.supportsTransactions;
    FEATURES.supportsIndices = EmbergraphGraph.FEATURES.supportsIndices;
    FEATURES.supportsKeyIndices = EmbergraphGraph.FEATURES.supportsKeyIndices;
    FEATURES.supportsVertexKeyIndex = EmbergraphGraph.FEATURES.supportsVertexKeyIndex;
    FEATURES.supportsEdgeKeyIndex = EmbergraphGraph.FEATURES.supportsEdgeKeyIndex;
    FEATURES.supportsEdgeRetrieval = EmbergraphGraph.FEATURES.supportsEdgeRetrieval;
    FEATURES.supportsVertexProperties = EmbergraphGraph.FEATURES.supportsVertexProperties;
    FEATURES.supportsEdgeProperties = EmbergraphGraph.FEATURES.supportsEdgeProperties;
    FEATURES.supportsThreadedTransactions = EmbergraphGraph.FEATURES.supportsThreadedTransactions;

    // override
    FEATURES.supportsTransactions = true; // EmbergraphGraph.FEATURES.supportsTransactions;
  }

  public void addListener(final EmbergraphGraphListener listener) {
    this.listeners.add(listener);
  }

  public void removeListener(final EmbergraphGraphListener listener) {
    this.listeners.remove(listener);
  }

  /** We need to batch and materialize these. */
  private final List<IChangeRecord> removes = new LinkedList<IChangeRecord>();

  /** Changed events coming from embergraph. */
  @Override
  public void changeEvent(final IChangeRecord record) {
    /*
     * Watch out for history change events.
     */
    if (record.getStatement().getSubject() instanceof BNode) {
      return;
    }
    /*
     * Adds come in already materialized. Removes do not. Batch and
     * materialize at commit or abort notification.
     */
    if (record.getChangeAction() == ChangeAction.REMOVED) {
      synchronized (removes) {
        removes.add(record);
      }
    } else {
      notify(record);
    }
  }

  /**
   * Turn a change record into a graph edit and notify the graph listeners.
   *
   * @param record Embergraph change record.
   */
  protected void notify(final IChangeRecord record) {
    final EmbergraphGraphEdit edit = toGraphEdit(record);
    if (edit != null) {
      for (EmbergraphGraphListener listener : listeners) {
        listener.graphEdited(edit, record.toString());
      }
    }
  }

  /**
   * Turn a embergraph change record into a graph edit.
   *
   * @param record Embergraph change record
   * @return graph edit
   */
  protected EmbergraphGraphEdit toGraphEdit(final IChangeRecord record) {

    final Action action;
    if (record.getChangeAction() == ChangeAction.INSERTED) {
      action = Action.ADD;
    } else if (record.getChangeAction() == ChangeAction.REMOVED) {
      action = Action.REMOVE;
    } else {
      /*
       * Truth maintenance.
       */
      return null;
    }

    final EmbergraphGraphAtom atom = super.toGraphAtom(record.getStatement());

    return new EmbergraphGraphEdit(action, atom);
  }

  /**
   * Materialize a batch of change records.
   *
   * @param records Embergraph change records
   * @return Same records with materialized values
   */
  protected List<IChangeRecord> materialize(final List<IChangeRecord> records) {

    try {

      final List<IChangeRecord> materialized = new LinkedList<IChangeRecord>();

      final AbstractTripleStore db = cxn().getTripleStore();

      // collect up the ISPOs out of the unresolved change records
      final ISPO[] spos = new ISPO[records.size()];
      int i = 0;
      for (IChangeRecord rec : records) {
        spos[i++] = rec.getStatement();
      }

      // use the database to resolve them into EmbergraphStatements
      final EmbergraphStatementIterator it =
          db.asStatementIterator(new ChunkedArrayIterator<ISPO>(i, spos, null /* keyOrder */));

      /*
       * the EmbergraphStatementIterator will produce EmbergraphStatement
       * objects in the same order as the original ISPO array
       */
      for (IChangeRecord rec : records) {
        final EmbergraphStatement stmt = it.next();
        materialized.add(new ChangeRecord(stmt, rec.getChangeAction()));
      }

      return materialized;
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** Notification of transaction beginning. */
  @Override
  public void transactionBegin() {
    for (EmbergraphGraphListener listener : listeners) {
      listener.transactionBegin();
    }
  }

  /** Notification of transaction preparing for commit. */
  @Override
  public void transactionPrepare() {
    notifyRemoves();
    for (EmbergraphGraphListener listener : listeners) {
      listener.transactionPrepare();
    }
  }

  /** Notification of transaction committed. */
  @Override
  public void transactionCommited(final long commitTime) {
    //        notifyRemoves();
    for (EmbergraphGraphListener listener : listeners) {
      listener.transactionCommitted(commitTime);
    }
  }

  /** Notification of transaction aborted. */
  @Override
  public void transactionAborted() {
    //        notifyRemoves();
    for (EmbergraphGraphListener listener : listeners) {
      listener.transactionAborted();
    }
  }

  @Override
  public void close() {}

  /** Materialize and notify listeners of the remove events. */
  protected void notifyRemoves() {
    if (listeners.size() > 0) {
      final List<IChangeRecord> removes;
      synchronized (this.removes) {
        removes = materialize(this.removes);
        this.removes.clear();
      }
      for (IChangeRecord remove : removes) {
        notify(remove);
      }
    } else {
      synchronized (this.removes) {
        this.removes.clear();
      }
    }
  }

  protected QueryEngine getQueryEngine() {

    final QueryEngine queryEngine =
        (QueryEngine) QueryEngineFactory.getInstance().getQueryController(getIndexManager());

    return queryEngine;
  }

  private IIndexManager getIndexManager() {

    final EmbergraphSailRepository repo = (EmbergraphSailRepository) this.getRepository();

    final IIndexManager indexMgr = repo.getSail().getIndexManager();

    return indexMgr;
  }

  //    @Override
  //    public synchronized Object getProperty(URI uri, String prop) {
  //        return super.getProperty(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized Object getProperty(URI uri, URI prop) {
  //        return super.getProperty(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized List<Object> getProperties(URI uri, String prop) {
  //        return super.getProperties(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized List<Object> getProperties(URI uri, URI prop) {
  //        return super.getProperties(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized Set<String> getPropertyKeys(URI uri) {
  //        return super.getPropertyKeys(uri);
  //    }
  //
  //    @Override
  //    public synchronized Object removeProperty(URI uri, String prop) {
  //        return super.removeProperty(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized Object removeProperty(URI uri, URI prop) {
  //        return super.removeProperty(uri, prop);
  //    }
  //
  //    @Override
  //    public synchronized void setProperty(URI uri, String prop, Object val) {
  //        super.setProperty(uri, prop, val);
  //    }
  //
  //    @Override
  //    public synchronized void setProperty(URI uri, URI prop, Literal val) {
  //        super.setProperty(uri, prop, val);
  //    }
  //
  //    @Override
  //    public synchronized void addProperty(URI uri, String prop, Object val) {
  //        super.addProperty(uri, prop, val);
  //    }
  //
  //    @Override
  //    public synchronized void addProperty(URI uri, URI prop, Literal val) {
  //        super.addProperty(uri, prop, val);
  //    }
  //
  //    @Override
  //    public synchronized void loadGraphML(String file) throws Exception {
  //        super.loadGraphML(file);
  //    }
  //
  //    @Override
  //    public synchronized Edge addEdge(Object key, Vertex from, Vertex to, String label) {
  //        return super.addEdge(key, from, to, label);
  //    }
  //
  //    @Override
  //    public synchronized Vertex addVertex(Object key) {
  //        return super.addVertex(key);
  //    }
  //
  //    @Override
  //    public synchronized Edge getEdge(Object key) {
  //        return super.getEdge(key);
  //    }
  //
  //    @Override
  //    public synchronized Iterable<Edge> getEdges() {
  //        return super.getEdges();
  //    }
  //
  //    @Override
  //    synchronized Iterable<Edge> getEdges(URI from, URI to, String... labels) {
  //        return super.getEdges(from, to, labels);
  //    }
  //
  //    @Override
  //    protected synchronized GraphQueryResult getElements(URI from, URI to, String... labels) {
  //        return super.getElements(from, to, labels);
  //    }
  //
  //    @Override
  //    synchronized Iterable<Edge> getEdges(String queryStr) {
  //        return super.getEdges(queryStr);
  //    }
  //
  //    @Override
  //    synchronized Iterable<Vertex> getVertices(URI from, URI to, String... labels) {
  //        return super.getVertices(from, to, labels);
  //    }
  //
  //    @Override
  //    synchronized Iterable<Vertex> getVertices(String queryStr, boolean subject) {
  //        return super.getVertices(queryStr, subject);
  //    }
  //
  //    @Override
  //    public synchronized Iterable<Edge> getEdges(String prop, Object val) {
  //        return super.getEdges(prop, val);
  //    }
  //
  //    @Override
  //    public synchronized Vertex getVertex(Object key) {
  //        return super.getVertex(key);
  //    }
  //
  //    @Override
  //    public synchronized Iterable<Vertex> getVertices() {
  //        return super.getVertices();
  //    }
  //
  //    @Override
  //    public synchronized Iterable<Vertex> getVertices(String prop, Object val) {
  //        return super.getVertices(prop, val);
  //    }
  //
  //    @Override
  //    public synchronized GraphQuery query() {
  //        return super.query();
  //    }
  //
  //    @Override
  //    public synchronized void removeEdge(Edge edge) {
  //        super.removeEdge(edge);
  //    }
  //
  //    @Override
  //    public synchronized void removeVertex(Vertex vertex) {
  //        super.removeVertex(vertex);
  //    }
  //
  //    @Override
  //    public Features getFeatures() {
  //        return super.getFeatures();
  //    }

  /**
   * Note: This is also responsible for noticing the time at which the query begins to execute and
   * storing the {@link RunningQuery} in the {@link #m_queries} map.
   *
   * @param The connection.
   */
  protected UUID setupQuery(
      final EmbergraphSailRepositoryConnection cxn,
      ASTContainer astContainer,
      final QueryType queryType,
      final String extId) {

    // Note the begin time for the query.
    final long begin = System.nanoTime();

    // Figure out the UUID under which the query will execute.
    final UUID queryUuid = setQueryId(astContainer, UUID.randomUUID());

    // Set to UUID of internal ID if it is null.
    final String extQueryId = extId == null ? queryUuid.toString() : extId;

    if (log.isDebugEnabled() && extId == null) {
      log.debug("Received null external query ID.  Using " + queryUuid.toString());
    }

    final boolean isUpdateQuery =
        queryType != QueryType.ASK
            && queryType != QueryType.CONSTRUCT
            && queryType != QueryType.DESCRIBE
            && queryType != QueryType.SELECT;

    final RunningQuery r = new RunningQuery(extQueryId, queryUuid, begin, isUpdateQuery);

    // Stuff it in the maps of running queries.
    m_queries.put(extQueryId, r);
    m_queries2.put(queryUuid, r);

    if (log.isDebugEnabled()) {
      log.debug("Setup Query (External ID, UUID):  ( " + extQueryId + " , " + queryUuid + " )");
      log.debug(
          "External query for " + queryUuid + " is :\n" + getQueryById(queryUuid).getExtQueryId());
      log.debug(runningQueriesToString());
    }

    return queryUuid;
  }

  /**
   * Determines the {@link UUID} which will be associated with the {@link IRunningQuery}. If {@link
   * QueryHints#QUERYID} has already been used by the application to specify the {@link UUID} then
   * that {@link UUID} is noted. Otherwise, a random {@link UUID} is generated and assigned to the
   * query by binding it on the query hints.
   *
   * <p>Note: The ability to provide metadata from the {@link ASTContainer} in the {@link
   * StatusServlet} or the "EXPLAIN" page depends on the ability to cross walk the queryIds as
   * established by this method.
   *
   * @param query The query.
   * @param queryUuid
   * @return The {@link UUID} which will be associated with the {@link IRunningQuery} and never
   *     <code>null</code>.
   */
  protected UUID setQueryId(final ASTContainer astContainer, UUID queryUuid) {

    // Figure out the effective UUID under which the query will run.
    final String queryIdStr = astContainer.getQueryHint(QueryHints.QUERYID);
    if (queryIdStr == null) {
      // Not specified, so generate and set on query hint.
      queryUuid = UUID.randomUUID();
    }

    astContainer.setQueryHint(QueryHints.QUERYID, queryUuid.toString());

    return queryUuid;
  }

  /**
   * The currently executing queries (does not include queries where a client has established a
   * connection but the query is not running because the {@link #queryService} is blocking).
   *
   * <p>Note: This includes both SPARQL QUERY and SPARQL UPDATE requests. However, the {@link
   * AbstractQueryTask#queryUuid} might not yet be bound since it is not set until the request
   * begins to execute. See {@link AbstractQueryTask#setQueryId(ASTContainer)}.
   */
  private static final ConcurrentHashMap<String /* extQueryId */, RunningQuery> m_queries =
      new ConcurrentHashMap<String, RunningQuery>();

  /**
   * The currently executing QUERY and UPDATE requests.
   *
   * <p>Note: This does not include requests where a client has established a connection to the
   * SPARQL end point but the request is not executing because the {@link #queryService} is
   * blocking).
   *
   * <p>Note: This collection was introduced because the SPARQL UPDATE requests are not executed on
   * the {@link QueryEngine} and hence we can not use {@link QueryEngine#getRunningQuery(UUID)} to
   * resolve the {@link Future}
   */
  private static final ConcurrentHashMap<UUID /* queryUuid */, RunningQuery> m_queries2 =
      new ConcurrentHashMap<UUID, RunningQuery>();

  public RunningQuery getQueryById(final UUID queryUuid) {

    return m_queries2.get(queryUuid);
  }

  @Override
  public RunningQuery getQueryByExternalId(String extQueryId) {

    return m_queries.get(extQueryId);
  }

  /** Remove the query from the internal queues. */
  @Override
  protected void tearDownQuery(UUID queryId) {

    if (queryId != null) {

      if (log.isDebugEnabled()) {
        log.debug("Tearing down query: " + queryId);
        log.debug("m_queries2 has " + m_queries2.size());
      }

      final RunningQuery r = m_queries2.get(queryId);

      if (r != null) {
        m_queries.remove(r.getExtQueryId(), r);
        m_queries2.remove(queryId);

        if (log.isDebugEnabled()) {
          log.debug("Tearing down query: " + queryId);
          log.debug("m_queries2 has " + m_queries2.size());
        }
      }
    }
  }

  /**
   * Helper method to determine if a query was cancelled.
   *
   * @param queryId
   * @return
   */
  protected boolean isQueryCancelled(final UUID queryId) {

    if (log.isDebugEnabled()) {
      log.debug(queryId);
    }

    RunningQuery q = getQueryById(queryId);

    if (log.isDebugEnabled() && q != null) {
      log.debug(queryId + " isCancelled: " + q.isCancelled());
    }

    if (q != null) {
      return q.isCancelled();
    }

    return false;
  }

  public String runningQueriesToString() {
    final Collection<RunningQuery> queries = m_queries2.values();

    final Iterator<RunningQuery> iter = queries.iterator();

    final StringBuffer sb = new StringBuffer();

    while (iter.hasNext()) {
      final RunningQuery r = iter.next();
      sb.append(r.getQueryUuid() + " : \n" + r.getExtQueryId());
    }

    return sb.toString();
  }

  public Collection<RunningQuery> getRunningQueries() {
    final Collection<RunningQuery> queries = m_queries2.values();

    return queries;
  }

  @Override
  public void cancel(final UUID queryId) {

    assert (queryId != null);
    QueryCancellationHelper.cancelQuery(queryId, this.getQueryEngine());

    RunningQuery q = getQueryById(queryId);

    if (q != null) {
      // Set the status to cancelled in the internal queue.
      q.setCancelled(true);
    }
  }

  @Override
  public void cancel(final String uuid) {
    cancel(UUID.fromString(uuid));
  }

  @Override
  public void cancel(final RunningQuery rQuery) {

    if (rQuery != null) {
      final UUID queryId = rQuery.getQueryUuid();
      cancel(queryId);
    }
  }

  private static Properties properties(final int maxQueryTime) {
    final Properties props = new Properties();
    props.setProperty(EmbergraphGraph.Options.MAX_QUERY_TIME, Integer.toString(maxQueryTime));
    return props;
  }

  public class EmbergraphReadOnlyGraph extends EmbergraphGraph {

    private final EmbergraphSailRepositoryConnection cxn;

    private EmbergraphReadOnlyGraph(
        final EmbergraphSailRepositoryConnection cxn,
        final BlueprintsValueFactory factory,
        final int maxQueryTime) {
      super(factory, properties(maxQueryTime));

      this.cxn = cxn;
    }

    @Override
    public void shutdown() {
      try {
        cxn.close();
      } catch (Exception ex) {
        log.warn("Error closing connection: " + ex);
      }
    }

    @Override
    public EmbergraphSailRepositoryConnection cxn() throws Exception {
      return cxn;
    }

    @Override
    public Collection<RunningQuery> getRunningQueries() {
      return EmbergraphGraphEmbedded.this.getRunningQueries();
    }

    @Override
    public void cancel(final UUID queryId) {
      EmbergraphGraphEmbedded.this.cancel(queryId);
    }

    @Override
    public void cancel(final String uuid) {
      EmbergraphGraphEmbedded.this.cancel(uuid);
    }

    @Override
    public void cancel(final RunningQuery r) {
      EmbergraphGraphEmbedded.this.cancel(r);
    }

    @Override
    public RunningQuery getQueryById(final UUID queryId2) {
      return EmbergraphGraphEmbedded.this.getQueryById(queryId2);
    }

    @Override
    public RunningQuery getQueryByExternalId(final String extQueryId) {
      return EmbergraphGraphEmbedded.this.getQueryByExternalId(extQueryId);
    }

    @Override
    protected UUID setupQuery(
        final EmbergraphSailRepositoryConnection cxn,
        final ASTContainer astContainer,
        final QueryType queryType,
        final String extQueryId) {
      return EmbergraphGraphEmbedded.this.setupQuery(cxn, astContainer, queryType, extQueryId);
    }

    @Override
    protected void tearDownQuery(final UUID queryId) {
      EmbergraphGraphEmbedded.this.tearDownQuery(queryId);
    }

    @Override
    protected boolean isQueryCancelled(final UUID queryId) {
      return EmbergraphGraphEmbedded.this.isQueryCancelled(queryId);
    }

    public StringBuilder dumpStore() throws Exception {
      return cxn.getTripleStore().dumpStore();
    }

    @Override
    public boolean isReadOnly() {
      return true;
    }
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }
}
