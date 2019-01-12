package org.embergraph.counters.ganglia;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import org.embergraph.counters.AbstractStatisticsCollector;
import org.embergraph.counters.AbstractStatisticsCollector.Options;
import org.embergraph.ganglia.DefaultMetadataFactory;
import org.embergraph.ganglia.GangliaMetadataFactory;
import org.embergraph.ganglia.GangliaService;
import org.embergraph.ganglia.GangliaSlopeEnum;
import org.embergraph.ganglia.IGangliaDefaults;
import org.embergraph.ganglia.IGangliaMetadataFactory;

/*
* A utility class which may be used to run the embergraph performance counter collection system
 * within an embedded {@link GangliaService}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 *     <p>TODO Provide *service* heartbeat. This would be a well known counter for the embedded
 *     GangliaService in the {@link #serviceName} namespace. The interval for that counter should be
 *     configured separately. The main purpose is to allow applications to decide that some service
 *     is missing.
 *     <p>TODO Research how to make ganglia recognize a value which is not being reported as "not
 *     available" rather than just painting the last reported value. Tmax? DMax?
 *     <p>TODO Can metrics be declared which automatically collect history from the sampled
 *     counters? It would be nice to abstract that stuff out of embergraph.
 *     <p>TODO We should be reporting out the CPU context switches and interrupts per second data
 *     from vmstat. Ganglia does not collect this stuff and it provides interesting insight into the
 *     CPU workload and instruction stalls, especially when correlated with the application workload
 *     (load, vs closure, vs query).
 */
public class EmbergraphGangliaService extends GangliaService {

  public EmbergraphGangliaService(
      String hostName,
      String serviceName,
      InetSocketAddress[] metricsServers,
      InetAddress listenGroup,
      int listenPort,
      boolean listen,
      boolean report,
      boolean mock,
      int quietPeriod,
      int initialDelay,
      int heartbeatInterval,
      int monitoringInterval,
      int globalDMax,
      IGangliaMetadataFactory metadataFactory) {

    super(
        hostName,
        serviceName,
        metricsServers,
        listenGroup,
        listenPort,
        listen,
        report,
        mock,
        quietPeriod,
        initialDelay,
        heartbeatInterval,
        monitoringInterval,
        globalDMax,
        metadataFactory);
  }

  /*
   * Runs a {@link GangliaService} as a standalone application.
   *
   * <p>Note: This routine is mainly for test as the primary purpose of the {@link GangliaService}
   * is to embed it within another application.
   *
   * @param args
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {

    /*
     * The host name for this host.
     */
    final String hostName = getCanonicalHostName();

    final String serviceName = GangliaService.class.getSimpleName();

    final int quietPeriod = IGangliaDefaults.QUIET_PERIOD;

    final int initialDelay = IGangliaDefaults.INITIAL_DELAY;

    /*
     * Note: Use ZERO (0) if you are running gmond on the same host. That
     * will prevent the GangliaService from transmitting a different
     * heartbeat, which would confuse gmond and gmetad.
     */
    final int heartbeatInterval = 0; // IFF using gmond.
    //		final int heartbeatInterval = IGangliaDefaults.HEARTBEAT_INTERVAL;

    final int monitoringInterval = 5; // IGangliaDefaults.MONITORING_INTERVAL;

    final InetAddress listenGroup = InetAddress.getByName(IGangliaDefaults.DEFAULT_GROUP);

    final int listenPort = IGangliaDefaults.DEFAULT_PORT;

    final String defaultUnits = IGangliaDefaults.DEFAULT_UNITS;

    final GangliaSlopeEnum defaultSlope = IGangliaDefaults.DEFAULT_SLOPE;

    final int defaultTMax = IGangliaDefaults.DEFAULT_TMAX;

    final int defaultDMax = IGangliaDefaults.DEFAULT_DMAX;

    final InetSocketAddress[] metricsServers =
        new InetSocketAddress[] {
          new InetSocketAddress(IGangliaDefaults.DEFAULT_GROUP, IGangliaDefaults.DEFAULT_PORT)
        };

    // Note: Factory is extensible (application can add its own delegates).
    final GangliaMetadataFactory metadataFactory =
        new GangliaMetadataFactory(
            new DefaultMetadataFactory(defaultUnits, defaultSlope, defaultTMax, defaultDMax));

    /*
     * Layer on the ability to (a) recognize and align host embergraph's
     * performance counters hierarchy with those declared by ganglia and;
     * (b) provide nice declarations for various application counters of
     * interest.
     */
    metadataFactory.add(
        new EmbergraphMetadataFactory(
            hostName, serviceName, defaultSlope, defaultTMax, defaultDMax, heartbeatInterval));

    // The embedded ganglia service.
    GangliaService service = null;

    AbstractStatisticsCollector statisticsCollector = null;

    try {

      service =
          new GangliaService(
              hostName,
              serviceName,
              metricsServers,
              listenGroup,
              listenPort,
              true, // listen
              true, // report
              false, // mock (does not transmit when true).
              quietPeriod,
              initialDelay,
              heartbeatInterval,
              monitoringInterval,
              defaultDMax,
              metadataFactory);

      /*
       * Start monitoring OS/platform metrics.
       */
      {
        final Properties properties = new Properties();

        properties.setProperty(Options.PROCESS_NAME, serviceName);

        properties.setProperty(
            Options.PERFORMANCE_COUNTERS_SAMPLE_INTERVAL, "" + monitoringInterval);

        statisticsCollector = AbstractStatisticsCollector.newInstance(properties);

        // Start collecting platform statistics.
        statisticsCollector.start();
      }

      // Collect and report host metrics.
      service.addMetricCollector(new HostMetricsCollector(statisticsCollector));

      // Collect and report service metrics.
      service.addMetricCollector(
          new ServiceMetricsCollector(statisticsCollector, null /* filter */));

      // Run the ganglia service.
      service.run();

    } finally {

      if (statisticsCollector != null) {
        // Stop host collection.
        statisticsCollector.stop();
      }
    }
  } // main()
}
