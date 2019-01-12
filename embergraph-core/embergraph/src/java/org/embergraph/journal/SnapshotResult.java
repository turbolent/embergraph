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

/*
 * Default {@link ISnapshotResult} implementation.
 *
 * @author bryan
 * @see <a href="http://trac.bigdata.com/ticket/1172">Online backup for Journal </a>
 */
class SnapshotResult implements ISnapshotResult {

  private final File file;
  private final boolean compressed;
  private final IRootBlockView rootBlock;

  public SnapshotResult(final File file, final boolean compressed, final IRootBlockView rootBlock) {
    this.file = file;
    this.compressed = compressed;
    this.rootBlock = rootBlock;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public IRootBlockView getRootBlock() {
    return rootBlock;
  }

  @Override
  public boolean getCompressed() {
    return compressed;
  }
}
