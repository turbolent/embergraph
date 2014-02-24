/**
   Copyright (C) SYSTAP, LLC 2006-2012.  All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.bigdata.rdf.graph;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.openrdf.model.URI;
import org.openrdf.model.Value;

import cutthecrap.utils.striterators.IStriterator;

/**
 * Execution context for an {@link IGASProgram}. This is distinct from the
 * {@link IGASEngine} so we can support distributed evaluation and concurrent
 * evaluation of multiple {@link IGASProgram}s.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * 
 * @param <VS>
 *            The generic type for the per-vertex state. This is scoped to the
 *            computation of the {@link IGASProgram}.
 * @param <ES>
 *            The generic type for the per-edge state. This is scoped to the
 *            computation of the {@link IGASProgram}.
 * @param <ST>
 *            The generic type for the SUM. This is often directly related to
 *            the generic type for the per-edge state, but that is not always
 *            true. The SUM type is scoped to the GATHER + SUM operation (NOT
 *            the computation).
 * 
 *            TODO Add option to order the vertices to provide a serializable
 *            execution plan (like GraphChi). I believe that this reduces to
 *            computing a DAG over the frontier before executing the GATHER and
 *            then executing the frontier such that the parallel execution is
 *            constrained by arcs in the DAG that do not have mutual
 *            dependencies. This would have to place a partial ordering over the
 *            vertices in the frontier and then process the frontier with
 *            limited parallelism based on that partial ordering.
 */
public interface IGASContext<VS, ES, ST> extends Callable<IGASStats> {

    /**
     * Return the program that is being evaluated.
     */
    IGASProgram<VS, ES, ST> getGASProgram();

    /**
     * The computation state.
     */
    IGASState<VS, ES, ST> getGASState();

    /**
     * The graph access object.
     */
    IGraphAccessor getGraphAccessor();

    /**
     * Specify the maximum number of iterations for the algorithm. A value of
     * ONE means that the algorithm will halt after the first round.
     * 
     * @param newValue
     *            The maximum number of iterations.
     * 
     * @throws IllegalArgumentException
     *             if the new value is non-positive.
     */
    void setMaxIterations(int newValue);

    /**
     * Return the maximum number iterations for the algorithm.
     */
    int getMaxIterations();

    /**
     * Specify the maximum number of vertices that may be visited. The algorithm
     * will halt if this value is exceeded.
     * 
     * @param newValue
     *            The maximum number of vertices in the frontier.
     * 
     * @throws IllegalArgumentException
     *             if the new value is non-positive.
     */
    void setMaxVisited(int newValue);

    /**
     * Return the maximum number of vertices that may be visited. The algorithm
     * will halt if this value is exceeded.
     */
    int getMaxVisited();

    /**
     * Return non-<code>null</code> iff there is a single link type to be
     * visited. This corresponds to a view of the graph as sparse connectivity
     * matrix. The {@link IGASEngine} can optimize traversal patterns using the
     * <code>POS</code> index.
     * <p>
     * Note: When this option is used, the scatter and gather will not visit the
     * property set for the vertex. Instead, the graph is treated as if it were
     * an unattributed graph and only mined for the connectivity data.
     * 
     * @return The {@link Value} for the predicate that identifies the desired
     *         link type (there can be many types of links - the return value
     *         specifies which attribute is of interest).
     * 
     *         FIXME define getLinkAttribType() (RDR)
     */
    URI getLinkType();

    /**
     * Set an optional constraint on the type of the visited links.
     * <p>
     * Note: When this option is used, the scatter and gather will not visit the
     * property set for the vertex. Instead, the graph is treated as if it were
     * an unattributed graph and only mined for the connectivity data (which may
     * include a link weight).
     * 
     * @param linkType
     *            The link type to visit (optional). When <code>null</code>, all
     *            link types are visited.
     */
    void setLinkType(URI linkType);

    /**
     * Set an optional {@link IReducer} that will run after the
     * {@link IGASProgram} is terminated. This may be used to extract results
     * from the visited vertices.
     * 
     * @param afterOp
     *            The {@link IReducer}.
     */
    <T> void setRunAfterOp(IReducer<VS, ES, ST, T> afterOp);

    /**
     * Return an optional {@link IReducer} that will run after the
     * {@link IGASProgram} is terminated. This may be used to extract results
     * from the visited vertices.
     */
    <T> IReducer<VS, ES, ST, T> getRunAfterOp();

    /**
     * Hook to impose a constraint on the visited edges and/or property values.
     * 
     * @param itr
     *            The iterator visiting those edges and/or property values.
     * 
     * @return Either the same iterator or a constrained iterator.
     * 
     *         TODO Rename as constrainEdgeFilter or even split into a
     *         constrainGatherFilter and a constraintScatterFilter.
     * 
     *         FIXME APPLY : If we need access to the vertex property values in
     *         APPLY (which we probably do, at least optionally), then there
     *         should be a similar method to decide whether the property values
     *         for the vertex are made available during the APPLY.
     */
    IStriterator constrainFilter(IStriterator eitr);
    
    /**
     * Execute one iteration.
     * 
     * @param stats
     *            Used to report statistics about the execution of the
     *            algorithm.
     * 
     * @return true iff the new frontier is empty.
     */
    boolean doRound(IGASStats stats) throws Exception, ExecutionException,
            InterruptedException;

    /**
     * Execute the associated {@link IGASProgram}.
     */
    @Override
    IGASStats call() throws Exception;

}