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
/*
 * Created on Feb 16, 2007
 */

package org.embergraph.btree;

import java.util.Iterator;

import org.embergraph.btree.proc.AbstractKeyArrayIndexProcedureConstructor;
import org.embergraph.btree.proc.IResultHandler;
import org.embergraph.journal.IResourceManager;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.service.Split;

import cutthecrap.utils.striterators.IFilter;

/**
 * A fly-weight wrapper that does not permit write operations and reads through
 * onto an underlying {@link IIndex}.
 * <p>
 * Note: use this class sparingly. An index loaded from a historical commit
 * point will always be read-only.
 * 
 * @see {@link IResourceManager#getIndex(String, long)}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ReadOnlyIndex extends DelegateIndex {
    
    public ReadOnlyIndex(IIndex src) {
        
        super(src);
        
    }

    /** {@link IndexMetadata} is cloned to disallow modification. */
    @Override
    final public IndexMetadata getIndexMetadata() {

        return super.getIndexMetadata().clone();
        
    }

    /**
     * {@link IResourceMetadata}[] is cloned to disallow modification (the
     * {@link IResourceMetadata} instances in the array are all dynamically
     * created so changes to them do not propagate back to the index).
     */
    @Override
    final public IResourceMetadata[] getResourceMetadata() {

        return super.getResourceMetadata().clone();
        
    }

    /**
     * Counter is read-only.
     */
    @Override
    final public ICounter getCounter() {

        return new ReadOnlyCounter(super.getCounter());
        
    }
    
    /**
     * Disabled.
     */
    @Override
    final public byte[] insert(byte[] key, byte[] value) {

        throw new UnsupportedOperationException();
        
    }

    /**
     * Disabled.
     */
    @Override
    final public byte[] remove(byte[] key) {

        throw new UnsupportedOperationException();
        
    }

    /**
     * {@link IRangeQuery#REMOVEALL} and {@link Iterator#remove()} are disabled.
     */
    @Override
    final public ITupleIterator rangeIterator(byte[] fromKey, byte[] toKey,
            int capacity, int flags, IFilter filter) {

        if ((flags & REMOVEALL) != 0) {

            /*
             * Note: Must be explicitly disabled!
             */
            
            throw new UnsupportedOperationException();
            
        }

        /*
         * Must explicitly disable Iterator#remove().
         */
        return new ReadOnlyEntryIterator(super.rangeIterator(fromKey, toKey,
                capacity, flags, filter));
        
    }
    
//    /**
//     * Overridden to ensure that procedure is applied against read-only view and
//     * not the {@link DelegateIndex}.
//     */
//    @Override
//    final public <T> T submit(final byte[] key, IIndexProcedure<T> proc) {
//    
//        return proc.apply(this);
//        
//    }
//
//    /**
//     * Overridden to ensure that procedure is applied against read-only view and
//     * not the {@link DelegateIndex}.
//     */
//    @Override
//    @SuppressWarnings("unchecked")
//    final public void submit(final byte[] fromKey, final byte[] toKey,
//            final IIndexProcedure proc, final IResultHandler handler) {
//
//        final Object result = proc.apply(this, handler);
//        
//        if (handler != null) {
//            
//            handler.aggregate(result, new Split(null,0,0));
//            
//        }
//        
//    }
    
    /**
     * Overridden to ensure that procedure is applied against read-only view and
     * not the {@link DelegateIndex}.
     */
    @SuppressWarnings("unchecked")
    @Override
    final public void submit(int fromIndex, int toIndex, byte[][] keys, byte[][] vals,
            AbstractKeyArrayIndexProcedureConstructor ctor, IResultHandler aggregator) {

        final Object result = ctor.newInstance(this,fromIndex, toIndex, keys, vals)
                .apply(this);
        
        if(aggregator != null) {
            
            aggregator.aggregate(result, new Split(null,fromIndex,toIndex));
            
        }
        
    }

}
