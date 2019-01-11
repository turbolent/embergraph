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
 * Created on Apr 18, 2008
 */

package org.embergraph.counters.httpd;

import java.net.InetAddress;
import java.util.Random;

import junit.framework.TestCase;

import org.embergraph.counters.CounterSet;
import org.embergraph.counters.History;
import org.embergraph.counters.HistoryInstrument;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.counters.Instrument;
import org.embergraph.counters.OneShotInstrument;
import org.embergraph.counters.PeriodEnum;
import org.embergraph.util.config.NicUtil;

/**
 * Utility class for testing {@link CounterSetHTTPD} or
 * {@link CounterSetHTTPDServer}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestCounterSetHTTPDServer extends TestCase {
    
    /**
     * Invoked during server startup to allow customization of the
     * {@link CounterSet} exposed by the httpd server.
     * 
     * @param root
     */
    protected void setUp(final CounterSet root) throws Exception {

        final Random r = new Random();
        {

            final CounterSet cset = root.makePath("localhost");

            final String localIpAddr = NicUtil.getIpAddress("default.nic",
                    "default", true);

            cset.addCounter("hostname", new OneShotInstrument<String>(
                    localIpAddr));

            cset.addCounter("ipaddr",
                    new OneShotInstrument<String>(localIpAddr));

            // 60 minutes of data : @todo replace with CounterSetBTree (no fixed limit).
            final HistoryInstrument<Double> history1 = new HistoryInstrument<Double>(
                    new History<Double>(new Double[60], PeriodEnum.Minutes
                            .getPeriodMillis(), true/*overwrite*/));

            cset.addCounter("random", new Instrument<Integer>() {

                @Override
                protected void sample() {

                    final Integer val = r.nextInt(100);

                    setValue(val);

                    /*
                     * Note: A multiplier is used to have time, as reported to
                     * the history instrument, pass faster than the clock time.
                     * This lets you test the UI out time minutes, hours, and
                     * days in far less than the corresponding wall clock time.
                     */
                    final long timestamp = System.currentTimeMillis()*60*60;
                    
                    history1.setValue((double)value, timestamp);
                    
                }

            });

            cset.addCounter("history1", history1);
            
        }
        
        {
            
            final CounterSet cset = root.makePath("www.embergraph.org");

            cset.addCounter("ipaddr", new OneShotInstrument<String>(
                    InetAddress.getByName("www.embergraph.org").getHostAddress()));

            cset.makePath("foo").addCounter("bar",
                    new OneShotInstrument<String>("baz"));
            
        }
        
    }

    /**
     * Starts a {@link CounterSetHTTPDServer} with some synthetic data.
     * <p>
     * Note: This test does not exit by itself. You use it to test the server
     * from a web browser.
     * 
     * @throws Exception
     */
    public void test_server() throws Exception {

        final CounterSet counterSet = new CounterSet();
        
        final ICounterSetAccess access = new ICounterSetAccess() {
            
            public CounterSet getCounters() {
             
                return counterSet;
                
            }

        };
        
        final DummyEventReportingService service = new DummyEventReportingService();

        setUp(counterSet);

        final int port = 8081;

        final CounterSetHTTPDServer server = new CounterSetHTTPDServer(port,
                access, service);

        server.run();

    }

}
