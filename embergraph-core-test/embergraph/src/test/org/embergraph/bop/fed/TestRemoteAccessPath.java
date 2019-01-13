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
 * Created on Sep 24, 2010
 */

package org.embergraph.bop.fed;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.E;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.ap.Predicate.Annotations;
import org.embergraph.bop.ap.R;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;
import org.embergraph.service.AbstractEmbeddedFederationTestCase;
import org.embergraph.service.DataService;
import org.embergraph.service.EmbeddedClient;
import org.embergraph.service.ndx.IClientIndex;
import org.embergraph.striterator.ChunkedArrayIterator;
import org.embergraph.striterator.IChunkedOrderedIterator;

/*
 * Unit tests of a remote access path.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo test read-committed access paths.
 * @todo test read historical access paths (read-only tx).
 * @todo test unisolated (writable) access paths.
 * @todo test fully isolated access paths.
 */
public class TestRemoteAccessPath extends AbstractEmbeddedFederationTestCase {

  /** */
  public TestRemoteAccessPath() {}

  /** @param name */
  public TestRemoteAccessPath(String name) {
    super(name);
  }

  // Namespace for the relation.
  private static final String namespace = TestRemoteAccessPath.class.getName();

  // The separator key between the index partitions.
  private byte[] separatorKey;

  /** The local persistence store for the {@link #queryEngine}. */
  private Journal queryEngineStore;

  /** The query controller. */
  private FederatedQueryEngine queryEngine;

  /** The timestamp or transaction identifier used for the test. */
  private long tx = ITx.READ_COMMITTED;

  public Properties getProperties() {

    final Properties properties = new Properties(super.getProperties());

    /*
     * Restrict to a single data service.
     */
    properties.setProperty(EmbeddedClient.Options.NDATA_SERVICES, "1");

    return properties;
  }

  public void setUp() throws Exception {

    super.setUp();

    assertNotNull(dataService0);
    assertNull(dataService1);

    //        final IEmbergraphFederation<?> fed = client.connect();

    // create index manager for the query controller.
    {
      final Properties p = new Properties();
      p.setProperty(Journal.Options.BUFFER_MODE, BufferMode.Transient.toString());
      queryEngineStore = new Journal(p);
    }

    //        dataService0 = fed.getDataService(dataServices[0]);
    //        dataService1 = fed.getDataService(dataServices[1]);
    {

      // @todo need to wait for the dataService to be running.
      assertTrue(((DataService) dataService0).getResourceManager().awaitRunning());

      // resolve the query engine on one of the data services.
      while (dataService0.getQueryEngine() == null) {

        if (log.isInfoEnabled()) log.info("Waiting for query engine on dataService0");

        Thread.sleep(250);
      }

      if (log.isInfoEnabled()) log.info("queryPeer : " + dataService0.getQueryEngine());
    }

    //        // resolve the query engine on the other data services.
    //        {
    //
    //            IQueryPeer other = null;
    //
    ////            assertTrue(((DataService) dataServer.getProxy())
    ////                    .getResourceManager().awaitRunning());
    //
    //            while ((other = dataService1.getQueryEngine()) == null) {
    //
    //                if (log.isInfoEnabled())
    //                    log.info("Waiting for query engine on dataService1");
    //
    //                Thread.sleep(250);
    //
    //            }
    //
    //            System.err.println("other     : " + other);
    //
    //        }

    loadData();

    /*
     * Optionally obtain a read-only transaction from the some commit point
     * on the db.
     */
    //        tx = fed.getTransactionService().newTx(ITx.READ_COMMITTED);

  }

  public void tearDown() throws Exception {

    // clear reference.
    separatorKey = null;

    if (queryEngineStore != null) {
      queryEngineStore.destroy();
      queryEngineStore = null;
    }
    if (queryEngine != null) {
      queryEngine.shutdownNow();
      queryEngine = null;
    }

    if (tx != ITx.READ_COMMITTED && tx != ITx.UNISOLATED) {
      // Some kind of transaction.
      fed.getTransactionService().abort(tx);
    }

    super.tearDown();
  }

  /*
   * Create and populate relation in the {@link #namespace}.
   *
   * @throws IOException
   */
  private void loadData() throws IOException {

    /*
     * The data to insert (in key order).
     */
    final E[] a = {
      // partition0
      new E("John", "Mary"), //
      new E("Leon", "Paul"), //
      // partition1
      new E("Mary", "John"), //
      new E("Mary", "Paul"), //
      new E("Paul", "Leon"), //
    };

    // The separator key between the two index partitions.
    separatorKey = KeyBuilder.newUnicodeInstance().append("Mary").getKey();

    final byte[][] separatorKeys = new byte[][] {new byte[] {}, separatorKey};

    // two partitions on the same data service.
    final UUID[] dataServices =
        new UUID[] {
          dataService0.getServiceUUID(), dataService0.getServiceUUID(),
        };

    /*
     * Create the relation with the primary index key-range partitioned
     * using the given separator keys and data services.
     */

    final R rel = new R(client.getFederation(), namespace, ITx.UNISOLATED, new Properties());

    if (client.getFederation().getResourceLocator().locate(namespace, ITx.UNISOLATED) == null) {

      rel.create(separatorKeys, dataServices);

      /*
       * Insert data into the appropriate index partitions.
       */
      rel.insert(new ChunkedArrayIterator<>(a.length, a, null /* keyOrder */));
    }
  }

  /*
   * Return an {@link IAsynchronousIterator} that will read a single, empty {@link IBindingSet}.
   *
   * @param bindingSet the binding set.
   */
  protected ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
      final IBindingSet bindingSet) {

    return new ThickAsynchronousIterator<>(
        new IBindingSet[][]{new IBindingSet[]{bindingSet}});
  }

  public void test_remoteAccessPath_readsOnBothPartitions() {

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{Var.var("name"), Var.var("value")},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(org.embergraph.bop.IPredicate.Annotations.TIMESTAMP, tx),
                new NV(Annotations.REMOTE_ACCESS_PATH, true),
                // Note: turns off shard-wise parallelism!
                new NV(Annotations.FLAGS, IRangeQuery.DEFAULT)));

    final E[] expected =
        new E[] {
          // partition0
          new E("John", "Mary"), //
          new E("Leon", "Paul"), //
          // partition1
          new E("Mary", "John"), //
          new E("Mary", "Paul"), //
          new E("Paul", "Leon"), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[][] {new IBindingSet[0]});

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(fed, queryEngineStore /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // verify that this is a scale-out view of the index.
    assertTrue(ap.getIndex() instanceof IClientIndex);

    // obtain range count from the access path.
    assertEquals(5L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  public void test_remoteAccessPath_readsOnPartition0() {

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("John"), Var.var("value")},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(org.embergraph.bop.IPredicate.Annotations.TIMESTAMP, tx),
                new NV(Annotations.REMOTE_ACCESS_PATH, true)));

    final E[] expected =
        new E[] {
          new E("John", "Mary"), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[][] {new IBindingSet[0]});

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(fed, queryEngineStore /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // verify that this is a scale-out view of the index.
    assertTrue(ap.getIndex() instanceof IClientIndex);

    // obtain range count from the access path.
    assertEquals(1L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  public void test_remoteAccessPath_readsOnPartition1() {

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), Var.var("value")},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(org.embergraph.bop.IPredicate.Annotations.TIMESTAMP, tx),
                new NV(Annotations.REMOTE_ACCESS_PATH, true)));

    final E[] expected =
        new E[] {
          new E("Mary", "John"), //
          new E("Mary", "Paul"), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[][] {new IBindingSet[0]});

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(fed, queryEngineStore /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // verify that this is a scale-out view of the index.
    assertTrue(ap.getIndex() instanceof IClientIndex);

    // obtain range count from the access path.
    assertEquals(2L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /*
   * Return an {@link IAsynchronousIterator} that will read a single, chunk containing all of the
   * specified {@link IBindingSet}s.
   *
   * @param bindingSetChunks the chunks of binding sets.
   */
  private static ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
      final IBindingSet[][] bindingSetChunks) {

    return new ThickAsynchronousIterator<>(bindingSetChunks);
  }

  protected class MockPipelineOp extends PipelineOp {

    public MockPipelineOp(final BOp[] args, final NV... anns) {

      super(args, NV.asMap(anns));
    }

    private static final long serialVersionUID = 1L;

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
      throw new UnsupportedOperationException();
    }
  }
}
