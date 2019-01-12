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
 * Created on Jun 10, 2011
 */

package org.embergraph.search;

/*
 * Mutable metadata for the occurrences of a token within a field of some document.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface ITermMetadata { // extends ITermDocVal {

  //    /*
  //     * The text of the token.
  //     */
  //    String termText();

  /** The term (aka token) frequency count. */
  int termFreq();

  /** Add an occurrence. */
  void add();

  /** The local term weight, which may be computed by a variety of methods. */
  double getLocalTermWeight();

  /** The local term weight, which may be computed by a variety of methods. */
  void setLocalTermWeight(double d);
}
