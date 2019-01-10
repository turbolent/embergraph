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
 * Created on Jun 20, 2008
 */

package org.embergraph.bop;

import com.bigdata.bop.Constant;

import junit.framework.TestCase2;

/**
 * Test suite for {@link Constant}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestConstant extends TestCase2 {

    /**
     * 
     */
    public TestConstant() {
    }

    /**
     * @param name
     */
    public TestConstant(String name) {
        super(name);
    }

    public void test_equals() {
        
        Constant c = new Constant<Integer>(1);
        
        Constant d = new Constant<Integer>(1);
        
        Constant e = new Constant<Integer>(3);

        Constant f = new Constant<Long>(1L);

        Constant g = new Constant<Long>(4L);

        // same reference.
        assertTrue(c.equals(c));

        // same value.
        assertTrue(c.equals(d));

        // different value.
        assertFalse(c.equals(e));
        
        // different type, but equivalent value.
        assertFalse(c.equals(f));

        // different type, different value.
        assertFalse(c.equals(g));

    }
    
}
