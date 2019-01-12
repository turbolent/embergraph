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
 * Created on Oct 25, 2007
 */

package org.embergraph.rdf.spo;

import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.AbstractTripleStoreTestCase;
import org.embergraph.rdf.store.TestTripleStore;
import org.embergraph.relation.accesspath.AccessPath;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.test.MockTermIdFactory;
import org.openrdf.model.Statement;

/**
 * Test suite for {@link SPOAccessPath}.
 * <p>
 * See also {@link TestTripleStore} which tests some of this stuff.
 * 
 * FIXME write tests for SLICE with non-zero offset and non-zero LIMIT.
 * 
 * FIXME write tests for SLICE where the maximum fully buffered limit is
 * exceeded so we are forced to use the asynchronous iterator on
 * {@link AccessPath}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSPOAccessPath extends AbstractTripleStoreTestCase {

    /**
     * 
     */
    public TestSPOAccessPath() {
        super();
    }

    /**
     * @param name
     */
    public TestSPOAccessPath(String name) {
        super(name);
    }

    private MockTermIdFactory factory;
    
    protected void setUp() throws Exception {
        
        super.setUp();
        
        factory = new MockTermIdFactory();
        
    }

    protected void tearDown() throws Exception {
        
        super.tearDown();
        
        factory = null;
        
    }

    /**
     * There are 8 distinct triple pattern bindings for a triple store that
     * select among 3 distinct access paths.
     */
    public void test_getAccessPath() {
       
        final AbstractTripleStore store = getStore();

        // constants used for s,p,o,c when bound. 0L used when unbound.
        final IV<?,?> S = factory.newTermId(VTE.URI, 1);
        final IV<?,?> P = factory.newTermId(VTE.URI, 2);
        final IV<?,?> O = factory.newTermId(VTE.URI, 3);
        final IV<?,?> C = factory.newTermId(VTE.URI, 4);
        final IV<?,?> _ = factory.newTermId(VTE.URI, 0);

        try {

            final SPORelation r = store.getSPORelation();

            if (store.isQuads()) {

                /*
                 * For a quad store there are 16 distinct binding patterns that
                 * select among 6 distinct access paths. there are some quad
                 * patterns which could be mapped onto more than one access
                 * path, but the code here checks the expected mapping. These
                 * mappings are similar to those in YARS2, but are the mappings
                 * generated by the "Magic" tuple logic.
                 */

                // SPOC
                assertEquals(SPOKeyOrder.SPOC, r.getAccessPath(_, _, _, _).getKeyOrder());
                assertEquals(SPOKeyOrder.SPOC, r.getAccessPath(S, _, _, _).getKeyOrder());
                assertEquals(SPOKeyOrder.SPOC, r.getAccessPath(S, P, _, _).getKeyOrder());
                assertEquals(SPOKeyOrder.SPOC, r.getAccessPath(S, P, O, _).getKeyOrder());
                assertEquals(SPOKeyOrder.SPOC, r.getAccessPath(S, P, O, C).getKeyOrder());
                
                // POCS
                assertEquals(SPOKeyOrder.POCS, r.getAccessPath(_, P, _, _).getKeyOrder());
                assertEquals(SPOKeyOrder.POCS, r.getAccessPath(_, P, O, _).getKeyOrder());
                assertEquals(SPOKeyOrder.POCS, r.getAccessPath(_, P, O, C).getKeyOrder());
                
                // OCSP
                assertEquals(SPOKeyOrder.OCSP, r.getAccessPath(_, _, O, _).getKeyOrder());
                assertEquals(SPOKeyOrder.OCSP, r.getAccessPath(_, _, O, C).getKeyOrder());
                assertEquals(SPOKeyOrder.OCSP, r.getAccessPath(S, _, O, C).getKeyOrder());
                
                // CSPO
                assertEquals(SPOKeyOrder.CSPO, r.getAccessPath(_, _, _, C).getKeyOrder());
                assertEquals(SPOKeyOrder.CSPO, r.getAccessPath(S, _, _, C).getKeyOrder());
                assertEquals(SPOKeyOrder.CSPO, r.getAccessPath(S, P, _, C).getKeyOrder());
                
                // PCSO
                assertEquals(SPOKeyOrder.PCSO, r.getAccessPath(_, P, _, C).getKeyOrder());

                // SOPC
                assertEquals(SPOKeyOrder.SOPC, r.getAccessPath(S, _, O, _).getKeyOrder());

            } else {
                
                assertEquals(SPOKeyOrder.SPO, r.getAccessPath(NULL, NULL, NULL,
                        NULL).getKeyOrder());

                assertEquals(SPOKeyOrder.SPO, r.getAccessPath(S, NULL, NULL,
                        NULL).getKeyOrder());

                assertEquals(SPOKeyOrder.SPO, r.getAccessPath(S, S, NULL, NULL)
                        .getKeyOrder());

                assertEquals(SPOKeyOrder.SPO, r.getAccessPath(S, S, S, NULL)
                        .getKeyOrder());

                assertEquals(SPOKeyOrder.POS, r.getAccessPath(NULL, S, NULL,
                        NULL).getKeyOrder());

                assertEquals(SPOKeyOrder.POS, r.getAccessPath(NULL, S, S, NULL)
                        .getKeyOrder());

                assertEquals(SPOKeyOrder.OSP, r.getAccessPath(NULL, NULL, S,
                        NULL).getKeyOrder());

                assertEquals(SPOKeyOrder.OSP, r.getAccessPath(S, NULL, S, NULL)
                        .getKeyOrder());

            }

        } finally {

            store.__tearDownUnitTest();

        }

    }

    /**
     * Unit test for predicate patterns in which the same variable appears in
     * more than one position of a triple pattern. The access path should
     * enforce a constraint to ensure that only elements having the same value
     * in each position for the same variable are visited by its iterator.
     * <p>
     * Note: This test applies to the triple store, provenance, and quad store
     * modes.
     */
    public void test_sameVariableConstraint_triples() {

        final AbstractTripleStore store = getStore();
        
        try {

            final EmbergraphValueFactory f = store.getValueFactory();
            
            final EmbergraphURI s1 = f.createURI("http://www.embergraph.org/rdf#s1");
            final EmbergraphURI s2 = f.createURI("http://www.embergraph.org/rdf#s2");
            final EmbergraphURI p1 = f.createURI("http://www.embergraph.org/rdf#p1");
            final EmbergraphURI o1 = f.createURI("http://www.embergraph.org/rdf#o1");
            final EmbergraphURI p2 = f.createURI("http://www.embergraph.org/rdf#p2");
            final EmbergraphURI o2 = f.createURI("http://www.embergraph.org/rdf#o2");

            {

                final StatementBuffer<Statement> buffer = new StatementBuffer<Statement>(
                        store, 10);

                buffer.add(s1, p1, o1);

                buffer.add(s1, s1, o1);

                buffer.add(s2, p2, o2);

                buffer.add(s1, p2, o2);

                buffer.flush();

            }

            // no shared variable (?g, ?h, o1)
            {

                final SPOPredicate predicate = new SPOPredicate(
                        store.getSPORelation().getNamespace(),
                        Var.var("g"), // s
                        Var.var("h"), // p
                        new Constant<IV>(o1.getIV()) // o
                );

                final IAccessPath<ISPO> accessPath = store.getSPORelation()
                        .getAccessPath(predicate);

                assertSameSPOs(new ISPO[] { // FIXME TERMS REFACTOR FAILS HERE
                                new SPO(s1.getIV(), p1.getIV(), o1
                                        .getIV(), StatementEnum.Explicit),
                                new SPO(s1.getIV(), s1.getIV(), o1
                                        .getIV(), StatementEnum.Explicit),
                        }, accessPath.iterator());
            }

            // shared 'g' variable (?g, ?g, o1)
            {
                final SPOPredicate predicate = new SPOPredicate(
                        store.getSPORelation().getNamespace(),
                        Var.var("g"), // s
                        Var.var("g"), // s
                        new Constant<IV>(o1.getIV()) // o
                );

                final IAccessPath<ISPO> accessPath = store.getSPORelation()
                        .getAccessPath(predicate);

                assertSameSPOs(new ISPO[] {
                        new SPO(s1.getIV(), s1.getIV(), o1.getIV(),
                                StatementEnum.Explicit),
                        }, accessPath.iterator());
            }

        } finally {

            store.__tearDownUnitTest();

        }
        
    }

    /**
     * Unit test for predicate patterns in which the same variable appears in
     * more than one position of a quad pattern. The access path should enforce
     * a constraint to ensure that only elements having the same value in each
     * position for the same variable are visited by its iterator.
     * <p>
     * Note: This test only applies to the quad store mode.
     * <p>
     * Note: In the provenance mode, it is impossible for a statement to use its
     * own statement identifier in any position other than the quad position.
     * Therefore any access path which was constrained such that s, p, or o used
     * shared a variable with c would result in an empty access path in the
     * data.
     */
    public void test_sameVariableConstraint_quads() {

        final AbstractTripleStore store = getStore();
        
        try {

            if(!store.isQuads()) {

                /*
                 * @todo modify test to work for triple store also? This is easy
                 * enough to do with an (s,p,o) predicate in which s and o are
                 * or s and p bound to the same variable.
                 */
         
                log.warn("Unit test requires quads.");
                
                return;
                
            }
            
            final EmbergraphValueFactory f = store.getValueFactory();
            
            final EmbergraphURI graphA = f.createURI("http://www.embergraph.org/graphA");
            final EmbergraphURI graphB = f.createURI("http://www.embergraph.org/graphB");
            final EmbergraphURI s = f.createURI("http://www.embergraph.org/rdf#s");
            final EmbergraphURI p1 = f.createURI("http://www.embergraph.org/rdf#p1");
            final EmbergraphURI o1 = f.createURI("http://www.embergraph.org/rdf#o1");
            final EmbergraphURI p2 = f.createURI("http://www.embergraph.org/rdf#p2");
            final EmbergraphURI o2 = f.createURI("http://www.embergraph.org/rdf#o2");

            {

                final StatementBuffer<Statement> buffer = new StatementBuffer<Statement>(
                        store, 10);

                buffer.add(graphA, p1, o1, graphA);

                buffer.add(graphA, p2, o2, graphA);

                buffer.add(s, p1, o1, graphA);

                buffer.add(s, p2, o2, graphB);

                buffer.flush();

            }

            // no shared variable (?g, p1, o1, ?h)
            {

                final SPOPredicate predicate = new SPOPredicate(
                        new BOp[] { Var.var("g"), // s
                                new Constant<IV>(p1.getIV()), // p
                                new Constant<IV>(o1.getIV()), // o
                                Var.var("h") // c
                        }, new NV(IPredicate.Annotations.RELATION_NAME,
                                new String[] { store.getSPORelation()
                                        .getNamespace()
                                }));

                final IAccessPath<ISPO> accessPath = store.getSPORelation()
                        .getAccessPath(predicate);

                assertSameSPOs(new ISPO[] {
                                new SPO(graphA.getIV(), p1.getIV(), o1
                                        .getIV(), graphA.getIV(),
                                        StatementEnum.Explicit),
                                new SPO(s.getIV(), p1.getIV(), o1
                                        .getIV(), graphA.getIV(),
                                        StatementEnum.Explicit),
                        }, accessPath.iterator());
            }

            // shared 'g' variable (?g, p1, o1, ?g)
            {
                final SPOPredicate predicate = new SPOPredicate(
                        new BOp[] { Var.var("g"), // s
                                new Constant<IV>(p1.getIV()), // p
                                new Constant<IV>(o1.getIV()), // o
                                Var.var("g") // c
                        }, new NV(IPredicate.Annotations.RELATION_NAME,
                                new String[] { store.getSPORelation()
                                        .getNamespace() }));

                final IAccessPath<ISPO> accessPath = store.getSPORelation()
                        .getAccessPath(predicate);

                assertSameSPOs(new ISPO[] {
                        new SPO(graphA.getIV(), p1.getIV(), o1
                                .getIV(), graphA.getIV(),
                                StatementEnum.Explicit),
                        }, accessPath.iterator());
            }
            
        } finally {

            store.__tearDownUnitTest();

        }
        
    }
    
    /**
     * @todo write tests of slice where offset=0, offset>0. test with limit at
     *       fence posts (0,1) and with limit GT the maximum that can be fully
     *       buffered. verify stable result sets by using a slice to page
     *       through the results.
     */
    public void test_slice() {
        
//        fail("write tests");
        
    }
    
}
