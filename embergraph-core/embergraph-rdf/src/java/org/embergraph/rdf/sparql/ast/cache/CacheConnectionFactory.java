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
 * Created on Mar 26, 2012
 */

package org.embergraph.rdf.sparql.ast.cache;

import java.util.WeakHashMap;
import org.apache.log4j.Logger;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.journal.AbstractJournal;
import org.embergraph.rdf.sparql.ast.QueryHints;

/*
 * A factory pattern for the {@link ICacheConnection}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class CacheConnectionFactory {

  private static final Logger log = Logger.getLogger(CacheConnectionFactory.class);

  /*
   * Weak key cache to enforce the singleton pattern.
   *
   * <p>Note: We do not want to keep any {@link ICacheConnection} objects wired into the memory
   * unless the application is holding a hard reference to the {@link QueryEngine}.
   */
  private static WeakHashMap<QueryEngine, ICacheConnection> instanceCache =
      new WeakHashMap<>();

  /*
   * Singleton factory test (does not create the cache).
   *
   * @param queryEngine The {@link QueryEngine}.
   * @return The query controller iff one has been obtained from the factory and its weak reference
   *     has not been cleared.
   */
  public static ICacheConnection getExistingCacheConnection(final QueryEngine queryEngine) {

    return instanceCache.get(queryEngine);
  }

  /*
   * Singleton factory.
   *
   * @param queryEngine The {@link QueryEngine}.
   * @return The {@link ICacheConnection}.
   */
  public static ICacheConnection getCacheConnection(final QueryEngine queryEngine) {

    if (queryEngine == null) throw new IllegalArgumentException();

    if (!QueryHints.CACHE_ENABLED || !(queryEngine.getIndexManager() instanceof AbstractJournal)) {

      /*
       * Feature is disabled.
       *
       * <p>Note: IBTreeManager does not support HTree methods, so TemporaryStore can not substitute
       * for AbstractJournal in CacheConnectionImpl. This is a GIST issue. There are a number of
       * methods that we would need to implement and they would also need to be on the IJournal
       * implementations in AbstractTask (Might be better to just directly wire the solutions are
       * named and durable rather than cachable).
       */
      return null;
    }

    ICacheConnection cache = instanceCache.get(queryEngine);

    if (cache == null) {

      synchronized (instanceCache) {
        if ((cache = instanceCache.get(queryEngine)) == null) {

          cache = newCacheConnection(queryEngine);

          instanceCache.put(queryEngine, cache);
        }
      }
    }

    return cache;
  }

  /*
   * Initialize a new {@link ICacheConnection} instance (or a connection to a cache fabric).
   *
   * @param queryEngine The query controller.
   * @return The new {@link ICacheConnection}.
   */
  private static ICacheConnection newCacheConnection(final QueryEngine queryEngine) {

    if (log.isInfoEnabled()) log.info("Initiallizing: " + queryEngine);

    final ICacheConnection cache = new CacheConnectionImpl(queryEngine);

    cache.init();

    return cache;
  }

  /** Return the #of live {@link ICacheConnection} instances. */
  public static int getCacheCount() {

    return instanceCache.size();
  }
}
