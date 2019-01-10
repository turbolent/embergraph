/*

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
 * Created on Aug 19, 2008
 */

package org.embergraph.bop.joinGraph.fast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase2;

import com.bigdata.bop.Constant;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.IConstant;
import com.bigdata.bop.IConstraint;
import com.bigdata.bop.IPredicate;
import com.bigdata.bop.IPredicate.Annotations;
import com.bigdata.bop.IVariableOrConstant;
import com.bigdata.bop.NV;
import com.bigdata.bop.Var;
import com.bigdata.bop.ap.Predicate;
import com.bigdata.bop.joinGraph.IEvaluationPlan;
import com.bigdata.bop.joinGraph.IEvaluationPlanFactory;
import com.bigdata.bop.joinGraph.IRangeCountFactory;
import com.bigdata.btree.keys.ISortKeyBuilder;
import com.bigdata.config.IValidator;
import com.bigdata.io.IStreamSerializer;
import com.bigdata.journal.IIndexManager;
import com.bigdata.journal.ITx;
import com.bigdata.mdi.PartitionLocator;
import com.bigdata.relation.IMutableRelation;
import com.bigdata.relation.IRelation;
import com.bigdata.relation.accesspath.IAccessPath;
import com.bigdata.relation.accesspath.IBlockingBuffer;
import com.bigdata.relation.accesspath.IBuffer;
import com.bigdata.relation.accesspath.IElementFilter;
import com.bigdata.relation.rule.IAccessPathExpander;
import com.bigdata.relation.rule.IRule;
import com.bigdata.relation.rule.IStep;
import com.bigdata.relation.rule.Rule;
import com.bigdata.relation.rule.eval.ActionEnum;
import com.bigdata.relation.rule.eval.IJoinNexus;
import com.bigdata.relation.rule.eval.IJoinNexusFactory;
import com.bigdata.relation.rule.eval.IRuleStatisticsFactory;
import com.bigdata.relation.rule.eval.IRuleTaskFactory;
import com.bigdata.relation.rule.eval.ISolution;
import com.bigdata.service.AbstractScaleOutFederation;
import com.bigdata.striterator.IChunkedOrderedIterator;

/**
 * Test harness for {@link DefaultEvaluationPlan2}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestDefaultEvaluationPlan extends TestCase2 {

    /**
     * 
     */
    public TestDefaultEvaluationPlan() {
    }

    /**
     * @param arg0
     */
    public TestDefaultEvaluationPlan(String arg0) {
        super(arg0);
    }

    /**
     * Factory fixture for {@link IEvaluationPlan}s.
     * 
     * @param joinNexus
     *            Per {@link IEvaluationPlanFactory}
     * @param rule
     *            {@link IEvaluationPlanFactory}
     */
    final IEvaluationPlan newPlan(IJoinNexus joinNexus, final IRule rule) {
            
//        return new DefaultEvaluationPlan(joinNexus,rule);
            
           return new DefaultEvaluationPlan2(joinNexus, rule);
        
    };
    
    /**
     * Based on LUBM query#8 with the U1 dataset.
     */
    public void test_lubmQuery8() {

        final String relation = "spo";
        final long timestamp = ITx.READ_COMMITTED;
        
        final Constant<?> rdfType = new Constant<String>("rdfType");
        final Constant<?> Department = new Constant<String>("Department");
        final Constant<?> Student = new Constant<String>("Student");
        final Constant<?> memberOf = new Constant<String>("memberOf");
        final Constant<?> subOrganizationOf = new Constant<String>("subOrganizationOf");
        final Constant<?> emailAddress = new Constant<String>("emailAddress");
        final Constant<?> University0 = new Constant<String>("University0");

        final IPredicate<?> pred0 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("y"), rdfType, Department },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );

        final IPredicate<?> pred1 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("x"), rdfType, Student },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IPredicate<?> pred2 = new Predicate( //
                new IVariableOrConstant[] {//
                Var.var("x"), memberOf, Var.var("y") },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IPredicate<?> pred3 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("y"), subOrganizationOf, University0 },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IPredicate<?> pred4 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("x"), emailAddress, Var.var("z") },
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IRule rule = new Rule(getName(), null/* head */,
                new IPredicate[] { pred0, pred1, pred2, pred3, pred4 }, //
                null// constraints
        );
        
        /*
         * Range counts with the predicates as given based on the LUBM U1
         * dataset.
         * 
         * rangeCount=15, tailIndex=0, tail=([kb.spo.], y, 8, 204)
         * rangeCount=6463, tailIndex=1, tail=([kb.spo.], x, 8, 368)
         * rangeCount=8330, tailIndex=2, tail=([kb.spo.], x, 276, y)
         * rangeCount=15, tailIndex=3, tail=([kb.spo.], y, 372, 6148)
         * rangeCount=8330, tailIndex=4, tail=([kb.spo.], x, 216, z)
         */
//        final long[] rangeCount = { 15, 6463, 8830, 15, 8330 };
        final Map<IPredicate,Long> rangeCount = new HashMap<IPredicate,Long>();
        {
            rangeCount.put(pred0, 15L);
            rangeCount.put(pred1, 6463L);
            rangeCount.put(pred2, 8830L);
            rangeCount.put(pred3, 15L);
            rangeCount.put(pred4, 8830L);
        }
        
        final IEvaluationPlan plan = newPlan(new MockJoinNexus(
                new MockRangeCountFactory(rangeCount)), rule);
        
        assertFalse(plan.isEmpty());
        
        final int[] expected = new int[] { 0, 3, 2, 1, 4 };
        
        final int[] actual = plan.getOrder();
        
        if (!Arrays.equals(expected, actual))
            fail("evaluation order: expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        
//        assertFalse("isFullyBound(0)", plan.isFullyBound(0));
//        assertTrue( "isFullyBound(1)", plan.isFullyBound(1));
//        assertFalse("isFullyBound(2)", plan.isFullyBound(2));
//        assertTrue( "isFullyBound(3)", plan.isFullyBound(3));
//        assertFalse("isFullyBound(4)", plan.isFullyBound(4));
//
//        assertEquals("getVarCount(0)", 1, plan.getVariableCount(0));
//        assertEquals("getVarCount(1)", 0, plan.getVariableCount(1));
//        assertEquals("getVarCount(2)", 1, plan.getVariableCount(2));
//        assertEquals("getVarCount(3)", 0, plan.getVariableCount(3));
//        assertEquals("getVarCount(4)", 1, plan.getVariableCount(4));
        
    }

//    public void test_rdf01() {
//
//        fail("write test");
//        
//    }
    
//    /**
//     * FIXME test all of these rules with some hard coded range counts based on
//     * some fake data and make sure that we are producing good join orderings in
//     * each case. Note that some data sets will not have anything for some of
//     * these rules. In those cases, we should verify that the plan detects that
//     * there will be no solutions AND also find a data set that can be used to
//     * verify a plan that does have solutions for the rule.
//     * <p>
//     * RuleRdf01.java RuleRdfs03.java RuleRdfs04a.java RuleRdfs04b.java
//     * RuleRdfs05.java RuleRdfs06.java RuleRdfs07.java RuleRdfs08.java
//     * RuleRdfs09.java RuleRdfs10.java RuleRdfs11.java RuleRdfs12.java
//     * RuleRdfs13.java
//     * <p>
//     * RuleFastClosure11.java RuleFastClosure13.java RuleFastClosure3.java
//     * RuleFastClosure5.java RuleFastClosure6.java RuleFastClosure7.java
//     * RuleFastClosure9.java
//     * <P>
//     * RuleOwlEquivalentClass.java RuleOwlEquivalentProperty.java
//     * RuleOwlSameAs1.java RuleOwlSameAs1b.java RuleOwlSameAs2.java
//     * RuleOwlSameAs3.java
//     */
//    public void test_rdfs02() {
//
//        fail("write test");
//        
//    }
       
    
    public void testRunFirstExpanders() {

        final String relation = "spo";
        final long timestamp = ITx.READ_COMMITTED;
        
        /*
         * ?l search Mike
         * ?s ?p ?l
         * ?s type Person
         * ?p type Property
         */
        
        final Constant<?> search = new Constant<String>("search");
        final Constant<?> Mike = new Constant<String>("Mike");
        final Constant<?> type = new Constant<String>("type");
        final Constant<?> Person = new Constant<String>("Person");
        final Constant<?> Property = new Constant<String>("Property");

        final IAccessPathExpander expander = new IAccessPathExpander() {

//			@Override
			public IAccessPath getAccessPath(IAccessPath accessPath) {
				throw new UnsupportedOperationException();
			}

//			@Override
			public boolean backchain() {
				return false;
			}

//			@Override
			public boolean runFirst() {
				return true;
			}
        	
        };
        
        final IPredicate<?> pred0 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("l"), search, Mike },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp),//
                new NV(Predicate.Annotations.ACCESS_PATH_EXPANDER, expander)
                );

        final IPredicate<?> pred1 = new Predicate(//
                new IVariableOrConstant[] {//
                Var.var("s"), Var.var("p"), Var.var("l") },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IPredicate<?> pred2 = new Predicate( //
                new IVariableOrConstant[] {//
                Var.var("s"), type, Person },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IPredicate<?> pred3 = new Predicate( //
                new IVariableOrConstant[] {//
                Var.var("p"), type, Property },//
                new NV(Predicate.Annotations.RELATION_NAME,
                        new String[] { relation }),//
                new NV(Annotations.TIMESTAMP,timestamp)//
                );
        
        final IRule rule = new Rule(getName(), null/* head */,
                new IPredicate[] { pred0, pred1, pred2, pred3 }, //
                null// constraints
        );
        
        final Map<IPredicate,Long> rangeCount = new HashMap<IPredicate,Long>();
        {
            rangeCount.put(pred0, 0L);
            rangeCount.put(pred1, 1000000L);
            rangeCount.put(pred2, 10000L);
            rangeCount.put(pred3, 100L);
        }
        
        final IEvaluationPlan plan = newPlan(new MockJoinNexus(
                new MockRangeCountFactory(rangeCount)), rule);
        
        assertFalse(plan.isEmpty());
        
        final int[] expected = new int[] { 0, 1, 3, 2 };
        
        final int[] actual = plan.getOrder();
        
        if (!Arrays.equals(expected, actual))
            fail("evaluation order: expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        
//        assertFalse("isFullyBound(0)", plan.isFullyBound(0));
//        assertTrue( "isFullyBound(1)", plan.isFullyBound(1));
//        assertFalse("isFullyBound(2)", plan.isFullyBound(2));
//        assertTrue( "isFullyBound(3)", plan.isFullyBound(3));
//        assertFalse("isFullyBound(4)", plan.isFullyBound(4));
//
//        assertEquals("getVarCount(0)", 1, plan.getVariableCount(0));
//        assertEquals("getVarCount(1)", 0, plan.getVariableCount(1));
//        assertEquals("getVarCount(2)", 1, plan.getVariableCount(2));
//        assertEquals("getVarCount(3)", 0, plan.getVariableCount(3));
//        assertEquals("getVarCount(4)", 1, plan.getVariableCount(4));
        
    }


    
    /**
     * Mock object.
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    private static class MockJoinNexus implements IJoinNexus {

        private final IRangeCountFactory rangeCountFactory;
        
        public MockJoinNexus(IRangeCountFactory rangeCountFactory) {
            
            this.rangeCountFactory = rangeCountFactory;
            
        }
        
        public boolean bind(final IRule rule, final int index, final Object e,
                final IBindingSet bindings) {
       
            return false;
            
        }
       
        public boolean bind(IPredicate<?> pred, IConstraint constraint[],
                Object e, IBindingSet bindings) {

            return false;
            
        }

//        public void copyValues(Object e, IPredicate predicate, IBindingSet bindingSet) {
//
//        }

        public boolean forceSerialExecution() {

            return false;
        }

        public ActionEnum getAction() {

            return null;
        }

        public IRelation getHeadRelationView(IPredicate pred) {

            return null;
        }

        public IIndexManager getIndexManager() {

            return null;
        }

        public IJoinNexusFactory getJoinNexusFactory() {

            return null;
        }

        public IEvaluationPlanFactory getPlanFactory() {

            return null;
        }

        public long getReadTimestamp(/*String relationName*/) {

            return 0;
        }

        public IRuleTaskFactory getRuleTaskFactory(boolean parallel, IRule rule) {

            return null;
        }

        public IAccessPath getTailAccessPath(IPredicate pred) {

            return null;
        }

        public IAccessPath getTailAccessPath(IRelation rel, IPredicate pred) {

            return null;
        }

        public IRelation getTailRelationView(IPredicate pred) {

            return null;
        }

        public long getWriteTimestamp() {

            return 0;
        }

        public IBindingSet newBindingSet(IRule rule) {

            return null;
        }

        public IBuffer<ISolution[]> newDeleteBuffer(IMutableRelation relation) {

            return null;
        }

        public IBuffer<ISolution[]> newInsertBuffer(IMutableRelation relation) {

            return null;
        }

        public IBlockingBuffer<ISolution[]> newQueryBuffer() {

            return null;
        }

        public ISolution newSolution(IRule rule, IBindingSet bindingSet) {

            return null;
        }

        public long runMutation(IStep step) throws Exception {

            return 0;
        }

        public IChunkedOrderedIterator<ISolution> runQuery(IStep step) throws Exception {

            return null;
        }

        public int solutionFlags() {

            return 0;
        }

        public IRangeCountFactory getRangeCountFactory() {
            
            return rangeCountFactory;
            
        }

        public IRuleStatisticsFactory getRuleStatisticsFactory() {
            return null;
        }

        public IConstant fakeBinding(IPredicate predicate, Var var) {
            return null;
        }

        public int getMaxParallelSubqueries() {
            return 0;
        }

        public int getChunkOfChunksCapacity() {
            return 0;
        }

        public int getFullyBufferedReadThreshold() {
            return 0;
        }

        public int getChunkCapacity() {
            return 0;
        }

        public IElementFilter<ISolution> getSolutionFilter() {
            return null;
        }

        public IBuffer<ISolution> newUnsynchronizedBuffer(IBuffer<ISolution[]> targetBuffer, int chunkCapacity) {
            return null;
        }

        public IStreamSerializer<ISolution[]> getSolutionSerializer() {
            return null;
        }

        public ISortKeyBuilder<IBindingSet> newBindingSetSortKeyBuilder(IRule rule) {
            return null;
        }

        public Iterator<PartitionLocator> locatorScan(AbstractScaleOutFederation fed, IPredicate predicate) {
            // TODO Auto-generated method stub
            return null;
        }

        public IStreamSerializer<IBindingSet[]> getBindingSetSerializer() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getProperty(String name, String defaultValue) {
            // TODO Auto-generated method stub
            return null;
        }

        public <T> T getProperty(String name, String defaultValue,
                IValidator<T> validator) {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private static class MockRangeCountFactory implements IRangeCountFactory {

        private final Map<IPredicate, Long> rangeCount;

        public MockRangeCountFactory(Map<IPredicate, Long> rangeCount) {

            this.rangeCount = rangeCount;

        }

        public long rangeCount(IPredicate pred) {

            Long rangeCount = this.rangeCount.get(pred);

            if (rangeCount == null)
                throw new IllegalArgumentException();

            if (log.isInfoEnabled())
                log.info("rangeCount=" + rangeCount + ", pred=" + pred);
            
            return rangeCount.longValue();
            
        }
        
    }
    
}
