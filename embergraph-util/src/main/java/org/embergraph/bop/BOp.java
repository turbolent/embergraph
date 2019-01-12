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
 * Created on Aug 12, 2010
 */

package org.embergraph.bop;

import cutthecrap.utils.striterators.IPropertySet;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * An operator, such as a constant, variable, join, sort, filter, etc. Abstract operations, such as
 * the AST, are also described with this data structure.
 *
 * <p>Operators are organized in a tree of operators. The <i>arity</i> of an operator is the number
 * of child operands declared by that operator class. The children of an operator are themselves
 * operators. Parents reference their children, but back references to the parents are not
 * maintained.
 *
 * <p>In addition to their arguments, operators may have a variety of annotations, including those
 * specific to an operator (such as the maximum number of iterators for a closure operator), those
 * shared by many operators (such as set of variables which are selected by a join or distributed
 * hash table), or those shared by all operators (such as a cost model).
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface BOp extends Cloneable, Serializable, IPropertySet {

  /** An empty array. */
  BOp[] NOARGS = new BOp[] {};

  /** An empty <em>immutable</em> annotations map. */
  Map<String, Object> NOANNS = Collections.emptyMap();

  /** The #of arguments to the operation. */
  int arity();

  /*
   * Return an argument to the operation.
   *
   * @param index The argument index in [0:{@link #arity()}-1].
   * @return The argument.
   */
  BOp get(int index);

  /** The operator's arguments as list. */
  List<BOp> args();

  /** An iterator visiting the operator's arguments. */
  Iterator<BOp> argIterator();

  /** A shallow copy of the operator's arguments. */
  BOp[] toArray();

  /*
   * A shallow copy of the operator's arguments using the generic type of the caller's array. If the
   * array has sufficient room, then the arguments are copied into the caller's array. If there is
   * space remaining, a <code>null</code> is appended to mark the end of the data.
   */
  <T> T[] toArray(final T[] a);

  /** The operator's annotations. */
  Map<String, Object> annotations();

  /*
   * Return the value of the named annotation.
   *
   * @param name The name of the annotation.
   * @param defaultValue The default value.
   * @return The annotation value -or- the <i>defaultValue</i> if the annotation was not bound.
   * @param <T> The generic type of the annotation value.
   */
  <T> T getProperty(final String name, final T defaultValue);

  /*
   * Unconditionally sets the property.
   *
   * @param name The name.
   * @param value The value.
   * @return A copy of this {@link BOp} on which the property has been set.
   */
  BOp setProperty(final String name, final Object value);

  /*
   * Return the value of the named annotation.
   *
   * @param name The name of the annotation.
   * @return The value of the annotation and <code>null</code> if the annotation is not bound.
   */
  // <T> T getProperty(final String name);

  //    /*
  //     * Return the value of the named annotation.
  //     *
  //     * @param name
  //     *            The name of the annotation.
  //     *
  //     * @return The value of the annotation.
  //     *
  //     * @throws IllegalArgumentException
  //     *             if the named annotation is not bound.
  //     */
  //    <T> T getRequiredProperty(final String name);

  /*
   * Return the value of the named annotation.
   *
   * @param name The name of the annotation.
   * @return The value of the annotation.
   * @throws IllegalStateException if the named annotation is not bound.
   * @todo Note: This variant without generics is required for some java compiler versions.
   * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954
   */
  Object getRequiredProperty(final String name);

  /** Deep copy clone of the operator. */
  BOp clone();

  /** Return a short (non-recursive) representation of the {@link BOp}. */
  String toShortString();

  /*
   * Return a
   *
   * @return
   */
  String toString();

  /*
   * Return the {@link Annotations#BOP_ID}.
   *
   * @throws IllegalStateException if that annotation is not bound.
   */
  int getId();

  /*
   * Return the evaluation context for the operator as specified by {@link
   * Annotations#EVALUATION_CONTEXT}.
   */
  BOpEvaluationContext getEvaluationContext();

  /*
   * Return <code>true</code> iff this operator is a controller.
   *
   * @see Annotations#CONTROLLER
   */
  boolean isController();

  /*
   * The contract of this method at this level is under-specified. Sub-classes may choose between:
   *
   * <p>- return a string representation of the object, similar to the use of {@link #toString()}
   *
   * <p>Or:
   *
   * <p>- return a pretty-print representation of the object with indent
   *
   * <p>Note that the former contract may or may not include recursive descent through a tree-like
   * object, whereas the latter almost certainly does.
   *
   * @param indent
   * @return
   */
  String toString(final int indent);

  /*
   * Interface declaring well known annotations.
   *
   * <p>Note: Annotation names should be {@link String#intern() interned} in order to avoid having
   * duplicate values for those strings on the heap.
   */
  interface Annotations {

    /*
     * The unique identifier within a query for a specific {@link BOp}. The {@link #QUERY_ID} and
     * the {@link #BOP_ID} together provide a unique identifier for the {@link BOp} within the
     * context of its owning query.
     */
    String BOP_ID = BOp.class.getName() + ".bopId";

    /*
     * The timeout for the operator evaluation (milliseconds).
     *
     * @see #DEFAULT_TIMEOUT
     * @todo Probably support both deadlines and timeouts. A deadline expresses when the query must
     *     be done while a timeout expresses how long it may run. A deadline may be imposed as soon
     *     as the query plan is formulated and could even be communicated from a remote client
     *     (e.g., as an httpd header). A timeout will always be interpreted with respect to the time
     *     when the query began to execute.
     */
    String TIMEOUT = BOp.class.getName() + ".timeout";

    /** The default timeout for operator evaluation. */
    long DEFAULT_TIMEOUT = Long.MAX_VALUE;

    /*
     * This annotation determines where an operator will be evaluated (default {@value
     * #DEFAULT_EVALUATION_CONTEXT}).
     *
     * @see BOpEvaluationContext
     */
    String EVALUATION_CONTEXT = BOp.class.getName() + ".evaluationContext";

    BOpEvaluationContext DEFAULT_EVALUATION_CONTEXT = BOpEvaluationContext.ANY;

    /*
     * A boolean annotation whose value indicates whether or not this is a control operator (default
     * {@value #DEFAULT_CONTROLLER}). A control operator is an operator which will issue subqueries
     * for its arguments. Thus control operators mark a boundary in pipelined evaluation. Some
     * examples of control operators include UNION, STEPS, and STAR (aka transitive closure).
     *
     * @see BOp#isController()
     */
    String CONTROLLER = BOp.class.getName() + ".controller";

    boolean DEFAULT_CONTROLLER = false;

    /** The namespace over which a query is executed. This is used by the MapGraph engine. */
    String NAMESPACE = BOp.class.getName() + ".namespace";
  }
}
