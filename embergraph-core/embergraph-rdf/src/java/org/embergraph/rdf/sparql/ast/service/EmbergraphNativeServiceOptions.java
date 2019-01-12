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
 * Created on Mar 3, 2012
 */

package org.embergraph.rdf.sparql.ast.service;

/*
 * Service options base class for embergraph aware services. Such services are expected to
 * interchange {@link IBindingSet}s containing {@link IV}s. The {@link IV}s are NOT guaranteed to be
 * materialized.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: EmbergraphServiceOptions.java 6077 2012-03-06 20:58:36Z thompsonbry $
 */
public class EmbergraphNativeServiceOptions extends ServiceOptionsBase
    implements INativeServiceOptions {

  /** Always returns <code>true</code>. */
  @Override
  public final boolean isEmbergraphNativeService() {
    return true;
  }

  /** Always returns <code>false</code>. */
  @Override
  public final boolean isRemoteService() {
    return false;
  }

  /** Always returns <code>false</code> (response is ignored). */
  @Override
  public final boolean isSparql10() {
    return false;
  }

  /** Always returns <code>null</code> (response is ignored). */
  @Override
  public final SPARQLVersion getSPARQLVersion() {
    return null;
  }
}
