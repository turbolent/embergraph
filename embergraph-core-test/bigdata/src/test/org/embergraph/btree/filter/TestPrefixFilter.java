/*

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
 * Created on Jun 16, 2008
 */

package org.embergraph.btree.filter;

import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase2;

import org.apache.log4j.Level;

import org.embergraph.btree.AbstractTupleCursorTestCase;
import org.embergraph.btree.BTree;
import org.embergraph.btree.DefaultTupleSerializer;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.ITupleSerializer;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.TestTuple;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.StrengthEnum;
import org.embergraph.rawstore.SimpleMemoryRawStore;
import org.embergraph.util.BytesUtil;

/**
 * Test suite for the {@link PrefixFilter}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestPrefixFilter extends TestCase2 {

    /**
     * 
     */
    public TestPrefixFilter() {
    }

    /**
     * @param name
     */
    public TestPrefixFilter(String name) {
        super(name);
    }

    /**
     * Used to form the prefix keys.
     * <p>
     * Note: The prefix keys are formed with {@link StrengthEnum#Identical}.
     * This is necessary in order to match all keys in the index since it causes
     * the secondary characteristics to NOT be included in the prefix key even
     * if they are present in the keys in the index.
     */
    final IKeyBuilder prefixKeyBuilder;
    {

        Properties properties = new Properties();
        
        properties.setProperty(KeyBuilder.Options.STRENGTH,
                StrengthEnum.Primary.toString());
        
        prefixKeyBuilder = KeyBuilder.newUnicodeInstance(properties);

    }

    /**
     * Used to form the keys for the index and to (de-)serialize the values
     * stored in the index.
     */
    final ITupleSerializer tupleSer = DefaultTupleSerializer.newInstance();
    
    protected ITuple<String> newTestTuple(String s) {

        return new TestTuple<String>(IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                tupleSer, s, s, false/* deleted */, 0L/* timestamp */);

    }

    protected byte[] asPrefixKey(Object key) {

        final byte[] b = prefixKeyBuilder.reset().append(key).getKey();

        if (log.isInfoEnabled())
            log.info("key=" + key + ", byte[]="+BytesUtil.toString(b));
        
        return b;
        
    }
    
    /**
     * Unit tests with a single prefix.
     */
    public void test_onePrefix() {

        final BTree btree;
        {
            
            IndexMetadata md = new IndexMetadata(UUID.randomUUID());
            
            md.setTupleSerializer(tupleSer);
            
            btree = BTree.create(new SimpleMemoryRawStore(), md);
            
        }
        
        /*
         * Note: The value is the original Unicode string that is then encoded
         * at PRIMARY strength to obtain the key. Since the key is PRIMARY
         * strength you can not have entries that differ only on their case.
         */
        
        btree.insert("Bryan", "Bryan");

        btree.insert("Bryan Thompson", "Bryan Thompson");
        
        btree.insert("Mike", "Mike");

        btree.insert("Mike Personick", "Mike Personick");
        
        btree.insert("Michael", "Michael");
        
        btree.insert("Michael Personick", "Michael Personick");

        btree.dump(Level.DEBUG, System.err);
        
        {
        
            final ITupleIterator<String> itr = btree.rangeIterator(
                    null/* fromKey */, null/* toKey */, 0/* capacity */,
                    IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                    new PrefixFilter<String>(
                                    asPrefixKey("Mike")));
            
//            ITupleIterator<String> itr = new CompletionScan<String>(
//                    // source cursor.
//                    new ReadOnlyBTreeTupleCursor<String>(btree, new Tuple<String>(
//                            btree, IRangeQuery.DEFAULT), null/* fromKey */,
//                            null/* toKey */),
//                    // prefix for the scan.
//                    asPrefixKey("Mike"));

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple(
                    "Mike"), itr.next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(
                    newTestTuple("Mike Personick"), itr.next());

            assertFalse(itr.hasNext());

        }

        {
            
            final ITupleIterator<String> itr = btree.rangeIterator(
                    null/* fromKey */, null/* toKey */, 0/* capacity */,
                    IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                    new PrefixFilter<String>(
                                    asPrefixKey("Bryan")));
            
//            ITupleIterator<String> itr = new CompletionScan<String>(
//                    // source cursor.
//                    new ReadOnlyBTreeTupleCursor<String>(btree, new Tuple<String>(
//                            btree, IRangeQuery.DEFAULT), null/* fromKey */,
//                            null/* toKey */),
//                    // prefix for the scan.
//                    asPrefixKey("Bryan"));

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple(
                    "Bryan"), itr.next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(
                    newTestTuple("Bryan Thompson"), itr.next());

            assertFalse(itr.hasNext());

        }

        {

            final ITupleIterator<String> itr = btree.rangeIterator(
                    null/* fromKey */, null/* toKey */, 0/* capacity */,
                    IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                    new PrefixFilter<String>(
                                    asPrefixKey("Mi")));

//            ITupleIterator<String> itr = new CompletionScan<String>(
//                    // source cursor.
//                    new ReadOnlyBTreeTupleCursor<String>(btree, new Tuple<String>(
//                            btree, IRangeQuery.DEFAULT), null/* fromKey */,
//                            null/* toKey */),
//                    // prefix for the scan.
//                    asPrefixKey("Mi"));

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple("Michael"), itr
                    .next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple("Michael Personick"), itr
                    .next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple("Mike"), itr
                    .next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(
                    newTestTuple("Mike Personick"), itr.next());

            assertFalse(itr.hasNext());

        }

        // nothing matches.
        {
            
            final ITupleIterator<String> itr = btree.rangeIterator(
                    null/* fromKey */, null/* toKey */, 0/* capacity */,
                    IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                    new PrefixFilter<String>(
                                    asPrefixKey("Ma")));
            
//            ITupleIterator<String> itr = new CompletionScan<String>(
//                    // source cursor.
//                    new ReadOnlyBTreeTupleCursor<String>(btree, new Tuple<String>(
//                            btree, IRangeQuery.DEFAULT), null/* fromKey */,
//                            null/* toKey */),
//                    // prefix for the scan.
//                    asPrefixKey("Ma"));

            assertFalse(itr.hasNext());

        }

    }

    /**
     * Unit tests with multiple prefixes.
     */
    public void test_multiPrefix() {

        final BTree btree;
        {
            
            IndexMetadata md = new IndexMetadata(UUID.randomUUID());
            
            md.setTupleSerializer(tupleSer);
            
            btree = BTree.create(new SimpleMemoryRawStore(), md);
            
        }
        
        /*
         * Note: The value is the original Unicode string that is then encoded
         * at PRIMARY strength to obtain the key. Since the key is PRIMARY
         * strength you can not have entries that differ only on their case.
         */
        
        btree.insert("Bryan", "Bryan");

        btree.insert("Bryan Thompson", "Bryan Thompson");
        
        btree.insert("Mike", "Mike");

        btree.insert("Mike Personick", "Mike Personick");
        
        btree.insert("Michael", "Michael");
        
        btree.insert("Michael Personick", "Michael Personick");

        {

            final ITupleIterator<String> itr = btree.rangeIterator(
                    null/* fromKey */, null/* toKey */, 0/* capacity */,
                    IRangeQuery.DEFAULT | IRangeQuery.CURSOR,
                    new PrefixFilter<String>(
                                    new byte[][] {
                                            asPrefixKey("Bryan"),
                                                asPrefixKey("Mike") }
                                    ));

//            ITupleIterator<String> itr = new CompletionScan<String>(
//                    // source cursor.
//                    new ReadOnlyBTreeTupleCursor<String>(btree, new Tuple<String>(
//                            btree, IRangeQuery.DEFAULT), null/* fromKey */,
//                            null/* toKey */),
//                    // prefix for the scan.
//                            new byte[][] {
//                        asPrefixKey("Bryan"),
//                            asPrefixKey("Mike") });

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple(
                    "Bryan"), itr.next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(
                    newTestTuple("Bryan Thompson"), itr.next());
            
            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(newTestTuple(
                    "Mike"), itr.next());

            assertTrue(itr.hasNext());

            AbstractTupleCursorTestCase.assertEquals(
                    newTestTuple("Mike Personick"), itr.next());

            assertFalse(itr.hasNext());

        }

    }

}
