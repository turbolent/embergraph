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
 * Created on Aug 25, 2010
 */

package org.embergraph.bop.bset;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.NV;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.BOpStats;
import org.embergraph.bop.engine.IChunkAccessor;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.ThickAsynchronousIterator;

import cutthecrap.utils.striterators.ICloseableIterator;

/**
 * This operator copies its source to its sink(s). Specializations exist which are
 * used to feed the the initial set of intermediate results into a pipeline (
 * {@link StartOp}) and which are used to replicate intermediate results to more
 * than one sink ({@link Tee}).
 * 
 * @see Annotations#SINK_REF
 * @see Annotations#ALT_SINK_REF
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class CopyOp extends PipelineOp {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public interface Annotations extends PipelineOp.Annotations {

        /**
         * An optional {@link IVariable}[] which specifies which variables will
         * have their bindings copied.
         */
        String SELECT = CopyOp.class.getName() + ".select";

        /**
         * An optional {@link IConstraint}[] which places restrictions on the
         * legal patterns in the variable bindings.
         */
        String CONSTRAINTS = CopyOp.class.getName() + ".constraints";

        /**
         * An optional {@link IBindingSet}[] to be used <strong>instead</strong>
         * of the default source.
         */
        String BINDING_SETS = CopyOp.class.getName() + ".bindingSets";
        
    }

    /**
     * Deep copy constructor.
     * 
     * @param op
     */
    public CopyOp(CopyOp op) {
        super(op);
    }

    /**
     * Shallow copy constructor.
     * 
     * @param args
     * @param annotations
     */
    public CopyOp(BOp[] args, Map<String, Object> annotations) {
        super(args, annotations);
    }

    public CopyOp(final BOp[] args, NV... annotations) {

        this(args, NV.asMap(annotations));
        
    }

    /**
     * @see Annotations#SELECT
     */
    public IVariable<?>[] getSelect() {

        return getProperty(Annotations.SELECT, null/* defaultValue */);

    }

    /**
     * @see Annotations#CONSTRAINTS
     */
    public IConstraint[] constraints() {

        return getProperty(Annotations.CONSTRAINTS, null/* defaultValue */);

    }

    /**
     * @see Annotations#BINDING_SETS
     */
    public IBindingSet[] bindingSets() {

        return getProperty(Annotations.BINDING_SETS, null/* defaultValue */);

    }

    public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

        return new FutureTask<Void>(new CopyTask(this, context));

    }

    /**
     * Copy the source to the sink.
     * 
     * @todo Optimize this. When using an {@link IChunkAccessor} we should be
     *       able to directly output the same chunk.
     */
    static private class CopyTask implements Callable<Void> {

        private final CopyOp op;

        private final BOpContext<IBindingSet> context;

        CopyTask(final CopyOp op,
                final BOpContext<IBindingSet> context) {

            this.op = op;
            
            this.context = context;

        }

        public Void call() throws Exception {

            // source.
            final ICloseableIterator<IBindingSet[]> source = context
                    .getSource();

            // default sink
            final IBlockingBuffer<IBindingSet[]> sink = context.getSink();
            
            // optional altSink.
            final IBlockingBuffer<IBindingSet[]> sink2 = context.getSink2();
            
            final BOpStats stats = context.getStats();

            final IVariable<?>[] select = op.getSelect();

            final IConstraint[] constraints = op.constraints();

            try {

                final IBindingSet[] bindingSets = op.bindingSets();

                if (bindingSets != null) {

                    // copy optional additional binding sets.
                    BOpUtility.copy(
                            new ThickAsynchronousIterator<IBindingSet[]>(
                                    new IBindingSet[][] { bindingSets }), sink,
                            sink2, null/* mergeSolution */, select,
                            constraints, stats);

                } else {

                    // copy binding sets from the source.
                    BOpUtility
                            .copy(source, sink, sink2, null/* mergeSolution */,
                                    select, constraints, stats);

                }
                
                // flush the sink.
                sink.flush();
                if (sink2 != null) // and the optional altSink.
                    sink2.flush();

                // Done.
                return null;
                
            } finally {
                
                sink.close();
                
                if (sink2 != null)
                    sink2.close();
                
                source.close();
                
            }

        }

    } // class CopyTask

}
