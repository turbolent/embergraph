package org.embergraph.rdf.graph.impl.bd;

import java.util.Properties;

import org.openrdf.model.ValueFactory;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

import org.embergraph.rdf.graph.IGraphAccessor;
import org.embergraph.rdf.graph.impl.bd.BigdataGASEngine.BigdataGraphAccessor;
import org.embergraph.rdf.graph.util.AbstractGraphFixture;
import org.embergraph.rdf.graph.util.SailGraphLoader;
import org.embergraph.rdf.sail.BigdataSail;
import org.embergraph.rdf.sail.BigdataSail.BigdataSailConnection;
import org.embergraph.rdf.store.AbstractTripleStore;

public class BigdataGraphFixture extends AbstractGraphFixture {

    private final BigdataSail sail;
    
    public BigdataGraphFixture(final Properties properties)
            throws SailException {

        sail = new BigdataSail(properties);

        sail.initialize();

    }

    public BigdataGraphFixture(final AbstractTripleStore kb)
            throws SailException {

        sail = new BigdataSail(kb);

        sail.initialize();

    }

    @Override
    public BigdataSail getSail() {

        return sail;
        
    }

    @Override
    public void destroy() throws Exception {

        if (sail.isOpen()) {

            sail.shutDown();

        }

        if (sail instanceof BigdataSail) {

            ((BigdataSail) sail).__tearDownUnitTest();

        }

    }

    @Override
    protected SailGraphLoader newSailGraphLoader(SailConnection cxn) {

        return new BigdataSailGraphLoader(cxn);

    }

    @Override
    public BigdataGASEngine newGASEngine(final int nthreads) {

        return new BigdataGASEngine(sail, nthreads);
        
    }

    @Override
    public IGraphAccessor newGraphAccessor(final SailConnection ignored) {

        return new BigdataGraphAccessor(sail.getIndexManager());

    }
    
    public static class BigdataSailGraphLoader extends SailGraphLoader {

        private final ValueFactory valueFactory;
        
        public BigdataSailGraphLoader(SailConnection cxn) {

            super(cxn);
            
            // Note: Needed for RDR.
            this.valueFactory = ((BigdataSailConnection) cxn).getBigdataSail()
                    .getValueFactory();
            
        }

        @Override
        protected ValueFactory getValueFactory() {
 
            return valueFactory;
            
        }

    }

}