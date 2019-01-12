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
 * Created on Oct 4, 2010
 */

package org.embergraph.rdf.spo;

import java.util.Random;
import java.util.UUID;
import org.embergraph.btree.BTree;
import org.embergraph.btree.FixedLengthPrefixSplits;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.IndexSegment;
import org.embergraph.btree.IndexSegmentBuilder;
import org.embergraph.btree.IndexSegmentStore;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.journal.IJournal;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.resources.AbstractTestSegSplitter;
import org.embergraph.resources.SplitUtility;
import org.embergraph.service.Split;
import org.embergraph.test.MockTermIdFactory;
import org.embergraph.util.BytesUtil;

/*
* Test suite for the {@link XXXCShardSplitHandler}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestXXXCShardSplitHandler extends AbstractTestSegSplitter {

  /** */
  public TestXXXCShardSplitHandler() {}

  /** @param name */
  public TestXXXCShardSplitHandler(String name) {
    super(name);
  }

  private MockTermIdFactory factory;

  protected void setUp() throws Exception {

    super.setUp();

    factory = new MockTermIdFactory();
  }

  protected void tearDown() throws Exception {

    super.tearDown();

    factory = null;
  }

  /*
   * Register a {@link BTree} against the journal, generate some data and commit the data.
   *
   * <p>Note: this uses {@link IV}[4] values to generate the keys, which is just like the statement
   * indices for a quads store with inlining enabled. If inlining is not enabled, then you can use
   * {@link FixedLengthPrefixSplits} which has its own test suite.
   *
   * @param store
   * @param ntuples
   * @param pmd
   * @param splitHandler
   */
  @SuppressWarnings("unchecked")
  private BTree generateQuadsData(
      final IJournal store,
      final String name,
      final int ntuples,
      final LocalPartitionMetadata pmd) {

    assert store != null;
    assert pmd != null;

    final byte[] fromKey = pmd.getLeftSeparatorKey();
    //        final byte[] toKey = pmd.getRightSeparatorKey();
    assert fromKey != null;
    //        assert toKey != null;

    final BTree btree;
    {
      final IndexMetadata md = new IndexMetadata(UUID.randomUUID());

      md.setPartitionMetadata(pmd);

      btree = (BTree) store.registerIndex(name, md);
    }

    final Random r = new Random();

    // bias to a small set of triples with a large number of contexts.
    final IV[] s = new IV[Math.max(ntuples / 200, 4)];
    final IV[] p = new IV[Math.max(ntuples / 500, 2)];
    final IV[] o = new IV[Math.max(ntuples / 400, 5)];
    final IV[] c = new IV[Math.max(ntuples / 10, 200)];

    if (log.isInfoEnabled())
      log.info(
          "ntuples="
              + ntuples
              + ", #s="
              + s.length
              + ", #p="
              + p.length
              + ", #o="
              + o.length
              + ", #c="
              + c.length);

    long v = 0;
    for (int i = 0; i < s.length; i++) {
      s[i] = factory.newTermId(VTE.URI, (int) (v = v + r.nextInt(100)));
    }
    for (int i = 0; i < p.length; i++) {
      p[i] = factory.newTermId(VTE.URI, (int) (v = v + r.nextInt(100)));
    }
    for (int i = 0; i < o.length; i++) {
      o[i] = factory.newTermId(VTE.URI, (int) (v = v + r.nextInt(100)));
    }
    for (int i = 0; i < c.length; i++) {
      c[i] = factory.newTermId(VTE.URI, (int) (v = v + r.nextInt(100)));
    }

    final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

    int ninsert = 0;
    int ntries = 0;
    while (ninsert < ntuples) {

      final IV S = s[r.nextInt(s.length)];
      final IV P = p[r.nextInt(p.length)];
      final IV O = o[r.nextInt(o.length)];

      // Nested loop for lots of Cs per triple.
      for (int i = 0; i < r.nextInt(10) + 10 && ninsert < ntuples; i++) {

        final IV C = c[r.nextInt(c.length)];

        final ISPO spo = new SPO(S, P, O, C, StatementEnum.Explicit);

        final byte[] key = SPOKeyOrder.SPOC.encodeKey(keyBuilder, spo);

        if (!btree.contains(key)) {

          btree.insert(key, null /* val */);

          // System.err.println(spo.toString());

          ninsert++;
        }
      }

      ntries++;

      if (ntries > ntuples * 4) {

        throw new RuntimeException(
            "Test setup is not terminating: ntuples="
                + ntuples
                + ", ntries="
                + ntries
                + ", ninsert="
                + ninsert);
      }
    }

    // verify generated correct #of tuples.
    assertEquals(ntuples, btree.getEntryCount());

    store.commit();

    // return view with lastCommitTime set.
    return (BTree) store.getIndex(name);
  }

  /** Unit test for {@link XXXCShardSplitHandler}. */
  public void test_xxxCShardSplitHandler() throws Exception {

    /*
     * Test parameters.
     */
    final byte[] fromKey = new byte[0];
    final byte[] toKey = null;

    final int ntuples = 10000;

    IndexSegmentBuilder builder = null;
    final IJournal store = getStore();

    try {

      final LocalPartitionMetadata pmd =
          new LocalPartitionMetadata(
              pidFactory.nextPartitionId(getName()),
              -1, // sourcePartitionId
              fromKey,
              toKey,
              new IResourceMetadata[] {store.getResourceMetadata()},
              null // cause
              //                    ,null // history
              );

      // Generates BTree w/ constrained keys and commits to store.
      final BTree src = generateQuadsData(store, getName(), ntuples, pmd);

      // Build the index segment (a compacting merge).
      builder = doBuild(getName(), src, src.getLastCommitTime(), fromKey, toKey);

      final IndexSegmentStore segStore = new IndexSegmentStore(builder.outFile);

      /*
       * Test ability to create two splits from the data when the split
       * handler accepts anything.
       */
      try {

        final int expectedSplitCount = 2;

        final long nominalShardSize = (long) (segStore.size() / (expectedSplitCount / 2.));

        final IndexSegment seg = segStore.loadIndexSegment();

        // Compute splits.
        final Split[] splits =
            SplitUtility.getSplits(pidFactory, pmd, seg, nominalShardSize, acceptAllSplits);

        // Validate splits.
        SplitUtility.validateSplits(pmd, splits, true /* checkStuff */);

        assertEquals("#splits", expectedSplitCount, splits.length);

      } finally {

        segStore.close();
      }

      /*
       * Test ability to create two splits when the split handler is
       * constrained to only accept an 8 byte prefix boundary.
       */
      try {

        final int expectedSplitCount = 2;

        final long nominalShardSize = (long) (segStore.size() / (expectedSplitCount / 2.));

        final IndexSegment seg = segStore.loadIndexSegment();

        // Compute splits.
        final Split[] splits =
            SplitUtility.getSplits(
                pidFactory, pmd, seg, nominalShardSize, new XXXCShardSplitHandler());

        // Validate splits.
        SplitUtility.validateSplits(pmd, splits, true /* checkStuff */);

        assertEquals("#splits", expectedSplitCount, splits.length);

        // the separator key between the two splits.
        final byte[] separatorKey = splits[0].pmd.getRightSeparatorKey();

        // verify the prefix non-null.
        assertNotNull(separatorKey);

        {

          // decode the separator key as an IV[3] array.
          @SuppressWarnings("unchecked")
          final IV[] terms = IVUtility.decode(separatorKey, 3 /* nterms */);

          // encode the first three components from the IV[].
          final IKeyBuilder keyBuilder = new KeyBuilder();
          IVUtility.encode(keyBuilder, terms[0]);
          IVUtility.encode(keyBuilder, terms[1]);
          IVUtility.encode(keyBuilder, terms[2]);
          final byte[] tmp = keyBuilder.getKey();

          // verify that the separator key exactly codes for a triple.
          assertTrue(BytesUtil.bytesEqual(separatorKey, tmp));
        }

      /*
       * Lookup the indexOf the prefix in the source B+Tree. For this
         * unit test (and for the quad store indices) it will be an
         * insertion point (the separator key is a triple but the index
         * holds quads so the separator key will not be in the index).
         *
         * Note: We could get this right by chance with a NOP split
         * handler. However, the odds are against it and this test will
         * nearly always correctly fail a broken split handler.
         */

        // Get insertion point (there are no 8-byte keys in the ndx).
        final long pos = seg.indexOf(separatorKey);
        assertTrue(pos < 0);

        // Convert to a tuple index.
        final long index = -(pos) - 1;

        if (log.isInfoEnabled()) log.info("index=" + index);

      } finally {

        segStore.close();
      }

    } finally {

      if (builder != null) {

        // delete the generated index segment.
        if (!builder.outFile.delete()) {

          log.warn("Could not delete: " + builder.outFile);
        }
      }

      store.destroy();
    }
  }
}
