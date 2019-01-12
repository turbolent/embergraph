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
package org.embergraph.rdf.changesets;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.EmbergraphStatementIterator;
import org.embergraph.striterator.ChunkedArrayIterator;

/**
 * This is a very simple implementation of a change log.  NOTE: This is not
 * a particularly great implementation.  First of all it ends up storing
 * two copies of the change set.  Secondly it needs to be smarter about
 * concurrency, or maybe we can be smart about it when we do the
 * implementation on the other side (the SAIL connection can just write
 * change events to a buffer and then the buffer can be drained by 
 * another thread that doesn't block the actual read/write operations,
 * although then we need to be careful not to issue the committed()
 * notification before the buffer is drained).
 * 
 * @author mike
 */
public class InMemChangeLog implements IChangeLog {

    private static final Logger log = Logger.getLogger(InMemChangeLog.class);
    
    /**
     * Running tally of new changes since the last commit notification.
     */
    private final Map<ISPO,IChangeRecord> changeSet = 
        new HashMap<ISPO, IChangeRecord>();
    
    /**
     * Keep a record of the change set as of the last commit.
     */
    private final Map<ISPO,IChangeRecord> committed = 
        new HashMap<ISPO, IChangeRecord>();
    
    /**
     * See {@link IChangeLog#changeEvent(IChangeRecord)}.
     */
    @Override
    public synchronized void changeEvent(final IChangeRecord record) {
        
        if (log.isInfoEnabled()) 
            log.info(record);
        
        changeSet.put(record.getStatement(), record);
        
    }
    
    @Override
    public void transactionBegin() {

    }

    @Override
    public void transactionPrepare() {
        
    }
    
    /**
     * See {@link IChangeLog#transactionCommited(long)}.
     */
    public synchronized void transactionCommited(final long commitTime) {
    
        if (log.isInfoEnabled())
            log.info("transaction committed: " + commitTime);

        committed.clear();
        
        committed.putAll(changeSet);
        
        changeSet.clear();
        
    }
    
    /**
     * See {@link IChangeLog#transactionAborted()}.
     */
    public synchronized void transactionAborted() {

        if (log.isInfoEnabled()) 
            log.info("transaction aborted");
        
        changeSet.clear();
        
    }
    
    /**
     * See {@link IChangeLog#close()}.
     */
    @Override
    public void close() {
    }

    /**
     * Return the change set as of the last commmit point.
     * 
     * @return
     *          a collection of {@link IChangeRecord}s as of the last commit
     *          point
     */
    public Collection<IChangeRecord> getLastCommit() {
        
        return committed.values();
        
    }
    
    /**
     * Return the change set as of the last commmit point, using the supplied
     * database to resolve ISPOs to BigdataStatements.
     * 
     * @return
     *          a collection of {@link IChangeRecord}s as of the last commit
     *          point
     */
    public Collection<IChangeRecord> getLastCommit(final AbstractTripleStore db) {
        
        return resolve(db, committed.values());
        
    }
    
    /**
     * Use the supplied database to turn a set of ISPO change records into
     * EmbergraphStatement change records.  BigdataStatements also implement
     * ISPO, the difference being that BigdataStatements also contain
     * materialized RDF terms for the 3 (or 4) positions, in addition to just
     * the internal identifiers (IVs) for those terms.
     * 
     * @param db
     *          the database containing the lexicon needed to materialize
     *          the EmbergraphStatement objects
     * @param unresolved
     *          the ISPO change records that came from IChangeLog notification
     *          events
     * @return
     *          the fully resolves EmbergraphStatement change records
     */
    private Collection<IChangeRecord> resolve(final AbstractTripleStore db, 
            final Collection<IChangeRecord> unresolved) {
        
        final Collection<IChangeRecord> resolved = 
            new LinkedList<IChangeRecord>();

        // collect up the ISPOs out of the unresolved change records
        final ISPO[] spos = new ISPO[unresolved.size()];
        int i = 0;
        for (IChangeRecord rec : unresolved) {
            spos[i++] = rec.getStatement();
        }
        
        // use the database to resolve them into BigdataStatements
        final EmbergraphStatementIterator it =
            db.asStatementIterator(
                    new ChunkedArrayIterator<ISPO>(i, spos, null/* keyOrder */));
        
        /* 
         * the EmbergraphStatementIterator will produce EmbergraphStatement objects
         * in the same order as the original ISPO array
         */
        for (IChangeRecord rec : unresolved) {
            
            final EmbergraphStatement stmt = it.next();
            
            resolved.add(new ChangeRecord(stmt, rec.getChangeAction()));
            
        }
        
        return resolved;
        
    }

}
