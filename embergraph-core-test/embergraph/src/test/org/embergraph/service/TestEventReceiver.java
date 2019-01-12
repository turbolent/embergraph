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
 * Created on Mar 15, 2009
 */

package org.embergraph.service;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase2;

import org.embergraph.bfs.EmbergraphFileSystem;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.io.SerializerUtil;
import org.embergraph.journal.IResourceLockService;
import org.embergraph.journal.ITransactionService;
import org.embergraph.journal.TemporaryStore;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.relation.locator.IResourceLocator;
import org.embergraph.service.EventReceiver.EventBTree;
import org.embergraph.service.ndx.IClientIndex;
import org.embergraph.sparse.SparseRowStore;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.httpd.AbstractHTTPD;

/**
 * Unit tests for the {@link EventReceiver}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestEventReceiver extends TestCase2 {

    /**
     * 
     */
    public TestEventReceiver() {
    }

    /**
     * @param arg0
     */
    public TestEventReceiver(String arg0) {
        super(arg0);
    }
    
    /**
     * Subclass overrides {@link #sendEvent()} to send to a configured
     * {@link EventReceiver} on the {@link MockFederation}.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    static class MyEvent extends Event {

        /**
         * 
         */
        private static final long serialVersionUID = 3987546249519888387L;

        /**
         * @param fed
         * @param resource
         * @param majorEventType
         */
        public MyEvent(IEmbergraphFederation fed, EventResource resource, Object majorEventType) {
            
            super(fed, resource, majorEventType);

        }

        protected MyEvent(final IEmbergraphFederation fed,
                final EventResource resource, final Object majorEventType,
                final Object minorEventType, final Map<String, Object> details) {

            super(fed, resource, majorEventType, minorEventType, details);

        }

        @Override
        public MyEvent newSubEvent(Object minorEventType) {

            return new MyEvent(this.fed, this.resource, this.majorEventType,
                    minorEventType, this.details);

        }
        
        /**
         * Serializes and then de-serializes the event and sends the
         * de-serialized reference to the {@link EventReceiver}. This simulates
         * RMI where the receiver always has a different instance for each
         * message received.
         */
        protected void sendEvent() throws IOException {

            final byte[] data = SerializerUtil.serialize(this);

            final MyEvent e = (MyEvent) SerializerUtil.deserialize(data);

            ((MockFederation) fed).eventReceiver.notifyEvent(e);
            
        }
        
    }
    
    /**
     * Test dispatch using both {@link Event#start()} and {@link Event#end()}.
     * 
     * @throws InterruptedException
     */
    public void test_start_end() throws InterruptedException {

        final EventBTree eventBTree = EventBTree.createTransient();
        
        final EventReceiver eventReceiver = new EventReceiver(
                1000/* eventHistoryMillis */, eventBTree);

        final IEmbergraphFederation fed = new MockFederation(eventReceiver);
        
        final Event e = new MyEvent(fed, new EventResource("testIndex"),
                "testEventType");

        assertEquals(0, eventReceiver.eventCache.size());

        e.start();

        /*
         * Verify that the event is now in the receiver's cache.
         */
        
        assertEquals(1, eventReceiver.eventCache.size());
        
        assertTrue(eventReceiver.eventCache.containsKey(e.eventUUID));
        
        assertFalse(eventReceiver.eventCache.get(e.eventUUID).complete);
        
        try {

            // wait a bit so that endTime != startTime.
            Thread.sleep(200/* ms */);

        } finally {

            e.end();

        }

        // the event is still in the cache.
        assertEquals(1, eventReceiver.eventCache.size());
        
        /*
         * Verify the event as recorded in the B+Tree.
         */
        
        assertEquals(1L, eventReceiver.rangeCount(e.startTime, e.startTime + 1));

        assertTrue(eventReceiver.rangeIterator(e.startTime, e.startTime + 1)
                .hasNext());
        
        final Event t = eventReceiver.rangeIterator(e.startTime,
                e.startTime + 1).next();

        assertTrue(t.complete);

        assertEquals(e.eventUUID, t.eventUUID);

        assertEquals(e.startTime, t.startTime);
        
        assertEquals(e.endTime, t.endTime);

        /*
         * Verify that the receiver will purge the event from its cache.
         */
        
        // wait until the event is old enough to be purged.
        Thread.sleep(eventReceiver.eventHistoryMillis);
        
        // request purge of old events.
        eventReceiver.pruneHistory(System.currentTimeMillis());
        
        // event is no longer in the cache.
        assertEquals(0, eventReceiver.eventCache.size());
        assertFalse(eventReceiver.eventCache.containsKey(e.eventUUID));
        
    }

    /**
     * Test dispatch using only {@link Event#end()} (instantaneous events).
     * 
     * @throws InterruptedException
     */
    public void test_endOnly() throws InterruptedException {

        final EventBTree eventBTree = EventBTree.createTransient();
        
        final EventReceiver eventReceiver = new EventReceiver(
                1000/* eventHistoryMillis */, eventBTree);

        final IEmbergraphFederation fed = new MockFederation(eventReceiver);
        
        final Event e = new MyEvent(fed, new EventResource("testIndex"),
                "testEventType");

        assertEquals(0, eventReceiver.eventCache.size());

        e.end();

        assertEquals(e.startTime, e.endTime);

        assertTrue(e.complete);

        /*
         * Verify that the event is in the receiver's cache.
         */

        assertEquals(1, eventReceiver.eventCache.size());
        
        assertTrue(eventReceiver.eventCache.containsKey(e.eventUUID));
        
        assertTrue(eventReceiver.eventCache.get(e.eventUUID).complete);

        /*
         * Verify the event as recorded in the B+Tree.
         */
        
        assertEquals(1L, eventReceiver.rangeCount(e.startTime, e.startTime+1));
        
        assertTrue(eventReceiver.rangeIterator(e.startTime, e.startTime + 1)
                .hasNext());
        
        final Event t = eventReceiver.rangeIterator(e.startTime,
                e.startTime + 1).next();

        assertTrue(t.complete);

        assertEquals(e.eventUUID, t.eventUUID);

        assertEquals(e.startTime, t.startTime);
        
        assertEquals(e.endTime, t.endTime);

        /*
         * Verify that the receiver will purge the event from its cache.
         */
        
        // wait until the event is old enough to be purged.
        Thread.sleep(eventReceiver.eventHistoryMillis);
        
        // request purge of old events.
        eventReceiver.pruneHistory(System.currentTimeMillis());
        
        // event is no longer in the cache.
        assertEquals(0, eventReceiver.eventCache.size());
        assertFalse(eventReceiver.eventCache.containsKey(e.eventUUID));
   
    }
    
    /**
     * Pumps a bunch of events through and then verifies that the start events
     * are correlated with the end events and that the size of the
     * {@link LinkedHashMap} is bounded by the #of incomplete events no older
     * than the configured eventHistoryMillis.
     * 
     * @throws InterruptedException
     */
    public void test_purgesHistory() throws InterruptedException {

        final long eventHistoryMillis = 1000L;
        
        final EventBTree eventBTree = EventBTree.createTransient();

        final EventReceiver eventReceiver = new EventReceiver(
                eventHistoryMillis, eventBTree);

        final IEmbergraphFederation fed = new MockFederation(eventReceiver);

        final Random r = new Random();
        
        final long begin = System.currentTimeMillis();
        long elapsed;
        int nevents = 0;
        while ((elapsed = System.currentTimeMillis() - begin) < eventHistoryMillis / 2) {
            
            final Event e = new MyEvent(fed, new EventResource("testIndex"),
                    "testEventType");

            if (r.nextDouble() < .2) {

                // instantaneous event.
                e.end();

            } else {

                /*
                 * event with duration.
                 */
                e.start();

                try {

                    Thread
                            .sleep(r.nextInt((int) eventHistoryMillis / 10)/* ms */);

                } finally {

                    e.end();

                }

            }

            nevents++;

        }

        // all the events should be in the cache.
        assertEquals(nevents, eventReceiver.eventCache.size());
        
        // sleep until the events should be expired.
        Thread.sleep(eventHistoryMillis);
        
        // prune the event cache using the specified timestamp.
        eventReceiver.pruneHistory(System.currentTimeMillis());

        // should be empty.
        assertEquals(0, eventReceiver.eventCache.size());
        
    }

    /**
     * Unit tests verifies that the APIs are thread-safe.
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    public void test_threadSafe() throws InterruptedException, ExecutionException {

        final long eventHistoryMillis = 1000L;
        
        final EventBTree eventBTree = EventBTree.createTransient();

        final EventReceiver eventReceiver = new EventReceiver(
                eventHistoryMillis, eventBTree);

        final IEmbergraphFederation fed = new MockFederation(eventReceiver);

        final ExecutorService exService = Executors
                .newCachedThreadPool(DaemonThreadFactory.defaultThreadFactory());

        final int nthreads = 10;
        final int nevents = 100;
        try {

            final List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();

            for (int i = 0; i < nthreads; i++) {

                tasks.add(new EventFactory(fed, nevents));

            }

            tasks.add( new EventConsumer(eventReceiver) );
            
            final List<Future<Void>> futures = exService.invokeAll(tasks);

            for(Future f : futures) {
                
                // verify no errors.
                f.get();
                
            }

        } finally {

            exService.shutdownNow();
            
        }
        
    }
    
    /**
     * Generates events.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    private static class EventFactory implements Callable<Void> {
        
        private final IEmbergraphFederation fed;
        private final int nevents;

        public EventFactory(final IEmbergraphFederation fed,
                final int nevents) {

            this.fed = fed;

            this.nevents = nevents;
            
        }

        public Void call() throws Exception {

            final Random r = new Random();

            for (int i = 0; i < nevents; i++) {

                final Event e = new MyEvent(fed,
                        new EventResource("testIndex"), "testEventType");

                if (r.nextDouble() < .2) {

                    // instantaneous event.
                    e.end();

                } else {

                    /*
                     * event with duration.
                     */
                    e.start();

                    try {

                        Thread.sleep(r.nextInt(100)/* ms */);

                    } catch (InterruptedException ex) {

                        break;
                        
                    } finally {

                        e.end();

                    }

                }

            }

            return null;
            
        }
        
    }

    /**
     * Reports on events (places a concurrent read load on the B+Tree on which
     * the events are stored).
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    static private class EventConsumer implements Callable<Void> {
       
        final IEventReportingService eventReportingService;
        
        public EventConsumer(final IEventReportingService eventReportingService) {
            
            this.eventReportingService = eventReportingService;
            
        }
        
        public Void call() throws Exception {
            
            for (int i = 0; i < 50; i++) {

                eventReportingService.rangeCount(0L, Long.MAX_VALUE);

                final Iterator<Event> itr = eventReportingService
                        .rangeIterator(0L, Long.MAX_VALUE);

                while (itr.hasNext()) {

                    itr.next();

                }

                Thread.sleep(100/* ms */);

            }

            return null;
            
        }
        
    }
    
    /**
     * Mock federation to support the unit tests in the outer class.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    static class MockFederation implements IEmbergraphFederation<IEventReceivingService> {

        private final IEventReceivingService eventReceiver;
        private final UUID serviceUUID = UUID.randomUUID();
        
        MockFederation(final IEventReceivingService eventReceiver) {
            
            this.eventReceiver = eventReceiver;
            
        }
        
        @Override
        public IEventReceivingService getService() {
            
            return eventReceiver;
            
        }
        
        @Override
        public Class<?> getServiceIface() {
        
            return IEventReceivingService.class;
//            return TestEventReceiver.class;
            
        }

        @Override
        public String getServiceName() {
  
            return IEventReceivingService.class.getName();
            
//            return TestEventReceiver.class.getName();
            
        }

        @Override
        public UUID getServiceUUID() {
            
            return serviceUUID;
            
        }

        /*
         * unused methods.
         */
        
        @Override
        public void destroy() {
            
        }

        @Override
        public void dropIndex(String name) {
            
        }

        @Override
        public IDataService getAnyDataService() {
            return null;
        }

        @Override
        public IEmbergraphClient getClient() {
            return null;
        }

        @Override
        public CounterSet getCounters() {
            return null;
        }

        @Override
        public IDataService getDataService(UUID serviceUUID) {
            return null;
        }

        @Override
        public IDataService getDataServiceByName(String name) {
            return null;
        }

        @Override
        public UUID[] getDataServiceUUIDs(int maxCount) {
            return null;
        }

        @Override
        public IDataService[] getDataServices(UUID[] uuid) {
            return null;
        }

        @Override
        public ExecutorService getExecutorService() {
            return null;
        }

        @Override
        public SparseRowStore getGlobalRowStore() {
            return null;
        }
        
        @Override
        public SparseRowStore getGlobalRowStore(final long timstamp) {
            return null;
        }

        @Override
        public String getHttpdURL() {
            return null;
        }

        @Override
        public IClientIndex getIndex(String name, long timestamp) {
            return null;
        }

        @Override
        public long getLastCommitTime() {
            return 0;
        }

        @Override
        public ILoadBalancerService getLoadBalancerService() {
            return null;
        }

        @Override
        public IMetadataIndex getMetadataIndex(String name, long timestamp) {
            return null;
        }

        @Override
        public IMetadataService getMetadataService() {
            return null;
        }

        @Override
        public String getServiceCounterPathPrefix() {
            return null;
        }

        @Override
        public CounterSet getServiceCounterSet() {
            return null;
        }

        @Override
        public ITransactionService getTransactionService() {
            return null;
        }

        @Override
        public boolean isDistributed() {
            return false;
        }

        @Override
        public boolean isScaleOut() {
            return false;
        }

        @Override
        public boolean isStable() {
            return false;
        }

        @Override
        public void registerIndex(IndexMetadata metadata) {
              
        }

        @Override
        public UUID registerIndex(IndexMetadata metadata, UUID dataServiceUUID) {
            return null;
        }

        @Override
        public UUID registerIndex(IndexMetadata metadata, byte[][] separatorKeys, UUID[] dataServiceUUIDs) {
            return null;
        }

        @Override
        public EmbergraphFileSystem getGlobalFileSystem() {
            return null;
        }

        @Override
        public IResourceLocator<?> getResourceLocator() {
            return null;
        }

        @Override
        public IResourceLockService getResourceLockService() {
            return null;
        }

        @Override
        public TemporaryStore getTempStore() {
            return null;
        }

        @Override
        public void didStart() {
              
        }

        @Override
        public boolean isServiceReady() {
            return false;
        }

        @Override
        public AbstractHTTPD newHttpd(int httpdPort, ICounterSetAccess access) throws IOException {
            return null;
        }

        @Override
        public void reattachDynamicCounters() {
            
        }

        @Override
        public void serviceJoin(IService service, UUID serviceUUID) {
            
        }

        @Override
        public void serviceLeave(UUID serviceUUID) {
            
        }

        @Override
        public CounterSet getHostCounterSet() {
            return null;
        }

        @Override
        public ScheduledFuture<?> addScheduledTask(Runnable task,
				long initialDelay, long delay, TimeUnit unit) {
			return null;
		}
		
      @Override
		public boolean getCollectPlatformStatistics() {
			return false;
		}

      @Override
		public boolean getCollectQueueStatistics() {
			return false;
		}

      @Override
		public int getHttpdPort() {
			return 0;
		}

        @Override
        public Iterator<String> indexNameScan(String prefix, long timestamp) {
            return null;
        }
        
        @Override
        public boolean isGroupCommit() {
           return false;
        }

		@Override
		public boolean isJiniFederation() {
			//BLZG-1370
			return false;
		}

    }
    
}
