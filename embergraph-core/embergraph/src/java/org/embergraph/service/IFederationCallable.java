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
 * Created on Apr 23, 2009
 */

package org.embergraph.service;

import java.io.Serializable;

/*
 * Interface for {@link Callable}s which require access to the {@link IEmbergraphFederation} when
 * running on an {@link IRemoteExecutor}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IFederationCallable extends Serializable {

  /*
   * Invoked before the task is executed to provide a reference to the {@link IEmbergraphFederation}
   * for the service on which the task is executing.
   *
   * @param fed The federation.
   * @throws IllegalArgumentException if the argument is <code>null</code>
   * @throws IllegalStateException if {@link #setFederation(IEmbergraphFederation)} has already been
   *     invoked and was set with a different value.
   */
  void setFederation(IEmbergraphFederation<?> fed);

  /*
   * Return the {@link IEmbergraphFederation} reference.
   *
   * @return The federation and never <code>null</code>.
   * @throws IllegalStateException if {@link #setFederation(IEmbergraphFederation)} has not been
   *     invoked.
   */
  IEmbergraphFederation<?> getFederation();
}
