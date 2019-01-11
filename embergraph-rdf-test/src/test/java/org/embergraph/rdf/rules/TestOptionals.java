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
 * Created on March 11, 2008
 */

package org.embergraph.rdf.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.Var;
import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.bop.joinGraph.IEvaluationPlanFactory;
import org.embergraph.bop.joinGraph.fast.DefaultEvaluationPlanFactory2;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.Rule;
import org.embergraph.relation.rule.eval.ActionEnum;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.IJoinNexusFactory;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.striterator.IChunkedOrderedIterator;

/**
 * @author <a href="mailto:mpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestOptionals extends AbstractInferenceEngineTestCase {

    /**
     * 
     */
    public TestOptionals() {
        super();
    }

    /**
     * @param name
     */
    public TestOptionals(String name) {
        super(name);
    }
    
//    public void test_optionals_nextedSubquery() 
//    {
//     
//        final Properties p = new Properties(getProperties());
//
//        p.setProperty(AbstractRelation.Options.NESTED_SUBQUERY, "true");
//        
//        doOptionalsTest(p);
//
//    } 
    
    public void test_optionals_pipeline() 
    {
     
        final Properties p = new Properties(getProperties());

//        p.setProperty(AbstractRelation.Options.NESTED_SUBQUERY, "false");
        
        doOptionalsTest(p);
        
    } 
    
    protected void doOptionalsTest(final Properties p) {
        
        // store with no owl:sameAs closure
        AbstractTripleStore db = getStore(p);
        
        try {

            final Map<Value, IV> termIds = new HashMap<Value, IV>();
            
            final URI A = new URIImpl("http://www.embergraph.org/A");
            final URI B = new URIImpl("http://www.embergraph.org/B");
//            final URI C = new URIImpl("http://www.bigdata.com/C");
//            final URI D = new URIImpl("http://www.bigdata.com/D");
//            final URI E = new URIImpl("http://www.bigdata.com/E");

//            final URI V = new URIImpl("http://www.bigdata.com/V");
            final URI W = new URIImpl("http://www.embergraph.org/W");
            final URI X = new URIImpl("http://www.embergraph.org/X");
            final URI Y = new URIImpl("http://www.embergraph.org/Y");
            final URI Z = new URIImpl("http://www.embergraph.org/Z");

            final Literal foo = new LiteralImpl("foo");
            final Literal bar = new LiteralImpl("bar");

            {
                StatementBuffer buffer = new StatementBuffer
                    ( db, 100/* capacity */
                      );

                buffer.add(A, RDF.TYPE, X);
                buffer.add(A, RDFS.LABEL, foo);
                buffer.add(A, RDFS.COMMENT, bar);
                buffer.add(B, RDF.TYPE, X);
                // buffer.add(B, RDFS.LABEL, foo);
                buffer.add(B, RDFS.COMMENT, bar);
                
                // write statements on the database.
                buffer.flush();
                
//                // database at once closure.
//                db.getInferenceEngine().computeClosure(null/*focusStore*/);

                // write on the store.
//                buffer.flush();
            }
            
            final IV a = db.getIV(A); termIds.put(A, a);
            final IV b = db.getIV(B); termIds.put(B, b);
//            final IV c = noClosure.getTermId(C);
//            final IV d = noClosure.getTermId(D);
//            final IV e = noClosure.getTermId(E);
//            final IV v = noClosure.getTermId(V);
            // final IV w = db.getTermId(W); termIds.put(W, w);
            final IV x = db.getIV(X); termIds.put(X, x);
            // final IV y = db.getTermId(Y); termIds.put(Y, y);
            // final IV z = db.getTermId(Z); termIds.put(Z, z);
            // final IV SAMEAS = db.getTermId(OWL.SAMEAS); termIds.put(OWL.SAMEAS, SAMEAS);
            termIds.put(foo, db.getIV(foo));
            termIds.put(bar, db.getIV(bar));
            final IV TYPE = db.getIV(RDF.TYPE); termIds.put(RDF.TYPE, TYPE);
            final IV LABEL = db.getIV(RDFS.LABEL); termIds.put(RDFS.LABEL, LABEL);
            final IV COMMENT = db.getIV(RDFS.COMMENT); termIds.put(RDFS.COMMENT, COMMENT);
            final IV RESOURCE = db.getIV(RDFS.RESOURCE); termIds.put(RDFS.RESOURCE, RESOURCE);
            
            if (log.isInfoEnabled())
                log.info("\n" +db.dumpStore(true, true, false));
  
            for (Map.Entry<Value, IV> e : termIds.entrySet()) {
                System.err.println(e.getKey() + " = " + e.getValue());
            }
            
            { // works great
                
                final String SPO = db.getSPORelation().getNamespace();
                final IVariableOrConstant<IV> s = Var.var("s");
                final IVariableOrConstant<IV> type = new Constant<IV>(TYPE);
                final IVariableOrConstant<IV> t = new Constant<IV>(x);
                final IVariableOrConstant<IV> label = new Constant<IV>(LABEL);
                final IVariableOrConstant<IV> comment = new Constant<IV>(COMMENT);
                final IVariableOrConstant<IV> l = Var.var("l");
                final IVariableOrConstant<IV> c = Var.var("c");
                final IRule rule =
                        new Rule("test_optional", null, // head
                                new IPredicate[] {
                                        new SPOPredicate(SPO, s, type, t),
                                        new SPOPredicate(SPO, s, comment, c),
                                        new SPOPredicate(SPO, s, label, l, true) },
                                // constraints on the rule.
                                null
                                );
                
                try {
                
                    int numSolutions = 0;
                    
                    IChunkedOrderedIterator<ISolution> solutions = runQuery(db, rule);
                    
                    while (solutions.hasNext()) {
                        
                        ISolution solution = solutions.next();
                        
                        System.err.println(solution);
                        
                        numSolutions++;
                        
                    }
                    // Note: Fails stochastically for nested subquery joins.
                    assertEquals("wrong # of solutions", 2, numSolutions);
                    
                } catch(Exception ex) {
                    
                    ex.printStackTrace();
                    
                }
            }

            { // does not work, only difference is the order the heads are
              // presented in the rule definition.  Note that the plan still gets
              // the execution order correct.  (See output from line 429, should
              // be [2,0,1], and it is.
            
                final String SPO = db.getSPORelation().getNamespace();
                final IVariableOrConstant<IV> s = Var.var("s");
                final IVariableOrConstant<IV> type = new Constant<IV>(TYPE);
                final IVariableOrConstant<IV> t = new Constant<IV>(x);
                final IVariableOrConstant<IV> label = new Constant<IV>(LABEL);
                final IVariableOrConstant<IV> comment = new Constant<IV>(COMMENT);
                final IVariableOrConstant<IV> l = Var.var("l");
                final IVariableOrConstant<IV> c = Var.var("c");
                final IRule rule =
                        new Rule("test_optional", null, // head
                                new IPredicate[] {
                                        new SPOPredicate(SPO, s, type, t),
                                        new SPOPredicate(SPO, s, label, l, true),
                                        new SPOPredicate(SPO, s, comment, c) },
                                // constraints on the rule.
                                null
                                );
                
                try {
                
                    int numSolutions = 0;
                    
                    IChunkedOrderedIterator<ISolution> solutions = runQuery(db, rule);
                    
                    while (solutions.hasNext()) {
                        
                        ISolution solution = solutions.next();
                        
                        System.err.println(solution);
                        
                        numSolutions++;
                        
                    }
                    
                    assertEquals("wrong # of solutions", 2, numSolutions);
                    
                } catch(Exception ex) {
                    
                    ex.printStackTrace();
                    
                }
            }

            { // two optionals does not work either
            
                final String SPO = db.getSPORelation().getNamespace();
                final IVariableOrConstant<IV> s = Var.var("s");
                final IVariableOrConstant<IV> type = new Constant<IV>(TYPE);
                final IVariableOrConstant<IV> t = new Constant<IV>(x);
                final IVariableOrConstant<IV> label = new Constant<IV>(LABEL);
                final IVariableOrConstant<IV> comment = new Constant<IV>(COMMENT);
                final IVariableOrConstant<IV> l = Var.var("l");
                final IVariableOrConstant<IV> c = Var.var("c");
                final IRule rule =
                        new Rule("test_optional", null, // head
                                new IPredicate[] {
                                        new SPOPredicate(SPO, s, type, t),
                                        new SPOPredicate(SPO, s, label, l, true),
                                        new SPOPredicate(SPO, s, comment, c, true) },
                                // constraints on the rule.
                                null
                                );
                
                try {
                
                    int numSolutions = 0;
                    
                    IChunkedOrderedIterator<ISolution> solutions = runQuery(db, rule);
                    
                    while (solutions.hasNext()) {
                        
                        ISolution solution = solutions.next();
                        
                        System.err.println(solution);
                        
                        numSolutions++;
                        
                    }
                    
                    assertEquals("wrong # of solutions", 2, numSolutions);
                    
                } catch(Exception ex) {
                    
                    ex.printStackTrace();
                    
                }
            }

        } finally {
            
            db.__tearDownUnitTest();
            
        }
        
    }

    private IChunkedOrderedIterator<ISolution> runQuery(AbstractTripleStore db, IRule rule)
        throws Exception {
        // run the query as a native rule.
        final IEvaluationPlanFactory planFactory =
                DefaultEvaluationPlanFactory2.INSTANCE;
        final IJoinNexusFactory joinNexusFactory =
                db.newJoinNexusFactory(RuleContextEnum.HighLevelQuery,
                        ActionEnum.Query, IJoinNexus.BINDINGS, null, // filter
                        false, // justify 
                        false, // backchain
                        planFactory);
        final IJoinNexus joinNexus =
                joinNexusFactory.newInstance(db.getIndexManager());
        final IEvaluationPlan plan = planFactory.newPlan(joinNexus, rule);
        StringBuilder sb = new StringBuilder();
        int order[] = plan.getOrder();
        for (int i = 0; i < order.length; i++) {
            sb.append(order[i]);
            if (i < order.length-1) {
                sb.append(",");
            }
        }
        System.err.println("order: [" + sb.toString() + "]");
        IChunkedOrderedIterator<ISolution> solutions = joinNexus.runQuery(rule);
        return solutions;
    }

}
