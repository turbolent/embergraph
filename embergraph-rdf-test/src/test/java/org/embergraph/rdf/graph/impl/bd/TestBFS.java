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
package org.embergraph.rdf.graph.impl.bd;

import org.embergraph.rdf.graph.IGASContext;
import org.embergraph.rdf.graph.IGASEngine;
import org.embergraph.rdf.graph.IGASState;
import org.embergraph.rdf.graph.IGraphAccessor;
import org.embergraph.rdf.graph.TraversalDirectionEnum;
import org.embergraph.rdf.graph.analytics.BFS;
import org.openrdf.sail.SailConnection;

/**
 * Test class for Breadth First Search (BFS) traversal.
 *
 * @see BFS
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class TestBFS extends AbstractEmbergraphGraphTestCase {

  public TestBFS() {}

  public TestBFS(String name) {
    super(name);
  }

  public void testBFS() throws Exception {

    final SmallGraphProblem p = setupSmallGraphProblem();

    final IGASEngine gasEngine = getGraphFixture().newGASEngine(1 /* nthreads */);

    try {

      final IGraphAccessor graphAccessor = getGraphFixture().newGraphAccessor(null /* ignored */);

      final IGASContext<BFS.VS, BFS.ES, Void> gasContext =
          gasEngine.newGASContext(graphAccessor, new BFS());

      final IGASState<BFS.VS, BFS.ES, Void> gasState = gasContext.getGASState();

      // Initialize the froniter.
      gasState.setFrontier(gasContext, p.getMike());

      // Converge.
      gasContext.call();

      assertEquals(0, gasState.getState(p.getMike()).depth());
      assertEquals(null, gasState.getState(p.getMike()).predecessor());

      assertEquals(1, gasState.getState(p.getFoafPerson()).depth());
      assertEquals(p.getMike(), gasState.getState(p.getFoafPerson()).predecessor());

      assertEquals(1, gasState.getState(p.getBryan()).depth());
      assertEquals(p.getMike(), gasState.getState(p.getBryan()).predecessor());

      assertEquals(2, gasState.getState(p.getMartyn()).depth());
      assertEquals(p.getBryan(), gasState.getState(p.getMartyn()).predecessor());

    } finally {

      gasEngine.shutdownNow();
    }
  }

  /**
   * Variant test in which we choose a vertex (<code>foaf:person</code>) in the middle of the graph
   * and insist on forward directed edges. Since the edges point from the person to the <code>
   * foaf:person</code> vertex, this BSF traversal does not discover any connected vertices.
   */
  public void testBFS_directed_forward() throws Exception {

    final SmallGraphProblem p = setupSmallGraphProblem();

    final IGASEngine gasEngine = getGraphFixture().newGASEngine(1 /* nthreads */);

    try {

      final SailConnection cxn = getGraphFixture().getSail().getConnection();

      try {

        final IGraphAccessor graphAccessor = getGraphFixture().newGraphAccessor(cxn);

        final IGASContext<BFS.VS, BFS.ES, Void> gasContext =
            gasEngine.newGASContext(graphAccessor, new BFS());

        final IGASState<BFS.VS, BFS.ES, Void> gasState = gasContext.getGASState();

        // Initialize the froniter.
        gasState.setFrontier(gasContext, p.getFoafPerson());

        // directed traversal.
        gasContext.setTraversalDirection(TraversalDirectionEnum.Forward);

        // Converge.
        gasContext.call();

        // starting vertex at (0,null).
        assertEquals(0, gasState.getState(p.getFoafPerson()).depth());
        assertEquals(null, gasState.getState(p.getFoafPerson()).predecessor());

        // no other vertices are visited.
        assertEquals(-1, gasState.getState(p.getMike()).depth());
        assertEquals(null, gasState.getState(p.getMike()).predecessor());

        assertEquals(-1, gasState.getState(p.getBryan()).depth());
        assertEquals(null, gasState.getState(p.getBryan()).predecessor());

        assertEquals(-1, gasState.getState(p.getMartyn()).depth());
        assertEquals(null, gasState.getState(p.getMartyn()).predecessor());

      } finally {

        try {
          cxn.rollback();
        } finally {
          cxn.close();
        }
      }

    } finally {

      gasEngine.shutdownNow();
    }
  }

  /**
   * Variant test in which we choose a vertex (<code>foaf:person</code>) in the middle of the graph
   * and insist on reverse directed edges. Since the edges point from the person to the <code>
   * foaf:person</code> vertex, forward BSF traversal does not discover any connected vertices.
   * However, since the traversal direction is reversed, the vertices are all one hop away.
   */
  public void testBFS_directed_reverse() throws Exception {

    final SmallGraphProblem p = setupSmallGraphProblem();

    final IGASEngine gasEngine = getGraphFixture().newGASEngine(1 /* nthreads */);

    try {

      final SailConnection cxn = getGraphFixture().getSail().getConnection();

      try {

        final IGraphAccessor graphAccessor = getGraphFixture().newGraphAccessor(cxn);

        final IGASContext<BFS.VS, BFS.ES, Void> gasContext =
            gasEngine.newGASContext(graphAccessor, new BFS());

        final IGASState<BFS.VS, BFS.ES, Void> gasState = gasContext.getGASState();

        // Initialize the froniter.
        gasState.setFrontier(gasContext, p.getFoafPerson());

        // directed traversal.
        gasContext.setTraversalDirection(TraversalDirectionEnum.Reverse);

        // Converge.
        gasContext.call();

        // starting vertex at (0,null).
        assertEquals(0, gasState.getState(p.getFoafPerson()).depth());
        assertEquals(null, gasState.getState(p.getFoafPerson()).predecessor());

        // All other vertices are 1-hop.
        assertEquals(1, gasState.getState(p.getMike()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getMike()).predecessor());

        assertEquals(1, gasState.getState(p.getBryan()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getBryan()).predecessor());

        assertEquals(1, gasState.getState(p.getMartyn()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getMartyn()).predecessor());

      } finally {

        try {
          cxn.rollback();
        } finally {
          cxn.close();
        }
      }

    } finally {

      gasEngine.shutdownNow();
    }
  }

  /**
   * Variant test in which we choose a vertex (<code>foaf:person</code>) in the middle of the graph
   * and insist on directed edges. Since the edges point from the person to the <code>foaf:person
   * </code> vertex, this BSF traversal does not discover any connected vertices.
   */
  public void testBFS_undirected() throws Exception {

    final SmallGraphProblem p = setupSmallGraphProblem();

    final IGASEngine gasEngine = getGraphFixture().newGASEngine(1 /* nthreads */);

    try {

      final SailConnection cxn = getGraphFixture().getSail().getConnection();

      try {

        final IGraphAccessor graphAccessor = getGraphFixture().newGraphAccessor(cxn);

        final IGASContext<BFS.VS, BFS.ES, Void> gasContext =
            gasEngine.newGASContext(graphAccessor, new BFS());

        final IGASState<BFS.VS, BFS.ES, Void> gasState = gasContext.getGASState();

        // Initialize the froniter.
        gasState.setFrontier(gasContext, p.getFoafPerson());

        // undirected traversal.
        gasContext.setTraversalDirection(TraversalDirectionEnum.Undirected);

        // Converge.
        gasContext.call();

        // starting vertex at (0,null).
        assertEquals(0, gasState.getState(p.getFoafPerson()).depth());
        assertEquals(null, gasState.getState(p.getFoafPerson()).predecessor());

        // All other vertices are 1-hop.
        assertEquals(1, gasState.getState(p.getMike()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getMike()).predecessor());

        assertEquals(1, gasState.getState(p.getBryan()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getBryan()).predecessor());

        assertEquals(1, gasState.getState(p.getMartyn()).depth());
        assertEquals(p.getFoafPerson(), gasState.getState(p.getMartyn()).predecessor());

      } finally {

        try {
          cxn.rollback();
        } finally {
          cxn.close();
        }
      }

    } finally {

      gasEngine.shutdownNow();
    }
  }
}
