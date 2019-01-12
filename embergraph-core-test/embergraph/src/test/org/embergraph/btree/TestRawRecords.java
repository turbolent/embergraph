/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2018. All rights reserved.
Copyright (C) Embergraph contributors 2019. All rights reserved.

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
package org.embergraph.btree;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.embergraph.btree.data.ILeafData;
import org.embergraph.io.TestCase3;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.rawstore.RawStoreDelegate;
import org.embergraph.rawstore.SimpleMemoryRawStore;

/**
 * Unit tests for a B+Tree with raw record support enabled (this is where a large <code>byte[]
 * </code> value is written directly onto the backing store rather than being stored within the
 * leaf).
 *
 * @author thompsonbry
 */
public class TestRawRecords extends AbstractBTreeTestCase {

  public TestRawRecords() {}

  public TestRawRecords(String name) {
    super(name);
  }

  /**
   * Unit test for the insert of a large <code>byte[]</code> value into an index such that it is
   * represented as a raw record on the backing store rather than inline within the B+Tree leaf.
   */
  public void test_insertLargeValue() {

    final IRawStore store = new SimpleMemoryRawStore();

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setRawRecords(true);
    metadata.setMaxRecLen(64);

    final BTree btree = BTree.create(store, metadata);

    assertEquals(64, btree.getMaxRecLen());

    assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());

    final byte[] key = new byte[] {1};
    final byte[] val = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(val);

    // insert an entry under that key.
    assertNull(btree.insert(key, val));

    // examine the root leaf.
    final Leaf root = (Leaf) btree.getRoot();

    // the addr of the raw record.
    final long addr = root.getRawRecord(0 /* entryIndex */);

    assertTrue(addr != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual = btree.readRawRecord(addr);

    // verify that the expected data were read.
    TestCase3.assertEquals(val, actual);
  }

  /**
   * Unit test in which we update a small value (inline within the leaf) with a large value (stored
   * as a raw record).
   */
  public void test_updateSmallValueWithLargeValue() {

    final IRawStore store = new SimpleMemoryRawStore();

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setRawRecords(true);
    metadata.setMaxRecLen(64);

    final BTree btree = BTree.create(store, metadata);

    assertEquals(64, btree.getMaxRecLen());

    assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());

    final byte[] key = new byte[] {1};
    final byte[] val = new byte[btree.getMaxRecLen() - 1];
    r.nextBytes(val);

    // insert an entry under that key.
    assertNull(btree.insert(key, val));

    // examine the root leaf.
    final Leaf root = (Leaf) btree.getRoot();

    // verify not a raw record.
    assertEquals(IRawStore.NULL, root.getRawRecord(0 /* entryIndex */));

    // create a new byte[] which will be handled as a large record.
    final byte[] newval = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(newval);

    // update the value under that key.
    final byte[] oldval = btree.insert(key, newval);

    // verify the old value was returned.
    assertEquals(val, oldval);

    // the addr of the raw record.
    final long addr = root.getRawRecord(0 /* entryIndex */);

    // verify a raw record.
    assertTrue(addr != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual = btree.readRawRecord(addr);

    // verify that the expected data were read.
    TestCase3.assertEquals(newval, actual);
  }

  /**
   * Unit test in which we update a large value (represented directly on the backing store) with a
   * small value (inline within the leaf). The test verifies that the original large value is
   * deleted.
   */
  public void test_updateLargeValueWithSmallValue() {

    final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setRawRecords(true);
    metadata.setMaxRecLen(64);

    final BTree btree = BTree.create(store, metadata);

    assertEquals(64, btree.getMaxRecLen());

    assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());

    // create a byte[] which will be handled as a large record.
    final byte[] key = new byte[] {1};
    final byte[] val = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(val);

    // insert an entry under that key.
    assertNull(btree.insert(key, val));

    // examine the root leaf.
    final Leaf root = (Leaf) btree.getRoot();

    // the addr of the raw record.
    final long addr = root.getRawRecord(0 /* entryIndex */);

    // verify a raw record.
    assertTrue(addr != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual = btree.readRawRecord(addr);

    // verify that the expected data were read.
    TestCase3.assertEquals(val, actual);

    // create a new byte[] which not will be handled as a large record.
    final byte[] newval = new byte[btree.getMaxRecLen() - 1];
    r.nextBytes(newval);

    // setup for the expected delete of the raw record.
    store.expectDelete = addr;

    // update the value under that key.
    final byte[] oldval = btree.insert(key, newval);

    // verify that the record was deleted.
    assertEquals(IRawStore.NULL, store.expectDelete);

    // verify the old value was returned.
    assertEquals(val, oldval);

    // verify not a raw record.
    assertEquals(IRawStore.NULL, root.getRawRecord(0 /* entryIndex */));
  }

  /**
   * Unit test in which we update a large value (represented directly on the backing store) with
   * another large value (also represented directly on the store). The test verifies that the
   * original large value is deleted.
   */
  public void test_updateLargeValueWithLargeValue() {

    final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setRawRecords(true);
    metadata.setMaxRecLen(64);

    final BTree btree = BTree.create(store, metadata);

    assertEquals(64, btree.getMaxRecLen());

    assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());

    // create a byte[] which will be handled as a large record.
    final byte[] key = new byte[] {1};
    final byte[] val = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(val);

    // insert an entry under that key.
    assertNull(btree.insert(key, val));

    // examine the root leaf.
    final Leaf root = (Leaf) btree.getRoot();

    // the addr of the raw record.
    final long addr = root.getRawRecord(0 /* entryIndex */);

    // verify a raw record.
    assertTrue(addr != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual = btree.readRawRecord(addr);

    // verify that the expected data were read.
    TestCase3.assertEquals(val, actual);

    // create a new byte[] which will be handled as a large record.
    final byte[] newval = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(newval);

    // setup for the expected delete of the raw record.
    store.expectDelete = addr;

    // update the value under that key.
    final byte[] oldval = btree.insert(key, newval);

    // verify that the record was deleted.
    assertEquals(IRawStore.NULL, store.expectDelete);

    // verify the old value was returned.
    assertEquals(val, oldval);

    // the addr of the raw record.
    final long addr2 = root.getRawRecord(0 /* entryIndex */);

    // verify a raw record.
    assertTrue(addr2 != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual2 = btree.readRawRecord(addr2);

    // verify that the expected data were read.
    TestCase3.assertEquals(newval, actual2);
  }

  /**
   * Unit test in which we insert a large value (represented directly on the backing store) and then
   * delete the key under which that value was stored. The test verifies that the original large
   * value is deleted.
   */
  public void test_insertLargeValueThenDelete() {

    final MyRawStore store = new MyRawStore(new SimpleMemoryRawStore());

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setRawRecords(true);
    metadata.setMaxRecLen(64);

    final BTree btree = BTree.create(store, metadata);

    assertEquals(64, btree.getMaxRecLen());

    assertTrue(((ILeafData) btree.getRoot()).hasRawRecords());

    // create a byte[] which will be handled as a large record.
    final byte[] key = new byte[] {1};
    final byte[] val = new byte[btree.getMaxRecLen() + 1];
    r.nextBytes(val);

    // insert an entry under that key.
    assertNull(btree.insert(key, val));

    // examine the root leaf.
    final Leaf root = (Leaf) btree.getRoot();

    // the addr of the raw record.
    final long addr = root.getRawRecord(0 /* entryIndex */);

    // verify a raw record.
    assertTrue(addr != IRawStore.NULL);

    // read the raw record from the store.
    final ByteBuffer actual = btree.readRawRecord(addr);

    // verify that the expected data were read.
    TestCase3.assertEquals(val, actual);

    // setup for the expected delete of the raw record.
    store.expectDelete = addr;

    // update the value under that key.
    final byte[] oldval = btree.remove(key);

    // verify that the record was deleted.
    assertEquals(IRawStore.NULL, store.expectDelete);

    // verify the old value was returned.
    assertEquals(val, oldval);
  }

  /**
   * Helper class is used to watch for deletes of raw records from the backing store.
   *
   * @author thompsonbry
   */
  private static class MyRawStore extends RawStoreDelegate {

    long expectDelete = IRawStore.NULL;

    public MyRawStore(final IRawStore delegate) {
      super(delegate);
    }

    public void delete(long addr) {
      if (expectDelete != IRawStore.NULL) {
        assertEquals(expectDelete, addr);
        expectDelete = IRawStore.NULL;
      }
      delegate.delete(addr);
    }
  }
}
