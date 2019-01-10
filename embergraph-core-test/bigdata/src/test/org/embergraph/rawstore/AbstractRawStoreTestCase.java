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
 * Created on Jan 31, 2007
 */

package org.embergraph.rawstore;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.bigdata.io.TestCase3;
import com.bigdata.journal.Journal;
import com.bigdata.journal.WORMStrategy;

/**
 * Base class for writing tests of the {@link IRawStore} interface.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractRawStoreTestCase extends TestCase3 {

    /**
     * 
     */
    public AbstractRawStoreTestCase() {
    }

    /**
     * @param name
     */
    public AbstractRawStoreTestCase(String name) {
        super(name);
    }

    /**
     * Return a new store that will serve as the fixture for the test. A stable
     * store must remove the pre-existing store. A transient store should open a
     * new store every time.
     * 
     * @return The fixture for the test.
     */
    abstract protected IRawStore getStore();

    /**
     * Test verifies correct rejection of a write operation when the caller
     * supplies an empty buffer (no bytes remaining).
     */
    public void test_write_correctRejection_emptyRecord() {
        
        final IRawStore store = getStore();

        try {

            try {

                store.write(ByteBuffer.wrap(new byte[] {}));

                fail("Expecting: " + IllegalArgumentException.class);

            } catch (IllegalArgumentException ex) {

                if (log.isInfoEnabled())
                    log.info("Ignoring expected exception: " + ex);

            }

            try {

                final ByteBuffer buf = ByteBuffer.wrap(new byte[2]);

                // advance the position to the limit so that no bytes remain.
                buf.position(buf.limit());

                store.write(buf);

                fail("Expecting: " + IllegalArgumentException.class);

            } catch (IllegalArgumentException ex) {

                if (log.isInfoEnabled())
                    log.info("Ignoring expected exception: " + ex);

            }

        } finally {

            store.destroy();

        }

    }

    /**
     * Test verifies correct rejection of a write operation when the caller
     * supplies a [null] buffer.
     */
    public void test_write_correctRejection_null() {
        
        final IRawStore store = getStore();
        
        try {
        
        try {

            store.write( null );
            
            fail("Expecting: "+IllegalArgumentException.class);
                
        } catch(IllegalArgumentException ex) {
            
            System.err.println("Ignoring expected exception: "+ex);
            
        }   
        
        } finally {
        
        store.destroy();
        
        }

    }
    
    /**
     * A read with a 0L address is always an error.
     */
    public void test_read_correctRejection_0L() {
        
        final IRawStore store = getStore();
        
        try {

        try {

            store.read( 0L );
            
            fail("Expecting: "+IllegalArgumentException.class);
                
        } catch(IllegalArgumentException ex) {
            
            System.err.println("Ignoring expected exception: "+ex);
            
        }   

        } finally {

            store.destroy();
            
        }
            
    }
    
    /**
     * A delete with an address encoding a zero length component is an error
     * (the address is ill-formed since we do not allow writes of zero length
     * records).
     */
    public void test_read_correctRejection_zeroLength() {
        
        final IRawStore store = getStore();

        try {

            final int nbytes = 0;
            
            final int offset = 10;
            
            store.read( store.toAddr(nbytes, offset) );
            
            fail("Expecting: "+IllegalArgumentException.class);
                
        } catch(IllegalArgumentException ex) {
            
            System.err.println("Ignoring expected exception: "+ex);
            
        } finally {
        
            store.destroy();
        
        }

    }

//    /**
//     * A read with a well-formed address that was never written is an error.
//     * 
//     * @todo Support for detecting this is not present in the WORM store. We
//     *       could detect an address beyond the end of the store, but that is
//     *       about it. In contrast, a RW store using an indirection table to
//     *       translate logical to physical addresses is able to "know" if an
//     *       address is valid.
//     */
//    public void test_read_correctRejection_neverWritten() {
//   
//        // @todo this test disabled until a RW store is implemented.
//        
////        final IRawStore store = getStore();
////
////        try {
////
////            final int nbytes = 100;
////            
////            final int offset = 0;
////            
////            store.read( store.toAddr(nbytes, offset) );
////            
////            fail("Expecting: "+IllegalArgumentException.class);
////                
////        } catch(IllegalArgumentException ex) {
////            
////            System.err.println("Ignoring expected exception: "+ex);
////            
////        } finally {
////
////        store.destroy();
////        
////    }
//
//    }
    
//    
//    /**
//     * A delete with a 0L address is always an error.
//     */
//    public void test_delete_correctRejection_0L() {
//        
//        final IRawStore store = getStore();
//
//        try {
//
//            store.delete( 0L );
//            
//            fail("Expecting: "+IllegalArgumentException.class);
//                
//        } catch(IllegalArgumentException ex) {
//            
//            System.err.println("Ignoring expected exception: "+ex);
//            
//        } finally {
//    store.destroy();
//}
//                
//    }
//    
//    /**
//     * A delete with an address encoding a zero length component is an error
//     * (the address is ill-formed since we do not allow writes of zero length
//     * records).
//     */
//    public void test_delete_correctRejection_zeroLength() {
//        
//        final IRawStore store = getStore();
//
//        try {
//
//            final int nbytes = 0;
//            
//            final int offset = 10;
//            
//            store.delete( Addr.toLong(nbytes, offset));
//            
//            fail("Expecting: "+IllegalArgumentException.class);
//                
//        } catch(IllegalArgumentException ex) {
//            
//            System.err.println("Ignoring expected exception: "+ex);
//            
//        } finally {
//    store.destroy();
//}
//                
//    }
//    
//    /**
//     * A delete with a well-formed address that was never written is an error.
//     */
//    public void test_delete_correctRejection_neverWritten() {
//        
//        final IRawStore store = getStore();
//
//        try {
//
//            final int nbytes = 100;
//            
//            final int offset = 0;
//            
//            store.delete( Addr.toLong(nbytes, offset) );
//            
//            fail("Expecting: "+IllegalArgumentException.class);
//                
//        } catch(IllegalArgumentException ex) {
//            
//            System.err.println("Ignoring expected exception: "+ex);
//            
//        } finally {
//    store.destroy();
//    }
//                
//    }
    
    /**
     * Test verifies that we can write and then read back a record.
     */
    public void test_writeRead() {
        
        final IRawStore store = getStore();
        
        try {
        
//        final Random r = new Random();
        
        final int len = 100;
        
        final byte[] expected = new byte[len];
        
        r.nextBytes(expected);
        
        final ByteBuffer tmp = ByteBuffer.wrap(expected);
        
        final long addr1 = store.write(tmp);

        // verify that the position is advanced to the limit.
        assertEquals(len,tmp.position());
        assertEquals(tmp.position(),tmp.limit());

        // read the data back.
        final ByteBuffer actual = store.read(addr1);
        
        assertEquals(expected,actual);
        
        /*
         * verify the position and limit after the read.
         */
        assertEquals(0,actual.position());
        assertEquals(expected.length,actual.limit());
        /*
         * Note: Assertion is violated when cache is disabled and checksums
         * are in use because the returned buffer may be a slice on a larger
         * buffer which also includes the trailing checksum field.
         */
//        assertEquals(actual.limit(),actual.capacity());
        
        } finally {

            store.destroy();
            
        }

    }

    /**
     * Test verifies that we can write and then read back a record twice.
     */
    public void test_writeReadRead() {
        
        final IRawStore store = getStore();
        
        try {
        
//        final Random r = new Random();
        
        final int len = 100;
        
        final byte[] expected = new byte[len];
        
        r.nextBytes(expected);
        
        final ByteBuffer tmp = ByteBuffer.wrap(expected);
        
        final long addr1 = store.write(tmp);

        // verify that the position is advanced to the limit.
        assertEquals(len, tmp.position());
        assertEquals(tmp.position(), tmp.limit());

        /*
         * 1st read.
         */
        {
            // read the data back.
            final ByteBuffer actual = store.read(addr1);

            assertEquals(expected, actual);

            /*
             * verify the position and limit after the read.
             */
            assertEquals(0, actual.position());
            assertEquals(expected.length, actual.limit());
        }

        /*
         * 2nd read.
         */
        {
            // read the data back.
            final ByteBuffer actual2 = store.read(addr1);

            assertEquals(expected, actual2);

            /*
             * verify the position and limit after the read.
             */
            assertEquals(0, actual2.position());
            assertEquals(expected.length, actual2.limit());
        }
    
        } finally {

            store.destroy();
            
        }

    }

//    /**
//     * Test verifies read behavior when the offered buffer has exactly the
//     * required #of bytes of remaining.
//     */
//    public void test_writeReadWith2ndBuffer_exactCapacity() {
//        
//        final IRawStore store = getStore();
//        try {
//        //Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len);
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // verify the data are record correct.
//        assertEquals(expected1,actual);
//
//        /*
//         * the caller's buffer MUST be used since it has sufficient bytes
//         * remaining
//         */
//        assertTrue("Caller's buffer was not used.", actual==buf);
//
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len,actual.limit());
//        } finally {store.destroy();}
//    }
//    
//    public void test_writeReadWith2ndBuffer_excessCapacity_zeroPosition() {
//        
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len+1);
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // verify the data are record correct.
//        assertEquals(expected1,actual);
//
//        /*
//         * the caller's buffer MUST be used since it has sufficient bytes
//         * remaining
//         */
//        assertTrue("Caller's buffer was not used.", actual==buf);
//
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len,actual.limit());
//        } finally {store.destroy();}
//    }
//    
//    public void test_writeReadWith2ndBuffer_excessCapacity_nonZeroPosition() {
//        
//        final IRawStore store = getStore();
//        try {        
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len+2);
//        buf.position(1); // advance the position by one byte.
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // copy the expected data leaving the first byte zero.
//        byte[] expected2 = new byte[len+1];
//        System.arraycopy(expected1, 0, expected2, 1, expected1.length);
//        
//        // verify the data are record correct.
//        assertEquals(expected2,actual);
//
//        /*
//         * the caller's buffer MUST be used since it has sufficient bytes
//         * remaining
//         */
//        assertTrue("Caller's buffer was not used.", actual==buf);
//
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len+1,actual.limit());
//
//  } finally {store.destroy();}
//    }
//    
//    /**
//     * Test verifies read behavior when the offered buffer does not have
//     * sufficient remaining capacity.
//     */
//    public void test_writeReadWith2ndBuffer_wouldUnderflow_nonZeroPosition() {
//    
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer that is large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len);
//        buf.position(1); // but advance the position so that there is not enough room.
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // verify the data are record correct.
//        assertEquals(expected1,actual);
//
//        /*
//         * the caller's buffer MUST NOT be used since it does not have
//         * sufficient bytes remaining.
//         */
//        assertFalse("Caller's buffer was used.", actual==buf);
//        
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len,actual.limit());
//        
//  } finally {store.destroy();}
//    }
//
//    /**
//     * Test verifies read behavior when the offered buffer does not have
//     * sufficient remaining capacity.
//     */
//    public void test_writeReadWith2ndBuffer_wouldUnderflow_zeroPosition() {
//    
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer that is not large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len-1);
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // verify the data are record correct.
//        assertEquals(expected1,actual);
//
//        /*
//         * the caller's buffer MUST NOT be used since it does not have
//         * sufficient bytes remaining.
//         */
//        assertFalse("Caller's buffer was used.", actual==buf);
//        
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len,actual.limit());
//        
//  } finally {store.destroy();}
//    }
//
//    /**
//     * Test verifies that an oversized buffer provided to
//     * {@link IRawStore#read(long, ByteBuffer)} will not cause more bytes to be
//     * read than are indicated by the {@link Addr address}.
//     */
//    public void test_writeReadWith2ndBuffer_wouldOverflow_zeroPosition() {
//    
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer that is more than large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len+1);
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // verify the data are record correct - only [len] bytes should be copied.
//        assertEquals(expected1,actual);
//
//        /*
//         * the caller's buffer MUST be used since it has sufficient bytes
//         * remaining.
//         */
//        assertTrue("Caller's buffer was used.", actual==buf);
//        
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len,actual.limit());
//        
//  } finally {store.destroy();}
//    }
//
//    /**
//     * Test verifies that an oversized buffer provided to
//     * {@link IRawStore#read(long, ByteBuffer)} will not cause more bytes to be
//     * read than are indicated by the {@link Addr address}.
//     */
//    public void test_writeReadWith2ndBuffer_wouldOverflow_nonZeroPosition() {
//    
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        // a buffer that is more than large enough to hold the record.
//        ByteBuffer buf = ByteBuffer.allocate(len+2);
//        
//        // non-zero position.
//        buf.position(1);
//
//        // read the data, offering our buffer.
//        ByteBuffer actual = store.read(addr1, buf);
//        
//        // copy the expected data leaving the first byte zero.
//        byte[] expected2 = new byte[len+1];
//        System.arraycopy(expected1, 0, expected2, 1, expected1.length);
//
//        // verify the data are record correct - only [len] bytes should be copied.
//        assertEquals(expected2,actual);
//
//        /*
//         * the caller's buffer MUST be used since it has sufficient bytes
//         * remaining.
//         */
//        assertTrue("Caller's buffer was used.", actual==buf);
//        
//        /*
//         * verify the position and limit after the read.
//         */
//        assertEquals(0,actual.position());
//        assertEquals(len+1,actual.limit());
//  } finally {store.destroy();}
//    }
//
    /**
     * Test verifies that write does not permit changes to the store state by
     * modifying the supplied buffer after the write operation (i.e., a copy
     * is made of the data in the buffer).
     */
    public void test_writeImmutable() {

        final IRawStore store = getStore();
        
        try {
            
//        final Random r = new Random();
        
        final int len = 100;
        
        final byte[] expected1 = new byte[len];
        
        r.nextBytes(expected1);

        // write
        final ByteBuffer tmp = ByteBuffer.wrap(expected1);
        
        final long addr1 = store.write(tmp);

        // verify that the position is advanced to the limit.
        assertEquals(len,tmp.position());
        assertEquals(tmp.position(),tmp.limit());

        // verify read.
        assertEquals(expected1,store.read(addr1));

        // clone the data.
        final byte[] expected2 = expected1.clone();
        
        // modify the original data.
        r.nextBytes(expected1);

        /*
         * verify read - this will fail if the original data was not copied by
         * the store.
         */
        assertEquals(expected2,store.read(addr1));

        } finally {

            store.destroy();
            
        }

    }

    /**
     * Test verifies that read does not permit changes to the store state by
     * modifying the returned buffer.
     */
    public void test_readImmutable() {
       
        final IRawStore store = getStore();
        
        try {
        
//        final Random r = new Random();
        
        final int len = 100;
        
        final byte[] expected1 = new byte[len];
        
        r.nextBytes(expected1);
        
        final ByteBuffer tmp = ByteBuffer.wrap(expected1);
        
        final long addr1 = store.write(tmp);

        // verify that the position is advanced to the limit.
        assertEquals(len,tmp.position());
        assertEquals(tmp.position(),tmp.limit());

        final ByteBuffer actual = store.read(addr1);
        
        assertEquals(expected1,actual);

        /*
         * If [actual] is not read-only then we modify [actual] and verify that
         * the state of the store is not changed.
         */
        if( ! actual.isReadOnly() ) {
            
            // overwrite [actual] with some random data.
            
            final byte[] tmp2 = new byte[100];
            
            r.nextBytes(tmp2);
            
            actual.clear();
            actual.put(tmp2);
            actual.flip();

            // verify no change in store state.
            
            assertEquals(expected1,store.read(addr1));

        }
        
        } finally {
 
            store.destroy();
            
        }

    }
    
    /**
     * Test writes a bunch of records and verifies that each can be read after
     * it is written.  The test then performs a random order read and verifies
     * that each of the records can be read correctly.
     */
    public void test_multipleWrites() {

        final IRawStore store = getStore();

        try {
        
//        final Random r = new Random();

        /*
         * write a bunch of random records.
         */
        final int limit = 100;
        
        final long[] addrs = new long[limit];
        
        final byte[][] records = new byte[limit][];
        
        for(int i=0; i<limit; i++) {

            final byte[] expected = new byte[r.nextInt(100) + 1];
        
            r.nextBytes(expected);
        
            final ByteBuffer tmp = ByteBuffer.wrap(expected);
            
            final long addr = store.write(tmp);

            // verify that the position is advanced to the limit.
            assertEquals(expected.length,tmp.position());
            assertEquals(tmp.position(),tmp.limit());

            assertEquals(expected,store.read(addr));
        
            addrs[i] = addr;
            
            records[i] = expected;
            
        }

        /*
         * now verify data with random reads.
         */

        final int[] order = getRandomOrder(limit);
        
        for(int i=0; i<limit; i++) {
            
            final long addr = addrs[order[i]];
            
            final byte[] expected = records[order[i]];

            assertEquals(expected,store.read(addr));
            
        }
    
        } finally {

            store.destroy();
            
        }

    }
    
    IRawStore ensureStreamStore(final IRawStore store) {
    	return store instanceof Journal ? ((Journal) store).getBufferStrategy() : store;
    }
    
	public void testSimpleStringStream() throws IOException, ClassNotFoundException {
		final IRawStore store = getStore();

		try {

			final IRawStore rawstore = ensureStreamStore(store);
			
			final IPSOutputStream psout = rawstore.getOutputStream();
			final ObjectOutputStream outdat = new ObjectOutputStream(psout);
			
			final String hw = "Hello World";
			
			outdat.writeObject(hw);
			outdat.flush();
			
			final long addr = psout.getAddr();
			
			final InputStream instr = rawstore.getInputStream(addr);
			
			final ObjectInputStream inobj = new ObjectInputStream(instr);
			final String tst = (String) inobj.readObject();
			
			assertTrue(hw.equals(tst));
			
		} finally {

			store.destroy();

		}
		
	}
	
	public void testSimpleStringStreamFromStandardAllocation() throws IOException, ClassNotFoundException {
		final IRawStore store = getStore();

		try {

			final IRawStore strategy = ensureStreamStore(store);
			
			final IPSOutputStream psout = strategy.getOutputStream();
			final ObjectOutputStream outdat = new ObjectOutputStream(psout);
			
			final String hw = "Hello World";
			
			// Now confirm similar with non-stream allocation
			final ByteArrayOutputStream bbout = new ByteArrayOutputStream();
			final ObjectOutputStream outdat2 = new ObjectOutputStream(bbout);
			
			outdat2.writeObject(hw);
			outdat2.flush();
			
			final ByteBuffer bb = ByteBuffer.wrap(bbout.toByteArray());
			final long addr2 = strategy.write(bb);
			
			// see if we can read back as stream after allocating as buffer
			
			final InputStream instr2 = strategy.getInputStream(addr2);
			
			final ObjectInputStream inobj2 = new ObjectInputStream(instr2);
			final String tst2 = (String) inobj2.readObject();
			
			assertTrue(hw.equals(tst2));
			

		} finally {

			store.destroy();

		}
		
	}
	
	public void testEmptyStream() throws IOException, ClassNotFoundException {
		final IRawStore store = getStore();

		try {

			final IRawStore strategy = ensureStreamStore(store);
			
			final IPSOutputStream psout = strategy.getOutputStream();
			
			final long addr = psout.getAddr();
			
			final InputStream instr = strategy.getInputStream(addr);
			
			assertTrue(-1 == instr.read());
		} finally {

			store.destroy();

		}
		
	}

	/**
	 * Writing a blob sized object stream is an excellent test since the ObjectInputStream
	 * will likely throw an exception if there is a data error.
	 */
	public void testBlobObjectStreams() throws IOException, ClassNotFoundException {
		final IRawStore store = getStore();

		try {

			final IRawStore strategy =  ensureStreamStore(store);
			IPSOutputStream psout = strategy.getOutputStream();
						
			ObjectOutputStream outdat = new ObjectOutputStream(psout);
			final String blobBit = "A bit of a blob...";
			
			for (int i = 0; i < 40000; i++)
				outdat.writeObject(blobBit);
			outdat.close();
			
			long addr = psout.getAddr(); // save and retrieve the address
			
			InputStream instr = strategy.getInputStream(addr);
			
			ObjectInputStream inobj = new ObjectInputStream(instr);
			for (int i = 0; i < 40000; i++) {
				try {
					final String tst = (String) inobj.readObject();
				
					assertTrue(blobBit.equals(tst));
				} catch (IOException ioe) {
					System.err.println("Problem at " + i);
					throw ioe;
				}
			}
			
			try {
				inobj.readObject();
				fail("Expected EOFException");
			} catch (EOFException eof) {
				// expected
			} catch (Exception ue) {
				fail("Expected EOFException not this", ue);
			}
		
			// confirm that the stream address can be freed
			strategy.delete(addr);
		} finally {
			store.destroy();
		}
	}

	/**
	 * This test exercises the stream interface and serves as an example of
	 * stream usage for compressed IO.
	 */
	public void testZipStreams() throws IOException, ClassNotFoundException {
		final IRawStore store = getStore();

		try {

			final IRawStore strategy =  ensureStreamStore(store);
			final IPSOutputStream psout = strategy.getOutputStream();
			
			ObjectOutputStream outdat = new ObjectOutputStream(new GZIPOutputStream(psout));
			final String blobBit = "A bit of a blob...";
			
			for (int i = 0; i < 40000; i++)
				outdat.writeObject(blobBit);
			outdat.close();
			
			long addr = psout.getAddr(); // save and retrieve the address
				
			InputStream instr = strategy.getInputStream(addr);
			
			ObjectInputStream inobj = new ObjectInputStream(new GZIPInputStream(instr));
			for (int i = 0; i < 40000; i++) {
				try {
					final String tst = (String) inobj.readObject();
				
					assertTrue(blobBit.equals(tst));
				} catch (IOException ioe) {
					fail("Problem at " + i, ioe);
				}
			}
			
			strategy.delete(addr);
		
		} finally {
			store.destroy();
		}
	}


//    /**
//     * Test verifies delete of a record and the behavior of read once the
//     * record has been deleted.
//     */
//    public void test_writeReadDeleteRead() {
//        
//        final IRawStore store = getStore();
//        try {
//        Random r = new Random();
//        
//        final int len = 100;
//        
//        byte[] expected1 = new byte[len];
//        
//        r.nextBytes(expected1);
//        
//        ByteBuffer tmp = ByteBuffer.wrap(expected1);
//        
//        long addr1 = store.write(tmp);
//
//        // verify that the position is advanced to the limit.
//        assertEquals(len,tmp.position());
//        assertEquals(tmp.position(),tmp.limit());
//
//        assertEquals(expected1,store.read(addr1, null));
//        
//        store.delete(addr1);
//
//        if (deleteInvalidatesAddress()) {
//
//            try {
//
//                store.read(addr1, null);
//
//                fail("Expecting: " + IllegalArgumentException.class);
//
//            } catch (IllegalArgumentException ex) {
//
//                System.err.println("Ignoring expected exception: " + ex);
//
//            }
//
//        } else {
//
//            store.read(addr1, null);
//
//        }
//     } finally {store.destroy();}
//    }

//    /**
//     * Note: This will leave a test file around each time since we can
//     * not really call closeAndDelete() when we are testing close().
//     */
    public void test_close() {

        final IRawStore store = getStore();

        try {

            assertTrue(store.isOpen());

            store.close();

            assertFalse(store.isOpen());

            try {

                store.close();

                fail("Expecting: " + IllegalStateException.class);

            } catch (IllegalStateException ex) {

                System.err.println("Ignoring expected exception: " + ex);
            }

        } finally {

            store.destroy();

        }

    }

    /**
     * A random number generated - the seed is NOT fixed.
     */
    final protected Random r = new Random();

    /**
     * Returns random data that will fit in N bytes. N is choosen randomly in
     * 1:1024.
     * 
     * @return A new {@link ByteBuffer} wrapping a new <code>byte[]</code> of
     *         random length and having random contents.
     */
    public ByteBuffer getRandomData() {
        
        final int nbytes = r.nextInt(1024) + 1;
        
        final byte[] bytes = new byte[nbytes];
        
        r.nextBytes(bytes);
        
        return ByteBuffer.wrap(bytes);
        
    }
    
}
