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
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* rdfs3:
 *
 * <pre>
 *    triple(v rdf:type x) :-
 *       triple(a rdfs:range x),
 *       triple(u a v).
 * </pre>
 *
 * Note: Literals can be entailed in the subject position by this rule and MUST be explicitly
 * filtered out. That task is handled by the {@link DoNotAddFilter}. {@link RuleRdfs04b} is the
 * other way that literals can be entailed into the subject position.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleRdfs03 extends Rule {

  /** */
  private static final long serialVersionUID = -7163102986933704154L;

  public RuleRdfs03(String relationName, Vocabulary vocab) {

    super(
        "rdfs03",
        new SPOPredicate(relationName, var("v"), vocab.getConstant(RDF.TYPE), var("x")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("a"), vocab.getConstant(RDFS.RANGE), var("x")),
          new SPOPredicate(relationName, var("u"), var("a"), var("v"))
        },
        null // constraints
        );
  }
}
