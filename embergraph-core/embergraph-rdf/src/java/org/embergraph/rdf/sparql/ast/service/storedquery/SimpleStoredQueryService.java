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
package org.embergraph.rdf.sparql.ast.service.storedquery;

import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.EmbergraphSailTupleQuery;
import org.embergraph.rdf.sparql.ast.eval.ServiceParams;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;

/*
* Simple stored query consisting of a parameterized SPARQL query.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public abstract class SimpleStoredQueryService extends StoredQueryService {

  /** Return the SPARQL query to be evaluated. */
  protected abstract String getQuery(
      final ServiceCallCreateParams createParams, final ServiceParams serviceParams);

  /*
   * Executes the SPARQL query returned by {@link #getQuery(ServiceCallCreateParams, ServiceParams)}
   */
  @Override
  protected TupleQueryResult doQuery(
      final EmbergraphSailRepositoryConnection cxn,
      final ServiceCallCreateParams createParams,
      final ServiceParams serviceParams)
      throws Exception {

    final String queryStr = getQuery(createParams, serviceParams);

    final String baseURI = createParams.getServiceURI().stringValue();

    final EmbergraphSailTupleQuery query =
        cxn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr, baseURI);

    return query.evaluate();
  }
} // SimpleStoredQueryService
