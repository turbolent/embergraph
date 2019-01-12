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
 * Created on Dec 13, 2005
 */
package org.embergraph.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/*
 * Implementation based on {@link WeakReference}.
 *
 * @version $Id$
 * @author thompsonbry
 */
public class WeakCacheEntry<K, T> extends WeakReference<T> implements IWeakRefCacheEntry<K, T> {

  private final K oid;

  public WeakCacheEntry(K key, T obj, ReferenceQueue<T> queue) {

    super(obj, queue);

    this.oid = key;
  }

  public K getKey() {

    return oid;
  }

  public T getObject() {

    return get();
  }

  public String toString() {
    return "Entry(oid=" + oid + ")";
  }
}
