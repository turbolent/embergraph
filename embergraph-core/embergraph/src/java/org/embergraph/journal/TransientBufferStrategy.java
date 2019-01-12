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
package org.embergraph.journal;

import java.io.File;
import java.nio.ByteBuffer;

/*
 * Transient buffer strategy uses a direct buffer but never writes on disk.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @see BufferMode#Transient
 */
public class TransientBufferStrategy extends BasicBufferStrategy {

  /*
   * The root blocks.
   */
  private final IRootBlockView[] rootBlocks = new IRootBlockView[2];

  /** Either zero (0) or one (1). */
  private int currentRootBlock = 0;

  /*
   * Note: I have not observed much performance gain from the use of a direct buffer for the
   * transient mode. Further, there is a BUG related to release of direct {@link ByteBuffer}s so
   * they are NOT in general recommended here.
   */
  TransientBufferStrategy(
      int offsetBits, long initialExtent, long maximumExtent, boolean useDirectBuffers) {

    super(
        maximumExtent,
        offsetBits,
        0, // nextOffset
        0, // headerSize
        initialExtent,
        BufferMode.Transient,
        (useDirectBuffers
            ? ByteBuffer.allocateDirect((int) initialExtent)
            : ByteBuffer.allocate((int) initialExtent)),
        false // readOnly
        );
  }

  public void deleteResources() {

    if (isOpen()) {

      throw new IllegalStateException();
    }

    // NOP.

  }

  public void force(boolean metadata) {

    // NOP.

  }

  /** Always returns <code>null</code>. */
  public File getFile() {

    return null;
  }

  public final boolean isStable() {

    return false;
  }

  public boolean isFullyBuffered() {

    return true;
  }

  public void writeRootBlock(final IRootBlockView rootBlock, final ForceEnum forceOnCommit) {

    if (rootBlock == null) throw new IllegalArgumentException();

    currentRootBlock = rootBlock.isRootBlock0() ? 0 : 1;

    rootBlocks[currentRootBlock] = rootBlock;
  }

  /*
   * There is no header.
   *
   * @return ZERO (0).
   */
  public final int getHeaderSize() {

    return 0;
  }

  public ByteBuffer readRootBlock(boolean rootBlock0) {

    return rootBlocks[rootBlock0 ? 0 : 1].asReadOnlyBuffer();
  }

  /** Protocol support for HAWrite */
  public void setNextOffset(long lastOffset) {
    // void
  }
}
