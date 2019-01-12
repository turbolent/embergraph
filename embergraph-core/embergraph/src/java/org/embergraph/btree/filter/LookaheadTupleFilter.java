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
 * Created on Aug 5, 2008
 */

package org.embergraph.btree.filter;

import cutthecrap.utils.striterators.FilterBase;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.embergraph.btree.AbstractTuple;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.ITupleSerializer;

/**
 * Lookahead filter for an {@link ITuple}. You can push back a single {@link ITuple} onto the
 * filter.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo unit tests.
 */
public class LookaheadTupleFilter<E> extends FilterBase implements ITupleFilter<E> {

  private static final long serialVersionUID = -5551926378159692251L;

  public LookaheadTupleFilter() {}

  /**
   * Extends iterator semantics for {@link ITuple} pushback.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   * @param <E>
   */
  public interface ILookaheadTupleIterator<E> extends ITupleIterator<E> {

    /**
     * Pushback the last visited tuple. It will be returned by {@link ITupleIterator#next()}.
     *
     * @throws IllegalStateException if no tuple has been read.
     * @throws IllegalStateException if the pushback limit has been exceeded.
     */
    void pushback();
  }

  @SuppressWarnings("unchecked")
  @Override
  public ILookaheadTupleIterator<E> filterOnce(Iterator src, Object context) {

    if (src instanceof ITupleCursor) {

      return new LookaheadTupleCursor((ITupleCursor) src, context);
    }

    return new LookaheadTupleIterator((ITupleIterator) src, context);
  }

  /**
   * Implementation based on an underlying {@link ITupleCursor}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   * @param <E>
   */
  private static class LookaheadTupleCursor<E> implements ILookaheadTupleIterator<E> {

    private final ITupleCursor<E> src;
    private final Object context;
    private boolean pushbackAllowed = false;

    public LookaheadTupleCursor(ITupleCursor<E> src, Object context) {

      this.src = src;
      this.context = context;
    }

    public void pushback() {

      if (!pushbackAllowed) throw new IllegalStateException();

      src.prior();

      pushbackAllowed = false;
    }

    public ITuple<E> next() {

      final ITuple<E> tuple = src.next();

      pushbackAllowed = true;

      return tuple;
    }

    public boolean hasNext() {

      return src.hasNext();
    }

    public void remove() {

      src.remove();
    }
  }

  /**
   * Implementation based on an underlying {@link ITupleIterator}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   * @param <E>
   */
  private static class LookaheadTupleIterator<E> implements ILookaheadTupleIterator<E> {

    private final ITupleIterator<E> src;
    private final Object context;

    /**
     * <code>true</code> iff pushback is allowed. when <code>true</code>, {@link #tupleBuffer} will
     * containing a copy of the data for the last visited {@link ITuple}. note that we always copy
     * the data for the visited tuples into {@link #tupleBuffer}. pushback itself consists of
     * clearing this flag.
     */
    private boolean pushbackAllowed = false;

    /**
     * when non-<code>null</code> and {@link #pushbackAllowed} is <code>false</code>, then this
     * buffer contains the next tuple to be read.
     */
    private AbstractTuple<E> tupleBuffer;

    /** @param src */
    public LookaheadTupleIterator(ITupleIterator<E> src, Object context) {

      this.src = src;
      this.context = context;
    }

    public void pushback() {

      if (!pushbackAllowed) throw new IllegalStateException();

      pushbackAllowed = false;
    }

    public ITuple<E> next() {

      if (!hasNext()) throw new NoSuchElementException();

      if (!pushbackAllowed && tupleBuffer != null) {

        // return the buffered tuple.

        pushbackAllowed = true;

        return tupleBuffer;
      }

      final ITuple<E> tuple = src.next();

      if (tupleBuffer == null) {

        tupleBuffer =
            new AbstractTuple<E>(tuple.flags()) {

              public int getSourceIndex() {
                return tuple.getSourceIndex();
              }

              public ITupleSerializer getTupleSerializer() {
                return tuple.getTupleSerializer();
              }
            };
      }

      // copy the next tuple's data into the buffer.
      tupleBuffer.copyTuple(tuple);

      // pushback of this tuple is allowed.
      pushbackAllowed = true;

      // return the buffered tuple.
      return tupleBuffer;
    }

    public boolean hasNext() {

      if (!pushbackAllowed && tupleBuffer != null) {

        // there is a buffered tuple to return.
        return true;
      }

      // the source iterator will visit another tuple.
      return src.hasNext();
    }

    public void remove() {

      throw new UnsupportedOperationException();
    }
  }
}
