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
 * Created on Sep 17, 2008
 */

package org.embergraph.service;

import java.io.IOException;
import java.util.UUID;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.counters.httpd.CounterSetHTTPD;
import org.embergraph.util.httpd.AbstractHTTPD;

/**
 * Interface allowing services to take over handling of events normally handled by the {@link
 * AbstractFederation}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T> The generic type of the client or service.
 */
public interface IFederationDelegate<T> {

  /** Return the client or service. */
  public T getService();

  /**
   * Return a name for the service. It is up to administrators to ensure that service names are
   * unique.
   *
   * @return A name for the service.
   */
  public String getServiceName();

  /**
   * Return the class or interface that is the most interesting facet of the client and which will
   * be used to identify this client in the performance counters reported to the {@link
   * ILoadBalancerService}.
   *
   * @return The class or interface and never <code>null</code>.
   */
  public Class getServiceIface();

  /**
   * The {@link UUID} assigned to the {@link IEmbergraphClient} or {@link AbstractService}.
   *
   * @see AbstractService#setServiceUUID(UUID)
   */
  public UUID getServiceUUID();

  /**
   * Offers the service an opportunity to dynamically detach and re-attach performance counters.
   * This can be invoked either in response to an http GET or the periodic reporting of performance
   * counters to the {@link ILoadBalancerService}. In general, implementations should limit the
   * frequency of update, e.g., to no more than once a second.
   *
   * <p>Note: For most purposes, this has been replaced by {@link ICounterSetAccess} which is now
   * passed into {@link CounterSetHTTPD}. That provides the necessary indirection for periodic
   * refresh of the performance counters. The {@link CounterSetHTTPD} now also handles the
   * limitation on the update frequency for the materialized counters.
   *
   * <p>However, there are still some counters which need to be dynamically reattached. For example,
   * any counter set which is dynamic in its structure, such as the DirectBufferPool.
   */
  public void reattachDynamicCounters();

  /** Return <code>true</code> iff the service is ready to start. */
  public boolean isServiceReady();

  /**
   * Invoked by the {@link AbstractFederation} once the deferred startup tasks are executed.
   * Services may use this event to perform additional initialization.
   */
  public void didStart();

  /**
   * Notice that the service has been discovered. This notice will be generated the first time the
   * service is discovered by a given {@link IEmbergraphClient}.
   *
   * @param service The service.
   * @param serviceUUID The service {@link UUID}.
   */
  public void serviceJoin(IService service, UUID serviceUUID);

  /**
   * Notice that the service is no longer available. This notice will be generated once for a given
   * {@link IEmbergraphClient} when the service is no longer available from any of its service
   * registrars.
   *
   * @param serviceUUID The service {@link UUID}.
   */
  public void serviceLeave(UUID serviceUUID);

  /**
   * Create a new {@link AbstractHTTPD} instance.
   *
   * @param port The port, or zero for a random port.
   * @param access Used to materialize the {@link CounterSet} that will be served up.
   * @return The httpd daemon.
   * @throws IOException
   */
  public AbstractHTTPD newHttpd(final int httpdPort, final ICounterSetAccess access)
      throws IOException;
}
