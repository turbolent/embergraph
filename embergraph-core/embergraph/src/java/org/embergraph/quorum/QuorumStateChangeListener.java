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
 * Created on Jun 14, 2010
 */

package org.embergraph.quorum;

import java.util.UUID;

/**
 * A non-remote interface containing <em>only</em> and <em>all</em> distributed quorum state change
 * messages for this {@link QuorumMember}. These messages are generated by the {@link QuorumWatcher}
 * for the {@link QuorumMember} when it notices the corresponding change in the distributed quorum
 * state. Thus, all methods on this interface indicate <em>observed</em> state changes. The {@link
 * QuorumActor} is responsible for causing changes in the distributed quorum state, but it DOES NOT
 * generate these messages - that task falls to the {@link QuorumWatcher}.
 *
 * <p>Quorum members have strict preconditions on their actions, which are documented by the {@link
 * QuorumActor}. In addition, {@link QuorumWatcher} maintains various postconditions (for example, a
 * member leave implies a service leave). Together, these preconditions and postconditions imply an
 * ordering over the {@link QuorumStateChangeListener}s.
 *
 * <p>However, because this interface reports <em>observed</em> state changes, it is possible that
 * some events may occur "out of order". For example, if the quorum is maintained within zookeeper
 * and a client looses its zookeeper connection, then zookeeper will the <i>ephemeral</i> znodes for
 * that client. The <i>order</i> in which other clients observe those state changes in essentially
 * arbitrary.
 *
 * <p>Implementations of this interface must not block in the event thread.
 *
 * @see QuorumMember
 * @see QuorumActor
 * @see QuorumWatcher
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: QuorumStateChangeListener.java 4069 2011-01-09 20:58:02Z thompsonbry $
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/695">HAJournalServer reports
 *     "follower" but is in SeekConsensus and is not participating in commits (javadoc
 *     clarifications)</a>
 */
public interface QuorumStateChangeListener {

  /** Invoked when this service is added as a quorum member. */
  void memberAdd();

  /** Invoked when this service is removed as a quorum member. */
  void memberRemove();

  /**
   * Invoked when this service is added to the write pipeline. The service always enters at the of
   * the pipeline.
   */
  void pipelineAdd();

  /** Invoked when this service is removed from the write pipeline. */
  void pipelineRemove();

  /**
   * Invoked for this service when the service is already in the pipeline and this service becomes
   * the first service in the write pipeline because all previous services in the pipeline order
   * have been removed from the pipeline (failover into the leader position).
   */
  void pipelineElectedLeader();

  /**
   * Invoked for this service when the downstream service in the write pipeline has changed.
   * Services always enter at the end of the write pipeline, but may be removed at any position in
   * the write pipeline.
   *
   * @param oldDownstreamId The {@link UUID} of the service which <em>was</em> downstream from this
   *     service in the write pipeline and <code>null</code> iff this service was the last service
   *     in the pipeline.
   * @param newDownstreamId The {@link UUID} of the service which <em>is</em> downstream from this
   *     service in the write pipeline and <code>null</code> iff this service <em>is</em> the last
   *     service in the pipeline.
   */
  void pipelineChange(final UUID oldDownStreamId, final UUID newDownStreamId);

  /**
   * Invoked for this service when the upstream service in the write pipeline has been removed. This
   * hook provides an opportunity for this service to close out its connection with the old upstream
   * service and to prepare to establish a new connection with the new downstream service.
   */
  void pipelineUpstreamChange();

  //    void castVote(long lastCommitTime);
  //    void withdrawVote(long lastCommitTime);
  //    void setLastValidToken();

  //    /**
  //     * Invoked when <em>this</em> quorum member is elected as the quorum leader.
  //     */
  //    void electedLeader();
  //
  //    /**
  //     * Invoked when <em>this</em> quorum member is elected as a quorum follower.
  //     * This event occurs both when the quorum meets and when a quorum member is
  //     * becomes synchronized with and then joins an already met quorum.
  //     */
  //    void electedFollower();

  /**
   * Invoked when a consensus has been achieved among <code>(k+1)/2</code> services concerning a
   * shared lastCommitTime (really, this is not a consensus but a simple majority). This message is
   * sent to each member service regardless of whether or not they participated in the consensus.
   *
   * <p>Once a consensus has been reached, each {@link QuorumMember} which agrees on that
   * <i>lastCommitTime</i> MUST do a {@link #serviceJoin()} before the quorum will meet. The first
   * quorum member to do a service join will be elected the leader. The remaining services to do a
   * service join will be elected followers.
   *
   * @param lastCommitTime The last commit time around which a consensus was established.
   * @see #serviceJoin()
   * @see #electedLeader(long)
   * @see #electedFollower(long)
   * @see #lostConsensus()
   */
  void consensus(final long lastCommitTime);

  /**
   * Invoked when the consensus is lost. Services do not withdraw their cast votes until a quorum
   * breaks and a new consensus needs to be established. This message is sent to each member service
   * regardless of whether or not they participated in the consensus.
   *
   * @see #consensus(long)
   */
  void lostConsensus();

  /** Invoked when this service joins the quorum. */
  void serviceJoin();

  /** Invoked when this service leaves the quorum. */
  void serviceLeave();

  //    /**
  //     * Invoked for all quorum members when the leader leaves the quorum. A
  //     * leader leave is also a quorum break, so services can generally just
  //     * monitor {@link #quorumBreak()} instead of this method. Also, services
  //     * will generally notice a quorum break because {@link Quorum#token()} will
  //     * have been cleared and will in any case not be the same token under which
  //     * the service was operating.
  //     */
  //    void leaderLeft();

  /**
   * Invoked when a quorum meets. The state of the met quorum can be queried using the <i>token</i>.
   * Quorum members can use this to decide whether they are the leader (using {@link
   * #isLeader(long)}, joined as a follower (using {@link #isFollower(long)}), or do not participate
   * in the quorum (this message is sent to all quorum members, so this service might not be part of
   * the met qourum).
   *
   * <p>The following pre-conditions will be satisfied before this message is sent to the {@link
   * QuorumMember}:
   *
   * <ul>
   *   <li>There will be at least <code>(k+1)/2</code> services which have voted for the same
   *       <i>lastCommitTime</i>.
   *   <li>There will be at <code>(k+1)/2</code> services joined with the quorum. The {@link
   *       Quorum#getJoined() join order} will be the same as the {@link Quorum#getVotes()} for the
   *       services which voted for the <i>lastCommitTime around which a consensus was formed.
   *   <li>If this quorum member is joined with the quorum it will have observed its own {@link
   *       #memberAdd()}, {@link #pipelineAdd()}, {@link #consensus(long)}, and {@link
   *       #serviceJoin()} events.
   *   <li>The joined services will be arranged in a write pipeline, with the leader at the head of
   *       that pipeline.
   * </ul>
   *
   * <p>When control returns from this method, the following post-conditions should be true:
   *
   * <ul>
   *   <li>The service should be prepared to accept reads.
   *   <li>If the service was elected as the quorum leader, then it should be prepared to accept
   *       writes.
   * </ul>
   *
   * If the {@link QuorumMember} is joined with the quorum but it can not satisfy these
   * post-conditions, then it must {@link QuorumActor#serviceLeave() leave} the {@link Quorum}.
   *
   * @param token The newly assigned quorum token.
   * @param leaderId The {@link UUID} of the service which was elected to be the quorum leader. This
   *     information is only valid for the scope of the accompanying quorum token. (The leaderId may
   *     be obtained from {@link #getLeader(long)} at any time for a met quorum.)
   */
  void quorumMeet(long token, UUID leaderId);

  /**
   * Invoked when a quorum breaks. The service MUST handle handle this event by (a) doing an abort()
   * which will any buffered writes and reload their current root block; and (b) casting a vote for
   * their current commit time. Once a consensus is reached on the current commit time, services
   * will be joined in the vote order, a new leader will be elected, and the quorum will meet again.
   * This message is sent to all member services, regardless of whether they were joined with the
   * met quorum.
   */
  void quorumBreak();
}
