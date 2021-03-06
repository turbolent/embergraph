package org.embergraph.rdf.sail;

import java.util.concurrent.TimeUnit;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.BindingsClause;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.ASTEvalHelper;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.repository.sail.SailTupleQuery;

public class EmbergraphSailTupleQuery extends SailTupleQuery implements EmbergraphSailQuery {

  //    private static final Logger log = Logger.getLogger(EmbergraphSailTupleQuery.class);

  private final ASTContainer astContainer;

  public ASTContainer getASTContainer() {

    return astContainer;
  }

  @Override
  public String toString() {

    return astContainer.toString();
  }

  public AbstractTripleStore getTripleStore() {

    return ((EmbergraphSailRepositoryConnection) getConnection()).getTripleStore();
  }

  public EmbergraphSailTupleQuery(
      final ASTContainer astContainer, final EmbergraphSailRepositoryConnection con) {

    super(null /* tupleQuery */, con);

    if (astContainer == null) throw new IllegalArgumentException();

    this.astContainer = astContainer;
  }

  @Override
  public TupleQueryResult evaluate() throws QueryEvaluationException {

    return evaluate((BindingsClause) null);
  }

  public TupleQueryResult evaluate(final BindingsClause bc) throws QueryEvaluationException {

    final QueryRoot originalQuery = astContainer.getOriginalAST();

    if (bc != null) originalQuery.setBindingsClause(bc);

    if (getMaxQueryTime() > 0)
      originalQuery.setTimeout(TimeUnit.SECONDS.toMillis(getMaxQueryTime()));

    originalQuery.setIncludeInferred(getIncludeInferred());

    final TupleQueryResult queryResult =
        ASTEvalHelper.evaluateTupleQuery(
            getTripleStore(), astContainer, new QueryBindingSet(getBindings()), getDataset());

    return queryResult;
  }

  public QueryRoot optimize() throws QueryEvaluationException {

    return optimize(null);
  }

  public QueryRoot optimize(final BindingsClause bc) throws QueryEvaluationException {

    final QueryRoot originalQuery = astContainer.getOriginalAST();

    if (bc != null) originalQuery.setBindingsClause(bc);

    if (getMaxQueryTime() > 0)
      originalQuery.setTimeout(TimeUnit.SECONDS.toMillis(getMaxQueryTime()));

    originalQuery.setIncludeInferred(getIncludeInferred());

    final QueryRoot optimized =
        ASTEvalHelper.optimizeQuery(
            astContainer,
            new AST2BOpContext(astContainer, getTripleStore()),
            new QueryBindingSet(getBindings()),
            getDataset());

    return optimized;
  }
}
