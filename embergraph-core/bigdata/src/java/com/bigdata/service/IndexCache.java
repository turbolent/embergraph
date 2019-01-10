package com.bigdata.service;

import com.bigdata.mdi.IMetadataIndex;
import com.bigdata.service.ndx.ClientIndexView;
import com.bigdata.service.ndx.IClientIndex;
import com.bigdata.service.ndx.IScaleOutClientIndex;

/**
 * Concrete implementation for {@link IClientIndex} views.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T>
 */
public class IndexCache extends AbstractIndexCache<IScaleOutClientIndex>{
    
    private final AbstractScaleOutFederation<?> fed;
    
    public IndexCache(final AbstractScaleOutFederation<?> fed, final int capacity,
            final long timeout) {

        super(capacity, timeout);

        if (fed == null)
            throw new IllegalArgumentException();
        
        this.fed = fed;

    }
    
    @Override
    protected ClientIndexView newView(final String name, final long timestamp) {
        
        final IMetadataIndex mdi = fed.getMetadataIndex(name, timestamp);

        // No such index.
        if (mdi == null) {

            if (log.isInfoEnabled())
                log.info("name=" + name + " @ " + timestamp
                        + " : is not registered");

            return null;

        }

        // Index exists.
        return new ClientIndexView(fed, name, timestamp, mdi);
        
    }
 
    protected void dropIndexFromCache(final String name) {

        // drop the index from the cache.
        super.dropIndexFromCache(name);
        
        // and drop the metadata index from its cache as well.
        fed.getMetadataIndexCache().dropIndexFromCache(name);
        
    }
    
}
