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
 * Created on Oct 29, 2011
 */

package org.embergraph.bop.solutions;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.join.JoinAnnotations;
import org.embergraph.relation.accesspath.IBlockingBuffer;

/*
 * Operator projects only the identified variables.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class ProjectionOp extends PipelineOp {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations extends PipelineOp.Annotations {

    /*
     * An {@link IVariable}[] identifying the variables to be retained in the {@link IBindingSet}s
     * written out by the operator.
     */
    String SELECT = JoinAnnotations.SELECT;
  }

  /** @param op */
  public ProjectionOp(final ProjectionOp op) {
    super(op);
  }

  /*
   * @param args
   * @param annotations
   */
  public ProjectionOp(final BOp[] args, final Map<String, Object> annotations) {
    super(args, annotations);
  }

  public ProjectionOp(final BOp[] args, final NV... annotations) {

    this(args, NV.asMap(annotations));
  }

  /** @see Annotations#SELECT */
  public IVariable<?>[] getVariables() {

    return (IVariable<?>[]) getRequiredProperty(Annotations.SELECT);
  }

  @Override
  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<Void>(new ChunkTask(this, context));
  }

  /** Task executing on the node. */
  private static class ChunkTask implements Callable<Void> {

    private final BOpContext<IBindingSet> context;

    /** The projected variables. */
    private final IVariable<?>[] vars;

    ChunkTask(final ProjectionOp op, final BOpContext<IBindingSet> context) {

      this.context = context;

      this.vars = op.getVariables();

      if (vars == null) throw new IllegalArgumentException();

      // @see #946 (Empty PROJECTION causes IllegalArgumentException)
      //            if (vars.length == 0)
      //                throw new IllegalArgumentException();

    }

    @Override
    public Void call() throws Exception {

      final BOpStats stats = context.getStats();

      final ICloseableIterator<IBindingSet[]> itr = context.getSource();

      final IBlockingBuffer<IBindingSet[]> sink = context.getSink();

      try {

        while (itr.hasNext()) {

          final IBindingSet[] a = itr.next();

          stats.chunksIn.increment();
          stats.unitsIn.add(a.length);

          for (int i = 0; i < a.length; i++) {

            a[i] = a[i].copy(vars);
          }

          sink.add(a);
        }

        sink.flush();

        // done.
        return null;

      } finally {

        sink.close();
      }
    }
  }
}
