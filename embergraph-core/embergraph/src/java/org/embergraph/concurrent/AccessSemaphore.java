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

package org.embergraph.concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/*
 * The AccessSemaphore implements an idiom of exclusive and shared access.
 *
 * <p>A typical use-case for exclusive/shared access might be single writer/multiple readers
 *
 * <p>Since this access mechanism builds on a simple semaphore, it is straightforward to bound the
 * number of shares permitted. By default this is set to MAXINT, but it need not be, and a
 * constructor is provided to define this limit.
 *
 * <p>It is NOT re-entrant.
 *
 * @author Martyn Cutcher
 */
public class AccessSemaphore {

  final int maxShares;
  private final Semaphore semaphore;

  volatile Thread exclusiveOwner = null;

  public AccessSemaphore() {
    this(Integer.MAX_VALUE);
  }

  public AccessSemaphore(final int maxShares) {
    this.maxShares = maxShares;
    this.semaphore = new Semaphore(maxShares, true /*fair*/);
  }

  public abstract class Access {
    boolean released = false;

    public final void release() {
      if (released) {
        throw new IllegalStateException();
      }
      doRelease();
      released = true;
    }

    protected abstract void doRelease();
  }

  public class ExclusiveAccess extends Access {
    protected void doRelease() {
      releaseExclusive();
    }
  }

  public class SharedAccess extends Access {
    protected void doRelease() {
      releaseShared();
    }
  }

  public static final class AccessSemaphoreNotReentrantException extends IllegalStateException {

    private static final long serialVersionUID = -234560202749194378L;
  }

  public final Access acquireExclusive() throws InterruptedException {

    if (exclusiveOwner != null && exclusiveOwner == Thread.currentThread()) {
      throw new AccessSemaphoreNotReentrantException();
    }
    semaphore.acquire(maxShares);

    exclusiveOwner = Thread.currentThread();

    return new ExclusiveAccess();
  }

  public final Access tryAcquireExclusive(final long timeout, final TimeUnit unit)
      throws InterruptedException {
    semaphore.tryAcquire(maxShares, timeout, unit);

    return new ExclusiveAccess();
  }

  private void releaseExclusive() {
    exclusiveOwner = null;
    semaphore.release(maxShares);
  }

  public final Access acquireShared() throws InterruptedException {
    semaphore.acquire();

    return new SharedAccess();
  }

  public final Access tryAcquireShared(final long timeout, final TimeUnit unit)
      throws InterruptedException {
    semaphore.tryAcquire(timeout, unit);

    return new SharedAccess();
  }

  private void releaseShared() {
    semaphore.release();
  }
}
