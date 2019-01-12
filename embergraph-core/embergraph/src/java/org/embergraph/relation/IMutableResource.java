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
 * Created on Jul 10, 2008
 */

package org.embergraph.relation;

import org.embergraph.relation.locator.ILocatableResource;

/**
 * Mutation interface
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IMutableResource<T> extends ILocatableResource<T> {

  /**
   * Create any logically contained resources (relations, indices). There is no presumption that
   * {@link #init()} is suitable for invocation from {@link #create()}. Instead, you are responsible
   * for invoking {@link #init()} from this method IFF it is appropriate to reuse its initialization
   * logic.
   */
  void create();

  /** Destroy any logically contained resources (relations, indices). */
  void destroy();
}
