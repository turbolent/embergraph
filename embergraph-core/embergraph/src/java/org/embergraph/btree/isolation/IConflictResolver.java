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
 * Created on Oct 23, 2006
 */

package org.embergraph.btree.isolation;

import java.io.Serializable;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.journal.Journal;
import org.embergraph.journal.Tx;

/*
* An interface invoked during backward validation when a write-write conflict has been detected.
 * The implementation must either resolve the write-write conflict by returning a new version in
 * which the conflict is resolved or report an unresolvable conflict, in which case backward
 * validation will force the transaction to abort.
 *
 * @see http://www.cs.brown.edu/~mph/Herlihy90a/p96-herlihy.pdf for an excellent discussion of data
 *     type specific state-based conflict resolution, including examples for things such as bank
 *     accounts.
 * @todo Write tests in which we do state-based conflict resolution for both the bank account
 *     examples in Herlihy and the examples that we will find in race conditions for the lexical
 *     terms and statements in an RDF model.
 * @todo How to handle cascading dependencies. For example, if there is a write-write conflict on
 *     the terms index of an RDF store then the reverse index and any statements written in the
 *     transaction for the term with which the write-write conflict exists must also be updated.
 *     This will require lots of application knowledge and access to the {@link Tx} object itself?
 * @todo The burden of deserialization is on the application and its implementation of this
 *     interface at present. Will object deserialization be buffered by a cache? Cache integrity is
 *     tricky here since the objects being reconciled belong to different transactional states. I
 *     can see a situation in which conflict resolution requires multiple changes in a graph
 *     structure, and could require simultaneous traversal of both a read-only view of the last
 *     committed state on the journal and a read-write view of the transaction. Resolution would
 *     then proceed by WRITE or DELETE operations on the transaction. The method signature would be
 *     changed to:
 *     <p>This would have to allow READ,WRITE,and DELETE operations during PREPARE.<br>
 *     Handling distributed conflicts could also require awareness of the int64 identifier. That is
 *     accessible to the caller if they have saved a reference to the {@link Journal} in the
 *     constructor. Such resolution would have to be network aware (i.e., aware that there was a
 *     distributed database). <br>
 *     If we change to allowing object graph traversals during conflict resolution, then we need to
 *     closely consider whether cycles can form and how they will ground out.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IConflictResolver extends Serializable {

  /*
   * Resolve a write-write conflict between a committed version on the journal and the current
   * version within a transaction that is validating.
   *
   * @param writeSet The view on which conflict resolver must write the resolved index entry.
   * @param txTuple The tuple written by the transaction that is being validated.
   * @param currentTuple The current tuple committed in the global state with which the write-write
   *     conflict exists.
   * @return true iff the conflict was resolved.
   */
  boolean resolveConflict(IIndex writeSet, ITuple txTuple, ITuple currentTuple)
      throws Exception;
}
