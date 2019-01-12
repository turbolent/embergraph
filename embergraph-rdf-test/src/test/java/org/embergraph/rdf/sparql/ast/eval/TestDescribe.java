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
 * Created on Aug 14, 2012.
 */
package org.embergraph.rdf.sparql.ast.eval;

import java.util.HashSet;
import java.util.Set;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.DescribeModeEnum;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.sparql.ast.cache.IDescribeCache;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.decls.FOAFVocabularyDecl;
import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;

/**
 * Data driven test suite for DESCRIBE queries, including the interaction with the optional DESCRIBE
 * cache.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestBasicQuery.java 6387 2012-07-21 18:37:51Z thompsonbry $
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/584">DESCRIBE CACHE </a>
 *     <p>TODO Test that resources that exist in the data (or solutions) but that were NOT included
 *     in the DESCRIBE projection are NOT injected into the DESCRIBE cache.
 */
public class TestDescribe extends AbstractDataDrivenSPARQLTestCase {

  /** */
  public TestDescribe() {}

  /** @param name */
  public TestDescribe(String name) {
    super(name);
  }

  /**
   * Return the {@link IDescribeCache} iff it is enabled.
   *
   * @return The {@link IDescribeCache} iff enabled and otherwise <code>null</code>.
   */
  protected IDescribeCache getDescribeCache(
      final ASTContainer astContainer, final AbstractTripleStore store) {

    final IDescribeCache describeCache;

    final AST2BOpContext context = new AST2BOpContext(astContainer, store);

    if (context.describeCache != null
        && astContainer.getOriginalAST().getQueryType() == QueryType.DESCRIBE) {

      /*
       * The DESCRIBE cache is enabled.
       */

      describeCache = context.getDescribeCache();

    } else {

      // DESCRIBE cache is not enabled.
      describeCache = null;
    }

    return describeCache;
  }

  /**
   * Return the expected description of the resource based on the solutions declared for the unit
   * test.
   *
   * <p>Note: This code is not smart enough to get the expected description for CBD. It would have
   * to fix point things, iterating through the expected statements if new blank nodes are
   * discovered for the graph we are building.
   *
   * @param resource The resource.
   * @param h
   * @return The expected description -or- <code>null</code> if there is no expected description for
   *     that resource.
   */
  protected Set<Statement> getExpectedDescription(
      final EmbergraphValue resource, final TestHelper h) {

    // The expected solutions for the DESCRIBE query.
    final Set<Statement> expectedStatements = h.expectedGraphQueryResult;

    final Set<Statement> graph = new HashSet<Statement>();

    for (Statement stmt : expectedStatements) {

      if (stmt.getSubject().equals(resource)) {

        graph.add(stmt);

      } else if (stmt.getObject().equals(resource)) {

        graph.add(stmt);
      }
    }

    return graph.isEmpty() ? null : graph;
  }

  /**
   * Assert that a resource is described by the cache.
   *
   * @param describedResource The resource
   * @param describeCache The cache.
   * @param h The {@link TestHelper}
   */
  private void assertDescribedResource(
      final EmbergraphValue describedResource,
      final IDescribeCache describeCache,
      final TestHelper h) {

    // Found in the cache after the DESCRIBE query.
    final Graph actualGraph = describeCache.lookup(describedResource.getIV());

    assertNotNull(actualGraph);

    // Verify that the correct description is reported.
    final Set<Statement> expectedGraph = getExpectedDescription(describedResource, h);

    // Check the description.
    h.compareGraphs(new HashSet<Statement>(actualGraph), expectedGraph);

    for (Statement stmt : actualGraph) {

      // Verify that we are getting back EmbergraphStatements.
      assertTrue(stmt instanceof EmbergraphStatement);

      // Verify that the IVs are set on those statements.

      final EmbergraphStatement st = (EmbergraphStatement) stmt;

      assertNotNull(st.s());

      assertNotNull(st.p());

      assertNotNull(st.o());

      if (st.getContext() != null) {

        assertNotNull(st.c());
      }
    }
  }

  /**
   * A simple DESCRIBE query of a constant using the default {@link DescribeModeEnum}.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE <http://www.embergraph.org/DC>
   * </pre>
   */
  public void test_describe_1() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-1", // testURI,
            "describe-1.rq", // queryFileURL
            "describe-1.trig", // dataFileURL
            "describe-1-result.trig" // resultFileURL
            );

    // This is marked as a DESCRIBE query.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI dc = f.createURI("http://www.embergraph.org/DC");

    final EmbergraphValue[] values = new EmbergraphValue[] {dc};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }

    // The original AST is still a DESCRIBE.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The rewritten AST is a CONSTRUCT.
    assertEquals(QueryType.CONSTRUCT, h.getASTContainer().getOptimizedAST().getQueryType());

    /*
     * The projection was not annotated with the describe mode, so we will
     * use the default describe mode.
     */
    assertNull(h.getASTContainer().getOptimizedAST().getProjection().getDescribeMode());
  }

  /**
   * A simple DESCRIBE query of a variable with a where clause.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE ?y
   * where {
   *   ?y rdf:type foaf:Person
   * }
   * </pre>
   */
  public void test_describe_2() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-2", // testURI,
            "describe-2.rq", // queryFileURL
            "describe-2.trig", // dataFileURL
            "describe-2-result.trig" // resultFileURL
            );

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI mike = f.createURI("http://www.embergraph.org/Mike");
    final EmbergraphURI bryan = f.createURI("http://www.embergraph.org/Bryan");

    final EmbergraphValue[] values = new EmbergraphValue[] {mike, bryan};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }
  }

  /**
   * A simple DESCRIBE query of a constant plus a variable with a where clause.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE ?y <http://www.embergraph.org/DC>
   * where {
   *   ?y rdf:type foaf:Person
   * }
   * </pre>
   */
  public void test_describe_3() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-3", // testURI,
            "describe-3.rq", // queryFileURL
            "describe-3.trig", // dataFileURL
            "describe-3-result.trig" // resultFileURL
            );

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI dc = f.createURI("http://www.embergraph.org/DC");
    final EmbergraphURI mike = f.createURI("http://www.embergraph.org/Mike");
    final EmbergraphURI bryan = f.createURI("http://www.embergraph.org/Bryan");

    final EmbergraphValue[] values = new EmbergraphValue[] {dc, mike, bryan};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }
  }

  /**
   * DESCRIBE a variable.
   *
   * <pre>
   * PREFIX : <http://www.embergraph.org/>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE ?x
   * WHERE {
   *  ?x rdf:type :person .
   *  ?x :likes :rdf .
   * }
   * </pre>
   *
   * @throws Exception
   */
  public void test_describe_4() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-4", // testURI,
            "describe-4.rq", // queryFileURL
            "describe-4.trig", // dataFileURL
            "describe-4-result.trig" // resultFileURL
            );

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI mike = f.createURI("http://www.embergraph.org/mike");

    final EmbergraphValue[] values = new EmbergraphValue[] {mike};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }
  }

  /**
   *
   *
   * <pre>
   * PREFIX : <http://www.embergraph.org/>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   *
   * DESCRIBE ?x ?y
   * WHERE {
   *  ?x :likes ?y .
   * }
   * </pre>
   */
  public void test_describe_5() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-5", // testURI,
            "describe-5.rq", // queryFileURL
            "describe-5.trig", // dataFileURL
            "describe-5-result.trig" // resultFileURL
            );

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI mike = f.createURI("http://www.embergraph.org/mike");
    final EmbergraphURI rdf = f.createURI("http://www.embergraph.org/rdf");

    final EmbergraphValue[] values = new EmbergraphValue[] {mike, rdf};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }
  }

  /**
   * A simple DESCRIBE query of a constant, but in this test we also verify that the cache entry is
   * invalidated by an update involving that resource.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE <http://www.embergraph.org/DC>
   * </pre>
   */
  public void test_describe_1_invalidation() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-1", // testURI,
            "describe-1.rq", // queryFileURL
            "describe-1.trig", // dataFileURL
            "describe-1-result.trig" // resultFileURL
            );

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI dc = f.createURI("http://www.embergraph.org/DC");

    final EmbergraphURI foafPerson = f.asValue(FOAFVocabularyDecl.Person);

    final EmbergraphURI rdfType = f.asValue(RDF.TYPE);

    final EmbergraphValue[] values = new EmbergraphValue[] {dc, foafPerson, rdfType};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    for (EmbergraphValue v : values) {

      assertNotNull(v.toString(), v.getIV());
    }

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      //            for (EmbergraphValue v : values) {

      assertDescribedResource(dc, describeCache, h);

      // }

      /*
       * Should cause the cache entry to be invalidated.
       *
       * TODO The DescribeServiceFactory will only notice an update that
       * goes through a EmbergraphSailConnection. This issue is documented at
       * CustomServiceFactory. This should probably be fixed as part of a
       * broader overhaul.
       */
      final EmbergraphSail sail = new EmbergraphSail(h.getTripleStore());
      try {
        sail.initialize();
        final EmbergraphSailConnection conn = sail.getConnection();
        try {
          conn.addStatement(dc, rdfType, foafPerson);
          conn.commit();
        } finally {
          conn.close();
        }
      } finally {
        sail.shutDown();
      }

      // This cache entry should be gone.
      assertNull(describeCache.lookup(dc.getIV()));
    }
  }

  /**
   * A simple DESCRIBE query of a constant using {@link DescribeModeEnum#SymmetricOneStep}.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE <http://www.embergraph.org/DC>
   * {
   *    hint:Query hint:describeMode "SymmetricOneStep"
   * }
   * </pre>
   */
  public void test_describe_SymmetricOneStep_1() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-SymmetricOneStep-1", // testURI,
            "describe-SymmetricOneStep-1.rq", // queryFileURL
            "describe-SymmetricOneStep-1.trig", // dataFileURL
            "describe-SymmetricOneStep-1-result.trig" // resultFileURL
            );

    // This is marked as a DESCRIBE query.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI dc = f.createURI("http://www.embergraph.org/DC");

    final EmbergraphValue[] values = new EmbergraphValue[] {dc};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }

    // The original AST is still a DESCRIBE.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The rewritten AST is a CONSTRUCT.
    assertEquals(QueryType.CONSTRUCT, h.getASTContainer().getOptimizedAST().getQueryType());

    /*
     * The projection was not annotated with the describe mode, so we will
     * use the default describe mode.
     */
    assertEquals(
        DescribeModeEnum.SymmetricOneStep,
        h.getASTContainer().getOptimizedAST().getProjection().getDescribeMode());
  }

  /**
   * A simple DESCRIBE query of a constant using {@link DescribeModeEnum#ForwardOneStep}.
   *
   * <pre>
   * PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
   * PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
   * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
   *
   * DESCRIBE <http://www.embergraph.org/DC>
   * {
   *    hint:Query hint:describeMode "ForwardOneStep"
   * }
   * </pre>
   */
  public void test_describe_ForwardOneStep_1() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-ForwardOneStep-1", // testURI,
            "describe-ForwardOneStep-1.rq", // queryFileURL
            "describe-ForwardOneStep-1.trig", // dataFileURL
            "describe-ForwardOneStep-1-result.trig" // resultFileURL
            );

    // This is marked as a DESCRIBE query.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The DESCRIBE cache that we are reading on.
    final IDescribeCache describeCache = getDescribeCache(h.getASTContainer(), h.getTripleStore());

    final EmbergraphValueFactory f = h.getTripleStore().getValueFactory();

    final EmbergraphURI dc = f.createURI("http://www.embergraph.org/DC");

    final EmbergraphValue[] values = new EmbergraphValue[] {dc};

    h.getTripleStore().getLexiconRelation().addTerms(values, values.length, true /* readOnly */);

    if (describeCache != null) {

      // Not in the cache before we run the DESCRIBE query.
      for (EmbergraphValue v : values) {

        assertNull(describeCache.lookup(v.getIV()));
      }
    }

    h.runTest();

    if (describeCache != null) {

      for (EmbergraphValue v : values) {

        assertDescribedResource(v, describeCache, h);
      }
    }

    // The original AST is still a DESCRIBE.
    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    // The rewritten AST is a CONSTRUCT.
    assertEquals(QueryType.CONSTRUCT, h.getASTContainer().getOptimizedAST().getQueryType());

    /*
     * The projection was annotated with the appropriate describe mode.
     */
    assertEquals(
        DescribeModeEnum.ForwardOneStep,
        h.getASTContainer().getOptimizedAST().getProjection().getDescribeMode());
  }

  /**
   * This test is used to verify that we compute {@link DescribeModeEnum#CBD} correctly.
   *
   * <pre>
   * DESCRIBE <http://example.com/aReallyGreatBook>
   * {
   *    hint:Query hint:describeMode "CBD"
   * }
   * </pre>
   *
   * This example is taken directly from <a href="http://www.w3.org/Submission/CBD/">CBD - Concise
   * Bounded Description </a>
   */
  public void test_describe_CBD_1() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-CBD-1", // testURI,
            "describe-CBD-1.rq", // queryFileURL
            "describe-CBD-1.rdf", // dataFileURL
            "describe-CBD-1-result.rdf" // resultFileURL
            );

    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    h.runTest();

    // The projection was annotated with the desired DescribeMode.
    assertEquals(
        DescribeModeEnum.CBD,
        h.getASTContainer().getOptimizedAST().getProjection().getDescribeMode());
  }

  /**
   * This test is used to verify that we compute {@link DescribeModeEnum#SCBD} correctly.
   *
   * <pre>
   * DESCRIBE <http://example.com/aReallyGreatBook>
   * {
   *    hint:Query hint:describeMode "SCBD"
   * }
   * </pre>
   *
   * This example is taken directly from <a href="http://www.w3.org/Submission/CBD/">CBD - Concise
   * Bounded Description </a>
   */
  public void test_describe_SCBD_1() throws Exception {

    final TestHelper h =
        new TestHelper(
            "describe-SCBD-1", // testURI,
            "describe-SCBD-1.rq", // queryFileURL
            "describe-SCBD-1.rdf", // dataFileURL
            "describe-SCBD-1-result.rdf" // resultFileURL
            );

    assertEquals(QueryType.DESCRIBE, h.getASTContainer().getOriginalAST().getQueryType());

    h.runTest();

    // The projection was annotated with the desired DescribeMode.
    assertEquals(
        DescribeModeEnum.SCBD,
        h.getASTContainer().getOptimizedAST().getProjection().getDescribeMode());
  }
}
