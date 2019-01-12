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
 * Created on Nov 17, 2006
 */
package org.embergraph.btree;

import org.embergraph.cache.IHardReferenceQueue;

/*
* Hard reference cache eviction listener writes a dirty node or leaf onto the persistence store.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class DefaultEvictionListener implements IEvictionListener {

  @Override
  public void evicted(final IHardReferenceQueue<PO> cache, final PO ref) {

    final AbstractNode<?> node = (AbstractNode<?>) ref;

    /*
     * Decrement the reference counter. When it reaches zero (0) we will
     * evict the node or leaf iff it is dirty.
     *
     * Note: The reference counts and the #of distinct nodes or leaves on
     * the writeRetentionQueue are not exact for a read-only B+Tree because
     * neither synchronization nor atomic counters are used to track that
     * information.
     */

    if (--node.referenceCount > 0) {

      return;
    }

    //        final AbstractBTree btree = node.btree;
    //
    //        final BTreeCounters counters = btree.getBtreeCounters();
    //
    //        counters.queueEvict.incrementAndGet();
    //
    //        if (--node.referenceCount > 0) {
    //
    //            return;
    //
    //        }
    //
    //        counters.queueEvictNoRef.incrementAndGet();

    doEviction(node);
  }

  private void doEviction(final AbstractNode<?> node) {

    final AbstractBTree btree = node.btree;

    if (btree.error != null) {
      /*
       * This occurs if an error was detected against a mutable view of the index (the unisolated
       * index view) and the caller has not discarded the index and caused it to be reloaded from
       * the most recent checkpoint.
       *
       * @see <a href="http://trac.blazegraph.com/ticket/1005">Invalidate BTree objects if error
       *     occurs during eviction </a>
       */
      throw new IllegalStateException(AbstractBTree.ERROR_ERROR_STATE, btree.error);
    }

    try {

      // Note: This assert can be violated for a read-only B+Tree since
      // there is less synchronization.
      assert btree.isReadOnly() || btree.ndistinctOnWriteRetentionQueue > 0;

      btree.ndistinctOnWriteRetentionQueue--;

      if (node.deleted) {

      /*
       * Deleted nodes are ignored as they are evicted from the queue.
         */

        return;
      }

      // this does not permit transient nodes to be coded.
      if (node.dirty && btree.store != null) {
        // // this causes transient nodes to be coded on eviction.
        // if (node.dirty) {

        //		        counters.queueEvictDirty.incrementAndGet();

        if (node.isLeaf()) {

        /*
       * A leaf is written out directly.
           */

          btree.writeNodeOrLeaf(node);

        } else {

        /*
       * A non-leaf node must be written out using a post-order
           * traversal so that all dirty children are written through
           * before the dirty parent. This is required in order to
           * assign persistent identifiers to the dirty children.
           */

          btree.writeNodeRecursive(node);
        }

        // is a coded data record.
        assert node.isCoded();

        // no longer dirty.
        assert !node.dirty;

        assert btree.store == null || node.identity != PO.NULL;
      } // isDirty

      // This does not insert into the cache.  That is handled by writeNodeOrLeaf.
      //	        if (btree.globalLRU != null) {
      //
      //	            /*
      //	             * Add the INodeData or ILeafData object to the global LRU, NOT the
      //	             * Node or Leaf.
      //	             *
      //	             * Note: The global LRU touch only occurs on eviction from the write
      //	             * retention queue. This is nice because it limits the touches on
      //	             * the global LRU, which could otherwise be a hot spot. We do a
      //	             * touch whether or not the node was persisted since we are likely
      //	             * to return to the node in either case.
      //	             */
      //
      //	            final IAbstractNodeData delegate = node.getDelegate();
      //
      //	            assert delegate != null : node.toString();
      //
      //	            assert delegate.isCoded() : node.toString();
      //
      //	            btree.globalLRU.add(delegate);
      //
      //	        }

    } catch (Throwable e) {

      if (!btree.readOnly) {

      /*
       * If the btree is mutable and an eviction fails, then the index MUST be discarded.
         *
         * @see <a href="http://trac.blazegraph.com/ticket/1005">Invalidate BTree objects if error
         *     occurs during eviction </a>
         */
        btree.error = e;

        // Throw as Error.
        throw new EvictionError(e);
      }

      // Launder the throwable.
      if (e instanceof RuntimeException) throw (RuntimeException) e;

      throw new RuntimeException(e);
    }
  }
}
