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

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.embergraph.io.ChecksumUtility;
import org.embergraph.journal.IRootBlockView;
import org.embergraph.journal.RootBlockView;
import org.embergraph.util.BytesUtil;

public class HASnapshotResponse implements IHASnapshotResponse, Serializable {

  private static final long serialVersionUID = 1L;

  private final boolean isRootBlock0;
  private final byte[] data;

  public HASnapshotResponse(final IRootBlockView rootBlock) {

    if (rootBlock == null) throw new IllegalArgumentException();

    this.isRootBlock0 = rootBlock.isRootBlock0();

    this.data = BytesUtil.toArray(rootBlock.asReadOnlyBuffer());
  }

  @Override
  public IRootBlockView getRootBlock() {

    return new RootBlockView(isRootBlock0, ByteBuffer.wrap(data), new ChecksumUtility());
  }
}
