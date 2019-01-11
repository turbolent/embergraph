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
 * Created on Oct 14, 2006
 */

package org.embergraph.journal;

import java.io.IOException;
import java.util.Properties;

import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;

import org.embergraph.rawstore.AbstractRawStoreTestCase;
import org.embergraph.rawstore.IRawStore;

/**
 * Test suite for {@link BufferMode#Transient} journals.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */

public class TestTransientJournal extends AbstractJournalTestCase {

    public TestTransientJournal() {
        super();
    }

    public TestTransientJournal(String name) {
        super(name);
    }

    public static Test suite() {

        final TestTransientJournal delegate = new TestTransientJournal(); // !!!! THIS CLASS !!!!

        /*
         * Use a proxy test suite and specify the delegate.
         */

        ProxyTestSuite suite = new ProxyTestSuite(delegate,
                "Transient Journal Test Suite");

        /*
         * List any non-proxied tests (typically bootstrapping tests).
         */
        
        // tests defined by this class.
        suite.addTestSuite(TestTransientJournal.class);

        // test suite for the IRawStore api.
        suite.addTestSuite( TestRawStore.class );

        // Note: test suite not used since there is no file channel to be closed by interrupts.
//        suite.addTestSuite( TestInterrupts.class );

        // test suite for MROW correctness.
        suite.addTestSuite( TestMROW.class );

        // test suite for MRMW correctness.
        suite.addTestSuite( TestMRMW.class );

        /*
         * Pickup the basic journal test suite. This is a proxied test suite, so
         * all the tests will run with the configuration specified in this test
         * class and its optional .properties file.
         */
        suite.addTest(TestJournalBasics.suite());

        return suite;

    }

    public Properties getProperties() {

        final Properties properties = super.getProperties();

        properties.setProperty(Journal.Options.COLLECT_PLATFORM_STATISTICS,
                "false");

        properties.setProperty(Journal.Options.COLLECT_QUEUE_STATISTICS,
                "false");

        properties.setProperty(Journal.Options.HTTPD_PORT, "-1"/* none */);

        properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient.toString());

        return properties;

    }

    /**
     * Verify normal operation and basic assumptions when creating a new journal
     * using {@link BufferMode#Transient}.
     * 
     * @throws IOException
     */
    public void test_create_transient01() throws IOException {

        final Properties properties = getProperties();

        final Journal journal = new Journal(properties);

        try {
        
        final TransientBufferStrategy bufferStrategy = (TransientBufferStrategy) journal.getBufferStrategy();

        assertFalse("isStable",bufferStrategy.isStable());
        assertTrue("isFullyBuffered",bufferStrategy.isFullyBuffered());
        assertEquals(Options.INITIAL_EXTENT, Long.parseLong(Options.DEFAULT_INITIAL_EXTENT),
                bufferStrategy.getExtent());
        assertEquals(Options.MAXIMUM_EXTENT, 0L/*soft limit for transient mode*/,
                bufferStrategy.getMaximumExtent());
        assertEquals(Options.BUFFER_MODE, BufferMode.Transient, bufferStrategy
                .getBufferMode());
        assertEquals("userExtent", bufferStrategy.getExtent(), bufferStrategy
                .getUserExtent());
        
        } finally {
            
            journal.destroy();
            
        }
        
    }
            
    /**
     * Test suite integration for {@link AbstractRawStoreTestCase}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     * 
     * @todo While the transient store uses root blocks there is no means
     *       currently defined to re-open a journal based on a transient store
     *       and hence it is not possible to extend
     *       {@link AbstractRestartSafeTestCase}.
     */
    public static class TestRawStore extends AbstractBufferStrategyTestCase {
        
        public TestRawStore() {
            super();
        }

        public TestRawStore(String name) {
            super(name);
        }

        protected BufferMode getBufferMode() {
            
            return BufferMode.Transient;
            
        }

//        public Properties getProperties() {
//
//            Properties properties = super.getProperties();
//
//            properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient.toString());
//
//            return properties;
//
//        }
//
//        protected IRawStore getStore() {
//            
//            return new Journal(getProperties());
//            
//        }

    }

    /**
     * Test suite integration for {@link AbstractMROWTestCase}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static class TestMROW extends AbstractMROWTestCase {
        
        public TestMROW() {
            super();
        }

        public TestMROW(String name) {
            super(name);
        }

        protected IRawStore getStore() {

            Properties properties = getProperties();
            
            properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient
                    .toString());
            
            return new Journal(properties);//.getBufferStrategy();
            
        }
        
    }

    /**
     * Test suite integration for {@link AbstractMRMWTestCase}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static class TestMRMW extends AbstractMRMWTestCase {
        
        public TestMRMW() {
            super();
        }

        public TestMRMW(String name) {
            super(name);
        }

        protected IRawStore getStore() {

            Properties properties = getProperties();
            
            properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient
                    .toString());
            
            return new Journal(properties);//.getBufferStrategy();
            
        }
        
    }

}
