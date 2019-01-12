package org.embergraph.btree.filter;

import java.util.Iterator;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;

/*
* Wraps an {@link Iterator} as an {@link ITupleIterator}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E>
 */
public class WrappedTupleIterator<E> implements ITupleIterator<E> {

  private final Iterator<E> src;

  public WrappedTupleIterator(final Iterator<E> src) {

    if (src == null) throw new IllegalArgumentException();

    this.src = src;
  }

  public boolean hasNext() {

    return src.hasNext();
  }

  @SuppressWarnings("unchecked")
  public ITuple<E> next() {

    return (ITuple<E>) src.next();
  }

  public void remove() {

    src.remove();
  }
}
