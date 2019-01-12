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
 * Created on Apr 28, 2008
 */

package org.embergraph.service;

import java.util.UUID;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.embergraph.Banner;
import org.embergraph.counters.AbstractStatisticsCollector;
import org.embergraph.counters.CounterSet;

/*
* Abstract base class defines protocols for setting the service {@link UUID}, etc.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractService implements IService {

  private static final Logger log = Logger.getLogger(AbstractService.class);

  private String serviceName;
  private UUID serviceUUID;

  /** */
  protected AbstractService() {

    // show the copyright banner during statup.
    Banner.banner();

    /*
     * Note: This is more or less the same pattern use by AbstractServer (in
     * the embergraph-jini module) to assign a default service name, but the
     * latter will report the server's class name. That class also makes
     * sure to reports the name which it assigns to the service.
     */

    serviceName = getServiceIface().getName() + "@" + getHostname() + "#" + hashCode();
  }

  /*
   * This method must be invoked to set the service {@link UUID}.
   *
   * <p>Note: This method gets invoked at different times depending on whether the service is
   * embedded (generally invoked from the service constructor) or written against a service
   * discovery framework (invoked once the service has been assigned a UUID by a registrar or during
   * re-start since the service UUID is read from a local file).
   *
   * <p>Several things depend on when this method is invoked, including the setup of the per-service
   * {@link CounterSet} reported by the service to the {@link ILoadBalancerService}.
   *
   * @param serviceUUID The {@link UUID} assigned to the service.
   * @throws IllegalArgumentException if the parameter is null.
   * @throws IllegalStateException if the service {@link UUID} has already been set to a different
   *     value.
   */
  public synchronized void setServiceUUID(final UUID serviceUUID) throws IllegalStateException {

    if (serviceUUID == null) throw new IllegalArgumentException();

    if (this.serviceUUID != null && !this.serviceUUID.equals(serviceUUID)) {

      throw new IllegalStateException();
    }

    if (log.isInfoEnabled()) log.info("uuid=" + serviceUUID);

    this.serviceUUID = serviceUUID;
  }

  public final UUID getServiceUUID() {

    return serviceUUID;
  }

  /** Return the most interesting interface for the service. */
  public abstract Class getServiceIface();

  /*
   * Starts the {@link AbstractService}.
   *
   * <p>Note: A {@link #start()} is required in order to give subclasses an opportunity to be fully
   * initialized before they are required to begin operations. It is impossible to encapsulate the
   * startup logic cleanly without this ctor() + start() pattern. Those familiar with Objective-C
   * will recognized this.
   *
   * @return <i>this</i> (the return type should be strengthened by the concrete implementation to
   *     return the actual type).
   */
  public abstract AbstractService start();

  /** Return the proxy used to access other services in the federation. */
  public abstract AbstractFederation getFederation();

  public final String getHostname() {

    return AbstractStatisticsCollector.fullyQualifiedHostName;
  }

  //    /*
//     * Shuts down the {@link #getFederation()} used by this service.
  //     */
  public synchronized void shutdown() {

    //        if(!isOpen()) return;

    //        getFederation().shutdown();

    //        open = false;

  }

  //    /*
//     * Shuts down the {@link #getFederation()} used by this service.
  //     */
  public synchronized void shutdownNow() {

    //        if(!isOpen()) return;

    //        getFederation().shutdownNow();

    //        open = false;

  }

  //    final public boolean isOpen() {
  //
  //        return open;
  //
  //    }
  //    private boolean open = true;

  public void destroy() {

    shutdownNow();
  }

  /*
   * Note: This is overridden in the jini integration to return a configured name for the service.
   */
  public String getServiceName() {

    return serviceName;
  }

  /*
   * Sets up the {@link MDC} logging context. You should do this on every client facing point of
   * entry and then call {@link #clearLoggingContext()} in a <code>finally</code> clause. You can
   * extend this method to add additional context.
   *
   * <p>This implementation adds the following parameters to the {@link MDC}.
   *
   * <dl>
   *   <dt>serviceName
   *   <dd>The serviceName is typically a configuration property for the service. This datum can be
   *       injected into log messages using <em>%X{serviceName}</em> in your log4j pattern layout.
   *   <dt>serviceUUID
   *   <dd>The serviceUUID is, in general, assigned asynchronously by the service registrar. Once
   *       the serviceUUID becomes available it will be added to the {@link MDC}. This datum can be
   *       injected into log messages using <em>%X{serviceUUID}</em> in your log4j pattern layout.
   *   <dt>hostname
   *   <dd>The hostname statically determined. This datum can be injected into log messages using
   *       <em>%X{hostname}</em> in your log4j pattern layout.
   * </dl>
   */
  protected void setupLoggingContext() {

    try {

      // Note: This _is_ a local method call.

      UUID serviceUUID = getServiceUUID();

      // Will be null until assigned by the service registrar.

      if (serviceUUID != null) {

        MDC.put("serviceUUID", serviceUUID);
      }

      MDC.put("serviceName", getServiceName());

      MDC.put("hostname", getHostname());

    } catch (Throwable t) {

      /*
       * Ignore.
       */

    }
  }

  /** Clear the logging context. */
  protected void clearLoggingContext() {

    MDC.remove("serviceName");

    MDC.remove("serviceUUID");

    MDC.remove("hostname");
  }
}
