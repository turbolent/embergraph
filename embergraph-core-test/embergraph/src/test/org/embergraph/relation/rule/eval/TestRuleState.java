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
 * Created on Jun 19, 2008
 */

package org.embergraph.relation.rule.eval;

import java.util.Arrays;
import junit.framework.TestCase2;

import org.embergraph.bop.Constant;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.Var;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.QueryOptions;
import org.embergraph.relation.rule.Rule;

/**
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestRuleState extends TestCase2 {

    /**
     * 
     */
    public TestRuleState() {
    }

    /**
     * @param name
     */
    public TestRuleState(String name) {
        super(name);
    }

    public void test_requiredVars() {
        
        // select ?a where { ?a x ?b . ?b x ?c . ?c x ?d . ?d x ?e . }
        
        final IConstant x = new Constant<Integer>(1);
        
        final IPredicate[] tails = new IPredicate[] {
                new SPOPredicate("", Var.var("d"), x, Var.var("e")),
                new SPOPredicate("", Var.var("a"), x, Var.var("b")),
                new SPOPredicate("", Var.var("b"), x, Var.var("c")),
                new SPOPredicate("", Var.var("c"), x, Var.var("d")),
        };
        
        final IVariable[] requiredVars = new IVariable[] {
                Var.var("a")
        };
        
        final Rule rule = 
            new Rule("", null, tails, QueryOptions.NONE, null, null, null, requiredVars);
        
        final int[] order = new int[] {
                1,2,3,0
        };
        
        final RuleState2 ruleState = new RuleState2(rule, order);
        
        for (int i = 0; i < 4; i++) {
            System.err.println(Arrays.toString(ruleState.requiredVars[i]));
        }
        
    }
    
    protected class RuleState2 extends RuleState {
        
        protected final IVariable[][] requiredVars;
        
        public RuleState2(final IRule rule, final int[] order) {
            
            super(rule);
            
            this.requiredVars = computeRequiredVarsForEachTail(rule, order);
            
        }
        
    }
    
}
