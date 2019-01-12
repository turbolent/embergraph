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
 * Created on Oct 3, 2008
 */

package org.embergraph.btree;

import cutthecrap.utils.striterators.IFilter;
import org.embergraph.btree.proc.AbstractKeyArrayIndexProcedureConstructor;
import org.embergraph.btree.proc.IKeyRangeIndexProcedure;
import org.embergraph.btree.proc.IResultHandler;
import org.embergraph.btree.proc.ISimpleIndexProcedure;
import org.embergraph.counters.CounterSet;
import org.embergraph.journal.AbstractJournal;
import org.embergraph.journal.ICommitRecord;
import org.embergraph.journal.IResourceManager;
import org.embergraph.journal.Journal;
import org.embergraph.mdi.IResourceMetadata;
import org.embergraph.resources.IndexManager;
import org.embergraph.resources.StoreManager.ManagedJournal;

/*
* A view of a named index that replaces its view for each high-level request if there has been an
 * intervening commit on the backing store. Each request, including each iterator request, will be
 * <strong>read-consistent</strong> as of the commit point resolved for that request. However,
 * subsequent requests will, of course, be read-consistent against then then current lastCommitTime.
 *
 * <p>This class is designed to work both with a {@link Journal} and with an {@link IndexManager}.
 * For the latter, the live {@link ManagedJournal} will periodically be replaced by another {@link
 * ManagedJournal}. This means that we need two levels of indirection. First, we need to be able to
 * identify the current journal and read the lastCommitTime off of the current root block for that
 * journal. Second, we need to replace the view of the {@link BTree} for the named index with the
 * canonical read-only {@link BTree} instance loaded from the commit record corresponding to the
 * lastCommitTime.
 *
 * <p>Note: We are not required to obtain a lock on the live journal since it will not be closed if
 * it overflows, just closed for writes. Therefore this class will provide a read-committed view as
 * of (a) the moment that it obtains the then current journal; and (b) checks the lastCommitTime on
 * that journal. If there are intervening commits or an overflow event then the data will be "only
 * slightly stale".
 *
 * <p>Note: This class has very little state of its own. The bulk of the state is on the {@link
 * BTree} objects corresponding to the lastCommitTime. Those are thread-safe for readers and are
 * shared across instances of this class.
 *
 * <p>Note: At any given moment, two instances of this class for the same named index MAY have a
 * different view. However, the views will always reflect the lastCommitTime for each instance that
 * is resolved when a method is invoked on its public API.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ReadCommittedView implements ILocalBTreeView {

  /*
   * Class encapsulates the state that provides the basis for the current view. A new instance is
   * allocated if (a) the live journal is changed; or (b) the lastCommitTime on the journal is
   * changed.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class Basis {

    final AbstractJournal journal;

    final ICommitRecord commitRecord;

    final BTree btree;

    /*
     * @param journal The live journal.
     * @param name The name of the index.
     */
    public Basis(final AbstractJournal journal, final String name) {

      // the live journal.
      this.journal = journal;

      // the most current commit point.
      this.commitRecord = journal.getCommitRecord();

      // the read-only view of the index as of that commit point.
      this.btree = (BTree) journal.getIndexWithCommitRecord(name, commitRecord);
    }
  }

  private final IResourceManager resourceManager;

  private final String name;

  private volatile Basis basis;

  /*
   * Return the read-committed view.
   *
   * @return the index
   */
  private synchronized BTree getIndex() {

    final AbstractJournal journal = resourceManager.getLiveJournal();

    if (journal != basis.journal
        || journal.getLastCommitTime() != basis.commitRecord.getTimestamp()) {

      basis = new Basis(journal, name);
    }

    /*
     * Note: We rely on the BTree returned here to enforce the read-only
     * contract for this view.
     */

    assert basis.btree.isReadOnly();

    return basis.btree;
  }

  /*
   * @param resourceManager The object that will report to us the live journal.
   * @param name The name of the index.
   */
  public ReadCommittedView(IResourceManager resourceManager, String name) {

    if (resourceManager == null) throw new IllegalArgumentException();

    if (name == null) throw new IllegalArgumentException();

    this.resourceManager = resourceManager;

    this.name = name;

    this.basis = new Basis(resourceManager.getLiveJournal(), name);
  }

  @Override
  public ICounter getCounter() {

    return getIndex().getCounter();
  }

  @Override
  public CounterSet getCounters() {

    return getIndex().getCounters();
  }

  @Override
  public IndexMetadata getIndexMetadata() {

    return getIndex().getIndexMetadata();
  }

  @Override
  public IResourceMetadata[] getResourceMetadata() {

    return getIndex().getResourceMetadata();
  }

  @Override
  public boolean contains(byte[] key) {

    return getIndex().contains(key);
  }

  @Override
  public boolean contains(Object key) {

    return getIndex().contains(key);
  }

  @Override
  public byte[] lookup(byte[] key) {

    return getIndex().lookup(key);
  }

  @Override
  public Object lookup(Object key) {

    return getIndex().lookup(key);
  }

  @Override
  public byte[] remove(byte[] key) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Object remove(Object key) {

    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] insert(byte[] key, byte[] value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] putIfAbsent(byte[] key, byte[] value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Object insert(Object key, Object value) {

    throw new UnsupportedOperationException();
  }

  @Override
  public long rangeCount() {

    return getIndex().rangeCount();
  }

  @Override
  public long rangeCount(byte[] fromKey, byte[] toKey) {

    return getIndex().rangeCount(fromKey, toKey);
  }

  @Override
  public long rangeCountExact(byte[] fromKey, byte[] toKey) {

    return getIndex().rangeCountExact(fromKey, toKey);
  }

  @Override
  public long rangeCountExactWithDeleted(byte[] fromKey, byte[] toKey) {

    return getIndex().rangeCountExactWithDeleted(fromKey, toKey);
  }

  /*
   * Note: We rely on the BTree returned by getIndex() to enforce the
   * read-only contract for the iterators exposed by this class.
   */

  /*
   * Note: The iterators returned by this view will be <em>read-consistent</em> as of the
   * lastCommitTime when they are created. In order for newly committed state to be visible you must
   * request a new iterator.
   */
  @Override
  public ITupleIterator rangeIterator() {

    return getIndex().rangeIterator();
  }

  @Override
  public ITupleIterator rangeIterator(
      byte[] fromKey, byte[] toKey, int capacity, int flags, IFilter filterCtor) {

    return getIndex().rangeIterator(fromKey, toKey, capacity, flags, filterCtor);
  }

  @Override
  public ITupleIterator rangeIterator(byte[] fromKey, byte[] toKey) {

    return getIndex().rangeIterator(fromKey, toKey);
  }

  /*
   * Note: We rely on the BTree returned by getIndex() to enforce the
   * read-only contract for the procedures processed by this class.
   */

  @Override
  public void submit(
      byte[] fromKey, byte[] toKey, IKeyRangeIndexProcedure proc, IResultHandler handler) {

    getIndex().submit(fromKey, toKey, proc, handler);
  }

  @Override
  public Object submit(byte[] key, ISimpleIndexProcedure proc) {

    return getIndex().submit(key, proc);
  }

  @Override
  public void submit(
      int fromIndex,
      int toIndex,
      byte[][] keys,
      byte[][] vals,
      AbstractKeyArrayIndexProcedureConstructor ctor,
      IResultHandler resultHandler) {

    getIndex().submit(fromIndex, toIndex, keys, vals, ctor, resultHandler);
  }

  //    public final BTreeCounters getBTreeCounters() {
  //
  //        return getIndex().getBtreeCounters();
  //
  //    }

  @Override
  public IBloomFilter getBloomFilter() {

    return getIndex().getBloomFilter();
  }

  @Override
  public BTree getMutableBTree() {

    return getIndex().getMutableBTree();
  }

  @Override
  public int getSourceCount() {

    return getIndex().getSourceCount();
  }

  @Override
  public AbstractBTree[] getSources() {

    return getIndex().getSources();
  }
}
