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
 * Created on Feb 10, 2012
 */

package org.embergraph.bop.engine;

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
import org.embergraph.striterator.Dechunkerator;

/*
 * Test suite for {@link LocalChunkMessage}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestLocalChunkMessage extends TestCase2 {

  /** */
  public TestLocalChunkMessage() {}

  /** @param name */
  public TestLocalChunkMessage(String name) {
    super(name);
  }

  /** Unit test for a message with a single chunk containing a single empty binding set. */
  public void test_oneChunkWithEmptyBindingSet() {

    final List<IBindingSet> data = new LinkedList<>();
    {
      data.add(new HashBindingSet());
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

    // build the chunk.
    final IChunkMessage<IBindingSet> msg =
        new LocalChunkMessage(queryController, queryId, bopId, partitionId, source);

    assertTrue(queryController == msg.getQueryController());

    assertEquals(queryId, msg.getQueryId());

    assertEquals(bopId, msg.getBOpId());

    assertEquals(partitionId, msg.getPartitionId());

    // the data is inline with the message.
    assertTrue(msg.isMaterialized());

    // verify the iterator.
    assertSameIterator(
        data.toArray(new IBindingSet[0]),
        new Dechunkerator<>(msg.getChunkAccessor().iterator()));
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
    //
    final IBindingSet[] source = data.toArray(new IBindingSet[0]);

    // build the chunk.
    final IChunkMessage<IBindingSet> msg =
        new LocalChunkMessage(queryController, queryId, bopId, partitionId, source);

    assertTrue(queryController == msg.getQueryController());

    assertEquals(queryId, msg.getQueryId());

    assertEquals(bopId, msg.getBOpId());

    assertEquals(partitionId, msg.getPartitionId());

    // the data is inline with the message.
    assertTrue(msg.isMaterialized());

    assertSameIterator(
        data.toArray(new IBindingSet[0]),
        new Dechunkerator<>(msg.getChunkAccessor().iterator()));
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
}
