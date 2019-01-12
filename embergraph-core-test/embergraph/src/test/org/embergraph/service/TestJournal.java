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
 * Created on Oct 2, 2008
 */

package org.embergraph.service;

import java.util.Properties;
import org.embergraph.journal.AbstractJournalTestCase;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.journal.Options;

/*
 * Delegate for {@link ProxyTestCase}s for services running against a {@link Journal}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestJournal extends AbstractJournalTestCase {

  public TestJournal() {
    super();
  }

  public TestJournal(String name) {
    super(name);
  }

  public Properties getProperties() {

    final Properties properties = super.getProperties();

    properties.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

    properties.setProperty(Options.CREATE_TEMP_FILE, "true");

    properties.setProperty(Options.DELETE_ON_EXIT, "true");

    properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

    return properties;
  }

  /*
   * Note: Since the write cache is a direct ByteBuffer we have to make it very small (or disable it
   * entirely) when running the test suite or the JVM will run out of memory - this is exactly the
   * same (Sun) bug which motivates us to reuse the same ByteBuffer when we overflow a journal using
   * a write cache. Since small write caches are disallowed, we wind up testing with the write cache
   * disabled!
   */
  private static final boolean writeCacheEnabled = true; // 512;

  /*
   * Extends the basic behavior to force a commit of the {@link Journal}. This makes the {@link
   * Journal} appear to have "auto-commit" semantics from the perspective of the unit tests that are
   * written to the assumption that the {@link IIndexManager} is an {@link IEmbergraphFederation}.
   * Otherwise those unit tests tend not to force a commit and hence restart-safe tests tend to fail
   * for one reason or another.
   */
  protected Journal reopenStore(final Journal store) {

    store.commit();

    return super.reopenStore(store);
  }
}
