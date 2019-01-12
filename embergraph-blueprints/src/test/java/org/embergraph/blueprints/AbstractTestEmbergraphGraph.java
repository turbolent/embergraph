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
package org.embergraph.blueprints;

import com.tinkerpop.blueprints.EdgeTestSuite;
import com.tinkerpop.blueprints.GraphQueryTestSuite;
import com.tinkerpop.blueprints.GraphTestSuite;
import com.tinkerpop.blueprints.VertexQueryTestSuite;
import com.tinkerpop.blueprints.VertexTestSuite;
import com.tinkerpop.blueprints.impls.GraphTest;
import java.util.Properties;
import junit.framework.TestCase2;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.Options;
import org.embergraph.rdf.vocab.NoVocabulary;

/** */
public abstract class AbstractTestEmbergraphGraph extends TestCase2 {

  /** */
  public AbstractTestEmbergraphGraph() {
    super();
  }

  /** @param name */
  public AbstractTestEmbergraphGraph(final String name) {
    super(name);
  }

  protected EmbergraphSail getSail() {

    return getSail(getProperties());
  }

  @Override
  public Properties getProperties() {

    final Properties props = new Properties();

    props.setProperty(Journal.Options.COLLECT_PLATFORM_STATISTICS, "false");

    props.setProperty(Journal.Options.COLLECT_QUEUE_STATISTICS, "false");

    props.setProperty(Journal.Options.HTTPD_PORT, "-1" /* none */);

    // transient means that there is nothing to delete after the test.
    //        props.setProperty(Options.BUFFER_MODE,BufferMode.Transient.toString());
    props.setProperty(Options.BUFFER_MODE, BufferMode.Disk.toString());

    /*
     * If an explicit filename is not specified...
     */
    if (props.get(Options.FILE) == null) {

      /*
       * Use a temporary file for the test. Such files are always deleted when
       * the journal is closed or the VM exits.
       */

      props.setProperty(Options.CREATE_TEMP_FILE, "true");

      props.setProperty(Options.DELETE_ON_EXIT, "true");
    }

    // no inference
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");

    // no text index
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    // triples mode
    props.setProperty(EmbergraphSail.Options.QUADS, "false");
    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");

    return props;
  }

  private Properties properties = null;

  @Override
  protected void tearDown() throws Exception {

    properties = null;
  }

  protected EmbergraphSail getSail(final Properties properties) {

    this.properties = properties;

    return new EmbergraphSail(properties);
  }

  protected EmbergraphSail reopenSail(final EmbergraphSail sail) {

    //        final Properties properties = sail.getProperties();

    if (sail.isOpen()) {

      try {

        sail.shutDown();

      } catch (Exception ex) {

        throw new RuntimeException(ex);
      }
    }

    return getSail(properties);
  }

  protected abstract GraphTest newEmbergraphGraphTest() throws Exception;

  public void testVertexTestSuite() throws Exception {
    final GraphTest test = newEmbergraphGraphTest();
    test.stopWatch();
    test.doTestSuite(new VertexTestSuite(test));
    GraphTest.printTestPerformance("VertexTestSuite", test.stopWatch());
  }

  public void testEdgeSuite() throws Exception {
    final GraphTest test = newEmbergraphGraphTest();
    test.stopWatch();
    test.doTestSuite(new EdgeTestSuite(test));
    GraphTest.printTestPerformance("EdgeTestSuite", test.stopWatch());
  }

  public void testGraphSuite() throws Exception {
    final GraphTest test = newEmbergraphGraphTest();
    test.stopWatch();
    test.doTestSuite(new GraphTestSuite(test));
    GraphTest.printTestPerformance("GraphTestSuite", test.stopWatch());
  }

  public void testVertexQueryTestSuite() throws Exception {
    final GraphTest test = newEmbergraphGraphTest();
    test.stopWatch();
    test.doTestSuite(new VertexQueryTestSuite(test));
    GraphTest.printTestPerformance("VertexQueryTestSuite", test.stopWatch());
  }

  public void testGraphQueryTestSuite() throws Exception {
    final GraphTest test = newEmbergraphGraphTest();
    test.stopWatch();
    test.doTestSuite(new GraphQueryTestSuite(test));
    GraphTest.printTestPerformance("GraphQueryTestSuite", test.stopWatch());
  }

  //    public void testTransactionalGraphTestSuite() throws Exception {
  //    	final GraphTest test = newEmbergraphGraphTest();
  //    	test.stopWatch();
  //        test.doTestSuite(new TransactionalGraphTestSuite(test));
  //        GraphTest.printTestPerformance("TransactionalGraphTestSuite", test.stopWatch());
  //    }
  //
  //    public void testGraphQueryForHasOR() throws Exception {
  //        final EmbergraphGraphTest test = newEmbergraphGraphTest();
  //        test.stopWatch();
  //        final EmbergraphTestSuite testSuite = new EmbergraphTestSuite(test);
  //        try {
  //            testSuite.testGraphQueryForHasOR();
  //        } finally {
  //            test.shutdown();
  //        }
  //
  //    }

  //    private static class EmbergraphTestSuite extends TestSuite {
  //
  //        public EmbergraphTestSuite(final GraphTest graphTest) {
  //            super(graphTest);
  //        }
  //
  //    }
  //
  //
  //    private class EmbergraphGraphTest extends GraphTest {
  //
  //		@Override
  //		public void doTestSuite(TestSuite testSuite) throws Exception {
  //	        for (Method method : testSuite.getClass().getDeclaredMethods()) {
  //	            if (method.getName().startsWith("test")) {
  //	                System.out.println("Testing " + method.getName() + "...");
  //	                try {
  //		                method.invoke(testSuite);
  //	                } catch (Exception ex) {
  //	                	ex.getCause().printStackTrace();
  //	                	throw ex;
  //	                } finally {
  //		                shutdown();
  //	                }
  //	            }
  //	        }
  //		}
  //
  //		private Map<String,EmbergraphSail> testSails = new LinkedHashMap<String, EmbergraphSail>();
  //
  //		@Override
  //		public Graph generateGraph(final String key) {
  //
  //			try {
  //	            if (testSails.containsKey(key) == false) {
  //	                final EmbergraphSail testSail = getSail();
  //	                testSail.initialize();
  //	                testSails.put(key, testSail);
  //	            }
  //
  //				final EmbergraphSail sail = testSails.get(key); //testSail; //getSail();
  //				final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
  //				final EmbergraphGraph graph = new EmbergraphGraphEmbedded(repo) {
  //
  //				    /*
  //				     * Test cases have weird semantics for shutdown.
  //				     */
  //					@Override
  //					public void shutdown() {
  //					    try {
  //				            if (cxn != null) {
  //    					        cxn.commit();
  //    					        cxn.close();
  //    					        cxn = null;
  //				            }
  //					    } catch (Exception ex) {
  //					        throw new RuntimeException(ex);
  //					    }
  //					}
  //
  //				};
  //				return graph;
  //			} catch (Exception ex) {
  //				throw new RuntimeException(ex);
  //			}
  //
  //		}
  //
  //		@Override
  //		public Graph generateGraph() {
  //
  //			return generateGraph(null);
  //		}
  //
  //		public void shutdown() {
  //		    for (EmbergraphSail sail : testSails.values()) {
  //		        sail.__tearDownUnitTest();
  //		    }
  //		    testSails.clear();
  //		}
  //
  //
  //    }

}
