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
package org.embergraph.rdf.rules;

import org.openrdf.model.vocabulary.RDFS;

import org.embergraph.bop.IConstraint;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.NE;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/**
 * rdfs7:
 * <pre>
 *       triple(?u,?b,?y) :-
 *          triple(?a,rdfs:subPropertyOf,?b),
 *          triple(?u,?a,?y).
 * </pre>
 */
public class RuleRdfs07 extends Rule  {

    /**
     * 
     */
    private static final long serialVersionUID = -4218605684644812934L;

    public RuleRdfs07( String relationName, Vocabulary vocab) {

        super( "rdfs07",//
                new SPOPredicate(relationName,var("u"), var("b"), var("y")),//
                new SPOPredicate[] {//
                    new SPOPredicate(relationName,var("a"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("b")),//
                    new SPOPredicate(relationName,var("u"), var("a"), var("y"))//
                },
                new IConstraint[] {
        			Constraint.wrap(new NE(var("a"),var("b")))
                }
        );

    }

}
