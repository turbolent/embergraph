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
package org.embergraph.relation;

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

        final TestSuite suite = new TestSuite("relations");

        // data declaration layer.
        suite.addTest(org.embergraph.relation.ddl.TestAll.suite());
        
        // test suite for rules, but not rule execution.
        suite.addTest(org.embergraph.relation.rule.TestAll.suite());
        
        // test suite for access paths.
        suite.addTest(org.embergraph.relation.accesspath.TestAll.suite());

        // test suite for locating resources.
        suite.addTest(org.embergraph.relation.locator.TestAll.suite());
        
        /*
         * Note: The relation impls, access path impls, and rule execution are
         * currently tested in the context of the RDF DB.
         */
//        suite.addTest(org.embergraph.relation.rdf.TestAll.suite());
        
        return suite;
        
    }
    
}
