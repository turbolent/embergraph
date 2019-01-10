/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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

package com.bigdata.counters;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;


/**
 * Base class for collection of performance counters as reported by a native process.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractProcessCollector implements IStatisticsCollector {

    protected static final Logger log = Logger.getLogger(AbstractProcessCollector.class);
    
    final private int interval;

    public int getInterval() {
        
        return interval;
        
    }
    
    protected ActiveProcess activeProcess;

    /**
     * 
     * @param interval
     *            The interval at which the performance counters will be
     *            read in milliseconds.
     */
    public AbstractProcessCollector(final int interval) {

        if (interval == 0)
            throw new IllegalArgumentException();
        
        this.interval = interval;
        
    }

    /**
     * Override if you want to impose settings on environment variables.
     */
    protected void setEnvironment(Map<String,String> env) {
        // Fixed numbers format for sysstat commands
        env.put("LC_NUMERIC", "C");
        
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Creates the {@link ActiveProcess} and the
     * {@link ActiveProcess#start(AbstractProcessReader)}s it passing in the
     * value returned by the {@link #getProcessReader()}
     */
    @Override
    public void start() {

        log.info("");
        
        activeProcess = new ActiveProcess(getCommand(), this);
        
        activeProcess.start(getProcessReader());

    }

    @Override
    public void stop() {

        log.info("");

        if (activeProcess != null) {

            activeProcess.stop();

            activeProcess = null;

        }

    }

    abstract public List<String> getCommand();

    abstract public AbstractProcessReader getProcessReader();

}
