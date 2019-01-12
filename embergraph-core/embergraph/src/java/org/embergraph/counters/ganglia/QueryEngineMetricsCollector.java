package org.embergraph.counters.ganglia;

import java.util.Iterator;
import org.apache.log4j.Logger;
import org.embergraph.counters.AbstractStatisticsCollector;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounter;
import org.embergraph.counters.ICounterSet;
import org.embergraph.ganglia.GangliaMunge;
import org.embergraph.ganglia.IGangliaMetricsCollector;
import org.embergraph.ganglia.IGangliaMetricsReporter;
import org.embergraph.journal.IIndexManager;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.service.IFederationDelegate;

/** Reflects query engine metrics from the data server nodes to ganglia. */
public class QueryEngineMetricsCollector implements IGangliaMetricsCollector {

  private static Logger log = Logger.getLogger(QueryEngineMetricsCollector.class);

  //    /*
//     * Match anything which does NOT include <code>.service.</code> in the
  //     * counter path.
  //     *
  //     * @see <a href="http://www.regular-expressions.info/lookaround.html">
  //     *      Regular Expressions Info </a>
  //     */
  //    static final Pattern filter;
  //    static {
  //
  //        // embergraph10.bigdata.com / service /
  //        // org.embergraph.service.IMetadataService /
  //        // dabb5034-9db0-4218-a6dd-f9032fc05ce6 / Query Engine /
  //        // blockedWorkQueueCount
  //
  //        filter = Pattern
  //                .compile("^.*/service/org.embergraph.service.IDataService/.*/Query Engine/.*$");
  //
  //    }

  //    private final IEmbergraphFederation<?> fed;
  private final IIndexManager indexManager;
  private final AbstractStatisticsCollector statisticsCollector;

  public QueryEngineMetricsCollector(
      final IIndexManager indexManager, final AbstractStatisticsCollector statisticsCollector) {

    if (indexManager == null) throw new IllegalArgumentException();

    if (statisticsCollector == null) throw new IllegalArgumentException();

    this.indexManager = indexManager;

    this.statisticsCollector = statisticsCollector;
  }

  @Override
  public void collect(final IGangliaMetricsReporter reporter) {

    // Common base path which is NOT included in the generated metric name.
    final String basePrefix =
        ICounterSet.pathSeparator
            + AbstractStatisticsCollector.fullyQualifiedHostName
            + ICounterSet.pathSeparator
            + statisticsCollector.getProcessName()
            + ICounterSet.pathSeparator;

    /*
     * Total path prefix for all metrics to be reported.
     *
     * Note: This includes the path components:
     * "/host/service/serviceIface/serviceUUID/"
     *
     * Thus it will NOT report per-host metrics.
     */
    final String pathPrefix = basePrefix + "Query Engine";

    if (indexManager instanceof IEmbergraphFederation) {

      // Note: Necessary for some kinds of things (lazily created).
      ((IFederationDelegate<?>) indexManager).reattachDynamicCounters();
    }

    final CounterSet counters = (CounterSet) indexManager.getCounters().getPath(pathPrefix);

    if (counters == null) {

      if (log.isInfoEnabled()) log.info("Counters not yet available for service.");

      return;
    }

    //		log.warn(fed.getCounters().toString());
    //		log.warn(counters.toString());

    @SuppressWarnings("rawtypes")
    final Iterator<ICounter> itr = counters.getCounters(null /*filter*/);

    while (itr.hasNext()) {

      final ICounter<?> c = itr.next();

      final Object value = c.getInstrument().getValue();

      // The full path to the metric name.
      final String path = c.getPath();

      /*
       * Reporting a bare metric name without any service indicator. This
       * works if there is just one QueryEngine per host. If there are
       * more then they will clobber one another's metrics.
       */
      final String s = path.substring(basePrefix.length());

      // Just the metric name (skips leading '/' and then translates the rest).
      final String metricName = GangliaMunge.munge(s.replace('/', '.'));

      reporter.setMetric(metricName, value);
    }
  }
}
