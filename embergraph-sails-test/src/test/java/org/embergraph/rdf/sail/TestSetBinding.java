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

import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.BindingImpl;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;

/*
 * Test suite for {@link AbstractQuery#setBinding(String, Value)}
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @version $Id$
 */
public class TestSetBinding extends ProxyEmbergraphSailTestCase {

  private static final Logger log = Logger.getLogger(TestSetBinding.class);

  @Override
  public Properties getProperties() {

    final Properties props = super.getProperties();

    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /** */
  public TestSetBinding() {}

  /** @param arg0 */
  public TestSetBinding(String arg0) {
    super(arg0);
  }

  public void testSetBinding() throws Exception {

    final EmbergraphSail sail = getSail();

    try {

      sail.initialize();

      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

      final EmbergraphSailRepositoryConnection cxn = repo.getConnection();

      try {

        cxn.setAutoCommit(false);

        final EmbergraphValueFactory vf = cxn.getValueFactory();

        // First step, load data.
        final String data =
            "@prefix ns:<http://localhost/pets#>. "
                + "@prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>. "
                + "    ns:snowball rdfs:label \"Snowball\"; "
                + "                ns:weight \"10\". "
                + "    ns:buffy rdfs:label \"Buffy\"; "
                + "                ns:weight \"8\".";

        if (log.isInfoEnabled()) log.info("Loading data");

        cxn.add(new StringReader(data), "", RDFFormat.TURTLE);

        cxn.commit();

        if (log.isInfoEnabled()) {

          // Dump the database.

          final RepositoryResult<Statement> stmts = cxn.getStatements(null, null, null, false);

          while (stmts.hasNext()) {

            final Statement tmp = stmts.next();

            log.info(tmp);
          }
        }

        // Resolve some Values that we will need below.
        final EmbergraphLiteral buffy = vf.createLiteral("Buffy");
        final EmbergraphLiteral snowball = vf.createLiteral("Snowball");
        final EmbergraphLiteral w1 = vf.createLiteral("8");
        final EmbergraphLiteral w2 = vf.createLiteral("10");
        cxn.getTripleStore().addTerms(new EmbergraphValue[] {buffy, snowball, w1, w2});
        assertNotNull(buffy.getIV());
        assertNotNull(snowball.getIV());
        assertNotNull(w1.getIV());
        assertNotNull(w2.getIV());

        // Second step, query data. Load query from resource and
        // execute.
        final String query =
            "PREFIX ns:<http://localhost/pets#> "
                + "\nPREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>"
                + "\nSELECT ?name ?weight WHERE {"
                + "\n?uri rdfs:label ?name."
                + "\n?uri ns:weight ?weight."
                + "\n}";

        if (log.isInfoEnabled()) log.info("Executing query: " + query);

        // Verify result w/o binding.
        {
          final TupleQuery tupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);

          final Collection<BindingSet> answer = new LinkedList<>();

          answer.add(
              createBindingSet(new BindingImpl("name", buffy), new BindingImpl("weight", w1)));

          answer.add(
              createBindingSet(new BindingImpl("name", snowball), new BindingImpl("weight", w2)));

          final TupleQueryResult result = tupleQuery.evaluate();

          compare(result, answer);
        }

        // Verify result w/ binding (one solution is eliminated).
        {
          final TupleQuery tupleQuery = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);

          final Collection<BindingSet> answer = new LinkedList<>();

          answer.add(
              createBindingSet(new BindingImpl("name", snowball), new BindingImpl("weight", w2)));

          tupleQuery.setBinding("name", snowball);

          final TupleQueryResult result = tupleQuery.evaluate();

          compare(result, answer);
        }

      } finally {
        cxn.close();
      }

    } finally {
      sail.__tearDownUnitTest();
    }
  }
}
