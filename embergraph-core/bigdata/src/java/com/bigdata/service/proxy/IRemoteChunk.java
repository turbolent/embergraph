package com.bigdata.service.proxy;

import java.io.Serializable;
import java.rmi.Remote;

import com.bigdata.striterator.IKeyOrder;

/**
 * Abstraction for a chunk from a remote iterator. Instances of this interface
 * are serialized by a remote service and materialized on the client. The
 * interface itself is NOT {@link Remote}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <E>
 *            The generic type of the elements in the chunk.
 */
public interface IRemoteChunk<E> extends Serializable {

    /**
     * <code>true</code> iff the iterator will not return any more chunks.
     */
    boolean isExhausted();

    /**
     * The elements in the current chunk -or- <code>null</code> iff there are
     * NO elements in the chunk.
     */
    public E[] getChunk();

    /**
     * The natural sort orded of the elements in this chunk -or-
     * <code>null</code> if the elements are not in any known order.
     * <p>
     * Note: The returned value should be the same each time for a given source
     * iterator. It is put here so that we can avoid an RMI for this property
     * and the expense of serializing the value with each chunk.
     */
    public IKeyOrder<E> getKeyOrder();

}
