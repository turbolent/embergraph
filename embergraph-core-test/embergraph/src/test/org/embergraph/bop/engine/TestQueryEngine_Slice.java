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
 * Created on Sep 17, 2010
 */

package org.embergraph.bop.engine;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.bindingSet.EmptyBindingSet;
import org.embergraph.bop.bset.StartOp;
import org.embergraph.bop.solutions.MemorySortOp;
import org.embergraph.bop.solutions.SliceOp;
import org.embergraph.bop.solutions.SliceOp.SliceStats;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;

/*
 * Stress test for {@link SliceOp} in which a large number of small chunks are fed into the query
 * such that the concurrency constraints of the slice are stress tested. {@link
 * SliceOp#isSharedState()} returns <code>true</code> so each invocation of the same {@link SliceOp}
 * operator instance should use the same {@link SliceStats} object. This test will fail if that is
 * not true.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestQueryEngine_Slice extends TestCase2 {

  /** */
  public TestQueryEngine_Slice() {}

  /** @param name */
  public TestQueryEngine_Slice(String name) {
    super(name);
  }

  @Override
  public Properties getProperties() {

    final Properties p = new Properties(super.getProperties());

    p.setProperty(Journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    return p;
  }

  private Journal jnl;
  private QueryEngine queryEngine;

  @Override
  public void setUp() {

    jnl = new Journal(getProperties());

    queryEngine = new QueryEngine(jnl);

    queryEngine.init();
  }

  @Override
  public void tearDown() {

    if (queryEngine != null) {
      queryEngine.shutdownNow();
      queryEngine = null;
    }

    if (jnl != null) {
      jnl.destroy();
      jnl = null;
    }
  }

  /*
   * Return an {@link IAsynchronousIterator} that will read a single, chunk containing all of the
   * specified {@link IBindingSet}s.
   *
   * @param bindingSetChunks the chunks of binding sets.
   */
  protected ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
      final IBindingSet[][] bindingSetChunks) {

    return new ThickAsynchronousIterator<>(bindingSetChunks);
  }

  public void testStressThreadSafe() throws Exception {

    for (int i = 0; i < 100; i++) {

      test_slice_threadSafe();
    }
  }

  public void test_slice_threadSafe() throws Exception {

    final long timeout = 10000; // ms

    final int ntrials = 10000;

    final int poolSize = 10;

    doSliceTest(500L /* offset */, 1500L /* limit */, timeout, ntrials, poolSize);
  }

  /*
   * @param timeout
   * @param ntrials
   * @param poolSize
   * @return The #of successful trials.
   * @throws Exception
   */
  protected void doSliceTest(
      final long offset,
      final long limit,
      final long timeout,
      final int ntrials,
      final int poolSize)
      throws Exception {

    final IBindingSet[][] chunks = new IBindingSet[ntrials][];
    {
      final Random r = new Random();
      final IBindingSet bset = EmptyBindingSet.INSTANCE;
      for (int i = 0; i < chunks.length; i++) {
        // random non-zero chunk size
        chunks[i] = new IBindingSet[r.nextInt(10) + 1];
        for (int j = 0; j < chunks[i].length; j++) {
          chunks[i][j] = bset;
        }
      }
    }

    final int startId = 1;
    final int sliceId = 2;

    /*
     * Note: The StartOp breaks up the initial set of chunks into multiple
     * IChunkMessages, which results in multiple invocations of the StartOp.
     */
    final PipelineOp startOp =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(SliceOp.Annotations.BOP_ID, startId),
                new NV(
                    MemorySortOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER)));

    final SliceOp query =
        new SliceOp(
            new BOp[] {startOp},
            NV.asMap(
                new NV(SliceOp.Annotations.BOP_ID, sliceId),
                new NV(SliceOp.Annotations.SHARED_STATE, true),
                new NV(SliceOp.Annotations.OFFSET, offset),
                new NV(SliceOp.Annotations.LIMIT, limit),
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS, false)));

    final UUID queryId = UUID.randomUUID();
    final IRunningQuery q =
        queryEngine.eval(
            queryId,
            query,
            null /* attributes */,
            new LocalChunkMessage(queryEngine, queryId, sliceId, -1 /* partitionId */, chunks));

    // consume solutions.
    int nsolutions = 0;
    final ICloseableIterator<IBindingSet[]> itr = q.iterator();
    try {
      while (itr.hasNext()) {
        nsolutions += itr.next().length;
      }
    } finally {
      itr.close();
    }

    // wait for the query to terminate.
    q.get();

    // Verify stats.
    final SliceStats stats = (SliceStats) q.getStats().get(sliceId);
    if (log.isInfoEnabled()) log.info(getClass().getName() + "." + getName() + " : " + stats);
    assertNotNull(stats);
    assertEquals(limit, stats.naccepted.get());
    assertEquals(limit, nsolutions);
  }
}
