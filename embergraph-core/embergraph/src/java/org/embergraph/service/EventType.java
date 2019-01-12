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
 * Created on Feb 3, 2009
 */

package org.embergraph.service;

import org.embergraph.btree.IndexSegment;
import org.embergraph.journal.Journal;
import org.embergraph.resources.OverflowManager;

/*
* Type safe enum for {@link Event}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public enum EventType {

  /*
   * Synchronous overflow is a fast operation that occurs when the live journal is nearly at its
   * maximum capacity. The index partition views are redefined onto a new journal during synchronous
   * overflow, but little or no data is copied off of the old journal.
   */
  SynchronousOverflow,

  /*
   * Asynchronous overflow migrates data off of the old journal and onto read-optimized index
   * segments and is responsible for running operation which split, join, or move index partitions.
   */
  AsynchronousOverflow,

  /*
   * Other kinds of events.
   */

  /*
   * Operation that builds an index segment. Builds may either be simple (from the {@link BTree} on
   * a {@link Journal} used to absorb writes for an index partition) or compacting merges (from the
   * full view of the index partition).
   */
  IndexSegmentBuild,

  /** An {@link IndexSegmentStore} open-close event (start is open, end is close). */
  IndexSegmentStoreOpenClose,

  /** An {@link IndexSegment} open-close event (start is open, end is close). */
  IndexSegmentOpenClose,

  /*
   * Purge resources.
   *
   * @see OverflowManager#purgeOldResources(long, boolean)
   */
  PurgeResources
}
