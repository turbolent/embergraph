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

package org.embergraph.rwstore;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.embergraph.io.IBufferAccess;
import org.embergraph.io.IReopenChannel;
import org.embergraph.io.writecache.IBackingReader;
import org.embergraph.io.writecache.WriteCache;
import org.embergraph.io.writecache.WriteCache.FileChannelScatteredWriteCache;
import org.embergraph.io.writecache.WriteCacheService;
import org.embergraph.quorum.Quorum;

/*
 * Defines the WriteCacheService to be used by the RWStore.
 *
 * @author mgc
 */
public class RWWriteCacheService extends WriteCacheService implements IWriteCacheManager {

  protected static final Logger log = Logger.getLogger(RWWriteCacheService.class);

  public RWWriteCacheService(
      final int nbuffers,
      final int minCleanListSize,
      final int readBuffers,
      final boolean prefixWrites,
      final int compactionThreshold,
      final int hotCacheSize,
      final int hotCacheThreshold,
      final long fileExtent,
      final IReopenChannel<? extends Channel> opener,
      final Quorum quorum,
      final IBackingReader reader)
      throws InterruptedException, IOException {

    super(
        nbuffers,
        minCleanListSize,
        readBuffers,
        prefixWrites,
        compactionThreshold,
        hotCacheSize,
        hotCacheThreshold,
        true /* useChecksum */,
        fileExtent,
        opener,
        quorum,
        reader);
  }

  /** The scattered write cache supports compaction. */
  @Override
  protected final boolean canCompact() {
    return true;
  }

  /*
   * Provide default {@link FileChannelScatteredWriteCache}.
   *
   * <p>Note: This is used by the unit tests, but not by the {@link RWStore}.
   */
  @Override
  public WriteCache newWriteCache(
      final IBufferAccess buf,
      final boolean useChecksum,
      final boolean bufferHasData,
      final IReopenChannel<? extends Channel> opener,
      final long fileExtent)
      throws InterruptedException {

    //        final boolean highlyAvailable = getQuorum() != null
    //                && getQuorum().isHighlyAvailable();
    final boolean highlyAvailable = getQuorum() != null;

    return new FileChannelScatteredWriteCache(
        buf,
        true /* useChecksum */,
        highlyAvailable,
        bufferHasData,
        (IReopenChannel<FileChannel>) opener,
        fileExtent,
        null /* BufferedWrite */);
  }

  @Override
  public boolean removeWriteToAddr(final long address, final int latchedAddr) {

    return clearWrite(address, latchedAddr);
  }
}
