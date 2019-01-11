/**

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
 * Created on Feb 9, 2007
 */

package org.embergraph.journal;

import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;

import org.embergraph.btree.IndexSegmentBuilder;
import org.embergraph.rawstore.AbstractRawStoreTestCase;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.util.Bytes;

/**
 * Base class for writing test cases for the different {@link IBufferStrategy}
 * implementations.
 * 
 * @todo write tests for
 *       {@link IBufferStrategy#transferTo(java.io.RandomAccessFile)}. This
 *       code is currently getting "checked" by the {@link IndexSegmentBuilder}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractBufferStrategyTestCase extends AbstractRawStoreTestCase {

    public AbstractBufferStrategyTestCase() {
    }

    public AbstractBufferStrategyTestCase(String name) {
        super(name);
    }

    abstract protected BufferMode getBufferMode();
    
    public Properties getProperties() {

        if (properties == null) {

            properties = super.getProperties();

            properties.setProperty(Options.BUFFER_MODE, getBufferMode()
                    .toString());

            /*
             * Use a temporary file for the test. Such files are always deleted when
             * the journal is closed or the VM exits.
             */
            properties.setProperty(Options.CREATE_TEMP_FILE,"true");
            properties.setProperty(Options.DELETE_ON_EXIT,"true");

        }
        
        return properties;

    }
    
    private Properties properties;

    protected IRawStore getStore() {
        
        return new Journal(getProperties());
        
    }

//    public void tearDown() throws Exception
//    {
//
//        super.tearDown();
//        
//        if(properties==null) return;
//        
//        String filename = properties.getProperty(Options.FILE);
//        
//        if(filename==null) return;
//        
//        File file = new File(filename);
//        
//        if(file.exists() && !file.delete()) {
//            
//            file.deleteOnExit();
//            
//        }
//        
//    }
     
    /**
     * Unit test for {@link AbstractBufferStrategy#overflow(long)}. The test
     * verifies that the extent and the user extent are correctly updated after
     * an overflow.
     */
    public void test_overflow() {
        
    	final Journal store = (Journal) getStore();
        
        try {
        
            if (!(store.getBufferStrategy() instanceof AbstractBufferStrategy))
                return;
        
        final AbstractBufferStrategy bufferStrategy = (AbstractBufferStrategy) store
            .getBufferStrategy();

        final long userExtent = bufferStrategy.getUserExtent();
        
        final long extent = bufferStrategy.getExtent();
        
        final long initialExtent = bufferStrategy.getInitialExtent();
        
        final long nextOffset = bufferStrategy.getNextOffset();
        
        assertEquals("extent",initialExtent, extent);
        
        final long needed = Bytes.kilobyte32;
        
        if(bufferStrategy.getBufferMode()==BufferMode.Mapped) {

            // operation is not supported for mapped files.
            try {
                
                bufferStrategy.overflow(needed);
                
                fail("Expecting: " + UnsupportedOperationException.class);
                
            } catch (UnsupportedOperationException ex) {
                
                System.err.println("Ignoring expected exception: " + ex);
                
            }
            
        } else {

            assertTrue("overflow()",bufferStrategy.overflow(needed));
            
            assertTrue("extent", extent + needed <= bufferStrategy
                    .getExtent());
            
            assertTrue("userExtent", userExtent + needed <= bufferStrategy
                    .getUserExtent());
            
            assertEquals(nextOffset,bufferStrategy.getNextOffset());

        }

        } finally {

            store.destroy();
            
        }
        
    }

    /**
     * Test verifies that a write up to the remaining extent does not trigger
     * an overflow.
     */
    public void test_writeNoExtend() {

        final Journal store = (Journal) getStore();

        try {

            final IBufferStrategy bufferStrategy = store.getBufferStrategy();

            if (bufferStrategy.getBufferMode() == BufferMode.DiskRW) {
                return;
            }

            final long userExtent = bufferStrategy.getUserExtent();

            final long extent = bufferStrategy.getExtent();

            final long initialExtent = bufferStrategy.getInitialExtent();

            final long nextOffset = bufferStrategy.getNextOffset();

            assertEquals("extent", initialExtent, extent);

            final long remaining = userExtent - nextOffset;

            writeRandomData(store, remaining, bufferStrategy.useChecksums());

            // no change in extent.
            assertEquals("extent", extent, bufferStrategy.getExtent());
            
            // no change in user extent.
            assertEquals("userExtent", userExtent, bufferStrategy
                    .getUserExtent());

        } finally {

            store.destroy();

        }

    }
    
    /**
     * Write random bytes on the store.
     * 
     * @param store
     *            The store.
     * 
     * @param nbytesToWrite
     *            The #of bytes to be written. If this is larger than the
     *            maximum record length then multiple records will be written.
     * 
     * @return The address of the last record written.
     */
    protected long writeRandomData(final Journal store, final long nbytesToWrite, final boolean allowChecksum) {

        final int maxRecordSize = store.getMaxRecordSize();
        
        final int chkAdjust = allowChecksum ? 4 : 0;
        
        assert nbytesToWrite > 0;
        
        long addr = 0L;
        
        AbstractBufferStrategy bufferStrategy = (AbstractBufferStrategy) store
                .getBufferStrategy();
        
        int n = 0;

        long leftover = nbytesToWrite;
        
        while (leftover > 0) {

            // this will be an int since maxRecordSize is an int.
            final int nbytes = (int) Math.min(maxRecordSize, leftover-chkAdjust);

            assert nbytes>0;
            
            final byte[] b = new byte[nbytes];

            final Random r = new Random();

            r.nextBytes(b);

            final ByteBuffer tmp = ByteBuffer.wrap(b);

            addr = bufferStrategy.write(tmp);

            n++;
            
            leftover -= nbytes+chkAdjust;
            
            System.err.println("Wrote record#" + n + " with " + nbytes
                    + " bytes: addr=" + store.toString(addr) + ", #leftover="
                    + leftover);

        }

        System.err.println("Wrote " + nbytesToWrite + " bytes in " + n
                + " records: last addr=" + store.toString(addr));

        assert addr != 0L;
        
        return addr;

    }
    
    /**
     * Test verifies that a write over the remaining extent triggers an
     * overflow. The test also makes sure that the existing data is recoverable
     * and that the new data is also recoverable (when the buffer is extended it
     * is typically copied while the length of a file is simply changed).
     */
    public void test_writeWithExtend() {

        final Journal store = (Journal) getStore();

        try {

            final IBufferStrategy bufferStrategy = store.getBufferStrategy();

            final BufferMode bm = bufferStrategy.getBufferMode();
            
            if (bm == BufferMode.DiskRW || bm == BufferMode.Mapped) {
            
                return;

            }

            final long userExtent = bufferStrategy.getUserExtent();

            final long extent = bufferStrategy.getExtent();

            final long initialExtent = bufferStrategy.getInitialExtent();

            final long nextOffset = bufferStrategy.getNextOffset();

            assertEquals("extent", initialExtent, extent);

            final long remaining = userExtent - nextOffset;

            final long addr = writeRandomData(store, remaining, bufferStrategy.useChecksums());

            // no change in extent.
            assertEquals("extent", extent, bufferStrategy.getExtent());

            // no change in user extent.
            assertEquals("userExtent", userExtent, bufferStrategy
                    .getUserExtent());

            // read back the last record of random data written on the store.
            final ByteBuffer b = bufferStrategy.read(addr);

            /*
             * now write some more random bytes forcing an extension of the
             * buffer. we verify both the original write on the buffer and the
             * new write. this helps to ensure that data was copied correctly
             * into the extended buffer.
             */

            final byte[] b2 = new byte[Bytes.kilobyte32];

//            new Random().
            r.nextBytes(b2);

            final ByteBuffer tmp2 = ByteBuffer.wrap(b2);

            final long addr2 = store.write(tmp2);
            // final long addr2 = writeRandomData(store,Bytes.kilobyte32);

            /*
             * Note: The DiskOnly strategy has a write cache. You have to force
             * it to disk before the new extent will show up.
             */
            bufferStrategy.force(false);

            // verify extension of buffer.
            assertTrue("extent",
                    extent + store.getByteCount(addr2) <= bufferStrategy
                            .getExtent());

            // verify extension of buffer.
            assertTrue("userExtent",
                    userExtent + store.getByteCount(addr2) <= bufferStrategy
                            .getUserExtent());

            // verify data written before we overflowed the buffer.
            assertEquals(b, bufferStrategy.read(addr));

            // verify data written after we overflowed the buffer.
            assertEquals(b2, bufferStrategy.read(addr2));

        } finally {

            store.destroy();

        }

    }
    
}
