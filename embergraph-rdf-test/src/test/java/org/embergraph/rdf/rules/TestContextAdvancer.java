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
 * Created on Oct 1, 2010
 */

package org.embergraph.rdf.rules;

import java.util.Properties;
import junit.framework.TestCase2;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IPredicate.Annotations;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.rdf.filter.StripContextFilter;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.ContextAdvancer;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.store.AbstractTestCase;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.LocalTripleStore;
import org.embergraph.relation.accesspath.IAccessPath;
import org.openrdf.model.vocabulary.RDF;

/**
 * Test suite for the {@link ContextAdvancer}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestContextAdvancer extends TestCase2 {

  /** */
  public TestContextAdvancer() {}

  public TestContextAdvancer(String name) {
    super(name);
  }

  /** Unit test verifies the {@link ContextAdvancer} against the {@link SPOKeyOrder#SPOC} index. */
  public void test_contextAdvancer() {

    final Properties properties = new Properties();

    properties.setProperty(AbstractTripleStore.Options.QUADS_MODE, "true");

    properties.setProperty(Journal.Options.BUFFER_MODE, BufferMode.Transient.toString());

    final Journal store = new Journal(properties);

    try {

      final LocalTripleStore db = new LocalTripleStore(store, "test", ITx.UNISOLATED, properties);

      db.create();

      //            final StatementBuffer<EmbergraphStatement> sb = new
      // StatementBuffer<EmbergraphStatement>(
      //                    db, 100/* capacity */);

      final EmbergraphValueFactory f = db.getValueFactory();

      final EmbergraphURI u1 = f.createURI("http://www.embergraph.org/u1");
      final EmbergraphURI u2 = f.createURI("http://www.embergraph.org/u2");
      final EmbergraphURI v1 = f.createURI("http://www.embergraph.org/v1");
      final EmbergraphURI v2 = f.createURI("http://www.embergraph.org/v2");
      final EmbergraphURI c1 = f.createURI("http://www.embergraph.org/c1");
      final EmbergraphURI c2 = f.createURI("http://www.embergraph.org/c2");
      final EmbergraphURI rdfType = f.createURI(RDF.TYPE.stringValue());

      final EmbergraphValue[] terms =
          new EmbergraphValue[] {
            u1, u2,
            v1, v2,
            c1, c2,
            rdfType
          };

      db.getLexiconRelation().addTerms(terms, terms.length, false /* readOnly */);

      final StatementEnum explicit = StatementEnum.Explicit;
      final EmbergraphStatement[] stmts =
          new EmbergraphStatement[] {
            f.createStatement(u1, rdfType, v1, c1, explicit),
            f.createStatement(u1, rdfType, v1, c2, explicit),
            f.createStatement(u1, rdfType, v2, c1, explicit),
            f.createStatement(u1, rdfType, v2, c2, explicit),
            f.createStatement(u2, rdfType, v1, c1, explicit),
            f.createStatement(u2, rdfType, v1, c2, explicit),
            f.createStatement(u2, rdfType, v2, c1, explicit),
            f.createStatement(u2, rdfType, v2, c2, explicit),
          };

      db.addStatements(stmts, stmts.length);

      db.commit();

      if (log.isInfoEnabled()) log.info(db.dumpStore());

      // The expected distinct statements w/o their context info.
      final EmbergraphStatement[] expectedDistinct =
          new EmbergraphStatement[] {
            f.createStatement(u1, rdfType, v1),
            f.createStatement(u1, rdfType, v2),
            f.createStatement(u2, rdfType, v1),
            f.createStatement(u2, rdfType, v2),
          };

      // predicate using the SPOC index.
      Predicate<ISPO> pred =
          new SPOPredicate(
              new BOp[] {Var.var("s"), Var.var("p"), Var.var("o"), Var.var("c")},
              NV.asMap(
                  //                            new NV(Predicate.Annotations.KEY_ORDER,
                  //                                    SPOKeyOrder.SPOC),
                  new NV(Annotations.TIMESTAMP, ITx.UNISOLATED)));

      //            final BOpContextBase context = new BOpContextBase(null/* fed */,
      //                    store/* indexManager */);

      // First verify assumptions without the advancer.
      {
        final IAccessPath<ISPO> ap = db.getSPORelation().getAccessPath(SPOKeyOrder.SPOC, pred);
        //                final IAccessPath<ISPO> ap = db.getSPORelation().getAccessPath(
        //                        SPOKeyOrder.SPOC, pred);

        assertEquals(SPOKeyOrder.SPOC, ap.getKeyOrder());

        assertEquals(stmts.length, ap.rangeCount(true /* exact */));
      }

      // Now verify assumptions with the advancer.
      {
        pred =
            (Predicate)
                pred.setProperty(
                    Predicate.Annotations.FLAGS, IRangeQuery.DEFAULT | IRangeQuery.CURSOR);

        pred = pred.addIndexLocalFilter(new ContextAdvancer());

        pred = pred.addAccessPathFilter(StripContextFilter.newInstance());

        final IAccessPath<ISPO> ap = db.getSPORelation().getAccessPath(SPOKeyOrder.SPOC, pred);
        //                final IAccessPath<ISPO> ap = context.getAccessPath(db
        //                        .getSPORelation(), pred);

        assertEquals(SPOKeyOrder.SPOC, ap.getKeyOrder());

        assertEquals(4, ap.rangeCount(true /* exact */));

        AbstractTestCase.assertSameSPOsAnyOrder(db, expectedDistinct, ap.iterator());
      }

    } finally {

      store.destroy();
    }
  }
}
