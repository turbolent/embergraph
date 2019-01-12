package org.embergraph.rdf.store;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphBNodeImpl;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.openrdf.model.Value;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.model.EmbergraphResource;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.striterator.AbstractChunkedResolverator;
import org.embergraph.striterator.IChunkedOrderedIterator;

/**
 * Efficiently resolve term identifiers in Embergraph {@link ISPO}s to RDF
 * {@link EmbergraphValue}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class EmbergraphStatementIteratorImpl
        extends
        AbstractChunkedResolverator<ISPO, EmbergraphStatement, AbstractTripleStore>
        implements EmbergraphStatementIterator {

    final private static Logger log = Logger
            .getLogger(EmbergraphStatementIteratorImpl.class);

    /**
     * An optional map of known blank node term identifiers and the
     * corresponding {@link EmbergraphBNodeImpl} objects. This map may be used to
     * resolve term identifiers to the corresponding blank node objects across a
     * "connection" context.
     */
    private final Map<IV, EmbergraphBNode> bnodes;

    /**
     * 
     * @param db
     *            Used to resolve term identifiers to {@link Value} objects.
     * @param src
     *            The source iterator (will be closed when this iterator is
     *            closed).
     */
    public EmbergraphStatementIteratorImpl(final AbstractTripleStore db,
            final IChunkedOrderedIterator<ISPO> src) {

        this(db, null/* bnodes */, src);
        
    }

    /**
     * 
     * @param db
     *            Used to resolve term identifiers to {@link Value} objects.
     * @param bnodes
     *            An optional map of known blank node term identifiers and the
     *            corresponding {@link EmbergraphBNodeImpl} objects. This map may
     *            be used to resolve blank node term identifiers to blank node
     *            objects across a "connection" context.
     * @param src
     *            The source iterator (will be closed when this iterator is
     *            closed).
     */
    public EmbergraphStatementIteratorImpl(final AbstractTripleStore db,
            final Map<IV, EmbergraphBNode> bnodes,
                final IChunkedOrderedIterator<ISPO> src) {
        
        super(db, src, new BlockingBuffer<EmbergraphStatement[]>(
                db.getChunkOfChunksCapacity(), 
                db.getChunkCapacity(),
                db.getChunkTimeout(),
                TimeUnit.MILLISECONDS));

        this.bnodes = bnodes;
        
    }

    /**
     * Strengthens the return type.
     */
    @Override
    public EmbergraphStatementIteratorImpl start(final ExecutorService service) {

        return (EmbergraphStatementIteratorImpl) super.start(service);
        
    }

    /**
     * Resolve a chunk of {@link ISPO}s into a chunk of {@link EmbergraphStatement}s.
     */
    @Override
    protected EmbergraphStatement[] resolveChunk(final ISPO[] chunk) {

        if (log.isDebugEnabled())
            log.debug("chunkSize=" + chunk.length);

        /*
         * Create a collection of the distinct term identifiers used in this
         * chunk.
         */
        
        final Collection<IV<?, ?>> ivs = new LinkedHashSet<IV<?, ?>>(
                chunk.length * state.getSPOKeyArity());

        for (ISPO spo : chunk) {

            {
                
                final IV<?,?> s = spo.s();

//                if (bnodes == null || !bnodes.containsKey(s))
//                    ivs.add(s);
                
                handleIV(s, ivs);
            
            }

//            ivs.add(spo.p());

            handleIV(spo.p(), ivs);
            
            {

                final IV<?,?> o = spo.o();

//                if (bnodes == null || !bnodes.containsKey(o))
//                    ivs.add(o);

                handleIV(o, ivs);
                
            }

            {
             
                final IV<?,?> c = spo.c();

//                if (c != null
//                        && (bnodes == null || !bnodes.containsKey(c)))
//                    ivs.add(c);

                if (c != null) 
                	handleIV(c, ivs);
                
            }

        }

        if (log.isDebugEnabled())
            log.debug("Resolving " + ivs.size() + " term identifiers");
        
        /*
         * Batch resolve term identifiers to EmbergraphValues, obtaining the
         * map that will be used to resolve term identifiers to terms for
         * this chunk.
         */
        final Map<IV<?, ?>, EmbergraphValue> terms = state.getLexiconRelation()
                .getTerms(ivs);

        final EmbergraphValueFactory valueFactory = state.getValueFactory();
        
        /*
         * The chunk of resolved statements.
         */
        final EmbergraphStatement[] stmts = new EmbergraphStatement[chunk.length];
        
        int i = 0;
        for (ISPO spo : chunk) {

            /*
             * Resolve term identifiers to terms using the map populated when we
             * fetched the current chunk.
             */
            final EmbergraphResource s = (EmbergraphResource) resolve(terms, spo.s());
            final EmbergraphURI p = (EmbergraphURI) resolve(terms, spo.p());
//            try {
//                p = (EmbergraphURI) resolve(terms, spo.p());
//            } catch (ClassCastException ex) {
//                log.error("spo="+spo+", p="+resolve(terms, spo.p()));
//                throw ex;
//            }
            final EmbergraphValue o = resolve(terms, spo.o());
            final IV<?,?> _c = spo.c();
            final EmbergraphResource c;
            if (_c != null) {
                /*
                 * FIXME This kludge to strip off the null graph should be
                 * isolated to the EmbergraphSail's package. Our own code should be
                 * protected from this behavior. Also see the
                 * EmbergraphSolutionResolverator.
                 */
                final EmbergraphResource tmp = (EmbergraphResource) resolve(terms, _c);
                if (tmp instanceof EmbergraphURI
                        && ((EmbergraphURI) tmp).equals(BD.NULL_GRAPH)) {
                    /*
                     * Strip off the "nullGraph" context.
                     */
                    c = null;
                } else {
                    c = tmp;
                }
            } else {
                c = null;
            }

            if (spo.hasStatementType() == false) {

                log.error("statement with no type: "
                        + valueFactory.createStatement(s, p, o, c, null, spo.getUserFlag()));

            }

            // the statement.
            final EmbergraphStatement stmt = valueFactory.createStatement(s, p, o,
                    c, spo.getStatementType(), spo.getUserFlag());

            // save the reference.
            stmts[i++] = stmt;

        }

        return stmts;

    }

    /**
     * Add the IV to the list of terms to materialize, and also
     * delegate to {@link #handleSid(SidIV, Collection, boolean)} if it's a
     * SidIV.
     */
    private void handleIV(final IV<?, ?> iv, 
    		final Collection<IV<?, ?>> ids) {
    	
//    	if (iv instanceof SidIV) {
//    		
//    		handleSid((SidIV<?>) iv, ids);
//    		
//    	}
    		
    	if (bnodes == null || !bnodes.containsKey(iv)) {
    	
    		ids.add(iv);
    		
    	}
    	
    }
    
//    /**
//     * Sids need to be handled specially because their individual ISPO
//     * components might need materialization as well.
//     */
//    private void handleSid(final SidIV<?> sid,
//    		final Collection<IV<?, ?>> ids) {
//    	
//    	final ISPO spo = sid.getInlineValue();
//    	
//    	handleIV(spo.s(), ids);
//    	
//    	handleIV(spo.p(), ids);
//    	
//    	handleIV(spo.o(), ids);
//    	
//    	if (spo.c() != null) {
//    		
//        	handleIV(spo.c(), ids);
//    		
//    	}
//
//    }


    
    /**
     * Resolve a term identifier to the {@link EmbergraphValue}, checking the
     * {@link #bnodes} map if it is defined.
     * 
     * @param terms
     *            The terms mapping obtained from the lexicon.
     * @param iv
     *            The term identifier.
     *            
     * @return The {@link EmbergraphValue}.
     */
    private EmbergraphValue resolve(final Map<IV<?,?>, EmbergraphValue> terms,
            final IV<?,?> iv) {

        EmbergraphValue v = null;

        if (bnodes != null) {

            v = bnodes.get(iv);

        }

        if (v == null) {

            v = terms.get(iv);

        }

        return v;

    }

}
