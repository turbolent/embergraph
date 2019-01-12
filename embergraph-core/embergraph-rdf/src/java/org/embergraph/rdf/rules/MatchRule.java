package org.embergraph.rdf.rules;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.INBinarySearch;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.spo.ExplicitSPOFilter;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.store.LocalTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.accesspath.ElementFilter;
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* Rule supporting {@link LocalTripleStore#match(Literal[], URI[], URI)}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class MatchRule extends Rule<SPO> {

  /** */
  private static final long serialVersionUID = -5002902183499739018L;

  public MatchRule(
      String relationName,
      Vocabulary vocab,
      IVariable<IV> lit,
      IConstant<IV>[] preds,
      IConstant<IV> cls) {

    super(
        "matchRule",
        new SPOPredicate(relationName, var("s"), var("t"), lit),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("s"), var("p"), lit),
          new SPOPredicate(
              new BOp[] {var("s"), vocab.getConstant(RDF.TYPE), var("t")},
              new NV(IPredicate.Annotations.RELATION_NAME, new String[] {relationName}),
              new NV(
                  IPredicate.Annotations.INDEX_LOCAL_FILTER,
                  ElementFilter.newInstance(ExplicitSPOFilter.INSTANCE))),
          new SPOPredicate(relationName, var("t"), vocab.getConstant(RDFS.SUBCLASSOF), cls)
        },
        new IConstraint[] {
          Constraint.wrap(new INBinarySearch(var("p"), preds)) // p IN preds
        });
  }
}
