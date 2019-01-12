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
 * Created on May 28, 2008
 */

package org.embergraph.btree;

import java.util.UUID;

import junit.framework.TestCase;

import org.embergraph.rawstore.SimpleMemoryRawStore;

/**
 * Test suite for {@link EmbergraphSet}.
 * <p>
 * Note: The behavior of the sub-set methods is determined by the behavior of
 * the sub-map methods on {@link EmbergraphMap}. Write your tests there.
 * 
 * @todo add unit tests for the first() and last() methods.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestEmbergraphSet extends TestCase {

    /**
     * 
     */
    public TestEmbergraphSet() {
    }

    /**
     * @param arg0
     */
    public TestEmbergraphSet(String arg0) {
        super(arg0);
    }

    /**
     * The test fixture - this is backed by a temporary store in order to
     * make the unit test cleanup simple.
     */
    EmbergraphSet<String> set;
    
    protected void setUp() throws Exception {
    
        final IndexMetadata indexMetadata = new IndexMetadata(UUID.randomUUID());
     
        /*
         * @todo write tests where delete markers are and are not enabled or
         * make these tests run against all variants.
         */
//        indexMetadata.setIsolatable(true);
        
        set = new EmbergraphSet<String>(BTree.create(
                new SimpleMemoryRawStore(), indexMetadata));

    }

    /**
     * basic tests of add(), isEmpty(), size(), contains(), and remove().
     */
    public void testSet() {
 
        assertTrue(set.isEmpty());
        
        assertEquals(0,set.size());

        assertFalse(set.contains("abc"));
        
        assertTrue(set.add("abc"));

        assertTrue(set.contains("abc"));
        
        assertFalse(set.isEmpty());
        
        assertEquals(1,set.size());
        
        assertFalse(set.add("abc"));
        
        assertTrue(set.remove("abc"));
        
        assertTrue(set.isEmpty());
        
        assertEquals(0,set.size());

        assertFalse(set.contains("abc"));

        assertFalse(set.remove("abc"));
        
    }
    
}
