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
 * Created on Mar 16, 2012
 */

package org.embergraph.bop.rdf.update;

import java.beans.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpContext;
import org.embergraph.bop.IBindingSet;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVCache;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.store.AbstractTripleStore;

/**
 * Vectored insert operator for RDF Statements. The solutions flowing through
 * this operator MUST bind the <code>s</code>, <code>p</code>, <code>o</code>,
 * and (depending on the database mode) MAY bind the <code>c</code> variable.
 * Those variables correspond to the Subject, Predicate, Object, and
 * Context/Graph position of an RDF {@link Statement} respectively. On input,
 * the variables must be real {@link IV}s. The {@link IVCache} does NOT need to
 * be set. The output is an empty solution.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public final class InsertStatementsOp extends AbstractAddRemoveStatementsOp {

    public InsertStatementsOp(final BOp[] args,
            final Map<String, Object> annotations) {

        super(args, annotations);

    }

    public InsertStatementsOp(final InsertStatementsOp op) {
        super(op);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public FutureTask<Void> eval(final BOpContext<IBindingSet> context) {

        return new FutureTask<Void>(new ChunkTask(context, this));

    }

    static private class ChunkTask implements Callable<Void> {

        private final BOpContext<IBindingSet> context;

        private final AbstractTripleStore tripleStore;

        private final boolean sids;

        private final boolean quads;

        public ChunkTask(final BOpContext<IBindingSet> context,
                final InsertStatementsOp op) {

            this.context = context;

            final String namespace = ((String[]) op
                    .getRequiredProperty(Annotations.RELATION_NAME))[0];

            final long timestamp = (Long) op
                    .getRequiredProperty(Annotations.TIMESTAMP);

            this.tripleStore = (AbstractTripleStore) context.getResource(
                    namespace, timestamp);

            this.sids = tripleStore.isStatementIdentifiers();

            this.quads = tripleStore.isQuads();

        }

        @Override
        public Void call() throws Exception {

            final boolean bindsC = sids | quads;

            // Build set of distinct ISPOs.
            final Set<ISPO> b = acceptSolutions(context, bindsC);

            // Convert into array.
            final ISPO[] stmts = b.toArray(new ISPO[b.size()]);

            // Write on the database.
            final long nmodified = tripleStore.addStatements(stmts,
                    stmts.length);

            // Increment by the #of statements written.
            context.getStats().mutationCount.add(nmodified);
            
            // done.
            return null;

        }

    } // ChunkTask

} // InsertStatements
