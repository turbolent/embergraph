package org.embergraph.bop.joinGraph.fast;

import org.embergraph.bop.joinGraph.IEvaluationPlan;
import org.embergraph.bop.joinGraph.IEvaluationPlanFactory;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.IJoinNexus;

/**
 * Factory for {@link DefaultEvaluationPlan2}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DefaultEvaluationPlanFactory2 implements IEvaluationPlanFactory {

  private static final long serialVersionUID = -8582953692299135634L;

  public static final transient DefaultEvaluationPlanFactory2 INSTANCE =
      new DefaultEvaluationPlanFactory2();

  public IEvaluationPlan newPlan(IJoinNexus joinNexus, IRule rule) {

    return new DefaultEvaluationPlan2(joinNexus, rule);
  }
}
