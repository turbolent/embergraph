package com.bigdata.rdf.inf;

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.bigdata.rdf.store.TempTripleStore;
import com.bigdata.striterator.IChunkedOrderedIterator;

public abstract class BackchainOwlSameAsIterator implements IChunkedOrderedIterator<ISPO> {
    
    protected final static Logger log = Logger.getLogger(BackchainOwlSameAsIterator.class);
    
    /**
     * The database.
     */
    protected AbstractTripleStore db;

    /**
     * This flag is <code>true</code> since we do NOT want statement
     * identifiers to be generated for inferences produced by the backchainer.
     */
    protected final boolean copyOnly = true;
    
    protected IV sameAs;

    final int chunkSize = 100;//10000;

    protected IChunkedOrderedIterator<ISPO> src;
    
    public BackchainOwlSameAsIterator(IChunkedOrderedIterator<ISPO> src,
            AbstractTripleStore db, IV sameAs) {

        if (src == null)
            throw new IllegalArgumentException();
        
        if (db == null)
            throw new IllegalArgumentException();
        
        if (sameAs == null)
            throw new IllegalArgumentException();
        
        this.src = src;
        
        this.db = db;
        
        this.sameAs = sameAs;
    
    }
    
    protected Set<IV> getSelfAndSames(IV iv) {
        Set<IV> selfAndSames = new TreeSet<IV>();
        selfAndSames.add(iv);
        getSames(iv, selfAndSames);
        return selfAndSames;
    }

    protected Set<IV> getSames(IV iv) {
        Set<IV> sames = new TreeSet<IV>();
        sames.add(iv);
        getSames(iv, sames);
        sames.remove(iv);
        return sames;
    }

    protected void getSames(IV id, Set<IV> sames) {
        IChunkedOrderedIterator<ISPO> it = db.getAccessPath(id, sameAs, null).iterator();
        try {
            while (it.hasNext()) {
                IV same = it.next().o();
                if (!sames.contains(same)) {
                    sames.add(same);
                    getSames(same, sames);
                }
            }
        } finally {
            it.close();
        }
        it = db.getAccessPath(null, sameAs, id).iterator();
        try {
            while (it.hasNext()) {
                IV same = it.next().s();
                if (!sames.contains(same)) {
                    sames.add(same);
                    getSames(same, sames);
                }
            }
        } finally {
            it.close();
        }
    }
    
    protected TempTripleStore createTempTripleStore() {
        // log.info("creating temp triple store for owl:sameAs backchainer");
        // System.err.println("creating temp triple store for owl:sameAs backchainer");
        final Properties props = db.getProperties();
        // do not store terms
        props.setProperty(AbstractTripleStore.Options.LEXICON, "false");
        // only store the SPO index
        props.setProperty(AbstractTripleStore.Options.ONE_ACCESS_PATH, "true");
        // @todo MikeP : test w/ SPO bloom filter enabled and see if this improves performance.
        props.setProperty(AbstractTripleStore.Options.BLOOM_FILTER, "false");
        return new TempTripleStore(db.getIndexManager().getTempStore(), props, db);
    }

    protected void dumpSPO(ISPO spo) {
//        System.err.println(spo.toString(db));
    }
    
}
