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
 * Created on Aug 18, 2010
 */

package org.embergraph.bop.join;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpBase;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IPredicate.Annotations;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.E;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.ap.R;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.INBinarySearch;
import org.embergraph.bop.constraint.NEConstant;
import org.embergraph.bop.engine.AbstractQueryEngineTestCase;
import org.embergraph.bop.engine.BlockingBufferWithStats;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;
import org.embergraph.striterator.ChunkedArrayIterator;

/*
 * Unit tests for the {@link PipelineJoin} operator.
 *
 * <p>Note: The logic to map binding sets over shards is tested independently.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestPipelineJoin extends TestCase2 {

  /** */
  public TestPipelineJoin() {}

  /** @param name */
  public TestPipelineJoin(String name) {
    super(name);
  }

  @Override
  public Properties getProperties() {

    final Properties p = new Properties(super.getProperties());

    p.setProperty(Journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    return p;
  }

  private static final String namespace = "ns";

  Journal jnl;

  @Override
  public void setUp() throws Exception {

    super.setUp();

    jnl = new Journal(getProperties());

    loadData(jnl);
  }

  /** Create and populate relation in the {@link #namespace}. */
  private void loadData(final Journal store) {

    // create the relation.
    final R rel = new R(store, namespace, ITx.UNISOLATED, new Properties());
    rel.create();

    // data to insert.
    final E[] a = {
      new E("John", "Mary"), //
      new E("Mary", "Paul"), //
      new E("Paul", "Leon"), //
      new E("Leon", "Paul"), //
      new E("Mary", "John"), //
    };

    // insert data (the records are not pre-sorted).
    rel.insert(new ChunkedArrayIterator<>(a.length, a, null /* keyOrder */));

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

  /*
   * Return an {@link IAsynchronousIterator} that will read a single {@link IBindingSet}.
   *
   * @param bindingSet the binding set.
   */
  protected ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
      final IBindingSet bindingSet) {

    return new ThickAsynchronousIterator<>(
        new IBindingSet[][]{new IBindingSet[]{bindingSet}});
  }

  /*
   * Unit test for a pipeline join without shared variables and fed by a single empty binding set.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_join_noSharedVariables_emptySourceSolution() {

    final int joinId = 2;
    final int predId = 3;

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), Var.var("x")},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(Predicate.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp));

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("Paul")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IAsynchronousIterator<IBindingSet[]> source =
        new ThickAsynchronousIterator<>(
            new IBindingSet[][]{new IBindingSet[]{new ListBindingSet()}});

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(1L, stats.unitsIn.get());
    assertEquals(2L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(1L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit test for a join without shared variables with multiple source solutions.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_join_noSharedVariables_multipleSourceSolutions() {

    //        final int startId = 1;
    final int joinId = 2;
    final int predId = 3;

    //		final BOp startOp = new CopyOp(new BOp[] {}, NV.asMap(new NV[] {
    //				new NV(Predicate.Annotations.BOP_ID, startId),
    //				}));

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), Var.var("x")},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(Predicate.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp)
            //				new NV(PipelineJoin.Annotations.COALESCE_DUPLICATE_ACCESS_PATHS, false)
        );

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("y")},
              new IConstant[] {new Constant<>("John"), new Constant<>("Jack")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("y")},
              new IConstant[] {new Constant<>("Paul"), new Constant<>("Jack")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("z")},
              new IConstant[] {new Constant<>("John"), new Constant<>("Jill")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("z")},
              new IConstant[] {new Constant<>("Paul"), new Constant<>("Jill")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IAsynchronousIterator<IBindingSet[]> source;
    {
      final IBindingSet bset1 = new ListBindingSet();
      final IBindingSet bset2 = new ListBindingSet();

      bset1.set(Var.var("y"), new Constant<>("Jack"));
      bset2.set(Var.var("z"), new Constant<>("Jill"));

      source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{new IBindingSet[]{bset1, bset2}});
    }

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(2L, stats.unitsIn.get());
    assertEquals(4L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(1L, stats.accessPathDups.get());
    assertEquals(1L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit test for a join with shared variables with multiple source solutions (the source solutions
   * already have a bound value for the shared variable so the join turns into a point test).
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_join_sharedVariables_multipleSourceSolutions() {

    final int joinId = 2;
    final int predId = 3;

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), Var.var("x")},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(Predicate.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp));

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("y")},
              new IConstant[] {new Constant<>("John"), new Constant<>("Jack")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x"), Var.var("z")},
              new IConstant[] {new Constant<>("Paul"), new Constant<>("Jill")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IAsynchronousIterator<IBindingSet[]> source;
    {
      final IBindingSet bset1 = new ListBindingSet();
      final IBindingSet bset2 = new ListBindingSet();

      bset1.set(Var.var("x"), new Constant<>("John"));
      bset1.set(Var.var("y"), new Constant<>("Jack"));

      bset2.set(Var.var("x"), new Constant<>("Paul"));
      bset2.set(Var.var("z"), new Constant<>("Jill"));

      source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{new IBindingSet[]{bset1, bset2}});
    }

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(2L, stats.unitsIn.get());
    assertEquals(2L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(2L, stats.accessPathCount.get());
    assertEquals(2L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());
  }

  /*
   * Unit test for a pipeline join in which we expect duplicate access paths to be eliminated.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_join_duplicateElimination() {

    //        final int startId = 1;
    final int joinId = 2;
    final int predId = 3;

    //        final BOp startOp =                 new CopyOp(new BOp[] {}, NV.asMap(new NV[] {
    //				new NV(Predicate.Annotations.BOP_ID, startId),
    //				}));

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), Var.var("x")},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(Predicate.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp));

    // the expected solutions (each solution appears twice since we feed two empty binding sets in).
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("Paul")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(
              new IVariable[] {Var.var("x")}, new IConstant[] {new Constant<>("Paul")}),
        };

    final PipelineJoinStats stats = query.newStats();

    // submit TWO (2) empty binding sets in ONE (1) chunk.
    final IAsynchronousIterator<IBindingSet[]> source =
        new ThickAsynchronousIterator<>(
            new IBindingSet[][]{new IBindingSet[]{new ListBindingSet(), new ListBindingSet()}});

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    //        ft.get();// wait for completion (before showing stats), then look for errors.
    //
    //        // show stats.
    //        System.err.println("stats: "+stats);

    // verify solutions.
    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(), ft);

    // verify stats.

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(2L, stats.unitsIn.get());
    assertEquals(4L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(1L, stats.accessPathDups.get());
    assertEquals(1L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit test for a join with an {@link IConstraint}. The constraint is used to filter out one of
   * the solutions where "Mary" is the present in the first column of the relation.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_join_constraint() {

    //        final Var<String> x = Var.var("x");
    final Var<String> y = Var.var("y");
    final IConstant<String>[] set =
        new IConstant[] {
            new Constant<>("John"),
        };

    //        final int startId = 1;
    final int joinId = 2;
    final int predId = 3;

    //		final BOp startOp = new CopyOp(new BOp[] {}, NV.asMap(new NV[] {
    //				new NV(BOpBase.Annotations.BOP_ID, startId),
    //				}));

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), y},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(BOpBase.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp),
            new NV(
                PipelineJoin.Annotations.CONSTRAINTS,
                new IConstraint[]{Constraint.wrap(new INBinarySearch<>(y, set))}));

    // the expected solution (just one).
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("John")})
        };

    final PipelineJoinStats stats = query.newStats();

    final IAsynchronousIterator<IBindingSet[]> source =
        new ThickAsynchronousIterator<>(
            new IBindingSet[][]{new IBindingSet[]{new ListBindingSet()}});

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(1L, stats.unitsIn.get());
    assertEquals(1L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(1L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit test for a join which selects a subset of the variables to pass along.
   *
   * <p>Note: The order of the expected solutions for this test depends on the order of the keys
   * associated with the tuples in the relation. Since the key is [name,value], the result order is
   * based on this tuple order:
   *
   * <pre>
   * E("John", "Mary")
   * E("Leon", "Paul")
   * E("Mary", "John")
   * E("Mary", "Paul")
   * E("Paul", "Leon")
   * </pre>
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_join_selectVariables() {

    final Var<String> x = Var.var("x");
    final Var<String> y = Var.var("y");

    final int joinId = 2;
    final int predId = 3;

    final Predicate<E> predOp =
        new Predicate<>(
            new IVariableOrConstant[]{x, y},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{
                /*startOp*/
            },
            new NV(BOpBase.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp),
            new NV(PipelineJoin.Annotations.SELECT, new IVariable[]{y}));

    /*
     * The expected solutions.
     */
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("Mary")}),
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("Paul")}),
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("Paul")}),
          new ListBindingSet(
              new IVariable[] {Var.var("y")}, new IConstant[] {new Constant<>("Leon")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IAsynchronousIterator<IBindingSet[]> source =
        new ThickAsynchronousIterator<>(
            new IBindingSet[][]{new IBindingSet[]{new ListBindingSet()}});

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(1L, stats.unitsIn.get());
    assertEquals(5L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(1L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(5L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit tests for optional joins. For an optional join, an alternative sink may be specified for
   * the join. When specified, it is used if the join fails (if not specified, the binding sets
   * which do not join are forwarded to the primary sink). Binding sets which join go to the primary
   * sink regardless.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_optionalJoin() {

    final Var<?> x = Var.var("x");

    //        final int startId = 1;
    final int joinId = 2;
    final int predId = 3;

    //		final BOp startOp = new CopyOp(new BOp[] {}, NV.asMap(new NV[] {
    //				new NV(Predicate.Annotations.BOP_ID, startId),
    //				}));

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), x},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Predicate.Annotations.OPTIONAL, Boolean.TRUE),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(BOpBase.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, pred)
            //				new NV(PipelineJoin.Annotations.OPTIONAL, Boolean.TRUE)
        );

    /*
     * Setup the source with two initial binding sets. One has nothing bound
     * and will join with (Mary,x:=John) and (Mary,x:=Paul). The other has
     * x:=Luke which does not join. However, this is an optional join so
     * x:=Luke should output anyway.
     */
    final IAsynchronousIterator<IBindingSet[]> source;
    {
      final IBindingSet bset1 = new ListBindingSet();
      final IBindingSet bset2 = new ListBindingSet();
      {
        bset2.set(x, new Constant<>("Luke"));
      }
      source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{new IBindingSet[]{bset1, bset2}});
    }

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Paul")}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Luke")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(2L, stats.unitsIn.get());
    assertEquals(3L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(2L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit test for an optional {@link PipelineJoin} when the {@link BOpContext#getSink2()
   * alternative sink} is specified.
   *
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public void test_optionalJoin_withAltSink() {

    final Var<?> x = Var.var("x");

    //        final int startId = 1;
    final int joinId = 2;
    final int predId = 3;

    //		final BOp startOp = new CopyOp(new BOp[] {}, NV.asMap(new NV[] {
    //				new NV(Predicate.Annotations.BOP_ID, startId),
    //				}));

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), x},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Predicate.Annotations.OPTIONAL, Boolean.TRUE),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(BOpBase.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, pred)
            //				new NV(PipelineJoin.Annotations.OPTIONAL, Boolean.TRUE)
        );

    /*
     * Setup the source with two initial binding sets. One has nothing bound
     * and will join with (Mary,x:=John) and (Mary,x:=Paul). The other has
     * x:=Luke which does not join. However, this is an optional join so
     * x:=Luke should output anyway.
     */
    final IAsynchronousIterator<IBindingSet[]> source;
    {
      final IBindingSet bset1 = new ListBindingSet();
      final IBindingSet bset2 = new ListBindingSet();
      {
        bset2.set(x, new Constant<>("Luke"));
      }
      source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{new IBindingSet[]{bset1, bset2}});
    }

    // the expected solutions for the default sink.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("John")}),
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Paul")}),
        };

    // the expected solutions for the alternative sink.
    final IBindingSet[] expected2 =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Luke")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final IBlockingBuffer<IBindingSet[]> sink2 =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
            -1 /* partitionId */,
            stats,
            query /* op */,
            false /* lastInvocation */,
            source,
            sink,
            sink2);

    // get task.
    final FutureTask<Void> ft = query.eval(context);

    // execute task.
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutions(expected, sink.iterator(), ft);

    AbstractQueryEngineTestCase.assertSameSolutions(expected2, sink2.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(2L, stats.unitsIn.get());
    assertEquals(3L, stats.unitsOut.get());
    assertEquals(2L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(2L, stats.accessPathCount.get());
    assertEquals(1L, stats.accessPathChunksIn.get());
    assertEquals(2L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }

  /*
   * Unit tests for optional joins with a constraint. The constraint is applied to test each
   * solution which joins. Solutions which do not join, or which join but fail the constraint, are
   * passed along as "optional" solutions.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_optionalJoin_withConstraint() {

    final Var<?> x = Var.var("x");

    final int joinId = 2;
    final int predId = 3;

    final Predicate<E> pred =
        new Predicate<>(
            new IVariableOrConstant[]{new Constant<>("Mary"), x},
            NV.asMap(
                new NV(Predicate.Annotations.RELATION_NAME, new String[]{namespace}),
                new NV(Predicate.Annotations.BOP_ID, predId),
                new NV(Predicate.Annotations.OPTIONAL, Boolean.TRUE),
                new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED)));

    final PipelineJoin<E> query =
        new PipelineJoin<>(
            new BOp[]{}, // args
            new NV(BOpBase.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, pred),
            // constraint d != Paul
            new NV(
                PipelineJoin.Annotations.CONSTRAINTS,
                new IConstraint[]{
                    Constraint.wrap(new NEConstant(x, new Constant<>("Paul")))
                }));

    /*
     * Setup the source with three source binding sets.
     *
     * The first source solution joins with (Mary,x:=John) and
     * (Mary,x:=Paul). However, the join with (Mary,x:=Paul) is failed by
     * the constraint. Since we were joining with bset1 and since we also
     * have the (Mary,x:=John) solution for bset1, bset1 is NOT passed along
     * as an "optional" join.
     *
     * The next source solution has x:=Luke which does not join. However,
     * this is an optional join so x:=Luke is output anyway.
     *
     * The last source solution has x:=Paul. This will fail the constraint
     * in the join (x!=Paul). However, since nothing joins for this source
     * solution, the source solution is passed along as an optional
     * solution. Note that the constraint does NOT filter the optional
     * solution.
     */
    final IAsynchronousIterator<IBindingSet[]> source;
    {
      final IBindingSet bset1 = new ListBindingSet();

      final IBindingSet bset2 = new ListBindingSet();
      {
        bset2.set(x, new Constant<>("Luke"));
      }

      final IBindingSet bset3 = new ListBindingSet();
      {
        bset3.set(x, new Constant<>("Paul"));
      }

      source =
          new ThickAsynchronousIterator<>(
              new IBindingSet[][]{new IBindingSet[]{bset1, bset2, bset3}});
    }

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          // bset1 : one join passes the constraint, so no optionals.
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("John")}),
          // bset2 : join fails, but bset2 is output anyway as "optional".
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Luke")}),
          // bset3: join fails, but bset3 is  output anyway as "optional".
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<>("Paul")}),
        };

    final PipelineJoinStats stats = query.newStats();

    final IBlockingBuffer<IBindingSet[]> sink =
        new BlockingBufferWithStats<>(query, stats);

    final BOpContext<IBindingSet> context =
        new BOpContext<>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
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
    jnl.getExecutorService().execute(ft);

    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, sink.iterator(), ft);

    // join task
    assertEquals(1L, stats.chunksIn.get());
    assertEquals(3L, stats.unitsIn.get());
    assertEquals(3L, stats.unitsOut.get());
    assertEquals(1L, stats.chunksOut.get());
    // access path
    assertEquals(0L, stats.accessPathDups.get());
    assertEquals(3L, stats.accessPathCount.get());
    assertEquals(2L, stats.accessPathChunksIn.get());
    assertEquals(3L, stats.accessPathUnitsIn.get());

    //        assertTrue(ft.isDone());
    //        assertFalse(ft.isCancelled());
    //        ft.get(); // verify nothing thrown.

  }
}
