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
 * Created on Feb 16, 2007
 */

package org.embergraph.journal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.UUID;
import org.embergraph.btree.BTree;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.rwstore.IRWStrategy;
import org.embergraph.service.AbstractTransactionService;

/*
 * Test the ability to get (exact match) and find (most recent less than or equal to) historical
 * commit records in a {@link Journal}. Also verifies that a canonicalizing cache is maintained (you
 * never obtain distinct concurrent instances of the same commit record).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestCommitHistory extends ProxyTestCase<Journal> {

  /** */
  public TestCommitHistory() {}

  /** @param name */
  public TestCommitHistory(String name) {
    super(name);
  }

  /*
   * Compare two {@link ICommitRecord}s for equality in their data.
   *
   * @param expected
   * @param actual
   */
  public void assertEquals(ICommitRecord expected, ICommitRecord actual) {

    if (expected == null) assertNull("Expected actual to be null", actual);
    else assertNotNull("Expected actual to be non-null", actual);

    assertEquals("timestamp", expected.getTimestamp(), actual.getTimestamp());

    assertEquals("#roots", expected.getRootAddrCount(), actual.getRootAddrCount());

    final int n = expected.getRootAddrCount();

    for (int i = 0; i < n; i++) {

      if (expected.getRootAddr(i) != actual.getRootAddr(i)) {

        assertEquals("rootAddr[" + i + "]", expected.getRootAddr(i), actual.getRootAddr(i));
      }
    }
  }

  /*
   * Test that {@link Journal#getCommitRecord(long)} returns null if invoked before anything has
   * been committed.
   *
   * @throws IOException
   */
  public void test_behaviorBeforeAnythingIsCommitted() throws IOException {

    final Journal journal = new Journal(getProperties());

    try {

      assertNull(journal.getCommitRecord(journal.getLocalTransactionManager().nextTimestamp()));

    } finally {

      journal.destroy();
    }
  }

  /** Test the ability to recover a {@link ICommitRecord} from the {@link CommitRecordIndex}. */
  public void test_recoverCommitRecord() {
    final Properties properties = getProperties();
    // Set a release age for RWStore if required
    properties.setProperty(AbstractTransactionService.Options.MIN_RELEASE_AGE, "5000");

    final Journal journal = new Journal(properties);

    try {

      /*
       * The first commit flushes the root leaves of some indices so we
       * get back a non-zero commit timestamp.
       */
      assertTrue(0L != journal.commit());

      /*
       * A follow up commit in which nothing has been written should
       * return a 0L timestamp.
       */
      assertEquals(0L, journal.commit());

      journal.write(ByteBuffer.wrap(new byte[] {1, 2, 3}));

      final long commitTime1 = journal.commit();

      assertTrue(commitTime1 != 0L);

      ICommitRecord commitRecord = journal.getCommitRecord(commitTime1);

      assertNotNull(commitRecord);

      assertNotNull(journal.getCommitRecord());

      assertEquals(commitTime1, journal.getCommitRecord().getTimestamp());

      assertEquals(journal.getCommitRecord(), commitRecord);

    } finally {

      journal.destroy();
    }
  }
  /*
   * Test the ability to recover a {@link ICommitRecord} from the {@link CommitRecordIndex}.
   *
   * <p>A second commit should be void and therefore the previous record should be retrievable.
   */
  public void test_recoverCommitRecordNoHistory() {
    final Properties properties = getProperties();
    // Set a release age for RWStore if required
    properties.setProperty(AbstractTransactionService.Options.MIN_RELEASE_AGE, "0");

    final Journal journal = new Journal(properties);

    try {

      /*
       * The first commit flushes the root leaves of some indices so we
       * get back a non-zero commit timestamp.
       */
      assertTrue(0L != journal.commit());

      /*
       * A follow up commit in which nothing has been written should
       * return a 0L timestamp.
       */
      assertEquals(0L, journal.commit());

      journal.write(ByteBuffer.wrap(new byte[] {1, 2, 3}));

      final long commitTime1 = journal.commit();

      assertTrue(commitTime1 != 0L);

      ICommitRecord commitRecord = journal.getCommitRecord(commitTime1);

      assertNotNull(commitRecord);

      assertNotNull(journal.getCommitRecord());

      assertEquals(commitTime1, journal.getCommitRecord().getTimestamp());

      assertEquals(journal.getCommitRecord(), commitRecord);

    } finally {

      journal.destroy();
    }
  }

  /** Tests whether the {@link CommitRecordIndex} is restart-safe. */
  public void test_commitRecordIndex_restartSafe() {

    final Properties properties = getProperties();
    // Set a release age for RWStore if required
    properties.setProperty(AbstractTransactionService.Options.MIN_RELEASE_AGE, "5000");

    Journal journal = new Journal(properties);

    try {

      if (!journal.isStable()) {

        // test only applies to restart-safe journals.
        return;
      }

      /*
       * Write a record directly on the store in order to force a commit
       * to write a commit record (if you write directly on the store it
       * will not cause a state change in the root addresses, but it will
       * cause a new commit record to be written with a new timestamp).
       */

      // write some data.
      journal.write(ByteBuffer.wrap(new byte[] {1, 2, 3}));

      // commit the store.
      final long commitTime1 = journal.commit();

      assertTrue(commitTime1 != 0L);

      ICommitRecord commitRecord1 = journal.getCommitRecord(commitTime1);

      assertEquals(commitTime1, commitRecord1.getTimestamp());

      assertEquals(commitTime1, journal.getRootBlockView().getLastCommitTime());

      /*
       * Close and then re-open the store and verify that the correct
       * commit record is returned.
       */
      journal = reopenStore(journal);

      ICommitRecord commitRecord2 = journal.getCommitRecord();

      assertEquals(commitRecord1, commitRecord2);

      /*
       * Now recover the commit record by searching the commit record
       * index.
       */
      ICommitRecord commitRecord3 = journal.getCommitRecord(commitTime1);

      assertEquals(commitRecord1, commitRecord3);
      assertEquals(commitRecord2, commitRecord3);

    } finally {

      journal.destroy();
    }
  }

  /*
   * Tests for finding (less than or equal to) historical commit records using the commit record
   * index. This also tests restart-safety of the index with multiple records (if the store is
   * stable).
   *
   * <p>The minReleaseAge property has been added to test historical data protection, and not just
   * the retention of the CommitRecords which currently are erroneously never removed.
   *
   * @throws IOException
   */
  public void test_commitRecordIndex_find() throws IOException {

    final Properties props = getProperties();
    props.setProperty(
        "org.embergraph.service.AbstractTransactionService.minReleaseAge", "2000"); // 2 seconds
    Journal journal = new Journal(props);

    try {

      final int limit = 10;

      final long[] commitTime = new long[limit];

      final long[] commitRecordIndexAddrs = new long[limit];

      final long[] dataRecordAddrs = new long[limit];
      final ByteBuffer[] dataRecords = new ByteBuffer[limit];

      final ICommitRecord[] commitRecords = new ICommitRecord[limit];

      for (int i = 0; i < limit; i++) {

        // write some data, this should be protected by minReleaseAge
        dataRecords[i] = ByteBuffer.wrap(new byte[] {1, 2, 3, (byte) i});
        dataRecordAddrs[i] = journal.write(dataRecords[i]);
        dataRecords[i].flip();
        if (i > 0) {
          journal.delete(dataRecordAddrs[i - 1]); // remove previous committed data
        }

        // commit the store.
        commitTime[i] = journal.commit();

        assertTrue(commitTime[i] != 0L);

        if (i > 0) assertTrue(commitTime[i] > commitTime[i - 1]);

        commitRecordIndexAddrs[i] = journal.getRootBlockView().getCommitRecordIndexAddr();

        assertTrue(commitRecordIndexAddrs[i] != 0L);

        final IBufferStrategy strat = journal.getBufferStrategy();
        if ((!(strat instanceof IRWStrategy)) && i > 0)
          assertTrue(commitRecordIndexAddrs[i] > commitRecordIndexAddrs[i - 1]);

        // get the current commit record.
        commitRecords[i] = journal.getCommitRecord();

        // test exact match on this timestamp.
        assertEquals(commitRecords[i], journal.getCommitRecord(commitTime[i]));

        if (i > 0) {

          // test exact match on the prior timestamp.
          assertEquals(commitRecords[i - 1], journal.getCommitRecord(commitTime[i - 1]));
        }

        /*
         * Obtain a unique timestamp from the same source that the journal
         * is using to generate the commit timestamps. This ensures that
         * there will be at least one possible timestamp between each commit
         * timestamp.
         */
        final long ts = journal.getLocalTransactionManager().nextTimestamp();

        assertTrue(ts > commitTime[i]);
      }

      if (journal.isStable()) {

        /*
         * Close and then re-open the store so that we will also be testing
         * restart-safety of the commit record index.
         */

        journal = reopenStore(journal);
      }

      /*
       * Verify the historical commit records on exact match (get).
       */
      {
        for (int i = 0; i < limit; i++) {

          assertEquals(commitRecords[i], journal.getCommitRecord(commitTime[i]));

          final ByteBuffer rdbuf = journal.read(dataRecordAddrs[i]);
          assertTrue(dataRecords[i].compareTo(rdbuf) == 0);
        }
      }

      /*
       * Verify access to historical records on LTE search (find).
       *
       * We ensured above that there is at least one possible timestamp value
       * between each pair of commit timestamps. We already verified that
       * timestamps that exactly match a known commit time return the
       * associated commit record.
       *
       * Now we verify that timestamps which proceed a known commit time but
       * follow after any earlier commit time, return the proceeding commit
       * record (finds the most recent commit record having a commit time less
       * than or equal to the probe time).
       */

      {
        for (int i = 1; i < limit; i++) {

          assertEquals(commitRecords[i - 1], journal.getCommitRecord(commitTime[i] - 1));
        }

        /*
         * Verify a null return if we probe with a timestamp before any
         * commit time.
         */
        assertNull(journal.getCommitRecord(commitTime[0] - 1));
      }

    } finally {

      journal.destroy();
    }
  }

  /*
   * Test verifies that exact match and find always return the same reference for the same commit
   * record (at least as long as the test holds a hard reference to the commit record of interest).
   */
  public void test_canonicalizingCache() {
    final Properties properties = getProperties();
    // Set a release age for RWStore if required
    properties.setProperty(AbstractTransactionService.Options.MIN_RELEASE_AGE, "5000");

    final Journal journal = new Journal(properties);

    try {

      /*
       * The first commit flushes the root leaves of some indices so we get
       * back a non-zero commit timestamp.
       */
      final long commitTime0 = journal.commit();

      assertTrue(commitTime0 != 0L);

      /*
       * obtain the commit record for that commit timestamp.
       */
      final ICommitRecord commitRecord0 = journal.getCommitRecord(commitTime0);

      // should be the same data that is held by the journal.
      assertEquals(commitRecord0, journal.getCommitRecord());

      /*
       * write a record on the store, commit the store, and note the commit
       * time.
       */
      journal.write(ByteBuffer.wrap(new byte[] {1, 2, 3}));

      final long commitTime1 = journal.commit();

      assertTrue(commitTime1 != 0L);

      /*
       * obtain the commit record for that commit timestamp.
       */
      final ICommitRecord commitRecord1 = journal.getCommitRecord(commitTime1);

      // should be the same data that is held by the journal.
      assertEquals(commitRecord1, journal.getCommitRecord());

      /*
       * verify that we obtain the same instance with find as with an exact
       * match.
       */

      assertTrue(commitRecord0 == journal.getCommitRecord(commitTime1 - 1));

      assertTrue(commitRecord1 == journal.getCommitRecord(commitTime1 + 0));

      assertTrue(commitRecord1 == journal.getCommitRecord(commitTime1 + 1));

    } finally {

      journal.destroy();
    }
  }

  /*
   * Test of the canonicalizing object cache used to prevent distinct instances of a historical
   * index from being created. The test also verifies that the historical named index is NOT the
   * same instance as the current unisolated index by that name.
   */
  public void test_objectCache() {

    final Journal journal = new Journal(getProperties());

    try {

      assertEquals("commitCounter", 0, journal.getCommitRecord().getCommitCounter());

      final String name = "abc";

      /*
       * register an index and commit the journal.
       */

      final IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

      final BTree liveIndex = journal.registerIndex(name, md);

      journal.commit();

      assertEquals("commitCounter", 1, journal.getCommitRecord().getCommitCounter());

      final long commitTime0 = journal.getCommitRecord().getTimestamp();

      assertNotSame(commitTime0, 0L);
      assertTrue(commitTime0 > 0L);

      /*
       * obtain the commit record for that commit timestamp.
       */
      final ICommitRecord commitRecord0 = journal.getCommitRecord(commitTime0);

      // should be the same data that is held by the journal.
      assertEquals(commitRecord0, journal.getCommitRecord());

      /*
       * verify that a request for last committed state the named index
       * returns a different instance than the "live" index.
       */

      final BTree historicalIndex0 = (BTree) journal.getIndexWithCommitRecord(name, commitRecord0);

      assertTrue(liveIndex != historicalIndex0);

      // re-request is still the same object.
      assertTrue(historicalIndex0 == journal.getIndexWithCommitRecord(name, commitRecord0));

      /*
       * The re-load address for the live index as of that commit record.
       */
      final long liveIndexAddr0 = liveIndex.getCheckpoint().getCheckpointAddr();

      /*
       * write a record on the store, commit the store, and note the
       * commit time.
       *
       * Note: This is a raw write on the store, not a write on an index,
       * so we have to do an explicit commit.
       */

      journal.write(ByteBuffer.wrap(new byte[] {1, 2, 3}));

      journal.commit();

      assertEquals("commitCounter", 2, journal.getCommitRecord().getCommitCounter());

      final long commitTime1 = journal.getCommitRecord().getTimestamp();

      assertTrue(commitTime1 > commitTime0);

      /*
       * we did NOT write on the named index, so its address in the store
       * must not change.
       */
      assertEquals(liveIndexAddr0, liveIndex.getCheckpoint().getCheckpointAddr());

      // obtain the commit record for that commit timestamp.
      final ICommitRecord commitRecord1 = journal.getCommitRecord(commitTime1);

      // should be the same data.
      assertEquals(commitRecord1, journal.getCommitRecord());

      /*
       * verify that we get the same historical index object for the new
       * commit record since the index state was not changed and it will
       * be reloaded from the same address.
       */
      assertTrue(historicalIndex0 == journal.getIndexWithCommitRecord(name, commitRecord1));

      // re-request is still the same object.
      assertTrue(historicalIndex0 == journal.getIndexWithCommitRecord(name, commitRecord0));

      // re-request is still the same object.
      assertTrue(historicalIndex0 == journal.getIndexWithCommitRecord(name, commitRecord1));

      /*
       * Now write on the live index and commit. verify that there is a
       * new historical index available for the new commit record, that it
       * is not the same as the live index, and that it is not the same as
       * the previous historical index (which should still be accessible).
       */

      // live index is the same reference.
      assertTrue(liveIndex == journal.getIndex(name));

      liveIndex.insert(new byte[] {1, 2}, new byte[] {1, 2});

      // do an explicit commit since we are not running a write task.
      journal.commit();

      assertEquals("commitCounter", 3, journal.getCommitRecord().getCommitCounter());

      final long commitTime2 = journal.getCommitRecord().getTimestamp();

      assertTrue(commitTime2 > commitTime1);

      // obtain the commit record for that commit timestamp.
      final ICommitRecord commitRecord2 = journal.getCommitRecord(commitTime2);

      // should be the same instance that is held by the journal.
      assertEquals(commitRecord2, journal.getCommitRecord());

      // must be a different index object.

      BTree historicalIndex2 = (BTree) journal.getIndexWithCommitRecord(name, commitRecord2);

      assertTrue(historicalIndex0 != historicalIndex2);

      // the live index must be distinct from the historical index.
      assertTrue(liveIndex != historicalIndex2);

    } finally {

      journal.destroy();
    }
  }
}
