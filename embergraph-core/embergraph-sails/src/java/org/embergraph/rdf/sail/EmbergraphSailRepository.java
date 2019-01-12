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

import java.io.IOException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.SailException;

/**
 * Embergraph specific {@link SailRepository} implementation class.
 *
 * @see EmbergraphSailRepositoryConnection
 */
public class EmbergraphSailRepository extends SailRepository {

  public EmbergraphSailRepository(final EmbergraphSail sail) {

    super(sail);
  }

  //    /* Gone since BLZG-2041: This was accessing the AbstractTripleStore without a Connection.
  //
  //     * @see BLZG-2041 EmbergraphSail should not locate the AbstractTripleStore
  //     * until a connection is requested
  //     */
  //    @Deprecated // This is accessing the AbstractTripleStore without a Connection.
  //    public AbstractTripleStore getDatabase() {
  //
  //        return ((EmbergraphSail) getSail()).getDatabase();
  //
  //    }

  @Override
  public EmbergraphSail getSail() {

    return (EmbergraphSail) super.getSail();
  }

  /**
   * {@inheritDoc}
   *
   * <p>The correct pattern for obtaining an updatable connection, doing work with that connection,
   * and committing or rolling back that update is as follows.
   *
   * <pre>
   *
   * EmbergraphSailConnection conn = null;
   * boolean ok = false;
   * try {
   *     conn = repo.getConnection();
   *     doWork(conn);
   *     conn.commit();
   *     ok = true;
   * } finally {
   *     if (conn != null) {
   *         if (!ok) {
   *             conn.rollback();
   *         }
   *         conn.close();
   *     }
   * }
   * </pre>
   *
   * @see EmbergraphSail#getConnection()
   * @see #getUnisolatedConnection()
   */
  @Override
  public EmbergraphSailRepositoryConnection getConnection() throws RepositoryException {

    try {

      return new EmbergraphSailRepositoryConnection(this, getSail().getConnection());

    } catch (SailException e) {

      throw new RepositoryException(e);
    }
  }

  /**
   * Obtain a read-only connection to the database at the last commit point. A read-only connection
   * should be used for all pure-readers, as the connection will not be blocked by concurrent
   * writers.
   *
   * @return a read-only connection to the database
   * @see EmbergraphSail#getReadOnlyConnection()
   */
  public EmbergraphSailRepositoryConnection getReadOnlyConnection() throws RepositoryException {

    return new EmbergraphSailRepositoryConnection(this, getSail().getReadOnlyConnection());
  }

  /**
   * Obtain a read-only connection to the database from a historical commit point. A read-only
   * connection should be used for all pure-readers, as the connection will not be blocked by
   * concurrent writers.
   *
   * @return a read-only connection to the database
   * @see EmbergraphSail#getReadOnlyConnection(long)
   */
  public EmbergraphSailRepositoryConnection getReadOnlyConnection(final long timestamp)
      throws RepositoryException {

    return new EmbergraphSailRepositoryConnection(this, getSail().getReadOnlyConnection(timestamp));
  }

  /**
   * Return a connection backed by a read-write transaction.
   *
   * @throws InterruptedException
   * @see EmbergraphSail#getReadWriteConnection()
   */
  public EmbergraphSailRepositoryConnection getReadWriteConnection()
      throws RepositoryException, InterruptedException {

    try {

      return new EmbergraphSailRepositoryConnection(this, getSail().getReadWriteConnection());

    } catch (IOException e) {

      throw new RepositoryException(e);
    }
  }

  /**
   * Return an unisolated connection to the database. Only one of these allowed at a time.
   *
   * <p>The correct pattern for obtaining an updatable connection, doing work with that connection,
   * and committing or rolling back that update is as follows.
   *
   * <pre>
   *
   * EmbergraphSailConnection conn = null;
   * boolean ok = false;
   * try {
   *     conn = repo.getConnection();
   *     doWork(conn);
   *     conn.commit();
   *     ok = true;
   * } finally {
   *     if (conn != null) {
   *         if (!ok) {
   *             conn.rollback();
   *         }
   *         conn.close();
   *     }
   * }
   * </pre>
   *
   * @return unisolated connection to the database
   * @see EmbergraphSail#getUnisolatedConnection()
   * @see #getConnection()
   */
  public EmbergraphSailRepositoryConnection getUnisolatedConnection() throws RepositoryException {

    try {

      return new EmbergraphSailRepositoryConnection(this, getSail().getUnisolatedConnection());

    } catch (InterruptedException e) {

      throw new RepositoryException(e);
    }
  }
}
