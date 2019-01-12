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
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/**
 * owl:sameAs1b
 * 
 * <pre>
 * (x owl:sameAs y), (y owl:sameAs z) -&gt; (x owl:sameAs z)
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleOwlSameAs1b extends Rule
{

    /**
     * 
     */
    private static final long serialVersionUID = -9190200251803516786L;

    /**
     * @param vocab
     */
    public RuleOwlSameAs1b(String relationName, Vocabulary vocab) {

        super(  "owlSameAs1b",
                new SPOPredicate(relationName,var("x"), vocab.getConstant(OWL.SAMEAS), var("z")),
                new SPOPredicate[] {
                    new SPOPredicate(relationName,var("x"), vocab.getConstant(OWL.SAMEAS), var("y")),
                    new SPOPredicate(relationName,var("y"), vocab.getConstant(OWL.SAMEAS), var("z"))
                }, new IConstraint[] {
        			Constraint.wrap(new NE(var("x"),var("y"))),
        			Constraint.wrap(new NE(var("y"),var("z"))),
					Constraint.wrap(new NE(var("x"),var("z"))),
                    }
                );
        
    }

}
