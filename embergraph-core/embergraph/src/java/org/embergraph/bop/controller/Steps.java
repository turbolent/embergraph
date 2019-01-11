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
 * Created on Aug 18, 2010
 */

package org.embergraph.bop.controller;

import java.util.Map;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;

/**
 * STEPS(ops)
 * 
 * <pre>
 * STEPS([],{subqueries=[a,b,c]})
 * </pre>
 * 
 * Will run the subqueries <i>a</i>, <i>b</i>, and <i>c</i> in sequence. Each
 * subquery will be initialized with a single empty {@link IBindingSet}. The
 * output of those subqueries will be routed to the STEPS operator (their
 * parent) unless the subqueries explicitly override this behavior using
 * {@link PipelineOp.Annotations#SINK_REF}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Steps extends AbstractSubqueryOp {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Deep copy constructor.
     */
    public Steps(Steps op) {
        super(op);
    }

    /**
     * Shallow copy constructor.
     * 
     * @param args
     * @param annotations
     */
    public Steps(final BOp[] args,
            final Map<String, Object> annotations) {

        super(args, annotations);

        if (getMaxParallelSubqueries() != 1) {
            /*
             * This version of the operator runs the subquery steps in a strict
             * sequence. This is appropriate for things like the fast closure
             * program, where some steps must execute in the given order while
             * others may execute concurrently. Use UNION if you want to have
             * concurrent evaluation of the subqueries.
             */
            throw new IllegalArgumentException(Annotations.MAX_PARALLEL_SUBQUERIES + "="
                    + getMaxParallelSubqueries());
        }
        

    }

    public Steps(final BOp[] args, NV... annotations) {

        this(args, NV.asMap(annotations));
        
    }

}
