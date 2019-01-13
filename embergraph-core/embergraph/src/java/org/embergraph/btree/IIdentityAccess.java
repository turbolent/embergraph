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
 * Created on Nov 17, 2006
 */
package org.embergraph.btree;

/*
 * An interface that declares how we access the persistent identity of an object.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IIdentityAccess {

  /** Null reference for the store (zero). */
  long NULL = 0L;

  /*
   * The persistent identity.
   *
   * @exception IllegalStateException if the object is not persistent.
   */
  long getIdentity() throws IllegalStateException;

  /** True iff the object is persistent. */
  boolean isPersistent();

  /** True iff an object has been logically deleted. */
  boolean isDeleted();

  /*
   * Deletes the persistence capable object. Both transient and persistent objects may be logically
   * deleted. If the object is persistent then its space on the store is deallocated.
   *
   * @throws IllegalStateException if the object is already deleted.
   */
  void delete() throws IllegalStateException;
}