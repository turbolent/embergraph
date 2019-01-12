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
package org.embergraph.rdf.lexicon;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.keys.KVO;
import org.embergraph.btree.proc.AbstractKeyArrayIndexProcedureConstructor;
import org.embergraph.btree.proc.IResultHandler;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.Term2IdWriteProc.Term2IdWriteProcConstructor;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.service.Split;
import org.embergraph.service.ndx.pipeline.KVOList;

/*
 * Synchronous RPC write on the TERM2ID index.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class Term2IdWriteTask implements Callable<KVO<EmbergraphValue>[]> {

  private static final transient Logger log = Logger.getLogger(Term2IdWriteTask.class);

  //    private final LexiconRelation r;
  private final IIndex termIdIndex;
  private final boolean readOnly;
  private final boolean storeBlankNodes;
  private final int termIdBitsToReverse;
  private final int numTerms;
  private final EmbergraphValue[] terms;
  private final WriteTaskStats stats;

  public Term2IdWriteTask(
      final IIndex termIdIndex,
      final boolean readOnly,
      final boolean storeBlankNodes,
      final int termIdBitsToReverse,
      final int numTerms,
      final EmbergraphValue[] terms,
      final WriteTaskStats stats) {

    if (termIdIndex == null) throw new IllegalArgumentException();

    if (terms == null) throw new IllegalArgumentException();

    if (numTerms < 0 || numTerms > terms.length) throw new IllegalArgumentException();

    if (stats == null) throw new IllegalArgumentException();

    //        this.r = r;

    this.termIdIndex = termIdIndex;

    this.readOnly = readOnly;

    this.storeBlankNodes = storeBlankNodes;

    this.termIdBitsToReverse = termIdBitsToReverse;

    this.numTerms = numTerms;

    this.terms = terms;

    this.stats = stats;
  }

  /*
   * Unify the {@link EmbergraphValue}s with the TERM2ID index, setting the term identifiers (TIDs)
   * on those values as a side-effect.
   *
   * @return A dense {@link KVO}[] chunk consisting of only those distinct terms whose term
   *     identifier was not already known. (This may be used to write on the reverse index).
   * @throws Exception
   */
  public KVO<EmbergraphValue>[] call() throws Exception {

    /*
     * Insert into the forward index (term -> id). This will either assign a
     * termId or return the existing termId if the term is already in the
     * lexicon.
     */

    // The #of distinct terms lacking a pre-assigned term identifier in [a].
    int ndistinct = 0;

    // A dense array of correlated tuples.
    final KVO<EmbergraphValue>[] a;
    {
      final KVO<EmbergraphValue>[] b;

      /*
       * First make sure that each term has an assigned sort key.
       */
      {
        final long _begin = System.currentTimeMillis();

        final Term2IdTupleSerializer tupleSer =
            (Term2IdTupleSerializer) termIdIndex.getIndexMetadata().getTupleSerializer();

        // may contain duplicates and/or terms with pre-assigned term
        // identifiers.
        b = generateSortKeys(tupleSer.getLexiconKeyBuilder(), terms, numTerms);

        stats.keyGenTime.add(System.currentTimeMillis() - _begin);
      }

      /*
       * Sort by the assigned sort key. This places the array into the
       * natural order for the term:id index.
       */
      {
        final long _begin = System.currentTimeMillis();

        Arrays.sort(b);

        stats.keySortTime.add(System.currentTimeMillis() - _begin);
      }

      /*
       * For each distinct term that does not have a pre-assigned term
       * identifier, add it to a remote unisolated batch operation that
       * assigns term identifiers.
       *
       * Note: Both duplicate term references and terms with their term
       * identifiers already assigned are dropped out in this step.
       */
      {
        final long _begin = System.currentTimeMillis();

        /*
         * Create a key buffer holding the sort keys. This does not
         * allocate new storage for the sort keys, but rather aligns the
         * data structures for the call to splitKeys(). This also makes
         * a[] into a dense copy of the references in b[], but without
         * duplicates and without terms that already have assigned term
         * identifiers. Note that keys[] and a[] are correlated.
         *
         * @todo Could be restated as an IDuplicateRemover, but note
         * that this case is specialized since it can drop terms whose
         * term identifier is known (they do not need to be written on
         * T2ID, but they still need to be written on the reverse index
         * to ensure a robust and consistent mapping).
         */
        final byte[][] keys = new byte[numTerms][];
        a = new KVO[numTerms];
        {
          for (int i = 0; i < numTerms; i++) {

            if (b[i].obj.getIV() != null) {

              if (log.isDebugEnabled()) log.debug("term identifier already assigned: " + b[i].obj);

              // term identifier already assigned.
              continue;
            }

            if (i > 0 && b[i - 1].obj == b[i].obj) {

              if (log.isDebugEnabled()) log.debug("duplicate term reference: " + b[i].obj);

              // duplicate reference.
              continue;
            }

            // assign to a[] (dense variant of b[]).
            a[ndistinct] = b[i];

            // assign to keys[] (dense and correlated with a[]).
            keys[ndistinct] = b[i].key;

            ndistinct++;
          }
        }

        if (ndistinct == 0) {

          /*
           * Nothing to be written.
           */

          return new KVO[0];
        }

        final AbstractKeyArrayIndexProcedureConstructor ctor =
            new Term2IdWriteProcConstructor(readOnly, storeBlankNodes, termIdBitsToReverse);

        // run the procedure.
        termIdIndex.submit(
            0 /* fromIndex */,
            ndistinct /* toIndex */,
            keys,
            null /* vals */,
            ctor,
            new Term2IdWriteProcResultHandler(a, readOnly, stats.nunknown));

        stats.indexTime.addAndGet(stats.forwardIndexTime = System.currentTimeMillis() - _begin);
      }
    }

    stats.ndistinct.addAndGet(ndistinct);

    return KVO.dense(a, ndistinct);
  } // call

  /*
   * Class applies the term identifiers assigned by the {@link Term2IdWriteProc} to the {@link
   * EmbergraphValue} references in the {@link KVO} correlated with each {@link Split} of data
   * processed by that procedure.
   *
   * <p>Note: Of necessity, this requires access to the {@link EmbergraphValue}s whose term
   * identifiers are being resolved. This implementation presumes that the array specified to the
   * ctor and the array returned for each chunk that is processed have correlated indices and that
   * the offset into {@link #a} is given by {@link Split#fromIndex}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class Term2IdWriteProcResultHandler
      implements IResultHandler<Term2IdWriteProc.Result, Void> {

    private final KVO<EmbergraphValue>[] a;
    private final boolean readOnly;

    /*
     * @todo this could be the value returned by {@link #getResult()} which would make the API
     *     simpler.
     */
    private final AtomicInteger nunknown;

    /*
     * @param a A dense array of {@link KVO}s.
     * @param readOnly if readOnly was specified for the {@link Term2IdWriteProc}.
     * @param nunknown Incremented as a side effect for each terms that could not be resolved (iff
     *     readOnly == true).
     */
    public Term2IdWriteProcResultHandler(
        final KVO<EmbergraphValue>[] a, final boolean readOnly, final AtomicInteger nunknown) {

      if (a == null) throw new IllegalArgumentException();

      if (nunknown == null) throw new IllegalArgumentException();

      this.a = a;

      this.readOnly = readOnly;

      this.nunknown = nunknown;
    }

    /*
     * Copy the assigned / discovered term identifiers onto the corresponding elements of the
     * terms[].
     */
    @Override
    public void aggregate(final Term2IdWriteProc.Result result, final Split split) {

      for (int i = split.fromIndex, j = 0; i < split.toIndex; i++, j++) {

        final IV termId = result.ivs[j];

        if (termId == null) {

          if (!readOnly) throw new AssertionError();

          nunknown.incrementAndGet();

        } else {

          // assign the term identifier.
          a[i].obj.setIV(termId);

          if (a[i] instanceof KVOList) {

            final KVOList<EmbergraphValue> tmp = (KVOList<EmbergraphValue>) a[i];

            if (!tmp.isDuplicateListEmpty()) {

              // assign the term identifier to the duplicates.
              tmp.map(new AssignTermId(termId));
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("termId=" + termId + ", term=" + a[i].obj);
          }
        }
      }
    }

    @Override
    public Void getResult() {

      return null;
    }
  }

  /*
   * Generate the sort keys for the terms.
   *
   * @param keyBuilder The object used to generate the sort keys.
   * @param terms The terms whose sort keys will be generated.
   * @param numTerms The #of terms in that array.
   * @return An array of correlated key-value-object tuples.
   *     <p>Note that {@link KVO#val} is <code>null</code> until we know that we need to write it on
   *     the reverse index.
   * @see LexiconKeyBuilder
   */
  @SuppressWarnings("unchecked")
  private final KVO<EmbergraphValue>[] generateSortKeys(
      final LexiconKeyBuilder keyBuilder, final EmbergraphValue[] terms, final int numTerms) {

    final KVO<EmbergraphValue>[] a = new KVO[numTerms];

    for (int i = 0; i < numTerms; i++) {

      final EmbergraphValue term = terms[i];

      a[i] = new KVO<EmbergraphValue>(keyBuilder.value2Key(term), null /* val */, term);
    }

    return a;
  }
}
