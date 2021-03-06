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
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
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
 * @see https://jira.blazegraph.com/browse/BLZG-1788 Typed literals in VALUES clause not matching
 *     data
 */
public class TestTicket1788 extends QuadsTestCase {

  public TestTicket1788() {}

  public TestTicket1788(String arg0) {
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
        conn.add(getClass().getResourceAsStream("TestTicket1788.n3"), "", RDFFormat.TURTLE);
        conn.commit();

        final String query =
            "SELECT * {\r\n"
                + "  ?s <http://p> ?o .\r\n"
                + "  values ?o { \"A\"^^xsd:string \"B\"^^xsd:string \"D\"^^xsd:string }\r\n"
                + "}";
        final TupleQuery q = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
        final TupleQueryResult tqr = q.evaluate();
        int cnt = 0;
        while (tqr.hasNext()) {
          BindingSet bindings = tqr.next();
          cnt++;
          if (log.isInfoEnabled()) log.info("bindings=" + bindings);
        }
        tqr.close();
        assertEquals(2, cnt);

      } finally {
        conn.close();
      }
    } finally {
      repo.shutDown();
    }
  }
}
