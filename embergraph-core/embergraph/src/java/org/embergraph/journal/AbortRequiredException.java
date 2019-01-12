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
package org.embergraph.journal;

/*
 * An instance of this exception is thrown when the storage layer must go through an abort before
 * new writes may be applied or committed.
 *
 * @author bryan
 * @see http://jira.blazegraph.com/browse/BLZG-181 (Add critical section protection to
 *     AbstractJournal.abort() and EmbergraphSailConnection.rollback())
 * @see http://jira.blazegraph.com/browse/BLZG-1236 (Recycler error in 1.5.1)
 */
public class AbortRequiredException extends IllegalStateException {

  /** */
  private static final long serialVersionUID = 1L;

  public AbortRequiredException() {}

  public AbortRequiredException(final String s) {
    super(s);
  }

  public AbortRequiredException(final Throwable cause) {
    super(cause);
  }

  public AbortRequiredException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
