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

import org.embergraph.bop.ap.SampleIndex.SampleType;
import org.embergraph.bop.joinGraph.rto.JGraph;
import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/*
* The query hint governing the choice of the sampling bais for the RTO optimizer.
 *
 * @see JGraph
 * @see SampleType
 * @see QueryHints#RTO_SAMPLE_TYPE
 */
final class RTOSampleTypeQueryHint extends AbstractQueryHint<SampleType> {

  public RTOSampleTypeQueryHint() {
    super(QueryHints.RTO_SAMPLE_TYPE, QueryHints.DEFAULT_RTO_SAMPLE_TYPE);
  }

  @Override
  public SampleType validate(final String value) {

    return SampleType.valueOf(value);
  }

  @Override
  public void handle(
      final AST2BOpContext ctx,
      final QueryRoot queryRoot,
      final QueryHintScope scope,
      final ASTBase op,
      final SampleType value) {

    switch (scope) {
      case Group:
      case GroupAndSubGroups:
      case Query:
      case SubQuery:
        if (op instanceof JoinGroupNode) {
          _setAnnotation(ctx, scope, op, getName(), value);
        }
        return;
    }
    throw new QueryHintException(scope, op, getName(), value);
  }
}
