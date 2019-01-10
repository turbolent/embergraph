package org.embergraph.bop.fed.shards;

import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.service.ndx.AbstractSplitter;

/**
 * Helper class efficiently splits an array of sorted keys into groups
 * associated with a specific index partition.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan
 *         Thompson</a>
 */
class Splitter extends AbstractSplitter {
    
    private final IMetadataIndex mdi;
    
    public Splitter(final IMetadataIndex mdi) {

        if (mdi == null)
            throw new IllegalArgumentException();
        
        this.mdi = mdi;

    }
    
    @Override
    protected IMetadataIndex getMetadataIndex(long ts) {
        
        return mdi;
        
    }
    
}