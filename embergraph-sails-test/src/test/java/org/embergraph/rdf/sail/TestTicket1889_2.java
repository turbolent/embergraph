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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import org.embergraph.rdf.sail.EmbergraphSail.Options;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
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
 * @author Igor Kim
 * @version $Id$
 * @see https://jira.blazegraph.com/browse/BLZG-1889 ArrayIndexOutOfBound Exception
 *     <p>This test case covers 2 ArrayIndexOutOfBoundsException occurrences: 1. Overflow of values
 *     array in StatementBuffer due to blank nodes are not cleared on flush 2. Overflow of values
 *     array in MergeUtility due to its capacity computed without reference to blank nodes
 *     <p>Test case covers both data load and insert update.
 */
public class TestTicket1889_2 extends TestTicket1889 {

  public TestTicket1889_2() {}

  public TestTicket1889_2(String arg0) {
    super(arg0);
  }

  @Override
  public Properties getProperties() {
    Properties properties = super.getProperties();
    properties.setProperty(Options.QUEUE_CAPACITY, "0");
    properties.setProperty(Options.TRUTH_MAINTENANCE, "true");
    return properties;
  }

  /*
   * Prepares data containing blank nodes, loads it into triplestore, then run an update, which
   * creates additional statements with blank nodes resulting number of statements loaded should be
   * 2*n. Total number of blank nodes will be n+k.
   *
   * @param repo Repository to load data
   * @param n Number of statements to be loaded
   * @param k Number of subjects to be loaded
   */
  @Override
  protected void executeQuery(final EmbergraphSailRepository repo, final int n, final int k)
      throws RepositoryException, MalformedQueryException,
      RDFParseException, IOException, UpdateExecutionException {
    final EmbergraphSailRepositoryConnection conn = repo.getConnection();
    conn.setAutoCommit(false);
    conn.clear();
    try {
      StringBuilder data = new StringBuilder();
      for (int i = 0; i < n; i++) {
        data.append("_:s")
            .append(i % k)
            .append(" <http://p> _:o")
            .append(i)
            .append(" <http://c> .\n");
      }
      conn.add(new ByteArrayInputStream(data.toString().getBytes()), "", RDFFormat.NQUADS);
      conn.commit();

      final String query =
          "prefix h: <http://>\r\n"
              + "\r\n"
              + "INSERT { \r\n"
              + "    ?s h:p1 ?o .\r\n"
              + "}\r\n"
              + "WHERE {\r\n"
              + "  ?s h:p ?o .\r\n"
              + "}";
      final Update q = conn.prepareUpdate(QueryLanguage.SPARQL, query);
      q.execute();
      assertEquals(
          n * 2 + 4 /*4 inferred statements about predicates*/,
          conn.getTripleStore().getStatementCount(true));
    } finally {
      conn.close();
    }
  }
}
