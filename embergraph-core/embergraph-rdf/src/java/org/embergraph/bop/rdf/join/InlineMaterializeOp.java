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

package org.embergraph.bop.rdf.join;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.NotMaterializedException;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.relation.accesspath.IBlockingBuffer;

/**
 * This operator is used as part of the EmbergraphValue materialization step inside the pipeline.
 * Inline IVs are routes to this bop to be materialized and have their EmbergraphValue cached on
 * them. The inline IVs need access to the LexiconRelation to materialize themselves, but only to
 * the class itself, not to the data in its indices. The lexicon's LexiconConfiguration is used by
 * the ExtensionIVs in the materialization process.
 *
 * @author mikepersonick
 */
public class InlineMaterializeOp<E> extends PipelineOp {

  /** */
  private static final long serialVersionUID = 3371029059242171846L;

  private static final transient Logger log = Logger.getLogger(InlineMaterializeOp.class);

  public interface Annotations extends PipelineOp.Annotations {

    /**
     * The {@link IPredicate} contains information about the how to find the lexicon relation and
     * which variable in the incoming binding sets needs materializing.
     */
    String PREDICATE = InlineMaterializeOp.class.getName() + ".predicate";
  }

  /**
   * Deep copy constructor.
   *
   * @param op
   */
  public InlineMaterializeOp(final InlineMaterializeOp<E> op) {
    super(op);
  }

  /**
   * Shallow copy constructor.
   *
   * @param args
   * @param anns
   */
  public InlineMaterializeOp(final BOp[] args, final Map<String, Object> anns) {
    super(args, anns);
  }

  public InlineMaterializeOp(final BOp[] args, final NV... anns) {
    super(args, NV.asMap(anns));
  }

  /**
   * {@inheritDoc}
   *
   * @see Annotations#PREDICATE
   */
  @SuppressWarnings("unchecked")
  public IPredicate<E> getPredicate() {

    return (IPredicate<E>) getRequiredProperty(Annotations.PREDICATE);
  }

  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<Void>(new MaterializeTask(this, context));
  }

  /** Copy the source to the sink after materializing the EmbergraphValues. */
  private static class MaterializeTask implements Callable<Void> {

    private final BOpStats stats;

    private final IVariable<IV> v;

    private final LexiconRelation lex;

    private final ICloseableIterator<IBindingSet[]> source;

    private final IBlockingBuffer<IBindingSet[]> sink;

    MaterializeTask(final InlineMaterializeOp op, final BOpContext<IBindingSet> context) {

      this.stats = context.getStats();

      final IPredicate predicate = op.getPredicate();

      if (predicate == null) throw new IllegalArgumentException();

      this.v = (IVariable<IV>) predicate.get(1);

      this.lex = (LexiconRelation) context.getRelation(predicate);

      this.source = context.getSource();

      this.sink = context.getSink();
    }

    public Void call() throws Exception {

      try {

        if (log.isDebugEnabled()) {
          log.debug("starting inline materialization");
        }

        while (source.hasNext()) {

          final IBindingSet[] chunk = source.next();

          if (log.isDebugEnabled()) {
            log.debug("chunk length: " + chunk.length);
          }

          stats.chunksIn.increment();
          stats.unitsIn.add(chunk.length);

          final IBindingSet[] def = new IBindingSet[chunk.length];

          int ndef = 0, nalt = 0;

          for (int i = 0; i < chunk.length; i++) {

            final IBindingSet bset = chunk[i];

            final IV iv = v.get(bset);

            if (log.isDebugEnabled()) {
              log.debug("materializing: " + iv);
            }

            if (iv != null && iv.isInline()) {

              /*
               * This will materialize the EmbergraphValue and cache
               * it on the IV as a side-effect.
               */
              iv.asValue(lex);

              if (!iv.hasValue()) {
                throw new NotMaterializedException();
              }

              if (log.isDebugEnabled()) {
                log.debug("value: " + iv.getValue());
              }

              def[ndef++] = bset;
            }
          }

          if (ndef > 0) {
            if (ndef == def.length) sink.add(def);
            else sink.add(Arrays.copyOf(def, ndef));
          }
        }

        sink.flush();

        return null;

      } finally {

        sink.close();

        if (log.isDebugEnabled()) {
          log.debug("finished inline materialization");
        }
      }
    }
  }
}
