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
 * Created on Aug 20, 2010
 */

package org.embergraph.relation.rule.eval;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Properties;
import java.util.WeakHashMap;
import org.apache.log4j.Logger;
import org.embergraph.bop.joinGraph.IEvaluationPlanFactory;
import org.embergraph.journal.IIndexManager;
import org.embergraph.relation.accesspath.IElementFilter;

/*
 * Base implementation for {@link IJoinNexusFactory}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class AbstractJoinNexusFactory implements IJoinNexusFactory {

  /** */
  private static final long serialVersionUID = 1L;

  private static final transient Logger log = Logger.getLogger(AbstractJoinNexusFactory.class);

  private final ActionEnum action;
  private final long writeTimestamp;
  private /*final*/ long readTimestamp;
  private final Properties properties;
  private final int solutionFlags;
  // @todo should be generic as IElementFilter<ISolution<?>>
  private final IElementFilter<?> solutionFilter;
  private final IEvaluationPlanFactory evaluationPlanFactory;
  private final IRuleTaskFactory defaultRuleTaskFactory;

  public String toString() {

    final StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());

    //        sb.append("{ ruleContext=" + ruleContext);

    sb.append("{ action=").append(action);

    sb.append(", writeTime=").append(writeTimestamp);

    sb.append(", readTime=").append(readTimestamp);

    sb.append(", properties=").append(properties);

    sb.append(", solutionFlags=").append(solutionFlags);

    sb.append(", solutionFilter=")
        .append(solutionFilter == null ? "N/A" : solutionFilter.getClass().getName());

    sb.append(", planFactory=").append(evaluationPlanFactory.getClass().getName());

    sb.append(", defaultRuleTaskFactory=").append(defaultRuleTaskFactory.getClass().getName());

    // allow extension by derived classes.
    toString(sb);

    sb.append("}");

    return sb.toString();
  }

  /*
   * Allows extension of {@link #toString()} by derived classes.
   *
   * @param sb
   */
  protected void toString(final StringBuilder sb) {}

  /*
   * @param action Indicates whether this is a Query, Insert, or Delete operation.
   * @param writeTimestamp The timestamp of the relation view(s) using to write on the {@link
   *     IMutableRelation}s (ignored if you are not execution mutation programs).
   * @param readTimestamp The timestamp of the relation view(s) used to read from the access paths.
   * @param solutionFlags Flags controlling the behavior of {@link #newSolution(IRule,
   *     IBindingSet)}.
   * @param solutionFilter An optional filter that will be applied to keep matching elements out of
   *     the {@link IBuffer} for Query or Mutation operations.
   * @param evaluationPlanFactory The factory used to generate {@link IEvaluationPlan}s for {@link
   *     IRule}s.
   * @param defaultRuleTaskFactory The factory that will be used to generate the {@link IStepTask}
   *     to execute an {@link IRule} unless the {@link IRule} explicitly specifies a factory object
   *     using {@link IRule#getTaskFactory()}.
   */
  protected AbstractJoinNexusFactory(
      final ActionEnum action,
      final long writeTimestamp,
      final long readTimestamp,
      final Properties properties,
      final int solutionFlags,
      final IElementFilter<?> solutionFilter,
      final IEvaluationPlanFactory evaluationPlanFactory,
      final IRuleTaskFactory defaultRuleTaskFactory) {

    if (action == null) throw new IllegalArgumentException();

    if (evaluationPlanFactory == null) throw new IllegalArgumentException();

    this.action = action;

    this.writeTimestamp = writeTimestamp;

    this.readTimestamp = readTimestamp;

    this.properties = properties;

    this.solutionFlags = solutionFlags;

    this.solutionFilter = solutionFilter;

    this.evaluationPlanFactory = evaluationPlanFactory;

    this.defaultRuleTaskFactory = defaultRuleTaskFactory;

    joinNexusCache = new WeakHashMap<>();
  }

  // @todo assumes one "central" relation (e.g., SPORelation)?
  public IJoinNexus newInstance(final IIndexManager indexManager) {

    if (indexManager == null) throw new IllegalArgumentException();

    synchronized (joinNexusCache) {
      final WeakReference<IJoinNexus> ref = joinNexusCache.get(indexManager);

      IJoinNexus joinNexus = ref == null ? null : ref.get();

      if (joinNexus == null) {

        joinNexus = newJoinNexus(indexManager);

        joinNexusCache.put(indexManager, new WeakReference<>(joinNexus));
      }

      return joinNexus;
    }
  }

  /*
   * Factory for {@link IJoinNexus} instances used by {@link #newInstance(IIndexManager)} as past of
   * its singleton pattern.
   */
  protected abstract IJoinNexus newJoinNexus(IIndexManager indexManager);

  private transient WeakHashMap<IIndexManager, WeakReference<IJoinNexus>> joinNexusCache;

  public final ActionEnum getAction() {

    return action;
  }

  public final Properties getProperties() {

    return properties;
  }

  public final long getReadTimestamp() {

    return readTimestamp;
  }

  public final void setReadTimestamp(final long readTimestamp) {

    if (this.readTimestamp == readTimestamp) {

      // NOP.
      return;
    }

    synchronized (joinNexusCache) {

      // discard cache since advancing the readTimestamp.
      joinNexusCache.clear();

      this.readTimestamp = readTimestamp;

      if (log.isInfoEnabled()) log.info("readTimestamp: " + readTimestamp);
    }
  }

  public final long getWriteTimestamp() {

    return writeTimestamp;
  }

  public final int getSolutionFlags() {

    return solutionFlags;
  }

  public final IElementFilter<?> getSolutionFilter() {

    return solutionFilter;
  }

  public final IEvaluationPlanFactory getEvaluationPlanFactory() {

    return evaluationPlanFactory;
  }

  public final IRuleTaskFactory getDefaultRuleTaskFactory() {

    return defaultRuleTaskFactory;
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    /*
     * Note: Must be explicitly allocated when de-serialized.
     */

    joinNexusCache = new WeakHashMap<>();

    in.defaultReadObject();
  }
}
