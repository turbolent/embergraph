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
 * Created on Oct 18, 2007
 */

package org.embergraph.rdf.store;

import java.io.File;
import java.util.Properties;

import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;

import org.embergraph.btree.BTree;
import org.embergraph.journal.Options;

/**
 * Proxy test suite for {@link LocalTripleStore} when the backing indices are
 * {@link BTree}s. This configuration does NOT support transactions since the
 * various indices are NOT isolatable.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestLocalTripleStoreWithoutInlining extends AbstractTestCase {

    /**
     * 
     */
    public TestLocalTripleStoreWithoutInlining() {
    }

    public TestLocalTripleStoreWithoutInlining(String name) {
        super(name);
    }
    
    public static Test suite() {

        final TestLocalTripleStoreWithoutInlining delegate = new TestLocalTripleStoreWithoutInlining(); // !!!! THIS CLASS !!!!

        /*
         * Use a proxy test suite and specify the delegate.
         */

        final ProxyTestSuite suite = new ProxyTestSuite(delegate,
                "Local Triple Store With Provenance Test Suite Without Inlining");

        /*
         * List any non-proxied tests (typically bootstrapping tests).
         */

        // ...
//        suite.addTestSuite(TestCompletionScan.class);
        
        /*
         * Proxied test suite for use only with the LocalTripleStore.
         */

        suite.addTestSuite(TestLocalTripleStoreTransactionSemantics.class);

        /*
         * Pickup the basic triple store test suite. This is a proxied test
         * suite, so all the tests will run with the configuration specified in
         * this test class and its optional .properties file.
         */

        // basic test suite.
        suite.addTest(TestTripleStoreBasics.suite());
        
        // rules, inference, and truth maintenance test suite.
        suite.addTest( org.embergraph.rdf.rules.TestAll.suite() );

        return suite;
        
    }

    public Properties getProperties() {

        final Properties properties = super.getProperties();

        // turn on statement identifiers.
        properties
                .setProperty(
                        org.embergraph.rdf.store.AbstractTripleStore.Options.STATEMENT_IDENTIFIERS,
                        "true");

        // triples only.
        properties.setProperty(
                org.embergraph.rdf.store.AbstractTripleStore.Options.QUADS,
                "false");

        // do not inline anything.
        properties.setProperty(
                org.embergraph.rdf.store.AbstractTripleStore.Options.INLINE_XSD_DATATYPE_LITERALS,
                "false");
        
//        properties.setProperty(
//                org.embergraph.rdf.store.AbstractTripleStore.Options.NESTED_SUBQUERY,
//                "true");

        return properties;

    }
    
    protected AbstractTripleStore getStore(final Properties properties) {
        
        return LocalTripleStore.getInstance( properties );
        
    }
 
    /**
     * Re-open the same backing store.
     * 
     * @param store
     *            the existing store.
     * 
     * @return A new store.
     * 
     * @exception Throwable
     *                if the existing store is closed, or if the store can not
     *                be re-opened, e.g., from failure to obtain a file lock,
     *                etc.
     */
    protected AbstractTripleStore reopenStore(final AbstractTripleStore store) {

        // close the store.
        store.close();

        if (!store.isStable()) {

            throw new UnsupportedOperationException(
                    "The backing store is not stable");

        }

        // Note: clone to avoid modifying!!!
        final Properties properties = (Properties) getProperties().clone();

        // Turn this off now since we want to re-open the same store.
        properties.setProperty(Options.CREATE_TEMP_FILE, "false");

        // The backing file that we need to re-open.
        final File file = ((LocalTripleStore) store).getIndexManager().getFile();

        assertNotNull(file);

        // Set the file property explicitly.
        properties.setProperty(Options.FILE, file.toString());

        return LocalTripleStore.getInstance(properties);

    }

}
