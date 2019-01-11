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
 * Created on Nov 5, 2007
 */

package org.embergraph.rdf.inf;

import java.util.Map;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.changesets.StatementWriter;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.BigdataBNode;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.accesspath.AbstractElementBuffer.DeleteBuffer;
import org.embergraph.relation.rule.eval.AbstractSolutionBuffer.DeleteSolutionBuffer;
import org.embergraph.striterator.ChunkedArrayIterator;

/**
 * A buffer for {@link SPO}s which causes the corresponding statements (and
 * their {@link Justification}s) be retracted from the database when it is
 * {@link #flush()}ed.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @deprecated by {@link DeleteBuffer} and {@link DeleteSolutionBuffer} and the
 *             changes to how truth maintenance is handled (rule rewrites).
 */
public class SPORetractionBuffer extends AbstractSPOBuffer {

    private final AbstractTripleStore store;
    private final boolean computeClosureForStatementIdentifiers;

    /**
     * Optional change log for change notification.
     */
    protected final IChangeLog changeLog;
    
    /**
     * @param store
     *            The database from which the statement will be removed when the
     *            buffer is {@link #flush()}ed.
     * @param capacity
     *            The capacity of the retraction buffer.
     * @param computeClosureForStatementIdentifiers
     *            See
     *            {@link AbstractTripleStore#removeStatements(org.embergraph.rdf.spo.ISPOIterator, boolean)}
     */
    public SPORetractionBuffer(AbstractTripleStore store, int capacity,
            boolean computeClosureForStatementIdentifiers) {
        
        this(store, capacity, computeClosureForStatementIdentifiers,
                null/* changeLog */);
        
    }
        
    /**
     * @param store
     *            The database from which the statement will be removed when the
     *            buffer is {@link #flush()}ed.
     * @param capacity
     *            The capacity of the retraction buffer.
     * @param computeClosureForStatementIdentifiers
     *            See
     *            {@link AbstractTripleStore#removeStatements(org.embergraph.rdf.spo.ISPOIterator, boolean)}
     * @param changeLog
     *            optional change log for change notification
     */
    public SPORetractionBuffer(AbstractTripleStore store, int capacity,
            boolean computeClosureForStatementIdentifiers,
            final IChangeLog changeLog) {
        
        super(store, null/*filter*/, capacity);
        
        if (store == null)
            throw new IllegalArgumentException();

        this.store = store;
        
        this.computeClosureForStatementIdentifiers = computeClosureForStatementIdentifiers;
        
        this.changeLog = changeLog;
        
    }

    public int flush() {

        if (isEmpty()) return 0;
        
        final long n;
        
        if (changeLog == null) {

            n = store.removeStatements(new ChunkedArrayIterator<ISPO>(numStmts,stmts,
                null/*keyOrder*/), computeClosureForStatementIdentifiers);
            
        } else {
            
            n = StatementWriter.removeStatements(
                    store, 
                    new ChunkedArrayIterator<ISPO>(
                            numStmts,stmts,null/*keyOrder*/),
                    computeClosureForStatementIdentifiers,
                    changeLog);
            
        }

        // reset the counter.
        numStmts = 0;

        // FIXME Note: being truncated to int, but whole class is deprecated.
        return (int) Math.min(Integer.MAX_VALUE, n);

    }

}
