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

package org.embergraph.bop.ap;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.FutureTask;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.Predicate.Annotations;
import org.embergraph.bop.ap.filter.BOpResolver;
import org.embergraph.bop.ap.filter.BOpTupleFilter;
import org.embergraph.bop.ap.filter.DistinctFilter;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.MockRunningQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.io.SerializerUtil;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;
import org.embergraph.striterator.ChunkedArrayIterator;
import org.embergraph.striterator.IChunkedOrderedIterator;

/**
 * Unit test for reading on an access path using a {@link Predicate}. This unit test works through
 * the create and population of a test relation with some data and verifies the ability to access
 * that data using some different access paths. This sets the ground for testing the evaluation of
 * {@link Predicate}s with various constraints, filters, etc.
 *
 * <p>Note: Tests of remote access path reads are done in the context of a embergraph federation
 * since there must be a data service in play for a remote access path.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestPredicateAccessPath.java 3466 2010-08-27 14:28:04Z thompsonbry $
 * @todo test read-committed access paths.
 * @todo test read historical access paths.
 * @todo test unisolated (writable) access paths.
 * @todo test fully isolated access paths.
 */
public class TestPredicateAccessPath extends TestCase2 {

  /** */
  public TestPredicateAccessPath() {}

  /** @param name */
  public TestPredicateAccessPath(String name) {
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

  R rel;

  public void setUp() throws Exception {

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
    rel.insert(new ChunkedArrayIterator<E>(a.length, a, null /* keyOrder */));

    // Do commit since not scale-out.
    store.commit();

    // should exist as of the last commit point.
    this.rel = (R) jnl.getResourceLocator().locate(namespace, ITx.READ_COMMITTED);

    assertNotNull(rel);
  }

  public void tearDown() throws Exception {

    if (jnl != null) {
      jnl.destroy();
      jnl = null;
    }

    // clear reference.
    rel = null;
  }

  public void test_keyOrderSerializable() {

    SerializerUtil.serialize(R.primaryKeyOrder);
  }

  /**
   * Using a predicate with nothing bound, verify that we get the right range count on the relation
   * and that we read the correct elements from the relation.
   */
  public void test_nothingBound() {

    // nothing bound.
    final IAccessPath<E> accessPath =
        rel.getAccessPath(
            new Predicate<E>(
                new BOp[] {Var.var("name"), Var.var("value")},
                new NV(IPredicate.Annotations.RELATION_NAME, new String[] {namespace})));

    // verify the range count.
    assertEquals(5, accessPath.rangeCount(true /* exact */));

    // visit that access path, verifying the elements and order.
    if (log.isInfoEnabled()) log.info("accessPath=" + accessPath);
    final E[] expected =
        new E[] {
          new E("John", "Mary"), //
          new E("Leon", "Paul"), //
          new E("Mary", "John"),
          new E("Mary", "Paul"), //
          new E("Paul", "Leon"),
        };
    final IChunkedOrderedIterator<E> itr = accessPath.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /**
   * Using a predicate which binds the [name] position, verify that we get the right range count on
   * the relation and verify the actual element pulled back from the access path.
   */
  public void test_scan() {

    final IAccessPath<E> accessPath =
        rel.getAccessPath(
            new Predicate<E>(
                new IVariableOrConstant[] {new Constant<String>("Mary"), Var.var("value")},
                new NV(Predicate.Annotations.RELATION_NAME, new String[] {namespace})));

    // verify the range count.
    assertEquals(2, accessPath.rangeCount(true /* exact */));

    // visit that access path, verifying the elements and order.
    if (log.isInfoEnabled()) log.info("accessPath=" + accessPath);

    final E[] expected =
        new E[] {
          new E("Mary", "John"), //
          new E("Mary", "Paul"), //
        };

    final IChunkedOrderedIterator<E> itr = accessPath.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /** Verify lookup and read on an {@link IPredicate}. */
  public void test_predicate_eval() {

    final Predicate<E> pred =
        new Predicate<E>(
            new IVariableOrConstant[] {new Constant<String>("Mary"), Var.var("value")},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[] {namespace}),
                new NV(IPredicate.Annotations.TIMESTAMP, ITx.READ_COMMITTED),
                new NV(Annotations.REMOTE_ACCESS_PATH, false)));

    final E[] expected =
        new E[] {
          new E("Mary", "John"), //
          new E("Mary", "Paul"), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[0]);

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<IBindingSet[]>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<IBindingSet>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // obtain range count from the access path.
    assertEquals(2L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /**
   * Unit test for an {@link IPredicate.Annotations#INDEX_LOCAL_FILTER}.
   *
   * @todo test with synchronous and asynchronous iterators.
   * @todo test with exact range count (filter must be applied).
   */
  public void test_indexLocalFilter() {

    final IVariable<?> x = Var.var("x");
    final IVariable<?> y = Var.var("y");

    /*
     * Filter accepts iff name := "Mary".
     */
    final BOpTupleFilter<E> filter =
        new BOpTupleFilter<E>(
            new BOp[] {
              /* filters */
            },
            null /* annotations */) {
          private static final long serialVersionUID = 1L;

          @Override
          protected boolean isValid(ITuple<E> tuple) {
            return tuple.getObject().name.equals("Mary");
          }
        };

    final Predicate<E> pred =
        new Predicate<E>(
            new IVariableOrConstant[] {x, y},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[] {namespace}),
                new NV(IPredicate.Annotations.TIMESTAMP, ITx.READ_COMMITTED),
                new NV(Annotations.INDEX_LOCAL_FILTER, filter)));

    final E[] expected =
        new E[] {
          new E("Mary", "John"), //
          new E("Mary", "Paul"), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[0]);

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<IBindingSet[]>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<IBindingSet>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // obtain range count from the access path.
    assertEquals(2L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /** Filter strips off the 'value' column. */
  private static class ValueStripper extends BOpResolver {
    public ValueStripper(ValueStripper op) {
      super(op);
    }

    public ValueStripper(BOp[] args, Map<String, Object> annotations) {
      super(args, annotations);
    }

    private static final long serialVersionUID = 1L;

    @Override
    protected Object resolve(Object obj) {
      return new E(((E) obj).name, "");
    }
  }

  /**
   * Unit test for an {@link IPredicate.Annotations#ACCESS_PATH_FILTER}.
   *
   * @todo test with synchronous and asynchronous iterators.
   * @todo test with exact range count (filter must be applied).
   */
  public void test_accessPathFilter() {

    final IVariable<?> x = Var.var("x");
    final IVariable<?> y = Var.var("y");

    /*
     * Filter strips off the 'value' column.
     */
    final BOpResolver stripper =
        new ValueStripper(
            new BOp[] {
              /* filters */
            },
            null /* annotations */);

    /*
     * Filter imposes distinct on the visited elements. It chains the filter
     * to strip off the 'value' column first, then applies itself to filter
     * for just the distinct elements.
     */
    final DistinctFilter distinctFilter =
        new DistinctFilter(new BOp[] {stripper /* filters */}, null /* annotations */);

    final Predicate<E> pred =
        new Predicate<E>(
            new IVariableOrConstant[] {x, y},
            NV.asMap(
                new NV(Annotations.RELATION_NAME, new String[] {namespace}),
                new NV(IPredicate.Annotations.TIMESTAMP, ITx.READ_COMMITTED),
                new NV(Annotations.ACCESS_PATH_FILTER, distinctFilter)));

    // the distinct values from the name column in index order.
    final E[] expected =
        new E[] {
          new E("John", ""), //
          new E("Leon", ""), //
          new E("Mary", ""), //
          new E("Paul", ""), //
        };

    final BOpStats statIsIgnored = new BOpStats();

    final ICloseableIterator<IBindingSet[]> sourceIsIgnored =
        newBindingSetIterator(new IBindingSet[0]);

    final IBlockingBuffer<IBindingSet[]> sinkIsIgnored =
        new BlockingBuffer<IBindingSet[]>(1 /* capacity */);

    final PipelineOp mockQuery = new MockPipelineOp(BOp.NOARGS);

    final BOpContext<IBindingSet> context =
        new BOpContext<IBindingSet>(
            new MockRunningQuery(null /* fed */, jnl /* indexManager */),
            -1 /* partitionId */,
            statIsIgnored,
            mockQuery /* op */,
            false /* lastInvocation */,
            sourceIsIgnored,
            sinkIsIgnored,
            null /* sink2 */);

    // lookup relation
    final R relation = (R) context.getRelation(pred);

    // obtain access path for that relation.
    final IAccessPath<E> ap = context.getAccessPath(relation, pred);

    // obtain range count from the access path.
    assertEquals(4L, ap.rangeCount(true /* exact */));

    // verify the data visited by the access path.
    final IChunkedOrderedIterator<E> itr = ap.iterator();
    try {
      int n = 0;
      while (itr.hasNext()) {
        final E e = itr.next();
        if (log.isInfoEnabled()) log.info(n + " : " + e);
        assertEquals(expected[n], e);
        n++;
      }
      assertEquals(expected.length, n);
    } finally {
      itr.close();
    }
  }

  /**
   * Return an {@link IAsynchronousIterator} that will read the source {@link IBindingSet}s.
   *
   * @param bsets The source binding sets.
   */
  private static ThickAsynchronousIterator<IBindingSet[]> newBindingSetIterator(
      final IBindingSet[] bsets) {

    return new ThickAsynchronousIterator<IBindingSet[]>(new IBindingSet[][] {bsets});
  }

  protected class MockPipelineOp extends PipelineOp {

    public MockPipelineOp(final BOp[] args, final NV... anns) {

      super(args, NV.asMap(anns));
    }

    private static final long serialVersionUID = 1L;

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
      throw new UnsupportedOperationException();
    }
  }
}
