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
package org.embergraph.rdf.rules;

import org.embergraph.bop.IConstraint;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.NE;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.vocabulary.RDFS;

/**
 * rdfs5: this variant uses a nested subquery and may be safely used during truth maintenance.
 *
 * <pre>
 *        triple(?u,rdfs:subPropertyOf,?x) :-
 *           triple(?u,rdfs:subPropertyOf,?v),
 *           triple(?v,rdfs:subPropertyOf,?x).
 * </pre>
 */
public class RuleRdfs05 extends Rule {

  /** */
  private static final long serialVersionUID = -8059593032588823526L;

  public RuleRdfs05(String relationName, Vocabulary vocab) {

    super(
        "rdfs05",
        new SPOPredicate(relationName, var("u"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("x")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("u"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("v")),
          new SPOPredicate(relationName, var("v"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("x"))
        },
        new IConstraint[] {
          Constraint.wrap(new NE(var("u"), var("v"))), Constraint.wrap(new NE(var("v"), var("x")))
        });
  }
}
