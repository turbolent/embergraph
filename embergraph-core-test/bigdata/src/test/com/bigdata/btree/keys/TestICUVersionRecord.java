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
 * Created on Mar 21, 2011
 */

package com.bigdata.btree.keys;

import com.bigdata.io.SerializerUtil;
import com.ibm.icu.util.VersionInfo;

import junit.framework.TestCase2;

/**
 * Test suite for {@link ICUVersionRecord}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestICUVersionRecord extends TestCase2 {

    /**
     * 
     */
    public TestICUVersionRecord() {
    }

    /**
     * @param name
     */
    public TestICUVersionRecord(String name) {
        super(name);
    }

    public void test_roundTrip() {

        final ICUVersionRecord r1 = ICUVersionRecord.newInstance();

        final ICUVersionRecord r2 = ICUVersionRecord.newInstance();
        
        assertTrue(r1.equals(r2));
        
        final ICUVersionRecord r3 = (ICUVersionRecord) SerializerUtil
                .deserialize(SerializerUtil.serialize(r1));

        assertTrue(r1.equals(r3));

    }
    
    public void test_roundTrip2() {

        final ICUVersionRecord r1 = new ICUVersionRecord(
                VersionInfo.getInstance(3, 6, 2, 1),//
                VersionInfo.getInstance(1, 8, 5, 7),//
                VersionInfo.getInstance(6, 3, 1, 8),//
                VersionInfo.getInstance(4, 6, 8, 12)//
                );

        final ICUVersionRecord r2 = new ICUVersionRecord( 
            VersionInfo.getInstance(3, 6, 2, 1),//
            VersionInfo.getInstance(1, 8, 5, 7),//
            VersionInfo.getInstance(6, 3, 1, 8),//
            VersionInfo.getInstance(4, 6, 8, 12)//
            );
        
        assertTrue(r1.equals(r2));
        
        assertFalse(r1.equals(ICUVersionRecord.newInstance()));
        
        final ICUVersionRecord r3 = (ICUVersionRecord) SerializerUtil
                .deserialize(SerializerUtil.serialize(r1));

        assertTrue(r1.equals(r3));

    }
    
}
