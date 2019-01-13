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
 * Created on Sep 20, 2010
 */

package org.embergraph.bop.rdf.join;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.rdf.internal.IV;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.UnsynchronizedArrayBuffer;

/*
 * DataSetJoin(left)[var=g; graphs={graphIds}]
 *
 * <p>SPARQL specific join of the source binding sets with an inline access path allowing <i>var</i>
 * to take on the given graphIds values. This join operator is useful when the multiplicity of the
 * graphs is small to moderate. If there are a very large number of graphs, then the operator tree
 * is to cumbersome and you would do better off joining against an index (whether temporary or
 * permanent) containing the graphs.
 *
 * <p>The evaluation context is {@link BOpEvaluationContext#ANY}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DataSetJoin extends PipelineOp {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations extends PipelineOp.Annotations {

    /** The variable to be bound. */
    String VAR = DataSetJoin.class.getName() + ".var";

    /*
     * The {@link Set} of {@link IV}s to be bound. A {@link LinkedHashSet} should be used for
     * efficiency since it provides fast ordered scans and fast point tests.
     */
    String GRAPHS = DataSetJoin.class.getName() + ".graphs";
  }

  /*
   * Deep copy constructor.
   *
   * @param op
   */
  public DataSetJoin(final DataSetJoin op) {

    super(op);
  }

  /*
   * Shallow copy constructor.
   *
   * @param args
   * @param annotations
   */
  public DataSetJoin(final BOp[] args, final Map<String, Object> annotations) {

    super(args, annotations);

    getVar();

    getGraphs();
  }

  public DataSetJoin(final BOp[] args, final NV... annotations) {

    this(args, NV.asMap(annotations));
  }

  public IVariable<?> getVar() {

    return (IVariable<?>) getRequiredProperty(Annotations.VAR);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<IV> getGraphs() {

    return (Set<IV>) getRequiredProperty(Annotations.GRAPHS);
  }

  @Override
  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<>(new DataSetJoinTask(this, context));
  }

  /** Specialized in-memory join. */
  private static class DataSetJoinTask implements Callable<Void> {

    private final DataSetJoin op;

    private final BOpContext<IBindingSet> context;

    private final IVariable<?> var;

    @SuppressWarnings("rawtypes")
    private final Set<IV> graphs;

    DataSetJoinTask(final DataSetJoin op, final BOpContext<IBindingSet> context) {

      this.op = op;

      this.context = context;

      var = op.getVar();

      graphs = op.getGraphs();
    }

    @Override
    public Void call() throws Exception {

      final ICloseableIterator<IBindingSet[]> source = context.getSource();

      final IBlockingBuffer<IBindingSet[]> sink = context.getSink();

      try {

        final BOpStats stats = context.getStats();

        final UnsynchronizedArrayBuffer<IBindingSet> tmp =
            new UnsynchronizedArrayBuffer<>(
                sink, IBindingSet.class, op.getChunkCapacity());

        while (source.hasNext()) {

          final IBindingSet[] chunk = source.next();

          stats.chunksIn.increment();

          stats.unitsIn.add(chunk.length);

          handleChunk(chunk, tmp);
        }

        tmp.flush();

        sink.flush();

        return null;

      } finally {

        sink.close();

        source.close();
      }
    }

    /*
     * Join source binding set chunk with {@link #graphs}.
     *
     * @param chunk A chunk of {@link IBindingSet}s from the source.
     * @param tmp Where to write the data.
     */
    @SuppressWarnings("rawtypes")
    private void handleChunk(
        final IBindingSet[] chunk, final UnsynchronizedArrayBuffer<IBindingSet> tmp) {

      for (IBindingSet bset : chunk) {

        final IConstant val = bset.get(var);

        if (val == null) {

          /*
           * When the value is unbound, we output the cross product.
           */

          for (IV c : graphs) {

            bset = bset.clone();

            bset.set(var, new Constant<>(c));

            tmp.add(bset);
          }

        } else {

          /*
           * When the value is bound the binding set will be output
           * iff the bound value for the variable is found in the
           * specified graphs.
           */
          if (graphs.contains(val.get())) {

            // match. output binding set.
            tmp.add(bset);
          }
        }
      }
    }
  }
}
