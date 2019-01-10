package com.bigdata.bop.engine;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import com.bigdata.bop.BOp;
import com.bigdata.bop.IBindingSet;
import com.bigdata.bop.fed.FederatedQueryEngine;
import com.bigdata.service.IService;

/**
 * Interface for a node participating in the exchange of NIO buffers to
 * support query execution.
 */
public interface IQueryPeer extends Remote {

    /**
     * The {@link UUID} of the service in which this {@link QueryEngine} is
     * running.
     * 
     * @return The {@link UUID} of the service in which this {@link QueryEngine}
     *         is running -or- a unique and distinct UUID if the
     *         {@link QueryEngine} is not running against an
     *         IBigdataFederation.
     * 
     * @see IService#getServiceUUID()
     */
    UUID getServiceUUID() throws RemoteException;

    /**
     * Declare a query to a peer. This message is sent to the peer before any
     * other message for that query and declares the query and the query
     * controller with which the peer must communicate during query evaluation.
     * 
     * @param queryDecl
     *            The query declaration.
     * 
     * @throws UnsupportedOperationException
     *             unless running in scale-out.
     * 
     * @deprecated This method is unused and will probably disappear. The nodes
     *             in a cluster reach back to the query controller using
     *             {@link IQueryClient#getQuery(UUID)} to resolve the query on
     *             its first reference. This is handled by the
     *             {@link FederatedQueryEngine} within its run() loop where it
     *             accepts {@link IChunkMessage}s.
     */
    void declareQuery(IQueryDecl queryDecl) throws RemoteException;

    /**
     * Notify a service that a buffer having data for some {@link BOp} in some
     * running query is available. The receiver may request the data when they
     * are ready. If the query is cancelled, then the sender will drop the
     * buffer.
     * 
     * @param msg
     *            The message.
     * 
     * @throws UnsupportedOperationException
     *             unless running in scale-out.
     */
    void bufferReady(IChunkMessage<IBindingSet> msg) throws RemoteException;

    /**
     * Notify a service that the query has been terminated. The peer MUST NOT
     * cancel the query synchronously as that can lead to a deadlock with the
     * query controller. Instead, the peer should queue a task to cancel the
     * query and then return.
     * 
     * @param queryId
     *            The query identifier.
     * @param cause
     *            The cause. When <code>null</code>, this is presumed to be
     *            normal query termination.
     */
    void cancelQuery(UUID queryId, Throwable cause) throws RemoteException;

}
