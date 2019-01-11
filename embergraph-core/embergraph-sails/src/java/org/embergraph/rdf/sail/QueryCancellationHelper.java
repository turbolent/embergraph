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

package org.embergraph.rdf.sail;

import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.rdf.sail.model.RunningQuery;

/**
 *
 * This class encapsulate functionality that is common to the REST API and 
 * Embedded Graph deployments.
 * 
 * @author beebs
 *
 */
public class QueryCancellationHelper {
	
	static private final transient Logger log = Logger
            .getLogger(QueryCancellationHelper.class);
	
	public static void cancelQuery(final UUID queryId,
			final QueryEngine queryEngine) {

		final Collection<UUID> queryIds = new LinkedList<UUID>();
		
		queryIds.add(queryId);

		cancelQueries(queryIds, queryEngine);

	}
	
	public static void cancelQueries(final Collection<UUID> queryIds,
			final QueryEngine queryEngine) {

		for (UUID queryId : queryIds) {

			if (!tryCancelQuery(queryEngine, queryId)) {
				//TODO:  BLZG-1464  Unify Embedded and REST cancellation
				if (!tryCancelUpdate(queryEngine,queryId,null)) {
					queryEngine.addPendingCancel(queryId);
					if (log.isInfoEnabled()) {
						log.info("No such QUERY or UPDATE: " + queryId);
					}
				}
			}

		}
	}
	
	

	/**
     * Attempt to cancel a running SPARQL Query
     * 
     * @param queryEngine
     * @param queryId
     * @return
     */	
	public static boolean tryCancelQuery(final QueryEngine queryEngine,
            final UUID queryId) {

        final IRunningQuery q;
        try {

            q = queryEngine.getRunningQuery(queryId);

        } catch (RuntimeException ex) {

            /*
             * Ignore.
             * 
             * Either the IRunningQuery has already terminated or this is an
             * UPDATE rather than a QUERY.
             */

            return false;

        }

        if (q != null && q.cancel(true/* mayInterruptIfRunning */)) {

            if (log.isInfoEnabled())
                log.info("Cancelled query: " + queryId);
            
            return true;

        }

        return false;

    }

    /**
     * Attempt to cancel a running SPARQL UPDATE request.
     * @param context
     * @param queryId
     * @return
     */

	//This should not be used for the StatusServlet until the Embedded and REST API are unified.
	public static boolean tryCancelUpdate(final QueryEngine queryEngine,
			RunningQuery query) {


		if (query != null) {

			final IRunningQuery q;
			try {

				q = queryEngine.getRunningQuery(query.getQueryUuid());
				
				if(q != null && q.cancel(true /* interrupt when running */)) {
					return true;
				}

			} catch (RuntimeException ex) {

				/*
				 * Ignore.
				 * 
				 * Either the IRunningQuery has already terminated or this is an
				 * UPDATE rather than a QUERY.
				 */

				return false;

			}
		}
        		
        		
//
//            if (query.queryTask instanceof UpdateTask) {
//
//                final Future<Void> f = ((UpdateTask) query.queryTask).updateFuture;
//
//                if (f != null) {
//
//                    if (f.cancel(true/* mayInterruptIfRunning */)) {
//
//                        return true;
//
//                    }
//
//                }
//
//            }
//
//        }
//
        // Either not found or found but not running when cancelled.
        return false;

   }

    //TODO:  Unify the webapp and embedded
	//This should not be used for the StatusServlet until the Embedded and REST API are unified.
	public static boolean tryCancelUpdate(final QueryEngine queryEngine,
			final UUID queryId, final Future<Void> f) {

		final IRunningQuery q;
		try {

			q = queryEngine.getRunningQuery(queryId);

			if (q != null && q.cancel(true /* interrupt when running */)) {
				return true;
			}

		} catch (RuntimeException ex) {

			/*
			 * Ignore.
			 * 
			 * Either the IRunningQuery has already terminated or this is an
			 * UPDATE rather than a QUERY.
			 */

			return false;

		}

		if (f != null) {

			if (f.cancel(true/* mayInterruptIfRunning */)) {

				return true;

			}

		}


        // Either not found or found but not running when cancelled.
        return false;
    }

}
