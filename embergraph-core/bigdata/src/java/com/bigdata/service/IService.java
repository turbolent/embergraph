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
 * Created on Apr 28, 2008
 */

package com.bigdata.service;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * Common service interface.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IService extends Remote {

    /**
     * The unique identifier for this service.
     * <p>
     * Note: Some service discovery frameworks (Jini) will assign the service a
     * {@link UUID} asynchronously after a new service starts, in which case
     * this method will return <code>null</code> until the service
     * {@link UUID} has been assigned.
     * 
     * @return The unique data service identifier.
     * 
     * @throws IOException
     *             since you can use this method with RMI.
     */
    UUID getServiceUUID() throws IOException;
    
    /**
     * Return the most interesting interface for the service.
     * 
     * @throws IOException
     *             since you can use this method with RMI.
     */
    Class getServiceIface() throws IOException;

    /**
     * The host on which this service is running.
     * 
     * @throws IOException
     *             since you can use this method with RMI.
     */
    String getHostname() throws IOException;
    
    /**
     * Return name by which a user might recognize this service.
     * 
     * @throws IOException
     *             since you can use this method with RMI.
     */
    String getServiceName() throws IOException;

//    /**
//     * Return the service directory.
//     * 
//     * @throws IOException
//     *             since you can use this method with RMI.
//     */
//    File getServiceDirectory() throws IOException;

    /**
     * Destroy the service. If the service is running, it is shutdown
     * immediately and then destroyed. This method has the same signature as
     * {@link DestroyAdmin#destroy()}.
     * 
     * @throws RemoteException
     */
    void destroy() throws RemoteException;
    
}
