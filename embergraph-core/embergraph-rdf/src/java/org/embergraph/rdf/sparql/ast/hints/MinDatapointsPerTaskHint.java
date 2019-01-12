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
 * Created on Oct 22, 2015
 */

package org.embergraph.rdf.sparql.ast.hints;

import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.join.PipelineJoin.Annotations;
import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/*
* Sets the {@link PipelineOp.Annotations#MIN_DATAPOINTS_PER_TASK} annotation of an operator.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
final class MinDatapointsPerTaskHint extends AbstractIntQueryHint {

  protected MinDatapointsPerTaskHint() {
    super(Annotations.MIN_DATAPOINTS_PER_TASK, Annotations.DEFAULT_MIN_DATAPOINTS_PER_TASK);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final Integer value) {

    if (op instanceof IQueryNode) {

      /*
       * Note: This is set on the queryHint Properties object and then
       * transferred to the pipeline operator when it is generated.
       */
      _setQueryHint(context, scope, op, getName(), value);
    }
  }
}
