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
 * Created on Apr 19, 2006
 */
package org.embergraph.cache;

/*
 * Interface receives notice of cache eviction events.
 *
 * @version $Id$
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see ICachePolicy
 */
public interface ICacheListener<K, T> {

  /*
   * The object was evicted from the cache.
   *
   * @param entry The cache entry for the object that is being evicted. The entry is no longer valid
   *     once this method returns and MAY be reused by the {@link ICachePolicy} implementation.
   */
  void objectEvicted(ICacheEntry<K, T> entry);
}
