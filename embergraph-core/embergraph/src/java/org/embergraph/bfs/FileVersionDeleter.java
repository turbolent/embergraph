package org.embergraph.bfs;

import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.filter.Advancer;
import org.embergraph.btree.filter.TupleUpdater;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.sparse.IRowStoreConstants;
import org.embergraph.sparse.KeyDecoder;
import org.embergraph.sparse.TPS.TPV;
import org.embergraph.sparse.TimestampChooser;
import org.embergraph.sparse.ValueType;
import org.embergraph.util.Bytes;

/**
 * A procedure that performs a key range scan, marking all non-deleted versions within the key range
 * as deleted (by storing a null property value for the {@link FileMetadataSchema#VERSION}).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class FileVersionDeleter extends TupleUpdater<TPV> {

  private static final long serialVersionUID = -3084247827028423921L;

  /** The timestamp specified to the ctor. */
  private final long timestamp;

  /** The timestamp chosen on the server (not serialized). */
  private transient long choosenTimestamp;

  /** <code>false</code> until we choose the timestamp (not serialized) */
  private transient boolean didInit = false;

  /** used to generate keys on the server (not serialized). */
  private transient KeyBuilder keyBuilder;

  /**
   * @param timestamp A valid timestamp or {@link IRowStoreConstants#AUTO_TIMESTAMP} or {@link
   *     IRowStoreConstants#AUTO_TIMESTAMP_UNIQUE}.
   */
  public FileVersionDeleter(long timestamp) {

    this.timestamp = timestamp;
  }

  /** Only visits the {@link FileMetadataSchema#VERSION} columns. */
  @Override
  protected boolean isValid(ITuple<TPV> tuple) {

    final KeyDecoder keyDecoder = new KeyDecoder(tuple.getKey());

    final String name = keyDecoder.getColumnName();

    return name.equals(FileMetadataSchema.VERSION);
  }

  /**
   * Appends a new tuple into the index whose key uses the {@link #choosenTimestamp} and whose value
   * is an encoded <code>null</code>. This is interepreted as a "deleted" file version tuple.
   *
   * <p>Note: The old tuple is not removed so you can continue to access the historical version
   * explicitly.
   *
   * @todo unit tests to verify that old versions are left behind on overflow (per the appropriate
   *     policy).
   * @todo unit test to verify continued access to the historical version.
   *     <p>FIXME Unit test to verify that the iterator correctly visits all file versions in the
   *     range. In particular, we will be seeing each file version tuple in the key range. There may
   *     be more than one of these, right? Therefore we need to use the {@link Advancer} pattern to
   *     skip beyond the last possible such tuple (which will in fact be the one that we write on
   *     here!).
   *     <p>One way to approach this is a logical row scan with a column name filter that only
   *     visits the "version" column and an {@link TupleUpdater} and writes a new tuple per the code
   *     below. We just need to make sure that we either do not then visit the new tuple or that we
   *     ignore a "version" row whose value is [null].
   */
  protected void update(final IIndex ndx, final ITuple<TPV> tuple) {

    final byte[] key = tuple.getKey();

    if (!didInit) {

      // choose timestamp for any writes.
      choosenTimestamp = TimestampChooser.chooseTimestamp(ndx, timestamp);

      keyBuilder = new KeyBuilder(key.length);

      didInit = true;
    }

    //        // remove the old tuple.
    //        ndx.remove(key);

    // copy everything from the key except the old timestamp.
    keyBuilder.reset().append(key, 0 /* off */, key.length - Bytes.SIZEOF_LONG);

    // append the new timestamp.
    keyBuilder.append(choosenTimestamp);

    /*
     * insert a new tuple using the new timestamp. the [null] will be
     * interpreted as a deleted file version.
     */
    ndx.insert(key, ValueType.encode(null));
  }
}
