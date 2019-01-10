package com.bigdata.service;

import com.bigdata.btree.proc.IIndexProcedure;
import com.bigdata.journal.AbstractTask;
import com.bigdata.journal.DropIndexTask;
import com.bigdata.journal.RegisterIndexTask;

/**
 * Interface for procedures that require access to the {@link IDataService} and
 * or the federation.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @todo register index and drop index could be rewritten as submitted
 *       procedures derived from this class. This would simplify the
 *       {@link IDataService} API and metrics collection further. The
 *       implementations would have to be distinct from
 *       {@link RegisterIndexTask} and {@link DropIndexTask} since those extend
 *       {@link AbstractTask} - that class does not implement
 *       {@link IIndexProcedure} and can not be sent across the wire.
 * 
 * @see IFederationCallable
 */
public interface IDataServiceCallable extends IFederationCallable {

    /**
     * Invoked before the task is executed to provide a reference to the
     * {@link IDataService} on which it is executing. This method is also
     * responsible for setting the {@link IBigdataFederation} reference using
     * {@link IFederationCallable#setFederation(IBigdataFederation)}.
     * 
     * @param dataService
     *            The data service.
     * 
     * @throws IllegalArgumentException
     *             if the argument is <code>null</code>
     * @throws IllegalStateException
     *             if {@link #setDataService(DataService)} has already been
     *             invoked and was set with a different value.
     */
    void setDataService(DataService dataService);
    
    /**
     * Return the {@link DataService}.
     * 
     * @return The data service and never <code>null</code>.
     * 
     * @throws IllegalStateException
     *             if {@link #setDataService(DataService)} has not been invoked.
     */
    DataService getDataService();
    
}
