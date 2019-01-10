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
package org.embergraph.bop;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.embergraph.bop.util.TestBOpUtility;

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
    public TestAll(final String arg0) {
     
        super(arg0);
        
    }

    /**
     * Returns a test that will run each of the implementation specific test
     * suites in turn.
     */
    public static Test suite()
    {

        final TestSuite suite = new TestSuite("bigdata operators");

        // test variable and constant impls.
        suite.addTestSuite(TestVar.class);
        suite.addTestSuite(TestConstant.class);

        // test binding set impls.
        suite.addTest(com.bigdata.bop.bindingSet.TestAll.suite());

        // unit tests for ctor existence and deep copy semantics
        suite.addTestSuite(TestDeepCopy.class);

        // counting variables, etc.
        suite.addTestSuite(TestBOpUtility.class);

        // bop utils.
        suite.addTest(com.bigdata.bop.util.TestAll.suite());

        // constraint operators (EQ, NE, etc).
        suite.addTest(com.bigdata.bop.constraint.TestAll.suite());

        // pure binding set operators.
        suite.addTest(com.bigdata.bop.bset.TestAll.suite());

        // bind(var,expr)
        suite.addTestSuite(TestBind.class);
        
        // index operators.
        suite.addTest(com.bigdata.bop.ndx.TestAll.suite());

        // access path filters
        suite.addTest(com.bigdata.bop.ap.filter.TestAll.suite());

        // access path operators
        suite.addTest(com.bigdata.bop.ap.TestAll.suite());

        // mutation operators
        suite.addTest(com.bigdata.bop.mutation.TestAll.suite());

        // join operators.
        suite.addTest(com.bigdata.bop.join.TestAll.suite());

        // aggregation operators.
        suite.addTest(com.bigdata.bop.solutions.TestAll.suite());

        // Unit tests for named solution set references.
        suite.addTestSuite(TestNamedSolutionSetRef.class);
        
        // query engine.
        suite.addTest(com.bigdata.bop.engine.TestAll.suite());

        // high level query optimization and evaluation.
        suite.addTest(com.bigdata.bop.controller.TestAll.suite());

        // join graph processing (RTO, etc).
        suite.addTest(com.bigdata.bop.joinGraph.TestAll.suite());

        /*
         * Note: This is tested later once we have gone through the core unit
         * tests for the services.
         */
        suite.addTest( com.bigdata.bop.fed.TestAll.suite() );

        return suite;
        
    }
    
}
