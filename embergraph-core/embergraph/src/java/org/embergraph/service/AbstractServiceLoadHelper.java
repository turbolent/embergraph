package org.embergraph.service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/*
 * Base class for abstract implementations with integration points for the {@link
 * LoadBalancerService}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractServiceLoadHelper implements IServiceLoadHelper {

  protected final long joinTimeout;

  /** @param joinTimeout */
  protected AbstractServiceLoadHelper(final long joinTimeout) {

    if (joinTimeout <= 0L) throw new IllegalArgumentException();

    this.joinTimeout = joinTimeout;
  }

  /*
   * Return <code>true</code> iff the service is under-utilized.
   *
   * @param score The score for the service.
   * @param scores The set of scores for the known services.
   */
  protected abstract boolean isUnderUtilizedDataService(
      final ServiceScore score, final ServiceScore[] scores);

  /*
   * Return <code>true</code> iff the given serviceUUID identifies an active {@link IDataService}.
   *
   * @param serviceUUID The service UUID.
   */
  protected abstract boolean isActiveDataService(UUID serviceUUID);

  /*
   * Await the join of an {@link IDataService}.
   *
   * @param timeout The timeout.
   * @param unit The unit for the timeout.
   */
  protected abstract void awaitJoin(long timeout, TimeUnit unit) throws InterruptedException;

  /*
   * Return an array of service UUIDs for all of the active {@link IDataService}s that we know about
   * right now (snapshot).
   *
   * @return The array and never <code>null</code>.
   */
  protected abstract UUID[] getActiveServices();
}
