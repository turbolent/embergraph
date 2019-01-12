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
 * Created on Mar 24, 2008
 */

package org.embergraph.service;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Logger;
import org.embergraph.Banner;

/**
 * Abstract base class for {@link IEmbergraphClient} implementations.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractClient<T> implements IEmbergraphClient<T> {

  protected static final Logger log = Logger.getLogger(IEmbergraphClient.class);

  /** The properties specified to the ctor. */
  private final Properties properties;

  public Properties getProperties() {

    return new Properties(properties);
  }

  private final int defaultRangeQueryCapacity;
  private final boolean batchApiOnly;
  private final int threadPoolSize;
  private final int maxStaleLocatorRetries;
  private final int maxParallelTasksPerRequest;
  private final long taskTimeout;
  private final int locatorCacheCapacity;
  private final long locatorCacheTimeout;
  private final int indexCacheCapacity;
  private final long indexCacheTimeout;
  private final boolean readConsistent;
  //    private final long tempStoreMaxExtent;

  /*
   * IEmbergraphClient API.
   */

  public int getThreadPoolSize() {

    return threadPoolSize;
  }

  public int getDefaultRangeQueryCapacity() {

    return defaultRangeQueryCapacity;
  }

  public boolean getBatchApiOnly() {

    return batchApiOnly;
  }

  public int getMaxStaleLocatorRetries() {

    return maxStaleLocatorRetries;
  }

  public int getMaxParallelTasksPerRequest() {

    return maxParallelTasksPerRequest;
  }

  public boolean isReadConsistent() {

    return readConsistent;
  }

  public long getTaskTimeout() {

    return taskTimeout;
  }

  public int getLocatorCacheCapacity() {

    return locatorCacheCapacity;
  }

  public long getLocatorCacheTimeout() {

    return locatorCacheTimeout;
  }

  public int getIndexCacheCapacity() {

    return indexCacheCapacity;
  }

  public long getIndexCacheTimeout() {

    return indexCacheTimeout;
  }

  //    public long getTempStoreMaxExtent() {
  //
  //        return tempStoreMaxExtent;
  //
  //    }

  /** @param properties See {@link IEmbergraphClient.Options} */
  protected AbstractClient(final Properties properties) {

    // show the copyright banner during statup.
    Banner.banner();

    if (properties == null) throw new IllegalArgumentException();

    this.properties = properties;

    // client thread pool setup.
    {
      threadPoolSize =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_THREAD_POOL_SIZE, Options.DEFAULT_CLIENT_THREAD_POOL_SIZE));

      if (log.isInfoEnabled()) log.info(Options.CLIENT_THREAD_POOL_SIZE + "=" + threadPoolSize);
    }

    // maxStaleLocatorRetries
    {
      maxStaleLocatorRetries =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_MAX_STALE_LOCATOR_RETRIES,
                  Options.DEFAULT_CLIENT_MAX_STALE_LOCATOR_RETRIES));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_MAX_STALE_LOCATOR_RETRIES + "=" + maxStaleLocatorRetries);

      if (maxStaleLocatorRetries < 0) {

        throw new RuntimeException(
            Options.CLIENT_MAX_STALE_LOCATOR_RETRIES + " must be non-negative");
      }
    }

    // readConsistent
    {
      readConsistent =
          Boolean.valueOf(
              properties.getProperty(
                  Options.CLIENT_READ_CONSISTENT, Options.DEFAULT_CLIENT_READ_CONSISTENT));

      if (log.isInfoEnabled()) log.info(Options.CLIENT_READ_CONSISTENT + "=" + readConsistent);
    }

    // maxParallelTasksPerRequest
    {
      maxParallelTasksPerRequest =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_MAX_PARALLEL_TASKS_PER_REQUEST,
                  Options.DEFAULT_CLIENT_MAX_PARALLEL_TASKS_PER_REQUEST));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_MAX_PARALLEL_TASKS_PER_REQUEST + "=" + maxParallelTasksPerRequest);

      if (maxParallelTasksPerRequest <= 0) {

        throw new RuntimeException(
            Options.CLIENT_MAX_PARALLEL_TASKS_PER_REQUEST + " must be positive");
      }
    }

    // task timeout
    {
      taskTimeout =
          Long.parseLong(
              properties.getProperty(
                  Options.CLIENT_TASK_TIMEOUT, Options.DEFAULT_CLIENT_TASK_TIMEOUT));

      if (log.isInfoEnabled()) log.info(Options.CLIENT_TASK_TIMEOUT + "=" + taskTimeout);
    }

    // defaultRangeQueryCapacity
    {
      defaultRangeQueryCapacity =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_RANGE_QUERY_CAPACITY,
                  Options.DEFAULT_CLIENT_RANGE_QUERY_CAPACITY));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_RANGE_QUERY_CAPACITY + "=" + defaultRangeQueryCapacity);
    }

    // batchApiOnly
    {
      batchApiOnly =
          Boolean.valueOf(
              properties.getProperty(
                  Options.CLIENT_BATCH_API_ONLY, Options.DEFAULT_CLIENT_BATCH_API_ONLY));

      if (log.isInfoEnabled()) log.info(Options.CLIENT_BATCH_API_ONLY + "=" + batchApiOnly);
    }

    // locator cache
    {
      locatorCacheCapacity =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_LOCATOR_CACHE_CAPACITY,
                  Options.DEFAULT_CLIENT_LOCATOR_CACHE_CAPACITY));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_LOCATOR_CACHE_CAPACITY + "=" + locatorCacheCapacity);

      locatorCacheTimeout =
          Long.parseLong(
              properties.getProperty(
                  Options.CLIENT_LOCATOR_CACHE_TIMEOUT,
                  Options.DEFAULT_CLIENT_LOCATOR_CACHE_TIMEOUT));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_LOCATOR_CACHE_TIMEOUT + "=" + locatorCacheTimeout);
    }

    // indexCacheCapacity
    {
      indexCacheCapacity =
          Integer.parseInt(
              properties.getProperty(
                  Options.CLIENT_INDEX_CACHE_CAPACITY,
                  Options.DEFAULT_CLIENT_INDEX_CACHE_CAPACITY));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_INDEX_CACHE_CAPACITY + "=" + indexCacheCapacity);

      if (indexCacheCapacity <= 0)
        throw new RuntimeException(Options.CLIENT_INDEX_CACHE_CAPACITY + " must be positive");
    }

    // index cache timeout
    {
      indexCacheTimeout =
          Long.parseLong(
              properties.getProperty(
                  Options.CLIENT_INDEX_CACHE_TIMEOUT, Options.DEFAULT_CLIENT_INDEX_CACHE_TIMEOUT));

      if (log.isInfoEnabled())
        log.info(Options.CLIENT_INDEX_CACHE_TIMEOUT + "=" + indexCacheTimeout);

      if (indexCacheTimeout < 0)
        throw new RuntimeException(Options.CLIENT_INDEX_CACHE_TIMEOUT + " must be non-negative");
    }

    //        // tempStoreMaxExtent
    //        {
    //
    //            tempStoreMaxExtent = Long.parseLong(properties.getProperty(
    //                    Options.TEMP_STORE_MAXIMUM_EXTENT,
    //                    Options.DEFAULT_TEMP_STORE_MAXIMUM_EXTENT));
    //
    //            if (log.isInfoEnabled())
    //                log.info(Options.TEMP_STORE_MAXIMUM_EXTENT + "="
    //                        + tempStoreMaxExtent);
    //
    //            if (tempStoreMaxExtent < 0)
    //                throw new RuntimeException(Options.TEMP_STORE_MAXIMUM_EXTENT
    //                        + " must be non-negative");
    //
    //        }

  }

  private final AtomicReference<IFederationDelegate<T>> delegate =
      new AtomicReference<IFederationDelegate<T>>();

  /** The delegate for the federation. */
  public final IFederationDelegate<T> getDelegate() {

    return delegate.get();
  }

  /**
   * Set the delegate for the federation.
   *
   * @param delegate The delegate.
   * @throws IllegalArgumentException if the argument is <code>null</code>.
   * @throws IllegalStateException if the property has already been set to a different value.
   * @throws IllegalStateException if the client is already connected.
   */
  public final
  //    synchronized
  void setDelegate(final IFederationDelegate<T> delegate) {

    if (delegate == null) {

      throw new IllegalArgumentException();
    }

    if (isConnected()) {

      throw new IllegalStateException();
    }

    // Try to set, expecting current value is [null]
    if (!this.delegate.compareAndSet(null /* expect */, delegate /* update */)) {

      // Try to set, expecting current value is [delegate].
      if (!this.delegate.compareAndSet(delegate /* expect */, delegate /* update */)) {

        // Current value is not [null] and is not [delegate].
        throw new IllegalStateException();
      }
    }

    //        if (this.delegate.get() != null && this.delegate.get() != delegate) {
    //
    //            throw new IllegalStateException();
    //
    //        }
    //
    //        this.delegate.set(delegate);

  }

  /**
   * Extended to {@link IEmbergraphClient#disconnect(boolean)} if the client is still connected when
   * it is finalized.
   */
  protected void finalize() throws Throwable {

    if (isConnected()) {

      log.warn("Disconnecting client or service");

      disconnect(true /* immediateShutdown */);
    }

    super.finalize();
  }
}
