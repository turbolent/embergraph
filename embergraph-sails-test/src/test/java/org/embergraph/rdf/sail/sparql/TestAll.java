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
 * Created on Nov 7, 2007
 */

package org.embergraph.rdf.sail.sparql;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite.
 * 
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

    public static Test suite() {

        final TestSuite suite = new TestSuite(TestAll.class.getPackage()
                .getName());

        /*
         * Test suite for expected AST translation targets. This is our primary
         * parser test suite.
         */
        suite.addTest(TestAll_AST.suite());

        try {

            /*
             * Manifest driven SPARQL parser compliance test suite.
             * 
             * Note: This is the DAWG test suite. It verifies the compliance of
             * the SPARQL parser in terms of correct acceptance and correct
             * rejection, but it does not check the translation targets for the
             * parser and it is not aware of embergraph specific SPARQL extensions.
             */

            // non-manifest driven suite for debugging.
            suite.addTestSuite(EmbergraphSPARQL2ASTParserTest.class);
            
            suite.addTest(Embergraph2ASTSPARQLSyntaxTest.suite());

            suite.addTest(Embergraph2ASTSPARQL11SyntaxTest.suite());
            
            //BLZG-1773
            suite.addTestSuite(TestPrefixDeclProcessor.class);

        } catch (Exception ex) {

            throw new RuntimeException(ex);

        }

        return suite;

    }

}
