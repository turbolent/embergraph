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
package org.embergraph.rdf.sail;

import java.util.concurrent.TimeUnit;

import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.repository.sail.SailGraphQuery;

import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.BindingsClause;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.ASTEvalHelper;
import org.embergraph.rdf.store.AbstractTripleStore;

public class EmbergraphSailGraphQuery extends SailGraphQuery implements
    EmbergraphSailQuery {

    // private static Logger log = Logger.getLogger(EmbergraphSailGraphQuery.class);

    private final ASTContainer astContainer;

    public ASTContainer getASTContainer() {

        return astContainer;

    }

    @Override
    public String toString() {

        return astContainer.toString();

    }
    
    public AbstractTripleStore getTripleStore() {

        return ((EmbergraphSailRepositoryConnection) getConnection())
                .getTripleStore();

    }

    public EmbergraphSailGraphQuery(final ASTContainer astContainer,
            final EmbergraphSailRepositoryConnection con) {

        super(null/*tupleQuery*/, con);

        if(astContainer == null)
            throw new IllegalArgumentException();
        
        this.astContainer = astContainer;
        
    }
    
    @Override
    public GraphQueryResult evaluate() throws QueryEvaluationException {

        return evaluate((BindingsClause) null);

    }

    public GraphQueryResult evaluate(final BindingsClause bc)
            throws QueryEvaluationException {

        final QueryRoot originalQuery = astContainer.getOriginalAST();

        if (bc != null)
            originalQuery.setBindingsClause(bc);

        if (getMaxQueryTime() > 0)
            originalQuery.setTimeout(TimeUnit.SECONDS
                    .toMillis(getMaxQueryTime()));

        originalQuery.setIncludeInferred(getIncludeInferred());

        final GraphQueryResult queryResult = ASTEvalHelper.evaluateGraphQuery(
                getTripleStore(), astContainer, new QueryBindingSet(
                        getBindings()), getDataset());

        return queryResult;

    }

    public QueryRoot optimize() throws QueryEvaluationException {

        return optimize((BindingsClause) null);

    }

    public QueryRoot optimize(final BindingsClause bc)
            throws QueryEvaluationException {

        final QueryRoot originalQuery = astContainer.getOriginalAST();

        if (bc != null)
            originalQuery.setBindingsClause(bc);

        if (getMaxQueryTime() > 0)
            originalQuery.setTimeout(TimeUnit.SECONDS
                    .toMillis(getMaxQueryTime()));

        originalQuery.setIncludeInferred(getIncludeInferred());

        final QueryRoot optimized = ASTEvalHelper.optimizeQuery(
                astContainer,
                new AST2BOpContext(astContainer, getTripleStore()),
                new QueryBindingSet(getBindings()), getDataset());

        return optimized;

    }

}
