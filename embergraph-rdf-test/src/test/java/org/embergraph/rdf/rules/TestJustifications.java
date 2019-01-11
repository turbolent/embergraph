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
 * Created on Nov 3, 2007
 */

package org.embergraph.rdf.rules;

import java.util.Arrays;
import java.util.Properties;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.Var;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.inf.FullyBufferedJustificationIterator;
import org.embergraph.rdf.inf.Justification;
import org.embergraph.rdf.inf.Justification.VisitedSPOSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.TempTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.IMutableRelation;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.rule.Rule;
import org.embergraph.relation.rule.eval.ActionEnum;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.relation.rule.eval.Solution;

/**
 * Test suite for writing, reading, chasing and retracting {@link Justification}s.
 * 
 * @todo test the comparator. it is especially important that that all
 *       justifications for the same entailment are clustered.
 *       <P>
 *       Check for overflow of long.
 *       <p>
 *       Check when this is a longer or shorter array than the other where the
 *       two justifications have the same data in common up to the end of
 *       whichever is the shorter array.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestJustifications extends AbstractRuleTestCase {

    /**
     * 
     */
    public TestJustifications() {
        super();
    }

    /**
     * @param name
     */
    public TestJustifications(String name) {
        super(name);
    }

    protected void assertEquals(Justification expected, Justification actual) {
        
        if(!expected.equals(actual)) {
            
            fail("Expecting "+expected+", not "+actual);
            
        }
        
    }
    
    /**
     * Creates a {@link Justification}, writes it on the store using
     * {@link RDFJoinNexus#newInsertBuffer(IMutableRelation)}, verifies that we
     * can read it back from the store, and then retracts the justified
     * statement and verifies that the justification was also retracted.
     */
    public void test_writeReadRetract() {

        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);

        try {

            if (!store.isJustify()) {

                log.warn("Test skipped - justifications not enabled");

            }
        
            /*
             * the explicit statement that is the support for the rule.
             */

            final IV U = store.addTerm(new URIImpl("http://www.embergraph.org/U"));
            final IV A = store.addTerm(new URIImpl("http://www.embergraph.org/A"));
            final IV Y = store.addTerm(new URIImpl("http://www.embergraph.org/Y"));

            store.addStatements(new SPO[] {//
                    new SPO(U, A, Y, StatementEnum.Explicit) //
                    }, //
                    1);

            assertTrue(store.hasStatement(U, A, Y));
            assertEquals(1, store.getStatementCount());

            final InferenceEngine inf = store.getInferenceEngine();

            final Vocabulary vocab = store.getVocabulary();
            
            // the rule.
            final Rule rule = new RuleRdf01(store.getSPORelation()
                    .getNamespace(), vocab);

            final IJoinNexus joinNexus = store.newJoinNexusFactory(
            		RuleContextEnum.DatabaseAtOnceClosure,
                    ActionEnum.Insert, IJoinNexus.ALL, null/* filter */)
                    .newInstance(store.getIndexManager());
            
            /*
             * The buffer that accepts solutions and causes them to be written
             * onto the statement indices and the justifications index.
             */
            final IBuffer<ISolution[]> insertBuffer = joinNexus.newInsertBuffer(
                    store.getSPORelation());
            
            // the expected justification (setup and verified below).
            final Justification jst;

            // the expected entailment.
            final SPO expectedEntailment = new SPO(//
                    A, vocab.get(RDF.TYPE), vocab.get(RDF.PROPERTY),
                    StatementEnum.Inferred);

            {
            
                final IBindingSet bindingSet = joinNexus.newBindingSet(rule);

                /*
                 * Note: rdfs1 is implemented using a distinct term scan. This
                 * has the effect of leaving the variables that do not appear in
                 * the head of the rule unbound. Therefore we DO NOT bind those
                 * variables here in the test case and they will be represented
                 * as ZERO (0L) in the justifications index and interpreted as
                 * wildcards.
                 */
//                bindingSet.set(Var.var("u"), new Constant<IV>(U));
                bindingSet.set(Var.var("a"), new Constant<IV>(A));
//                bindingSet.set(Var.var("y"), new Constant<IV>(Y));
                
                final ISolution solution = new Solution(joinNexus, rule,
                        bindingSet);

                /*
                 * Verify the justification that will be built from that
                 * solution.
                 */
                {

                    jst = new Justification(solution);

                    /*
                     * Verify the bindings on the head of the rule as
                     * represented by the justification.
                     */
                    assertEquals(expectedEntailment, jst.getHead());

                    /*
                     * Verify the bindings on the tail of the rule as
                     * represented by the justification. Again, note that the
                     * variables that do not appear in the head of the rule are
                     * left unbound for rdfs1 as a side-effect of evaluation
                     * using a distinct term scan.
                     */
                    final SPO[] expectedTail = new SPO[] {//
                            new SPO(NULL, A, NULL, StatementEnum.Inferred)//
                    };
                    
                    if(!Arrays.equals(expectedTail, jst.getTail())) {
                    
                        fail("Expected: "+Arrays.toString(expectedTail)+", but actual: "+jst);
                        
                    }
                    
                }
                
                // insert solution into the buffer.
                insertBuffer.add(new ISolution[]{solution});
                
            }
            
//            SPOAssertionBuffer buf = new SPOAssertionBuffer(store, store,
//                    null/* filter */, 100/* capacity */, true/* justified */);
//
//            assertTrue(buf.add(head, jst));

            // no justifications before hand.
            assertEquals(0L, store.getSPORelation().getJustificationIndex().rangeCount());

            // flush the buffer.
            assertEquals(1L, insertBuffer.flush());

            // one justification afterwards.
            assertEquals(1L, store.getSPORelation().getJustificationIndex().rangeCount());

            /*
             * verify read back from the index.
             */
            {

                final ITupleIterator<Justification> itr = store
                        .getSPORelation().getJustificationIndex()
                        .rangeIterator();

                while (itr.hasNext()) {

                    final ITuple<Justification> tuple = itr.next();
                    
                    // de-serialize the justification from the key.
                    final Justification tmp = tuple.getObject();
                    
                    // verify the same.
                    assertEquals(jst, tmp);

                    // no more justifications in the index.
                    assertFalse(itr.hasNext());
                    
                }
                
            }

            /*
             * test iterator with a single justification. 
             */
            {
             
                final FullyBufferedJustificationIterator itr = new FullyBufferedJustificationIterator(
                        store, expectedEntailment);
                
                assertTrue(itr.hasNext());
                
                final Justification tmp = itr.next();
                
                assertEquals(jst, tmp);
                
            }
            
            // an empty focusStore.
            final TempTripleStore focusStore = new TempTripleStore(store
                    .getIndexManager().getTempStore(), store.getProperties(),
                    store);

            try {

                /*
                 * The inference (A rdf:type rdf:property) is grounded by the
                 * explicit statement (U A Y).
                 */

                assertTrue(Justification.isGrounded(inf, focusStore, store,
                        expectedEntailment, false/* testHead */,
                        true/* testFocusStore */, new VisitedSPOSet(focusStore
                                .getIndexManager())));

                // add the statement (U A Y) to the focusStore.
                focusStore.addStatements(new SPO[] {//
                        new SPO(U, A, Y, StatementEnum.Explicit) //
                        },//
                        1);

                /*
                 * The inference is no longer grounded since we have declared
                 * that we are also retracting its grounds.
                 */
                assertFalse(Justification.isGrounded(inf, focusStore, store,
                        expectedEntailment, false/* testHead */,
                        true/* testFocusStore */, new VisitedSPOSet(focusStore
                                .getIndexManager())));

            } finally {

                /*
                 * Destroy the temp kb, but not the backing TemporaryStore. That
                 * will be destroyed when we destroy the IndexManager associated
                 * with the main store (below).
                 */
                focusStore.destroy();

            }

            /*
             * remove the justified statements.
             */

            assertEquals(1L, store.getAccessPath(expectedEntailment.s,
                    expectedEntailment.p, expectedEntailment.o).removeAll());

            /*
             * verify that the justification for that statement is gone.
             */
            {
                
                final ITupleIterator<?> itr = store.getSPORelation()
                        .getJustificationIndex().rangeIterator();
                
                assertFalse(itr.hasNext());
                
            }
            
        } finally {

            store.__tearDownUnitTest();

        }

    }

}
