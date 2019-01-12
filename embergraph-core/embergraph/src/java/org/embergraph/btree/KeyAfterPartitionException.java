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
 * Created on Dec 6, 2008
 */

package org.embergraph.btree;

import org.embergraph.mdi.ISeparatorKeys;
import org.embergraph.util.BytesUtil;

/*
 * An exception thrown when a key lies after the half-open range of an index partition.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class KeyAfterPartitionException extends KeyOutOfRangeException {

  /** */
  private static final long serialVersionUID = 150631245388779308L;

  /*
   * @param key
   * @param allowUpperBound
   * @param pmd
   */
  public KeyAfterPartitionException(
      final byte[] key, final boolean allowUpperBound, final ISeparatorKeys pmd) {

    super(
        "key=" + BytesUtil.toString(key) + ", allowUpperBound=" + allowUpperBound + ", pmd=" + pmd);
  }
}
