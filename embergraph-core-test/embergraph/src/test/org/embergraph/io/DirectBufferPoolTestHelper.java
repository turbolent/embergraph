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
 * Created on May 27, 2011
 */

package org.embergraph.io;

import junit.extensions.proxy.IProxyTest;
import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * Some helper methods for CI.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DirectBufferPoolTestHelper {

    private final static Logger log = Logger.getLogger(DirectBufferPoolTestHelper.class);

    /**
     * Verify that any buffers acquired by the test have been released.
     * <p>
     * Note: This clears the counter as a side effect to prevent a cascade
     * of tests from being failed.
     */
    public static void checkBufferPools(final TestCase test) {

        checkBufferPools(test, null/*delegate*/);
        
    }

    /**
     * Verify that any buffers acquired by the test have been released (variant
     * when using an {@link IProxyTest}).
     * <p>
     * Note: This clears the counter as a side effect to prevent a cascade of
     * tests from being failed.
     * 
     * @param test
     *            The unit test instance.
     * @param testClass
     *            The instance of the delegate test class for a proxy test
     *            suite. For example, TestWORMStrategy.
     */
    public static void checkBufferPools(final TestCase test,
            final TestCase testClass) {
        
        final long nacquired = DirectBufferPool.totalAcquireCount.get();
        final long nreleased = DirectBufferPool.totalReleaseCount.get();
        DirectBufferPool.totalAcquireCount.set(0L);
        DirectBufferPool.totalReleaseCount.set(0L);
        
        if (nacquired != nreleased) {

            /*
             * At least one buffer was acquired which was never released.
             */

            log.error("Test did not release buffer(s)"
                    + ": nacquired=" + nacquired
                    + ", nreleased=" + nreleased
                    + ", test=" + test.getClass() + "." + test.getName()
                    + (testClass == null ? "" : ", testClass="
                            + testClass.getClass().getName())
            );

        }
        
    }

}
