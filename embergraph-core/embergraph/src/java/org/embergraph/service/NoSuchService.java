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
 * Created on Nov 17, 2008
 */

package org.embergraph.service;

/**
 * Exception thrown when a service was requested but has not been discovered or
 * is otherwise not available.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @todo this exception should be used consistently when the caller uses one of
 *       the service discover methods on {@link IEmbergraphFederation} and gets
 *       back a <code>null</code> instead of a service. For example,
 *       {@link IEmbergraphFederation#getMetadataService()}.
 */
public class NoSuchService extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -997167267628835644L;

    /**
     * 
     */
    public NoSuchService() {
    }

    /**
     * @param message
     */
    public NoSuchService(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public NoSuchService(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public NoSuchService(String message, Throwable cause) {
        super(message, cause);
    }

}
