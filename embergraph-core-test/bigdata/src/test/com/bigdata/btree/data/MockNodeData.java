package com.bigdata.btree.data;

import com.bigdata.btree.raba.IRaba;
import com.bigdata.io.AbstractFixedByteArrayBuffer;

/**
 * Mock object for {@link INodeData}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MockNodeData extends AbstractMockNodeData implements INodeData {

    /**
     * The #of tuples spanned by this node.
     */
    private long spannedTupleCount;

    private final long[] childAddr;

    /**
     * The #of tuples spanned by each child of this node.
     */
    private final long[] childEntryCount;

    private final boolean hasVersionTimestamps;

    private final long minVersionTimestamp;

    private final long maxVersionTimestamp;
    
    final public long getSpannedTupleCount() {

        return spannedTupleCount;

    }

    /**
     * Bounds check.
     * 
     * @throws IndexOutOfBoundsException
     *             if <i>index</i> is LT ZERO (0)
     * @throws IndexOutOfBoundsException
     *             if <i>index</i> is GE <i>nkeys</i>
     */
    protected boolean assertChildIndex(final int index) {
        
        if (index < 0 || index > getKeys().size())
            throw new IndexOutOfBoundsException();
        
        return true;
        
    }

    final public long getChildAddr(final int index) {

        assertChildIndex(index);
        
        return childAddr[index];
        
    }

    final public long getChildEntryCount(final int index) {

        assertChildIndex(index);
        
        return childEntryCount[index];
        
    }

    final public int getChildCount() {

        return getKeyCount() + 1;

    }

    final public boolean isLeaf() {

        return false;

    }

    final public boolean isReadOnly() {
        
        return true;
        
    }

    /**
     * No.
     */
    final public boolean isCoded() {
        
        return false;
        
    }
    
    final public AbstractFixedByteArrayBuffer data() {
        
        throw new UnsupportedOperationException();
        
    }

    public MockNodeData(final IRaba keys, final long spannedTupleCount,
            final long[] childAddr, final long[] childEntryCount,
            boolean hasVersionTimestamps, long minVersionTimestamp,
            long maxVersionTimestamp) {

        super(keys);

        assert spannedTupleCount >= keys.size();

        assert childAddr != null;
        
        assert childEntryCount != null;

        assert keys.capacity() + 1 == childAddr.length : "keys.capacity="
                + keys.capacity() + ", childAddr.length=" + childAddr.length;
        
        assert keys.capacity() + 1 == childEntryCount.length : "keys.capacity="
                + keys.capacity() + ", childEntryCount.length="
                + childEntryCount.length;

        this.spannedTupleCount = spannedTupleCount;

        this.childAddr = childAddr;

        this.childEntryCount = childEntryCount;

        this.hasVersionTimestamps = hasVersionTimestamps;
        
        this.minVersionTimestamp = minVersionTimestamp;
        
        this.maxVersionTimestamp = maxVersionTimestamp;

    }

    final public boolean hasVersionTimestamps() {
        
        return hasVersionTimestamps;
        
    }

    final public long getMaximumVersionTimestamp() {
        
        if (!hasVersionTimestamps())
            throw new UnsupportedOperationException();
        
        return maxVersionTimestamp;
        
    }

    final public long getMinimumVersionTimestamp() {
        
        if (!hasVersionTimestamps())
            throw new UnsupportedOperationException();
        
        return minVersionTimestamp;
        
    }

}
