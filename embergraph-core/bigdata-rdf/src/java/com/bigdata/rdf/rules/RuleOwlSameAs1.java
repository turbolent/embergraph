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
 * Created on Nov 1, 2007
 */

package com.bigdata.rdf.rules;

import org.openrdf.model.vocabulary.OWL;

import com.bigdata.bop.IConstraint;
import com.bigdata.bop.constraint.Constraint;
import com.bigdata.bop.constraint.NE;
import com.bigdata.rdf.spo.SPOPredicate;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.relation.rule.Rule;

/**
 * owl:sameAs1
 * 
 * <pre>
 * (x owl:sameAs y) -&gt; (y owl:sameAs x)
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleOwlSameAs1 extends Rule {

    /**
     * 
     */
    private static final long serialVersionUID = 81604260337012668L;

    /**
     * @param vocab
     */
    public RuleOwlSameAs1(String relationName, Vocabulary vocab) {

        super(  "owlSameAs1",//
                new SPOPredicate(relationName,var("y"), vocab.getConstant(OWL.SAMEAS), var("x")), //
                new SPOPredicate[] { //
                    new SPOPredicate(relationName,var("x"), vocab.getConstant(OWL.SAMEAS), var("y"))//
                },
                new IConstraint[] {
        			Constraint.wrap(new NE(var("x"),var("y")))
                }
                );
        
    }

}
