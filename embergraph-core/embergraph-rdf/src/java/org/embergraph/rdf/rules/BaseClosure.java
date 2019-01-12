package org.embergraph.rdf.rules;

import java.util.Collections;
import java.util.List;
import org.embergraph.rdf.axioms.Axioms;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/*
* Base class for classes that provide closure programs.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class BaseClosure {

  /*
   * The database whose configuration will determine which entailments are to be maintained and
   * which of those entailments are computed by forward closure vs backchained.
   */
  protected final AbstractTripleStore db;

  protected final boolean rdfsOnly;
  protected final boolean forwardChainRdfTypeRdfsResource;
  protected final boolean forwardChainOwlSameAsClosure;
  protected final boolean forwardChainOwlSameAsProperties;
  protected final boolean forwardChainOwlEquivalentProperty;
  protected final boolean forwardChainOwlEquivalentClass;
  protected final boolean forwardChainOwlTransitiveProperty;
  protected final boolean forwardChainOwlInverseOf;
  protected final boolean forwardChainOwlHasValue;
  protected final boolean forwardChainOwlSymmetricProperty;
  protected final boolean enableOwlFunctionalAndInverseFunctionalProperty;

  /** The {@link Axioms} declared for the database. */
  final Axioms axioms;

  /** Various term identifiers that we need to construct the rules. */
  protected final Vocabulary vocab;

  /*
   * @param db The database whose configuration will determine which entailments are to be
   *     maintained and which of those entailments are computed by forward closure vs backchained.
   * @throws IllegalArgumentException if the <i>db</i> is <code>null</code>.
   */
  protected BaseClosure(AbstractTripleStore db) {

    if (db == null) throw new IllegalArgumentException();

    this.db = db;

    final InferenceEngine inf = db.getInferenceEngine();

    axioms = db.getAxioms();

    vocab = db.getVocabulary();

    rdfsOnly = axioms.isRdfSchema() && !axioms.isOwlSameAs();

    forwardChainRdfTypeRdfsResource = inf.forwardChainRdfTypeRdfsResource;

    forwardChainOwlSameAsClosure = inf.forwardChainOwlSameAsClosure;

    forwardChainOwlSameAsProperties = inf.forwardChainOwlSameAsProperties;

    forwardChainOwlEquivalentProperty = inf.forwardChainOwlEquivalentProperty;

    forwardChainOwlEquivalentClass = inf.forwardChainOwlEquivalentClass;

    forwardChainOwlTransitiveProperty = inf.forwardChainOwlTransitiveProperty;

    forwardChainOwlInverseOf = inf.forwardChainOwlInverseOf;

    forwardChainOwlHasValue = inf.forwardChainOwlHasValue;

    forwardChainOwlSymmetricProperty = inf.forwardChainOwlSymmetricProperty;

    enableOwlFunctionalAndInverseFunctionalProperty =
        inf.enableOwlFunctionalAndInverseFunctionalProperty;
  }

  /*
   * Return the program that will be used to compute the closure of the database.
   *
   * @param database The database whose closure will be updated.
   * @param focusStore When non-<code>null</code>, the focusStore will be closed against the
   *     database with the entailments written into the database. When <code>null</code>, the entire
   *     database will be closed (database-at-once closure).
   * @return The program to be executed.
   * @todo the returned program can be cached for a given database and focusStore (or for the
   *     database if no focusStore is used).
   */
  public abstract MappedProgram getProgram(String database, String focusStore);

  /*
   * Allow subclasses of the fast and full closure programs to provide a set of custom rules that
   * will be run towards the end of the standard closure program.
   */
  protected List<Rule> getCustomRules(final String database) {
    return Collections.EMPTY_LIST;
  }
}
