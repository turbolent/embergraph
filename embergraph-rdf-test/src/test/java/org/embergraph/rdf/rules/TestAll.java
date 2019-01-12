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
package org.embergraph.rdf.rules;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.embergraph.rdf.spo.TestSPORelation;

/**
 * Aggregates test suites into increasing dependency order.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestAll extends TestCase {

  /** */
  public TestAll() {}

  /** @param arg0 */
  public TestAll(String arg0) {
    super(arg0);
  }

  /** Returns a test that will run each of the implementation specific test suites in turn. */
  public static Test suite() {

    final TestSuite suite = new TestSuite("Inference and truth maintenance");

    // test suite for the axiom models and their persistence.
    suite.addTest(org.embergraph.rdf.axioms.TestAll.suite());

    /*
     * test ability to insert, update, or remove elements from a relation
     * and the ability to select the right access path given a predicate for
     * that relation and query for those elements (we have to test all this
     * stuff together since testing query requires us to have some data in
     * the relation).
     */
    suite.addTestSuite(TestSPORelation.class);

    /*
     * test suite for modelsEqual().
     */
    suite.addTestSuite(TestModelsEqual.class);

    /*
     * test forward chainer rules.
     */

    // test suite for rule re-writes for RDF DB truth maintenance.
    suite.addTestSuite(TestTMUtility.class);

    // test suite for rule re-writes.
    suite.addTestSuite(TestMappedProgram.class);

    // test suite for writing, reading, chasing and retracting justifications.
    suite.addTestSuite(TestJustifications.class);

    // test suite for distinct term scan
    suite.addTestSuite(TestDistinctTermScan.class);

    // test suite for the ContextAdvancer.
    suite.addTestSuite(TestContextAdvancer.class);

    // test suite for rdf1.
    suite.addTestSuite(TestRuleRdf01.class);

    // test that rdfs3 does not let literals into the subject.
    suite.addTestSuite(TestRuleRdfs03.class);

    // test suite for rdfs4.
    suite.addTestSuite(TestRuleRdfs04.class);

    // Note: rdfs 2, 3, 7, and 9 use the same base class.
    suite.addTestSuite(TestRuleRdfs07.class);

    // Note: rdfs 6, 8, 10, 12, and 13 use the same base class.
    suite.addTestSuite(TestRuleRdfs10.class);

    // Note: rdfs 5 and 11 use the same base class.
    suite.addTestSuite(TestRuleRdfs11.class);

    // test suite for the "match" rule (entity matching).
    suite.addTestSuite(TestMatch.class);

    // Note: fast closure rules using the same base class.
    suite.addTestSuite(TestRuleFastClosure_11_13.class);

    // Note: fast closure rules using the same base class.
    suite.addTestSuite(TestRuleFastClosure_3_5_6_7_9.class);

    // owl:sameAs rules.
    suite.addTestSuite(TestRuleOwlSameAs.class);

    // test owl:equivalentClass
    suite.addTestSuite(TestRuleOwlEquivalentClass.class);

    // test owl:equivalentProperty
    suite.addTestSuite(TestRuleOwlEquivalentProperty.class);

    // test owl:transitiveProperty
    suite.addTestSuite(TestRuleOwlTransitiveProperty.class);

    // test owl:inverseOf
    suite.addTestSuite(TestRuleOwlInverseOf.class);

    // test owl:hasValue
    suite.addTestSuite(TestRuleOwlHasValue.class);

    /*
     * Test for optionals and for query options applied during native rule
     * and program evaluation.
     *
     * Note: These features MUST also be tested at the Sesame integration
     * level. Tests at that level test the correct transform of the query
     * operation tree into an IRule or IProgram and the correct handling of
     * the generated (typically asynchronous) iterator reading on the
     * generated solutions.
     */

    // test suite for optionals.
    suite.addTestSuite(TestOptionals.class);

    // test suite for SLICE (OFFSET + LIMIT).
    suite.addTestSuite(TestSlice.class);

    // test suite for DISTINCT.
    suite.addTestSuite(TestDistinct.class);

    // test suite for ORDER_BY.
    suite.addTestSuite(TestOrderBy.class);

    // test suite for UNION, including interaction with SLICE, DISTINCT, and ORDER_BY.
    suite.addTestSuite(TestUnion.class);

    /*
     * Tests for computing closure.
     */

    // test suite for fix point closure of some rule sets (full and fast).
    suite.addTestSuite(TestDatabaseAtOnceClosure.class);

    // compare two means of computing owl:sameAs for equivalence.
    suite.addTestSuite(TestCompareFullAndFastClosure.class);

    /*
     * Test entailments that are computed at query time rather than when the
     * data are loaded into the store.
     *
     * Note: These are sometimes referred to as "backchained" rules, but in
     * fact they are either highly specialized code, e.g., for the (x
     * rdf:Type: rdfs:Resource) entailments, or an application of the
     * relevant rules using the forward chainer once the rules have been
     * constrained by the triple pattern (similar to magic sets but less
     * general).
     */

    // test suite for backward chaining of (?x rdf:type rdfs:Resource).
    suite.addTestSuite(TestBackchainTypeResourceIterator.class);

    //        // test suite for owl:sameAs {2,3} (constained forward evaluation).
    //        suite.addTestSuite( TestBackchainOwlSameAs.class );

    // test suite for owl:sameAs {2,3} (backward chaining).
    suite.addTestSuite(TestBackchainOwlSameAsPropertiesIterator.class);

    // test suite for RDF specific rule evaluation support.
    suite.addTest(org.embergraph.rdf.internal.constraints.TestAll.suite());

    /*
     * Test truth maintenance. This examines the incremental assertion and
     * retraction of statements buffered in a [focusStore] against the
     * database.  Database-at-once closure is tested above.
     */

    // test suite for basic TM mechanism encapsulated by this class.
    suite.addTestSuite(TestTruthMaintenance.class);

    return suite;
  }
}
