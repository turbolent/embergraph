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
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;

/*
 * owl:hasValue
 *
 * <pre>
 *  (x rdf:type a), (a rdf:type owl:Restriction), (a owl:onProperty p), (a owl:hasValue v) -&gt; (x p v)
 * </pre>
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class RuleOwlHasValue extends Rule {

  /** */
  private static final long serialVersionUID = 274895423478542354L;

  /** @param vocab */
  public RuleOwlHasValue(String relationName, Vocabulary vocab) {

    super(
        "owl:hasValue",
        new SPOPredicate(relationName, var("x"), var("p"), var("v")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("x"), vocab.getConstant(RDF.TYPE), var("a")),
          new SPOPredicate(
              relationName,
              var("a"),
              vocab.getConstant(RDF.TYPE),
              vocab.getConstant(OWL.RESTRICTION)),
          new SPOPredicate(relationName, var("a"), vocab.getConstant(OWL.ONPROPERTY), var("p")),
          new SPOPredicate(relationName, var("a"), vocab.getConstant(OWL.HASVALUE), var("v"))
        },
        null // constraints
        );
  }
}
