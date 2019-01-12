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
 * Created on Jan 31, 2008
 */

package org.embergraph.btree;

/*
* An interface defining non-batch methods for inserting, removing, lookup, and containment tests
 * where keys and values are implicitly converted to and from <code>byte[]</code>s using the {@link
 * ITupleSerializer} configured on the {@link IndexMetadata} object for the {@link IIndex}.
 *
 * @todo Add generic parameters for the application key type and the application value type.
 * @todo Add {@link IRangeQuery} variants with automatic conversion of application keys to unsigned
 *     byte[] keys.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IAutoboxBTree {

  /*
   * Insert with auto-magic handling of keys and value objects.
   *
   * @param key The key is implicitly converted to an <strong>unsigned</strong> <code>byte[]</code>.
   * @param value The value is implicitly converted to a <code>byte[]</code>.
   * @return The de-serialized old value -or- <code>null</code> if there was no value stored under
   *     that key.
   */
  Object insert(Object key, Object value);

  /*
   * Lookup a value for a key.
   *
   * @param key The key is implicitly converted to an <strong>unsigned</strong> <code>byte[]</code>.
   * @return The de-serialized value or <code>null</code> if there is no entry for that key.
   */
  Object lookup(Object key);

  /*
   * Return true iff there is an entry for the key.
   *
   * @param key The key is implicitly converted to an <strong>unsigned</strong> <code>byte[]</code>.
   * @return True if the btree contains an entry for that key.
   */
  boolean contains(Object key);

  /*
   * Remove the key and its associated value.
   *
   * @param key The key is implicitly converted to an <strong>unsigned</strong> <code>byte[]</code>.
   * @return The de-serialized value stored under that key or <code>null</code> if the key was not
   *     found.
   */
  Object remove(Object key);
}
