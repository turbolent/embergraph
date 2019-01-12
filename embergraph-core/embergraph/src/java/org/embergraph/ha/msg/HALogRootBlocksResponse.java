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
package org.embergraph.ha.msg;

import java.nio.ByteBuffer;
import org.embergraph.io.ChecksumUtility;
import org.embergraph.journal.IRootBlockView;
import org.embergraph.journal.RootBlockView;
import org.embergraph.util.BytesUtil;

public class HALogRootBlocksResponse implements IHALogRootBlocksResponse {

  /** */
  private static final long serialVersionUID = 1L;

  private final boolean openIsRootBlock0;
  private final boolean closeIsRootBlock0;

  private final byte[] openData;
  private final byte[] closeData;

  public HALogRootBlocksResponse(
      final IRootBlockView openRootBlock, final IRootBlockView closeRootBlock) {

    if (openRootBlock == null) throw new IllegalArgumentException();

    if (closeRootBlock == null) throw new IllegalArgumentException();

    this.openIsRootBlock0 = openRootBlock.isRootBlock0();

    this.closeIsRootBlock0 = closeRootBlock.isRootBlock0();

    this.openData = BytesUtil.toArray(openRootBlock.asReadOnlyBuffer());

    this.closeData = BytesUtil.toArray(closeRootBlock.asReadOnlyBuffer());
  }

  @Override
  public IRootBlockView getOpenRootBlock() {

    return new RootBlockView(openIsRootBlock0, ByteBuffer.wrap(openData), new ChecksumUtility());
  }

  @Override
  public IRootBlockView getCloseRootBlock() {

    return new RootBlockView(closeIsRootBlock0, ByteBuffer.wrap(closeData), new ChecksumUtility());
  }

  public String toString() {

    return getClass()
        + "{openRootBlock="
        + getOpenRootBlock()
        + ", closeRootBlock="
        + getCloseRootBlock()
        + "}";
  }
}
