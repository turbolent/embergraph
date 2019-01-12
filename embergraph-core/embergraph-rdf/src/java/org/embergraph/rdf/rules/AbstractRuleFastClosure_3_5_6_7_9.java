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
package org.embergraph.rdf.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.Var;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.spo.SPOPredicate;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.relation.IRelation;
import org.embergraph.relation.RelationFusedView;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.locator.IResourceLocator;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.QueryOptions;
import org.embergraph.relation.rule.Rule;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.IRuleTaskFactory;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.relation.rule.eval.IStepTask;
import org.embergraph.relation.rule.eval.RuleStats;
import org.embergraph.striterator.IChunkedOrderedIterator;

/*
 * Rule used in steps 3, 5, 6, 7, and 9 of the fast forward closure program.
 *
 * <pre>
 *    (?x, {P}, ?y) -&gt; (?x, propertyId, ?y)
 * </pre>
 *
 * where <code>{P}</code> is the closure of the subproperties of one of the FIVE (5) reserved
 * keywords:
 *
 * <ul>
 *   <li><code>rdfs:subPropertyOf</code>
 *   <li><code>rdfs:subClassOf</code>
 *   <li><code>rdfs:domain</code>
 *   <li><code>rdfs:range</code>
 *   <li><code>rdf:type</code>
 * </ul>
 *
 * The caller MUST define an {@link IRuleTaskFactory} that provides a concrete implementation of
 * {@link FastClosureRuleTask} which knows how to compute "{P}" when they instantiate this rule.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractRuleFastClosure_3_5_6_7_9 extends Rule {

  // private final Set<IV> P;

  //    private final IConstant<IV> rdfsSubPropertyOf;

  //    private final IConstant<IV> propertyId;

  // private final Var x, y, SetP;

  /*
   * @param propertyId
   * @param taskFactory An implementation returning a concrete instance of {@link
   *     FastClosureRuleTask}.
   */
  public AbstractRuleFastClosure_3_5_6_7_9(
      final String name,
      final String relationName,
      final IConstant<IV> rdfsSubPropertyOf,
      final IConstant<IV> propertyId,
      final IRuleTaskFactory taskFactory
      // , Set<IV> P
      ) {

    super(
        name,
        new SPOPredicate(relationName, var("x"), propertyId, var("y")),
        new SPOPredicate[] {new SPOPredicate(relationName, var("x"), var("{P}"), var("y"))},
        QueryOptions.NONE,
        null, // constraints
        null, // constants
        taskFactory);

    if (rdfsSubPropertyOf == null) throw new IllegalArgumentException();

    if (propertyId == null) throw new IllegalArgumentException();

    // this.P = P;

    //        this.rdfsSubPropertyOf = rdfsSubPropertyOf;
    //
    //        this.propertyId = propertyId;

    // this.x = var("x");
    // this.y = var("y");
    // this.SetP = var("{P}");

  }

  /*
   * Custom rule execution task. You must implement {@link #getSet()}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  protected abstract static class FastClosureRuleTask implements IStepTask {

    private final String database;

    private final String focusStore;

    private final IRule rule;

    private final IJoinNexus joinNexus; // Note: Not serializable.

    private final IBuffer<ISolution[]> buffer; // Note: Not serializable.

    // private final Set<IV> P;

    protected final IConstant<IV> rdfsSubPropertyOf;

    protected final IConstant<IV> propertyId;

    /** @see #getView() */
    private transient IRelation<ISPO> view = null;

    /*
     * <code>(?x, {P}, ?y) -> (?x, propertyId, ?y)</code> Note: Both the database and the (optional)
     * focusStore relation names MUST be declared for these rules. While the rule only declares a
     * single tail predicate, there is a "hidden" query based on the [database + focusStore] fused
     * view that populates the P,D,C,R, or T Set which is an input to the custom evaluation of the
     * rule.
     *
     * @param database Name of the database relation (required).
     * @param focusStore Optional name of the focusStore relation (may be null). When non-<code>null
     *     </code>, this is used to query the fused view of the [database + focusStore] in {@link
     *     FastClosureRuleTask#getView()}.
     * @param rule The rule.
     * @param joinNexus
     * @param buffer A buffer used to accumulate chunks of entailments.
     * @param rdfsSubPropertyOf The {@link Constant} corresponding to the term identifier for <code>
     *     rdfs:subPropertyOf</code>.
     * @param propertyId The propertyId to be used in the assertions.
     */
    public FastClosureRuleTask(
        String database,
        String focusStore,
        IRule rule,
        IJoinNexus joinNexus,
        IBuffer<ISolution[]> buffer,
        // Set<IV> P,
        IConstant<IV> rdfsSubPropertyOf,
        IConstant<IV> propertyId) {

      if (database == null) throw new IllegalArgumentException();

      if (rule == null) throw new IllegalArgumentException();

      if (joinNexus == null) throw new IllegalArgumentException();

      if (buffer == null) throw new IllegalArgumentException();

      // if (P == null)
      // throw new IllegalArgumentException();

      if (rdfsSubPropertyOf == null) throw new IllegalArgumentException();

      if (propertyId == null) throw new IllegalArgumentException();

      this.database = database;

      this.focusStore = focusStore; // MAY be null.

      this.rule = rule;

      this.joinNexus = joinNexus;

      this.buffer = buffer;

      // this.P = P;

      this.rdfsSubPropertyOf = rdfsSubPropertyOf;

      this.propertyId = propertyId;
    }

    public RuleStats call() {

      if (INFO) log.info("running: rule=" + rule.getName() + ", propertyId=" + propertyId);

      final RuleStats stats = joinNexus.getRuleStatisticsFactory().newInstance(rule);

      final long begin = System.currentTimeMillis();

      /*
       * Note: Since this task is always applied to a single tail rule,
       * the {@link TMUtility} rewrite of the rule will always read from
       * the focusStore alone. This makes the choice of the relation on
       * which to read easy - just read on whichever relation is specified
       * for tail[0].
       */
      //            final String relationName = rule.getHead().getOnlyRelationName();
      //
      //            final long timestamp = joinNexus.getReadTimestamp(relationName);
      //
      //            final SPORelation relation = (SPORelation) joinNexus
      //                    .getIndexManager().getResourceLocator().locate(
      //                            relationName, timestamp);

      final SPORelation relation = (SPORelation) joinNexus.getTailRelationView(rule.getTail(0));

      /*
       * Query for the set {P} rather than requiring it as an input.
       *
       * Note: This is really aligning relations with different
       * arity/shape (long[1] vs SPO)
       *
       * @todo Make {P} a chunked iterator, proceed by chunk, and put each
       * chunk into ascending Long[] order. However the methods that
       * compute {P} are a custom closure operator and they fix point {P}
       * in memory. To generalize with a chunked iterator there would need
       * to be a backing EmbergraphLongSet and that will be less efficient
       * unless the property hierarchy scale is very large.
       */
      final IV[] a = getSortedArray(getSet());

      /*
       * For each p in the chunk.
       *
       * @todo execute subqueries in parallel against shared thread pool.
       */
      for (IV p : a) {

        if (IVUtility.equals(p, propertyId.get())) {

          /*
           * The rule refuses to consider triple patterns where the
           * predicate for the subquery is the predicate for the
           * generated entailments since the support would then entail
           * itself.
           */

          continue;
        }

        stats.subqueryCount[0]++;

        final IAccessPath<ISPO> accessPath = relation.getAccessPath(null, p, null);

        final IChunkedOrderedIterator<ISPO> itr2 = accessPath.iterator();

        // ISPOIterator itr2 = (state.focusStore == null ?
        // state.database
        // .getAccessPath(NULL, p, NULL).iterator()
        // : state.focusStore.getAccessPath(NULL, p, NULL)
        // .iterator());

        final IBindingSet bindingSet = joinNexus.newBindingSet(rule);

        try {

          while (itr2.hasNext()) {

            final ISPO[] chunk = itr2.nextChunk(SPOKeyOrder.POS);

            stats.chunkCount[0]++;

            stats.elementCount[0] += chunk.length;

            if (DEBUG) {

              log.debug("stmts1: chunk=" + chunk.length + "\n" + Arrays.toString(chunk));
            }

            final IBuffer<ISolution> tmp = joinNexus.newUnsynchronizedBuffer(buffer, chunk.length);

            for (ISPO spo : chunk) {

              /*
               * Note: since P includes rdfs:subPropertyOf (as
               * well as all of the sub-properties of
               * rdfs:subPropertyOf) there are going to be some
               * axioms in here that we really do not need to
               * reassert and generally some explicit statements
               * as well.
               *
               * @todo so, filter out explicit and axioms?
               *
               * @todo clone the bindingSet first?
               */

              assert spo.p().equals(p) : "spo.p=" + spo.p() + ", p=" + p;

              if (joinNexus.bind(rule, 0, spo, bindingSet)) {

                //                            joinNexus.copyValues(spo, rule.getTail(0),
                //                                    bindingSet);
                //
                //                            if (rule.isConsistent(bindingSet)) {

                tmp.add(joinNexus.newSolution(rule, bindingSet));

                stats.solutionCount.incrementAndGet();
              }
            } // next spo in chunk.

            // flush onto the chunked solution buffer.
            tmp.flush();
          } // while(itr2)

        } finally {

          itr2.close();
        }
      } // next p in {P}

      stats.elapsed += System.currentTimeMillis() - begin;

      return stats;
    }

    /*
     * Convert a {@link Set} of term identifiers into a sorted array of term identifiers.
     *
     * <p>Note: When issuing multiple queries against the database, it is generally faster to issue
     * those queries in key order.
     *
     * @return The sorted term identifiers.
     */
    protected IV[] getSortedArray(Set<IV> ivs) {

      final int n = ivs.size();

      final IV[] a = new IV[n];

      int i = 0;

      for (IV iv : ivs) {

        a[i++] = iv;
      }

      Arrays.sort(a);

      return a;
    }

    /*
     * Return the {@link IRelation} (or {@link RelationFusedView}) used by the {@link #getSet()}
     * impls for their {@link IAccessPath}s.
     */
    protected synchronized IRelation<ISPO> getView() {

      if (view == null) {

        /*
         * Setup the [database] or [database + focusStore] view used to
         * compute the closure.
         */
        final IResourceLocator resourceLocator = joinNexus.getIndexManager().getResourceLocator();

        if (focusStore == null) {

          final long timestamp = joinNexus.getReadTimestamp(/*database*/ );

          return (IRelation<ISPO>) resourceLocator.locate(database, timestamp);

        } else {

          final long timestamp0 = joinNexus.getReadTimestamp(/*database*/ );

          final long timestamp1 = joinNexus.getReadTimestamp(/*focusStore*/ );

          return new RelationFusedView<ISPO>(
                  (IRelation<ISPO>) resourceLocator.locate(database, timestamp0),
                  (IRelation<ISPO>) resourceLocator.locate(focusStore, timestamp1))
              .init();
        }
        // final IAccessPath accessPath = (focusStore == null
        // ? database.getAccessPath(NULL, p, NULL)
        // : new AccessPathFusedView(focusStore
        // .getAccessPath(NULL, p, NULL),
        // database.getAccessPath(NULL, p, NULL)
        // ));

      }

      return view;
    }

    /*
     * Return the set of term identifiers that will be processed by the rule. When the closure is
     * being computed for truth maintenance the implementation MUST read from the
     * [database+focusStore] fused view. Otherwise it reads from the database.
     *
     * <p>Note: The subclass need only invoke {@link #getSubProperties()} or {@link
     * #getSubPropertiesOf(IConstant)} as appropriate for the rule.
     *
     * @return The set.
     */
    protected abstract Set<IV> getSet();

    /*
     * Delegates to {@link SubPropertyClosureTask}
     *
     * @return The closure.
     */
    protected Set<IV> getSubProperties() {

      return new SubPropertyClosureTask(getView(), rdfsSubPropertyOf).call();
    }

    /*
     * Delegates to {@link SubPropertiesOfClosureTask}
     *
     * @param propertyId The property of interest.
     * @return The closure.
     */
    protected Set<IV> getSubPropertiesOf(IConstant<IV> propertyId) {

      return new SubPropertiesOfClosureTask(getView(), rdfsSubPropertyOf, propertyId).call();
    }
  } // FastClosureRuleTask

  /*
   * Computes the set of possible sub properties of rdfs:subPropertyOf (<code>P</code>). This is
   * used by step <code>2</code> in {@link RDFSVocabulary#fastForwardClosure()}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class SubPropertyClosureTask implements Callable<Set<IV>> {

    protected static final Logger log = Logger.getLogger(SubPropertyClosureTask.class);

    private final IRelation<ISPO> view; // Note: Not serializable.
    private final IConstant<IV> rdfsSubPropertyOf;

    public SubPropertyClosureTask(IRelation<ISPO> view, IConstant<IV> rdfsSubPropertyOf) {

      if (view == null) throw new IllegalArgumentException();

      if (rdfsSubPropertyOf == null) throw new IllegalArgumentException();

      this.view = view;

      this.rdfsSubPropertyOf = rdfsSubPropertyOf;
    }

    public Set<IV> call() {

      return getSubProperties();
    }

    /*
     * Compute the closure.
     *
     * @return A set containing the term identifiers for the members of P.
     */
    public Set<IV> getSubProperties() {

      final Set<IV> P = new HashSet<IV>();

      P.add(rdfsSubPropertyOf.get());

      /*
       * query := (?x, P, P), adding new members to P until P reaches fix
       * point.
       */
      {
        int nbefore;
        int nafter = 0;
        int nrounds = 0;

        final Set<IV> tmp = new HashSet<IV>();

        do {

          nbefore = P.size();

          tmp.clear();

          /*
           * query := (?x, p, ?y ) for each p in P, filter ?y element
           * of P.
           */

          for (IV p : P) {

            final SPOPredicate pred =
                new SPOPredicate(
                    "view", // @todo the label here is ignored, but should be the ordered names of
                    // the relations in the view.
                    Var.var("x"),
                    new Constant<IV>(p),
                    Var.var("y"));

            final IAccessPath<ISPO> accessPath = view.getAccessPath(pred);

            //                        final IAccessPath accessPath = (focusStore == null
            //                        ? database.getAccessPath(NULL, p, NULL)
            //                                : new AccessPathFusedView(focusStore
            //                                        .getAccessPath(NULL, p, NULL),
            //                                        database.getAccessPath(NULL, p, NULL)
            //                                ));

            final IChunkedOrderedIterator<ISPO> itr = accessPath.iterator();

            try {

              while (itr.hasNext()) {

                final ISPO[] stmts = itr.nextChunk();

                for (ISPO stmt : stmts) {

                  if (P.contains(stmt.o())) {

                    tmp.add(stmt.s());
                  }
                }
              }
            } finally {

              itr.close();
            }
          }

          P.addAll(tmp);

          nafter = P.size();

          nrounds++;

        } while (nafter > nbefore);
      }

      //            if (log.isDebugEnabled()) {
      //
      //                Set<String> terms = new HashSet<String>();
      //
      //                for (Long id : P) {
      //
      //                    terms.add(database.toString(id));
      //
      //                }
      //
      //                log.debug("P: " + terms);
      //
      //            }

      return P;
    }
  }

  /*
   * Query the <i>database</i> for the sub properties of a given property.
   *
   * <p>Pre-condition: The closure of <code>rdfs:subPropertyOf</code> has been asserted on the
   * database.
   *
   * @param p The constant corresponding to the term identifier for the property whose
   *     sub-properties will be obtain.
   * @return A set containing the term identifiers for the sub properties of <i>p</i>.
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @version $Id$
   */
  public static class SubPropertiesOfClosureTask implements Callable<Set<IV>> {

    protected static final Logger log = Logger.getLogger(SubPropertyClosureTask.class);

    private final IRelation<ISPO> view; // Note: Not serializable.
    private final IConstant<IV> rdfsSubPropertyOf;
    private final IConstant<IV> p;

    public SubPropertiesOfClosureTask(
        IRelation<ISPO> view, IConstant<IV> rdfsSubPropertyOf, IConstant<IV> p) {

      if (view == null) throw new IllegalArgumentException();

      if (rdfsSubPropertyOf == null) throw new IllegalArgumentException();

      if (p == null) throw new IllegalArgumentException();

      this.view = view;

      this.rdfsSubPropertyOf = rdfsSubPropertyOf;

      this.p = p;
    }

    public Set<IV> call() {

      return getSubPropertiesOf(p);
    }

    /*
     * Compute the closure.
     *
     * @param p The property of interest.
     * @return The closure.
     */
    public Set<IV> getSubPropertiesOf(IConstant<IV> p) {

      final SPOPredicate pred = new SPOPredicate("view", Var.var("x"), rdfsSubPropertyOf, p);

      final IAccessPath<ISPO> accessPath = view.getAccessPath(pred);

      //            final IAccessPath accessPath =
      //            (focusStore == null
      //            ? database.getAccessPath(NULL/* x */, rdfsSubPropertyOf.get(), p)
      //                    : new AccessPathFusedView(
      //                            focusStore.getAccessPath(NULL/* x */,
      //                                    rdfsSubPropertyOf.get(), p),
      //                            database.getAccessPath(NULL/* x */,
      //                                    rdfsSubPropertyOf.get(), p)
      //                    ));

      //            if (log.isDebugEnabled()) {
      //
      //                log.debug("p=" + database.toString(p));
      //
      //            }

      final Set<IV> tmp = new HashSet<IV>();

      /*
       * query := (?x, rdfs:subPropertyOf, p).
       *
       * Distinct ?x are gathered in [tmp].
       *
       * Note: This query is two-bound on the POS index.
       */

      final IChunkedOrderedIterator<ISPO> itr = accessPath.iterator();

      try {

        while (itr.hasNext()) {

          final ISPO[] stmts = itr.nextChunk();

          for (ISPO spo : stmts) {

            boolean added = tmp.add(spo.s());

            if (DEBUG) log.debug(spo.toString(/* database */ ) + ", added subject=" + added);
          }
        }

      } finally {

        itr.close();
      }

      //            if (log.isDebugEnabled()) {
      //
      //                Set<String> terms = new HashSet<String>();
      //
      //                for (Long id : tmp) {
      //
      //                    terms.add(database.toString(id));
      //
      //                }
      //
      //                log.debug("sub properties: " + terms);
      //
      //            }

      return tmp;
    }
  }
}
