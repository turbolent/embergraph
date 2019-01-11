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
 * Created on Oct 30, 2007
 */

package org.embergraph.rdf.rules;

import java.util.Properties;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.inf.BackchainTypeResourceIterator;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.BigdataURI;
import org.embergraph.rdf.model.BigdataValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.rio.IStatementBuffer;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.striterator.IChunkedOrderedIterator;

/**
 * Test suite for {@link BackchainTypeResourceIterator}.
 * 
 * @todo write a test where we compute the forward closure of a data set with (x
 *       type Resource) entailments included in the rule set and then compare it
 *       to the forward closure of the same data set computed without those
 *       entailments and using backward chaining to supply the entailments. The
 *       result graphs should be equals.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestBackchainTypeResourceIterator extends AbstractRuleTestCase {

    /**
     * 
     */
    public TestBackchainTypeResourceIterator() {
        super();
    }

    /**
     * @param name
     */
    public TestBackchainTypeResourceIterator(String name) {
        super(name);
    }

    /**
     * Test when only the subject of the triple pattern is bound. In this case
     * the iterator MUST add a single entailment (s rdf:Type rdfs:Resource)
     * unless it is explicitly present in the database.
     */
    public void test_subjectBound() {
     
        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);

        try {

            final Vocabulary vocab = store.getVocabulary();

            final BigdataValueFactory f = store.getValueFactory();
            
            final BigdataURI A = f.createURI("http://www.foo.org/A");
            final BigdataURI B = f.createURI("http://www.foo.org/B");
            final BigdataURI C = f.createURI("http://www.foo.org/C");

            /*
             * add statements to the store.
             * 
             * Note: this gives us TWO (2) distinct subjects (A and B), but we 
             * will only visit statements for (A).
             */
            {
                
                final IStatementBuffer buffer = new StatementBuffer(store, 10/* capacity */);

                buffer.add(A, RDF.TYPE, B);
                buffer.add(B, RDF.TYPE, C);

                buffer.flush();
                
            }
            
            final IChunkedOrderedIterator<ISPO> itr;
            {

                final IAccessPath<ISPO> accessPath = store.getAccessPath(store
                        .getIV(A), NULL, NULL);
             
                itr = BackchainTypeResourceIterator.newInstance(//
                    accessPath.iterator(),//
                    accessPath,//
                    store, //
                    vocab.get(RDF.TYPE), //
                    vocab.get(RDFS.RESOURCE)//
                    );
                
            }
            
            assertSameSPOsAnyOrder(store,
                    
                    new SPO[]{
                    
                    new SPO(//
                            store.getIV(A),//
                            store.getIV(RDF.TYPE),//
                            store.getIV(B),//
                            StatementEnum.Explicit),
                            
                    new SPO(//
                            store.getIV(A), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Inferred)
                    },
                    
                    itr
                    
            );            
            
        } finally {
            
            store.__tearDownUnitTest();
            
        }
        
    }

    /**
     * Variant test where there is an explicit ( s rdf:type rdfs:Resource ) in
     * the database for the given subject. For this test we verify that the
     * iterator visits an "explicit" statement rather than adding its own
     * inference.
     */
    public void test_subjectBound2() {
     
        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);
        
        try {

            final Vocabulary vocab = store.getVocabulary();

            final BigdataValueFactory f = store.getValueFactory();
            
            final BigdataURI A = f.createURI("http://www.foo.org/A");
            final BigdataURI B = f.createURI("http://www.foo.org/B");
            final BigdataURI C = f.createURI("http://www.foo.org/C");

            /*
             * add statements to the store.
             * 
             * Note: this gives us TWO (2) distinct subjects (A and B), but we
             * will only visit statements for (A).
             */
            {
                
                IStatementBuffer buffer = new StatementBuffer(store, 10/* capacity */);

                buffer.add(A, RDF.TYPE, B);

                buffer.add(A, RDF.TYPE, RDFS.RESOURCE);

                buffer.add(B, RDF.TYPE, C);

                buffer.flush();
                
            }

            if(log.isInfoEnabled()) log.info("\n"+store.dumpStore());

            final IChunkedOrderedIterator<ISPO> itr;
            {

                final IAccessPath<ISPO> accessPath = store.getAccessPath(store
                        .getIV(A), NULL, NULL);

                itr = BackchainTypeResourceIterator.newInstance(//
                        accessPath.iterator(),//
                        accessPath,//
                        store, //
                        vocab.get(RDF.TYPE), //
                        vocab.get(RDFS.RESOURCE)//
                        );
                
            }

            assertSameSPOsAnyOrder(store, new SPO[]{
                    
                    new SPO(//
                            store.getIV(A),//
                            store.getIV(RDF.TYPE),//
                            store.getIV(B),//
                            StatementEnum.Explicit),
                            
                    new SPO(//
                            store.getIV(A), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Explicit)
                    },
                
                itr
                );
            
        } finally {
            
            store.__tearDownUnitTest();
            
        }
        
    }

    /**
     * Test when the triple pattern has no bound variables. In this case the
     * iterator MUST add an entailment for each distinct resource in the store
     * unless there is also an explicit (s rdf:type rdfs:Resource) assertion in
     * the database.
     */
    public void test_noneBound() {
        
        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);

        try {

            final Vocabulary vocab = store.getVocabulary();

            final BigdataValueFactory f = store.getValueFactory();
            
            final BigdataURI A = f.createURI("http://www.foo.org/A");
            final BigdataURI B = f.createURI("http://www.foo.org/B");
            final BigdataURI C = f.createURI("http://www.foo.org/C");

            /*
             * add statements to the store.
             * 
             * Note: this gives us TWO (2) distinct subjects (A and B). Since
             * nothing is bound in the query we will visit both explicit
             * statements and also the (s type resource) entailments for both
             * distinct subjects.
             */
            {
                
                final IStatementBuffer buffer = new StatementBuffer(store, 10/* capacity */);

                buffer.add(A, RDF.TYPE, B);

                buffer.add(A, RDF.TYPE, RDFS.RESOURCE);

                buffer.add(B, RDF.TYPE, C);

                buffer.flush();
                
            }

            final IChunkedOrderedIterator<ISPO> itr;
            {
                final IAccessPath<ISPO> accessPath = store.getAccessPath(NULL,
                        NULL, NULL);

                itr = BackchainTypeResourceIterator.newInstance(//
                        accessPath.iterator(),//
                        accessPath,//
                        store, //
                        vocab.get(RDF.TYPE), //
                        vocab.get(RDFS.RESOURCE)//
                );

            }
            
            assertSameSPOsAnyOrder(store, new SPO[]{

                    new SPO(//
                            store.getIV(A),//
                            store.getIV(RDF.TYPE),//
                            store.getIV(B),//
                            StatementEnum.Explicit),

                    new SPO(//
                            store.getIV(B),//
                            store.getIV(RDF.TYPE),//
                            store.getIV(C),//
                            StatementEnum.Explicit),
                    
                    new SPO(//
                            store.getIV(A), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Explicit),
                    
                    new SPO(//
                            store.getIV(B), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Inferred),
                    
                    new SPO(//
                            store.getIV(C), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Inferred),
                    
                    new SPO(//
                            store.getIV(RDFS.RESOURCE), //
                            store.getIV(RDF.TYPE), //
                            store.getIV(RDFS.RESOURCE), //
                            StatementEnum.Inferred)

            },
            
            itr);
            
        } finally {
            
            store.__tearDownUnitTest();
            
        }

    }
    
    /**
     * Test for other triple patterns (all bound, predicate bound, object bound,
     * etc). In all cases the iterator MUST NOT add any entailments.
     * 
     * @todo this is only testing a single access path.
     */
    public void test_otherBound_01() {
        
        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);
        
        try {

            final Vocabulary vocab = store.getVocabulary();

            final BigdataValueFactory f = store.getValueFactory();
            
            final BigdataURI A = f.createURI("http://www.foo.org/A");
            final BigdataURI B = f.createURI("http://www.foo.org/B");
            final BigdataURI C = f.createURI("http://www.foo.org/C");

            /*
             * add statements to the store.
             * 
             * Note: this gives us TWO (2) distinct subjects (A and B).
             */
            {
                
                IStatementBuffer buffer = new StatementBuffer(store, 10/* capacity */);

                buffer.add(A, RDF.TYPE, B);
                buffer.add(B, RDF.TYPE, C);

                buffer.flush();
                
            }

            final IChunkedOrderedIterator<ISPO> itr;
            {
                final IAccessPath<ISPO> accessPath = store.getAccessPath(NULL,
                        NULL, store.getIV(B));

                itr = BackchainTypeResourceIterator.newInstance(//
                        accessPath.iterator(),//
                        accessPath,//
                        store, //
                        vocab.get(RDF.TYPE), //
                        vocab.get(RDFS.RESOURCE)//
                        );
                
            }

            /*
             * Note: Since we are reading with the object bound, only the
             * explicit statements for that object should make it into the
             * iterator.
             */

            assertSameSPOsAnyOrder(store, new SPO[]{
                    
                    new SPO(//
                            store.getIV(A),//
                            store.getIV(RDF.TYPE),//
                            store.getIV(B),//
                            StatementEnum.Explicit)
                    
                },
                    itr);
            
        } finally {
            
            store.__tearDownUnitTest();
            
        }

    }
    
    /**
     * Backchain test when the subject is both bound and unbound and where the
     * predicate is bound to <code>rdf:type</code> and the object is bound to
     * <code>rdfs:Resource</code>.
     * 
     * FIXME test all access paths, including where the predicate is NULL and
     * where it is rdf:type and where the object is NULL and where it is
     * rdfs:Resource.
     */
    public void test_backchain_foo_type_resource() {

        final Properties properties = super.getProperties();
        
        // override the default axiom model.
        properties.setProperty(org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        
        final AbstractTripleStore store = getStore(properties);

        try {

            final Vocabulary vocab = store.getVocabulary();
            
            final BigdataValueFactory f = store.getValueFactory();
            
            final BigdataURI S = f.createURI("http://www.embergraph.org/s");
            final BigdataURI P = f.createURI("http://www.embergraph.org/p");
            final BigdataURI O = f.createURI("http://www.embergraph.org/o");
            
            final IV s = store.addTerm(S);
            final IV p = store.addTerm(P);
            final IV o = store.addTerm(O);

            final IV rdfType = vocab.get(RDF.TYPE);

            final IV rdfsResource = vocab.get(RDFS.RESOURCE);
            
            store.addStatements(new SPO[] {//
                    new SPO(s, p, o, StatementEnum.Explicit) //
                    }, 1);

            if(log.isInfoEnabled()) {
                log.info("\n:"+store.dumpStore());
            }
            
            AbstractTestCase.assertSameSPOs(new SPO[] {
                    new SPO(s, p, o, StatementEnum.Explicit),
                    },
                    store.getAccessPath(NULL, NULL, NULL).iterator()
                    );

            {
                // where s is bound.
                final IAccessPath<ISPO> accessPath = store.getAccessPath(s,
                        rdfType, //
                        rdfsResource//
                        );

                final IChunkedOrderedIterator<ISPO> itr = BackchainTypeResourceIterator
                        .newInstance(//
                                accessPath.iterator(),//
                                accessPath,//
                                store, //
                                rdfType, //
                                rdfsResource//
                        );

                AbstractTestCase.assertSameSPOs(new SPO[] { new SPO(s,
                        rdfType, //
                        rdfsResource,//
                        StatementEnum.Inferred), }, itr);
            }

            {
                // where s is unbound.
                final IAccessPath<ISPO> accessPath = store.getAccessPath(NULL,
                        rdfType, //
                        rdfsResource//
                );
                
                final IChunkedOrderedIterator<ISPO> itr = BackchainTypeResourceIterator.newInstance(//
                        accessPath.iterator(),//
                        accessPath,//
                        store, //
                        rdfType, //
                        rdfsResource//
                );

                AbstractTestCase.assertSameSPOs(new SPO[] { //
                        new SPO(s, rdfType, rdfsResource, StatementEnum.Inferred),
                        new SPO(o, rdfType, rdfsResource, StatementEnum.Inferred),
                        }, itr);
            }
            
        } finally {

            store.__tearDownUnitTest();
            
        }
            
    }

}
