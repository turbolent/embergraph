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
 * Created on Nov 29, 2007
 */

package org.embergraph.service;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for embedded services.
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
    public static Test suite() {

        final TestSuite suite = new TestSuite("bigdata services");

        // event handling
        suite.addTestSuite(TestEventParser.class);
        suite.addTestSuite(TestEventReceiver.class);
        
        // tests of the round-robin aspects of the LBS (isolated behaviors).
        suite.addTestSuite(TestLoadBalancerRoundRobin.class);

        // tests for the ResourceService.
        suite.addTest(TestAll_ResourceService.suite());
        
        // tests of the metadata index.
        suite.addTestSuite(TestMetadataIndex.class);

        // tests of the client's view of a scale-out index.
        suite.addTest(org.embergraph.service.ndx.TestAll.suite());
        
        // test ability to re-open an embedded federation.
        suite.addTestSuite(TestRestartSafe.class);

        // unit tests for the distributed transaction service's snapshots.
        suite.addTestSuite(TestSnapshotHelper.class);

        // unit tests of the commit time index for the dist. transaction service.
        suite.addTestSuite(TestDistributedTransactionServiceRestart.class);
        
        // unit tests of single-phase and distributed tx commit protocol.
        suite.addTestSuite(TestDistributedTransactionService.class);

        // test suite for dynamic sharding.
        suite.addTest(TestAll_DynamicSharding.suite());
        
        // test scale-out operator semantics. 
        /*
         * Note: this was being run 3 times (!). It is invoked out of the
         * org.embergraph.bop test suite now.
         */
//        suite.addTest(org.embergraph.bop.fed.TestAll.suite());
        
        /*
         * Stress test of concurrent clients writing on a single data service.
         */
        suite.addTestSuite(StressTestConcurrent.class);

        return suite;
        
    }
    
}
