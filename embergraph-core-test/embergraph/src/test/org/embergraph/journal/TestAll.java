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

package org.embergraph.journal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Runs all tests for all journal implementations.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestAll extends TestCase {

    /**
     * 
     */
    public TestAll() {
    }

    /**
     * @param arg0
     */
    public TestAll(String arg0) {
        super(arg0);
    }

    /**
     * Returns a test that will run each of the implementation specific test
     * suites in turn.
     */
    public static Test suite()
    {

        final TestSuite suite = new TestSuite("journal");

        // test ability of the platform to synchronize writes to disk.
        suite.addTestSuite( TestRandomAccessFileSynchronousWrites.class );
        
        // test the ability to (de-)serialize the root addreses.
        suite.addTestSuite( TestCommitRecordSerializer.class );
        
        // test the root block api.
        suite.addTestSuite( TestRootBlockView.class );

        // tests of the index used to access historical commit records
        suite.addTestSuite( TestCommitRecordIndex.class );

        // test suites for file names based on commit counters.
        suite.addTestSuite( TestCommitCounterUtility.class );

        // test suite for ClocksNotSynchronizedException.
        suite.addTestSuite( TestClockSkewDetection.class );

        /*
         * Test a scalable temporary store (uses the transient and disk-only
         * buffer modes).
         */
        suite.addTest( TestTemporaryStore.suite() );
        
        /*
         * Test the different journal modes.
         * 
         * -DminimizeUnitTests="true" is used when building the project site to keep
         * down the nightly build demands.
         */
        
//        if(Boolean.parseBoolean(System.getProperty("minimizeUnitTests","false"))) {

        suite.addTest( TestTransientJournal.suite() );

        /*
         * Commented out since this mode is not used and there is an occasional
         * test failure in:
         * 
         * org.embergraph.journal.TestConcurrentJournal.test_concurrentReadersAreOk
         * 
         * This error is stochastic and appears to be restricted to
         * BufferMode#Direct. This is a journal mode based by a fixed capacity
         * native ByteBuffer serving as a write through cache to the disk. Since
         * the buffer can not be extended, that journal mode is not being
         * excercised by anything. If you like, I can deprecate the Direct
         * BufferMode and turn disable its test suite. (There is also a "Mapped"
         * BufferMode whose tests we are not running due to problems with Java
         * releasing native heap ByteBuffers and closing memory mapped files.
         * Its use is strongly discouraged in the javadoc, but it has not been
         * excised from the code since it might be appropriate for some
         * applications.)
         */
//            suite.addTest( TestDirectJournal.suite() );

            /*
             * Note: The mapped journal is somewhat problematic and its tests are
             * disabled for the moment since (a) we have to pre-allocate large
             * extends; (b) it does not perform any better than other options; and
             * (c) we can not synchronously unmap or delete a mapped file which
             * makes cleanup of the test suites difficult and winds up spewing 200M
             * files all over your temp directory.
             */
            
//            suite.addTest( TestMappedJournal.suite() );

//        }

        // Remove since we are no longer using the DiskOnlyStrategy for the Journal.
        //suite.addTest( TestDiskJournal.suite() );

        suite.addTest( TestWORMStrategy.suite() );

//        suite.addTest( org.embergraph.rwstore.TestAll.suite() );

        // test suite for memory leaks in the journal shutdown protocol.
        suite.addTestSuite(TestJournalShutdown.class);

//        /* @todo This has been moved up to the top-level for how to help
//         * distinguish HA related build errors from Journal build errors.
//        
//         * High Availability test suite.
//         * 
//         * Note: There is a separate test suite for DataService high
//         * availability and for the zookeeper HA integration.
//         */
//        suite.addTest(org.embergraph.journal.ha.TestAll.suite());
        
        return suite;

    }

}
