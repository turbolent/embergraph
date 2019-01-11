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
 * Created on Mar 13, 2008
 */

package org.embergraph.counters;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.system.SystemUtil;

import org.embergraph.Banner;
import org.embergraph.counters.httpd.CounterSetHTTPD;
import org.embergraph.counters.linux.StatisticsCollectorForLinux;
import org.embergraph.counters.osx.StatisticsCollectorForOSX;
import org.embergraph.counters.win.StatisticsCollectorForWindows;
import org.embergraph.io.DirectBufferPool;
import org.embergraph.util.Bytes;
import org.embergraph.util.httpd.AbstractHTTPD;
import org.embergraph.util.httpd.Config;

/**
 * Base class for collecting data on a host. The data are described by a
 * hierarchical collection of {@link ICounterSet}s and {@link ICounter}s. A
 * {@link IRequiredHostCounters minimum set of counters} is defined which SHOULD
 * be available for decision-making. Implementations are free to report any
 * additional data which they can make available. Reporting is assumed to be
 * periodic, e.g., every 60 seconds or so. The purpose of these data is to
 * support decision-making concerning the over- and under-utilization of hosts
 * in support of load balancing of services deployed over those hosts.
 * <p>
 * An effort has been made to align the core set of counters for both Windows
 * and Un*x platforms so as to support the declared counters on all platforms.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
abstract public class AbstractStatisticsCollector implements IStatisticsCollector {

    protected static final String ps = ICounterSet.pathSeparator;
    
    final protected static Logger log = Logger
            .getLogger(AbstractStatisticsCollector.class);

    /** {@link InetAddress#getCanonicalHostName()} for this host. */
    static final public String fullyQualifiedHostName;

    /** The path prefix under which all counters for this host are found. */
    static final public String hostPathPrefix;

    static {
    
        fullyQualifiedHostName = Banner.getFullyqualifiedhostname();

        hostPathPrefix = ICounterSet.pathSeparator + fullyQualifiedHostName
                + ICounterSet.pathSeparator;

        if (log.isInfoEnabled()) {
//            log.info("hostname  : " + hostname);
            log.info("FQDN      : " + fullyQualifiedHostName);
            log.info("hostPrefix: " + hostPathPrefix);
        }
        
    }
    
    /** Reporting interval in seconds. */
    final protected int interval;
    
    /** The name of this process. */
    private final String processName;
    
    /**
     * The interval in seconds at which the counter values are read from the
     * host platform.
     */
    @Override
    public int getInterval() {

        return interval;
        
	}

	/**
	 * The name of the process (or more typically its service {@link UUID})
	 * whose per-process performance counters are to be collected.
	 */
	public String getProcessName() {

		return processName;

	}

	protected AbstractStatisticsCollector(final int interval,
			final String processName) {

        if (interval <= 0)
            throw new IllegalArgumentException();

        if(processName == null)
        	throw new IllegalArgumentException();
        
        if(log.isInfoEnabled()) log.info("interval=" + interval);
        
        this.interval = interval;
        
        this.processName = processName;
        
    }
    
//    /**
//     * Return the load average for the last minute if available and -1
//     * otherwise.
//     * <p>
//     * Note: The load average is available on 1.6+ JVMs.
//     * 
//     * @see OperatingSystemMXBean
//     */
//    public double getSystemLoadAverage()
//    {
//        
////        double version = Double.parseDouble(System.getProperty("java.vm.version"));
////      if(version>=1.6) {
//        
//        double loadAverage = -1;
//        
//        final OperatingSystemMXBean mbean = ManagementFactory
//                .getOperatingSystemMXBean();
//        
//        /*
//         * Use reflection since method is only available as of 1.6
//         */
//        Method method;
//        try {
//            method = mbean.getClass().getMethod("getSystemLoadAverage",
//                    new Class[] {});
//            loadAverage = (Double) method.invoke(mbean, new Object[] {});
//        } catch (SecurityException e) {
//            log.warn(e.getMessage(), e);
//        } catch (NoSuchMethodException e) {
//            // Note: method is only defined since 1.6
//            log.warn(e.getMessage(), e);
//        } catch (IllegalAccessException e) {
//            log.warn(e.getMessage(), e);
//        } catch (InvocationTargetException e) {
//            log.warn(e.getMessage(), e);
//        }
//
//        return loadAverage;
//
//    }
    
    /**
     * {@link CounterSet} hierarchy.
     */
    private CounterSet countersRoot;

    /**
     * Return the counter hierarchy. The returned hierarchy only includes those
     * counters whose values are available from the JVM. This collection is
     * normally augmented with platform specific performance counters collected
     * using an {@link AbstractProcessCollector}.
     * <p>
     * Note: Subclasses MUST extend this method to initialize their own
     * counters.
     * 
     * TODO Why does this use the older <code>synchronized</code> pattern with a
     * shared {@link #countersRoot} object rather than returning a new object
     * per request? Check assumptions in the scale-out and local journal code
     * bases for this.
     */
	@Override
    synchronized 
    public CounterSet getCounters() {
        
        if (countersRoot == null) {

//        final CounterSet 
            countersRoot = new CounterSet();

            // os.arch
            countersRoot.addCounter(hostPathPrefix
                    + IHostCounters.Info_Architecture,
                    new OneShotInstrument<String>(System.getProperty("os.arch")));
            
            // os.name
            countersRoot.addCounter(hostPathPrefix
                    + IHostCounters.Info_OperatingSystemName,
                    new OneShotInstrument<String>(System.getProperty("os.name")));
            
            // os.version
            countersRoot.addCounter(hostPathPrefix
                    + IHostCounters.Info_OperatingSystemVersion,
                    new OneShotInstrument<String>(System.getProperty("os.version")));
            
            // #of processors.
            countersRoot.addCounter(hostPathPrefix
                    + IHostCounters.Info_NumProcessors,
                    new OneShotInstrument<Integer>(SystemUtil.numProcessors()));
            
            // processor info
            countersRoot.addCounter(hostPathPrefix
                    + IHostCounters.Info_ProcessorInfo,
                    new OneShotInstrument<String>(SystemUtil.cpuInfo()));
            
        }
        
        return countersRoot;
        
    }

    /**
     * Adds the Info and Memory counter sets under the <i>serviceRoot</i>.
     * 
     * @param serviceRoot
     *            The {@link CounterSet} corresponding to the service (or
     *            client).
     * @param serviceName
     *            The name of the service.
     * @param serviceIface
     *            The class or interface that best represents the service or
     *            client.
     * @param properties
     *            The properties used to configure that service or client.
     */
    static public void addBasicServiceOrClientCounters(
            final CounterSet serviceRoot, final String serviceName,
            final Class serviceIface, final Properties properties) {
        
        // Service info.
        {

            final CounterSet serviceInfoSet = serviceRoot.makePath("Info");

            serviceInfoSet.addCounter("Service Type",
                    new OneShotInstrument<String>(serviceIface.getName()));

            serviceInfoSet.addCounter("Service Name",
                    new OneShotInstrument<String>(serviceName));

            AbstractStatisticsCollector.addServiceProperties(serviceInfoSet,
                    properties);
            
        }

        serviceRoot.attach(getMemoryCounterSet());
        
    }

    /**
     * Return the {@link IProcessCounters#Memory memory counter set}. This
     * should be attached to the service root.
     */
    static public CounterSet getMemoryCounterSet() {
        
        final CounterSet serviceRoot = new CounterSet();
        
        // Service per-process memory data
        {

            serviceRoot.addCounter(
                    IProcessCounters.Memory_runtimeMaxMemory,
                    new OneShotInstrument<Long>(Runtime.getRuntime().maxMemory()));

            serviceRoot.addCounter(IProcessCounters.Memory_runtimeFreeMemory,
                    new Instrument<Long>() {
                        @Override
                        public void sample() {
                            setValue(Runtime.getRuntime().freeMemory());
                        }
                    });

            serviceRoot.addCounter(IProcessCounters.Memory_runtimeTotalMemory,
                    new Instrument<Long>() {
                        @Override
                        public void sample() {
                            setValue(Runtime.getRuntime().totalMemory());
                        }
                    });

            // add counters for garbage collection.
            AbstractStatisticsCollector
                    .addGarbageCollectorMXBeanCounters(serviceRoot
                            .makePath(ICounterHierarchy.Memory_GarbageCollectors));

            // add counters for memory pools.
            AbstractStatisticsCollector
                    .addMemoryPoolMXBeanCounters(serviceRoot
                            .makePath(ICounterHierarchy.Memory_Memory_Pools));

            /*
             * Add counters reporting on the various DirectBufferPools.
             */
            serviceRoot.makePath(
                    IProcessCounters.Memory + ICounterSet.pathSeparator
                            + "DirectBufferPool").attach(
                    DirectBufferPool.getCounters());

//          @see BLZG-1501 (remove LRUNexus)
//            if (LRUNexus.INSTANCE != null) {
//
//                /*
//                 * Add counters reporting on the global LRU and the per-store
//                 * caches.
//                 */
//
//                serviceRoot.makePath(
//                        IProcessCounters.Memory + ICounterSet.pathSeparator
//                                + "LRUNexus").attach(
//                        LRUNexus.INSTANCE.getCounterSet());
//
//            }
            
        }
        
        return serviceRoot;

    }

    /**
     * Lists out all of the properties and then report each property using a
     * {@link OneShotInstrument}.
     * 
     * @param serviceInfoSet
     *            The {@link ICounterHierarchy#Info} {@link CounterSet} for the
     *            service.
     * @param properties
     *            The properties to be reported out.
     */
    static public void addServiceProperties(final CounterSet serviceInfoSet,
            final Properties properties) {

        final CounterSet ptmp = serviceInfoSet.makePath("Properties");

        final Enumeration<?> e = properties.propertyNames();

        while (e.hasMoreElements()) {

            final String name;
            final String value;
            try {

                name = (String) e.nextElement();

                value = (String) properties.getProperty(name);

            } catch (ClassCastException ex) {

                log.warn(ex.getMessage());

                continue;

            }

            if (value == null)
                continue;

            ptmp.addCounter(name, new OneShotInstrument<String>(value));

        }

    }
    
    /**
     * Adds/updates counters relating to JVM Garbage Collection. These counters
     * should be located within a per-service path.
     * 
     * @param counterSet
     *            The counters set that is the direct parent.
     */
	static public void addGarbageCollectorMXBeanCounters(
			final CounterSet counterSet) {

        final String name_pools = "Memory Pool Names";

        final String name_count = "Collection Count";

        final String name_time = "Cumulative Collection Time";

        synchronized (counterSet) {

            final List<GarbageCollectorMXBean> list = ManagementFactory
                    .getGarbageCollectorMXBeans();

            for (final GarbageCollectorMXBean bean : list) {

                final String name = bean.getName();

                // counter set for this GC bean (may be pre-existing).
                final CounterSet tmp = counterSet.makePath(name);

                synchronized (tmp) {

                    // memory pool names.
                    {
                        if (tmp.getChild(name_pools) == null) {
                            
                            tmp.addCounter(name_pools,
                                    new Instrument<String>() {

                                        @Override
                                        protected void sample() {

                                            setValue(Arrays.toString(bean
                                                    .getMemoryPoolNames()));

                                        }
                        
                            });
                        
                        }
                        
                    }

                    // collection count.
                    {
                        if (tmp.getChild(name_count) == null) {
                            tmp.addCounter(name_count, new Instrument<Long>() {

                                @Override
                                protected void sample() {

                                    setValue(bean.getCollectionCount());

                                }
                            });
                        }
                    }

                    // collection time.
                    {
                        if (tmp.getChild(name_time) == null) {
                            tmp.addCounter(name_time, new Instrument<Long>() {

                                @Override
                                protected void sample() {

                                    setValue(bean.getCollectionTime());

                                }
                            });
                        }
                    }

                }

            }

        }

    }
    
    
    /**
     * Adds/updates counters relating to JVM Memory Pools. These counters
     * should be located within a per-service path.
     * 
     * @param counterSet
     *            The counters set that is the direct parent.
     */
    static public void addMemoryPoolMXBeanCounters(
            final CounterSet counterSet) {

        final String name_pool = "Memory Pool";

        final String name_max = "Maximum Usage";

        final String name_used = "Current Usage";

        synchronized (counterSet) {

            final List<MemoryPoolMXBean> list = ManagementFactory
                    .getMemoryPoolMXBeans();

            for (final MemoryPoolMXBean bean : list) {

                final String name = bean.getName();

                // counter set for this GC bean (may be pre-existing).
                final CounterSet tmp = counterSet.makePath(name);

                synchronized (tmp) {

                    // memory pool names.
                    {
                        if (tmp.getChild(name_pool) == null) {
                            
                            tmp.addCounter(name_pool,
                                    new Instrument<String>() {

                                        @Override
                                        protected void sample() {

                                            setValue(bean.getName());

                                        }
                        
                            });
                        
                        }
                        
                    }

                    // usage (max). 
                    {
                        if (tmp.getChild(name_max) == null) {
                            tmp.addCounter(name_max, new Instrument<Long>() {

                                @Override
                                protected void sample() {

                                    final MemoryUsage u = bean.getUsage();
                                    
                                    setValue(u.getMax());

                                }
                            });
                        }
                    }

                    // usage (current)
                    {
                        if (tmp.getChild(name_used) == null) {
                            tmp.addCounter(name_used, new Instrument<Long>() {

                                @Override
                                protected void sample() {

                                    final MemoryUsage u = bean.getUsage();

                                    setValue(u.getUsed());

                                }
                            });
                        }
                    }

                }

            }

        }

    }

    /**
     * Start collecting host performance data -- must be extended by the
     * concrete subclass.
     */
    @Override
    public void start() {

        if (log.isInfoEnabled())
            log.info("Starting collection.");

        installShutdownHook();

    }

    /**
     * Stop collecting host performance data -- must be extended by the concrete
     * subclass.
     */
    @Override
    public void stop() {
        
        if (log.isInfoEnabled())
            log.info("Stopping collection.");

    }

    /**
     * Installs a {@link Runtime#addShutdownHook(Thread)} that executes
     * {@link #stop()}.
     * <p>
     * Note: The runtime shutdown hook appears to be a robust way to handle ^C
     * by providing a clean service termination. However, under eclipse (at
     * least when running under Windows) you may find that the shutdown hook
     * does not run when you terminate a Java application and that typedef
     * process build up in the Task Manager as a result. This should not be the
     * case during normal deployment.
     */
    protected void installShutdownHook() {
     
        final Thread t = new Thread() {
            
            @Override
            public void run() {
                
                AbstractStatisticsCollector.this.stop();
                
            }
            
        };
        
        t.setDaemon(true);
        
        Runtime.getRuntime().addShutdownHook(t);

    }

    /**
     * Options for {@link AbstractStatisticsCollector}
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    public interface Options {
        
        /**
         * The interval in seconds at which the performance counters of the host
         * platform will be sampled (default 60).
         */
        public String PERFORMANCE_COUNTERS_SAMPLE_INTERVAL = AbstractStatisticsCollector.class
                .getPackage().getName()
                + ".interval";

        public String DEFAULT_PERFORMANCE_COUNTERS_SAMPLE_INTERVAL = "60";

        /**
         * The name of the process whose per-process performance counters are to
         * be collected (required, no default). This causes the per-process
         * counters to be reported using the path:
         * 
         * <strong>/<i>fullyQualifiedHostname</i>/<i>processName</i>/...</strong>
         * <p>
         * Note: Services are generally associated with a {@link UUID} and that
         * {@link UUID} is generally used as the service name. A single host may
         * run many different services and will report the counters for each
         * service using the path formed as described above.
         */
        public String PROCESS_NAME = AbstractStatisticsCollector.class
                .getPackage().getName()
                + ".processName";
        
    }
    
    /**
     * Create an instance appropriate for the operating system on which the JVM
     * is running.
     * 
     * @param properties
     *            See {@link Options}
     * 
     * @throws UnsupportedOperationException
     *             If there is no implementation available on the operating
     *             system on which you are running.
     * 
     * @see Options
     */
    public static AbstractStatisticsCollector newInstance(
            final Properties properties) {

        final int interval = Integer.parseInt(properties.getProperty(
                Options.PERFORMANCE_COUNTERS_SAMPLE_INTERVAL,
                Options.DEFAULT_PERFORMANCE_COUNTERS_SAMPLE_INTERVAL));

        if (interval <= 0)
            throw new IllegalArgumentException();
        
        final String processName = properties.getProperty(Options.PROCESS_NAME);
        
        if (processName == null)
            throw new IllegalArgumentException(
                    "Required option not specified: " + Options.PROCESS_NAME);
        
//        final String osname = System.getProperty("os.name").toLowerCase();
//        
//        if(osname.equalsIgnoreCase("linux")) {
        if(SystemUtil.isLinux()) {
            
            return new StatisticsCollectorForLinux(interval, processName);
            
//        } else if(osname.contains("windows")) {
            
        } else if(SystemUtil.isWindows()) {
            
            return new StatisticsCollectorForWindows(interval, processName);
            
//        } else if(osname.contains("os x")) {
            
        } else if(SystemUtil.isOSX()) {

        	return new StatisticsCollectorForOSX(interval, processName);
            
        } else {
            
            throw new UnsupportedOperationException(
                    "No implementation available on "
                            + System.getProperty("os.getname"));
            
        }
        
    }
    
    /**
     * Utility runs the {@link AbstractStatisticsCollector} appropriate for your
     * operating system. Before performance counter collection starts the static
     * counters will be written on stdout. The appropriate process(es) are then
     * started to collect the dynamic performance counters. Collection will
     * occur every {@link Options#PERFORMANCE_COUNTERS_SAMPLE_INTERVAL} seconds.
     * The program will make 10 collections by default and will write the
     * updated counters on stdout every
     * {@link Options#PERFORMANCE_COUNTERS_SAMPLE_INTERVAL} seconds.
     * <p>
     * Parameters also may be specified using <code>-D</code>. See
     * {@link Options}.
     * 
     * @param args <code>[<i>interval</i> [<i>count</i>]]</code>
     *            <p>
     *            <i>interval</i> is the collection interval in seconds and
     *            defaults to
     *            {@link Options#DEFAULT_PERFORMANCE_COUNTERS_SAMPLE_INTERVAL}.
     *            <p>
     *            <i>count</i> is the #of collections to be made and defaults
     *            to <code>10</code>. Specify zero (0) to run until halted.
     * 
     * @throws InterruptedException
     * @throws RuntimeException
     *             if the arguments are not valid.
     * @throws UnsupportedOperationException
     *             if no implementation is available for your operating system.
     */
    public static void main(final String[] args) throws InterruptedException {
        Banner.banner();
        final int DEFAULT_COUNT = 10;
        final int nargs = args.length;
        final int interval;
        final int count;
        if (nargs == 0) {
            interval = Integer.parseInt(Options.DEFAULT_PERFORMANCE_COUNTERS_SAMPLE_INTERVAL);
            count = DEFAULT_COUNT;
        } else if (nargs == 1) {
            interval = Integer.parseInt(args[0]);
            count = DEFAULT_COUNT;
        } else if (nargs == 2) {
            interval = Integer.parseInt(args[0]);
            count = Integer.parseInt(args[1]);
        } else {
            throw new RuntimeException("usage: [interval [count]]");
        }
        
        if (interval <= 0)
            throw new RuntimeException("interval must be positive");
        
        if (count < 0)
            throw new RuntimeException("count must be non-negative");

        final Properties properties = new Properties(System.getProperties());
        
        if (nargs != 0) {
            
            // Override the interval property from the command line.
            properties.setProperty(Options.PERFORMANCE_COUNTERS_SAMPLE_INTERVAL,""+interval);
            
        }

        if(properties.getProperty(Options.PROCESS_NAME)==null) {
            
            /*
             * Set a default process name if none was specified in the
             * environment.
             * 
             * Note: Normally the process name is specified explicitly by the
             * service which instantiates the performance counter collection for
             * that process. We specify a default here since main() is used for
             * testing purposes only.
             */

            properties.setProperty(Options.PROCESS_NAME,"testService");
            
        }
        
        final AbstractStatisticsCollector client = AbstractStatisticsCollector
                .newInstance( properties );

        final CounterSet counterSet = client.getCounters();
        
        counterSet.attach(getMemoryCounterSet());
        
        // write counters before we start the client
        System.out.println(counterSet.toString());
        
        System.err.println("Starting performance counter collection: interval="
                + client.interval + ", count=" + count);
        
        client.start();

        /*
         * HTTPD service reporting out statistics.
         */
        AbstractHTTPD httpd = null;
        {
            final int port = Config.HTTP_PORT;
            if (port != 0) {
                try {
                    httpd = new CounterSetHTTPD(port, client);
                } catch (IOException e) {
                    log.warn("Could not start httpd: port=" + port+" : "+e);
                }
            }
            
        }
        
        int n = 0;
        
        final long begin = System.currentTimeMillis();
        
        // Note: runs until killed when count==0.
        while (count == 0 || n < count) {
        
            Thread.sleep(client.interval * 1000/*ms*/);

            final long elapsed = System.currentTimeMillis() - begin;
            
            System.err.println("Report #"+n+" after "+(elapsed/1000)+" seconds ");
            
            System.out.println(client.getCounters().toString());
            
            n++;
            
        }
        
        System.err.println("Stopping performance counter collection");
        
        client.stop();

        if (httpd != null)
            httpd.shutdown();
        
        System.err.println("Done");
        
    }

    /**
     * Converts KB to bytes.
     * 
     * @param kb
     *            The #of kilobytes.
     *            
     * @return The #of bytes.
     */
    static public Double kb2b(final String kb) {

        final double d = Double.parseDouble(kb);
        
        final double x = d * Bytes.kilobyte32;
        
        return x;
        
    }

}
