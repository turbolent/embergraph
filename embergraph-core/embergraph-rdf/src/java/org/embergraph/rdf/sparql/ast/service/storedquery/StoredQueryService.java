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

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Arrays;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.EmbergraphSailTupleQuery;
import org.embergraph.rdf.sail.Sesame2EmbergraphIterator;
import org.embergraph.rdf.sparql.ast.eval.ASTEvalHelper;
import org.embergraph.rdf.sparql.ast.eval.AbstractServiceFactoryBase;
import org.embergraph.rdf.sparql.ast.eval.ServiceParams;
import org.embergraph.rdf.sparql.ast.service.ExternalServiceCall;
import org.embergraph.rdf.sparql.ast.service.IServiceOptions;
import org.embergraph.rdf.sparql.ast.service.OpenrdfNativeServiceOptions;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.sparql.ast.service.ServiceRegistry;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.task.AbstractApiTask;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;

/**
 * A SERVICE that exposes a stored query for execution. The stored query may be a SPARQL query or
 * arbitrary procedural application logic, but it must evaluate to a solution multi-set. The service
 * interface is written to the openrdf interfaces in order to remove the burden of dealing with
 * embergraph {@link IV}s from the application.
 *
 * <p>In order to use a stored query, a concrete instance of this class must be registered against
 * the {@link ServiceRegistry}. The choice of the SERVICE URI is up to the application. The
 * effective value of the baseURI during query evaluation will be the SERVICE URI.
 *
 * <pre>
 * final URI serviceURI = new URIImpl(
 *         &quot;http://www.embergraph.org/rdf/stored-query#my-stored-query&quot;);
 *
 * ServiceRegistry.getInstance().add(serviceURI, new MyStoredQueryService());
 * </pre>
 *
 * Thereafter, the stored query may be referenced from SPARQL using its assigned service URI:
 *
 * <pre>
 * SELECT * {
 *    SERVICE <http://www.embergraph.org/rdf/stored-query#my-stored-query> { }
 * }
 * </pre>
 *
 * The SERVICE invocation may include a group graph pattern that will be parsed and made accessible
 * to the stored query service as a {@link ServiceParams} object. For example:
 *
 * <pre>
 * SELECT * {
 *    SERVICE <http://www.embergraph.org/rdf/stored-query#my-stored-query> {
 *       bd:serviceParam :color :"blue" .
 *       bd:serviceParam :color :"green" .
 *       bd:serviceParam :size  :"large" .
 *    }
 * }
 * </pre>
 *
 * will provide the stored query with two bindings for the <code>:color = {"blue", "green"}</code>
 * and one binding for <code>:size = {"large"}</code>. The value key names, the allowed value types
 * for each key name, and the interpretation of those values are all specific to a given stored
 * query service implementation class. They will be provided to that class as a {@link
 * ServiceParams} object.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="http://trac.blazegraph.com/ticket/989">Stored Query Service</a>
 *     <p>FIXME Wiki page.
 *     <p>FIXME Generalize to support groovy scripting.
 *     <p>TODO We could use {@link ASTEvalHelper} to evaluate at the embergraph level without
 *     forcing the materialization of any variable bindings from the lexicon indices. This would be
 *     faster for some purposes, especially if the stored procedure is only used to JOIN into an
 *     outer query as in <code>SELECT * { SERVICE bsq:my-service {} }</code>
 */
public abstract class StoredQueryService extends AbstractServiceFactoryBase {

  public interface Options {

    //        /**
    //         * The namespace used for stored query service.
    //         */
    //        String NAMESPACE = "http://www.embergraph.org/rdf/stored-query#";

  }

  private static final transient Logger log = Logger.getLogger(StoredQueryService.class);

  private final OpenrdfNativeServiceOptions serviceOptions;

  public StoredQueryService() {

    serviceOptions = new OpenrdfNativeServiceOptions();
  }

  @Override
  public IServiceOptions getServiceOptions() {

    return serviceOptions;
  }

  @Override
  public final ExternalServiceCall create(final ServiceCallCreateParams params) {

    if (params == null) throw new IllegalArgumentException();

    final AbstractTripleStore store = params.getTripleStore();

    if (store == null) throw new IllegalArgumentException();

    final ServiceNode serviceNode = params.getServiceNode();

    if (serviceNode == null) throw new IllegalArgumentException();

    final ServiceParams serviceParams = ServiceParams.gatherServiceParams(params);

    return create(params, serviceParams);
  }

  public ExternalServiceCall create(
      final ServiceCallCreateParams createParams, final ServiceParams serviceParams) {

    /*
     * Create and return the ServiceCall object which will execute this
     * query.
     */

    return new StoredQueryServiceCall(createParams, serviceParams);
  }

  /**
   * Abstract method for core application logic. The implementation may execute a SPARQL query, or a
   * series or SPARQL or other operations under application control.
   *
   * @param cxn The connection that should be used to read on the SPARQL database. The connection
   *     will be closed by the caller.
   * @param createParams The SERVICE creation parameters.
   * @param serviceParams The SERVICE invocation parameters.
   * @return The solution multi-set.
   * @throws Exception
   */
  protected abstract TupleQueryResult doQuery(
      final EmbergraphSailRepositoryConnection cxn,
      final ServiceCallCreateParams createParams,
      final ServiceParams serviceParams)
      throws Exception;

  private class StoredQueryServiceCall implements ExternalServiceCall {

    private final ServiceCallCreateParams createParams;
    private final ServiceParams serviceParams;

    public StoredQueryServiceCall(
        final ServiceCallCreateParams createParams, final ServiceParams serviceParams) {

      if (createParams == null) throw new IllegalArgumentException();

      if (serviceParams == null) throw new IllegalArgumentException();

      this.createParams = createParams;
      this.serviceParams = serviceParams;
    }

    @Override
    public IServiceOptions getServiceOptions() {

      return createParams.getServiceOptions();
    }

    @Override
    public ICloseableIterator<BindingSet> call(final BindingSet[] bindingSets) throws Exception {

      if (log.isInfoEnabled()) {
        log.info(bindingSets.length);
        log.info(Arrays.toString(bindingSets));
        log.info(serviceParams);
      }

      final AbstractTripleStore tripleStore = createParams.getTripleStore();

      final Future<TupleQueryResult> ft =
          AbstractApiTask.submitApiTask(
              tripleStore.getIndexManager(),
              new StoredQueryTask(
                  tripleStore.getNamespace(), tripleStore.getTimestamp(), bindingSets));

      try {

        final TupleQueryResult tupleQueryResult = ft.get();

        return new Sesame2EmbergraphIterator<BindingSet, QueryEvaluationException>(
            tupleQueryResult);

      } finally {

        ft.cancel(true /* mayInterruptIfRunning */);
      }
    }

    /**
     * Task to execute the stored query.
     *
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     */
    private class StoredQueryTask extends AbstractApiTask<TupleQueryResult> {

      /**
       * FIXME This is ignoring the exogenous bindings. This is more or less equivalent to bottom-up
       * evaluation. It would be more efficient if we could flow in the exogenous bindings but this
       * is not supported before openrdf 2.7 (we hack this in {@link EmbergraphSailTupleQuery}).
       */
      private final BindingSet[] bindingSets;

      public StoredQueryTask(
          final String namespace, final long timestamp, final BindingSet[] bindingSets) {

        super(namespace, timestamp);

        this.bindingSets = bindingSets;
      }

      @Override
      public boolean isReadOnly() {

        return true;
      }

      @Override
      public TupleQueryResult call() throws Exception {
        EmbergraphSailRepositoryConnection cxn = null;
        boolean success = false;
        try {
          // Note: Will be UPDATE connection if UPDATE request!!!
          cxn = getQueryConnection();
          if (log.isTraceEnabled()) log.trace("Query running...");
          final TupleQueryResult ret = doQuery(cxn, createParams, serviceParams);
          success = true;
          if (log.isTraceEnabled()) log.trace("Query done.");
          return ret;
        } finally {
          if (cxn != null) {
            if (!success && !cxn.isReadOnly()) {
              /*
               * Force rollback of the connection.
               *
               * Note: It is possible that the commit has already
               * been processed, in which case this rollback()
               * will be a NOP. This can happen when there is an
               * IO error when communicating with the client, but
               * the database has already gone through a commit.
               */
              try {
                // Force rollback of the connection.
                cxn.rollback();
              } catch (Throwable t) {
                log.error(t, t);
              }
            }
            try {
              // Force close of the connection.
              cxn.close();
            } catch (Throwable t) {
              log.error(t, t);
            }
          }
        }
      }
    } // StoredQueryApiTask
  } // StoredQueryServiceCall
} // StoredQueryService
