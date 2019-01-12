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
 * Created on Nov 7, 2011
 */

package org.embergraph.bop.join;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IQueryAttributes;
import org.embergraph.bop.NV;
import org.embergraph.bop.controller.INamedSolutionSetRef;
import org.embergraph.bop.controller.NamedSetAnnotations;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.UnsyncLocalOutputBuffer;

/*
 * An N-way merge join on the Java heap.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class JVMMergeJoin extends AbstractMergeJoin {

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations
      extends AccessPathJoinAnnotations, JVMHashJoinAnnotations, NamedSetAnnotations {

    //        /*
    //         * The {@link NamedSolutionSetRef} used to locate the named solution
    //         * sets. The value stored under the attribute must be an
    //         * {@link IHashJoinUtility}[]. There must be at least 2 entries in the
    //         * array.
    //         *
    //         * @see NamedSolutionSetRef
    //         * @see HTreeNamedSubqueryOp.Annotations#NAMED_SET_REF
    //         */
    //        String NAMED_SET_REF = HTreeNamedSubqueryOp.Annotations.NAMED_SET_REF;

    /*
     * Constraints to be applied by the join (in addition to any associated with the {@link
     * HTreeHashJoinUtility} state in the {@link #NAMED_SET_REF}).
     */
    String CONSTRAINTS = JoinAnnotations.CONSTRAINTS;

    /*
     * Only {@link JoinTypeEnum#Normal} and {@link JoinTypeEnum#Optional} merge joins are supported.
     *
     * @see JoinAnnotations#JOIN_TYPE
     */
    String JOIN_TYPE = JoinAnnotations.JOIN_TYPE;
    //        String OPTIONAL = JoinAnnotations.OPTIONAL;
    //
    //        boolean DEFAULT_OPTIONAL = JoinAnnotations.DEFAULT_OPTIONAL;

    /*
     * When <code>true</code> the hash index identified by {@link #NAMED_SET_REF} will be released
     * when this operator is done (default {@value #DEFAULT_RELEASE}).
     *
     * <p>Note: Whether or not the hash index can be released depends on whether or not the hash
     * index will be consumed by more than one operator in the query plan. For example, a named
     * solution set can be consumed by more than one operator and thus must not be released until
     * all such operators are done.
     *
     * <p>TODO Alternatively, we could specify the #of different locations in the query plan where
     * the named solution set will be consumed. This could be part of the {@link
     * HTreeHashJoinUtility} state, in which case it would only be set as an annotation on the
     * operator which generates the hash index.
     */
    String RELEASE = HTreeSolutionSetHashJoinOp.class + ".release";

    boolean DEFAULT_RELEASE = true;
  }

  /*
   * @param args
   * @param annotations
   */
  public JVMMergeJoin(BOp[] args, Map<String, Object> annotations) {

    super(args, annotations);

    //        final JoinTypeEnum joinType = (JoinTypeEnum)
    // getRequiredProperty(Annotations.JOIN_TYPE);
    //        switch (joinType) {
    //        case Normal:
    //        case Optional:
    //            break;
    //        default:
    //            throw new UnsupportedOperationException(Annotations.JOIN_TYPE + "="
    //                    + joinType);
    //        }

    if (!isLastPassRequested()) {

      /*
       * FIXME I am not convinced that "LAST PASS" evaluation semantics
       * are required here. However, we should not be evaluating this
       * operator more than once.
       */

      throw new UnsupportedOperationException("Requires " + Annotations.LAST_PASS);
    }
  }

  /** @param op */
  public JVMMergeJoin(JVMMergeJoin op) {

    super(op);
  }

  public JVMMergeJoin(final BOp[] args, NV... annotations) {

    this(args, NV.asMap(annotations));
  }

  @Override
  public FutureTask<Void> eval(BOpContext<IBindingSet> context) {

    return new FutureTask<Void>(new ChunkTask<IBindingSet>(context, this));
  }

  /** Task executing on the node. */
  private static class ChunkTask<E> implements Callable<Void> {

    private final BOpContext<IBindingSet> context;

    private final JVMMergeJoin op;

    private final JVMHashJoinUtility[] state;

    private final IConstraint[] constraints;

    //        private final IVariable<?>[] selectVars;

    //        private final boolean optional;

    private final boolean release;

    //        private final BaseJoinStats stats;

    private final IBlockingBuffer<IBindingSet[]> sink;

    private final IBlockingBuffer<IBindingSet[]> sink2;

    public ChunkTask(final BOpContext<IBindingSet> context, final JVMMergeJoin op) {

      this.context = context;

      //            this.stats = (BaseJoinStats) context.getStats();

      //            this.selectVars = (IVariable<?>[]) op
      //                    .getProperty(Annotations.SELECT);

      this.constraints = (IConstraint[]) op.getProperty(Annotations.CONSTRAINTS);

      //            final JoinTypeEnum joinType = (JoinTypeEnum) op
      //                    .getRequiredProperty(Annotations.JOIN_TYPE);
      //
      //            this.optional = joinType.isOptional();
      //
      //            this.optional = op.getProperty(Annotations.OPTIONAL,
      //                    Annotations.DEFAULT_OPTIONAL);

      this.release = op.getProperty(Annotations.RELEASE, Annotations.DEFAULT_RELEASE);

      this.sink = context.getSink();

      this.sink2 = context.getSink2();

      this.op = op;

      // The names of the attributes used to discover the solution sets.
      final INamedSolutionSetRef[] namedSetRef =
          (INamedSolutionSetRef[]) op.getRequiredProperty(Annotations.NAMED_SET_REF);

      state = new JVMHashJoinUtility[namedSetRef.length];

      if (state.length < 2) {

        throw new RuntimeException("Merge join requires at least 2 sources.");
      }

      for (int i = 0; i < state.length; i++) {

        final IQueryAttributes attrs = context.getQueryAttributes(namedSetRef[i].getQueryId());

        state[i] = (JVMHashJoinUtility) attrs.get(namedSetRef[i]);

        if (state[i] == null) {

          /*
           * The solution set was not found!
           *
           * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/534#comment:2">BSBM BI
           *     Q5 Error when using MERGE JOIN </a>
           */
          throw new RuntimeException("Not found: " + namedSetRef[i]);
        }
      }
    }

    public Void call() throws Exception {

      try {

        if (context.isLastInvocation()) {

          final UnsyncLocalOutputBuffer<IBindingSet> unsyncBuffer =
              new UnsyncLocalOutputBuffer<IBindingSet>(op.getChunkCapacity(), sink);

          final IHashJoinUtility[] others = new IHashJoinUtility[state.length - 1];

          for (int i = 1; i < state.length; i++) {

            others[i - 1] = state[i];
          }

          state[0].mergeJoin(
              others, unsyncBuffer, constraints, state[0].getJoinType().isOptional());

          unsyncBuffer.flush();

          sink.flush();
        }

        // Done.
        return null;

      } finally {

        if (release && context.isLastInvocation()) {

          /*
           * Note: It is possible to INCLUDE the named temporary
           * solution set multiple times within a query. If we want to
           * release() the hash tree then we need to know how many
           * times the temporary solution set is being included and
           * decrement a counter each time. When the counter reaches
           * zero, we can release the hash index.
           */

          for (IHashJoinUtility h : state) {

            h.release();
          }
        }

        sink.close();

        if (sink2 != null) sink2.close();
      }
    }
  } // class ChunkTask
}
