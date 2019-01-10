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
 * Created on Feb 19, 2008
 */

package org.embergraph.service;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.embergraph.bfs.BigdataFileSystem.Options;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.keys.TestKeyBuilder;
import org.embergraph.btree.proc.BatchInsert.BatchInsertConstructor;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;

/**
 * Test suite for the ability to re-open an {@link EmbeddedFederation}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRestartSafe extends AbstractEmbeddedFederationTestCase {

    /**
     * 
     */
    public TestRestartSafe() {
    }

    /**
     * @param arg0
     */
    public TestRestartSafe(String arg0) {
        super(arg0);
    }

    /**
     * Overriden to specify the {@link BufferMode#Disk} mode.
     */
    public Properties getProperties() {
        
        final Properties properties = new Properties(super.getProperties());

        properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

        return properties;

    }
    
    /**
     * Test creates a new embedded federation (this is done in setUp() by the
     * super class), registers a scale-out index with the metadata service
     * having an index partition on each of the data services and then writes
     * some data such that there is data for that scale-out index on each of the
     * data services. The federation is then closed and a new instance of the
     * federation is opened and we verify that the metadata and data services
     * were discovered, that the index is still registered, and that the data is
     * still in the index. We also verify that the next partition number
     * assigned to the metadata index is strictly ascending after restart.
     * 
     * @throws IOException
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void test_restartSafe() throws IOException, InterruptedException, ExecutionException {

        /*
         * Verify the #of data services and note the UUID for the data and
         * metadata services.
         */
        assertEquals("#dataServices", 2,
                ((EmbeddedFederation) fed).getDataServiceCount());
        
        final UUID metadataServiceUUID = fed.getMetadataService()
                .getServiceUUID();

        final UUID dataService0UUID = ((EmbeddedFederation) fed)
                .getDataService(0).getServiceUUID();
        
        final UUID dataService1UUID = ((EmbeddedFederation) fed)
                .getDataService(1).getServiceUUID();

        /*
         * Register a scale-out index with data on each of the data services.
         */
        
        final String name = "testIndex";
        
        final IndexMetadata metadata = new IndexMetadata(name,UUID.randomUUID());

        metadata.setDeleteMarkers(true);
        
        final UUID indexUUID = fed.registerIndex(metadata, new byte[][]{//
                new byte[]{},
                new byte[]{5}
        }, new UUID[]{//
                dataService0.getServiceUUID(),
                dataService1.getServiceUUID() });

        /*
         * Setup the data to write on the scale-out index. The keys are choosen
         * so that 1/2 of the data will show up on each of the data services.
         */

        final int nentries = 10;

        final byte[][] keys = new byte[nentries][];
        final byte[][] vals = new byte[nentries][];

        for (int i = 0; i < nentries; i++) {

            keys[i] = TestKeyBuilder.asSortKey(i);

            vals[i] = new byte[4];

            r.nextBytes(vals[i]);

        }

        /*
         * Write data on the index.
         */
        {

            IIndex ndx = fed.getIndex(name,ITx.UNISOLATED);

            ndx.submit(0/*fromIndex*/,nentries/*toIndex*/, keys, vals,
                    BatchInsertConstructor.RETURN_NO_VALUES, null/*handler*/);

        }

        /*
         * Verify read-back of the data on the index.
         */
        {

            IIndex ndx = fed.getIndex(name,ITx.UNISOLATED);

            assertEquals(nentries, ndx.rangeCount(null, null));

            ITupleIterator itr = ndx.rangeIterator(null, null);
            
            int i = 0;
            
            while(itr.hasNext()) {
                
                ITuple tuple = itr.next();
                
                assertEquals(keys[i],tuple.getKey());

                assertEquals(vals[i],tuple.getValue());
                
                i++;
                
            }
            
            assertEquals(nentries, i);
            
        }

        // Have an index partition identifier assigned before restart.
        final int nextPartitionId0 = metadataService.nextPartitionId(name);
        final int nextPartitionId1 = metadataService.nextPartitionId(name);
        assertEquals("nextPartitionId",nextPartitionId0+1,nextPartitionId1);
        
        /*
         * Close down the embedded federation.
         * 
         * See setUp() in the parent class.
         */
        
        client.disconnect(true/*immediateShutdown*/);
        
        client = null;
        
        fed = null;
        
        dataService0 = null;
        
        dataService1 = null;
        
        metadataService = null;

        /*
         * Open the embedded federation again.
         *
         * See setUp() in the parent class.
         */
        
        client = new EmbeddedClient(getProperties());
        
        fed = client.connect();

        assertEquals("#dataServices", 2,
                ((EmbeddedFederation) fed).ndataServices);
        
        dataService0 = ((EmbeddedFederation)fed).getDataService(0);

        dataService1 = ((EmbeddedFederation)fed).getDataService(1);

        metadataService = fed.getMetadataService();

        /*
         * Verify the data and metadata service UUIDs.
         * 
         * Note: there is only one metadata service so there is no uncertainty
         * about which UUID it must have.
         * 
         * However, there are two data services. On restart either data service
         * could be assigned to either index ZERO or ONE in the dataServices[].
         * Therefore we check to make sure that the service UUIDs for the data
         * services are (a) distinct; and (b) each one is the service UUID for
         * one of the expected data services.
         */
        
        assertEquals("metadataService UUID", metadataServiceUUID, fed
                .getMetadataService().getServiceUUID());

        // verify services have distinct UUIDs.
        assertNotSame(dataService0.getServiceUUID(), dataService1
                .getServiceUUID());
        
        // verify dataService[0] has one of the expected data service UUIDs.
        if (!dataService0.getServiceUUID().equals(dataService0UUID)
                && !dataService0.getServiceUUID().equals(dataService1UUID)) {

            fail("Not expecting data service with UUID: "
                    + dataService0.getServiceUUID());

        }
        
        // verify dataService[1] has one of the expected data service UUIDs.
        if (!dataService1.getServiceUUID().equals(dataService0UUID)
                && !dataService1.getServiceUUID().equals(dataService1UUID)) {

            fail("Not expecting data service with UUID: "
                    + dataService1.getServiceUUID());

        }

        /*
         * Verify the scale-out index is registered.
         */
        
        assertNotNull(fed.getIndex(name,ITx.UNISOLATED));

        assertEquals(indexUUID, fed.getIndex(name,ITx.UNISOLATED)
                .getIndexMetadata().getIndexUUID());
        
        /*
         * Verify the next partition identifier assigned.
         */
        assertEquals("nextPartitionId", nextPartitionId1 + 1, metadataService
                .nextPartitionId(name));
        
        /*
         * Verify read-back of the data on the index.
         */
        {

            final IIndex ndx = fed.getIndex(name,ITx.UNISOLATED);

            assertEquals(nentries, ndx.rangeCount());

            final ITupleIterator itr = ndx.rangeIterator();
            
            int i = 0;
            
            while(itr.hasNext()) {
                
                final ITuple tuple = itr.next();
                
                assertEquals(keys[i],tuple.getKey());

                assertEquals(vals[i],tuple.getValue());
                
                i++;
                
            }
            
            assertEquals(nentries, i);
            
        }

    }

}
