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
 * Created on Oct 10, 2007
 */
package org.embergraph.journal;

/**
 * Exception thrown when there is a commit failure, especially for the group commit mechanisms.
 *
 * @author bryan
 */
public class CommitException extends RuntimeException {

  /** */
  private static final long serialVersionUID = 1L;

  public CommitException() {}

  public CommitException(String message) {
    super(message);
  }

  public CommitException(Throwable cause) {
    super(cause);
  }

  public CommitException(String message, Throwable cause) {
    super(message, cause);
  }

  public CommitException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
