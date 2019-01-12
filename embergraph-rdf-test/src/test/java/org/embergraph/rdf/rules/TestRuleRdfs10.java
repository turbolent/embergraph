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
 * Created on Apr 13, 2007
 */

package org.embergraph.rdf.rules;

import java.util.Properties;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.rio.IStatementBuffer;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
 * Note: rdfs 6, 8, 10, 12, and 13 use the same base clase.
 *
 * @see RuleRdfs06
 * @see RuleRdfs08
 * @see RuleRdfs10
 * @see RuleRdfs12
 * @see RuleRdfs13
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRuleRdfs10 extends AbstractRuleTestCase {

  /** */
  public TestRuleRdfs10() {}

  /** @param name */
  public TestRuleRdfs10(String name) {
    super(name);
  }

  /*
   * Test of {@link RuleRdfs10} where the data satisifies the rule exactly once.
   *
   * <pre>
   * (?u,rdfs:subClassOf,?u) :- (?u,rdf:type,rdfs:Class).
   * </pre>
   *
   * @throws Exception
   */
  public void test_rdfs10_01() throws Exception {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      URI U = new URIImpl("http://www.foo.org/U");

      IStatementBuffer buffer = new StatementBuffer(store, 100 /* capacity */);

      buffer.add(U, RDF.TYPE, RDFS.CLASS);

      // write on the store.
      buffer.flush();

      // verify statement(s).
      assertTrue(store.hasStatement(U, RDF.TYPE, RDFS.CLASS));
      assertEquals(1, store.getStatementCount());

      Rule r = new RuleRdfs10(store.getSPORelation().getNamespace(), store.getVocabulary());

      // apply the rule.
      applyRule(store, r, -1 /* solutionCount */, 1 /* mutationCount */);

      /*
       * validate the state of the primary store.
       */

      // told
      assertTrue(store.hasStatement(U, RDF.TYPE, RDFS.CLASS));

      // entailed
      assertTrue(store.hasStatement(U, RDFS.SUBCLASSOF, U));

      // final #of statements in the store.
      assertEquals(2, store.getStatementCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Test of {@link RuleRdfs10} where the data satisifies the rule exactly twice.
   *
   * <pre>
   * (?u,rdfs:subClassOf,?u) :- (?u,rdf:type,rdfs:Class).
   * </pre>
   *
   * @throws Exception
   */
  public void test_rdfs10_02() throws Exception {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      URI U1 = new URIImpl("http://www.foo.org/U1");
      URI U2 = new URIImpl("http://www.foo.org/U2");

      IStatementBuffer buffer = new StatementBuffer(store, 100 /* capacity */);

      buffer.add(U1, RDF.TYPE, RDFS.CLASS);
      buffer.add(U2, RDF.TYPE, RDFS.CLASS);

      // write on the store.
      buffer.flush();

      // verify statement(s).
      assertTrue(store.hasStatement(U1, RDF.TYPE, RDFS.CLASS));
      assertTrue(store.hasStatement(U1, RDF.TYPE, RDFS.CLASS));
      assertEquals(2, store.getStatementCount());

      final Rule r = new RuleRdfs10(store.getSPORelation().getNamespace(), store.getVocabulary());

      // apply the rule.
      applyRule(store, r, -1 /*solutionCount*/, 2 /*mutationCount*/);

      /*
       * validate the state of the primary store.
       */

      // told
      assertTrue(store.hasStatement(U1, RDF.TYPE, RDFS.CLASS));
      assertTrue(store.hasStatement(U2, RDF.TYPE, RDFS.CLASS));

      // entailed
      assertTrue(store.hasStatement(U1, RDFS.SUBCLASSOF, U1));
      assertTrue(store.hasStatement(U2, RDFS.SUBCLASSOF, U2));

      // final #of statements in the store.
      assertEquals(4, store.getStatementCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
