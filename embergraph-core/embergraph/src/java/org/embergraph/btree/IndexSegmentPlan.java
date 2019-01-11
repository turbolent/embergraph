/*
Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

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
package org.embergraph.btree;

import org.apache.log4j.Logger;

import org.embergraph.btree.IndexMetadata.Options;

/**
 * A plan for building a B+-Tree based on an input branching factor and #of
 * entries.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class IndexSegmentPlan {

    protected static final transient Logger log = Logger
            .getLogger(IndexSegmentPlan.class);

    /**
     * A summary representation of the index build plan. The branching factor
     * and the #of entries are the inputs.  The outputs include the height of
     * the B+Tree that should be generated and the #of nodes and leaves that
     * will exist in that B+Tree. 
     */
    public String toString() {

        return getClass() + "{branchingFactor=" + m + ", nentries=" + nentries
                + ", height=" + height + ", nnodes=" + nnodes + ", nleaves="
                + nleaves + "}";
        
    }
    
    /**
     * The branching factor of the output tree (input).
     */
    final public int m;
    
    /**
     * The minimum #of values that may be placed into non-root leaf (and
     * also the minimum #of children that may be placed into a non-root
     * node). (the minimum capacity).
     */
    final public int m2; 
    
    /**
     * The #of entries in the btree (input).
     */
    final public long nentries;
    
    /**
     * The #of leaves that will exist in the output tree. When nleaves == 1
     * the output tree will consist of a root leaf. In this case we do not
     * open a temporary file for the nodes since there will not be any.
     */
    final public long nleaves; 

    /**
     * The #of non-leaf nodes in the output tree.
     */
    final public long nnodes;
    
    /**
     * The height of the output tree (#of levels in the output tree).
     */
    final public int height;

    /**
     * The #of entries to place into each leaf. The array is dimensioned to
     * {@link #nleaves}. This is a convenience reference to the last array in
     * {@link #numInNode}.
     */
    final public int[] numInLeaf;
    
    /**
     * The #of nodes at each level of the tree, including the level containing
     * the leaves.
     * 
     * @see #nleaves, which is the #of leaves in the output tree.
     */
    final public long[] numInLevel;

    /**
     * The #of children / values to place into each node in each level of the
     * output tree. The first index is the level in the tree, starting from
     * level zero which is the root and increasing through level [height+1],
     * which is the level containing the leaves of the output tree.
     * 
     * @see #numInLeaf numInLeaf, which is a reference to the last element of
     *      this array.
     */
    final public int[][] numInNode;

    /**
     * Create a plan for building a B+-Tree. The plan has only these two inputs.
     * Everything else about the plan is deterministic based on those values.
     * 
     * @param m
     *            The branching factor of the output tree (#of keys/values for a
     *            leaf or the #of children for a node).
     * @param nentries
     *            The #of entries in the tree.
     * 
     * @throws IllegalArgumentException
     *             if the branching factor is less than
     *             {@value Options#MIN_BRANCHING_FACTOR}.
     * @throws IllegalArgumentException
     *             if the #of index entries is negative (zero is allowed as a
     *             special case).
     */
    public IndexSegmentPlan(final int m, final long nentries) {

        if (m < Options.MIN_BRANCHING_FACTOR)
            throw new IllegalArgumentException();
        
        if (nentries < 0)
            throw new IllegalArgumentException();

        // The branching factor of the output tree.
        this.m = m;
        
        // The #of entries in the btree.
        this.nentries = nentries;
        
        // The minimum capacity of a leaf (or a node).
        m2 = (m+1)/2; 
        
        if(nentries == 0) {
        
            /*
             * Special case for an empty tree.
             */

            if (log.isInfoEnabled())
                log.info("Empty tree.");
            
            nleaves = 1;
            height = 0;
            numInLeaf = new int[]{0};
            numInNode = new int[][]{new int[]{0}};
            numInLevel = new long[]{1};
            nnodes = 0;
            
            return;
        
        }
        
        // The #of leaves in the output tree.
        nleaves = (int)Math.ceil((double)nentries / (double)m); 
        
        // The height of the output tree.
        height = getMinimumHeight(m,nleaves);

        if (log.isInfoEnabled())
            log.info("branchingFactor=" + m + ", nentries=" + nentries
                    + ", nleaves=" + nleaves + ", height=" + height);
        
        // #of entries in each leaf.
        numInLeaf = distributeKeys(m, m2, nleaves, nentries);

        /*
         * Figure out how many nodes are in each level of the output tree. We
         * start from the leaves and compute the #of nodes required to hold that
         * many child references.
         */

        numInNode = new int[height+1][];

        numInLevel = new long[height+1];

        /*
         * The first time through this loop the #of children is initialized to
         * the #of leaves. Thereafter is is [numThisLevel] for the previous
         * level.
         */
        long nchildren = nleaves;

        int nnodes = 0;
        
        for (int h = height - 1; h >= 0; h--) {

            /*
             * Compute the minimum #of nodes required to hold the references for
             * the children of the level in the tree beneath this one.
             */
			final long numThisLevel = (long) Math.ceil((double) nchildren
					/ (double) m);

            numInLevel[h] = numThisLevel;

            /*
             * Distribute the children among the nodes allocated for this level.
             */
            numInNode[h] = distributeChildren(m, m2, numThisLevel, nchildren);

            nchildren = numThisLevel;

            nnodes += numThisLevel;

        }
        
        numInNode[height] = numInLeaf;
        
        numInLevel[height] = nleaves;

        this.nnodes = nnodes;

    }

    /**
     * Chooses the minimum height for a tree having a specified branching factor
     * and a specified #of leaves.
     * 
     * @param m
     *            The branching factor.
     * @param nleaves
     *            The #of leaves that must be addressable by the tree.
     * 
     * @throws UnsupportedOperationException
     *             if it is not possible to build a B+Tree with that branching
     *             factor and that many leaves without exceeding <i>maxHeight</i>
     *             (statically configured to <code>10</code>).
     */
    public static int getMinimumHeight(final int m, final long nleaves) {
        
        final int maxHeight = 10;
        
        for (int h = 0; h <= maxHeight; h++) {
        
            /*
             * The maximum #of leaves addressable by a tree of height h and the
             * given branching factor.
             * 
             * Note: Java guarantees that Math.pow(int,int) produces the exact
             * result iff that result can be represented as an integer. This
             * useful feature lets us avoid having to deal with precision issues
             * or write our own integer version of pow (computing m*m h times).
             */
			final double d = (double) Math.pow(m, h);
            
			if (d >= nleaves) {
            
                /*
                 * h is the smallest height tree of the specified branching
                 * factor m capable of addressing the specified #of leaves.
                 */
                return h;
                
            }
            
        }
        
        throw new UnsupportedOperationException(
                "Can not build tree with height less than " + maxHeight
                        + " given branchingFactor=" + m + ",  nleaves="
                        + nleaves);
    }

	/**
	 * Distributes the keys among the leaves.
	 * <p>
	 * We want to fill up every leaf, but we have to make sure that the last
	 * leaf is not under capacity. To that end, we calculate the #of entries
	 * that would remain if we filled up n-1 leaves completely. If the #of
	 * remaining entries is less than or equal to the minimum capacity of a
	 * leaf, then we have to adjust the allocation of entries such that the last
	 * leaf is at its minimum capacity. This is done by computing the shortage
	 * and then distributing that shortage among the leaves. Once we have
	 * deferred enough entries we are guaranteed that the final leaf will not be
	 * under capacity.
	 * 
	 * @param m
	 *            The branching factor in the output tree.
	 * @param m2
	 *            The minimum capacity for a leaf in the output tree, which is
	 *            computed as (m+1)/2.
	 * @param nleaves
	 *            The #of leaves in the output tree.
	 * @param nentries
	 *            The #of entries to be inserted into the output tree.
	 * 
	 * @return An array indicating how many entries should be inserted into each
	 *         leaf of the output tree. The array index is the leaf order
	 *         (origin zero). The value is the capacity to which that leaf
	 *         should be filled.
	 * @throws IllegalArgumentException
	 *             if there is a problem with the arguments.
	 * 
	 * @see TestIndexSegmentPlan
	 * @see TestIndexSegmentBuilderWithSmallTree#test_problem3_buildOrder3()
	 */
    public static int[] distributeKeys(final int m, final int m2,
            final long nleaves, final long nentries) {

		if (m < Options.MIN_BRANCHING_FACTOR)
			throw new IllegalArgumentException();
		if (m > Options.MAX_INDEX_SEGMENT_BRANCHING_FACTOR)
			throw new IllegalArgumentException();
		if (m2 < (m + 1) / 2)
			throw new IllegalArgumentException();
		if (m2 > m)
			throw new IllegalArgumentException();
		if (nleaves <= 0)
			throw new IllegalArgumentException();
		if (nleaves > Integer.MAX_VALUE) {
			/*
			 * Note: We can not build a plan with more than MAX_INT leaves since
			 * that would require an array with an int64 index.
			 */
			throw new IllegalArgumentException();
		}
		if (nentries <= 0)
			throw new IllegalArgumentException();

        if (nleaves == 1) {
            
            /*
             * If there is just a root leaf then any number (up to the leafs
             * capacity) will fit into that root leaf.
             */
            
			if (nentries > m)
				throw new RuntimeException();
            
			return new int[] { (int) nentries }; // Note: nentries<=m<=MAX_INT.
            
        }
            
		final int[] n = new int[(int) nleaves]; // Note: nleaves<=MAX_INT.

        /*
         * Default each leaf to m entries.
         */
        for (int i = 0; i < nleaves; i++) {
            
            n[i] = m;
            
        }

        /*
         * The #of entries that would be allocated to the last leaf if we filled
         * each proceeding leaf to its capacity of [m] tuples.
         */
        final long remaining = nentries - ((nleaves - 1) * m);

		/*
		 * If the #of entries remaining would put the leaf under capacity then
		 * we compute the shortage. We need to defer this many entries from the
		 * previous leaves in order to have the last leaf reach its minimum
		 * capacity.
		 * 
		 * Note: This will be a small integer in [0:m2).
		 */
		int shortage = (int) (remaining < m2 ? m2 - remaining : 0);

        if( remaining < m2 ) {

			// The last leaf will be at minimum capacity.
			n[(int) (nleaves - 1)] = m2;
            
        } else {
			/*
			 * The remainder will go into the last leaf without underflow.
			 * 
			 * Note: remaining will be a small integer (LT m).
			 */
			n[(int) (nleaves - 1)] = (int) remaining;
        }

        /*
         * If the shortage is greater than the #of previous leaves, then we need
         * to short some leaves by more than one entry. This scenario can be
         * observed when building a tree with m := 9 and 10 entries. In that
         * case there are only two leaves and we wind up shorting the previous
         * leaf by 4 bringing both leaves down to their minimum capacity of 5.
         */
        if (shortage > 0) {
            
            while (shortage > 0) {

				for (int i = (int) (nleaves - 2); i >= 0 && shortage > 0; i--) {

                    n[i]--;

					shortage--;

                }
                
            }
            
        }

        return n;
        
    }
    
    /**
     * Distributes the children among the nodes of a given level.
     * <p>
     * Note: This is just an alias for
     * {@link #distributeKeys(int, int, long, long)}. The only difference when
     * distributing children among nodes is that the result returned to the
     * caller must be interpreted as the #of children to assigned to each node
     * NOT the #of keys (for leaves the #of values and the #of keys is always
     * the same).
     * 
     * @param m
     *            The branching factor in the output tree.
     * @param m2
     *            The minimum capacity, which should be computed as (m+1)/2.
     * @param nnodes
     *            The #of nodes in the output tree for some given level of the
     *            output tree.
     * @param nchildren
     *            The #of children to be distributed among those nodes.
     * 
     * @return An array indicating how many children should be inserted into
     *         each node of the output tree at the given level. The array index
     *         is the node order (origin zero). The value is the #of children
     *         which must be assigned to that leaf.
     * 
     * @see TestIndexSegmentPlan
     * @see TestIndexSegmentBuilderWithSmallTree#test_problem3_buildOrder3()
     */
	public static int[] distributeChildren(int m, int m2, long nnodes,
			long nchildren) {

		return distributeKeys(m, m2, nnodes, nchildren);

    }
    
}
