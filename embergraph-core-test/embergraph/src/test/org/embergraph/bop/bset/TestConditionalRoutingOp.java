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
 * Created on Aug 19, 2010
 */

package org.embergraph.bop.bset;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import junit.framework.TestCase2;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.HashBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.EQConstant;
import org.embergraph.bop.engine.AbstractQueryEngineTestCase;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.BlockingBufferWithStats;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.bop.solutions.JVMDistinctBindingSetsOp;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;

import cutthecrap.utils.striterators.ICloseableIterator;

/**
 * Unit tests for {@link JVMDistinctBindingSetsOp}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @todo write a unit test in which some variables are unbound.
 */
public class TestConditionalRoutingOp extends TestCase2 {

    /**
     * 
     */
    public TestConditionalRoutingOp() {
    }

    /**
     * @param name
     */
    public TestConditionalRoutingOp(String name) {
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
     * Unit test for conditional routing of binding sets.
     * 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void test_conditionalRouting() throws InterruptedException,
            ExecutionException {

        final Var<?> x = Var.var("x");
        
        final int bopId = 1;
        
        final ConditionalRoutingOp query = new ConditionalRoutingOp(new BOp[]{},
                NV.asMap(new NV[]{//
                    new NV(BOp.Annotations.BOP_ID,bopId),//
                    new NV(ConditionalRoutingOp.Annotations.CONDITION,
                    		Constraint.wrap(new EQConstant(x,new Constant<String>("Mary")))),//
                }));
        
        // the expected solutions (default sink).
        final IBindingSet[] expected = new IBindingSet[] {//
                new ListBindingSet(//
                        new IVariable[] { x },//
                        new IConstant[] { new Constant<String>("Mary") }//
                ), new ListBindingSet(//
                        new IVariable[] { x },//
                        new IConstant[] { new Constant<String>("Mary") }//
                ), };

        // the expected solutions (alt sink).
        final IBindingSet[] expected2 = new IBindingSet[] {//
        new ListBindingSet(//
                new IVariable[] { x },//
                new IConstant[] { new Constant<String>("John") }//
                ), new ListBindingSet(//
                        new IVariable[] { x },//
                        new IConstant[] { new Constant<String>("Paul") }//
                ), new ListBindingSet(//
                        new IVariable[] { x },//
                        new IConstant[] { new Constant<String>("Paul") }//
                ), new ListBindingSet(//
                        new IVariable[] { x },//
                        new IConstant[] { new Constant<String>("Leon") }//
                ), };

        final BOpStats stats = query.newStats();

        final ICloseableIterator<IBindingSet[]> source = newBindingSetIterator(data
                .toArray(new IBindingSet[0]));

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(
                query, stats);
        final IBlockingBuffer<IBindingSet[]> sink2 = new BlockingBufferWithStats<IBindingSet[]>(
                query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, null/* indexManager */),
                -1/* partitionId */, stats, query/* op */,
                false/* lastInvocation */, source, sink, sink2);

        // get task.
        final FutureTask<Void> ft = query.eval(context);
        
        // execute task.
        ft.run();

        AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);
        AbstractQueryEngineTestCase.assertSameSolutions(expected2, sink2.iterator(), ft);
        
//        assertTrue(ft.isDone());
//        assertFalse(ft.isCancelled());
//        ft.get(); // verify nothing thrown.

        assertEquals(1L, stats.chunksIn.get());
        assertEquals(6L, stats.unitsIn.get());
        assertEquals(6L, stats.unitsOut.get());
        assertEquals(2L, stats.chunksOut.get());

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
