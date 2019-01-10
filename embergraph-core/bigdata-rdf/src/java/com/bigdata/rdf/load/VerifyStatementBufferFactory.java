package com.bigdata.rdf.load;

import java.util.concurrent.atomic.AtomicLong;

import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class VerifyStatementBufferFactory implements IStatementBufferFactory {

    private final AbstractTripleStore db;

    private final int bufferCapacity;

    public final AtomicLong nterms = new AtomicLong(),
            ntermsNotFound = new AtomicLong(), ntriples = new AtomicLong(),
            ntriplesNotFound = new AtomicLong();

    public VerifyStatementBufferFactory(AbstractTripleStore db,
            int bufferCapacity) {

        this.db = db;
       
        this.bufferCapacity = bufferCapacity;
        
    }
    
    /**
     * Return the {@link StatementBuffer} to be used for a task.
     */
    public StatementBuffer newStatementBuffer() {

        /*
         * DO NOT use a thread-local here. That is likely to lead to a memory
         * leak.
         */

//        return threadLocal.get();

        return new VerifyStatementBuffer(db, bufferCapacity, nterms,
                ntermsNotFound, ntriples, ntriplesNotFound);
        
    }
    
//    private ThreadLocal<StatementBuffer> threadLocal = new ThreadLocal<StatementBuffer>() {
//
//        protected synchronized StatementBuffer initialValue() {
//
//            return new VerifyStatementBuffer(db, bufferCapacity, nterms,
//                    ntermsNotFound, ntriples, ntriplesNotFound);
//            
//        }
//
//    };
    
}