/**

Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

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

package com.bigdata.rdf.sparql.ast.eval.cache;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase2;

import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.Var;
import com.bigdata.bop.bindingSet.ListBindingSet;
import com.bigdata.bop.engine.QueryEngine;
import com.bigdata.bop.fed.QueryEngineFactory;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.Journal;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.IVCache;
import com.bigdata.rdf.internal.VTE;
import com.bigdata.rdf.internal.XSD;
import com.bigdata.rdf.internal.impl.BlobIV;
import com.bigdata.rdf.internal.impl.TermId;
import com.bigdata.rdf.internal.impl.literal.XSDBooleanIV;
import com.bigdata.rdf.internal.impl.literal.XSDIntegerIV;
import com.bigdata.rdf.model.BigdataLiteral;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.rdf.model.BigdataValueFactory;
import com.bigdata.rdf.model.BigdataValueFactoryImpl;
import com.bigdata.rdf.sparql.ast.cache.ISparqlCache;
import com.bigdata.rdf.sparql.ast.cache.SparqlCache;
import com.bigdata.rdf.sparql.ast.cache.SparqlCacheFactory;
import com.bigdata.rdf.sparql.ast.eval.IEvaluationContext;
import com.bigdata.striterator.CloseableIteratorWrapper;
import com.bigdata.striterator.Dechunkerator;
import com.bigdata.striterator.ICloseableIterator;

/**
 * Test suite for managing named solution sets.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 *          TODO We should actually be verifying the order for those solution
 *          sets where the maintenance of the order is part of their CREATE
 *          declaration.
 */
public class TestSolutionSetCache extends TestCase2 {

    public TestSolutionSetCache() {
    }

    public TestSolutionSetCache(final String name) {
        super(name);
    }

    protected Journal journal;
    protected QueryEngine queryEngine;
    protected ISparqlCache cache;
    
    /** Note: Not used yet by the {@link SparqlCache}.
     */
    protected IEvaluationContext ctx = null;

    /**
     * The namespace for the {@link BigdataValueFactory}.
     * 
     */
    protected String namespace = getName();

    /**
     * The value factory for that namespace.
     */
    protected BigdataValueFactory valueFactory = BigdataValueFactoryImpl
            .getInstance(namespace);

    /**
     * A {@link TermId} whose {@link IVCache} is set.
     */
    protected TermId<BigdataLiteral> termId;

    /**
     * A {@link TermId} whose {@link IVCache} is set.
     */
    protected TermId<BigdataLiteral> termId2;

    /**
     * A {@link BlobIV} whose {@link IVCache} is set.
     */
    protected BlobIV<BigdataLiteral> blobIV;

    /** A "mockIV". */
    protected TermId<BigdataValue> mockIV1;

    /** A "mockIV". */
    protected TermId<BigdataValue> mockIV2;

    /** A "mockIV". */
    protected TermId<BigdataValue> mockIV3;
    
    /** An inline IV whose {@link IVCache} is set. */
    protected XSDIntegerIV<BigdataLiteral> inlineIV;

    /** An inline IV whose {@link IVCache} is NOT set. */
    protected IV<?,?> inlineIV2;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Setup the backing Journal.
        {
            final Properties properties = new Properties();
            properties.put(Journal.Options.BUFFER_MODE,
                    BufferMode.MemStore.toString());
            properties.put(Journal.Options.CREATE_TEMP_FILE, "true");
            properties.put(Journal.Options.INITIAL_EXTENT, ""
                    + Journal.Options.minimumInitialExtent);
            journal = new Journal(properties);
        }
        
        // Setup the QueryEngine.
        queryEngine = QueryEngineFactory.getQueryController(journal);
        
        // Setup the solution set cache.
        cache = SparqlCacheFactory.getSparqlCache(queryEngine);

        /*
         * Declare some IVs.
         */
        termId = new TermId<BigdataLiteral>(VTE.LITERAL, 12/* termId */);
        termId.setValue(valueFactory.createLiteral("abc"));

        termId2 = new TermId<BigdataLiteral>(VTE.LITERAL, 36/* termId */);
        termId2.setValue(valueFactory.createLiteral("xyz"));

        blobIV = new BlobIV<BigdataLiteral>(VTE.LITERAL, 912/* hash */,
                (short) 0/* collisionCounter */);
        blobIV.setValue(valueFactory.createLiteral("bigfoo"));
        
        mockIV1 = (TermId) TermId.mockIV(VTE.LITERAL);
        mockIV1.setValue((BigdataValue) valueFactory.createLiteral("red"));

        mockIV2 = (TermId) TermId.mockIV(VTE.LITERAL);
        mockIV2.setValue((BigdataValue) valueFactory.createLiteral("blue"));

        mockIV3 = (TermId) TermId.mockIV(VTE.LITERAL);
        mockIV3.setValue((BigdataValue) valueFactory.createLiteral("green"));

        inlineIV = new XSDIntegerIV<BigdataLiteral>(BigInteger.valueOf(100));
        inlineIV.setValue((BigdataLiteral) valueFactory.createLiteral("100",
                XSD.INTEGER));
        
        inlineIV2 = XSDBooleanIV.TRUE;
    }

    @Override
    protected void tearDown() throws Exception {

        if (cache != null) {
            cache.close();
            cache = null;
        }
        
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
    
    /**
     * Unit test for saving an empty named solution set and then reading it
     * back.
     */
    public void test_putGet() {

        /*
         * Setup the source solution set chunks.
         */
        final List<IBindingSet[]> in = new LinkedList<IBindingSet[]>();
        {
            
            final List<IBindingSet> t = new LinkedList<IBindingSet>();
            
            // An empty binding set.
            t.add(new ListBindingSet());
            
            in.add(t.toArray(new IBindingSet[0]));
            
        }

        final String solutionSet = getName();

        try {
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        cache.putSolutions(ctx, solutionSet,
                new CloseableIteratorWrapper<IBindingSet[]>(in.iterator()));

        final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(ctx,
                solutionSet);

        assertSameSolutionsAnyOrder(flatten(in.iterator()), out);

    }

    /**
     * Unit test for saving a two empty solutions into a named solution set and
     * then reading it back.
     */
    public void test_putGet2() {

        /*
         * Setup the source solution set chunks.
         */
        final List<IBindingSet[]> in = new LinkedList<IBindingSet[]>();
        {
            final List<IBindingSet> t = new LinkedList<IBindingSet>();
            
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
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        cache.putSolutions(ctx, solutionSet,
                new CloseableIteratorWrapper<IBindingSet[]>(in.iterator()));

        final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(ctx,
                solutionSet);

        assertSameSolutionsAnyOrder(flatten(in.iterator()), out);

    }

    /**
     * Unit test for saving some non-empty solutions into a named solution set
     * and then reading it back.
     */
    @SuppressWarnings("rawtypes")
    public void test_putGet3() {

        /*
         * Setup the source solution set chunks.
         */
        final List<IBindingSet[]> in = new LinkedList<IBindingSet[]>();
        {
            final IVariable<?> x = Var.var("x");
            final IVariable<?> y = Var.var("y");
            final IVariable<?> z = Var.var("z");

            {
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

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
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        cache.putSolutions(ctx, solutionSet,
                new CloseableIteratorWrapper<IBindingSet[]>(in.iterator()));

        final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(ctx,
                solutionSet);

        assertSameSolutionsAnyOrder(flatten(in.iterator()), out);

    }

    /**
     * Unit test for saving some non-empty solutions in multiple chunks into a
     * named solution set and then reading it back.
     */
    @SuppressWarnings("rawtypes")
    public void test_putGet4() {

        /*
         * Setup the source solution set chunks.
         */
        final List<IBindingSet[]> in = new LinkedList<IBindingSet[]>();
        {
            final IVariable<?> x = Var.var("x");
            final IVariable<?> y = Var.var("y");
            final IVariable<?> z = Var.var("z");

            {
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

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
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

                {
                    final ListBindingSet b = new ListBindingSet();
                    b.set(x, new Constant<IV>(termId));
                    b.set(y, new Constant<IV>(termId2));
                    t.add(b);
                }

                in.add(t.toArray(new IBindingSet[0]));
            }

            {
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

                {
                    final ListBindingSet b = new ListBindingSet();
                    t.add(b);
                }

                in.add(t.toArray(new IBindingSet[0]));
            }
            
        }

        final String solutionSet = getName();

        try {
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        cache.putSolutions(ctx, solutionSet,
                new CloseableIteratorWrapper<IBindingSet[]>(in.iterator()));

        final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(ctx,
                solutionSet);

        assertSameSolutionsAnyOrder(flatten(in.iterator()), out);

    }

    /**
     * Unit test for clearing a named solution set.
     */
    @SuppressWarnings("rawtypes")
    public void test_clearSolutionSet() {

        /*
         * Setup the source solution set chunks.
         */
        final List<IBindingSet[]> in = new LinkedList<IBindingSet[]>();
        {
            final IVariable<?> x = Var.var("x");
            final IVariable<?> y = Var.var("y");
            final IVariable<?> z = Var.var("z");

            {
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

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
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

                {
                    final ListBindingSet b = new ListBindingSet();
                    b.set(x, new Constant<IV>(termId));
                    b.set(y, new Constant<IV>(termId2));
                    t.add(b);
                }

                in.add(t.toArray(new IBindingSet[0]));
            }

            {
                final List<IBindingSet> t = new LinkedList<IBindingSet>();

                {
                    final ListBindingSet b = new ListBindingSet();
                    t.add(b);
                }

                in.add(t.toArray(new IBindingSet[0]));
            }
            
        }

        final String solutionSet = getName();

        try {
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        // write the solution set.
        cache.putSolutions(ctx, solutionSet,
                new CloseableIteratorWrapper<IBindingSet[]>(in.iterator()));

        // read them back
        {
            final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(
                    ctx, solutionSet);

            assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
        }

        // read them back again.
        {
            final ICloseableIterator<IBindingSet[]> out = cache.getSolutions(
                    ctx, solutionSet);

            assertSameSolutionsAnyOrder(flatten(in.iterator()), out);
        }
        
        // Clear the solution set.
        cache.clearSolutions(ctx, solutionSet);

        // Verify gone.
        try {
            cache.getSolutions(ctx, solutionSet);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

    }


    /**
     * Unit test for clearing all named solution sets.
     */
    public void test_clearAllSolutionSets() {

        final List<IBindingSet[]> in1 = new LinkedList<IBindingSet[]>();
        {

            final List<IBindingSet> t = new LinkedList<IBindingSet>();

            {
                final ListBindingSet b = new ListBindingSet();
                t.add(b);
            }

            in1.add(t.toArray(new IBindingSet[0]));
        }

        final List<IBindingSet[]> in2 = new LinkedList<IBindingSet[]>();
        {

            final List<IBindingSet> t = new LinkedList<IBindingSet>();

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
            cache.getSolutions(ctx, solutionSet1);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        try {
            cache.getSolutions(ctx, solutionSet2);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        // write the solution sets.
        cache.putSolutions(ctx, solutionSet1,
                new CloseableIteratorWrapper<IBindingSet[]>(in1.iterator()));
        cache.putSolutions(ctx, solutionSet2,
                new CloseableIteratorWrapper<IBindingSet[]>(in2.iterator()));

        // read them back
        assertSameSolutionsAnyOrder(flatten(in1.iterator()),
                cache.getSolutions(ctx, solutionSet1));
        assertSameSolutionsAnyOrder(flatten(in2.iterator()),
                cache.getSolutions(ctx, solutionSet2));

        // Clear all named solution set.
        cache.clearAllSolutions(ctx);

        // Verify gone.
        try {
            cache.getSolutions(ctx, solutionSet1);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

        // Verify gone.
        try {
            cache.getSolutions(ctx, solutionSet2);
            fail("Expecting: " + IllegalStateException.class);
        } catch (IllegalStateException ex) {
            if (log.isInfoEnabled())
                log.info("Ignoring expected exception: " + ex);
        }

    }

    /**
     * Flatten out the iterator into a single chunk.
     * 
     * @param itr
     *            An iterator visiting solution chunks.
     *            
     * @return A flat chunk of solutions.
     */
    static protected IBindingSet[] flatten(final Iterator<IBindingSet[]> itr) {

        try {

            final List<IBindingSet> t = new LinkedList<IBindingSet>();

            while (itr.hasNext()) {

                final IBindingSet[] a = itr.next();
                
                for(IBindingSet b : a) {
                    
                    t.add(b);
                    
                }
                
            }

            return t.toArray(new IBindingSet[t.size()]);

        } finally {
            
            if (itr instanceof ICloseableIterator) {
            
                ((ICloseableIterator<?>) itr).close();
                
            }
            
        }

    }

    static protected void assertSameSolutionsAnyOrder(
            final IBindingSet[] expected,
            final ICloseableIterator<IBindingSet[]> itr) {
    
        assertSameSolutionsAnyOrder("", expected, itr);
        
    }
    
    static protected void assertSameSolutionsAnyOrder(final String msg,
            final IBindingSet[] expected,
            final ICloseableIterator<IBindingSet[]> itr) {
    
        try {
    
            final Iterator<IBindingSet> actual = new Dechunkerator<IBindingSet>(
                    itr);
    
            /*
             * Populate a map that we will use to realize the match and
             * selection without replacement logic. The map uses counters to
             * handle duplicate keys. This makes it possible to write tests in
             * which two or more binding sets which are "equal" appear.
             */
    
            final int nrange = expected.length;
    
            final java.util.Map<IBindingSet, AtomicInteger> range = new java.util.LinkedHashMap<IBindingSet, AtomicInteger>();
    
            for (int j = 0; j < nrange; j++) {
    
                AtomicInteger count = range.get(expected[j]);
    
                if (count == null) {
    
                    count = new AtomicInteger();
    
                }
    
                range.put(expected[j], count);
    
                count.incrementAndGet();
                
            }
    
            // Do selection without replacement for the objects visited by
            // iterator.
    
            for (int j = 0; j < nrange; j++) {
    
                if (!actual.hasNext()) {
    
//                    if(runningQuery.isDone()) runningQuery.get();
                    
                    fail(msg
                            + ": Iterator exhausted while expecting more object(s)"
                            + ": index=" + j);
    
                }
    
//                if(runningQuery.isDone()) runningQuery.get();
    
                final IBindingSet actualObject = actual.next();
    
//                if(runningQuery.isDone()) runningQuery.get();
    
                if (log.isInfoEnabled())
                    log.info("visting: " + actualObject);
    
                final AtomicInteger counter = range.get(actualObject);
    
                if (counter == null || counter.get() == 0) {
    
                    fail("Object not expected" + ": index=" + j + ", object="
                            + actualObject);
    
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