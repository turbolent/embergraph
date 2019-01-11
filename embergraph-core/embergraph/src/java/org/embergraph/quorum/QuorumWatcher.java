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
 * Created on Jun 6, 2010
 */
package org.embergraph.quorum;

import java.rmi.Remote;

/**
 * An interface that watches for changes in the distributed quorum state. An
 * implementation of this interface is generally an inner class of the concrete
 * {@link AbstractQuorum} and is responsible for updating the internal state of
 * the {@link AbstractQuorum} as it the distributed quorum state. The quorum
 * internal state always <em>reflects</em> the distributed quorum state.
 * <p>
 * For example, the zookeeper implementation will watch the zpath whose children
 * are the member services. If a new child appears, it will invoke
 * {@link AbstractQuorum#memberAdd(java.util.UUID)} so the
 * {@link AbstractQuorum} can update its internal state.
 * <p>
 * Since this interface <i>watches</i> the distributed state of the quorum, it
 * does not have any specific methods which it must declare other than those
 * which manage its life cycle (start/terminate). All of the interesting methods
 * are on the {@link AbstractQuorum} class.
 * <p>
 * The {@link QuorumWatcher} is responsible for generating these events during
 * its discovery phase when it starts running. The {@link QuorumWatcher} MUST
 * report the discovered state in a manner consistent with the preconditions and
 * postconditions defined for the {@link QuorumActor} the {@link QuorumWatcher}.
 * <p>
 * The {@link QuorumActor} provides the complementary functionality of
 * <i>causing</i> changes in the distributed state of the quorum.
 * 
 * @param <S>
 * @param <C>
 * 
 * @see QuorumActor
 * 
 * @author thompsonbry@users.sourceforge.net
 */
public interface QuorumWatcher<S extends Remote, C extends QuorumClient<S>> {

//    /**
//     * Start asynchronous processing.
//     */
//    public void start();
//    
//    /**
//     * Terminate asynchronous processing.
//     */
//    public void terminate();
    
}
