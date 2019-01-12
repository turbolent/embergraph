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
 * Created on Oct 22, 2010
 */

package org.embergraph.bop.engine;

import org.embergraph.bop.IBindingSet;

/**
 * Interface dispatches an {@link IBindingSet}[] chunk generated by a running operator task. Each
 * task may produce zero or more such chunks. The chunks may be combined together by the caller in
 * order to have "chunkier" processing by this interface. The interface is responsible for
 * generating the appropriate {@link IChunkMessage}(s) for each {@link IBindingSet}[] chunk. In
 * standalone there is a one-to-one relationship between input chunks and output messages. In
 * scale-out, we map each {@link IBindingSet} over the shard(s) for the next operator, which is a
 * many-to-one mapping.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IChunkHandler {

  /**
   * Take an {@link IBindingSet}[] chunk generated by some pass over an operator and make it
   * available to the target operator. How this is done depends on whether the query is running
   * against a standalone database or the scale-out database.
   *
   * <p>Note: The return value is used as part of the termination criteria for the query which
   * depends on (a) the #of running operator tasks and (b) the #of {@link IChunkMessage}s generated
   * (available) and consumed. The return value of this method increases the #of {@link
   * IChunkMessage}s available to the query.
   *
   * @param query The query.
   * @param bopId The operator which wrote on the sink.
   * @param sinkId The identifier of the target operator.
   * @param chunk The intermediate results to be passed to that target operator.
   * @return The #of {@link IChunkMessage} sent. This will always be ONE (1) for scale-up. For
   *     scale-out, there will be at least one {@link IChunkMessage} per index partition over which
   *     the intermediate results were mapped.
   */
  int handleChunk(IRunningQuery query, int bopId, int sinkId, IBindingSet[] chunk);
}
