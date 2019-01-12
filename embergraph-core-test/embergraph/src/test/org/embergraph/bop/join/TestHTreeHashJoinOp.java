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

package org.embergraph.bop.join;

import java.util.Map;
import java.util.UUID;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IPredicate.Annotations;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.NV;
import org.embergraph.bop.NamedSolutionSetRefUtility;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.controller.NamedSetAnnotations;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.internal.IV;
import org.embergraph.util.Bytes;

/**
 * Unit tests for the {@link HTreeHashJoinOp} operator.
 *
 * <p>Note: The logic to map binding sets over shards is tested independently.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
@SuppressWarnings("rawtypes")
public class TestHTreeHashJoinOp extends AbstractHashJoinOpTestCase {

  /** */
  public TestHTreeHashJoinOp() {}

  /** @param name */
  public TestHTreeHashJoinOp(String name) {
    super(name);
  }

  @Override
  protected PipelineOp newJoin(
      final BOp[] args,
      final int joinId,
      final IVariable<IV>[] joinVars,
      final Predicate<IV> predOp,
      final UUID queryId,
      final NV... annotations) {

    final Map<String, Object> tmp =
        NV.asMap(
            new NV(BOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
            new NV(Predicate.Annotations.BOP_ID, joinId),
            new NV(PipelineJoin.Annotations.PREDICATE, predOp),
            new NV(
                HTreeHashJoinAnnotations.RELATION_NAME,
                predOp.getRequiredProperty(Predicate.Annotations.RELATION_NAME)),
            new NV(HashJoinAnnotations.JOIN_VARS, joinVars),
            new NV(PipelineOp.Annotations.LAST_PASS, true),
            new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
            new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
            new NV(
                NamedSetAnnotations.NAMED_SET_REF,
                NamedSolutionSetRefUtility.newInstance(queryId, getName(), joinVars)));

    if (predOp.isOptional()) {
      // Note: memory can not be constrained since predicate is optional.
      tmp.put(PipelineOp.Annotations.MAX_MEMORY, Long.MAX_VALUE);
    }

    if (annotations != null) {

      for (NV nv : annotations) {

        tmp.put(nv.getName(), nv.getValue());
      }
    }

    final PipelineOp joinOp = new HTreeHashJoinOp<IV>(args, tmp);

    return joinOp;
  }

  /** Correct rejection tests for the constructor. */
  public void test_correctRejection() {

    final BOp[] emptyArgs = new BOp[] {};
    final int joinId = 1;
    final int predId = 2;
    @SuppressWarnings("unchecked")
    final IVariable<IV> x = Var.var("x");

    final UUID queryId = UUID.randomUUID();

    final IVariable[] joinVars = new IVariable[] {x};

    final NV namedSet =
        new NV(
            NamedSetAnnotations.NAMED_SET_REF,
            NamedSolutionSetRefUtility.newInstance(queryId, getName(), joinVars));

    final Predicate<IV> pred =
        new Predicate<IV>(
            new IVariableOrConstant[] {new Constant<String>("Mary"), Var.var("x")},
            NV.asMap(
                new NV[] {
                  new NV(Predicate.Annotations.RELATION_NAME, new String[] {setup.spoNamespace}),
                  new NV(Predicate.Annotations.BOP_ID, predId),
                  new NV(Annotations.TIMESTAMP, ITx.READ_COMMITTED),
                }));

    // w/o variables.
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                //                            new NV(HashJoinAnnotations.JOIN_VARS,new
                // IVariable[]{x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // bad evaluation context.
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.ANY),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + UnsupportedOperationException.class);
    } catch (UnsupportedOperationException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // missing predicate.
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.SHARDED),
                //                            new NV(AccessPathJoinAnnotations.PREDICATE,pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // last pass evaluation not requested.
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.SHARDED),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, false),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // maxParallel not set to ONE (1).
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.SHARDED),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 2),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + IllegalArgumentException.class);
    } catch (IllegalArgumentException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // maxMemory not overridden.
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.SHARDED),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                //                            new NV(PipelineOp.Annotations.MAX_MEMORY,
                //                                    Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + UnsupportedOperationException.class);
    } catch (UnsupportedOperationException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // maxMemory != MAX_LONG and predicate is OPTIONAL
    try {
      @SuppressWarnings("unchecked")
      final Predicate<IV> pred2 =
          (Predicate<IV>) pred.setProperty(IPredicate.Annotations.OPTIONAL, true);
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.SHARDED),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred2),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                namedSet,
              }));
      fail("Expecting: " + UnsupportedOperationException.class);
    } catch (UnsupportedOperationException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }

    // w/o named set
    try {
      new HTreeHashJoinOp<IV>(
          emptyArgs,
          NV.asMap(
              new NV[] {
                new NV(BOp.Annotations.BOP_ID, joinId),
                new NV(HashJoinAnnotations.JOIN_VARS, new IVariable[] {x}),
                new NV(PipelineOp.Annotations.EVALUATION_CONTEXT, BOpEvaluationContext.CONTROLLER),
                new NV(AccessPathJoinAnnotations.PREDICATE, pred),
                new NV(PipelineOp.Annotations.LAST_PASS, true),
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),
                new NV(PipelineOp.Annotations.MAX_MEMORY, Bytes.megabyte),
                //                            namedSet,
              }));
      fail("Expecting: " + IllegalStateException.class);
    } catch (IllegalStateException ex) {
      if (log.isInfoEnabled()) log.info("Ignoring expected exception: " + ex);
    }
  }
}
