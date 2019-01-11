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
 * Created on Sep 13, 2008
 */

package org.embergraph.service;

import java.util.Properties;

import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.PartitionLocator;

/**
 * Client class for {@link AbstractScaleOutFederation}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T>
 *            The generic type of the client or service.
 */
public abstract class AbstractScaleOutClient<T> extends AbstractClient<T> {

    /**
     * @param properties
     */
    public AbstractScaleOutClient(final Properties properties) {
        
        super(properties);
        
    }

    /**
     * Options understood by the {@link AbstractScaleOutClient}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public interface Options extends IBigdataClient.Options {
        
        /**
         * Option selects the policy that will be used to cache
         * {@link PartitionLocator}s from an {@link IMetadataIndex} on the
         * client.
         * 
         * <strong>Warning: This feature is expected to evolve.</strong>
         */
        String METADATA_INDEX_CACHE_POLICY = IBigdataClient.class.getName()
                + ".metadataIndex.cachePolicy";

        String DEFAULT_METADATA_INDEX_CACHE_POLICY = MetadataIndexCachePolicy.CacheAll
                .toString();
        
    }

    /**
     * Policy options for caching {@link PartitionLocator}s for an
     * {@link IMetadataIndex}.
     * 
     * <strong>Warning: This feature is expected to evolve.</strong>
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static enum MetadataIndexCachePolicy {
        
        /**
         * Cache the entire {@link IMetadataIndex}.
         */
        CacheAll,
        
        /**
         * Do not cache anything.
         */
        NoCache;
        
    }
    
}
