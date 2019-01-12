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
 * Created on Nov 9, 2005
 */
package org.embergraph.btree.keys;

/*
 * An instance of this class is thrown where there is no successor to a value within its value
 * space.
 */
public class NoSuccessorException extends RuntimeException {

  /** */
  private static final long serialVersionUID = -2290043197024119560L;

  public NoSuccessorException() {}

  public NoSuccessorException(String msg) {

    super(msg);
  }
}
