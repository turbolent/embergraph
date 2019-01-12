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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.DefaultInstrumentFactory;
import org.embergraph.counters.History;
import org.embergraph.counters.History.SampleIterator;
import org.embergraph.counters.HistoryInstrument;
import org.embergraph.counters.ICounter;
import org.embergraph.counters.ICounterSet;
import org.embergraph.counters.ICounterSet.IInstrumentFactory;
import org.embergraph.counters.IHostCounters;
import org.embergraph.counters.IRequiredHostCounters;
import org.embergraph.counters.PeriodEnum;
import org.embergraph.counters.httpd.DummyEventReportingService;
import org.embergraph.journal.ConcurrencyManager.IConcurrencyManagerCounters;
import org.embergraph.resources.ResourceManager.IResourceManagerCounters;
import org.embergraph.resources.StoreManager.IStoreManagerCounters;
import org.embergraph.service.DataService.IDataServiceCounters;
import org.embergraph.util.Bytes;
import org.embergraph.util.concurrent.IQueueCounters.IThreadPoolExecutorTaskCounters;
import org.xml.sax.SAXException;

/*
 * Some static utility methods.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class QueryUtil {

  protected static final Logger log = Logger.getLogger(QueryUtil.class);

  /*
   * Return the data captured by {@link Pattern} from the path of the specified <i>counter</i>.
   *
   * <p>Note: This is used to extract the parts of the {@link Pattern} which are of interest -
   * presuming that you mark some parts with capturing groups and other parts that you do not care
   * about with non-capturing groups.
   *
   * <p>Note: There is a presumption that {@link Pattern} was used to select the counters and that
   * <i>counter</i> is one of the selected counters, in which case {@link Pattern} is KNOWN to match
   * {@link ICounterNode#getPath()}.
   *
   * @return The captured groups -or- <code>null</code> if there are no capturing groups.
   */
  public static String[] getCapturedGroups(final Pattern pattern, final ICounter counter) {

    if (counter == null) throw new IllegalArgumentException();

    if (pattern == null) {

      // no pattern, so no captured groups.
      return null;
    }

    final Matcher m = pattern.matcher(counter.getPath());

    // #of capturing groups in the pattern.
    final int groupCount = m.groupCount();

    if (groupCount == 0) {

      // No capturing groups.
      return null;
    }

    if (!m.matches()) {

      throw new IllegalArgumentException("No match? counter=" + counter + ", regex=" + pattern);
    }

    /*
     * Pattern is matched w/ at least one capturing group so assemble a
     * label from the matched capturing groups.
     */

    if (log.isDebugEnabled()) {
      log.debug("input  : " + counter.getPath());
      log.debug("pattern: " + pattern);
      log.debug("matcher: " + m);
      log.debug("result : " + m.toMatchResult());
    }

    final String[] groups = new String[groupCount];

    for (int i = 1; i <= groupCount; i++) {

      final String s = m.group(i);

      if (log.isDebugEnabled()) log.debug("group[" + i + "]: " + m.group(i));

      groups[i - 1] = s;
    }

    return groups;
  }

  /*
   * Generate a {@link Pattern} from the OR of zero or more strings which must be matched and zero
   * or more regular expressions which must be matched.
   *
   * @param filter A list of strings to be matched (may be null).
   * @param regex A list of regular expressions to be matched (may be null).
   * @return The {@link Pattern} -or- <code>null</code> if both collects are empty.
   */
  public static Pattern getPattern(
      final Collection<String> filter, final Collection<String> regex) {

    final Pattern pattern;

    // the regex that we build up (if any).
    final StringBuilder sb = new StringBuilder();

    /*
     * Joins multiple values for -filter together in OR of quoted patterns.
     */

    if (filter != null) {

      for (String val : filter) {

        if (log.isInfoEnabled()) log.info("filter" + "=" + val);

        if (sb.length() > 0) {

          // OR of previous pattern and this pattern.
          sb.append("|");
        }

        // non-capturing group.
        sb.append("(?:.*" + Pattern.quote(val) + ".*)");
      }
    }

    /*
     * Joins multiple values for -regex together in OR of patterns.
     */

    if (regex != null) {
      for (String val : regex) {

        if (log.isInfoEnabled()) log.info("regex" + "=" + val);

        if (sb.length() > 0) {

          // OR of previous pattern and this pattern.
          sb.append("|");
        }

        // Non-capturing group.
        sb.append("(?:" + val + ")");
      }
    }

    if (sb.length() > 0) {

      final String s = sb.toString();

      if (log.isInfoEnabled()) log.info("effective regex filter=" + s);

      pattern = Pattern.compile(s);

    } else {

      pattern = null;
    }

    return pattern;
  }

  /*
   * Generate a {@link Pattern} from the OR of zero or more regular expressions which must be
   * matched.
   *
   * @param regex A list of regular expressions to be matched (may be null).
   * @return The {@link Pattern} -or- <code>null</code> if both collects are empty.
   */
  public static Pattern getPattern(final Collection<Pattern> regex) {

    final StringBuilder sb = new StringBuilder();

    for (Pattern val : regex) {

      if (log.isInfoEnabled()) log.info("regex" + "=" + val);

      if (sb.length() > 0) {

        // OR of previous pattern and this pattern.
        sb.append("|");
      }

      // Non-capturing group.
      sb.append("(?:" + val + ")");
    }

    final String s = sb.toString();

    if (log.isInfoEnabled()) log.info("effective regex filter=" + s);

    return Pattern.compile(s);
  }

  /*
   * Read counters matching the optional filter from the file into the given {@link CounterSet}.
   *
   * @param file The file.
   * @param counterSet The {@link CounterSet}.
   * @param filter An optional filter.
   * @param nslots The #of periods worth of data to be retained. This is used when a counter not
   *     already present in <i>counterSet</i> is encountered and controls the #of slots to be
   *     retained by {@link History} allocated for that counter.
   * @param unit The unit in which the #of slots was expressed.
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public static void readCountersFromFile(
      final File file,
      final CounterSet counterSet,
      final Pattern filter,
      final int nslots,
      final PeriodEnum unit)
      throws IOException, SAXException, ParserConfigurationException {

    /*
     * @todo does not roll minutes into hours or hours into days until
     * overflow of the minutes, which is only after N days.
     *
     * @todo this can easily run out of memory if you try to read several
     * hours worth of performance counters without filtering them by a
     * regex.
     */
    final IInstrumentFactory instrumentFactory =
        new DefaultInstrumentFactory(nslots, unit, false /* overwrite */);

    readCountersFromFile(file, counterSet, filter, instrumentFactory);
  }

  /*
   * Read counters matching the optional filter from the file into the given {@link CounterSet}.
   *
   * @param file The file.
   * @param counterSet The {@link CounterSet}.
   * @param filter An optional filter.
   * @param instrumentFactory
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public static void readCountersFromFile(
      final File file,
      final CounterSet counterSet,
      final Pattern filter,
      final IInstrumentFactory instrumentFactory)
      throws IOException, SAXException, ParserConfigurationException {

    if (log.isInfoEnabled()) log.info("reading file: " + file);

    InputStream is = null;

    try {

      // use large buffers.  these files are 200-300M each!
      is = new BufferedInputStream(new FileInputStream(file), Bytes.megabyte32 * 1);

      counterSet.readXML(is, instrumentFactory, filter);

      if (log.isInfoEnabled()) {

        int n = 0;
        long firstTimestamp = 0;
        long lastTimestamp = 0;

        final Iterator<ICounter> itr = counterSet.getCounters(null /* filter */);

        while (itr.hasNext()) {

          final ICounter c = itr.next();

          n++;

          System.err.println("Retained: " + c);

          if (!(c.getInstrument() instanceof HistoryInstrument)) {

            continue;
          }

          final History h = ((HistoryInstrument) c.getInstrument()).getHistory();

          final SampleIterator sitr = h.iterator();

          final long firstSampleTime = sitr.getFirstSampleTime();

          final long lastSampleTime = sitr.getLastSampleTime();

          if (firstSampleTime > 0 && (firstSampleTime < firstTimestamp || firstTimestamp == 0)) {

            firstTimestamp = firstSampleTime;
          }

          if (lastSampleTime > lastTimestamp) {

            lastTimestamp = lastSampleTime;
          }
        }

        log.info(
            "There are now "
                + n
                + " counters covering "
                + new Date(firstTimestamp)
                + " : "
                + new Date(lastTimestamp));
      }

    } finally {

      if (is != null) {

        is.close();
      }
    }
  }

  /*
   * Task reads counters matching a regular expression into the caller's {@link CounterSet}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class ReadCounterSetXMLFileTask implements Callable<Void> {

    final File file;

    final CounterSet counterSet;

    final int nsamples;

    final PeriodEnum period;

    final Pattern regex;

    /*
     * @param file
     * @param counterSet
     * @param nsamples
     * @param period
     * @param regex
     */
    public ReadCounterSetXMLFileTask(
        final File file,
        final CounterSet counterSet,
        final int nsamples,
        final PeriodEnum period,
        final Pattern regex) {

      this.file = file;

      this.counterSet = counterSet;

      this.nsamples = nsamples;

      this.period = period;

      this.regex = regex;
    }

    public Void call() throws Exception {

      QueryUtil.readCountersFromFile(file, counterSet, regex, nsamples, period);

      return null;
    }

    public String toString() {

      return getClass()
          + "{ file="
          + file
          + ", nsamples="
          + nsamples
          + ", period="
          + period
          + ", regex="
          + regex
          + "}";
    }
  }

  /*
   * Read in {@link Event}s logged on a file in a tab-delimited format.
   *
   * @param service The events will be added to this service.
   * @param file The file from which the events will be read.
   * @throws IOException
   * @todo support reading the events.jnl, which should be opened in a read-only mode.
   */
  public static void readEvents(final DummyEventReportingService service, final File file)
      throws IOException {

    System.out.println("reading events file: " + file);

    BufferedReader reader = null;

    try {

      reader = new BufferedReader(new FileReader(file));

      service.readCSV(reader);

      System.out.println(
          "read " + service.rangeCount(0L, Long.MAX_VALUE) + " events from file: " + file);

    } finally {

      if (reader != null) {

        reader.close();
      }
    }
  }

  /*
   * Return the specified files, substituting recursively spanned files when a given file is a
   * directory.
   *
   * @param in A collection of zero or more files.
   * @param filter A filter which will select the files to be accepted.
   * @return A collection of the accepted files.
   */
  public static Collection<File> collectFiles(final Collection<File> in, final FileFilter filter) {

    if (in == null) throw new IllegalArgumentException();

    if (filter == null) throw new IllegalArgumentException();

    final Collection<File> out = new LinkedList<File>();

    for (File file : in) {

      if (file.isDirectory()) {

        // recursively process the files in the directory.

        if (log.isInfoEnabled()) log.info("Reading directory: " + file);

        final File[] files = file.listFiles(filter);

        out.addAll(collectFiles(Arrays.asList(files), filter));

      } else {

        // the file is not a directory.
        if (filter.accept(file)) {

          out.add(file);
        }
      }
    }

    return out;
  }

  /*
   * Return a {@link Pattern} which will match the minimum set of performance counters required by
   * the load balancer to perform its function.
   */
  public static Pattern getRequiredPerformanceCountersFilter() {

    return requiredPerformanceCountersFilter;
  }

  private static final String[] requiredPerformanceCounterPaths =
      new String[] {
        IRequiredHostCounters.Memory_majorFaultsPerSecond,
        IRequiredHostCounters.LogicalDisk_PercentFreeSpace,
        IRequiredHostCounters.CPU_PercentProcessorTime,
        IHostCounters.CPU_PercentIOWait,
        IDataServiceCounters.concurrencyManager
            + ICounterSet.pathSeparator
            + IConcurrencyManagerCounters.writeService
            + ICounterSet.pathSeparator
            + IThreadPoolExecutorTaskCounters.AverageQueuingTime,
        IDataServiceCounters.resourceManager
            + ICounterSet.pathSeparator
            + IResourceManagerCounters.StoreManager
            + ICounterSet.pathSeparator
            + IStoreManagerCounters.DataDirBytesAvailable,
        IDataServiceCounters.resourceManager
            + ICounterSet.pathSeparator
            + IResourceManagerCounters.StoreManager
            + ICounterSet.pathSeparator
            + IStoreManagerCounters.TmpDirBytesAvailable,
      };

  private static final Pattern requiredPerformanceCountersFilter =
      QueryUtil.getPattern(
          Arrays.asList(requiredPerformanceCounterPaths) /* strings */, null /* regex */);

  /*
   * Utility may be used to read the required performance counters for the load balancer from zero
   * or more files specified on the command line. The results are written using the XML interchange
   * format on stdout.
   *
   * @param args The file(s).
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public static void main(String[] args)
      throws IOException, SAXException, ParserConfigurationException {

    final Pattern filter = getRequiredPerformanceCountersFilter();

    System.err.println("required counter pattern: " + filter);

    final CounterSet counterSet = new CounterSet();

    for (String s : args) {

      final File file = new File(s);

      readCountersFromFile(
          file,
          counterSet,
          filter,
          new DefaultInstrumentFactory(60 /* slots */, PeriodEnum.Minutes, false /* overwrite */));
    }

    System.out.println("counters: " + counterSet.asXML(null /* filter */));
  }
}
