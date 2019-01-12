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
package org.embergraph.btree.proc;

import java.util.Comparator;
import org.embergraph.service.Split;

/**
 * A key/value pair where the key is a Split.
 *
 * @param <K>
 * @param <V>
 * @author bryan
 */
public class SplitValuePair<K extends Split, V> {

  public final K key;
  public final V val;

  public SplitValuePair(final K key, final V val) {
    this.key = key;
    this.val = val;
  }

  /**
   * Sorts {@link SplitValuePair}s.
   *
   * @author bryan
   */
  public static class PairComparator<K extends Split, V>
      implements Comparator<SplitValuePair<K, V>> {

    @Override
    public int compare(final SplitValuePair<K, V> o1, final SplitValuePair<K, V> o2) {
      if (o1.key.fromIndex < o2.key.fromIndex) return -1;
      if (o1.key.fromIndex > o2.key.fromIndex) return 1;
      return 0;
    }
  }
}
