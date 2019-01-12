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
import org.embergraph.rdf.sparql.ast.DescribeModeEnum;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/*
* Query hint used to indicate the {@link DescribeMode} that will be used to evaluate a DESCRIBE
 * query.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/578">Concise Bounded Description
 *     </a>
 * @see QueryHints#DESCRIBE_MODE
 */
final class DescribeModeHint extends AbstractQueryHint<DescribeModeEnum> {

  protected DescribeModeHint() {
    super(QueryHints.DESCRIBE_MODE, QueryHints.DEFAULT_DESCRIBE_MODE);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final DescribeModeEnum value) {

    if (op instanceof ProjectionNode) {

      // _setQueryHint(context, scope, op, getName(), value);
      ((ProjectionNode) op).setDescribeMode(value);

      return;
    }

    // throw new QueryHintException(scope, op, getName(), value);

  }

  @Override
  public DescribeModeEnum validate(String value) {

    return DescribeModeEnum.valueOf(value);
  }
}
