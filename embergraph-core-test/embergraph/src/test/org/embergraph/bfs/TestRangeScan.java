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
 * Created on Feb 15, 2008
 */

package org.embergraph.bfs;

import org.embergraph.sparse.SparseRowStore;

/**
 * Test suite for {@link EmbergraphFileSystem#getDocumentHeaders(String, String)}.
 * <p>
 * Note: The correct behavior of this method depends on the
 * {@link SparseRowStore}'s logical row iterator.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRangeScan extends AbstractRepositoryTestCase {

    /**
     * 
     */
    public TestRangeScan() {
    }

    /**
     * @param arg0
     */
    public TestRangeScan(String arg0) {
        super(arg0);
    }

    public void test_rangeScan() {
        
        fail("write test");
        
    }
    
}
