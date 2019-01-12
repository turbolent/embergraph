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
 * Created on Mar 28, 2008
 */

package org.embergraph.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.embergraph.bfs.EmbergraphFileSystem;
import org.embergraph.bfs.GlobalFileSystemHelper;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.counters.AbstractStatisticsCollector;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterSet;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.counters.IServiceCounters;
import org.embergraph.counters.OneShotInstrument;
import org.embergraph.counters.ganglia.EmbergraphGangliaService;
import org.embergraph.counters.ganglia.EmbergraphMetadataFactory;
import org.embergraph.counters.ganglia.HostMetricsCollector;
import org.embergraph.counters.ganglia.QueryEngineMetricsCollector;
import org.embergraph.counters.query.QueryUtil;
import org.embergraph.ganglia.DefaultMetadataFactory;
import org.embergraph.ganglia.GangliaMetadataFactory;
import org.embergraph.ganglia.GangliaSlopeEnum;
import org.embergraph.ganglia.IGangliaDefaults;
import org.embergraph.ganglia.util.GangliaUtil;
import org.embergraph.journal.NoSuchIndexException;
import org.embergraph.journal.TemporaryStore;
import org.embergraph.journal.TemporaryStoreFactory;
import org.embergraph.relation.locator.DefaultResourceLocator;
import org.embergraph.service.IEmbergraphClient.Options;
import org.embergraph.service.ndx.IClientIndex;
import org.embergraph.service.ndx.ScaleOutIndexCounters;
import org.embergraph.sparse.GlobalRowStoreHelper;
import org.embergraph.sparse.SparseRowStore;
import org.embergraph.util.Bytes;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.InnerCause;
import org.embergraph.util.concurrent.ShutdownHelper;
import org.embergraph.util.concurrent.TaskCounters;
import org.embergraph.util.concurrent.ThreadPoolExecutorStatisticsTask;
import org.embergraph.util.httpd.AbstractHTTPD;

/*
 * Abstract base class for {@link IEmbergraphFederation} implementations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T> The generic type of the client or service.
 * @todo implement {@link IServiceShutdown}. When it is declared here it messes up the Options
 *     interface hierarchy. What appears to be happening is that the IServiceShutdown.Options
 *     interface is flattened into IServiceShutdown and it shadows the Options that are being used.
 */
public abstract class AbstractFederation<T> implements IEmbergraphFederation<T> {

  protected static final Logger log = Logger.getLogger(IEmbergraphFederation.class);

  /** The client (if connected). */
  private final AtomicReference<AbstractClient<T>> client =
      new AtomicReference<AbstractClient<T>>();

  private final boolean collectPlatformStatistics;
  private final boolean collectQueueStatistics;
  private final int httpdPort;

  /*
   * <code>true</code> iff open. Note that during shutdown this will be set to <code>false</code>
   * before the client reference is cleared in order to avoid an infinite recursion when we request
   * that the client disconnect itself so its reference to the federation will be cleared along with
   * the federation's reference to the client.
   */
  private final AtomicBoolean open = new AtomicBoolean(false);

  @Override
  public AbstractClient<T> getClient() {

    final AbstractClient<T> t = client.get();

    if (t == null) throw new IllegalStateException();

    return t;
  }

  public final boolean isOpen() {

    return open.get();
  }

  /*
   * Normal shutdown allows any existing client requests to federation services to complete but does
   * not schedule new requests, disconnects from the federation, and then terminates any background
   * processing that is being performed on the behalf of the client (service discovery, etc).
   *
   * <p>Note: concrete implementations MUST extend this method.
   *
   * <p>Note: Clients use {@link IEmbergraphClient#disconnect(boolean)} to disconnect from a
   * federation. The federation implements that disconnect using either {@link #shutdown()} or
   * {@link #shutdownNow()}.
   *
   * <p>The implementation must be a NOP if the federation is already shutdown.
   */
  public synchronized void shutdown() {

    if (!open.compareAndSet(true /* expect */, false /* update */)) {
      // Already closed.
      return;
    }

    final long begin = System.currentTimeMillis();

    if (log.isInfoEnabled()) log.info("begin");

    try {

      {
        /*
         * Note: The embedded GangliaService is executed on the main
         * thread pool. We need to terminate the GangliaService in order
         * for the thread pool to shutdown.
         */

        final FutureTask<Void> ft = gangliaFuture.getAndSet(null);

        if (ft != null) {

          ft.cancel(true /* mayInterruptIfRunning */);
        }

        // Clear the state reference.
        gangliaService.set(null);
      }

      // allow client requests to finish normally.
      new ShutdownHelper(threadPool, 10L /*logTimeout*/, TimeUnit.SECONDS) {

        @Override
        public void logTimeout() {

          log.warn(
              "Awaiting thread pool termination: elapsed="
                  + TimeUnit.NANOSECONDS.toMillis(elapsed())
                  + "ms");
        }
      };

      {
        final AbstractStatisticsCollector t = statisticsCollector.getAndSet(null);

        if (t != null) {

          t.stop();
        }
      }

      // terminate sampling and reporting tasks.
      new ShutdownHelper(scheduledExecutorService, 10L /* logTimeout */, TimeUnit.SECONDS) {

        @Override
        public void logTimeout() {

          log.warn(
              "Awaiting sample service termination: elapsed="
                  + TimeUnit.NANOSECONDS.toMillis(elapsed())
                  + "ms");
        }
      };

    } catch (InterruptedException e) {

      log.warn("Interrupted awaiting thread pool termination.", e);
    }

    // drain any events in one last report.
    new SendEventsTask().run();

    // optional httpd service for the local counters.
    {
      final AbstractHTTPD t = httpd.getAndSet(null);

      if (t != null) {

        t.shutdown();
      }

      httpdURL.set(null);
    }

    if (log.isInfoEnabled()) log.info("done: elapsed=" + (System.currentTimeMillis() - begin));

    {
      // Get and Release our reference to the client.
      final AbstractClient<T> t = client.getAndSet(null);

      if (t != null) {

        // Force the client to release its reference to the federation.
        t.disconnect(false /* immediateShutdown */);
      }
    }

    tempStoreFactory.closeAll();
  }

  /*
   * Immediate shutdown terminates any client requests to federation services, disconnects from the
   * federation, and then terminate any background processing that is being performed on the behalf
   * of the client (service discovery, etc).
   *
   * <p>Note: concrete implementations MUST extend this method to either disconnect from the remote
   * federation or close the embedded federation and then clear the {@link #fed} reference so that
   * the client is no longer "connected" to the federation.
   *
   * <p>Note: Clients use {@link IEmbergraphClient#disconnect(boolean)} to disconnect from a
   * federation. The federation implements that disconnect using either {@link #shutdown()} or
   * {@link #shutdownNow()}.
   *
   * <p>The implementation must be a NOP if the federation is already shutdown.
   */
  public synchronized void shutdownNow() {

    if (!open.compareAndSet(true /* expect */, false /* update */)) {
      // Already closed.
      return;
    }

    final long begin = System.currentTimeMillis();

    if (log.isInfoEnabled()) log.info("begin");

    // stop client requests.
    threadPool.shutdownNow();

    {
      final AbstractStatisticsCollector t = statisticsCollector.getAndSet(null);

      if (t != null) {

        t.stop();
      }
    }

    {
      final FutureTask<Void> ft = gangliaFuture.getAndSet(null);

      if (ft != null) {

        ft.cancel(true /* mayInterruptIfRunning */);
      }

      // Clear the state reference.
      gangliaService.set(null);
    }

    // terminate sampling and reporting tasks immediately.
    scheduledExecutorService.shutdownNow();

    // discard any events still in the queue.
    events.clear();

    // terminate the optional httpd service for the client's live counters.
    {
      final AbstractHTTPD t = httpd.getAndSet(null);

      if (t != null) {

        t.shutdownNow();
      }

      httpdURL.set(null);
    }

    if (log.isInfoEnabled()) log.info("done: elapsed=" + (System.currentTimeMillis() - begin));

    {

      // Get and release our reference to the client.
      final AbstractClient<T> t = client.get();

      if (t != null) {

        // Force the client to release its reference to the federation.
        t.disconnect(true /* immediateShutdown */);
      }
    }

    tempStoreFactory.closeAll();
  }

  @Override
  public synchronized void destroy() {

    if (isOpen()) shutdownNow();

    tempStoreFactory.closeAll();
  }

  /** @throws IllegalStateException if the client has disconnected from the federation. */
  protected final void assertOpen() {

    if (client == null) {

      throw new IllegalStateException();
    }
  }

  /** Used to run application tasks. */
  private final ThreadPoolExecutor threadPool;

  /** Used to sample and report on the queue associated with the {@link #threadPool}. */
  private final ScheduledExecutorService scheduledExecutorService;

  /** A service which may be used to schedule performance counter sampling tasks. */
  public ScheduledExecutorService getScheduledExecutorService() {

    return scheduledExecutorService;
  }

  /*
   * {@inheritDoc}
   *
   * @see IEmbergraphClient.Options#COLLECT_PLATFORM_STATISTICS
   */
  @Override
  public boolean getCollectPlatformStatistics() {

    return collectPlatformStatistics;
  }

  /*
   * {@inheritDoc}
   *
   * @see IEmbergraphClient.Options#COLLECT_QUEUE_STATISTICS
   */
  @Override
  public boolean getCollectQueueStatistics() {

    return collectQueueStatistics;
  }

  /*
   * {@inheritDoc}
   *
   * @see IEmbergraphClient.Options#HTTPD_PORT
   */
  @Override
  public int getHttpdPort() {

    return httpdPort;
  }

  /** httpd reporting the live counters for the client while it is connected to the federation. */
  private final AtomicReference<AbstractHTTPD> httpd = new AtomicReference<AbstractHTTPD>();

  /** The URL that may be used to access the httpd service exposed by this client. */
  private final AtomicReference<String> httpdURL = new AtomicReference<String>();

  @Override
  public final String getHttpdURL() {

    return httpdURL.get();
  }

  /** Locator for relations, etc. */
  private final DefaultResourceLocator<?> resourceLocator;

  @Override
  public DefaultResourceLocator<?> getResourceLocator() {

    assertOpen();

    return resourceLocator;
  }

  /*
   * Counters that aggregate across all tasks submitted by the client against the connected
   * federation. Those counters are sampled by a {@link ThreadPoolExecutorStatisticsTask} and
   * reported by the client to the {@link ILoadBalancerService}.
   */
  private final TaskCounters taskCounters = new TaskCounters();

  /*
   * Counters for each scale-out index accessed by the client.
   *
   * @todo A hard reference map is used to prevent the counters from being finalized so that they
   *     will reflect the use by the client over the life of its operations. However, this could be
   *     a problem for an {@link IDataService} or {@link IClientService} since any number of clients
   *     could run over time. If there were a large #of distinct scale-out indices then this would
   *     effectively represent a memory leak. The other way to handle this would be a {@link
   *     ConcurrentWeakValueCacheWithTimeout} where the timeout was relatively long - 10m or more.
   * @todo indices isolated by a read-write tx will need their own counters for each isolated view
   *     or we can just let the counters indicates the unisolated writes.
   */
  private final Map<String, ScaleOutIndexCounters> scaleOutIndexCounters =
      new HashMap<String, ScaleOutIndexCounters>();

  /*
   * Return the {@link TaskCounters} which aggregate across all operations performed by the client
   * against the connected federation. These {@link TaskCounters} are sampled by a {@link
   * ThreadPoolExecutorStatisticsTask} and the sampled data are reported by the client to the {@link
   * ILoadBalancerService}.
   */
  public TaskCounters getTaskCounters() {

    return taskCounters;
  }

  /*
   * Return the {@link ScaleOutIndexCounters} for the specified scale-out index for this client.
   * There is only a single instance per scale-out index and all operations by this client on that
   * index are aggregated by that instance. These counters are reported by the client to the {@link
   * ILoadBalancerService}.
   *
   * @param name The scale-out index name.
   */
  public ScaleOutIndexCounters getIndexCounters(final String name) {

    if (name == null) throw new IllegalArgumentException();

    synchronized (scaleOutIndexCounters) {
      ScaleOutIndexCounters t = scaleOutIndexCounters.get(name);

      if (t == null) {

        t = new ScaleOutIndexCounters(this);

        scaleOutIndexCounters.put(name, t);

        /*
         * Attach to the counters reported by the client to the LBS.
         *
         * Note: The counters should not exist under this path since we
         * are just creating them now, but if they do then they are
         * replaced.
         */
        getServiceCounterSet()
            .makePath("Indices")
            .makePath(name)
            .attach(t.getCounters(), true /* replace */);
      }

      return t;
    }
  }

  /*
   * Collects interesting statistics on the client's host and process for reporting to the {@link
   * ILoadBalancerService}.
   */
  private final AtomicReference<AbstractStatisticsCollector> statisticsCollector =
      new AtomicReference<AbstractStatisticsCollector>();

  /*
   * Future for an embedded {@link GangliaService} which listens to <code>gmond</code> instances and
   * other {@link GangliaService}s and reports out metrics from {@link #getCounters()} to the
   * ganglia network.
   */
  private final AtomicReference<FutureTask<Void>> gangliaFuture =
      new AtomicReference<FutureTask<Void>>();

  /** The embedded ganglia peer. */
  private final AtomicReference<EmbergraphGangliaService> gangliaService =
      new AtomicReference<EmbergraphGangliaService>();

  @Override
  public ScheduledFuture<?> addScheduledTask(
      final Runnable task, final long initialDelay, final long delay, final TimeUnit unit) {

    if (task == null) throw new IllegalArgumentException();

    if (log.isInfoEnabled())
      log.info(
          "Scheduling task: task="
              + task.getClass()
              + ", initialDelay="
              + initialDelay
              + ", delay="
              + delay
              + ", unit="
              + unit);

    return scheduledExecutorService.scheduleWithFixedDelay(task, initialDelay, delay, unit);
  }

  /** The embedded ganglia peer. This can be used to obtain load balanced host reports, etc. */
  public final EmbergraphGangliaService getGangliaService() {

    return gangliaService.get();
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: This method must use stateful counters because the federation services all need to be
   * able to report their history. If this were to return a new {@link CounterSet} each time, then
   * the services would not remember any history (including the load balancer). Returning a new
   * object every time here basically throws away the data we want.
   */
  @Override
  public final CounterSet getCounters() {

    countersLock.lock();
    try {

      if (countersRoot == null) {

        countersRoot = new CounterSet();
        {
          final AbstractStatisticsCollector tmp = statisticsCollector.get();

          if (tmp != null) {

            countersRoot.attach(tmp.getCounters());
          }
        }

        serviceRoot = countersRoot.makePath(getServiceCounterPathPrefix());

        {
          final String s = httpdURL.get();

          if (s != null) {

            // add counter reporting that url to the load balancer.
            serviceRoot.addCounter(IServiceCounters.LOCAL_HTTPD, new OneShotInstrument<String>(s));
          }
        }

        /*
         * Basic counters.
         */

        AbstractStatisticsCollector.addBasicServiceOrClientCounters(
            serviceRoot, getServiceName(), getServiceIface(), getClient().getProperties());
      }

      /*
       * Note: We can not invoke this here. It causes recursion back into
       * this method from the reattachDynamicCounters() impls.
       */
      //            reattachDynamicCounters();

      return countersRoot;

    } finally {

      countersLock.unlock();
    }
  }

  /*
   * Lock guarding {@link #countersRoot} and {@link #serviceRoot}.
   *
   * <p>Note: These objects can not be guarded by <code>synchronized(this)</code> without risking a
   * deadlock in {@link #shutdown()}.
   */
  private final Lock countersLock = new ReentrantLock(false /*fair*/);

  /*
   * The top-level of the counterset hierarchy.
   *
   * <p>Guarded by {@link #countersLock}
   */
  private CounterSet countersRoot;

  /*
   * The root of the service specific section of the counterset hierarchy.
   *
   * <p>Guarded by {@link #countersLock}
   */
  private CounterSet serviceRoot;

  @Override
  public CounterSet getHostCounterSet() {

    final String pathPrefix =
        ICounterSet.pathSeparator + AbstractStatisticsCollector.fullyQualifiedHostName;

    return (CounterSet) getCounters().getPath(pathPrefix);
  }

  @Override
  public CounterSet getServiceCounterSet() {

    // note: defines [serviceRoot] as side effect.
    getCounters();

    return serviceRoot;
  }

  @Override
  public String getServiceCounterPathPrefix() {

    final String hostname = AbstractStatisticsCollector.fullyQualifiedHostName;

    return getServiceCounterPathPrefix(getServiceUUID(), getServiceIface(), hostname);
  }

  /*
   * The path prefix under which all of the client or service's counters are located. The returned
   * path prefix is terminated by an {@link ICounterSet#pathSeparator}.
   *
   * @param serviceUUID The service {@link UUID}.
   * @param serviceIface The primary interface or class for the service.
   * @param hostname The fully qualified name of the host on which the service is running.
   */
  public static String getServiceCounterPathPrefix(
      final UUID serviceUUID, final Class serviceIface, final String hostname) {

    if (serviceUUID == null) throw new IllegalArgumentException();

    if (serviceIface == null) throw new IllegalArgumentException();

    if (hostname == null) throw new IllegalArgumentException();

    final String ps = ICounterSet.pathSeparator;

    final String pathPrefix =
        ps + hostname + ps + "service" + ps + serviceIface.getName() + ps + serviceUUID + ps;

    return pathPrefix;
  }

  @Override
  public ExecutorService getExecutorService() {

    assertOpen();

    return threadPool;
  }

  protected AbstractFederation(final IEmbergraphClient<T> client) {

    if (client == null) throw new IllegalArgumentException();

    this.open.set(true);

    final AbstractClient<T> client2 = (AbstractClient<T>) client;

    this.client.set(client2);

    if (client2.getDelegate() == null) {

      /*
       * If no one has set the delegate by this point then we setup a
       * default delegate.
       */

      client2.setDelegate(new DefaultClientDelegate<T>(client, null /* clientOrService */));
    }

    final int threadPoolSize = client.getThreadPoolSize();

    if (threadPoolSize == 0) {

      threadPool =
          (ThreadPoolExecutor)
              Executors.newCachedThreadPool(
                  new DaemonThreadFactory(getClass().getName() + ".executorService"));

    } else {

      threadPool =
          (ThreadPoolExecutor)
              Executors.newFixedThreadPool(
                  threadPoolSize,
                  new DaemonThreadFactory(getClass().getName() + ".executorService"));
    }

    scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            new DaemonThreadFactory(getClass().getName() + ".scheduledService"));

    tempStoreFactory = new TemporaryStoreFactory(client.getProperties());

    //        tempStoreFactory = new TemporaryStoreFactory(this.client
    //                .getTempStoreMaxExtent());

    final Properties properties = client.getProperties();
    {
      collectPlatformStatistics =
          Boolean.parseBoolean(
              properties.getProperty(
                  Options.COLLECT_PLATFORM_STATISTICS,
                  Options.DEFAULT_COLLECT_PLATFORM_STATISTICS));

      if (log.isInfoEnabled())
        log.info(Options.COLLECT_PLATFORM_STATISTICS + "=" + collectPlatformStatistics);
    }

    {
      collectQueueStatistics =
          Boolean.parseBoolean(
              properties.getProperty(
                  Options.COLLECT_QUEUE_STATISTICS, Options.DEFAULT_COLLECT_QUEUE_STATISTICS));

      if (log.isInfoEnabled())
        log.info(Options.COLLECT_QUEUE_STATISTICS + "=" + collectQueueStatistics);
    }

    {
      httpdPort =
          Integer.parseInt(properties.getProperty(Options.HTTPD_PORT, Options.DEFAULT_HTTPD_PORT));

      if (log.isInfoEnabled()) log.info(Options.HTTPD_PORT + "=" + httpdPort);

      if (httpdPort < 0 && httpdPort != -1)
        throw new RuntimeException(
            Options.HTTPD_PORT + " must be -1 (disabled), 0 (random port), or positive");
    }

    addScheduledTask(
        new SendEventsTask(), // task to run.
        100, // initialDelay (ms)
        2000, // delay
        TimeUnit.MILLISECONDS // unit
        );

    getExecutorService().execute(new StartDeferredTasksTask());

    // Setup locator.
    resourceLocator =
        new DefaultResourceLocator(
            this,
            null, // delegate
            ((AbstractClient<T>) client).getLocatorCacheCapacity(),
            ((AbstractClient<T>) client).getLocatorCacheTimeout());
  }

  /*
   * The {@link IEmbergraphFederation} supports group commit (and always has). The client side API
   * submits tasks. Those tasks are scheduled on the {@link IDataService} using the group commit
   * mechanisms.
   */
  @Override
  public boolean isGroupCommit() {
    return true;
  }

  @Override
  public void registerIndex(final IndexMetadata metadata) {

    assertOpen();

    registerIndex(metadata, null);
  }

  @Override
  public UUID registerIndex(final IndexMetadata metadata, UUID dataServiceUUID) {

    assertOpen();

    if (dataServiceUUID == null) {

      // see if there is an override.
      dataServiceUUID = metadata.getInitialDataServiceUUID();

      if (dataServiceUUID == null) {

        final ILoadBalancerService loadBalancerService = getLoadBalancerService();

        if (loadBalancerService == null) {

          try {

            /*
             * As a failsafe (or at least a failback) we ask the
             * client for ANY data service that it knows about and
             * use that as the data service on which we will
             * register this index. This lets us keep going if the
             * load balancer is dead when this request comes
             * through.
             */

            dataServiceUUID = getAnyDataService().getServiceUUID();

          } catch (Exception ex) {

            log.error(ex);

            throw new RuntimeException(ex);
          }

        } else {

          try {

            dataServiceUUID = loadBalancerService.getUnderUtilizedDataService();

          } catch (Exception ex) {

            throw new RuntimeException(ex);
          }
        }
      }
    }

    return registerIndex(metadata, new byte[][] {new byte[] {}}, new UUID[] {dataServiceUUID});
  }

  public UUID registerIndex(
      final IndexMetadata metadata, final byte[][] separatorKeys, final UUID[] dataServiceUUIDs) {

    assertOpen();

    try {

      UUID indexUUID =
          getMetadataService().registerScaleOutIndex(metadata, separatorKeys, dataServiceUUIDs);

      return indexUUID;

    } catch (Exception ex) {

      throw new RuntimeException(ex);
    }
  }

  /** Return the cache for {@link IIndex} objects. */
  protected abstract AbstractIndexCache<? extends IClientIndex> getIndexCache();

  /*
   * Applies an {@link AbstractIndexCache} and strengthens the return type.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public IClientIndex getIndex(final String name, final long timestamp) {

    if (log.isInfoEnabled()) log.info("name=" + name + " @ " + timestamp);

    assertOpen();

    return getIndexCache().getIndex(name, timestamp);
  }

  @Override
  public void dropIndex(final String name) {

    if (log.isInfoEnabled()) log.info("name=" + name);

    assertOpen();

    try {

      getMetadataService().dropScaleOutIndex(name);

      if (log.isInfoEnabled()) log.info("dropped scale-out index.");

      getIndexCache().dropIndexFromCache(name);

    } catch (Exception e) {
      if (InnerCause.isInnerCause(e, NoSuchIndexException.class)) {
        /*
         * Wrap with the root cause per the API for dropIndex().
         */
        final NoSuchIndexException tmp = new NoSuchIndexException(name);
        tmp.initCause(e);
        throw tmp;
      }
      throw new RuntimeException(e);
    }
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: This implementation fully buffers the namespace scan.
   */
  @Override
  public Iterator<String> indexNameScan(final String prefix, final long timestamp) {

    if (log.isInfoEnabled()) log.info("prefix=" + prefix + " @ " + timestamp);

    assertOpen();

    try {

      final String namespace =
          MetadataService.METADATA_INDEX_NAMESPACE + (prefix == null ? "" : prefix);

      final IMetadataService mds = getMetadataService();

      if (mds == null) {

        throw new RuntimeException("Could not discover the metadata service");
      }

      final String[] names = (String[]) mds.submit(new ListIndicesTask(timestamp, namespace)).get();

      return Arrays.asList(names).iterator();

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
  }

  @Override
  public SparseRowStore getGlobalRowStore() {

    return globalRowStoreHelper.getGlobalRowStore();
  }

  @Override
  public SparseRowStore getGlobalRowStore(final long timestamp) {

    return globalRowStoreHelper.get(timestamp);
  }

  private final GlobalRowStoreHelper globalRowStoreHelper = new GlobalRowStoreHelper(this);

  @Override
  public EmbergraphFileSystem getGlobalFileSystem() {

    return globalFileSystemHelper.getGlobalFileSystem();
  }

  private final GlobalFileSystemHelper globalFileSystemHelper = new GlobalFileSystemHelper(this);

  @Override
  public TemporaryStore getTempStore() {

    return tempStoreFactory.getTempStore();
  }

  private final TemporaryStoreFactory tempStoreFactory;

  /*
   * Forces the immediate reporting of the {@link CounterSet} to the {@link ILoadBalancerService}.
   * Any errors will be logged, not thrown.
   */
  public void reportCounters() {

    new ReportTask(this).run();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public T getService() {

    return getClient().getDelegate().getService();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public String getServiceName() {

    return getClient().getDelegate().getServiceName();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public Class<?> getServiceIface() {

    return getClient().getDelegate().getServiceIface();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public UUID getServiceUUID() {

    return getClient().getDelegate().getServiceUUID();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public boolean isServiceReady() {

    final AbstractClient<T> thisClient = client.get();

    if (thisClient == null) return false;

    final IFederationDelegate<T> delegate = thisClient.getDelegate();

    if (delegate == null) return false;

    // assertOpen();

    return delegate.isServiceReady();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public void reattachDynamicCounters() {

    getClient().getDelegate().reattachDynamicCounters();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public void didStart() {

    getClient().getDelegate().didStart();
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public AbstractHTTPD newHttpd(final int httpdPort, final ICounterSetAccess accessor)
      throws IOException {

    return getClient().getDelegate().newHttpd(httpdPort, accessor);
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public void serviceJoin(final IService service, final UUID serviceUUID) {

    if (!isOpen()) return;

    if (log.isInfoEnabled()) {

      log.info("service=" + service + ", serviceUUID" + serviceUUID);
    }

    getClient().getDelegate().serviceJoin(service, serviceUUID);
  }

  /** Delegated. {@inheritDoc} */
  @Override
  public void serviceLeave(final UUID serviceUUID) {

    if (!isOpen()) return;

    if (log.isInfoEnabled()) {

      log.info("serviceUUID=" + serviceUUID);
    }

    // @todo really, we should test like this everywhere.
    final AbstractClient<T> thisClient = client.get();

    if (thisClient != null && thisClient.isConnected()) {

      thisClient.getDelegate().serviceLeave(serviceUUID);
    }
  }

  //    /*
  //     * Return <code>true</code> if the service startup preconditions are
  //     * noticably satisified before the timeout elapsed.
  //     *
  //     * @param timeout
  //     * @param unit
  //     *
  //     * @return <code>true</code> if the preconditions are satisified.
  //     *
  //     * @throws InterruptedException
  //     */
  //    protected boolean awaitPreconditions(final long timeout,
  //            final TimeUnit unit) throws InterruptedException {
  //
  //        return client.getDelegate().isServiceReady();
  //
  //    }

  private static String ERR_NO_SERVICE_UUID = "Service UUID is not assigned yet.";

  private static String ERR_SERVICE_NOT_READY = "Service is not ready yet.";

  /*
   * This task starts an (optional) {@link AbstractStatisticsCollector}, an (optional) httpd
   * service, and the (required) {@link ReportTask}.
   *
   * <p>Note: The {@link ReportTask} will relay any collected performance counters to the {@link
   * ILoadBalancerService}, but it also lets the {@link ILoadBalancerService} know which services
   * exist, which is important for some of its functions.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  protected class StartDeferredTasksTask implements Runnable {

    /*
     * Note: The logger is named for this class, but since it is an inner class the name uses a "$"
     * delimiter (vs a ".") between the outer and the inner class names.
     */
    private final Logger log = Logger.getLogger(StartDeferredTasksTask.class);

    /** The timestamp when we started running this task. */
    final long begin = System.currentTimeMillis();

    private StartDeferredTasksTask() {}

    @Override
    public void run() {

      try {

        startDeferredTasks();

      } catch (RejectedExecutionException t) {

        if (isOpen()) {
          /*
           * Only an error if the federation is still open.
           */
          log.error(t, t);
        }

      } catch (Throwable t) {

        log.error(t, t);

        return;
      }
    }

    /*
     * Starts performance counter collection.
     *
     * @throws IOException if {@link IDataService#getServiceUUID()} throws this exception (it never
     *     should since it is a local method call).
     */
    protected void startDeferredTasks() throws IOException {

      try {

        // elapsed time since we started running this task.
        final long elapsed = System.currentTimeMillis() - begin;

        // Wait for the service ID to become available, trying every
        // two seconds, while logging failures.
        while (true) {
          if (getServiceUUID() != null) {
            break;
          }
          if (elapsed > 1000 * 10)
            log.warn(
                ERR_NO_SERVICE_UUID
                    + " : iface="
                    + getServiceIface()
                    + ", name="
                    + getServiceName()
                    + ", elapsed="
                    + elapsed);
          else if (log.isInfoEnabled()) log.info(ERR_NO_SERVICE_UUID);
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
          }
        }

        // Wait for the service to become ready, trying every
        // two seconds, while logging failures.
        while (true) {
          if (isServiceReady()) {
            break;
          }
          if (elapsed > 1000 * 10)
            log.warn(
                ERR_SERVICE_NOT_READY
                    + " : iface="
                    + getServiceIface()
                    + ", name="
                    + getServiceName()
                    + ", elapsed="
                    + elapsed);
          else if (log.isInfoEnabled()) log.info(ERR_SERVICE_NOT_READY + " : " + elapsed);
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
          }
        }

        /*
         * Start collecting performance counters (if enabled).
         *
         * Note: This needs to be done first since the counters from the
         * platform will otherwise not be incorporated into those
         * reported by the federation.
         */
        startPlatformStatisticsCollection();

        /*
         * start collection on various work queues.
         *
         * Note: The data service starts collection for its queues
         * (actually, the concurrency managers queues) once it is up and
         * running.
         *
         * @todo have it collect using the same scheduled thread pool.
         */
        startQueueStatisticsCollection();

        /*
         * Start embedded Ganglia peer. It will develop a snapshot of
         * the cluster metrics in memory and will self-report metrics
         * from the performance counter hierarchy to the ganglia
         * network.
         */
        {
          final Properties properties = getClient().getProperties();

          final boolean listen =
              Boolean.valueOf(
                  properties.getProperty(
                      IEmbergraphClient.Options.GANGLIA_LISTEN,
                      IEmbergraphClient.Options.DEFAULT_GANGLIA_LISTEN));

          final boolean report =
              Boolean.valueOf(
                  properties.getProperty(
                      IEmbergraphClient.Options.GANGLIA_REPORT,
                      IEmbergraphClient.Options.DEFAULT_GANGLIA_REPORT));

          if (listen || report) startGangliaService(statisticsCollector.get());
        }

        // // notify the load balancer of this service join.
        // notifyJoin();

        // start reporting to the load balancer.
        startReportTask();

        // start the local httpd service reporting on this service.
        startHttpdService();

        // notify delegates that deferred startup has occurred.
        AbstractFederation.this.didStart();

      } catch (IllegalStateException ex) {
        /*
         * If the federation was concurrently closed, log @ WARN and
         * return rather than throwing out the exception.
         */
        if (!isOpen()) {
          log.warn("Shutdown: deferred tasks will not start.");
          return;
        }
        throw ex;
      }
    }

    /*
     * Setup sampling on the client's thread pool. This collects interesting statistics about the
     * thread pool for reporting to the load balancer service.
     */
    protected void startQueueStatisticsCollection() {

      if (!getCollectQueueStatistics()) {

        if (log.isInfoEnabled())
          log.info("Queue statistics collection disabled: " + getServiceIface());

        return;
      }

      final long initialDelay = 0; // initial delay in ms.
      final long delay = 1000; // delay in ms.
      final TimeUnit unit = TimeUnit.MILLISECONDS;

      final String relpath = "Thread Pool";

      final ThreadPoolExecutorStatisticsTask threadPoolExecutorStatisticsTask =
          new ThreadPoolExecutorStatisticsTask(relpath, threadPool, taskCounters);

      getServiceCounterSet()
          .makePath(relpath)
          .attach(threadPoolExecutorStatisticsTask.getCounters());

      addScheduledTask(threadPoolExecutorStatisticsTask, initialDelay, delay, unit);
    }

    /** Start collecting performance counters from the OS (if enabled). */
    protected void startPlatformStatisticsCollection() {

      final UUID serviceUUID = getServiceUUID();

      final Properties p = getClient().getProperties();

      if (!getCollectPlatformStatistics()) {

        return;
      }

      p.setProperty(
          AbstractStatisticsCollector.Options.PROCESS_NAME,
          "service"
              + ICounterSet.pathSeparator
              + getServiceIface().getName()
              + ICounterSet.pathSeparator
              + serviceUUID.toString());

      {
        final AbstractStatisticsCollector tmp = AbstractStatisticsCollector.newInstance(p);

        tmp.start();

        statisticsCollector.set(tmp);
      }

      if (log.isInfoEnabled()) log.info("Collecting platform statistics: uuid=" + serviceUUID);
    }

    /*
     * Start embedded Ganglia peer. It will develop a snapshot of the cluster metrics in memory and
     * will self-report metrics from the performance counter hierarchy to the ganglia network.
     *
     * @param statisticsCollector Performance counters will be harvested from here.
     * @see https://sourceforge.net/apps/trac/bigdata/ticket/441 (Ganglia Integration).
     */
    protected void startGangliaService(final AbstractStatisticsCollector statisticsCollector) {

      if (statisticsCollector == null) return;

      try {

        final Properties properties = getClient().getProperties();

        final String hostName = AbstractStatisticsCollector.fullyQualifiedHostName;

        /*
         * Note: This needs to be the value reported by the statistics
         * collector since that it what makes it into the counter set
         * path prefix for this service.
         *
         * TODO This implies that we can not enable the embedded ganglia
         * peer unless platform level statistics collection is enabled.
         * We should be able to separate out the collection of host
         * metrics from whether or not we are collecting metrics from
         * the embergraph service. Do this when moving the host and process
         * (pidstat) collectors into the embergraph-ganglia module.
         *
         * TODO The LBS currently does not collect platform statistics.
         * Once we move the platform statistics collection into the
         * embergraph-ganglia module we can change that. However, the LBS
         * will be going aware entirely once we (a) change over the
         * ganglia state for host reports; and (b)
         */
        final String serviceName = statisticsCollector.getProcessName();

        final InetAddress listenGroup =
            InetAddress.getByName(
                properties.getProperty(
                    IEmbergraphClient.Options.GANGLIA_LISTEN_GROUP,
                    IEmbergraphClient.Options.DEFAULT_GANGLIA_LISTEN_GROUP));

        final int listenPort =
            Integer.valueOf(
                properties.getProperty(
                    IEmbergraphClient.Options.GANGLIA_LISTEN_PORT,
                    IEmbergraphClient.Options.DEFAULT_GANGLIA_LISTEN_PORT));

        final boolean listen =
            Boolean.valueOf(
                properties.getProperty(
                    IEmbergraphClient.Options.GANGLIA_LISTEN,
                    IEmbergraphClient.Options.DEFAULT_GANGLIA_LISTEN));

        final boolean report =
            Boolean.valueOf(
                properties.getProperty(
                    IEmbergraphClient.Options.GANGLIA_REPORT,
                    IEmbergraphClient.Options.DEFAULT_GANGLIA_REPORT));

        // Note: defaults to the listenGroup and port if nothing given.
        final InetSocketAddress[] metricsServers =
            GangliaUtil.parse(
                // server(s)
                properties.getProperty(
                    IEmbergraphClient.Options.GANGLIA_SERVERS,
                    IEmbergraphClient.Options.DEFAULT_GANGLIA_SERVERS),
                // default host (same as listenGroup)
                listenGroup.getHostName(),
                // default port (same as listenGroup)
                listenPort);

        final int quietPeriod = IGangliaDefaults.QUIET_PERIOD;

        final int initialDelay = IGangliaDefaults.INITIAL_DELAY;

        /*
         * Note: Use ZERO (0) if you are running gmond on the same host.
         * That will prevent the GangliaService from transmitting a
         * different heartbeat, which would confuse gmond and gmetad.
         */
        final int heartbeatInterval = 0; // IFF using gmond.
        // final int heartbeatInterval =
        // IGangliaDefaults.HEARTBEAT_INTERVAL;

        // Use the report delay for the interval in which we scan the
        // performance counters.
        final int monitoringInterval =
            (int)
                TimeUnit.MILLISECONDS.toSeconds(
                    Long.parseLong(
                        properties.getProperty(
                            Options.REPORT_DELAY, Options.DEFAULT_REPORT_DELAY)));

        final String defaultUnits = IGangliaDefaults.DEFAULT_UNITS;

        final GangliaSlopeEnum defaultSlope = IGangliaDefaults.DEFAULT_SLOPE;

        final int defaultTMax = IGangliaDefaults.DEFAULT_TMAX;

        final int defaultDMax = IGangliaDefaults.DEFAULT_DMAX;

        // Note: Factory is extensible (application can add its own
        // delegates).
        final GangliaMetadataFactory metadataFactory =
            new GangliaMetadataFactory(
                new DefaultMetadataFactory(defaultUnits, defaultSlope, defaultTMax, defaultDMax));

        /*
         * Layer on the ability to (a) recognize and align host
         * embergraph's performance counters hierarchy with those declared
         * by ganglia and; (b) provide nice declarations for various
         * application counters of interest.
         */
        metadataFactory.add(
            new EmbergraphMetadataFactory(
                hostName, serviceName, defaultSlope, defaultTMax, defaultDMax, heartbeatInterval));

        // The embedded ganglia peer.
        final EmbergraphGangliaService gangliaService =
            new EmbergraphGangliaService(
                hostName,
                serviceName,
                metricsServers,
                listenGroup,
                listenPort,
                listen, // listen
                report, // report
                false, // mock,
                quietPeriod,
                initialDelay,
                heartbeatInterval,
                monitoringInterval,
                defaultDMax, // globalDMax
                metadataFactory);

        // Collect and report host metrics.
        gangliaService.addMetricCollector(new HostMetricsCollector(statisticsCollector));

        // Collect and report QueryEngine metrics.
        gangliaService.addMetricCollector(
            new QueryEngineMetricsCollector(AbstractFederation.this, statisticsCollector));

        /*
         * TODO The problem with reporting per-service statistics is
         * that ganglia lacks a facility to readily aggregate statistics
         * across services on a host (SMS + anything). The only way this
         * can readily be made to work is if each service has a distinct
         * metric for the same value (e.g., Mark and Sweep GC). However,
         * that causes a very large number of distinct metrics. I have
         * commented this out for now while I think it through some
         * more. Maybe we will wind up only reporting the per-host
         * counters to ganglia?
         *
         * Maybe the right way to handle this is to just filter by the
         * service type? Basically, that is what we are doing for the
         * QueryEngine metrics.
         */
        // Collect and report service metrics.
        //                gangliaService.addMetricCollector(new ServiceMetricsCollector(
        //                        statisticsCollector, null/* filter */));

        // Wrap as Future.
        final FutureTask<Void> ft = new FutureTask<Void>(gangliaService, null);

        // Save reference to future.
        gangliaFuture.set(ft);

        // Set the state reference.
        AbstractFederation.this.gangliaService.set(gangliaService);

        // Start the embedded ganglia service.
        getExecutorService().submit(ft);

      } catch (RejectedExecutionException t) {

        /*
         * Ignore.
         *
         * Note: This occurs if the federation shutdown() before we
         * start the embedded ganglia peer. For example, it is common
         * when running a short lived utility service such as
         * ListServices.
         */

      } catch (Throwable t) {

        log.error(t, t);
      }
    }

    /** Start task to report service and counters to the load balancer. */
    protected void startReportTask() {

      final Properties p = getClient().getProperties();

      final long delay =
          Long.parseLong(p.getProperty(Options.REPORT_DELAY, Options.DEFAULT_REPORT_DELAY));

      if (log.isInfoEnabled()) log.info(Options.REPORT_DELAY + "=" + delay);

      if (delay > 0L) {

        final TimeUnit unit = TimeUnit.MILLISECONDS;

        final long initialDelay = delay;

        addScheduledTask(new ReportTask(AbstractFederation.this), initialDelay, delay, unit);

        if (log.isInfoEnabled()) log.info("Started ReportTask.");
      }
    }

    /*
     * Start the local httpd service (if enabled). The service is started on the {@link
     * #getHttpdPort()}, on a randomly assigned port if the port is <code>0</code>, or NOT started
     * if the port is <code>-1</code>. If the service is started, then the URL for the service is
     * reported to the load balancer and also written into the file system. When started, the httpd
     * service will be shutdown with the federation.
     *
     * @throws UnsupportedEncodingException
     */
    protected void startHttpdService() throws UnsupportedEncodingException {

      final String path = getServiceCounterPathPrefix();

      final int httpdPort = getHttpdPort();

      if (httpdPort == -1) {

        if (log.isInfoEnabled()) log.info("httpd disabled: " + path);

        return;
      }

      final AbstractHTTPD httpd;
      try {

        httpd = newHttpd(httpdPort, AbstractFederation.this);

      } catch (IOException e) {

        log.error("Could not start httpd: port=" + httpdPort + ", path=" + path, e);

        return;
      }

      if (httpd != null) {

        // save reference to the daemon.
        AbstractFederation.this.httpd.set(httpd);

        // the URL that may be used to access the local httpd.
        final String s =
            "http://"
                + AbstractStatisticsCollector.fullyQualifiedHostName
                + ":"
                + httpd.getPort()
                + "/?path="
                + URLEncoder.encode(path, "UTF-8");

        httpdURL.set(s);

        if (log.isInfoEnabled()) log.info("start:\n" + s);
      }
    }
  } // class StartDeferredTasks

  /*
   * Periodically report performance counter data to the {@link ILoadBalancerService}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class ReportTask implements Runnable {

    /*
     * Note: The logger is named for this class, but since it is an inner class the name uses a "$"
     * delimiter (vs a ".") between the outer and the inner class names.
     */
    protected final Logger log = Logger.getLogger(ReportTask.class);

    private final AbstractFederation<?> fed;

    public ReportTask(AbstractFederation<?> fed) {

      if (fed == null) throw new IllegalArgumentException();

      this.fed = fed;
    }

    /** Note: Don't throw anything here since we don't want to have the task suppressed! */
    @Override
    public void run() {

      try {

        fed.reattachDynamicCounters();

      } catch (Throwable t) {

        log.error("Could not update performance counter view : " + t, t);
      }

      try {

        /*
         * Report the performance counters to the load balancer.
         */

        reportPerformanceCounters();

      } catch (Throwable t) {

        log.error("Could not report performance counters : " + t, t);
      }
    }

    /*
     * Send performance counters to the load balancer.
     *
     * @throws IOException
     */
    protected void reportPerformanceCounters() throws IOException {

      // Note: This _is_ a local method call.
      final UUID serviceUUID = fed.getServiceUUID();

      // Will be null until assigned by the service registrar.
      if (serviceUUID == null) {

        if (log.isInfoEnabled()) log.info("Service UUID not assigned yet.");

        return;
      }

      final ILoadBalancerService loadBalancerService = fed.getLoadBalancerService();

      if (loadBalancerService == null) {

        log.warn("Could not discover load balancer service.");

        return;
      }

      if (serviceUUID.equals(loadBalancerService.getServiceUUID())) {
        // Do not notify ourselves.
        return;
      }

      /*
       * @todo When sending all counters, this is probably worth
       * compressing as there will be a lot of redundancy and a lot of
       * data.
       */
      final ByteArrayOutputStream baos = new ByteArrayOutputStream(Bytes.kilobyte32 * 2);

      final Properties p = fed.getClient().getProperties();

      final boolean reportAll =
          Boolean.valueOf(p.getProperty(Options.REPORT_ALL, Options.DEFAULT_REPORT_ALL));

      fed.getCounters()
          .asXML(
              baos,
              "UTF-8",
              reportAll ? null /* filter */ : QueryUtil.getRequiredPerformanceCountersFilter());

      if (log.isInfoEnabled())
        log.info(
            "reportAll="
                + reportAll
                + ", service="
                + fed.getServiceName()
                + ", #bytesReported="
                + baos.size());

      loadBalancerService.notify(serviceUUID, baos.toByteArray());

      if (log.isInfoEnabled()) log.info("Notified the load balancer.");
    }
  }

  /** @todo it may be possible to optimize this for the jini case. */
  @Override
  public IDataService[] getDataServices(final UUID[] uuids) {

    final IDataService[] services = new IDataService[uuids.length];

    final IEmbergraphFederation<?> fed = this;

    int i = 0;

    // UUID of the metadata service (if forced to discover it).
    UUID mdsUUID = null;

    for (UUID uuid : uuids) {

      IDataService service = fed.getDataService(uuid);

      if (service == null) {

        if (mdsUUID == null) {

          try {

            mdsUUID = fed.getMetadataService().getServiceUUID();

          } catch (IOException ex) {

            throw new RuntimeException(ex);
          }
        }

        if (uuid == mdsUUID) {

          /*
           * @todo getDataServices(int maxCount) DOES NOT return MDS
           * UUIDs because we don't want people storing application
           * data there, but getDataService(UUID) should probably work
           * for the MDS UUID also since once you have the UUID you
           * want the service.
           */

          service = fed.getMetadataService();
        }
      }

      if (service == null) {

        throw new RuntimeException("Could not discover service: uuid=" + uuid);
      }

      services[i++] = service;
    }

    return services;
  }

  /*
   * Queues up an event to be sent to the {@link ILoadBalancerService}. Events are maintained on a
   * non-blocking queue (no fixed capacity) and sent by a scheduled task.
   *
   * @param e
   * @see SendEventsTask
   */
  protected void sendEvent(final Event e) {

    if (isOpen()) {

      events.add(e);
    }
  }

  /** Queue of events sent periodically to the {@link ILoadBalancerService}. */
  private final BlockingQueue<Event> events = new LinkedBlockingQueue<Event>();

  /*
   * Sends events to the {@link ILoadBalancerService}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   *     <p>FIXME should discard events if too many build up on the client.
   */
  private class SendEventsTask implements Runnable {

    public SendEventsTask() {}

    /** Note: Don't throw anything - it will cancel the scheduled task. */
    @Override
    public void run() {

      try {

        final ILoadBalancerService lbs = getLoadBalancerService();

        if (lbs == null) {

          // Can't drain events
          return;
        }

        final long begin = System.currentTimeMillis();

        final LinkedList<Event> c = new LinkedList<Event>();

        events.drainTo(c);

        /*
         * @todo since there is a delay before events are sent along it
         * is quite common that the end() event will have been generated
         * such that the event is complete before we send it along.
         * there should be an easy way to notice this and avoid sending
         * an event twice when we can get away with just sending it
         * once. however the decision must be atomic with respect to the
         * state change in the event so that we do not lose any data by
         * concluding that we have handled the event when in fact its
         * state was not complete before we sent it along.
         */

        for (Event e : c) {

          // avoid modification when sending the event.
          synchronized (e) {
            lbs.notifyEvent(e);
          }
        }

        if (log.isInfoEnabled()) {

          final int nevents = c.size();

          if (nevents > 0)
            log.info(
                "Sent " + c.size() + " events in " + (System.currentTimeMillis() - begin) + "ms");
        }

      } catch (Throwable t) {

        log.warn(getServiceName(), t);
      }
    }
  }
}
