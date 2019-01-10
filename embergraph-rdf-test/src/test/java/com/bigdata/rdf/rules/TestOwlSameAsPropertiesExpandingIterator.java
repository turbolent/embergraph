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
 * Created on March 11, 2008
 */

package com.bigdata.rdf.rules;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import com.bigdata.rdf.inf.OwlSameAsPropertiesExpandingIterator;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.model.StatementEnum;
import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.spo.SPO;
import com.bigdata.rdf.spo.SPOAccessPath;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.striterator.IChunkedOrderedIterator;

/**
 * Test suite for {@link OwlSameAsPropertiesExpandingIterator}.
 * 
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestOwlSameAsPropertiesExpandingIterator extends AbstractInferenceEngineTestCase {

    /**
     * 
     */
    public TestOwlSameAsPropertiesExpandingIterator() {
        super();
    }

    /**
     * @param name
     */
    public TestOwlSameAsPropertiesExpandingIterator(String name) {
        super(name);
    }

    /**
     * Test the various access paths for backchaining the property collection
     * normally done through owl:sameAs {2,3}.
     */
    public void test_backchain() 
    {
     
        // store with no owl:sameAs closure
        final AbstractTripleStore db = getStore();
        
        try {

//            InferenceEngine inf = noClosure.getInferenceEngine();
            
//            Rule[] rules = inf.getRuleModel();
//            for( Rule rule : rules ) {
//                System.err.println(rule.getName());
//            }
            
            final URI A = new URIImpl("http://www.bigdata.com/A");
            final URI B = new URIImpl("http://www.bigdata.com/B");
//            final URI C = new URIImpl("http://www.bigdata.com/C");
//            final URI D = new URIImpl("http://www.bigdata.com/D");
//            final URI E = new URIImpl("http://www.bigdata.com/E");

//            final URI V = new URIImpl("http://www.bigdata.com/V");
            final URI W = new URIImpl("http://www.bigdata.com/W");
            final URI X = new URIImpl("http://www.bigdata.com/X");
            final URI Y = new URIImpl("http://www.bigdata.com/Y");
            final URI Z = new URIImpl("http://www.bigdata.com/Z");

            {
//                TMStatementBuffer buffer = new TMStatementBuffer
//                ( inf, 100/* capacity */, BufferEnum.AssertionBuffer
//                  );
                StatementBuffer buffer = new StatementBuffer
                    ( db, 100/* capacity */
                      );

                buffer.add(X, A, Z);
                buffer.add(Y, B, W);
                buffer.add(X, OWL.SAMEAS, Y);
                buffer.add(Z, OWL.SAMEAS, W);
                
                // write statements on the database.
                buffer.flush();
                
                // database at once closure.
                db.getInferenceEngine().computeClosure(null/*focusStore*/);

                // write on the store.
//                buffer.flush();
            }
            
            final IV a = db.getIV(A);
            final IV b = db.getIV(B);
//            final IV c = noClosure.getTermId(C);
//            final IV d = noClosure.getTermId(D);
//            final IV e = noClosure.getTermId(E);
//            final IV v = noClosure.getTermId(V);
            final IV w = db.getIV(W);
            final IV x = db.getIV(X);
            final IV y = db.getIV(Y);
            final IV z = db.getIV(Z);
            final IV same = db.getIV(OWL.SAMEAS);
            final IV type = db.getIV(RDF.TYPE);
            final IV property = db.getIV(RDF.PROPERTY);
            final IV subpropof = db.getIV(RDFS.SUBPROPERTYOF);
            
            if (log.isInfoEnabled())
                log.info("\n" +db.dumpStore(true, true, false));
  
            { // test S
            
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(y,NULL,NULL);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );
/*
                while(itr.hasNext()) {
                    ISPO spo = itr.next();
                    System.err.println(spo.toString(db));
                }
*/                
                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(y,b,w,
                                StatementEnum.Explicit),
                        new SPO(y,b,z,
                                StatementEnum.Inferred),
                        new SPO(y,a,w,
                                StatementEnum.Inferred),
                        new SPO(y,a,z,
                                StatementEnum.Inferred),
                        new SPO(y,same,x,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test SP
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(y,b,NULL);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );
/*
                while(itr.hasNext()) {
                    ISPO spo = itr.next();
                    System.err.println(spo.toString(db));
                }
*/                
                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(y,b,w,
                                StatementEnum.Explicit),
                        new SPO(y,b,z,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test O
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(NULL,NULL,w);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );
/*
                while(itr.hasNext()) {
                    ISPO spo = itr.next();
                    System.err.println(spo.toString(db));
                }
*/                
                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(y,b,w,
                                StatementEnum.Explicit),
                        new SPO(y,a,w,
                                StatementEnum.Inferred),
                        new SPO(x,b,w,
                                StatementEnum.Inferred),
                        new SPO(x,a,w,
                                StatementEnum.Inferred),
                        new SPO(z,same,w,
                                StatementEnum.Explicit)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test PO
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(NULL,a,w);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );

                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(y,a,w,
                                StatementEnum.Inferred),
                        new SPO(x,a,w,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test SO
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(x,NULL,z);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );

                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(x,a,z,
                                StatementEnum.Explicit),
                        new SPO(x,b,z,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test SPO
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(x,b,z);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );

                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(x,b,z,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test P
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(NULL,a,NULL);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );
/*
                while(itr.hasNext()) {
                    ISPO spo = itr.next();
                    System.err.println(spo.toString(db));
                }
*/                
                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(x,a,z,
                                StatementEnum.Explicit),
                        new SPO(x,a,w,
                                StatementEnum.Inferred),
                        new SPO(y,a,z,
                                StatementEnum.Inferred),
                        new SPO(y,a,w,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
            { // test ???
                
                SPOAccessPath accessPath = (SPOAccessPath)db.getAccessPath(NULL,NULL,NULL);
                
                IChunkedOrderedIterator<ISPO> itr = new OwlSameAsPropertiesExpandingIterator(//
                        getValue(accessPath,0/*S*/),
                        getValue(accessPath,1/*P*/),
                        getValue(accessPath,2/*O*/),
                        db, //
                        same,//
                        accessPath.getKeyOrder()
                        );
/*
                while(itr.hasNext()) {
                    ISPO spo = itr.next();
                    System.err.println(spo.toString(db));
                }
*/                
                assertSameSPOsAnyOrder(db,
                    
                    new SPO[]{
                        new SPO(x,a,z,
                                StatementEnum.Explicit),
                        new SPO(y,b,w,
                                StatementEnum.Explicit),
                        new SPO(x,same,y,
                                StatementEnum.Explicit),
                        new SPO(z,same,w,
                                StatementEnum.Explicit),
                        new SPO(x,a,w,
                                StatementEnum.Inferred),
                        new SPO(x,b,z,
                                StatementEnum.Inferred),
                        new SPO(x,b,w,
                                StatementEnum.Inferred),
                        new SPO(y,a,z,
                                StatementEnum.Inferred),
                        new SPO(y,a,w,
                                StatementEnum.Inferred),
                        new SPO(y,b,z,
                                StatementEnum.Inferred),
                        new SPO(y,same,x,
                                StatementEnum.Inferred),
                        new SPO(w,same,z,
                                StatementEnum.Inferred),
                        new SPO(a,type,property,
                                StatementEnum.Inferred),
                        new SPO(b,type,property,
                                StatementEnum.Inferred),
                        new SPO(a,subpropof,a,
                                StatementEnum.Inferred),
                        new SPO(b,subpropof,b,
                                StatementEnum.Inferred)
                    },
                    
                    itr,
                    true // ignore axioms
                    
                );
                
            }
          
        } finally {
            
            db.__tearDownUnitTest();
            
        }
        
    }

}
