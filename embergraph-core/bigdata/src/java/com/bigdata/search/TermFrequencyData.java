package com.bigdata.search;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.Token;

/**
 * Models the term-frequency data associated with a single field of some
 * document.
 * 
 * @param <V>
 *            The generic type of the document identifier.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TermFrequencyData<V extends Comparable<V>> {

    /** The document identifier. */
    final public V docId;
    
    /** The field identifier. */
    final public int fieldId;
    
    /** The #of terms added (includes duplicates). */
    private int totalTermCount = 0;
    
    /**
     * The set of distinct tokens and their {@link ITermMetadata}.
     */
    final public LinkedHashMap<String, ITermMetadata> terms = new LinkedHashMap<String, ITermMetadata>();
    
    public TermFrequencyData(final V docId, final int fieldId,
            final String token) {

        // Note: will be null when indexing a query.
//        if (docId == null)
//            throw new IllegalArgumentException();
        
        this.docId = docId;
        
        this.fieldId = fieldId;
        
        add( token );
        
    }

    /**
     * Add a {@link Token}.
     * 
     * @param token
     *            The token.
     * 
     * @return true iff the termText did not previously exist for this
     *         {@link TermFrequencyData}.
     */
    public boolean add(final String token) {
        
        final boolean newTerm;
        
        ITermMetadata termMetadata = terms.get(token);

        if (termMetadata == null) {
            
            termMetadata = new TermMetadata();
            
            terms.put(token, termMetadata);
            
            newTerm = true;
            
        } else {
            
            newTerm = false;
            
        }
        
        termMetadata.add();

        totalTermCount++;
        
        return newTerm;
        
    }
 
    /**
     * The #of distinct terms.
     */
    public int distinctTermCount() {
     
        return terms.size();
        
    }

    /**
     * The total #of terms, including duplicates.
     */
    public int totalTermCount() {

        return totalTermCount;
        
    }

    /**
     * Computes the normalized term-frequency vector. This is a unit vector
     * whose magnitude is <code>1.0</code>. The magnitude of the term frequency
     * vector is computed using the integer term frequency values reported by
     * {@link TermMetadata#termFreq()}. The normalized values are then set on
     * {@link TermMetadata#localTermWeight}.
     * 
     * @return The magnitude of the un-normalized
     *         {@link TermMetadata#termFreq()} vector.
     */
    public double normalize() {
     
        /*
         * Compute magnitude.
         */
        double magnitude = 0d;
        
        for(ITermMetadata md : terms.values()) { 
            
            final double termFreq = md.termFreq();

            // sum of squares.
            magnitude += (termFreq * termFreq);
            
        }

        magnitude = Math.sqrt(magnitude);
     
        /*
         * normalizedWeight = termFreq / magnitude for each term.
         */

        for(ITermMetadata md : terms.values()) { 
            
            final double termFreq = md.termFreq();

            md.setLocalTermWeight(termFreq / magnitude);
            
        }

        return magnitude;
        
    }
    
    public Map.Entry<String, ITermMetadata> getSingletonEntry() {
    	
    	if (terms.size() != 1) {
    		throw new RuntimeException("not a singleton");
    	}
    	
    	return terms.entrySet().iterator().next();
    	
    }
    
}
