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
 * Created on Nov 3, 2011
 */

package org.embergraph.bop.join;

import org.embergraph.bop.HTreeAnnotations;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.htree.HTree;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValue;

/**
 * Annotations in common for {@link HTree} based hash joins.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface HTreeHashJoinAnnotations extends HTreeAnnotations,
        HashJoinAnnotations, JoinAnnotations {

    /**
     * The namespace of the lexicon relation.
     * <p>
     * Note: This is necessary for the ivCache index used to optimize the
     * serialized representation of the solution {@link IV}s. We need to know
     * how to serialize and deserialize {@link EmbergraphValue}s from the ivCache.
     * 
     * @see Predicate.Annotations#RELATION_NAME
     */
    String RELATION_NAME = Predicate.Annotations.RELATION_NAME;
    
}
