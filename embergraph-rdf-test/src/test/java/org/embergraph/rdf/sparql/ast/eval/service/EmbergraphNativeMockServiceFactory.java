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
 * Created on Mar 7, 2012
 */

package org.embergraph.rdf.sparql.ast.eval.service;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.List;
import org.embergraph.bop.IBindingSet;
import org.embergraph.rdf.sparql.ast.eval.AbstractServiceFactoryBase;
import org.embergraph.rdf.sparql.ast.service.EmbergraphServiceCall;
import org.embergraph.rdf.sparql.ast.service.IServiceOptions;
import org.embergraph.rdf.sparql.ast.service.OpenrdfNativeServiceOptions;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.striterator.CloseableIteratorWrapper;

/*
 * Mock service reports the solutions provided in the constructor.
 *
 * <p>Note: This can not be used to test complex queries because the caller needs to know the order
 * in which the query will be evaluated in order to know the correct response for the mock service.
 */
public class EmbergraphNativeMockServiceFactory extends AbstractServiceFactoryBase {

  private final OpenrdfNativeServiceOptions serviceOptions = new OpenrdfNativeServiceOptions();

  private final List<IBindingSet> serviceSolutions;

  public EmbergraphNativeMockServiceFactory(final List<IBindingSet> serviceSolutions) {

    this.serviceSolutions = serviceSolutions;
  }

  @Override
  public EmbergraphServiceCall create(final ServiceCallCreateParams params) {

    TestEmbergraphNativeServiceEvaluation.assertNotNull(params);

    TestEmbergraphNativeServiceEvaluation.assertNotNull(params.getTripleStore());

    TestEmbergraphNativeServiceEvaluation.assertNotNull(params.getServiceNode());

    return new MockEmbergraphServiceCall();
  }

  @Override
  public IServiceOptions getServiceOptions() {
    return serviceOptions;
  }

  private class MockEmbergraphServiceCall implements EmbergraphServiceCall {

    @Override
    public ICloseableIterator<IBindingSet> call(final IBindingSet[] bindingSets) {

      TestEmbergraphNativeServiceEvaluation.assertNotNull(bindingSets);

      // System.err.println("ServiceCall: in="+Arrays.toString(bindingSets));

      // System.err.println("ServiceCall: out="+serviceSolutions);

      return new CloseableIteratorWrapper<>(serviceSolutions.iterator());
    }

    @Override
    public IServiceOptions getServiceOptions() {
      return serviceOptions;
    }
  }
}
