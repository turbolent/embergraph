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
 * Created on Aug 18, 2010
 */

package org.embergraph.bop.controller;

import cutthecrap.utils.striterators.ICloseableIterator;
import cutthecrap.utils.striterators.SingleValueIterator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IQueryAttributes;
import org.embergraph.bop.ISingleThreadedOp;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.AbstractRunningQuery;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.join.JVMHashJoinAnnotations;
import org.embergraph.bop.join.JVMHashJoinUtility;
import org.embergraph.bop.join.JVMSolutionSetHashJoinOp;
import org.embergraph.bop.join.JoinTypeEnum;
import org.embergraph.bop.join.NamedSolutionSetStats;
import org.embergraph.relation.accesspath.IBlockingBuffer;

/*
* Evaluation of a subquery, producing a named result set. This operator passes through any source
 * binding sets without modification. The subquery is evaluated exactly once, the first time this
 * operator is invoked for a given query plan. If some variables are known to be bound, then they
 * should be rewritten into constants or their bindings should be inserted into the subquery using
 * LET() operator.
 *
 * <p>This operator is NOT thread-safe. It relies on the query engine to provide synchronization for
 * the "run-once" contract of the subquery. The operator MUST be run on the query controller.
 *
 * @see JVMSolutionSetHashJoinOp
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class JVMNamedSubqueryOp extends PipelineOp implements INamedSubqueryOp, ISingleThreadedOp {

  private static final transient Logger log = Logger.getLogger(JVMNamedSubqueryOp.class);

  /** */
  private static final long serialVersionUID = 1L;

  public interface Annotations
      extends SubqueryAnnotations, JVMHashJoinAnnotations, NamedSetAnnotations {}

  /** Deep copy constructor. */
  public JVMNamedSubqueryOp(final JVMNamedSubqueryOp op) {
    super(op);
  }

  /*
   * Shallow copy constructor.
   *
   * @param args
   * @param annotations
   */
  public JVMNamedSubqueryOp(final BOp[] args, final Map<String, Object> annotations) {

    super(args, annotations);

    if (getEvaluationContext() != BOpEvaluationContext.CONTROLLER) {
      throw new IllegalArgumentException(
          BOp.Annotations.EVALUATION_CONTEXT + "=" + getEvaluationContext());
    }

    assertMaxParallelOne();

    if (!isAtOnceEvaluation()) throw new IllegalArgumentException();

    getRequiredProperty(Annotations.SUBQUERY);

    getRequiredProperty(Annotations.NAMED_SET_REF);

    // Join variables must be specified.
    final IVariable<?>[] joinVars = (IVariable[]) getRequiredProperty(Annotations.JOIN_VARS);

    //        if (joinVars.length == 0)
    //            throw new IllegalArgumentException(Annotations.JOIN_VARS);

    for (IVariable<?> var : joinVars) {

      if (var == null) throw new IllegalArgumentException(Annotations.JOIN_VARS);
    }
  }

  public JVMNamedSubqueryOp(final BOp[] args, final NV... annotations) {

    this(args, NV.asMap(annotations));
  }

  @Override
  public BOpStats newStats() {

    return new NamedSolutionSetStats();
  }

  @Override
  public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

    return new FutureTask<Void>(new ControllerTask(this, context));
  }

  /*
   * Evaluates the subquery for each source binding set. If the controller operator is interrupted,
   * then the subqueries are cancelled. If a subquery fails, then all subqueries are cancelled.
   */
  private static class ControllerTask implements Callable<Void> {

    private final BOpContext<IBindingSet> context;

    private final NamedSolutionSetStats stats;

    /** The subquery which is evaluated for each input binding set. */
    private final PipelineOp subquery;

    /** Metadata to identify the named solution set. */
    private final INamedSolutionSetRef namedSetRef;

    /*
     * The {@link IQueryAttributes} for the {@link IRunningQuery} off which we will hang the named
     * solution set.
     */
    private final IQueryAttributes attrs;

    /*
     * <code>true</code> iff this is the first time the task is being invoked, in which case we will
     * evaluate the subquery and save its result set on {@link #solutions}.
     */
    private final boolean first;

    private final JVMHashJoinUtility state;

    public ControllerTask(final JVMNamedSubqueryOp op, final BOpContext<IBindingSet> context) {

      if (op == null) throw new IllegalArgumentException();

      if (context == null) throw new IllegalArgumentException();

      this.context = context;

      this.stats = ((NamedSolutionSetStats) context.getStats());

      this.subquery = (PipelineOp) op.getRequiredProperty(Annotations.SUBQUERY);

      this.namedSetRef = (INamedSolutionSetRef) op.getRequiredProperty(Annotations.NAMED_SET_REF);

      {

      /*
       * First, see if the map already exists.
         *
         * Note: Since the operator is not thread-safe, we do not need
         * to use a putIfAbsent pattern here.
         */

      /*
       * Lookup the attributes for the query on which we will hang the
         * solution set. See BLZG-1493 (if queryId is null, use the query
         * attributes for this running query).
         */
        attrs = context.getQueryAttributes(namedSetRef.getQueryId());

        JVMHashJoinUtility state = (JVMHashJoinUtility) attrs.get(namedSetRef);

        if (state == null) {

        /*
       * Note: This operator does not support optional semantics.
           */
          state = new JVMHashJoinUtility(op, JoinTypeEnum.Normal);

          if (attrs.putIfAbsent(namedSetRef, state) != null) throw new AssertionError();

          this.first = true;

        } else {

          this.first = false;
        }

        this.state = state;
      }
    }

    /** Evaluate. */
    @Override
    public Void call() throws Exception {

      try {

        final IBindingSet[] bindingSets = BOpUtility.toArray(context.getSource(), stats);

        if (first) {

          //                    final IBindingSet tmp;
          //                    if(a.length != 1) {
          //                        // Unbound if more than one source solution (should not happen).
          //                        tmp = new ListBindingSet();
          //                    } else {
          //                        // Only one solution.
          //                        tmp = a[0];
          //                    }

          // Generate the result set and write it on the HTree.
          new SubqueryTask(bindingSets, subquery, context).call();
        }

        // source.
        final Iterator<IBindingSet[]> source = new SingleValueIterator<IBindingSet[]>(bindingSets);

        // default sink
        final IBlockingBuffer<IBindingSet[]> sink = context.getSink();

        BOpUtility.copy(
            source,
            sink,
            null, // sink2
            null, // mergeSolution (aka parent's source solution).
            null, // selectVars (aka projection).
            null, // constraints
            null // stats were updated above.
            //                        context.getStats()
            );

        sink.flush();

        // Done.
        return null;

      } finally {

        context.getSource().close();

        context.getSink().close();

        if (context.getSink2() != null) context.getSink2().close();
      }
    }

    /** Run a subquery. */
    private class SubqueryTask implements Callable<Void> {

      /** The evaluation context for the parent query. */
      private final BOpContext<IBindingSet> parentContext;

      /** The source binding sets. */
      private final IBindingSet[] bindingSets;

      /** The root operator for the subquery. */
      private final BOp subQueryOp;

      public SubqueryTask(
          final IBindingSet[] bindingSets,
          final BOp subQuery,
          final BOpContext<IBindingSet> parentContext) {

        this.bindingSets = bindingSets;

        this.subQueryOp = subQuery;

        this.parentContext = parentContext;
      }

      @Override
      public Void call() throws Exception {

        // The subquery
        IRunningQuery runningSubquery = null;
        // The iterator draining the subquery
        ICloseableIterator<IBindingSet[]> subquerySolutionItr = null;
        try {

          final QueryEngine queryEngine = parentContext.getRunningQuery().getQueryEngine();

          runningSubquery = queryEngine.eval(subQueryOp, bindingSets);

          try {

            // Declare the child query to the parent.
            ((AbstractRunningQuery) parentContext.getRunningQuery()).addChild(runningSubquery);

            // Iterator visiting the subquery solutions.
            subquerySolutionItr = runningSubquery.iterator();

            // Buffer the solutions on the hash index.
            final long ncopied = state.acceptSolutions(subquerySolutionItr, stats);

            // Wait for the subquery to halt / test for errors.
            runningSubquery.get();

            // Report the #of solutions in the named solution set.
            stats.solutionSetSize.add(ncopied);

            //                        // Publish the solution set on the query context.
            //                        saveSolutionSet();

            if (log.isInfoEnabled())
              log.info("Solution set " + namedSetRef + " has " + ncopied + " solutions.");

          } catch (InterruptedException ex) {

            // this thread was interrupted, so cancel the subquery.
            runningSubquery.cancel(true /* mayInterruptIfRunning */);

            // rethrow the exception.
            throw ex;
          }

        } catch (Throwable t) {

          if (runningSubquery == null || runningSubquery.getCause() != null) {
          /*
       * If things fail before we start the subquery, or if a
             * subquery fails (due to abnormal termination), then
             * propagate the error to the parent and rethrow the
             * first cause error out of the subquery.
             *
             * Note: IHaltable#getCause() considers exceptions
             * triggered by an interrupt to be normal termination.
             * Such exceptions are NOT propagated here and WILL NOT
             * cause the parent query to terminate.
             */
            throw new RuntimeException(
                ControllerTask.this
                    .context
                    .getRunningQuery()
                    .halt(runningSubquery == null ? t : runningSubquery.getCause()));
          }

        } finally {

          try {

            // ensure subquery is halted.
            if (runningSubquery != null) runningSubquery.cancel(true /* mayInterruptIfRunning */);

          } finally {

            // ensure the subquery solution iterator is closed.
            if (subquerySolutionItr != null) subquerySolutionItr.close();
          }
        }

        // Done.
        return null;
      }
    } // SubqueryTask
  } // ControllerTask
}
