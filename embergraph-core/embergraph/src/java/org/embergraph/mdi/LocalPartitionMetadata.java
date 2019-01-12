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
package org.embergraph.mdi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.UUID;
import org.embergraph.io.LongPacker;
import org.embergraph.io.ShortPacker;
import org.embergraph.util.BytesUtil;

/*
 * An immutable object providing metadata about a local index partition, including the partition
 * identifier, the left and right separator keys defining the half-open key range of the index
 * partition, and optionally defining the {@link IResourceMetadata}[] required to materialize a view
 * of that index partition.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class LocalPartitionMetadata implements IPartitionMetadata, Externalizable {

  /** */
  private static final long serialVersionUID = -1511361004851335936L;

  /*
   * The maximum length of the history string (4kb).
   *
   * <p>Note: The history is written each time the {@link IndexMetadata} is written and is read each
   * time it is read so this can be the main driver of the size of the {@link IndexMetadata} record.
   *
   * @deprecated
   */
  protected static final transient int MAX_HISTORY_LENGTH = 0; // 4 * Bytes.kilobyte32;

  /** The unique partition identifier. */
  private int partitionId;

  /*
   * @see #getSourcePartitionId()
   * @deprecated MoveTask manages without this field (it was required by the previous MOVE
   *     implementation).
   */
  private int sourcePartitionId;

  /** */
  private byte[] leftSeparatorKey;

  private byte[] rightSeparatorKey;

  /*
   * Description of the resources required to materialize a view of the index partition (optional -
   * not stored when the partition metadata is stored on an {@link IndexSegmentStore}).
   *
   * <p>The entries in the array reflect the creation time of the resources. The earliest resource
   * is listed first. The most recently created resource is listed last.
   *
   * <p>When present, the #of sources in the index partition view includes: the mutable {@link
   * BTree}, any {@link BTree}s on historical journal(s) still incorporated into the view, and any
   * {@link IndexSegment}s incorporated into the view.
   */
  private IResourceMetadata[] resources;

  /*
   * The reason why an index partition was created together with some metadata about when it was
   * created.
   */
  private IndexPartitionCause cause;

  //    /*
  //     * A history of operations giving rise to the current partition metadata.
  //     * E.g., register(timestamp), copyOnOverflow(timestamp), split(timestamp),
  //     * join(partitionId,partitionId,timestamp), etc. This is truncated when
  //     * serialized to keep it from growing without bound.
  //     *
  //     * @deprecated See {@link #getHistory()}
  //     */
  //    private String history;
  //
  //    /*
  //     * If the history string exceeds {@link #MAX_HISTORY_LENGTH} characters then
  //     * truncates it to the last {@link #MAX_HISTORY_LENGTH}-3 characters,
  //     * prepends "...", and returns the result. Otherwise returns the entire
  //     * history string.
  //     *
  //     * @deprecated See {@link #history}
  //     */
  //    protected String getTruncatedHistory() {
  //
  //        if (MAX_HISTORY_LENGTH == 0)
  //            return "";
  //
  //        String history = this.history;
  //
  //        if(history.length() > MAX_HISTORY_LENGTH) {
  //
  //            /*
  //             * Truncate the history.
  //             */
  //
  //            final int len = history.length();
  //
  //            final int fromIndex = len - (MAX_HISTORY_LENGTH - 3);
  //
  //            assert fromIndex > 0 : "len=" + len + ", fromIndex=" + fromIndex
  //                    + ", maxHistoryLength=" + MAX_HISTORY_LENGTH;
  //
  //            history = "..." + history.substring(fromIndex, len);
  //
  //        }
  //
  //        return history;
  //
  //    }

  /** De-serialization constructor. */
  public LocalPartitionMetadata() {}

  /*
   * @param partitionId The unique partition identifier assigned by the {@link MetadataIndex}.
   * @param sourcePartitionId <code>-1</code> unless this index partition is the target for a move,
   *     in which case this is the partition identifier of the source index partition.
   * @param leftSeparatorKey The first key that can enter this index partition. The left separator
   *     key for the first index partition is always <code>new byte[]{}</code>. The left separator
   *     key MAY NOT be <code>null</code>.
   * @param rightSeparatorKey The first key that is excluded from this index partition or <code>null
   *     </code> iff there is no upper bound.
   * @param resources A description of each {@link Journal} or {@link IndexSegment} resource(s)
   *     required to compose a view of the index partition (optional).
   *     <p>The entries in the array reflect the creation time of the resources. The earliest
   *     resource is listed first. The most recently created resource is listed last.
   *     <p>Note: This is required if the {@link LocalPartitionMetadata} record will be saved on the
   *     {@link IndexMetadata} of a {@link BTree}. It is NOT recommended when it will be saved on
   *     the {@link IndexMetadata} of an {@link IndexSegment}. When the {@link IndexMetadata} is
   *     sent to a remote {@link DataService} this field MUST be <code>null</code> and the remote
   *     {@link DataService} will fill it in on arrival.
   * @param cause The underlying cause for the creation of the index partition.
   */
  //    * @param history
  //    *            A human interpretable history of the index partition. The
  //    *            history is a series of whitespace delimited records each of
  //    *            more or less the form <code>foo(x,y,z)</code>. The history
  //    *            gets truncated when the {@link LocalPartitionMetadata} is
  //    *            serialized in order to prevent it from growing without bound.
  public LocalPartitionMetadata(
      final int partitionId,
      final int sourcePartitionId,
      final byte[] leftSeparatorKey,
      final byte[] rightSeparatorKey, //
      final IResourceMetadata[] resources,
      final IndexPartitionCause cause
      //            final String history
      ) {

    /*
     * Set fields first so that toString() can be used in thrown exceptions.
     */

    this.partitionId = partitionId;

    this.sourcePartitionId = sourcePartitionId;

    this.leftSeparatorKey = leftSeparatorKey;

    this.rightSeparatorKey = rightSeparatorKey;

    this.resources = resources;

    this.cause = cause;

    //        this.history = history;

    /*
     * Test arguments.
     */

    if (leftSeparatorKey == null) throw new IllegalArgumentException("leftSeparatorKey");

    // Note: rightSeparatorKey MAY be null.

    if (rightSeparatorKey != null) {

      final int cmp = BytesUtil.compareBytes(leftSeparatorKey, rightSeparatorKey);

      if (cmp >= 0) {

        throw new IllegalArgumentException(
            "Separator keys are " + (cmp == 0 ? "equal" : "out of order") + " : " + this);
      }
    }

    if (resources != null) {

      if (resources.length == 0) {

        throw new IllegalArgumentException("Empty resources array.");
      }

      for (IResourceMetadata t : resources) {

        if (t == null) throw new IllegalArgumentException("null value in resources[]");
      }

      /*
       * This is the "live" journal.
       *
       * Note: The "live" journal is still available for writes. Index
       * segments created off of this journal will therefore have a
       * createTime that is greater than the firstCommitTime of this
       * journal while being LTE to the lastCommitTime of the journal and
       * strictly LT the commitTime of any other resource for this index
       * partition.
       */

      if (!resources[0].isJournal()) {

        throw new RuntimeException("Expecting a journal as the first resource: " + this);
      }

      /*
       * Scan from 1 to n-1 - these are historical resources
       * (non-writable). resources[0] is always the live journal. The
       * other resources may be either historical journals or index
       * segments.
       *
       * The order of the array is the order of the view. Reads on a
       * FusedView will process the resources in the order in which they
       * are specified by this array.
       *
       * The live journal gets listed first since it can continue to
       * receive writes and therefore logically comes before any other
       * resource in the ordering since any writes on the live index on
       * the journal will be more recent than the data on the index
       * segment.
       *
       * Normally, each successive entry in the resources[] will have an
       * earlier createTime (smaller number) than the one that follows it.
       * However, there is one exception. The createTime of the live
       * journal MAY be less than the createTime of index segments created
       * from that journal - this will be true if those indices are
       * created from a historical view found on that journal and put into
       * play while the journal is still the live journal. To work around
       * this we start at the 2nd entry in the array.
       */

      /*
       * Note: The practice of sending and index segment generated on one
       * data service to another data service introduces another way in
       * which the resource timestamp order can be broken. During the next
       * synchronous overflow event you can see things like this:
       *
       * resourceMetadata=[
       * JournalMetadata{filename=journal28417.jnl,uuid=add43d12-29b5-44e5-b26a-ae1b0694f67d,createTime=1236974533730},
       * JournalMetadata{filename=journal28409.jnl,uuid=b954caf8-431b-42ae-9453-4c009398bec2,createTime=1236974293720},
       * SegmentMetadata{filename=U8000_spo_OSP_part00050_28412.seg,uuid=cd954860-76fa-41ff-b788-e73a21b2c306,createTime=1236974525108},
       * SegmentMetadata{filename=U8000_spo_OSP_part00050_28411.seg,uuid=35840589-6fb5-4691-b271-cf660186cd4b,createTime=1236974523976} ]
       *
       * This is in fact well-formed. However, because the index segments
       * were generated on a different host, the create times get out of
       * wack. For that reason, I have disabled checking here.
       */
      final boolean checkCreateTimes = false;

      if (checkCreateTimes && resources.length > 2) {

        long lastTimestamp = resources[1 /*2ndEntry*/].getCreateTime();

        for (int i = 2 /* 3rd entry */; i < resources.length; i++) {

          // createTime of the resource.
          final long thisTimestamp = resources[i].getCreateTime();

          if (lastTimestamp <= thisTimestamp) {

            throw new RuntimeException(
                "Resources out of timestamp order @ index=" + i + " : " + this);
          }

          lastTimestamp = resources[i].getCreateTime();
        }
      }
    }
  }

  public final int getPartitionId() {

    return partitionId;
  }

  /*
   * <code>-1</code> unless this index partition is the target for a move, in which case this is the
   * partition identifier of the source index partition and the move operation has not been
   * completed. This property is used to prevent the target data service from de-defining the index
   * partition using a split, join or move operation while the MOVE operation is proceeding. The
   * property is cleared to <code>-1</code> (which is an invalid index partition identifier) once
   * the move has been completed successfully.
   *
   * @deprecated MoveTask manages without this field (it was required by the previous MOVE
   *     implementation).
   */
  public final int getSourcePartitionId() {

    return sourcePartitionId;
  }

  public final byte[] getLeftSeparatorKey() {

    return leftSeparatorKey;
  }

  public final byte[] getRightSeparatorKey() {

    return rightSeparatorKey;
  }

  /*
   * Description of the resources required to materialize a view of the index partition (optional,
   * but required for a {@link BTree}).
   *
   * <p>The entries in the array reflect the creation time of the resources. The earliest resource
   * is listed first. The most recently created resource is listed last. The order of the resources
   * corresponds to the order in which a fused view of the index partition will be read. Reads begin
   * with the most "recent" data for the index partition and stop as soon as there is a "hit" on one
   * of the resources (including a hit on a deleted index entry).
   *
   * <p>When present, the #of sources in the index partition view includes: the mutable {@link
   * BTree}, any {@link BTree}s on historical journal(s) still incorporated into the view, and any
   * {@link IndexSegment}s incorporated into the view.
   *
   * <p>Note: the {@link IResourceMetadata}[] is only available when the {@link
   * LocalPartitionMetadata} is attached to the {@link IndexMetadata} of a {@link BTree} and is NOT
   * defined when the {@link LocalPartitionMetadata} is attached to an {@link IndexSegment}. The
   * reason is that the index partition view is always described by the {@link BTree} and that view
   * evolves as journals overflow. On the other hand, {@link IndexSegment}s are used as resources in
   * index partition views but exist in a one to many relationship to those views.
   */
  public final IResourceMetadata[] getResources() {

    return resources;
  }

  /*
   * The reason why an index partition was created together with some metadata about when it was
   * created.
   */
  public final IndexPartitionCause getIndexPartitionCause() {

    return cause;
  }

  //    /*
  //     * A history of the changes to the index partition.
  //     *
  //     * @deprecated I've essentially disabled the history (it is always empty
  //     *             when it is persisted). I found it nearly impossible to read.
  //     *             There are much saner ways to track what is going on in the
  //     *             federation. An analysis of the {@link Event} log is much more
  //     *             useful. If nothing else, you could examine the index
  //     *             partition in the metadata index by scanning the commit points
  //     *             and reading its state in each commit and reporting all state
  //     *             changes.
  //     */
  //    final public String getHistory() {
  //
  //        return history;
  //
  //    }

  public final int hashCode() {

    // per the interface contract.
    return partitionId;
  }

  // Note: used by assertEquals in the test cases.
  public boolean equals(final Object o) {

    if (this == o) return true;

    final LocalPartitionMetadata o2 = (LocalPartitionMetadata) o;

    if (partitionId != o2.partitionId) return false;

    if (!BytesUtil.bytesEqual(leftSeparatorKey, o2.leftSeparatorKey)) {

      return false;
    }

    if (rightSeparatorKey == null) {

      if (o2.rightSeparatorKey != null) return false;

    } else {

      if (!BytesUtil.bytesEqual(rightSeparatorKey, o2.rightSeparatorKey)) {

        return false;
      }
    }

    if (resources.length != o2.resources.length) return false;

    for (int i = 0; i < resources.length; i++) {

      if (!resources[i].equals(o2.resources[i])) return false;
    }

    return true;
  }

  public String toString() {

    return "{ partitionId="
        + partitionId
        + (sourcePartitionId != -1 ? ", sourcePartitionId=" + sourcePartitionId : "")
        + ", leftSeparator="
        + BytesUtil.toString(leftSeparatorKey)
        + ", rightSeparator="
        + BytesUtil.toString(rightSeparatorKey)
        + ", resourceMetadata="
        + Arrays.toString(resources)
        + ", cause="
        + cause
        +
        //        ", history="+history+
        "}";
  }

  /*
   * Externalizable
   */

  private static final transient short VERSION0 = 0x0;

  /*
   * This version adds support for {@link IResourceMetadata#getCommitTime()}, but that field is only
   * serialized for a journal.
   */
  private static final transient short VERSION1 = 0x1;

  /*
   * This version serializes the {@link #partitionId} as 32-bits clean and gets rid of the <code>
   * history</code> field.
   */
  private static final transient short VERSION2 = 0x2;

  /** The current version. */
  private static final transient short VERSION = VERSION2;

  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {

    final short version = ShortPacker.unpackShort(in);

    switch (version) {
      case VERSION0:
      case VERSION1:
      case VERSION2:
        break;
      default:
        throw new IOException("Unknown version: " + version);
    }

    if (version < VERSION2) {
      partitionId = (int) LongPacker.unpackLong(in);
    } else {
      partitionId = in.readInt();
    }

    sourcePartitionId = in.readInt(); // MAY be -1.

    final int nresources = ShortPacker.unpackShort(in);

    final int leftLen = (int) LongPacker.unpackLong(in);

    final int rightLen = (int) LongPacker.unpackLong(in);

    leftSeparatorKey = new byte[leftLen];

    in.readFully(leftSeparatorKey);

    if (rightLen != 0) {

      rightSeparatorKey = new byte[rightLen];

      in.readFully(rightSeparatorKey);

    } else {

      rightSeparatorKey = null;
    }

    cause = (IndexPartitionCause) in.readObject();

    if (version < VERSION2) {
      /* history = */ in.readUTF();
    }

    resources = nresources > 0 ? new IResourceMetadata[nresources] : null;

    for (int j = 0; j < nresources; j++) {

      final boolean isIndexSegment = in.readBoolean();

      final String filename = in.readUTF();

      //            long nbytes = LongPacker.unpackLong(in);

      final UUID uuid = new UUID(in.readLong() /*MSB*/, in.readLong() /*LSB*/);

      final long createTime = in.readLong();

      long commitTime = 0L;
      if (version >= VERSION1 && !isIndexSegment) {

        commitTime = in.readLong();
      }

      resources[j] =
          (isIndexSegment
              ? new SegmentMetadata(filename, /*nbytes,*/ uuid, createTime)
              : new JournalMetadata(filename, /*nbytes,*/ uuid, createTime, commitTime));
    }
  }

  public void writeExternal(final ObjectOutput out) throws IOException {

    ShortPacker.packShort(out, VERSION);

    if (VERSION < VERSION2) {
      LongPacker.packLong(out, partitionId);
    } else {
      out.writeInt(partitionId);
    }

    out.writeInt(sourcePartitionId); // MAY be -1.

    final int nresources = (resources == null ? 0 : resources.length);

    assert nresources < Short.MAX_VALUE;

    ShortPacker.packShort(out, (short) nresources);

    LongPacker.packLong(out, leftSeparatorKey.length);

    LongPacker.packLong(out, rightSeparatorKey == null ? 0 : rightSeparatorKey.length);

    out.write(leftSeparatorKey);

    if (rightSeparatorKey != null) {

      out.write(rightSeparatorKey);
    }

    out.writeObject(cause);

    if (VERSION < VERSION2) {
      out.writeUTF(""); // getTruncatedHistory()
    }

    /*
     * Note: we serialize using the IResourceMetadata interface so that we
     * can handle different subclasses and then special case the
     * deserialization based on the boolean flag. This is significantly more
     * compact than using an Externalizable for each ResourceMetadata object
     * since we do not have to write the class names for those objects.
     */

    for (int j = 0; j < nresources; j++) {

      final IResourceMetadata rmd = resources[j];

      final boolean isSegment = rmd.isIndexSegment();

      out.writeBoolean(isSegment);

      out.writeUTF(rmd.getFile());

      //            LongPacker.packLong(out,rmd.size());

      final UUID resourceUUID = rmd.getUUID();

      out.writeLong(resourceUUID.getMostSignificantBits());

      out.writeLong(resourceUUID.getLeastSignificantBits());

      out.writeLong(rmd.getCreateTime());

      if (!isSegment) {

        out.writeLong(rmd.getCommitTime());
      }
    }
  }
}
