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
 * Created on Aug 5, 2009
 */

package org.embergraph.btree.data;

import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.raba.ReadOnlyKeysRaba;
import org.embergraph.btree.raba.ReadOnlyValuesRaba;
import org.embergraph.io.DataOutputBuffer;

/**
 * Test suite for the B+Tree {@link ILeafData} records (accessing coded data in
 * place).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractLeafDataRecordTestCase extends
        AbstractNodeOrLeafDataRecordTestCase {

    /**
     * 
     */
    public AbstractLeafDataRecordTestCase() {
    }

    /**
     * @param name
     */
    public AbstractLeafDataRecordTestCase(String name) {
        super(name);
    }

    @Override
    protected boolean mayGenerateLeaves() {
        return true;
    }

    @Override
    protected boolean mayGenerateNodes() {
        return false;
    }

    /**
     * Unit test for an empty leaf.
     */
    public void test_emptyLeaf() {

        final int m = 3;
        final int nkeys = 0;
        final byte[][] keys = new byte[m + 1][];
        final byte[][] vals = new byte[m + 1][];

        final ILeafData expected = mockLeafFactory(new ReadOnlyKeysRaba(
                nkeys, keys), new ReadOnlyValuesRaba(nkeys, vals));

        doRoundTripTest(expected, coder, new DataOutputBuffer());
        
//        final ILeafData actual = new ReadOnlyLeafData(expected, keysCoder,
//                valuesCoder, false/* doubleLinked */, new DataOutputBuffer());
//
//        assertSameLeafData(expected, actual);

    }

    /**
     * A single empty key (byte[0]) paired with a <code>null</code> value.
     */
    public void test_tupleCount1_emptyKey_nullVal() {
 
        final int m = 3;
        final int nkeys = 1;
        final byte[][] keys = new byte[m + 1][];
        final byte[][] vals = new byte[m + 1][];

        keys[0] = new byte[0];
        
        final ILeafData expected = mockLeafFactory(new ReadOnlyKeysRaba(
                nkeys, keys), new ReadOnlyValuesRaba(nkeys, vals));

        doRoundTripTest(expected, coder, new DataOutputBuffer());
        
//        final ILeafData actual = new ReadOnlyLeafData(expected, keysCoder,
//                valuesCoder, false/* doubleLinked */, new DataOutputBuffer());
//
//        assertSameLeafData(expected, actual);
//        
//        // check some keys that are not in the leaf.
//        assertEquals(
//                expected.getKeys().search(new byte[] { 1 }),
//                actual.getKeys().search(new byte[] { 1 }));
//        assertEquals(
//                expected.getKeys().search(new byte[] { 0, 1, 2 }),
//                actual.getKeys().search(new byte[] { 0, 1, 2 }));

    }

    /**
     * A single, empty key (byte[0]) paired with a delete marker.
     */
    public void test_tupleCount1_emptyKey_deleted() {
        
        final int m = 3;
        final int nkeys = 1;
        final byte[][] keys = new byte[m + 1][];
        final byte[][] vals = new byte[m + 1][];
        final boolean[] deleteMarkers = new boolean[m + 1];
        final long[] versionTimestamps = new long[m + 1];

        keys[0] = new byte[0];
        deleteMarkers[0] = true;
        versionTimestamps[0] = System.currentTimeMillis();

        final ILeafData expected = mockLeafFactory(
                new ReadOnlyKeysRaba(nkeys, keys),
                new ReadOnlyValuesRaba(nkeys, vals),
                deleteMarkers,
                versionTimestamps,
                null// rawRecords
        );

        // spot check mock object impl.
        assertTrue(expected.getDeleteMarker(0));
        assertEquals(versionTimestamps[0], expected.getVersionTimestamp(0));
        assertEquals(versionTimestamps[0], expected.getMinimumVersionTimestamp());
        assertEquals(versionTimestamps[0], expected.getMaximumVersionTimestamp());

        doRoundTripTest(expected, coder, new DataOutputBuffer());
        
//        final ILeafData actual = new ReadOnlyLeafData(expected, keysCoder,
//                valuesCoder, false/* doubleLinked */, new DataOutputBuffer());
//
//        assertSameLeafData(expected, actual);
//
//        // check some keys that are not in the leaf.
//        assertEquals(
//                expected.getKeys().search(new byte[] { 1 }),
//                actual.getKeys().search(new byte[] { 1 }));
//        assertEquals(
//                expected.getKeys().search(new byte[] { 0, 1, 2 }),
//                actual.getKeys().search(new byte[] { 0, 1, 2 }));

    }
    
    /**
     * A leaf w/ two keys and version timestamps.
     */
    public void test_tupleCount2VersionTimestamps() {
        
        final int m = 3;
        final int nkeys = 2;
        final byte[][] keys = new byte[m + 1][];
        final byte[][] vals = new byte[m + 1][];
        final boolean[] deleteMarkers = new boolean[m + 1];
        final long[] versionTimestamps = new long[m + 1];

        keys[0] = new byte[]{1,2,3};
        keys[1] = new byte[]{2,2,3};
        deleteMarkers[0] = false;
        deleteMarkers[1] = false;
        versionTimestamps[0] = System.currentTimeMillis();
        versionTimestamps[1] = System.currentTimeMillis()+20;

        final ILeafData expected = mockLeafFactory(
                new ReadOnlyKeysRaba(nkeys, keys),
                new ReadOnlyValuesRaba(nkeys, vals),
                deleteMarkers,
                versionTimestamps,
                null// rawRecords
        );

        assertFalse(expected.getDeleteMarker(0));
        assertFalse(expected.getDeleteMarker(1));
        
        assertEquals(versionTimestamps[0], expected.getVersionTimestamp(0));
        assertEquals(versionTimestamps[0], expected.getMinimumVersionTimestamp());
        assertEquals(versionTimestamps[1], expected.getMaximumVersionTimestamp());

        doRoundTripTest(expected, coder, new DataOutputBuffer());
        
    }
    
    /**
     * A single empty key (byte[0]) paired with a large value represented as a
     * raw record.
     */
    public void test_tupleCount1_emptyKey_largeVal() {
 
        final int m = 3;
        final int nkeys = 1;
        final byte[][] keys = new byte[m + 1][];
        final byte[][] vals = new byte[m + 1][];
        final boolean[] rawRecords = new boolean[m + 1];

        keys[0] = new byte[0];
        vals[0] = AbstractBTree.encodeRecordAddr(recordAddrBuf, nextAddr());
        rawRecords[0] = true;
        
        final ILeafData expected = mockLeafFactory(
        		new ReadOnlyKeysRaba(
                nkeys, keys),
                new ReadOnlyValuesRaba(nkeys, vals),
                null, // deleteMarkers
                null, // version timestamps
                rawRecords
        		);

        doRoundTripTest(expected, coder, new DataOutputBuffer());
        
    }

}
