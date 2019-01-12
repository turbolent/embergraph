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
package org.embergraph.rdf.sail.webapp.lbs.policy.counters;

import org.embergraph.counters.IHostCounters;
import org.embergraph.rdf.sail.webapp.lbs.IHostMetrics;
import org.embergraph.rdf.sail.webapp.lbs.IHostScoringRule;

/*
 * Best effort computation of a workload score based on CPU Utilization and IO Wait defined as
 * follows:
 *
 * <pre>
 * (1d + cpu_wio * 100d) / (1d + cpu_idle)
 * </pre>
 *
 * <p>Note: Not all platforms report all metrics. For example, OSX does not report IO Wait, which is
 * a key metric for the workload of a database. If a metric is not available for a host, then a
 * fallback value is used.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 *     <p>FIXME GC time is a per-JVM metric that should be incorporated into our default scoring
 *     strartegy. It will only get reported by the ganglia plug in if it is setup to self-report
 *     that data. And it may not report it correctly if there is more than one {@link
 *     HAJournalService} per host. It is also available from the <code>/embergraph/counters</code>
 *     and could be exposed as a JMX MBean.
 */
public class DefaultHostScoringRule implements IHostScoringRule {

  private static final String CPU_NOT_IDLE = IHostCounters.CPU_PercentProcessorTime;

  private static final String CPU_WIO = IHostCounters.CPU_PercentIOWait;

  @Override
  public String[] getMetricNames() {

    return new String[] {CPU_NOT_IDLE, CPU_WIO};
  }

  @Override
  public double getScore(final IHostMetrics metrics) {

    final double cpu_idle = 1d - metrics.getNumeric(CPU_NOT_IDLE, .5d);

    final double cpu_wio = metrics.getNumeric(CPU_WIO, .05d);

    final double hostScore = (1d + cpu_wio * 100d) / (1d + cpu_idle);

    return hostScore;
  }
}
