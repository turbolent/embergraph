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

import org.embergraph.bop.rdf.filter.NativeDistinctFilter;
import org.embergraph.rdf.sparql.ast.ASTBase;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;

/**
 * Query hint for turning the {@link NativeDistinctFilter} on/off.
 */
final class NativeDistinctSPOThresholdHint extends AbstractLongQueryHint {

    protected NativeDistinctSPOThresholdHint() {
        super(QueryHints.NATIVE_DISTINCT_SPO_THRESHOLD,
                QueryHints.DEFAULT_NATIVE_DISTINCT_SPO_THRESHOLD);
    }

    @Override
    public void handle(final AST2BOpContext context,
            final QueryRoot queryRoot,
            final QueryHintScope scope, final ASTBase op, final Long value) {

        if (scope == QueryHintScope.Query) {

            context.nativeDistinctSPOThreshold = value;

            return;

            // } else {

            // super.attach(context, scope, op, value);

        }

        throw new QueryHintException(scope, op, getName(), value);

    }

}
