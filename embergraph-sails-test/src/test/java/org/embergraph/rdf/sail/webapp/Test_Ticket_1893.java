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
package org.embergraph.rdf.sail.webapp;

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.Test;
import org.embergraph.rdf.sail.remote.EmbergraphSailRemoteRepositoryConnection;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

/*
 * This test checks if plain literals and string literals are properly indexed in FTS, and also
 * rebuild text index produces properly indexed FTS.
 *
 * @see https://jira.blazegraph.com/browse/BLZG-1893
 */
public class Test_Ticket_1893 extends AbstractProtocolTest {

  public Test_Ticket_1893(String name) {
    super(name);
  }

  public static Test suite() {
    return ProxySuiteHelper.suiteWhenStandalone(
        Test_Ticket_1893.class, "test.*", TestMode.quads
        //				TestMode.sids,
        //				TestMode.triples
        );
  }

  private static final String QUERY =
      "prefix bds: <http://www.embergraph.org/rdf/search#>\n"
          + "select ?s \n"
          + "where {\n"
          + "  ?o bds:search \"Test*\" .\n"
          + "  ?s ?p ?o .\n"
          + "}";

  /*
   * Execute an ASK query including a SERVICE keyword which gets sent back to this server. The test
   * succeeeds if the query returns true, and fails otherwise
   *
   * @param args
   * @throws IOException
   */
  public void test_simple() throws Exception {

    setMethodisPostUrlEncodedData();
    //		m_repo.getRemoteRepositoryManager().createRepository(namespace, properties);
    EmbergraphSailRemoteRepositoryConnection conn =
        m_repo.getEmbergraphSailRemoteRepository().getConnection();
    conn.prepareUpdate(
            QueryLanguage.SPARQL, "INSERT { <http://s1> <http://p> \"Test123\" } WHERE { }")
        .execute();

    checkResults(conn, 1);

    conn.prepareUpdate(
            QueryLanguage.SPARQL,
            "INSERT { <http://s2> <http://p> \"Test234\"^^xsd:string } WHERE { }")
        .execute();

    checkResults(conn, 2);

    conn.prepareUpdate(
            QueryLanguage.SPARQL, "INSERT { <http://s3> <http://p> \"Test345\" } WHERE { }")
        .execute();

    checkResults(conn, 3);

    m_repo.getRemoteRepositoryManager().rebuildTextIndex(namespace, true);

    checkResults(conn, 3);
  }

  private void checkResults(final EmbergraphSailRemoteRepositoryConnection conn, final int count)
      throws QueryEvaluationException, RepositoryException, MalformedQueryException {
    final TupleQueryResult result = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY).evaluate();

    Collection<Value> subjects = new ArrayList<>();
    try {
      while (result.hasNext()) {
        BindingSet bs = result.next();
        subjects.add(bs.getBinding("s").getValue());
      }
    } finally {
      result.close();
    }
    if (log.isInfoEnabled()) log.info(subjects);
    assertEquals(count, subjects.size());
  }
}
