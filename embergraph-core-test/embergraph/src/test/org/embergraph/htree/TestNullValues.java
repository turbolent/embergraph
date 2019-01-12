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

package org.embergraph.htree;

import java.io.IOException;
import org.embergraph.btree.data.IAbstractNodeDataCoder;
import org.embergraph.btree.raba.codec.IRabaCoder;
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
public class TestNullValues extends AbstractHTreeTestCase {

  /** */
  public TestNullValues() {}

  /** @param name */
  public TestNullValues(String name) {
    super(name);
  }

  private static final boolean bufferNodes = true;

  /*
   * Tests the ability to store a <code>null</code> in a tuple of a {@link HTree}, to reload the
   * {@link HTree} and find the <code>null</code> value still under the tuple.
   *
   * @throws IOException
   * @throws Exception
   */
  public void test_nullValues() throws IOException, Exception {

    final IRawStore store = new SimpleMemoryRawStore();

    try {

      HTree btree = getHTree(store, 3 /* addressBits */);

      final byte[] k1 = new byte[] {1};

      assertNull(btree.lookupFirst(k1));
      assertFalse(btree.contains(k1));

      assertNull(btree.insert(k1, null));

      assertNull(btree.lookupFirst(k1));
      assertTrue(btree.contains(k1));

      final long addrCheckpoint1 = btree.writeCheckpoint();

      btree = HTree.load(store, addrCheckpoint1, true /* readOnly */);

      assertNull(btree.lookupFirst(k1));
      assertTrue(btree.contains(k1));

    } finally {

      store.destroy();
    }
  }
}
