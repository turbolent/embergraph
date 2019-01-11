/*

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
 * Created on Jun 25, 2008
 */

package org.embergraph.relation.rule.eval;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.embergraph.bop.BOp;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IConstraint;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.Var;
import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.bop.joinGraph.IEvaluationPlanFactory;
import org.embergraph.bop.joinGraph.IRangeCountFactory;
import org.embergraph.btree.keys.ISortKeyBuilder;
import org.embergraph.config.Configuration;
import org.embergraph.config.IValidator;
import org.embergraph.io.IStreamSerializer;
import org.embergraph.journal.AbstractTask;
import org.embergraph.journal.ConcurrencyManager;
import org.embergraph.journal.IIndexManager;
import org.embergraph.mdi.IMetadataIndex;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.relation.AbstractResource;
import org.embergraph.relation.IMutableRelation;
import org.embergraph.relation.IRelation;
import org.embergraph.relation.RelationFusedView;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBlockingBuffer;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.relation.accesspath.UnsynchronizedArrayBuffer;
import org.embergraph.relation.locator.IResourceLocator;
import org.embergraph.relation.rule.IProgram;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.IStep;
import org.embergraph.relation.rule.Rule;
import org.embergraph.relation.rule.eval.pipeline.JoinTask;
import org.embergraph.service.AbstractScaleOutFederation;
import org.embergraph.service.IDataService;
import org.embergraph.service.IDataServiceCallable;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.embergraph.striterator.IKeyOrder;

/**
 * Interface provides an interoperability nexus for the {@link IPredicate}s,
 * {@link IBindingSet}s, and {@link ISolution}s for the evaluation of an
 * {@link IRule} and is responsible for resolving the relation symbol to the
 * {@link IRelation} object. Instances of this interface may be type-specific
 * and allow you to control various implementation classes used during
 * {@link IRule} execution.
 * <p>
 * Note: This interface is NOT {@link Serializable}. Use an
 * {@link IJoinNexusFactory} to create instances of this interface.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IJoinNexus {

    /**
     * The factory for rule statistics objects.
     */
    IRuleStatisticsFactory getRuleStatisticsFactory();
    
    /**
     * The factory object is used to materialize appropriate {@link IJoinNexus}
     * instances when the rule execution crosses an RMI boundary.
     */
    IJoinNexusFactory getJoinNexusFactory();

    /**
     * The factory object for range counts used by {@link IEvaluationPlan}s.
     */
    IRangeCountFactory getRangeCountFactory();

    /**
     * The kind of operation that is being executed (Query, Insert, or Delete).
     */
    ActionEnum getAction();

	/**
     * When <code>true</code>, rule level parallelism is disabled and the
     * {@link ISolution} buffers are flushed after after every {@link IStep}.
     * This can be enabled if you are exploring apparent concurrency problems
     * with the rules. It should normally be <code>false</code> for better
     * performance.
     * @deprecated by bop annotations.
     */
    boolean forceSerialExecution();
    
    /**
     * The maximum #of subqueries for the first join dimension that will be
     * issued in parallel. Use ZERO(0) to avoid submitting tasks to the
     * {@link ExecutorService} entirely and ONE (1) to submit a single task at a
     * time to the {@link ExecutorService}.
     * @deprecated by bop annotations.
     */
    int getMaxParallelSubqueries();
    
    /**
     * The #of elements in a chunk for query or mutation. This is normally a
     * relatively large value on the order of <code>10,000</code> or better.
     * @deprecated by bop annotations.
     */
    int getChunkCapacity();
    
    /**
     * The #of chunks that can be held by an {@link IBuffer} that is the target
     * or one or more {@link UnsynchronizedArrayBuffer}s. This is generally a
     * small value on the order of the #of parallel producers that might be
     * writing on the {@link IBuffer} since the capacity of the
     * {@link UnsynchronizedArrayBuffer}s is already quite large (10k or better
     * elements, defining a single "chunk" from a single producer).
     * 
     * @see #getMaxParallelSubqueries()
     * @see #getChunkCapacity()
     * @deprecated by bop annotations.
     */
    int getChunkOfChunksCapacity();

    /**
     * The #of elements that will be materialized in a fully buffered read by an
     * {@link IAccessPath}. When this threshold is exceeded the
     * {@link IAccessPath} will use an {@link IAsynchronousIterator} instead.
     * This value should on the order of {@link #getChunkCapacity()}.
     * 
     * @see IAccessPath#iterator(long,long, int)
     * @see AbstractResource.Options#FULLY_BUFFERED_READ_THRESHOLD
     * @deprecated by bop annotations.
     */
    int getFullyBufferedReadThreshold();

    /**
     * Resolve the property value using the {@link IIndexManager}, the namespace
     * of the resource, and the {@link Properties} instance to be tested as
     * hidden parameters.
     * 
     * @param name
     *            The property name.
     * @param defaultValue
     *            The default.
     * 
     * @return The resolved property value.
     * 
     * @see Configuration
     */
    String getProperty(final String name, final String defaultValue);

    /**
     * Resolves, parses, and validates the property value.
     * 
     * @param name
     *            The property name.
     * @param defaultValue
     *            The default value.
     * @return
     */
    <T> T getProperty(final String name, final String defaultValue,
            IValidator<T> validator);

    /**
     * Binds variables from a visited element.
     * <p>
     * Note: The bindings are propagated before the constraints are verified so
     * this method will have a side-effect on the bindings even if the
     * constraints were not satisfied. Therefore you should clone the bindings
     * before calling this method.
     * 
     * @param rule
     *            The rule.
     * @param index
     *            The index of the {@link IPredicate} in the body of the
     *            {@link Rule}.
     * @param e
     *            An element materialized by the {@link IAccessPath} for that
     *            {@link IPredicate}.
     * @param bindingSet
     *            the bindings to which new bindings from the element will be
     *            applied.
     * 
     * @return <code>true</code> unless the new bindings would violate any of
     *         the {@link IConstraint}s declared for the {@link Rule}).
     * 
     * @throws NullPointerException
     *             if an argument is <code>null</code>.
     * @throws IndexOutOfBoundsException
     *             if the <i>index</i> is out of bounds.
     * 
     * @deprecated by {@link #bind(IPredicate, Object, IBindingSet)}
     */
    boolean bind(final IRule rule, final int index, final Object e,
            final IBindingSet bindings);

    /**
     * Binds variables from a visited element.
     * <p>
     * Note: The bindings are propagated before the constraints are verified so
     * this method will have a side-effect on the bindings even if the
     * constraints were not satisfied. Therefore you should clone the bindings
     * before calling this method.
     * 
     * @param pred
     *            The {@link IPredicate} from which the element was read.
     * @param constraints
     *            An array of constraints which must be satisfied (optional).
     * @param e
     *            An element materialized by the {@link IAccessPath} for that
     *            {@link IPredicate}.
     * @param bindingSet
     *            the bindings to which new bindings from the element will be
     *            applied.
     * 
     * @return <code>true</code> unless the new bindings would violate any of
     *         the optional {@link IConstraint}.
     * 
     * @throws NullPointerException
     *             if an argument is <code>null</code>.
     */
    boolean bind(IPredicate<?> pred, IConstraint[] constraints, Object e,
            IBindingSet bindings);

    /**
     * Return a 'fake' binding for the given variable in the specified
     * predicate. The binding should be such that it is of a legal type for the
     * slot in the predicate associated with that variable. This is used to
     * discovery the {@link IKeyOrder} associated with the {@link IAccessPath}
     * that will be used to evaluate the predicate when it appears in the tail
     * of an {@link IRule} for a given {@link IEvaluationPlan}.
     * 
     * @param predicate
     *            The predicate.
     * @param var
     *            A variable appearing in that predicate.
     *            
     * @return The 'fake' binding.
     */
    IConstant fakeBinding(IPredicate predicate, Var var);
    
    /**
     * Create a new {@link ISolution}. The behavior of this method generally
     * depends on bit flags specified when the {@link IJoinNexus} was created.
     * <p>
     * Note: For many purposes, it is only the computed {@link #ELEMENT}s that
     * are of interest. For high-level query, you will generally specify only
     * the {@link #BINDINGS}. The {@link #BINDINGS} are also useful for some
     * truth maintenance applications. The {@link #RULE} is generally only of
     * interest for inspecting the behavior of some rule set.
     * 
     * @param rule
     *            The rule.
     * @param bindingSet
     *            The bindings (the implementation MUST clone the bindings if
     *            they will be saved with the {@link ISolution}).
     * 
     * @return The new {@link ISolution}.
     * 
     * @throws IllegalArgumentException
     *             if any parameter is <code>null</code>.
     * 
     * @see #ELEMENT
     * @see #BINDINGS
     * @see #RULE
     * @see #solutionFlags()
     * @see Solution
     */
    ISolution newSolution(IRule rule, IBindingSet bindingSet);

    /**
     * Return an {@link ISortKeyBuilder} for an {@link IBindingSet}. The sort
     * key may be used to SORT {@link IBindingSet}s or to impose a DISTINCT
     * filter on {@link ISolution}s, etc.
     * 
     * @param rule
     *            The rule that will determine the order imposed amoung the
     *            bound variables (which variable is 1st, 2nd, 3rd, etc.).
     * 
     * @return The sort key builder (NOT thread-safe).
     */
    ISortKeyBuilder<IBindingSet> newBindingSetSortKeyBuilder(IRule rule); 
    
    /**
     * The flags that effect the behavior of {@link #newSolution(IRule, IBindingSet)}.
     */
    int solutionFlags();
    
    /**
     * Bit flag indicating that {@link #newSolution(IRule, IBindingSet)} should
     * materialize an element from the {@link IRule} and {@link IBindingSet} and
     * make it available via {@link ISolution#get()}.
     */
    final int ELEMENT = 1 << 0;

    /**
     * Bit flag indicating that {@link #newSolution(IRule, IBindingSet)} should
     * clone the {@link IBindingSet} and make it available via
     * {@link ISolution#getBindingSet()}.
     */
    final int BINDINGS = 1 << 1;

    /**
     * Bit flag indicating that {@link #newSolution(IRule, IBindingSet)} make
     * the {@link IRule} that generated the {@link ISolution} available via
     * {@link ISolution#getRule()}.
     */
    final int RULE = 1 << 2;

    /**
     * {@link #ELEMENT} and {@link #BINDINGS} and {@link #RULE}.
     */
    final int ALL = ELEMENT|BINDINGS|RULE;
    
    /**
     * Factory for {@link IBindingSet} implementations.
     * <p>
     * Note: The factory MUST apply any bound
     * {@link IRule#getConstants() constants} for the {@link IRule} before
     * returning the {@link IBindingSet}.
     * 
     * @param rule
     *            The rule whose bindings will be stored in the binding set.
     * 
     * @return A new binding set suitable for that rule.
     */
    IBindingSet newBindingSet(IRule rule);

    /**
     * Return the effective {@link IRuleTaskFactory} for the rule. When the rule
     * is a step of a sequential program writing on one or more
     * {@link IMutableRelation}s, then the returned {@link IStepTask} must
     * automatically flush the buffer after the rule executes in order to ensure
     * that the state of the {@link IMutableRelation}(s) are updated before the
     * next {@link IRule} is executed.
     * 
     * @param parallel
     *            <code>true</code> unless the rule is a step is a sequential
     *            {@link IProgram}. Note that a sequential step MUST flush its
     *            buffer since steps are run in sequence precisely because they
     *            have a dependency!
     * @param rule
     *            A rule that is a step in some program. If the program is just
     *            a rule then the value of <i>parallel</i> does not matter. The
     *            buffer will is cleared when it flushed so a re-flushed is
     *            always a NOP.
     * 
     * @return The {@link IStepTask} to execute for that rule.
     * 
     * @see RunRuleAndFlushBufferTaskFactory
     * @see RunRuleAndFlushBufferTask
     * 
     * @deprecated by annotations on individual {@link BOp}s which specify which
     *             join operator is to be used on a join by join basis.
     */
    IRuleTaskFactory getRuleTaskFactory(boolean parallel, IRule rule);
    
    /**
     * Return the factory for {@link IEvaluationPlan}s.
     * 
     * @return The factory.
     */
    IEvaluationPlanFactory getPlanFactory();
    
    /**
     * Equivalent to {@link IJoinNexusFactory#getWriteTimestamp()}.
     */
    long getWriteTimestamp();
    
    /**
     * Equivalent to {@link IJoinNexusFactory#getReadTimestamp()}.
     */
    long getReadTimestamp();
    
    /**
     * Locate and return the view of the relation identified by the
     * {@link IPredicate}. The implementation must choose a view that will
     * accept writes iff this is a mutation operation and which is associated
     * with an appropriate timestamp.
     * 
     * @param pred
     *            The {@link IPredicate}, which MUST be the head of some
     *            {@link IRule}.
     * 
     * @return The {@link IRelation}, which will never be a fused view and
     *         which will accept writes iff the rules are being executed as a
     *         mutation operation.
     */
    IRelation getHeadRelationView(IPredicate pred);
    
    /**
     * Locate and return the view of the relation(s) identified by the
     * {@link IPredicate}.
     * <p>
     * Note: This method is responsible for returning a fused view when more
     * than one relation name was specified for the {@link IPredicate}. It
     * SHOULD be used whenever the {@link IRelation} is selected based on a
     * predicate in the tail of an {@link IRule} and could therefore be a fused
     * view of more than one relation instance. (The head of the {@link IRule}
     * must be a simple {@link IRelation} and not a view.)
     * <p>
     * Note: The implementation should choose the read timestamp for each
     * relation in the view using {@link #getReadTimestamp(String)}.
     * 
     * @param pred
     *            The {@link IPredicate}, which MUST be a tail from some {@link IRule}.
     * 
     * @return The {@link IRelation}, which might be a
     *         {@link RelationFusedView}.
     */
    IRelation getTailRelationView(IPredicate pred);

//    /**
//     * Obtain an access path reading from the view for the relation associated
//     * with the specified predicate (from the tail of some rule).
//     * 
//     * @param pred
//     *            The predicate.
//     * 
//     * @return The access path.
//     * 
//     * @deprecated by {@link #getTailAccessPath(IRelation, IPredicate)} which
//     *             factors out obtaining the {@link IRelation} view into the
//     *             caller, which turns out to be a significant cost savings.
//     */
//    IAccessPath getTailAccessPath(IPredicate pred);

    /**
     * Obtain an access path reading from relation for the specified predicate
     * (from the tail of some rule).
     * <p>
     * Note that passing in the {@link IRelation} is important since it
     * otherwise must be discovered using the {@link IResourceLocator}. By
     * requiring the caller to resolve it before hand and pass it into this
     * method the contention and demand on the {@link IResourceLocator} cache is
     * reduced.
     * 
     * @param relation
     *            The relation.
     * @param pred
     *            The predicate. When {@link IPredicate#getPartitionId()} is
     *            set, the returned {@link IAccessPath} MUST read on the
     *            identified local index partition (directly, not via RMI).
     * 
     * @return The access path.
     */
    IAccessPath getTailAccessPath(IRelation relation, IPredicate pred);
    
    /**
     * Return an iterator visiting the {@link PartitionLocator} for the index
     * partitions from which an {@link IAccessPath} must read in order to
     * materialize all elements which would be visited for that predicate.
     * <p>
     * Note: You can use an {@link IDataServiceCallable} to obtain the
     * reference of the {@link IDataService} and pass that into your
     * {@link AbstractTask} in order to have the federation reference available
     * when running under the {@link ConcurrencyManager}.
     * 
     * @param predicate
     *            The predicate, with whatever bindings already applied.
     * @param fed
     *            The federation, which is required in order to access the
     *            {@link IMetadataIndex} for a scale-out index.
     * @param joinNexus
     *            The {@link IJoinNexus}.
     * 
     * @return The iterator.
     */
    Iterator<PartitionLocator> locatorScan(
            final AbstractScaleOutFederation<?> fed, final IPredicate<?> predicate);
    
    /**
     * Used to locate indices, relations and relation containers.
     */
    IIndexManager getIndexManager();

    /**
     * Run as a query.
     * 
     * @param step
     *            The {@link IRule} or {@link IProgram}.
     * 
     * @return An iterator from which you can read the solutions.
     * 
     * @throws IllegalStateException
     *             unless this is an {@link ActionEnum#Query}.
     * @throws IllegalArgumentException
     *             if either argument is <code>null</code>.
     * 
     * @todo ISolution is only used when constructing entailments for which we
     *       also need to know the rule which licensed the entailment in order
     *       to generate the justification chain. This should all be done by a
     *       CONSTRUCT operator. The {@link #solutionFlags()} should also go
     *       away, including {@link #BINDINGS}, {@link #ELEMENT}, {@link #RULE},
     *       and {@link #ALL}.
     */
    IChunkedOrderedIterator<ISolution> runQuery(IStep step) throws Exception;

    /**
     * Run as mutation operation (it will write any solutions onto the relations
     * named in the head of the various {@link IRule}s).
     * 
     * @param step
     *            The {@link IRule} or {@link IProgram}.
     * 
     * @return The mutation count (#of distinct elements modified in the
     *         relation(s)).
     * 
     * @throws IllegalArgumentException
     *             unless {@link ActionEnum#isMutation()} is <code>true</code>.
     * @throws IllegalArgumentException
     *             if either argument is <code>null</code>.
     */
    long runMutation(IStep step) throws Exception;
    
    /**
     * Return a thread-safe buffer onto which chunks of computed
     * {@link ISolution}s will be written. The client will drain
     * {@link ISolution}s from buffer using {@link IBlockingBuffer#iterator()}.
     */
    IBlockingBuffer<ISolution[]> newQueryBuffer();

    /**
     * Return a thread-safe buffer onto which chunks of computed
     * {@link ISolution}s will be written. When the buffer is
     * {@link IBuffer#flush() flushed} the chunked {@link ISolution}s will be
     * inserted into the {@link IMutableRelation}.
     * 
     * @param relation
     *            The relation.
     * 
     * @return The buffer.
     */
    IBuffer<ISolution[]> newInsertBuffer(IMutableRelation relation);

    /**
     * Return a thread-safe buffer onto which chunks of computed
     * {@link ISolution}s will be written. When the buffer is
     * {@link IBuffer#flush() flushed} the chunks of {@link ISolution}s will be
     * deleted from the {@link IMutableRelation}.
     * 
     * @param relation
     *            The relation.
     * 
     * @return The buffer.
     */
    IBuffer<ISolution[]> newDeleteBuffer(IMutableRelation relation);

    /**
     * Return a buffer suitable for a single-threaded writer that flushes onto
     * the specified <i>targetBuffer</i>.
     * <p>
     * The returned buffer MUST apply the optional filter value returned by
     * {@link #getSolutionFilter()} in order to keep individual
     * {@link ISolution}s out of the buffer. Filtering is done at this level
     * since the <i>targetBuffer</i> contains chunks of solutions.
     * 
     * @param targetBuffer
     *            A thread-safe buffer for chunks of {@link ISolution}s that
     *            was allocated with {@link #newQueryBuffer()},
     *            {@link #newInsertBuffer(IMutableRelation)}, or
     *            {@link #newDeleteBuffer(IMutableRelation)}.
     * @param chunkCapacity
     *            The capacity of the new buffer. This should be maximum chunk
     *            size that will be produced or {@link #getChunkCapacity()} if
     *            you do not have better information.
     * 
     * @return A non-thread-safe buffer.
     */
    IBuffer<ISolution> newUnsynchronizedBuffer(
            IBuffer<ISolution[]> targetBuffer, int chunkCapacity);
    
    /**
     * Return the {@link IElementFilter} that will be used to reject solutions
     * based on the bindings for the head of the rule -or- <code>null</code>
     * if no filter will be imposed. This may be used for query or mutation.
     * 
     * @return The optional filter.
     * 
     * @todo Normally the {@link IElementFilter} is specified in terms of the
     *       elements of some {@link IRelation} and then wrapped as a
     *       {@link SolutionFilter}.
     *       <p>
     *       The return type here should be strongly typed. The generic argument
     *       is not a sufficient guarantee and the compiler can be fooled by
     *       passing an {@link IElementFilter} that is already defined in terms
     *       of an {@link ISolution} rather than element of an {@link IRelation}.
     */
    IElementFilter<ISolution> getSolutionFilter();
    
    /**
     * Return the object that is responsible for (de-)serializing chunks of
     * {@link ISolution}s.  This is used by high-level query to transfer
     * {@link ISolution}s back to the client.
     */
    IStreamSerializer<ISolution[]> getSolutionSerializer();
    
    /**
     * Return the object that is responsible for (de-)serializing chunks of
     * {@link IBindingSet}s. This is used by {@link JoinTask}s to transfer
     * intermediate {@link IBindingSet}s from one join dimension to the next.
     */
    IStreamSerializer<IBindingSet[]> getBindingSetSerializer();
  
}
