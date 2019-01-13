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
 * Created on Sep 4, 2011
 */

package org.embergraph.rdf.sparql.ast.eval.rto;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.PipelineOp;
import org.embergraph.bop.engine.IRunningQuery;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.engine.QueryEngine.IRunningQueryListener;
import org.embergraph.bop.fed.QueryEngineFactory;
import org.embergraph.bop.joinGraph.rto.JoinGraph;
import org.embergraph.bop.joinGraph.rto.Path;
import org.embergraph.journal.IBTreeManager;
import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.embergraph.rdf.sparql.ast.QueryHints;
import org.embergraph.rdf.sparql.ast.eval.AbstractDataDrivenSPARQLTestCase;

/*
 * Data driven test suite for the Runtime Query Optimizer (RTO).
 *
 * <p>Note: We reduce the stochastic behavior of the algorithm by using non-random sampling
 * techniques. However, the main correctness issues for the RTO are the handling of different kinds
 * of join groups, not the specific join orderings that it produces. The join orderings depend on
 * how the cutoff joins are sampled.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: TestBasicQuery.java 6440 2012-08-14 17:57:33Z thompsonbry $
 * @see JGraph
 * @see JoinGraph
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/64">Runtime Query Optimization</a>
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/258">Integrate RTO into SAIL</a>
 *     <p>TODO See the stubbed out test suite for the RTO for some examples of join groups that it
 *     should be handling.
 *     <p>TODO The RTO should be extended (together with test coverage) to handle more interesting
 *     kinds of join groups (optionals, sub-selects, property paths, SERVICE calls, etc).
 *     <p>Note: When handling sub-groups, etc., the RTO needs to flow solutions into the sub-query.
 *     <p>TODO Test case to verify that we can reorder inside of a MINUS (we already have a test for
 *     inside of UNION and OPTIONAL).
 *     <p>TODO Test case to verify that we do not reorder inside of a SERVICE call. Currently it
 *     won't since it is invoked from within AST2BOpUtility#convertJoinGroup(), but this would be an
 *     issue if the RTO was turned into an {@link IASTOptimizer}.
 *     <p>TODO Test case to verify that exogenous bindings are visible to the RTO. Specifically,
 *     make sure that the exogenous bindings are applied when the RTO does bottom-up evaluation to
 *     order the join group. It is Ok if we just handle the case with a single exogenous solution
 *     for now since we do not systematically optimize the case for multiple exogenous solutions
 *     yet.
 *     <p>TODO When adding an {@link IASTOptimizer} for the RTO, modify this class to test for the
 *     inclusion of the JoinGraphNode for the RTO.
 *     <p>TODO Automate the larger data scale tests on these data sets as part of CI and provide
 *     automated reporting over time on those performance runs. Once this is done, there will be no
 *     more reason to keep the older TestJoinGraphOnLubm and related tests (these are in <a
 *     href="https://www.dropbox.com/sh/d3emvoj9oh8kblf/JXcEP4Ddtq">dropbox </a>).
 *     <p>TODO Add some govtrack queries. Those queries use quads mode and have a lot of interesting
 *     query constructions.
 *     <p>TODO TESTS: Provide test coverage for running queries with complex FILTERs (triples mode
 *     is Ok).
 *     <p>TODO TESTS: Test with FILTERs that can not run until after all joins. Such filters are
 *     only attached when the [pathIsComplete] flag is set. This might only occur when we have
 *     FILTERs that depend on variables that are only "maybe" bound by an OPTIONAL join.
 *     <p>TODO TESTS: Quads mode tests. We need to look in depth at how the quads mode access paths
 *     are evaluated. There are several different conditions. We need to look at each condition and
 *     at whether and how it can be made compatible with cutoff evaluation. (This is somewhat
 *     similar to the old scan+filter versus nested query debate on quads mode joins.)
 *     <p>TODO TESTS: Scale-out tests. For scale-out, we need to either mark the join's evaluation
 *     context based on whether or not the access path is local or remote (and whether the index is
 *     key-range distributed or hash partitioned).
 */
public class AbstractRTOTestCase extends AbstractDataDrivenSPARQLTestCase {

  protected static final Logger log = Logger.getLogger(AbstractRTOTestCase.class);

  /** */
  public AbstractRTOTestCase() {}

  /** @param name */
  public AbstractRTOTestCase(String name) {
    super(name);
  }

  /*
   * Helper class supports inspection of the terminated {@link IRunningQuery}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  protected static class MyQueryListener implements IRunningQueryListener {

    private final UUID queryId;
    private final Set<IRunningQuery> queries = new LinkedHashSet<>();

    public MyQueryListener(final UUID queryId) {

      if (queryId == null) throw new IllegalArgumentException();

      this.queryId = queryId;
    }

    @Override
    public void notify(final IRunningQuery q) {

      //            if(q.getQueryId().equals(queryId)) {

      queries.add(q);

      //            }

    }

    /** Return each {@link IRunningQuery} that was noticed by this listener. */
    public Set<IRunningQuery> getRunningQueries() {

      if (queries.isEmpty()) fail("Not found.");

      return queries;
    }
  }

  /*
   * Helper to run the test and examine the RTO determined solution.
   *
   * @param expected The expected join ordering.
   * @param helper
   */
  protected void assertSameJoinOrder(final int[] expected, final TestHelper helper)
      throws Exception {

    assertSameJoinOrder(Collections.singletonList(expected), helper);
  }

  /*
   * Helper to run the test and examine the RTO determined solution.
   *
   * @param expectedOrders The expected join ordering(s)
   * @param helper
   */
  protected void assertSameJoinOrder(final List<int[]> expectedOrders, final TestHelper helper)
      throws Exception {

    /*
     * Assign a UUID to this query so we can get at its outcome.
     */
    final UUID queryId = UUID.randomUUID();

    helper.getASTContainer().setQueryHint(QueryHints.QUERYID, queryId.toString());

    final QueryEngine queryEngine =
        QueryEngineFactory.getInstance()
            .getExistingQueryController((IBTreeManager) helper.getTripleStore().getIndexManager());

    // Hook up our listener and run the test.
    final ASTContainer astContainer;
    final MyQueryListener l = new MyQueryListener(queryId);
    try {
      // Register the listener.
      queryEngine.addListener(l);
      // Run the test.
      astContainer = helper.runTest();
    } finally {
      // Unregister the listener.
      queryEngine.removeListener(l);
    }

    //        final QueryRoot optimizedAST = astContainer.getOptimizedAST();

    final PipelineOp queryPlan = astContainer.getQueryPlan();

    /*
     * Note: Some queries may have more than one JoinGraph instance. They
     * will throw an exception here. You can (a) turn off all but one of the
     * places where the RTO is running; (b) modify the test harness to be
     * more general and verify each of the RTO instances that actually ran;
     * or (c) move that query into a part of the test suite that is only
     * concerned with getting the right answer and not verifying that the
     * join ordering remains consistent in CI runs.
     */
    final JoinGraph joinGraph = BOpUtility.getOnly(queryPlan, JoinGraph.class);

    assertNotNull(joinGraph);

    /*
     * The join path selected by the RTO.
     *
     * Note: The RTO might be running inside of a named subquery. If so,
     * then the Path is not attached to the main query. This is why we have
     * to check each query that was noticed by our listener.
     */
    final Path path;
    {
      Path tmp = null;
      for (IRunningQuery q : l.getRunningQueries()) {
        tmp = joinGraph.getPath(q);
        if (tmp != null) break;
      }
      path = tmp;
    }

    // Verify that a path was attached to the query.
    assertNotNull(path);

    if (log.isInfoEnabled()) log.info("path=" + path);

    for (int[] expected : expectedOrders) {
      if (Arrays.equals(expected, path.getVertexIds())) {
        return;
      }
    }

    {
      final StringBuilder sb = new StringBuilder();
      sb.append("RTO JOIN ORDER");
      sb.append("expectedOrders=(");
      boolean first = true;
      for (int[] expected : expectedOrders) {
        if (!first) {
          sb.append(", ");
          first = false;
        }
        sb.append(" ").append(Arrays.toString(expected));
      }
      sb.append(", actual=").append(Arrays.toString(path.getVertexIds()));
      // joinGraph.getQueryPlan(q)
      fail(sb.toString());
    }
  }
}
