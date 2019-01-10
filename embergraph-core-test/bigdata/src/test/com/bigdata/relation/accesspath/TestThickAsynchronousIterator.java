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
 * Created on Oct 28, 2008
 */

package com.bigdata.relation.accesspath;

import java.util.concurrent.TimeUnit;

import junit.framework.TestCase2;

import com.bigdata.io.SerializerUtil;

/**
 * Unit tests for {@link ThickAsynchronousIterator}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestThickAsynchronousIterator extends TestCase2 {

    /**
     * 
     */
    public TestThickAsynchronousIterator() {
    }

    /**
     * @param arg0
     */
    public TestThickAsynchronousIterator(String arg0) {
        super(arg0);
    }

    public void test_emptyIterator() {
    
        final String[] expected = new String[]{};
        
        final IAsynchronousIterator<String> itr = new ThickAsynchronousIterator<String>(expected);
        
        doRoundTripTest(expected, itr);
        
    }
    
    public void test_singleValueIterator() {

        final String[] expected = new String[]{"a"};
        
        final IAsynchronousIterator<String> itr = new ThickAsynchronousIterator<String>(expected);
        
        doRoundTripTest(expected, itr);

    }
    
    public void test_multipleValueIterator() {
        
        final String[] expected = new String[]{"a","b","c"};
        
        final IAsynchronousIterator<String> itr = new ThickAsynchronousIterator<String>(expected);
        
        doRoundTripTest(expected, itr);

    }

    protected void doRoundTripTest(final String[] expected,
            final IAsynchronousIterator<String> itr) {

        @SuppressWarnings("unchecked")
        final IAsynchronousIterator<String> itr2 = (IAsynchronousIterator<String>) SerializerUtil
                .deserialize(SerializerUtil.serialize(itr));

        assertSameIterator(expected, itr);

        assertSameIterator(expected, itr2);
        
    }

    protected void assertSameIterator(final String[] expected, final IAsynchronousIterator<String> actual) {

        for (int i = 0; i < expected.length; i++) {

            assertTrue(actual.hasNext());

            try {
                assertTrue(actual.hasNext(1L, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                fail("Interrupted", e);
            }

            final String val = actual.next();
            
            assertEquals(expected[i],val);
            
        }
        
        assertFalse(actual.hasNext());

        try {
            assertFalse(actual.hasNext(1L,TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail("Interrupted", e);
        }
    
    }
    
}
