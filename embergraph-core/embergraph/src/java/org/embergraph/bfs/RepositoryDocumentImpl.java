package org.embergraph.bfs;

import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.embergraph.sparse.IRowStoreConstants;
import org.embergraph.sparse.ITPS;
import org.embergraph.sparse.ITPV;

/*
 * A read-only view of a {@link Document} that has been read from a {@link EmbergraphFileSystem}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class RepositoryDocumentImpl implements DocumentHeader, Document {

  private final EmbergraphFileSystem repo;

  private final String id;

  /*
   * The result of the atomic read on the file's metadata. This representation is significantly
   * richer than the current set of property values.
   */
  final ITPS tps;

  /*
   * The current version identifer -or- <code>-1</code> iff there is no current version for the file
   * (including when there is no record of any version for the file).
   */
  final int version;

  /** The property set for the current file version. */
  private final Map<String, Object> metadata;

  /*
   * Read the metadata for the current version of the file from the repository.
   *
   * @param id The file identifier.
   * @param tps The logical row describing the metadata for some file in the repository.
   */
  public RepositoryDocumentImpl(final EmbergraphFileSystem repo, final String id, final ITPS tps) {

    if (repo == null) throw new IllegalArgumentException();

    if (id == null) throw new IllegalArgumentException();

    this.repo = repo;

    this.id = id;

    this.tps = tps;

    if (tps != null) {

      ITPV tmp = tps.get(FileMetadataSchema.VERSION);

      if (tmp.getValue() != null) {

        /*
         * Note the current version identifer.
         */

        this.version = (Integer) tmp.getValue();

        /*
         * Save a simplifed view of the propery set for the current
         * version.
         */

        this.metadata = tps.asMap();

        EmbergraphFileSystem.log.info("id=" + id + ", current version=" + version);

      } else {

        /*
         * No current version.
         */

        this.version = -1;

        this.metadata = null;

        EmbergraphFileSystem.log.warn("id=" + id + " : no current version");
      }

    } else {

      /*
       * Nothing on record for that file identifier.
       */

      this.version = -1;

      this.metadata = null;

      EmbergraphFileSystem.log.warn("id=" + id + " : no record of any version(s)");
    }

    if (EmbergraphFileSystem.DEBUG && metadata != null) {

      Iterator<Map.Entry<String, Object>> itr = metadata.entrySet().iterator();

      while (itr.hasNext()) {

        Map.Entry<String, Object> entry = itr.next();

        EmbergraphFileSystem.log.debug(
            "id="
                + id
                + ", version="
                + getVersion()
                + ", ["
                + entry.getKey()
                + "]=["
                + entry.getValue()
                + "]");
      }
    }
  }

  /*
   * Read the metadata for the current version of the file from the repository.
   *
   * @param id The file identifier.
   */
  public RepositoryDocumentImpl(final EmbergraphFileSystem repo, final String id) {

    this(
        repo,
        id,
        repo.getFileMetadataIndex()
            .read(
                EmbergraphFileSystem.metadataSchema,
                id,
                IRowStoreConstants.MIN_TIMESTAMP,
                IRowStoreConstants.CURRENT_ROW,
                null /* filter */));
  }

  /*
   * Assert that a version of the file existed when this view was constructed.
   *
   * @throws IllegalStateException unless a version of the file existed at the time that this view
   *     was constructed.
   */
  protected final void assertExists() {

    if (version == -1) {

      throw new IllegalStateException("No current version: id=" + id);
    }
  }

  public final boolean exists() {

    return version != -1;
  }

  public final int getVersion() {

    assertExists();

    return (Integer) metadata.get(FileMetadataSchema.VERSION);
  }

  /*
   * Note: This is obtained from the earliest available timestamp of the {@link
   * FileMetadataSchema#ID} property.
   */
  public final long getEarliestVersionCreateTime() {

    assertExists();

    Iterator<ITPV> itr = tps.iterator();

    while (itr.hasNext()) {

      ITPV tpv = itr.next();

      if (tpv.getName().equals(FileMetadataSchema.ID)) {

        return tpv.getTimestamp();
      }
    }

    throw new AssertionError();
  }

  public final long getVersionCreateTime() {

    assertExists();

    /*
     * The timestamp for the most recent value of the VERSION property.
     */

    final long createTime = tps.get(FileMetadataSchema.VERSION).getTimestamp();

    return createTime;
  }

  public final long getMetadataUpdateTime() {

    assertExists();

    /*
     * The timestamp for the most recent value of the ID property.
     */

    final long metadataUpdateTime = tps.get(FileMetadataSchema.ID).getTimestamp();

    return metadataUpdateTime;
  }

  /*
   * Return an array containing all non-eradicated values of the {@link FileMetadataSchema#VERSION}
   * property for this file as of the time that this view was constructed.
   *
   * @see EmbergraphFileSystem#getAllVersionInfo(String)
   */
  public final ITPV[] getAllVersionInfo() {

    return repo.getAllVersionInfo(id);
  }

  public final InputStream getInputStream() {

    assertExists();

    return repo.inputStream(id, getVersion());
  }

  public final Reader getReader() throws UnsupportedEncodingException {

    assertExists();

    return repo.reader(id, getVersion(), getContentEncoding());
  }

  public final String getContentEncoding() {

    assertExists();

    return (String) metadata.get(FileMetadataSchema.CONTENT_ENCODING);
  }

  public final String getContentType() {

    assertExists();

    return (String) metadata.get(FileMetadataSchema.CONTENT_TYPE);
  }

  public final String getId() {

    return id;
  }

  public final Object getProperty(String name) {

    return metadata.get(name);
  }

  public final Map<String, Object> asMap() {

    assertExists();

    return Collections.unmodifiableMap(metadata);
  }
}
