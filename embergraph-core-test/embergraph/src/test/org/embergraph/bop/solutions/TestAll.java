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
package org.embergraph.bop.solutions;


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

        final TestSuite suite = new TestSuite("solution modifier operators");
        
        /*
         * Slice
         */
        
        // test slice(offset,limit) operator.
        suite.addTestSuite(TestSliceOp.class);

        /*
         * Distinct
         */
        
        // test distinct operator for binding sets using ConcurrentHashMap
        suite.addTestSuite(TestJVMDistinctBindingSets.class);

        // test distinct operator for binding sets using HTree.
        suite.addTestSuite(TestHTreeDistinctBindingSets.class);

        /*
         * Sorting
         */
        
        // Test suite for comparator for IVs used in ORDER BY implementations.
        suite.addTestSuite(TestIVComparator.class);
        
        // in-memory sort operator.
        suite.addTestSuite(TestMemorySortOp.class);

        /*
         * Aggregation
         */
        
        // Validation logic for aggregation operators.
        suite.addTestSuite(TestGroupByState.class);
        
        // Test suite for rewrites of the SELECT and HAVING clauses.
        suite.addTestSuite(TestGroupByRewriter.class);
        
        // In-memory generalized aggregation operator
        suite.addTestSuite(TestMemoryGroupByOp.class);

        // FIXME Enable test for Native memory generalized aggregation operator
//        suite.addTestSuite(TestHTreeGroupByOp.class);

        // Pipelined aggregation operator.
        suite.addTestSuite(TestPipelinedAggregationOp.class);

        return suite;
        
    }
    
}
