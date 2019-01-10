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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.bset;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import junit.framework.TestCase2;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpContext;
import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.NV;
import com.bigdata.bop.Var;
import com.bigdata.bop.bindingSet.HashBindingSet;
import com.bigdata.bop.constraint.Constraint;
import com.bigdata.bop.constraint.EQConstant;
import com.bigdata.bop.engine.AbstractQueryEngineTestCase;
import com.bigdata.bop.engine.BOpStats;
import com.bigdata.bop.engine.BlockingBufferWithStats;
import com.bigdata.bop.engine.MockRunningQuery;
import com.bigdata.relation.accesspath.IAsynchronousIterator;
import com.bigdata.relation.accesspath.IBlockingBuffer;
import com.bigdata.relation.accesspath.ThickAsynchronousIterator;

import cutthecrap.utils.striterators.ICloseableIterator;

/**
 * Test suite for {@link CopyOp}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestCopyBindingSets extends TestCase2 {

    /**
     * 
     */
    public TestCopyBindingSets() {
    }

    /**
     * @param name
     */
    public TestCopyBindingSets(String name) {
        super(name);
    }

    List<IBindingSet> data = null;

    public void setUp() throws Exception {

        setUpData();

    }

    /**
     * Setup the data.
     */
    private void setUpData() {

        final Var<?> x = Var.var("x");

        data = new LinkedList<IBindingSet>();
        IBindingSet bset = null;
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("John"));
            data.add(bset);
        }
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("Mary"));
            data.add(bset);
        }
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("Mary"));
            data.add(bset);
        }
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("Paul"));
            data.add(bset);
        }
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("Paul"));
            data.add(bset);
        }
        {
            bset = new HashBindingSet();
            bset.set(x, new Constant<String>("Leon"));
            data.add(bset);
        }

    }

    public void tearDown() throws Exception {

        // clear reference.
        data = null;

    }

    /**
     * Unit test for copying the input to the output.
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void test_copyBindingSets() throws InterruptedException,
            ExecutionException {

        final int bopId = 1;

        final CopyOp query = new CopyOp(new BOp[] {}, NV
                .asMap(new NV[] {//
                new NV(BOp.Annotations.BOP_ID, bopId),//
                }));

        // the expected solutions (default sink).
        final IBindingSet[] expected = data.toArray(new IBindingSet[0]);

        final BOpStats stats = query.newStats();

        final ICloseableIterator<IBindingSet[]> source = newBindingSetIterator(data
                .toArray(new IBindingSet[0]));

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */),
                -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        // execute task.
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);

//        assertTrue(ft.isDone());
//        assertFalse(ft.isCancelled());
//        ft.get(); // verify nothing thrown.

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(6L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());

    }
    
    /**
     * Testing against {@link Tee} which is a specialized {@link CopyOp} which requires
     * that the alternate sink is also specified. Gives us code coverage of {@link Tee}
     * as well.
     */
    public void test_copyToSinkAndAltSink() throws InterruptedException, ExecutionException {

        final int bopId = 1;

        final Tee query = new Tee(new BOp[] {}, NV
                .asMap(new NV[] {//
                new NV(BOp.Annotations.BOP_ID, bopId),//
                new NV(CopyOp.Annotations.ALT_SINK_REF, 2),
                }));

        // the expected solutions (default sink).
        final IBindingSet[] expected = data.toArray(new IBindingSet[0]);

        final BOpStats stats = query.newStats();

        final ICloseableIterator<IBindingSet[]> source = newBindingSetIterator(data
                .toArray(new IBindingSet[0]));

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);
        final IBlockingBuffer<IBindingSet[]> altSink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */),
                -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, altSink);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        // execute task.
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);
        AbstractQueryEngineTestCase.assertSameSolutions(expected, altSink.iterator(), ft);

//        assertTrue(ft.isDone());
//        assertFalse(ft.isCancelled());
//        ft.get(); // verify nothing thrown.

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(12L, stats.unitsOut.get());
        assertEquals(2L, stats.chunksOut.get());      
    }

    /**
     * Unit test for copying the input to the output with an {@link IConstraint}
     * .
     * 
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void test_copyBindingSetsWithConstraint()
            throws InterruptedException, ExecutionException {

        final Var<?> x = Var.var("x");

        final int bopId = 1;

        final CopyOp query = new CopyOp(new BOp[] {}, NV
                .asMap(new NV[] {//
                        new NV(BOp.Annotations.BOP_ID, bopId),//
                        new NV(CopyOp.Annotations.CONSTRAINTS,
                                new IConstraint[] {
                        		Constraint.wrap(new EQConstant(x, new Constant<String>("Mary")))
                        }),//
                }));

        // the expected solutions (default sink).
        final List<IBindingSet> expected = new LinkedList<IBindingSet>();
        {
            {
                final IBindingSet bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Mary"));
                expected.add(bset);
            }
            {
                final IBindingSet bset = new HashBindingSet();
                bset.set(x, new Constant<String>("Mary"));
                expected.add(bset);
            }
        }

        final BOpStats stats = query.newStats();

        final ICloseableIterator<IBindingSet[]> source = newBindingSetIterator(data
                .toArray(new IBindingSet[0]));

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */),
                -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);

        // execute task.
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected
                .toArray(new IBindingSet[] {}), sink.iterator(), ft);

//        assertTrue(ft.isDone());
//        assertFalse(ft.isCancelled());
//        ft.get(); // verify nothing thrown.

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(2L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());

    }

    /**
     * Return an {@link IAsynchronousIterator} that will read the source
     * {@link IBindingSet}s.
     * 
     * @param bsets
     *            The source binding sets.
     */
    private static ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
            final IBindingSet[] bsets) {
     
        return new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { bsets });
        
    }

}
