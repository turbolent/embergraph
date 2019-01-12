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
 * Created on Nov 1, 2015.
 */

package org.embergraph.rdf.sparql.ast.hints;

import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.PropertyPathNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/*
 * Query hint to enable users choose the gearing used for evaluating recursive property paths.
 * Generally speaking, the problem with recursive property path queries is that it is inherently
 * hard to choose the gearing. This hint allows the user to take control over the gearing.
 *
 * @author <a href="mailto:ms@blazegraph.com">Michael Schmidt</a>
 */
final class GearingHint extends AbstractStringQueryHint {

  protected GearingHint() {

    super(QueryHints.GEARING, null /* default */);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final String value) {

    switch (scope) {
      case Prior:
        {
          if (op instanceof PropertyPathNode) {
            _setQueryHint(context, scope, op, getName(), value);
            return;
          }

          // fall through
        }
      default:
        break;
    }

    // query hint does not make sense in other situations
    throw new QueryHintException(scope, op, getName(), value);
  }
}
