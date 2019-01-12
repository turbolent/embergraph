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
 * Created on Nov 11, 2007
 */

package org.embergraph.rdf.spo;

import org.embergraph.rdf.inf.Justification;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.accesspath.AbstractElementBuffer.InsertBuffer;
import org.embergraph.relation.rule.Rule;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.relation.rule.eval.AbstractSolutionBuffer.InsertSolutionBuffer;

/**
 * A buffer that is written on by {@link Rule}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @depreated by {@link InsertBuffer} and {@link InsertSolutionBuffer}
 */
public interface ISPOAssertionBuffer extends ISPOBuffer {

    /**
     * The #of justifications currently in the buffer.
     * 
     * @deprecated not used.
     */
    public int getJustificationCount();

    /**
     * Add a statement and an optional justification to the buffer.
     * 
     * @deprecated by {@link ISolution}s in an {@link IBuffer}.
     */
    public boolean add(SPO stmt, Justification justification);

}
