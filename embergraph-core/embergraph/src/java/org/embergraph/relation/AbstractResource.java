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
 * Created on Jul 10, 2008
 */

package org.embergraph.relation;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.config.Configuration;
import org.embergraph.config.IValidator;
import org.embergraph.config.IntegerValidator;
import org.embergraph.config.LongValidator;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IJournal;
import org.embergraph.journal.IResourceLock;
import org.embergraph.journal.NoSuchIndexException;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.rdf.sparql.ast.cache.CacheConnectionFactory;
import org.embergraph.rdf.sparql.ast.cache.ICacheConnection;
import org.embergraph.relation.accesspath.AccessPath;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.locator.DefaultResourceLocator;
import org.embergraph.relation.locator.ILocatableResource;
import org.embergraph.relation.rule.eval.ProgramTask;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.sparse.GlobalRowStoreUtil;

/*
 * Base class for locatable resources.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E>
 */
public abstract class AbstractResource<E> implements IMutableResource<E> {

  protected static final transient Logger log = Logger.getLogger(AbstractResource.class);

  protected final IIndexManager indexManager;

  private final String namespace;

  private final String containerNamespace;

  private final long timestamp;
  private final Long commitTime;

  private final Properties properties;

  private final int chunkOfChunksCapacity;
  private final int chunkCapacity;
  private final long chunkTimeout;
  private final int fullyBufferedReadThreshold;
  private final boolean forceSerialExecution;
  private final int maxParallelSubqueries;
  //    private final boolean nestedSubquery;

  /*
   * The capacity of the buffers accumulating chunks from concurrent producers.
   *
   * @see Options#CHUNK_OF_CHUNKS_CAPACITY
   * @deprecated by {@link BOp} annotations.
   */
  public final int getChunkOfChunksCapacity() {

    return chunkOfChunksCapacity;
  }

  /*
   * The target chunk size.
   *
   * @see Options#CHUNK_CAPACITY
   * @deprecated by {@link BOp} annotations.
   */
  public final int getChunkCapacity() {

    return chunkCapacity;
  }

  /*
   * The timeout in milliseconds that the {@link BlockingBuffer} will wait for another chunk to
   * combine with the current chunk before returning the current chunk. This may be ZERO (0) to
   * disable the chunk combiner.
   *
   * @see Options#CHUNK_TIMEOUT
   * @deprecated by {@link BOp} annotations.
   */
  public final long getChunkTimeout() {

    return chunkTimeout;
  }

  /*
   * If the estimated range count for an {@link IAccessPath#iterator(long,long, int)} is LTE this
   * threshold then do a fully buffered (synchronous) read. Otherwise we will do an asynchronous
   * read.
   *
   * @see Options#FULLY_BUFFERED_READ_THRESHOLD
   * @deprecated by {@link BOp} annotations.
   */
  public int getFullyBufferedReadThreshold() {

    return fullyBufferedReadThreshold;
  }

  /*
   * When <code>true</code>, rule sets will be forced to execute sequentially even when they are not
   * flagged as a sequential program.
   *
   * @see Options#FORCE_SERIAL_EXECUTION
   * @deprecated by {@link BOp} annotations.
   */
  public boolean isForceSerialExecution() {

    return forceSerialExecution;
  }

  /*
   * The maximum #of subqueries for the first join dimension that will be issued in parallel. Use
   * ZERO(0) to avoid submitting tasks to the {@link ExecutorService} entirely and ONE (1) to submit
   * a single task at a time to the {@link ExecutorService}.
   *
   * @see Options#MAX_PARALLEL_SUBQUERIES
   * @deprecated by {@link BOp} annotations.
   */
  public int getMaxParallelSubqueries() {

    return maxParallelSubqueries;
  }

  /*
   * Options for locatable resources.
   *
   * @todo most of these options effect asynchronous iterators, access path behavior, and join
   *     behavior. these are general features for embergraph resources, but some of the code to
   *     support this stuff is still local to the RDF module. That can be fixed using an abstract
   *     base class for {@link IJoinNexus} and {@link IJoinNexusFactory}.
   * @todo some of these defaults need to be re-examined. see notes in the javadoc below.
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public interface Options {

    /*
     * Set the maximum #of chunks from concurrent producers that can be buffered before an {@link
     * IBuffer} containing chunks of {@link ISolution}s would block (default {@link
     * #DEFAULT_CHUNK_OF_CHUNKS_CAPACITY}). This is used to provision a {@link BlockingQueue} for
     * {@link BlockingBuffer}. A value of ZERO(0) indicates that a {@link SynchronousQueue} should
     * be used instead. The best value may be more than the #of concurrent producers if the
     * producers are generating small chunks, e.g., because there are few solutions for a join
     * subquery.
     *
     * @deprecated by {@link BOp} annotations.
     */
    String CHUNK_OF_CHUNKS_CAPACITY = BlockingBuffer.class.getName() + ".chunkOfChunksCapacity";

    /*
     * Default for {@link #CHUNK_OF_CHUNKS_CAPACITY}
     *
     * @deprecated by {@link BOp} annotations.
     */
    String DEFAULT_CHUNK_OF_CHUNKS_CAPACITY = "10"; // was 1000

    /*
     * Sets the capacity of the {@link IBuffer}s used to accumulate a chunk when evaluating rules,
     * etc (default {@value #CHUNK_CAPACITY}). Note that many processes use a {@link BlockingBuffer}
     * to accumulate "chunks of chunks".
     *
     * @see #CHUNK_OF_CHUNKS_CAPACITY
     * @deprecated by {@link BOp} annotations.
     */
    String CHUNK_CAPACITY = IBuffer.class.getName() + ".chunkCapacity";

    /*
     * Default for {@link #CHUNK_CAPACITY}
     *
     * <p>Note: This used to be 20k, but chunks of chunks works better than just a large chunk.
     *
     * @deprecated by {@link BOp} annotations.
     */
    String DEFAULT_CHUNK_CAPACITY = "100"; // was 100

    /*
     * The timeout in milliseconds that the {@link BlockingBuffer} will wait for another chunk to
     * combine with the current chunk before returning the current chunk (default {@link
     * #DEFAULT_CHUNK_TIMEOUT}). This may be ZERO (0) to disable the chunk combiner.
     *
     * @deprecated by {@link BOp} annotations.
     */
    String CHUNK_TIMEOUT = BlockingBuffer.class.getName() + ".chunkTimeout";

    /*
     * The default for {@link #CHUNK_TIMEOUT}.
     *
     * @todo this is probably much larger than we want. Try 10ms.
     * @deprecated by {@link BOp} annotations.
     */
    String DEFAULT_CHUNK_TIMEOUT = "10"; // was 1000

    /*
     * If the estimated rangeCount for an {@link AccessPath#iterator()} is LTE this threshold then
     * use a fully buffered (synchronous) iterator. Otherwise use an asynchronous iterator whose
     * capacity is governed by {@link #CHUNK_OF_CHUNKS_CAPACITY}.
     *
     * @deprecated by {@link BOp} annotations.
     */
    String FULLY_BUFFERED_READ_THRESHOLD =
        AccessPath.class.getName() + ".fullyBufferedReadThreadshold";

    /*
     * Default for {@link #FULLY_BUFFERED_READ_THRESHOLD}
     *
     * @todo figure out how good this value is.
     * @deprecated by {@link BOp} annotations.
     */
    String DEFAULT_FULLY_BUFFERED_READ_THRESHOLD = "200"; // ""+20*Bytes.kilobyte32;

    /*
     * When <code>true</code> ({@value #DEFAULT_FORCE_SERIAL_EXECUTION}), rule sets will be forced
     * to execute sequentially even when they are not flagged as a sequential program.
     *
     * @todo The following discussion applies to the {@link AbstractTripleStore}. and should be
     *     relocated.
     *     <p>The {@link #CLOSURE_CLASS} option defaults to {@link FastClosure}, which has very
     *     little possible parallelism (it is mostly a sequential program by nature). For that
     *     reason, {@link #FORCE_SERIAL_EXECUTION} defaults to <code>false</code> since the overhead
     *     of parallel execution is more likely to lower the observed performance with such limited
     *     possible parallelism. However, when using {@link FullClosure} the benefits of parallelism
     *     MAY justify its overhead.
     *     <p>The following data are for LUBM datasets.
     *     <pre>
     * U1  Fast Serial   : closure =  2250ms; 2765, 2499, 2530
     * U1  Fast Parallel : closure =  2579ms; 2514, 2594
     * U1  Full Serial   : closure = 10437ms.
     * U1  Full Parallel : closure = 10843ms.
     *
     * U10 Fast Serial   : closure = 41203ms, 39171ms (38594, 35360 when running in caller's thread rather than on the executorService).
     * U10 Fast Parallel : closure = 30722ms.
     * U10 Full Serial   : closure = 108110ms.
     * U10 Full Parallel : closure = 248550ms.
     * </pre>
     *     Note that the only rules in the fast closure program that have potential parallelism are
     *     {@link RuleFastClosure5} and {@link RuleFastClosure6} and these rules are not being
     *     triggered by these datasets, so there is in fact NO potential parallelism (in the data)
     *     for these datasets.
     *     <p>It is possible that a machine with more cores would perform better under the "full"
     *     closure program with parallel rule execution (these data were collected on a laptop with
     *     2 cores) since performance tends to be CPU bound for small data sets. However, the
     *     benefit of the "fast" closure program is so large that there is little reason to consider
     *     parallel rule execution for the "full" closure program.
     * @todo collect new timings for this option. The LUBM performance has basically doubled since
     *     these data were collected. Look further into ways in which overhead might be reduced for
     *     rule parallelism and also for when rule parallelism is not enabled.
     * @todo rename as parallel_rule_execution.
     * @deprecated by {@link BOp} annotations.
     */
    String FORCE_SERIAL_EXECUTION = ProgramTask.class.getName() + ".forceSerialExecution";

    /** @deprecated by {@link BOp} annotations. */
    String DEFAULT_FORCE_SERIAL_EXECUTION = "true";

    /*
     * The maximum #of subqueries for the first join dimension that will be issued in parallel. Use
     * ZERO(0) to avoid submitting tasks to the {@link ExecutorService} entirely and ONE (1) to
     * submit a single task at a time to the {@link ExecutorService}.
     *
     * @deprecated by {@link BOp} annotations.
     */
    String MAX_PARALLEL_SUBQUERIES = ProgramTask.class.getName() + ".maxParallelSubqueries";

    /** @deprecated by {@link BOp} annotations. */
    String DEFAULT_MAX_PARALLEL_SUBQUERIES = "5";
  }

  protected AbstractResource(
      final IIndexManager indexManager,
      final String namespace,
      final Long timestamp,
      final Properties properties) {

    this(null /* container */, indexManager, namespace, timestamp, properties);
  }

  /*
   * Alternative version used when a resource exists within some container. The additional
   * <i>container</i> argument provides access to the container before the container has been
   * written to the global row store.
   */
  protected AbstractResource(
      final ILocatableResource container,
      final IIndexManager indexManager,
      final String namespace,
      final Long timestamp,
      final Properties properties) {

    // Note: [container] MAY be null.

    if (indexManager == null) throw new IllegalArgumentException();

    if (namespace == null) throw new IllegalArgumentException();

    if (timestamp == null) throw new IllegalArgumentException();

    if (properties == null) throw new IllegalArgumentException();

    // Note: Non-null if this resource exists in some container
    this.container = container;

    // Note: Bound before we lookup property values!
    this.indexManager = indexManager;

    // Note: Bound before we lookup property values!
    this.namespace = namespace;

    this.containerNamespace = properties.getProperty(RelationSchema.CONTAINER);

    this.timestamp = timestamp;

    this.properties = properties;

    properties.setProperty(RelationSchema.NAMESPACE, namespace);

    properties.setProperty(RelationSchema.CLASS, getClass().getName());

    if (log.isInfoEnabled()) {

      log.info(
          "namespace="
              + namespace
              + ", timestamp="
              + timestamp
              + ", container="
              + containerNamespace
              + ", indexManager="
              + indexManager);
    }

    /*
     * Resolve the commit time from which this view was materialized (if
     * known and otherwise null).
     */
    commitTime = (Long) properties.get(RelationSchema.COMMIT_TIME);

    forceSerialExecution =
        Boolean.parseBoolean(
            getProperty(Options.FORCE_SERIAL_EXECUTION, Options.DEFAULT_FORCE_SERIAL_EXECUTION));

    maxParallelSubqueries =
        getProperty(
            Options.MAX_PARALLEL_SUBQUERIES,
            Options.DEFAULT_MAX_PARALLEL_SUBQUERIES,
            IntegerValidator.GTE_ZERO);

    chunkOfChunksCapacity =
        getProperty(
            Options.CHUNK_OF_CHUNKS_CAPACITY,
            Options.DEFAULT_CHUNK_OF_CHUNKS_CAPACITY,
            IntegerValidator.GT_ZERO);

    chunkCapacity =
        getProperty(
            Options.CHUNK_CAPACITY, Options.DEFAULT_CHUNK_CAPACITY, IntegerValidator.GT_ZERO);

    chunkTimeout =
        getProperty(Options.CHUNK_TIMEOUT, Options.DEFAULT_CHUNK_TIMEOUT, LongValidator.GTE_ZERO);

    fullyBufferedReadThreshold =
        getProperty(
            Options.FULLY_BUFFERED_READ_THRESHOLD,
            Options.DEFAULT_FULLY_BUFFERED_READ_THRESHOLD,
            IntegerValidator.GT_ZERO);
  }

  public final String getNamespace() {

    return namespace;
  }

  public final String getContainerNamespace() {

    return containerNamespace;
  }

  /*
   * Return the container.
   *
   * @return The container -or- <code>null</code> if there is no container.
   */
  public ILocatableResource getContainer() {

    if (container == null) {

      synchronized (this) {
        if (container == null) {

          if (getContainerNamespace() != null) {

            if (log.isInfoEnabled()) {

              log.info("resolving container: " + getContainerNamespace());
            }

            container =
                getIndexManager()
                    .getResourceLocator()
                    .locate(getContainerNamespace(), getTimestamp());
          }
        }
      }
    }

    return container;
  }

  private volatile ILocatableResource container;

  public final long getTimestamp() {

    return timestamp;
  }

  /*
   * The commit time from which a read-only view was materialized (if known) and otherwise <code>
   * null</code>.
   *
   * @see https://sourceforge.net/apps/trac/bigdata/ticket/266
   */
  protected Long getCommitTime() {

    return commitTime;
  }

  /*
   * Wrap and return the properties specified to the ctor. Wrapping the {@link Properties} object
   * prevents inadvertent side-effects.
   */
  public final Properties getProperties() {

    return new Properties(properties);
  }

  /*
   * Return the {@link Properties} object without wrapping it. This method can be used in those
   * cases where you need to access non-String property values. The caller is responsible for
   * avoiding mutation to the returned Properties object.
   *
   * <p>Note: This explicitly does NOT wrap the properties. Doing so makes it impossible to access
   * the default properties using Hashtable#get(), which in turn means that we can not access
   * non-String objects which have been materialized from the GRS in the {@link Properties}. This
   * does introduce some potential for side-effects between read-only instances of the same resource
   * view which share the same properties object.
   */
  protected final Properties getBareProperties() {

    return properties;
  }

  /*
   * Return the object used to locate indices, relations, and relation containers and to execute
   * operations on those resources.
   *
   * <p>Note: For scale-out, this is <em>always</em> the federation's index manager NOT the data
   * service local index manager. This is an artifact of how we resolve the metadata for the
   * relation against the global row store.
   *
   * @return The {@link IIndexManager}.
   * @todo If we support the notion of a "relation shard" then this could become the shard's data
   *     service local index manager in that instance but, regardless, we would need a means to
   *     resolve the metadata for the relation against the federation's index manager
   */
  public IIndexManager getIndexManager() {

    return indexManager;
  }

  public final ExecutorService getExecutorService() {

    return indexManager.getExecutorService();
  }

  /** The class name, timestamp and namespace for the relation view. */
  public String toString() {

    return getClass().getSimpleName()
        + "{timestamp="
        + timestamp
        + ", namespace="
        + namespace
        + ", container="
        + containerNamespace
        + ", indexManager="
        + indexManager
        + "}";
  }

  /** The default implementation only logs the event. */
  public AbstractResource<E> init() {

    if (log.isInfoEnabled()) log.info(toString());

    return this;
  }

  /*
   * @todo Lock service supporting shared locks, leases and lease renewal, escalation of shared
   *     locks to exclusive locks, deadlock detection, and possibly a resource hierarchy. Leases
   *     should be Callable objects that are submitted by the client to its executor service so that
   *     they will renew automatically until cancelled (and will cancel automatically if not
   *     renewed).
   *     <p>There is existing code that could be adapted for this purpose. It might have to be
   *     adapted to support lock escalation (shared to exclusive), a resource hierarchy, and a delay
   *     queue to cancel leases that are not renewed. It would have to be wrapped up as a
   *     low-latency service and made available via the {@link IEmbergraphFederation}. It also needs
   *     to use a weak reference cache for the collection of resource queues so that they are GC'd
   *     rather than growing as new resources are locked and never shrinking.
   *     <p>If we require pre-declaration of locks, then we do not need the dependency graph since
   *     deadlocks can only arise with 2PL.
   *     <p>Since the service is remote it should use {@link UUID}s to identify the lock owner(s).
   *     <p>The lock service would be used to bracket operations such as relation {@link #create()}
   *     and {@link #destroy()} and would be used to prevent those operations while a lease is held
   *     by concurrent processes with a shared lock.
   *     <p>Add ctor flag to create iff not found?
   *     <p>There needs to be a lock protocol for subclasses so that they can ensure that they are
   *     the only task running create (across the federation) and so that they can release the lock
   *     when they are done. The lock can be per the notes above, but the protocol with the subclass
   *     will require some coordinating methods.
   *     <p>Full transactions are another way to solve this problem.
   */
  @Override
  public void create() {

    if (log.isInfoEnabled()) log.info(toString());

    /*
     * Convert the Properties to a Map.
     */
    final Map<String, Object> map = GlobalRowStoreUtil.convert(properties);

    // Write the map on the row store.
    final Map<String, Object> afterMap =
        indexManager.getGlobalRowStore().write(RelationSchema.INSTANCE, map);

    if (log.isDebugEnabled()) {

      log.debug("Properties after write: " + afterMap);
    }

    /*
     * Add this instance to the locator cache.
     *
     * Note: Normally, the instances are created by the locator cache
     * itself. In general the only time the application creates an instance
     * directly is when it is going to attempt to create the relation. This
     * takes advantage of that pattern to notify the locator that it should
     * cache this instance.
     */

    ((DefaultResourceLocator) getIndexManager().getResourceLocator()).putInstance(this);
  }

  @Override
  public void destroy() {

    if (log.isInfoEnabled()) log.info(toString());

    /*
     * Destroy all indices spanned by the namespace for this relation.
     */
    {
      final Iterator<String> itr = indexManager.indexNameScan(namespace + ".", timestamp);

      while (itr.hasNext()) {

        final String name = itr.next();

        try {
          indexManager.dropIndex(name);
        } catch (NoSuchIndexException ex) {
          // If the index does not exist, keep on going.
          log.warn("Ignored: " + ex);
        }
      }
    }

    /*
     * Destroy any caches associated with this relation.
     */
    {
      if (indexManager instanceof IJournal || indexManager instanceof IEmbergraphFederation) {

        /*
         * Note: No cache associated with a TemporaryStore (since no
         * QueryEngine is associated with a TemporaryStore).
         *
         * Note: If the cache is remote, then we need to create the
         * QueryEngine instance on demand so we can access the cache.
         * Hence getQueryController() and not getExistingQueryEngine().
         *
         * TODO This could wind up using the wrong index manager if the
         * destroy() is executed by an AbstractTask (neither the Journal
         * nor the JiniFederation).
         */
        final QueryEngine queryEngine =
            QueryEngineFactory.getInstance().getQueryController(indexManager);

        /*
         * Connect to the cache provider.
         *
         * Note: This will create the cache if it does not exist. At all
         * other places in the code we use getExistingSparqlCache() to
         * access the cache IFF it exists. Here is where we create it.
         */
        final ICacheConnection cacheConn = CacheConnectionFactory.getCacheConnection(queryEngine);

        if (cacheConn != null) {

          cacheConn.destroyCaches(namespace, timestamp);
        }
      }
    }

    // Delete the entry for this relation from the row store.
    indexManager.getGlobalRowStore().delete(RelationSchema.INSTANCE, namespace);

    // Clear the entry from the resource locator cache.
    indexManager.getResourceLocator().discard(this, true /* destroyed */);
  }

  /*
   * Acquires an exclusive lock for the {@link #getNamespace()}.
   *
   * @return the lock.
   * @throws RuntimeException if anything goes wrong.
   * @see IResourceLockService
   */
  protected IResourceLock acquireExclusiveLock() {

    return indexManager.getResourceLockService().acquireLock(getNamespace());
  }

  /*
   * Release the lock.
   *
   * @param resourceLock The lock.
   */
  protected void unlock(final IResourceLock resourceLock) {

    resourceLock.unlock();
  }

  /*
   * Resolve the property value using the {@link IIndexManager}, the namespace of the resource, and
   * the {@link Properties} instance to be tested as hidden parameters.
   *
   * @param globalName The global property name.
   * @param defaultValue The default.
   * @return The resolved property value.
   * @see Configuration
   */
  public String getProperty(final String localName, final String defaultValue) {

    return Configuration.getProperty(indexManager, properties, namespace, localName, defaultValue);
  }

  /*
   * Resolves, parses, and validates the property value.
   *
   * @param name The property name.
   * @param defaultValue The default value.
   * @return
   */
  public <T> T getProperty(
      final String name, final String defaultValue, final IValidator<T> validator) {

    return Configuration.getProperty(
        indexManager, properties, namespace, name, defaultValue, validator);
  }

  //    /*
  //     * Sets the property on the underlying properties object but DOES NOT set
  //     * the property on the global row store (GRS). This method may be used when
  //     * a resource is newly created in order to cache objects which are persisted
  //     * on the GRS.
  //     *
  //     * @param name
  //     *            The property name.
  //     * @param value
  //     *            The property value.
  //     */
  //    protected void setProperty(final String name, final Object value) {
  //
  //        properties.put(name, value);
  //
  //    }

  public boolean isReadOnly() {

    return TimestampUtility.isReadOnly(getTimestamp());
  }

  protected final void assertWritable() {

    if (isReadOnly()) {

      throw new IllegalStateException("READ_ONLY");
    }
  }
}
