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
import com.bigdata.bop.constraint.NEConstant;
import com.bigdata.rdf.spo.SPOPredicate;
import com.bigdata.rdf.vocab.Vocabulary;
import com.bigdata.relation.rule.Rule;


/**
 * owl:sameAs3
 * 
 * <pre>
 * (x owl:sameAs y), (z a x) -&gt; (z a y).
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleOwlSameAs3 extends Rule {

    /**
     * 
     */
    private static final long serialVersionUID = -8182152612151810691L;

    public RuleOwlSameAs3(String relationName, Vocabulary vocab) {

        super(  "owlSameAs3", //
                new SPOPredicate(relationName,var("z"), var("a"), var("y")), //
                new SPOPredicate[] { //
                    new SPOPredicate(relationName,var("x"), vocab.getConstant(OWL.SAMEAS), var("y")),//
                    new SPOPredicate(relationName,var("z"), var("a"), var("x"))//
                },
                new IConstraint[] {
                    /*
                     * Reject (z sameAs y) as the head.
                     */
//                    new RejectAnythingSameAsItself(var("z"),var("a"),var("y"),vocab.getConstant(OWL.SAMEAS))
        			Constraint.wrap(new NEConstant(var("a"),vocab.getConstant(OWL.SAMEAS))),
        			Constraint.wrap(new NE(var("x"),var("y")))
                }
        );

    }

}
