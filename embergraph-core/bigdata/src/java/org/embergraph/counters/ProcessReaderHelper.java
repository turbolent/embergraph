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

package org.embergraph.counters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;

public abstract class ProcessReaderHelper extends
        AbstractProcessReader {

    /**
     * The {@link Reader} from which the output of the process will be read.
     */
    protected LineNumberReader r = null;

    public ProcessReaderHelper() {

    }

    /**
     * Creates a {@link LineNumberReader} from the {@link InputStream}.
     * 
     * @param is
     *            The input stream from which the output of the process will
     *            be read.
     */
    public void start(InputStream is) {

        if (r != null)
            throw new IllegalStateException();
        
        super.start( is );
        
        r = new LineNumberReader( new InputStreamReader( is ));
        
    }

    /**
     * Override to return the {@link ActiveProcess}.
     */
    abstract protected ActiveProcess getActiveProcess();
    
    /**
     * Returns the next line and blocks if a line is not available.
     * 
     * @return The next line.
     * 
     * @throws InterruptedException 
     * 
     * @throws IOException
     *             if the source is closed.
     * @throws InterruptedException
     *             if the thread has been interrupted (this is
     *             normal during shutdown).
     */
    public String readLine() throws IOException, InterruptedException {
        
//        final Thread t = Thread.currentThread();
        
        while(getActiveProcess().isAlive()) {
            
            if(Thread.interrupted()) {
                
                throw new InterruptedException();
                
            }
            
            if(!r.ready()) {
                
                Thread.sleep(100/*ms*/);
                
                continue;
                
            }

            final String s = r.readLine();
            
            if(log.isDebugEnabled()) {
                
                log.debug(s);
                
            }
            
            return s;
            
        }
        
        throw new IOException("Closed");
        
    }
    
    public void run() {
        
        try {
         
            readProcess();
            
        } catch (InterruptedException e) {
            
            if(log.isInfoEnabled())
                log.info("Interrupted - will halt.");
            
        } catch (Exception e) {
            
            /*
             * Note: An IOException here generally means that the process from
             * which we were reading has been killed. This can happen if you
             * kill it externally. It can also happen with immediate shutdown.
             * 
             * FIXME If performance counter collection is killed but the client
             * is not shutdown then counter collection SHOULD be restarted. This
             * is especially true for the data services since the load balancer
             * will otherwise not be able to measure their loads, recommend
             * moves, etc.
             */

            log.error("Counter collection halted: " + e.getMessage(), e);
            
        }
        
    }

    /**
     * Responsible for reading the data.
     * 
     * @throws Exception
     */
    abstract protected void readProcess() throws Exception;

}
