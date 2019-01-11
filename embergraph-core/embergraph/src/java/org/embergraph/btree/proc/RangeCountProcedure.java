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
package org.embergraph.btree.proc;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.embergraph.btree.IIndex;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.mdi.LocalPartitionMetadata;

/**
 * This procedure computes a range count on an index.
 */
public class RangeCountProcedure extends AbstractKeyRangeIndexProcedure<Long>
        implements IParallelizableIndexProcedure<Long> {

    private static final long serialVersionUID = 5856712176446915328L;

    private boolean exact;
    private boolean deleted;
    
    /**
     * De-serialization ctor.
     */
    public RangeCountProcedure() {
        
        super();
        
    }

    /**
     * Range count using the specified bounds.
     * 
     * @param exact
     *            iff an exact range count is required.
     * @param deleted
     *            iff deleted tuples must be included in the count.
     * @param fromKey
     *            The lower bound (inclusive) -or- <code>null</code> if there
     *            is no lower bound.
     * @param toKey
     *            The upper bound (exclusive) -or- <code>null</code> if there
     *            is no upper bound.
     * 
     * @throws IllegalArgumentException
     *             if <i>exact</i> is <code>false</code> and <i>deleted</i>
     *             is <code>true</code> (there is no approximate reporting for
     *             range counts of deleted and undeleted tuples).
     */
    public RangeCountProcedure(boolean exact, boolean deleted, byte[] fromKey,
            byte[] toKey) {

        super( fromKey, toKey );
        
        if (!exact && deleted)
            throw new IllegalArgumentException();
        
        this.exact = exact;
        
        this.deleted = deleted;
        
    }

    @Override
    public final boolean isReadOnly() {
        
        return true;
        
    }
    
    /**
     * Return <code>true</code> iff the result count must be exact.
     */
    public final boolean isExact() {
        
        return exact;
        
    }

    /**
     * Return <code>true</code> iff deleted tuples must be included in the
     * result.
     */
    public final boolean isDeleted() {
        
        return deleted;
        
    }

    /**
     * <p>
     * Range count of entries in a key range for the index.
     * </p>
     * <p>
     * Note: When the index {@link IndexMetadata#getDeleteMarkers()} this method
     * reports the upper bound estimate of the #of key-value pairs in the key
     * range of the index. The estimate is an upper bound because duplicate or
     * deleted entries in that have not been eradicated through a suitable
     * compacting merge will be reported. An exact count may be obtained using a
     * range iterator by NOT requesting either the keys or the values.
     * </p>
     * 
     * @return The upper bound estimate of the #of key-value pairs in the key
     *         range of the named index.
     */
    @Override
    public Long apply(final IIndex ndx) {

        /*
         * Constrain the (fromKey, toKey) so that they address only the current
         * index partition. This allows the same instance of the procedure to be
         * mapped across a range of index partitions while constraining the query
         * to lie within the index partition.
         * 
         * Note: This uses a local variable to prevent side effects.
         */

        final LocalPartitionMetadata pmd = ndx.getIndexMetadata()
                .getPartitionMetadata();

        final byte[] fromKey = constrainFromKey(this.fromKey, pmd);

        final byte[] toKey = constrainToKey(this.toKey, pmd);

        final long rangeCount;
        if (exact) {
            if (deleted) {
                rangeCount = ndx.rangeCountExactWithDeleted(fromKey, toKey);
            } else {
                rangeCount = ndx.rangeCountExact(fromKey, toKey);
            }
        } else {
            rangeCount = ndx.rangeCount(fromKey, toKey);
        }
                
        return Long.valueOf(rangeCount);

    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {

        super.readExternal(in);
        
        exact = in.readBoolean();

        deleted = in.readBoolean();
        
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {

        super.writeExternal(out);
        
        out.writeBoolean(exact);
        
        out.writeBoolean(deleted);
        
    }

}
