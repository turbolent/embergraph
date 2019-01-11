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
 * Created on Oct 11, 2011
 */

package org.embergraph.bop.join;

import java.util.UUID;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpEvaluationContext;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.NamedSolutionSetRefUtility;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.controller.INamedSolutionSetRef;

/**
 * Test suite for {@link HashIndexOp} that uses a {@link JVMHashJoinUtility}
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestJVMHashIndexOp extends HashIndexOpTestCase {

    /**
     * 
     */
    public TestJVMHashIndexOp() {
    }

    /**
     * @param name
     */
    public TestJVMHashIndexOp(String name) {
        super(name);
    }

    @Override
    protected HashIndexOp newHashIndexOp(final String namespace,
            final BOp[] args, final NV... anns) {

        final NV[] anns1 = anns;

        final NV[] anns2 = concat(anns1, new NV[] {
                new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                        JVMHashJoinUtility.factory),
                });

        return new HashIndexOp(args, anns2);

    }
    
    @Override
    protected SolutionSetHashJoinOp newSolutionSetHashJoinOp(final BOp[] args,
            final NV... anns) {

        return new JVMSolutionSetHashJoinOp(args, anns);

    }

    /**
     * Correct rejection tests for the constructor (must run on the controller,
     * parallelism is not allowed since the solutions must be written onto an
     * HTree and that is not thread safe for mutation, join variables may be
     * empty but not null, selected may be null, solution set name must be
     * specified; last pass semantics must be requested, etc).
     */
    public void test_hashIndexOp_ctor() {

        @SuppressWarnings("rawtypes")
        final IVariable[] joinVars = new IVariable[]{Var.var("x")};

        @SuppressWarnings("rawtypes")
        final IVariable[] selected = new IVariable[]{Var.var("y")};
        
        final UUID queryId = UUID.randomUUID();
        
        final String solutionSetName = "set1";
        
        final INamedSolutionSetRef namedSolutionSet = NamedSolutionSetRefUtility
                .newInstance(queryId, solutionSetName, joinVars);

        new HashIndexOp(BOp.NOARGS,//
                new NV(BOp.Annotations.BOP_ID, 1),//
                new NV(BOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                new NV(PipelineOp.Annotations.LAST_PASS, true),//
                new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                new NV(HashIndexOp.Annotations.SELECT, selected),//
                new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                        JVMHashJoinUtility.factory),//
                new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                new NV(IPredicate.Annotations.RELATION_NAME, "kb")
        );

        // Must run on the query controller.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
//                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
//                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalArgumentException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }

        // Parallel evaluation is not permitted since operator writes on HTree.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
//                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalArgumentException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }

        // Last pass evaluation must be requested since operator defers outputs
        // until all inputs have been consumed.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
//                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalArgumentException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }

        // OPTIONAL semantics are supported.
        new HashIndexOp(BOp.NOARGS,//
                new NV(BOp.Annotations.BOP_ID, 1),//
                new NV(BOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                new NV(PipelineOp.Annotations.LAST_PASS, true),//
                new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                new NV(HashIndexOp.Annotations.SELECT, selected),//
                new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                        JVMHashJoinUtility.factory),//
                new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                new NV(IPredicate.Annotations.RELATION_NAME, "kb")
        );

        // Join vars must be specified.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
//                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalStateException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }

        // Join vars may be an empty [].
        new HashIndexOp(BOp.NOARGS,//
                new NV(BOp.Annotations.BOP_ID, 1),//
                new NV(BOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                new NV(PipelineOp.Annotations.LAST_PASS, true),//
                new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                new NV(HashIndexOp.Annotations.JOIN_VARS, new IVariable[] {}),//
                new NV(HashIndexOp.Annotations.SELECT, selected),//
                new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                        JVMHashJoinUtility.factory),//
                new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                new NV(IPredicate.Annotations.RELATION_NAME, "kb")
        );
        
        // The selected variables annotation is optional.
        new HashIndexOp(BOp.NOARGS,//
                new NV(BOp.Annotations.BOP_ID, 1),//
                new NV(BOp.Annotations.EVALUATION_CONTEXT,
                        BOpEvaluationContext.CONTROLLER),//
                new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                new NV(PipelineOp.Annotations.LAST_PASS, true),//
                new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                new NV(HashIndexOp.Annotations.JOIN_VARS, new IVariable[] {}),//
                new NV(HashIndexOp.Annotations.SELECT, null),//
                new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                        JVMHashJoinUtility.factory),//
                new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                new NV(IPredicate.Annotations.RELATION_NAME, "kb")
        );
        
        // The IHashJoinUtility must be specified.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
//                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
//                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet),//
                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalStateException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }
        
        // The solution set name must be specified.
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
//                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet)//
            new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalStateException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }
        
        // The relation name must be specified
        try {
            new HashIndexOp(BOp.NOARGS,//
                    new NV(BOp.Annotations.BOP_ID, 1),//
                    new NV(BOp.Annotations.EVALUATION_CONTEXT,
                            BOpEvaluationContext.CONTROLLER),//
                    new NV(PipelineOp.Annotations.MAX_PARALLEL, 1),//
                    new NV(PipelineOp.Annotations.LAST_PASS, true),//
                    new NV(HashIndexOp.Annotations.JOIN_TYPE, JoinTypeEnum.Normal),//
                    new NV(HashIndexOp.Annotations.JOIN_VARS, joinVars),//
                    new NV(HashIndexOp.Annotations.SELECT, selected),//
                    new NV(HashIndexOp.Annotations.HASH_JOIN_UTILITY_FACTORY,
                            JVMHashJoinUtility.factory),//
                    new NV(HashIndexOp.Annotations.NAMED_SET_REF, namedSolutionSet)//
//                    new NV(IPredicate.Annotations.RELATION_NAME, "kb")
            );
        } catch(IllegalStateException ex) {
            if(log.isInfoEnabled())
                log.info("Ignoring expected exception: "+ex);
        }
        
    }

}
