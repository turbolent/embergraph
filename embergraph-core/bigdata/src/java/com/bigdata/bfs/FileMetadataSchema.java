package com.bigdata.bfs;

import com.bigdata.sparse.KeyType;
import com.bigdata.sparse.Schema;
import com.bigdata.sparse.SparseRowStore;

/**
 * The schema for metadata about file versions stored in the
 * {@link BigdataFileSystem}. Some well known properties are always defined,
 * but any property may be stored - ideally within their own namespace!
 * <p>
 * Note: File version creation time and update times are available using the
 * {@link SparseRowStore}, which stores and reports the timestamp for each
 * property value. Convenience methods are available on
 * {@link RepositoryDocumentImpl} to report those timestamps. Timestamps for
 * file blocks can NOT be obtained.
 * <p>
 * Note: A content length property was deliberately NOT defined. The design is
 * geared towards very large file and asynchronous read/write of file blocks.
 * The length of short files may be readily computed by the expediency of
 * sucking their contents into a buffer. Large files should always be processed
 * using a stream-oriented technique or distributed to concurrent clients in
 * block sized pieces.
 * 
 * @todo other obvious metadata would include the user identifier associated
 *       with each update request.
 */
public class FileMetadataSchema extends Schema {
    
    /**
     * 
     */
    private static final long serialVersionUID = 2908749650061841935L;

    /**
     * The content identifer is an arbitrary Unicode {@link String} whose value
     * is defined by the client. This is generally a pathname, similar to a path
     * in a file system.
     */
    public static transient final String ID = FileMetadataSchema.class
            .getPackage().getName()
            + ".Id";
    
    /**
     * The MIME type associated with the content (the same semantics as the
     * HTTP <code>Content-Type</code> header).
     */
    public static transient final String CONTENT_TYPE = FileMetadataSchema.class
            .getPackage().getName()
            + ".ContentType";

    /**
     * The encoding, if any, used to convert the byte[] content to
     * characters.
     * <p>
     * Note: This is typically deduced from an analysis of the MIME Type in
     * <code>Content-Type</code> header and at times the leading bytes of
     * the response body itself.
     */
    public static transient final String CONTENT_ENCODING = FileMetadataSchema.class
            .getPackage().getName()
            + ".ContentEncoding";

    /**
     * The file version number. Together the file {@link #ID} and the file
     * {@link #VERSION} form the primary key for the data index. A
     * <code>null</code> value is stored in this field when the file version
     * is deleted.
     */
    public static transient final String VERSION = FileMetadataSchema.class
            .getPackage().getName()
            + ".Version";

    /**
     * Optional boolean property. When present, the text in the file version
     * will be tokenized and indexed for free text search. Non-text files should
     * not set this property. Instead, a workflow or map/reduce job should be
     * written to generate a text-only variant for the indexer.
     * 
     * @todo need a property that is the "URI" of the indexed document?
     */
    public static transient final String INDEX_TEXT = FileMetadataSchema.class
            .getPackage().getName()
            + ".IndexText";
    
    public FileMetadataSchema() {
        
        super( "__bfs"/*FileMetadataSchema.class.getName()*/, ID, KeyType.Unicode);
        
    }
    
}