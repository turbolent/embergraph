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
 * Created on Aug 10, 2007
 */

package org.embergraph.search;

import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.bigdata.journal.AbstractIndexManagerTestCase;
import com.bigdata.journal.IIndexManager;
import com.bigdata.service.TestEDS;
import com.bigdata.service.TestJournal;

/**
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
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

    public static Test suite()
    {

        final TestSuite suite = new TestSuite("Full Text Index and Search");

        // @todo test other facets of search that do not interact with persistence.
        
        // search backed by a Journal.
        suite.addTest(proxySuite(new TestJournal("Journal Search"),"Journal"));

//        // search backed by LDS.
//        suite.addTest(proxySuite(new TestLDS("LDS Search"),"LDS"));

        // search backed by EDS.
        suite.addTest(proxySuite(new TestEDS("EDS Search"),"EDS"));
        
        suite.addTestSuite(TestLanguageRange.class);

        return suite;

    }

    /**
     * Create and populate a {@link ProxyTestSuite} with the unit tests that we
     * will run against any of the {@link IIndexManager} implementations.
     * 
     * @param delegate
     *            The delegate for the proxied unit tests.
     * @param name
     *            The name of the test suite.
     * @return The {@link ProxyTestSuite} populated with the unit tests.
     */
    protected static ProxyTestSuite proxySuite(
            final AbstractIndexManagerTestCase<? extends IIndexManager> delegate,
            final String name) {

        final ProxyTestSuite suite = new ProxyTestSuite(delegate, name);

        // test key formation
        suite.addTestSuite(TestKeyBuilder.class);
 
        // test of search correctness, focusing on cosine computations.
        suite.addTestSuite(TestSearch.class);
        
        // test of prefix search
        suite.addTestSuite(TestPrefixSearch.class);
        
        // test verifies search index is restart safe.
        suite.addTestSuite(TestSearchRestartSafe.class);
        
        // Check behavior of DefaultAnalyzerFactory, see also trac 915
        suite.addTestSuite(TestDefaultAnalyzerFactory.class);
        
        // Check default behavior of ConfigurableAnalyzerFactory
        // which is intended to be the same as the intended
        // behavior of DefaultAnalyzerFactory
        suite.addTestSuite(TestConfigurableAsDefaultAnalyzerFactory.class);
        suite.addTestSuite(TestConfigurableAnalyzerFactory.class);
        suite.addTestSuite(TestUnconfiguredAnalyzerFactory.class);

        return suite;
    }
    
}
