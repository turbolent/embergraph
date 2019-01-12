/**
 * The Notice below must appear in each file of the Source Code of any copy you distribute of the
 * Licensed Product. Contributors to any Modifications may add their own copyright notices to
 * identify their own contributions.
 *
 * <p>License:
 *
 * <p>The contents of this file are subject to the CognitiveWeb Open Source License Version 1.1 (the
 * License). You may not copy or use this file, in either source code or executable form, except in
 * compliance with the License. You may obtain a copy of the License from
 *
 * <p>http://www.CognitiveWeb.org/legal/license/
 *
 * <p>Software distributed under the License is distributed on an AS IS basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>Copyrights:
 *
 * <p>Portions created by or assigned to CognitiveWeb are Copyright (c) 2003-2003 CognitiveWeb. All
 * Rights Reserved. Contact information for CognitiveWeb is available at
 *
 * <p>http://www.CognitiveWeb.org
 *
 * <p>Portions Copyright (c) 2002-2003 Bryan Thompson.
 *
 * <p>Acknowledgements:
 *
 * <p>Special thanks to the developers of the Jabber Open Source License 1.0 (JOSL), from which this
 * License was derived. This License contains terms that differ from JOSL.
 *
 * <p>Special thanks to the CognitiveWeb Open Source Contributors for their suggestions and support
 * of the Cognitive Web.
 *
 * <p>Modifications:
 */
/*
 * Created on Jul 7, 2008
 */

package org.embergraph.rdf.rules;

import java.util.Arrays;
import java.util.Properties;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.rio.IStatementBuffer;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.spo.DistinctTermAdvancer;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/**
 * Test suite for the {@link DistinctTermAdvancer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestDistinctTermScan extends AbstractRuleTestCase {

  /** */
  public TestDistinctTermScan() {}

  /** @param name */
  public TestDistinctTermScan(String name) {
    super(name);
  }

  /**
   * FIXME The distinct term scan has been moved into {@link AbstractRuleDistinctTermScan} and needs
   * to be evaluated in that context.
   */
  public void test_getDistinctTermIdentifiers() {

    final Properties properties = super.getProperties();

    // override the default axiom model.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final AbstractTripleStore store = getStore(properties);

    try {

      URI A = new URIImpl("http://www.foo.org/A");
      URI B = new URIImpl("http://www.foo.org/B");
      URI C = new URIImpl("http://www.foo.org/C");
      URI D = new URIImpl("http://www.foo.org/D");
      URI E = new URIImpl("http://www.foo.org/E");

      {
        IStatementBuffer buffer = new StatementBuffer(store, 100 /* capacity */);

        buffer.add(A, B, C);
        buffer.add(C, B, D);
        buffer.add(A, E, C);

        // flush statements to the store.
        buffer.flush();
      }

      assertTrue(store.hasStatement(A, B, C));
      assertTrue(store.hasStatement(C, B, D));
      assertTrue(store.hasStatement(A, E, C));

      // distinct subject term identifiers.
      {
        IV[] expected = new IV[] {store.getIV(A), store.getIV(C)};

        // term identifiers will be in ascending order.
        Arrays.sort(expected);

        assertSameIterator(expected, store.getSPORelation().distinctTermScan(SPOKeyOrder.SPO));
      }

      // distinct predicate term identifiers.
      {
        IV[] expected = new IV[] {store.getIV(B), store.getIV(E)};

        // term identifiers will be in ascending order.
        Arrays.sort(expected);

        assertSameIterator(expected, store.getSPORelation().distinctTermScan(SPOKeyOrder.POS));
      }

      // distinct object term identifiers.
      {
        IV[] expected = new IV[] {store.getIV(C), store.getIV(D)};

        // term identifiers will be in ascending order.
        Arrays.sort(expected);

        assertSameIterator(expected, store.getSPORelation().distinctTermScan(SPOKeyOrder.OSP));
      }

    } finally {

      store.__tearDownUnitTest();
    }
  }
}
