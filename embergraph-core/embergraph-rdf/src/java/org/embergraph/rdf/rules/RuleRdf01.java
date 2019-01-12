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

import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.openrdf.model.vocabulary.RDF;

/*
 * rdf1:
 *
 * <pre>
 * (?a rdf:type rdf:Property) :- ( ?u ?a ?y ).
 * </pre>
 *
 * Note: This rule is evaluated using a {@link DistinctTermScan}. The variables that DO NOT appear
 * in the head of the rule remain unbound in the generated solutions (?u and ?y). When
 * justifications are generated, those unbound variables will be represented as ZERO (0L)s and
 * interpreted as wildcards.
 */
public class RuleRdf01 extends AbstractRuleDistinctTermScan {

  /** */
  private static final long serialVersionUID = -7423082674586471243L;

  public RuleRdf01(String relationName, Vocabulary vocab) {

    super(
        "rdf01",
        new SPOPredicate(
            relationName, var("a"), vocab.getConstant(RDF.TYPE), vocab.getConstant(RDF.PROPERTY)),
        new SPOPredicate[] {new SPOPredicate(relationName, var("u"), var("a"), var("y"))},
        null // constraints
        );
  }
}
