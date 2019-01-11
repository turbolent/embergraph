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
 * Created on May 18, 2011
 */

package org.embergraph.btree;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Stress test suite for {@link TestIndexSegmentBuilderWithSmallTree}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll_IndexSegmentBuilderWithSmallTrees extends TestCase {

    /**
     * 
     */
    public TestAll_IndexSegmentBuilderWithSmallTrees() {
    }

    /**
     * @param name
     */
    public TestAll_IndexSegmentBuilderWithSmallTrees(String name) {
        super(name);
    }

    /**
     * Return a test suite which will run each of the unit tests for
     * TestIndexSegmentBuilderWithSmallTrees a number of times. This will
     * exercise some random perturbations in things like whether or not
     * rawRecords are enabled or whether or not we are fully buffering the nodes
     * when building the index segment.
     */
    public static Test suite() {

        final TestSuite suite = new TestSuite("IndexSegmentBuilderWithSmallTrees");

        for (int i = 0; i < 10; i++) {

            final TestSuite suite2 = new TestSuite(
                    TestIndexSegmentBuilderWithSmallTree.class);

            suite.addTest(suite2);

        }

        return suite;

    }

}
