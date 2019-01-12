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
 * Created on Sep 16, 2009
 */

package org.embergraph.rdf.sail;

import java.util.Properties;
import org.apache.log4j.Logger;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.changesets.IChangeRecord;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryResult;

/** Test suite {@link RDRHistory}. */
public class TestRDRHistory extends ProxyEmbergraphSailTestCase {

  private static final Logger log = Logger.getLogger(TestRDRHistory.class);

  @Override
  public Properties getProperties() {

    return getProperties(RDRHistory.class);
  }

  public Properties getProperties(final Class<? extends RDRHistory> cls) {

    final Properties props = super.getProperties();

    // no inference
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    // turn on RDR history
    props.setProperty(AbstractTripleStore.Options.RDR_HISTORY_CLASS, cls.getName());

    return props;
  }

  /** */
  public TestRDRHistory() {}

  /** @param arg0 */
  public TestRDRHistory(String arg0) {
    super(arg0);
  }

  /** Test basic add/remove. */
  public void testAddAndRemove() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getProperties());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI(":s");
      final URI p = vf.createURI(":p");
      final URI o = vf.createURI(":o");

      final EmbergraphStatement stmt = vf.createStatement(s, p, o);

      // Add statement (first time added).
      cxn.add(stmt);
      cxn.commit();

      if (log.isInfoEnabled()) {
        log.info(cxn.getTripleStore().dumpStore().insert(0, '\n'));
      }

      // Now 2 statements.
      // 1. ground statement (Explicit)
      // 2. <<s,p,o>> blaze:history:added timestamp1
      assertEquals(2, cxn.size());

      // Verify ground statement exists (Explicit).
      {
        final RepositoryResult<Statement> stmts = cxn.getStatements(s, p, o, true);
        try {
          assertTrue(stmts.hasNext());
          final EmbergraphStatement tmp = (EmbergraphStatement) stmts.next();
          assertEquals(StatementEnum.Explicit, tmp.getStatementType());
          assertFalse(stmts.hasNext()); // Should be no more statements.
        } finally {
          stmts.close();
        }
      }

      {
        final EmbergraphBNode sid = vf.createBNode(stmt);
        final RepositoryResult<Statement> stmts =
            cxn.getStatements(sid, RDRHistory.Vocab.ADDED, null, true);
        try {
          assertTrue(stmts.hasNext());
          final EmbergraphStatement tmp = (EmbergraphStatement) stmts.next();
          final Literal l = (Literal) tmp.getObject();
          assertEquals(XSD.DATETIME, l.getDatatype());
          assertFalse(stmts.hasNext()); // Should be no more statements.
        } finally {
          stmts.close();
        }
      }

      // Remove statement (first time removed).
      cxn.remove(stmt);
      cxn.commit();

      if (log.isInfoEnabled()) {
        log.info(cxn.getTripleStore().dumpStore().insert(0, '\n'));
      }

      // Now 3 statements
      // 1. ground statement (was Explicit, now History)
      // 2. <<s,p,o>> blaze:history:added timestamp1
      // 3. <<s,p,o>> blaze:history:removed timestamp2
      assertEquals(3, cxn.size());

      // Verify ground statement no longer found (is History mode now).
      {
        final RepositoryResult<Statement> stmts = cxn.getStatements(s, p, o, true);
        try {
          assertFalse(stmts.hasNext());
        } finally {
          stmts.close();
        }
      }

      // Verify blaze:history:removed now found.
      {
        final EmbergraphBNode sid = vf.createBNode(stmt);
        final RepositoryResult<Statement> stmts =
            cxn.getStatements(sid, RDRHistory.Vocab.REMOVED, null, true);
        try {
          assertTrue(stmts.hasNext());
          final EmbergraphStatement tmp = (EmbergraphStatement) stmts.next();
          final Literal l = (Literal) tmp.getObject();
          assertEquals(XSD.DATETIME, l.getDatatype());
        } finally {
          stmts.close();
        }
      }

      // Add same statement again (2nd time).
      cxn.add(stmt);
      cxn.commit();

      if (log.isInfoEnabled()) {
        log.info(cxn.getTripleStore().dumpStore().insert(0, '\n'));
      }

      // Now 4 statements
      // 1. ground statement (was History, now Explicit)
      // 2. <<s,p,o>> blaze:history:added timestamp1
      // 3. <<s,p,o>> blaze:history:removed timestamp2
      // 4. <<s,p,o>> blaze:history:added timestamp3
      assertEquals(4, cxn.size());

      // Ground statement is found again.
      {
        final RepositoryResult<Statement> stmts = cxn.getStatements(s, p, o, true);
        try {
          assertTrue(stmts.hasNext());
        } finally {
          stmts.close();
        }
      }

      {
        final EmbergraphBNode sid = vf.createBNode(stmt);
        final RepositoryResult<Statement> stmts = cxn.getStatements(sid, null, null, true);
        try {
          int adds = 0;
          int removes = 0;
          while (stmts.hasNext()) {
            final Statement result = stmts.next();
            final Literal l = (Literal) result.getObject();
            assertTrue(l.getDatatype().equals(XSD.DATETIME));
            final URI action = result.getPredicate();
            if (action.equals(RDRHistory.Vocab.ADDED)) {
              adds++;
            } else if (action.equals(RDRHistory.Vocab.REMOVED)) {
              removes++;
            } else {
              fail();
            }
          }
          assertEquals(2, adds);
          assertEquals(1, removes);
        } finally {
          stmts.close();
        }
      }

    } finally {
      if (cxn != null) cxn.close();

      sail.__tearDownUnitTest();
    }
  }

  /** Test custom history handler. */
  public void testCustomHistory() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getProperties(CustomRDRHistory.class));

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI(":s");
      final URI p = vf.createURI(":p");
      final URI o = vf.createURI(":o");
      final Literal l = vf.createLiteral("o");

      EmbergraphStatement stmt1 = vf.createStatement(s, p, o);
      EmbergraphStatement stmt2 = vf.createStatement(s, p, l);
      cxn.add(stmt1);
      cxn.add(stmt2);
      cxn.commit();

      if (log.isInfoEnabled()) {
        log.info(cxn.getTripleStore().dumpStore().insert(0, '\n'));
      }

      assertEquals(3, cxn.size());

      {
        final RepositoryResult<Statement> stmts = cxn.getStatements(s, p, o, true);
        try {
          assertTrue(stmts.hasNext());
        } finally {
          stmts.close();
        }
      }

      {
        final RepositoryResult<Statement> stmts = cxn.getStatements(s, p, l, true);
        try {
          assertTrue(stmts.hasNext());
        } finally {
          stmts.close();
        }
      }

      {
        final EmbergraphBNode sid = vf.createBNode(stmt1);
        final RepositoryResult<Statement> stmts =
            cxn.getStatements(sid, RDRHistory.Vocab.ADDED, null, true);
        try {
          assertFalse(stmts.hasNext());
        } finally {
          stmts.close();
        }
      }

      {
        final EmbergraphBNode sid = vf.createBNode(stmt2);
        final RepositoryResult<Statement> stmts =
            cxn.getStatements(sid, RDRHistory.Vocab.ADDED, null, true);
        try {
          assertTrue(stmts.hasNext());
          final Literal l2 = (Literal) stmts.next().getObject();
          assertTrue(l2.getDatatype().equals(XSD.DATETIME));
        } finally {
          stmts.close();
        }
      }

    } finally {
      if (cxn != null) cxn.close();

      sail.__tearDownUnitTest();
    }
  }

  /** Test the SPARQL integration. */
  public void testSparqlIntegration() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getProperties());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);

      {
        final EmbergraphSailRepositoryConnection read = repo.getReadOnlyConnection();

        try {
          read.prepareTupleQuery(QueryLanguage.SPARQL, "select * { ?s ?p ?o }").evaluate();
        } finally {
          read.close();
        }
      }

      cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();

      {
        final String sparql =
            "insert { "
                + "  ?s <:p> \"foo\" . "
                + "} where { "
                + "  values (?s) { "
                + "    (<:s1>) "
                + "    (<:s2>) "
                + "  } "
                + "}";

        cxn.prepareUpdate(QueryLanguage.SPARQL, sparql).execute();
        cxn.commit();
      }

      {
        final String sparql =
            "delete { "
                + "  ?s <:p> ?o . "
                + "} insert { "
                + "  ?s <:p> \"bar\" . "
                + "} where { "
                + "  ?s <:p> ?o . "
                +
                //                        "  values (?s) { " +
                //                        "    (<:s1>) " +
                //                        "  } " +
                "}";

        cxn.prepareUpdate(QueryLanguage.SPARQL, sparql).execute();
        cxn.commit();
      }

      if (log.isInfoEnabled()) {
        log.info(cxn.getTripleStore().dumpStore().insert(0, '\n'));
      }

      assertEquals(10, cxn.size());

      {
        final String sparql =
            "select ?s ?p ?o ?action ?time \n"
                + "where { \n"
                +
                //                    "  service bd:history { \n" +
                //                    "    << ?s ?p ?o >> ?action ?time . \n" +
                //                    "  } \n" +
                "  bind(<< ?s ?p ?o >> as ?sid) . \n"
                + "  hint:Prior hint:history true . \n"
                + "  ?sid ?action ?time . \n"
                + "  values (?s) { \n"
                + "    (<:s1>) \n"
                + "  } \n"
                + "}";

        final TupleQueryResult result =
            cxn.prepareTupleQuery(QueryLanguage.SPARQL, sparql).evaluate();
        try {
          int i = 0;
          while (result.hasNext()) {
            final BindingSet bs = result.next();
            i++;
            if (log.isDebugEnabled()) {
              log.debug(bs);
            }
          }

          assertEquals(3, i);
        } finally {
          result.close();
        }
      }

      {
        final String sparql =
            "select ?s ?p ?o ?action ?time \n"
                + "where { \n"
                +
                //                    "  service bd:history { \n" +
                //                    "    << ?s ?p ?o >> ?action ?time . \n" +
                //                    "  } \n" +
                "  bind(<< ?s ?p ?o >> as ?sid) . \n"
                + "  hint:Prior hint:history true . \n"
                + "  ?sid ?action ?time . \n"
                + "}";

        final TupleQueryResult result =
            cxn.prepareTupleQuery(QueryLanguage.SPARQL, sparql).evaluate();
        try {
          int i = 0;
          while (result.hasNext()) {
            final BindingSet bs = result.next();
            i++;
            if (log.isDebugEnabled()) {
              log.debug(bs);
            }
          }

          assertEquals(6, i);
        } finally {
          result.close();
        }
      }

      {
        final String sparql =
            "select ?s ?p ?o ?action ?time \n"
                + "where { \n"
                +
                //                    "  service bd:history { \n" +
                //                    "    << ?s <:p> ?o >> <:removed> ?time . \n" +
                //                    "  } \n" +
                "  bind(<< ?s <:p> ?o >> as ?sid) . \n"
                + "  hint:Prior hint:history true . \n"
                + "  ?sid <blaze:history:removed> ?time . \n"
                + "}";

        final TupleQueryResult result =
            cxn.prepareTupleQuery(QueryLanguage.SPARQL, sparql).evaluate();
        try {
          int i = 0;
          while (result.hasNext()) {
            final BindingSet bs = result.next();
            i++;
            if (log.isDebugEnabled()) {
              log.debug(bs);
            }
          }

          assertEquals(2, i);
        } finally {
          result.close();
        }
      }

    } finally {
      if (cxn != null) cxn.close();

      sail.__tearDownUnitTest();
    }
  }

  /**
   * Test whether the RDRHistory can handle statements that are added and removed in the same
   * commit.
   */
  public void testFullyRedundantEvents() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getProperties());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI(":s");
      final URI p = vf.createURI(":p");
      final Literal o = vf.createLiteral("foo");
      final EmbergraphStatement stmt = vf.createStatement(s, p, o);
      final EmbergraphBNode sid = vf.createBNode(stmt);

      cxn.add(stmt);
      cxn.commit();

      assertEquals(1, cxn.getTripleStore().getAccessPath(sid, null, null).rangeCount(false));

      cxn.remove(stmt);
      cxn.add(stmt);
      cxn.commit();

      assertEquals(1, cxn.getTripleStore().getAccessPath(sid, null, null).rangeCount(false));

    } finally {
      if (cxn != null) cxn.close();

      sail.__tearDownUnitTest();
    }
  }

  /**
   * Test whether the RDRHistory can handle statements that are added and removed in the same
   * commit.
   */
  public void testPartiallyRedundantEvents() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getProperties());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = (EmbergraphSailRepositoryConnection) repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI(":s");
      final URI p = vf.createURI(":p");
      final Literal o = vf.createLiteral("foo");
      final Literal bar = vf.createLiteral("bar");
      final EmbergraphStatement stmt = vf.createStatement(s, p, o);
      final EmbergraphStatement stmt2 = vf.createStatement(s, p, bar);
      final EmbergraphBNode sid = vf.createBNode(stmt);
      final EmbergraphBNode sid2 = vf.createBNode(stmt2);

      cxn.add(stmt);
      cxn.commit();

      assertEquals(1, cxn.getTripleStore().getAccessPath(sid, null, null).rangeCount(false));

      cxn.remove(stmt);
      cxn.add(stmt);
      cxn.add(stmt2);
      cxn.commit();

      assertEquals(1, cxn.getTripleStore().getAccessPath(sid, null, null).rangeCount(false));
      assertEquals(1, cxn.getTripleStore().getAccessPath(sid2, null, null).rangeCount(false));

    } finally {
      if (cxn != null) cxn.close();

      sail.__tearDownUnitTest();
    }
  }

  public static class CustomRDRHistory extends RDRHistory {

    public CustomRDRHistory(AbstractTripleStore database) {
      super(database);
    }

    /** Only accept stmts where isLiteral(stmt.o) */
    @Override
    protected boolean accept(final IChangeRecord record) {
      return record.getStatement().o().isLiteral();
    }
  }
}
