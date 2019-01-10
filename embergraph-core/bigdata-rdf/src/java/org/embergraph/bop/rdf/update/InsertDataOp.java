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
 * Created on Mar 12, 2012
 */

package org.embergraph.bop.rdf.update;

import java.util.Map;
import java.util.concurrent.FutureTask;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.PipelineOp;
import org.embergraph.rdf.spo.ISPO;

/**
 * Operator to insert {@link ISPO}s into bigdata.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class InsertDataOp extends PipelineOp {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param op
     */
    public InsertDataOp(PipelineOp op) {
        super(op);
    }

    /**
     * @param args
     * @param annotations
     */
    public InsertDataOp(BOp[] args, Map<String, Object> annotations) {
        super(args, annotations);
    }

    @Override
    public FutureTask<Void> eval(BOpContext<IBindingSet> context) {
        // TODO Auto-generated method stub
        return null;
    }

}
