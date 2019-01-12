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

import org.embergraph.bop.IConstraint;
import org.embergraph.bop.constraint.Constraint;
import org.embergraph.bop.constraint.NE;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

/**
 * owl:FunctionalProperty
 *
 * <pre>
 *   (p rdf:type owl:FunctionalProperty), (a p b), (a p c) -&gt;
 *   (b owl:sameAs c)
 * </pre>
 */
@SuppressWarnings("rawtypes")
public class RuleOwlFunctionalProperty extends Rule {

  /** */
  private static final long serialVersionUID = -6688762355076324400L;

  /** @param vocab */
  public RuleOwlFunctionalProperty(String relationName, Vocabulary vocab) {

    super(
        "owlFunctionalProperty",
        new SPOPredicate(relationName, var("b"), vocab.getConstant(OWL.SAMEAS), var("c")),
        new SPOPredicate[] {
          new SPOPredicate(
              relationName,
              var("x"),
              vocab.getConstant(RDF.TYPE),
              vocab.getConstant(OWL.FUNCTIONALPROPERTY)),
          new SPOPredicate(relationName, var("a"), var("x"), var("b")),
          new SPOPredicate(relationName, var("a"), var("x"), var("c"))
        },
        new IConstraint[] {Constraint.wrap(new NE(var("b"), var("c")))});
  }

  //    /**
  //     * If this rule ever becomes consistent in the data then the rule will
  //     * throw a {@link ConstraintViolationException} and the closure operation
  //     * will fail.
  //     */
  //    @Override
  //	public boolean isConsistent(final IBindingSet bset) {
  //
  //		boolean ret = super.isConsistent(bset);
  //
  //		if (ret && isFullyBound(bset)) {
  //
  //			throw new ConstraintViolationException(getName());
  //
  //		}
  //
  //		return ret;
  //
  //	}

}
