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

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/**
 * rdfs6:
 * <pre>
 * triple( ?u rdfs:subPropertyOf ?u ) :-
 *    triple( ?u rdf:type rdf:Property ). 
 * </pre>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleRdfs06 extends Rule {

    /**
     * 
     */
    private static final long serialVersionUID = -8553569740061410325L;

    public RuleRdfs06(String relationName, Vocabulary vocab) {

        super(  "rdfs06",//
                new SPOPredicate(relationName,var("u"), vocab.getConstant(RDFS.SUBPROPERTYOF), var("u")),//
                new SPOPredicate[] {//
                    new SPOPredicate(relationName,var("u"), vocab.getConstant(RDF.TYPE), vocab.getConstant(RDF.PROPERTY)) //
                },//
                null // constraints
                );

    }
    
}
