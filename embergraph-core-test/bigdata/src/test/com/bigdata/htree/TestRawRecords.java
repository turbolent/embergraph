package com.bigdata.htree;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;

import com.bigdata.btree.HTreeIndexMetadata;
import com.bigdata.io.TestCase3;
import com.bigdata.rawstore.IRawStore;
import com.bigdata.rawstore.SimpleMemoryRawStore;

/**
 * Unit tests for a {@link HTree} with raw record support enabled (this is where
 * a large <code>byte[]</code> value is written directly onto the backing store
 * rather than being stored within the leaf).
 * 
 * @author thompsonbry
 */
public class TestRawRecords extends AbstractHTreeTestCase {

	public TestRawRecords() {
	}

	public TestRawRecords(String name) {
		super(name);
	}
	
	/**
	 * Unit test for the insert of a large <code>byte[]</code> value into an
	 * index such that it is represented as a raw record on the backing store
	 * rather than inline within the B+Tree leaf.
	 */
	public void test_insertLargeValue() {

        final IRawStore store = new SimpleMemoryRawStore();

        try {
        
		final Random r = new Random();
		
        final HTreeIndexMetadata metadata = new HTreeIndexMetadata(UUID.randomUUID());

        metadata.setAddressBits(2);
        
        metadata.setRawRecords(true);
        metadata.setMaxRecLen(64);

		final HTree htree = HTree.create(store, metadata);

		assertEquals(64, htree.getMaxRecLen());

		assertTrue(htree.rawRecords);

		final byte[] key = new byte[] { 1, 2, 3 };
		final byte[] val = new byte[htree.getMaxRecLen() + 1];
		r.nextBytes(val);
		
		// insert an entry under that key.
		assertNull(htree.insert(key, val));

		// this test assumes that everything is in a single bucket page.
		assertEquals(1, htree.getLeafCount());

		// examine the root.
		final DirectoryPage root = (DirectoryPage) htree.getRoot();
		
		// all references should be to the same bucket page.
		final BucketPage bucket = (BucketPage) root.childRefs[0].get();

		// the addr of the raw record.
		final long addr = bucket.getRawRecord(0/* entryIndex */);

		assertTrue(addr != IRawStore.NULL);

		// read the raw record from the store.
		final ByteBuffer actual = htree.readRawRecord(addr);

		// verify that the expected data were read.
		TestCase3.assertEquals(val, actual);
		
        } finally {
        	
        	store.destroy();
        	
        }
		
	}

	/*
	 * FIXME The rest of the tests in this file deal with update, which we do
	 * not yet support on the HTree.
	 */
	
//	/**
//	 * Unit test in which we update a small value (inline within the leaf) with
//	 * a large value (stored as a raw record).
//	 */
//	public void test_updateSmallValueWithLargeValue() {
//		
//		final Random r = new Random();
//		
//        final IRawStore store = new SimpleMemoryRawStore();
//
//        try {
//        
//        final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
//
//        metadata.setAddressBits(2);
//
//        metadata.setRawRecords(true);
//        metadata.setMaxRecLen(64);
//
//		final HTree btree = HTree.create(store, metadata);
//
//		assertEquals(64, btree.getMaxRecLen());
//
//		assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());
//
//		assertTrue(btree.rawRecords);
//
//		final byte[] key = new byte[] { 1, 2, 3 };
//		final byte[] val = new byte[btree.getMaxRecLen() - 1];
//		r.nextBytes(val);
//		
//		// insert an entry under that key.
//		assertNull(btree.insert(key, val));
//		
//		// this test assumes that everything is in a single bucket page.
//		assertEquals(1, btree.getLeafCount());
//
//		// examine the root.
//		final DirectoryPage root = (DirectoryPage) btree.getRoot();
//		
//		// all references should be to the same bucket page.
//		final BucketPage bucket = (BucketPage) root.childRefs[0].get();
//
//		// verify not a raw record.
//		assertEquals(IRawStore.NULL,bucket.getRawRecord(0/* entryIndex */));
//
//		// create a new byte[] which will be handled as a large record. 
//		final byte[] newval = new byte[btree.getMaxRecLen() + 1];
//		r.nextBytes(newval);
//		
//		// update the value under that key.
//		final byte[] oldval = btree.insert(key, newval);
//		if(true) {
//			/*
//			 * This test will not work as written because insert() does not
//			 * have update semantics for the HTree.
//			 */
//			fail("insert() does NOT have update semantics for the HTree");
//		}
//		// verify the old value was returned.
//		assertEquals(val, oldval);
//		
//		// the addr of the raw record.
//		final long addr = bucket.getRawRecord(0/* entryIndex */);
//
//		// verify a raw record.
//		assertTrue(addr != IRawStore.NULL);
//
//		// read the raw record from the store.
//		final ByteBuffer actual = btree.readRawRecord(addr);
//
//		// verify that the expected data were read.
//		TestCase3.assertEquals(newval, actual);
//		
//        } finally {
//        	
//        	store.destroy();
//        	
//        }
//		
//	}
//
//	/**
//	 * Unit test in which we update a large value (represented directly on the
//	 * backing store) with a small value (inline within the leaf). The test
//	 * verifies that the original large value is deleted.
//	 */
//	public void test_updateLargeValueWithSmallValue() {
//		
//        final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());
//
//        final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
//        
//        metadata.setRawRecords(true);
//        metadata.setMaxRecLen(64);
//
//		final BTree btree = BTree.create(store, metadata);
//
//		assertEquals(64, btree.getMaxRecLen());
//
//		assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());
//
//		// create a byte[] which will be handled as a large record.
//		final byte[] key = new byte[] { 1 };
//		final byte[] val = new byte[btree.getMaxRecLen() + 1];
//		r.nextBytes(val);
//		
//		// insert an entry under that key.
//		assertNull(btree.insert(key, val));
//		
//		// examine the root leaf.
//		final Leaf root = (Leaf) btree.getRoot();
//		
//		// the addr of the raw record.
//		final long addr = root.getRawRecord(0/* entryIndex */);
//
//		// verify a raw record.
//		assertTrue(addr != IRawStore.NULL);
//
//		// read the raw record from the store.
//		final ByteBuffer actual = btree.readRawRecord(addr);
//
//		// verify that the expected data were read.
//		TestCase3.assertEquals(val, actual);
//
//		// create a new byte[] which not will be handled as a large record. 
//		final byte[] newval = new byte[btree.getMaxRecLen() - 1];
//		r.nextBytes(newval);
//		
//		// setup for the expected delete of the raw record.
//		store.expectDelete = addr;
//		
//		// update the value under that key.
//		final byte[] oldval = btree.insert(key, newval);
//
//		// verify that the record was deleted.
//		assertEquals(IRawStore.NULL, store.expectDelete);
//
//		// verify the old value was returned.
//		assertEquals(val, oldval);
//
//		// verify not a raw record.
//		assertEquals(IRawStore.NULL,root.getRawRecord(0/* entryIndex */));
//
//	}
//
//	/**
//	 * Unit test in which we update a large value (represented directly on the
//	 * backing store) with another large value (also represented directly on the
//	 * store). The test verifies that the original large value is deleted.
//	 */
//	public void test_updateLargeValueWithLargeValue() {
//		
//        final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());
//
//        final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
//        
//        metadata.setRawRecords(true);
//        metadata.setMaxRecLen(64);
//
//		final BTree btree = BTree.create(store, metadata);
//
//		assertEquals(64, btree.getMaxRecLen());
//
//		assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());
//
//		// create a byte[] which will be handled as a large record.
//		final byte[] key = new byte[] { 1 };
//		final byte[] val = new byte[btree.getMaxRecLen() + 1];
//		r.nextBytes(val);
//		
//		// insert an entry under that key.
//		assertNull(btree.insert(key, val));
//		
//		// examine the root leaf.
//		final Leaf root = (Leaf) btree.getRoot();
//		
//		// the addr of the raw record.
//		final long addr = root.getRawRecord(0/* entryIndex */);
//
//		// verify a raw record.
//		assertTrue(addr != IRawStore.NULL);
//
//		// read the raw record from the store.
//		final ByteBuffer actual = btree.readRawRecord(addr);
//
//		// verify that the expected data were read.
//		TestCase3.assertEquals(val, actual);
//
//		// create a new byte[] which will be handled as a large record. 
//		final byte[] newval = new byte[btree.getMaxRecLen() + 1];
//		r.nextBytes(newval);
//		
//		// setup for the expected delete of the raw record.
//		store.expectDelete = addr;
//		
//		// update the value under that key.
//		final byte[] oldval = btree.insert(key, newval);
//
//		// verify that the record was deleted.
//		assertEquals(IRawStore.NULL, store.expectDelete);
//
//		// verify the old value was returned.
//		assertEquals(val, oldval);
//
//		// the addr of the raw record.
//		final long addr2 = root.getRawRecord(0/* entryIndex */);
//
//		// verify a raw record.
//		assertTrue(addr2 != IRawStore.NULL);
//
//		// read the raw record from the store.
//		final ByteBuffer actual2 = btree.readRawRecord(addr2);
//
//		// verify that the expected data were read.
//		TestCase3.assertEquals(newval, actual2);
//		
//	}
//
//	/**
//	 * Unit test in which we insert a large value (represented directly on the
//	 * backing store) and then delete the key under which that value was stored.
//	 * The test verifies that the original large value is deleted.
//	 */
//	public void test_insertLargeValueThenDelete() {
//		
//        final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());
//
//        final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
//        
//        metadata.setRawRecords(true);
//        metadata.setMaxRecLen(64);
//
//		final BTree btree = BTree.create(store, metadata);
//
//		assertEquals(64, btree.getMaxRecLen());
//
//		assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());
//
//		// create a byte[] which will be handled as a large record.
//		final byte[] key = new byte[] { 1 };
//		final byte[] val = new byte[btree.getMaxRecLen() + 1];
//		r.nextBytes(val);
//		
//		// insert an entry under that key.
//		assertNull(btree.insert(key, val));
//		
//		// examine the root leaf.
//		final Leaf root = (Leaf) btree.getRoot();
//		
//		// the addr of the raw record.
//		final long addr = root.getRawRecord(0/* entryIndex */);
//
//		// verify a raw record.
//		assertTrue(addr != IRawStore.NULL);
//
//		// read the raw record from the store.
//		final ByteBuffer actual = btree.readRawRecord(addr);
//
//		// verify that the expected data were read.
//		TestCase3.assertEquals(val, actual);
//
//		// setup for the expected delete of the raw record.
//		store.expectDelete = addr;
//		
//		// update the value under that key.
//		final byte[] oldval = btree.remove(key);
//
//		// verify that the record was deleted.
//		assertEquals(IRawStore.NULL, store.expectDelete);
//
//		// verify the old value was returned.
//		assertEquals(val, oldval);
//
//	}
//
//	/**
//	 * Helper class is used to watch for deletes of raw records from the backing
//	 * store.
//	 * 
//	 * @author thompsonbry
//	 */
//	private static class MyRawStore implements IRawStore {
//		
//		final IRawStore delegate;
//		
//		long expectDelete = IRawStore.NULL;
//
//		public MyRawStore(final IRawStore delegate) {
//			this.delegate = delegate;
//		}
//
//		public void close() {
//			delegate.close();
//		}
//
//		public void delete(long addr) {
//			if (expectDelete != IRawStore.NULL) {
//				assertEquals(expectDelete, addr);
//				expectDelete = IRawStore.NULL;
//			}
//			delegate.delete(addr);
//		}
//
//		public void deleteResources() {
//			delegate.deleteResources();
//		}
//
//		public void destroy() {
//			delegate.destroy();
//		}
//
//		public void force(boolean metadata) {
//			delegate.force(metadata);
//		}
//
//		public int getByteCount(long addr) {
//			return delegate.getByteCount(addr);
//		}
//
//		public CounterSet getCounters() {
//			return delegate.getCounters();
//		}
//
//		public File getFile() {
//			return delegate.getFile();
//		}
//
//		public long getOffset(long addr) {
//			return delegate.getOffset(addr);
//		}
//
//		public IResourceMetadata getResourceMetadata() {
//			return delegate.getResourceMetadata();
//		}
//
//		public UUID getUUID() {
//			return delegate.getUUID();
//		}
//
//		public boolean isFullyBuffered() {
//			return delegate.isFullyBuffered();
//		}
//
//		public boolean isOpen() {
//			return delegate.isOpen();
//		}
//
//		public boolean isReadOnly() {
//			return delegate.isReadOnly();
//		}
//
//		public boolean isStable() {
//			return delegate.isStable();
//		}
//
//		public ByteBuffer read(long addr) {
//			return delegate.read(addr);
//		}
//
//		public long size() {
//			return delegate.size();
//		}
//
//		public long toAddr(int nbytes, long offset) {
//			return delegate.toAddr(nbytes, offset);
//		}
//
//		public String toString(long addr) {
//			return delegate.toString(addr);
//		}
//
//		public long write(ByteBuffer data, long oldAddr) {
//			return delegate.write(data, oldAddr);
//		}
//
//		public long write(ByteBuffer data) {
//			return delegate.write(data);
//		}
//
//	}

}
