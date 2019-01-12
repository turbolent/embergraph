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
 * Created on Aug 20, 2008
 */

package org.embergraph.rdf.rules;

import org.apache.log4j.Logger;
import org.embergraph.bop.IPredicate;
import org.embergraph.btree.IIndex;
import org.embergraph.rdf.axioms.Axioms;
import org.embergraph.rdf.inf.BackchainTypeResourceIterator;
import org.embergraph.rdf.inf.OwlSameAsPropertiesExpandingIterator;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.striterator.ChunkedWrappedIterator;
import org.embergraph.striterator.IChunkedIterator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.embergraph.striterator.IKeyOrder;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
 * A read-only {@link IAccessPath} that backchains certain inferences.
 *
 * <p>Note: Low level methods may not behave quite as expected since some elements will be generated
 * by the backchainer and hence present in the underlying {@link SPORelation}. See the notes on the
 * various methods in the API for more details.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BackchainAccessPath implements IAccessPath<ISPO> {

  protected static final transient Logger log = Logger.getLogger(BackchainAccessPath.class);

  protected static final boolean INFO = log.isInfoEnabled();

  protected static final boolean DEBUG = log.isDebugEnabled();

  private final AbstractTripleStore database;
  private final IAccessPath<ISPO> accessPath;

  /*
   * Message thread related to the introduction of this property and possible side-effects when
   * computing closure.
   *
   * <p>I have refactored to allow the joinNexus(Factory) to pass along [isOwlSameAsUsed]. it is
   * true iff the axiom model supports sameAs AND there is an owl:sameAs assertion in the data. It
   * is evaluated once per program by AbstractTripleStore#newJoinNexusFactory(...).
   *
   * <p>I have one question. Can a closure rule entail an owl:sameAs assertion if there are none in
   * the data? I.e., are there scenarios under which [isOwlSameAsUsed] would evaluate to [false] if
   * tested before closure and to [true] if evaluated after closure. I don't think that it matters
   * either way since we don't use the sameAs backchainer during closure itself, but I wanted to run
   * it past you anyway. -bryan
   *
   * <p>The only way that could happen is if there were a property that was a subproperty of
   * owl:sameAs and that subproperty was used in the data. I've never seen anything like that, but
   * it is technically possible. -mike
   *
   * <p>Ok. But still, it is not a problem since we are not using the backchainer during closure,
   * right? -bryan
   *
   * <p>We do not use the backchainer during closure, correct. -mike
   */
  private Boolean isOwlSameAsUsed;

  /*
   * @param database The database whose entailments will be backchained.
   * @param accessPath The source {@link IAccessPath}.
   */
  public BackchainAccessPath(AbstractTripleStore database, IAccessPath<ISPO> accessPath) {

    this(database, accessPath, null);
  }

  /*
   * @param database The database whose entailments will be backchained.
   * @param accessPath The source {@link IAccessPath}.
   * @param isOwlSameAsUsed When non-<code>null</code>, this {@link Boolean} indicates whether the
   *     statement pattern <code>(x owl:sameAs y)</code> is known to be empty in the data. Specify
   *     <code>null</code> if you do not know this up front. This parameter is used to factor out
   *     the test for this statement pattern, but that test is only performed if {@link
   *     Axioms#isOwlSameAs()} is <code>true</code>.
   */
  public BackchainAccessPath(
      AbstractTripleStore database, IAccessPath<ISPO> accessPath, Boolean isOwlSameAsUsed) {

    if (database == null) throw new IllegalArgumentException();

    if (accessPath == null) throw new IllegalArgumentException();

    this.database = database;

    this.accessPath = accessPath;

    // MAY be null
    this.isOwlSameAsUsed = isOwlSameAsUsed;
  }

  /** The source {@link IAccessPath}. */
  public final IAccessPath<ISPO> getSource() {

    return accessPath;
  }

  @Override
  public final IIndex getIndex() {

    return accessPath.getIndex();
  }

  @Override
  public final IKeyOrder<ISPO> getKeyOrder() {

    return accessPath.getKeyOrder();
  }

  @Override
  public final IPredicate<ISPO> getPredicate() {

    return accessPath.getPredicate();
  }

  @Override
  public boolean isEmpty() {

    final IChunkedIterator<ISPO> itr = iterator(0L /* offset */, 1L /* limit */, 1 /* capacity */);

    try {

      return !itr.hasNext();

    } finally {

      itr.close();
    }
  }

  /*
   * {@inheritDoc}
   *
   * <p>Visits elements in the source {@link IAccessPath} plus all entailments licensed by the
   * {@link InferenceEngine}.
   */
  @Override
  public IChunkedOrderedIterator<ISPO> iterator() {

    return iterator(0L /* offset */, 0L /* limit */, 0 /* capacity */);
  }

  //    /*
  //     * Visits elements in the source {@link IAccessPath} plus all entailments
  //     * licensed by the {@link InferenceEngine} as configured.
  //     */
  //    public IChunkedOrderedIterator<ISPO> iterator(final int limit,
  //            final int capacity) {
  //
  //        return iterator(0L/*offset*/,limit,capacity);
  //
  //    }

  /*
   * {@inheritDoc}
   *
   * @todo handle non-zero offset and larger limits?
   */
  @Override
  public IChunkedOrderedIterator<ISPO> iterator(final long offset, long limit, int capacity) {

    if (offset > 0L) throw new UnsupportedOperationException();

    if (limit == Long.MAX_VALUE) limit = 0L;

    if (limit > Integer.MAX_VALUE) throw new UnsupportedOperationException();

    //        return iterator((int) limit, capacity);
    //
    //    }
    //
    //    /*
    //     * Visits elements in the source {@link IAccessPath} plus all entailments
    //     * licensed by the {@link InferenceEngine} as configured.
    //     */
    //    public IChunkedOrderedIterator<ISPO> iterator(final int limit,
    //            final int capacity) {

    if (INFO) {

      log.info(accessPath.getPredicate().toString());
    }

    final IPredicate<ISPO> predicate = accessPath.getPredicate();

    final InferenceEngine inf = database.getInferenceEngine();

    final Vocabulary vocab = database.getVocabulary();

    final Axioms axioms = database.getAxioms();

    final IChunkedOrderedIterator<ISPO> owlSameAsItr;

    if (!axioms.isOwlSameAs()) {

      /*
       * No owl:sameAs entailments.
       */

      owlSameAsItr = null;

    } else if (inf.forwardChainOwlSameAsClosure && !inf.forwardChainOwlSameAsProperties) {

      if (isOwlSameAsUsed != null && !isOwlSameAsUsed.booleanValue()) {

        /*
         * The caller asserted that no owl:sameAs assertions exist in
         * the KB, so we do not need to backchain owl:sameAs.
         */

        owlSameAsItr = null;

      } else {

        final IV owlSameAs = vocab.get(OWL.SAMEAS);

        if (isOwlSameAsUsed == null) {

          /*
           * The caller did not specify whether or not there are
           * owl:sameAs assertions in the data so we have to test the
           * data ourselves.
           */

          isOwlSameAsUsed = database.getAccessPath(null, owlSameAs, null).isEmpty();
        }

        if (isOwlSameAsUsed.booleanValue()) {

          /*
           * No owl:sameAs assertions in the KB, so we do not need to
           * backchain owl:sameAs.
           */

          owlSameAsItr = null;

        } else {

          // There is at least one owl:sameAs assertion in the data.
          final SPO spo = new SPO(predicate);

          owlSameAsItr =
              new OwlSameAsPropertiesExpandingIterator(
                  spo.s, spo.p, spo.o, database, owlSameAs, accessPath.getKeyOrder());
        }
      }

    } else {

      // no owl:sameAs entailments.
      owlSameAsItr = null;
    }

    /*
     * Wrap it up as a chunked iterator.
     *
     * Note: If we are not adding any entailments then we just use the
     * source iterator directly.
     *
     * FIXME Why is the filter being passed in here? Can the backchaining
     * iterators produce entailments that would violate the filter? If so,
     * then shouldn't the filter be applied by the backchainers themselves
     * so that they do not overgenerate? Is this because those filters might
     * cause a problem when reading on the other tails used by the sameAs
     * expansion? (This comment also applies for the type resource
     * backchainer, below).
     */

    if (predicate.getIndexLocalFilter() != null)
      throw new UnsupportedOperationException("indexLocalFilter in expander: " + this);
    if (predicate.getAccessPathFilter() != null)
      throw new UnsupportedOperationException("accessPathFilter in expander: " + this);
    final IElementFilter<ISPO> filter = null;
    //        final IElementFilter<ISPO> filter = predicate.getConstraint();
    //        final IFilter indexLocalFilter = predicate.getIndexLocalFilter();
    //        final IFilter accessPathFilter = predicate.getAccessPathFilter();
    //
    //        final Striterator tmp = new Striterator(owlSameAsItr);
    //        if(indexLocalFilter!=null)
    //            tmp.addFilter(indexLocalFilter);
    //        if(accessPathFilter!=null)
    //            tmp.addFilter(accessPathFilter);

    IChunkedOrderedIterator<ISPO> itr =
        (owlSameAsItr == null
            ? accessPath.iterator(offset, limit, capacity)
            : new ChunkedWrappedIterator<ISPO>(
                owlSameAsItr,
                capacity == 0 ? inf.database.getChunkCapacity() : capacity,
                null /* keyOrder */,
                filter));

    if (axioms.isRdfSchema() && !inf.forwardChainRdfTypeRdfsResource) {

      final IV rdfType = vocab.get(RDF.TYPE);

      final IV rdfsResource = vocab.get(RDFS.RESOURCE);

      /*
       * Backchain (x rdf:type rdfs:Resource ), which is an entailment
       * declared for RDFS Schema.
       *
       * @todo pass the filter in here also.
       */

      itr =
          BackchainTypeResourceIterator.newInstance(
              itr, accessPath, database, rdfType, rdfsResource);
    }

    return itr;
  }

  /*
   * In progress.
  public IChunkedOrderedIterator<ISPO> iterator2(int limit, int capacity) {

      if (log.isInfoEnabled()) {

          log.info(accessPath.getPredicate().toString());

      }

      // pass the limit and capacity through to the source access path.
      final IChunkedOrderedIterator<ISPO> src = null;
          // accessPath.iterator(limit, capacity);

      final IChunkedOrderedIterator<ISPO> owlSameAsItr;

      final IPredicate<ISPO> predicate = accessPath.getPredicate();

      final SPO spo = new SPO(predicate);

      if (inf.rdfsOnly) {

          // no owl:sameAs entailments.
          owlSameAsItr = accessPath.iterator(limit, capacity);

      } else if(inf.forwardChainOwlSameAsClosure && !inf.forwardChainOwlSameAsProperties) {

          if (inf.database.getAccessPath(NULL, inf.owlSameAs.get(), NULL)
                  .rangeCount(false/*exact*/
  /*) == 0L) {

  /*
   * No owl:sameAs assertions in the KB, so we do not need to
   * backchain owl:sameAs.
   */
  /*

          owlSameAsItr = accessPath.iterator(limit, capacity);

      } else {

          owlSameAsItr = new OwlSameAsPropertiesExpandingIterator(
                  spo.s, spo.p, spo.o,
                  inf.database,
                  inf.owlSameAs.get(), accessPath.getKeyOrder());
      }

  } else {

      // no owl:sameAs entailments.
      owlSameAsItr = accessPath.iterator(limit, capacity);

  }

  /*
   * Wrap it up as a chunked iterator.
   *
   * Note: If we are not adding any entailments then we just use the
   * source iterator directly.
   *
   * @todo why is the filter being passed in here? Can the backchaining
   * iterators produce entailments that would violate the filter? If so,
   * then shouldn't the filter be applied by the backchainers themselves
   * so that they do not overgenerate? (This comment also applies for the
   * type resource backchainer, below).
   */
  /*

  final IElementFilter<ISPO> filter = predicate.getConstraint();

  IChunkedOrderedIterator<ISPO> itr = (owlSameAsItr instanceof OwlSameAsPropertiesExpandingIterator
          ? new ChunkedWrappedIterator<ISPO>(owlSameAsItr,
                  capacity == 0 ? inf.database.queryBufferCapacity
                          : capacity, null/* keyOrder */
  /*, filter)
          : src
  );

  if (!inf.forwardChainRdfTypeRdfsResource) {

      /*
       * Backchain (x rdf:type rdfs:Resource ).
       *
       * @todo pass the filter in here also.
       */
  /*

          itr = BackchainTypeResourceIterator.newInstance(
                  itr,
                  accessPath,
                  inf.database,
                  inf.rdfType.get(),
                  inf.rdfsResource.get()
                  );

      }

      return itr;

  }
  */

  /*
   * {@inheritDoc}
   *
   * <p>When <code>exact == false</code> this does not count the backchained entailments. When
   * <code>exact == true</code> traverses the {@link #iterator()} so as to produce an exact count of
   * the #of elements that would in fact be visited, which combines those from the database with
   * those generated dynamically (NOT efficient).
   */
  @Override
  public long rangeCount(boolean exact) {

    if (!exact) return accessPath.rangeCount(exact);

    log.warn("Will materialize statements and generate inferences");

    final IChunkedIterator<ISPO> itr = iterator();

    long n = 0L;

    try {

      while (itr.hasNext()) {

        itr.next();

        n++;
      }

    } finally {

      itr.close();
    }

    return n;
  }

  //    /*
  //     * Delegated to the source {@link IAccessPath} (does not visit any
  //     * entailments).
  //     */
  //    @Override
  //    public ITupleIterator<ISPO> rangeIterator() {
  //
  //        return accessPath.rangeIterator();
  //
  //    }

  @Override
  public long removeAll() {

    return accessPath.removeAll();
  }

  public String toString() {

    return super.toString()
        + "{isOwlSameAsUsed="
        + isOwlSameAsUsed
        + ", source="
        + accessPath
        + "}";
  }
}
