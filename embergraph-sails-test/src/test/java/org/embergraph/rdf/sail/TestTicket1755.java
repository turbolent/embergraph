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
import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.XSDBooleanIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.sparql.ast.ConstantNode;
import org.embergraph.rdf.sparql.ast.FilterNode;
import org.embergraph.rdf.sparql.ast.QueryRoot;
import org.embergraph.rdf.sparql.ast.ValueExpressionNode;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/**
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
 * @see https://jira.blazegraph.com/browse/BLZG-1755 Date literals in complex FILTER not properly
 *     resolved
 */
public class TestTicket1755 extends QuadsTestCase {

  public TestTicket1755() {}

  public TestTicket1755(String arg0) {
    super(arg0);
  }

  public void testBug() throws Exception {

    final EmbergraphSail sail = getSail();
    try {
      executeQuery(new EmbergraphSailRepository(sail));
    } finally {
      sail.__tearDownUnitTest();
    }
  }

  private void executeQuery(final EmbergraphSailRepository repo)
      throws RepositoryException, MalformedQueryException, QueryEvaluationException,
          RDFParseException, IOException {
    try {
      repo.initialize();
      final EmbergraphSailRepositoryConnection conn = repo.getConnection();
      conn.setAutoCommit(false);
      try {
        conn.add(getClass().getResourceAsStream("TestTicket1755.n3"), "", RDFFormat.TURTLE);
        conn.commit();

        {
          final String query =
              "SELECT ?s WHERE {\r\n"
                  + "   ?s <http://begin> ?begin.\r\n"
                  + "   ?s <http://end> ?end.\r\n"
                  + "   FILTER(\"0521-01-01\"^^<http://www.w3.org/2001/XMLSchema#date> >= ?begin)\r\n"
                  + "}";
          /*
           * There are 3 IVs in the parsed tree:
           * LiteralExtensionIV in Constant as an argument of ConstantNode in FunctionNode
           * LiteralExtensionIV in EmbergraphValue in ConstantNode as an argument of FunctionNode
           * LiteralExtensionIV in Constant as an argument of valueExpression annotation of FunctionNode
           */
          int expectedIVs = 3;
          testQuery(conn, query, expectedIVs);
        }
        {
          final String query =
              "SELECT ?s WHERE {\r\n"
                  + "   ?s <http://begin> ?begin.\r\n"
                  + "   ?s <http://end> ?end.\r\n"
                  + "   FILTER((\"0521-01-01\"^^<http://www.w3.org/2001/XMLSchema#date> >= ?begin) && true)\r\n"
                  + "}";
          /*
           * There are 6 IVs in the parsed tree:
           * LiteralExtensionIV in Constant as an argument of CompareBOp in valueExpression annotation of FunctionNode
           * XSDBooleanIV in Constant as an  argument of EBVBOp in valueExpression annotation of FunctionNode
           * LiteralExtensionIV in Constant as an argument of valueExpression annotation of FunctionNode
           * LiteralExtensionIV in EmbergraphValue in ConstantNode as an argument of FunctionNode
           * LiteralExtensionIV in Constant as an argument of ConstantNode
           * XSDBoolean in Constant as an argument  of  ConstantNode
           */
          int expectedIVs = 6;
          testQuery(conn, query, expectedIVs);
        }

      } finally {
        conn.close();
      }
    } finally {
      repo.shutDown();
    }
  }

  @SuppressWarnings("unchecked")
  private void testQuery(
      final EmbergraphSailRepositoryConnection conn, final String query, final int expectedIVs)
      throws MalformedQueryException, RepositoryException, QueryEvaluationException {
    TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
    TupleQueryResult tqr = tq.evaluate();
    try {
      int cnt = 0;
      while (tqr.hasNext()) {
        tqr.next();
        cnt++;
      }
      //			assertEquals("Expected 1 row in resultset", 1, cnt);
      QueryRoot queryRoot = ((EmbergraphSailTupleQuery) tq).getASTContainer().getOriginalAST();
      cnt = 0;
      for (Object filterNode : queryRoot.getWhereClause().getChildren(FilterNode.class)) {
        cnt += checkNode((BOp) filterNode);
      }
      assertEquals("Expected inlined IV for date literal", expectedIVs, cnt);
    } finally {
      tqr.close();
    }
  }

  private int checkNode(BOp bop) {
    int cnt = 0;
    for (BOp arg : bop.args()) {
      cnt += checkNode(arg);
    }
    if (bop instanceof ValueExpressionNode) {
      for (BOp arg : ((ValueExpressionNode) bop).getValueExpression().args()) {
        cnt += checkNode(arg);
      }
    }
    if (bop instanceof ConstantNode) {
      // constant node in AST
      EmbergraphValue value = ((ConstantNode) bop).getValue();
      if (value instanceof EmbergraphLiteral
          && XMLSchema.DATE.equals(((EmbergraphLiteral) value).getDatatype())) {
        assertFalse(((EmbergraphLiteral) value).getIV().isNullIV());
        assertTrue(value.getIV() instanceof LiteralExtensionIV);
        cnt++;
      }
    }
    if (bop instanceof Constant) {
      // constant in valueExpr
      IV value = (IV) ((Constant) bop).get();
      assertFalse(value.isNullIV());
      assertTrue(value instanceof LiteralExtensionIV || value instanceof XSDBooleanIV);
      cnt++;
    }
    return cnt;
  }
}
