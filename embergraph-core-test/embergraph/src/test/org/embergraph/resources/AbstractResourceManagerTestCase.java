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
 * Created on Feb 22, 2008
 */

package org.embergraph.resources;

import cutthecrap.utils.striterators.IFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.embergraph.bfs.EmbergraphFileSystem;
import org.embergraph.bop.engine.IQueryPeer;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.ResultSet;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.proc.IIndexProcedure;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.journal.AbstractLocalTransactionManager;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ConcurrencyManager;
import org.embergraph.journal.IResourceLockService;
import org.embergraph.journal.ITransactionService;
import org.embergraph.journal.RegisterIndexTask;
import org.embergraph.journal.TemporaryStore;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.IndexPartitionCause;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.rawstore.IBlock;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.relation.locator.IResourceLocator;
import org.embergraph.resources.ResourceManager.Options;
import org.embergraph.service.AbstractTransactionService;
import org.embergraph.service.DataService;
import org.embergraph.service.IDataService;
import org.embergraph.service.IEmbergraphClient;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.service.ILoadBalancerService;
import org.embergraph.service.IMetadataService;
import org.embergraph.service.IService;
import org.embergraph.service.Session;
import org.embergraph.service.ndx.IClientIndex;
import org.embergraph.sparse.SparseRowStore;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.httpd.AbstractHTTPD;

/*
 * Base class for {@link ResourceManager} test suites that can use normal startup and shutdown.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractResourceManagerTestCase extends AbstractResourceManagerBootstrapTestCase {

  /** */
  public AbstractResourceManagerTestCase() {
    super();
  }

  /** @param arg0 */
  public AbstractResourceManagerTestCase(String arg0) {
    super(arg0);
  }

  /** Forces the use of persistent journals so that we can do overflow operations and the like. */
  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    // Note: test requires data on disk.
    properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

    // Disable index copy - overflow will always cause an index segment build.
    properties.setProperty(Options.COPY_INDEX_THRESHOLD, "0");

    return properties;
  }

  protected IMetadataService metadataService;
  protected ResourceManager resourceManager;
  protected ConcurrencyManager concurrencyManager;
  private AbstractTransactionService txService;
  protected AbstractLocalTransactionManager localTransactionManager;
  private ExecutorService executorService;
  private IEmbergraphFederation fed;

  /** Setup test fixtures. */
  @Override
  protected void setUp() throws Exception {

    super.setUp();

    metadataService = new MockMetadataService();

    final Properties properties = getProperties();

    resourceManager =
        new ResourceManager(properties) {

          private final UUID dataServiceUUID = UUID.randomUUID();

          //            @Override
          public IEmbergraphFederation getFederation() {

            return fed;
          }

          //            @Override
          public DataService getDataService() {

            throw new UnsupportedOperationException();
          }

          //            @Override
          public UUID getDataServiceUUID() {

            return dataServiceUUID;
          }
        };

    txService =
        new MockTransactionService(properties) {

          @Override
          protected void setReleaseTime(long releaseTime) {

            super.setReleaseTime(releaseTime);

            if (log.isInfoEnabled())
              log.info(
                  "Propagating new release time to the resourceManager: releaseTime="
                      + releaseTime
                      + ", releaseAge="
                      + getMinReleaseAge());

            // propagate the new release time to the resource manager.
            resourceManager.setReleaseTime(releaseTime);
          }
        }.start();

    localTransactionManager = new MockLocalTransactionManager(txService);

    concurrencyManager =
        new ConcurrencyManager(properties, localTransactionManager, resourceManager);

    resourceManager.setConcurrencyManager(concurrencyManager);

    assertTrue(resourceManager.awaitRunning());

    executorService = Executors.newCachedThreadPool(DaemonThreadFactory.defaultThreadFactory());

    fed = new MockFederation();
  }

  @Override
  protected void tearDown() throws Exception {

    if (executorService != null) executorService.shutdownNow();

    if (fed != null) fed.destroy();

    if (metadataService != null) metadataService.destroy();

    if (resourceManager != null) resourceManager.shutdownNow();

    if (concurrencyManager != null) concurrencyManager.shutdownNow();

    if (localTransactionManager != null) localTransactionManager.shutdownNow();

    if (txService != null) {
      txService.destroy();
    }

    super.tearDown();
  }

  /*
   * A minimal implementation of {@link IMetadataService} - only those methods actually used by the
   * {@link ResourceManager} are implemented. This avoids conflicts with the {@link ResourceManager}
   * instance whose behavior we are trying to test.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  protected static class MockMetadataService implements IMetadataService {

    private AtomicInteger partitionId = new AtomicInteger(0);

    private final Session session = new Session();

    public int nextPartitionId(String name) {
      return partitionId.incrementAndGet();
    }

    public UUID registerScaleOutIndex(
        IndexMetadata metadata, byte[][] separatorKeys, UUID[] dataServices) {
      throw new UnsupportedOperationException();
    }

    public void dropScaleOutIndex(String name) {
      throw new UnsupportedOperationException();
    }

    public UUID getServiceUUID() {
      throw new UnsupportedOperationException();
    }

    public String getStatistics() {
      throw new UnsupportedOperationException();
    }

    public void registerIndex(String name, IndexMetadata metadata) {
      throw new UnsupportedOperationException();
    }

    public IndexMetadata getIndexMetadata(String name, long timestamp) {
      throw new UnsupportedOperationException();
    }

    public void dropIndex(String name) {
      throw new UnsupportedOperationException();
    }

    public ResultSet rangeIterator(
        long tx, String name, byte[] fromKey, byte[] toKey, int capacity, int flags, IFilter filter) {
      throw new UnsupportedOperationException();
    }

    public Future submit(long tx, String name, IIndexProcedure proc) {
      throw new UnsupportedOperationException();
    }

    public IBlock readBlock(IResourceMetadata resource, long addr) {
      throw new UnsupportedOperationException();
    }

    public void splitIndexPartition(
        String name, PartitionLocator oldLocator, PartitionLocator[] newLocators) {

      log.info(
          "Split index partition: name="
              + name
              + ", oldLocator="
              + oldLocator
              + " into "
              + Arrays.toString(newLocators));
    }

    public void joinIndexPartition(
        String name, PartitionLocator[] oldLocators, PartitionLocator newLocator) {

      log.info(
          "Join index partitions: name="
              + name
              + ", oldLocators="
              + Arrays.toString(oldLocators)
              + " into "
              + newLocator);
    }

    public void moveIndexPartition(
        String name, PartitionLocator oldLocator, PartitionLocator newLocator) {

      log.info(
          "Move index partition: name="
              + name
              + ", oldLocator="
              + oldLocator
              + " to "
              + newLocator);
    }

    public PartitionLocator get(String name, long timestamp, byte[] key) {

      return null;
    }

    public PartitionLocator find(String name, long timestamp, byte[] key) {

      return null;
    }

    public void forceOverflow(boolean immediate, boolean compactingMerge) {

      throw new UnsupportedOperationException();
    }

    public boolean isOverflowActive() {

      throw new UnsupportedOperationException();
    }

    public long getAsynchronousOverflowCounter() {

      throw new UnsupportedOperationException();
    }

    public void destroy() {}

    public Future<?> submit(Callable<?> proc) {

      return null;
    }

    public String getHostname() {

      return null;
    }

    public Class getServiceIface() {

      return null;
    }

    public String getServiceName() {

      return null;
    }

    public boolean purgeOldResources(long timeout, boolean truncateJournal) {
      // TODO Auto-generated method stub
      return false;
    }

    public void setReleaseTime(long releaseTime) {
      // TODO Auto-generated method stub

    }

    public void abort(long tx) {

      throw new UnsupportedOperationException();
    }

    public long singlePhaseCommit(long tx) {

      throw new UnsupportedOperationException();
    }

    public void prepare(long tx, long revisionTime) {

      throw new UnsupportedOperationException();
    }

    public Session getSession() {
      return session;
    }

    public IQueryPeer getQueryEngine() {
      return null;
    }
  }

  /*
   * A minimal implementation of only those methods actually utilized by the {@link ResourceManager}
   * during the unit tests.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  protected class MockFederation implements IEmbergraphFederation<MockMetadataService> {

    private final MockMetadataService metadataService = new MockMetadataService();

    public MockMetadataService getService() {

      return metadataService;
    }

    public void destroy() {}

    public void dropIndex(String name) {}

    public IDataService getAnyDataService() {

      return null;
    }

    public IEmbergraphClient getClient() {

      return null;
    }

    public String getServiceCounterPathPrefix() {

      return null;
    }

    public CounterSet getCounters() {

      return null;
    }

    public IDataService getDataService(UUID serviceUUID) {

      return null;
    }

    public UUID[] getDataServiceUUIDs(int maxCount) {

      return null;
    }

    public ExecutorService getExecutorService() {

      return executorService;
    }

    public SparseRowStore getGlobalRowStore() {

      return null;
    }

    public SparseRowStore getGlobalRowStore(final long timestamp) {

      return null;
    }

    public IClientIndex getIndex(String name, long timestamp) {

      return null;
    }

    public IKeyBuilder getKeyBuilder() {

      return null;
    }

    public ILoadBalancerService getLoadBalancerService() {

      return null;
    }

    public IMetadataIndex getMetadataIndex(String name, long timestamp) {

      return null;
    }

    public IMetadataService getMetadataService() {

      return metadataService;
    }

    public ITransactionService getTransactionService() {

      return txService;
    }

    public boolean isDistributed() {

      return false;
    }

    public boolean isScaleOut() {

      return false;
    }

    public boolean isStable() {

      return false;
    }

    @Override
    public boolean isGroupCommit() {
      return false;
    }

    public long getLastCommitTime() {

      return 0;
    }

    public void registerIndex(IndexMetadata metadata) {}

    public UUID registerIndex(IndexMetadata metadata, UUID dataServiceUUID) {

      return null;
    }

    public UUID registerIndex(
        IndexMetadata metadata, byte[][] separatorKeys, UUID[] dataServiceUUIDs) {

      return null;
    }

    public IResourceLocator getResourceLocator() {

      return null;
    }

    public IResourceLockService getResourceLockService() {

      return null;
    }

    public EmbergraphFileSystem getGlobalFileSystem() {

      return null;
    }

    public TemporaryStore getTempStore() {

      return null;
    }

    public String getHttpdURL() {

      return null;
    }

    public CounterSet getServiceCounterSet() {

      return null;
    }

    public IDataService getDataServiceByName(String name) {
      // TODO Auto-generated method stub
      return null;
    }

    public IDataService[] getDataServices(UUID[] uuid) {
      // TODO Auto-generated method stub
      return null;
    }

    public void didStart() {}

    public Class getServiceIface() {
      return getClass();
    }

    public String getServiceName() {
      return getClass().getName();
    }

    public UUID getServiceUUID() {
      return serviceUUID;
    }

    private final UUID serviceUUID = UUID.randomUUID();

    public boolean isServiceReady() {
      return true;
    }

    public AbstractHTTPD newHttpd(int httpdPort, ICounterSetAccess access) {
      return null;
    }

    public void reattachDynamicCounters() {}

    public void serviceJoin(IService service, UUID serviceUUID) {}

    public void serviceLeave(UUID serviceUUID) {}

    public CounterSet getHostCounterSet() {
      return null;
    }

    public ScheduledFuture<?> addScheduledTask(
        Runnable task, long initialDelay, long delay, TimeUnit unit) {
      return null;
    }

    public boolean getCollectPlatformStatistics() {
      return false;
    }

    public boolean getCollectQueueStatistics() {
      return false;
    }

    public int getHttpdPort() {
      return 0;
    }

    @Override
    public Iterator<String> indexNameScan(String prefix, long timestamp) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isJiniFederation() {
      // BLZG-1370
      return false;
    }
  }

  /*
   * Utility method to register an index partition on the {@link #resourceManager}.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  protected void registerIndex(final String name) throws InterruptedException, ExecutionException {

    final IndexMetadata indexMetadata = new IndexMetadata(name, UUID.randomUUID());
    {

      // must support delete markers
      indexMetadata.setDeleteMarkers(true);

      // must be an index partition.
      indexMetadata.setPartitionMetadata(
          new LocalPartitionMetadata(
              0, // partitionId
              -1, // not a move.
              new byte[] {}, // leftSeparator
              null, // rightSeparator
              new IResourceMetadata[] {
                resourceManager.getLiveJournal().getResourceMetadata(),
              },
              IndexPartitionCause.register(resourceManager)
              //                    ,"" // history
              ));

      // submit task to register the index and wait for it to complete.
      concurrencyManager
          .submit(new RegisterIndexTask(concurrencyManager, name, indexMetadata))
          .get();
    }
  }

  /*
   * Test helper.
   *
   * @param expected
   * @param actual
   */
  protected void assertSameResources(final IRawStore[] expected, final Set<UUID> actual) {

    if (log.isInfoEnabled()) {

      log.info("\nexpected=" + Arrays.toString(expected) + "\nactual=" + actual);
    }

    // copy to avoid side-effects.
    final Set<UUID> tmp = new HashSet<>(actual);

    for (IRawStore iRawStore : expected) {

      final UUID uuid = iRawStore.getResourceMetadata().getUUID();

      assertFalse(tmp.isEmpty());

      if (!tmp.remove(uuid)) {

        fail("Expecting " + iRawStore.getResourceMetadata());
      }
    }

    assertTrue(tmp.isEmpty());
  }
}
