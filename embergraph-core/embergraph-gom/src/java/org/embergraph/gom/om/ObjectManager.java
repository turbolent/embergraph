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
 * Created on Mar 19, 2012
 */
package org.embergraph.gom.om;

import cutthecrap.utils.striterators.ICloseableIterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.gom.gpo.GPO;
import org.embergraph.gom.gpo.IGPO;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphResource;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sail.EmbergraphSailRepository;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.Sesame2EmbergraphIterator;
import org.embergraph.rdf.sparql.ast.cache.CacheConnectionFactory;
import org.embergraph.rdf.sparql.ast.cache.ICacheConnection;
import org.embergraph.rdf.sparql.ast.cache.IDescribeCache;
import org.embergraph.striterator.CloseableIteratorWrapper;
import org.openrdf.model.Graph;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;

/*
 * An {@link IObjectManager} for use with an embedded database, including JSP pages running in the
 * same webapp as the NanoSparqlServer and applications that do not expose a public web interface.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class ObjectManager extends ObjectMgrModel {

  private static final Logger log = Logger.getLogger(ObjectManager.class);

  private final EmbergraphSailRepository m_repo;
  private final boolean readOnly;
  private final IDescribeCache m_describeCache;

  /*
   * @param endpoint A SPARQL endpoint that may be used to communicate with the database.
   * @param cxn A connection to the database.
   * @throws RepositoryException
   */
  public ObjectManager(final String endpoint, final EmbergraphSailRepository cxn)
      throws RepositoryException {

    super(endpoint, (EmbergraphValueFactory) cxn.getValueFactory());

    m_repo = cxn;

    //        final AbstractTripleStore tripleStore = cxn.getDatabase();

    this.readOnly = !cxn.isWritable();

    /*
     * FIXME The DESCRIBE cache feature is not yet finished. This code will
     * not obtain a connection to the DESCRIBE cache unless an unisolated
     * query or update operation has already run against the query engine.
     * This is a known bug and will be resolved as we work through the MVCC
     * cache coherence for the DESCRIBE cache.
     */
    {
      final QueryEngine queryEngine =
          QueryEngineFactory.getInstance()
              .getStandaloneQueryController((Journal) m_repo.getSail().getIndexManager());

      final ICacheConnection cacheConn =
          CacheConnectionFactory.getExistingCacheConnection(queryEngine);

      if (cacheConn != null) {

        // FIXME The sail is no longer associated with a timestamp. See BLZG-2041.
        m_describeCache = null;
        //                m_describeCache = cacheConn.getDescribeCache(
        //                        cxn.getSail().getNamespace(), cxn.getSail().getTimestamp());

      } else {

        m_describeCache = null;
      }
    }

    /*
     * Note: This MUST NOT be done by default. It breaks the ACID contract
     * since any incremental write will be combined with any other writes
     * because this class does not (and MUST NOT) hold the UNISOLATED
     * connection across its life cycle.
     */
    //        /*
    //         * Local ObjectManager can flush incrementally from the dirty list
    //         *
    //         * A maximum size of 4000 dirty objects is a sensible default.
    //         */
    //        m_maxDirtyListSize = 4000;
  }

  /*
   * This may be used to break ACID and perform incremental eviction of dirty objects to the backing
   * store. However, the use of this method is NOT recommended as the updates will become durable
   * incrementally rather than atomically.
   *
   * @param newValue The new maximum dirty list size (default is {@link Integer#MAX_VALUE}).
   */
  public void setMaxDataListSize(final int newValue) {

    if (newValue <= 0) throw new IllegalArgumentException();

    this.m_maxDirtyListSize = newValue;
  }

  /** @return direct repository connection */
  public EmbergraphSailRepository getRepository() {
    return m_repo;
  }

  @Override
  public void close() {
    super.close();
    try {
      if (m_repo.getSail().isOpen()) m_repo.shutDown();
    } catch (RepositoryException e) {
      // Per the API.
      throw new IllegalStateException(e);
    }
  }

  @Override
  public ICloseableIterator<BindingSet> evaluate(final String query) {

    final EmbergraphSailRepositoryConnection cxn;
    try {
      cxn = getQueryConnection();
    } catch (RepositoryException e1) {
      throw new RuntimeException(e1);
    }

    try {

      // Setup the query.
      final TupleQuery q = cxn.prepareTupleQuery(QueryLanguage.SPARQL, query);

      // Note: evaluate() runs asynchronously and must be closed().
      final TupleQueryResult res = q.evaluate();

      // Will close the TupleQueryResult.
      return new Sesame2EmbergraphIterator<BindingSet, QueryEvaluationException>(res) {
        public void close() {
          // Close the TupleQueryResult.
          super.close();
          try {
            // Close the connection.
            cxn.close();
          } catch (RepositoryException e) {
            throw new RuntimeException(e);
          }
        }
      };

    } catch (Throwable t) {

      // Error preparing the query.
      try {
        // Close the connection
        cxn.close();
      } catch (RepositoryException e) {
        log.error(e, e);
      }

      throw new RuntimeException("query=" + query, t);
    }
  }

  public ICloseableIterator<Statement> evaluateGraph(final String query) {

    final EmbergraphSailRepositoryConnection cxn;
    try {
      cxn = getQueryConnection();
    } catch (RepositoryException e1) {
      throw new RuntimeException(e1);
    }

    try {

      // Setup the query.
      final GraphQuery q = cxn.prepareGraphQuery(QueryLanguage.SPARQL, query);

      // Note: evaluate() runs asynchronously and must be closed().
      final GraphQueryResult res = q.evaluate();

      // Will close the TupleQueryResult.
      return new Sesame2EmbergraphIterator<Statement, QueryEvaluationException>(res) {
        public void close() {
          // Close the TupleQueryResult.
          super.close();
          try {
            // Close the connection.
            cxn.close();
          } catch (RepositoryException e) {
            throw new RuntimeException(e);
          }
        }
      };

    } catch (Throwable t) {

      // Error preparing the query.
      try {
        // Close the connection
        cxn.close();
      } catch (RepositoryException e) {
        log.error(e, e);
      }

      throw new RuntimeException("query=" + query, t);
    }
  }

  @Override
  public void execute(String updateStr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPersistent() {
    return true;
  }

  @Override
  protected void materializeWithDescribe(final IGPO gpo) {

    if (gpo == null) throw new IllegalArgumentException();

    /*
     * At present the DESCRIBE query will simply return a set of statements
     * equivalent to a TupleQuery <id, ?, ?>.
     */

    if (m_describeCache != null) {

      final IV<?, ?> iv = addResolveIV(gpo);

      final Graph g = m_describeCache.lookup(iv);

      if (g != null) {

        initGPO((GPO) gpo, new CloseableIteratorWrapper<>(g.iterator()));

        return;
      }
    }

    super.materializeWithDescribe(gpo);
  }

  /*
   * Attempt to add/resolve the {@link IV} for the {@link IGPO}.
   *
   * @param gpo The {@link IGPO}.
   * @return The {@link IV} -or- <code>null</code> iff this is a read-only connection and the {@link
   *     EmbergraphResource} associated with that {@link IGPO} is not in the lexicon.
   *     <p>FIXME This code path is horribly inefficient. It is create a new connection for each
   *     such resolution (since BLZG-2041) and handling resolution on an object by object basis. It
   *     is also not making a clear distinction between a read-only and up-to-the moment read/write
   *     connection.
   */
  private IV<?, ?> addResolveIV(final IGPO gpo) {

    final EmbergraphResource id = gpo.getId();

    IV<?, ?> iv = id.getIV();

    if (iv == null) {

      /*
       * Attempt to resolve the IV. If the connection allows updates then
       * this will cause an IV to be assigned if the Resource was not
       * already in the lexicon.
       */

      final EmbergraphValue[] values = new EmbergraphValue[] {id};

      EmbergraphSailRepositoryConnection conn = null;

      try {

        conn = getQueryConnection();

        conn.getTripleStore().getLexiconRelation().addTerms(values, values.length, readOnly);

        // Note: MAY still be null!
        iv = id.getIV();

      } catch (RepositoryException ex) {

        throw new RuntimeException(ex);

      } finally {

        if (conn != null) {
          try {
            conn.close();
          } catch (RepositoryException ex2) {
            log.warn(ex2, ex2);
          }
        }
      }
    }

    // May be null.
    return iv;
  }

  @Override
  protected void flushStatements(final List<Statement> m_inserts, final List<Statement> m_removes) {

    EmbergraphSailRepositoryConnection cxn = null;
    try {

      // Connection supporting updates.
      cxn = getConnection();

      // handle batch removes
      for (Statement stmt : m_removes) {

        cxn.remove(stmt);
      }

      // handle batch inserts
      for (Statement stmt : m_inserts) {

        cxn.add(stmt);
      }

      // Atomic commit.
      cxn.commit();

    } catch (Throwable t) {

      if (cxn != null) {
        try {
          cxn.rollback();
        } catch (RepositoryException e) {
          log.error(e, e);
        }
      }

    } finally {

      if (cxn != null) {
        try {
          cxn.close();
        } catch (RepositoryException e) {
          log.error(e, e);
        }
      }
    }
  }

  /*
   * Return an updatable connection.
   *
   * @throws RepositoryException
   */
  private EmbergraphSailRepositoryConnection getConnection() throws RepositoryException {

    final EmbergraphSailRepositoryConnection c = m_repo.getConnection();

    c.setAutoCommit(false);

    return c;
  }

  /*
   * Return a read-only connection.
   *
   * @throws RepositoryException
   */
  private EmbergraphSailRepositoryConnection getQueryConnection() throws RepositoryException {

    final EmbergraphSailRepositoryConnection c = m_repo.getReadOnlyConnection();

    return c;
  }
}
