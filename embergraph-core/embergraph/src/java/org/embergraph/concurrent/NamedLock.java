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
 * Created on Jul 3, 2008
 */

package org.embergraph.concurrent;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*
 * A factory for named {@link Lock}s. A simple {@link Lock} manages access to a single resource.
 * However, a {@link NamedLock} manages access to the members of a set of named resources. This is
 * more efficient when the latency of the operation once the lock is acquired is significant, e.g.,
 * an RMI call or a disk IO.
 *
 * <p>The locks are stored in a {@link WeakHashMap} so that they will be garbage collected if there
 * are no threads waiting in the queue for a given named lock.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T> The generic type for the "name". This MUST implement hashCode() and equals() since the
 *     instances of this type will serve as keys in a {@link Map}.
 */
public class NamedLock<T> {

  private final Map<T, ReentrantLock> locks = new WeakHashMap<>();

  /*
   * Return the canonical instance of the lock for a named resource.
   *
   * @param name The name.
   * @return The canonical instance of the lock for that name.
   */
  protected Lock lockFactory(final T name) {

    if (name == null) throw new IllegalArgumentException();

    ReentrantLock lock;

    synchronized (locks) {
      lock = locks.get(name);

      if (lock == null) {

        lock = new ReentrantLock();

        locks.put(name, lock);
      }
    }

    return lock;
  }

  /*
   * Block until the {@link Lock} for the named resource is available, then {@link Lock#lock()} the
   * {@link Lock} and return the locked {@link Lock}.
   *
   * @param name The name of the resource whose {@link Lock} is desired.
   * @return The {@link Lock}. It will have already been {@link Lock#lock()}ed.
   */
  public Lock acquireLock(final T name) {

    final Lock lock = lockFactory(name);

    lock.lock();

    return lock;
  }

  public Lock acquireLock(final T name, final long timeout, final TimeUnit unit)
      throws InterruptedException, TimeoutException {

    final Lock lock = lockFactory(name);

    if (!lock.tryLock(timeout, unit)) throw new TimeoutException();

    return lock;
  }
}
