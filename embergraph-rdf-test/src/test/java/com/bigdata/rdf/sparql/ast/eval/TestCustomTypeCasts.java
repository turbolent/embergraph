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
 * Created on November 30, 2015
 */

package com.bigdata.rdf.sparql.ast.eval;

import org.embergraph.rdf.internal.constraints.XsdLongBOp;
import org.embergraph.rdf.internal.constraints.XsdUnsignedLongBOp;

/**
 * Test suite for standard type cast function such as {@link XsdLongBOp} and
 * {@link XsdUnsignedLongBOp}.
 * 
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestCustomTypeCasts extends AbstractDataDrivenSPARQLTestCase {
    
    public TestCustomTypeCasts() {
    }

    public TestCustomTypeCasts(String name) {
        super(name);
    }

    /**
     * Casting to xsd:long (non-standard extension).
     * 
     * @throws Exception
     */
    public void test_type_casts_long() throws Exception {

        new TestHelper(
            "type_cast_long",// testURI
            "type_cast_long.rq", // queryURI
            "empty.trig", // dataURI
            "type_cast_long.srx" // resultURI
        ).runTest();
        
    }

    /**
     * Casting to xsd:unsignedLong (non-standard extension).
     * 
     * @throws Exception
     */
    public void test_type_casts_unsigned_long() throws Exception {

        new TestHelper(
            "type_cast_unsigned_long",// testURI
            "type_cast_unsigned_long.rq", // queryURI
            "empty.trig", // dataURI
            "type_cast_unsigned_long.srx" // resultURI
        ).runTest();
        
    }
}
