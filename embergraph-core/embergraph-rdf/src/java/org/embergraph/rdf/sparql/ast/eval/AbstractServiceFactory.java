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
 * Created on Sep 9, 2011
 */

package org.embergraph.rdf.sparql.ast.eval;

import org.embergraph.rdf.sparql.ast.service.BigdataServiceCall;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.rdf.sparql.ast.service.ServiceFactory;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.store.AbstractTripleStore;

/**
 * An abstract {@link ServiceFactory} that deals with service parameters (magic
 * predicates that configure the service) in a standardized manner using the
 * {@link ServiceParams} helper class.
 */
public abstract class AbstractServiceFactory extends AbstractServiceFactoryBase {

    public AbstractServiceFactory() {
    
    }

    /**
     * Create a {@link BigdataServiceCall}.  Does the work of collecting
     * the service parameter triples and then delegates to 
     * {@link #create(ServiceCallCreateParams, ServiceParams)}.
     */
    @Override
    final public BigdataServiceCall create(final ServiceCallCreateParams params) {

        if (params == null)
            throw new IllegalArgumentException();

        final AbstractTripleStore store = params.getTripleStore();

        if (store == null)
            throw new IllegalArgumentException();

        final ServiceNode serviceNode = params.getServiceNode();

        if (serviceNode == null)
            throw new IllegalArgumentException();

        final ServiceParams serviceParams = ServiceParams.gatherServiceParams(params);
        
        return create(params, serviceParams);
        
    }
    
    /**
     * Implemented by subclasses - verify the group and create the service call.
     */
    public abstract BigdataServiceCall create(
    		final ServiceCallCreateParams params,
    		final ServiceParams serviceParams);
    
}
