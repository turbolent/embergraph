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
 * Created on Jan 27, 2007
 */

package org.embergraph.rdf.sail;

import cutthecrap.utils.striterators.Resolver;
import cutthecrap.utils.striterators.Striterator;
import java.util.Iterator;
import java.util.Properties;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.changesets.ChangeAction;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sparql.ast.service.history.HistoryChangeRecord;
import org.embergraph.rdf.sparql.ast.service.history.HistoryServiceFactory;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.AbstractRelation;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.sail.SailException;

/**
 * Test the index supporting the {@link HistoryServiceFactory}.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/607">History Service</a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 *     <p>TODO The unit tests should cover both the tx and non-tx modes.
 *     <p>For read/write tx, we can not use an unisolated history index since changes could become
 *     committed before a given tx commits due to a concurrent tx commit. That would break the
 *     semantics of the history index if the tx then fails rather than committing since some of its
 *     changes would have become visible and durable anyway.
 *     <p>We might need a write-write conflict resolver for the history index when read-write tx are
 *     used. Otherwise a conflict on the history index could cause a tx to fail. I suspect that we
 *     can reconcile exactly the same write-write conflicts on the history index that we can
 *     reconcile on the statement indices, but this needs to be worked through in detail.
 */
public class TestHistoryIndex extends ProxyEmbergraphSailTestCase {

  //    private static final Logger log = Logger.getLogger(TestHistoryIndex.class);

  /** */
  public TestHistoryIndex() {}

  /** @param name */
  public TestHistoryIndex(String name) {
    super(name);
  }

  /**
   * Return the pre-existing history index.
   *
   * @param tripleStore The KB.
   * @return The history index -or- <code>null</code> if it was not configured.
   */
  private IIndex getHistoryIndex(final AbstractTripleStore tripleStore) {

    final SPORelation spoRelation = tripleStore.getSPORelation();

    final String fqn = AbstractRelation.getFQN(spoRelation, SPORelation.NAME_HISTORY);

    final IIndex ndx = spoRelation.getIndex(fqn);

    return ndx;
  }

  /** Unit test verifies that the history index is not created if the option is not enabled. */
  public void test_historyIndexDisabled() throws SailException {

    final Properties properties = getProperties();

    // disable the history service.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.HISTORY_SERVICE, "false");

    final EmbergraphSail sail = getSail(properties);

    try {

      sail.initialize();

      final EmbergraphSailConnection conn = sail.getConnection();

      try {

        // Resolve the index that the connection will write on.
        final IIndex ndx = getHistoryIndex(conn.getTripleStore());

        // The index should not exist.
        assertNull(ndx);

        conn.rollback();

      } finally {

        conn.close();
      }

    } finally {

      sail.__tearDownUnitTest();
    }
  }

  /**
   * Unit test works its way through two commit points, verifying the state changes in the history
   * index in depth. In the first commit point, two statements are added and both should appear in
   * the history index. In the second commit point, one of the statements is removed and an entry
   * for that removal is also added to the history index.
   */
  public void test_historyIndex01() throws SailException {

    final Properties properties = getProperties();

    // enable the history service.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.HISTORY_SERVICE, "true");

    // disable inference.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final EmbergraphSail sail = getSail(properties);

    try {

      sail.initialize();

      /*
       * Verify that we can add some statements and they will appear in
       * the history index.
       */
      final long revisionTime0;
      final ISPO stmt0, stmt1;
      {
        final EmbergraphSailConnection conn = sail.getConnection();

        try {

          // Expected revision time for the history index entries.
          revisionTime0 = conn.getTripleStore().getIndexManager().getLastCommitTime() + 1;

          // Resolve the index that the connection will write on.
          final IIndex ndx = getHistoryIndex(conn.getTripleStore());

          // The index should exist.
          assertNotNull(ndx);

          // The index should be empty.
          assertEquals(0L, ndx.rangeCount());

          final EmbergraphValueFactory f = (EmbergraphValueFactory) sail.getValueFactory();

          final EmbergraphURI A = f.createURI("http://www.embergraph.org/A");
          final EmbergraphURI B = f.createURI("http://www.embergraph.org/B");
          final EmbergraphURI C = f.createURI("http://www.embergraph.org/C");
          final EmbergraphURI rdfType = f.asValue(RDF.TYPE);

          conn.addStatement(A, rdfType, B);
          conn.addStatement(A, rdfType, C);

          conn.commit();

          // Should be 2 entries (more if inference is enabled).
          assertEquals(2L, ndx.rangeCount());

          stmt0 = new SPO(A.getIV(), rdfType.getIV(), B.getIV(), StatementEnum.Explicit);

          stmt1 = new SPO(A.getIV(), rdfType.getIV(), C.getIV(), StatementEnum.Explicit);

          @SuppressWarnings("unchecked")
          final Iterator<HistoryChangeRecord> itr =
              new Striterator(ndx.rangeIterator())
                  .addFilter(
                      new Resolver() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Object resolve(final Object obj) {
                          return ((ITuple<HistoryChangeRecord>) obj).getObject();
                        }
                      });

          final HistoryChangeRecord[] a =
              new HistoryChangeRecord[] {
                new HistoryChangeRecord(stmt0, ChangeAction.INSERTED, revisionTime0),
                new HistoryChangeRecord(stmt1, ChangeAction.INSERTED, revisionTime0),
              };

          // for (HistoryChangeRecord r : a) {
          // System.err.println("Expected: " + r);
          // System.out.println("Actual  : " + itr.next());
          // }

          // Verify the records in the index.
          assertSameIteratorAnyOrder(a, itr);

        } finally {

          conn.close();
        }
      }

      /*
       * Verify that we can remove a statements and it will appear in the
       * history index as a new entry. The statement that we do not remove
       * will have only one entry.
       */
      final long revisionTime1;
      {
        final EmbergraphSailConnection conn = sail.getConnection();

        try {

          // Expected revision time for the history index entries.
          revisionTime1 = conn.getTripleStore().getIndexManager().getLastCommitTime() + 1;

          // Resolve the index that the connection will write on.
          final IIndex ndx = getHistoryIndex(conn.getTripleStore());

          // The index should exist.
          assertNotNull(ndx);

          // The index should not be empty.
          assertEquals(2L, ndx.rangeCount());

          final EmbergraphValueFactory f = (EmbergraphValueFactory) sail.getValueFactory();

          final EmbergraphURI A = f.createURI("http://www.embergraph.org/A");
          final EmbergraphURI B = f.createURI("http://www.embergraph.org/B");
          //                    final EmbergraphURI C = f
          //                            .createURI("http://www.embergraph.org/C");
          final EmbergraphURI rdfType = f.asValue(RDF.TYPE);

          conn.removeStatements(A, rdfType, B);

          conn.commit();

          // Should be 3 entries (more if inference is enabled).
          assertEquals(3L, ndx.rangeCount());

          @SuppressWarnings("unchecked")
          final Iterator<HistoryChangeRecord> itr =
              new Striterator(ndx.rangeIterator())
                  .addFilter(
                      new Resolver() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Object resolve(final Object obj) {
                          return ((ITuple<HistoryChangeRecord>) obj).getObject();
                        }
                      });

          final HistoryChangeRecord[] a =
              new HistoryChangeRecord[] {
                new HistoryChangeRecord(stmt0, ChangeAction.INSERTED, revisionTime0),
                new HistoryChangeRecord(stmt1, ChangeAction.INSERTED, revisionTime0),
                new HistoryChangeRecord(stmt0, ChangeAction.REMOVED, revisionTime1),
              };

          //                     for (HistoryChangeRecord r : a) {
          //                     System.err.println("Expected: " + r);
          //                     System.out.println("Actual  : " + itr.next());
          //                     }

          // Verify the records in the index.
          assertSameIteratorAnyOrder(a, itr);

        } finally {

          conn.close();
        }
      }

    } finally {

      sail.__tearDownUnitTest();
    }
  }

  /**
   * Unit test works its way through two commit points when the index pruning is set to 1
   * millisecond, verifying the state changes in the history index in depth. In the first commit
   * point, two statements are added and both should appear in the history index. In the second
   * commit point, one of the statements is removed and an entry for that removal is also added to
   * the history index. However, both of the original entries in the history index have been aged
   * out so they no longer appear.
   */
  public void test_historyIndexWithPruning01() throws SailException {

    final Properties properties = getProperties();

    // enable the history service.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.HISTORY_SERVICE, "true");

    // prune history after 1 millisecond.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.HISTORY_SERVICE_MIN_RELEASE_AGE, "1");

    // disable inference.
    properties.setProperty(
        org.embergraph.rdf.store.AbstractTripleStore.Options.AXIOMS_CLASS,
        NoAxioms.class.getName());

    final EmbergraphSail sail = getSail(properties);

    try {

      sail.initialize();

      /*
       * Verify that we can add some statements and they will appear in
       * the history index.
       */
      final long revisionTime0;
      final ISPO stmt0, stmt1;
      {
        final EmbergraphSailConnection conn = sail.getConnection();

        try {

          // Expected revision time for the history index entries.
          revisionTime0 = conn.getTripleStore().getIndexManager().getLastCommitTime() + 1;

          // Resolve the index that the connection will write on.
          final IIndex ndx = getHistoryIndex(conn.getTripleStore());

          // The index should exist.
          assertNotNull(ndx);

          // The index should be empty.
          assertEquals(0L, ndx.rangeCount());

          final EmbergraphValueFactory f = (EmbergraphValueFactory) sail.getValueFactory();

          final EmbergraphURI A = f.createURI("http://www.embergraph.org/A");
          final EmbergraphURI B = f.createURI("http://www.embergraph.org/B");
          final EmbergraphURI C = f.createURI("http://www.embergraph.org/C");
          final EmbergraphURI rdfType = f.asValue(RDF.TYPE);

          conn.addStatement(A, rdfType, B);
          conn.addStatement(A, rdfType, C);

          conn.commit();

          // Should be 2 entries (more if inference is enabled).
          assertEquals(2L, ndx.rangeCount());

          stmt0 = new SPO(A.getIV(), rdfType.getIV(), B.getIV(), StatementEnum.Explicit);

          stmt1 = new SPO(A.getIV(), rdfType.getIV(), C.getIV(), StatementEnum.Explicit);

          @SuppressWarnings("unchecked")
          final Iterator<HistoryChangeRecord> itr =
              new Striterator(ndx.rangeIterator())
                  .addFilter(
                      new Resolver() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Object resolve(final Object obj) {
                          return ((ITuple<HistoryChangeRecord>) obj).getObject();
                        }
                      });

          final HistoryChangeRecord[] a =
              new HistoryChangeRecord[] {
                new HistoryChangeRecord(stmt0, ChangeAction.INSERTED, revisionTime0),
                new HistoryChangeRecord(stmt1, ChangeAction.INSERTED, revisionTime0),
              };

          // for (HistoryChangeRecord r : a) {
          // System.err.println("Expected: " + r);
          // System.out.println("Actual  : " + itr.next());
          // }

          // Verify the records in the index.
          assertSameIteratorAnyOrder(a, itr);

        } finally {

          conn.close();
        }
      }

      /*
       * Verify that we can remove a statements and it will appear in the
       * history index as a new entry. The statement that we do not remove
       * will have only one entry.
       */
      final long revisionTime1;
      {
        final EmbergraphSailConnection conn = sail.getConnection();

        try {

          // Expected revision time for the history index entries.
          revisionTime1 = conn.getTripleStore().getIndexManager().getLastCommitTime() + 1;

          // Resolve the index that the connection will write on.
          final IIndex ndx = getHistoryIndex(conn.getTripleStore());

          // The index should exist.
          assertNotNull(ndx);

          // The index should not be empty.
          assertEquals(2L, ndx.rangeCount());

          final EmbergraphValueFactory f = (EmbergraphValueFactory) sail.getValueFactory();

          final EmbergraphURI A = f.createURI("http://www.embergraph.org/A");
          final EmbergraphURI B = f.createURI("http://www.embergraph.org/B");
          //                    final EmbergraphURI C = f
          //                            .createURI("http://www.embergraph.org/C");
          final EmbergraphURI rdfType = f.asValue(RDF.TYPE);

          conn.removeStatements(A, rdfType, B);

          conn.commit();

          // Should be 1 entry since others were pruned out.
          assertEquals(1L, ndx.rangeCount());

          @SuppressWarnings("unchecked")
          final Iterator<HistoryChangeRecord> itr =
              new Striterator(ndx.rangeIterator())
                  .addFilter(
                      new Resolver() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected Object resolve(final Object obj) {
                          return ((ITuple<HistoryChangeRecord>) obj).getObject();
                        }
                      });

          /*
           * The older entries should have been pruned out.
           */
          final HistoryChangeRecord[] a =
              new HistoryChangeRecord[] {
                //                            new HistoryChangeRecord(stmt0,
                //                                    ChangeAction.INSERTED, revisionTime0),
                //                            new HistoryChangeRecord(stmt1,
                //                                    ChangeAction.INSERTED, revisionTime0),
                new HistoryChangeRecord(stmt0, ChangeAction.REMOVED, revisionTime1),
              };

          //                     for (HistoryChangeRecord r : a) {
          //                     System.err.println("Expected: " + r);
          //                     System.out.println("Actual  : " + itr.next());
          //                     }

          // Verify the records in the index.
          assertSameIteratorAnyOrder(a, itr);

        } finally {

          conn.close();
        }
      }

    } finally {

      sail.__tearDownUnitTest();
    }
  }
}
