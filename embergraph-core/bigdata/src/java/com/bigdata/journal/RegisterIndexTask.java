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
package com.bigdata.journal;

import java.util.UUID;

import com.bigdata.btree.IIndex;
import com.bigdata.btree.IndexMetadata;
import com.bigdata.mdi.IResourceMetadata;
import com.bigdata.mdi.LocalPartitionMetadata;
import com.bigdata.service.IDataService;

/**
 * Register a named index (unisolated write operation).
 * <p>
 * Note: The return value of {@link #doTask()} is the {@link UUID} of the named
 * index. You can test this value to determine whether the index was created
 * based on the supplied {@link IIndex} object or whether the index was
 * pre-existing at the time that this operation was executed.
 * <p>
 * Note: the registered index will NOT be visible to unisolated readers or
 * isolated operations until the next commit. However, unisolated writers that
 * execute after the index has been registered will be able to see the
 * registered index.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * 
 * @see DropIndexTask
 */
public class RegisterIndexTask extends AbstractTask<UUID> {

    final private IndexMetadata metadata;
    
    /**
     * @param journal
     * @param name
     *            The name under which to register the index.
     * @param metadata
     *            The index metadata.
     *            <p>
     *            The {@link LocalPartitionMetadata#getResources()} property on
     *            the {@link IndexMetadata#getPartitionMetadata()} SHOULD NOT be
     *            set. The correct {@link IResourceMetadata}[] will be assigned
     *            when the index is registered on the {@link IDataService}.
     */
    public RegisterIndexTask(IConcurrencyManager concurrencyManager,
            String name, IndexMetadata metadata) {

        super(concurrencyManager, ITx.UNISOLATED, name);

        if (metadata == null)
            throw new NullPointerException();
        
        this.metadata = metadata;
        
    }

    /**
     * Create the named index if it does not exist.
     * 
     * @return The {@link UUID} of the named index.
     */
    @Override
    protected UUID doTask() throws Exception {

        final String name = getOnlyResource();

        IIndex ndx = getJournal().getIndex(name);
            
        if (ndx != null) {

            final UUID indexUUID = ndx.getIndexMetadata().getIndexUUID();

            if (log.isInfoEnabled())
                log.info("Index exists: name=" + name + ", indexUUID=" + indexUUID);

            return indexUUID;

        }

        // register the index.
        ndx = getJournal().registerIndex(name, metadata);

        final UUID indexUUID = ndx.getIndexMetadata().getIndexUUID();

        if (log.isInfoEnabled())
            log.info("Registered index: name=" + name + ", class="
                    + ndx.getClass() + ", indexUUID=" + indexUUID);

        return indexUUID;
        
    }

}
