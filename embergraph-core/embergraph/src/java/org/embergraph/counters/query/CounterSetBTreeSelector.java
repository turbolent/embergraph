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
 * Created on Apr 6, 2009
 */

package org.embergraph.counters.query;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounter;
import org.embergraph.counters.PeriodEnum;
import org.embergraph.counters.store.CounterSetBTree;

/*
 * Reads the relevant performance counter data from the store.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class CounterSetBTreeSelector implements ICounterSelector {

  //    private static final Logger log = Logger.getLogger(CounterSetBTreeSelector.class);

  private final CounterSetBTree btree;

  /** @param btree The {@link CounterSetBTree}. */
  public CounterSetBTreeSelector(final CounterSetBTree btree) {

    if (btree == null) throw new IllegalStateException();

    this.btree = btree;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public ICounter[] selectCounters(
      final int depth,
      final Pattern pattern,
      final long fromTime,
      final long toTime,
      final PeriodEnum period,
      final boolean historyRequiredIsIgnored) {

    final CounterSet counterSet =
        btree.rangeIterator(fromTime, toTime, period.toTimeUnit(), pattern, depth);

    // filter was already applied.
    final Iterator<ICounter> itr = counterSet.getCounters(null /* filter */);

    final Vector<ICounter> v = new Vector<>();

    //        int ndistinct = 0;

    while (itr.hasNext()) {

      final ICounter c = itr.next();

      //            if (depth != 0 && c.getDepth() > depth)
      //                continue;
      //
      //            ndistinct++;
      //
      //            if (log.isDebugEnabled())
      //                log.debug("Selected: ndistinct=" + ndistinct + " : " + c);

      v.add(c);
    }

    //        if (log.isInfoEnabled())
    //            log.info("Selected: ndistinct=" + ndistinct);

    return v.toArray(new ICounter[v.size()]);
  }
}
