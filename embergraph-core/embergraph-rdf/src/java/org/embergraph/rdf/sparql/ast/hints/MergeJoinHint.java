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
 * Created on Nov 27, 2011
 */

package org.embergraph.rdf.sparql.ast.hints;

import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/** Query hint for turning the enabling/disabling merge joins. */
final class MergeJoinHint extends AbstractBooleanQueryHint {

  protected MergeJoinHint() {
    super(QueryHints.MERGE_JOIN, QueryHints.DEFAULT_MERGE_JOIN);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final Boolean value) {

    if (scope == QueryHintScope.Query) {

      context.mergeJoin = value;

      return;
    }

    throw new QueryHintException(scope, op, getName(), value);
  }
}
