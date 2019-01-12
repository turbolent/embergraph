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
package org.embergraph.sparse;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.SuccessorUtil;

/*
* A schema for a sparse row store. Note that more than one schema may be used with the same index.
 * The name of the schema is always encoded as the first component of the key.
 *
 * @todo support optional strong typing for column values?
 * @todo support optional required columns?
 */
public class Schema implements Externalizable {

  /** */
  private static final long serialVersionUID = -7619134292028385179L;

  private String name;
  private String primaryKey;
  private KeyType primaryKeyType;
  private transient byte[] schemaBytes;

  /** De-serialization ctor. */
  public Schema() {}

  /*
   * @param name The schema name.
   * @param primaryKey The name of the column whose value is the (application defined) primary key.
   * @param primaryKeyType The data type for the primary key.
   */
  public Schema(final String name, final String primaryKey, final KeyType primaryKeyType) {

    NameChecker.assertSchemaName(name);

    NameChecker.assertColumnName(primaryKey);

    if (primaryKeyType == null) throw new IllegalArgumentException();

    this.name = name;

    this.primaryKey = primaryKey;

    this.primaryKeyType = primaryKeyType;

    // eager computation of the encoded scheme name.
    getSchemaBytes();
  }

  /** The name of the schema. */
  public String getName() {

    return name;
  }

  /** The name of the column whose value is the primary key. */
  public String getPrimaryKeyName() {

    return primaryKey;
  }

  /** The data type that is used for the primary key when forming the total key. */
  public KeyType getPrimaryKeyType() {

    return primaryKeyType;
  }

  /** The Unicode sort key encoding of the schema name. */
  protected final byte[] getSchemaBytes() {

    if (schemaBytes == null) {

      if (SparseRowStore.schemaNameUnicodeClean) {
      /*
       * One time encoding of the schema name as UTF8.
         */
        schemaBytes = name.getBytes(StandardCharsets.UTF_8);
      } else {
      /*
       * One time encoding of the schema name as a Unicode sort key.
         */
        schemaBytes = asSortKey(name);
      }
    }

    return schemaBytes;
  }

  //    /*
//     * The length of the value that will be returned by {@link #getSchemaBytes()}
  //     */
  //    final protected int getSchemaBytesLength() {
  //
  //        return getSchemaBytes().length;
  //
  //    }

  /*
   * Key builder stuff.
   */

  /*
   * Helper method appends a typed value to the compound key (this is used to get the primary key
   * into the compound key).
   *
   * <p>Note: This automatically appends the <code>nul</code> terminator byte if {@link
   * KeyType#isFixedLength()} is <code>false</code>. That byte flags the end of the primary key for
   * variable length primary keys.
   *
   * @param keyType The target data type.
   * @param v The value.
   * @return The {@link #keyBuilder}.
   * @see KeyDecoder
   */
  protected final IKeyBuilder appendPrimaryKey(
      final IKeyBuilder keyBuilder, final Object v, final boolean successor) {

    final KeyType keyType = getPrimaryKeyType();

    if (successor) {

      switch (keyType) {
        case Integer:
          return keyBuilder.append(successor(keyBuilder, ((Number) v).intValue()));
        case Long:
          return keyBuilder.append(successor(keyBuilder, ((Number) v).longValue()));
        case Float:
          return keyBuilder.append(successor(keyBuilder, ((Number) v).floatValue()));
        case Double:
          return keyBuilder.append(successor(keyBuilder, ((Number) v).doubleValue()));
        case Unicode:
          {
            final String tmp = v.toString();
            if (SparseRowStore.primaryKeyUnicodeClean) {
              keyBuilder
                  .append(SuccessorUtil.successor(tmp.getBytes(StandardCharsets.UTF_8)))
                  .appendNul();
            } else {
              // primary key in backwards compatibility mode.
              keyBuilder.appendText(tmp, true /* unicode */, true /* successor */).appendNul();
            }
            return keyBuilder;
          }
        case ASCII:
          return keyBuilder
              .appendText(v.toString(), false /*unicode*/, true /*successor*/)
              .appendNul();
        case Date:
          return keyBuilder.append(successor(keyBuilder, ((Date) v).getTime()));
          //            case UnsignedBytes:
          //                return keyBuilder.append((byte[])v).appendNul();
        default:
          throw new UnsupportedOperationException();
      }

    } else {

      switch (keyType) {
        case Integer:
          return keyBuilder.append(((Number) v).intValue());
        case Long:
          return keyBuilder.append(((Number) v).longValue());
        case Float:
          return keyBuilder.append(((Number) v).floatValue());
        case Double:
          return keyBuilder.append(((Number) v).doubleValue());
        case Unicode:
          {
            final String tmp = v.toString();
            if (SparseRowStore.primaryKeyUnicodeClean) {
              keyBuilder.append(tmp.getBytes(StandardCharsets.UTF_8)).appendNul();
            } else {
              // primary key in backwards compatibility mode.
              keyBuilder
                  .appendText(v.toString(), true /* unicode */, false /* successor */)
                  .appendNul();
            }
            return keyBuilder;
          }
        case ASCII:
          return keyBuilder
              .appendText(v.toString(), false /*unicode*/, false /*successor*/)
              .appendNul();
        case Date:
          return keyBuilder.append(((Date) v).getTime());
          //            case UnsignedBytes:
          //                return keyBuilder.append((byte[]) v);
        default:
          throw new UnsupportedOperationException();
      }
    }

    //        return keyBuilder;

  }

  /*
   * Return the successor of a primary key object.
   *
   * @param v The object.
   * @return The successor.
   * @throws UnsupportedOperationException if the primary key type is {@link KeyType#Unicode}. See
   *     {@link #toKey(Object)}, which correctly forms the successor key in all cases.
   */
  private final Object successor(final IKeyBuilder keyBuilder, final Object v) {

    final KeyType keyType = getPrimaryKeyType();

    switch (keyType) {
      case Integer:
        return SuccessorUtil.successor(((Number) v).intValue());
      case Long:
        return SuccessorUtil.successor(((Number) v).longValue());
      case Float:
        return SuccessorUtil.successor(((Number) v).floatValue());
      case Double:
        return SuccessorUtil.successor(((Number) v).doubleValue());
      case Unicode:
      case ASCII:
        //        case UnsignedBytes:
      /*
       * Note: See toKey() for how to correctly form the sort key for the
         * successor of a Unicode value.
         */
        throw new UnsupportedOperationException();
        //            return SuccessorUtil.successor(v.toString());
        //            return SuccessorUtil.successor(v.toString());
      case Date:
        return SuccessorUtil.successor(((Date) v).getTime());
      default:
        throw new UnsupportedOperationException();
    }
    //
    //        return keyBuilder;

  }

  /*
   * Forms the key in {@link #keyBuilder} that should be used as the first key (inclusive) for a
   * range query that will visit all index entries for the specified primary key.
   *
   * @param primaryKey The primary key.
   * @return The {@link #keyBuilder}, which will have the schema and the primary key already
   *     formatted in its buffer.
   * @see KeyDecoder
   */
  protected final IKeyBuilder fromKey(final IKeyBuilder keyBuilder, final Object primaryKey) {

    keyBuilder.reset();

    // append the (encoded) schema name.
    keyBuilder.append(getSchemaBytes());
    keyBuilder.appendSigned(primaryKeyType.getByteCode());
    keyBuilder.appendNul();

    if (primaryKey != null) {

      // append the (encoded) primary key.
      appendPrimaryKey(keyBuilder, primaryKey, false /* successor */);
    }

    return keyBuilder;
  }

  /*
   * The prefix that identifies all tuples in the logical row for this schema having the indicated
   * value for their primary key.
   *
   * @param primaryKey The value of the primary key for the logical row.
   * @return
   */
  public final byte[] getPrefix(final IKeyBuilder keyBuilder, final Object primaryKey) {

    return fromKey(keyBuilder, primaryKey).getKey();
  }

  /*
   * Note: use SuccessorUtil.successor(key.clone()) instead.
   */

  //    /*
//     * Forms the key in {@link #keyBuilder} that should be used as the last key
  //     * (exclusive) for a range query that will visit all index entries for the
  //     * specified primary key.
  //     *
  //     * @param primaryKey
  //     *            The primary key.
  //     *
  //     * @return The {@link #keyBuilder}, which will have the schema and the
  //     *         successor of the primary key already formatted in its buffer.
  //     *
  //     * @see KeyDecoder
  //     *
  //     * @throws IllegalArgumentException
  //     *             if either argument is <code>null</code>.
  //     */
  //    final protected IKeyBuilder toKey(IKeyBuilder keyBuilder, Object primaryKey) {
  //
  //        if (primaryKey == null)
  //            throw new IllegalArgumentException();
  //
  //        keyBuilder.reset();
  //
  //        // append the (encoded) schema name.
  //        keyBuilder.append(getSchemaBytes());
  //        keyBuilder.append(primaryKeyType.getByteCode());
  //        keyBuilder.appendNul();
  //
  //        // append successor of the (encoded) primary key.
  //        appendPrimaryKey(keyBuilder, primaryKey, true/* successor */);
  //
  //        return keyBuilder;
  //
  //    }

  /*
   * Encodes a key for the {@link Schema}.
   *
   * @param keyBuilder
   * @param primaryKey The primary key for the logical row (required).
   * @param col The column name (required).
   * @param timestamp The timestamp (required).
   * @return The encoded key.
   * @throws IllegalArgumentException if <i>keyBuilder</i> is <code>null</code>.
   * @throws IllegalArgumentException if <i>primaryKey</i> is <code>null</code>.
   * @throws IllegalArgumentException if <i>col</i> is not valid as the name of a column.
   */
  public byte[] getKey(
      final IKeyBuilder keyBuilder,
      final Object primaryKey,
      final String col,
      final long timestamp) {

    if (keyBuilder == null) throw new IllegalArgumentException();

    if (primaryKey == null) throw new IllegalArgumentException();

    NameChecker.assertColumnName(col);

    // encode the schema name and the primary key.
    fromKey(keyBuilder, primaryKey);

    /*
     * The column name. Note that the column name is NOT stored with Unicode
     * compression so that we can decode it without loss.
     */

    keyBuilder.append(col.getBytes(StandardCharsets.UTF_8)).appendNul();

    keyBuilder.append(timestamp);

    final byte[] key = keyBuilder.getKey();

    return key;
  }

  private static final transient short VERSION0 = 0x0;

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    final short version = in.readShort();

    if (version != VERSION0) throw new IOException("Unknown version=" + version);

    name = in.readUTF();

    primaryKey = in.readUTF();

    primaryKeyType = KeyType.getKeyType(in.readByte());

    // eager computation of the encoded scheme name.
    getSchemaBytes();
  }

  public void writeExternal(ObjectOutput out) throws IOException {

    out.writeShort(VERSION0);

    out.writeUTF(name);

    out.writeUTF(primaryKey);

    out.writeByte(primaryKeyType.getByteCode());
  }

  public String toString() {

    return "Schema{name="
        + getName()
        + ",primaryKeyName="
        + getPrimaryKeyName()
        + ",primaryKeyType="
        + getPrimaryKeyType()
        + "}";
  }

  /*
   * Used for historical compatibility to unbox an application key (convert it to an unsigned
   * byte[]).
   */
  private static final IKeyBuilder _keyBuilder = KeyBuilder.newUnicodeInstance();

  /*
   * Utility method for historical compatibility converts an application key to a sort key (an
   * unsigned byte[] that imposes the same sort order).
   *
   * <p>Note: This method is thread-safe.
   *
   * @param val An application key.
   * @return The unsigned byte[] equivalent of that key. This will be <code>null</code> iff the
   *     <i>key</i> is <code>null</code>. If the <i>key</i> is a byte[], then the byte[] itself will
   *     be returned.
   */
  private static final byte[] asSortKey(final Object val) {

    if (val == null) {

      return null;
    }

    if (val instanceof byte[]) {

      return (byte[]) val;
    }

    /*
     * Synchronize on the keyBuilder to avoid concurrent modification of its
     * state.
     */

    synchronized (_keyBuilder) {
      return _keyBuilder.getSortKey(val);
    }
  }
}
