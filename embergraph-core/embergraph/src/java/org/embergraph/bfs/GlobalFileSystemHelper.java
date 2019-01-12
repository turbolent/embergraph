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
 * Created on Jul 3, 2008
 */

package org.embergraph.bfs;

import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.ITx;

/*
* Helper class.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class GlobalFileSystemHelper {

  public static final transient String GLOBAL_FILE_SYSTEM_NAMESPACE = "__globalFileSystem";

  private final IIndexManager indexManager;

  protected static final transient Logger log = Logger.getLogger(GlobalFileSystemHelper.class);

  protected static final boolean INFO = log.isInfoEnabled();

  public GlobalFileSystemHelper(IIndexManager indexManager) {

    if (indexManager == null) throw new IllegalArgumentException();

    this.indexManager = indexManager;
  }

  /** The {@link ITx#UNISOLATED} view. */
  public synchronized EmbergraphFileSystem getGlobalFileSystem() {

    if (INFO) log.info("");

    if (globalRowStore == null) {

      // setup the repository view.
      globalRowStore =
          new EmbergraphFileSystem(
              indexManager, GLOBAL_FILE_SYSTEM_NAMESPACE, ITx.UNISOLATED, new Properties());

      // register the indices.
      globalRowStore.create();
    }

    return globalRowStore;
  }

  private transient EmbergraphFileSystem globalRowStore;

  /** {@link ITx#READ_COMMITTED} view. */
  public EmbergraphFileSystem getReadCommitted() {

    if (INFO) log.info("");

    return (EmbergraphFileSystem)
        indexManager.getResourceLocator().locate(GLOBAL_FILE_SYSTEM_NAMESPACE, ITx.READ_COMMITTED);
  }
}
