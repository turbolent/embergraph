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
 * Created on Oct 14, 2006
 */

package com.bigdata.rdf.store;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestCase2;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.bigdata.btree.IIndex;
import com.bigdata.btree.ITupleIterator;
import com.bigdata.btree.ITupleSerializer;
import com.bigdata.btree.UnisolatedReadWriteIndex;
import com.bigdata.btree.keys.IKeyBuilder;
import com.bigdata.btree.proc.AbstractKeyArrayIndexProcedure.ResultBitBuffer;
import com.bigdata.btree.proc.BatchContains.BatchContainsConstructor;
import com.bigdata.btree.proc.IResultHandler;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.Journal;
import com.bigdata.journal.Options;
import com.bigdata.journal.TestHelper;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.internal.IVUtility;
import com.bigdata.rdf.model.BigdataResource;
import com.bigdata.rdf.model.BigdataURI;
import com.bigdata.rdf.model.BigdataValue;
import com.bigdata.rdf.model.BigdataValueSerializer;
import com.bigdata.rdf.model.StatementEnum;
import com.bigdata.rdf.rio.BasicRioLoader;
import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.spo.SPO;
import com.bigdata.rdf.spo.SPOComparator;
import com.bigdata.rdf.spo.SPOKeyOrder;
import com.bigdata.rdf.spo.SPOTupleSerializer;
import com.bigdata.relation.accesspath.AbstractArrayBuffer;
import com.bigdata.relation.accesspath.IBuffer;
import com.bigdata.service.AbstractClient;
import com.bigdata.service.Split;
import com.bigdata.striterator.IChunkedOrderedIterator;
import com.bigdata.striterator.IKeyOrder;
import com.bigdata.util.BytesUtil;

/**
 * <p>
 * Abstract harness for testing under a variety of configurations. In order to
 * test a specific configuration, create a concrete instance of this class. The
 * configuration can be described using a mixture of a <code>.properties</code>
 * file of the same name as the test class and custom code.
 * </p>
 * <p>
 * When debugging from an IDE, it is very helpful to be able to run a single
 * test case. You can do this, but you MUST define the property
 * <code>testClass</code> as the name test class that has the logic required
 * to instantiate and configure an appropriate object manager instance for the
 * test.
 * </p>
 */
abstract public class AbstractTestCase
    extends TestCase2
{

    /** A <code>null</code> {@link IV} reference (NOT a NullIV object). */
    @SuppressWarnings("rawtypes")
    protected final IV NULL = null;
    
    //
    // Constructors.
    //

    public AbstractTestCase() {}
    
    public AbstractTestCase(String name) {super(name);}

    //************************************************************
    //************************************************************
    //************************************************************
    
    /**
     * Invoked from {@link TestCase#setUp()} for each test in the suite.
     */
    protected void setUp(final ProxyTestCase testCase) throws Exception {

        begin = System.currentTimeMillis();
        
        if (log.isInfoEnabled())
        log.info("\n\n================:BEGIN:" + testCase.getName()
                + ":BEGIN:====================");

		// @see BLZG-1501 (remove LRUNexus)
//        if (LRUNexus.INSTANCE != null) {
//            // flush everything before/after a unit test.
//            LRUNexus.INSTANCE.discardAllCaches();
//        }
        
    }

    /**
     * Invoked from {@link TestCase#tearDown()} for each test in the suite.
     */
    protected void tearDown(final ProxyTestCase testCase) throws Exception {

		// @see BLZG-1501 (remove LRUNexus)
//        if (LRUNexus.INSTANCE != null) {
//            // flush everything before/after a unit test.
//            LRUNexus.INSTANCE.discardAllCaches();
//        }
        
        final long elapsed = System.currentTimeMillis() - begin;
        
        if (log.isInfoEnabled())
        log.info("\n================:END:" + testCase.getName()
                + " ("+elapsed+"ms):END:====================\n");

        TestHelper.checkJournalsClosed(testCase, this);
        
    }
    
    private long begin;
    
    //
    // Properties
    //
    
    private Properties m_properties;
    
    /**
     * <p>
     * Returns properties read from a hierarchy of sources. The underlying
     * properties read from those sources are cached, but a new properties
     * object is returned on each invocation (to prevent side effects by the
     * caller).
     * </p>
     * <p>
     * In general, a test configuration critically relies on both the properties
     * returned by this method and the appropriate properties must be provided
     * either through the command line or in a properties file.
     * </p>
     * 
     * @return A new properties object.
     */
    public Properties getProperties() {
        
        if( m_properties == null ) {
            
            /*
             * Read properties from a hierarchy of sources and cache a
             * reference.
             */
            m_properties = super.getProperties();

            // disregard the inherited properties.
//            m_properties = new Properties();
            

            // m_properties = new Properties( m_properties );

            // disable statistics collection (federation)
            m_properties
                    .setProperty(
                            AbstractClient.Options.COLLECT_PLATFORM_STATISTICS,
                            "false");
            m_properties.setProperty(
                    AbstractClient.Options.COLLECT_QUEUE_STATISTICS, "false");
            m_properties.setProperty(AbstractClient.Options.HTTPD_PORT, "-1");

            // disable statistics collection (journal)
            m_properties.setProperty(
                    Journal.Options.COLLECT_PLATFORM_STATISTICS, "false");
            m_properties.setProperty(Journal.Options.COLLECT_QUEUE_STATISTICS,
                    "false");
            m_properties
                    .setProperty(Journal.Options.HTTPD_PORT, "-1"/* none */);

            m_properties.setProperty(Options.BUFFER_MODE,BufferMode.Disk.toString());
//            m_properties.setProperty(Options.BUFFER_MODE,BufferMode.Transient.toString());

            /*
             * If an explicit filename is not specified...
             */
            if(m_properties.get(Options.FILE)==null) {

                /*
                 * Use a temporary file for the test. Such files are always deleted when
                 * the journal is closed or the VM exits.
                 */

                m_properties.setProperty(Options.CREATE_TEMP_FILE,"true");
            
                m_properties.setProperty(Options.DELETE_ON_EXIT,"true");
                
            }
            
        }        
        
        return new Properties(m_properties);
        
    }

    /**
     * This method is invoked from methods that MUST be proxied to this class.
     * {@link GenericProxyTestCase} extends this class, as do the concrete
     * classes that drive the test suite for specific GOM integration test
     * configuration. Many method on this class must be proxied from
     * {@link GenericProxyTestCase} to the delegate. Invoking this method from
     * the implementations of those methods in this class provides a means of
     * catching omissions where the corresponding method is NOT being delegated.
     * Failure to delegate these methods means that you are not able to share
     * properties or object manager instances across tests, which means that you
     * can not do configuration-based testing of integrations and can also wind
     * up with mutually inconsistent test fixtures between the delegate and each
     * proxy test.
     */
    
    protected void checkIfProxy() {
        
        if( this instanceof ProxyTestCase ) {
            
            throw new AssertionError();
            
        }
        
    }

    //************************************************************
    //************************************************************
    //************************************************************
    //
    // Test helpers.
    //

//    protected static final long N = IRawTripleStore.N;

    abstract protected AbstractTripleStore getStore(Properties properties);
    
    abstract protected AbstractTripleStore reopenStore(AbstractTripleStore store);

    public void assertEquals(SPO expected, SPO actual) {
        
        assertEquals(null,expected,actual);
        
    }
    
    public void assertEquals(String msg, SPO expected, SPO actual) {
        
        if(!expected.equals(actual)) {
                    
            if( msg == null ) {
                msg = "";
            } else {
                msg = msg + " : ";
            }

            fail(msg+"Expecting: "+expected+" not "+actual);
            
        }
        
    }
    
    public void assertEquals(SPO[] expected, SPO[] actual) {

        assertEquals(null,expected,actual);
        
    }
    
    public void assertEquals(String msg, SPO[] expected, SPO[] actual) {
    
        if( msg == null ) {
            msg = "";
        } else {
            msg = msg + " : ";
        }

        if( expected == null && actual == null ) {
            
            return;
            
        }
        
        if( expected == null && actual != null ) {
            
            fail( msg+"Expected a null array." );
            
        }
        
        if( expected != null && actual == null ) {
            
            fail( msg+"Not expecting a null array." );
            
        }
        
        if (expected.length != actual.length) {
            /*
             * Only do message construction if we know that the assert will
             * fail.
             */
            assertEquals(msg + "length differs.", expected.length,
                    actual.length);
        }
        
        for( int i=0; i<expected.length; i++ ) {
            
            try {

                assertEquals(expected[i], actual[i]);
                
            } catch (AssertionFailedError ex) {
                
                /*
                 * Only do the message construction once the assertion is known
                 * to fail.
                 */
                
                fail(msg + "values differ: index=" + i, ex);
                
            }
            
        }
        
    }
    
//    /**
//     * Method verifies that the <i>actual</i> {@link Iterator} produces the
//     * expected objects in the expected order. Objects are compared using
//     * {@link Object#equals( Object other )}. Errors are reported if too few or
//     * too many objects are produced, etc.
//     * 
//     * Note: refactored to {@link TestCase2}.
//     */
//    static public void assertSameItr(Object[] expected, Iterator<?> actual) {
//
//        assertSameIterator("", expected, actual);
//
//    }

    static public void assertSameSPOs(ISPO[] expected, IChunkedOrderedIterator<ISPO>actual) {

        assertSameSPOs("", expected, actual);

    }

    static public void assertSameSPOs(String msg, ISPO[] expected, IChunkedOrderedIterator<ISPO> actual) {

        /*
         * clone expected[] and put into the same order as the iterator.
         */
        
        expected = expected.clone();

        final IKeyOrder<ISPO> keyOrder = actual.getKeyOrder();

        if (keyOrder != null) {

            Arrays.sort(expected, keyOrder.getComparator());

        }

        int i = 0;

        while (actual.hasNext()) {

            if (i >= expected.length) {

                // buffer up to N 'extra' values from the itr for the err msg.
                final Vector<ISPO> v = new Vector<ISPO>();
                
                while (actual.hasNext() && v.size() < 10) {
                
                    v.add(actual.next());
                    
                }
                
                fail(msg + ": The iterator is willing to visit more than "
                        + expected.length + " objects.  The next " + v.size()
                        + " objects would be: " + Arrays.toString(v.toArray()));

            }

            final ISPO g = actual.next();

            if (!expected[i].equals(g)) {
                
                /*
                 * Only do message construction if we know that the assert will
                 * fail.
                 */
                fail(msg + ": Different objects at index=" + i + ": expected="
                        + expected[i] + ", actual=" + g);
            }

            i++;

        }

        if (i < expected.length) {

            fail(msg + ": The iterator SHOULD have visited " + expected.length
                    + " objects, but only visited " + i + " objects.");

        }

    }

    /**
     * Verify that the iterator visits the expected {@link ISPO}s in any order
     * without duplicates.
     * 
     * @param store
     *            Used to resolve term identifiers for messages.
     * @param expected
     * @param actual
     */
    static public void assertSameSPOsAnyOrder(AbstractTripleStore store,
            ISPO[] expected, IChunkedOrderedIterator<ISPO> actual) {

        assertSameSPOsAnyOrder(store, expected, actual, false);
    }
    
    /**
     * Verify that the iterator visits the expected {@link ISPO}s in any order
     * without duplicates, optionally ignoring axioms.
     * 
     * @param store
     *            Used to resolve term identifiers for messages.
     * @param expected
     * @param actual (The iterator will be closed).
     * @param ignoreAxioms
     */
    static public void assertSameSPOsAnyOrder(AbstractTripleStore store,
            ISPO[] expected, IChunkedOrderedIterator<ISPO> actual,
            boolean ignoreAxioms) {
        
        try {

            Map<ISPO, ISPO> map = new TreeMap<ISPO, ISPO>(SPOComparator.INSTANCE);

            for (ISPO tmp : expected) {

                map.put(tmp, tmp);

            }

            int i = 0;

            while (actual.hasNext()) {

                final ISPO actualSPO = actual.next();
                
                if (ignoreAxioms && actualSPO.isAxiom()) {
                    continue;
                }

                if (log.isInfoEnabled())
                    log.info("actual: " + actualSPO.toString(store));

                final ISPO expectedSPO = map.remove(actualSPO);

                if (expectedSPO == null) {

                    fail("Not expecting: " + actualSPO.toString(store)
                            + " at index=" + i);

                }

                // log.info("expected: "+expectedSPO.toString(store));

                StatementEnum expectedType =
                        expectedSPO.hasStatementType() ? expectedSPO.getStatementType()
                                : null;
                StatementEnum actualType =
                        actualSPO.hasStatementType() ? actualSPO.getStatementType()
                                : null;
                if (expectedType != actualType) {
                    // defer message generation until assert fails.
                    assertEquals("expected=" + expectedSPO + ",actual="
                            + actualSPO, expectedType, actualType);
                }

                i++;

            }

            if (!map.isEmpty()) {

                // @todo convert term identifiers before rendering.
                if (log.isInfoEnabled())
                    log.info("Iterator empty but still expecting: "
                            + map.values());

                fail("Expecting: " + map.size() + " more statements: "
                        + map.values());

            }

        } finally {

            actual.close();

        }

    }
    
    static public void assertSameStatements(Statement[] expected,
            BigdataStatementIterator actual) {

        assertSameStatements("", expected, actual);

    }

    /**
     * @todo since there is no way to know the natural order for the statement
     *       iterator we can not sort expected into the same order. therefore
     *       this should test for the same statements in any order
     */
    static public void assertSameStatements(final String msg,
            final Statement[] expected, final BigdataStatementIterator actual) {

        int i = 0;

        while (actual.hasNext()) {

            if (i >= expected.length) {

                fail(msg + ": The iterator is willing to visit more than "
                        + expected.length + " objects.");

            }

            Statement g = actual.next();
            
            if (!expected[i].equals(g)) {
                
                /*
                 * Only do message construction if we know that the assert will
                 * fail.
                 */
                fail(msg + ": Different objects at index=" + i + ": expected="
                        + expected[i] + ", actual=" + g);
            }

            i++;

        }

        if (i < expected.length) {

            fail(msg + ": The iterator SHOULD have visited " + expected.length
                    + " objects, but only visited " + i + " objects.");

        }

    }

    /**
     * Verify that TERM2ID and ID2TERM have the same range count and that
     * all ID2TERM entries resolve a TERM2ID entry and that each TERM2ID
     * entry leads to an ID2TERM entry.
     * 
     * @param store2
     */
    static public void assertLexiconIndicesConsistent(
            final AbstractTripleStore store) {

        final IIndex t2id = store.getLexiconRelation().getTerm2IdIndex();

        final IIndex id2t = store.getLexiconRelation().getId2TermIndex();

        final BigdataValueSerializer<BigdataValue> valSer = store
                .getValueFactory().getValueSerializer();

        /*
         * First, scan the TERMS index and verify that each IV maps to an entry
         * in the ID2TERMS index which maps back to the original entry in the
         * TERMS index.
         */
        {

            final ITupleSerializer t2idTupleSer = t2id.getIndexMetadata()
                    .getTupleSerializer();

            final IKeyBuilder keyBuilder = id2t.getIndexMetadata()
                    .getTupleSerializer().getKeyBuilder();

            final ITupleIterator<IV> itr = t2id.rangeIterator();

            while (itr.hasNext()) {

                final IV<?, ?> iv = (IV) itr.next().getObject();

                keyBuilder.reset();

                final byte[] ivAsKey = iv.encode(keyBuilder).getKey();

                // TODO inefficient point lookup.
                final byte[] encodedValue = id2t.lookup(ivAsKey);

                assertNotNull(encodedValue);

                // Decode the Value.
                final BigdataValue decodedValue = valSer
                        .deserialize(encodedValue);

                // Generate key for T2ID index.
                final byte[] term2IdKey = t2idTupleSer
                        .serializeKey(decodedValue);

                // TODO inefficient point lookup.
                final byte[] encodedIV = t2id.lookup(term2IdKey);

                if (encodedIV == null) {

                    fail("No entry in TERMS index: v=" + decodedValue + ", iv="
                            + iv);

                }

                if (!BytesUtil.bytesEqual(ivAsKey, encodedIV)) {
                    /*
                     * The IV that we got back by round tripping back through
                     * the TERMS2ID index does not agree with the IV we obtained
                     * from the iterator scanning the TERMS2ID index.
                     */
                    fail("IV: original=" + BytesUtil.toString(ivAsKey)
                            + ", afterRoundTrip="
                            + BytesUtil.toString(encodedIV));
                }

            }

        }

        /*
         * Scan the ID2TERM index, verifying that each entry maps to an entry in
         * the TERMS2ID index which is associated with the same IV.
         */
        {

            final ITupleSerializer t2idTupleSer = t2id.getIndexMetadata()
                    .getTupleSerializer();

            final IKeyBuilder keyBuilder = id2t.getIndexMetadata()
                    .getTupleSerializer().getKeyBuilder();

            final ITupleIterator<BigdataValue> itr = id2t.rangeIterator();

            while (itr.hasNext()) {

                final BigdataValue v = itr.next().getObject();

                final IV<?, ?> iv = v.getIV();

                assertNotNull(v.stringValue(), iv);

                // Generate key for T2ID index.
                final byte[] term2IdKey = t2idTupleSer.serializeKey(v);

                // TODO inefficient point lookup.
                final byte[] encodedIV = t2id.lookup(term2IdKey);

                if (encodedIV == null) {

                    fail("No entry in TERMS index: v=" + v + ", iv=" + iv);

                }

                final IV<?, ?> decodedIV = IVUtility.decodeFromOffset(
                        encodedIV, 0/* offset */);

                if (!iv.equals(decodedIV)) {
                    /*
                     * The IV that we got back by round tripping back through
                     * the TERMS2ID index does not agree with the IV we obtained
                     * from the iterator scanning the ID2TERMS index.
                     */
                    fail("IV: original=" + iv + ", afterRoundTrip=" + decodedIV
                            + " for value=" + v);
                }

            }

        }

        /*
         * Verify that the range counts on TERM2ID and ID2TERM are the same.
         */
        {

            final long rc1 = t2id.rangeCount();
            final long rc2 = id2t.rangeCount();

            assertEquals("lexicon range counts: t2id=" + rc1 + ", id2t=" + rc2,
                    rc1, rc2);

        }

    }
    
    /**
     * Validates that the same statements are found in each of the statement
     * indices.
     */
    static public void assertStatementIndicesConsistent(
            final AbstractTripleStore db, final int maxerrors) {

        if (log.isInfoEnabled())
            log.info("Verifying statement indices");

        final AtomicInteger nerrs = new AtomicInteger(0);

        final int from, to;
        if (db.getSPOKeyArity() == 3) {
            from = SPOKeyOrder.FIRST_TRIPLE_INDEX;
            to = SPOKeyOrder.LAST_TRIPLE_INDEX;
        } else {
            from = SPOKeyOrder.FIRST_QUAD_INDEX;
            to = SPOKeyOrder.LAST_QUAD_INDEX;
        }

        for (int i = from; i <= to; i++) {

            for (int j = from; j <= to; j++) {

                if (i <= j) {
                    // Just compare the upper diagonal.
                    continue;
                }

                assertSameStatements(db, SPOKeyOrder.valueOf(i),
                        SPOKeyOrder.valueOf(j), nerrs, maxerrors);

            }

        }

        assertEquals(0, nerrs.get());

    }
    
    /**
     * Verify all statements reported by one access path are found on another
     * access path.
     * <p>
     * Note: This is basically a JOIN. If there is an expected tuple that is not
     * found then it is an error. Like a JOIN, we need to process a chunk at a
     * time for efficiency and unroll the inner loop (this is necessary for
     * efficiency not only when using scale-out, but also to avoid doing a whole
     * bunch of point tests against an {@link UnisolatedReadWriteIndex}).
     * Unlike a normal JOIN, the join order is fixed - we visit the expected
     * access path and then join against the actual access path.
     * 
     * @param db
     *            The database.
     * @param keyOrderExpected
     *            Scan this statement index.
     * @param keyOrderActual
     *            Verifying that each statement is also present in this index.
     * @param nerrs
     *            Used to report the #of errors.
     */
    static private void assertSameStatements(//
            final AbstractTripleStore db,//
            final SPOKeyOrder keyOrderExpected,//
            final SPOKeyOrder keyOrderActual,//
            final AtomicInteger nerrs,//
            final int maxerrors
    ) {

        if (log.isInfoEnabled())
            log.info("Verifying " + keyOrderExpected + " against "
                    + keyOrderActual);

        // the access path that is being tested.
        final IIndex actualIndex = db.getSPORelation().getIndex(keyOrderActual);

        // the serializer for the access path that is being tested.
        final SPOTupleSerializer tupleSer = (SPOTupleSerializer) actualIndex
                .getIndexMetadata().getTupleSerializer();

        /*
         * An iterator over the access path whose statements are being treated
         * as ground truth for this pass over the data.
         */
        final IChunkedOrderedIterator<ISPO> itre = db.getAccessPath(
                keyOrderExpected).iterator();

        try {

            while (itre.hasNext()) {

                if (nerrs.get() > 10)
                    throw new RuntimeException("Too many errors");

                /*
                 * This is a chunk of expected statements in the natural order
                 * for the "actual" access path.
                 */
                final ISPO[] expectedChunk = itre.nextChunk(keyOrderActual);

                /*
                 * Construct a batch contains test for those statements and
                 * submit it to the actual index. The aggregator will verify
                 * that each expected statement exists in the actual index and
                 * report an error for those that were not found.
                 */
                final int fromIndex = 0;
                final int toIndex = expectedChunk.length;
                final byte[][] keys = new byte[expectedChunk.length][];
                final byte[][] vals = null;

                for (int i = 0; i < expectedChunk.length; i++) {

                    keys[i] = tupleSer.serializeKey(expectedChunk[i]);

                }
                
                final AtomicLong nfound = new AtomicLong();

                final IResultHandler<?, ?> resultHandler = new IResultHandler<ResultBitBuffer, Long>() {

                	@Override
                    public void aggregate(final ResultBitBuffer result, final Split split) {

                        // #of elements in this split.
                        final int n = result.getResultCount();

                        // flag indicating found/not found for each tuple.
                        final boolean[] a = result.getResult();

                        int delta = 0;

                        for (int i = 0; i < n; i++) {

                            if (a[i]) {

                                // found.
                                delta++;

                            } else {

                                /*
                                 * This happens when the statement is not in the
                                 * index AND there is no successor of the
                                 * statement in the index.
                                 */

                                final ISPO expectedSPO = expectedChunk[i];

                                log.error("Statement not found" + ": index="
                                        + keyOrderActual + ", stmt="
                                        + expectedSPO);

                                nerrs.incrementAndGet();

                            }

                        }

                        nfound.addAndGet(delta);

                    }

                    /**
                     * The #of statements found.
                     */
                	@Override
                    public Long getResult() {

                        return nfound.get();

                    }

                };

                actualIndex.submit(fromIndex, toIndex, keys, vals,
                        BatchContainsConstructor.INSTANCE, resultHandler);

            }

        } finally {

            itre.close();

        }

    }
    
    /**
     * Helper class verifies that all statements identified by a re-parse of
     * some RDF/XML file are present in the KB.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    static protected class StatementVerifier extends BasicRioLoader {

        final private AbstractTripleStore db;

        final private AtomicInteger nerrs;
        
        final private int maxerrors;

        final IBuffer<Statement> buffer;
        
        /**
         * 
         * @param db
         *            The database.
         * @param capacity
         *            The buffer capacity (the maximum #of statements to be
         *            processed in a batch).
         * @param nerrs
         *            Used to track and report the #of errors as a side-effect.
         * @param maxerrors
         *            The maximum #of errors before the test will abort.
         */
        public StatementVerifier(final AbstractTripleStore db, final int capacity,
                final AtomicInteger nerrs, final int maxerrors) {

            super(db.getValueFactory());
            
            this.db = db;

            this.nerrs = nerrs;

            this.maxerrors = maxerrors;
            
            this.buffer = new AbstractArrayBuffer<Statement>(capacity,
                    Statement.class, null/* filter */) {

                @Override
                protected long flush(final int n, final Statement[] a) {

                    verifyStatements( n , a );
                    
                    return n;

                }
                
            };

        }

        /**
         * Report an error.
         * 
         * @param msg
         *            The error message.
         */
        private void error(final String msg) {

            log.error(msg);
            
            if (nerrs.incrementAndGet() > maxerrors) {

                throw new RuntimeException("Too many errors");

            }

        }

        /**
         * Extended to flush the {@link #buffer}.
         */
        protected void success() {

            super.success();
            
            buffer.flush();
            
        }
        
        public RDFHandler newRDFHandler() {

            return new RDFHandlerBase() {

                public void handleStatement(final Statement stmt) {
                    
                    buffer.add(stmt);
                    
                }

            };

        }

        private void verifyStatements(final int n, final Statement[] a) {

            final Map<Value, BigdataValue> termSet = new LinkedHashMap<Value, BigdataValue>(
                    n);
            {

                for (int i = 0; i < n; i++) {

                    final Statement stmt = a[i];

                    termSet.put(stmt.getSubject(), db.getValueFactory()
                            .asValue(stmt.getSubject()));

                    termSet.put(stmt.getPredicate(), db.getValueFactory()
                            .asValue(stmt.getPredicate()));

                    termSet.put(stmt.getObject(), db.getValueFactory()
                            .asValue(stmt.getObject()));

                }

                final int nterms = termSet.size();

                final BigdataValue[] terms = new BigdataValue[nterms];

                int i = 0;
                for (BigdataValue term : termSet.values()) {

                    terms[i++] = term;

                }

                db.getLexiconRelation()
                        .addTerms(terms, nterms, true/* readOnly */);

                int nunknown = 0;
                for (BigdataValue term : terms) {

                    if (term.getIV() == null) {

                        error("Unknown term: " + term);
                        
                        nunknown++;

                    }

                }

                if (nunknown > 0) {
                    
                    log.warn("" + nunknown + " out of " + nterms
                            + " terms were not found.");
                    
                }
                
            }
            
            if (log.isInfoEnabled())
            log.info("There are " + termSet.size()
                    + " distinct terms in the parsed statements.");

            /*
             * Now verify reverse lookup for those terms.
             */
            {
                
                final Set<IV<?,?>> ivs  = new LinkedHashSet<IV<?,?>>(termSet.size());
                
                for(BigdataValue term : termSet.values()) {
                    
                    final IV<?,?> iv = term.getIV();
                    
                    if (iv == null || iv.isNullIV()) {

                        // ignore terms that we know were not found.
                        continue;
                        
                    }
                    
                    ivs.add(iv);
                    
                }

                // batch resolve ids to terms.
                final Map<IV<?, ?>, BigdataValue> reverseMap = db
                        .getLexiconRelation().getTerms(ivs);

                for(BigdataValue expectedTerm : termSet.values()) {
                    
                    final IV<?,?> iv = expectedTerm.getIV();
                    
                    if (iv == null) {

                        // ignore terms that we know were not found.
                        continue;
                        
                    }

                    final BigdataValue actualTerm = reverseMap.get(iv);

                    if (actualTerm == null || !actualTerm.equals(expectedTerm)) {

                        error("expectedTerm=" + expectedTerm
                                        + ", assigned termId=" + iv
                                        + ", but reverse lookup reports: "
                                        + actualTerm);
                        
                    }
                    
                }
                
            }
            
            /*
             * Now verify the statements using the assigned term identifiers.
             * 
             * @todo not handling the context position - it is either unbound or
             * a statement identifier.
             */
            {

                final SPO[] b = new SPO[n];

                int n2 = 0;
                for (int i = 0; i < n; i++) {

                    final Statement stmt = a[i];

                    final BigdataResource s = (BigdataResource) db
                            .asValue(termSet.get(stmt.getSubject()));

                    final BigdataURI p = (BigdataURI) db.asValue(termSet
                            .get(stmt.getPredicate()));

                    final BigdataValue o = (BigdataValue) db.asValue(termSet
                            .get(stmt.getObject()));

                    boolean ok = true;
                    if (s == null) {
                        
                        log.error("Subject not found: "+stmt.getSubject());
                        ok = false;
                        
                    }
                    if(p == null) {
                        
                        log.error("Predicate not found: "+stmt.getPredicate());
                        ok = false;
                        
                    }
                    if(o == null) {
                        
                        log.error("Object not found: "+stmt.getObject());
                        ok = false;

                    }
                 
                    if(!ok) {
                        
                        log.error("Unable to resolve statement with unresolvable terms: "+stmt);
                        
                    } else {

                        // Leave the StatementType blank for bulk complete.
                        b[n2++] = new SPO(s.getIV(), p.getIV(), o
                                .getIV() /*, StatementType */);

                    }
                    
                }

                final IChunkedOrderedIterator<ISPO> itr = db
                        .bulkCompleteStatements(b, n2);

                try {

                    int i = 0;
                    
                    while (itr.hasNext()) {

                        final ISPO spo = itr.next();

                        if (!spo.hasStatementType()) {

                            error("Statement not found: " + spo.toString(db));

                        }
                        
                        i++;

                    }
                    
                    if (log.isInfoEnabled())
                        log.info("Verified " + i
                                + " statements parsed from file.");

                } finally {

                    itr.close();

                }
                
            }

        }

    }

    /**
     * Recursively removes any files and subdirectories and then removes the
     * file (or directory) itself.
     * 
     * @param f
     *            A file or directory.
     */
    protected void recursiveDelete(File f) {
        
        if(f.isDirectory()) {
            
            File[] children = f.listFiles();
            
            for(int i=0; i<children.length; i++) {
                
                recursiveDelete( children[i] );
                
            }
            
        }
        
        if (f.exists()) {

            log.warn("Removing: " + f);

            if (!f.delete()) {

                throw new RuntimeException("Could not remove: " + f);

            }

        }

    }
    
}
