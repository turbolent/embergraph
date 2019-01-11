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
 * Created on Feb 16, 2007
 */

package org.embergraph.journal;

import java.util.UUID;

import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;

/**
 * Test suite for transactions reading from a start time corresponding to the
 * last commit time on the database.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestReadCommittedTx<S extends Journal> extends ProxyTestCase<S> {

    /**
     * 
     */
    public TestReadCommittedTx() {
    }

    /**
     * @param name
     */
    public TestReadCommittedTx(String name) {
        super(name);
    }

    public void test_readCommitted() {

        final Journal journal = getStore();
        
        try {

            final String name = "abc";

            final byte[] k1 = new byte[] { 1 };

            final byte[] v1 = new byte[] { 1 };

            {

                /*
                 * register an index, write on the index, and commit the
                 * journal.
                 */
                IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

                md.setIsolatable(true);

                journal.registerIndex(md);

                IIndex ndx = journal.getIndex(name);

                ndx.insert(k1, v1);

                journal.commit();

            }

            {

                /*
                 * create a read-only transaction, verify that we can read the
                 * value written on the index but that we can not write on the
                 * index.
                 */

                final long tx1 = journal.newTx(ITx.READ_COMMITTED);

                assertTrue(Math.abs(tx1) >= journal.getLastCommitTime());
                
                IIndex ndx = journal.getIndex(name, tx1);

                assertNotNull(ndx);

                assertEquals((byte[]) v1, (byte[]) ndx.lookup(k1));

                try {
                    ndx.insert(k1, new byte[] { 1, 2, 3 });
                    fail("Expecting: " + UnsupportedOperationException.class);
                } catch (UnsupportedOperationException ex) {
                    System.err.println("Ignoring expected exception: " + ex);
                }

                journal.commit(tx1);

            }

            {

                /*
                 * do it again, but this time we will abort the read-only
                 * transaction.
                 */

                final long tx1 = journal.newTx(ITx.READ_COMMITTED);

                assertTrue(Math.abs(tx1) >= journal.getLastCommitTime());

                IIndex ndx = journal.getIndex(name, tx1);

                assertNotNull(ndx);

                assertEquals((byte[]) v1, (byte[]) ndx.lookup(k1));

                try {
                    ndx.insert(k1, new byte[] { 1, 2, 3 });
                    fail("Expecting: " + UnsupportedOperationException.class);
                } catch (UnsupportedOperationException ex) {
                    System.err.println("Ignoring expected exception: " + ex);
                }

                journal.abort(tx1);

            }

        } finally {

            journal.destroy();

        }
        
    }

}
