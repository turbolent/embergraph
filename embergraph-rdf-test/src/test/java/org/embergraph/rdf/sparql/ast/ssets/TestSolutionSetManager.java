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
 * Created on Apr 10, 2012
 */

package org.embergraph.rdf.sparql.ast.ssets;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.TestCase2;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVCache;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.sparql.ast.cache.CacheConnectionImpl;
import org.embergraph.rdf.sparql.ast.eval.IEvaluationContext;
import org.embergraph.striterator.CloseableIteratorWrapper;
import org.embergraph.striterator.Dechunkerator;

/*
 * Test suite for managing named solution sets.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSolutionSetManager extends TestCase2 {

  public TestSolutionSetManager() {}

  public TestSolutionSetManager(final String name) {
    super(name);
  }

  protected Journal journal;
  protected QueryEngine queryEngine;
  //    protected ICacheConnection cacheConn;
  protected ISolutionSetManager solutionSetsManager;

  /** Note: Not used yet by the {@link CacheConnectionImpl}. */
  protected IEvaluationContext ctx = null;

  /** The namespace for the {@link EmbergraphValueFactory}. */
  protected String namespace = getName();

  /** The value factory for that namespace. */
  protected EmbergraphValueFactory valueFactory = EmbergraphValueFactoryImpl.getInstance(namespace);

  /** A {@link TermId} whose {@link IVCache} is set. */
  protected TermId<EmbergraphLiteral> termId;

  /** A {@link TermId} whose {@link IVCache} is set. */
  protected TermId<EmbergraphLiteral> termId2;

  /** A {@link BlobIV} whose {@link IVCache} is set. */
  protected BlobIV<EmbergraphLiteral> blobIV;

  /** A "mockIV". */
  protected TermId<EmbergraphValue> mockIV1;

  /** A "mockIV". */
  protected TermId<EmbergraphValue> mockIV2;

  /** A "mockIV". */
  protected TermId<EmbergraphValue> mockIV3;

  /** An inline IV whose {@link IVCache} is set. */
  protected XSDIntegerIV<EmbergraphLiteral> inlineIV;

  /** An inline IV whose {@link IVCache} is NOT set. */
  protected IV<?, ?> inlineIV2;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // Setup the backing Journal.
    {
      final Properties properties = new Properties();
      properties.put(Journal.Options.BUFFER_MODE, BufferMode.MemStore.toString());
      properties.put(Journal.Options.CREATE_TEMP_FILE, "true");
      properties.put(Journal.Options.INITIAL_EXTENT, "" + Journal.Options.minimumInitialExtent);
      journal = new Journal(properties);
    }

    // Setup the QueryEngine.
    queryEngine = QueryEngineFactory.getInstance().getQueryController(journal);

    //        // Setup the solution set cache.
    //        cacheConn = CacheConnectionFactory.getCacheConnection(queryEngine);

    // TODO MVCC VIEWS: This is ONLY using the UNISOLATED VIEW.  Test MVCC semantics.
    solutionSetsManager = new SolutionSetManager(journal, namespace, ITx.UNISOLATED);

    /*
     * Declare some IVs.
     */
    termId = new TermId<>(VTE.LITERAL, 12 /* termId */);
    termId.setValue(valueFactory.createLiteral("abc"));

    termId2 = new TermId<>(VTE.LITERAL, 36 /* termId */);
    termId2.setValue(valueFactory.createLiteral("xyz"));

    blobIV =
        new BlobIV<>(
            VTE.LITERAL, 912 /* hash */, (short) 0 /* collisionCounter */);
    blobIV.setValue(valueFactory.createLiteral("bigfoo"));

    mockIV1 = (TermId) TermId.mockIV(VTE.LITERAL);
    mockIV1.setValue(valueFactory.createLiteral("red"));

    mockIV2 = (TermId) TermId.mockIV(VTE.LITERAL);
    mockIV2.setValue(valueFactory.createLiteral("blue"));

    mockIV3 = (TermId) TermId.mockIV(VTE.LITERAL);
    mockIV3.setValue(valueFactory.createLiteral("green"));

    inlineIV = new XSDIntegerIV<>(BigInteger.valueOf(100));
    inlineIV.setValue(valueFactory.createLiteral("100", XSD.INTEGER));

    inlineIV2 = XSDBooleanIV.valueOf(true);
  }

  @Override
  protected void tearDown() throws Exception {

    if (solutionSetsManager != null) {
      solutionSetsManager.close();
      solutionSetsManager = null;
    }

    //        if (cacheConn != null) {
    //            cacheConn.close();
    //            cacheConn = null;
    //        }

    if (queryEngine != null) {
      queryEngine.shutdownNow();
      queryEngine = null;
    }

    if (journal != null) {
      journal.destroy();
      journal = null;
    }

    // Clear references.
    valueFactory.remove();
    valueFactory = null;
    namespace = null;
    termId = termId2 = null;
    blobIV = null;
    mockIV1 = mockIV2 = mockIV3 = null;
    inlineIV = null;
    inlineIV2 = null;

    super.tearDown();
  }

  /** Unit test for saving an empty named solution set and then reading it back. */
  public void test_putGet() {

    /*
     * Setup the source solution set chunks.
     */
    final List<IBindingSet[]> in = new LinkedList<>();
    {
      final List<IBindingSet> t = new LinkedList<>();

      // An empty binding set.
      t.add(new ListBindingSet());

      in.add(t.toArray(new IBindingSet[0]));
    }

    final String solutionSet = getName();

    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    assertFalse(solutionSetsManager.existsSolutions(solutionSet));

    solutionSetsManager.putSolutions(
        solutionSet, new CloseableIteratorWrapper<>(in.iterator()));

    assertTrue(solutionSetsManager.existsSolutions(solutionSet));

    final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

    assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
  }

  /*
   * Unit test for saving a two empty solutions into a named solution set and then reading it back.
   */
  public void test_putGet2() {

    /*
     * Setup the source solution set chunks.
     */
    final List<IBindingSet[]> in = new LinkedList<>();
    {
      final List<IBindingSet> t = new LinkedList<>();

      // An empty binding set.
      {
        final ListBindingSet b = new ListBindingSet();
        t.add(b);
      }

      // Another empty binding set.
      {
        final ListBindingSet b = new ListBindingSet();
        t.add(b);
      }

      in.add(t.toArray(new IBindingSet[0]));
    }

    final String solutionSet = getName();

    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    solutionSetsManager.putSolutions(
        solutionSet, new CloseableIteratorWrapper<>(in.iterator()));

    final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

    assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
  }

  /*
   * Unit test for saving some non-empty solutions into a named solution set and then reading it
   * back.
   */
  @SuppressWarnings("rawtypes")
  public void test_putGet3() {

    /*
     * Setup the source solution set chunks.
     */
    final List<IBindingSet[]> in = new LinkedList<>();
    {
      final IVariable<?> x = Var.var("x");
      final IVariable<?> y = Var.var("y");
      final IVariable<?> z = Var.var("z");

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId));
          b.set(y, new Constant<IV>(termId2));
          t.add(b);
        }

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId2));
          b.set(y, new Constant<IV>(inlineIV));
          b.set(z, new Constant<IV>(blobIV));
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }
    }

    final String solutionSet = getName();

    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    solutionSetsManager.putSolutions(
        solutionSet, new CloseableIteratorWrapper<>(in.iterator()));

    final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

    assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
  }

  /*
   * Unit test for saving some non-empty solutions in multiple chunks into a named solution set and
   * then reading it back.
   */
  @SuppressWarnings("rawtypes")
  public void test_putGet4() {

    /*
     * Setup the source solution set chunks.
     */
    final List<IBindingSet[]> in = new LinkedList<>();
    {
      final IVariable<?> x = Var.var("x");
      final IVariable<?> y = Var.var("y");
      final IVariable<?> z = Var.var("z");

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId2));
          b.set(y, new Constant<IV>(inlineIV));
          b.set(z, new Constant<IV>(blobIV));
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId));
          b.set(y, new Constant<IV>(termId2));
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }
    }

    final String solutionSet = getName();

    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    solutionSetsManager.putSolutions(
        solutionSet, new CloseableIteratorWrapper<>(in.iterator()));

    final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

    assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
  }

  /** Unit test for clearing a named solution set. */
  @SuppressWarnings("rawtypes")
  public void test_clearSolutionSet() {

    /*
     * Setup the source solution set chunks.
     */
    final List<IBindingSet[]> in = new LinkedList<>();
    {
      final IVariable<?> x = Var.var("x");
      final IVariable<?> y = Var.var("y");
      final IVariable<?> z = Var.var("z");

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId2));
          b.set(y, new Constant<IV>(inlineIV));
          b.set(z, new Constant<IV>(blobIV));
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          b.set(x, new Constant<IV>(termId));
          b.set(y, new Constant<IV>(termId2));
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }

      {
        final List<IBindingSet> t = new LinkedList<>();

        {
          final ListBindingSet b = new ListBindingSet();
          t.add(b);
        }

        in.add(t.toArray(new IBindingSet[0]));
      }
    }

    final String solutionSet = getName();

    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // write the solution set.
    solutionSetsManager.putSolutions(
        solutionSet, new CloseableIteratorWrapper<>(in.iterator()));

    // read them back
    {
      final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

      assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
    }

    // read them back again.
    {
      final ICloseableIterator<IBindingSet[]> out = solutionSetsManager.getSolutions(solutionSet);

      assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
    }

    // Clear the solution set.
    solutionSetsManager.clearSolutions(solutionSet);

    // Verify gone.
    try {
      solutionSetsManager.getSolutions(solutionSet);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }

  /** Unit test for clearing all named solution sets. */
  public void test_clearAllSolutionSets() {

    final List<IBindingSet[]> in1 = new LinkedList<>();
    {
      final List<IBindingSet> t = new LinkedList<>();

      {
        final ListBindingSet b = new ListBindingSet();
        t.add(b);
      }

      in1.add(t.toArray(new IBindingSet[0]));
    }

    final List<IBindingSet[]> in2 = new LinkedList<>();
    {
      final List<IBindingSet> t = new LinkedList<>();

      {
        final ListBindingSet b = new ListBindingSet();
        t.add(b);
      }

      {
        final ListBindingSet b = new ListBindingSet();
        t.add(b);
      }

      in1.add(t.toArray(new IBindingSet[0]));
    }

    final String solutionSet1 = getName() + 1;
    final String solutionSet2 = getName() + 2;

    try {
      solutionSetsManager.getSolutions(solutionSet1);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    try {
      solutionSetsManager.getSolutions(solutionSet2);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // write the solution sets.
    solutionSetsManager.putSolutions(
        solutionSet1, new CloseableIteratorWrapper<>(in1.iterator()));
    solutionSetsManager.putSolutions(
        solutionSet2, new CloseableIteratorWrapper<>(in2.iterator()));

    // read them back
    assertSameSolutionsAnyOrder(
        flatten(in1.iterator()), solutionSetsManager.getSolutions(solutionSet1));
    assertSameSolutionsAnyOrder(
        flatten(in2.iterator()), solutionSetsManager.getSolutions(solutionSet2));

    // Clear all named solution set.
    solutionSetsManager.clearAllSolutions();

    //        ((CacheConnectionImpl)cacheConn).getStore().commit();

    // Verify gone.
    try {
      final ICloseableIterator<IBindingSet[]> itr = solutionSetsManager.getSolutions(solutionSet1);
      try {
        assertFalse(itr.hasNext());
      } finally {
        itr.close();
      }
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
    assertFalse(solutionSetsManager.existsSolutions(solutionSet1));

    // Verify gone.
    try {
      solutionSetsManager.getSolutions(solutionSet2);
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
    assertFalse(solutionSetsManager.existsSolutions(solutionSet2));
  }

  /*
   * Flatten out the iterator into a single chunk.
   *
   * @param itr An iterator visiting solution chunks.
   * @return A flat chunk of solutions.
   */
  protected static IBindingSet[] flatten(final Iterator<IBindingSet[]> itr) {

    try {

      final List<IBindingSet> t = new LinkedList<>();

      while (itr.hasNext()) {

        final IBindingSet[] a = itr.next();

        t.addAll(Arrays.asList(a));
      }

      return t.toArray(new IBindingSet[0]);

    } finally {

      if (itr instanceof ICloseableIterator) {

        ((ICloseableIterator<?>) itr).close();
      }
    }
  }

  protected static void assertSameSolutionsAnyOrder(
      final IBindingSet[] expected, final ICloseableIterator<IBindingSet[]> itr) {

    assertSameSolutionsAnyOrder("", expected, itr);
  }

  protected static void assertSameSolutionsAnyOrder(
      final String msg, final IBindingSet[] expected, final ICloseableIterator<IBindingSet[]> itr) {

    try {

      final Iterator<IBindingSet> actual = new Dechunkerator<>(itr);

      /*
       * Populate a map that we will use to realize the match and
       * selection without replacement logic. The map uses counters to
       * handle duplicate keys. This makes it possible to write tests in
       * which two or more binding sets which are "equal" appear.
       */

      final int nrange = expected.length;

      final java.util.Map<IBindingSet, AtomicInteger> range =
          new java.util.LinkedHashMap<>();

      for (IBindingSet iBindingSet : expected) {

        AtomicInteger count = range.get(iBindingSet);

        if (count == null) {

          count = new AtomicInteger();
        }

        range.put(iBindingSet, count);

        count.incrementAndGet();
      }

      // Do selection without replacement for the objects visited by
      // iterator.

      for (int j = 0; j < nrange; j++) {

        if (!actual.hasNext()) {

          //                    if(runningQuery.isDone()) runningQuery.get();

          fail(msg + ": Iterator exhausted while expecting more object(s)" + ": index=" + j);
        }

        //                if(runningQuery.isDone()) runningQuery.get();

        final IBindingSet actualObject = actual.next();

        //                if(runningQuery.isDone()) runningQuery.get();

        if (log.isInfoEnabled()) log.info("visting: " + actualObject);

        final AtomicInteger counter = range.get(actualObject);

        if (counter == null || counter.get() == 0) {

          fail("Object not expected" + ": index=" + j + ", object=" + actualObject);
        }

        counter.decrementAndGet();
      }

      if (actual.hasNext()) {

        fail("Iterator will deliver too many objects.");
      }

      //            // The query should be done. Check its Future.
      //            runningQuery.get();
      //
      //        } catch (InterruptedException ex) {
      //
      //            throw new RuntimeException("Query evaluation was interrupted: "
      //                    + ex, ex);
      //
      //        } catch(ExecutionException ex) {
      //
      //            throw new RuntimeException("Error during query evaluation: " + ex,
      //                    ex);

    } finally {

      itr.close();
    }
  }
}
