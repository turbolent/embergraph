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
 * Created on Feb 4, 2007
 */

package org.embergraph;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/*
* Aggregates test suites in increase dependency order.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll extends TestCase {

  /** Static flags to HA and/or Quorum to be excluded, preventing hangs in CI */

  /** */
  public TestAll() {}

  /** @param arg0 */
  public TestAll(String arg0) {
    super(arg0);
  }

  /** Aggregates the tests in increasing dependency order. */
  public static Test suite() {

    /*
     * log4j defaults to DEBUG which will produce simply huge amounts of
     * logging information when running the unit tests. Therefore we
     * explicitly set the default logging level to WARN. If you are using a
     * log4j configuration file then this is unlikely to interact with your
     * configuration, and in any case you can override specific loggers.
     */
    {
      final Logger log = Logger.getRootLogger();

      if (log.getLevel().equals(Level.DEBUG)) {

        log.setLevel(Level.WARN);

        log.warn("Defaulting debugging level to WARN for the unit tests");
      }
    }

    final TestSuite suite = new TestSuite("embergraph");

    // core embergraph packages.
    suite.addTest(org.embergraph.cache.TestAll.suite());
    suite.addTest(org.embergraph.io.TestAll.suite());
    suite.addTest(org.embergraph.net.TestAll.suite());
    suite.addTest(org.embergraph.config.TestAll.suite());
    // suite.addTest( org.embergraph.util.TestAll.suite() );
    suite.addTest(org.embergraph.util.concurrent.TestAll.suite());
    suite.addTest(org.embergraph.jsr166.TestAll.suite());
    suite.addTest(org.embergraph.striterator.TestAll.suite());
    suite.addTest(org.embergraph.counters.TestAll.suite());
    suite.addTest(org.embergraph.rawstore.TestAll.suite());
    suite.addTest(org.embergraph.btree.TestAll.suite());
    suite.addTest(org.embergraph.htree.TestAll.suite());
    suite.addTest(org.embergraph.concurrent.TestAll.suite());
    suite.addTest(org.embergraph.quorum.TestAll.suite());
    suite.addTest(org.embergraph.ha.TestAll.suite());
    // Note: this has a dependency on the quorum package.
    suite.addTest(org.embergraph.io.writecache.TestAll.suite());
    suite.addTest(org.embergraph.journal.TestAll.suite());
    suite.addTest(org.embergraph.rwstore.TestAll.suite());
    suite.addTest(org.embergraph.resources.TestAll.suite());
    suite.addTest(org.embergraph.relation.TestAll.suite());
    suite.addTest(org.embergraph.bop.TestAll.suite());
    suite.addTest(org.embergraph.relation.rule.eval.TestAll.suite());
    suite.addTest(org.embergraph.mdi.TestAll.suite());
    suite.addTest(org.embergraph.service.TestAll.suite());
    //        suite.addTest( org.embergraph.bop.fed.TestAll.suite() );//This was being run 3
    // times(!)
    suite.addTest(org.embergraph.sparse.TestAll.suite());
    suite.addTest(org.embergraph.search.TestAll.suite());
    suite.addTest(org.embergraph.bfs.TestAll.suite());
    //        suite.addTest( org.embergraph.service.mapReduce.TestAll.suite() );

    // Jini integration
    // BLZG-1370 moved to jini package.
    // suite.addTest(org.embergraph.jini.TestAll.suite());

    // RDF
    // Moved into embergraph-rdf-test
    // suite.addTest(org.embergraph.rdf.TestAll.suite());
    // Moved into embergraph-sails-test
    //       suite.addTest(org.embergraph.rdf.sail.TestAll.suite());

    // The REST API test suite.
    // Moved into embergraph-sails-test
    //      suite.addTest(org.embergraph.rdf.sail.webapp.TestAll.suite());

    /*
     * The Generic Object Model and Graph API (includes remote tests against
     * the NanoSparqlServer layer).
     */
    suite.addTest(org.embergraph.gom.TestAll.suite());

    return suite;
  }
}
