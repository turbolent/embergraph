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
 * Created on Mar 29, 2011
 */
package org.embergraph.rdf.store;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sail.EmbergraphValueReplacer;
import org.embergraph.relation.accesspath.BlockingBuffer;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.striterator.AbstractChunkedResolverator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;

/*
* Efficiently resolve openrdf {@link BindingSet}s to embergraph {@link IBindingSet}s (this is a
 * streaming API).
 *
 * @see EmbergraphValueReplacer
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: EmbergraphOpenRDFBindingSetsResolverator.java 6151 2012-03-16 17:56:22Z thompsonbry
 *     $
 */
public class EmbergraphOpenRDFBindingSetsResolverator
    extends AbstractChunkedResolverator<BindingSet, IBindingSet, AbstractTripleStore> {

  private static final Logger log =
      Logger.getLogger(EmbergraphOpenRDFBindingSetsResolverator.class);

  /*
   * @param db Used to resolve RDF {@link Value}s to {@link IV}s.
   * @param src The source iterator (will be closed when this iterator is closed).
   *     <p>FIXME must accept reverse bnodes map (from term identifier to blank nodes) for
   *     resolution of blank nodes within a Sesame connection context. [Is this comment relevant for
   *     this class?]
   */
  public EmbergraphOpenRDFBindingSetsResolverator(
      final AbstractTripleStore db, final IChunkedOrderedIterator<BindingSet> src) {

    super(
        db,
        src,
        new BlockingBuffer<IBindingSet[]>(
            db.getChunkOfChunksCapacity(),
            db.getChunkCapacity(),
            db.getChunkTimeout(),
            TimeUnit.MILLISECONDS));
  }

  /** Strengthens the return type. */
  public EmbergraphOpenRDFBindingSetsResolverator start(final ExecutorService service) {

    return (EmbergraphOpenRDFBindingSetsResolverator) super.start(service);
  }

  /*
   * Resolve a chunk of {@link BindingSet}s into a chunk of {@link IBindingSet}s in which RDF {@link
   * Value}s have been resolved to {@link IV}s.
   */
  protected IBindingSet[] resolveChunk(final BindingSet[] chunk) {

    return resolveChunk(state.getLexiconRelation(), chunk);
  }

  /*
   * Resolve a chunk of {@link BindingSet}s into a chunk of {@link IBindingSet}s in which RDF {@link
   * Value}s have been resolved to {@link IV}s.
   */
  public static IBindingSet[] resolveChunk(final LexiconRelation r, final BindingSet[] chunk) {

    if (log.isInfoEnabled()) log.info("Fetched chunk: size=" + chunk.length);

    /*
     * Create a collection of the distinct term identifiers used in this
     * chunk.
     *
     * Note: The [initialCapacity] is only an estimate. There are normally
     * multiple values in each binding set. However, it is also common for
     * the same Value to appear across different solutions in a chunk.
     */

    final int initialCapacity = chunk.length;

    final Collection<Value> valueSet = new LinkedHashSet<Value>(initialCapacity);

    for (BindingSet bindingSet : chunk) {

      for (Binding binding : bindingSet) {

        final Value value = binding.getValue();

        if (value != null) {

          valueSet.add(value);
        }
      }
    }

    if (log.isInfoEnabled()) log.info("Resolving " + valueSet.size() + " term identifiers");

    //        final LexiconRelation r = state.getLexiconRelation();

    final EmbergraphValueFactory vf = r.getValueFactory();

    final int nvalues = valueSet.size();

    /*
     * Convert to a EmbergraphValue[], building up a Map used to translate from
     * Value to EmbergraphValue as we go.
     */
    final EmbergraphValue[] values = new EmbergraphValue[nvalues];
    final Map<Value, EmbergraphValue> map = new LinkedHashMap<Value, EmbergraphValue>(nvalues);
    {
      int i = 0;

      for (Value value : valueSet) {

        final EmbergraphValue val = vf.asValue(value);

        map.put(value, val);

        values[i++] = val;
      }
    }

    // Batch resolve against the database.
    r.addTerms(values, nvalues, true /*readOnly*/);

    // Assemble a chunk of resolved elements
    {
      final IBindingSet[] chunk2 = new IBindingSet[chunk.length];
      int i = 0;
      for (BindingSet e : chunk) {

        final IBindingSet f = getBindingSet(e, map);

        chunk2[i++] = f;
      }

      // return the chunk of resolved elements.
      return chunk2;
    }
  }

  /*
   * Resolve the RDF {@link Value}s in the {@link BindingSet} using the map populated when we
   * fetched the current chunk and return the {@link IBindingSet} for that solution in which term
   * identifiers have been resolved to their corresponding {@link EmbergraphValue}s.
   *
   * @param solution A solution whose {@link Long}s will be interpreted as term identifiers and
   *     resolved to the corresponding {@link EmbergraphValue}s.
   * @return The corresponding {@link IBindingSet} in which the term identifiers have been resolved
   *     to {@link EmbergraphValue}s.
   * @throws IllegalStateException if the {@link IBindingSet} was not materialized with the {@link
   *     ISolution}.
   */
  @SuppressWarnings("unchecked")
  private static IBindingSet getBindingSet(
      final BindingSet bindingSet, final Map<Value, EmbergraphValue> map) {

    if (bindingSet == null) throw new IllegalArgumentException();

    if (map == null) throw new IllegalArgumentException();

    final IBindingSet out = new ListBindingSet();

    for (Binding binding : bindingSet) {

      final String name = binding.getName();

      final Value value = binding.getValue();

      final EmbergraphValue outVal = map.get(value);

      assert outVal != null;

      /*
       * This pattern is no good. What is happening here is that we
       * have an unknown term in the query or in the incoming binding
       * sets. This is ok. What we need to do in this case is stamp a
       * fresh dummy internal value, set this as the IV on the unknown
       * EmbergraphValue, and most importantly cache the unknown
       * EmbergraphValue on the dummy IV so that a) it can be accessed
       * and used in the query and b) we don't try to re-materialize
       * it later (and fail).
       */
      //
      //            final Constant<?> c;
      //
      //            if (outVal.getIV() == null) {
      //
      //                c = new Constant(TermId.mockIV(VTE.valueOf(value)));
      //
      //            } else {
      //
      //                c = new Constant(outVal.getIV());
      //
      //            }

      if (outVal.getIV() == null) {

        @SuppressWarnings("rawtypes")
        final IV dummy = TermId.mockIV(VTE.valueOf(outVal));

        outVal.setIV(dummy);
      }

      @SuppressWarnings("rawtypes")
      final IV iv = outVal.getIV();

      /*
       * We might as well always cache the materialized value on the IV
       * now, this will save time during materialization steps later.
       */
      iv.setValue(outVal);

      @SuppressWarnings("rawtypes")
      final Constant<?> c = new Constant(iv);

      out.set(Var.var(name), c);
    }

    return out;
  }
}
