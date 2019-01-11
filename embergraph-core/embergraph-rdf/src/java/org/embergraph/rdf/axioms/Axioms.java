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
 * Created on Mar 30, 2005
 */
package org.embergraph.rdf.axioms;

import java.util.Iterator;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.spo.SPO;


/**
 * Abstraction for a set of RDFS Axioms.
 * 
 * @author personickm
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface Axioms {
        
    /**
     * The namespace of the owning {@link LexiconRelation}.
     * @return
     */
    String getNamespace();
    
    /**
     * Test for an axiom.
     * 
     * @param s
     *            The internal value ({@link IV}) in the subject position.
     * @param p
     *            The internal value ({@link IV}) in the predicate position.
     * @param o
     *            The internal value ({@link IV}) in the object position.
     *            
     * @throws IllegalStateException
     *             if the axioms have not been defined.
     */
    boolean isAxiom(IV s, IV p, IV o);

    /**
     * The #of defined axioms.
     * 
     * @throws IllegalStateException
     *             if the axioms have not been defined.
     */
    int size();

    /**
     * The axioms in {s:p:o} order by their term identifiers.
     * 
     * @throws IllegalStateException
     *             if the axioms have not been defined.
     */
    Iterator<SPO> axioms();

    /**
     * <code>true</code> iff there are NO axioms.
     */
    boolean isNone();
    
    /**
     * <code>true</code> iff this set of axioms includes those for RDF Schema.
     */
    boolean isRdfSchema();
    
    /**
     * <code>true</code> iff this set of axioms includes those for
     * <code>owl:sameAs</code>, <code>owl:equivalentClass</code>, and
     * <code>owl:equivalentProperty</code>.
     */
    boolean isOwlSameAs();
    
}
