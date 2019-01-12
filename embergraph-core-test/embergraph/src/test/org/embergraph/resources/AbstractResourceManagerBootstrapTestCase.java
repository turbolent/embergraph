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
 * Created on Feb 22, 2008
 */

package org.embergraph.resources;

import java.io.File;
import java.util.Properties;
import junit.framework.TestCase2;
import org.embergraph.resources.ResourceManager.Options;

/**
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class AbstractResourceManagerBootstrapTestCase extends TestCase2 {

  /** */
  public AbstractResourceManagerBootstrapTestCase() {
    super();
  }

  /** @param arg0 */
  public AbstractResourceManagerBootstrapTestCase(final String arg0) {
    super(arg0);
  }

  public Properties getProperties() {

    Properties properties = new Properties(super.getProperties());

    if (log.isInfoEnabled()) log.info("Setting " + Options.DATA_DIR + "=" + dataDir);

    properties.setProperty(
        org.embergraph.resources.ResourceManager.Options.DATA_DIR, dataDir.toString());

    //        // disable the write cache to avoid memory leak in the test suite.
    //        properties.setProperty(Options.WRITE_CACHE_ENABLED, "false");

    return properties;
  }

  /** The data directory. */
  File dataDir;
  /** The subdirectory containing the journal resources. */
  File journalsDir;
  /** The subdirectory spanning the index segment resources. */
  File segmentsDir;
  /** The temp directory. */
  File tmpDir = new File(System.getProperty("java.io.tmpdir"));

  /** Sets up the per-test data directory. */
  @Override
  protected void setUp() throws Exception {

    super.setUp();

    /*
     * Create a normal temporary file whose path is the path of the data
     * directory and then delete the temporary file.
     */

    dataDir = File.createTempFile(getName(), "", tmpDir).getCanonicalFile();

    assertTrue(dataDir.delete());

    assertFalse(dataDir.exists());

    journalsDir = new File(dataDir, "journals");

    segmentsDir = new File(dataDir, "segments");
  }

  @Override
  protected void tearDown() throws Exception {

    super.tearDown();

    if (dataDir != null) {

      recursiveDelete(dataDir);
    }

    dataDir = null;
    journalsDir = null;
    segmentsDir = null;
    tmpDir = null;
  }

  /**
   * Recursively removes any files and subdirectories and then removes the file (or directory)
   * itself.
   *
   * @param f A file or directory.
   */
  private void recursiveDelete(final File f) {

    if (f.isDirectory()) {

      final File[] children = f.listFiles();

      if (children == null) {

        // No such file or directory exists.
        return;
      }

      for (int i = 0; i < children.length; i++) {

        recursiveDelete(children[i]);
      }
    }

    if (log.isInfoEnabled()) log.info("Removing: " + f);

    if (f.exists() && !f.delete()) {

      log.warn("Could not remove: " + f);
    }
  }
}
