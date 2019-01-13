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
 * Created on Nov 8, 2011
 */

package org.embergraph.bop.solutions;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.NamedSolutionSetRefUtility;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.HashBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.engine.AbstractQueryEngineTestCase;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.BlockingBufferWithStats;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;

/*
 * Abstract base class for DISTINCT SOLUTIONS test suites.
 *
 * <p>TODO Write a unit test in which some variables are unbound.
 *
 * <p>TODO Write unit test to verify that only the variables which are being made DISTINCT are
 * projected.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractDistinctSolutionsTestCase extends TestCase2 {

  /** */
  public AbstractDistinctSolutionsTestCase() {}

  /** @param name */
  public AbstractDistinctSolutionsTestCase(String name) {
    super(name);
  }

  protected Setup setup;

  public void setUp() {

    //        jnl = new Journal(getProperties());

    setup = new Setup(getName());
  }

  public void tearDown() {

    //        if (jnl != null) {
    //            jnl.destroy();
    //            jnl = null;
    //        }
    //
    // clear reference.
    if (setup != null) {
      setup.destroy();
      setup = null;
    }
  }

  /** Setup for a problem used by many of the join test suites. */
  protected static class Setup {

    protected final String namespace;

    protected final IV<?, ?> brad, john, fred, jane, mary, paul, leon;

    protected final List<IBindingSet> data;

    @SuppressWarnings("rawtypes")
    public Setup(final String namespace) {

      if (namespace == null) throw new IllegalArgumentException();

      this.namespace = namespace;

      brad = makeIV(new LiteralImpl("Brad"));

      john = makeIV(new LiteralImpl("John"));

      fred = makeIV(new LiteralImpl("Fred"));

      jane = makeIV(new LiteralImpl("Jane "));

      mary = makeIV(new LiteralImpl("Mary"));

      paul = makeIV(new LiteralImpl("Paul"));

      leon = makeIV(new LiteralImpl("Leon"));

      final Var<?> x = Var.var("x");
      final Var<?> y = Var.var("y");

      data = new LinkedList<>();
      IBindingSet bset = null;
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(john));
        bset.set(y, new Constant<IV>(mary));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(mary));
        bset.set(y, new Constant<IV>(paul));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(mary));
        bset.set(y, new Constant<IV>(jane));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(paul));
        bset.set(y, new Constant<IV>(leon));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(paul));
        bset.set(y, new Constant<IV>(john));
        data.add(bset);
      }
      {
        bset = new HashBindingSet();
        bset.set(x, new Constant<IV>(leon));
        bset.set(y, new Constant<IV>(paul));
        data.add(bset);
      }
    }

    /*
     * Return a (Mock) IV for a Value.
     *
     * @param v The value.
     * @return The Mock IV.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private IV makeIV(final Value v) {
      final EmbergraphValueFactory valueFactory = EmbergraphValueFactoryImpl.getInstance(namespace);
      final EmbergraphValue bv = valueFactory.asValue(v);
      final IV iv = new TermId(VTE.valueOf(v), nextId++);
      iv.setValue(bv);
      return iv;
    }

    private long nextId = 1L; // Note: First id MUST NOT be 0L !!!

    protected void destroy() {
      // NOP.
    }
  }

  /*
   * Factory for a DISTINCT SOLUTIONS operator.
   *
   * @param args
   * @param anns
   * @return
   */
  protected abstract PipelineOp newDistinctBindingSetsOp(final BOp[] args, final NV... anns);

  /*
   * Unit test for distinct.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  @SuppressWarnings("rawtypes")
  public void test_distinctBindingSets() {

    final UUID queryId = UUID.randomUUID();

    final Var<?> x = Var.var("x");
    //        final Var<?> y = Var.var("y");

    final IVariable<?>[] vars = new IVariable[] {x};

    final int distinctId = 1;

    final PipelineOp query =
        newDistinctBindingSetsOp(
            new BOp[] {},
            new NV(HTreeDistinctBindingSetsOp.Annotations.BOP_ID, distinctId),
            new NV(HTreeDistinctBindingSetsOp.Annotations.VARIABLES, vars),
            new NV(
                HTreeDistinctBindingSetsOp.Annotations.NAMED_SET_REF,
                NamedSolutionSetRefUtility.newInstance(queryId, getName(), vars)),
            new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
            new NV(PipelineOp.Annotations.SHARED_STATE, true),
            new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
            new NV(IPredicate.Annotations.RELATION_NAME, new String[] {"dummy"}));

    // the expected solutions
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<IV>(setup.john)}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<IV>(setup.mary)}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<IV>(setup.paul)}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<IV>(setup.leon)}),
        };

    final MockQueryContext queryContext = new MockQueryContext(queryId);
    try {

      final BOpStats stats = query.newStats();

      final IAsynchronousIterator<IBindingSet[]> source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{setup.data.toArray(new IBindingSet[0])});

      final IBlockingBuffer<IBindingSet[]> sink =
          new BlockingBufferWithStats<>(query, stats);

      final BOpContext<IBindingSet> context =
          new BOpContext<>(
              new MockRunningQuery(null /* fed */, null /* indexManager */, queryContext),
              -1 /* partitionId */,
              stats,
              query /* op */,
              false /* lastInvocation */,
              source,
              sink,
              null /* sink2 */);

      // get task.
      final FutureTask<Void> ft = query.eval(context);

      // execute task.
      // jnl.getExecutorService().execute(ft);
      ft.run();

      AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder("", expected, sink.iterator(), ft);

      // assertTrue(ft.isDone());
      // assertFalse(ft.isCancelled());
      // ft.get(); // verify nothing thrown.

      assertEquals(1L, stats.chunksIn.get());
      assertEquals(6L, stats.unitsIn.get());
      assertEquals(4L, stats.unitsOut.get());
      assertEquals(1L, stats.chunksOut.get());

    } finally {

      queryContext.close();
    }
  }
}
