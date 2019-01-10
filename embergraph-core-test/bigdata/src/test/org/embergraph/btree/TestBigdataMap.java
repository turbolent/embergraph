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
 * Created on May 28, 2008
 */

package org.embergraph.btree;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase;

import com.bigdata.btree.keys.DefaultKeyBuilderFactory;
import com.bigdata.btree.keys.IKeyBuilderFactory;
import com.bigdata.btree.keys.TestKeyBuilder;
import com.bigdata.io.SerializerUtil;
import com.bigdata.rawstore.SimpleMemoryRawStore;

/**
 * Test suite for {@link BigdataMap}.
 * 
 * @todo write tests where delete markers are and are not enabled or make these
 *       tests run against all variants.
 * 
 * @todo test for FusedView
 * 
 * @todo test for scale-out indices.
 * 
 * @todo add unit tests entrySet() - the behavior of keySet() and values() is
 *       fully determined by the behavior of entrySet().
 * 
 * @todo add unit tests for the sub-map methods.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBigdataMap extends TestCase {

    /**
     * 
     */
    public TestBigdataMap() {
    }

    /**
     * @param arg0
     */
    public TestBigdataMap(String arg0) {
        super(arg0);
    }

    /**
     * The test fixture - this is backed by a temporary store in order to
     * make the unit test cleanup simple.
     */
    BigdataMap<String, String> map;
    
    protected void setUp() throws Exception {
    
        final IndexMetadata indexMetadata = new IndexMetadata(UUID.randomUUID());

        /*
         * Note: makes the keys recoverable (from the values) but requires that
         * the keys and the values are the same (basically, that you are using
         * the map like a set).
         */
        indexMetadata.setTupleSerializer(new StringSerializer(
                new DefaultKeyBuilderFactory(new Properties())));
        
        map = new BigdataMap<String, String>(BTree.create(
                new SimpleMemoryRawStore(), indexMetadata));

    }

    /**
     * Basic tests of isEmpty(), size(), containsKey(), put(), and remove().
     */
    public void testMap() {

        assertTrue(map.isEmpty());

        assertEquals(0, map.size());

        assertEquals(0L, map.rangeCount(false/* exactCount */));

        assertEquals(0L, map.rangeCount(true/* exactCount */));

        assertFalse(map.containsKey("abc"));

        assertFalse(map.containsValue("abc"));

        assertNull(map.get("abc"));

        try {
            map.firstKey();
            fail("Expecting: "+NoSuchElementException.class);
        } catch(NoSuchElementException ex) {
            // expected exception.
        }
        
        assertNull(map.put("abc", "abc"));

        assertFalse(map.isEmpty());

        assertEquals(1, map.size());
        
        assertTrue(map.containsKey("abc"));

        assertTrue(map.containsValue("abc"));

        assertEquals("abc", map.get("abc"));

        assertEquals("abc", map.firstKey());

        assertEquals("abc", map.lastKey());

        assertEquals("abc", map.remove("abc"));

        assertEquals(null, map.remove("abc"));

        assertTrue(map.isEmpty());

        assertEquals(0, map.size());

        assertEquals(0L, map.rangeCount(false/* exactCount */));

        assertEquals(0L, map.rangeCount(true/* exactCount */));

        assertFalse(map.containsKey("abc"));

        assertFalse(map.containsValue("abc"));

    }

    /**
     * Handles {@link String} keys and values and makes the keys available for
     * {@link BigdataMap} and {@link BigdataSet} (under the assumption that the
     * key and the value are the same!). The actual index order is governed by
     * {@link TestKeyBuilder#asSortKey(Object)}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    static class StringSerializer extends DefaultTupleSerializer<String, String> {

        private static final long serialVersionUID = -3916736517088617622L;
        
        /**
         * De-serialization ctor.
         */
        public StringSerializer() {}
        
        public StringSerializer(IKeyBuilderFactory keyBuilderFactory) {
            
            super(keyBuilderFactory);
            
        }

        /**
         * Note: The key is materialized from the value since the encoding to
         * the unsigned byte[] is not reversable.
         */
        @Override
        public String deserializeKey(ITuple tuple) {

            return (String) SerializerUtil.deserialize(tuple.getValue());

        }

        @Override
        public byte[] serializeKey(Object key) {
            
            return getKeyBuilder().reset().append((String) key).getKey();
            
        }

        @Override
        public byte[] serializeVal(String obj) {
            
            return SerializerUtil.serialize(obj);
            
        }

        @Override
        public String deserialize(ITuple tuple) {

            return (String) SerializerUtil.deserialize(tuple.getValue());
            
        }
        
        /**
         * The initial version (no additional persistent state).
         */
        private final static transient byte VERSION0 = 0;

        /**
         * The current version.
         */
        private final static transient byte VERSION = VERSION0;

        public void readExternal(final ObjectInput in) throws IOException,
                ClassNotFoundException {

            super.readExternal(in);
            
            final byte version = in.readByte();
            
            switch (version) {
            case VERSION0:
                break;
            default:
                throw new UnsupportedOperationException("Unknown version: "
                        + version);
            }

        }

        public void writeExternal(final ObjectOutput out) throws IOException {

            super.writeExternal(out);
            
            out.writeByte(VERSION);
            
        }

    } // StringSerializer

}
