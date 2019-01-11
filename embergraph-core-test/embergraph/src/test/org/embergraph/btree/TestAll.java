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
package org.embergraph.btree;

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

        final TestSuite suite = new TestSuite(TestAll.class.getPackage()
                .getName());

        // test low level variable length byte[] operations.
        suite.addTestSuite(TestBytesUtil.class);
        suite.addTestSuite(TestGetBitsFromByteArray.class);
        suite.addTestSuite(TestGetBitsFromInt32.class);

        // unsigned byte[] key encoding and decoding.
        suite.addTest(org.embergraph.btree.keys.TestAll.suite());

        // test rabas implementations, including key search and coded data.
        suite.addTest(org.embergraph.btree.raba.TestAll.suite());
        
        // test suite for the B+Tree node and leaf data records.
        suite.addTest(org.embergraph.btree.data.TestAll.suite());

        // core B+Tree API tests, including w/ and w/o persistence.
        suite.addTest(TestAll_BTreeBasics.suite());

        // pick up the index segment test suites.
        suite.addTest(TestAll_IndexSegment.suite());

        /*
         * Test the Map and Set implementations.
         */
        suite.addTestSuite(TestBigdataMap.class);
        suite.addTestSuite(TestBigdataSet.class);

        /*
         * Test fused views, including iterators for the fused view.
         */
        suite.addTest(org.embergraph.btree.view.TestAll.suite());

        /*
         * Test transactional isolation support, including iterators and
         * iterator#remove() for the isolated index.
         */
        suite.addTest(org.embergraph.btree.isolation.TestAll.suite());

        // test index procedures.
        suite.addTest(org.embergraph.btree.proc.TestAll.suite());

        return suite;
        
    }
    
}
