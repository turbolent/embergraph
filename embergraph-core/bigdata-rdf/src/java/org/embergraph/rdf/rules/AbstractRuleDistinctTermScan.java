/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Oct 29, 2007
 */

package org.embergraph.rdf.rules;

import java.io.Serializable;

import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.IVariable;
import com.bigdata.bop.bindingSet.ListBindingSet;
import com.bigdata.rdf.internal.IV;
import com.bigdata.rdf.spo.SPOKeyOrder;
import com.bigdata.rdf.spo.SPOPredicate;
import com.bigdata.rdf.spo.SPORelation;
import com.bigdata.relation.accesspath.IBuffer;
import com.bigdata.relation.rule.IRule;
import com.bigdata.relation.rule.Rule;
import com.bigdata.relation.rule.eval.IJoinNexus;
import com.bigdata.relation.rule.eval.IRuleTaskFactory;
import com.bigdata.relation.rule.eval.ISolution;
import com.bigdata.relation.rule.eval.IStepTask;
import com.bigdata.relation.rule.eval.RuleStats;
import com.bigdata.striterator.IChunkedIterator;

/**
 * Base class for rules having a single predicate that is none bound in the tail
 * and a single variable in the head. These rules can be evaluated using a
 * distinctTermScan rather than a full index scan. For example:
 * 
 * <pre>
 *  rdf1:   (?u ?a ?y) -&gt; (?a rdf:type rdf:Property)
 *  rdfs4a: (?u ?a ?x) -&gt; (?u rdf:type rdfs:Resource)
 *  rdfs4b: (?u ?a ?v) -&gt; (?v rdf:type rdfs:Resource)
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: AbstractRuleDistinctTermScan.java 3448 2010-08-18 20:55:58Z
 *          thompsonbry $
 */
abstract public class AbstractRuleDistinctTermScan extends Rule {
    
    /**
     * The sole unbound variable in the head of the rule.
     */
    private final IVariable<IV> h;

    /**
     * The access path that corresponds to the position of the unbound variable
     * reference from the head.
     */
    private final SPOKeyOrder keyOrder;
    
    private final IRuleTaskFactory taskFactory;

    public AbstractRuleDistinctTermScan(String name, SPOPredicate head,
            SPOPredicate[] body, IConstraint[] constraints) {

        super(name, head, body, constraints);
        
        // head must be one unbound; that variable will be bound by the scan. 
        assert head.getVariableCount() == 1;

        // tail must have one predicate.
        assert body.length == 1;
        
        // the predicate in the tail must be "none" bound.
//        assert body[0].getVariableCount() == IRawTripleStore.N;
        assert body[0].getVariableCount() == body[0].arity();//head.arity();

        // figure out which position in the head is the variable.
        if(head.s().isVar()) {
            
            h = (IVariable<IV>)head.s();
            
        } else if( head.p().isVar() ) {
            
            h = (IVariable<IV>)head.p();
            
        } else if( head.o().isVar() ) {
        
            h = (IVariable<IV>)head.o();
        
        } else {
            
            throw new AssertionError();
            
        }

        /*
         * figure out which access path we need for the distinct term scan which
         * will bind the variable in the head.
         */
        if (body[0].s() == h) {

            keyOrder = SPOKeyOrder.SPO;

        } else if (body[0].p() == h) {

            keyOrder = SPOKeyOrder.POS;

        } else if (body[0].o() == h) {

            keyOrder = SPOKeyOrder.OSP;

        } else {

            throw new AssertionError();
            
        }

        taskFactory = new DistinctTermScanRuleTaskFactory(h, keyOrder);        
    }
    
    /**
     * Factory for custom evaluation of the distinct term scan rule.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    private static class DistinctTermScanRuleTaskFactory implements
            IRuleTaskFactory {

        /**
         * 
         */
        private static final long serialVersionUID = 3290328271137950004L;

        /**
         * The sole unbound variable in the head of the rule.
         */
        private final IVariable<IV> h;
        
        /**
         * The access path that corresponds to the position of the unbound
         * variable reference from the head.
         */
        private final SPOKeyOrder keyOrder;

        public DistinctTermScanRuleTaskFactory(IVariable<IV> h,
                SPOKeyOrder keyOrder) {

            this.h = h;

            this.keyOrder = keyOrder;
            
        }
        
        public IStepTask newTask(IRule rule, IJoinNexus joinNexus,
                IBuffer<ISolution[]> buffer) {

            return new DistinctTermScan(rule, joinNexus, buffer, h,
                    keyOrder);

        }
        
    }
    
    public IRuleTaskFactory getTaskFactory() {
        
        return taskFactory;
        
    }
    
    /**
     * Selects the distinct term identifiers, substituting their binding in the
     * sole unbound variable in the head of the rule.
     */
    protected static class DistinctTermScan implements IStepTask, Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = -7570511260700545025L;
        
        private final IJoinNexus joinNexus;
        private final IBuffer<ISolution[]> buffer;
        
        private final IRule rule;
        
        /**
         * The sole unbound variable in the head of the rule.
         */
        private final IVariable<IV> h;

        /**
         * The access path that corresponds to the position of the unbound variable
         * reference from the head.
         */
        private final SPOKeyOrder keyOrder;
        
        /**
         * 
         * @param rule
         *            The rule (may have been specialized).
         * @param joinNexus
         * @param buffer
         *            The buffer on which the {@link ISolution}s will be
         *            written.
         * @param h
         *            The sole unbound variable in the head of the rule.
         * @param keyOrder
         *            The access path that corresponds to the position of the
         *            unbound variable reference from the head.
         */
        public DistinctTermScan(final IRule rule, final IJoinNexus joinNexus,
                final IBuffer<ISolution[]> buffer, final IVariable<IV> h,
                final SPOKeyOrder keyOrder) {
        
            if (rule == null)
                throw new IllegalArgumentException();

            if (joinNexus == null)
                throw new IllegalArgumentException();

            if (buffer == null)
                throw new IllegalArgumentException();

            if (h == null)
                throw new IllegalArgumentException();
            
            if (keyOrder == null)
                throw new IllegalArgumentException();
            
            this.rule = rule;
            
            this.joinNexus = joinNexus;
            
            this.buffer = buffer;
            
            this.h = h;
            
            this.keyOrder = keyOrder;
            
        }
        
        public RuleStats call() {

            final long computeStart = System.currentTimeMillis();

            /*
             * Note: Since this task is always applied to a single tail rule,
             * the {@link TMUtility} rewrite of the rule will always read from
             * the focusStore alone. This makes the choice of the relation on
             * which to read easy - just read on whichever relation is specified
             * for tail[0].
             */
//            final String relationName = rule.getHead().getOnlyRelationName();
//
//            /*
//             * find the distinct predicates in the KB (efficient op).
//             */
//            final long timestamp = joinNexus.getReadTimestamp(relationName);
//
//            final SPORelation relation = (SPORelation) joinNexus
//                    .getIndexManager().getResourceLocator().locate(
//                            relationName, timestamp);

            final SPORelation relation = (SPORelation) joinNexus
                    .getTailRelationView(rule.getTail(0));
            
//            final SPOAccessPath accessPath = relation.getAccessPath(
//                    keyOrder, rule.getTail(0));
            
//            IAccessPath accessPath = state.focusStore == null ? state.database
//                    .getAccessPath(keyOrder) : state.focusStore
//                    .getAccessPath(keyOrder);

            final RuleStats ruleStats = joinNexus.getRuleStatisticsFactory().newInstance(rule);
            
            // there is only a single unbound variable for this rule.
            final IBindingSet bindingSet = new ListBindingSet(/*1 capacity*/);

            final IChunkedIterator<IV> itr = relation.distinctTermScan(keyOrder); 
            
            try {

                while (itr.hasNext()) {

                    // Note: chunks are in ascending order since using scan on SPO index.
                    final IV[] chunk = itr.nextChunk();

                    ruleStats.chunkCount[0]++;

                    ruleStats.elementCount[0] += chunk.length;

                    final IBuffer<ISolution> tmp = joinNexus
                            .newUnsynchronizedBuffer(buffer, chunk.length);
                    
                    for (IV iv : chunk) {

                        // [id] is a distinct term identifier for the selected
                        // access path.

                        /*
                         * bind the unbound variable in the head of the rule.
                         * 
                         * Note: This explicitly leaves the other variables in
                         * the head unbound so that the justifications will be
                         * wildcards for those variables.
                         */

                        bindingSet.set(h, new Constant<IV>(iv));

                        if (rule.isConsistent(bindingSet)) {

                            tmp.add(joinNexus.newSolution(rule, bindingSet));

                            ruleStats.solutionCount.incrementAndGet();

                        }

                    }
                    
                    // flush results onto the chunked solution buffer.
                    tmp.flush();
                    
                }

            } finally {

                itr.close();

                ruleStats.elapsed += System.currentTimeMillis() - computeStart;

            }

            return ruleStats;
            
        }

    }

}
