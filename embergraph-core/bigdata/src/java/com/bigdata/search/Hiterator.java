package com.bigdata.search;

import java.util.Iterator;

/**
 * Visits search results in order of decreasing relevance.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Hiterator<A extends IHit> implements Iterator<A> {

	/**
	 * The index into the array of hits wrapped by this iterator.
	 */
	private int rank = 0;
	
	/**
	 * The array of hits wrapped by this iterator.
	 */
	private final A[] hits;
	
	/**
	 * 
	 * @param hits
	 */
	public Hiterator(final A[] hits) {

		if (hits == null)
			throw new IllegalArgumentException();

		this.hits = hits;

	}
	
	public boolean hasNext() {
		
		return rank < hits.length;
		
	}
	
	public A next() {
		
		return hits[rank++];
		
	}
	
    /**
     * @throws UnsupportedOperationException
     */
    public void remove() {
        
        throw new UnsupportedOperationException();
        
    }

    public String toString() {
        
        return "Hiterator{nhits=" + hits.length + "} : "
                + hits;
        
    }
    
    public int size() {
    	
    	return hits.length;
    	
    }

//	
//	
//    final private Collection<A> hits;
//    final private Iterator<A> src;
////    final private double minCosine;
////    final private int maxRank;
//
//    /**
//     * The rank of the last hit returned by {@link #next()}. The rank is in
//     * [1:nhits] and is incremented as we return each hit in {@link #next()}.
//     */
//    private int rank = 0;
//    
//    /** set by {@link #hasNext()}. */
//    private boolean exhausted = false;
//    
//    /** set by {@link #hasNext()}. */
//    private A nextHit = null;
//    
////    /** set by {@link #next()}. */
////    private A lastHit = null;
//
////    /**
////     * The minimum cosine that will be visited as specified to
////     * {@link FullTextIndex#search(String, String, double, int)}
////     */
////    public double minCosine() {
////        
////        return minCosine;
////        
////    }
////    
////    /**
////     * The maximum rank that will be visited as specified to
////     * {@link FullTextIndex#search(String, String, double, int)}
////     */
////    public int maxRank() {
////        
////        return maxRank;
////        
////    }
//
//    /**
//     * 
//     * @param hits
//     */
//    public Hiterator(final Collection<A> hits
////            ,final double minCosine, final int maxRank
//            ) {
//
//        if (hits == null)
//            throw new IllegalArgumentException();
//
////        if (minCosine < 0d || minCosine > 1d)
////            throw new IllegalArgumentException();
//
////        if (maxRank <= 0)
////            throw new IllegalArgumentException();
//
//        this.hits = hits;
//
////        this.minCosine = minCosine;
////        
////        this.maxRank = maxRank;
//        
//        this.src = hits.iterator();
//        
//    }
//
////    /**
////     * The #of hits (approximate).
////     * 
////     * @todo differentiate between the #of hits and the #of hits that satisfy
////     *       the minCosine and maxRank criteria
////     * 
////     * @todo this and other search engine metadata (elapsed time) might go on a
////     *       different object from which we can obtain the {@link Hiterator}.
////     */
////    public long size() {
////        
////        return hits.size();
////        
////    }
//    
//    public boolean hasNext() {
//
//        if(exhausted) return false;
//        
//        if(nextHit!=null) return true;
//        
//        if(!src.hasNext()) {
//            
//            exhausted = true;
//            
//            return false;
//            
//        }
//
//        nextHit = src.next();
//
////        if (rank + 1 > maxRank || nextHit.getCosine() < minCosine) {
////
////            exhausted = true;
////
////            return false;
////
////        }
//
//        return true;
//        
//    }
//
//    public A next() {
//     
//        if(!hasNext()) throw new NoSuchElementException();
//        
//        final A tmp = nextHit;
//        
//        nextHit = null;
//        
//        rank++;
//        
//        return tmp;
//        
//    }
//    
//    /**
//     * The rank of the last hit returned (origin ONE).
//     * 
//     * @throws IllegalStateException
//     *             if nothing has been visited yet.
//     */
//    public int rank() {
//
//        if (rank == 0)
//            throw new IllegalStateException();
//        
//        return rank;
//        
//    }
//
//    /**
//     * @throws UnsupportedOperationException
//     */
//    public void remove() {
//        
//        throw new UnsupportedOperationException();
//        
//    }
//
//    public String toString() {
//        
//        return "Hiterator{"+/*minCosine=" + minCosine
//                + ", maxRank=" + maxRank + ",*/" nhits=" + hits.size() + "} : "
//                + hits;
//        
//    }
//    
//    public int size() {
//    	return hits.size();
//    }
    
}
