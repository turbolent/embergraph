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
 * rdfs2:
 *
 * <pre>
 * ( u rdf:type x) :- ( a rdfs:domain x), ( u a y ).
 * </pre>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleRdfs02 extends Rule {

  /** */
  private static final long serialVersionUID = 3378266702848919291L;

  public RuleRdfs02(String relationName, Vocabulary vocab) {

    super(
        "rdfs02",
        new SPOPredicate(relationName, var("u"), vocab.getConstant(RDF.TYPE), var("x")),
        new SPOPredicate[] {
          new SPOPredicate(relationName, var("a"), vocab.getConstant(RDFS.DOMAIN), var("x")),
          new SPOPredicate(relationName, var("u"), var("a"), var("y"))
        },
        null // constraints
        );
  }
}
