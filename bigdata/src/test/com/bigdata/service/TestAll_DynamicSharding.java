/**

Copyright (C) SYSTAP, LLC 2006-2015.  All rights reserved.

Contact:
     SYSTAP, LLC
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@systap.com

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
 * Created on Sep 6, 2010
 */

package com.bigdata.service;

import junit.framework.Test;
import junit.framework.TestCase2;
import junit.framework.TestSuite;

/**
 * Test suite for dynamic sharding.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll_DynamicSharding extends TestCase2 {

    /**
     * 
     */
    public TestAll_DynamicSharding() {
    }

    /**
     * @param name
     */
    public TestAll_DynamicSharding(String name) {
        super(name);
    }

    /**
     * Returns a test that will run each of the implementation specific test
     * suites in turn.
     */
    public static Test suite() {

        final TestSuite suite = new TestSuite("dynamic sharding");

        // test basic journal overflow scenario.
        suite.addTestSuite(TestOverflow.class);

        // test suite for GRS overflow.
        suite.addTestSuite(TestOverflowGRS.class);

        // test split/join (inserts eventually split; deletes eventually join).
        suite.addTestSuite(TestSplitJoin.class);

        // test scatter splits with 2DS.
        suite.addTestSuite(TestScatterSplit.class);

        // test journal overflow scenarios (move)
        suite.addTestSuite(TestMove.class);

        return suite;

    }

}
