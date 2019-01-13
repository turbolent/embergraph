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
package org.embergraph.rdf.sail.webapp.lbs;

import java.util.Comparator;
import org.embergraph.counters.AbstractStatisticsCollector;

/*
 * Helper class pairs a hostname and the normalized availabilty for that host. The availability is
 * based on (normalized) <code>1 - LOAD</code> for the host. The <code>LOAD</code> is computed using
 * the {@link IHostMetrics} and an {@link IHostScoringRule} for computing the workload of a host
 * from those metrics. The availability is then computed from the LOAD and normalized.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class HostScore {

  /** The hostname. */
  private final String hostname;

  /** <code>true</code> iff the host is this host. */
  private final boolean thisHost;

  /** The normalized availability for the host. */
  private final double availability;

  /*
   * Return the normalized availability for the host. Load balancing decision are based on this
   * normalized {@link #getAvailability() score} (work is assigned to hosts in inverse proportion to
   * the normalized load of the host).
   */
  public double getAvailability() {
    return availability;
  }

  /** Return the hostname. */
  public String getHostname() {
    return hostname;
  }

  /** Return <code>true</code> iff the host is this host. */
  public boolean isThisHost() {
    return thisHost;
  }

  @Override
  public String toString() {

    return "HostScore"
        + "{hostname="
        + hostname
        + ", thisHost="
        + thisHost
        + ", availabilty="
        + availability
        + "}";
  }

  /*
   * @param hostname The hostname (required, must be non-empty).
   * @param availability The normalized availability score for this host.
   */
  public HostScore(final String hostname, final double availability) {

    if (hostname == null) throw new IllegalArgumentException();

    if (hostname.trim().length() == 0) throw new IllegalArgumentException();

    if (availability < 0d || availability > 1d) throw new IllegalArgumentException();

    this.hostname = hostname;

    this.availability = availability;

    this.thisHost = AbstractStatisticsCollector.fullyQualifiedHostName.equals(hostname);
  }

  /*
   * Places elements into order by decreasing {@link #getAvailability() normalized load}. The {@link
   * #getHostname()} is used to break any ties.
   */
  public static final Comparator<HostScore> COMPARE_BY_SCORE =
      (t1, t2) -> {

        if (t1.availability < t2.availability) {

          return 1;

        } else if (t1.availability > t2.availability) {

          return -1;
        }

        return t1.hostname.compareTo(t2.hostname);
      };

  /*
   * Orders by hostname. This provides a stable way of viewing the data in <code>/embergraph/status
   * </code>.
   */
  public static final Comparator<HostScore> COMPARE_BY_HOSTNAME =
      (t1, t2) -> t1.hostname.compareTo(t2.hostname);
}
