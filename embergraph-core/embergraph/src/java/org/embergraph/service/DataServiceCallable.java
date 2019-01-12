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
 * Created on Apr 23, 2009
 */

package org.embergraph.service;


/**
 * Base class for {@link IDataServiceCallable}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class DataServiceCallable<T> extends FederationCallable<T>
        implements IDataServiceCallable {

    private transient DataService dataService;

    /**
     * Deserialization ctor.
     */
    public DataServiceCallable() {
    }

    /**
     * Sets the {@link DataService} reference and the {@link IEmbergraphFederation}
     * reference (if not already set).
     */
    synchronized public void setDataService(final DataService dataService) {

        if (dataService == null)
            throw new IllegalArgumentException();

        if (this.dataService != null && this.dataService != dataService)
            throw new IllegalStateException();

        this.dataService = dataService;

        setFederation(dataService.getFederation());
        
    }

    public DataService getDataService() {

        if (dataService == null)
            throw new IllegalStateException();

        return dataService;

    }
    
    /**
     * Return <code>true</code> iff the {@link DataService} reference has been
     * set.
     */
    public boolean isDataService() {
        
        return dataService != null;
        
    }

}
