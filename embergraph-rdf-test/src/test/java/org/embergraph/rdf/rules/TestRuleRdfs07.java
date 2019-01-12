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
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.rule.Rule;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;

/*
* Note: rdfs 2, 3, 7, and 9 use the same base class.
 *
 * @see RuleRdfs02
 * @see RuleRdfs03
 * @see RuleRdfs07
 * @see RuleRdfs09
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRuleRdfs07 extends AbstractRuleTestCase {

  /** */
  public TestRuleRdfs07() {}

  /** @param name */
  public TestRuleRdfs07(String name) {
    super(name);
  }

  /*
   * Test of {@link RuleRdfs07} where the data satisifies the rule exactly once.
   *
   * <pre>
   *         triple(?u,?b,?y) :-
   *            triple(?a,rdfs:subPropertyOf,?b),
   *            triple(?u,?a,?y).
   * </pre>
   *
   * @throws Exception
   */
  public void test_rdfs07_01() throws Exception {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      final EmbergraphValueFactory f = store.getValueFactory();

      final URI A = f.createURI("http://www.foo.org/A");
      final URI B = f.createURI("http://www.foo.org/B");
      final URI U = f.createURI("http://www.foo.org/U");
      final URI Y = f.createURI("http://www.foo.org/Y");

      final URI rdfsSubPropertyOf = RDFS.SUBPROPERTYOF;

      store.addStatement(A, rdfsSubPropertyOf, B);
      store.addStatement(U, A, Y);

      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B));
      assertTrue(store.hasStatement(U, A, Y));
      assertFalse(store.hasStatement(U, B, Y));
      assertEquals(2, store.getStatementCount());

      final Rule r = new RuleRdfs07(store.getSPORelation().getNamespace(), store.getVocabulary());

      // apply the rule.
      //            RuleStats stats =
      applyRule(store, r, -1 /*solutionCount*/, 1 /*mutationCount*/);

      // @todo must verify by hand (detailed stats not available to client).
      //            assertEquals("#subqueries",1,stats.nsubqueries[0]);
      //            assertEquals("#subqueries",0,stats.nsubqueries[1]);

      /*
       * validate the state of the primary store.
       */
      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B));
      assertTrue(store.hasStatement(U, A, Y));
      assertTrue(store.hasStatement(U, B, Y));
      assertEquals(3, store.getStatementCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Test of {@link RuleRdfs07} where the data satisifies the rule twice -- there are two matches in
   * the subquery for the same binding on "?a". Only one subquery is made since there is only one
   * match for the first triple pattern.
   *
   * <pre>
   *           triple(?u,?b,?y) :-
   *              triple(?a,rdfs:subPropertyOf,?b),
   *              triple(?u,?a,?y).
   * </pre>
   *
   * @throws Exception
   */
  public void test_rdfs07_02() throws Exception {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      final EmbergraphValueFactory f = store.getValueFactory();

      final URI A = f.createURI("http://www.foo.org/A");
      final URI B = f.createURI("http://www.foo.org/B");
      final URI U1 = f.createURI("http://www.foo.org/U1");
      final URI Y1 = f.createURI("http://www.foo.org/Y1");
      final URI U2 = f.createURI("http://www.foo.org/U2");
      final URI Y2 = f.createURI("http://www.foo.org/Y2");

      final URI rdfsSubPropertyOf = RDFS.SUBPROPERTYOF;

      store.addStatement(A, rdfsSubPropertyOf, B);
      store.addStatement(U1, A, Y1);
      store.addStatement(U2, A, Y2);

      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B));
      assertTrue(store.hasStatement(U1, A, Y1));
      assertTrue(store.hasStatement(U2, A, Y2));
      assertFalse(store.hasStatement(U1, B, Y1));
      assertFalse(store.hasStatement(U2, B, Y2));
      assertEquals(3, store.getStatementCount());

      final Rule r = new RuleRdfs07(store.getSPORelation().getNamespace(), store.getVocabulary());

      // apply the rule.
      //            RuleStats stats =
      applyRule(store, r, -1 /*solutionCount*/, 2 /*mutationCount*/);

      // @todo must verify by hand (detailed stats not available to client).
      //            assertEquals("#subqueries",1,stats.nsubqueries[0]);
      //            assertEquals("#subqueries",0,stats.nsubqueries[1]);

      /*
       * validate the state of the primary store.
       */
      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B));
      assertTrue(store.hasStatement(U1, A, Y1));
      assertTrue(store.hasStatement(U2, A, Y2));
      assertTrue(store.hasStatement(U1, B, Y1));
      assertTrue(store.hasStatement(U2, B, Y2));
      assertEquals(5, store.getStatementCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Test of {@link RuleRdfs07} where the data satisifies the rule twice -- there are two matches on
   * the first triple pattern that have the same subject. However, only one subquery is made since
   * both matches on the first triple pattern have the same subject.
   *
   * <p>Note: This test is used to verify that the JOIN reorders the results from the first triple
   * pattern into SPO order so that fewer subqueries need to be executed (only one subquery in this
   * case).
   *
   * <pre>
   *              triple(?u,?b,?y) :-
   *                 triple(?a,rdfs:subPropertyOf,?b),
   *                 triple(?u,?a,?y).
   * </pre>
   *
   * @throws Exception
   */
  public void test_rdfs07_03() throws Exception {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      final EmbergraphValueFactory f = store.getValueFactory();

      final URI A = f.createURI("http://www.foo.org/A");
      final URI B1 = f.createURI("http://www.foo.org/B1");
      final URI B2 = f.createURI("http://www.foo.org/B2");
      final URI U = f.createURI("http://www.foo.org/U");
      final URI Y = f.createURI("http://www.foo.org/Y");

      final URI rdfsSubPropertyOf = RDFS.SUBPROPERTYOF;

      store.addStatement(A, rdfsSubPropertyOf, B1);
      store.addStatement(A, rdfsSubPropertyOf, B2);
      store.addStatement(U, A, Y);

      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B1));
      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B2));
      assertTrue(store.hasStatement(U, A, Y));
      assertEquals(3, store.getStatementCount());

      final Rule r = new RuleRdfs07(store.getSPORelation().getNamespace(), store.getVocabulary());

      // apply the rule.
      //            RuleStats stats =
      applyRule(store, r, -1 /*solutionCount*/, 2 /*mutationCount*/);

      // FIXME enable tests when working on subquery elimination
      // @todo detailed stats not available to the client.
      //            /*
      //             * Verify that only one subquery is issued (iff subquery elimination
      //             * is turned on for the rule).
      //             */
      //
      // assertEquals("#subqueries",(inf.rdfs7.subqueryElimination)?1:2,stats.nsubqueries[0]);
      //            assertEquals("#subqueries",0,stats.nsubqueries[1]);

      /*
       * validate the state of the primary store.
       */
      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B1));
      assertTrue(store.hasStatement(A, rdfsSubPropertyOf, B2));
      assertTrue(store.hasStatement(U, A, Y));
      assertTrue(store.hasStatement(U, B1, Y)); // entailed.
      assertTrue(store.hasStatement(U, B2, Y)); // entailed.
      assertEquals(5, store.getStatementCount());

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
