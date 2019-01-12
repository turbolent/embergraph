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
 * Created on Sep 1, 2011
 */

package org.embergraph.rdf.sparql.ast.optimizers;

import org.apache.log4j.Logger;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphStatementImpl;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.algebra.StatementPattern.Scope;

import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import org.embergraph.rdf.sparql.ast.AssignmentNode;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.ConstructNode;
import org.embergraph.rdf.sparql.ast.DescribeModeEnum;
import org.embergraph.rdf.sparql.ast.IQueryNode;
import org.embergraph.rdf.sparql.ast.JoinGroupNode;
import org.embergraph.rdf.sparql.ast.ProjectionNode;
import org.embergraph.rdf.sparql.ast.QueryNodeWithBindingSet;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.sparql.ast.UnionNode;
import org.embergraph.rdf.sparql.ast.VarNode;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpContext;
import org.embergraph.rdf.vocab.decls.FOAFVocabularyDecl;

/**
 * Test suite for the {@link ASTDescribeOptimizer}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestASTDescribeOptimizer extends AbstractASTEvaluationTestCase {

    private static final Logger log = Logger
            .getLogger(TestASTDescribeOptimizer.class);

    public TestASTDescribeOptimizer() {
        super();
    }

    public TestASTDescribeOptimizer(String name) {
        super(name);
    }

    /**
     * Unit test for the AST rewrite of a simple describe query
     * 
     * <pre>
     * describe <http://www.bigdata.com/Mike>
     * </pre>
     * 
     * This test verifies that the correct CONSTRUCT clause is generated and
     * that a WHERE clause is added which will capture the necessary bindings to
     * support that CONSTUCT.
     */
    public void test_describeOptimizer_iri_only() {

        final EmbergraphValueFactory f = store.getValueFactory();

        // Add some data.
        {

            final EmbergraphURI g = f.createURI("http://www.embergraph.org");
            
            final EmbergraphStatement[] stmts = new EmbergraphStatement[] {

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Mike"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Bryan"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/DC"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("DC"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

            };

            final StatementBuffer<EmbergraphStatement> buf = new StatementBuffer<EmbergraphStatement>(
                    store, 10/* capacity */);

            for (EmbergraphStatement stmt : stmts) {

                buf.add(stmt);

            }

            // write on the database.
            buf.flush();

        }

        final EmbergraphURI rdfType = f.createURI(RDF.TYPE.toString());

        final EmbergraphURI rdfsLabel = f.createURI(RDFS.LABEL.toString());

        final EmbergraphURI foafPerson = f.createURI(FOAFVocabularyDecl.Person
                .toString());

        final EmbergraphURI mikeURI = f
                .createURI("http://www.embergraph.org/Mike");

        final EmbergraphURI bryanURI = f
                .createURI("http://www.embergraph.org/Bryan");

        final EmbergraphLiteral mikeLabel = f.createLiteral("Mike");

        final EmbergraphLiteral bryanLabel = f.createLiteral("Bryan");

        final EmbergraphValue[] values = new EmbergraphValue[] { rdfType,
                rdfsLabel, foafPerson, mikeURI, bryanURI, mikeLabel,
                bryanLabel };

        // resolve IVs.
        store.getLexiconRelation()
                .addTerms(values, values.length, true/* readOnly */);

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            final VarNode anonvar = new VarNode("-iri-1");
            anonvar.setAnonymous(true);
            projection.addProjectionExpression(new AssignmentNode(anonvar,
                    new ConstantNode(mikeURI.getIV())));

        }

        /*
         * Setup the expected AST model.
         */
        final QueryRoot expected = new QueryRoot(QueryType.CONSTRUCT);
        {
            
            /*
             * Setup the projection node.
             * 
             * Note: [Actually, this is also used to carry the DescribeMode, so
             * now it is always present.] This is present if the DESCRIBE cache
             * is being maintained based on observed solutions to DESCRIBE
             * queries.
             */
            {
                final ProjectionNode projection = new ProjectionNode();
                expected.setProjection(projection);
                projection.setReduced(true);
                final VarNode anonvar = new VarNode("-iri-1");
                anonvar.setAnonymous(true);
                projection.addProjectionExpression(new AssignmentNode(anonvar,
                        new ConstantNode(mikeURI.getIV())));
            }
            
            final VarNode p0a = new VarNode("p0a");
            final VarNode p0b = new VarNode("p0b");
            final VarNode o0 = new VarNode("o0");
            final VarNode s0 = new VarNode("s0");
            final ConstantNode term0 = new ConstantNode(mikeURI.getIV());

            /*
             * Setup the CONSTRUCT node.
             */
            // term1 ?p1a ?o1 .
            // ?s1 ?p1b term1 .
            {
                final ConstructNode constructNode = new ConstructNode();
                expected.setConstruct(constructNode);
                constructNode
                        .addChild(new StatementPatternNode(term0, p0a, o0));
                constructNode
                        .addChild(new StatementPatternNode(s0, p0b, term0));
            }
            
            /*
             * Setup the WHERE clause.
             * 
             * Note: The original WHERE clause is preserved and we add a
             * union of two statement patterns for each term (variable or
             * constant) in the DESCRIBE clause.
             */
            // {
            // term1 ?p1a ?o1 .
            // } union {
            // ?s1 ?p1b term1 .
            // }
            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);
            final UnionNode union = new UnionNode();
            whereClause.addChild(union);
            union.addChild(new JoinGroupNode(new StatementPatternNode(term0, p0a, o0)));
            union.addChild(new JoinGroupNode(new StatementPatternNode(s0, p0b, term0)));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

//            if (context.getDescribeCache() != null) {
//
//                expected.setProjection(projection);
//                
//            }
            
            final IQueryNode actual = new ASTDescribeOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null)).
                    getQueryNode();

            assertSameAST(expected, actual);

        }

    }

    /**
     * Unit test for the AST rewrite of a simple describe query
     * 
     * <pre>
     * describe <http://www.bigdata.com/Mike>
     * </pre>
     * 
     * where the {@link ProjectionNode} has been explicitly marked to specify
     * {@link DescribeModeEnum#ForwardOneStep}.
     * <p>
     * This test verifies that the correct CONSTRUCT clause is generated and
     * that a WHERE clause is added which will capture the necessary bindings to
     * support that CONSTUCT.
     */
    public void test_describeOptimizer_iri_only_describeMode_forwardOneStep() {

        final EmbergraphValueFactory f = store.getValueFactory();

        // Add some data.
        {

            final EmbergraphURI g = f.createURI("http://www.embergraph.org");
            
            final EmbergraphStatement[] stmts = new EmbergraphStatement[] {

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Mike"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Bryan"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/DC"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("DC"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

            };

            final StatementBuffer<EmbergraphStatement> buf = new StatementBuffer<EmbergraphStatement>(
                    store, 10/* capacity */);

            for (EmbergraphStatement stmt : stmts) {

                buf.add(stmt);

            }

            // write on the database.
            buf.flush();

        }

        final EmbergraphURI rdfType = f.createURI(RDF.TYPE.toString());

        final EmbergraphURI rdfsLabel = f.createURI(RDFS.LABEL.toString());

        final EmbergraphURI foafPerson = f.createURI(FOAFVocabularyDecl.Person
                .toString());

        final EmbergraphURI mikeURI = f
                .createURI("http://www.embergraph.org/Mike");

        final EmbergraphURI bryanURI = f
                .createURI("http://www.embergraph.org/Bryan");

        final EmbergraphLiteral mikeLabel = f.createLiteral("Mike");

        final EmbergraphLiteral bryanLabel = f.createLiteral("Bryan");

        final EmbergraphValue[] values = new EmbergraphValue[] { rdfType,
                rdfsLabel, foafPerson, mikeURI, bryanURI, mikeLabel,
                bryanLabel };

        // resolve IVs.
        store.getLexiconRelation()
                .addTerms(values, values.length, true/* readOnly */);

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            final VarNode anonvar = new VarNode("-iri-1");
            anonvar.setAnonymous(true);
            projection.addProjectionExpression(new AssignmentNode(anonvar,
                    new ConstantNode(mikeURI.getIV())));
            
            projection.setDescribeMode(DescribeModeEnum.ForwardOneStep);

        }

        /*
         * Setup the expected AST model.
         */
        final QueryRoot expected = new QueryRoot(QueryType.CONSTRUCT);
        {
            
            /*
             * Setup the projection node.
             * 
             * Note: [Actually, this is also used to carry the DescribeMode, so
             * now it is always present.] This is present if the DESCRIBE cache
             * is being maintained based on observed solutions to DESCRIBE
             * queries.
             */
            {
                final ProjectionNode projection = new ProjectionNode();
                expected.setProjection(projection);
                projection.setReduced(true);
                final VarNode anonvar = new VarNode("-iri-1");
                anonvar.setAnonymous(true);
                projection.addProjectionExpression(new AssignmentNode(anonvar,
                        new ConstantNode(mikeURI.getIV())));
                projection.setDescribeMode(DescribeModeEnum.ForwardOneStep);
            }
            
            final VarNode p0a = new VarNode("p0a");
//            final VarNode p0b = new VarNode("p0b");
            final VarNode o0 = new VarNode("o0");
//            final VarNode s0 = new VarNode("s0");
            final ConstantNode term0 = new ConstantNode(mikeURI.getIV());

            /*
             * Setup the CONSTRUCT node.
             */
            // term1 ?p1a ?o1 .
            // ?s1 ?p1b term1 .
            {
                final ConstructNode constructNode = new ConstructNode();
                expected.setConstruct(constructNode);
                constructNode
                        .addChild(new StatementPatternNode(term0, p0a, o0));
//                constructNode
//                        .addChild(new StatementPatternNode(s0, p0b, term0));
            }
            
            /*
             * Setup the WHERE clause.
             * 
             * Note: The original WHERE clause is preserved and we add a
             * union of two statement patterns for each term (variable or
             * constant) in the DESCRIBE clause.
             */
            // {
            // term1 ?p1a ?o1 .
            // } union {
            // ?s1 ?p1b term1 .
            // }
            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);
            final UnionNode union = new UnionNode();
            whereClause.addChild(union);
            union.addChild(new JoinGroupNode(new StatementPatternNode(term0, p0a, o0)));
//            union.addChild(new JoinGroupNode(new StatementPatternNode(s0, p0b, term0)));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

//            if (context.getDescribeCache() != null) {
//
//                expected.setProjection(projection);
//                
//            }
            
            final IQueryNode actual = new ASTDescribeOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null)).
                    getQueryNode();

            assertSameAST(expected, actual);

        }

    }

    /**
     * Unit test for the AST rewrite of a simple describe query
     * 
     * <pre>
     * describe <http://www.bigdata.com/Mike>
     * </pre>
     * 
     * where the {@link ProjectionNode} has been explicitly marked to specify
     * {@link DescribeModeEnum#CBD}.
     * <p>
     * This test verifies that the correct CONSTRUCT clause is generated and
     * that a WHERE clause is added which will capture the necessary bindings to
     * support that CONSTUCT.
     */
    public void test_describeOptimizer_iri_only_describeMode_CBD() {

        final EmbergraphValueFactory f = store.getValueFactory();

        // Add some data.
        {

            final EmbergraphURI g = f.createURI("http://www.embergraph.org");
            
            final EmbergraphStatement[] stmts = new EmbergraphStatement[] {

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Mike"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Bryan"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/DC"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("DC"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

            };

            final StatementBuffer<EmbergraphStatement> buf = new StatementBuffer<EmbergraphStatement>(
                    store, 10/* capacity */);

            for (EmbergraphStatement stmt : stmts) {

                buf.add(stmt);

            }

            // write on the database.
            buf.flush();

        }

        final EmbergraphURI rdfType = f.createURI(RDF.TYPE.toString());

        final EmbergraphURI rdfsLabel = f.createURI(RDFS.LABEL.toString());

        final EmbergraphURI foafPerson = f.createURI(FOAFVocabularyDecl.Person
                .toString());

        final EmbergraphURI mikeURI = f
                .createURI("http://www.embergraph.org/Mike");

        final EmbergraphURI bryanURI = f
                .createURI("http://www.embergraph.org/Bryan");

        final EmbergraphLiteral mikeLabel = f.createLiteral("Mike");

        final EmbergraphLiteral bryanLabel = f.createLiteral("Bryan");

        final EmbergraphURI rdfSubject = f.createURI(RDF.SUBJECT.toString());

        final EmbergraphValue[] values = new EmbergraphValue[] { rdfType,
                rdfsLabel, foafPerson, mikeURI, bryanURI, mikeLabel,
                bryanLabel, rdfSubject };

        // resolve IVs.
        store.getLexiconRelation()
                .addTerms(values, values.length, true/* readOnly */);

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            final VarNode anonvar = new VarNode("-iri-1");
            anonvar.setAnonymous(true);
            projection.addProjectionExpression(new AssignmentNode(anonvar,
                    new ConstantNode(mikeURI.getIV())));
            
            projection.setDescribeMode(DescribeModeEnum.CBD);

        }

        /*
         * Setup the expected AST model.
         */
        final QueryRoot expected = new QueryRoot(QueryType.CONSTRUCT);
        {
            
            /*
             * Setup the projection node.
             * 
             * Note: [Actually, this is also used to carry the DescribeMode, so
             * now it is always present.] This is present if the DESCRIBE cache
             * is being maintained based on observed solutions to DESCRIBE
             * queries.
             */
            {
                final ProjectionNode projection = new ProjectionNode();
                expected.setProjection(projection);
                projection.setReduced(true);
                final VarNode anonvar = new VarNode("-iri-1");
                anonvar.setAnonymous(true);
                projection.addProjectionExpression(new AssignmentNode(anonvar,
                        new ConstantNode(mikeURI.getIV())));
                projection.setDescribeMode(DescribeModeEnum.CBD);
            }
            
            final VarNode p0a = new VarNode("p0a");
//            final VarNode p0b = new VarNode("p0b");
            final VarNode o0 = new VarNode("o0");
//            final VarNode s0 = new VarNode("s0");
            final ConstantNode term0 = new ConstantNode(mikeURI.getIV());
            final VarNode stmtVar = new VarNode("stmt0");
            final ConstantNode termRdfSubject = new ConstantNode(rdfSubject.getIV());

            /*
             * Setup the CONSTRUCT node.
             */
            // term1 ?p1a ?o1 .
            // ?s1 ?p1b term1 .
            {
                final ConstructNode constructNode = new ConstructNode();
                expected.setConstruct(constructNode);
                constructNode
                        .addChild(new StatementPatternNode(term0, p0a, o0));
//                constructNode
//                        .addChild(new StatementPatternNode(s0, p0b, term0));
                constructNode.addChild(new StatementPatternNode(stmtVar,
                        termRdfSubject, term0));
            }
            
            /*
             * Setup the WHERE clause.
             * 
             * Note: The original WHERE clause is preserved and we add a
             * union of two statement patterns for each term (variable or
             * constant) in the DESCRIBE clause.
             */
            // {
            // term1 ?p1a ?o1 .
            // } union {
            // ?s1 ?p1b term1 .
            // }
            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);
            final UnionNode union = new UnionNode();
            whereClause.addChild(union);
            union.addChild(new JoinGroupNode(new StatementPatternNode(term0, p0a, o0)));
            union.addChild(new JoinGroupNode(new StatementPatternNode(stmtVar, termRdfSubject, term0)));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

//            if (context.getDescribeCache() != null) {
//
//                expected.setProjection(projection);
//                
//            }
            
            final IQueryNode actual = new ASTDescribeOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null)).
                    getQueryNode();

            assertSameAST(expected, actual);

        }

    }

    /**
     * Unit test for the AST rewrite of a simple describe query involving an IRI
     * and a variable bound by a WHERE clause. The IRI in this case was chosen
     * such that it would NOT be selected by the WHERE clause.
     * 
     * <pre>
     * describe <http://www.bigdata.com/DC> ?x 
     * where {
     *   ?x rdf:type foaf:Person
     * }
     * </pre>
     * 
     * This test verifies that the correct CONSTRUCT clause is generated and
     * that a WHERE clause is added which will capture the necessary bindings to
     * support that CONSTUCT while preserving the original WHERE clause.
     */
    public void test_describeOptimizer_iri_and_var() {

        final EmbergraphValueFactory f = store.getValueFactory();

        // Add some data.
        {

            final EmbergraphURI g = f.createURI("http://www.embergraph.org");
            
            final EmbergraphStatement[] stmts = new EmbergraphStatement[] {

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Mike"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Bryan"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/DC"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("DC"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

            };

            final StatementBuffer<EmbergraphStatement> buf = new StatementBuffer<EmbergraphStatement>(
                    store, 10/* capacity */);

            for (EmbergraphStatement stmt : stmts) {

                buf.add(stmt);

            }

            // write on the database.
            buf.flush();

        }

        final EmbergraphURI rdfType = f.createURI(RDF.TYPE.toString());

        final EmbergraphURI rdfsLabel = f.createURI(RDFS.LABEL.toString());

        final EmbergraphURI foafPerson = f.createURI(FOAFVocabularyDecl.Person
                .toString());

        final EmbergraphURI mikeURI = f
                .createURI("http://www.embergraph.org/Mike");

        final EmbergraphURI bryanURI = f
                .createURI("http://www.embergraph.org/Bryan");

        final EmbergraphLiteral mikeLabel = f.createLiteral("Mike");

        final EmbergraphLiteral bryanLabel = f.createLiteral("Bryan");

        final EmbergraphURI dcURI = f.createURI("http://www.embergraph.org/DC");

        final EmbergraphValue[] values = new EmbergraphValue[] { rdfType,
                rdfsLabel, foafPerson, mikeURI, bryanURI, mikeLabel,
                bryanLabel, dcURI };

        // resolve IVs.
        store.getLexiconRelation()
                .addTerms(values, values.length, true/* readOnly */);

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            final VarNode anonvar = new VarNode("-iri-1");
            anonvar.setAnonymous(true);
            projection.addProjectionExpression(new AssignmentNode(anonvar,
                    new ConstantNode(dcURI.getIV())));

            projection.addProjectionVar(new VarNode("x"));

            final JoinGroupNode whereClause = new JoinGroupNode();
            queryRoot.setWhereClause(whereClause);

            whereClause.addChild(new StatementPatternNode(new VarNode("x"),
                    new ConstantNode(rdfType.getIV()), new ConstantNode(
                            foafPerson.getIV()), null/* c */,
                    Scope.DEFAULT_CONTEXTS));
        }

        /*
         * Setup the expected AST model.
         */
        final QueryRoot expected = new QueryRoot(QueryType.CONSTRUCT);
        {
            
            /*
             * Setup the projection node.
             * 
             * Note: This is only present if the DESCRIBE cache is being
             * maintained based on observed solutions to DESCRIBE queries.
             */
            {
                final ProjectionNode projection = new ProjectionNode();
                expected.setProjection(projection);
                
                projection.setReduced(true);
                
                final VarNode anonvar = new VarNode("-iri-1");
                anonvar.setAnonymous(true);
                projection.addProjectionExpression(new AssignmentNode(anonvar,
                        new ConstantNode(dcURI.getIV())));

                projection.addProjectionVar(new VarNode("x"));

            }
            
            /*
             * Setup the CONSTRUCT node.
             */
            // term1 ?p1a ?o1 .
            // ?s1 ?p1b term1 .
            final ConstructNode constructNode = new ConstructNode();
            expected.setConstruct(constructNode);

            // DC
            final ConstantNode term0 = new ConstantNode(dcURI.getIV());
            final VarNode p0a = new VarNode("p0a");
            final VarNode p0b = new VarNode("p0b");
            final VarNode o0 = new VarNode("o0");
            final VarNode s0 = new VarNode("s0");
            constructNode
                    .addChild(new StatementPatternNode(term0, p0a, o0));
            constructNode
                    .addChild(new StatementPatternNode(s0, p0b, term0));

            // ?x
            final VarNode term1 = new VarNode("x");
            final VarNode p1a = new VarNode("p1a");
            final VarNode p1b = new VarNode("p1b");
            final VarNode o1 = new VarNode("o1");
            final VarNode s1 = new VarNode("s1");
            constructNode
                    .addChild(new StatementPatternNode(term1, p1a, o1));
            constructNode
                    .addChild(new StatementPatternNode(s1, p1b, term1));

            /*
             * Setup the WHERE clause.
             * 
             * Note: The original WHERE clause is preserved and we add a
             * union of two statement patterns for each term (variable or
             * constant) in the DESCRIBE clause.
             */
            // {
            // term1 ?p1a ?o1 .
            // } union {
            // ?s1 ?p1b term1 .
            // }
            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);

            // original WHERE clause.
            whereClause.addChild(new StatementPatternNode(new VarNode("x"),
                    new ConstantNode(rdfType.getIV()), new ConstantNode(
                            foafPerson.getIV()), null/* c */,
                    Scope.DEFAULT_CONTEXTS));

            // union of 2 statement patterns per described term.
            final UnionNode union = new UnionNode();
            whereClause.addChild(union);

            union.addChild(new JoinGroupNode(new StatementPatternNode(term0, p0a, o0)));
            union.addChild(new JoinGroupNode(new StatementPatternNode(s0, p0b, term0)));

            union.addChild(new JoinGroupNode(new StatementPatternNode(term1, p1a, o1)));
            union.addChild(new JoinGroupNode(new StatementPatternNode(s1, p1b, term1)));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

//            if (context.getDescribeCache() != null) {
//
//                expected.setProjection(projection);
//                
//            }
            
            final IQueryNode actual = new ASTDescribeOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null)).
                    getQueryNode();

            assertSameAST(expected, actual);

        }

    }

    /**
     * <code>DESCRIBE *</code> is a short hand for all variables in the query.
     * 
     * <pre>
     * describe * where { x rdf:type foaf:Person }
     * </pre>
     */
    public void test_describeOptimizer_star() {

        final EmbergraphValueFactory f = store.getValueFactory();

        // Add some data.
        {

            final EmbergraphURI g = f.createURI("http://www.embergraph.org");
            
            final EmbergraphStatement[] stmts = new EmbergraphStatement[] {

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDF.TYPE.toString()),
                            f.createURI(FOAFVocabularyDecl.Person.toString()),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Mike"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Mike"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/Bryan"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("Bryan"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

                    new EmbergraphStatementImpl(
                            f.createURI("http://www.embergraph.org/DC"),
                            f.createURI(RDFS.LABEL.toString()),
                            f.createLiteral("DC"),
                            g, // context
                            StatementEnum.Explicit,
                            false// userFlag
                    ),

            };

            final StatementBuffer<EmbergraphStatement> buf = new StatementBuffer<EmbergraphStatement>(
                    store, 10/* capacity */);

            for (EmbergraphStatement stmt : stmts) {

                buf.add(stmt);

            }

            // write on the database.
            buf.flush();

        }

        final EmbergraphURI rdfType = f.createURI(RDF.TYPE.toString());

        final EmbergraphURI rdfsLabel = f.createURI(RDFS.LABEL.toString());

        final EmbergraphURI foafPerson = f.createURI(FOAFVocabularyDecl.Person
                .toString());

        final EmbergraphURI mikeURI = f
                .createURI("http://www.embergraph.org/Mike");

        final EmbergraphURI bryanURI = f
                .createURI("http://www.embergraph.org/Bryan");

        final EmbergraphLiteral mikeLabel = f.createLiteral("Mike");

        final EmbergraphLiteral bryanLabel = f.createLiteral("Bryan");

        final EmbergraphURI dcURI = f.createURI("http://www.embergraph.org/DC");

        final EmbergraphValue[] values = new EmbergraphValue[] { rdfType,
                rdfsLabel, foafPerson, mikeURI, bryanURI, mikeLabel,
                bryanLabel, dcURI };

        // resolve IVs.
        store.getLexiconRelation()
                .addTerms(values, values.length, true/* readOnly */);

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            projection.addProjectionVar(new VarNode("*"));

            final JoinGroupNode whereClause = new JoinGroupNode();
            queryRoot.setWhereClause(whereClause);

            whereClause.addChild(new StatementPatternNode(new VarNode("x"),
                    new ConstantNode(rdfType.getIV()), new ConstantNode(
                            foafPerson.getIV()), null/* c */,
                    Scope.DEFAULT_CONTEXTS));
        }

        /*
         * Setup the expected AST model.
         */
        final QueryRoot expected = new QueryRoot(QueryType.CONSTRUCT);
        {

            /*
             * Setup the projection necessary for maintaining the DESCRIBE
             * cache.
             */
            {

                final ProjectionNode projection = new ProjectionNode();
                expected.setProjection(projection);
                
                projection.setReduced(true);

                projection.addProjectionVar(new VarNode("x"));

            }
            
            /*
             * Setup the CONSTRUCT node.
             */
            // term1 ?p1a ?o1 .
            // ?s1 ?p1b term1 .
            final ConstructNode constructNode = new ConstructNode();
            expected.setConstruct(constructNode);

            // ?x
            final VarNode term0 = new VarNode("x");
            final VarNode p0a = new VarNode("p0a");
            final VarNode p0b = new VarNode("p0b");
            final VarNode o0 = new VarNode("o0");
            final VarNode s0 = new VarNode("s0");
            constructNode
                    .addChild(new StatementPatternNode(term0, p0a, o0));
            constructNode
                    .addChild(new StatementPatternNode(s0, p0b, term0));

            /*
             * Setup the WHERE clause.
             * 
             * Note: The original WHERE clause is preserved and we add a
             * union of two statement patterns for each term (variable or
             * constant) in the DESCRIBE clause.
             */
            // {
            // term1 ?p1a ?o1 .
            // } union {
            // ?s1 ?p1b term1 .
            // }
            final JoinGroupNode whereClause = new JoinGroupNode();
            expected.setWhereClause(whereClause);

            // original WHERE clause.
            whereClause.addChild(new StatementPatternNode(new VarNode("x"),
                    new ConstantNode(rdfType.getIV()), new ConstantNode(
                            foafPerson.getIV()), null/* c */,
                    Scope.DEFAULT_CONTEXTS));

            // union of 2 statement patterns per described term.
            final UnionNode union = new UnionNode();
            whereClause.addChild(union);

            union.addChild(new JoinGroupNode(new StatementPatternNode(term0, p0a, o0)));
            union.addChild(new JoinGroupNode(new StatementPatternNode(s0, p0b, term0)));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

//            if (context.getDescribeCache() != null) {
//
//                expected.setProjection(projection);
//                
//            }

            IQueryNode actual;
            
            actual = new ASTWildcardProjectionOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null))
                    .getQueryNode();
            
            actual = new ASTDescribeOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null))
                    .getQueryNode();

            assertSameAST(expected, actual);

        }

    }

    /**
     * This query is illegal since there are no variables to be described.
     * <pre>
     * describe *
     * </pre>
     */
    public void test_describeOptimizer_star_no_vars() {

        final QueryRoot queryRoot = new QueryRoot(QueryType.DESCRIBE);
        {

            final ProjectionNode projection = new ProjectionNode();
            queryRoot.setProjection(projection);

            projection.addProjectionVar(new VarNode("*"));

        }

        /*
         * Rewrite the query and verify that the expected AST was produced.
         */
        {

            final ASTContainer astContainer = new ASTContainer(queryRoot);
            
            final AST2BOpContext context = new AST2BOpContext(astContainer,
                    store);

            IQueryNode tmp = new ASTWildcardProjectionOptimizer().optimize(
                    context, new QueryNodeWithBindingSet(queryRoot, null))
                    .getQueryNode();

            try {
                new ASTDescribeOptimizer()
                    .optimize(context, new QueryNodeWithBindingSet(tmp, null));
                fail("Expecting " + RuntimeException.class);
            } catch (RuntimeException ex) {
                if (log.isInfoEnabled())
                    log.info("Ignoring expected exception: " + ex);
            }

        }

    }

}
