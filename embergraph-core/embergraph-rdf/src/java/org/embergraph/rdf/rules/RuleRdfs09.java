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
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* rdfs9:
 *
 * <pre>
 *       triple(?v,rdf:type,?x) :-
 *          triple(?u,rdfs:subClassOf,?x),
 *          triple(?v,rdf:type,?u).
 * </pre>
 */
public class RuleRdfs09 extends Rule {

  /** */
  private static final long serialVersionUID = 6301379050758674236L;

  public RuleRdfs09(String relationName, Vocabulary vocab) {

    super(
        "rdfs09",
        new SPOPredicate(relationName, var("v"), vocab.getConstant(RDF.TYPE), var("x")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("u"), vocab.getConstant(RDFS.SUBCLASSOF), var("x")),
          new SPOPredicate(relationName, var("v"), vocab.getConstant(RDF.TYPE), var("u"))
        },
        new IConstraint[] {Constraint.wrap(new NE(var("u"), var("x")))});
  }
}
