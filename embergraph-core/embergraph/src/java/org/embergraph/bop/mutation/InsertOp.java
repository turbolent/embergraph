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
 * Created on Aug 25, 2010
 */

package org.embergraph.bop.mutation;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IShardwisePipelineOp;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.btree.ILocalBTreeView;
import org.embergraph.btree.ITupleSerializer;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.journal.IIndexManager;
import org.embergraph.relation.IRelation;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.service.DataService;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.striterator.IKeyOrder;

/*
 * This operator writes elements constructed from binding sets and an orders list of variables and
 * constants on an index.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E> The generic type of the elements written onto the index.
 */
public class InsertOp<E> extends PipelineOp implements IShardwisePipelineOp<E> {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations extends PipelineOp.Annotations {

    /*
     * An {@link IPredicate}. The {@link IPredicate#asBound(IBindingSet)} predicate will be used to
     * create the elements to be inserted into the relation.
     *
     * @see IPredicate#asBound(IBindingSet)
     * @see IRelation#newElement(java.util.List, IBindingSet)
     */
    String SELECTED = (InsertOp.class.getName() + ".selected").intern();

    /** The namespace of the relation to which the index belongs. */
    String RELATION = (InsertOp.class.getName() + ".relation").intern();

    /** The {@link IKeyOrder} for the index. */
    String KEY_ORDER = (InsertOp.class.getName() + ".keyOrder").intern();
  }

  /*
   * Deep copy constructor.
   *
   * @param op
   */
  public InsertOp(InsertOp<E> op) {
    super(op);
  }

  /*
   * Shallow copy constructor.
   *
   * @param args
   * @param annotations
   */
  public InsertOp(BOp[] args, Map<String, Object> annotations) {

    super(args, annotations);

    getRequiredProperty(Annotations.SELECTED);
  }

  /** @see Annotations#SELECTED */
  @SuppressWarnings("unchecked")
  public IPredicate<E> getPredicate() {

    return (IPredicate<E>) getRequiredProperty(Annotations.SELECTED);
  }

  /** @see Annotations#RELATION */
  public String getRelation() {

    return (String) getRequiredProperty(Annotations.RELATION);
  }

  /** @see Annotations#KEY_ORDER */
  @SuppressWarnings("unchecked")
  public IKeyOrder<E> getKeyOrder() {

    return (IKeyOrder<E>) getRequiredProperty(Annotations.KEY_ORDER);
  }

  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<>(new InsertTask<>(this, context));
  }

  /** Create elements from the selected bindings and insert them onto the named index. */
  private static class InsertTask<E> implements Callable<Void> {

    private final BOpStats stats;

    private final BOpContext<IBindingSet> context;

    private final ICloseableIterator<IBindingSet[]> source;

    /** Only used to close the sink when we are done. */
    private final IBlockingBuffer<IBindingSet[]> sink;

    private IPredicate<E> predicate;

    private final IRelation<E> relation;

    private final IKeyOrder<E> keyOrder;

    //        @SuppressWarnings("unchecked")
    InsertTask(final InsertOp<E> op, final BOpContext<IBindingSet> context) {

      this.context = context;

      stats = context.getStats();

      source = context.getSource();

      sink = context.getSink();

      predicate = op.getPredicate();

      relation = context.getRelation(predicate);

      keyOrder = op.getKeyOrder();
    }

    /*
     * @todo This does not order the tuples before writing on the local index. I am not sure that it
     *     should. I think that order is generally obtained from how we organize the tuples when
     *     mapping them across shards. However, for standalone databases it may make sense to insert
     *     a SORT on the selected attributes before the INSERT.
     */
    public Void call() throws Exception {

      final ILocalBTreeView ndx =
          getMutableLocalIndexView(relation, keyOrder, context.getPartitionId());

      final IKeyBuilder keyBuilder = ndx.getIndexMetadata().getKeyBuilder();

      final ITupleSerializer tupleSer = ndx.getIndexMetadata().getTupleSerializer();

      try {

        while (source.hasNext()) {

          final IBindingSet[] chunk = source.next();

          stats.chunksIn.increment();
          stats.unitsIn.add(chunk.length);

          int nwritten = 0;
          final List<BOp> args = predicate.args();
          for (int i = 0; i < chunk.length; i++) {

            final IBindingSet bset = chunk[i];

            final E e = relation.newElement(args, bset);

            final byte[] key = keyOrder.getKey(keyBuilder, e);

            if (!ndx.contains(key)) {

              final byte[] val = tupleSer.serializeVal(e);

              ndx.insert(key, val);

              nwritten++;
            }
          }

          if (nwritten > 0) {
            stats.unitsOut.add(nwritten);
            stats.chunksOut.increment();
          }
        }

        return null;

      } finally {

        sink.close();
      }
    }

    /*
     * Return an mutable view of the specified index.
     *
     * @param <T> The generic type of the elements in the relation.
     * @param relation The relation.
     * @param keyOrder The key order for that index.
     * @param partitionId The partition identifier and <code>-1</code> unless running against an
     *     {@link IEmbergraphFederation}.
     * @return The mutable view of the index.
     * @throws UnsupportedOperationException if there is an attempt to read on an index partition
     *     when the database is not an {@link IEmbergraphFederation} or when the database is an
     *     {@link IEmbergraphFederation} unless the index partition was specified.
     * @todo validate for standalone. probably needs to be wrapped as an {@link
     *     UnisolatedReadWriteIndex} whcih migtht be done by how we get the relation view.
     * @todo validate for s/o. Since this goes through a common code path, what we really need to
     *     test is getMutableLocalIndexView(). The rest of the insert operation can be tested
     *     against a local Journal.
     *     <p>FIXME This must obtain the appropriate lock for the mutable index in scale-out.
     *     <p>FIXME Allow remote writes as well if a remote access path is marked on the {@link
     *     IPredicate}.
     *     <p>FIXME There is currently a problem obtaining the UNISOLATED index in scale-out using
     *     the DelegateIndexManager. The issue is down in the guts of how AbstractTask exposes its
     *     views of the indices and manifests as a problem an assert in Name2Addr concerning the
     *     dirtyListener implementation.
     */
    public <T> ILocalBTreeView getMutableLocalIndexView(
        final IRelation<T> relation, final IKeyOrder<T> keyOrder, final int partitionId) {

      if (true) {
        /*
         * FIXME Concurrency control and locks. Maybe submit as an
         * AbstractTask?
         */
        throw new UnsupportedOperationException();
      }

      final IEmbergraphFederation<?> fed = context.getFederation();
      final IIndexManager indexManager = context.getIndexManager();
      final long writeTimestamp = predicate.getTimestamp();

      final String namespace = relation.getNamespace();

      final ILocalBTreeView ndx;

      if (partitionId == -1) {

        if (fed != null) {
          // This is scale-out so the partition identifier is required.
          throw new UnsupportedOperationException();
        }

        // The index is not partitioned.
        ndx =
            (ILocalBTreeView)
                indexManager.getIndex(namespace + "." + keyOrder.getIndexName(), writeTimestamp);

      } else {

        if (fed == null) {
          /*
           * This is not scale-out so index partitions are not
           * supported.
           */
          throw new UnsupportedOperationException();
        }

        // The name of the desired index partition.
        final String name =
            DataService.getIndexPartitionName(
                namespace + "." + keyOrder.getIndexName(), partitionId);

        // MUST be a local index view.
        ndx = (ILocalBTreeView) indexManager.getIndex(name, writeTimestamp);
      }

      return ndx;
    }
  }

  // E[] a = null;
  // Note: useful if we will sort before writing on the index.
  //  if (i == 0)
  //      a = (E[]) java.lang.reflect.Array.newInstance(e
  //              .getClass());
  //
  //  a[i] = e;

  //    /*
  //     * This is a shard wise operator.
  //     */
  //    @Override
  //    public BOpEvaluationContext getEvaluationContext() {
  //
  //        return BOpEvaluationContext.SHARDED;
  //
  //    }

}
