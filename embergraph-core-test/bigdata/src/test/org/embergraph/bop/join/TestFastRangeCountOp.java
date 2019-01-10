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
 * Created on Aug 18, 2010
 */

package org.embergraph.bop.join;

import java.math.BigInteger;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import junit.framework.TestCase2;

import com.bigdata.bop.BOp;
import com.bigdata.bop.BOpContext;
import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstant;
import com.bigdata.bop.IPredicate.Annotations;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.IVariableOrConstant;
import com.bigdata.bop.NV;
import com.bigdata.bop.Var;
import com.bigdata.bop.ap.E;
import com.bigdata.bop.ap.Predicate;
import com.bigdata.bop.ap.R;
import com.bigdata.bop.bindingSet.ListBindingSet;
import com.bigdata.bop.engine.AbstractQueryEngineTestCase;
import com.bigdata.bop.engine.BOpStats;
import com.bigdata.bop.engine.BlockingBufferWithStats;
import com.bigdata.bop.engine.MockRunningQuery;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.ITx;
import com.bigdata.journal.Journal;
import com.bigdata.rdf.internal.impl.literal.XSDIntegerIV;
import com.bigdata.relation.accesspath.IAsynchronousIterator;
import com.bigdata.relation.accesspath.IBlockingBuffer;
import com.bigdata.relation.accesspath.ThickAsynchronousIterator;
import com.bigdata.striterator.ChunkedArrayIterator;

/**
 * Unit tests for the {@link FastRangeCountOp} operator.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestFastRangeCountOp extends TestCase2 {

    /**
     * 
     */
    public TestFastRangeCountOp() {
    }

    /**
     * @param name
     */
    public TestFastRangeCountOp(String name) {
        super(name);
    }

    @Override
    public Properties getProperties() {

        final Properties p = new Properties(super.getProperties());

        p.setProperty(Journal.Options.BUFFER_MODE, BufferMode.Transient
                .toString());

        return p;
        
    }

    static private final String namespace = "ns";
    
    Journal jnl;
    
    @Override
    public void setUp() throws Exception {

        super.setUp();
        
        jnl = new Journal(getProperties());

        loadData(jnl);

    }
    
    /**
     * Create and populate relation in the {@link #namespace}.
     */
    private void loadData(final Journal store) {

        // create the relation.
        final R rel = new R(store, namespace, ITx.UNISOLATED, new Properties());
        rel.create();

        // data to insert.
        final E[] a = {//
                new E("John", "Mary"),// 
                new E("Mary", "Paul"),// 
                new E("Paul", "Leon"),// 
                new E("Leon", "Paul"),// 
                new E("Mary", "John"),// 
        };

        // insert data (the records are not pre-sorted).
        rel.insert(new ChunkedArrayIterator<E>(a.length, a, null/* keyOrder */));

        // Do commit since not scale-out.
        store.commit();

    }

    @Override
    public void tearDown() throws Exception {

        if (jnl != null) {
            jnl.destroy();
            jnl = null;
        }

        super.tearDown();
        
    }

    /**
     * Return an {@link IAsynchronousIterator} that will read a single
     * {@link IBindingSet}.
     * 
     * @param bindingSet
     *            the binding set.
     */
    protected ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
            final IBindingSet bindingSet) {

        return new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { new IBindingSet[] { bindingSet } });

    }

    /**
	 * Unit test corresponding to
	 * 
	 * <pre>
	 * SELECT COUNT(*) as ?count) { ("Mary",?X) }
	 * </pre>
	 */
    public void test_fastRangeCount_01()
            throws InterruptedException, ExecutionException {

        final int joinId = 2;
        final int predId = 3;

		final Predicate<E> predOp = new Predicate<E>(new IVariableOrConstant[] {
				new Constant<String>("Mary"), Var.var("x") }, NV
				.asMap(new NV[] {//
						new NV(Predicate.Annotations.RELATION_NAME,
								new String[] { namespace }),//
						new NV(Predicate.Annotations.BOP_ID, predId),//
						new NV(Annotations.TIMESTAMP,
								ITx.READ_COMMITTED),//
				}));

		final FastRangeCountOp<E> query = new FastRangeCountOp<E>(
				new BOp[] {},// args
				new NV(FastRangeCountOp.Annotations.BOP_ID, joinId),//
				new NV(FastRangeCountOp.Annotations.PREDICATE, predOp),//
				new NV(FastRangeCountOp.Annotations.COUNT_VAR, Var.var("count"))//
				);

        // the expected solutions.
        final IBindingSet[] expected = new IBindingSet[] {//
                new ListBindingSet(//
                        new IVariable[] { Var.var("count") },//
                        new IConstant[] { new Constant<XSDIntegerIV>(new XSDIntegerIV(BigInteger.valueOf(2L))) }//
                ),//
        };

        final BOpStats stats = query.newStats();

        final IAsynchronousIterator<IBindingSet[]> source = new ThickAsynchronousIterator<IBindingSet[]>(
                new IBindingSet[][] { new IBindingSet[] { new ListBindingSet()} });

        final IBlockingBuffer<IBindingSet[]> sink = new BlockingBufferWithStats<IBindingSet[]>(query, stats);

        final BOpContext<IBindingSet> context = new BOpContext<IBindingSet>(
                new MockRunningQuery(null/* fed */, jnl/* indexManager */
                ), -1/* partitionId */, stats,query/* op */,
                false/* lastInvocation */, 
                source, sink, null/* sink2 */);

        // get task.
        final FutureTask<Void> ft = query.eval(context);
        
        // execute task.
        jnl.getExecutorService().execute(ft);

        AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(),
                ft);

        // join task
        assertEquals(1L, stats.chunksIn.get());
        assertEquals(1L, stats.unitsIn.get());
        assertEquals(1L, stats.unitsOut.get());
        assertEquals(1L, stats.chunksOut.get());

    }

}
