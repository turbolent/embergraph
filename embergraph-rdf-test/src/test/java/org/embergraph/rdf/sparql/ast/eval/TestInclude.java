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
 * Created on Sep 4, 2011
 */

package org.embergraph.rdf.sparql.ast.eval;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.bop.join.HashIndexOp;
import org.embergraph.bop.join.NestedLoopJoinOp;
import org.embergraph.bop.join.PipelineJoin;
import org.embergraph.bop.join.SolutionSetHashJoinOp;
import org.embergraph.bop.rdf.join.ChunkedMaterializationOp;
import org.embergraph.bop.solutions.ProjectionOp;
import org.embergraph.bop.solutions.SliceOp;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.IBTreeManager;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.BigdataLiteral;
import org.embergraph.rdf.model.BigdataURI;
import org.embergraph.rdf.model.BigdataValue;
import org.embergraph.rdf.model.BigdataValueFactory;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.NamedSubqueryRoot;
import org.embergraph.rdf.sparql.ast.eval.TestTCK.TCKStressTests;
import org.embergraph.rdf.sparql.ast.ssets.ISolutionSetManager;
import org.embergraph.rdf.sparql.ast.ssets.SolutionSetManager;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rwstore.IRWStrategy;
import org.embergraph.rwstore.sector.MemStore;

/**
 * Data driven test suite for INCLUDE of named solution sets NOT generated by a
 * {@link NamedSubqueryRoot}.  This test suite is examines several details,
 * including the ability to locate and join with a pre-existing named solution
 * set, the ability to deliver the named solution set in order
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestNamedSubQuery.java 6080 2012-03-07 18:38:55Z thompsonbry $
 */
public class TestInclude extends AbstractDataDrivenSPARQLTestCase {

    /**
     *
     */
    public TestInclude() {
    }

    /**
     * @param name
     */
    public TestInclude(String name) {
        super(name);
    }
    
    /**
     * Overridden to force the use of the {@link MemStore} since the solution
     * set cache is only enabled for {@link IRWStrategy} instances.
     */
    @Override
    public Properties getProperties() {

        // Note: clone to avoid modifying!!!
        final Properties properties = (Properties) super.getProperties().clone();

        properties.setProperty(org.embergraph.journal.Options.BUFFER_MODE,
                BufferMode.MemStore.name());
        
        return properties;
        
    }

    protected <T> IConstant<T> asConst(final T val) {

        return new Constant<T>(val);
        
    }
    
    /**
	 * This test populates a named solution set and then examines the ability to
	 * deliver a SLICE of that named solution set in the same order in which the
	 * data were stored. Normally, the named solution set would be created using
	 * <code>INSERT INTO SOLUTIONS</code>, but in this case we drop down a level
	 * and handle the creation of the named solution set in the test setup.
	 * 
	 * <pre>
	 * SELECT ?x ?y WHERE { 
	 * 
	 *    # Turn off the join order optimizer.
	 *    hint:Query hint:optimizer "None" .
	 * 
	 *    # Run joins in the given order (INCLUDE is 2nd).
	 *    
	 *    # bind x => {Mike;Bryan}
	 *    ?x rdf:type foaf:Person .
	 *    
	 *    # join on (x) => {(x=Mike,y=2);(x=Bryan;y=4)} 
	 *    INCLUDE %solutionSet1 .
	 * 
	 * }
	 * </pre>
	 */
    public void test_include_02() throws Exception {

        final TestHelper testHelper = new TestHelper(
        		"include_02",// name
        		"include_02.rq",// query URL
        		"include_02.trig",// data URL
        		"include_02.srx",// results URL
        		true  // check order(!)
        		);

        final AbstractTripleStore tripleStore = testHelper.getTripleStore();
        
        final BigdataValueFactory vf = tripleStore.getValueFactory();
        
		final QueryEngine queryEngine = QueryEngineFactory.getInstance()
				.getQueryController(tripleStore.getIndexManager());
		
        final ISolutionSetManager sparqlCache = new SolutionSetManager(
                (IBTreeManager) queryEngine.getIndexManager(),
                tripleStore.getNamespace(), tripleStore.getTimestamp());

		final String solutionSet = "%solutionSet1";
		
        final IVariable<?> x = Var.var("x");
        final IVariable<?> y = Var.var("y");
        final IVariable<?> z = Var.var("z");
        
		final XSDNumericIV<BigdataLiteral> one = new XSDNumericIV<BigdataLiteral>(
				1);
		one.setValue(vf.createLiteral(1));
		
		final XSDNumericIV<BigdataLiteral> two = new XSDNumericIV<BigdataLiteral>(
				2);
//		two.setValue(vf.createLiteral(2));
		
		final XSDNumericIV<BigdataLiteral> three = new XSDNumericIV<BigdataLiteral>(
				3);
//		three.setValue(vf.createLiteral(3));
		
		final XSDNumericIV<BigdataLiteral> four = new XSDNumericIV<BigdataLiteral>(
				4);
		four.setValue(vf.createLiteral(4));
		
		final XSDNumericIV<BigdataLiteral> five = new XSDNumericIV<BigdataLiteral>(
				5);
		five.setValue(vf.createLiteral(5));
		
        final List<IBindingSet> bsets = new LinkedList<IBindingSet>();
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(one));
            bset.set(y, asConst(two));
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(three));
            bset.set(y, asConst(four));
            bset.set(z, asConst(five));
            bsets.add(bset);
        }

        final IBindingSet[] bindingSets = bsets.toArray(new IBindingSet[]{});

		sparqlCache.putSolutions(solutionSet,
				BOpUtility.asIterator(bindingSets));

        final ASTContainer astContainer = testHelper.runTest();

        final PipelineOp queryPlan = astContainer.getQueryPlan();
        
        // top level should be the SLICE operator.
        assertTrue(queryPlan instanceof SliceOp);

        // sole argument should be the PROJECTION operator.
        final PipelineOp projectionOp = (PipelineOp) queryPlan.get(0);

        assertTrue(projectionOp instanceof ProjectionOp);

        // sole argument should be the INCLUDE operator.
        final PipelineOp includeOp = (PipelineOp) projectionOp.get(0);
        
        // the INCLUDE should be evaluated using a solution set SCAN.
        assertTrue(includeOp instanceof NestedLoopJoinOp);
        
    }

    /**
     * A unit test for an INCLUDE with another JOIN. For this test, the INCLUDE
     * will run first:
     * 
     * <pre>
     * %solutionSet1::
     * {x=:Mike,  y=2}
     * {x=:Bryan, y=4}
     * {x=:DC,    y=1}
     * </pre>
     * 
     * <pre>
     * prefix : <http://www.bigdata.com/> 
     * prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
     * prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
     * prefix foaf: <http://xmlns.com/foaf/0.1/> 
     * 
     * SELECT ?x ?y WHERE { 
     * 
     *    # Turn off the join order optimizer.
     *    hint:Query hint:optimizer "None" .
     * 
     *    # Run joins in the given order (INCLUDE is 1st).
     *    
     *    # SCAN => {(x=Mike,y=2);(x=Bryan;y=4);(x=DC,y=1)} 
     *    INCLUDE %solutionSet1 .
     * 
     *    # JOIN on (x) => {(x=Mike,y=2);(x=Bryan,y=4)}
     *    ?x rdf:type foaf:Person .
     *    
     * }
     * </pre>
     * 
     * Note: This excercises the code path in {@link AST2BOpUtility} where we do
     * a SCAN on the named solution set for the INCLUDE and then join with the
     * access path.
     * 
     * @see #test_include_03()
     * 
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/531" >
     *      SPARQL UPDATE for NAMED SOLUTION SETS </a>
     */
    public void test_include_03a() throws Exception {
        
        final TestHelper testHelper = new TestHelper(
                "include_03a",// name
                "include_03a.rq",// query URL
                "include_03.trig",// data URL
                "include_03.srx",// results URL
                false // check order
                );

        final AbstractTripleStore tripleStore = testHelper.getTripleStore();
        
        final BigdataValueFactory vf = tripleStore.getValueFactory();
        
        final QueryEngine queryEngine = QueryEngineFactory.getInstance()
                .getQueryController(tripleStore.getIndexManager());
        
        final ISolutionSetManager sparqlCache = new SolutionSetManager(
                (IBTreeManager) queryEngine.getIndexManager(),
                tripleStore.getNamespace(), tripleStore.getTimestamp());

        final String solutionSet = "%solutionSet1";
        
        final IVariable<?> x = Var.var("x");
        final IVariable<?> y = Var.var("y");

        // Resolve terms pre-loaded into the kb.
        final BigdataURI Mike = vf.createURI("http://www.bigdata.com/Mike"); 
        final BigdataURI Bryan = vf.createURI("http://www.bigdata.com/Bryan");
        final BigdataURI DC = vf.createURI("http://www.bigdata.com/DC");
        {
            tripleStore.addTerms(new BigdataValue[] { Mike, Bryan, DC });
            assertNotNull(Mike.getIV());
            assertNotNull(Bryan.getIV());
            assertNotNull(DC.getIV());
        }

        final XSDNumericIV<BigdataLiteral> one = new XSDNumericIV<BigdataLiteral>(
                1);
        one.setValue(vf.createLiteral(1));
        
        final XSDNumericIV<BigdataLiteral> two = new XSDNumericIV<BigdataLiteral>(
                2);
        two.setValue(vf.createLiteral(2));
        
//      final XSDNumericIV<BigdataLiteral> three = new XSDNumericIV<BigdataLiteral>(
//              3);
//      three.setValue(vf.createLiteral(3));
        
        final XSDNumericIV<BigdataLiteral> four = new XSDNumericIV<BigdataLiteral>(
                4);
        four.setValue(vf.createLiteral(4));
        
//      final XSDNumericIV<BigdataLiteral> five = new XSDNumericIV<BigdataLiteral>(
//              5);
//      five.setValue(vf.createLiteral(5));
        
        final List<IBindingSet> bsets = new LinkedList<IBindingSet>();
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(Mike.getIV()));
            bset.set(y, asConst(two));
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(Bryan.getIV()));
            bset.set(y, asConst(four));
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(DC.getIV()));
            bset.set(y, asConst(one));
            bsets.add(bset);
        }

        final IBindingSet[] bindingSets = bsets.toArray(new IBindingSet[]{});

        sparqlCache.putSolutions(solutionSet,
                BOpUtility.asIterator(bindingSets));

        final ASTContainer astContainer = testHelper.runTest();

        final PipelineOp queryPlan = astContainer.getQueryPlan();

        // top level should be chunked materialization operator
        assertTrue(queryPlan instanceof ChunkedMaterializationOp);
        
        // top level should be the PROJECTION operator.
        final PipelineOp projectionOp = (PipelineOp) queryPlan.get(0);
        assertTrue(projectionOp instanceof ProjectionOp);

        // sole argument should be the PIPELINE JOIN operator.
        final PipelineOp joinOp = (PipelineOp) projectionOp.get(0);
        assertTrue(joinOp instanceof PipelineJoin);

        /*
         * The sole argument of JOIN should be the INCLUDE operator, which
         * should be evaluated using a solution set SCAN. This is where we start
         * evaluation for this query.
         */
        final PipelineOp includeOp = (PipelineOp) joinOp.get(0);
        assertTrue(includeOp instanceof NestedLoopJoinOp);

    }

    /**
     * A unit test for an INCLUDE which is NOT the first JOIN in the WHERE
     * clause. This condition is enforced by turning off the join order
     * optimizer for this query.
     * <p>
     * Note: Since there is another JOIN in this query, there is no longer any
     * order guarantee for the resulting solutions.
     * 
     * <pre>
     * %solutionSet1::
     * {x=:Mike,  y=2}
     * {x=:Bryan, y=4}
     * {x=:DC,    y=1}
     * </pre>
     * 
     * <pre>
     * prefix : <http://www.bigdata.com/> 
     * prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
     * prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
     * prefix foaf: <http://xmlns.com/foaf/0.1/> 
     * 
     * SELECT ?x ?y WHERE { 
     * 
     *    # Turn off the join order optimizer.
     *    hint:Query hint:optimizer "None" .
     * 
     *    # Run joins in the given order (INCLUDE is 2nd).
     *    
     *    # bind x => {Mike;Bryan}
     *    ?x rdf:type foaf:Person .
     *    
     *    # join on (x) => {(x=Mike,y=2);(x=Bryan;y=4)} 
     *    INCLUDE %solutionSet1 .
     * 
     * }
     * </pre>
     * 
     * @see #test_include_03a()
     * 
     * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/531" >
     *      SPARQL UPDATE for NAMED SOLUTION SETS </a>
     */
    public void test_include_03() throws Exception {
    	
        final TestHelper testHelper = new TestHelper(
        		"include_03",// name
        		"include_03.rq",// query URL
        		"include_03.trig",// data URL
        		"include_03.srx",// results URL
        		false // check order
        		);

        final AbstractTripleStore tripleStore = testHelper.getTripleStore();
        
        final BigdataValueFactory vf = tripleStore.getValueFactory();
        
		final QueryEngine queryEngine = QueryEngineFactory.getInstance()
				.getQueryController(tripleStore.getIndexManager());
		
        final ISolutionSetManager sparqlCache = new SolutionSetManager(
                (IBTreeManager) queryEngine.getIndexManager(),
                tripleStore.getNamespace(), tripleStore.getTimestamp());

		final String solutionSet = "%solutionSet1";
		
        final IVariable<?> x = Var.var("x");
        final IVariable<?> y = Var.var("y");

        // Resolve terms pre-loaded into the kb.
        final BigdataURI Mike = vf.createURI("http://www.bigdata.com/Mike"); 
        final BigdataURI Bryan = vf.createURI("http://www.bigdata.com/Bryan");
        final BigdataURI DC = vf.createURI("http://www.bigdata.com/DC");
		{
			tripleStore.addTerms(new BigdataValue[] { Mike, Bryan, DC });
			assertNotNull(Mike.getIV());
			assertNotNull(Bryan.getIV());
			assertNotNull(DC.getIV());
		}

		final XSDNumericIV<BigdataLiteral> one = new XSDNumericIV<BigdataLiteral>(
				1);
		one.setValue(vf.createLiteral(1));
		
		final XSDNumericIV<BigdataLiteral> two = new XSDNumericIV<BigdataLiteral>(
				2);
		two.setValue(vf.createLiteral(2));
		
//		final XSDNumericIV<BigdataLiteral> three = new XSDNumericIV<BigdataLiteral>(
//				3);
//		three.setValue(vf.createLiteral(3));
		
		final XSDNumericIV<BigdataLiteral> four = new XSDNumericIV<BigdataLiteral>(
				4);
		four.setValue(vf.createLiteral(4));
		
//		final XSDNumericIV<BigdataLiteral> five = new XSDNumericIV<BigdataLiteral>(
//				5);
//		five.setValue(vf.createLiteral(5));
		
		        /**
         * <pre>
         * %solutionSet1::
         * {x=:Mike,  y=2}
         * {x=:Bryan, y=4}
         * {x=:DC,    y=1}
         * </pre>
         */
        final List<IBindingSet> bsets = new LinkedList<IBindingSet>();
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(Mike.getIV()));
            bset.set(y, asConst(two));
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(Bryan.getIV()));
            bset.set(y, asConst(four));
            bsets.add(bset);
        }
        {
            final IBindingSet bset = new ListBindingSet();
            bset.set(x, asConst(DC.getIV()));
            bset.set(y, asConst(one));
            bsets.add(bset);
        }

        final IBindingSet[] bindingSets = bsets.toArray(new IBindingSet[]{});

		sparqlCache.putSolutions(solutionSet,
				BOpUtility.asIterator(bindingSets));

        final ASTContainer astContainer = testHelper.runTest();

        /**
         * The plan should be:
         * 
         * 1. A PipelineJoin for the initial triple pattern
         * 
         * 2. A HashIndexOp to generate an appropriate index for the join with
         * the solution set. (The main point of this test is to verify that we
         * build an appropriate hash index to do the join rather than using a
         * NestedLoopJoinOp.)
         * 
         * 3. A SolutionSetHashJoin
         * 
         * 4. A ProjectionOp.
         */
//        com.bigdata.bop.solutions.ProjectionOp[6](JVMSolutionSetHashJoinOp[5])[ com.bigdata.bop.BOp.bopId=6, com.bigdata.bop.BOp.evaluationContext=CONTROLLER, com.bigdata.bop.PipelineOp.sharedState=true, com.bigdata.bop.join.JoinAnnotations.select=[x, y], com.bigdata.bop.engine.QueryEngine.queryId=562dbadb-afcc-4a2c-bf70-2486f1061dc3]
//                com.bigdata.bop.join.JVMSolutionSetHashJoinOp[5](JVMHashIndexOp[4])[ com.bigdata.bop.BOp.bopId=5, com.bigdata.bop.BOp.evaluationContext=CONTROLLER, com.bigdata.bop.PipelineOp.sharedState=true, namedSetRef=NamedSolutionSetRef{queryId=562dbadb-afcc-4a2c-bf70-2486f1061dc3,namedSet=%solutionSet1,joinVars=[x]}, com.bigdata.bop.join.JoinAnnotations.constraints=null, class com.bigdata.bop.join.SolutionSetHashJoinOp.release=false]
//                  com.bigdata.bop.join.JVMHashIndexOp[4](PipelineJoin[3])[ com.bigdata.bop.BOp.bopId=4, com.bigdata.bop.BOp.evaluationContext=CONTROLLER, com.bigdata.bop.PipelineOp.maxParallel=1, com.bigdata.bop.PipelineOp.lastPass=true, com.bigdata.bop.PipelineOp.sharedState=true, com.bigdata.bop.join.JoinAnnotations.joinType=Normal, com.bigdata.bop.join.HashJoinAnnotations.joinVars=[x], com.bigdata.bop.join.JoinAnnotations.select=null, namedSetSourceRef=NamedSolutionSetRef{queryId=562dbadb-afcc-4a2c-bf70-2486f1061dc3,namedSet=%solutionSet1,joinVars=[]}, namedSetRef=NamedSolutionSetRef{queryId=562dbadb-afcc-4a2c-bf70-2486f1061dc3,namedSet=%solutionSet1,joinVars=[x]}]
//                    com.bigdata.bop.join.PipelineJoin[3]()[ com.bigdata.bop.BOp.bopId=3, com.bigdata.bop.join.JoinAnnotations.constraints=null, com.bigdata.bop.BOp.evaluationContext=ANY, com.bigdata.bop.join.AccessPathJoinAnnotations.predicate=SPOPredicate[1]]

        final PipelineOp queryPlan = astContainer.getQueryPlan();

        // top level should be chunked materialization operator
        assertTrue(queryPlan instanceof ChunkedMaterializationOp);
        
        // top level should be the PROJECTION operator.
        assertTrue(queryPlan.get(0) instanceof ProjectionOp);

        // sole argument should be the SOLUTION SET HASH JOIN operator.
        final PipelineOp solutionSetHashJoinOp = (PipelineOp) queryPlan.get(0).get(0);

        assertTrue(solutionSetHashJoinOp instanceof SolutionSetHashJoinOp);

        // sole argument should be the HASH INDEX BUILD operator.
        final PipelineOp hashIndexOp = (PipelineOp) solutionSetHashJoinOp.get(0);
        
        assertTrue(hashIndexOp instanceof HashIndexOp);

        // sole argument should be the PIPELINE JOIN (triple pattern).
        final PipelineOp pipelineJoinOp = (PipelineOp) hashIndexOp.get(0);

        assertTrue(pipelineJoinOp instanceof PipelineJoin);

    }
    
    /**
     * Execute the stress tests a couple of times.
     * 
     * @throws Exception
     */
    public void test_stressTests() throws Exception {

        for (int i = 0; i < 100; i++) {
            final TestSuite suite = new TestSuite(
                IncludeStressTests.class.getSimpleName());

            suite.addTestSuite(IncludeStressTests.class);
            suite.run(new TestResult());
        }
    }
    
    
    /**
     * Tests to be executed in a stress test fashion, i.e. multiple times.
     * 
     * @author msc
     */
    public static class IncludeStressTests 
    extends AbstractDataDrivenSPARQLTestCase  {
       
       
        /**
         * 
         */
        public IncludeStressTests() {
        }

        /**
         * @param name
         */
        public IncludeStressTests(String name) {
            super(name);
        }       
       
        /**
         * This test populates a named solution set and then examines the ability to
         * deliver that named solution set in the same order in which the data were
         * stored. Normally, the named solution set would be created using
         * <code>INSERT INTO SOLUTIONS</code>, but in this case we drop down a level
         * and handle the creation of the named solution set in the test setup.
         * 
         * <pre>
         * SELECT * WHERE { INCLUDE %solutionSet1 }
         * </pre>
         */
        public void test_include_01() throws Exception {

            final TestHelper testHelper = new TestHelper("include_01",// name
                "include_01.rq",// query URL
                "include_01.trig",// data URL
                "include_01.srx",// results URL
                true // check order(!)
            );

            final AbstractTripleStore tripleStore = testHelper.getTripleStore();

            final BigdataValueFactory vf = tripleStore.getValueFactory();

            final QueryEngine queryEngine = QueryEngineFactory.getInstance()
                .getQueryController(tripleStore.getIndexManager());

            final ISolutionSetManager sparqlCache = new SolutionSetManager(
                (IBTreeManager) queryEngine.getIndexManager(),
                tripleStore.getNamespace(), tripleStore.getTimestamp());

            final String solutionSet = "%solutionSet1";

            final IVariable<?> x = Var.var("x");
            final IVariable<?> y = Var.var("y");
            final IVariable<?> z = Var.var("z");

            final XSDNumericIV<BigdataLiteral> one = 
                new XSDNumericIV<BigdataLiteral>(1);
            one.setValue(vf.createLiteral(1));

            final XSDNumericIV<BigdataLiteral> two = 
                new XSDNumericIV<BigdataLiteral>(2);

            final XSDNumericIV<BigdataLiteral> three = 
                new XSDNumericIV<BigdataLiteral>(3);

            final XSDNumericIV<BigdataLiteral> four = 
                new XSDNumericIV<BigdataLiteral>(4);
            four.setValue(vf.createLiteral(4));

           final XSDNumericIV<BigdataLiteral> five = 
                new XSDNumericIV<BigdataLiteral>(5);
           five.setValue(vf.createLiteral(5));

           final List<IBindingSet> bsets = new LinkedList<IBindingSet>();
           {
                final IBindingSet bset = new ListBindingSet();
                bset.set(x, asConst(one));
                bset.set(y, asConst(two));
                bsets.add(bset);
           }

           {
               final IBindingSet bset = new ListBindingSet();
               bsets.add(bset);
           }
           
           {
               final IBindingSet bset = new ListBindingSet();
               bset.set(x, asConst(three));
               bset.set(y, asConst(four));
               bset.set(z, asConst(five));
               bsets.add(bset);
           }

           final IBindingSet[] bindingSets = bsets.toArray(new IBindingSet[] {});

           sparqlCache.putSolutions(solutionSet,
           BOpUtility.asIterator(bindingSets));

           final ASTContainer astContainer = testHelper.runTest();

           final PipelineOp queryPlan = astContainer.getQueryPlan();

           // top level should be the PROJECTION operator.
           assertTrue(queryPlan instanceof ProjectionOp);

           // sole argument should be the INCLUDE operator.
           final PipelineOp includeOp = (PipelineOp) queryPlan.get(0);

           // the INCLUDE should be evaluated using a solution set SCAN.
           assertTrue(includeOp instanceof NestedLoopJoinOp);

      }

      protected <T> IConstant<T> asConst(final T val) {

         return new Constant<T>(val);

      }
    }   
    
}
