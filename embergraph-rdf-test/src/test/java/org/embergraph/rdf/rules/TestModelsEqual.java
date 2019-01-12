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
 * Created on Aug 25, 2008
 */

package org.embergraph.rdf.rules;

import cutthecrap.utils.striterators.ICloseableIterator;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.TripleStoreUtility;
import org.openrdf.model.vocabulary.RDFS;

/*
 * Test suite for {@link TripleStoreUtility#modelsEqual(AbstractTripleStore, AbstractTripleStore)}
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestModelsEqual extends AbstractRuleTestCase {

  /** */
  public TestModelsEqual() {}

  /** @param name */
  public TestModelsEqual(String name) {
    super(name);
  }

  /*
   * Test compares two stores with the same data, then removes a statement from one store and
   * re-compares the stores to verify that the inconsistency is detected.
   *
   * @throws Exception
   */
  public void test_modelsEqual() throws Exception {

    final AbstractTripleStore store1 = getStore();
    final AbstractTripleStore store2 = getStore();
    assertTrue(store1 != store2);
    try {

      // write statements on one store.
      {
        final EmbergraphValueFactory f = store1.getValueFactory();

        final EmbergraphURI A = f.createURI("http://www.embergraph.org/a");
        final EmbergraphURI B = f.createURI("http://www.embergraph.org/b");
        final EmbergraphURI C = f.createURI("http://www.embergraph.org/c");
        final EmbergraphURI D = f.createURI("http://www.embergraph.org/d");
        final EmbergraphURI SCO = f.asValue(RDFS.SUBCLASSOF);

        StatementBuffer buf = new StatementBuffer(store1, 10);

        buf.add(A, SCO, B);
        buf.add(B, SCO, C);
        buf.add(C, SCO, D);

        buf.flush();
      }

      // write the same statements on the other store.
      {
        final EmbergraphValueFactory f = store1.getValueFactory();

        final EmbergraphURI A = f.createURI("http://www.embergraph.org/a");
        final EmbergraphURI B = f.createURI("http://www.embergraph.org/b");
        final EmbergraphURI C = f.createURI("http://www.embergraph.org/c");
        final EmbergraphURI D = f.createURI("http://www.embergraph.org/d");
        final EmbergraphURI SCO = f.asValue(RDFS.SUBCLASSOF);

        StatementBuffer buf = new StatementBuffer(store2, 10);

        buf.add(A, SCO, B);
        buf.add(B, SCO, C);
        buf.add(C, SCO, D);

        buf.flush();
      }

      // verify all in store1 also found in store2.
      {
        final ICloseableIterator<EmbergraphStatement> notFoundItr =
            TripleStoreUtility.notFoundInTarget(store1, store2);
        try {
          assertFalse(notFoundItr.hasNext());
        } finally {
          notFoundItr.close();
        }
      }

      // verify all in store2 also found in store1.
      {
        final ICloseableIterator<EmbergraphStatement> notFoundItr =
            TripleStoreUtility.notFoundInTarget(store2, store1);
        try {
          assertFalse(notFoundItr.hasNext());
        } finally {
          notFoundItr.close();
        }
      }

      // high-level test.
      assertTrue(TripleStoreUtility.modelsEqual(store1, store2));

      // now remove one statement from store2.
      {
        final EmbergraphValueFactory f = store1.getValueFactory();

        final EmbergraphURI A = f.createURI("http://www.embergraph.org/a");
        final EmbergraphURI B = f.createURI("http://www.embergraph.org/b");
        final EmbergraphURI SCO = f.asValue(RDFS.SUBCLASSOF);

        assertEquals(1L, store2.removeStatements(A, SCO, B));

        assertFalse(store2.hasStatement(A, SCO, B));

        log.info("Removed one statement from store2.");
      }

      // verify one in store1 NOT FOUND in store2.
      {
        final ICloseableIterator<EmbergraphStatement> notFoundItr =
            TripleStoreUtility.notFoundInTarget(store1, store2);
        int nnotFound = 0;
        try {
          while (notFoundItr.hasNext()) {
            notFoundItr.next();
            nnotFound++;
          }
        } finally {
          notFoundItr.close();
        }
        assertEquals("#notFound", 1, nnotFound);
      }

      // verify all in store2 found in store1.
      {
        final ICloseableIterator<EmbergraphStatement> notFoundItr =
            TripleStoreUtility.notFoundInTarget(store2, store1);
        try {
          assertFalse(notFoundItr.hasNext());
        } finally {
          notFoundItr.close();
        }
      }

      // high-level test.
      assertFalse(TripleStoreUtility.modelsEqual(store1, store2));

    } finally {
      store1.__tearDownUnitTest();
      store2.__tearDownUnitTest();
    }
  }
}
