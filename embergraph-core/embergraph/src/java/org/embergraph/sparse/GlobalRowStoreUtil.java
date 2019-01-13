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
package org.embergraph.sparse;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/*
 * Utility method for use with a {@link SparseRowStore}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class GlobalRowStoreUtil {

  /** Convert the Properties to a Map. */
  public static Map<String, Object> convert(final Properties properties) {

    final Map<String, Object> map = new HashMap<>();

    final Enumeration<?> e = properties.propertyNames();

    while (e.hasMoreElements()) {

      final Object key = e.nextElement();

      //            if (!(key instanceof String)) {
      //
      //                log.warn("Will not store non-String key: " + key);
      //
      //                continue;
      //
      //            }

      final String name = (String) key;

      map.put(name, properties.getProperty(name));
    }

    return map;
  }
}
