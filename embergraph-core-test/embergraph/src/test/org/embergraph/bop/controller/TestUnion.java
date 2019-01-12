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
 * Created on Sep 2, 2010
 */

package org.embergraph.bop.controller;

import java.util.Properties;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.E;
import org.embergraph.bop.ap.R;
import org.embergraph.bop.bindingSet.EmptyBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.bset.StartOp;
import org.embergraph.bop.engine.AbstractQueryEngineTestCase;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.engine.StandaloneChunkHandler;
import org.embergraph.bop.solutions.SliceOp;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.striterator.ChunkedArrayIterator;

/**
 * Test suite for {@link Union}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestUnionBindingSets.java 3500 2010-09-03 00:27:45Z thompsonbry $
 */
public class TestUnion extends TestCase2 {

  /** */
  public TestUnion() {}

  /** @param name */
  public TestUnion(String name) {
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
  protected void setUp() throws Exception {

    jnl = new Journal(getProperties());

    loadData(jnl);

    queryEngine = new QueryEngine(jnl);

    queryEngine.init();
  }

  /** Create and populate relation in the {@link #namespace}. */
  private void loadData(final Journal store) {

    final String namespace = "ns";

    // create the relation.
    final R rel = new R(store, namespace, ITx.UNISOLATED, new Properties());
    rel.create();

    // data to insert (in key order for convenience).
    final E[] a = {
      new E("John", "Mary"), // [0]
      new E("Leon", "Paul"), // [1]
      new E("Mary", "Paul"), // [2]
      new E("Paul", "Leon"), // [3]
    };

    // insert data (the records are not pre-sorted).
    rel.insert(new ChunkedArrayIterator<E>(a.length, a, null /* keyOrder */));

    // Do commit since not scale-out.
    store.commit();
  }

  @Override
  protected void tearDown() throws Exception {

    if (queryEngine != null) {
      queryEngine.shutdownNow();
      queryEngine = null;
    }

    if (jnl != null) {
      jnl.destroy();
      jnl = null;
    }
  }

  /**
   * Verifies that the UNION of two operators is computed. The operators do not route around the
   * UNION, so their solutions are copied to the UNION and from the UNION to onto the top-level
   * query buffer. For this test variant, both subqueries run using their default inputs, which is a
   * single empty binding set each. This gives us two empty binding sets as the output of the union.
   *
   * @throws Exception
   */
  public void test_union_defaultInputs() throws Exception {

    final int startId1 = 1;
    final int startId2 = 2;
    final int unionId = 3;

    final BOp startOp1 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId1),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER)));

    final BOp startOp2 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId2),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER)));

    final BOp unionOp =
        new Union(
            new BOp[0],
            NV.asMap(
                new NV(Union.Annotations.BOP_ID, unionId),
                new NV(Union.Annotations.SUBQUERIES, new BOp[] {startOp1, startOp2}),
                new NV(
                    QueryEngine.Annotations.CHUNK_HANDLER, StandaloneChunkHandler.TEST_INSTANCE),
                //                        new NV(Union.Annotations.EVALUATION_CONTEXT,
                //                                BOpEvaluationContext.CONTROLLER),
                //                        new NV(Union.Annotations.CONTROLLER, true),
            ));

    final BOp query = unionOp;

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          EmptyBindingSet.INSTANCE, EmptyBindingSet.INSTANCE,
        };

    final IRunningQuery runningQuery = queryEngine.eval(query);

    // verify solutions.
    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, runningQuery);
  }

  public void test_union_consumesSource() throws Exception {

    final int startId1 = 1;
    final int startId2 = 2;
    final int unionId = 3;

    final BOp startOp1 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId1),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER)));

    final BOp startOp2 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId2),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER)));

    final BOp unionOp =
        new Union(
            new BOp[] {},
            NV.asMap(
                new NV(Union.Annotations.BOP_ID, unionId),
                new NV(Union.Annotations.SUBQUERIES, new BOp[] {startOp1, startOp2}),
                new NV(
                    QueryEngine.Annotations.CHUNK_HANDLER, StandaloneChunkHandler.TEST_INSTANCE),
                //                        new NV(Union.Annotations.EVALUATION_CONTEXT,
                //                                BOpEvaluationContext.CONTROLLER),
                //                        new NV(Union.Annotations.CONTROLLER, true),
            ));

    final BOp query = unionOp;

    /*
     * Create an initial non-empty binding set.
     */
    final IBindingSet bset = new ListBindingSet();
    bset.set(Var.var("x"), new Constant<String>("John"));
    bset.set(Var.var("y"), new Constant<String>("Mary"));

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          bset, // one copy from the left side of the union.
          bset, // one copy from the right side of the union.
        };

    final IRunningQuery runningQuery = queryEngine.eval(query, bset);

    // verify solutions.
    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, runningQuery);
  }

  /**
   * Verifies that the UNION of two operators is computed.
   *
   * @throws Exception
   */
  public void test_union() throws Exception {

    final int startId1 = 1;
    final int startId2 = 2;
    final int unionId = 3;
    final int sliceId = 4;

    final IVariable<?> x = Var.var("x");
    final IVariable<?> y = Var.var("y");

    final IBindingSet[] bindingSets1 = new IBindingSet[1];
    {
      final IBindingSet tmp = new ListBindingSet();
      tmp.set(x, new Constant<String>("Leon"));
      bindingSets1[0] = tmp;
    }

    final IBindingSet[] bindingSets2 = new IBindingSet[1];
    {
      final IBindingSet tmp = new ListBindingSet();
      tmp.set(x, new Constant<String>("Mary"));
      tmp.set(y, new Constant<String>("John"));
      bindingSets2[0] = tmp;
    }

    final BOp startOp1 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId1),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(StartOp.Annotations.BINDING_SETS, bindingSets1)));

    final BOp startOp2 =
        new StartOp(
            new BOp[] {},
            NV.asMap(
                new NV(StartOp.Annotations.BOP_ID, startId2),
                new NV(StartOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(StartOp.Annotations.BINDING_SETS, bindingSets2)));

    final BOp unionOp =
        new Union(
            new BOp[] {},
            NV.asMap(
                new NV(Union.Annotations.BOP_ID, unionId),
                new NV(Union.Annotations.SUBQUERIES, new BOp[] {startOp1, startOp2})
                //                        new NV(Union.Annotations.EVALUATION_CONTEXT,
                //                                BOpEvaluationContext.CONTROLLER),
                //                        new NV(Union.Annotations.CONTROLLER, true),
            ));

    final BOp sliceOp =
        new SliceOp(
            new BOp[] {unionOp},
            NV.asMap(
                new NV(Union.Annotations.BOP_ID, sliceId),
                new NV(Union.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(PipelineOp.Annotations.SHARED_STATE, true),
                new NV(PipelineOp.Annotations.REORDER_SOLUTIONS, false),
                new NV(
                    QueryEngine.Annotations.CHUNK_HANDLER, StandaloneChunkHandler.TEST_INSTANCE)));

    final BOp query = sliceOp;

    // the expected solutions.
    final IBindingSet[] expected =
        new IBindingSet[] {
          new ListBindingSet(new IVariable[] {x}, new IConstant[] {new Constant<String>("Leon")}),
          new ListBindingSet(
              new IVariable[] {x, y},
              new IConstant[] {new Constant<String>("Mary"), new Constant<String>("John")}),
        };

    final IRunningQuery runningQuery = queryEngine.eval(query);

    // verify solutions.
    AbstractQueryEngineTestCase.assertSameSolutionsAnyOrder(expected, runningQuery);
  }
}
