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
 * Created on Nov 26, 2015
 */
package org.embergraph.bop.join;

import cutthecrap.utils.striterators.ICloseableIterator;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.PipelineOp;
import org.embergraph.relation.accesspath.UnsyncLocalOutputBuffer;

/*
* Special interface for pipelines hash join implementations, offering a method that combines
 * acceptance and outputting of solutions.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface PipelinedHashJoinUtility {

  /*
   * AcceptAndOutputSolutions is a special method for building the hash index of the {@link
   * JVMPipelinedHashIndex}, which accepts and immediately forwards relevant solutions (non-blocking
   * index).
   */
  long acceptAndOutputSolutions(
      final UnsyncLocalOutputBuffer<IBindingSet> out,
      final ICloseableIterator<IBindingSet[]> itr,
      final NamedSolutionSetStats stats,
      final IConstraint[] joinConstraints,
      final PipelineOp subquery,
      final IBindingSet[] bsFromBindingsSetSource,
      final IVariable<?>[] projectInVars,
      final IVariable<?> askVar,
      final boolean isLastInvocation,
      final int distinctProjectionBufferThreshold,
      final int incomingBindingsBufferThreshold,
      final BOpContext<IBindingSet> context);
}
