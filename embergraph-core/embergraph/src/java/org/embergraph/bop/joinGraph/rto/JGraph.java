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
 * Created on Feb 22, 2011
 */

package org.embergraph.bop.joinGraph.rto;

import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.ap.SampleIndex.SampleType;
import org.embergraph.bop.engine.QueryEngine;
import org.embergraph.bop.joinGraph.NoSolutionsException;
import org.embergraph.bop.joinGraph.PartitionedJoinGroup;
import org.embergraph.bop.rdf.join.DataSetJoin;
import org.embergraph.rdf.sparql.ast.eval.AST2BOpRTO;
import org.embergraph.util.concurrent.ExecutionExceptions;

/*
* A runtime optimizer for a join graph. The {@link JoinGraph} bears some similarity to ROX (Runtime
 * Optimizer for XQuery), but has several significant differences:
 *
 * <ol>
 *   <li>1. ROX starts from the minimum cardinality edge of the minimum cardinality vertex. The
 *       {@link JoinGraph} starts with one or more low cardinality vertices.
 *   <li>2. ROX always extends the last vertex added to a given join path. The {@link JoinGraph}
 *       extends all vertices having unexplored edges in each breadth first expansion.
 *   <li>3. ROX is designed to interleave operator-at-once evaluation of join path segments which
 *       dominate other join path segments. The {@link JoinGraph} is designed to prune all join
 *       paths which are known to be dominated by other join paths for the same set of vertices in
 *       each round and iterates until a join path is identified which uses all vertices and has the
 *       minimum expected cumulative estimated cardinality. Join paths which survive pruning are
 *       re-sampled as necessary in order to obtain better information about edges in join paths
 *       which have a low estimated cardinality in order to address a problem with underflow of the
 *       cardinality estimates.
 * </ol>
 *
 * TODO For join graphs with a large number of vertices we may need to constrain the #of vertices
 * which are explored in parallel. This could be done by only branching the N lowest cardinality
 * vertices from the already connected edges. Since fewer vertices are being explored in parallel,
 * paths are more likely to converge onto the same set of vertices at which point we can prune the
 * dominated paths.
 *
 * <p>TODO Compare the cumulative expected cardinality of a join path with the tuples read on a join
 * path. The latter allows us to also explore alternative join strategies, such as the parallel
 * subquery versus scan and filter decision for named graph and default graph SPARQL queries.
 *
 * <p>TODO Coalescing duplicate access paths can dramatically reduce the work performed by a
 * pipelined nested index subquery. (A hash join eliminates all duplicate access paths using a scan
 * and filter approach.) If we will run a pipeline nested index subquery join, then should the
 * runtime query optimizer prefer paths with duplicate access paths?
 *
 * @todo Examine the overhead of the runtime optimizer. Look at ways to prune its costs. For
 *     example, by pruning the search, by recognizing when the query is simple enough to execute
 *     directly, by recognizing when we have already materialized the answer to the query, etc.
 * @todo Cumulative estimated cardinality is an estimate of the work to be done. However, the actual
 *     cost of a join depends on whether we will use nested index subquery or a hash join and the
 *     cost of that operation on the database. There could be counter examples where the cost of the
 *     hash join with a range scan using the unbound variable is LT the nested index subquery. For
 *     those cases, we will do the same amount of IO on the hash join but there will still be a
 *     lower cardinality to the join path since we are feeding in fewer solutions to be joined.
 * @todo Look at the integration with the SAIL. We need decorate to joins with some annotations.
 *     Those will have to be correctly propagated to the "edges" in order for edge sampling and
 *     incremental evaluation (or final evaluation) to work. The {@link DataSetJoin} essentially
 *     inlines one of its access paths. That should really be changed into an inline access path and
 *     a normal join operator so we can defer some of the details concerning the join operator
 *     annotations until we decide on the join path to be executed. An inline AP really implies an
 *     inline relation, which in turn implies that the query is a searchable context for query-local
 *     resources.
 *     <p>For s/o, when the AP is remote, the join evaluation context must be ANY and otherwise (for
 *     s/o) it must be SHARDED.
 *     <p>Since the join graph is fed the vertices (APs), it does not have access to the annotated
 *     joins so we need to generated appropriately annotated joins when sampling an edge and when
 *     evaluation a subquery.
 *     <p>One solution would be to always use the unpartitioned views of the indices for the runtime
 *     query optimizer, which is how we are estimating the range counts of the access paths right
 *     now. [Note that the static query optimizer ignores named and default graphs, while the
 *     runtime query optimizer SHOULD pay attention to these things and exploit their conditional
 *     selectivity for the query plan.]
 * @todo Handle optional join graphs by first applying the runtime optimizer to the main join graph
 *     and obtaining a sample for the selected join path. That sample will then be feed into the the
 *     optional join graph in order to optimize the join order within the optional join graph (a
 *     join order which is selective in the optional join graph is better since it build up the #of
 *     intermediate results more slowly and hence do less work).
 *     <p>This is very much related to accepting a collection of non-empty binding sets when running
 *     the join graph. However, optional join graph should be presented in combination with the
 *     original join graph and the starting paths must be constrained to have the selected join path
 *     for the original join graph as a prefix. With this setup, the original join graph has been
 *     locked in to a specific join path and the sampling of edges and vertices for the optional
 *     join graph can proceed normally.
 *     <p>True optionals will always be appended as part of the "tail plan" for any join graph and
 *     can not be optimized as each optional join must run regardless (as long as the intermediate
 *     solution survives the non-optional joins).
 * @todo There are two cases where a join graph must be optimized against a specific set of inputs.
 *     In one case, it is a sample (this is how optimization of an optional join group proceeds per
 *     above). In the other case, the set of inputs is fixed and is provided instead of a single
 *     empty binding set as the starting condition. This second case is actually a bit more
 *     complicated since we can not use a random sample of vertices unless they do not share any
 *     variables with the initial binding sets. When there is a shared variable, we need to do a
 *     cutoff join of the edge with the initial binding sets. When there is not a shared variable,
 *     we can sample the vertex and then do a cutoff join.
 * @todo When we run into a cardinality estimation underflow (the expected cardinality goes to zero)
 *     we could double the sample size for just those join paths which hit a zero estimated
 *     cardinality and re-run them within the round. This would imply that we keep per join path
 *     limits. The vertex and edge samples are already aware of the limit at which they were last
 *     sampled so this should not cause any problems there.
 *     <p>A related option would be to deepen the samples only when we are in danger of cardinality
 *     estimation underflow. E.g., a per-path limit. Resampling vertices may only make sense when we
 *     increase the limit since otherwise we may find a different correlation with the new sample
 *     but the comparison of paths using one sample base with paths using a different sample base in
 *     a different round does not carry forward the cardinality estimates from the prior round
 *     (unless we do something like a weighted moving average).
 * @todo When comparing choices among join paths having fully bound tails where the estimated
 *     cardinality has also gone to zero, we should prefer to evaluate vertices in the tail with
 *     better index locality first. For example, if one vertex had one variable in the original plan
 *     while another had two variables, then solutions which reach the 2-var vertex could be spread
 *     out over a much wider range of the selected index than those which reach the 1-var vertex.
 *     [In order to support this, we would need a means to indicate that a fully bound access path
 *     should use an index specified by the query optimizer rather than the primary index for the
 *     relation. In addition, this suggests that we should keep bloom filters for more than just the
 *     SPO(C) index in scale-out.]
 * @todo Examine behavior when we do not have perfect covering indices. This will mean that some
 *     vertices can not be sampled using an index and that estimation of their cardinality will have
 *     to await the estimation of the cardinality of the edge(s) leading to that vertex. Still, the
 *     approach should be able to handle queries without perfect / covering automatically. Then
 *     experiment with carrying fewer statement indices for quads.
 * @todo Unit test when there are no solutions to the query. In this case there will be no paths
 *     identified by the optimizer and the final path length becomes zero.
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/64">Runtime Query Optimization</a>
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/258">Integrate RTO into SAIL</a>
 * @see <a
 *     href="http://www-db.informatik.uni-tuebingen.de/files/research/pathfinder/publications/rox-demo.pdf">
 *     ROX </a>
 */
public class JGraph {

  private static final String NA = "N/A";

  private static final transient Logger log = Logger.getLogger(JGraph.class);

  /*
   * The pipeline operator for executing the RTO. This provides additional context from the AST
   * model that is necessary to handle some kinds of FILTERs (e.g., those which require conditional
   * routing patterns for chunked materialization).
   */
  private final JoinGraph joinGraph;

  /** Vertices of the join graph. */
  private final Vertex[] V;

  /*
   * Constraints on the join graph. A constraint is applied once all variables referenced by a
   * constraint are known to be bound.
   */
  private final IConstraint[] C;

  /** The kind of samples that will be taken when we sample a {@link Vertex}. */
  private final SampleType sampleType;

  public List<Vertex> getVertices() {
    return Collections.unmodifiableList(Arrays.asList(V));
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("JoinGraph");
    sb.append("{V=[");
    for (Vertex v : V) {
      sb.append("\nV[" + v.pred.getId() + "]=" + v);
    }
    sb.append("{C=[");
    for (IConstraint c : C) {
      sb.append("\nC[" + c.getId() + "]=" + c);
    }
    sb.append("\n]}");
    return sb.toString();
  }

  /*
   * @param joinGraph The pipeline operator that is executing the RTO. This defines the join graph
   *     (vertices, edges, and constraints) and also provides access to the AST and related metadata
   *     required to execute the join graph.
   * @throws IllegalArgumentException if the joinGraph is <code>null</code>.
   * @throws IllegalArgumentException if the {@link JoinGraph#getVertices()} is <code>null</code>.
   * @throws IllegalArgumentException if the {@link JoinGraph#getVertices()} is an empty array.
   * @throws IllegalArgumentException if any element of the {@link JoinGraph#getVertices()}is <code>
   *     null</code>.
   * @throws IllegalArgumentException if any constraint uses a variable which is never bound by the
   *     given predicates.
   * @throws IllegalArgumentException if <i>sampleType</i> is <code>null</code>.
   * @todo unit test for a constraint using a variable which is never bound. the constraint should
   *     be attached at the last vertex in the join path. this will cause the query to fail unless
   *     the variable was already bound, e.g., by a parent query or in the solutions pumped into the
   *     {@link JoinGraph} operator.
   * @todo unit test when the join graph has a single vertex (we never invoke the RTO for less than
   *     3 vertices since with one vertex you just run it and with two vertices you run the lower
   *     cardinality vertex first (though there might be cases where filters require materialization
   *     where running for two vertices could make sense)).
   */
  public JGraph(final JoinGraph joinGraph) {

    if (joinGraph == null) throw new IllegalArgumentException();

    this.joinGraph = joinGraph;

    final IPredicate<?>[] v = joinGraph.getVertices();

    if (v == null) throw new IllegalArgumentException();

    if (v.length < 2) throw new IllegalArgumentException();

    V = new Vertex[v.length];

    for (int i = 0; i < v.length; i++) {

      if (v[i] == null) throw new IllegalArgumentException();

      V[i] = new Vertex(v[i]);
    }

    final IConstraint[] constraints = joinGraph.getConstraints();

    if (constraints != null) {
      C = new IConstraint[constraints.length];
      for (int i = 0; i < constraints.length; i++) {
        if (constraints[i] == null) throw new IllegalArgumentException();
        C[i] = constraints[i];
      }
    } else {
      // No constraints.
      C = null;
    }

    final SampleType sampleType = joinGraph.getSampleType();

    if (sampleType == null) throw new IllegalArgumentException();

    this.sampleType = sampleType;
  }

  //    /*
//     * Find a good join path in the data given the join graph. The join path is
  //     * not guaranteed to be the best join path (the search performed by the
  //     * runtime optimizer is not exhaustive) but it should always be a "good"
  //     * join path and may often be the "best" join path.
  //     *
  //     * @param queryEngine
  //     *            The query engine.
  //     * @param limit
  //     *            The limit for sampling a vertex and the initial limit for
  //     *            cutoff join evaluation.
  //     * @param nedges
  //     *            The edges in the join graph are sorted in order of increasing
  //     *            cardinality and up to <i>nedges</i> of the edges having the
  //     *            lowest cardinality are used to form the initial set of join
  //     *            paths. For each edge selected to form a join path, the
  //     *            starting vertex will be the vertex of that edge having the
  //     *            lower cardinality.
  //     * @param sampleType
  //     *            Type safe enumeration indicating the algorithm which will be
  //     *            used to sample the initial vertices.
  //     *
  //     * @return The join path identified by the runtime query optimizer as the
  //     *         best path given the join graph and the data.
  //     *
  //     * @throws NoSolutionsException
  //     *             If there are no solutions for the join graph in the data (the
  //     *             query does not have any results).
  //     * @throws IllegalArgumentException
  //     *             if <i>queryEngine</i> is <code>null</code>.
  //     * @throws IllegalArgumentException
  //     *             if <i>limit</i> is non-positive.
  //     * @throws IllegalArgumentException
  //     *             if <i>nedges</i> is non-positive.
  //     * @throws Exception
  //     */
  //    public Path runtimeOptimizer(final QueryEngine queryEngine,
  //            final int limit, final int nedges) throws NoSolutionsException,
  //            Exception {
  //
  //        final Map<PathIds, EdgeSample> edgeSamples = new LinkedHashMap<PathIds, EdgeSample>();
  //
  //        return runtimeOptimizer(queryEngine, limit, nedges, edgeSamples);
  //
  //    }

  /*
   * Find a good join path in the data given the join graph. The join path is not guaranteed to be
   * the best join path (the search performed by the runtime optimizer is not exhaustive) but it
   * should always be a "good" join path and may often be the "best" join path.
   *
   * @param queryEngine The query engine.
   * @param edgeSamples A map that will be populated with the samples associated with each
   *     non-pruned join path. This map is used to associate join path segments (expressed as an
   *     ordered array of bopIds) with edge sample to avoid redundant effort.
   * @return The join path identified by the runtime query optimizer as the best path given the join
   *     graph and the data.
   * @throws NoSolutionsException If there are no solutions for the join graph in the data (the
   *     query does not have any results).
   * @throws IllegalArgumentException if <i>queryEngine</i> is <code>null</code>.
   * @throws IllegalArgumentException if <i>limit</i> is non-positive.
   * @throws IllegalArgumentException if <i>nedges</i> is non-positive.
   * @throws Exception
   *     <p>TODO It is possible that this could throw a {@link NoSolutionsException} if the cutoff
   *     joins do not use a large enough sample to find a join path which produces at least one
   *     solution (except that no solutions for an optional join do not cause the total to fail, nor
   *     do no solutions for some part of a UNION).
   *     <p>TODO We need to automatically increase the depth of search for queries where we have
   *     cardinality estimation underflows or punt to another method to decide the join order.
   *     <p>TODO RTO: HEAP MANAGMENT : The edgeSamples map holds references to the cutoff join
   *     samples. To ensure that the map has the minimum heap footprint, it must be scanned each
   *     time we prune the set of active paths and any entry which is not a prefix of an active path
   *     should be removed.
   *     <p>TODO RTO: MEMORY MANAGER : When an entry is cleared from this map, the corresponding
   *     allocation in the memory manager (if any) must be released. The life cycle of the map needs
   *     to be bracketed by a try/finally in order to ensure that all allocations associated with
   *     the map are released no later than when we leave the lexicon scope of that clause.
   */
  public Path runtimeOptimizer(
      final QueryEngine queryEngine, final Map<PathIds, EdgeSample> edgeSamples)
      throws Exception, NoSolutionsException {

    if (queryEngine == null) throw new IllegalArgumentException();

    /*
     * The limit for sampling a vertex and the initial limit for cutoff join
     * evaluation.
     */
    final int limit = joinGraph.getLimit();

    if (limit <= 0) throw new IllegalArgumentException();

    /*
     * The edges in the join graph are sorted in order of increasing
     * cardinality and up to <i>nedges</i> of the edges having the lowest
     * cardinality are used to form the initial set of join paths. For each
     * edge selected to form a join path, the starting vertex will be the
     * vertex of that edge having the lower cardinality.
     */
    final int nedges = joinGraph.getNEdges();

    if (nedges <= 0) throw new IllegalArgumentException();

    if (edgeSamples == null) throw new IllegalArgumentException();

    // Setup the join graph.
    Path[] paths = round0(queryEngine, limit, nedges);

    /*
     * The initial paths all have one edge, and hence two vertices. Each
     * round adds one more vertex to each path. We are done once we have
     * generated paths which include all vertices.
     *
     * This occurs at round := nvertices - 1
     *
     * Note: There are a few edge cases, such as when sampling can not
     * find any solutions, even with an increased sampling limit.
     * Eventually we wind up proving that there are no solutions for the
     * query.
     */

    final int nvertices = V.length;

    int round = 1; // #of rounds.
    int nunderflow = 0; // #of paths with card. underflow (no solutions).

    while (paths.length > 0 && round < nvertices - 1) {

      /*
       * Resample the paths.
       *
       * Note: Since the vertex samples are random, it is possible for the
       * #of paths with cardinality estimate underflow to jump up and down
       * due to the sample which is making its way through each path in
       * each round.
       *
       * Note: The RTO needs an escape hatch here. Otherwise, it is
       * possible for it to spin in a loop while resampling.
       *
       * TODO For example, if the sum of the expected IOs for some path(s)
       * strongly dominates all other paths sharing the same vertices,
       * then we should prune those paths even if there is a cardinality
       * estimate underflow in those paths. This will allow us to focus
       * our efforts on those paths having less IO cost while we seek
       * cardinality estimates which do not underflow.
       *
       * TODO We should be examining the actual sampling limit that is
       * currently in place on each vertex and for each path. This is
       * available by inspection of the VertexSamples and EdgeSamples, but
       * it is not passed back out of the resamplePaths() method as a
       * side-effect. We should limit how much we are willing to raise the
       * limit, e.g., by specifying a MAX_LIMIT annotation on the
       * JoinGraph operator.
       */
      for (int i = 0; i < 3; i++) {

        nunderflow = resamplePaths(queryEngine, limit, round, paths, edgeSamples);

        if (nunderflow == 0) {

          // No paths have cardinality estimate underflow.
          break;
        }

      /*
       * Show information about the paths and the paths that are
         * experiencing cardinality underflow.
         */

        log.warn(
            "Cardinality estimate underflow - resampling: round="
                + round
                + ", npaths="
                + paths.length
                + ", nunderflow="
                + nunderflow
                + ", limit="
                + limit
                + "\n"
                + showTable(paths, null /* pruned */, edgeSamples));
      }

      if (nunderflow > 0) {

        log.warn("Continuing: some paths have cardinality underflow!");
      }

      /*
       * Extend the paths by one vertex.
       */
      paths = expand(queryEngine, limit, round++, paths, edgeSamples);
    }

    if (paths.length == 0) {

      // There are no solutions for the join graph in the data.
      throw new NoSolutionsException();
    }

    /*
     * In general, there should be one winner. However, if there are paths
     * with cardinality estimate underflow (that is, paths that do not have
     * any solutions when sampling the path) then there can be multiple
     * solutions.
     *
     * When this occurs we have a choice. We can either the path with the
     * minimum cost (min sumEstCard) or we can take a path that does not
     * have a cardinality estimate underflow because we actually have an
     * estimate for that path. In principle, the path with the minimum
     * estimated cost should be the better choice, but we do not have any
     * information about the order of the joins beyond the point where the
     * cardinality underflow begins on that path. If we take a path that
     * does not have a cardinality estimate underflow, then at least we know
     * that the join order has been optimized for the entire path.
     */

    final Path selectedPath;

    if (paths.length != 1) {

      log.warn(
          "Multiple paths exist: npaths="
              + paths.length
              + ", nunderflow="
              + nunderflow
              + "\n"
              + showTable(paths, null /* pruned */, edgeSamples));

      Path t = null;

      for (Path p : paths) {

        if (p.edgeSample.isUnderflow()) {

        /*
       * Skip paths with cardinality estimate underflow. They are
           * not fully tested in the data since no solutions have made
           * it through all of the joins.
           */

          continue;
        }

        if (t == null || p.sumEstCard < t.sumEstCard) {

          // Accept path with the least cost.
          t = p;
        }
      }

      if (t == null) {

        /* Arbitrary choice if all paths underflow.
         *
         * TODO Or throw out NoSolutionsException?
         */
        t = paths[0];
      }

      selectedPath = t;

    } else {

      selectedPath = paths[0];
    }

    if (log.isInfoEnabled()) {

      log.info(
          "\n*** Selected join path: "
              + Arrays.toString(selectedPath.getVertexIds())
              + "\n"
              + showPath(selectedPath, edgeSamples));
    }

    return selectedPath;
  }

  /*
   * Return a permutation vector which may be used to reorder the given {@link IPredicate}[] into
   * the evaluation order selected by the runtime query optimizer.
   *
   * @throws IllegalArgumentException if the argument is <code>null</code>.
   * @throws IllegalArgumentException if the given {@link Path} does not cover all vertices in the
   *     join graph.
   */
  public int[] getOrder(final Path p) {

    if (p == null) throw new IllegalArgumentException();

    final IPredicate<?>[] path = p.getPredicates();

    if (path.length != V.length) {
      throw new IllegalArgumentException(
          "Wrong path length: #vertices=" + V.length + ", but path.length=" + path.length);
    }

    final int[] order = new int[V.length];

    for (int i = 0; i < order.length; i++) {

      boolean found = false;
      for (int j = 0; j < order.length; j++) {

        if (path[i].getId() == V[j].pred.getId()) {
          order[i] = j;
          found = true;
          break;
        }
      }

      if (!found) throw new RuntimeException("No such vertex: id=" + path[i].getId());
    }

    return order;
  }

  /*
   * Choose the starting vertices.
   *
   * @param nedges The maximum #of edges to choose.
   * @param paths The set of possible initial paths to choose from.
   * @return Up to <i>nedges</i> minimum cardinality paths.
   */
  public Path[] chooseStartingPaths(final int nedges, final Path[] paths) {

    final List<Path> tmp = new LinkedList<Path>();

    // Sort them by ascending expected cardinality.
    Arrays.sort(paths, 0, paths.length, EstimatedCardinalityComparator.INSTANCE);

    // Choose the top-N edges (those with the least cardinality).
    for (int i = 0; i < paths.length && i < nedges; i++) {

      tmp.add(paths[i]);
    }

    return tmp.toArray(new Path[tmp.size()]);
  }

  /*
   * Choose up to <i>nedges</i> edges to be the starting point. For each of the <i>nedges</i> lowest
   * cardinality edges, the starting vertex will be the vertex with the lowest cardinality for that
   * edge.
   *
   * <p>Note: An edge can not serve as a starting point for exploration if it uses variables (for
   * example, in a CONSTRAINT) which are not bound by either vertex (since the variable(s) are not
   * bound, the constraint would always fail).
   *
   * @param queryEngine The query engine.
   * @param limit The cutoff used when sampling the vertices and when sampling the edges.
   * @param nedges The maximum #of edges to choose. Those having the smallest expected cardinality
   *     will be chosen.
   * @return An initial set of paths starting from at most <i>nedges</i>.
   * @throws Exception
   * @todo UNIT TEST : with runFirst predicates in the join graph. [If we allow "runFirst"
   *     predicates into the join graph, then an initial non-empty join path needs to be constructed
   *     from those vertices. The operators for those APs would also have to be modified to support
   *     cutoff evaluation or to be fully materialized.]
   */
  public Path[] round0(final QueryEngine queryEngine, final int limit, final int nedges)
      throws Exception {

    /*
     * Sample the vertices.
     */
    sampleAllVertices(queryEngine, limit);

    if (log.isInfoEnabled()) {
      final StringBuilder sb = new StringBuilder();
      sb.append("limit=" + limit + ", nedges=" + nedges);
      sb.append(", sampled vertices::\n");
      for (Vertex v : V) {
        if (v.sample != null) {
          sb.append("id=" + v.pred.getId() + " : ");
          sb.append(v.sample.toString());
          sb.append("\n");
        }
      }
      log.info(sb.toString());
    }

    /*
     * Estimate the cardinality for each edge.
     */
    final Path[] a = estimateInitialEdgeWeights(queryEngine, limit);

    //        if (log.isDebugEnabled()) {
    //            final StringBuilder sb = new StringBuilder();
    //            sb.append("All possible initial paths:\n");
    //            for (Path x : a) {
    //                sb.append(x.toString());
    //                sb.append("\n");
    //            }
    //            log.debug(sb.toString());
    //        }
    if (log.isInfoEnabled()) log.info("\n*** Initial Paths\n" + JGraph.showTable(a));

    /*
     * Choose the initial set of paths.
     */
    final Path[] paths_t0 = chooseStartingPaths(nedges, a);

    if (log.isInfoEnabled()) log.info("\n*** Paths @ t0\n" + JGraph.showTable(paths_t0));

    /*
     * Discard samples for vertices which were not chosen as starting points
     * for join paths.
     *
     * Note: We do not need the samples of the other vertices once we decide
     * on the initial vertices from which the join paths will be grown so
     * they could even be discarded at this point.
     */
    {
      final Set<Vertex> initialVertexSet = new LinkedHashSet<Vertex>();

      for (Path x : paths_t0) {

        initialVertexSet.add(x.vertices[0]);
      }

      for (Vertex v : V) {

        if (!initialVertexSet.contains(v)) {

          // Discard sample.
          v.sample = null;
        }
      }
    }

    return paths_t0;
  }

  /*
   * Resample the initial vertices for the specified join paths and then resample the cutoff join
   * for each given join path in path order.
   *
   * @param queryEngine The query engine.
   * @param limitIn The original limit.
   * @param round The round number in [1:n].
   * @param a The set of paths from the previous round. For the first round, this is formed from the
   *     initial set of edges to consider.
   * @param edgeSamples A map used to associate join path segments (expressed as an ordered array of
   *     bopIds) with {@link EdgeSample}s to avoid redundant effort.
   * @return The number of join paths which are experiencing cardinality estimate underflow.
   * @throws Exception
   */
  protected int resamplePaths(
      final QueryEngine queryEngine,
      final int limitIn,
      final int round,
      final Path[] a,
      final Map<PathIds, EdgeSample> edgeSamples)
      throws Exception {

    if (queryEngine == null) throw new IllegalArgumentException();
    if (limitIn <= 0) throw new IllegalArgumentException();
    if (round <= 0) throw new IllegalArgumentException();
    if (a == null) throw new IllegalArgumentException();
    if (a.length == 0) throw new IllegalArgumentException();

    /*
     * Re-sample the vertices which are the initial vertex of any of the
     * existing paths.
     *
     * Note: We do not need to resample vertices unless they are the first
     * vertex in some path. E.g., the initial vertices from which we start.
     * The inputs to an EdgeSample are always either the sample of an
     * initial vertex or the sample of a prior cutoff join in the join
     * path's own history.
     *
     * Note: A request to re-sample a vertex is a NOP unless the limit has
     * been increased since the last time the vertex was sampled. It is also
     * a NOP if the vertex has been fully materialized.
     *
     * Note: Before resampling the vertices, decide what the maximum limit
     * will be for each vertex by examining the paths using that vertex,
     * their current sample limit (TODO there should be a distinct limit for
     * the vertex and for each cutoff join), and whether or not each path
     * experiences a cardinality estimate underflow.
     */
    {
      if (log.isDebugEnabled()) log.debug("Re-sampling in-use vertices.");

      final Map<Vertex, AtomicInteger /* limit */> vertexLimit =
          new LinkedHashMap<Vertex, AtomicInteger>();

      for (Path x : a) {

        final int limit = x.getNewLimit(limitIn);

        final Vertex v = x.vertices[0];

        AtomicInteger theLimit = vertexLimit.get(v);
        if (theLimit == null) {
          vertexLimit.put(v, theLimit = new AtomicInteger());
        }
        theLimit.set(limit);
      }

      // re-sample vertices (this is done in parallel).
      sampleVertices(queryEngine, vertexLimit);
    }

    /*
     * Re-sample the cutoff join for each edge in each of the existing paths
     * using the newly re-sampled vertices.
     *
     * Note: The only way to increase the accuracy of our estimates for
     * edges as we extend the join paths is to re-sample each edge in the
     * join path in path order.
     *
     * Note: An edge must be sampled for each distinct join path prefix in
     * which it appears within each round. However, it is common for
     * surviving paths to share a join path prefix, so do not re-sample a
     * given path prefix more than once per round.
     *
     * FIXME PARALLELIZE: Parallelize the re-sampling for the active paths.
     * This step is responsible for deepening the samples on the non-pruned
     * paths. There is a data race that can occur since the [edgeSamples]
     * map can contain samples for the same sequence of edges in different
     * paths. This is because two paths can shared a common prefix sequence
     * of edges, e.g., [2, 4, 6, 7] and [2, 4, 6, 9] share the path prefix
     * [2, 4, 6]. Therefore both inspection and update of the [edgeSamples]
     * map MUST be synchronized. This code is single threaded since that
     * synchronization mechanism has not yet been put into place. The
     * obvious way to handle this is to use a memoization pattern for the
     * [ids] key for the [edgeSamples] map. This will ensure that the
     * threads that need to resample a given edge will coordinate with the
     * first such thread doing the resampling and the other thread(s)
     * blocking until the resampled edge is available.
     */
    if (log.isDebugEnabled()) log.debug("Re-sampling in-use path segments.");

    // #of paths with cardinality estimate underflow.
    int nunderflow = 0;
    final List<Callable<Boolean>> tasks = new LinkedList<Callable<Boolean>>();
    for (Path x : a) {

      tasks.add(new ResamplePathTask(queryEngine, x, limitIn, edgeSamples));
    } // next Path [x].

    // Execute in the caller's thread.
    for (Callable<Boolean> task : tasks) {

      if (task.call()) {

        nunderflow++;
      }
    }

    return nunderflow;
  }

  /*
   * Resample the edges along a join path. Edges are resampled based on the desired cutoff limit and
   * only as necessary.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class ResamplePathTask implements Callable<Boolean> {

    private final QueryEngine queryEngine;
    private final Path x;
    private final int limitIn;
    private final Map<PathIds, EdgeSample> edgeSamples;

    public ResamplePathTask(
        final QueryEngine queryEngine,
        final Path x,
        final int limitIn,
        final Map<PathIds, EdgeSample> edgeSamples) {
      this.queryEngine = queryEngine;
      this.x = x;
      this.limitIn = limitIn;
      this.edgeSamples = edgeSamples;
    }

    @Override
    public Boolean call() throws Exception {
      /*
       * Get the new sample limit for the path.
       *
       * TODO We only need to increase the sample limit starting at the
       * vertex where we have a cardinality underflow or variability in
       * the cardinality estimate. This is increasing the limit in each
       * round of expansion, which means that we are reading more data
       * than we really need to read.
       */
      final int limit = x.getNewLimit(limitIn);

      // The cutoff join sample of the one step shorter path segment.
      EdgeSample priorEdgeSample = null;

      for (int segmentLength = 2; segmentLength <= x.vertices.length; segmentLength++) {

        // Generate unique key for this join path segment.
        final PathIds ids = new PathIds(BOpUtility.getPredIds(x.getPathSegment(segmentLength)));

        // Look for sample for this path in our cache.
        EdgeSample edgeSample = edgeSamples.get(ids);

        if (edgeSample != null && edgeSample.limit < limit && !edgeSample.isExact()) {
          if (log.isTraceEnabled()) log.trace("Will resample at higher limit: " + ids);
          // Time to resample this edge.
          edgeSamples.remove(ids);
          edgeSample = null;
        }

        if (priorEdgeSample == null) {

        /*
       * This is the first edge in the path.
           *
           * Test our local table of join path segment estimates to
           * see if we have already re-sampled that edge. If not, then
           * re-sample it now.
           */

          assert segmentLength == 2;

          if (edgeSample == null) {

          /*
       * Re-sample the 1st edge in the join path, updating the
             * sample on the edge as a side-effect. The cutoff
             * sample is based on the vertex sample for the minimum
             * cardinality vertex.
             */

            edgeSample =
                AST2BOpRTO.cutoffJoin(
                    queryEngine,
                    joinGraph,
                    limit,
                    x.getPathSegment(2), // 1st edge.
                    C, // constraints
                    V.length == 2, // pathIsComplete
                    x.vertices[0].sample // source sample.
                    );

            // Cache the sample.
            if (edgeSamples.put(ids, edgeSample) != null) throw new AssertionError();
          }

          // Save sample. It will be used to re-sample the next edge.
          priorEdgeSample = edgeSample;

        } else {

        /*
       * The path segment is at least 3 vertices long.
           */
          assert ids.length() >= 3;

          if (edgeSample == null) {

          /*
       * This is some N-step edge in the path, where N is
             * greater than ONE (1). The source vertex is the vertex
             * which already appears in the prior edges of this join
             * path. The target vertex is the next vertex which is
             * visited by the join path. The sample passed in is the
             * prior edge sample -- that is, the sample from the
             * path segment without the target vertex. This is the
             * sample that we just updated when we visited the prior
             * edge of the path.
             */

            edgeSample =
                AST2BOpRTO.cutoffJoin(
                    queryEngine,
                    joinGraph,
                    limit,
                    x.getPathSegment(ids.length()),
                    C, // constraints
                    V.length == ids.length(), // pathIsComplete
                    priorEdgeSample);

            if (log.isTraceEnabled()) log.trace("Resampled: " + ids + " : " + edgeSample);

            if (edgeSamples.put(ids, edgeSample) != null) throw new AssertionError();
          }

          // Save sample. It will be used to re-sample the next edge.
          priorEdgeSample = edgeSample;
        }
      } // next path prefix in Path [x]

      if (priorEdgeSample == null) throw new AssertionError();

      // Save the result on the path.
      x.edgeSample = priorEdgeSample;

      final boolean underflow = x.edgeSample.estimateEnum == EstimateEnum.Underflow;
      if (underflow) {
        if (log.isDebugEnabled()) log.debug("Cardinality underflow: " + x);
      }

      // Done.
      return underflow;
    }
  }

  /*
   * Do one breadth first expansion. In each breadth first expansion we extend each of the active
   * join paths by one vertex for each remaining vertex which enjoys a constrained join with that
   * join path. In the event that there are no remaining constrained joins, we will extend the join
   * path using an unconstrained join if one exists. In all, there are three classes of joins to be
   * considered:
   *
   * <ol>
   *   <li>The target predicate directly shares a variable with the source join path. Such joins are
   *       always constrained since the source predicate will have bound that variable.
   *   <li>The target predicate indirectly shares a variable with the source join path via a
   *       constraint can run for the target predicate and which shares a variable with the source
   *       join path. These joins are indirectly constrained by the shared variable in the
   *       constraint. BSBM Q5 is an example of this case.
   *   <li>Any predicates may always be join to an existing join path. However, joins which do not
   *       share variables either directly or indirectly will be full cross products. Therefore such
   *       joins are added to the join path only after all constrained joins have been consumed.
   * </ol>
   *
   * @param queryEngine The query engine.
   * @param limitIn The original limit.
   * @param round The round number in [1:n].
   * @param a The set of paths from the previous round. For the first round, this is formed from the
   *     initial set of edges to consider.
   * @param edgeSamples A map used to associate join path segments (expressed as an ordered array of
   *     bopIds) with {@link EdgeSample}s to avoid redundant effort.
   * @return The set of paths which survived pruning in this round.
   * @throws Exception
   */
  public Path[] expand(
      final QueryEngine queryEngine,
      int limitIn,
      final int round,
      final Path[] a,
      Map<PathIds, EdgeSample> edgeSamples)
      throws Exception {

    if (queryEngine == null) throw new IllegalArgumentException();
    if (limitIn <= 0) throw new IllegalArgumentException();
    if (round <= 0) throw new IllegalArgumentException();
    if (a == null) throw new IllegalArgumentException();
    if (a.length == 0) throw new IllegalArgumentException();

    /*
     * Ensure that we use a synchronized view of this collection since we
     * will write on it from parallel threads when we expand the join paths
     * in parallel.
     */
    edgeSamples = Collections.synchronizedMap(edgeSamples);

    //        // increment the limit by itself in each round.
    //        final int limit = (round + 1) * limitIn;

    if (log.isDebugEnabled()) log.debug("round=" + round + ", #paths(in)=" + a.length);

    /*
     * Expand each path one step from each vertex which branches to an
     * unused vertex.
     */

    if (log.isDebugEnabled()) log.debug("Expanding paths: #paths(in)=" + a.length);

    // The new set of paths to be explored.
    final List<Path> tmpAll = new LinkedList<Path>();

    // Setup tasks to expand the current join paths.
    final List<Callable<List<Path>>> tasks = new LinkedList<Callable<List<Path>>>();
    for (Path x : a) {

      tasks.add(new ExpandPathTask(queryEngine, x, edgeSamples));
    }

    // Expand paths in parallel.
    final List<Future<List<Path>>> futures =
        queryEngine.getIndexManager().getExecutorService().invokeAll(tasks);

    // Check future, collecting new paths from each task.
    for (Future<List<Path>> f : futures) {

      tmpAll.addAll(f.get());
    }

    /*
     * Now examine the set of generated and sampled join paths. If any paths
     * span the same vertices then they are alternatives and we can pick the
     * best alternative now and prune the other alternatives for those
     * vertices.
     */
    final Path[] paths_tp1 = tmpAll.toArray(new Path[tmpAll.size()]);

    final Path[] paths_tp1_pruned = pruneJoinPaths(paths_tp1, edgeSamples);

    if (log.isDebugEnabled()) // shows which paths were pruned.
    log.info(
          "\n*** round="
              + round
              + ": paths{in="
              + a.length
              + ",considered="
              + paths_tp1.length
              + ",out="
              + paths_tp1_pruned.length
              + "}\n"
              + JGraph.showTable(paths_tp1, paths_tp1_pruned));

    if (log.isInfoEnabled()) // only shows the surviving paths.
    log.info(
          "\n*** round="
              + round
              + ": paths{in="
              + a.length
              + ",considered="
              + paths_tp1.length
              + ",out="
              + paths_tp1_pruned.length
              + "}\n"
              + JGraph.showTable(paths_tp1_pruned));

    return paths_tp1_pruned;
  }

  /*
   * Task expands a path by one edge into one or more new paths.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class ExpandPathTask implements Callable<List<Path>> {

    private final QueryEngine queryEngine;
    private final Path x;
    /*
     * Note: The collection provided by the caller MUST be thread-safe since this task will be run
     * by parallel threads over the different join paths from the last round. There will not be any
     * conflict over writes on this map since each {@link PathIds} instance resulting from the
     * expansion will be unique, but we still need to use a thread-safe collection since there will
     * be concurrent modifications to this map.
     */
    private final Map<PathIds, EdgeSample> edgeSamples;

    public ExpandPathTask(
        final QueryEngine queryEngine, final Path x, final Map<PathIds, EdgeSample> edgeSamples) {
      this.queryEngine = queryEngine;
      this.x = x;
      this.edgeSamples = edgeSamples;
    }

    @Override
    public List<Path> call() throws Exception {
      /*
       * We already increased the sample limit for the path in the loop
       * above.
       */
      final int limit = x.edgeSample.limit;

      /*
       * The set of vertices used to expand this path in this round.
       */
      final Set<Vertex> used = new LinkedHashSet<Vertex>();

      /*
       * Any vertex which (a) does not appear in the path to be
       * extended; (b) has not already been used to extend the path;
       * and (c) does not share any variables indirectly via
       * constraints is added to this collection.
       *
       * If we are not able to extend the path at least once using a
       * constrained join then we will use this collection as the
       * source of unconnected edges which need to be used to extend
       * the path.
       */
      final Set<Vertex> nothingShared = new LinkedHashSet<Vertex>();

      // The new set of paths to be explored as extensions to this path.
      final List<Path> tmp = new LinkedList<Path>();

      // Consider all vertices.
      for (Vertex tVertex : V) {

        // Figure out which vertices are already part of this path.
        final boolean vFound = x.contains(tVertex);

        if (vFound) {
          // Vertex is already part of this path.
          if (log.isTraceEnabled())
            log.trace("Vertex: " + tVertex + " - already part of this path.");
          continue;
        }

        if (used.contains(tVertex)) {
          // Vertex already used to extend this path.
          if (log.isTraceEnabled())
            log.trace("Vertex: " + tVertex + " - already used to extend this path.");
          continue;
        }

        // FIXME RTO: Replace with StaticAnalysis.
        if (!PartitionedJoinGroup.canJoinUsingConstraints(
            x.getPredicates(), // path
            tVertex.pred, // vertex
            C // constraints
            )) {
        /*
       * Vertex does not share variables either directly
           * or indirectly.
           */
          if (log.isTraceEnabled())
            log.trace("Vertex: " + tVertex + " - unconstrained join for this path.");
          nothingShared.add(tVertex);
          continue;
        }

        // add the new vertex to the set of used vertices.
        used.add(tVertex);

        // Extend the path to the new vertex.
        final Path p =
            x.addEdge(
                queryEngine,
                joinGraph,
                limit,
                tVertex,
                C,
                x.getVertexCount() + 1 == V.length // pathIsComplete
                );

        // Add to the set of paths for this round.
        tmp.add(p);

        // Record the sample for the new path.
        if (edgeSamples.put(new PathIds(p.getVertexIds()), p.edgeSample) != null)
          throw new AssertionError();

        if (log.isTraceEnabled())
          log.trace(
              "Extended path with dynamic edge: vnew=" + tVertex.pred.getId() + ", new path=" + p);
      } // next target vertex.

      if (tmp.isEmpty()) {

      /*
       * No constrained joins were identified as extensions of this
         * join path, so we must consider edges which represent fully
         * unconstrained joins.
         */

        assert !nothingShared.isEmpty();

      /*
       * Choose any vertex from the set of those which do
         * not share any variables with the join path. Since
         * all of these are fully unconstrained joins we do
         * not want to expand the join path along multiple
         * edges in this iterator, just along a single
         * unconstrained edge.
         */
        final Vertex tVertex = nothingShared.iterator().next();

        // Extend the path to the new vertex.
        final Path p =
            x.addEdge(
                queryEngine,
                joinGraph,
                limit,
                tVertex,
                C,
                x.getVertexCount() + 1 == V.length // pathIsComplete
                );

        // Add to the set of paths for this round.
        tmp.add(p);

        if (log.isTraceEnabled())
          log.trace(
              "Extended path with dynamic edge: vnew=" + tVertex.pred.getId() + ", new path=" + p);
      } // if(tmp.isEmpty())

      return tmp;
    }
  }

  /*
   * Return the {@link Vertex} whose {@link IPredicate} is associated with the given {@link
   * BOp.Annotations#BOP_ID}.
   *
   * @param bopId The bop identifier.
   * @return The {@link Vertex} -or- <code>null</code> if there is no such vertex in the join graph.
   */
  public Vertex getVertex(final int bopId) {
    for (Vertex v : V) {
      if (v.pred.getId() == bopId) return v;
    }
    return null;
  }

  /*
   * Obtain a sample and estimated cardinality (fast range count) for each vertex.
   *
   * @param queryEngine The query engine.
   * @param limit The sample size.
   *     <p>TODO Only sample vertices with an index.
   *     <p>TODO If any required join has a vertex with a proven exact cardinality of zero, then
   *     there are no solutions for the join group. Throw a {@link NoSolutionsException} and have
   *     the caller handle it?
   *     <p>TODO Consider other cases where we can avoid sampling a vertex or an initial edge.
   *     <p>Be careful about rejecting high cardinality vertices here as they can lead to good
   *     solutions (see the "bar" data set example).
   *     <p>BSBM Q5 provides a counter example where (unless we translate it into a key-range
   *     constraint on an index) some vertices do not share a variable directly and hence will
   *     materialize the full cross product before filtering which is *really* expensive.
   */
  private void sampleAllVertices(final QueryEngine queryEngine, final int limit) {

    final Map<Vertex, AtomicInteger> vertexLimit = new LinkedHashMap<Vertex, AtomicInteger>();

    for (Vertex v : V) {

      vertexLimit.put(v, new AtomicInteger(limit));
    }

    sampleVertices(queryEngine, vertexLimit);
  }

  /*
   * (Re-)sample a set of vertices. Sampling is done in parallel.
   *
   * <p>Note: A request to re-sample a vertex is a NOP unless the limit has been increased since the
   * last time the vertex was sampled. It is also a NOP if the vertex has been fully materialized.
   *
   * @param queryEngine
   * @param vertexLimit A map whose keys are the {@link Vertex vertices} to be (re-)samples and
   *     whose values are the <code>limit</code> to be used when sampling the associated vertex.
   *     This map is read-only so it only needs to be thread-safe for concurrent readers.
   */
  private void sampleVertices(
      final QueryEngine queryEngine, final Map<Vertex, AtomicInteger> vertexLimit) {

    // Setup tasks to sample vertices.
    final List<Callable<Void>> tasks = new LinkedList<Callable<Void>>();
    for (Map.Entry<Vertex, AtomicInteger> e : vertexLimit.entrySet()) {

      final Vertex v = e.getKey();

      final int limit = e.getValue().get();

      tasks.add(new SampleVertexTask(queryEngine, v, limit, sampleType));
    }

    // Sample vertices in parallel.
    final List<Future<Void>> futures;
    try {

      futures = queryEngine.getIndexManager().getExecutorService().invokeAll(tasks);

    } catch (InterruptedException e) {
      // propagate interrupt.
      Thread.currentThread().interrupt();
      return;
    }

    // Check futures for errors.
    final List<Throwable> causes = new LinkedList<Throwable>();
    for (Future<Void> f : futures) {
      try {
        f.get();
      } catch (InterruptedException e) {
        log.error(e);
        causes.add(e);
      } catch (ExecutionException e) {
        log.error(e);
        causes.add(e);
      }
    }

    /*
     * If there were any errors, then throw an exception listing them.
     */
    if (!causes.isEmpty()) {
      // Throw exception back to the leader.
      if (causes.size() == 1) throw new RuntimeException(causes.get(0));
      throw new RuntimeException("nerrors=" + causes.size(), new ExecutionExceptions(causes));
    }
  }

  /*
   * Task to sample a vertex.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class SampleVertexTask implements Callable<Void> {

    private final QueryEngine queryEngine;
    private final Vertex v;
    private final int limit;
    private final SampleType sampleType;

    public SampleVertexTask(
        final QueryEngine queryEngine,
        final Vertex v,
        final int limit,
        final SampleType sampleType) {

      this.queryEngine = queryEngine;
      this.v = v;
      this.limit = limit;
      this.sampleType = sampleType;
    }

    @Override
    public Void call() throws Exception {

      v.sample(queryEngine, limit, sampleType);

      return null;
    }
  }

  /*
   * Estimate the cardinality of each edge. This is only invoked by {@link #round0(QueryEngine, int,
   * int)} when it is trying to select the minimum cardinality edges which it will use to create the
   * initial set of join paths from which the exploration will begin.
   *
   * @param queryEngine The query engine.
   * @param limit The sample size.
   * @throws Exception
   *     <p>TODO It would be very interesting to see the variety and/or distribution of the values
   *     bound when the edge is sampled. This can be easily done using a hash map with a counter.
   *     That could tell us a lot about the cardinality of the next join path (sampling the join
   *     path also tells us a lot, but it does not explain it as much as seeing the histogram of the
   *     bound values). I believe that there are some interesting online algorithms for computing
   *     the N most frequent observations and the like which could be used here.
   */
  private Path[] estimateInitialEdgeWeights(final QueryEngine queryEngine, final int limit)
      throws Exception {

    final List<Callable<Path>> tasks = new LinkedList<Callable<Path>>();

    /*
     * Examine all unordered vertex pairs (v1,v2) once. If any vertex has
     * not been sampled, then it is ignored. For each unordered vertex pair,
     * determine which vertex has the minimum cardinality and create an
     * ordered vertex pair (v,vp) such that [v] is the vertex with the
     * lesser cardinality. Finally, perform a cutoff join of (v,vp) and
     * create a join path with a single edge (v,vp) using the sample
     * obtained from the cutoff join.
     */

    final boolean pathIsComplete = 2 == V.length;

    for (int i = 0; i < V.length; i++) {

      final Vertex v1 = V[i];

      if (v1.sample == null) {

      /*
       * We can only estimate the cardinality of edges connecting
         * vertices for which samples were obtained.
         */

        continue;
      }

      for (int j = i + 1; j < V.length; j++) {

        final Vertex v2 = V[j];

        if (v2.sample == null) {

        /*
       * We can only estimate the cardinality of edges connecting
           * vertices for which samples were obtained.
           */

          continue;
        }

      /*
       * Figure out which vertex has the smaller cardinality. The sample
         * of that vertex is used since it is more representative than the
         * sample of the other vertex.
         */

        final Vertex v, vp;
        if (v1.sample.estCard < v2.sample.estCard) {
          v = v1;
          vp = v2;
        } else {
          v = v2;
          vp = v1;
        }

        if (!PartitionedJoinGroup.canJoinUsingConstraints(new IPredicate[] {v.pred}, vp.pred, C)) {

        /*
       * If there are no shared variables, either directly or
           * indirectly via the constraints, then we can not use this
           * as an initial edge.
           *
           * TODO It may be possible to execute the join in one
           * direction or the other but not both. This seems to be
           * true for RangeBOp.
           *
           * @todo UNIT TEST : correct rejection of initial paths for
           * vertices which are unconstrained joins.
           *
           * @todo UNIT TEST : correct acceptance of initial paths for
           * vertices which are unconstrained joins IFF there are no
           * constrained joins in the join graph.
           */

          continue;
        }

        tasks.add(new CutoffJoinTask(queryEngine, limit, v, vp, pathIsComplete));
      } // next other vertex.
    } // next vertex

    /*
     * Now sample those paths in parallel.
     */

    final List<Path> paths = new LinkedList<Path>();

    //        // Sample the paths in the caller's thread.
    //        for(Callable<Path> task : tasks) {
    //
    //            paths.add(task.call());
    //
    //        }

    // Sample the paths in parallel.
    final List<Future<Path>> futures =
        queryEngine.getIndexManager().getExecutorService().invokeAll(tasks);

    // Check future, collecting new paths from each task.
    for (Future<Path> f : futures) {

      paths.add(f.get());
    }

    return paths.toArray(new Path[paths.size()]);
  }

  /*
   * Cutoff sample an initial join path consisting of two vertices.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class CutoffJoinTask implements Callable<Path> {

    private final QueryEngine queryEngine;
    private final int limit;
    private final Vertex v;
    private final Vertex vp;
    private final boolean pathIsComplete;

    public CutoffJoinTask(
        final QueryEngine queryEngine,
        final int limit,
        final Vertex v,
        final Vertex vp,
        final boolean pathIsComplete) {
      this.queryEngine = queryEngine;
      this.limit = limit;
      this.v = v;
      this.vp = vp;
      this.pathIsComplete = pathIsComplete;
    }

    @Override
    public Path call() throws Exception {

      // The path segment
      final IPredicate<?>[] preds = new IPredicate[] {v.pred, vp.pred};

      // cutoff join of the edge (v,vp)
      final EdgeSample edgeSample =
          AST2BOpRTO.cutoffJoin(
              queryEngine, //
              joinGraph,
              limit, // sample limit
              preds, // ordered path segment.
              C, // constraints
              pathIsComplete,
              v.sample // sourceSample
              );

      final Path p = new Path(v, vp, edgeSample);

      return p;
    }
  }

  /*
   * Prune paths which are dominated by other paths. Paths are extended in each round. Paths from
   * previous rounds are always pruned. Of the new paths in each round, the following rule is
   * applied to prune the search to just those paths which are known to dominate the other paths
   * covering the same set of vertices:
   *
   * <p>If there is a path, [p] != [p1], where [p] is an unordered variant of [p1] (that is the
   * vertices of p are the same as the vertices of p1), and the cumulative cost of [p] is LTE the
   * cumulative cost of [p1], then [p] dominates (or is equivalent to) [p1] and p1 should be pruned.
   *
   * @param a A set of paths.
   * @param edgeSamples The set of samples for path segments. Samples which are no longer in use
   *     after pruning will be cleared from the map and their materialized solution sets will be
   *     discarded.
   * @return The set of paths with all dominated paths removed.
   */
  public Path[] pruneJoinPaths(final Path[] a, final Map<PathIds, EdgeSample> edgeSamples) {
    final boolean neverPruneUnderflow = true;
    /*
     * Find the length of the longest path(s). All shorter paths are
     * dropped in each round.
     */
    int maxPathLen = 0;
    for (Path p : a) {
      if (p.vertices.length > maxPathLen) {
        maxPathLen = p.vertices.length;
      }
    }
    final StringBuilder sb = new StringBuilder();
    final Formatter f = new Formatter(sb);
    final Set<Path> pruned = new LinkedHashSet<Path>();
    for (int i = 0; i < a.length; i++) {
      final Path Pi = a[i];
      if (Pi.edgeSample == null) throw new RuntimeException("Not sampled: " + Pi);
      if (Pi.vertices.length < maxPathLen) {
      /*
       * Only the most recently generated set of paths survive to
         * the next round.
         */
        pruned.add(Pi);
        continue;
      }
      if (pruned.contains(Pi)) {
        // already pruned.
        continue;
      }
      if (neverPruneUnderflow && Pi.edgeSample.estimateEnum == EstimateEnum.Underflow) {
        // Do not use path to prune if path has cardinality underflow.
        continue;
      }
      for (int j = 0; j < a.length; j++) {
        if (i == j) continue;
        final Path Pj = a[j];
        if (Pj.edgeSample == null) throw new RuntimeException("Not sampled: " + Pj);
        if (pruned.contains(Pj)) {
          // already pruned.
          continue;
        }
        final boolean isPiSuperSet = Pi.isUnorderedVariant(Pj);
        if (!isPiSuperSet) {
          // Can not directly compare these join paths.
          continue;
        }
        final long costPi = Pi.sumEstCost;
        final long costPj = Pj.sumEstCost;
        final boolean lte = costPi <= costPj;
        List<Integer> prunedByThisPath = null;
        if (lte) {
          prunedByThisPath = new LinkedList<Integer>();
          if (pruned.add(Pj)) prunedByThisPath.add(j);
          for (int k = 0; k < a.length; k++) {
            final Path x = a[k];
            if (x.beginsWith(Pj)) {
              if (pruned.add(x)) prunedByThisPath.add(k);
            }
          }
        }
        if (log.isDebugEnabled()) {
          f.format(
              "Comparing: P[%2d] with P[%2d] : %10d %2s %10d %s",
              i,
              j,
              costPi,
              (lte ? "<=" : ">"),
              costPj,
              lte ? " *** pruned " + prunedByThisPath : "");
          log.debug(sb);
          sb.setLength(0);
        }
      } // Pj
    } // Pi
    /*
     * Generate a set of paths which will be retained.
     */
    final Path[] b;
    {
      final Set<Path> keep = new LinkedHashSet<Path>();
      for (Path p : a) {
        if (pruned.contains(p)) continue;
        keep.add(p);
      }
      // convert the retained paths to an array.
      b = keep.toArray(new Path[keep.size()]);
    }
    /*
     * Clear any entries from the edgeSamples map which are not prefixes of
     * the retained join paths.
     */
    {
      final Iterator<Map.Entry<PathIds, EdgeSample>> itr = edgeSamples.entrySet().iterator();

      int ncleared = 0;
      while (itr.hasNext()) {

        final Map.Entry<PathIds, EdgeSample> e = itr.next();

        final PathIds ids = e.getKey();

        // Consider the retained paths.
        boolean found = false;

        for (Path p : b) {

          if (p.beginsWith(ids.ids)) {

            // This sample is still in use.
            found = true;

            break;
          }
        }

        if (!found) {

        /*
       * Clear sample no longer in use.
           *
           * Note: In fact, holding onto the sample metadata is
           * relatively cheap if there was a reason to do so (it only
           * effects the size of the [edgeSamples] map). It is holding
           * onto the materialized solution set which puts pressure on
           * the heap.
           */
          if (log.isTraceEnabled()) log.trace("Clearing sample: " + ids);

          // release the sampled solution set.
          e.getValue().releaseSample();

          // clear the entry from the array.
          itr.remove();

          ncleared++;
        }
      }

      if (ncleared > 0 && log.isDebugEnabled()) log.debug("Cleared " + ncleared + " samples");
    }

    // Return the set of retained paths.
    return b;
  }

  /*
   * Comma delimited table showing the estimated join hit ratio, the estimated cardinality, and the
   * set of vertices for each of the specified join paths.
   *
   * @param a An array of join paths.
   * @return A table with that data.
   */
  public static String showTable(final Path[] a) {

    return showTable(a, null /* pruned */);
  }

  /*
   * Return a comma delimited table showing the estimated join hit ratio, the estimated cardinality,
   * and the set of vertices for each of the specified join paths.
   *
   * @param a A set of paths (typically those before pruning).
   * @param pruned The set of paths after pruning (those which were retained) (optional). When
   *     given, the paths which were pruned are marked in the table.
   * @return A table with that data.
   */
  public static String showTable(final Path[] a, final Path[] pruned) {

    return showTable(a, pruned, null /* edgeSamples */);
  }

  /*
   * Return a comma delimited table showing the estimated join hit ratio, the estimated cardinality,
   * and the set of vertices for each of the specified join paths.
   *
   * @param a A set of paths (typically those before pruning).
   * @param pruned The set of paths after pruning (those which were retained) (optional). When
   *     given, the paths which were pruned are marked in the table.
   * @param edgeSamples When non-<code>null</code>, the details will be shown (using {@link
   *     #showPath(Path, Map)}) for each path that is experiencing cardinality estimate underflow
   *     (no solutions in the data).
   * @return A table with that data.
   */
  public static String showTable(
      final Path[] a, final Path[] pruned, final Map<PathIds, EdgeSample> edgeSamples) {
    final StringBuilder sb = new StringBuilder(128 * a.length);
    final List<Path> underflowPaths = new LinkedList<Path>();
    final Formatter f = new Formatter(sb);
    f.format(
        "%-4s %10s%1s * %10s (%8s %8s %8s %8s %8s %8s) = %10s %10s%1s : %10s %10s %10s %10s",
        "path",
        "srcCard",
        "", // sourceSampleExact
        "f",
        // (
        "in",
        "sumRgCt", // sumRangeCount
        "tplsRead",
        "out",
        "limit",
        "adjCard",
        // ) =
        "estRead",
        "estCard",
        "", // estimateIs(Exact|LowerBound|UpperBound)
        "sumEstRead",
        "sumEstCard",
        "sumEstCost",
        "joinPath\n");
    for (int i = 0; i < a.length; i++) {
      final Path x = a[i];
      // true iff the path survived pruning.
      Boolean prune = null;
      if (pruned != null) {
        prune = Boolean.TRUE;
        for (Path y : pruned) {
          if (y == x) {
            prune = Boolean.FALSE;
            break;
          }
        }
      }
      final EdgeSample edgeSample = x.edgeSample;
      if (edgeSample == null) {
        f.format(
            "%4d %10s%1s * %10s (%8s %8s %8s %8s %8s %8s) = %10s %10s%1s : %10s %10s %10s",
            i, NA, "", NA, NA, NA, NA, NA, NA, NA, NA, NA, "", NA, NA, NA);
      } else {
        f.format(
            "%4d %10d%1s * % 10.2f (%8d %8d %8d %8d %8d %8d) = %10d % 10d%1s : % 10d % 10d % 10d",
            i,
            edgeSample.sourceSample.estCard,
            edgeSample.sourceSample.estimateEnum.getCode(),
            edgeSample.f,
            edgeSample.inputCount,
            edgeSample.sumRangeCount,
            edgeSample.tuplesRead,
            edgeSample.outputCount,
            edgeSample.limit,
            edgeSample.adjCard,
            // =
            edgeSample.estRead,
            edgeSample.estCard,
            edgeSample.estimateEnum.getCode(),
            x.sumEstRead,
            x.sumEstCard,
            x.sumEstCost);
      }
      sb.append("  [");
      for (Vertex v : x.getVertices()) {
        f.format("%2d ", v.pred.getId());
      }
      sb.append("]");
      if (pruned != null) {
        if (prune) sb.append(" pruned");
      }
      // for (Edge e : x.edges)
      // sb.append(" (" + e.v1.pred.getId() + " " + e.v2.pred.getId()
      // + ")");
      sb.append("\n");
      if (x.edgeSample.isUnderflow()) {
        underflowPaths.add(x);
      }
    }

    if (edgeSamples != null && !underflowPaths.isEmpty()) {

      // Show paths with cardinality estimate underflow.
      sb.append("\nPaths with cardinality estimate underflow::\n");

      for (Path p : underflowPaths) {

        sb.append(showPath(p, edgeSamples));

        sb.append("----\n");
      }
    }

    return sb.toString();
  }

  /*
   * Show the details of a join path, including the estimated cardinality and join hit ratio for
   * each step in the path.
   *
   * @param p The join path (required).
   * @param edgeSamples A map containing the samples utilized by the {@link Path} (required).
   */
  public static String showPath(final Path x, final Map<PathIds, EdgeSample> edgeSamples) {
    if (x == null) throw new IllegalArgumentException();
    if (edgeSamples == null) throw new IllegalArgumentException();
    final StringBuilder sb = new StringBuilder();
    final Formatter f = new Formatter(sb);
    {
      /*
       * @todo show limit on samples?
       */
      f.format(
          "%4s %10s%1s * %10s (%8s %8s %8s %8s %8s %8s) = %10s %10s%1s : %10s %10s", // %10s %10s",
          "vert",
          "srcCard",
          "", // sourceSampleExact
          "f",
          // (
          "in",
          "sumRgCt", // sumRangeCount
          "tplsRead", // tuplesRead
          "out",
          "limit",
          "adjCard",
          // ) =
          "estRead",
          "estCard",
          "", // estimateIs(Exact|LowerBound|UpperBound)
          "sumEstRead",
          "sumEstCard");
      long sumEstRead = 0; // sum(estRead), where estRead := tuplesRead*f
      long sumEstCard = 0; // sum(estCard)
      //            double sumEstCost = 0; // sum(f(estCard,estRead))
      for (int i = 0; i < x.vertices.length; i++) {
        final int[] ids = BOpUtility.getPredIds(x.getPathSegment(i + 1));
        final int predId = x.vertices[i].pred.getId();
        final SampleBase sample;
        if (i == 0) {
          sample = x.vertices[i].sample;
          if (sample != null) {
            sumEstRead = sample.estCard; // dbls as estRead for vtx
            sumEstCard = sample.estCard;
          }
        } else {
          // edge sample from the caller's map.
          sample = edgeSamples.get(new PathIds(ids));
          if (sample != null) {
            sumEstRead += ((EdgeSample) sample).estRead;
            sumEstCard += ((EdgeSample) sample).estCard;
          }
        }
        sb.append("\n");
        if (sample == null) {
          f.format(
              "% 4d %10s%1s * %10s (%8s %8s %8s %8s %8s %8s) = %10s %10s%1s : %10s %10s", // %10s
                                                                                          // %10s",
              predId, NA, "", NA, NA, NA, NA, NA, NA, NA, NA, NA, "", NA, NA); // ,NA,NA);
        } else if (sample instanceof VertexSample) {
        /*
       * Show the vertex sample for the initial vertex.
           *
           * Note: we do not store all fields for a vertex sample
           * which are stored for an edge sample because so many of
           * the values are redundant for a vertex sample. Therefore,
           * this sets up local variables which are equivalent to the
           * various edge sample columns that we will display.
           */
          final long sumRangeCount = sample.estCard;
          final long estRead = sample.estCard;
          final long tuplesRead = Math.min(sample.estCard, sample.limit);
          final long outputCount = Math.min(sample.estCard, sample.limit);
          final long adjCard = Math.min(sample.estCard, sample.limit);
          f.format(
              "% 4d %10s%1s * %10s (%8s %8s %8s %8s %8s %8s) = % 10d % 10d%1s : %10d %10d", // %10d
                                                                                            // %10s",
              predId,
              " ", // srcSample.estCard
              " ", // srcSample.estimateEnum
              " ", // sample.f,
              " ", // sample.inputCount,
              sumRangeCount,
              tuplesRead,
              outputCount,
              sample.limit, // limit
              adjCard, // adjustedCard
              estRead, // estRead
              sample.estCard, // estCard
              sample.estimateEnum.getCode(),
              sumEstRead,
              sumEstCard);
        } else {
          // Show the sample for a cutoff join with the 2nd+ vertex.
          final EdgeSample edgeSample = (EdgeSample) sample;
          f.format(
              "% 4d %10d%1s * % 10.2f (%8d %8d %8d %8d %8d %8d) = % 10d % 10d%1s : %10d %10d", // %10d %10",
              predId,
              edgeSample.sourceSample.estCard,
              edgeSample.sourceSample.estimateEnum.getCode(),
              edgeSample.f,
              edgeSample.inputCount,
              edgeSample.sumRangeCount,
              edgeSample.tuplesRead,
              edgeSample.outputCount,
              edgeSample.limit,
              edgeSample.adjCard,
              edgeSample.estRead,
              edgeSample.estCard,
              edgeSample.estimateEnum.getCode(),
              sumEstRead,
              sumEstCard);
        }
      }
    }
    sb.append("\n");
    return sb.toString();
  }
} // class JGraph
