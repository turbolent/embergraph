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
 * Created on Mar 13, 2007
 */

package org.embergraph.resources;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.embergraph.btree.IndexSegmentBuilder;
import org.embergraph.cache.ICacheEntry;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.Instrument;
import org.embergraph.counters.OneShotInstrument;
import org.embergraph.journal.IConcurrencyManager;
import org.embergraph.mdi.LocalPartitionMetadata;
import org.embergraph.service.DataService;
import org.embergraph.service.IMetadataService;
import org.embergraph.service.MetadataService;
import org.embergraph.util.ReverseLongComparator;

/**
 * The {@link ResourceManager} has broad responsibility for journal files, index
 * segment files, maintaining index views during overflow processing, and
 * managing the transparent decomposition of scale-out indices and the
 * distribution of the key-range index partitions for those scale-out indices.
 * <p>
 * This class is implemented in several layers:
 * <dl>
 * <dt>{@link ResourceManager}</dt>
 * <dd>Concrete implementation.</dd>
 * <dt>{@link OverflowManager}</dt>
 * <dd>Overflow processing.</dd>
 * <dt>{@link IndexManager}</dt>
 * <dd>Manages indices</dd>
 * <dt>{@link StoreManager}</dt>
 * <dd>Manages the journal and index segment files, including the release of
 * old resources.</dd>
 * <dt>{@link ResourceEvents}</dt>
 * <dd>Event reporting API</dd>
 * </dl>
 * 
 * @todo Document backup procedures for the journal (the journal is a
 *       log-structured store; it can be deployed on RAID for media robustness;
 *       can be safely copied using normal file copy mechanisms and restored;
 *       can be compacted offline; a compact snapshot of a commit point (such as
 *       the last commit point) can be generated online and used for recovery;
 *       and can be exported onto index segments which can later be restored to
 *       any journal, but those index segments need additional metadata to
 *       recreate the appropriate relations which is found in the sparse row
 *       store) and the federation (relies on service failover (primary and
 *       secondaries) and a history retention policy; can be recovered from
 *       existing index partitions in a bottom up matter, but that recovery code
 *       has not been written).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class ResourceManager extends OverflowManager implements
        IPartitionIdFactory {

    /**
     * Logger.
     */
    protected static final Logger log = Logger.getLogger(ResourceManager.class);

    /**
     * Interface defines and documents the counters and counter namespaces for
     * the {@link ResourceManager}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static interface IResourceManagerCounters
        extends IOverflowManagerCounters, IIndexManagerCounters, IStoreManagerCounters {
    
        /**
         * The namespace for counters pertaining to the {@link OverflowManager}.
         */
        String OverflowManager = "Overflow Manager";

        /**
         * The namespace for counters pertaining for overflow tasks (within the
         * {@link #OverflowManager} namespace).
         */
        String IndexPartitionTasks = "Overflow Tasks";
        
        /**
         * The namespace for counters pertaining to the {@link IndexManager}.
         */
        String IndexManager = "Index Manager";
        
        /** 
         * The namespace for counters pertaining to the {@link StoreManager}.
         */
        String StoreManager = "Store Manager";

        /**
         * The namespace for counters pertaining to the live
         * {@link ManagedJournal}.
         * <p>
         * Note: these counters are detached and reattached to the new live
         * journal during overflow processing.
         */
        String LiveJournal = "Live Journal";
        
    }

//    /**
//     * <strong>WARNING: The {@link DataService} transfers all of the children
//     * from this object into the hierarchy reported by
//     * {@link AbstractFederation#getServiceCounterSet()} and this object will be
//     * empty thereafter.</strong>
//     */
    /*synchronized*/ public CounterSet getCounters() {
        
//        if (root == null) {

        final CounterSet root = new CounterSet();

            // ResourceManager
            {

                // ... nothing really - its all under other headings.

            }
            
            // Live Journal
            {
                
                /*
                 * Note: these counters are detached and reattached to the new
                 * live journal during overflow processing.
                 * 
                 * @todo This assumes that the StoreManager is running.
                 * Normally, this will be true since the DataService does not
                 * setup its counter set until the store manager is running and
                 * the service UUID has been assigned. However, eagerly
                 * requesting the counters set would violate that assumption and
                 * cause an exception to be thrown here since the live journal
                 * is not defined until the StoreManager is running.
                 * 
                 * It would be best to modify this to attach the live journal
                 * counters when the StoreManager startup completes successfully
                 * rather than assuming that it already has done so. However,
                 * the counter set for the ResourceManager is not currently
                 * defined until the StoreManager is running...
                 */

                root.makePath(IResourceManagerCounters.LiveJournal).attach(
                        getLiveJournal().getCounters());

            }
            
            // OverflowManager
            {

                final CounterSet tmp = root
                        .makePath(IResourceManagerCounters.OverflowManager);

                tmp.addCounter(IOverflowManagerCounters.OverflowEnabled,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(isOverflowEnabled());
                            }
                        });

                tmp.addCounter(IOverflowManagerCounters.OverflowAllowed,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(isOverflowAllowed());
                            }
                        });

                tmp.addCounter(IOverflowManagerCounters.ShouldOverflow,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(shouldOverflow());
                            }
                        });

                tmp.addCounter(IOverflowManagerCounters.SynchronousOverflowCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(getSynchronousOverflowCount());
                            }
                        });

                tmp.attach(overflowCounters.getCounters());

                /*
                 * Note: In order to report this counter we need access to the
                 * OverflowManager itself.
                 */
                tmp.addCounter(
                        IOverflowManagerCounters.AsynchronousOverflowMillis,
                        new Instrument<Long>() {
                            public void sample() {
                                long t = overflowCounters.asynchronousOverflowMillis
                                        .get();
                                if (isOverflowEnabled() && !isOverflowAllowed()) {
                                    /*
                                     * Include time from the active (ongoing)
                                     * asynchronous overflow operation.
                                     */
                                    t += (System.currentTimeMillis() - overflowCounters.asynchronousOverflowStartMillis
                                            .get());
                                }
                                setValue(t);
                            }
                        });

                /*
                 * These are some counters tracked by the IndexManager but
                 * reported under the same path as the OverflowManager's
                 * per-index partition task counters.
                 */
                {
                
                    final CounterSet tmp2 = tmp
                            .makePath(IResourceManagerCounters.IndexPartitionTasks);

                    tmp2.addCounter(IIndexPartitionTaskCounters.ConcurrentBuildCount,
                            new Instrument<Integer>() {
                                public void sample() {
                                    setValue(concurrentBuildTaskCount.get());
                                }
                            });

                    tmp2.addCounter(IIndexPartitionTaskCounters.ConcurrentMergeCount,
                            new Instrument<Integer>() {
                                public void sample() {
                                    setValue(concurrentMergeTaskCount.get());
                                }
                            });

                    tmp2.addCounter(IIndexPartitionTaskCounters.RunningBuilds,
                            new Instrument<String>() {
                                public void sample() {
                                    /*
                                     * Put the running tasks into order by their
                                     * elapsed execution time.
                                     */
                                    final TreeMap<Long/* elapsed */, IndexSegmentBuilder> map = new TreeMap<Long, IndexSegmentBuilder>(
                                            new ReverseLongComparator());
                                    final long now = System.currentTimeMillis();
                                    for (IndexSegmentBuilder task : buildTasks
                                            .values()) {
                                        final long startTime = task
                                                .getStartTime();
                                        if (startTime == 0) {
                                            // task has not started.
                                            continue;
                                        }
                                        final long elapsed = (startTime == 0 ? 0L
                                                : now - startTime);
                                        map.put(elapsed, task);
                                    }
                                    /*
                                     * Format the list of running tasks.
                                     */
//                                    int n = 0;
                                    final StringBuilder sb = new StringBuilder();
//                                    sb.append("There are " + map.size()
//                                            + " running index segment builds.");
                                    for(Map.Entry<Long, IndexSegmentBuilder> e : map.entrySet()) {
                                        final long elapsed = e.getKey();
                                        final IndexSegmentBuilder task = e
                                                .getValue();
//                                        if (n > 0)
//                                            sb.append(" ");
                                        final String name = task.metadata
                                                .getName();
                                        final LocalPartitionMetadata pmd = task.metadata
                                                .getPartitionMetadata();
                                        final int partitionId = (pmd == null ? -1
                                                : pmd.getPartitionId());
                                        final String cause = (pmd == null ? "N/A"
                                                : pmd.getIndexPartitionCause()
                                                        .toString());
                                        final String indexPartitionName = DataService
                                                .getIndexPartitionName(name,
                                                        partitionId);
                                        sb.append(indexPartitionName
                                                + "{elapsed=" + elapsed
                                                + "ms, cause=" + cause
                                                + ", compactingMerge="
                                                + task.compactingMerge
                                                + ", commitTime=" + task.commitTime
                                                + ", plan=" + task.plan + "} ");
                                    }
                                    setValue(sb.toString());
                                }
                            });

                }

            }

            // IndexManager
            {
                
                final CounterSet tmp = root
                        .makePath(IResourceManagerCounters.IndexManager);

//                // save a reference.
//                indexManagerRoot = tmp;
                
                tmp.addCounter(IIndexManagerCounters.StaleLocatorCacheCapacity,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(staleLocatorCache.capacity());
                            }
                        });
                
                tmp.addCounter(IIndexManagerCounters.StaleLocatorCacheSize,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(getStaleLocatorCount());
                            }
                        });
                
                if(false)
                    tmp.addCounter(IIndexManagerCounters.StaleLocators,
                        new Instrument<String>() {
                        public void sample() {
                            final StringBuilder sb = new StringBuilder();
                            final Iterator<ICacheEntry<String/* name */, StaleLocatorReason>> itr = staleLocatorCache
                                    .entryIterator();
                            while (itr.hasNext()) {
                                try {
                                    final ICacheEntry<String/* name */, StaleLocatorReason> entry = itr
                                            .next();
                                    sb.append(entry.getKey() + "="
                                            + entry.getObject() + "\n");
                                } catch (NoSuchElementException ex) {
                                    // Ignore - concurrent modification.
                                }
                            }
                            setValue(sb.toString());
                        }
                    });
                
                tmp.addCounter(IIndexManagerCounters.IndexCount,
                        new Instrument<Long>() {
                            public void sample() {
                                final ManagedJournal liveJournal = getLiveJournal();
                                final long lastCommitTime = liveJournal.getLastCommitTime();
                                if (lastCommitTime == 0L) {
                                    /*
                                     * This warning will be issued for the first
                                     * live journal for a data service since
                                     * there are no commit points until the
                                     * application registers an index on that
                                     * data service.
                                     */
                                    if(log.isInfoEnabled())
                                            log.info("No commit points on the live journal?");
                                    return;
                                }
                                final long indexCount = liveJournal
                                        .getName2Addr(lastCommitTime)
                                        .rangeCount();
                                setValue(indexCount);
                            }
                        });

                tmp.addCounter(IIndexManagerCounters.IndexCacheCapacity,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(getIndexCacheCapacity());
                            }
                        });
                
                tmp.addCounter(IIndexManagerCounters.IndexCacheSize,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(getIndexCacheSize());
                            }
                        });
                
                tmp.addCounter(IIndexManagerCounters.IndexSegmentCacheCapacity,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(getIndexSegmentCacheCapacity());
                            }
                        });
                
                tmp.addCounter(IIndexManagerCounters.IndexSegmentCacheSize,
                        new Instrument<Integer>() {
                            public void sample() {
                                setValue(getIndexSegmentCacheSize());
                            }
                        });

//                tmp.addCounter(IIndexManagerCounters.IndexSegmentOpenLeafCount,
//                        new Instrument<Integer>() {
//                            public void sample() {
//                                setValue(getIndexSegmentOpenLeafCount());
//                            }
//                        });
//
//                tmp.addCounter(IIndexManagerCounters.IndexSegmentOpenLeafByteCount,
//                        new Instrument<Long>() {
//                            public void sample() {
//                                setValue(getIndexSegmentOpenLeafByteCount());
//                            }
//                        });

                // attach the index partition counters.
                tmp.makePath(IIndexManagerCounters.Indices).attach(
                        getIndexCounters());

            }

            // StoreManager
            {

                final CounterSet tmp = root
                        .makePath(IResourceManagerCounters.StoreManager);

                tmp.addCounter(IStoreManagerCounters.DataDir,
                        new Instrument<String>() {
                            public void sample() {
                                setValue(dataDir == null ? "N/A" : dataDir
                                        .getAbsolutePath());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.TmpDir,
                        new Instrument<String>() {
                            public void sample() {
                                setValue(tmpDir == null ? "N/A" : tmpDir
                                        .getAbsolutePath());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.IsOpen,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(isOpen());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.IsStarting,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(isStarting());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.IsRunning,
                        new Instrument<Boolean>() {
                            public void sample() {
                                setValue(isRunning());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.StoreCacheCapacity,
                        new OneShotInstrument<Integer>(storeCache.capacity()));

                tmp.addCounter(IStoreManagerCounters.StoreCacheSize,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue((long) getStoreCacheSize());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.ManagedJournalCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(getManagedJournalCount());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.ManagedSegmentStoreCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(getManagedSegmentCount());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.JournalReopenCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(journalReopenCount.get());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.SegmentStoreReopenCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(segmentStoreReopenCount.get());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.JournalDeleteCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(journalDeleteCount.get());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.SegmentStoreDeleteCount,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(segmentStoreDeleteCount.get());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.BytesUnderManagement,
                        new Instrument<Long>() {
                            public void sample() {
                                if (isRunning()) {
                                    setValue(getBytesUnderManagement());
                                }
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.JournalBytesUnderManagement,
                        new Instrument<Long>() {
                            public void sample() {
                                if (isRunning()) {
                                    setValue(getJournalBytesUnderManagement());
                                }
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.SegmentBytesUnderManagement,
                        new Instrument<Long>() {
                            public void sample() {
                                if (isRunning()) {
                                    setValue(getSegmentBytesUnderManagement());
                                }
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.BytesDeleted,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(bytesDeleted.get());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.DataDirBytesAvailable,
                        new Instrument<Long>() {
                            public void sample() {
                                if (!isTransient())
                                    setValue(getDataDirFreeSpace());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.TmpDirBytesAvailable,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(getTempDirFreeSpace());
                            }
                        });

                tmp.addCounter(
                        IStoreManagerCounters.MaximumJournalSizeAtOverflow,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(maximumJournalSizeAtOverflow);
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.PurgeResourcesMillis,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(purgeResourcesMillis);
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.ReleaseTime,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(getReleaseTime());
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.LastOverflowTime,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(lastOverflowTime);
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.LastCommitTimePreserved,
                        new Instrument<Long>() {
                            public void sample() {
                                setValue(lastCommitTimePreserved);
                            }
                        });

                tmp.addCounter(IStoreManagerCounters.LastCommitTime,
                        new Instrument<Long>() {
                            public void sample() {
                                final ManagedJournal liveJournal;
                                try {
                                    liveJournal = getLiveJournal();
                                    setValue(liveJournal.getLastCommitTime());
                                } catch (Throwable t) {
                                    log.warn(t);
                                }
                            }
                        });

                /*
                 * Performance counters for the service used to let other data
                 * services read index segments or journals from this service.
                 */
                final CounterSet tmp2 = tmp.makePath("resourceService");

                tmp2.attach(getResourceService().counters.getCounters());
                
            }

//        }

        return root;

    }

//    private CounterSet root;

//    /**
//     * The counter set that corresponds to the {@link IndexManager}.
//     */
//    public CounterSet getIndexManagerCounters() {
//
//        if (indexManagerRoot == null) {
//            
//            getCounters();
//            
//        }
//        
//        return indexManagerRoot;
//        
//    }
//    private CounterSet indexManagerRoot;
        
    /**
     * {@link ResourceManager} options.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    public static interface Options extends OverflowManager.Options {
        
    }
    
    private IConcurrencyManager concurrencyManager;
    
    public IConcurrencyManager getConcurrencyManager() {

        if (concurrencyManager == null) {

            // Not assigned!

            throw new IllegalStateException();

        }

        return concurrencyManager;
        
    }

    public void setConcurrencyManager(
            final IConcurrencyManager concurrencyManager) {

        if (concurrencyManager == null)
            throw new IllegalArgumentException();

        if (this.concurrencyManager != null)
            throw new IllegalStateException();

        this.concurrencyManager = concurrencyManager;
        
    }

    /**
     * (Re-)open the {@link ResourceManager}.
     * <p>
     * Note: You MUST use {@link #setConcurrencyManager(IConcurrencyManager)}
     * after calling this constructor (the parameter can not be passed in since
     * there is a circular dependency between the {@link IConcurrencyManager}
     * and {@link ManagedJournal#getLocalTransactionManager()}.
     * 
     * @param properties
     *            See {@link Options}.
     * 
     * @see DataService#start()
     */
    public ResourceManager(final Properties properties) {

        super(properties);
        
    }

    /**
     * Requests a new index partition identifier from the
     * {@link MetadataService} for the specified scale-out index (RMI).
     * 
     * @return The new index partition identifier.
     * 
     * @throws RuntimeException
     *             if something goes wrong.
     */
    public int nextPartitionId(final String scaleOutIndexName) {

        final IMetadataService mds = getFederation().getMetadataService();

        if (mds == null) {

            throw new RuntimeException("Metadata service not discovered.");
            
        }
        
        try {

            // obtain new partition identifier from the metadata service (RMI)
            final int newPartitionId = mds.nextPartitionId(scaleOutIndexName);

            return newPartitionId;

        } catch (Throwable t) {

            throw new RuntimeException(t);

        }

    }

//    /**
//     * Impose flow control on data destined for the named index partition. When
//     * invoked, clients will no longer be authorized to buffer data on this data
//     * service to be written on the named index partition.  
//     * 
//     * @param name
//     *            The index partition.
//     * 
//     * @todo Implement flow control (this method is a place holder).
//     */
//    protected void suspendWrites(String name) {
//        
//        // NOP
//
//    }
//    
//    /**
//     * Allow clients to buffer writes for the named index partition.
//     * 
//     * @param name
//     * 
//     * @todo Implement flow control (this method is a place holder).
//     */
//    protected void resumeWrites(String name) {
//       
//        // NOP
//        
//    }

}
