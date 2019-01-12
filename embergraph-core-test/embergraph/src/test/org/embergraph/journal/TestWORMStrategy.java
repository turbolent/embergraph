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
 * Created on Oct 14, 2006
 */

package org.embergraph.journal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import org.embergraph.io.DirectBufferPool;
import org.embergraph.rawstore.IRawStore;

/**
 * Test suite for {@link WORMStrategy} journal.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @see TestWORMStrategyNoCache
 * @see TestWORMStrategyOneCacheBuffer
 */
public class TestWORMStrategy extends AbstractJournalTestCase {

  public TestWORMStrategy() {
    super();
  }

  public TestWORMStrategy(String name) {
    super(name);
  }

  public static Test suite() {

    final TestWORMStrategy delegate = new TestWORMStrategy(); // !!!! THIS CLASS !!!!

    /*
     * Use a proxy test suite and specify the delegate.
     */

    final ProxyTestSuite suite = new ProxyTestSuite(delegate, "DiskWORM Journal Test Suite");

    /*
     * List any non-proxied tests (typically bootstrapping tests).
     */

    // tests defined by this class.
    suite.addTestSuite(TestWORMStrategy.class);

    // test suite for the IRawStore api.
    suite.addTestSuite(TestRawStore.class);

    // test suite for handling asynchronous close of the file channel.
    suite.addTestSuite(TestInterrupts.class);

    // test suite for MROW correctness.
    suite.addTestSuite(TestMROW.class);

    // test suite for MRMW correctness.
    suite.addTestSuite(TestMRMW.class);

    /*
     * Pickup the basic journal test suite. This is a proxied test suite, so
     * all the tests will run with the configuration specified in this test
     * class and its optional .properties file.
     */
    suite.addTest(TestJournalBasics.suite());

    return suite;
  }

  @Override
  public Properties getProperties() {

    final Properties properties = super.getProperties();

    properties.setProperty(Journal.Options.COLLECT_PLATFORM_STATISTICS, "false");

    properties.setProperty(Journal.Options.COLLECT_QUEUE_STATISTICS, "false");

    properties.setProperty(Journal.Options.HTTPD_PORT, "-1" /* none */);

    properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

    //        properties.setProperty(Options.CREATE_TEMP_FILE, "true");

    properties.setProperty(Options.DELETE_ON_EXIT, "true");

    properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

    return properties;
  }

  /**
   * Verify normal operation and basic assumptions when creating a new journal using {@link
   * BufferMode#DiskWORM}.
   *
   * @throws IOException
   */
  public void test_create_disk01() throws IOException {

    final Properties properties = getProperties();

    final Journal journal = new Journal(properties);

    try {

      final WORMStrategy bufferStrategy = (WORMStrategy) journal.getBufferStrategy();

      assertTrue("isStable", bufferStrategy.isStable());
      assertFalse("isFullyBuffered", bufferStrategy.isFullyBuffered());
      // assertEquals(Options.FILE, properties.getProperty(Options.FILE),
      // bufferStrategy.file.toString());
      assertEquals(
          Options.INITIAL_EXTENT,
          Long.parseLong(Options.DEFAULT_INITIAL_EXTENT),
          bufferStrategy.getInitialExtent());
      assertEquals(
          Options.MAXIMUM_EXTENT,
          0L /* soft limit for disk mode */,
          bufferStrategy.getMaximumExtent());
      assertNotNull("raf", bufferStrategy.getRandomAccessFile());
      assertEquals(Options.BUFFER_MODE, BufferMode.DiskWORM, bufferStrategy.getBufferMode());

    } finally {

      journal.destroy();
    }
  }

  /**
   * Unit test verifies that {@link Options#CREATE} may be used to initialize a journal on a newly
   * created empty file.
   *
   * @throws IOException
   */
  public void test_create_emptyFile() throws IOException {

    final File file = File.createTempFile(getName(), Options.JNL);

    final Properties properties = new Properties();

    properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

    properties.setProperty(Options.FILE, file.toString());

    properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

    final Journal journal = new Journal(properties);

    try {

      assertEquals(file, journal.getFile());

    } finally {

      journal.destroy();
    }
  }

  /**
   * Test suite integration for {@link AbstractRestartSafeTestCase}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class TestRawStore extends AbstractRestartSafeTestCase {

    public TestRawStore() {
      super();
    }

    public TestRawStore(String name) {
      super(name);
    }

    @Override
    protected BufferMode getBufferMode() {

      return BufferMode.DiskWORM;
    }
  }

  /**
   * Test suite integration for {@link AbstractInterruptsTestCase}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class TestInterrupts extends AbstractInterruptsTestCase {

    public TestInterrupts() {
      super();
    }

    public TestInterrupts(String name) {
      super(name);
    }

    @Override
    protected IRawStore getStore() {

      final Properties properties = getProperties();

      properties.setProperty(Options.DELETE_ON_EXIT, "true");

      properties.setProperty(Options.CREATE_TEMP_FILE, "true");

      properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

      properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

      return new Journal(properties); // .getBufferStrategy();
      //            return new Journal(properties);

    }
  }

  /**
   * Test suite integration for {@link AbstractMROWTestCase}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class TestMROW extends AbstractMROWTestCase {

    public TestMROW() {
      super();
    }

    public TestMROW(String name) {
      super(name);
    }

    @Override
    protected Journal getStore() {

      final Properties properties = getProperties();

      properties.setProperty(Options.CREATE_TEMP_FILE, "true");

      properties.setProperty(Options.DELETE_ON_EXIT, "true");

      properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

      properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

      return new Journal(properties); // .getBufferStrategy();
    }
  }

  /**
   * Test suite integration for {@link AbstractMRMWTestCase}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  public static class TestMRMW extends AbstractMRMWTestCase {

    public TestMRMW() {
      super();
    }

    public TestMRMW(String name) {
      super(name);
    }

    @Override
    protected IRawStore getStore() {

      final Properties properties = getProperties();

      properties.setProperty(Options.CREATE_TEMP_FILE, "true");

      properties.setProperty(Options.DELETE_ON_EXIT, "true");

      properties.setProperty(Options.BUFFER_MODE, BufferMode.DiskWORM.toString());

      properties.setProperty(Options.WRITE_CACHE_ENABLED, "" + writeCacheEnabled);

      /*
       * The following two properties are dialed way down in order to
       * raise the probability that we will observe the following error
       * during this test.
       *
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6371642
       *
       * FIXME We should make the MRMW test harder and focus on
       * interleaving concurrent extensions of the backing store for both
       * WORM and R/W stores.
       */

      // Note: Use a relatively small initial extent.
      properties.setProperty(
          Options.INITIAL_EXTENT, "" + DirectBufferPool.INSTANCE.getBufferCapacity() * 1);

      // Note: Use a relatively small extension each time.
      properties.setProperty(
          Options.MINIMUM_EXTENSION,
          "" + (long) (DirectBufferPool.INSTANCE.getBufferCapacity() * 1.1));

      return new Journal(properties); // .getBufferStrategy();
    }
  }

  /**
   * The write cache is enabled for this version of the test suite and the default #of write cache
   * buffers will be used as specified by {@link Options#DEFAULT_WRITE_CACHE_BUFFER_COUNT}.
   */
  private static final boolean writeCacheEnabled = true;
}
