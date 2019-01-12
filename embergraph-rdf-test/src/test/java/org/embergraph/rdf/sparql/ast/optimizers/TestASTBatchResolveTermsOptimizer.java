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
 * Created on Aug 29, 2011
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import java.util.Collections;

import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.StatementPattern.Scope;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IValueExpression;
import org.embergraph.bop.Var;
import org.embergraph.bop.bindingSet.ListBindingSet;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.IPrefixDecls.Annotations;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.sparql.ast.eval.ASTDeferredIVResolution;

/**
 * Test suite for {@link ASTDeferredIVResolution}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestASTServiceNodeOptimizer.java 6080 2012-03-07 18:38:55Z thompsonbry $
 */
public class TestASTBatchResolveTermsOptimizer extends AbstractASTEvaluationTestCase {

    /**
     * 
     */
    public TestASTBatchResolveTermsOptimizer() {
    }

    /**
     * @param name
     */
    public TestASTBatchResolveTermsOptimizer(String name) {
        super(name);
    }

    /**
	 * Given
	 * 
	 * <pre>
	 * SELECT VarNode(s) VarNode(p) VarNode(v)
	 *   JoinGroupNode {
	 *     JoinGroupNode {
	 *       JoinGroupNode [context=ConstantNode(TermId(0L)[http://example/out])] {
	 *         StatementPatternNode(VarNode(s), VarNode(p), VarNode(v), ConstantNode(TermId(0L)[http://example/out])) [scope=NAMED_CONTEXTS]
	 *       }
	 *     }
	 *   }
	 * </pre>
	 * 
	 * where the unknown term is <code>http://example/out</code> and IS in fact
	 * in the lexicon, the {@link IValueExpression} for the {@link ConstantNode}
	 * associated with that mock IV is rewritten to the resolved IV.
     * @throws MalformedQueryException 
	 */
    public void test_batchResolveTerms_01() throws MalformedQueryException {

        /*
		 * Note: DO NOT share structures in this test!!!!
		 * 
		 * Note: This test depends on having multiple BigdataURIs for the
		 * unknown term. In one case the IV is known and in the other case it is
		 * not known.
		 */

    		final EmbergraphValueFactory f = store.getValueFactory();

    		// A version where a mock IV is associated with the term. 
    		final EmbergraphURI unknown1 = f.createURI("http://example/out");
    		unknown1.setIV(TermId.mockIV(VTE.URI));
    		assertFalse(unknown1.isRealIV());
    		unknown1.getIV().setValue(unknown1);
    		
    		// A version where a real IV is associated with the term. 
    		final EmbergraphURI known1 = f.createURI("http://example/out");
    		store.addTerms(new EmbergraphValue[]{known1});
    		assertTrue(known1.isRealIV());
    		
        final IBindingSet[] bsets = new IBindingSet[] {
                new ListBindingSet()
        };

        /**
         * The source AST.
         */
        final QueryRoot given = new QueryRoot(QueryType.SELECT);
        {

            final ProjectionNode projection = new ProjectionNode();
            given.setProjection(projection);

            projection.addProjectionVar(new VarNode("s"));
            projection.addProjectionVar(new VarNode("p"));
            projection.addProjectionVar(new VarNode("v"));

            final JoinGroupNode whereClause = new JoinGroupNode();
            given.setWhereClause(whereClause);

            {

				final JoinGroupNode graphPattern = new JoinGroupNode();
				graphPattern.setContext(new ConstantNode(new Constant(unknown1
						.getIV())));
				whereClause.addChild(graphPattern);

				final JoinGroupNode innerGroup = new JoinGroupNode();
				graphPattern.addChild(innerGroup);

				innerGroup.addChild(new StatementPatternNode(new VarNode("s"),
						new VarNode("p"), new VarNode("v"), new ConstantNode(
								new Constant(unknown1.getIV()))/* c */,
						Scope.NAMED_CONTEXTS));

            }

        }

        /**
         * The expected AST after the rewrite.
         */
        final QueryRoot expected = new QueryRoot(QueryType.SELECT);
        {

            final ProjectionNode projection = new ProjectionNode();
            expected.setProjection(projection);
            expected.setProperty(Annotations.PREFIX_DECLS, Collections.emptyMap());

            projection.addProjectionVar(new VarNode("s"));
            projection.addProjectionVar(new VarNode("p"));
            projection.addProjectionVar(new VarNode("v"));

            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);
            expected.setProperty(Annotations.PREFIX_DECLS, Collections.emptyMap());
            {

				final JoinGroupNode graphPattern = new JoinGroupNode();
				graphPattern.setContext(new ConstantNode(new Constant(known1
						.getIV())));
				whereClause.addChild(graphPattern);

                final JoinGroupNode innerGroup = new JoinGroupNode();
                graphPattern.addChild(innerGroup);
                
				innerGroup.addChild(new StatementPatternNode(new VarNode("s"),
						new VarNode("p"), new VarNode("v"), new ConstantNode(
								new Constant(known1.getIV()))/* c */,
						Scope.NAMED_CONTEXTS));

            }

        }

        ASTContainer astContainer = new ASTContainer(given);

        ASTDeferredIVResolution.resolveQuery(store, astContainer);

        QueryRoot actual = astContainer.getOriginalAST();
        
        assertSameAST(expected, actual);

    }

    /**
	 * A variant of the test above where the Constant/2 constructor was used and
	 * we need to propagate the variable associated with that constant.
     * @throws MalformedQueryException 
	 */
	public void test_batchResolveTerms_02() throws MalformedQueryException {

        /*
		 * Note: DO NOT share structures in this test!!!!
		 * 
		 * Note: This test depends on having multiple BigdataURIs for the
		 * unknown term. In one case the IV is known and in the other case it is
		 * not known.
		 */

    		final EmbergraphValueFactory f = store.getValueFactory();

    		// A version where a mock IV is associated with the term. 
    		final EmbergraphURI unknown1 = f.createURI("http://example/out");
    		unknown1.setIV(TermId.mockIV(VTE.URI));
    		assertFalse(unknown1.isRealIV());
    		unknown1.getIV().setValue(unknown1);
    		
    		// A version where a real IV is associated with the term. 
    		final EmbergraphURI known1 = f.createURI("http://example/out");
    		store.addTerms(new EmbergraphValue[]{known1});
    		assertTrue(known1.isRealIV());
    		
        final IBindingSet[] bsets = new IBindingSet[] {
                new ListBindingSet()
        };

        /**
         * The source AST.
         */
        final QueryRoot given = new QueryRoot(QueryType.SELECT);
        {

            final ProjectionNode projection = new ProjectionNode();
            given.setProjection(projection);

            projection.addProjectionVar(new VarNode("s"));
            projection.addProjectionVar(new VarNode("p"));
            projection.addProjectionVar(new VarNode("v"));

            final JoinGroupNode whereClause = new JoinGroupNode();
            given.setWhereClause(whereClause);

            {

				final JoinGroupNode graphPattern = new JoinGroupNode();
				graphPattern.setContext(new ConstantNode(new Constant(Var
						.var("x"), unknown1.getIV())));
				whereClause.addChild(graphPattern);

				final JoinGroupNode innerGroup = new JoinGroupNode();
				graphPattern.addChild(innerGroup);

				innerGroup.addChild(new StatementPatternNode(new VarNode("s"),
						new VarNode("p"), new VarNode("v"),
						new ConstantNode(new Constant(Var.var("x"), unknown1
								.getIV()))/* c */, Scope.NAMED_CONTEXTS));

            }

        }

        /**
         * The expected AST after the rewrite.
         */
        final QueryRoot expected = new QueryRoot(QueryType.SELECT);
        {

			final ProjectionNode projection = new ProjectionNode();
			expected.setProjection(projection);
            expected.setProperty(Annotations.PREFIX_DECLS, Collections.emptyMap());

			projection.addProjectionVar(new VarNode("s"));
			projection.addProjectionVar(new VarNode("p"));
			projection.addProjectionVar(new VarNode("v"));

			final JoinGroupNode whereClause = new JoinGroupNode();
			expected.setWhereClause(whereClause);

			{

				final JoinGroupNode graphPattern = new JoinGroupNode();
				graphPattern.setContext(new ConstantNode(new Constant(Var
						.var("x"), known1.getIV())));
				whereClause.addChild(graphPattern);

				final JoinGroupNode innerGroup = new JoinGroupNode();
				graphPattern.addChild(innerGroup);

				innerGroup.addChild(new StatementPatternNode(new VarNode("s"),
						new VarNode("p"), new VarNode("v"),
						new ConstantNode(new Constant(Var.var("x"), known1
								.getIV()))/* c */, Scope.NAMED_CONTEXTS));

            }

        }

        ASTContainer astContainer = new ASTContainer(given);
        final AST2BOpContext context = new AST2BOpContext(astContainer, store);

        ASTDeferredIVResolution.resolveQuery(store, astContainer);

        QueryRoot actual = astContainer.getOriginalAST();

        assertSameAST(expected, actual);

	}
    
}
