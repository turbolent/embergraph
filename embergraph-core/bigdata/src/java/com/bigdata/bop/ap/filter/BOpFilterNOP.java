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
 * Created on Sep 28, 2010
 */

package com.bigdata.bop.ap.filter;

import java.util.Iterator;
import java.util.Map;

import com.bigdata.bop.BOp;

/**
 * A filter which does nothing. This may be used to stack existing
 * {@link BOpFilterBase} operators.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BOpFilterNOP extends BOpFilterBase {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

//    /**
//     * Deserialization.
//     */
//    public BOpFilterNOP() {
//        super();
//    }
    
    /**
     * Deep copy.
     * 
     * @param op
     */
    public BOpFilterNOP(BOpFilterNOP op) {
        super(op);
    }

    /**
     * Shallow copy.
     * 
     * @param args
     * @param annotations
     */
    public BOpFilterNOP(BOp[] args, Map<String, Object> annotations) {
        super(args, annotations);
    }

    @Override
    final protected Iterator filterOnce(Iterator src, Object context) {
        return src;
    }

}
