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

package org.embergraph.rdf.sail;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.sail.sparql.ast.VisitorException;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.GraphPatternGroup;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.StatementPatternNode;
import org.embergraph.rdf.store.TempTripleStore.Options;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/*
 * Unit test template for use in submission of bugs.
 *
 * <p>This test case will delegate to an underlying backing store. You can specify this store via a
 * JVM property as follows: <code>-DtestClass=org.embergraph.rdf.sail.TestEmbergraphSailWithQuads
 * </code>
 *
 * <p>There are three possible configurations for the testClass:
 *
 * <ul>
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithQuads (quads mode)
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithoutSids (triples mode)
 *   <li>org.embergraph.rdf.sail.TestEmbergraphSailWithSids (SIDs mode)
 * </ul>
 *
 * <p>The default for triples and SIDs mode is for inference with truth maintenance to be on. If you
 * would like to turn off inference, make sure to do so in {@link #getProperties()}.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 * @author Igor Kim
 * @see https://jira.blazegraph.com/browse/BLZG-2043 This testcase checks, that with disabled
 *     inlining both existing and not existing literals parsed as inlined IVs in SPARQL
 */
public class TestTicket2043 extends QuadsTestCase {

  public TestTicket2043() {}

  public TestTicket2043(String arg0) {
    super(arg0);
  }

  @Override
  public Properties getProperties() {
    Properties properties = super.getProperties();

    // Enable inlining
    properties.setProperty(Options.INLINE_XSD_DATATYPE_LITERALS, "false");
    properties.setProperty(Options.INLINE_DATE_TIMES, "false");
    properties.setProperty(Options.INLINE_TEXT_LITERALS, "false");

    return properties;
  }

  public void testBug() throws Exception {

    final EmbergraphSail sail = getSail();
    try {
      EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      try {
        repo.initialize();
        final RepositoryConnection conn = repo.getConnection();
        conn.setAutoCommit(false);
        try {
          conn.add(getClass().getResourceAsStream("TestTicket2043.n3"), "", RDFFormat.TURTLE);
          conn.commit();
          // Check existing int literal
          executeQuery(conn, "1", 2);
          // Check not existing int literal
          executeQuery(conn, "2", 0);
          // Check not existing plain literal
          executeQuery(conn, "\"3\"", 0);
          // Check not existing boolean literal
          executeQuery(conn, "true", 0);
          // Check not existing datetime literal
          executeQuery(conn, "\"2000-01-01T00:00:00Z\"^^xsd:dateTime", 0);
        } finally {
          conn.close();
        }
      } finally {
        repo.shutDown();
      }
    } finally {
      sail.__tearDownUnitTest();
    }
  }

  private void executeQuery(
      final RepositoryConnection conn, final String value, final int expectedCnt)
      throws RepositoryException, MalformedQueryException, QueryEvaluationException {

    final String query =
        "PREFIX :    <http://example/>\r\n"
            + "\r\n"
            + "SELECT ?a ?y ?d ?z\r\n"
            + "{\r\n"
            + "    ?a :p ?c OPTIONAL { ?a :r ?d }.  \r\n"
            + "    ?a ?p "
            + value
            + " { ?p a ?y } UNION { ?a ?z ?p } \r\n"
            + "}";
    final TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
    int cnt = 0;
    final TupleQueryResult tqr = q.evaluate();
    try {
      while (tqr.hasNext()) {
        final Set<String> bindingNames = tqr.next().getBindingNames();
        cnt++;
        if (log.isInfoEnabled()) log.info("bindingNames=" + bindingNames);
      }
    } finally {
      tqr.close();
    }
    // assert number of solutions
    assertEquals(expectedCnt, cnt);

    // also assert class of constant node value is TermId, as inlining is disabled
    ASTContainer ast = ((EmbergraphSailTupleQuery) q).getASTContainer();
    QueryRoot qr = ast.getOptimizedAST();
    GraphPatternGroup<?> gp = qr.getGraphPattern();
    int ivsCnt = 0;
    for (int i = 0; i < gp.arity(); i++) {
      BOp bop = gp.get(i);
      if (bop instanceof StatementPatternNode && bop.get(2) instanceof ConstantNode) {
        IV<?, ?> x = ((ConstantNode) bop.get(2)).getValueExpression().get();
        assertTrue(x instanceof TermId);
        ivsCnt++;
      }
    }
    assertEquals(1, ivsCnt);
  }
}
