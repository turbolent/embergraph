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

import com.bigdata.rdf.sparql.ast.ASTBase;
import com.bigdata.rdf.sparql.ast.JoinGroupNode;
import com.bigdata.rdf.sparql.ast.QueryHints;
import com.bigdata.rdf.sparql.ast.QueryOptimizerEnum;
import com.bigdata.rdf.sparql.ast.QueryRoot;
import com.bigdata.rdf.sparql.ast.eval.AST2BOpContext;

/**
 * The query hint governing the choice of the join order optimizer.
 */
public final class OptimizerQueryHint extends
        AbstractQueryHint<QueryOptimizerEnum> {

    public OptimizerQueryHint() {
        super(QueryHints.OPTIMIZER, QueryOptimizerEnum.Static);
    }

    @Override
    public QueryOptimizerEnum validate(final String value) {

        return QueryOptimizerEnum.valueOf(value);

    }

    @Override
    public void handle(final AST2BOpContext ctx, 
            final QueryRoot queryRoot,
            final QueryHintScope scope,
            final ASTBase op, final QueryOptimizerEnum value) {

        switch (scope) {
        case Query:
//            /*
//             * Also stuff the query hint on the global context for things which
//             * look there.
//             */
//            conditionalSetGlobalProperty(ctx, getName(), value);
        case Group:
        case GroupAndSubGroups:
        case SubQuery:
            if (op instanceof JoinGroupNode) {
                _setAnnotation(ctx, scope, op, getName(), value);
            }
            return;
        }
        throw new QueryHintException(scope, op, getName(), value);

    }

}
