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
package org.embergraph.bop.fed;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Aggregates test suites into increasing dependency order.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
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

        final TestSuite suite = new TestSuite("scale-out operator evaluation");

        // unit tests for mapping binding sets over shards. 
        suite.addTest(com.bigdata.bop.fed.shards.TestAll.suite());

        // unit tests for mapping binding sets over nodes.
        // @todo uncomment this test suite when the functionality is implemented.
//        suite.addTest(com.bigdata.bop.fed.nodes.TestAll.suite());

        /*
         * Chunk message tests.
         */
        
        // The payload is inline with the RMI message.
        suite.addTestSuite(TestThickChunkMessage.class);

        // The payload is transfered using NIO and the ResourceService.
        suite.addTestSuite(TestNIOChunkMessage.class);

        // unit tests for a remote access path.
        suite.addTestSuite(TestRemoteAccessPath.class);
        
        // look for memory leaks in the query engine factory.
        suite.addTestSuite(TestQueryEngineFactory.class);
        
        /*
         * Unit tests for the federated query engine against an embedded
         * federation with a single data service.
         * 
         * Note: The multi-data service test suites are located in the
         * bigdata-jini module since they must be executed against a full
         * federation.
         */
        suite.addTestSuite(TestFederatedQueryEngine.class);
        
        return suite;
        
    }
    
}
