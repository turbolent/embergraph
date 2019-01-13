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

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.bindingSet.EmptyBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.GroupNodeBase;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.TermNode;
import org.embergraph.rdf.sparql.ast.service.EmbergraphNativeServiceOptions;
import org.embergraph.rdf.sparql.ast.service.EmbergraphServiceCall;
import org.embergraph.rdf.sparql.ast.service.IServiceOptions;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.BD;
import org.embergraph.relation.accesspath.ThickCloseableIterator;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/*
 * A factory for a service that simulates the VALUES syntax in SPARQL:
 *
 * <p>service bd:values { # service params bd:serviceParam bd:values.var ?var . bd:serviceParam
 * bd:values.val "val1" . bd:serviceParam bd:values.val "val2" . ... }
 */
public class ValuesServiceFactory extends AbstractServiceFactory {

  private static final Logger log = Logger.getLogger(ValuesServiceFactory.class);

  /** The URI service key. */
  public static final URI SERVICE_KEY = new URIImpl(BD.NAMESPACE + "values");

  /** The service params for this service. */
  public interface ValuesParams {

    /** The limit on the sample. */
    URI VAR = new URIImpl(SERVICE_KEY.stringValue() + ".var");

    /** The seed on the sample. */
    URI VAL = new URIImpl(SERVICE_KEY.stringValue() + ".val");
  }

  /*
   * Note: This could extend the base class to allow for search service
   * configuration options.
   */
  private final EmbergraphNativeServiceOptions serviceOptions;

  public ValuesServiceFactory() {

    serviceOptions = new EmbergraphNativeServiceOptions();
    serviceOptions.setRunFirst(true);
  }

  @Override
  public EmbergraphNativeServiceOptions getServiceOptions() {

    return serviceOptions;
  }

  public EmbergraphServiceCall create(
      final ServiceCallCreateParams params, final ServiceParams serviceParams) {

    final AbstractTripleStore store = params.getTripleStore();

    final ServiceNode serviceNode = params.getServiceNode();

    /*
     * Validate the predicates for a given service call.
     */
    verifyGraphPattern(store, serviceNode.getGraphPattern(), serviceParams);

    /*
     * Create and return the ServiceCall object which will execute this
     * query.
     */
    return new ValuesCall(store, getServiceOptions(), serviceParams);
  }

  /*
   * Verify that there is only a single statement pattern node and that the service parameters are
   * valid.
   */
  private void verifyGraphPattern(
      final AbstractTripleStore database,
      final GroupNodeBase<IGroupMemberNode> group,
      final ServiceParams serviceParams) {

    final Iterator<Map.Entry<URI, List<TermNode>>> it = serviceParams.iterator();

    if (!serviceParams.contains(ValuesParams.VAR)) {

      throw new RuntimeException("must provide a variable for: " + ValuesParams.VAR);
    }

    if (!serviceParams.contains(ValuesParams.VAL)) {

      throw new RuntimeException("must provide at least one value for: " + ValuesParams.VAL);
    }

    while (it.hasNext()) {

      final URI param = it.next().getKey();

      if (ValuesParams.VAR.equals(param)) {

        final List<TermNode> vars = serviceParams.get(param);

        if (vars == null || vars.size() != 1 || vars.get(0).isConstant()) {
          throw new RuntimeException("must provide exactly one variable for: " + param);
        }

      } else if (ValuesParams.VAL.equals(param)) {

        final List<TermNode> vals = serviceParams.get(param);

        if (vals == null || vals.size() == 0) {
          throw new RuntimeException("must provide at least one value for: " + param);
        }

        for (TermNode val : vals) {
          if (val.isVariable()) {
            throw new RuntimeException("must provide constant values for: " + param);
          }
        }

      } else {

        throw new RuntimeException("unrecognized param: " + param);
      }
    }
  }

  /*
   * Note: This has the {@link AbstractTripleStore} reference attached. This is not a {@link
   * Serializable} object. It MUST run on the query controller.
   */
  private static class ValuesCall implements EmbergraphServiceCall {

    private final AbstractTripleStore db;
    private final IServiceOptions serviceOptions;
    private final ServiceParams serviceParams;

    public ValuesCall(
        final AbstractTripleStore db,
        final IServiceOptions serviceOptions,
        final ServiceParams serviceParams) {

      if (db == null) throw new IllegalArgumentException();

      if (serviceOptions == null) throw new IllegalArgumentException();

      if (serviceParams == null) throw new IllegalArgumentException();

      this.db = db;
      this.serviceOptions = serviceOptions;
      this.serviceParams = serviceParams;
    }

    /** Run a sample index op over the access path. */
    @Override
    public ICloseableIterator<IBindingSet> call(final IBindingSet[] bc) {

      if (log.isInfoEnabled()) {
        log.info(bc.length);
        log.info(Arrays.toString(bc));
        log.info(serviceParams);
      }

      if (bc != null && bc.length > 0 && !bc[0].equals(EmptyBindingSet.INSTANCE)) {
        throw new RuntimeException("cannot run with incoming bindings");
      }

      final IVariable<IV> var = serviceParams.getAsVar(ValuesParams.VAR);

      final List<TermNode> vals = serviceParams.get(ValuesParams.VAL);

      final IBindingSet[] bSets = new IBindingSet[vals.size()];

      for (int i = 0; i < bSets.length; i++) {

        bSets[i] = new ListBindingSet();

        bSets[i].set(var, (IConstant<IV>) vals.get(i).getValueExpression());
      }

      return new ThickCloseableIterator<>(bSets, bSets.length);
    }

    @Override
    public IServiceOptions getServiceOptions() {

      return serviceOptions;
    }
  }
}
