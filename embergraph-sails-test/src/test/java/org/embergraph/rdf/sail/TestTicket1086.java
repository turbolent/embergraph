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
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.vocab.NoVocabulary;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

/*
 * Test suite for ticket #1086: when loading quads data into a triples store, there now is a config
 * option EmbergraphSail.Options.REJECT_QUADS_IN_TRIPLE_MODE. When this option is not set, the quads
 * shall be simply loaded will stripping the context away; otherwise, an exception shall be thrown.
 * This test case tests both situations.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class TestTicket1086 extends ProxyEmbergraphSailTestCase {

  public Properties getTriplesNoInference() {

    Properties props = super.getProperties();

    // triples with sids
    props.setProperty(EmbergraphSail.Options.QUADS, "false");
    props.setProperty(EmbergraphSail.Options.STATEMENT_IDENTIFIERS, "false");

    // no inference
    props.setProperty(EmbergraphSail.Options.TRUTH_MAINTENANCE, "false");
    props.setProperty(EmbergraphSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
    props.setProperty(EmbergraphSail.Options.VOCABULARY_CLASS, NoVocabulary.class.getName());
    props.setProperty(EmbergraphSail.Options.JUSTIFY, "false");
    props.setProperty(EmbergraphSail.Options.TEXT_INDEX, "false");

    return props;
  }

  /*
   * Returns a configuration where stripping of quads within the loading process is disabled.
   *
   * @return
   */
  public Properties getTriplesNoInferenceNoQuadsStripping() {

    Properties props = getTriplesNoInference();
    props.setProperty(EmbergraphSail.Options.REJECT_QUADS_IN_TRIPLE_MODE, "true");

    return props;
  }

  /** */
  public TestTicket1086() {}

  /** @param arg0 */
  public TestTicket1086(String arg0) {
    super(arg0);
  }

  /** When loading quads into a triple store, the context is striped away by default. */
  public void testQuadStripping() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getTriplesNoInference());

    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI("http://test/s");
      final URI p = vf.createURI("http://test/p");
      final URI o = vf.createURI("http://test/o");
      final URI c = vf.createURI("http://test/c");

      EmbergraphStatement stmt = vf.createStatement(s, p, o, c);
      cxn.add(stmt);

      RepositoryResult<Statement> stmts = cxn.getStatements(null, null, null, false);
      Statement res = stmts.next();
      assertEquals(s, res.getSubject());
      assertEquals(p, res.getPredicate());
      assertEquals(o, res.getObject());
      assertEquals(null, res.getContext());

    } finally {
      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }
  }

  /*
   * When loading quads into a triple store and the EmbergraphSail option
   * REJECT_QUADS_IN_TRIPLE_MODE is set to true, an exception will be thrown.
   */
  public void testQuadStrippingRejected() throws Exception {

    EmbergraphSailRepositoryConnection cxn = null;

    final EmbergraphSail sail = getSail(getTriplesNoInferenceNoQuadsStripping());

    boolean exceptionEncountered = false;
    try {

      sail.initialize();
      final EmbergraphSailRepository repo = new EmbergraphSailRepository(sail);
      cxn = repo.getConnection();

      final EmbergraphValueFactory vf = (EmbergraphValueFactory) sail.getValueFactory();
      final URI s = vf.createURI("http://test/s");
      final URI p = vf.createURI("http://test/p");
      final URI o = vf.createURI("http://test/o");
      final URI c = vf.createURI("http://test/c");

      EmbergraphStatement stmt = vf.createStatement(s, p, o, c);
      cxn.add(stmt);

    } catch (RepositoryException e) {

      exceptionEncountered = true; // expected !

    } finally {

      if (cxn != null) cxn.close();
      sail.__tearDownUnitTest();
    }

    assertTrue(exceptionEncountered);
  }
}
