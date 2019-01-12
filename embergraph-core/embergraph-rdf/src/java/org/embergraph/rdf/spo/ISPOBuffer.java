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
 * Created on Oct 30, 2007
 */

package org.embergraph.rdf.spo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/*
 * A buffer for {@link SPO}s. {@link ISPOBuffer}s are used to collect {@link SPO}s into chunks that
 * can be sorted in order to support efficient batch operations on statement indices.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @deprecated by {@link IBuffer}
 */
public interface ISPOBuffer {

  Logger log = Logger.getLogger(ISPOBuffer.class);

  /** True iff the {@link #log} level is INFO or less. */
  boolean INFO = log.getEffectiveLevel().toInt() <= Level.INFO.toInt();

  /** True iff the {@link #log} level is DEBUG or less. */
  boolean DEBUG = log.getEffectiveLevel().toInt() <= Level.DEBUG.toInt();

  /** The #of statements currently in the buffer. */
  int size();

  /** True iff there are no statements in the buffer. */
  boolean isEmpty();

  /*
   * Adds an {@link ISPO}.
   *
   * @param spo The {@link ISPO}.
   * @return true if the buffer will store the statement (i.e., the statement is not excluded by the
   *     filter).
   */
  boolean add(ISPO spo);

  /*
   * Flush any buffered statements to the backing store.
   *
   * @return The cumulative #of statements that were written on the indices since the last time the
   *     counter was reset. A statement that was previously an axiom or inferred and that is
   *     converted to an explicit statement by this method will be reported in this count as well as
   *     any statement that was not pre-existing in the database. Statement removal also counts as a
   *     "write".
   * @see #flush(boolean)
   */
  int flush();
}
