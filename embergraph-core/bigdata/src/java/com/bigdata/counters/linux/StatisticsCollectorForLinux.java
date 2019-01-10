package com.bigdata.counters.linux;

import java.util.UUID;

import com.bigdata.counters.AbstractStatisticsCollector;
import com.bigdata.counters.CounterSet;
import com.bigdata.counters.PIDUtil;

/**
 * Collection of host performance data using <code>vmstat</code> and
 * <code>sysstat</code> suite.
 * 
 * @see http://pagesperso-orange.fr/sebastien.godard/
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class StatisticsCollectorForLinux extends AbstractStatisticsCollector {

    /**
     * The process identifier for this process (the JVM).
     */
    static protected int pid;
    static {

        pid = PIDUtil.getLinuxPIDWithBash();

    }
    
    /**
     * The Linux {@link KernelVersion}.
     */
    static protected KernelVersion kernelVersion;
    static {

        kernelVersion = KernelVersion.get();

    }

    /**
     * The name of the process (or more typically its service {@link UUID})
     * whose per-process performance counters are to be collected.
     */
    protected final String processName;

    /**
     * Reports on the host CPU utilization (these data are also available using
     * vmstat).
     */
    protected SarCpuUtilizationCollector sar1;

    /**
     * Reports on the host page faults, swap space and can report CPU
     * utilization.
     */
    protected VMStatCollector vmstat;

    /**
     * reports on process performance counters (CPU, MEM, IO).
     */
    protected PIDStatCollector pidstat;

    public void start() {
        
        if (log.isInfoEnabled())
            log.info("starting collectors");
        
        super.start();
        
        if (sar1 != null)
			try {
				sar1.start();
			} catch (Throwable t) {
				log.error(t, t);
			}

        if (vmstat != null)
			try {
				vmstat.start();
			} catch (Throwable t) {
				log.error(t, t);
			}
        
        if (pidstat != null)
			try {
				pidstat.start();
			} catch (Throwable t) {
				log.error(t, t);
			}

    }

    public void stop() {

        if (log.isInfoEnabled())
            log.info("stopping collectors");
        
        super.stop();

        if (sar1 != null)
			try {
				sar1.stop();
			} catch (Throwable t) {
				log.error(t, t);
			}

        if (vmstat != null)
			try {
				vmstat.stop();
			} catch (Throwable t) {
				log.error(t, t);
			}

        if (pidstat != null)
			try {
				pidstat.stop();
			} catch (Throwable t) {
				log.error(t, t);
			}

    }
    
//    private boolean countersAdded = false;
    
    @Override
    /*synchronized*/ public CounterSet getCounters() {
        
        final CounterSet root = super.getCounters();
        
//        if( ! countersAdded ) {

            if (sar1 != null) {
             
                /*
                 * These are per-host counters. We attach them under the fully
                 * qualified hostname.
                 */

                root.makePath(fullyQualifiedHostName)
                        .attach(sar1.getCounters());
                
            }

            if (vmstat != null) {

                /*
                 * These are per-host counters. We attach them under the fully
                 * qualified hostname.
                 */

                root.makePath(fullyQualifiedHostName).attach(
                        vmstat.getCounters());
        
            }
            
            if (pidstat != null) {
            
                /*
                 * These are per-process counters. We attach them under a path
                 * described by the fully qualified hostname followed by the process
                 * name.
                 */

                root.makePath(fullyQualifiedHostName + ps + processName)
                        .attach(pidstat.getCounters());

            }
            
//            countersAdded = true;
//            
//        }
        
        return root;
        
    }
    
    /**
     * 
     * @param interval
     *            The interval at which the performance counters will be
     *            collected in seconds.
     * @param processName
     *            The name of the process (or more typically its service
     *            {@link UUID}) whose per-process performance counters are to
     *            be collected.
     */
    public StatisticsCollectorForLinux(final int interval,
            final String processName) {

        super(interval, processName);

        if (processName == null)
            throw new IllegalArgumentException();
        
        this.processName = processName;
        
        final boolean collectCPUStatsWithSAR = false;
        
        if(collectCPUStatsWithSAR) {

            // host wide collection
            sar1 = new SarCpuUtilizationCollector(interval,kernelVersion);
        
        } else {
            
            sar1 = null;
            
        }

        // host wide collection
        vmstat = new VMStatCollector(interval, !collectCPUStatsWithSAR);
        
        // process specific collection.
        pidstat = new PIDStatCollector(pid, interval, kernelVersion);

    }

}
