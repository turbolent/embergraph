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

package org.embergraph.rdf.sail;

import org.apache.log4j.Logger;
import org.embergraph.journal.ITx;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sail.sparql.Embergraph2ASTSPARQLParser;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.QueryType;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailQuery;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;

/*
 * Class with support for a variety of embergraph specific extensions,
 *
 * <p>{@inheritDoc}
 *
 * @author mrpersonick
 */
public class EmbergraphSailRepositoryConnection extends SailRepositoryConnection {

  private static final transient Logger log =
      Logger.getLogger(EmbergraphSailRepositoryConnection.class);

  @Override
  public String toString() {
    return getClass().getName()
        + "{timestamp="
        + TimestampUtility.toString(getTripleStore().getTimestamp())
        + "}";
  }

  public EmbergraphSailRepositoryConnection(
      EmbergraphSailRepository repository, SailConnection sailConnection) {

    super(repository, sailConnection);
  }

  @Override
  public EmbergraphSailRepository getRepository() {

    return (EmbergraphSailRepository) super.getRepository();
  }

  @Override
  public EmbergraphSailConnection getSailConnection() {

    return (EmbergraphSailConnection) super.getSailConnection();
  }

  @Override
  public EmbergraphValueFactory getValueFactory() {

    return (EmbergraphValueFactory) super.getValueFactory();
  }

  /*
   * {@inheritDoc}
   *
   * <p>This code path is optimized for cases where isolatable indices are not in use and where
   * inferences can not appear.
   *
   * @see <a href="http://trac.bigdata.com/ticket/1178" > Optimize hasStatement() </a>
   *     <p>TODO This should be using {@link EmbergraphSailConnection#hasStatement(Resource, URI,
   *     Value, boolean, Resource...)} for improved performance. See #1178.
   */
  @Override
  public boolean hasStatement(
      final Resource s,
      final URI p,
      final Value o,
      final boolean includeInferred,
      final Resource... contexts)
      throws RepositoryException {
    return super.hasStatement(s, p, o, includeInferred, contexts);
    //      try {
    //
    //         return getSailConnection().hasStatement(s, p, o, includeInferred, contexts);
    //
    //      } catch (SailException e) {
    //
    //         throw new RepositoryException(e);
    //
    //      }

  }

  @Override
  public EmbergraphSailGraphQuery prepareGraphQuery(
      final QueryLanguage ql, final String qs, final String baseURI)
      throws MalformedQueryException {

    return (EmbergraphSailGraphQuery) prepareQuery(ql, qs, baseURI);
  }

  @Override
  public EmbergraphSailTupleQuery prepareTupleQuery(
      final QueryLanguage ql, final String qs, final String baseURI)
      throws MalformedQueryException {

    return (EmbergraphSailTupleQuery) prepareQuery(ql, qs, baseURI);
  }

  @Override
  public EmbergraphSailBooleanQuery prepareBooleanQuery(
      final QueryLanguage ql, final String qs, final String baseURI)
      throws MalformedQueryException {

    return (EmbergraphSailBooleanQuery) prepareQuery(ql, qs, baseURI);
  }

  @Override
  public SailQuery prepareQuery(final QueryLanguage ql, final String qs, final String baseURI)
      throws MalformedQueryException {

    if (ql == QueryLanguage.SPARQL) {

      return (SailQuery) prepareNativeSPARQLQuery(ql, qs, baseURI);
    }

    throw new MalformedQueryException("Unsupported language: " + ql);
  }

  /*
   * {@inheritDoc}
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/448">SPARQL 1.1 Update </a>
   */
  @Override
  public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
      throws MalformedQueryException {

    if (ql == QueryLanguage.SPARQL) {

      return prepareNativeSPARQLUpdate(ql, update, baseURI);
    }

    throw new MalformedQueryException("Unsupported language: " + ql);
  }

  /*
   * Parse a SPARQL query
   *
   * @param ql The {@link QueryLanguage}.
   * @param queryStr The query.
   * @param baseURI The base URI.
   * @return An object wrapping the embergraph AST model for that query.
   * @throws MalformedQueryException
   * @throws UnsupportedOperationException if the query language is not SPARQL.
   */
  public EmbergraphSailQuery prepareNativeSPARQLQuery(
      final QueryLanguage ql, final String queryStr, final String baseURI)
      throws MalformedQueryException {

    if (ql != QueryLanguage.SPARQL) throw new UnsupportedOperationException(ql.toString());

    if (log.isDebugEnabled()) {
      log.debug(queryStr);
    }

    // Flush buffers since we are about to resolve Values to IVs.
    getSailConnection().flushStatementBuffers(true /* assertions */, true /* retractions */);

    final ASTContainer astContainer =
        new Embergraph2ASTSPARQLParser().parseQuery2(queryStr, baseURI);

    final QueryType queryType = astContainer.getOriginalAST().getQueryType();

    switch (queryType) {
      case SELECT:
        return new EmbergraphSailTupleQuery(astContainer, this);
      case DESCRIBE:
      case CONSTRUCT:
        return new EmbergraphSailGraphQuery(astContainer, this);
      case ASK:
        {
          return new EmbergraphSailBooleanQuery(astContainer, this);
        }
      default:
        throw new RuntimeException("Unknown query type: " + queryType);
    }
  }

  /*
   * Parse a SPARQL UPDATE request.
   *
   * @param ql The {@link QueryLanguage}.
   * @param updateStr The update request.
   * @param baseURI The base URI.
   * @return An object wrapping the embergraph AST model for that request.
   * @throws MalformedQueryException
   * @throws UnsupportedOperationException if the {@link QueryLanguage} is not SPARQL.
   */
  public EmbergraphSailUpdate prepareNativeSPARQLUpdate(
      final QueryLanguage ql, final String updateStr, final String baseURI)
      throws MalformedQueryException {

    if (getTripleStore().isReadOnly()) throw new UnsupportedOperationException("Read only");

    if (ql != QueryLanguage.SPARQL) throw new UnsupportedOperationException(ql.toString());

    if (log.isDebugEnabled()) {
      log.debug(updateStr);
    }

    // Flush buffers since we are about to resolve Values to IVs.
    getSailConnection().flushStatementBuffers(true /* assertions */, true /* retractions */);

    // Parse the UPDATE request.
    final ASTContainer astContainer =
        new Embergraph2ASTSPARQLParser().parseUpdate2(updateStr, baseURI);

    return new EmbergraphSailUpdate(astContainer, this);
  }

  /** Return <code>true</code> if the connection does not permit mutation. */
  public final boolean isReadOnly() {

    return getSailConnection().isReadOnly();
  }

  /** Return <code>true</code> if this is the {@link ITx#UNISOLATED} connection. */
  public final boolean isUnisolated() {

    return getSailConnection().isUnisolated();
  }

  /*
   * Commit, returning the timestamp associated with the new commit point.
   *
   * <p>Note: auto-commit is an EXTREMELY bad idea. Performance will be terrible. The database will
   * swell to an outrageous size. TURN OFF AUTO COMMIT.
   *
   * @return The timestamp associated with the new commit point. This will be <code>0L</code> if the
   *     write set was empty such that nothing was committed.
   * @see EmbergraphSail.Options#ALLOW_AUTO_COMMIT
   */
  public long commit2() throws RepositoryException {

    // auto-commit is heinously inefficient
    if (isAutoCommit() && !getSailConnection().getAllowAutoCommit()) {

      throw new RepositoryException("please set autoCommit to false");
    }

    // Note: Just invokes sailConnection.commit()
    //        super.commit();
    try {
      //            sailConnection.commit();
      return getSailConnection().commit2();
    } catch (SailException e) {
      throw new RepositoryException(e);
    }
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: auto-commit is an EXTREMELY bad idea. Performance will be terrible. The database will
   * swell to an outrageous size. TURN OFF AUTO COMMIT.
   *
   * @see EmbergraphSail.Options#ALLOW_AUTO_COMMIT
   */
  @Override
  public void commit() throws RepositoryException {

    commit2();
  }

  /*
   * Flush the statement buffers. The {@link EmbergraphSailConnection} heavily buffers assertions
   * and retractions. Either a {@link #flush()} or a {@link #commit()} is required before executing
   * any operations directly against the backing {@link AbstractTripleStore} so that the buffered
   * assertions or retractions will be written onto the KB and become visible to other methods. This
   * is not a transaction issue -- just a buffer issue. The public methods on the {@link
   * EmbergraphSailConnection} all flush the buffers before performing any queries against the
   * underlying {@link AbstractTripleStore}.
   *
   * @throws RepositoryException
   */
  public void flush() throws RepositoryException {

    try {

      getSailConnection().flush();

    } catch (Exception ex) {

      throw new RepositoryException(ex);
    }
  }

  /*
   * Return the backing {@link AbstractTripleStore} object. Caution MUST be used when accessing this
   * object as the access goes around the SAIL API.
   */
  public AbstractTripleStore getTripleStore() {

    return getSailConnection().getTripleStore();
  }

  /*
   * Computes the closure of the triple store for RDF(S)+ entailments.
   *
   * <p>This computes the closure of the database. This can be used if you do NOT enable truth
   * maintenance and choose instead to load up all of your data first and then compute the closure
   * of the database. Note that some rules may be computed by eager closure while others are
   * computed at query time.
   *
   * <p>Note: If there are already entailments in the database AND you have retracted statements
   * since the last time the closure was computed then you MUST delete all entailments from the
   * database before re-computing the closure.
   *
   * <p>Note: This method does NOT commit the database. See {@link AbstractTripleStore#commit()} and
   * {@link #getTripleStore()}.
   *
   * @see #removeAllEntailments()
   */
  public void computeClosure() throws RepositoryException {

    try {

      getSailConnection().computeClosure();

    } catch (Exception ex) {

      throw new RepositoryException(ex);
    }
  }

  /*
   * Removes all "inferred" statements from the database (does NOT commit the database).
   *
   * @throws RepositoryException
   */
  public void removeAllEntailments() throws RepositoryException {

    try {

      getSailConnection().removeAllEntailments();

    } catch (Exception ex) {

      throw new RepositoryException(ex);
    }
  }

  /*
   * Set the change log on the SAIL connection. See {@link IChangeLog} and {@link IChangeRecord}.
   *
   * @param log the change log
   */
  public void addChangeLog(final IChangeLog log) {

    getSailConnection().addChangeLog(log);
  }

  /*
   * Remove a change log from the SAIL connection. See {@link IChangeLog} and {@link IChangeRecord}.
   *
   * @param log the change log
   */
  public void removeChangeLog(final IChangeLog log) {

    getSailConnection().removeChangeLog(log);
  }
}
