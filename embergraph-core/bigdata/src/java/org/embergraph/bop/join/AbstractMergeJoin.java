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
/*
 * Created on Nov 14, 2011
 */

package org.embergraph.bop.join;

import java.util.Map;

import com.bigdata.bop.BOp;
import com.bigdata.bop.NV;
import com.bigdata.bop.PipelineOp;

/**
 * Abstract base class for MERGE JOIN implementations.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class AbstractMergeJoin extends PipelineOp {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param args
     * @param annotations
     */
    public AbstractMergeJoin(BOp[] args, Map<String, Object> annotations) {
        
        super(args, annotations);
    }

    /**
     * @param op
     */
    public AbstractMergeJoin(AbstractMergeJoin op) {

        super(op);
        
    }

    public AbstractMergeJoin(final BOp[] args, NV... annotations) {

        this(args, NV.asMap(annotations));
        
    }

}
