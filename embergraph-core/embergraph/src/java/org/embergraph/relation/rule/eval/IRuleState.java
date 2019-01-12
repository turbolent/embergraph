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
 * Created on Nov 4, 2008
 */

package org.embergraph.relation.rule.eval;

import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariable;
import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.Rule;
import org.embergraph.striterator.IKeyOrder;

/**
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface IRuleState {

  /** The {@link IRule} being evaluated. */
  IRule getRule();

  /** The evaluation plan. */
  IEvaluationPlan getPlan();

  /**
   * The #of unbound variables for the predicates in the tail of the {@link Rule} for the {@link
   * #getPlan() evaluation plan}. The array is correlated with the predicates index in the tail of
   * the rule NOT with its evaluation order.
   */
  int[] getNVars();

  /**
   * An array of the {@link IKeyOrder} that will be used for each predicate in the tail of the rule.
   * The array is correlated with the predicates index in the tail of the rule NOT with its
   * evaluation order.
   *
   * <p>Note: The fully qualified index name for a given predicate is the name of the relation for
   * that predicate plus {@link IKeyOrder#getIndexName()}.
   */
  IKeyOrder[] getKeyOrder();

  /**
   * A list of variables required for each tail, by tailIndex. Used to filter downstream variable
   * binding sets.
   */
  IVariable[][] getRequiredVars();

  /** Externalizes the rule and the evaluation order. */
  String toString();

  /**
   * Shows the bindings (if given), the computed evaluation order, and the computed {@link
   * IKeyOrder} for each {@link IPredicate} in the rule.
   *
   * @param bindingSet When non-<code>null</code>, the current variable bindings will be displayed.
   *     Otherwise, the names of variables will be displayed rather than their bindings.
   */
  String toString(IBindingSet bindingSet);
}
