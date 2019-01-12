package org.embergraph.btree.filter;

import java.util.Iterator;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor;
import org.embergraph.btree.ITupleIterator;

/*
* Filter allows mutation of the visited {@link ITuple}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E>
 */
public abstract class TupleUpdater<E> extends TupleFilter<E> {

  private static final long serialVersionUID = 8825811070321638652L;

  public TupleUpdater() {

    super();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ITupleIterator<E> filterOnce(Iterator src, Object context) {

    return new Updaterator((ITupleCursor<E>) src, context, this);
  }

  /*
   * You may implement this method to update the state of the visited tuple in the backing index.
   *
   * <p>Note: If you modify <i>tuple</i> then that modification will be visitible to the consumer of
   * the iterator.
   *
   * @param ndx The index on which the {@link ITupleCursor} is reading.
   * @param tuple The tuple that is being visited.
   */
  protected abstract void update(IIndex ndx, ITuple<E> tuple);

  protected class Updaterator extends TupleFilter.TupleFilterator<E> {

    public Updaterator(
        final ITupleIterator<E> src, final Object context, final TupleFilter<E> filter) {

      super(src, context, filter);
    }

    @Override
    protected void visit(final ITuple<E> tuple) {

      final IIndex ndx = ((ITupleCursor<E>) src).getIndex();

      update(ndx, tuple);
    }
  }
}
