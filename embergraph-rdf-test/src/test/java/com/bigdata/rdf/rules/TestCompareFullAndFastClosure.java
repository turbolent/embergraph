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
 * Created on Jul 15, 2008
 */

package com.bigdata.rdf.rules;

import java.io.IOException;
import java.util.Properties;

import org.openrdf.rio.RDFFormat;

import com.bigdata.rdf.rio.LoadStats;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.store.DataLoader;
import com.bigdata.rdf.store.TripleStoreUtility;
import com.bigdata.rdf.store.AbstractTripleStore.Options;

/**
 * Test suite comparing full fix point closure of RDFS entailments against the
 * fast closure program for some known data sets (does not test truth
 * maintenance under assertion and retraction or the justifications).
 * 
 * @todo also compare pipeline vs nested subquery closure (but only for LTS and
 *       LDS as nested subquery is too slow for EDS and JDS - or compare to LTS
 *       nested subquery for the EDS and JDS cases).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestCompareFullAndFastClosure extends AbstractRuleTestCase {

    /**
     * 
     */
    public TestCompareFullAndFastClosure() {
    }

    /**
     * @param name
     */
    public TestCompareFullAndFastClosure(String name) {
        super(name);
    }

    public void test_compareEntailments() throws Exception {
        
        // String[] resource = new String[]{"/com/bigdata/rdf/rules/testOwlSameAs.rdf"};
        // String[] resource = new String[]{"/com/bigdata/rdf/rules/testOwlSameAs.rdf"};
        String[] resource = new String[]{"com/bigdata/rdf/rules/small.rdf"};
        String[] baseURL = new String[]{""};
        RDFFormat[] format = new RDFFormat[]{RDFFormat.RDFXML};

        doCompareEntailments(resource, baseURL, format);
        
    }
    
    /**
     * 
     * @param resource
     * @param baseURL
     * @param format
     * @throws IOException
     */
    protected void doCompareEntailments(final String resource[],
            final String baseURL[], final RDFFormat[] format) throws Exception {

        final Properties properties = new Properties(getProperties());
        
        // close each set of resources after it has been loaded.
        properties.setProperty(DataLoader.Options.CLOSURE,
                DataLoader.ClosureEnum.Batch.toString());

        AbstractTripleStore store1 = null;
        AbstractTripleStore store2 = null;
        
        try {

        { // use the "full" forward closure.

            final Properties tmp = new Properties(properties);

            tmp.setProperty(Options.CLOSURE_CLASS, FullClosure.class.getName());
            /*
             * tmp.setProperty(DataLoader.Options.CLOSURE, ClosureEnum.None.toString());
             */
            store1 = getStore(tmp);

        }

        { // use the "fast" forward closure.

            final Properties tmp = new Properties(properties);

            tmp.setProperty(Options.CLOSURE_CLASS, FastClosure.class.getName());
/*
            tmp.setProperty(DataLoader.Options.CLOSURE,
                    ClosureEnum.None.toString());
*/
            store2 = getStore(tmp);

        }
        
            {

                final LoadStats loadStats = store1.getDataLoader().loadData(resource,
                        baseURL, format);
                
                // store1.getInferenceEngine().computeClosure(null);
                
                if (log.isInfoEnabled())
                    log.info("Full forward closure: " + loadStats);

                if (log.isInfoEnabled())
                    log.info(store1.dumpStore(store1, true, true, false, true));
                
            }
            
            {

                final LoadStats loadStats = store2.getDataLoader().loadData(
                        resource, baseURL, format);

                // store2.getInferenceEngine().computeClosure(null);
                
                if (log.isInfoEnabled())
                    log.info("Fast forward closure: " + loadStats);
                
                if (log.isInfoEnabled())
                    log.info(store2.dumpStore(store2, true, true, false, true));
                
            }
        
            /*
             * Note: Both graphs have the same configuration and therefore
             * should have the same statements in the data without the
             * backchainer.
             */
            assertTrue(TripleStoreUtility.modelsEqual(store1, store2));

        } finally {

            if (store1 != null)
                store1.__tearDownUnitTest();
            if (store2 != null)
                store2.__tearDownUnitTest();

//            // both stores are using the same index manager.
//            final IIndexManager indexManager = store1.getIndexManager();
//
//            if(store1.isOpen())
//                store1.destroy();
//            
//            if(store2.isOpen())
//                store2.destroy();
//            
//            indexManager.destroy();

        }
        
    }
    
}
