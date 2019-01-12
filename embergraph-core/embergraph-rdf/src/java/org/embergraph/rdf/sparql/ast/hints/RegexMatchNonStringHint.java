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

import org.embergraph.bop.IValueExpression;
import org.embergraph.rdf.internal.constraints.RegexBOp;
import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.IValueExpressionNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/*
* {@link https://jira.blazegraph.com/browse/BLZG-1780}
 *
 * <p>{@see https://www.w3.org/TR/sparql11-query/#func-regex}
 *
 * <p>{@see https://www.w3.org/TR/sparql11-query/#restrictString}
 *
 * <p>By default, regex is only applied to Literal String values. Enabling this query hint will
 * attempt to autoconvert non-String literals into their string value. This is the equivalent of
 * always using the str(...) function.
 */
final class RegexMatchNonStringHint extends AbstractBooleanQueryHint {

  protected RegexMatchNonStringHint() {
    super(QueryHints.REGEX_MATCH_NON_STRING, QueryHints.DEFAULT_REGEX_MATCH_NON_STRING);
  }

  @Override
  public void handle(
      final AST2BOpContext context,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final Boolean value) {

    if (op instanceof FilterNode) {

      final IValueExpressionNode n = ((FilterNode) op).getValueExpressionNode();

      assert (n != null);

      @SuppressWarnings("rawtypes")
      final IValueExpression n2 = n.getValueExpression();

      if (n2 != null && n2 instanceof RegexBOp) {
        ((RegexBOp) n2).setMatchNonString(value);
      }
    }
  }
}
