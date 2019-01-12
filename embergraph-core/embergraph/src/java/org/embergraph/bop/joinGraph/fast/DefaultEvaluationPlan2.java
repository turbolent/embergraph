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
 * Created on Jun 26, 2008
 */

package org.embergraph.bop.joinGraph.fast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOpUtility;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.bop.joinGraph.IRangeCountFactory;
import org.embergraph.relation.rule.IAccessPathExpander;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.IStarJoin;
import org.embergraph.relation.rule.eval.IJoinNexus;

/*
 * The evaluation order is determined by analysis of the propagation of bindings. The most selective
 * predicate is chosen first (having the fewest unbound variables with ties broken by a range count
 * on the data) and "fake" bindings are propagated to the other predicates in the tail. This process
 * is repeated until all variables are bound and an evaluation order has been determined.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DefaultEvaluationPlan2 implements IEvaluationPlan {

  protected static final transient Logger log = Logger.getLogger(DefaultEvaluationPlan2.class);

  protected static final transient boolean DEBUG = log.isDebugEnabled();

  protected static final transient boolean INFO = log.isInfoEnabled();

  /** @todo not serializable but used by {@link #rangeCount(int)}, which is a problem. */
  private final IRangeCountFactory rangeCountFactory;

  private final IRule rule;

  private final int tailCount;

  private static final transient long BOTH_OPTIONAL = Long.MAX_VALUE - 1;

  private static final transient long ONE_OPTIONAL = Long.MAX_VALUE - 2;

  private static final transient long NO_SHARED_VARS = Long.MAX_VALUE - 3;

  /*
   * The computed evaluation order. The elements in this array are the order in which each tail
   * predicate will be evaluated. The index into the array is the index of the tail predicate whose
   * evaluation order you want. So <code>[2,0,1]</code> says that the predicates will be evaluated
   * in the order tail[2], then tail[0], then tail[1].
   */
  private int[ /* order */] order;

  public int[] getOrder() {

    if (order == null) {

      /*
       * This will happen if you try to use toString() during the ctor
       * before the order has been computed.
       */

      throw new IllegalStateException();
    }
    //        calc();

    return order;
  }

  /*
   * Cache of the computed range counts for the predicates in the tail. The elements of this array
   * are initialized to -1L, which indicates that the range count has NOT been computed. Range
   * counts are computed on demand and MAY be zero. Only an approximate range count is obtained.
   * Such approximate range counts are an upper bound on the #of elements that are spanned by the
   * access pattern. Therefore if the range count reports ZERO (0L) it is a real zero and the access
   * pattern does not match anything in the data. The only other caveat is that the range counts are
   * valid as of the commit point on which the access pattern is reading. If you obtain them for
   * {@link ITx#READ_COMMITTED} or {@link ITx#UNISOLATED} views then they could be invalidated by
   * concurrent writers.
   */
  private long[ /*tailIndex*/] rangeCount;

  /** Keeps track of which tails have been used already and which still need to be evaluated. */
  private transient boolean[ /*tailIndex*/] used;

  /*
   * <code>true</code> iff the rule was proven to have no solutions.
   *
   * @todo this is not being computed.
   */
  private boolean empty = false;

  public boolean isEmpty() {

    return empty;
  }

  /*
   * Computes an evaluation plan for the rule.
   *
   * @param joinNexus The join nexus.
   * @param rule The rule.
   */
  public DefaultEvaluationPlan2(final IJoinNexus joinNexus, final IRule rule) {

    this(joinNexus.getRangeCountFactory(), rule);
  }

  /*
   * Computes an evaluation plan for the rule.
   *
   * @param rangeCountFactory The range count factory.
   * @param rule The rule.
   */
  public DefaultEvaluationPlan2(final IRangeCountFactory rangeCountFactory, final IRule rule) {

    if (rangeCountFactory == null) throw new IllegalArgumentException();

    if (rule == null) throw new IllegalArgumentException();

    this.rangeCountFactory = rangeCountFactory;

    this.rule = rule;

    this.tailCount = rule.getTailCount();

    if (DEBUG) {

      log.debug("rule=" + rule);
    }

    calc(rule);

    if (DEBUG) {
      for (int i = 0; i < tailCount; i++) {
        log.debug(order[i]);
      }
    }
  }

  /** Compute the evaluation order. */
  private void calc(final IRule rule) {

    if (order != null) return;

    order = new int[tailCount];
    rangeCount = new long[tailCount];
    used = new boolean[tailCount];

    // clear arrays.
    for (int i = 0; i < tailCount; i++) {
      order[i] = -1; // -1 is used to detect logic errors.
      rangeCount[i] = -1L; // -1L indicates no range count yet.
      used[i] = false; // not yet evaluated
    }

    if (tailCount == 1) {
      order[0] = 0;
      return;
    }

    /*
    if (tailCount == 2) {
        order[0] = cardinality(0) <= cardinality(1) ? 0 : 1;
        order[1] = cardinality(0) <= cardinality(1) ? 1 : 0;
        return;
    }
    */

    final Set<IVariable<?>> runFirstVars = new HashSet<IVariable<?>>();

    int startIndex = 0;
    for (int i = 0; i < tailCount; i++) {
      final IPredicate pred = rule.getTail(i);
      final IAccessPathExpander expander = pred.getAccessPathExpander();
      if (expander != null && expander.runFirst()) {
        if (DEBUG) log.debug("found a run first, tail " + i);
        final Iterator<IVariable<?>> it = BOpUtility.getArgumentVariables(pred);
        while (it.hasNext()) {
          runFirstVars.add(it.next());
        }
        order[startIndex++] = i;
        used[i] = true;
      }
    }

    // if there are no more tails left after the expanders, we're done
    if (startIndex == tailCount) {
      return;
    }

    // if there is only one tail left after the expanders
    if (startIndex == tailCount - 1) {
      if (DEBUG) log.debug("one tail left");
      for (int i = 0; i < tailCount; i++) {
        // only check unused tails
        if (used[i]) {
          continue;
        }
        order[tailCount - 1] = i;
        used[i] = true;
        return;
      }
    }

    int preferredFirstTail = -1;
    // give preferential treatment to a tail that shares variables with the
    // runFirst expanders
    for (int i = 0; i < tailCount; i++) {
      // only check unused tails
      if (used[i]) {
        continue;
      }
      final IPredicate pred = rule.getTail(i);
      final Iterator<IVariable<?>> it = BOpUtility.getArgumentVariables(pred);
      while (it.hasNext()) {
        if (runFirstVars.contains(it.next())) {
          preferredFirstTail = i;
        }
      }
      if (preferredFirstTail != -1) break;
    }

    // if there are only two tails left after the expanders
    if (startIndex == tailCount - 2) {
      if (DEBUG) log.debug("two tails left");
      int t1 = -1;
      int t2 = -1;
      for (int i = 0; i < tailCount; i++) {
        // only check unused tails
        if (used[i]) {
          continue;
        }
        // find the two unused tail indexes
        if (t1 == -1) {
          t1 = i;
        } else {
          t2 = i;
          break;
        }
      }
      if (DEBUG) log.debug(t1 + ", " + t2);
      if (preferredFirstTail != -1) {
        order[tailCount - 2] = preferredFirstTail;
        order[tailCount - 1] = preferredFirstTail == t1 ? t2 : t1;
      } else {
        order[tailCount - 2] = cardinality(t1) <= cardinality(t2) ? t1 : t2;
        order[tailCount - 1] = cardinality(t1) <= cardinality(t2) ? t2 : t1;
      }
      return;
    }

    /*
     * There will be (tails-1) joins, we just need to figure out what
     * they should be.
     */
    Join join = preferredFirstTail == -1 ? getFirstJoin() : getFirstJoin(preferredFirstTail);
    int t1 = ((Tail) join.getD1()).getTail();
    int t2 = ((Tail) join.getD2()).getTail();
    if (preferredFirstTail == -1) {
      order[startIndex] = cardinality(t1) <= cardinality(t2) ? t1 : t2;
      order[startIndex + 1] = cardinality(t1) <= cardinality(t2) ? t2 : t1;
    } else {
      order[startIndex] = t1;
      order[startIndex + 1] = t2;
    }
    used[order[startIndex]] = true;
    used[order[startIndex + 1]] = true;
    for (int i = startIndex + 2; i < tailCount; i++) {
      join = getNextJoin(join);
      order[i] = ((Tail) join.getD2()).getTail();
      used[order[i]] = true;
    }
  }

  /*
   * Start by looking at every possible initial join. Take every tail and match it with every other
   * tail to find the lowest possible cardinality. See {@link
   * #computeJoinCardinality(org.embergraph.bop.joinGraph.fast.DefaultEvaluationPlan2.IJoinDimension,
   * org.embergraph.bop.joinGraph.fast.DefaultEvaluationPlan2.IJoinDimension)} for more on this.
   */
  private Join getFirstJoin() {
    if (DEBUG) {
      log.debug("evaluating first join");
    }
    long minJoinCardinality = Long.MAX_VALUE;
    long minTailCardinality = Long.MAX_VALUE;
    long minOtherTailCardinality = Long.MAX_VALUE;
    Tail minT1 = null;
    Tail minT2 = null;
    for (int i = 0; i < tailCount; i++) {
      // only check unused tails
      if (used[i]) {
        continue;
      }
      Tail t1 = new Tail(i, rangeCount(i), getVars(i));
      long t1Cardinality = cardinality(i);
      for (int j = 0; j < tailCount; j++) {
        // check only non-same and unused tails
        if (i == j || used[j]) {
          continue;
        }
        Tail t2 = new Tail(j, rangeCount(j), getVars(j));
        long t2Cardinality = cardinality(j);
        long joinCardinality = computeJoinCardinality(t1, t2);
        long tailCardinality = Math.min(t1Cardinality, t2Cardinality);
        long otherTailCardinality = Math.max(t1Cardinality, t2Cardinality);
        if (DEBUG) log.debug("evaluating " + i + " X " + j + ": cardinality= " + joinCardinality);
        if (joinCardinality < minJoinCardinality) {
          if (DEBUG) log.debug("found a new min: " + joinCardinality);
          minJoinCardinality = joinCardinality;
          minTailCardinality = tailCardinality;
          minOtherTailCardinality = otherTailCardinality;
          minT1 = t1;
          minT2 = t2;
        } else if (joinCardinality == minJoinCardinality) {
          if (tailCardinality < minTailCardinality) {
            if (DEBUG) log.debug("found a new min: " + joinCardinality);
            minJoinCardinality = joinCardinality;
            minTailCardinality = tailCardinality;
            minOtherTailCardinality = otherTailCardinality;
            minT1 = t1;
            minT2 = t2;
          } else if (tailCardinality == minTailCardinality) {
            if (otherTailCardinality < minOtherTailCardinality) {
              if (DEBUG) log.debug("found a new min: " + joinCardinality);
              minJoinCardinality = joinCardinality;
              minTailCardinality = tailCardinality;
              minOtherTailCardinality = otherTailCardinality;
              minT1 = t1;
              minT2 = t2;
            }
          }
        }
      }
    }
    // the join variables is the union of the join dimensions' variables
    Set<String> vars = new HashSet<String>();
    vars.addAll(minT1.getVars());
    vars.addAll(minT2.getVars());
    return new Join(minT1, minT2, minJoinCardinality, vars);
  }

  private Join getFirstJoin(final int preferredFirstTail) {
    if (DEBUG) {
      log.debug("evaluating first join");
    }

    long minJoinCardinality = Long.MAX_VALUE;
    long minOtherTailCardinality = Long.MAX_VALUE;
    Tail minT2 = null;
    final int i = preferredFirstTail;
    final Tail t1 = new Tail(i, rangeCount(i), getVars(i));
    for (int j = 0; j < tailCount; j++) {
      // check only non-same and unused tails
      if (i == j || used[j]) {
        continue;
      }
      Tail t2 = new Tail(j, rangeCount(j), getVars(j));
      long t2Cardinality = cardinality(j);
      long joinCardinality = computeJoinCardinality(t1, t2);
      if (DEBUG) log.debug("evaluating " + i + " X " + j + ": cardinality= " + joinCardinality);
      if (joinCardinality < minJoinCardinality) {
        if (DEBUG) log.debug("found a new min: " + joinCardinality);
        minJoinCardinality = joinCardinality;
        minOtherTailCardinality = t2Cardinality;
        minT2 = t2;
      } else if (joinCardinality == minJoinCardinality) {
        if (t2Cardinality < minOtherTailCardinality) {
          if (DEBUG) log.debug("found a new min: " + joinCardinality);
          minJoinCardinality = joinCardinality;
          minOtherTailCardinality = t2Cardinality;
          minT2 = t2;
        }
      }
    }

    // the join variables is the union of the join dimensions' variables
    Set<String> vars = new HashSet<String>();
    vars.addAll(t1.getVars());
    vars.addAll(minT2.getVars());
    return new Join(t1, minT2, minJoinCardinality, vars);
  }

  /*
   * Similar to {@link #getFirstJoin()}, but we have one join dimension already calculated.
   *
   * @param d1 the first join dimension
   * @return the new join with the lowest cardinality from the remaining tails
   */
  private Join getNextJoin(IJoinDimension d1) {
    if (DEBUG) {
      log.debug("evaluating next join");
    }
    long minJoinCardinality = Long.MAX_VALUE;
    long minTailCardinality = Long.MAX_VALUE;
    Tail minTail = null;
    for (int i = 0; i < tailCount; i++) {
      // only check unused tails
      if (used[i]) {
        continue;
      }
      Tail tail = new Tail(i, rangeCount(i), getVars(i));
      long tailCardinality = cardinality(i);
      long joinCardinality = computeJoinCardinality(d1, tail);
      if (DEBUG)
        log.debug(
            "evaluating " + d1.toJoinString() + " X " + i + ": cardinality= " + joinCardinality);
      if (joinCardinality < minJoinCardinality) {
        if (DEBUG) log.debug("found a new min: " + joinCardinality);
        minJoinCardinality = joinCardinality;
        minTailCardinality = tailCardinality;
        minTail = tail;
      } else if (joinCardinality == minJoinCardinality) {
        if (tailCardinality < minTailCardinality) {
          if (DEBUG) log.debug("found a new min: " + joinCardinality);
          minJoinCardinality = joinCardinality;
          minTailCardinality = tailCardinality;
          minTail = tail;
        }
      }
    }
    // if we are at the "no shared variables" tails, order by range count
    if (minJoinCardinality == NO_SHARED_VARS) {
      minJoinCardinality = Long.MAX_VALUE;
      for (int i = 0; i < tailCount; i++) {
        // only check unused tails
        if (used[i]) {
          continue;
        }
        Tail tail = new Tail(i, rangeCount(i), getVars(i));
        long tailCardinality = cardinality(i);
        if (tailCardinality < minJoinCardinality) {
          if (DEBUG) log.debug("found a new min: " + tailCardinality);
          minJoinCardinality = tailCardinality;
          minTail = tail;
        }
      }
    }
    // the join variables is the union of the join dimensions' variables
    Set<String> vars = new HashSet<String>();
    vars.addAll(d1.getVars());
    vars.addAll(minTail.getVars());
    return new Join(d1, minTail, minJoinCardinality, vars);
  }

  /*
   * Return the range count for the predicate, ignoring any bindings. The range count for the tail
   * predicate is cached the first time it is requested and returned from the cache thereafter. The
   * range counts are requested using the "non-exact" range count query, so the range counts are
   * actually the upper bound. However, if the upper bound is ZERO (0) then the range count really
   * is ZERO (0).
   *
   * @param tailIndex The index of the predicate in the tail of the rule.
   * @return The range count for that tail predicate.
   */
  public long rangeCount(final int tailIndex) {

    if (rangeCount[tailIndex] == -1L) {

      final IPredicate predicate = rule.getTail(tailIndex);

      final IAccessPathExpander expander = predicate.getAccessPathExpander();

      if (expander != null && expander.runFirst()) {

        /*
         * Note: runFirst() essentially indicates that the cardinality
         * of the predicate in the data is to be ignored. Therefore we
         * do not request the actual range count and just return -1L as
         * a marker indicating that the range count is not available.
         */

        return -1L;
      }

      final long rangeCount = rangeCountFactory.rangeCount(rule.getTail(tailIndex));

      this.rangeCount[tailIndex] = rangeCount;
    }

    return rangeCount[tailIndex];
  }

  /*
   * Return the cardinality of a particular tail, which is the range count if not optional and
   * infinite if optional.
   */
  public long cardinality(final int tailIndex) {
    IPredicate tail = rule.getTail(tailIndex);
    if (tail.isOptional() || tail instanceof IStarJoin) {
      return Long.MAX_VALUE;
    } else {
      return rangeCount(tailIndex);
    }
  }

  public String toString() {
    return Arrays.toString(getOrder());
  }

  /*
   * This is the secret sauce. There are three possibilities for computing the join cardinality,
   * which we are defining as the upper-bound for solutions for a particular join. First, if there
   * are no shared variables then the cardinality will just be the simple sum of the cardinality of
   * each join dimension. If there are shared variables but no unshared variables, then the
   * cardinality will be the minimum cardinality from the join dimensions. If there are shared
   * variables but also some unshared variables, then the join cardinality will be the maximum
   * cardinality from each join dimension.
   *
   * <p>Any join involving an optional will have infinite cardinality, so that optionals get placed
   * at the end.
   *
   * @param d1 the first join dimension
   * @param d2 the second join dimension
   * @return the join cardinality
   */
  protected long computeJoinCardinality(IJoinDimension d1, IJoinDimension d2) {
    // two optionals is worse than one
    if (d1.isOptional() && d2.isOptional()) {
      return BOTH_OPTIONAL;
    }
    if (d1.isOptional() || d2.isOptional()) {
      return ONE_OPTIONAL;
    }
    final boolean sharedVars = hasSharedVars(d1, d2);
    final boolean unsharedVars = hasUnsharedVars(d1, d2);
    final long joinCardinality;
    if (sharedVars == false) {
      // no shared vars - take the sum
      // joinCardinality = d1.getCardinality() + d2.getCardinality();
      // different approach - give preference to shared variables
      joinCardinality = NO_SHARED_VARS;
    } else {
      if (unsharedVars == false) {
        // shared vars and no unshared vars - take the min
        joinCardinality = Math.min(d1.getCardinality(), d2.getCardinality());
      } else {
        // shared vars and unshared vars - take the max
        /*
         * This modification to the join planner results in
         * significantly faster queries for the bsbm benchmark (3x - 5x
         * overall). It takes a more optimistic perspective on the
         * intersection of two statement patterns, predicting that this
         * will constraint, rather than increase, the multiplicity of
         * the solutions. However, this COULD lead to pathological cases
         * where the resulting join plan is WORSE than it would have
         * been otherwise. For example, this change produces a 3x to 5x
         * improvement in the BSBM benchmark results. However, it has a
         * negative effect on LUBM Q2.
         *
         * Update: Ok so just to go into a little detail - yesterday's
         * change means we choose the join ordering based on an
         * optimistic view of the cardinality of any particular join. If
         * you have two triple patterns that share variables but that
         * also have unshared variables, then technically the maximum
         * cardinality of the join is the maximum range count of the two
         * tails. But often the true cardinality of the join is closer
         * to the minimum range count than the maximum. So yesterday we
         * started assigning an expected cardinality for the join of the
         * minimum range count rather than the maximum. What this means
         * is that a lot of the time when those joins move toward the
         * front of the line the query will do a lot better, but
         * occasionally (LUBM 2), the query will do much much worse
         * (when the true cardinality is closer to the max range count).
         *
         * Today we put in an extra tie-breaker condition. We already
         * had one tie-breaker - if two joins have the same expected
         * cardinality we chose the one with the lower minimum range
         * count. But the new tie-breaker is that if two joins have the
         * same expected cardinality and minimum range count, we now
         * chose the one that has the minimum range count on the other
         * tail (the minimum maximum if that makes sense).
         */
        joinCardinality = Math.min(d1.getCardinality(), d2.getCardinality());
        //                    Math.max(d1.getCardinality(), d2.getCardinality());
      }
    }
    return joinCardinality;
  }

  /*
   * Get the named variables for a given tail. Is there a better way to do this?
   *
   * @param tail the tail
   * @return the named variables
   */
  protected Set<String> getVars(int tail) {
    final Set<String> vars = new HashSet<String>();
    IPredicate pred = rule.getTail(tail);
    for (int i = 0; i < pred.arity(); i++) {
      IVariableOrConstant term = pred.get(i);
      if (term.isVar()) {
        vars.add(term.getName());
      }
    }
    return vars;
  }

  /*
   * Look for shared variables.
   *
   * @param d1 the first join dimension
   * @param d2 the second join dimension
   * @return true if there are shared variables, false otherwise
   */
  protected boolean hasSharedVars(IJoinDimension d1, IJoinDimension d2) {
    for (String var : d1.getVars()) {
      if (d2.getVars().contains(var)) {
        return true;
      }
    }
    return false;
  }

  /*
   * Look for unshared variables.
   *
   * @param d1 the first join dimension
   * @param d2 the second join dimension
   * @return true if there are unshared variables, false otherwise
   */
  protected boolean hasUnsharedVars(IJoinDimension d1, IJoinDimension d2) {
    for (String var : d1.getVars()) {
      if (d2.getVars().contains(var) == false) {
        return true;
      }
    }
    for (String var : d2.getVars()) {
      if (d1.getVars().contains(var) == false) {
        return true;
      }
    }
    return false;
  }

  /*
   * A join dimension can be either a tail, or a previous join. Either way we need to know its
   * cardinality, its variables, and its tails.
   */
  private interface IJoinDimension {
    long getCardinality();

    Set<String> getVars();

    String toJoinString();

    boolean isOptional();
  }

  /*
   * A join implementation of a join dimension. The join can consist of two tails, or one tail and
   * another join. Theoretically it could be two joins as well, which might be a future optimization
   * worth thinking about.
   */
  private static class Join implements IJoinDimension {

    private final IJoinDimension d1, d2;
    private final long cardinality;
    private final Set<String> vars;

    public Join(IJoinDimension d1, IJoinDimension d2, long cardinality, Set<String> vars) {
      this.d1 = d1;
      this.d2 = d2;
      this.cardinality = cardinality;
      this.vars = vars;
    }

    public IJoinDimension getD1() {
      return d1;
    }

    public IJoinDimension getD2() {
      return d2;
    }

    public Set<String> getVars() {
      return vars;
    }

    public long getCardinality() {
      return cardinality;
    }

    public boolean isOptional() {
      return false;
    }

    public String toJoinString() {
      return d1.toJoinString() + " X " + d2.toJoinString();
    }
  }

  /** A tail implementation of a join dimension. */
  private class Tail implements IJoinDimension {

    private final int tail;
    private final long cardinality;
    private final Set<String> vars;

    public Tail(int tail, long cardinality, Set<String> vars) {
      this.tail = tail;
      this.cardinality = cardinality;
      this.vars = vars;
    }

    public int getTail() {
      return tail;
    }

    public long getCardinality() {
      return cardinality;
    }

    public Set<String> getVars() {
      return vars;
    }

    public boolean isOptional() {
      return rule.getTail(tail).isOptional();
    }

    public String toJoinString() {
      return String.valueOf(tail);
    }
  }
}
