/*

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
 * Created on Aug 5, 2009
 */

package org.embergraph.btree.data;

import org.embergraph.btree.raba.codec.CanonicalHuffmanRabaCoder;

/**
 * Test suite using the {@link CanonicalHuffmanRabaCoder} to provide key
 * compression.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestNodeDataRecord_CanonicalHuffman extends AbstractNodeDataRecordTestCase {

    /**
     * 
     */
    public TestNodeDataRecord_CanonicalHuffman() {
    }

    /**
     * @param name
     */
    public TestNodeDataRecord_CanonicalHuffman(String name) {
        super(name);
    }

    @Override
    protected boolean mayGenerateLeaves() {
        return false;
    }

    @Override
    protected boolean mayGenerateNodes() {
        return true;
    }

    protected void setUp() throws Exception {
        
        super.setUp();

        coder = new DefaultNodeCoder(CanonicalHuffmanRabaCoder.INSTANCE);

    }

}
