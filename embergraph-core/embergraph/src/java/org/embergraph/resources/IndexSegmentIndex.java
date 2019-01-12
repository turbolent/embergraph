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
package org.embergraph.resources;

import java.util.UUID;
import org.embergraph.btree.BTree;
import org.embergraph.btree.Checkpoint;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.IndexSegmentStore;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.io.SerializerUtil;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.mdi.SegmentMetadata;
import org.embergraph.rawstore.IRawStore;
import org.embergraph.util.Bytes;

/**
 * {@link BTree} mapping {@link IndexSegmentStore} <em>createTime</em>s to {@link IResourceMetadata}
 * records. The keys are the long integers (commitTimes) followed by the index segment UUID to break
 * ties (this is not the scale-out index UUID, but the UUID of the specific index segment). The
 * values are {@link IResourceMetadata} objects.
 *
 * <p>Note: Access to this object MUST be synchronized.
 *
 * <p>Note: This is used as a transient data structure that is populated from the file system by the
 * {@link ResourceManager}.
 */
public class IndexSegmentIndex extends BTree {

  /** Instance used to encode the timestamp into the key. */
  private final IKeyBuilder keyBuilder = new KeyBuilder(Bytes.SIZEOF_LONG + Bytes.SIZEOF_UUID);

  /**
   * Create a transient instance.
   *
   * @return The new instance.
   */
  public static IndexSegmentIndex createTransient() {

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    metadata.setBTreeClassName(IndexSegmentIndex.class.getName());

    return (IndexSegmentIndex) BTree.createTransient(metadata);
  }

  /**
   * Load from the store.
   *
   * @param store The backing store.
   * @param checkpoint The {@link Checkpoint} record.
   * @param metadata The metadata record for the index.
   */
  public IndexSegmentIndex(
      IRawStore store, Checkpoint checkpoint, IndexMetadata metadata, boolean readOnly) {

    super(store, checkpoint, metadata, readOnly);
  }

  /**
   * Encodes the commit time into a key.
   *
   * @param commitTime The commit time.
   * @param The UUID of the resource.
   * @return The corresponding key.
   */
  protected byte[] getKey(long commitTime, UUID uuid) {

    return keyBuilder.reset().append(commitTime).append(uuid).getKey();
  }

  /**
   * Add an entry under the commitTime and resource UUID associated with the {@link
   * IResourceMetadata} record.
   *
   * @param resourceMetadata The {@link IResourceMetadata} record.
   * @exception IllegalArgumentException if <i>commitTime</i> is <code>0L</code>.
   * @exception IllegalArgumentException if <i>resourceMetadata</i> is <code>null</code>.
   * @exception IllegalArgumentException if there is already an entry registered under for the given
   *     timestamp.
   */
  public synchronized void add(final SegmentMetadata resourceMetadata) {

    if (resourceMetadata == null) throw new IllegalArgumentException();

    assert resourceMetadata.isIndexSegment();

    final long createTime = resourceMetadata.getCreateTime();

    if (createTime == 0L) throw new IllegalArgumentException();

    final byte[] key = getKey(createTime, resourceMetadata.getUUID());

    if (super.contains(key)) {

      throw new IllegalArgumentException("entry exists: timestamp=" + createTime);
    }

    // add a serialized entry to the persistent index.
    super.insert(key, SerializerUtil.serialize(resourceMetadata));
  }
}
