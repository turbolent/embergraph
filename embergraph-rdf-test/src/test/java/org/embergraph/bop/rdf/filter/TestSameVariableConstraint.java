/**

The Notice below must appear in each file of the Source Code of any
copy you distribute of the Licensed Product.  Contributors to any
Modifications may add their own copyright notices to identify their
own contributions.

License:

The contents of this file are subject to the CognitiveWeb Open Source
License Version 1.1 (the License).  You may not copy or use this file,
in either source code or executable form, except in compliance with
the License.  You may obtain a copy of the License from

  http://www.CognitiveWeb.org/legal/license/

Software distributed under the License is distributed on an AS IS
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
the License for the specific language governing rights and limitations
under the License.

Copyrights:

Portions created by or assigned to CognitiveWeb are Copyright
(c) 2003-2003 CognitiveWeb.  All Rights Reserved.  Contact
information for CognitiveWeb is available at

  http://www.CognitiveWeb.org

Portions Copyright (c) 2002-2003 Bryan Thompson.

Acknowledgements:

Special thanks to the developers of the Jabber Open Source License 1.0
(JOSL), from which this License was derived.  This License contains
terms that differ from JOSL.

Special thanks to the CognitiveWeb Open Source Contributors for their
suggestions and support of the Cognitive Web.

Modifications:

*/
/*
 * Created on Sep 30, 2009
 */

package org.embergraph.bop.rdf.filter;

import junit.framework.TestCase2;

import org.embergraph.bop.Constant;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.filter.SameVariableConstraint;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.test.MockTermIdFactory;

/**
 * Test suite for {@link SameVariableConstraint}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSameVariableConstraint extends TestCase2 {

    /**
     * 
     */
    public TestSameVariableConstraint() {
     
    }

    /**
     * @param name
     */
    public TestSameVariableConstraint(String name) {
        super(name);
    }

    private String relation;

    private Constant<IV> a;
    
    private Constant<IV> b;
    
    private Constant<IV> c;
    
    private Constant<IV> d;
    
    private MockTermIdFactory factory;
    
    protected void setUp() throws Exception {

        super.setUp();

        factory = new MockTermIdFactory();

        relation = "r";

        a = new Constant<IV>(factory.newTermId(VTE.URI));

        b = new Constant<IV>(factory.newTermId(VTE.URI));

        c = new Constant<IV>(factory.newTermId(VTE.URI));

        d = new Constant<IV>(factory.newTermId(VTE.URI));

    }
    
    protected void tearDown() throws Exception {
    
        factory = null;

        relation = null;

        a = b = c = d = null;
        
        super.tearDown();
        
    }
    
    public void test_no_dups1() {

        // (a,b,c,d)
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, b, c, d)));

        // (?a,b,c,d)
        // (a,?b,c,d)
        // (a,b,?c,d)
        // (a,b,c,?d)
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, Var.var("a"), b, c, d)));

        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, Var.var("b"), c, d)));

        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, b, Var.var("c"), d)));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, b, c, Var.var("d"))));

        // (?a,?b,c,d)
        // (?a,b,?c,d)
        // (?a,b,c,?d)
        // (a,?b,?c,d)
        // (a,?b,c,?d)
        // (a,b,?c,?d)
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, Var.var("a"), Var.var("b"), c, d)));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, Var.var("a"), c, Var.var("c"), d)));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, Var.var("a"), b, c, Var.var("d"))));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, Var.var("b"), Var.var("c"), d)));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, Var.var("b"), c, Var.var("d"))));
        
        assertNull(SameVariableConstraint.newInstance(new SPOPredicate(
                relation, a, b, Var.var("c"), Var.var("d"))));
        
    }
    
    // (?a,?a,c,d)
    // (?a,b,?a,d)
    // (?a,b,c,?a)
    // (a,?b,?b,d)
    // (a,?b,c,?b)
    // (a,b,?c,?c)
    // (a,?b,?c,?c)
    // (?a,?a,?a,d) 
    public void test_one_dup() {

        {
         
            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("a"), c, d));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 0, 1 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), b, Var.var("a"), d));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 0, 2 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), b, c, Var.var("a")));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 0, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            a, Var.var("b"), Var.var("b"), d));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 1, 2 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            a, Var.var("b"), c, Var.var("b")));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 1, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            a, b, Var.var("c"), Var.var("c")));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 2, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            a, Var.var("c"), Var.var("c"), Var.var("c")));

            assertNotNull(constraint);

            assertEquals(new int[] { 3, 1, 2, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("c"), b, Var.var("c"), Var.var("c")));

            assertNotNull(constraint);

            assertEquals(new int[] { 3, 0, 2, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("c"), Var.var("c"), c, Var.var("c")));

            assertNotNull(constraint);

            assertEquals(new int[] { 3, 0, 1, 3 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("a"), c, Var.var("d")));

            assertNotNull(constraint);

            assertEquals(new int[] { 2, 0, 1 }, constraint.getIndices());

        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("a"), Var.var("a"), Var.var("d")));

            assertNotNull(constraint);

            assertEquals(new int[] { 3, 0, 1, 2 }, constraint.getIndices());

        }

    }

    /*
     * Note: this test depends on the vars being encoded into the indices[] in
     * their presentation order.
     */
    // (?a,?a,?b,?b)
    // (?a,?b,?a,?b)
    // (?a,?b,?b,?a)
    public void test_two_dups() {

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("a"), Var.var("b"), Var.var("b")));

            assertNotNull(constraint);

            assertEquals(new int[] {
                    2, 0, 1,
                    2, 2, 3,
                    }, constraint.getIndices());
            
        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("b"), Var.var("a"), Var.var("b")));

            assertNotNull(constraint);

            assertEquals(new int[] {
                    2, 0, 2,
                    2, 1, 3,
                    }, constraint.getIndices());
            
        }

        {

            final SameVariableConstraint<ISPO> constraint = SameVariableConstraint
                    .newInstance(new SPOPredicate(relation,
                            Var.var("a"), Var.var("b"), Var.var("b"), Var.var("a")));

            assertNotNull(constraint);

            assertEquals(new int[] {
                    2, 0, 3,
                    2, 1, 2,
                    }, constraint.getIndices());
            
        }

    }

}
