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
 * Created on Aug 26, 2010
 */

package org.embergraph.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import junit.framework.TestCase2;
import org.embergraph.util.InnerCause;

/**
 * Test suite for {@link Haltable}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestHaltable extends TestCase2 {

  /** */
  public TestHaltable() {}

  /** @param name */
  public TestHaltable(String name) {
    super(name);
  }

  /**
   * Unit test where the {@link Haltable} is cancelled.
   *
   * @throws ExecutionException
   */
  public void test_cancel() throws InterruptedException, ExecutionException {

    final Haltable<Void> f = new Haltable<Void>();

    assertFalse(f.isDone());
    assertFalse(f.isCancelled());
    assertNull(f.getCause());

    assertTrue(f.cancel(true /* mayInterruptIfRunning */));

    assertTrue(f.isDone());
    assertTrue(f.isCancelled());
    assertNull(f.getCause());

    try {
      f.get();
      fail("Expecting: " + CancellationException.class);
    } catch (CancellationException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    assertTrue(InnerCause.isInnerCause(f.getAsThrownCause(), InterruptedException.class));
  }

  /**
   * Unit test where the {@link Haltable} returns the result of its computation.
   *
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public void test_get() throws InterruptedException, ExecutionException {

    final Haltable<Long> f = new Haltable<Long>();

    assertFalse(f.isDone());
    assertFalse(f.isCancelled());
    assertNull(f.getCause());

    final Long result = Long.valueOf(12);

    // set the result.
    f.halt(result);

    assertTrue(result == f.get());

    assertTrue(f.isDone());
    assertFalse(f.isCancelled());
    assertNull(f.getCause());
    assertNull(f.getAsThrownCause());

    assertFalse(f.cancel(true /*mayInterruptIfRunning*/));

    assertTrue(result == f.get());
  }

  public void test_get_timeout() throws InterruptedException, ExecutionException {

    final Haltable<Long> f = new Haltable<Long>();

    assertFalse(f.isDone());
    assertFalse(f.isCancelled());
    assertNull(f.getCause());

    final Long result = Long.valueOf(12);

    {
      final long timeout = TimeUnit.SECONDS.toNanos(1L);
      final long begin = System.nanoTime();
      try {
        f.get(timeout, TimeUnit.NANOSECONDS);
        fail("Expecting: " + TimeoutException.class);
      } catch (TimeoutException e) {
        // ignore
      }
      final long elapsed = System.nanoTime() - begin;
      if (elapsed < timeout || (elapsed > (2 * timeout))) {
        fail("elapsed=" + elapsed + ", timeout=" + timeout);
      }
    }

    // set the result.
    f.halt(result);

    assertTrue(result == f.get());

    assertTrue(f.isDone());
    assertFalse(f.isCancelled());
    assertNull(f.getCause());
    assertNull(f.getAsThrownCause());

    assertFalse(f.cancel(true /*mayInterruptIfRunning*/));

    assertTrue(result == f.get());
  }
}
