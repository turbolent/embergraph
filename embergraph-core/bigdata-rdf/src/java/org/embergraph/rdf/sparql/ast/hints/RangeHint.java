/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Mark a statement pattern as "range safe", which in effect means it 
 * uses only one datatype in it value space (for bindings for O) and
 * that the filters in the query are respecting that datatype.
 */
final class RangeHint extends AbstractQueryHint<Boolean> {

    protected RangeHint() {
        super(QueryHints.RANGE_SAFE, false);
    }

    @Override
    public void handle(final AST2BOpContext context,
            final QueryRoot queryRoot,
            final QueryHintScope scope, final ASTBase op,
            final Boolean value) {

        if (op instanceof StatementPatternNode) {

            _setQueryHint(context, scope, op, getName(), value);

            return;

        }

    }

    @Override
    public Boolean validate(String value) {

        return Boolean.valueOf(value);
        
    }

}
