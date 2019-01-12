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
 * Created on Oct 22, 2007
 */

package org.embergraph.rdf.rules;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.Vocabulary;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;

/*
* Test suite for {@link AbstractRuleFastClosure_11_13}.
 *
 * @see RuleFastClosure11
 * @see RuleFastClosure13
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRuleFastClosure_11_13 extends AbstractRuleTestCase {

  /** */
  public TestRuleFastClosure_11_13() {}

  /** @param name */
  public TestRuleFastClosure_11_13(String name) {
    super(name);
  }

  /*
   * Tests {@link RuleFastClosure11} with the minimum data required to compute a single entailment.
   */
  public void test_RuleFastForwardClosure11() throws Exception {

    AbstractTripleStore store = getStore();

    try {

      final EmbergraphValueFactory f = store.getValueFactory();

      final IV a = store.addTerm(f.createURI("http://www.embergraph.org/a"));
      final IV b = store.addTerm(f.createURI("http://www.embergraph.org/b"));
      final IV y = store.addTerm(f.createURI("http://www.embergraph.org/y"));
      final IV x = store.addTerm(f.createURI("http://www.embergraph.org/x"));
      final IV z = store.addTerm(f.createURI("http://www.embergraph.org/z"));

      final Vocabulary vocab = store.getVocabulary();

      // told:
      {
        SPO[] told =
            new SPO[] {
              new SPO(x, y, z, StatementEnum.Explicit),
              new SPO(y, vocab.get(RDFS.SUBPROPERTYOF), a, StatementEnum.Explicit),
              new SPO(a, vocab.get(RDFS.DOMAIN), b, StatementEnum.Explicit)
            };

        store.addStatements(told, told.length);
      }

      // entails:
      // store.addStatement(x, inf.rdfType.get(), b);

      //            store.commit();

      if (log.isInfoEnabled()) log.info("\n" + store.dumpStore());

      /*
       * (?x, ?y, ?z), (?y, rdfs:subPropertyOf, ?a), (?a, rdfs:domain, ?b) ->
       * (?x, rdf:type, ?b).
       */
      RuleFastClosure11 rule = new RuleFastClosure11(store.getSPORelation().getNamespace(), vocab);

      /*
       * Test run the rule.
       */

      applyRule(store, rule, 1 /*solutionCount*/, 1 /*mutationCount*/);

      // check entailments.
      assertTrue(store.hasStatement(x, vocab.get(RDF.TYPE), b));

      //            store.commit();

    } finally {

      store.__tearDownUnitTest();
    }
  }

  /*
   * Tests {@link RuleFastClosure13} with the minimum data required to compute a single entailment.
   *
   * @throws Exception
   */
  public void test_RuleFastForwardClosure13() throws Exception {

    AbstractTripleStore store = getStore();

    try {

      final EmbergraphValueFactory f = store.getValueFactory();

      final IV a = store.addTerm(f.createURI("http://www.embergraph.org/a"));
      final IV b = store.addTerm(f.createURI("http://www.embergraph.org/b"));
      final IV y = store.addTerm(f.createURI("http://www.embergraph.org/y"));
      final IV x = store.addTerm(f.createURI("http://www.embergraph.org/x"));
      final IV z = store.addTerm(f.createURI("http://www.embergraph.org/z"));

      final Vocabulary vocab = store.getVocabulary();

      // told:
      {
        SPO[] told =
            new SPO[] {
              new SPO(x, y, z, StatementEnum.Explicit),
              new SPO(y, vocab.get(RDFS.SUBPROPERTYOF), a, StatementEnum.Explicit),
              new SPO(a, vocab.get(RDFS.RANGE), b, StatementEnum.Explicit)
            };

        store.addStatements(told, told.length);
      }

      // entails:
      // store.addStatement(z, inf.rdfType.get(), b);

      //            store.commit();

      if (log.isInfoEnabled()) log.info("\n" + store.dumpStore());

      /*
       * (?x, ?y, ?z), (?y, rdfs:subPropertyOf, ?a), (?a, rdfs:domain, ?b) ->
       * (?z, rdf:type, ?b).
       */
      RuleFastClosure13 rule = new RuleFastClosure13(store.getSPORelation().getNamespace(), vocab);

      /*
       * Test run the rule.
       */

      applyRule(store, rule, 1 /*solutionCount*/, 1 /*mutationCount*/);

      // check entailments.
      assertTrue(store.hasStatement(z, vocab.get(RDF.TYPE), b));

      //            store.commit();

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
