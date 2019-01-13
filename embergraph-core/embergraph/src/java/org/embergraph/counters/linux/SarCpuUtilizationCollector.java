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
 * Created on Mar 26, 2008
 */

package org.embergraph.counters.linux;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.embergraph.counters.AbstractProcessCollector;
import org.embergraph.counters.AbstractProcessReader;
import org.embergraph.counters.ActiveProcess;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterHierarchy;
import org.embergraph.counters.IHostCounters;
import org.embergraph.counters.IInstrument;
import org.embergraph.counters.IRequiredHostCounters;
import org.embergraph.counters.ProcessReaderHelper;

/*
 * Collects statistics on the CPU utilization for the entire host using <code>sar -u</code>.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class SarCpuUtilizationCollector extends AbstractProcessCollector
    implements ICounterHierarchy, IRequiredHostCounters, IHostCounters {

  /*
   * Inner class integrating the current values with the {@link ICounterSet} hierarchy.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  abstract class I<T> implements IInstrument<T> {

    protected final String path;

    public String getPath() {

      return path;
    }

    public I(final String path) {

      if (path == null) throw new IllegalArgumentException();

      this.path = path;
    }

    @Override
    public long lastModified() {

      return lastModified.get();
    }

    /** @throws UnsupportedOperationException always. */
    @Override
    public void setValue(final T value, final long timestamp) {

      throw new UnsupportedOperationException();
    }
  }

  /*
   * Double precision counter with scaling factor.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  class DI extends I<Double> {

    protected final double scale;

    DI(final String path, final double scale) {

      super(path);

      this.scale = scale;
    }

    @Override
    public Double getValue() {

      final Double value = (Double) vals.get(path);

      // no value is defined.
      if (value == null) return 0d;

      final double d = value.doubleValue() * scale;

      return d;
    }
  }

  /*
   * Map containing the current values for the configured counters. The keys are paths into the
   * {@link CounterSet}. The values are the data most recently read from <code>sar</code>.
   */
  private final Map<String, Object> vals = new ConcurrentHashMap<>();

  /** The timestamp associated with the most recently collected values. */
  private final AtomicLong lastModified = new AtomicLong(System.currentTimeMillis());

  /*
   * @param interval The reporting interval in seconds.
   * @param kernelVersion
   */
  public SarCpuUtilizationCollector(final int interval, final KernelVersion kernelVersion) {

    super(interval);
  }

  @Override
  public List<String> getCommand() {

    final List<String> command = new LinkedList<>();

    command.add(SysstatUtil.getPath("sar").getPath());

    // Note: Request the CPU stats.
    command.add("-u");

    // Note: configured interval.
    command.add("" + getInterval());

    // Note: count of zero means to repeat for ever.
    command.add("0");

    return command;
  }

  @Override
  public CounterSet getCounters() {

    final CounterSet root = new CounterSet();

    @SuppressWarnings("rawtypes")
    final List<I> inst = new LinkedList<>();

    /*
     * Note: Counters are all declared as Double to facilitate aggregation.
     *
     * Note: sar reports percentages in [0:100] so we convert them to [0:1]
     * using a scaling factor.
     */

    inst.add(new DI(IRequiredHostCounters.CPU_PercentProcessorTime, .01d));

    inst.add(new DI(IHostCounters.CPU_PercentUserTime, .01d));
    inst.add(new DI(IHostCounters.CPU_PercentSystemTime, .01d));
    inst.add(new DI(IHostCounters.CPU_PercentIOWait, .01d));

    for (@SuppressWarnings("rawtypes") I i : inst) {

      root.addCounter(i.getPath(), i);
    }

    return root;
  }

  /*
   * Extended to force <code>sar</code> to use a consistent timestamp format regardless of locale by
   * setting <code>S_TIME_FORMAT="ISO"</code> in the environment.
   */
  @Override
  protected void setEnvironment(final Map<String, String> env) {

    super.setEnvironment(env);

    env.put("S_TIME_FORMAT", "ISO");
  }

  @Override
  public AbstractProcessReader getProcessReader() {

    return new SarReader();
  }

  /*
   * Sample output for <code>sar -u 1 10</code>
   *
   * <pre>
   *    Linux 2.6.22.14-72.fc6 (hostname)    2008-03-17
   *
   *   04:14:45 PM     CPU     %user     %nice   %system   %iowait    %steal     %idle
   *   04:14:46 PM     all      0.00      0.00      0.00      0.00      0.00    100.00
   *   ...
   *   Average:        all      0.00      0.00      0.00      0.00      0.00    100.00
   * </pre>
   *
   * There is a banner, which is followed by the a repeating sequence {blank line, header, data
   * line(s)}. This sequence repeats every so often when sar emits new headers.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class SarReader extends ProcessReaderHelper {

    @Override
    protected ActiveProcess getActiveProcess() {

      if (activeProcess == null) throw new IllegalStateException();

      return activeProcess;
    }

    public SarReader() {

      super();
    }

    @Override
    protected void readProcess() throws Exception {

      if (log.isInfoEnabled()) log.info("begin");

      for (int i = 0; i < 10 && !getActiveProcess().isAlive(); i++) {

        if (log.isInfoEnabled()) log.info("waiting for the readerFuture to be set.");

        Thread.sleep(100 /*ms*/);
      }

      if (log.isInfoEnabled()) log.info("running");

      // The most recently read header.
      String header;

      // skip banner.
      final String banner = readLine();

      if (log.isInfoEnabled()) log.info("banner: " + banner);

      {

        // skip blank line.
        final String blank = readLine();
        assert blank.trim().length() == 0 : "Expecting a blank line";

        // header.
        header = readLine();

        if (log.isInfoEnabled()) log.info("header: " + header);
      }

      while (true) {

        // data.
        final String data = readLine();

        if (data.trim().length() == 0) {

          header = readLine();

          if (log.isInfoEnabled()) log.info("header: " + header);

          continue;
        }

        try {

          //                *   04:14:45 PM     CPU     %user     %nice   %system   %iowait
          // %steal     %idle
          //                *   04:14:46 PM     all      0.00      0.00      0.00      0.00
          // 0.00    100.00

          //                {
          //                    final String s = data.substring(0, 11);
          //                    try {
          //                        lastModified = f.parse(s).getTime();
          //                        if(log.isInfoEnabled())
          //                            log.info("["
          //                                        + s
          //                                        + "] parsed as milliseconds="
          //                                        + lastModified
          //                                        + ", date="
          //                                        + DateFormat.getDateTimeInstance(
          //                                                DateFormat.FULL,
          //                                                DateFormat.FULL).format(new
          // Date(lastModified)));
          //                    } catch (Exception e) {
          //                        log.warn("Could not parse time: [" + s + "] : " + e);
          //                        // should be pretty close.
          //                        lastModified = System.currentTimeMillis();
          //                    }
          //                }
          /*
           * Note: This timestamp should be _very_ close to the value
           * reported by sysstat. Also, using the current time is MUCH
           * easier and less error prone than attempting to parse the TIME
           * OF DAY written by sysstat and correct it into a UTC time by
           * adjusting for the UTC time of the start of the current day,
           * which is what we would have to do.
           */
          lastModified.set(System.currentTimeMillis());

          //                final String user = data.substring(20-1, 30-1);
          ////              final String nice = data.substring(30-1, 40-1);
          //              final String system = data.substring(40-1, 50-1);
          //              final String iowait = data.substring(50-1, 60-1);
          ////              final String steal = data.substring(60-1, 70-1);
          //              final String idle = data.substring(70-1, 80-1);

          final String[] fields = SysstatUtil.splitDataLine(data);

          final String user = fields[2];
          //                final String nice = fields[3];
          final String system = fields[4];
          final String iowait = fields[5];
          //                final String steal = fields[6];
          final String idle = fields[7];

          if (log.isInfoEnabled())
            log.info(
                "\n%user="
                    + user
                    + ", %system="
                    + system
                    + ", iowait="
                    + iowait
                    + ", idle="
                    + idle
                    + "\n"
                    + header
                    + "\n"
                    + data);

          vals.put(IHostCounters.CPU_PercentUserTime, Double.parseDouble(user));

          vals.put(IHostCounters.CPU_PercentSystemTime, Double.parseDouble(system));

          vals.put(IHostCounters.CPU_PercentIOWait, Double.parseDouble(iowait));

          vals.put(
              IRequiredHostCounters.CPU_PercentProcessorTime, (100d - Double.parseDouble(idle)));

        } catch (Exception ex) {

          /*
           * Issue warning for parsing problems.
           */

          log.warn(ex.getMessage() + "\nheader: " + header + "\n  data: " + data, ex);
        }
      }
    }
  }
}
