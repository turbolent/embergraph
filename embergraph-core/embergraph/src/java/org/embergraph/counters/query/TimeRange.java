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

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Utility class to interpret URL query parameters as a time range. Some examples of supported
 * expressions include:
 *
 * <dl>
 *   <dt>12m
 *   <dd>12 minutes into the available history.
 *   <dt>-4h
 *   <dd>4 hours before the end of the available history.
 * </dl>
 *
 * The general form is either <code>([+-]?)([0-9]+)([mhd])</code>.
 *
 * <p>The implied fromTime is the start of the available history. The implied toTime is the end of
 * the available history. The implied units are {@link TimeUnit#MINUTES}.
 *
 * <p>Some query examples are:
 *
 * <dl>
 *   <dt>fromTime=-10m&period=Minutes
 *   <dd>The last 10 minutes of the available history.
 *   <dt>toTime=+4h
 *   <dd>The first 4 hours of the available history, with one sample every minute.
 *   <dt>toTime=+4h&period=Hours
 *   <dd>The first 4 hours of the available history, with one sample every hour.
 *   <dt>fromTime=+2h&toTime=+4h
 *   <dd>The data from the 2nd hour up to (but not including) the 4th hour of the available history,
 *       with one sample every minute.
 * </dl>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @todo fence posts all over for toTime, which needs to be correctly adjusted to reveal the end of
 *     the last minute of interest by placing the toTime to the first millisecond beyond the end of
 *     that minute.
 * @todo if no suffix then directly gives the milliseconds since the epoch?
 * @todo the {@link XHTMLRenderer.URLQueryModel} should also support fromDate and toDate, where the
 *     values are dates/date-times and where there is an optional URL query parameter to specify the
 *     format string used to interpret the data/time.
 */
public class TimeRange {

  /*
   * @todo <code>true</code> iff the adjusted time is {@link #dur} {@link #unit}s after some
   *     (unspecified) reference time; <code>false</code> iff the adjusted time is {@link #dur}
   *     {@link #unit}s before some (unspecified) reference time; and <code>null</code> if {@link
   *     #dur} should be directly interpreted as a time in the specified {@link #unit}s.
   */
  public final Boolean relative;

  /** The units in which the duration was specified. */
  public final TimeUnit unit;

  /** The duration in the specified units. */
  public final long dur;

  /*
   * @todo Return the adjusted timestamp. This is {@link #dur} unless {@link #relative} is <code>
   *     true</code> in which case it is the given timestamp <i>minus</i> {@link #dur}.
   * @param ts A reference timestamp from within the available history.
   * @return The exclusive upper bound.
   */
  public long getAdjustedTimestamp(final long ts) {

    final long ms = unit.toMillis(dur);

    if (relative) {

      return ms;
    }

    return ts - ms;
  }

  public TimeRange(final String s) {

    if (s == null) throw new IllegalArgumentException();

    final Matcher m = pattern.matcher(s);

    if (!m.matches()) throw new IllegalArgumentException(s);

    // anchored from the start unless prefixed with '-'.
    relative = !("-".equals(m.group(1)));

    final char ch = m.group(2).charAt(0);

    switch (ch) {
      case 'm':
        unit = TimeUnit.MINUTES;
        break;
      case 'h':
        unit = TimeUnit.HOURS;
        break;
      case 'd':
        unit = TimeUnit.DAYS;
        break;
      default:
        throw new AssertionError();
    }

    // duration.
    dur = Long.valueOf(m.group(1));
  }

  /** */
  protected static final Pattern pattern = Pattern.compile("([+-]?)([0-9]+)([mhd])");
}
