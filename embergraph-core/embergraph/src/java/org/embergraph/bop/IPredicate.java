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
 * Created on Jun 19, 2008
 */

package org.embergraph.bop;

import cutthecrap.utils.striterators.FilterBase;
import cutthecrap.utils.striterators.IFilter;
import java.io.Serializable;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.bop.ap.filter.BOpFilterBase;
import org.embergraph.bop.ap.filter.BOpTupleFilter;
import org.embergraph.bop.ap.filter.DistinctFilter;
import org.embergraph.bop.join.HTreeHashJoinOp;
import org.embergraph.bop.join.JVMHashJoinOp;
import org.embergraph.bop.join.PipelineJoin;
import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleCursor;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.filter.Advancer;
import org.embergraph.btree.filter.TupleFilter;
import org.embergraph.mdi.PartitionLocator;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.relation.IRelation;
import org.embergraph.relation.accesspath.AccessPath;
import org.embergraph.relation.accesspath.ElementFilter;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.rule.IAccessPathExpander;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.pipeline.JoinMasterTask;
import org.embergraph.service.ndx.IClientIndex;
import org.embergraph.striterator.IKeyOrder;

/**
 * An immutable constraint on the elements visited using an {@link IAccessPath}. The slots in the
 * predicate corresponding to variables are named and those names establish binding patterns access
 * {@link IPredicate}s. Access is provided to slots by ordinal index regardless of whether or not
 * they are named variables.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IPredicate<E> extends BOp, Cloneable, Serializable {

  /** Interface declaring well known annotations. */
  interface Annotations
      extends BOp.Annotations, BufferAnnotations, ILocatableResourceAnnotations {

    /**
     * Optional property <em>overrides</em> the {@link IKeyOrder} to use for the access path.
     * Sometimes changing which index is used for a join can change the locality of the join and
     * improve join performance.
     *
     * <p>This property mostly makes sense for the quads mode of the database where there is
     * typically more than one {@link SPOKeyOrder} which begins with the same key component. E.g.,
     * {@link SPOKeyOrder#POCS} versus {@link SPOKeyOrder#PCSO}.
     *
     * <p>An {@link IKeyOrder} override can also make sense when reading on a fully bound access
     * path if there is an alternative key order which might provide better temporal locality. For
     * example, by reading on the same {@link IKeyOrder} as the previous join.
     *
     * <p><strong>WARNING:</strong> DO NOT override the {@link IKeyOrder} unless you are also taking
     * control of (or taking into account) the join order -or- if a hash join will be used against
     * the predicate. Failure to heed this advice can result in an extremely bad access path as the
     * join ordering may cause the variables which are actually bound when the predicate is
     * evaluated to be quite different from what you might otherwise expect.
     *
     * @see https://sourceforge.net/apps/trac/bigdata/ticket/150 (chosing the index for testing
     *     fully bound access paths based on index locality)
     */
    String KEY_ORDER = IPredicate.class.getName() + ".keyOrder";

    /**
     * <code>true</code> iff the predicate has SPARQL optional semantics (default {@value
     * #DEFAULT_OPTIONAL}).
     *
     * <p>Note: When a JOIN is associated with an optional predicate and there are {@link
     * IConstraint}s attached to that join, the constraints are evaluated only for solutions which
     * join. Solutions which do not join (or which join but fail the constraints) are output without
     * testing the constraints a second time.
     *
     * <p>Note: JOIN and SUBQUERY operators will route optional solutions to the altSink if the
     * altSink is specified and to the default sink otherwise.
     */
    String OPTIONAL = IPredicate.class.getName() + ".optional";

    boolean DEFAULT_OPTIONAL = false;

    //        /**
    //         * Constraints on the elements read from the relation.
    //         *
    //         * @deprecated This is being replaced by two classes of filters. One
    //         *             which is always evaluated local to the index and one
    //         *             which is evaluated in the JVM in which the access path is
    //         *             evaluated once the {@link ITuple}s have been resolved to
    //         *             elements of the relation.
    //         *             <p>
    //         *             This was historically an {@link IElementFilter}, which is
    //         *             an {@link IFilterTest}. {@link #INDEX_LOCAL_FILTER} is an
    //         *             {@link IFilter}. You can wrap the {@link IFilterTest} as
    //         *             an {@link IFilter} using {@link ElementFilter}.
    //         */
    //        String CONSTRAINT = "constraint";

    //      /**
    //      * An optional {@link IConstraint}[] which places restrictions on the
    //      * legal patterns in the variable bindings.
    //      */
    //     String CONSTRAINTS = PipelineJoin.class.getName() + ".constraints";

    /**
     * An optional {@link IFilter} that will be evaluated local to the to the index. When the index
     * is remote, the filter will be sent to the node on which the index resides and evaluated
     * there. This makes it possible to efficiently filter out tuples which are not of interest for
     * a given access path.
     *
     * <p>Note: The filter MUST NOT change the type of data visited by the iterator - it must remain
     * an {@link ITupleIterator}. An attempt to change the type of the visited objects will result
     * in a runtime exception. This filter must be "aware" of the reuse of tuples within tuple
     * iterators. See {@link BOpTupleFilter}, {@link TupleFilter} and {@link ElementFilter} for
     * starting points.
     *
     * <p>You can chain {@link BOpFilterBase} filters by nesting them inside of one another. You can
     * chain {@link FilterBase} filters together as well.
     *
     * @see #ACCESS_PATH_FILTER
     * @see IRangeQuery#rangeIterator(byte[], byte[], int, int, IFilter)
     */
    String INDEX_LOCAL_FILTER = IPredicate.class.getName() + ".indexLocalFilter";

    /**
     * An optional {@link BOpFilterBase} to be applied to the elements of the relation as they are
     * materialized from the index. {@link ITuple}s are automatically resolved into relation
     * "elements" before this filter is applied.
     *
     * <p>Unlike {@link #INDEX_LOCAL_FILTER}, this an {@link #ACCESS_PATH_FILTER} is never sent to a
     * remote index for evaluation. This makes it possible to impose {@link DistinctFilter} across a
     * {@link #REMOTE_ACCESS_PATH}.
     *
     * <p>You can chain {@link BOpFilterBase} filters by nesting them inside of one another. You can
     * chain {@link FilterBase} filters together as well.
     */
    String ACCESS_PATH_FILTER = IPredicate.class.getName() + ".accessPathFilter";

    /**
     * Access path expander pattern. This allows you to wrap or replace the {@link IAccessPath}.
     *
     * <p>Note: Access path expanders in scale-out are logically consistent when used with a {@link
     * #REMOTE_ACCESS_PATH}. However, remote access paths often lack the performance of a local
     * access path. In order for the expander to be consistent with a local access path it MUST NOT
     * rewrite the predicate in such a manner as to read on data onto found on the shard onto which
     * the predicate was mapped during query evaluation.
     *
     * <p>In general, scale-out query depends on binding sets being mapped onto the shards against
     * which they will read or write. When an expander changes the bindings on an {@link
     * IPredicate}, it typically changes the access path which will be used. This is only supported
     * when the access path is {@link #REMOTE_ACCESS_PATH}.
     *
     * <p>Note: If an expander generates nested {@link IAccessPath}s then it typically must strip
     * off both the {@link #ACCESS_PATH_EXPANDER} and the {@link #ACCESS_PATH_EXPANDER} from the
     * {@link IPredicate} before generating the inner {@link IAccessPath} and then layer the {@link
     * #ACCESS_PATH_FILTER} back onto the expanded visitation pattern.
     *
     * @see IAccessPathExpander
     */
    String ACCESS_PATH_EXPANDER = IPredicate.class.getName() + ".accessPathExpander";

    /**
     * The partition identifier -or- <code>-1</code> if the predicate does not address a specific
     * shard.
     */
    String PARTITION_ID = IPredicate.class.getName() + ".partitionId";

    int DEFAULT_PARTITION_ID = -1;

    /**
     * Boolean option determines whether the predicate will use a data service local access path
     * (partitioned index view) or a remote access path (global index view) (default {@value
     * #DEFAULT_REMOTE_ACCESS_PATH}).
     *
     * <p>Note: "Remote" has the semantics that the access path has a total view of the index. In
     * scale-out this is achieved using RMI and an {@link IClientIndex}. "Local" has the semantics
     * that the access path has a partitioned view of the index. In scale-out, this corresponds to a
     * shard. In standalone, there is no difference between "local" and "remote" index views since
     * the indices are not partitioned.
     *
     * <p>Local access paths (in scale-out) are much more efficient and should be used for most
     * purposes. However, it is not possible to impose certain kinds of filters on a partitioned
     * index. For example, a DISTINCT filter requires a global index view.
     *
     * <p>When the access path is local (aka partitioned), the parent operator must be annotated to
     * use a {@link BOpEvaluationContext#SHARDED shard wise} or {@link BOpEvaluationContext#HASHED
     * node-wise} mapping of the binding sets.
     *
     * <p>Remote access paths (in scale-out) use a scale-out index view. This view makes the
     * scale-out index appear as if it were monolithic rather than partitioned. The monolithic view
     * of a scale-out index can be used to impose a DISTINCT filter since all tuples will flow back
     * to the caller.
     *
     * <p>When the access path is remote the parent operator should use {@link
     * BOpEvaluationContext#ANY} to prevent the binding sets from being moved around when the access
     * path is remote. Note that the {@link BOpEvaluationContext} is basically ignored for
     * standalone since there is only one place for the operator to run - on the query controller.
     *
     * @see BOpEvaluationContext
     */
    String REMOTE_ACCESS_PATH = IPredicate.class.getName() + ".remoteAccessPath";

    boolean DEFAULT_REMOTE_ACCESS_PATH = true;

    /**
     * If the estimated rangeCount for an {@link AccessPath#iterator()} is LTE this threshold then
     * use a fully buffered (synchronous) iterator. Otherwise use an asynchronous iterator whose
     * capacity is governed by {@link #CHUNK_OF_CHUNKS_CAPACITY}.
     *
     * @see #DEFAULT_FULLY_BUFFERED_READ_THRESHOLD
     */
    String FULLY_BUFFERED_READ_THRESHOLD =
        IPredicate.class.getName() + ".fullyBufferedReadThreshold";

    /**
     * Default for {@link #FULLY_BUFFERED_READ_THRESHOLD}.
     *
     * @todo Experiment with this. It should probably be something close to the branching factor,
     *     e.g., 100.
     */
    int DEFAULT_FULLY_BUFFERED_READ_THRESHOLD = 100; // trunk=20*Bytes.kilobyte32;

    /**
     * Specify the {@link IRangeQuery} flags for the {@link IAccessPath} ( default is {@link
     * IRangeQuery#KEYS}, {@link IRangeQuery#VALS}).
     *
     * <p>Note: Most access paths are read-only so it is nearly always a good idea to set the {@link
     * IRangeQuery#READONLY} flag.
     *
     * <p>Note: Access paths used to support high-level query can nearly always use {@link
     * IRangeQuery#PARALLEL} iterator semantics, which permits the iterator to run in parallel
     * across index partitions in scale-out. This flag only effects operations which use a global
     * index view in scale-out ( pipeline joins do something different).
     *
     * <p>Note: Some expanders may require the {@link IRangeQuery#CURSOR} flag. For example, {@link
     * Advancer} patterns use an {@link ITupleCursor} rather than an {@link ITupleIterator}.
     * However, since the cursors are <i>slightly</i> slower, they should only be specified when
     * their semantics are necessary.
     *
     * @see #DEFAULT_FLAGS
     */
    String FLAGS = IPredicate.class.getName() + ".flags";

    /** The default flags will visit the keys and values of the non-deleted tuples. */
    int DEFAULT_FLAGS = IRangeQuery.KEYS | IRangeQuery.VALS
        //                | IRangeQuery.PARALLEL
        ;

    //		/**
    //		 * Boolean property whose value is <code>true</code> iff this operator
    //		 * writes on a database.
    //		 * <p>
    //		 * Most operators operate solely on streams of elements or binding sets.
    //		 * Some operators read or write on the database using an access path,
    //		 * which is typically described by an {@link IPredicate}. This property
    //		 * MUST be <code>true</code> when access path is used to write on the
    //		 * database.
    //		 * <p>
    //		 * Operators which read or write on the database must declare the
    //		 * {@link Annotations#TIMESTAMP} associated with that operation.
    //		 *
    //		 * @see #TIMESTAMP
    //		 */
    //		String MUTATION = IPredicate.class.getName() + ".mutation";
    //
    //        boolean DEFAULT_MUTATION = false;

    /**
     * Note: This annotation is not currently integrated. It is intended to provide a means to
     * constrain the key-range of the predicate based on an allowable value range for some slot in
     * that predicate.
     */
    String RANGE = IPredicate.class.getName() + ".range";

    /**
     * Limits the number of elements read from the access path for this predicate when used in a
     * join ({@link HTreeHashJoinOp}, {@link JVMHashJoinOp}, {@link PipelineJoin}). Note that this
     * is limiting INPUT to a join, it says nothing about the output from a join.
     */
    String CUTOFF_LIMIT = IPredicate.class.getName() + ".cutoffLimit";

    /** Deault is to not cut off the join. */
    long DEFAULT_CUTOFF_LIMIT = Long.MAX_VALUE;
  }

  /**
   * Resource identifier (aka namespace) identifies the {@link IRelation} associated with this
   * {@link IPredicate}.
   *
   * @throws IllegalStateException if there is more than on element in the view.
   * @see Annotations#RELATION_NAME
   * @todo Rename as getRelationName()
   */
  String getOnlyRelationName();

  /**
   * Return the ith element of the relation view. The view is an ordered array of resource
   * identifiers that describes the view for the relation.
   *
   * @param index The index into the array of relation names in the view.
   * @deprecated Unions of predicates must be handled explicitly as a union of pipeline operators
   *     reading against the different predicate.
   */
  String getRelationName(int index);

  /**
   * The #of elements in the relation view.
   *
   * @deprecated per {@link #getRelationName(int)}.
   */
  int getRelationCount();

  /**
   * The index partition identifier and <code>-1</code> if no partition identifier was specified.
   *
   * @return The index partition identifier -or- <code>-1</code> if the predicate is not locked to a
   *     specific index partition.
   * @see Annotations#PARTITION_ID
   * @see PartitionLocator
   * @see AccessPath
   * @see JoinMasterTask
   */
  int getPartitionId();

  /**
   * Sets the index partition identifier constraint.
   *
   * @param partitionId The index partition identifier.
   * @return The constrained {@link IPredicate}.
   * @throws IllegalArgumentException if the index partition identified is a negative integer.
   * @throws IllegalStateException if the index partition identifier was already specified.
   * @see Annotations#PARTITION_ID
   */
  IPredicate<E> setPartitionId(int partitionId);

  /**
   * <code>true</code> iff the predicate is optional when evaluated as the right-hand side of a
   * join. An optional predicate will match once after all matches in the data have been exhausted.
   * By default, the match will NOT bind any variables that have been determined to be bound by the
   * predicate based on the computed {@link IEvaluationPlan}.
   *
   * <p>For mutation, some {@link IRelation}s may require that all variables appearing in the head
   * are bound. This and similar constraints can be enforced using {@link IConstraint}s on the
   * {@link IRule}.
   *
   * @return <code>true</code> iff this predicate is optional when evaluating a JOIN.
   */
  boolean isOptional();

  /**
   * Returns the object that may be used to selectively override the evaluation of the predicate.
   *
   * @return The {@link IAccessPathExpander}.
   * @see Annotations#ACCESS_PATH_EXPANDER
   * @todo Replace with {@link IAccessPathExpander#getAccessPath(IAccessPath)} , which is the only
   *     method declared by {@link IAccessPathExpander}?
   */
  IAccessPathExpander<E> getAccessPathExpander();

  //    /**
  //     * An optional constraint on the visitable elements.
  //     *
  //     * @see Annotations#CONSTRAINT
  //     *
  //     * @deprecated This is being replaced by two classes of filters. One which
  //     *             is always evaluated local to the index and one which is
  //     *             evaluated in the JVM in which the access path is evaluated
  //     *             once the {@link ITuple}s have been resolved to elements of
  //     *             the relation.
  //     */
  //    public IElementFilter<E> getConstraint();

  //    /**
  //     * Return the optional {@link IConstraint}[] to be applied by a join which
  //     * evaluates this {@link IPredicate}.
  //     * <p>
  //     * Note: The {@link Annotations#CONSTRAINTS} are annotated on the
  //     * {@link IPredicate} rather than the join operators so they may be used
  //     * with join graphs, which are expressed solely as an unordered set of
  //     * {@link IPredicate}s. Using join graphs, we are able to do nifty things
  //     * such as runtime query optimization which would not be possible if the
  //     * annotations were decorating the joins since we would be unable to
  //     * dynamically generate the join operators with the necessary annotations.
  //     *
  //     * @see Annotations#CONSTRAINTS
  //     */
  //    public IConstraint[] constraints();

  /**
   * Return the optional filter to be evaluated local to the index.
   *
   * @see Annotations#INDEX_LOCAL_FILTER
   */
  IFilter getIndexLocalFilter();

  /**
   * Return the optional filter to be evaluated once tuples have been converted into relation
   * elements by the access path (local to the caller).
   *
   * @see Annotations#ACCESS_PATH_FILTER
   */
  IFilter getAccessPathFilter();

  /**
   * Return the {@link IKeyOrder} override for this {@link IPredicate} by the query optimizer.
   *
   * @return The assigned {@link IKeyOrder} or <code>null</code> if the {@link IKeyOrder} was not
   *     overridden.
   * @see Annotations#KEY_ORDER
   */
  IKeyOrder<E> getKeyOrder();

  //    /**
  //     * Set the {@link IKeyOrder} annotation on the {@link IPredicate}, returning
  //     * a new {@link IPredicate} in which the annotation takes on the given
  //     * binding.
  //     *
  //     * @param keyOrder
  //     *            The {@link IKeyOrder}.
  //     *
  //     * @return The new {@link IPredicate}.
  //     *
  //     * @see Annotations#KEY_ORDER
  //     */
  //    public IPredicate<E> setKeyOrder(final IKeyOrder<E> keyOrder);

  /**
   * Figure out if all positions in the predicate which are required to form the key for this access
   * path are bound in the predicate.
   */
  boolean isFullyBound(IKeyOrder<E> keyOrder);

  /**
   * @deprecated This is only used in a few places, which should probably use {@link
   *     BOpUtility#getArgumentVariableCount(BOp)} instead.
   */
  int getVariableCount();

  /**
   * The #of arguments in the predicate required for the specified {@link IKeyOrder} which are
   * unbound.
   *
   * @param keyOrder The key order.
   * @return The #of unbound arguments for that {@link IKeyOrder}.
   */
  int getVariableCount(IKeyOrder<E> keyOrder);

  /**
   * Return <code>true</code> if this is a remote access path.
   *
   * @see Annotations#REMOTE_ACCESS_PATH
   */
  boolean isRemoteAccessPath();

  /**
   * Return the variable or constant at the specified index.
   *
   * @param index The index.
   * @return The variable or constant at the specified index.
   * @throws IllegalArgumentException if the index is less than zero or GTE the {@link #arity()} of
   *     the {@link IPredicate}.
   */
  /*
   * Note: the return value can not be parameterized without breaking code.
   */
  @SuppressWarnings("rawtypes")
  IVariableOrConstant get(int index);

  /**
   * Return the asBound value at the specified index for the given element. This method does not
   * consider the bindings of the predicate instance.
   *
   * <p>Note: there is no general means available to implement this method of an awareness of the
   * internal structure of the element type. General purpose record types, such as GOM or relation
   * records, can generally implement this method in the context of the "schema" imposed by the
   * predicate.
   *
   * @param e The element, which must implement {@link IElement}.
   * @param index The index.
   * @return The value.
   * @throws UnsupportedOperationException If this operation is not supported by the {@link
   *     IPredicate} implementation or for the given element type.
   * @deprecated by {@link IElement#get(int)} which does exactly what this method is trying to do.
   */
  IConstant<?> get(E e, int index);

  /**
   * Return a new instance in which all occurrences of the given variable have been replaced by the
   * specified constant.
   *
   * <p>Note: The optimal {@link IKeyOrder} often changes when binding a variable on a predicate.
   *
   * @param var The variable.
   * @param val The constant.
   * @return A new instance of the predicate in which all occurrences of the variable have been
   *     replaced by the constant.
   * @throws IllegalArgumentException if either argument is <code>null</code>.
   */
  Predicate<E> asBound(final IVariable<?> var, final IConstant<?> val);

  /**
   * Return a new instance in which all occurrences of the variable appearing in the binding set
   * have been replaced by their bound values.
   *
   * <p>Note: The optimal {@link IKeyOrder} often changes when binding a variable on a predicate.
   *
   * @param bindingSet The binding set.
   * @return The as-bound {@link IPredicate} -or- <code>null</code> if the {@link IPredicate} can
   *     not be unified with the {@link IBindingSet}.
   * @see #815 (RDR query does too much work)
   */
  IPredicate<E> asBound(IBindingSet bindingSet);

  /**
   * Extract the as bound value from the predicate. When the predicate is not
   * bound at that index, the value of the variable is taken from the binding
   * set.
   *
   * @param index
   *            The index into that predicate.
   * @param bindingSet
   *            The binding set.
   *
   * @return The bound value -or- <code>null</code> if no binding is available
   *         (the predicate is not bound at that index and the variable at
   *         that index in the predicate is not bound in the binding set).
   *
   * @throws IndexOutOfBoundsException
   *             unless the <i>index</i> is in [0:{@link #arity()-1], inclusive.
   * @throws IllegalArgumentException
   *             if the <i>bindingSet</i> is <code>null</code>.
   */
  Object asBound(int index, IBindingSet bindingSet);

  /**
   * A copy of this {@link IPredicate} in which the <i>relationName</i>(s) replace the existing set
   * of relation name(s).
   *
   * @param relationName The relation name(s).
   * @throws IllegalArgumentException if <i>relationName</i> is empty.
   * @throws IllegalArgumentException if <i>relationName</i> is <code>null</code>
   * @throws IllegalArgumentException if any element of <i>relationName</i> is <code>null</code>
   * @deprecated This will be modified to use a scalar relation name per {@link
   *     #getOnlyRelationName()}.
   */
  IPredicate<E> setRelationName(String[] relationName);

  /** Representation of the predicate without variable bindings. */
  String toString();

  /**
   * Representation of the predicate with variable bindings.
   *
   * @param bindingSet The variable bindings
   */
  String toString(IBindingSet bindingSet);

  /**
   * Compares the bindings of two predicates for equality.
   *
   * @param other Another predicate.
   * @return true iff the predicate have the same arity and their ordered bindings are the same.
   *     when both predicates have a variable at a given index, the names of the variables must be
   *     the same.
   */
  boolean equals(Object other);

  /**
   * The hash code is defined as
   *
   * <pre>
   * get(0).hashCode()*31&circ;(n-1) + get(1).hashCode()*31&circ;(n-2) + ... + get(n-1).hashCode()
   * </pre>
   *
   * using <code>int</code> arithmetic, where <code>n</code> is the {@link #arity()} of the
   * predicate, and <code>^</code> indicates exponentiation.
   *
   * <p>Note: This is similar to how {@link String#hashCode()} is defined.
   */
  int hashCode();

  /**
   * Sets the {@link org.embergraph.bop.BOp.Annotations#BOP_ID} annotation.
   *
   * @param bopId The bop id.
   * @return The newly annotated {@link IPredicate}.
   */
  IPredicate<E> setBOpId(int bopId);

  /**
   * Return a copy of this predicate with a different {@link IVariableOrConstant} for the arg
   * specified by the supplied index parameter.
   */
  @SuppressWarnings("rawtypes")
  IPredicate<E> setArg(int index, IVariableOrConstant arg);

  //	/**
  //	 * Return <code>true</code> iff this operator is an access path which writes
  //	 * on the database.
  //	 *
  //	 * @see Annotations#MUTATION
  //	 */
  //	boolean isMutation();

  /**
   * The timestamp or transaction identifier on which the operator will read or write.
   *
   * @see Annotations#TIMESTAMP
   * @throws IllegalStateException if {@link Annotations#TIMESTAMP} was not specified.
   */
  long getTimestamp();
}
