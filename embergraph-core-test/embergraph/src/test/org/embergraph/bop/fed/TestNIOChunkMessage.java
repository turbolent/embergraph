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
 * Created on Sep 11, 2010
 */

package org.embergraph.bop.fed;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import junit.framework.TestCase2;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.HashBindingSet;
import org.embergraph.bop.engine.IChunkMessage;
import org.embergraph.bop.engine.IHaltOpMessage;
import org.embergraph.bop.engine.IQueryClient;
import org.embergraph.bop.engine.IQueryDecl;
import org.embergraph.bop.engine.IStartOpMessage;
import org.embergraph.io.DirectBufferPoolAllocator.IAllocationContext;
import org.embergraph.service.ManagedResourceService;
import org.embergraph.striterator.Dechunkerator;
import org.embergraph.util.config.NicUtil;

/*
 * Unit tests for {@link NIOChunkMessage}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @todo test with large source chunks which span more than one direct buffer.
 * @todo concurrency test.
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/160">ResourceService should use
 *     NIO for file and buffer transfers</a>
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/486">Support NIO solution set
 *     interchange on the cluster</a>
 */
public class TestNIOChunkMessage extends TestCase2 {

  /** */
  public TestNIOChunkMessage() {}

  /** @param name */
  public TestNIOChunkMessage(String name) {
    super(name);
  }

  private ManagedResourceService resourceService;

  @Override
  public void setUp() throws Exception {

    resourceService =
        new ManagedResourceService(
            new InetSocketAddress(
                InetAddress.getByName(
                    NicUtil.getIpAddress("default.nic", "default", true /* loopbackOk */)),
                0 /* port */),
            0 /* requestServicePoolSize */) {

          @Override
          protected File getResource(UUID uuid) {
            throw new UnsupportedOperationException();
          }
        };
  }

  @Override
  public void tearDown() {

    if (resourceService != null) {
      resourceService.shutdownNow();
      resourceService = null;
    }
  }

  /** Unit test for a message with a single chunk of binding sets. */
  public void test_oneChunk() {

    final Var<?> x = Var.var("x");
    final Var<?> y = Var.var("y");

    final List<IBindingSet> data = new LinkedList<>();
    {
      IBindingSet bset = null;
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("John"));
        bset.set(y, new Constant<>("Mary"));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("Mary"));
        bset.set(y, new Constant<>("Paul"));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("Mary"));
        bset.set(y, new Constant<>("Jane"));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("Paul"));
        bset.set(y, new Constant<>("Leon"));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("Paul"));
        bset.set(y, new Constant<>("John"));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<>("Leon"));
        bset.set(y, new Constant<>("Paul"));
        data.add(bset);
      }
    }

    final IQueryClient queryController = new MockQueryController();
    final UUID queryId = UUID.randomUUID();
    final int bopId = 1;
    final int partitionId = 2;
    //        final IBlockingBuffer<IBindingSet[]> source = new BlockingBuffer<IBindingSet[]>(
    //                10);
    //
    //        // populate the source.
    //        source.add(data.toArray(new IBindingSet[0]));
    //
    //        // close the source.
    //        source.close();
    final IBindingSet[] source = data.toArray(new IBindingSet[0]);

    /*
     * Note: The allocations made when the data are received are
     */
    final IAllocationContext allocationContext =
        resourceService.getAllocator().getAllocationContext(getName());

    try {

      // build the chunk.
      final MyNIOChunkMessage<IBindingSet> msg =
          new MyNIOChunkMessage<>(
              queryController,
              queryId,
              bopId,
              partitionId,
              allocationContext,
              source,
              resourceService.getAddr());

      try {

        assertTrue(queryController == msg.getQueryController());

        assertEquals(queryId, msg.getQueryId());

        assertEquals(bopId, msg.getBOpId());

        assertEquals(partitionId, msg.getPartitionId());

        // the data is not inline with the message.
        assertFalse(msg.isMaterialized());

        /*
         * Directly invoke the core materialize method on the message
         * impl.
         */
        msg.materialize(resourceService, allocationContext);

        // the data was materialized.
        assertTrue(msg.isMaterialized());

        // visit and verify the data.
        assertSameIterator(
            data.toArray(new IBindingSet[0]),
            new Dechunkerator<>(msg.getChunkAccessor().iterator()));

      } finally {

        msg.release();
      }

      // no longer materialized.
      assertFalse(msg.isMaterialized());

    } finally {

      allocationContext.release();
    }
  }

  /** Mock object. */
  private static class MockQueryController implements IQueryClient {

    @Override
    public void haltOp(IHaltOpMessage msg) throws RemoteException {}

    @Override
    public void startOp(IStartOpMessage msg) throws RemoteException {}

    @Override
    public void bufferReady(IChunkMessage<IBindingSet> msg) throws RemoteException {}

    @Override
    public void declareQuery(IQueryDecl queryDecl) {}

    @Override
    public UUID getServiceUUID() throws RemoteException {
      return null;
    }

    @Override
    public PipelineOp getQuery(UUID queryId) throws RemoteException {
      return null;
    }

    @Override
    public void cancelQuery(UUID queryId, Throwable cause) throws RemoteException {}

    @Override
    public UUID[] getRunningQueries() {
      return null;
    }
  }

  private static class MyNIOChunkMessage<E> extends NIOChunkMessage<E> {

    /** */
    private static final long serialVersionUID = 1L;

    /*
     * @param queryController
     * @param queryId
     * @param sinkId
     * @param partitionId
     * @param allocationContext
     * @param source
     * @param addr
     */
    public MyNIOChunkMessage(
        IQueryClient queryController,
        UUID queryId,
        int sinkId,
        int partitionId,
        IAllocationContext allocationContext,
        E[] source,
        InetSocketAddress addr) {

      super(queryController, queryId, sinkId, partitionId, allocationContext, source, addr);
    }

    /** Overridden to expose to the unit test. */
    @Override
    protected void materialize(
        final ManagedResourceService resourceService, final IAllocationContext allocationContext) {
      super.materialize(resourceService, allocationContext);
    }
  }
}
