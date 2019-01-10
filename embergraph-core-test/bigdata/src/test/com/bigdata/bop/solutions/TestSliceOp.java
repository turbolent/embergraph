/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Sep 5, 2010
 */

package com.bigdata.bop.solutions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.TestCase2;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpContext;
import com.bigdata.bop.BOpEvaluationContext;
import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstant;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.NV;
import com.bigdata.bop.PipelineOp;
import com.bigdata.bop.Var;
import com.bigdata.bop.bindingSet.EmptyBindingSet;
import com.bigdata.bop.bindingSet.HashBindingSet;
import com.bigdata.bop.bindingSet.ListBindingSet;
import com.bigdata.bop.engine.AbstractQueryEngineTestCase;
import com.bigdata.bop.engine.BOpStats;
import com.bigdata.bop.engine.BlockingBufferWithStats;
import com.bigdata.bop.engine.IRunningQuery;
import com.bigdata.bop.solutions.SliceOp.SliceStats;
import com.bigdata.journal.IIndexManager;
import com.bigdata.relation.accesspath.BlockingBuffer;
import com.bigdata.relation.accesspath.IAsynchronousIterator;
import com.bigdata.relation.accesspath.IBlockingBuffer;
import com.bigdata.relation.accesspath.ThickAsynchronousIterator;
import com.bigdata.service.IBigdataFederation;
import com.bigdata.util.InnerCause;

/**
 * Unit tests for {@link SliceOp}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSliceOp extends TestCase2 {

    /**
     * 
     */
    public TestSliceOp() {
    }

    /**
     * @param name
     */
    public TestSliceOp(String name) {
        super(name);
    }

    ArrayList<IBindingSet> data;

    public void setUp() throws Exception {

        setUpData();

    }

    /**
     * Setup the data.
     */
    private void setUpData() {

        final Var<?> x = Var.var("x");
        final Var<?> y = Var.var("y");

        data = new ArrayList<IBindingSet>();
            IBindingSet bset = null;
            { // 0
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("John"));
                bset.set(y, new Constant<String>("Mary"));
                data.add(bset);
            }
            { // 1
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Mary"));
                bset.set(y, new Constant<String>("Paul"));
                data.add(bset);
            }
            { // 2
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Mary"));
                bset.set(y, new Constant<String>("Jane"));
                data.add(bset);
            }
            { // 3
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Paul"));
                bset.set(y, new Constant<String>("Leon"));
                data.add(bset);
            }
            { // 4
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Paul"));
                bset.set(y, new Constant<String>("John"));
                data.add(bset);
            }
            { // 5
                bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Leon"));
                bset.set(y, new Constant<String>("Paul"));
                data.add(bset);
            }

    }

    public void tearDown() throws Exception {
        
        // clear reference.
        data = null;

    }

    /**
     * Unit test for correct visitation for a variety of offset/limit values.
     * 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void test_slice_offset2_limit3() throws InterruptedException,
            ExecutionException {

        final Var<?> x = Var.var("x");
        final Var<?> y = Var.var("y");

        final int bopId = 1;

        final long offset = 2L;
        final long limit = 3L;
        
        final SliceOp query = new SliceOp(new BOp[]{},
                NV.asMap(new NV[]{//
                    new NV(SliceOp.Annotations.BOP_ID, bopId),//
                    new NV(SliceOp.Annotations.OFFSET, offset),//
                    new NV(SliceOp.Annotations.LIMIT, limit),//
                    new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                    new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));
        
        assertEquals("offset", offset, query.getOffset());
        
        assertEquals("limit", limit, query.getLimit());

        // the expected solutions
        final IBindingSet[] expected = new IBindingSet[] {//
//                new ListBindingSet(//
//                        new IVariable[] { x, y },//
//                        new IConstant[] { new Constant<String>("John"),
//                                new Constant<String>("Mary"), }//
//                ),
//                new ListBindingSet(//
//                        new IVariable[] { x, y },//
//                        new IConstant[] { new Constant<String>("Mary"),
//                                new Constant<String>("Paul"), }//
//                ),
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Mary"),
                                new Constant<String>("Jane") }//
                ),
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Paul"),
                                new Constant<String>("Leon") }//
                ),
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Paul"),
                                new Constant<String>("John") }//
                ),
//                new ListBindingSet(//
//                        new IVariable[] { x, y },//
//                        new IConstant[] { new Constant<String>("Leon"),
//                                new Constant<String>("Paul") }//
//                ),ne
        };

        final SliceStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);
        
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator());
        
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
//        try {
            ft.get(); // verify nothing thrown.
//            fail("Expecting inner cause : " + InterruptedException.class);
//        } catch (Throwable t) {
//            if (InnerCause.isInnerCause(t, InterruptedException.class)) {
//                if (log.isInfoEnabled())
//                    log.info("Ignoring expected exception: " + t, t);
//            } else {
//                fail("Expecting inner cause " + InterruptedException.class
//                        + ", not " + t, t);
//            }
//        }

        // check the slice stats first.
        assertEquals(limit, stats.naccepted.get());
        assertEquals(offset+limit, stats.nseen.get());

        // then the general purpose bop stats (less critical).
        assertEquals(1L, stats.chunksIn.get());
        assertEquals(offset+limit, stats.unitsIn.get());
        assertEquals(limit, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());

    }

    /**
     * Unit test for correct visitation for a variety of offset/limit values.
     * 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void test_slice_offset1_limit3() throws InterruptedException,
            ExecutionException {

        final Var<?> x = Var.var("x");
        final Var<?> y = Var.var("y");

        final int bopId = 1;

        final long offset = 1;
        final long limit = 3;
        
        final SliceOp query = new SliceOp(new BOp[]{},
                NV.asMap(new NV[]{//
                    new NV(SliceOp.Annotations.BOP_ID, bopId),//
                    new NV(SliceOp.Annotations.OFFSET, offset),//
                    new NV(SliceOp.Annotations.LIMIT, limit),//
                    new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                    new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));
        
        assertEquals("offset", offset, query.getOffset());
        
        assertEquals("limit", limit, query.getLimit());

        // the expected solutions
        final IBindingSet[] expected = new IBindingSet[] {//
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Mary"),
                                new Constant<String>("Paul"), }//
                ),
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Mary"),
                                new Constant<String>("Jane") }//
                ),
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Paul"),
                                new Constant<String>("Leon") }//
                ), };

        final SliceStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);
        
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator());
        
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
        ft.get(); // verify nothing thrown.

        assertEquals(limit, stats.naccepted.get());
        assertEquals(offset+limit, stats.nseen.get());

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(4L, stats.unitsIn.get());
        assertEquals(3L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());

    }

    /**
     * Unit test where the offset is never satisfied. For this test, all binding
     * sets will be consumed but none will be emitted.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void test_slice_offsetNeverSatisfied() throws InterruptedException,
            ExecutionException {

        final int bopId = 1;

        final long offset = 100L;
        final long limit = 3L;
        
        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                new NV(SliceOp.Annotations.OFFSET, offset),//
                new NV(SliceOp.Annotations.LIMIT, limit),//
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        assertEquals("offset", offset, query.getOffset());

        assertEquals("limit", limit, query.getLimit());

        // the expected solutions (none)
        final IBindingSet[] expected = new IBindingSet[0];
        
        final SliceStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        ft.run();

        /*
         * Note: When the slice does not have a limit (or if we write a test
         * where the #of source binding sets can not satisfy the offset and/or
         * limit) then the sink WILL NOT be closed by the slice. Therefore, in
         * order for the iterator to terminate we first check the Future of the
         * SliceTask and then _close_ the sink before consuming the iterator.
         */
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
        ft.get(); // verify nothing thrown.
        sink.close(); // close the sink so the iterator will terminate!

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator());

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(0L, stats.unitsOut.get());
        assertEquals(0L, stats.chunksOut.get());
        assertEquals(6L, stats.nseen.get());
        assertEquals(0L, stats.naccepted.get());

    }

    /**
     * Unit test where the offset plus the limit is never satisfied. For this
     * test, all binding sets will be consumed and some will be emitted, but the
     * slice is never satisfied.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void test_slice_offsetPlusLimitNeverSatisfied() throws InterruptedException,
            ExecutionException {

        final Var<?> x = Var.var("x");
        final Var<?> y = Var.var("y");

        final int bopId = 1;

        final long offset = 2L;
        final long limit = 10L;
        
        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                new NV(SliceOp.Annotations.OFFSET, offset),//
                new NV(SliceOp.Annotations.LIMIT, limit),//
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        assertEquals("offset", offset, query.getOffset());

        assertEquals("limit", limit, query.getLimit());

        // the expected solutions
        final IBindingSet[] expected = new IBindingSet[] {//
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Mary"),
                                new Constant<String>("Jane") }//
                ),//
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Paul"),
                                new Constant<String>("Leon") }//
                ),//
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Paul"),
                                new Constant<String>("John") }//
                ),//
                new ListBindingSet(//
                        new IVariable[] { x, y },//
                        new IConstant[] { new Constant<String>("Leon"),
                                new Constant<String>("Paul") }//
                ),//
        };

        final SliceStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        ft.run();

        /*
         * Note: When the slice does not have a limit (or if we write a test
         * where the #of source binding sets can not satisfy the offset and/or
         * limit) then the sink WILL NOT be closed by the slice. Therefore, in
         * order for the iterator to terminate we first check the Future of the
         * SliceTask and then _close_ the sink before consuming the iterator.
         */
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
        ft.get(); // verify nothing thrown.
        sink.close(); // close the sink so the iterator will terminate!

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator());

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(4L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());
        assertEquals(6L, stats.nseen.get());
        assertEquals(4L, stats.naccepted.get());

    }

    /**
     * Unit test where the slice accepts everything.
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void test_slice_offset0_limitAll() throws InterruptedException,
            ExecutionException {

        final int bopId = 1;

        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                // new NV(SliceOp.Annotations.OFFSET, 1L),//
                // new NV(SliceOp.Annotations.LIMIT, 3L),//
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        assertEquals("offset", 0L, query.getOffset());

        assertEquals("limit", Long.MAX_VALUE, query.getLimit());

        // the expected solutions
        final IBindingSet[] expected = data.toArray(new IBindingSet[0]);

        final SliceStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        ft.run();

        /*
         * Note: When the slice does not have a limit (or if we write a test
         * where the #of source binding sets can not satisfy the offset and/or
         * limit) then the sink WILL NOT be closed by the slice. Therefore, in
         * order for the iterator to terminate we first check the Future of the
         * SliceTask and then _close_ the sink before consuming the iterator.
         */
        assertTrue(ft.isDone());
        assertFalse(ft.isCancelled());
        ft.get(); // verify nothing thrown.
        sink.close(); // close the sink so the iterator will terminate!

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator());

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(6L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());
        assertEquals(6L, stats.nseen.get());
        assertEquals(6L, stats.naccepted.get());

    }

    public void test_slice_correctRejection_badOffset()
            throws InterruptedException {

        final int bopId = 1;

        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                        new NV(SliceOp.Annotations.OFFSET, -1L),//
                        new NV(SliceOp.Annotations.LIMIT, 3L),//
                        new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                                BOpEvaluationContext.CONTROLLER),//
                        new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                        new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        assertEquals("offset", -1L, query.getOffset());

        assertEquals("limit", 3L, query.getLimit());

        final BOpStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        try {
            query.eval(context);
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

    }

    public void test_slice_correctRejection_badLimit()
            throws InterruptedException {

        final int bopId = 1;

        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                new NV(SliceOp.Annotations.OFFSET, 1L),//
                new NV(SliceOp.Annotations.LIMIT, -1L),//
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        assertEquals("offset", 1L, query.getOffset());

        // Note: A limit of zero is a bad idea, but it is legal.
        assertEquals("limit", -1L, query.getLimit());

        final BOpStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { data.toArray(new IBindingSet[0]) });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */
                , sink), -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        try {
            query.eval(context);
            fail("Expecting: " + IllegalArgumentException.class);
        } catch (IllegalArgumentException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

    }

    public void test_slice_threadSafe() throws Exception {

        final long timeout = 10000; // ms

        final int ntrials = 10000;

        final int poolSize = 10;

        doStressTest(500L/* offset */, 1500L/* limit */, timeout, ntrials,
                poolSize);
        
    }

    /**
     * 
     * @param timeout
     * @param ntrials
     * @param poolSize
     * 
     * @return The #of successful trials.
     * 
     * @throws Exception
     */
    protected int doStressTest(final long offset, final long limit,
            final long timeout, final int ntrials, final int poolSize)
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
        final int bopId = 1;
        final SliceOp query = new SliceOp(new BOp[] {}, NV.asMap(new NV[] {//
                new NV(SliceOp.Annotations.BOP_ID, bopId),//
                new NV(SliceOp.Annotations.OFFSET, offset),//
                new NV(SliceOp.Annotations.LIMIT, limit),//
                new NV(SliceOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.SHARED_STATE,true),//
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS,false),//
                }));

        final SliceStats stats = query.newStats();

        // start time in nanos.
        final long begin = System.nanoTime();

        // timeout in nanos.
        final long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);

        final ThreadPoolExecutor service = (ThreadPoolExecutor) Executors
                .newFixedThreadPool(poolSize);

        try {

            service.prestartAllCoreThreads();

            final List<FutureTask<Void>> futures = new LinkedList<FutureTask<Void>>();

            for (int i = 0; i < ntrials; i++) {

                final IBindingSet[] chunk = chunks[i];

                final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                        new IBindingSet[][] { chunk });

                final IBlockingBuffer<IBindingSet[]> sink = new BlockingBuffer<IBindingSet[]>(
                        chunk.length);

                final IRunningQuery q = new MockRunningQuery(null/* fed */,
                        null/* indexManager */, sink);

                final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                        q, -1/* partitionId */, stats, query/* op */,
                        false/* lastInvocation */, source, sink, null/* sink2 */);

                final FutureTask<Void> ft = query.eval(context);

                futures.add(ft);

                service.execute(ft);

            }

            int nerror = 0;
            int ncancel = 0;
            int ntimeout = 0;
            int nsuccess = 0;
            int ninterrupt = 0;
            for (FutureTask<Void> ft : futures) {
                // remaining nanoseconds.
                final long remaining = nanos - (System.nanoTime() - begin);
                if (remaining <= 0)
                    ft.cancel(true/* mayInterruptIfRunning */);
                try {
                    ft.get(remaining, TimeUnit.NANOSECONDS);
                    nsuccess++;
                } catch (CancellationException ex) {
                    ncancel++;
                } catch (TimeoutException ex) {
                    ntimeout++;
                } catch (ExecutionException ex) {
                    if (InnerCause.isInnerCause(ex, InterruptedException.class)) {
                        ninterrupt++;
                    } else {
                        log.error(ex, ex);
                        nerror++;
                    }
                }
            }

            final long nseen = stats.nseen.get();
            
            final long naccepted = stats.naccepted.get();
            
            final long nexpected = limit;

            final String msg = "offset=" + offset + ", limit=" + limit
                    + ", nseen=" + nseen + ",naccepted=" + naccepted
                    + ", nexpected=" + nexpected + ", nerror=" + nerror
                    + ", ncancel=" + ncancel + ", ntimeout=" + ntimeout
                    + ", ninterrupt=" + ninterrupt + ", nsuccess=" + nsuccess;

            if (log.isInfoEnabled())
                log.info(getClass().getName() + "." + getName() + " : " + msg);

            if (nerror > 0)
                fail(msg);

            if (nexpected != naccepted)
                fail(msg);
            
            return nsuccess;
            
        } finally {

            service.shutdownNow();
            
        }
        
    }

    private static class MockRunningQuery extends
            com.bigdata.bop.engine.MockRunningQuery {
        
        private final IBlockingBuffer<IBindingSet[]> sink;
        
        public MockRunningQuery(final IBigdataFederation<?> fed,
                final IIndexManager indexManager,
                final IBlockingBuffer<IBindingSet[]> sink) {

            super(fed, indexManager);

            this.sink = sink;

        }

        /**
         * Overridden to close the sink so the slice will terminate.
         */
        @Override
        public void halt(Void v) {
            sink.close();
        }

    }

}
