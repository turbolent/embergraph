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
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/**
 * Rule for step 13 of {@link InferenceEngine#fastForwardClosure()}.
 *
 * <pre>
 * (?z, rdf:type, ?b ) :-
 *       (?x, ?y, ?z),
 *       (?y, rdfs:subPropertyOf, ?a),
 *       (?a, rdfs:range, ?b ).
 * </pre>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleFastClosure13 extends AbstractRuleFastClosure_11_13 {

  /** */
  private static final long serialVersionUID = -2159515669069118668L;

  /** @param vocab */
  public RuleFastClosure13(String relationName, Vocabulary vocab) {

    super(
        "fastClosure13",
        new SPOPredicate(relationName, var("z"), vocab.getConstant(RDF.TYPE), var("b")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("x"), var("y"), var("z")),
          new SPOPredicate(relationName, var("y"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("a")),
          new SPOPredicate(relationName, var("a"), vocab.getConstant(RDFS.RANGE), var("b"))
        },
        new IConstraint[] {Constraint.wrap(new NE(var("y"), var("a")))});
  }
}
