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
 * Created on Sep 2, 2014
 */
package org.embergraph.btree;

/*
 * Error marks an mutable index as in an inconsistent state arising from an exception during
 * eviction of a dirty node or leaf from a mutable index. The index MUST be reloaded from the
 * current checkpoint record.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see <a href="http://trac.blazegraph.com/ticket/1005">Invalidate BTree objects if error occurs
 *     during eviction </a>
 */
public class EvictionError extends IndexInconsistentError {

  /** */
  private static final long serialVersionUID = 1L;

  public EvictionError() {}

  public EvictionError(String message) {
    super(message);
  }

  public EvictionError(Throwable cause) {
    super(cause);
  }

  public EvictionError(String message, Throwable cause) {
    super(message, cause);
  }

  public EvictionError(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
