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
 * Created on Apr 16, 2009
 */

package org.embergraph.service.ndx.pipeline;

import org.embergraph.btree.keys.KVO;
import org.embergraph.relation.accesspath.BlockingBuffer;

/*
 * Class exists solely to make it easier to write the unit tests by aligning the various generic
 * types across the master, the subtask, and their statistics objects.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class MockMaster<
        H extends MockMasterStats<L, HS>,
        O extends Object,
        E extends KVO<O>,
        S extends MockSubtask,
        L extends Object,
        HS extends MockSubtaskStats>
    extends AbstractMasterTask<H, E, S, L> {

  public MockMaster(
      final H stats,
      final BlockingBuffer<E[]> buffer,
      final long sinkIdleTimeout,
      final long sinkPollTimeout) {

    super(stats, buffer, sinkIdleTimeout, sinkPollTimeout);
  }
}
