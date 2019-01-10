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
/*
 * Created on Feb 11, 2008
 */

package org.embergraph.btree.view;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.embergraph.btree.AbstractBTree;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IAutoboxBTree;
import org.embergraph.btree.IBloomFilter;
import org.embergraph.btree.ICounter;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ILinearList;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.ITupleSerializer;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.IndexSegment;
import org.embergraph.btree.IndexSegmentStore;
import org.embergraph.btree.ReadOnlyIndex;
import org.embergraph.btree.Tuple;
import org.embergraph.btree.filter.Reverserator;
import org.embergraph.btree.filter.TupleRemover;
import org.embergraph.btree.filter.WrappedTupleIterator;
import org.embergraph.btree.isolation.IsolatedFusedView;
import org.embergraph.btree.proc.AbstractKeyArrayIndexProcedureConstructor;
import org.embergraph.btree.proc.IKeyRangeIndexProcedure;
import org.embergraph.btree.proc.IResultHandler;
import org.embergraph.btree.proc.ISimpleIndexProcedure;
import org.embergraph.counters.CounterSet;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.relation.accesspath.AccessPath;
import org.embergraph.service.MetadataService;
import org.embergraph.service.Split;

import cutthecrap.utils.striterators.IFilter;

/**
 * <p>
 * A fused view providing read-write operations on multiple B+-Trees mapping
 * variable length unsigned byte[] keys to arbitrary values. The sources MUST
 * support deletion markers. The order of the sources MUST correspond to the
 * recency of their data. Writes will be directed to the first source in the
 * sequence (the most recent source). Deletion markers are used to prevent a
 * miss on a key for a source from reading through to an older source. If a
 * deletion marker is encountered the index entry will be understood as "not
 * found" in the fused view rather than reading through to an older source where
 * it might still have a binding.
 * </p>
 * 
 * @todo consider implementing {@link IAutoboxBTree} here and collapsing
 *       {@link ILocalBTreeView} and {@link IAutoboxBTree}.
 * 
 * @todo Can I implement {@link ILinearList} here? That would make it possible
 *       to use keyAt() and indexOf() and might pave the way for a
 *       {@link MetadataService} that supports overflow since the index segments
 *       could be transparent at that point.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class FusedView implements IIndex, ILocalBTreeView {//, IValueAge {

    protected static final Logger log = Logger.getLogger(FusedView.class);

    /**
     * Error message if the view has more than {@link Long#MAX_VALUE} elements
     * and you requested an exact range count.
     */
    static protected transient final String ERR_RANGE_COUNT_EXCEEDS_MAX_LONG = "The range count can not be expressed as a 64-bit signed integer";

    /**
     * Encapsulates the sources.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private interface ISources extends Iterable<AbstractBTree> {

        /**
         * The mutable {@link BTree} for the view.
         */
        public BTree getMutableBTree();

        /**
         * The #of sources in the view.
         */
        public int getSourceCount();

        /**
         * Visits the sources in order.
         */
        @Override
        public Iterator<AbstractBTree> iterator();
        
        /**
         * Cloned copy of the sources objects.
         */
        public AbstractBTree[] getSources();

    }
    
    /**
     * Implementation based on a hard reference array which directly captures
     * the {@link AbstractBTree}[].
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private static class HardRefSources implements ISources {
        
        /**
         * A hard reference to the mutable {@link BTree} from index zero of the
         * sources specified to the ctor.
         */
        private final BTree btree;

        @Override
        public BTree getMutableBTree() {
            return btree;
        }
        
        @Override
        public int getSourceCount() {
            return srcs.length;
        }

        @Override
        public Iterator<AbstractBTree> iterator() {
       
            return Arrays.asList(srcs).iterator();
            
        }

        /**
         * Holds the various btrees that are the sources for the view.
         * 
         * FIXME Change this to assemble the AbstractBTree[] dynamically from the
         * {@link #btree} hard reference and hard references to the
         * {@link IndexSegmentStore} using
         * {@link IndexSegmentStore#loadIndexSegment()}. We could actually use hard
         * references for the index segments inside of a {@link WeakReference} to an
         * array of those references.
         */
        private final AbstractBTree[] srcs;

        @Override
        final public AbstractBTree[] getSources() {

            // Note: clone the array to prevent modification.
            return srcs.clone();
            
        }

        public HardRefSources(final AbstractBTree[] a) {
            
            checkSources(a);
            
            this.btree = (BTree) a[0];
            
            this.srcs = a.clone();

        }
        
    }

    /**
     * Implementation using a hard reference for the mutable {@link BTree} and
     * any other {@link BTree}s in the view and hard references to the
     * {@link IndexSegmentStore}s for the non-{@link BTree} sources in the
     * view. and hard. The {@link IndexSegmentStore} internally uses a
     * {@link WeakReference} to (re-)open the {@link IndexSegment} on demand.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private static class WeakRefSources implements ISources {

        /**
         * The #of sources.
         */
        private final int count;
        
        /**
         * A hard reference to the mutable {@link BTree} from index zero of the
         * sources specified to the ctor.
         */
        private final BTree btree;
        
        /**
         * A hard reference to any source which was a {@link BTree} with
         * <code>null</code>s in the other elements of the array.
         */
        private final BTree[] btreeSources;

        /**
         * A hard reference to the {@link IndexSegmentStore} for any source that
         * was an {@link IndexSegment} with <code>null</code>s in the other 
         * elements of the array.
         */
        private final IndexSegmentStore[] segmentStores;
        
        @Override
        public BTree getMutableBTree() {
            return btree;
        }
        
        @Override
        public int getSourceCount() {
            return count;
        }

        @Override
        public AbstractBTree[] getSources() {

            final AbstractBTree[] a = new AbstractBTree[count];

            for (int i = 0; i < count; i++) {

                if (btreeSources[i] != null) {

                    a[i] = btreeSources[i];

                } else {

                    /*
                     * Note: This provides a canonicalizing mapping using a weak
                     * reference and thereby decouples the FusedView from a hard
                     * reference to the IndexSegment.
                     */

                    a[i] = segmentStores[i].loadIndexSegment();

                }

            }

            return a;

        }

        @Override
        public Iterator<AbstractBTree> iterator() {

            return Arrays.asList(getSources()).iterator();
            
        }

        public WeakRefSources(final AbstractBTree[] a) {
            
            checkSources(a);

            this.count = a.length;

            this.btree = (BTree) a[0];

            this.btreeSources = new BTree[count];

            this.segmentStores = new IndexSegmentStore[count];

            for (int i = 0; i < count; i++) {

                if (a[i] instanceof BTree) {

                    btreeSources[i] = (BTree) a[i];

                } else {

                    segmentStores[i] = ((IndexSegment) a[i]).getStore();

                }
                
            }

        }
        
    }
    
    private final ISources sources;
    
    @Override
    final public AbstractBTree[] getSources() {

        return sources.getSources();
        
    }

    @Override
    final public int getSourceCount() {
        
        return sources.getSourceCount();
        
    }
    
    @Override
    final public BTree getMutableBTree() {
        
        return sources.getMutableBTree();
        
    }
    
//    /**
//     * A {@link ThreadLocal} {@link Tuple} that is used to copy the value
//     * associated with a key out of the btree during lookup operations.
//     * <p>
//     * Note: This field is NOT static. This limits the scope of the
//     * {@link ThreadLocal} {@link Tuple} to the containing {@link FusedView}
//     * instance.
//     */
//    protected final ThreadLocal<Tuple> lookupTuple = new ThreadLocal<Tuple>() {
//
//        @Override
//        protected Tuple initialValue() {
//
//            return new Tuple(getMutableBTree(),VALS);
//
//        }
//
//    };
//    
//    /**
//     * A {@link ThreadLocal} {@link Tuple} that is used for contains() tests.
//     * The tuple does not copy either the keys or the values. Contains is
//     * implemented as a lookup operation that either return this tuple or
//     * <code>null</code>. When isolation is supported, the version metadata
//     * is examined to determine if the matching entry is flagged as deleted in
//     * which case contains() will report "false".
//     * <p>
//     * Note: This field is NOT static. This limits the scope of the
//     * {@link ThreadLocal} {@link Tuple} to the containing {@link FusedView}
//     * instance.
//     */
//    protected final ThreadLocal<Tuple> containsTuple = new ThreadLocal<Tuple>() {
//
//        @Override
//        protected com.bigdata.btree.Tuple initialValue() {
//
//            return new Tuple(getMutableBTree(), 0);
//            
//        }
//        
//    };
    
    @Override
    public String toString() {
        
    	final StringBuilder sb = new StringBuilder();
        
        sb.append(getClass().getSimpleName());
        
        sb.append("{ ");

        sb.append(Arrays.toString(getSources()));
        
        sb.append("}");
        
        return sb.toString();
        
    }
    
    protected void assertNotReadOnly() {
        
        if (getMutableBTree().isReadOnly()) {

            // Can't write on this view.
            throw new IllegalStateException();
            
        }
        
    }
    
    @Override
    public IResourceMetadata[] getResourceMetadata() {

        final int n = getSourceCount();
        
        final IResourceMetadata[] resources = new IResourceMetadata[n];

        int i = 0;
        for(AbstractBTree t : sources) {
//        for (int i = 0; i < srcs.length; i++) {

            resources[i++] = t.getStore().getResourceMetadata();

        }

        return resources;

    }

    public FusedView(final AbstractBTree src1, final AbstractBTree src2) {

        this(new AbstractBTree[] { src1, src2 });

    }

    /**
     * 
     * @param srcs
     *            The ordered sources for the fused view. The order of the
     *            elements in this array determines which value will be selected
     *            for a given key by lookup() and which value is retained by
     *            rangeQuery().
     * 
     * @exception IllegalArgumentException
     *                if a source is used more than once.
     * @exception IllegalArgumentException
     *                unless all sources have the same indexUUID
     * @exception IllegalArgumentException
     *                unless all sources support delete markers.
     */
    public FusedView(final AbstractBTree[] srcs) {

        /*
         * Note: This has been abstracted and modified to NOT hold a hard
         * reference to the index segments in the view so that they may be
         * closed even while the view itself is open. This was done in an
         * attempt to reduce the memory demand associated with open index
         * segments.
         */
        
        if(false) {
        
            // hard reference to each source in the view.
            sources = new HardRefSources(srcs);
            
        } else {
            
            // hard reference only to BTrees in the view.
            sources = new WeakRefSources(srcs);
            
        }
        
    }
    
    /**
     * Checks the sources to make sure that they all support delete markers
     * (required for views), all non-null, and all have the same index UUID.
     * 
     * @param srcs
     *            The sources for a view.
     */
    static void checkSources(final AbstractBTree[] srcs) {
        
        if (srcs == null)
            throw new IllegalArgumentException("sources is null");

        /*
         * @todo allow this as a degenerate case, or create a factory that
         * produces the appropriate view?
         */
        if (srcs.length < 2) {
            
            throw new IllegalArgumentException(
                    "At least two sources are required");
            
        }
        
        for (int i = 0; i < srcs.length; i++) {
            
            if (srcs[i] == null)
                throw new IllegalArgumentException("Source null @ index=" + i);

            if (!srcs[i].getIndexMetadata().getDeleteMarkers()) {

                throw new IllegalArgumentException(
                        "Source does not maintain delete markers @ index=" + i);

            }

            for (int j = 0; j < i; j++) {
                
                if (srcs[i] == srcs[j])
                    
                    throw new IllegalArgumentException(
                            "Source used more than once"
                            );
                

                if (!srcs[i].getIndexMetadata().getIndexUUID().equals(
                        srcs[j].getIndexMetadata().getIndexUUID())) {
                    
                    throw new IllegalArgumentException(
                            "Sources have different index UUIDs @ index=" + i
                            );
                    
                }
                
            }
            
        }
        
    }

    @Override
    public IndexMetadata getIndexMetadata() {
        
        return getMutableBTree().getIndexMetadata();
        
    }
    
     
//    public IBloomFilter getBloomFilter() {
//
//        // double checked locking.
//        if (bloomFilter == null) {
//
//            synchronized (this) {
//                if (noBloom)
//                    return null;
//                for (AbstractBTree tree : getSources()) {
//                    if (tree.getBloomFilter() == null) {
//                        noBloom = true;
//                        return null;
//                    }
//                }
//                bloomFilter = new FusedBloomFilter();
//
//            }
//
//        }
//
//        return bloomFilter;
//
//    }
//
//    private volatile boolean noBloom = false;

    @Override
    public IBloomFilter getBloomFilter() {
        
        // double checked locking.
        if (bloomFilter == null) {

            synchronized (this) {

                bloomFilter = new FusedBloomFilter();
                
            }

        }

        return bloomFilter;

    }

    private volatile IBloomFilter bloomFilter = null;

    @Override
    final public CounterSet getCounters() {

    	final CounterSet counterSet = new CounterSet();

        int i = 0;
        for(AbstractBTree t : sources) {
            
            counterSet.makePath("view[" + i + "]").attach(t.getCounters());
            
            i++;
        
        }

        return counterSet;
        
    }

    /**
     * The counter for the first source.
     */
    @Override
    public ICounter getCounter() {
        
        return getMutableBTree().getCounter();
        
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Resolves the old value against the view and then directs the write to the
     * first of the sources specified to the ctor.
     */
    @Override
    public byte[] insert(final byte[] key, final byte[] value) {

        final byte[] oldval = lookup(key);
        
        getMutableBTree().insert(key, value);
        
        return oldval;

    }
    
    /**
	 * {@inheritDoc}
	 * <p>
	 * This case is a bit tricky. Since it is possible for the value stored
	 * under a key to be null, we need to obtain the Tuple for the key from the
	 * view. If the tuple is null or deleted, then we can do an unconditional
	 * insert. Otherwise there is an entry under the key and we return the value
	 * of the entry from the Tuple. Note that the value COULD be a null.
	 */
    @Override
    public byte[] putIfAbsent(final byte[] key, final byte[] value) {

    	final Tuple tuple = lookup(key, getMutableBTree().getLookupTuple());

        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Interpret a deletion marker as "not found".
             */

        	// unconditional insert.
        	getMutableBTree().insert(key, value);

        	// nothing was in the index under that key.
            return null;
            
        }

        // return the pre-existing value under the key.
        return tuple.getValue();

    }
    
    public Object insert(Object key, Object val) {

        key = getTupleSerializer().serializeKey(key);

        val = getTupleSerializer().serializeVal(val);

        final ITuple tuple = lookup((byte[]) key, getMutableBTree().getLookupTuple());

        // direct the write to the first source.
        getMutableBTree().insert((byte[]) key, (byte[]) val);
        
        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Either there was no entry under that key for any source or the
             * entry is already marked as deleted in the view.
             */
            
            return null;
            
        }

        return tuple.getObject();
        
    }

    /**
     * Resolves the old value against the view and then directs the write to the
     * first of the sources specified to the ctor. The remove is in fact treated
     * as writing a deleted marker into the index.
     */
    public byte[] remove(final byte[] key) {

        /*
         * Slight optimization prevents remove() from writing on the index if
         * there is no entry under that key for any source (or if there is
         * already a deleted entry under that key).
         */

        final Tuple tuple = lookup(key, getMutableBTree().getLookupTuple());

        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Either there was no entry under that key for any source or the
             * entry is already marked as deleted in the view so we are done.
             */
            
            return null;
            
        }

        final byte[] oldval = tuple.getValue();

        // remove from the 1st source.
        getMutableBTree().remove(key);
        
        return oldval;

    }
    
    public Object remove(Object key) {

        key = getTupleSerializer().serializeKey(key);
        
        /*
         * Slight optimization prevents remove() from writing on the index if
         * there is no entry under that key for any source (or if there is
         * already a deleted entry under that key).
         */
        final Tuple tuple = lookup((byte[])key, getMutableBTree().getLookupTuple());

        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Either there was no entry under that key for any source or the
             * entry is already marked as deleted in the view so we are done.
             */
            
            return null;
            
        }

        // remove from the 1st source.
        getMutableBTree().remove(key);

        return tuple.getObject();

    }

    /**
     * {@inheritDoc}
     * <p>
     * Return the first value for the key in an ordered search of the trees in
     * the view.
     */
    @Override
    final public byte[] lookup(final byte[] key) {

        final Tuple tuple = lookup(key, getMutableBTree().getLookupTuple());

        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Interpret a deletion marker as "not found".
             */
            
            return null;
            
        }

        return tuple.getValue();
        
    }

    @Override
    public Object lookup(Object key) {

        key = getTupleSerializer().serializeKey(key);

        final Tuple tuple = lookup((byte[]) key, getMutableBTree().getLookupTuple());

        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Interpret a deletion marker as "not found".
             */
            
            return null;
            
        }

        return tuple.getObject();
        
    }

    /**
     * Per {@link AbstractBTree#lookup(byte[], Tuple)} but0 processes the
     * {@link AbstractBTree}s in the view in their declared sequence and stops
     * when it finds the first index entry for the key, even it the entry is
     * marked as deleted for that key.
     * 
     * @param key
     *            The search key.
     * @param tuple
     *            A tuple to be populated with data and metadata about the index
     *            entry (required).
     * 
     * @return <i>tuple</i> iff an index entry was found under that <i>key</i>.
     */
    final public Tuple lookup(final byte[] key, final Tuple tuple) {

        return lookup(0, key, tuple);

    }

    /**
     * Core implementation processes the {@link AbstractBTree}s in the view in
     * their declared sequence and stops when it finds the first index entry for
     * the key, even it the entry is marked as deleted for that key.
     * 
     * @param startIndex
     *            The index of the first source to be read. This permits the
     *            lookup operation to start at an index into the {@link #srcs}
     *            other than zero. This is used by {@link IsolatedFusedView} to
     *            read from just the groundState (everything except the
     *            writeSet, which is the source at index zero(0)).
     * @param key
     *            The search key.
     * @param tuple
     *            A tuple to be populated with data and metadata about the index
     *            entry (required).
     * 
     * @return <i>tuple</i> iff an index entry was found under that <i>key</i>.
     */
    final protected Tuple lookup(final int startIndex, final byte[] key,
            final Tuple tuple) {

        for(AbstractBTree t : sources) {

            if( t.lookup(key, tuple) == null) {
                
                // No match yet.
                
                continue;
                
            }

            return tuple;
            
        }

        // no match.
        
        return null;

    }
    
	/**
	 * {@inheritDoc}
	 * <p>
	 * Processes the {@link AbstractBTree}s in the view in sequence and returns
	 * true iff the first {@link AbstractBTree} with an index entry under the
	 * key is non-deleted.
	 */
    @Override
    final public boolean contains(final byte[] key) {

        final Tuple tuple = lookup(key, getMutableBTree().getContainsTuple());
        
        if (tuple == null || tuple.isDeletedVersion()) {

            /*
             * Interpret a deletion marker as "not found".
             */
            
            return false;
            
        }

        return true;
        
    }

    @Override
    public boolean contains(Object key) {
        
        key = getTupleSerializer().serializeKey(key);

        return contains((byte[]) key);
        
    }

    private ITupleSerializer getTupleSerializer() {
        
        return getIndexMetadata().getTupleSerializer();
        
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Returns the sum of the range count on each index in the view. This is the
     * maximum #of entries that could lie within that key range. However, the
     * actual number could be less if there are entries for the same key in more
     * than one source index.
     */
    @Override
   final public long rangeCount() {

        return rangeCount(null/* fromKey */, null/* toKey */);
        
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the sum of the range count on each index in the view. This is the
     * maximum #of entries that could lie within that key range. However, the
     * actual number could be less if there are entries for the same key in more
     * than one source index.
     * 
     * @todo this could be done using concurrent threads.
     */
    @Override
    final public long rangeCount(byte[] fromKey, byte[] toKey) {

        if (fromKey == null || toKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey / toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries! However,
             * if there is only a BTree in the view then the partition metadata
             * might not be defined, so we check for that first.
             * 
             * TODO Review this assertion and optimize range count which span a
             * shard view. See
             * http://sourceforge.net/apps/trac/bigdata/ticket/470 (Optimize
             * range counts on cluster)
             */

            final LocalPartitionMetadata pmd = getIndexMetadata()
                    .getPartitionMetadata();

            if (pmd != null) {
            
                if (fromKey == null) {
                
                    fromKey = pmd.getLeftSeparatorKey();
                    
                }

                if (toKey == null) {

                    toKey = pmd.getRightSeparatorKey();

                }

            }

        }
        
        long count = 0;
        
        for(AbstractBTree t : sources) {

            final long inc = t.rangeCount(fromKey, toKey);

            if (count + inc < count) {

                log.warn(ERR_RANGE_COUNT_EXCEEDS_MAX_LONG);
                
                return Long.MAX_VALUE;
                
            }

            count += inc;
            
        }
        
        return count;
        
    }

    /**
     * {@inheritDoc}
     * <p>
     * The exact range count is obtained using a key-range scan over the view.
     */
    @Override
    final public long rangeCountExact(byte[] fromKey, byte[] toKey) {

        if (fromKey == null || toKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey / toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries! However,
             * if there is only a BTree in the view then the partition metadata
             * might not be defined, so we check for that first.
             */

            final LocalPartitionMetadata pmd = getIndexMetadata()
                    .getPartitionMetadata();

            if (pmd != null) {
            
                if (fromKey == null) {
                
                    fromKey = pmd.getLeftSeparatorKey();
                    
                }

                if (toKey == null) {

                    toKey = pmd.getRightSeparatorKey();

                }

            }

        }

        final ITupleIterator itr = rangeIterator(fromKey, toKey,
                0/* capacity */, 0/* flags */, null/* filter */);

        long n = 0;

        while (itr.hasNext()) {

            itr.next();

            if (n == Long.MAX_VALUE)
                throw new RuntimeException(ERR_RANGE_COUNT_EXCEEDS_MAX_LONG);
            
            n++;

        }

        return n;
        
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * An exact range count that includes any deleted tuples. This is obtained
     * using a key-range scan over the view.
     * 
     * @see #rangeCountExact(byte[], byte[])
     */
    @Override
    public long rangeCountExactWithDeleted(byte[] fromKey, byte[] toKey) {

        if (fromKey == null || toKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey / toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries! However,
             * if there is only a BTree in the view then the partition metadata
             * might not be defined, so we check for that first.
             */

            final LocalPartitionMetadata pmd = getIndexMetadata()
                    .getPartitionMetadata();

            if (pmd != null) {

                if (fromKey == null) {

                    fromKey = pmd.getLeftSeparatorKey();

                }

                if (toKey == null) {

                    toKey = pmd.getRightSeparatorKey();

                }

            }

        }

        // set the DELETED flag so we also see the deleted tuples.
        final Iterator itr = rangeIterator(fromKey, toKey, 0/* capacity */,
                IRangeQuery.DELETED/* flags */, null/* filter */);

        long n = 0L;

        while (itr.hasNext()) {

            itr.next();

            if (n == Long.MAX_VALUE)
                throw new RuntimeException(ERR_RANGE_COUNT_EXCEEDS_MAX_LONG);

            n++;

        }

        return n;

    }

    @Override
    public ITupleIterator rangeIterator() {

        return rangeIterator(null, null);
        
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an iterator that visits the distinct entries. When an entry
     * appears in more than one index, the entry is chosen based on the order
     * in which the indices were declared to the constructor.
     */
    @Override
    final public ITupleIterator rangeIterator(final byte[] fromKey,
            final byte[] toKey) {

        return rangeIterator(fromKey, toKey, 0/* capacity */,
                DEFAULT/* flags */, null/* filter */);
        
    }

    /**
     * <p>
     * Core implementation.
     * </p>
     * <p>
     * Note: The {@link FusedView}'s iterator first obtains an ordered array of
     * iterators for each of the source {@link AbstractBTree}s. The <i>filter</i>
     * is NOT passed through to these source iterators. Instead, an
     * {@link FusedTupleIterator} is obtained and the filter is applied to that
     * iterator. This means that filters always see a fused representation of
     * the source iterators.
     * </p>
     * <p>
     * Note: This implementation supports {@link IRangeQuery#REVERSE}. This may
     * be used to locate the {@link ITuple} before a specified key, which is a
     * requirement for several aspects of the overall architecture including
     * atomic append of file blocks, locating an index partition in the metadata
     * index, and finding the last member of a set or map.
     * </p>
     * <p>
     * Note: When the {@link IRangeQuery#CURSOR} flag is specified, it is passed
     * through and an {@link ITupleCursor} is obtained for each source
     * {@link AbstractBTree}. A {@link FusedTupleCursor} is then obtained which
     * implements the {@link ITupleCursor} extensions.
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public ITupleIterator rangeIterator(//
            byte[] fromKey,//
            byte[] toKey, //
            final int capacity, //
            final int flags,//
            final IFilter filter//
            ) {

        if (fromKey == null || toKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey / toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries! However,
             * if there is only a BTree in the view then the partition metadata
             * might not be defined, so we check for that first.
             */

            final LocalPartitionMetadata pmd = getIndexMetadata()
                    .getPartitionMetadata();

            if (pmd != null) {
            
                if (fromKey == null) {
                
                    fromKey = pmd.getLeftSeparatorKey();
                    
                }

                if (toKey == null) {

                    toKey = pmd.getRightSeparatorKey();

                }

            }

        }

        // reverse scan?
        final boolean reverseScan = (flags & REVERSE) != 0;
        
        // cursor requested?
        final boolean cursorMode = (flags & CURSOR) != 0;
        
        // read only?
        final boolean readOnly = ((flags & READONLY) != 0);

        // iff the aggregate iterator should visit deleted entries.
        final boolean deleted = (flags & DELETED) != 0;
        
        // removeAll?
        final boolean removeAll = (flags & REMOVEALL) != 0;
        
        if (readOnly && removeAll) {

            // REMOVEALL is not compatible with READONLY.
            throw new IllegalArgumentException();

        }

        final int n = sources.getSourceCount();

        if (log.isInfoEnabled())
            log.info("nsrcs=" + n + ", flags=" + flags + ", readOnly="
                    + readOnly + ", deleted=" + deleted + ", reverseScan="
                    + reverseScan);
        
        /*
         * Note: We request KEYS since we need to compare the keys in order to
         * decide which tuple to return next.
         * 
         * Note: We request DELETED so that we will see deleted entries. This is
         * necessary in order for processing to stop at the first entry for a
         * give key regardless of whether it is deleted or not. If the caller
         * does not want to see the deleted tuples, then they are silently
         * dropped from the aggregate iterator.
         * 
         * Note: The [filter] is NOT passed through to the source iterators.
         * This is because the filter must be applied to the aggregate iterator
         * in order to operate on the fused view.
         * 
         * Note: The REVERSE flag is NOT passed through to the source iterators.
         * It is handled below by layering on a filter.
         * 
         * Note: The REMOVEALL flag is NOT passed through to the source
         * iterators. It is handled below by laying on a filter.
         */
        final int sourceFlags = (//
                (flags | KEYS | DELETED)//
                | (reverseScan || removeAll ? CURSOR : 0) //
                )//
                & (~REMOVEALL)// turn off
                & (~REVERSE)// turn off
                ;
        
        /*
         * The source iterator produces a fused view of the source indices. We
         * then layer the filter(s) over the fused view iterator. A subclass is
         * used if CURSOR support is required for the fused view.
         */
        ITupleIterator src;
        
        if (cursorMode || removeAll || reverseScan) {
        
            /*
             * CURSOR was specified for the aggregate iterator or is required to
             * support REMOVEALL.
             */

            final ITupleCursor[] itrs = new ITupleCursor[n];

            int i = 0;
            for(AbstractBTree t : sources) {

                itrs[i++] = (ITupleCursor) t.rangeIterator(fromKey, toKey,
                        capacity, sourceFlags, null/* filter */);

            }

            // Note: aggregate source implements ITupleCursor.
            src = new FusedTupleCursor(flags, deleted, itrs, 
                    readOnly?new ReadOnlyIndex(this):this);
            
        } else {

            /*
             * CURSOR was neither specified nor required for the aggregate
             * iterator.
             * 
             * Note: If reverse was specified, then we pass it into the source
             * iterators for the source B+Trees. The resulting fused iterator
             * will already have reversal traversal semantics so we do not need
             * to layer a Reverserator on top.
             */

            final ITupleIterator[] itrs = new ITupleIterator[n];

            int i = 0;
            for (AbstractBTree t : sources) {

               itrs[i++] = t.rangeIterator(fromKey, toKey, capacity,
                        sourceFlags, null/* filter */);

            }

            src = new FusedTupleIterator(flags, deleted, itrs);

        }

        if (reverseScan) {

            /*
             * Reverse scan iterator.
             * 
             * Note: The reverse scan MUST be layered directly over the
             * ITupleCursor. Most critically, REMOVEALL combined with a REVERSE
             * scan needs to process the tuples in reverse index order and then
             * delete them as it goes.
             */

            src = new Reverserator((ITupleCursor) src);

        }

        if (filter != null) {

            /*
             * Apply the optional filter.
             * 
             * Note: This needs to be after the reverse scan and before
             * REMOVEALL (those are the assumptions for the flags).
             */
            
            src = new WrappedTupleIterator(filter
                    .filter(src, null/* context */));
            
        }
        
        if ((flags & REMOVEALL) != 0) {
            
            assertNotReadOnly();
            
            /*
             * Note: This iterator removes each tuple that it visits from the
             * source iterator.
             */
            
            src = new TupleRemover() {
                private static final long serialVersionUID = 1L;
                @Override
                protected boolean remove(ITuple e) {
                    // remove all visited tuples.
                    return true;
                }
            }.filterOnce(src, null/* context */);

        }
        
        return src;

    }
    
    @Override
	final public <T> T submit(final byte[] key, final ISimpleIndexProcedure<T> proc) {

        return proc.apply(this);

    }

    @Override
    @SuppressWarnings("unchecked")
    final public void submit(byte[] fromKey, byte[] toKey,
            final IKeyRangeIndexProcedure proc, final IResultHandler handler) {

        if (fromKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey/toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries!
             */
            fromKey = getIndexMetadata().getPartitionMetadata()
                    .getLeftSeparatorKey();

        }

        if (toKey == null) {

            /*
             * Note: When an index partition is split, the new index partitions
             * will initially use the same source index segments as the original
             * index partition. Therefore we MUST impose an explicit constraint
             * on the fromKey/toKey if none is given so that we do not read
             * tuples lying outside of the index partition boundaries!
             */
            toKey = getIndexMetadata().getPartitionMetadata()
                    .getRightSeparatorKey();

        }
        
        final Object result = proc.apply(this);
        
        if (handler != null) {
            
            handler.aggregate(result, new Split(null,0,0));
            
        }
        
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void submit(final int fromIndex, final int toIndex,
            final byte[][] keys, final byte[][] vals,
            final AbstractKeyArrayIndexProcedureConstructor ctor,
            final IResultHandler aggregator) {

        final Object result = ctor.newInstance(this, fromIndex, toIndex, keys,
                vals).apply(this);

        if (aggregator != null) {

            aggregator.aggregate(result, new Split(null, fromIndex, toIndex));

        }
        
    }

    /**
     * Inner class providing a fused view of the optional bloom filters
     * associated with each of the source indices.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    protected class FusedBloomFilter implements IBloomFilter {

        /**
         * Unsupported operation.
         * 
         * @throws UnsupportedOperationException
         *             always.
         */
        public boolean add(byte[] key) {
            
            throw new UnsupportedOperationException();
            
        }

        /**
         * Applies the {@link IBloomFilter} for each source index in turn and
         * returns <code>true</code> if ANY of the component index filters
         * return <code>true</code> (if any filters say that their index has
         * data for that key then you need to read the index). If a filter does
         * not exist (or has been disabled) for a given component index then the
         * code treats the filter as always reporting <code>true</code> (that
         * is, forcing us to check the index). So a source component index
         * without a bloom filter or an index with a disabled bloom filter acts
         * as its filter has a high false positive rate, however the test is a
         * NOP so it is cheap.
         */
        public boolean contains(final byte[] key) {

            final AbstractBTree[] srcs = getSources();
            
            for (int i = 0; i < srcs.length; i++) {

                final AbstractBTree src = srcs[i];
                
                final IBloomFilter filter = src.getBloomFilter();

                if ((i == 0 || i == 1) && srcs.length > i + 1
                        && src instanceof BTree
                        && srcs[i + 1].getBloomFilter() != null) {

                    /*
                     * Do a real point test when we have a FusedView and the 1st
                     * or 2nd component of the view is a BTree and there are
                     * additional components in the view and they have a bloom
                     * filter enabled. This covers the case where the BTree is
                     * absorbing writes (either isolated or unisolated, which is
                     * why we allow the 1st or 2nd component) and there are
                     * additional views, which presumably are IndexSegments. The
                     * reasoning is that the BTree contains test will be
                     * relatively fast and we still get to apply the bloom
                     * filters on the index segments, thereby avoiding a disk
                     * hit in those cases where the bloom filter for the mutable
                     * index on a ManagedJournal has been turned off but the
                     * index segments have perfect bloom filters that we still
                     * want to leverage. Testing the BTree instance here might
                     * touch the disk, but it will force the node path into the
                     * cache so if we do get a 'true' response from one of the
                     * bloom filters and have to test the indices it will not
                     * add additional disk hits.
                     */
                    
                    if(src.contains(key)) {
                        
                        // proven (but interpreted as probable hit).
                        return true;
                        
                    }
                    
                    // test the next source index.
                    continue;
                    
                }
                
                if (filter == null || filter.contains(key)) {

                    /*
                     * Either no filter, a disabled filter, or the filter exists
                     * and reports that it has seen the key. At the worst, this
                     * is a false positive and we will be forced to check the
                     * index.
                     */
                    
                    return true;
                    
                }

            }

            // proven to not be in the index.
            return false;

        }

        /**
         * This implementation notifies the bloom filter for the first source
         * index (if it exists). Normally false positives will be reported
         * directly to the specific bloom filter instance by the contains() or
         * lookup() method for that index. However, the
         * {@link AccessPath} also tests the bloom filter and needs a
         * means to report false positives. It should be the only one that calls
         * this method on this implementation class.
         */
        public void falsePos() {

            final IBloomFilter filter = getMutableBTree().getBloomFilter();

            if (filter != null) {

                filter.falsePos();

            }
            
        }
        
    }

}
