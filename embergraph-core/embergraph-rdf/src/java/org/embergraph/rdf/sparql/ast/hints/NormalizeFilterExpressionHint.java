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
 * Created on June 29, 2015.
 */

package org.embergraph.rdf.sparql.ast.hints;

import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.optimizers.ASTFilterNormalizationOptimizer;

/**
 * Query hint to enable/disable normalization and decomposition of complex FILTER expressions with
 * {@link QueryHints#NORMALIZE_FILTER_EXPRESSIONS}. The hint applies to groups or subgroups.
 *
 * <p>Note: This sets an AST annotation which is interpreted by the {@link
 * ASTFilterNormalizationOptimizer}.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
final class NormalizeFilterExpressionHint extends AbstractBooleanQueryHint {

  protected NormalizeFilterExpressionHint() {

    super(QueryHints.NORMALIZE_FILTER_EXPRESSIONS, null /* default */);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final Boolean value) {

    switch (scope) {
      case Group:
      case GroupAndSubGroups:
      case Query:
      case SubQuery:
        if (op instanceof JoinGroupNode) {
          _setAnnotation(context, scope, op, getName(), value);
        }
        return;
      default:
        break;
    }

    throw new QueryHintException(scope, op, getName(), value);
  }
}
