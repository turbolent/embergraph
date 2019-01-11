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

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.vocab.Vocabulary;
import org.embergraph.relation.rule.Rule;

/**
 * rdfs13:
 * 
 * <pre>
 *  (?u rdfs:subClassOf rdfs:Literal) :-
 *     (?u rdf:type rdfs:Datatype).
 * </pre>
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RuleRdfs13 extends Rule {

    /**
     * 
     */
    private static final long serialVersionUID = -1221118582286314313L;

    public RuleRdfs13(String relationName, Vocabulary vocab) {

        super("rdfs13", //
                new SPOPredicate(relationName, var("u"), vocab.getConstant(RDFS.SUBCLASSOF), vocab.getConstant(RDFS.LITERAL)),//
                new SPOPredicate[] { //
                    new SPOPredicate(relationName, var("u"), vocab.getConstant(RDF.TYPE), vocab.getConstant(RDFS.DATATYPE)) //
                },//
                null // constraints
        );

    }
    
}
