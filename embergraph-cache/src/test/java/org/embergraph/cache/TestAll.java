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
 * Created on Apr 21, 2006
 */
package org.embergraph.cache;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.embergraph.testutil.ExperimentDriver;

/**
 * Aggregates unit tests into dependency order.
 * 
 * @version $Id$
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestAll extends TestCase {

    /**
     * 
     */
    public TestAll() {
        super();
    }

    /**
     * @param arg0
     */
    public TestAll(String arg0) {
        super(arg0);
    }

    public static junit.framework.Test suite() {
        
        final TestSuite suite = new TestSuite("cache");
        
        suite.addTestSuite(TestRingBuffer.class);
        
        suite.addTestSuite(TestHardReferenceQueue.class);

        suite.addTestSuite(TestSynchronizedHardReferenceQueueWithTimeout.class);

        suite.addTestSuite(TestHardReferenceQueueWithBatchingUpdates.class);

       //Disabled see BLZG-1417 
       // suite.addTestSuite(TestConcurrentWeakValueCacheWithBatchingUpdates.class);
        
//        // Test all ICacheEntry implementations.
//        retval.addTestSuite( TestCacheEntry.class );

        // Test LRU semantics.
        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestLRUCache.class);

        // Test cache semantics with weak/soft reference values.
        suite.addTestSuite(TestWeakValueCache.class);

        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestStoreAndAddressLRUCache.class);

        // Note: This implementation is not used.
//        suite.addTestSuite(TestHardReferenceGlobalLRU.class);

        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestHardReferenceGlobalLRURecycler.class);

        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestHardReferenceGlobalLRURecyclerExplicitDeleteRequired.class);

        /*
         * A high concurrency cache based on the infinispan project w/o support
         * for memory cap. This implementation has the disadvantage that we can
         * not directly manage the amount of memory which will be used by the
         * cache. It has pretty much been replaced by the BCHMGlobalLRU2, which
         * gets tested below.
         *
         * @todo commented out since causing a problem w/ the CI builds (live
         * lock).  I am not sure which of these two access policies is at fault.
         * I suspect the LRU since it has more aberrant behavior.
         */
//        suite.addTestSuite(TestBCHMGlobalLRU.class); // w/ LRU access policy
//        suite.addTestSuite(TestBCHMGlobalLRUWithLIRS.class); // w/ LIRS 

        /*
         * These are test suites for the same high concurrency cache with
         * support for memory cap. The cache can be configured with thread-lock
         * buffers or striped locks, so we test it both ways.
         */
        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestBCHMGlobalLRU2WithThreadLocalBuffers.class);
//        suite.addTestSuite(TestBCHMGlobalLRU2WithThreadLocalBuffersAndLIRS.class);
        //BLZG-1497 moved to org.embergraph.cache.lru
        //suite.addTestSuite(TestBCHMGlobalLRU2WithStripedLocks.class);
//        suite.addTestSuite(TestBCHMGlobalLRU2WithStripedLocksAndLIRS.class);

        /*
         * Run the stress tests.
         * 
         * @todo I have commented this out since it is suspect of failing the
         * build. Probably one of the cache implementations is experiencing high
         * contention on the CI machine (which has more cores). 5/21/2010 BBT.
         * See above.  This appears to be one of the infinispan-based caches.
         */
//        suite.addTestSuite(StressTests.class);

        return suite;
    }

    /**
     * Glue class used to execute the stress tests.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static class StressTests extends TestCase {

        public StressTests() {

        }

        public StressTests(String name) {
            super(name);
        }

        /**
         * FIXME Modify the stress test configuration file to run each condition
         * of interest. It is only setup for a few conditions right now.
         */
        public void test() throws Exception {
            ExperimentDriver
                    .doMain(
                            new File(
                                    "embergraph/src/test/org/embergraph/cache/StressTestGlobalLRU.xml"),
                            1/* nruns */, true/* randomize */);
        }
    }

}
