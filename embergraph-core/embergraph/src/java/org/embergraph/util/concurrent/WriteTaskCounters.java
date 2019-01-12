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
 * Created on Apr 15, 2009
 */

package org.embergraph.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;
import org.embergraph.journal.WriteExecutorService;

/*
* Extended for the {@link WriteExecutorService}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo add counter task checkpoint time (as part of task service time) and report out as moving
 *     average.
 */
public class WriteTaskCounters extends TaskCounters {

  /** */
  public WriteTaskCounters() {}

  /*
   * Cumulative elapsed time in nanoseconds consumed by tasks while waiting for an resource lock.
   */
  public final AtomicLong lockWaitingNanoTime = new AtomicLong();

  /** Cumulative elapsed time in nanoseconds consumed by tasks awaiting group commit. */
  public final AtomicLong commitWaitingNanoTime = new AtomicLong();

  /** Cumulative elapsed time in nanoseconds servicing group commit. */
  public final AtomicLong commitServiceNanoTime = new AtomicLong();
}
