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
/*
 * Created on Oct 16, 2006
 */

package org.embergraph.journal;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.Tuple;
import org.embergraph.btree.isolation.IsolatedFusedView;
import org.embergraph.util.InnerCause;

/*
* Test suite for fully-isolated read-write transactions.
 *
 * @todo verify with writes on multiple indices (partial ordering over the commits)
 * @todo verify partial ordering imposed on concurrent operations on the same tx for indices
 *     declared by those operations, etc.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestTx extends ProxyTestCase<Journal> {

  public TestTx() {}

  public TestTx(String name) {

    super(name);
  }

  //    /*
//     * Writes some interesting constants on {@link System#err}.
  //     */
  //    public void test_constants() {
  //
  //        if(log.isInfoEnabled()) log.info("min : "+new Date(Long.MIN_VALUE));
  //        if(log.isInfoEnabled()) log.info("min1: "+new Date(Long.MIN_VALUE+1));
  //        if(log.isInfoEnabled()) log.info("-1L : "+new Date(-1));
  //        if(log.isInfoEnabled()) log.info(" 0L : "+new Date(0L));
  //        if(log.isInfoEnabled()) log.info("max1: "+new Date(Long.MAX_VALUE-1));
  //        if(log.isInfoEnabled()) log.info("max : "+new Date(Long.MAX_VALUE));
  //
  //    }

  /*
   * Test verifies that a transaction may start when there are (a) no commits on the journal; and
   * (b) no indices have been registered.
   *
   * <p>Note: The transaction will be unable to isolate an index if the index has not been
   * registered already by an unisolated operation.
   */
  public void test_noIndicesRegistered() {

    final Journal journal = getStore();

    try {

      journal.commit();

      final long tx = journal.newTx(ITx.UNISOLATED);

      /*
       * nothing written on this transaction.
       */

      // commit.
      assertEquals(0L, journal.commit(tx));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Verify that an index is not visible in the tx unless the native transaction in which it is
   * registered has already committed before the tx starts.
   */
  public void test_indexNotVisibleUnlessCommitted() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      // register index in unisolated scope, but do not commit yet.
      {
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);
      }

      // start tx1.
      final long tx1 = journal.newTx(ITx.UNISOLATED);

      // the index is not visible in tx1.
      assertNull(journal.getIndex(name, tx1));

      // do unisolated commit.
      assertNotSame(0L, journal.commit());

      // start tx2.
      final long tx2 = journal.newTx(ITx.UNISOLATED);

      // the index still is not visible in tx1.
      assertNull(journal.getIndex(name, tx1));

      // the index is visible in tx2.
      assertNotNull(journal.getIndex(name, tx2));

      journal.abort(tx1);

      journal.abort(tx2);

    } finally {

      journal.destroy();
    }
  }

  /*
   * Test verifies that you always get the same object back when you ask for an isolated named
   * index. This is important both to conserve resources and since the write set is in the isolated
   * index -- you lose it and it is gone.
   */
  public void test_sameIndexObject() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      {
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        journal.commit();
      }

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      final IIndex ndx1 = journal.getIndex(name, tx1);

      assertNotNull(ndx1);

      final long tx2 = journal.newTx(ITx.UNISOLATED);

      final IIndex ndx2 = journal.getIndex(name, tx2);

      assertTrue(tx1 != tx2);

      assertTrue(ndx1 != ndx2);

      assertNotNull(ndx2);

      assertTrue(ndx1 == journal.getIndex(name, tx1));

      assertTrue(ndx2 == journal.getIndex(name, tx2));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Create a journal, setup an index, write an entry on that index, and commit the store. Setup a
   * transaction and verify that we can isolate that index and read the written value. Write a value
   * on the unisolated index and verify that it is not visible within the transaction.
   */
  public void test_readIsolation() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      final byte[] k1 = new byte[] {1};
      final byte[] k2 = new byte[] {2};

      final byte[] v1 = new byte[] {1};
      final byte[] v2 = new byte[] {2};

      {

      /*
       * register the index, write an entry on the unisolated index,
         * and commit the journal.
         */

        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        IIndex index = journal.getIndex(name);

        assertNull(index.insert(k1, v1));

        assertNotSame(0L, journal.commit());
      }

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      if (log.isDebugEnabled()) log.debug("State A, tx1: " + tx1 + "\n" + showCRI(journal));

      {

      /*
       * verify that the write is visible in a transaction that starts
         * after the commit.
         */

        IIndex index = journal.getIndex(name, tx1);

        assertTrue(index.contains(k1));

        assertEquals(v1, index.lookup(k1));
      }

      {

      /*
       * obtain the unisolated index and write another entry and
         * commit the journal.
         */

        IIndex index = journal.getIndex(name);

        assertNull(index.insert(k2, v2));
        assertTrue(index.contains(k2));

        final long c2 = journal.commit();
        if (log.isDebugEnabled()) log.debug("State B, c2: " + c2 + "\n" + showCRI(journal));
        assertNotSame(0L, c2);
      }

      {

      /*
       * verify that the entry written on the unisolated index is not
         * visible to the transaction that started before that write.
         */

        IIndex index = journal.getIndex(name, tx1);

        assertTrue(index.contains(k1));
        assertFalse(index.contains(k2));
      }

      {
        final IIndex index = journal.getIndex(name);

        assertTrue(index.contains(k1));
        assertTrue(index.contains(k2));
      }

      /*
       * start another transaction and verify that the 2nd committed
       * write is now visible to that transaction.
       */

      final long tx2 = journal.newTx(ITx.UNISOLATED);

      if (log.isDebugEnabled())
        log.debug("tx1: " + tx1 + ", tx2: " + tx2 + "\n" + showCRI(journal));

      {
      /*
       * start another transaction and verify that the 2nd committed
         * write is now visible to that transaction.
         */

        IIndex index = journal.getIndex(name, tx2);

        assertTrue(index.contains(k1));
        assertTrue(index.contains(k2));
      }

      journal.abort(tx1);

      journal.abort(tx2);

    } finally {

      journal.destroy();
    }
  }

  private static String showCRI(final Journal journal) {
    final ITupleIterator<CommitRecordIndex.Entry> commitRecords;
    /*
     * Commit can be called prior to Journal initialisation, in which case
     * the commitRecordIndex will not be set.
     */
    final IIndex commitRecordIndex = journal.getReadOnlyCommitRecordIndex();
    if (commitRecordIndex == null) { // TODO Why is this here?
      return "EMPTY";
    }

    //		final IndexMetadata metadata = commitRecordIndex.getIndexMetadata();

    commitRecords = commitRecordIndex.rangeIterator();

    StringBuilder out = new StringBuilder();
    while (commitRecords.hasNext()) {

      final ITuple<CommitRecordIndex.Entry> tuple = commitRecords.next();

      final CommitRecordIndex.Entry entry = tuple.getObject();

      try {

        final ICommitRecord record =
            CommitRecordSerializer.INSTANCE.deserialize(journal.read(entry.addr));

        out.append(record.toString() + "\n");
      } catch (RuntimeException re) {

        throw new RuntimeException("Problem with entry at " + entry.addr, re);
      }
    }
    return out.toString();
  }

  /*
   * Test verifies that an isolated write is visible inside of a transaction (tx1) but not in a
   * concurrent transaction (tx2) and not in the unisolated index until the tx1 commits. Once the
   * tx1 commits, the write is visible in the unisolated index. The write never becomes visible in
   * tx2. If tx2 attempts to write a value under the same key then a write-write conflict is
   * reported and validation fails.
   */
  public void test_writeIsolation() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      final byte[] k1 = new byte[] {1};

      final byte[] v1 = new byte[] {1};
      final byte[] v1a = new byte[] {1, 1};

      {

      /*
       * register an index and commit the journal.
         */

        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        assertNotSame(0L, journal.commit());
      }

      /*
       * create two transactions.
       */

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      final long tx2 = journal.newTx(ITx.UNISOLATED);

      assertNotSame(tx1, tx2);

      assertTrue(Math.abs(tx1) >= journal.getRootBlockView().getLastCommitTime());

      assertTrue(Math.abs(tx2) > Math.abs(tx1));

      {

      /*
       * Write an entry in tx1.
         *
         * Verify that the entry is not visible in the unisolated index
         * or in the index as isolated by tx2.
         */

        final IsolatedFusedView ndx1 = (IsolatedFusedView) journal.getIndex(name, tx1);

        assertFalse(ndx1.contains(k1));

        assertNull(ndx1.insert(k1, v1));

        // existence check in tx1.
        assertTrue(ndx1.contains(k1));

        // not visible in the other tx.
        assertFalse(journal.getIndex(name, tx2).contains(k1));

        // not visible in the unisolated index.
        assertFalse(journal.getIndex(name).contains(k1));

      /*
       * Commit tx1.
         *
         * Verify that the write is still not visible in tx2 but that it
         * is now visible in the unisolated index.
         */

        // grab hard ref. to the local state for the tx before commit()
        final Tx localState = journal.getLocalTransactionManager().getTx(tx1);

        // commit tx1.
        final long commitTime1 = journal.commit(tx1);
        assertNotSame(0L, commitTime1);

        // still not visible in the other tx.
        assertFalse(journal.getIndex(name, tx2).contains(k1));

        // but now visible in the unisolated index.
        assertTrue(journal.getIndex(name).contains(k1));

        // check the version timestamp in the unisolated index.
        {
          final BTree btree = journal.getIndex(name);

          final ITuple<?> tuple = btree.lookup(k1, new Tuple(btree, IRangeQuery.ALL));

          assertNotNull(tuple);

          assertFalse(tuple.isDeletedVersion());

        /*
       * Verify that the revisionTime was written onto the tuple
           * in the post-commit view of the unisolated index.
           */
          assertEquals("revisionTime", localState.getRevisionTime(), tuple.getVersionTimestamp());
        }

      /*
       * write a conflicting entry in tx2 and verify that validation
         * of tx2 fails.
         */

        assertNull(journal.getIndex(name, tx2).insert(k1, v1a));

        // check the version counter in tx2.
        {
          final IsolatedFusedView isolatedView = (IsolatedFusedView) journal.getIndex(name, tx2);

          final BTree btree = journal.getIndex(name);

          Tuple<?> tuple = btree.lookup(k1, new Tuple(btree, IRangeQuery.ALL));

          tuple = isolatedView.getWriteSet().lookup(k1, tuple);

          assertNotNull(tuple);

          assertFalse(tuple.isDeletedVersion());

        /*
       * Verify the versionTimestamp on the tuple in the view
           * isolated by [tx2]. It should now be the start time for
           * tx2 since we just overwrote that tuple.
           */
          assertEquals("versionTimestamp", Math.abs(tx2), tuple.getVersionTimestamp());
        }

        try {

          journal.commit(tx2);

          fail("Expecting: " + ValidationError.class);

        } catch (ValidationError ex) {

          if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
        }
      }

    } finally {

      journal.destroy();
    }
  }

  // Delete object.

  /*
   * Two transactions (tx0, tx1) are created. A version (v0) is written onto tx0 for a key (id0).
   * The test verifies the write and verifies that the write is not visible in tx1. The v0 is then
   * deleted from tx0 and then another version (v1) is written on tx0 under the same key. Both
   * transactions prepare and commit. The end state is that (id0,v1) is visible in the database
   * after the commit.
   */
  public void test_delete001() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      {
      /*
       * register an index and commit the journal.
         */
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        journal.commit();
      }

      /*
       * create transactions.
       */

      final long tx0 = journal.newTx(ITx.UNISOLATED);

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      assertNotSame(tx0, tx1);

      assertTrue(Math.abs(tx0) >= journal.getRootBlockView().getLastCommitTime());

      assertTrue(Math.abs(tx1) > Math.abs(tx0));

      /*
       * Write v0 on tx0.
       */
      final byte[] id0 = new byte[] {0};
      final byte[] v0 = getRandomData().array();

      journal.getIndex(name, tx0).insert(id0, v0);

      assertEquals(v0, journal.getIndex(name, tx0).lookup(id0));

      /*
       * Verify that the version does NOT show up in a concurrent
       * transaction.
       */
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // delete the version.
      assertEquals(v0, journal.getIndex(name, tx0).remove(id0));

      // no longer visible in that transaction.
      assertFalse(journal.getIndex(name, tx0).contains(id0));

      /*
       * Test delete after delete (succeeds, but returns null).
       */
      assertNull(journal.getIndex(name, tx0).remove(id0));

      /*
       * Test write after delete (succeeds, returning null).
       */
      final byte[] v1 = getRandomData().array();
      assertNull(journal.getIndex(name, tx0).insert(id0, v1));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

      // Prepare and commit tx0.
      assertNotSame(0L, journal.commit(tx0));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // Now visible in global scope.
      assertTrue(journal.getIndex(name).contains(id0));

      // Prepare and commit tx1 (no writes).
      assertEquals(0L, journal.commit(tx1));

      // Still visible in global scope.
      assertTrue(journal.getIndex(name).contains(id0));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Two transactions (tx0, tx1) are created. A version (v0) is written onto tx0 for a key (id0).
   * The test verifies the write and verifies that the write is not visible in tx1. The v0 is then
   * deleted from tx0 and then another version (v1) is written on tx0 under the same key and
   * isolation is re-verified. Finally, v1 is deleted from tx0. Both transactions prepare and
   * commit. The end state is that no entry for id0 is visible in the database after the commit.
   */
  public void test_delete002() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      {
      /*
       * register an index and commit the journal.
         */
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        journal.commit();
      }

      /*
       * create transactions.
       */

      final long tx0 = journal.newTx(ITx.UNISOLATED);

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      assertNotSame(tx0, tx1);

      assertTrue(Math.abs(tx0) >= journal.getRootBlockView().getLastCommitTime());

      assertTrue(Math.abs(tx1) > Math.abs(tx0));

      /*
       * Write v0 on tx0.
       */
      final byte[] id0 = new byte[] {1};
      final byte[] v0 = getRandomData().array();
      journal.getIndex(name, tx0).insert(id0, v0);
      assertEquals(v0, journal.getIndex(name, tx0).lookup(id0));

      /*
       * Verify that the version does NOT show up in a concurrent
       * transaction.
       */
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // delete the version.
      assertEquals(v0, journal.getIndex(name, tx0).remove(id0));

      // no longer visible in that transaction.
      assertFalse(journal.getIndex(name, tx0).contains(id0));

      /*
       * Test delete after delete (succeeds, but returns null).
       */
      assertNull(journal.getIndex(name, tx0).remove(id0));

      /*
       * Test write after delete (succeeds, returning null).
       */
      final byte[] v1 = getRandomData().array();
      assertNull(journal.getIndex(name, tx0).insert(id0, v1));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

      /*
       * Delete v1.
       */
      assertEquals(v1, journal.getIndex(name, tx0).remove(id0));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

      /*
       * Prepare and commit tx0.
       *
       * Note: We MUST NOT propagate a delete marker onto the unisolated
       * index since no entry for that key is visible was visible when the
       * tx0 began.
       *
       * Note: this should wind up as a NOP commit since the net result
       * will be no writes on the unisolated index and hence no writes
       * on the backing store.
       */
      assertEquals(0L, journal.commit(tx0));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

      // Prepare and commit tx1 (no writes).
      assertEquals(0L, journal.commit(tx1));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Two transactions (tx0, tx1) are created. A version (v0) is written onto tx0 for a key (id0).
   * The test verifies the write and verifies that the write is not visible in tx1. The v0 is then
   * deleted from tx0. Another version (v1) is written on tx1 under the same key and isolation is
   * re-verified. Both transactions prepare and commit. Since no entry for id0 was pre-existing in
   * the global scope the delete in tx0 does not propagate into the global scope and the end state
   * is that the write (id0,v1) from tx1 is visible in the database after the commit.
   */
  public void test_delete003() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      {
      /*
       * register an index and commit the journal.
         */
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        journal.commit();
      }

      /*
       * create transactions.
       */

      final long tx0 = journal.newTx(ITx.UNISOLATED);

      final long tx1 = journal.newTx(ITx.UNISOLATED);

      assertNotSame(tx0, tx1);

      assertTrue(Math.abs(tx0) >= journal.getRootBlockView().getLastCommitTime());

      assertTrue(Math.abs(tx1) > Math.abs(tx0));

      /*
       * Write v0 on tx0.
       */
      final byte[] id0 = new byte[] {1};
      final byte[] v0 = getRandomData().array();
      journal.getIndex(name, tx0).insert(id0, v0);
      assertEquals(v0, journal.getIndex(name, tx0).lookup(id0));

      /*
       * Verify that the version does NOT show up in a concurrent
       * transaction.
       */
      assertFalse(journal.getIndex(name, tx1).contains(id0));

      // delete the version.
      assertEquals(v0, journal.getIndex(name, tx0).remove(id0));

      // no longer visible in that transaction.
      assertFalse(journal.getIndex(name, tx0).contains(id0));

      /*
       * write(id0,v1) in tx1.
       */
      final byte[] v1 = getRandomData().array();
      assertNull(journal.getIndex(name, tx1).insert(id0, v1));

      // Still not visible in concurrent transaction.
      assertFalse(journal.getIndex(name, tx0).contains(id0));

      // Still not visible in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

      /*
       * Prepare and commit tx0.
       *
       * Note: We MUST NOT propagate a delete marker onto the unisolated
       * index since no entry for that key is visible was visible when the
       * tx0 began.
       *
       * Note: this should wind up as a NOP commit since the net result
       * will be no writes on the unisolated index and hence no writes
       * on the backing store.
       */
      assertEquals(0L, journal.commit(tx0));

      // Prepare and commit tx1.
      assertNotSame(0L, journal.commit(tx1));

      // (id0,v1) is now visible in global scope.
      assertTrue(journal.getIndex(name).contains(id0));
      assertEquals(v1, journal.getIndex(name).lookup(id0));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Transaction semantics tests.
   */

  /*
   * Simple test of commit semantics (no conflict). Four transactions are started: tx0, which starts
   * first. tx1 which starts next and on which we will write one data version; tx2, which begins
   * after tx1 but before tx1 commits - the change will NOT be visible in this transaction; and tx3,
   * which begins after tx1 commits - the change will be visible in this transaction.
   */
  public void test_commit_noConflict01() {

    final Journal journal = getStore();

    try {

      final String name = "abc";
      final long commitTime0;
      {
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        commitTime0 = journal.commit();
        if (log.isInfoEnabled()) log.info("commitTime0: " + journal.getCommitRecord());

        assertNotSame(0L, commitTime0);
        assertEquals("commitCounter", 1L, journal.getCommitRecord().getCommitCounter());
      }

      /*
       * Transaction that starts before the transaction on which we write.
       * The change will not be visible in this scope.
       */
      final long tx0 = journal.newTx(ITx.UNISOLATED);

      // transaction on which we write and later commit.
      final long tx1 = journal.newTx(ITx.UNISOLATED);

      // new transaction - commit will not be visible in this scope.
      final long tx2 = journal.newTx(ITx.UNISOLATED);

      if (log.isInfoEnabled()) log.info("commitTime0   =" + commitTime0);
      if (log.isInfoEnabled()) log.info("tx0: startTime=" + tx0);
      if (log.isInfoEnabled()) log.info("tx1: startTime=" + tx1);
      if (log.isInfoEnabled()) log.info("tx2: startTime=" + tx2);

      assertTrue(commitTime0 <= Math.abs(tx0));
      assertTrue(Math.abs(tx0) < Math.abs(tx1));
      assertTrue(Math.abs(tx1) < Math.abs(tx2));

      final byte[] id1 = new byte[] {1};

      final byte[] v0 = getRandomData().array();

      // write data version on tx1
      assertNull(journal.getIndex(name, tx1).insert(id1, v0));

      // data version visible in tx1.
      assertEquals(v0, journal.getIndex(name, tx1).lookup(id1));

      // data version not visible in global scope.
      assertNull(journal.getIndex(name).lookup(id1));

      // data version not visible in tx0.
      assertNull(journal.getIndex(name, tx0).lookup(id1));

      // data version not visible in tx2.
      assertNull(journal.getIndex(name, tx2).lookup(id1));

      // commit.
      final long tx1CommitTime = journal.commit(tx1);
      assertNotSame(0L, tx1CommitTime);
      if (log.isInfoEnabled()) log.info("tx1: startTime=" + tx1 + ", commitTime=" + tx1CommitTime);
      if (log.isInfoEnabled()) log.info("tx1: after commit: " + journal.getCommitRecord());
      assertEquals("commitCounter", 2L, journal.getCommitRecord().getCommitCounter());

      // data version now visible in global scope.
      assertEquals(v0, journal.getIndex(name).lookup(id1));

      // new transaction - commit is visible in this scope.
      final long tx3 = journal.newTx(ITx.UNISOLATED);
      assertTrue(Math.abs(tx2) < Math.abs(tx3));
      assertTrue(Math.abs(tx3) >= tx1CommitTime);
      if (log.isInfoEnabled()) log.info("tx3: startTime=" + tx3);
      // if(log.isInfoEnabled()) log.info("tx3: ground state:
      // "+((Tx)journal.getTx(tx3)).commitRecord);

      // data version still not visible in tx0.
      assertNull(journal.getIndex(name, tx0).lookup(id1));

      // data version still not visible in tx2.
      assertNull(journal.getIndex(name, tx2).lookup(id1));

      /*
       * What commit record was written by tx1 and what commit record is
       * being used by tx3?
       */

      // data version visible in the new tx (tx3).
      assertEquals(v0, journal.getIndex(name, tx3).lookup(id1));

      /*
       * commit tx0 - nothing was written, no conflict should result.
       */
      assertEquals(0L, journal.commit(tx0));
      assertEquals("commitCounter", 2L, journal.getCommitRecord().getCommitCounter());

      /*
       * commit tx1 - nothing was written, no conflict should result.
       */
      assertEquals(0L, journal.commit(tx2));
      assertEquals("commitCounter", 2L, journal.getCommitRecord().getCommitCounter());

      // commit tx3 - nothing was written, no conflict should result.
      assertEquals(0L, journal.commit(tx3));
      assertEquals("commitCounter", 2L, journal.getCommitRecord().getCommitCounter());

      // data version in global scope was not changed by any other commit.
      assertEquals(v0, journal.getIndex(name).lookup(id1));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Test in which a transaction deletes a pre-existing version (that is, a version that existed in
   * global scope when the transaction was started).
   */
  public void test_deletePreExistingVersion_noConflict() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      {
        IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        journal.registerIndex(md);

        journal.commit();
      }

      final byte[] id0 = new byte[] {1};

      final byte[] v0 = getRandomData().array();

      // data version not visible in global scope.
      assertNull(journal.getIndex(name).lookup(id0));

      // write data version in global scope.
      journal.getIndex(name).insert(id0, v0);

      // data version visible in global scope.
      assertEquals(v0, journal.getIndex(name).lookup(id0));

      // commit the unisolated write.
      journal.commit();

      // start transaction.
      final long tx0 = journal.newTx(ITx.UNISOLATED);

      // data version visible in the transaction.
      assertEquals(v0, journal.getIndex(name, tx0).lookup(id0));

      // delete version in transaction scope.
      assertEquals(v0, journal.getIndex(name, tx0).remove(id0));

      // data version still visible in global scope.
      assertTrue(journal.getIndex(name).contains(id0));
      assertEquals(v0, journal.getIndex(name).lookup(id0));

      // data version not visible in transaction.
      assertFalse(journal.getIndex(name, tx0).contains(id0));
      assertNull(journal.getIndex(name, tx0).lookup(id0));

      // commit.
      journal.commit(tx0);

      // data version now deleted in global scope.
      assertFalse(journal.getIndex(name).contains(id0));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Unit test written to verify that a read-only tx gets the same view of an index when it has the
   * same ground state as another read-only tx. That view should be a simple {@link BTree} rather
   * than an {@link IsolatedFusedView}.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/266">Refactor native long tx id
   *     to thin object</a>
   * @see <a href="http://sourceforge.net/apps/trac/bigdata/ticket/546" > Add cache for access to
   *     historical index views on the Journal by name and commitTime. </a>
   */
  public void test_readOnlyTx() {

    final Journal journal = getStore();

    try {

      final String name = "abc";

      final long commitTime1;
      {
        final IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

      /*
       * Note: Thie tests w/ and w/o isolation. Isolation is NOT
         * required for this. We are testing the semantics of read-only
         * transactions and those are available for indices which do NOT
         * support isolation as well as those which do.
         */

        md.setIsolatable(r.nextBoolean());

        journal.registerIndex(md);

        commitTime1 = journal.commit();
      }

      // The unisolated index view.
      final BTree un = journal.getIndex(name);

      // The index exists.
      assertNotNull(un);

      // The unisolated view is not in the historical index cache.
      assertEquals("historicalIndexCacheSize", 0, journal.getHistoricalIndexCacheSize());

      // The unisolated view is not in the index cache.
      assertEquals("indexCacheSize", 0, journal.getIndexCacheSize());

      // start 2 transactions. they will read from the same commit point.
      final long tx0 = journal.newTx(ITx.READ_COMMITTED);
      final long tx1 = journal.newTx(ITx.READ_COMMITTED);

      // txids MUST be distinct.
      assertTrue(tx0 != tx1);

      // resolve the same index for those transactions.
      final ILocalBTreeView tx0Index = journal.getIndex(name, tx0);
      final ILocalBTreeView tx1Index = journal.getIndex(name, tx1);

      /*
       * The index shows up exactly once in the historical index cache.
       *
       * Note: The Name2Addr index will also wind up in this cache when it
       * is fetched for a historical commit time.
       */
      assertEquals(
          "historicalIndexCacheSize", 1 + 1 /* Name2Addr */, journal.getHistoricalIndexCacheSize());

      /*
       * The index shows up exactly once in the index cache.
       *
       * Note: Name2Addr is NOT present in this cache.
       */
      assertEquals("indexCacheSize", 1, journal.getIndexCacheSize());

      // Verify that the views are BTree instances vs IsolatedFusedViews
      if (!(tx0Index instanceof BTree))
        fail("Expecting " + BTree.class + " but have " + tx0Index.getClass());

      if (!(tx1Index instanceof BTree))
        fail("Expecting " + BTree.class + " but have " + tx1Index.getClass());

      // Lookup the underlying ITx objects.
      final Tx tx0Obj = (Tx) journal.getTransactionManager().getTx(tx0);
      final Tx tx1Obj = (Tx) journal.getTransactionManager().getTx(tx1);

      // Should be the commitTime for the commit above.
      assertEquals("commitTime", commitTime1, tx0Obj.getReadsOnCommitTime());

      // Should read on the same commit time.
      assertEquals(
          "readsOnCommitTime", tx0Obj.getReadsOnCommitTime(), tx1Obj.getReadsOnCommitTime());

      // The tranaction objects must be distinct.
      assertTrue(tx0Obj != tx1Obj);

      // The indices are the same reference.
      assertTrue(tx0Index == tx1Index);

      // The read-only views are not the same as the unisolated view.
      assertFalse(un == tx0Index);

    } finally {

      journal.destroy();
    }
  }

  /** Stress test for concurrent transactions against a single named index. */
  public void testStress() throws InterruptedException, ExecutionException {

    final int ntx = 30;
    final int nops = 10000;

    final Journal store = getStore();

    try {

      /*
       * Register the index. Each store can hold multiple named indices.
       */
      {
        final IndexMetadata indexMetadata = new IndexMetadata("testIndex", UUID.randomUUID());

      /*
       * Note: You MUST explicitly enable transaction processing for a
         * B+Tree when you register the index. Transaction processing
         * requires that the index maintain both per-tuple delete
         * markers and per-tuple version identifiers. While scale-out
         * indices always always maintain per-tuple delete markers,
         * neither local nor scale-out indices maintain the per-tuple
         * version identifiers by default.
         */
        indexMetadata.setIsolatable(true);

        // register the index.
        store.registerIndex(indexMetadata);

        // commit the store so the index is on record.
        store.commit();
      }

      /*
       * Run a set of concurrent tasks. Each task executes within its own
       * transaction. Conflicts between the transactions are increasingly
       * likely as the #of transactions or the #of operations per
       * transaction increases. When there is a conflict, the transaction
       * for which the conflict was detected will be aborted when it tries
       * to commit.
       */
      final List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();

      for (int i = 0; i < ntx; i++) {

        tasks.add(new ReadWriteTxTask(store, "testIndex", nops));
      }

      // run tasks on the journal's executor service.
      final List<Future<Void>> futures = store.getExecutorService().invokeAll(tasks);

      int i = 0;
      int nok = 0;
      for (Future<Void> future : futures) {

        // check for errors.
        try {
          future.get();
          nok++;
        } catch (ExecutionException ex) {
          if (InnerCause.isInnerCause(ex, ValidationError.class)) {
          /*
       * Normal exception. There was a conflict and one or the
             * transactions could not be committed.
             */
            System.out.println(
                "Note: task[" + i + "] could not be committed due to a write-write conflict.");
          } else {
            // Unexpected exception.
            throw ex;
          }
        }
        i++;
      }

      /*
       * Show #of transactions which committed successfully and the #of
       * transactions which were executed.
       */
      System.out.println(
          "" + nok + " out of " + tasks.size() + " transactions were committed successfully.");

      /*
       * Show the operations executed and #of tuples in the B+Tree.
       */
      System.out.println(
          "nops="
              + (nops * tasks.size())
              + ", rangeCount="
              + store.getIndex("testIndex").rangeCount());

      System.out.println(new Date().toString());

    } finally {

      // destroy the backing store.
      store.destroy();
    }
  }

  /*
   * Task performs random CRUD operations, range counts, and range scans isolated by a transaction
   * and commits when it is done.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  static class ReadWriteTxTask implements Callable<Void> {

    final Journal jnl;

    final String indexName;

    final int nops;

    final Random r = new Random();

    // #of distinct keys for this task.
    final int range = 1000;

    /*
     * @param jnl The journal.
     * @param indexName The name of the index.
     * @param nops The #of operations to execute against that {@link BTree}.
     */
    public ReadWriteTxTask(final Journal jnl, final String indexName, final int nops) {

      this.jnl = jnl;
      this.indexName = indexName;
      this.nops = nops;
    }

    /*
     * Starts a read-write transaction, obtains a view of the B+Tree isolated by the transaction,
     * performs a series of operations on the isolated view, and then commits the transaction.
     *
     * <p>Note: When multiple instances of this task are run concurrently it becomes increasingly
     * likely that a write-write conflict will be detected when you attempt to commit the
     * transaction, in which case the commit(txid) will fail and an appropriate error will be thrown
     * out of the task.
     *
     * @throws ValidationError if there is a write-write conflict during commit processing (the
     *     transaction write set conflicts with the write set of a concurrent transaction which has
     *     already successfully committed).
     * @throws Exception
     */
    public Void call() throws Exception {

      // Start a transaction.
      final long txid = jnl.newTx(ITx.UNISOLATED);

      try {

      /*
       * Obtain a view of the index isolated by the transaction.
         */
        final IIndex ndx = jnl.getIndex(indexName, txid);

        for (int i = 0; i < nops; i++) {

          switch (r.nextInt(4)) {
            case 0:
            /*
       * write on the index, inserting or updating the value
               * for the key.
               */
              ndx.insert("key#" + r.nextInt(range), r.nextLong());
              break;
            case 1:
              /* write on the index, removing the key iff found. */
              ndx.remove("key#" + r.nextInt(range));
              break;
            case 2:
            /*
       * lookup a key in the index.
               */
              ndx.lookup("key#" + r.nextInt(range));
              break;
            case 3:
            /*
       * range count the index.
               */
              ndx.rangeCount();
              break;
            case 4:
              {
              /*
       * run a range iterator over the index.
                 */
                final Iterator<ITuple<?>> itr = ndx.rangeIterator();
                while (itr.hasNext()) {
                  itr.next();
                }
                break;
              }
            default:
              throw new AssertionError("case not handled");
          }
        }

      } catch (Throwable t) {

        jnl.abort(txid);

        throw new RuntimeException(t);
      }

      /*
       * Commit the transaction. if the commit fails, then the transaction
       * is aborted.
       */
      jnl.commit(txid);

      // done.
      return null;
    }
  }
}
