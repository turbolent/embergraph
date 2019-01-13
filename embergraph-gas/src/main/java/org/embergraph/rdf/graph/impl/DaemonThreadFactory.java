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
package org.embergraph.rdf.graph.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/*
 * A thread factory that configures the thread as a daemon thread.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: DaemonThreadFactory.java 5824 2011-12-29 20:52:02Z thompsonbry $
 */
/*
 * Note: This is a clone of the same-named class in the embergraph module. The
 * clone exists to have it under the Apache 2 license requiring the creation of
 * a embergraph-commons module.
 */
class DaemonThreadFactory implements ThreadFactory {

  private final ThreadFactory delegate;
  private final String basename; // MAY be null.
  private int counter = 0; // used iff basename was given.

  private static ThreadFactory _default = new DaemonThreadFactory();

  /*
   * Returns an instance based on {@link Executors#defaultThreadFactory()} that configures the
   * thread for daemon mode.
   */
  public static ThreadFactory defaultThreadFactory() {

    return _default;
  }

  /** Uses {@link Executors#defaultThreadFactory()} as the delegate. */
  public DaemonThreadFactory() {

    this(Executors.defaultThreadFactory(), null /*basename*/);
  }

  public DaemonThreadFactory(String basename) {

    this(Executors.defaultThreadFactory(), basename);
  }

  /*
   * Uses the specified delegate {@link ThreadFactory}.
   *
   * @param delegate The delegate thread factory that is responsible for creating the threads.
   * @param basename Optional prefix that will be used to assign names to the generated threads.
   */
  public DaemonThreadFactory(final ThreadFactory delegate, final String basename) {

    if (delegate == null) throw new IllegalArgumentException();

    this.delegate = delegate;

    this.basename = basename;
  }

  public Thread newThread(final Runnable r) {

    final Thread t = delegate.newThread(r);

    if (basename != null) {

      counter++;

      t.setName(basename + counter);
    }

    t.setDaemon(true);

    //        System.err.println("new thread: "+t.getName()+", id="+t.getId());

    return t;
  }
}
