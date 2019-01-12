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
 * Created on Jun 26, 2008
 */

package org.embergraph.relation.accesspath;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.embergraph.relation.IMutableRelation;
import org.embergraph.relation.IRelation;
import org.embergraph.striterator.IChunkedIterator;

/**
 * Interface provides an iterator to drain chunks from an {@link IBuffer}.
 *
 * <h2>CONOPS</h2>
 *
 * <p>This interface is useful where one (or more) processes will write asynchronously on the {@link
 * IBuffer} while another drains it via the {@link #iterator()}. For better performance in a
 * multi-threaded environment, each thread is given an {@link UnsynchronizedArrayBuffer} of some
 * capacity. The threads populate their {@link UnsynchronizedArrayBuffer}s in parallel using
 * non-blocking operations. The {@link UnsynchronizedArrayBuffer}s in turn are configured to flush
 * <em>chunks</em> of elements onto an either an {@link IBuffer} whose generic type is <code>E[]
 * </code>. Each element in the target {@link IBuffer} is therefore a chunk of elements from one of
 * the source {@link UnsynchronizedArrayBuffer}s.
 *
 * <p>There are two families of synchronized {@link IBuffer}s
 *
 * <ol>
 *   <li>An {@link IBuffer} that targets a mutable {@link IRelation}; and
 *   <li>An {@link IBlockingBuffer} that exposes an {@link IAsynchronousIterator} for reading chunks
 *       of elements.
 * </ol>
 *
 * <p>This design means that blocking operations are restricted to chunk-at-a-time operations,
 * primarily when an {@link UnsynchronizedArrayBuffer}<code>&lt;E&gt;</code> overflows onto an
 * {@link IBuffer}<<code>&lt;E[]&gt;</code> and when the {@link IBuffer}<<code>&lt;E[]&gt;</code>
 * either is flushed onto an {@link IMutableRelation} or drained by an {@link IChunkedIterator}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E> The generic type of the elements in the chunks.
 */
public interface IBlockingBuffer<E> extends IRunnableBuffer<E> {

  /**
   * Return an iterator reading from the buffer. It is NOT safe for concurrent processes to consume
   * the iterator. The iterator will visit elements in the order in which they were written on the
   * buffer, but note that the elements may be written onto the {@link IBlockingBuffer} by
   * concurrent processes in which case the order is not predictable without additional
   * synchronization.
   *
   * @return The iterator.
   */
  public IAsynchronousIterator<E> iterator();

  /**
   * This is a NOP since the {@link #iterator()} is the only way to consume data written on the
   * buffer.
   *
   * @return ZERO (0L)
   */
  public long flush();

  /**
   * Set the {@link Future} for the source processing writing on the {@link IBlockingBuffer} (the
   * producer).
   *
   * <p>Note: You should always wrap the task as a {@link FutureTask} and set the {@link Future} on
   * the {@link IBlockingBuffer} before you start the consumer. This ensures that the producer will
   * be cancelled if the consumer is interrupted.
   *
   * @param future The {@link Future}.
   * @throws IllegalArgumentException if the argument is <code>null</code>.
   * @throws IllegalStateException if the future has already been set.
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/707">BlockingBuffer.close() does
   *     not unblock threads </a>
   * @todo There should be a generic type for this.
   */
  public void setFuture(Future future);
}
