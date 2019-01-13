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
/*
 * Created on Apr 7, 2007
 */

package org.embergraph.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;

/*
 * Fast special purpose serialization onto a managed byte[] buffer conforming to the {@link
 * DataOutput} API.
 *
 * <p>Note: The base classes provide all of the same functionality without declaring {@link
 * IOException} as a thrown exception.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DataOutputBuffer extends ByteArrayBuffer implements DataOutput {

  //    private static transient final Logger log = Logger
  //            .getLogger(DataOutputBuffer.class);

  /** Uses the default for {@link ByteArrayBuffer#ByteArrayBuffer()}. */
  public DataOutputBuffer() {

    super();
  }

  /** @param initialCapacity The initial capacity of the internal byte[]. */
  public DataOutputBuffer(final int initialCapacity) {

    super(initialCapacity);
  }

  /*
   * @param len The #of bytes of data already in the provided buffer.
   * @param buf The buffer, with <i>len</i> pre-existing bytes of valid data. The buffer reference
   *     is used directly rather than making a copy of the data.
   */
  public DataOutputBuffer(final int len, final byte[] buf) {

    super(len /* pos */, buf.length /* readLimit */, buf);
  }

  /*
   * Reads the entire input stream into the buffer. The data are then available in {@link #buf} from
   * position 0 (inclusive) through position {@link #pos} (exclusive).
   */
  public DataOutputBuffer(final InputStream in) throws IOException {

    super();

    // temporary buffer for read from the input stream.
    final byte[] b = new byte[remaining()];

    while (true) {

      int nread = in.read(b);

      if (nread == -1) break;

      write(b, 0, nread);
    }
  }

  /*
   * Reads the entire input stream into the buffer. The data are then available in {@link #buf} from
   * position 0 (inclusive) through position {@link #pos} (exclusive).
   */
  public DataOutputBuffer(final ObjectInput in) throws IOException {

    super();

    // temporary buffer for read from the input stream.
    byte[] b = new byte[remaining()];

    while (true) {

      int nread = in.read(b);

      if (nread == -1) break;

      write(b, 0, nread);
    }
  }

  /** Conforms the return type to an instance of this class. {@inheritDoc} */
  public DataOutputBuffer reset() {

    return (DataOutputBuffer) super.reset();
  }

  /*
   * Read <i>len</i> bytes into the buffer.
   *
   * @param in The input source.
   * @param len The #of bytes to read.
   * @throws EOFException if the EOF is reached before <i>len</i> bytes have been read.
   * @throws IOException if an I/O error occurs.
   * @todo read many bytes at a time.
   * @todo write test.
   */
  public final void write(final DataInput in, final int len) throws IOException {

    ensureCapacity(len);

    int c = 0;

    byte b;

    while (c < len) {

      b = in.readByte();

      buf[this.pos++] = (byte) (b & 0xff);

      c++;
    }

    limit = pos;
  }

  public final void writeBoolean(final boolean v) {

    if (pos + 1 > buf.length) ensureCapacity(pos + 1);

    buf[pos++] = v ? (byte) 1 : (byte) 0;

    limit = pos;
  }

  public final void writeByte(final int v) {

    if (pos + 1 > buf.length) ensureCapacity(pos + 1);

    buf[pos++] = (byte) (v & 0xff);

    limit = pos;
  }

  public final void writeDouble(final double v) {

    putDouble(v);
  }

  public final void writeFloat(final float v) {

    putFloat(v);
  }

  public final void writeInt(final int v) {

    putInt(v);
  }

  public final void writeLong(final long v) {

    putLong(v);
  }

  public final void writeShort(final int v) {

    //        if (len + 2 > buf.length)
    //            ensureCapacity(len + 2);
    //
    //        // big-endian
    //        buf[len++] = (byte) (v >>> 8);
    //        buf[len++] = (byte) (v >>> 0);

    putShort((short) v);
  }

  public final void writeChar(final int v) {

    if (pos + 2 > buf.length) ensureCapacity(pos + 2);

    buf[pos++] = (byte) (v >>> 8);
    buf[pos++] = (byte) (v >>> 0);

    limit = pos;
  }

  public void writeBytes(final String s) {

    // #of bytes == #of characters (writes only the low bytes).
    final int len = s.length();

    if (this.pos + len > buf.length) ensureCapacity(this.pos + len);

    for (int i = 0; i < len; i++) {

      write((byte) s.charAt(i));
    }

    limit = pos;
  }

  public void writeChars(final String s) {

    // #of characters (twice as many bytes).
    final int len = s.length();

    if (this.pos + (len * 2) > buf.length) ensureCapacity(this.pos + (len * 2));

    for (int i = 0; i < len; i++) {

      final char v = s.charAt(i);

      buf[this.pos++] = (byte) (v >>> 8);
      buf[this.pos++] = (byte) (v >>> 0);

      //            write((v >>> 8) & 0xFF);
      //
      //            write((v >>> 0) & 0xFF);

    }

    limit = pos;
  }

  // This inefficiency has been fixed.
  //    /*
  //     * @todo This is not wildly efficient (it would be fine if
  //     *       DataOutputStream#writeUTF(String str, DataOutput out)} was public)
  //     *       but the use cases for serializing the nodes and leaves of a btree
  //     *       do not suggest any requirement for Unicode (if you assume that the
  //     *       application values are already being serialized as byte[]s - which
  //     *       is always true when there is a client-server divide). It is used by
  //     *       {@link Name2Addr} to store the index names. It used to be used to
  //     *       write Value into the lexicon, but that is now handled using a
  //     *       {@link IUnicodeCompressor}.
  //     *
  //     * @todo Consider changing the access modified on the desired method using
  //     *       reflection.
  //     */
  public void writeUTF(final String str) throws IOException {

    // This is the old, inefficient version.
    //        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //
    //        final DataOutputStream dos = new DataOutputStream(baos);
    //
    //        dos.writeUTF(str);
    //
    //        dos.flush();
    //
    //        write(baos.toByteArray());
    //        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // This is the new, more efficient version.
    final DataOutputStream dos = new DataOutputStream(this);

    dos.writeUTF(str);

    dos.flush();

    //        write(baos.toByteArray());

  }

  /*
   * Version of {@link #writeUTF(String)} which wraps the {@link IOException}.
   *
   * @param str The string.
   */
  public void writeUTF2(final String str) {
    try {
      writeUTF(str);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
