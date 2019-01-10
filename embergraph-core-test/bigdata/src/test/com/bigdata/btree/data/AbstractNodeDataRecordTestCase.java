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
 * Created on Aug 5, 2009
 */

package com.bigdata.btree.data;

import com.bigdata.btree.raba.ReadOnlyKeysRaba;
import com.bigdata.io.DataOutputBuffer;

/**
 * Test suite for the B+Tree {@link INodeData} records (accessing coded data in
 * place).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractNodeDataRecordTestCase extends
        AbstractNodeOrLeafDataRecordTestCase {

    /**
     * 
     */
    public AbstractNodeDataRecordTestCase() {
    }

    /**
     * @param name
     */
    public AbstractNodeDataRecordTestCase(String name) {
        super(name);
    }

    @Override
    protected boolean mayGenerateLeaves() {
        return false;
    }

    @Override
    protected boolean mayGenerateNodes() {
        return true;
    }

   /**
     * Unit test for an empty node (this is not a legal instance since only the
     * root leaf may ever be empty).
     */
    public void test_emptyNode() {

        final int m = 3;
        final int nkeys = 0;
        final byte[][] keys = new byte[m][];
        final int spannedTupleCount = 0;
        final long[] childAddr = new long[m + 1];
        final long[] childEntryCount = new long[m + 1];
        final boolean hasVersionTimestamps = false;
        final long minimumVersionTimestamp = 0L;
        final long maximumVersionTimestamp = 0L;

        // Must not be 0L.  See #855.
        childAddr[0] = 12L;

        final INodeData expected = new MockNodeData(new ReadOnlyKeysRaba(nkeys,
                keys), spannedTupleCount, childAddr, childEntryCount,
                hasVersionTimestamps, minimumVersionTimestamp,
                maximumVersionTimestamp);

        doRoundTripTest(expected, coder, new DataOutputBuffer());

    }

    /**
     * Empty node with version timestamps (this is not a legal instance since
     * only the root leaf may ever be empty).
     */
    public void test_emptyNodeVersionTimestamps() {

        final int m = 3;
        final int nkeys = 0;
        final byte[][] keys = new byte[m][];
        final int spannedTupleCount = 0;
        final long[] childAddr = new long[m + 1];
        final long[] childEntryCount = new long[m + 1];
        final boolean hasVersionTimestamps = true;
        final long minimumVersionTimestamp = System.currentTimeMillis();
        final long maximumVersionTimestamp = System.currentTimeMillis() + 20;

        // Must not be 0L.  See #855.
        childAddr[0] = 12L;
        
        final INodeData expected = new MockNodeData(new ReadOnlyKeysRaba(nkeys,
                keys), spannedTupleCount, childAddr, childEntryCount,
                hasVersionTimestamps, minimumVersionTimestamp,
                maximumVersionTimestamp);

        doRoundTripTest(expected, coder, new DataOutputBuffer());

    }

    /**
     * This the minimum legal node for a branching factor of 3. It has one key
     * and two children.
     */
    public void test_tupleCount1() {
        
        final int m = 3;
        final int nkeys = 1; // 1 key so 2 children.
        final byte[][] keys = new byte[m][];
        final long[] childAddr = new long[] { 10, 20, 0, 0 };
        final long childEntryCount[] = new long[] { 4, 7, 0, 0 };
        final boolean hasVersionTimestamps = false;
        final long minimumVersionTimestamp = 0L;
        final long maximumVersionTimestamp = 0L;

        keys[0] = new byte[] { 1, 2, 3 };

        long entryCount = 0;
        for (int i = 0; i <= nkeys; i++) {

            entryCount += childEntryCount[i];

        }

        final INodeData expected = new MockNodeData(new ReadOnlyKeysRaba(nkeys,
                keys), entryCount, childAddr, childEntryCount,
                hasVersionTimestamps, minimumVersionTimestamp,
                maximumVersionTimestamp);

        doRoundTripTest(expected, coder, new DataOutputBuffer());

    }

    /**
     * This the minimum legal node for a branching factor of 3. It has one key
     * and two children.
     */
    public void test_tupleCount1WithVersionTimestamps() {
        
        final int m = 3;
        final int nkeys = 1; // 1 key so 2 children.
        final byte[][] keys = new byte[m][];
        final long[] childAddr = new long[] { 10, 20, 0, 0 };
        final long childEntryCount[] = new long[] { 4, 7, 0, 0 };
        final boolean hasVersionTimestamps = true;
        final long minimumVersionTimestamp = System.currentTimeMillis();
        final long maximumVersionTimestamp = System.currentTimeMillis() + 20;

        keys[0] = new byte[] { 1, 2, 3 };

        long entryCount = 0;
        for (int i = 0; i <= nkeys; i++) {

            entryCount += childEntryCount[i];

        }

        final INodeData expected = new MockNodeData(new ReadOnlyKeysRaba(nkeys,
                keys), entryCount, childAddr, childEntryCount,
                hasVersionTimestamps, minimumVersionTimestamp,
                maximumVersionTimestamp);

        doRoundTripTest(expected, coder, new DataOutputBuffer());

    }

}
