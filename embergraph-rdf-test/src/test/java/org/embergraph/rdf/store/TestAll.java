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
 * Created on Oct 19, 2007
 */

package org.embergraph.rdf.store;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Runs tests for each {@link ITripleStore} implementation.
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

        final TestSuite suite = new TestSuite("RDF Stores");

        /*
         * Run each of the kinds of triple stores through both their specific
         * and shared unit tests.
         */

        suite.addTest( org.embergraph.rdf.store.TestTempTripleStore.suite() );
        
        suite.addTest( org.embergraph.rdf.store.TestLocalTripleStore.suite() );

//        suite.addTest( org.embergraph.rdf.store.TestLocalTripleStoreWORM.suite() );

        suite.addTest( org.embergraph.rdf.store.TestLocalTripleStoreWithoutInlining.suite() );

        suite.addTest( org.embergraph.rdf.store.TestLocalTripleStoreWithoutStatementIdentifiers.suite() );

//        suite.addTest( org.embergraph.rdf.store.TestLocalTripleStoreWithIsolatableIndices.suite() );

        // @todo test quad store for LDS and EDS.
//        suite.addTest( org.embergraph.rdf.store.TestLocalQuadStore.suite() );

//        suite.addTest( TestScaleOutTripleStoreWithLocalDataServiceFederation.suite() );

/*
 * @todo We should run this test suite against a CI cluster on a single machine using
 * the full bigdata federation rather than EDS.
 */
//        suite.addTest(org.embergraph.rdf.store.TestScaleOutTripleStoreWithEmbeddedFederation
//                        .suite());

//        if (Boolean.parseBoolean(System.getProperty("maven.test.services.skip",
//                "false"))
//                || !JiniServicesHelper.isJiniRunning()) {
//
//            /*
//             * Test scale-out RDF database.
//             * 
//             * Note: This test suite sets up a local bigdata federation for each
//             * test. See the test suite for more information about required Java
//             * properties.
//             */
//
//            suite.addTest(org.embergraph.rdf.store.TestScaleOutTripleStoreWithJiniFederation
//                    .suite());
//
//        }

        return suite;
        
    }
    
}
