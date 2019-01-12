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
 * Created on Nov 1, 2007
 */
package org.embergraph.rdf.rules;

import org.openrdf.model.vocabulary.OWL;

import org.embergraph.bop.IConstraint;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.NE;
import org.embergraph.bop.constraint.NEConstant;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/**
 * owl:sameAs2
 * 
 * <pre>
 * (x owl:sameAs y), (x a z) -&gt; (y a z).
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleOwlSameAs2 extends Rule  {

    /**
     * 
     */
    private static final long serialVersionUID = -8016648183305295727L;

    /**
     * @param vocab
     */
    public RuleOwlSameAs2(String relationName, Vocabulary vocab) {

        super(  "owlSameAs2",
                new SPOPredicate(relationName,var("y"), var("a"), var("z")),
                new SPOPredicate[] {
                    new SPOPredicate(relationName,var("x"), vocab.getConstant(OWL.SAMEAS), var("y")),
                    new SPOPredicate(relationName,var("x"), var("a"), var("z"))
                },
                new IConstraint[]{
                    /*
                     * Reject (y sameAs z) as the head.
                     */
//                    new RejectAnythingSameAsItself(var("y"),var("a"),var("z"),vocab.getConstant(OWL.SAMEAS))
        			Constraint.wrap(new NEConstant(var("a"),vocab.getConstant(OWL.SAMEAS))),
        			Constraint.wrap(new NE(var("x"),var("y")))
                }
        );
       
    }

}
