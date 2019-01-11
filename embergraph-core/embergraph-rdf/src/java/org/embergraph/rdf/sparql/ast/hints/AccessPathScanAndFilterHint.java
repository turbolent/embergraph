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
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/**
 * For named and default graph access paths where access path cost estimation is
 * disabled by setting the {@link #ACCESS_PATH_SAMPLE_LIMIT} to ZERO (0), this
 * query hint determines whether a SCAN + FILTER or PARALLEL SUBQUERY (aka
 * as-bound data set join) approach.
 */
final class AccessPathScanAndFilterHint extends AbstractBooleanQueryHint {

    protected AccessPathScanAndFilterHint() {
        super(QueryHints.ACCESS_PATH_SCAN_AND_FILTER,
                QueryHints.DEFAULT_ACCESS_PATH_SCAN_AND_FILTER);
    }

    @Override
    public void handle(final AST2BOpContext context, final QueryRoot queryRoot,
            final QueryHintScope scope, final ASTBase op, final Boolean value) {

        if (op instanceof StatementPatternNode) {

            _setAnnotation(context, scope, op, getName(), value);

        }

    }

}
