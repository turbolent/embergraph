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
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContextBase;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.ap.SampleIndex;
import org.embergraph.bop.bindingSet.EmptyBindingSet;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.sparql.ast.GroupNodeBase;
import org.embergraph.rdf.sparql.ast.IGroupMemberNode;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.TermNode;
import org.embergraph.rdf.sparql.ast.service.EmbergraphNativeServiceOptions;
import org.embergraph.rdf.sparql.ast.service.EmbergraphServiceCall;
import org.embergraph.rdf.sparql.ast.service.IServiceOptions;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.rdf.sparql.ast.service.ServiceFactory;
import org.embergraph.rdf.sparql.ast.service.ServiceNode;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.BD;
import org.embergraph.relation.accesspath.EmptyCloseableIterator;
import org.embergraph.relation.accesspath.ThickCloseableIterator;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;

/**
 * A factory for a statement pattern sampling service. It accepts a group with a single triple
 * pattern in it:
 *
 * <p>service bd:sample { ?s rdf:type ex:Foo .
 *
 * <p># optional service params for the sample bd:serviceParam bd:sample.limit 200 . bd:serviceParam
 * bd:sample.seed 0 . bd:serviceParam bd:sample.sampleType \"RANDOM\" . }
 *
 * <p>The service params are optional and let you set parameters on the sample.
 *
 * <p>This service will use the SampleIndex operator to take a random sample of tuples from an
 * access path.
 *
 * @see {@link SampleIndex}
 */
public class SampleServiceFactory extends AbstractServiceFactory implements ServiceFactory {

  private static final Logger log = Logger.getLogger(SampleServiceFactory.class);

  /** The URI service key. */
  public static final URI SERVICE_KEY = new URIImpl(BD.NAMESPACE + "sample");

  /** The service params for this service. */
  public interface SampleParams {

    /** The limit on the sample. */
    URI LIMIT = new URIImpl(SERVICE_KEY.stringValue() + ".limit");

    /** Default = 100. */
    int DEFAULT_LIMIT = SampleIndex.Annotations.DEFAULT_LIMIT;

    /** The seed on the sample. */
    URI SEED = new URIImpl(SERVICE_KEY.stringValue() + ".seed");

    /** Default = 0. */
    long DEFAULT_SEED = SampleIndex.Annotations.DEFAULT_SEED;

    /** The sample type. */
    URI SAMPLE_TYPE = new URIImpl(SERVICE_KEY.stringValue() + ".sampleType");

    /** Default = "RANDOM". */
    String DEFAULT_SAMPLE_TYPE = SampleIndex.Annotations.DEFAULT_SAMPLE_TYPE;
  }

  /*
   * Note: This could extend the base class to allow for search service
   * configuration options.
   */
  private final EmbergraphNativeServiceOptions serviceOptions;

  public SampleServiceFactory() {

    serviceOptions = new EmbergraphNativeServiceOptions();
    serviceOptions.setRunFirst(true);
  }

  @Override
  public EmbergraphNativeServiceOptions getServiceOptions() {

    return serviceOptions;
  }

  @Override
  public EmbergraphServiceCall create(
      final ServiceCallCreateParams params, final ServiceParams serviceParams) {

    final AbstractTripleStore store = params.getTripleStore();

    final ServiceNode serviceNode = params.getServiceNode();

    /*
     * Validate the predicates for a given service call.
     */
    final StatementPatternNode sp =
        verifyGraphPattern(store, serviceNode.getGraphPattern(), serviceParams);

    /*
     * Create and return the ServiceCall object which will execute this
     * query.
     */
    return new SampleCall(store, sp, getServiceOptions(), serviceParams);
  }

  /**
   * Verify that there is only a single statement pattern node and that the service parameters are
   * valid.
   */
  private StatementPatternNode verifyGraphPattern(
      final AbstractTripleStore database,
      final GroupNodeBase<IGroupMemberNode> group,
      final ServiceParams serviceParams) {

    final Iterator<Map.Entry<URI, List<TermNode>>> it = serviceParams.iterator();

    while (it.hasNext()) {

      final URI param = it.next().getKey();

      if (SampleParams.LIMIT.equals(param)) {

        if (serviceParams.getAsInt(param, null) == null) {
          throw new RuntimeException("must provide a value for: " + param);
        }

      } else if (SampleParams.SEED.equals(param)) {

        if (serviceParams.getAsLong(param, null) == null) {
          throw new RuntimeException("must provide a value for: " + param);
        }

      } else if (SampleParams.SAMPLE_TYPE.equals(param)) {

        if (serviceParams.getAsString(param, null) == null) {
          throw new RuntimeException("must provide a value for: " + param);
        }

      } else {

        throw new RuntimeException("unrecognized param: " + param);
      }
    }

    StatementPatternNode sp = null;

    for (IGroupMemberNode node : group) {

      if (!(node instanceof StatementPatternNode)) {

        throw new RuntimeException("only statement patterns allowed");
      }

      final StatementPatternNode tmp = (StatementPatternNode) node;

      if (tmp.s().isConstant() && BD.SERVICE_PARAM.equals(tmp.s().getValue())) {

        continue;
      }

      if (sp != null) {

        throw new RuntimeException("group must contain a single statement pattern");
      }

      sp = tmp;
    }

    return sp;
  }

  /**
   * Note: This has the {@link AbstractTripleStore} reference attached. This is not a {@link
   * Serializable} object. It MUST run on the query controller.
   */
  private static class SampleCall implements EmbergraphServiceCall {

    private final AbstractTripleStore db;
    private final StatementPatternNode sp;
    private final IServiceOptions serviceOptions;
    private final ServiceParams serviceParams;

    public SampleCall(
        final AbstractTripleStore db,
        final StatementPatternNode sp,
        final IServiceOptions serviceOptions,
        final ServiceParams serviceParams) {

      if (db == null) throw new IllegalArgumentException();

      if (sp == null) throw new IllegalArgumentException();

      if (serviceOptions == null) throw new IllegalArgumentException();

      if (serviceParams == null) throw new IllegalArgumentException();

      this.db = db;
      this.sp = sp;
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

      @SuppressWarnings("unchecked")
      IPredicate<ISPO> pred =
          db.getPredicate(
              sp.s() != null && sp.s().isConstant() ? (Resource) sp.s().getValue() : null,
              sp.p() != null && sp.p().isConstant() ? (URI) sp.p().getValue() : null,
              sp.o() != null && sp.o().isConstant() ? sp.o().getValue() : null,
              sp.c() != null && sp.c().isConstant() ? (Resource) sp.c().getValue() : null);

      if (pred == null) {

        return new EmptyCloseableIterator<IBindingSet>();
      }

      pred =
          (IPredicate<ISPO>)
              pred.setProperty(
                  IPredicate.Annotations.TIMESTAMP, db.getSPORelation().getTimestamp());

      final int limit = serviceParams.getAsInt(SampleParams.LIMIT, SampleParams.DEFAULT_LIMIT);

      final long seed = serviceParams.getAsLong(SampleParams.SEED, SampleParams.DEFAULT_SEED);

      final String type =
          serviceParams.getAsString(SampleParams.SAMPLE_TYPE, SampleParams.DEFAULT_SAMPLE_TYPE);

      @SuppressWarnings({"unchecked", "rawtypes"})
      final SampleIndex<?> sampleOp =
          new SampleIndex(
              new BOp[] {},
              NV.asMap(
                  new NV(SampleIndex.Annotations.PREDICATE, pred),
                  new NV(SampleIndex.Annotations.LIMIT, limit),
                  new NV(SampleIndex.Annotations.SEED, seed),
                  new NV(SampleIndex.Annotations.SAMPLE_TYPE, type)));

      final BOpContextBase context =
          new BOpContextBase(
              QueryEngineFactory.getInstance().getQueryController(db.getIndexManager()));

      final ISPO[] elements = (ISPO[]) sampleOp.eval(context);

      final IBindingSet[] bSets = new IBindingSet[elements.length];

      for (int i = 0; i < elements.length; i++) {

        bSets[i] = new ListBindingSet();

        if (sp.s() != null && sp.s().isVariable())
          bSets[i].set(
              (IVariable<IV>) sp.s().getValueExpression(), new Constant<IV>(elements[i].s()));

        if (sp.p() != null && sp.p().isVariable())
          bSets[i].set(
              (IVariable<IV>) sp.p().getValueExpression(), new Constant<IV>(elements[i].p()));

        if (sp.o() != null && sp.o().isVariable())
          bSets[i].set(
              (IVariable<IV>) sp.o().getValueExpression(), new Constant<IV>(elements[i].o()));

        if (sp.c() != null && sp.c().isVariable())
          bSets[i].set(
              (IVariable<IV>) sp.c().getValueExpression(), new Constant<IV>(elements[i].c()));
      }

      return new ThickCloseableIterator<IBindingSet>(bSets, bSets.length);
    }

    @Override
    public IServiceOptions getServiceOptions() {

      return serviceOptions;
    }
  }
}
