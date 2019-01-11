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
 * Created on Sep 17, 2008
 */

package org.embergraph.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.counters.IProcessCounters;
import org.embergraph.counters.httpd.CounterSetHTTPD;
import org.embergraph.io.DirectBufferPool;
import org.embergraph.util.httpd.AbstractHTTPD;

/**
 * Basic delegate for services that need to override the service UUID and
 * service interface reported to the {@link ILoadBalancerService}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DefaultServiceFederationDelegate<T extends AbstractService>
        implements IFederationDelegate<T> {

    protected final static Logger log = Logger
            .getLogger(DefaultServiceFederationDelegate.class);
    
    final protected T service;
    
    public DefaultServiceFederationDelegate(final T service) {
        
        if (service == null)
            throw new IllegalArgumentException();
        
        this.service = service;
        
    }

    public T getService() {
        
        return service;
        
    }
    
    public String getServiceName() {
        
        return service.getServiceName();
        
    }
    
    public UUID getServiceUUID() {
        
        return service.getServiceUUID();
        
    }
    
    public Class getServiceIface() {
       
        return service.getServiceIface();
        
    }

	public void reattachDynamicCounters() {

	    /* Reattaches the {@link DirectBufferPool} counters. */
		// The service's counter set hierarchy.
		final CounterSet serviceRoot = service.getFederation()
				.getServiceCounterSet();

		// Ensure path exists.
		final CounterSet tmp = serviceRoot.makePath(IProcessCounters.Memory);

		/*
		 * Add counters reporting on the various DirectBufferPools.
		 */
		synchronized (tmp) {

			// detach the old counters (if any).
			tmp.detach("DirectBufferPool");

			// attach the current counters.
			tmp.makePath("DirectBufferPool").attach(
					DirectBufferPool.getCounters());

        }


    }

    /**
     * Returns <code>true</code>
     */
    public boolean isServiceReady() {
        
        return true;

    }
    
    /**
     * NOP
     */
    public void didStart() {
        
    }
    
    /** NOP */
    public void serviceJoin(IService service, UUID serviceUUID) {

    }

    /** NOP */
    public void serviceLeave(UUID serviceUUID) {

    }

    public AbstractHTTPD newHttpd(final int httpdPort,
            final ICounterSetAccess access) throws IOException {
        
        return new CounterSetHTTPD(httpdPort, access, service) {

            @Override
            public Response doGet(final Request req) throws Exception {

                try {

                    reattachDynamicCounters();

                } catch (Exception ex) {

                    /*
                     * Typically this is because the live journal has been
                     * concurrently closed during the request.
                     */

                    log.warn("Could not re-attach dynamic counters: " + ex, ex);

                }

                return super.doGet(req);

            }

        };

    }

    /**
     * Writes the URL of the local httpd service for the {@link DataService}
     * onto a file named <code>httpd.url</code> in the specified directory.
     */
    protected void logHttpdURL(final File file) {

        // delete in case old version exists.
        file.delete();

        final String httpdURL = service.getFederation().getHttpdURL();

        if (httpdURL != null) {

            try {

                final Writer w = new BufferedWriter(new FileWriter(file));

                try {

                    w.write(httpdURL);

                } finally {

                    w.close();

                }

            } catch (IOException ex) {

                log.warn("Problem writing httpdURL on file: " + file);

            }

        }

    }

}
