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
 * Created on Jun 17, 2008
 */

package com.bigdata.rdf.rules;

import java.util.HashMap;
import java.util.Map;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.Var;
import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.store.AbstractTripleStoreTestCase;

import cutthecrap.utils.striterators.ICloseableIterator;

/**
 * Unit tests for {@link AbstractTripleStore#match(Literal[], URI[], URI)}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestMatch extends AbstractTripleStoreTestCase {

    /**
     * 
     */
    public TestMatch() {
    }

    /**
     * @param name
     */
    public TestMatch(String name) {
        super(name);
    }

    /**
     * Unit test for {@link AbstractTripleStore#match(Literal[], URI[], URI)}
     */
    public void test_match() {

//        LocalTripleStore store = new LocalTripleStore(getProperties());

        final URI bryan = new URIImpl("http://www.bigdata.com/bryan");
        
        final URI mike = new URIImpl("http://www.bigdata.com/mike");

        final URI paul = new URIImpl("http://www.bigdata.com/paul");

        final URI person = new URIImpl("http://www.bigdata.com/person");

        final URI chiefScientist = new URIImpl("http://www.bigdata.com/chiefScientist");

        final URI chiefEngineer = new URIImpl("http://www.bigdata.com/chiefEngineer");

        final AbstractTripleStore store = getStore();

        try {

            /*
             * Populate the KB with some statements. See the method under test
             * for the shape of the data required to materialize a result.
             */
            {

                StatementBuffer sb = new StatementBuffer(store,100);

                /*
                    new Triple(var("s"), var("p"), lit),
                    
                    new Triple(var("s"), inf.rdfType, var("t"),ExplicitSPOFilter.INSTANCE),
                    
                    new Triple(var("t"), inf.rdfsSubClassOf, cls)

                 */
                

                sb.add(bryan, RDFS.LABEL, new LiteralImpl("bryan"));

                sb.add(bryan, RDFS.LABEL, new LiteralImpl("bryan thompson"));

                sb.add(bryan, RDF.TYPE, chiefScientist);

                sb.add(mike, RDFS.LABEL, new LiteralImpl("mike"));

                sb.add(mike, RDFS.LABEL, new LiteralImpl("mike personick"));

                sb.add(mike, RDF.TYPE, chiefEngineer);

                // Note: will not be matched since no explicit type that is subClassOf person.
                sb.add(paul, RDFS.LABEL, new LiteralImpl("paul"));

                sb.add(chiefScientist, RDFS.SUBCLASSOF, person);

                sb.add(chiefEngineer, RDFS.SUBCLASSOF, person);
                
                sb.flush();
                
            }

//            /*
//             * Note: Since the match rule will use a read-committed view, a
//             * commit is required in order for that view to reflect the writes
//             * that we just performed on the database.
//             */
//            store.commit();
            
            /*
             * Do run the match rule for "bryan". There should be exactly one
             * matched subject (bryan) with two bindings sets ("bryan") and
             * ("bryan thompson") since there are two completions for "bryan"
             * that satisify the rest of the requirements.
             */
            {
                
                final Map<Literal,Map<String,Value>> expected = new HashMap<Literal,Map<String,Value>>();
                
                {
                    final Map<String,Value> bindingSet = new HashMap<String, Value>();

                    bindingSet.put("s", bryan);

                    bindingSet.put("t", chiefScientist);

                    bindingSet.put("p", RDFS.LABEL);

                    bindingSet.put("lit", new LiteralImpl("bryan"));
                    
                    expected.put(new LiteralImpl("bryan"), bindingSet);
                    
                }
 
                {
                    final Map<String, Value> bindingSet = new HashMap<String, Value>();

                    bindingSet.put("s", bryan);

                    bindingSet.put("t", chiefScientist);

                    bindingSet.put("p", RDFS.LABEL);

                    bindingSet.put("lit", new LiteralImpl("bryan thompson"));
                    
                    expected.put(new LiteralImpl("bryan thompson"), bindingSet);
                    
                }
                
                final ICloseableIterator<IBindingSet> itr = store.match(//
                        new Literal[] { new LiteralImpl("bryan") },//
                        new URI[] { RDFS.LABEL },//
                        person//
                        );

                try {

                    while (itr.hasNext()) {

                        final IBindingSet actualBindingSet= itr.next();

                        if (log.isInfoEnabled())
                            log.info(actualBindingSet.toString());

                        if (expected.isEmpty()) {

                            fail("Nothing else is expected: found="
                                    + actualBindingSet);

                        }

                        final Literal lit = (Literal) actualBindingSet.get(Var
                                .var("lit")).get();

                        assertNotNull("lit not bound: "+actualBindingSet, lit);
                        
                        final Map<String, Value> expectedBindingSet = expected
                                .remove(lit);

                        if (expectedBindingSet == null) {

                            fail("Not expecting: " + actualBindingSet);

                        }

                        assertEquals("s", expectedBindingSet.get("s"),
                                (Value)actualBindingSet.get(Var.var("s")).get());

                        assertEquals("t", expectedBindingSet.get("t"),
                                (Value)actualBindingSet.get(Var.var("t")).get());

                        assertEquals("p", expectedBindingSet.get("p"),
                                (Value)actualBindingSet.get(Var.var("p")).get());

                    }

                    if (!expected.isEmpty()) {

                        fail("Additional terms were expected: not found="
                                + expected);

                    }

                } finally {

                    itr.close();

                }

            }
                        
            /*
             * Do a completion scan on "paul". There should be NO matches since
             * there is no explicit type for that subject.
             */
            {
                
                final ICloseableIterator<IBindingSet> itr = store.match(//
                        new Literal[] { new LiteralImpl("paul") },//
                        new URI[] { RDFS.LABEL },//
                        person//
                        );

                try {
                
                    assertFalse(itr.hasNext());
                    
                } finally {
                    
                    itr.close();
                    
                }
                
            }
            
        } finally {
            
            store.__tearDownUnitTest();
            
        }
        
    }
    
}
