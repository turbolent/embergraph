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
 * Created on Feb 5, 2010
 */

package org.embergraph.btree.isolation;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import org.embergraph.btree.AbstractBTreeTestCase;
import org.embergraph.btree.BTree;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.journal.Options;
import org.embergraph.journal.ValidationError;

/**
 * This is a test suite for mixing full transactions with unisolated operations on the same indices.
 * These two classes of operations can be interleaved as long as the unisolated operations make
 * appropriate assignment of revision timestamps to the tuples in the B+Tree.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestMixedModeOperations extends AbstractBTreeTestCase {

  /** */
  public TestMixedModeOperations() {}

  /** @param name */
  public TestMixedModeOperations(String name) {
    super(name);
  }

  public void test_mixedOps() throws IOException {

    final Properties properties = new Properties();

    properties.setProperty(Options.BUFFER_MODE, BufferMode.Transient.toString());

    final Journal journal = new Journal(properties);

    final String name = "test";

    try {

      /*
       * Register the index.
       */
      {
        final IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

        md.setIsolatable(true);

        final BTree ndx = BTree.create(journal, md);

        journal.registerIndex(name, ndx);
      }

      /*
       * The commit time associated with the initial registration of the
       * BTree.
       */
      final long t0 = journal.commit();

      if (log.isInfoEnabled()) log.info("t0=" + t0);

      /*
       * Run a transaction which writes a tuple onto the index and
       * commits.
       */
      // The commit time for that transaction.
      final long t1;
      {
        // start a transaction.
        final long tx1Start = journal.newTx(ITx.UNISOLATED);

        if (log.isInfoEnabled()) log.info("tx1Start=" + tx1Start);

        // a view of the B+Tree isolated by tx1.
        final ILocalBTreeView tx1View = journal.getIndex(name, tx1Start);

        tx1View.insert(new byte[] {2}, new byte[] {2});

        t1 = journal.commit(tx1Start);

        if (log.isInfoEnabled()) log.info("t1=" + t1);
      }

      /*
       * Verify that the tuple written by tx1 is now visible in the
       * unisolated index and that the current revision timestamp for
       * unisolated writes is GT the lastCommitTime on the journal.
       */
      {
        final ILocalBTreeView ndx = journal.getIndex(name, ITx.UNISOLATED);

        assertEquals(new byte[] {2}, ndx.lookup(new byte[] {2}));

        final long revisionTimestamp = ((BTree) ndx).getRevisionTimestamp();

        if (log.isInfoEnabled()) log.info("unisolated revisionTimestamp=" + revisionTimestamp);

        assertTrue(revisionTimestamp > t1);
      }

      /*
       * Verify that an intervening unisolated write will cause a tx to
       * fail validation if there is a conflict on a specific tuple.
       */
      {
        // start a transaction and write on a tuple.
        final long tx2Start = journal.newTx(ITx.UNISOLATED);
        {
          if (log.isInfoEnabled()) log.info("tx2Start=" + tx2Start);

          final ILocalBTreeView ndx = journal.getIndex(name, tx2Start);

          ndx.insert(new byte[] {3}, new byte[] {3});
        }
        // run an unisolated operation that updates the same tuple and
        // commit.
        final long t2;
        {
          final ILocalBTreeView ndx = journal.getIndex(name, ITx.UNISOLATED);

          ndx.insert(new byte[] {3}, new byte[] {3});

          t2 = journal.commit();

          if (log.isInfoEnabled()) log.info("t2=" + t2);

          // Verify that the revision timestamp was advanced.
          {
            final long revisionTimestamp = ((BTree) ndx).getRevisionTimestamp();

            if (log.isInfoEnabled()) log.info("unisolated revisionTimestamp=" + revisionTimestamp);

            assertTrue(revisionTimestamp > t2);
          }
        }

        // attempt to commit the transaction (this should fail).
        try {

          journal.commit(tx2Start);

          fail("Expecting: " + ValidationError.class);

        } catch (ValidationError ex) {

          if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
        }
      }

    } finally {

      journal.destroy();
    }
  }
}
