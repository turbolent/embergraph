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
 * Created on May 5, 2009
 */

package org.embergraph.btree;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.rawstore.SimpleMemoryRawStore;

/*
 * Test of storing null values under a key with persistence.
 *
 * <p>Note that the stress tests for the {@link IRabaCoder}s and the {@link IAbstractNodeDataCoder}s
 * already test the ability to encode and decode with nulls, delete markers, and version timestamps.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestNullValues extends AbstractBTreeTestCase {

  /** */
  public TestNullValues() {}

  /** @param name */
  public TestNullValues(String name) {
    super(name);
  }

  private static final boolean bufferNodes = true;

  /*
   * Tests the ability to store a <code>null</code> in a tuple of a {@link BTree}, to reload the
   * {@link BTree} and find the <code>null</code> value still under the tuple, and to build an
   * {@link IndexSegmentBuilder} from the {@link BTree} and find the <code>null</code> value under
   * the tuple.
   *
   * @throws IOException
   * @throws Exception
   */
  public void test_nullValues() throws Exception {

    final IRawStore store = new SimpleMemoryRawStore();

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    BTree btree = BTree.create(store, metadata);

    final byte[] k1 = new byte[] {1};

    assertNull(btree.lookup(k1));
    assertFalse(btree.contains(k1));

    assertNull(btree.insert(k1, null));

    assertNull(btree.lookup(k1));
    assertTrue(btree.contains(k1));

    final long addrCheckpoint1 = btree.writeCheckpoint();

    btree = BTree.load(store, addrCheckpoint1, true /*readOnly*/);

    assertNull(btree.lookup(k1));
    assertTrue(btree.contains(k1));

    File outFile = null;
    File tmpDir = null;

    try {

      outFile = new File(getName() + ".seg");

      if (outFile.exists() && !outFile.delete()) {

        throw new RuntimeException("Could not delete file: " + outFile);
      }

      tmpDir = outFile.getAbsoluteFile().getParentFile();

      final long commitTime = System.currentTimeMillis();

      @SuppressWarnings("unused")
      final IndexSegmentCheckpoint checkpoint =
          IndexSegmentBuilder.newInstance(
                  outFile,
                  tmpDir,
                  btree.getEntryCount(),
                  btree.rangeIterator(),
                  3 /* m */,
                  btree.getIndexMetadata(),
                  commitTime,
                  true /* compactingMerge */,
                  bufferNodes)
              .call();

      //          @see BLZG-1501 (remove LRUNexus)
      //            if (LRUNexus.INSTANCE != null) {
      //
      //                /*
      //                 * Clear the records for the index segment from the cache so we will
      //                 * read directly from the file. This is necessary to ensure that the
      //                 * data on the file is good rather than just the data in the cache.
      //                 */
      //
      //                LRUNexus.INSTANCE.deleteCache(checkpoint.segmentUUID);
      //
      //            }

      /*
       * Verify can load the index file and that the metadata associated
       * with the index file is correct (we are only checking those
       * aspects that are easily defined by the test case and not, for
       * example, those aspects that depend on the specifics of the length
       * of serialized nodes or leaves).
       */

      final IndexSegmentStore segStore = new IndexSegmentStore(outFile);

      final IndexSegment seg = segStore.loadIndexSegment();

      try {

        assertNull(seg.lookup(k1));
        assertTrue(seg.contains(k1));

      } finally {

        seg.close();
      }

    } finally {

      if (outFile != null && outFile.exists() && !outFile.delete()) {

        log.warn("Could not delete file: " + outFile);
      }
    }
  }
}
