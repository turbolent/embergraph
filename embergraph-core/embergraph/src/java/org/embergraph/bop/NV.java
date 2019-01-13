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
 * Created on Aug 17, 2010
 */

package org.embergraph.bop;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/*
A name-value pair.
*
* @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
* @version $Id$
*/
public class NV implements Serializable, Comparable<NV> {

  /** */
  private static final long serialVersionUID = -6919300153058191480L;

  private final String name;

  private final Object value;

  public String getName() {

    return name;
  }

  public Object getValue() {

    return value;
  }

  public String toString() {

    return name + "=" + value;
  }

  /** @throws IllegalArgumentException if the <i>name</i> is <code>null</code>. */
  public NV(final String name, final Object value) {

    if (name == null) throw new IllegalArgumentException();

    //        if (value == null)
    //            throw new IllegalArgumentException();

    this.name = name;

    this.value = value;
  }

  public int hashCode() {

    return name.hashCode();
  }

  public boolean equals(final Object o) {

    if (this == o) return true;

    if (!(o instanceof NV)) return false;

    return name.equals(((NV) o).name) && value.equals(((NV) o).value);
  }

  /** Places into order by <code>name</code>. */
  public int compareTo(final NV o) {

    return name.compareTo(o.name);
  }

  /*
   * Combines the two arrays, appending the contents of the 2nd array to the contents of the first
   * array.
   *
   * @param a
   * @param b
   * @return
   */
  public static NV[] concat(final NV[] a, final NV[] b) {

    if (a == null && b == null) return a;

    if (a == null) return b;

    if (b == null) return a;

    final NV[] c = new NV[a.length + b.length];
    //                (NV[]) java.lang.reflect.Array.newInstance(a.getClass()
    //                .getComponentType(), a.length + b.length);

    System.arraycopy(a, 0, c, 0, a.length);

    System.arraycopy(b, 0, c, a.length, b.length);

    return c;
  }

  /*
   * Wrap a single name and value as a map.
   *
   * @param name The key.
   * @param val The value.
   * @return The map.
   */
  public static Map<String, Object> asMap(final String name, final Object val) {

    final Map<String, Object> tmp = new LinkedHashMap<>(1);

    tmp.put(name, val);

    return tmp;
  }

  /*
   * Wrap name/value pairs as a map.
   *
   * @param nameValuePairs Pairs each being a string followed by an object, being the name value
   *     pair in the resulting map.
   * @return The map.
   */
  public static Map<String, Object> asMap(final Object... nameValuePairs) {

    if (nameValuePairs.length % 2 != 0) throw new IllegalArgumentException();

    final Map<String, Object> rslt = new LinkedHashMap<>(nameValuePairs.length / 2);

    for (int i = 0; i < nameValuePairs.length; i += 2) {

      rslt.put((String) nameValuePairs[i], nameValuePairs[i + 1]);
    }

    return rslt;
  }

  /*
   * Wrap an array name/value pairs as a {@link Map}.
   *
   * @param a The array.
   * @return The map.
   */
  public static Map<String, Object> asMap(final NV... a) {

    /*
     * Note: Not possible for modifiable BOps (AST).
     */
    //        if (a.length == 1) {
    //
    //            return Collections.singletonMap(a[0].name, a[0].value);
    //
    //        }

    final Map<String, Object> tmp = new LinkedHashMap<>(a.length);

    for (int i = 0; i < a.length; i++) {

      tmp.put(a[i].name, a[i].value);
    }

    return tmp;
  }
}
