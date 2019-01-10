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
 * Created on Apr 18, 2007
 */

package org.embergraph.relation.rule;

import java.util.Map;

import junit.framework.TestCase2;

import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.Var;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.NE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.test.MockTermIdFactory;

/**
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractRuleTestCase extends TestCase2 {
    
    /**
     * 
     */
    public AbstractRuleTestCase() {
    }

    /**
     * @param name
     */
    public AbstractRuleTestCase(String name) {
        super(name);
    }

    private static MockTermIdFactory f = new MockTermIdFactory();
    
    protected final static Constant<IV> rdfsSubClassOf = new Constant<IV>(
            f.newTermId(VTE.URI));
    
    protected final static Constant<IV> rdfsResource = new Constant<IV>(
            f.newTermId(VTE.URI));
    
    protected final static Constant<IV> rdfType = new Constant<IV>(
            f.newTermId(VTE.URI));
    
    protected final static Constant<IV> rdfsClass = new Constant<IV>(
            f.newTermId(VTE.URI));

    protected final static Constant<IV> rdfProperty = new Constant<IV>(
            f.newTermId(VTE.URI));

    /**
     * this is rdfs9:
     * 
     * <pre>
     * (?u,rdfs:subClassOf,?x), (?v,rdf:type,?u) -> (?v,rdf:type,?x)
     * </pre>
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    @SuppressWarnings("serial")
    static protected class TestRuleRdfs9 extends Rule {
        
        public TestRuleRdfs9(String relation) {
            
            super(  "rdfs9",//
                    new P(relation,var("v"), rdfType, var("x")), //
                    new IPredicate[] {//
                            new P(relation, var("u"), rdfsSubClassOf, var("x")),//
                            new P(relation, var("v"), rdfType, var("u")) //
                    },//
                    new IConstraint[] {
            			Constraint.wrap(new NE(var("u"),var("x")))
                        }
            );
            
        }

    }
    
    /**
     * rdfs4a:
     * 
     * <pre>
     * (?u ?a ?x) -&gt; (?u rdf:type rdfs:Resource)
     * </pre>
     * 
     * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
     * @version $Id$
     */
    @SuppressWarnings("serial")
    static protected class TestRuleRdfs04a extends Rule {

        public TestRuleRdfs04a(String relation) {

            super("rdfs4a",//
                    new P(relation,//
                            Var.var("u"), rdfType, rdfsResource), //
                    new IPredicate[] { //
                    new P(relation,//
                            Var.var("u"), Var.var("a"), Var.var("x")) //
                    },
                    /* constraints */
                    null);

        }

    }

    protected static class P extends SPOPredicate {

        /**
         * Required shallow copy constructor.
         */
        public P(final BOp[] values, final Map<String, Object> annotations) {
            super(values, annotations);
        }

        /**
         * Constructor required for {@link com.bigdata.bop.BOpUtility#deepCopy(FilterNode)}.
         */
        public P(final P op) {
            super(op);
        }

        /**
         * @param relation
         * @param s
         * @param p
         * @param o
         */
        public P(String relation, IVariableOrConstant<IV> s,
                IVariableOrConstant<IV> p, IVariableOrConstant<IV> o) {

//            super(relation, new IVariableOrConstant[] { s, p, o });
            super(relation, s, p, o );
            
        }
        
    }
    
    protected static class MyRule extends Rule {

        public MyRule( IPredicate head, IPredicate[] body) {

            super(MyRule.class.getName(), head, body, null/* constraints */);

        }

    }

}
