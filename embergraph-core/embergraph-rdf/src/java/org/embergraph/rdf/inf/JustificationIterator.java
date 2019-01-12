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
 * Created on Nov 14, 2007
 */

package org.embergraph.rdf.inf;

import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.journal.IIndexManager;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.util.Bytes;

/**
 * Iterator visits {@link Justification}s reading from the justification index. The iterator
 * optionally supports asynchronous read ahead.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @deprecated replace with appropriate access path reading on the justifications index for
 *     SPORelation (or treat as a secondary relation).
 */
public class JustificationIterator implements IJustificationIterator {

  private static final transient Logger log = Logger.getLogger(JustificationIterator.class);

  /** The maximum #of statements that will be buffered by the iterator. */
  private static final transient int MAXIMUM_CAPACITY = 10 * Bytes.kilobyte32; // was 100k

  private boolean open = true;

  /** The actual capacity of the buffer (never zero). */
  private final int capacity;

  /**
   * The #of statements that have been read <strong>from the source</strong> and placed into the
   * buffer. All such statements will also have passed the optional {@link IElementFilter}.
   */
  private int numBuffered;

  /** The #of statements that have been read by the caller using {@link #next()}. */
  private int numReadByCaller;

  /** The #of chunks that have been read by the caller. */
  private int nchunks = 0;

  /**
   * A buffer holding {@link Justification}s that have not been visited. {@link Justification}s that
   * have been visited are taken from the buffer, making room for new statements which can be filled
   * in asynchronously by the {@link Reader}.
   */
  private ArrayBlockingQueue<Justification> buffer;

  /** The source iterator reading on the selected justification index. */
  private ITupleIterator<?> src;

  //    /**
  //     * The executor service for the {@link Reader} (iff the {@link Reader} runs
  //     * asynchronously).
  //     */
  //    private final ExecutorService readService;

  /**
   * The future for the {@link Reader} and <code>null</code> if a synchronous read was performed
   * (fully buffered read in the caller's thread).
   */
  private final FutureTask<Object> ft;

  /**
   * Set to true iff an asynchronous {@link Reader} is used AND there is nothing more to be read.
   */
  private final AtomicBoolean readerDone = new AtomicBoolean(false);

  /** The minimum desirable chunk size for {@link #nextChunk()}. */
  private static final int MIN_CHUNK_SIZE = 100;

  //    /**
  //     * If NO results show up within this timeout then {@link #nextChunk()} will
  //     * throw a {@link RuntimeException} to abort the reader - the probably cause
  //     * is a network outage.
  //     */
  //    static private final long TIMEOUT = Long.MAX_VALUE;

  /**
   * Create an iterator reading from the justifications index.
   *
   * @param capacity The maximum #of statements that will be buffered. When ZERO (0) the iterator
   *     will range count the access path fully buffer if there are less than {@link
   *     #MAXIMUM_CAPACITY} statements selected by the triple pattern. When non-zero, the caller's
   *     value is used - this gives you control when you really, really want to have something fully
   *     buffered, e.g., for an in-memory self-join.
   */
  public JustificationIterator(final IIndexManager indexManager, final IIndex ndx, int capacity) {

    if (indexManager == null) throw new IllegalArgumentException();

    if (ndx == null) throw new IllegalArgumentException();

    if (capacity < 0) throw new IllegalArgumentException();

    /*
     * When true, asynchronous read-ahead will be used to refill the buffer
     * as it becomes depleted. When false, read-ahead will be synchronous
     * (this is useful when you want to read at most N statements from the
     * index).
     */
    boolean async = true;

    // The fast range count (upper bound)
    final long rangeCount = ndx.rangeCount();

    if (capacity == 0) {

      /*
       * Attempt to fully buffer the justifications.
       */

      if (capacity > MAXIMUM_CAPACITY || rangeCount > MAXIMUM_CAPACITY) {

        /*
         * If the capacity would exceed the maximum then we limit
         * the capacity to the maximum.
         */

        capacity = MAXIMUM_CAPACITY;

      } else {

        // Otherwise use the range count (upper bound).
        capacity = (int) rangeCount;
      }

    } else {

      if (capacity > rangeCount) {

        /*
         * If the caller has over-estimated the actual range count for
         * the index then reduce the capacity to the real range count.
         * This makes it safe for the caller to request a capacity of 1M
         * SPOs and only a "right-sized" buffer will be allocated.
         *
         * Note: The range count is generally an upper bound rather than
         * an exact value.
         */

        capacity = (int) rangeCount;

        /*
         * Note: If the caller is making a best effort attempt to read
         * everything into memory AND the data will fit within the
         * caller's specified capacity, then we disable asynchronous
         * reads so that they will get everything in one chunk.
         */

        async = false;
      }
    }

    if (rangeCount < 100) {

      // Disable async reads if we are not reading much data.

      async = false;
    }

    if (capacity == 0) {

      /*
       * Note: The ArrayBlockingQueue has a minimum capacity of ONE (1).
       */

      capacity = 1;
    }

    this.capacity = capacity;

    this.src =
        ndx.rangeIterator(
            null /* fromKey */,
            null /* toKey */,
            0 /* capacity */,
            IRangeQuery.KEYS,
            null /* filter */);

    this.buffer = new ArrayBlockingQueue<Justification>(capacity);

    if (async) {

      // wrap reader as Future
      ft = new FutureTask<Object>(new Reader());

      // submit for asynchronous read ahead
      indexManager.getExecutorService().submit(ft);

    } else {

      // Fill the buffer (synchronous).

      ft = null;

      fillBuffer();
    }
  }

  /**
   * Reads from the statement index, filling the {@link #buffer}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  private class Reader implements Callable<Object> {

    /**
     * Runs the {@link Reader}.
     *
     * @return <code>null</code>.
     */
    public Object call() throws Exception {

      while (src.hasNext()) {

        final Justification t = (Justification) src.next().getObject();

        try {

          /*
           * Note: This will block if the buffer is at capacity.
           */

          buffer.put(t);

          numBuffered++;

        } catch (InterruptedException ex) {

          throw new RuntimeException(ex);
        }
      }

      // Nothing left to read.

      readerDone.set(true);

      return null;
    }
  }

  /**
   * (Re-)fills the buffer up to its capacity or the exhaustion of the source iterator.
   *
   * @return false if the buffer is still empty.
   */
  private boolean fillBuffer() {

    assertOpen();

    if (ft != null) {

      // This method MUST NOT be invoked when using the async reader.
      throw new AssertionError();
    }

    try {
      // if(log.isDebugEnabled()) log.debug("(Re-)filling buffer: remainingCapacity="
      // + buffer.remainingCapacity());

      while (src.hasNext() && buffer.remainingCapacity() > 0) {

        final Justification jst = (Justification) src.next().getObject();

        try {

          buffer.put(jst);

          numBuffered++;

        } catch (InterruptedException ex) {

          throw new RuntimeException(ex);
        }
      }

      // false if the buffer is still empty.

      return !buffer.isEmpty();

    } finally {

      if (log.isDebugEnabled())
        log.debug(
            "(Re-)filled buffer: size="
                + buffer.size()
                + ", remainingCapacity="
                + buffer.remainingCapacity()
                + ", done="
                + !src.hasNext());
    }
  }

  public boolean hasNext() {

    if (!open) return false;

    if (buffer.isEmpty()) {

      /*
       * The buffer is empty, but there may be more data available from
       * the underlying iterator.
       */

      if (ft != null) {

        // async reader - so wait on it.
        awaitReader();

      } else {

        // sync reader - so fill the buffer in this thread.
        fillBuffer();
      }

      if (buffer.isEmpty()) {

        // the buffer is still empty, so the iterator is exhausted.
        return false;
      }
    }

    // at least one Justification in the buffer.
    return true;
  }

  public Justification next() {

    if (!hasNext()) {

      throw new NoSuchElementException();
    }

    final Justification t;

    try {

      t = buffer.take();

    } catch (InterruptedException ex) {

      throw new RuntimeException(ex);
    }

    numReadByCaller++;

    return t;
  }

  /**
   * Returns a chunk whose size is the #of statements currently in the buffer.
   *
   * <p>Note: When asynchronous reads are used, the buffer will be transparently refilled and should
   * be ready for a next chunk by the time you are done with this one.
   */
  public Justification[] nextChunk() {

    if (!hasNext()) {

      throw new NoSuchElementException();
    }

    if (ft != null) {

      // make sure that we fill the buffer before we deliver a chunk.
      awaitReader();
    }

    // there are at least this many in the buffer.
    final int n = buffer.size();

    // allocate the array.
    final Justification[] stmts = new Justification[n];

    for (int i = 0; i < n; i++) {

      stmts[i] = next();
    }

    if (log.isDebugEnabled())
      log.debug(
          "chunkSize="
              + n
              + ", nchunks="
              + nchunks
              + ", #read(caller)="
              + numReadByCaller
              + ", #read(src)="
              + numBuffered);

    return stmts;
  }

  /**
   * Await some data from the {@link Reader}.
   *
   * <p>Note: If there is some data available this will continue to wait until at least {@link
   * #MIN_CHUNK_SIZE} statements are available from the {@link Reader} -or- until the reader signals
   * that it is {@link #readerDone done}. This helps to keep up the chunk size and hence the
   * efficiency of batch operations when we might otherwise get into a race with the {@link Reader}.
   */
  private void awaitReader() {

    if (ft == null) {

      /*
       * This method MUST NOT be invoked unless you are using the async
       * reader.
       */

      throw new AssertionError();
    }

    //        final long begin = System.currentTimeMillis();

    /*
     * Wait for at least N records to show up.
     */

    final int N = capacity < MIN_CHUNK_SIZE ? capacity : MIN_CHUNK_SIZE;

    while (buffer.size() < N && !readerDone.get()) {

      try {

        /*
         * TODO This uses a Thread.sleep() to avoid a lock ordering
         * problem because we did not have access to the lock used
         * internally by the blocking queue when this code was written.
         * However, we now have incorporated at least one JSR166
         * blocking queue class which can use the caller's lock. That
         * makes it possible to setup conditions which can be awaited
         * for full/not-full, etc., but you have to be careful not to
         * violate the manner in which the lock signal/notify semantics
         * are used internally by the blocking queue implementation.
         */

        Thread.sleep(10 /*ms*/);

      } catch (InterruptedException ex) {

        throw new RuntimeException(ex);
      }

      //            final long elapsed = System.currentTimeMillis() - begin;
      //
      //            if (elapsed > TIMEOUT && buffer.isEmpty()) {
      //
      //                throw new RuntimeException("Timeout after " + elapsed + "ms");
      //
      //            }

    }
  }

  /** @throws UnsupportedOperationException always */
  public void remove() {

    assertOpen();

    throw new UnsupportedOperationException();
  }

  public void close() {

    if (!open) {

      // Already closed.
      return;
    }

    log.debug("Closing iterator");

    open = false;

    if (ft != null) {

      // terminate the Reader.
      ft.cancel(true /* mayInterruptIfRunning */);

      //            try {
      //
      //                readService.awaitTermination(500, TimeUnit.MILLISECONDS);
      //
      //            } catch (InterruptedException e) {
      //
      //                log.warn("Read service did not terminate: " + e);
      //
      //            }

    }

    // discard buffer.
    buffer.clear();

    buffer = null;

    // discard the source iterator.
    src = null;
  }

  private final void assertOpen() {

    if (!open) throw new IllegalStateException();
  }
}
