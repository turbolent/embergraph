/*

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
package com.bigdata.rdf.inf;

import java.util.NoSuchElementException;
import com.bigdata.btree.IIndex;
import com.bigdata.btree.IRangeQuery;
import com.bigdata.btree.ITupleIterator;
import com.bigdata.btree.keys.IKeyBuilder;
import com.bigdata.btree.keys.KeyBuilder;
import com.bigdata.btree.keys.SuccessorUtil;
import com.bigdata.rdf.spo.ISPO;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * Fully buffers and then visits all {@link Justification}s for a given
 * statement.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class FullyBufferedJustificationIterator implements IJustificationIterator {

    /** the database. */
    private final AbstractTripleStore db;
    
    /** the statement whose justifications are being visited. */
    private final ISPO head;

    /**
     * Private key builder.
     * <p>
     * Note: This capacity estimate is based on N longs per SPO, one head,
     * and 2-3 SPOs in the tail. The capacity will be extended automatically
     * if necessary.
     */
    private final IKeyBuilder keyBuilder;

    /** the index in which the justifications are stored. */
    private final IIndex ndx;
    private final Justification[] justifications;
    private final int numJustifications;
    
    private boolean open = true;
    private int i = 0;
    private Justification current = null;
    
    /**
     * 
     * @param db
     * @param head The statement whose justifications will be materialized.
     */
    public FullyBufferedJustificationIterator(final AbstractTripleStore db,
            final ISPO head) {

        assert db != null;
        
        assert head != null;
        
        this.db = db;
        
        this.head = head;
        
        this.ndx = db.getSPORelation().getJustificationIndex();
        
        keyBuilder = KeyBuilder.newInstance();

        head.s().encode(keyBuilder);
        head.p().encode(keyBuilder);
        head.o().encode(keyBuilder);
        
        final byte[] fromKey = keyBuilder.getKey();

        final byte[] toKey = SuccessorUtil.successor(fromKey.clone());

        final long rangeCount = ndx.rangeCount(fromKey, toKey);

        if (rangeCount > 5000000) {

            // Limit at 5M.  See https://sourceforge.net/apps/trac/bigdata/ticket/606 (Array Limits in Truth Maintenance)
            
            throw new RuntimeException(
                    "Too many justifications to materialize: " + rangeCount);

        }

        this.justifications = new Justification[(int) rangeCount ];

        /*
         * Materialize the matching justifications.
         */
        
        final ITupleIterator itr = ndx.rangeIterator(fromKey, toKey,
                0/* capacity */, IRangeQuery.KEYS, null/* filter */);

        int i = 0;

        while (itr.hasNext()) {

            justifications[i++] = (Justification) itr.next().getObject();
            
        }

        this.numJustifications = i;
        
    }
    
    public boolean hasNext() {

        if(!open) return false;
        
        assert i <= numJustifications;
        
        if (i == numJustifications) {

            return false;
            
        }

        return true;
        
    }

    public Justification next() {
        
        if (!hasNext()) {

            throw new NoSuchElementException();
        
        }
        
        current = justifications[i++];
        
        return current;
        
    }

    /**
     * Removes the last {@link Justification} visited from the database
     * (non-batch API).
     */
    public void remove() {

        if (!open)
            throw new IllegalStateException();

        if(current==null) {
            
            throw new IllegalStateException();
            
        }
        
        /*
         * Remove the justifications from the store (note that there is no value
         * stored under the key).
         */

        ndx.remove(Justification.getKey(keyBuilder, current));
        
    }

    public void close() {

        if(!open) return;
        
        open = false;
        
    }

    public Justification[] nextChunk() {
    
        if (!hasNext()) {

            throw new NoSuchElementException();
            
        }

        final Justification[] ret;
        
        if (i == 0 && numJustifications == justifications.length) {
            
            /*
             * The SPO[] does not have any unused elements and nothing has been
             * returned to the caller by next() so we can just return the
             * backing array in this case.
             */
            
            ret = justifications;
            
        } else {

            /*
             * Create and return a new SPO[] containing only the statements
             * remaining in the iterator.
             */
            
            final int remaining = numJustifications - i;
            
            ret = new Justification[remaining];
            
            System.arraycopy(justifications, i, ret, 0, remaining);
            
        }
        
        // indicate that all statements have been consumed.
        
        i = numJustifications;
        
        return ret;

    }

}
