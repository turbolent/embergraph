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
 * Created on Mar 29, 2012
 */

package org.embergraph.rdf.sparql.ast.service;

import org.apache.http.conn.ClientConnectionManager;
import org.eclipse.jetty.client.HttpClient;
import org.embergraph.bop.join.BaseJoinStats;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.URI;

/*
 * Interface for the parameters used by a {@link ServiceFactory} to create a {@link ServiceCall}
 * instance.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface ServiceCallCreateParams {

  /** The end point for which the {@link ServiceCall} will be invoked. */
  URI getServiceURI();

  /** The {@link AbstractTripleStore} against which the query is being evaluated. */
  AbstractTripleStore getTripleStore();

  /*
   * The embergraph AST object modeling the SPARQL <code>SERVICE</code> clause. This object provides
   * access to the parsed structure of the SERVICE graph pattern and the original text image of the
   * graph pattern (assuming that it was generated by parsing a SPARQL query). The {@link
   * ServiceFactory} can use this information to interpret the {@link ServiceCall} invocation
   * context.
   */
  ServiceNode getServiceNode();

  /** Return the {@link ClientConnectionManager} used to make remote SERVICE call requests. */
  HttpClient getClientConnectionManager();

  /** The configuration options associated with the {@link ServiceFactory}. */
  IServiceOptions getServiceOptions();

  /*
   * Statistics associated with the runtime evaluation of the service call. May be used by internal
   * services to report on, e.g., access path statistics.
   */
  BaseJoinStats getStats();
}
