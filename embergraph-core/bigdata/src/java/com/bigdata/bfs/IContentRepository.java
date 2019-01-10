package com.bigdata.bfs;

import java.util.Iterator;

/**
 * Document-centric interface for the {@link BigdataFileSystem}.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @deprecated This is part of the BFS support.
 */
public interface IContentRepository 
{

    /**
     * Fetch a single document object based on a URI.
     * 
     * @param id
     *            the identifier of the document to fetch
     * 
     * @return the document -or- null if there is no such document.
     */
    Document read( String id);
    
    /**
     * Create a new persistent document in this repository based on the metadata
     * and content in the supplied document object.
     * 
     * @param document
     *            an object containing the content and metadata to persist
     * 
     * @return The new version.
     */
    int create( Document document );
    
    /**
     * Update an existing persistent document in this repository based on the
     * metadata and content in the supplied document object. The document to be
     * updated will be identified by the {@link DocumentHeader#getId()} method.
     * 
     * @param document
     *            an object containing the content and metadata to update
     * 
     * @return The new version.
     */
    int update( Document document );
    
    /**
     * Delete a single document.
     * 
     * @param id
     *            the identifier of the document to delete
     * 
     * @return The #of blocks that were deleted for that file.
     */
    long delete(String id);
    
    /**
     * Delete all documents in the identified key range.
     * <p>
     * Note: If you assign identifiers using a namespace then you can use this
     * method to rapidly delete all documents within that namespace.
     * 
     * @param fromId
     *            The identifier of the first document to be deleted or
     *            <code>null</code> if there is no lower bound.
     * @param toId
     *            The identifier of the first document that will NOT be deleted
     *            or <code>null</code> if there is no upper bound.
     * 
     * @return The #of files that were deleted.
     */
    long deleteAll(String fromId, String toId);
    
    /**
     * Return a listing of the documents and metadata about them in this
     * repository.
     * <p>
     * Note: If you assign identifiers using a namespace then you can use this
     * method to efficiently visit all documents within that namespace.
     * 
     * @param fromId
     *            The identifier of the first document to be visited or
     *            <code>null</code> if there is no lower bound.
     * @param toId
     *            The identifier of the first document that will NOT be visited
     *            or <code>null</code> if there is no upper bound.
     * 
     * @return an iterator of {@link DocumentHeader}s.
     */
    Iterator<? extends DocumentHeader> getDocumentHeaders(String fromId,
            String toId);

    /**
     * Full text search against the indexed documents.
     * 
     * @param query
     *            A query.
     * 
     * @return An iterator visiting the identifiers of the documents in order of
     *         decreasing relevance to the query.
     * 
     * @todo return more metadata about the search results.
     * 
     * @todo allow fromId, toId to restrict to a given namespace?
     * 
     * @todo register analyzers against MIME types and index those that are
     *       selected on index or update.
     * 
     * @todo refactor out of this API since more than one free text index could
     *       be used over the repo.
     */
    Iterator<String> search(String query);
    
}
