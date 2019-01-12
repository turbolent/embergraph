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

package org.embergraph.rdf.sail;

import info.aduna.iteration.CloseableIteration;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import junit.extensions.proxy.IProxyTest;
import junit.framework.Test;
import org.openrdf.model.Resource;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;

/**
 * This class provides proxy delegation logic for abstract methods declared by {@link
 * AbstractEmbergraphSailTestCase} and is used to extend the set of tests that will be applied to
 * all implementations of the generic object model Java API. If you want to test a new
 * implementation, you MUST extend the {@link AbstractEmbergraphSailTestCase} instead and implement
 * its abstract methods for your implementation. This class provides an implementation neutral way
 * to add new tests, not a means for testing specific generic object model Java API implementations.
 *
 * <p>In order to add new tests for the generic object model Java APIs, you extend this class and
 * write test methods.
 *
 * @see AbstractEmbergraphTestCase
 */
public abstract class ProxyEmbergraphSailTestCase extends AbstractEmbergraphSailTestCase
    implements IProxyTest {

  //	protected final transient static Logger log =
  // Logger.getLogger(ProxyEmbergraphSailTestCase.class);

  public ProxyEmbergraphSailTestCase() {}

  public ProxyEmbergraphSailTestCase(String name) {
    super(name);
  }

  // ************************************************************
  // ************************ IProxyTest ************************
  // ************************************************************

  private Test m_delegate = null;

  public void setDelegate(Test delegate) {

    m_delegate = delegate;
  }

  public Test getDelegate() throws IllegalStateException {

    return m_delegate;
  }

  /**
   * Returns the delegate after first making sure that it is non-null and extends {@link
   * AbstractEmbergraphTestCase}.
   */
  public AbstractEmbergraphSailTestCase getOurDelegate() {

    if (m_delegate == null) {

      /*
       * This property gives the class name of the concrete instance of
       * AbstractTestSuite that we need to instantiate so that we can run
       * or debug a single test at a time! This is designed to support
       * running or debugging a single test that has failed after running
       * the entire test suite in an IDE such as Eclipse.
       *
       * Note: We reach out to System.getProperty() and not
       * getProperties() to avoid infinite recursion through
       * getOurDelegate. The only way this makes sense anyway is when you
       * define -DtestClass=... as a JVM property.
       *
       * @todo document.
       */
      String testClass = System.getProperty("testClass");
      if (testClass == null) {

        throw new IllegalStateException(
            "testClass: property not defined, could not configure delegate.");
      }
      try {
        Class cl = Class.forName(testClass);
        m_delegate = (Test) cl.newInstance();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }

      // throw new IllegalStateException
      // ( "The delegate is not configured."
      // );

    }

    if (m_delegate instanceof AbstractEmbergraphSailTestCase) {

      return (AbstractEmbergraphSailTestCase) m_delegate;
    }

    throw new IllegalStateException(
        "The delegate MUST extend "
            + AbstractEmbergraphSailTestCase.class.getName()
            + ", not "
            + m_delegate.getClass().getName());
  }

  // ************************************************************
  // ********************* proxied methods **********************
  // ************************************************************

  /*
   * Note: All methods on the delegate MUST be proxied here or they will be
   * answered by our base class which is the same Class as the delegate, but
   * whose instance fields have not been initialized! (An instance of the
   * proxy is created for each test, while one instance of the delegate serves
   * an entire suite of tests.)
   */

  private int startupActiveThreads = 0;

  @Override
  protected void setUp() throws Exception {

    startupActiveThreads = Thread.currentThread().getThreadGroup().activeCount();

    getOurDelegate().setUp(this);
  }

  private static boolean s_checkThreads = true;

  @Override
  protected void tearDown() throws Exception {

    getOurDelegate().tearDown(this);

    if (s_checkThreads) {

      final ThreadGroup grp = Thread.currentThread().getThreadGroup();
      final int tearDownActiveThreads = grp.activeCount();
      if (startupActiveThreads != tearDownActiveThreads) {
        final Thread[] threads = new Thread[tearDownActiveThreads];
        grp.enumerate(threads);
        final StringBuilder info = new StringBuilder();
        boolean first = true;
        for (Thread t : threads) {
          if (t == null) continue;
          if (!first) info.append(',');
          info.append("[" + t.getName() + "]");
          first = false;
        }

        final String failMessage =
            "Threads left active after task"
                + ": test="
                + getName()
                + ", delegate="
                + getOurDelegate().getClass().getName()
                + ", startupCount="
                + startupActiveThreads
                + ", teardownCount="
                + tearDownActiveThreads
                + ", thisThread="
                + Thread.currentThread().getName()
                + ", threads: "
                + info;

        if (grp.activeCount() != startupActiveThreads) log.error(failMessage);

        /*
         * Wait up to 2 seconds for threads to die off so the next test
         * will run more cleanly.
         */
        for (int i = 0; i < 20; i++) {
          Thread.sleep(100);
          if (grp.activeCount() != startupActiveThreads) break;
        }
      }
    }

    super.tearDown();
  }

  /** The properties as configured by the delegate. */
  @Override
  public Properties getProperties() {
    return getOurDelegate().getProperties();
  }

  /** Create a SAIL using the delegate and using the properties as configured by the delegate. */
  protected EmbergraphSail getSail() {
    return getOurDelegate().getSail(getProperties());
  }

  /**
   * Create a SAIL using the delegate using the specified properties (typically overriding one or
   * more properties).
   */
  protected EmbergraphSail getSail(Properties properties) {
    return getOurDelegate().getSail(properties);
  }

  /** Close and then re-open the SAIL. */
  protected EmbergraphSail reopenSail(EmbergraphSail sail) {
    return getOurDelegate().reopenSail(sail);
  }

  /**
   * Verifies that the iterator visits the specified objects in some arbitrary ordering and that the
   * iterator is exhausted once all expected objects have been visited. The implementation uses a
   * selection without replacement "pattern".
   */
  public static void assertSameIterationAnyOrder(
      final Resource[] expected, final CloseableIteration<?, ? extends Exception> actual)
      throws Exception {

    assertSameIterationAnyOrder("", expected, actual);
  }

  /**
   * Verifies that the iterator visits the specified objects in some arbitrary ordering and that the
   * iterator is exhausted once all expected objects have been visited. The implementation uses a
   * selection without replacement "pattern".
   */
  @SuppressWarnings("unchecked")
  public static void assertSameIterationAnyOrder(
      String msg,
      final Resource[] expected,
      final CloseableIteration<?, ? extends Exception> actual)
      throws Exception {

    // Populate a map that we will use to realize the match and
    // selection without replacement logic.

    final int nrange = expected.length;

    final java.util.Map range = new java.util.HashMap();

    for (int j = 0; j < nrange; j++) {

      range.put(expected[j], expected[j]);
    }

    // Do selection without replacement for the objects visited by
    // iterator.

    for (int j = 0; j < nrange; j++) {

      if (!actual.hasNext()) {

        fail(msg + ": Index exhausted while expecting more object(s)" + ": index=" + j);
      }

      final Object actualObject = actual.next();

      if (range.remove(actualObject) == null) {

        fail("Object not expected" + ": index=" + j + ", object=" + actualObject);
      }
    }

    if (actual.hasNext()) {

      fail("Iterator will deliver too many objects.");
    }
  }

  protected BindingSet createBindingSet(final Binding... bindings) {
    final QueryBindingSet bindingSet = new QueryBindingSet();
    if (bindings != null) {
      for (Binding b : bindings) {
        bindingSet.addBinding(b);
      }
    }
    return bindingSet;
  }

  protected void compare(final TupleQueryResult actual, final Collection<BindingSet> expected)
      throws QueryEvaluationException {

    try {

      final Collection<BindingSet> extraResults = new LinkedList<BindingSet>();
      Collection<BindingSet> missingResults = new LinkedList<BindingSet>();

      int resultCount = 0;
      int nmatched = 0;
      while (actual.hasNext()) {
        BindingSet bindingSet = actual.next();
        resultCount++;
        boolean match = false;
        if (log.isInfoEnabled()) log.info(bindingSet);
        Iterator<BindingSet> it = expected.iterator();
        while (it.hasNext()) {
          if (it.next().equals(bindingSet)) {
            it.remove();
            match = true;
            nmatched++;
            break;
          }
        }
        if (match == false) {
          extraResults.add(bindingSet);
        }
      }
      missingResults = expected;

      for (BindingSet bs : extraResults) {
        if (log.isInfoEnabled()) {
          log.info("extra result: " + bs);
        }
      }

      for (BindingSet bs : missingResults) {
        if (log.isInfoEnabled()) {
          log.info("missing result: " + bs);
        }
      }

      if (!extraResults.isEmpty() || !missingResults.isEmpty()) {
        fail(
            "matchedResults="
                + nmatched
                + ", extraResults="
                + extraResults.size()
                + ", missingResults="
                + missingResults.size());
      }

    } finally {

      actual.close();
    }
  }
}
