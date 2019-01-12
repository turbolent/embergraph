package org.embergraph.relation.rule.eval;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.apache.log4j.Logger;
import org.embergraph.bop.IBindingSet;
import org.embergraph.journal.IIndexManager;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.pipeline.DistributedJoinMasterTask;
import org.embergraph.relation.rule.eval.pipeline.JoinMasterTask;
import org.embergraph.relation.rule.eval.pipeline.JoinTask;
import org.embergraph.relation.rule.eval.pipeline.LocalJoinMasterTask;
import org.embergraph.service.IEmbergraphFederation;

/*
* Default factory for tasks to execute {@link IRule}s.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class DefaultRuleTaskFactory implements IRuleTaskFactory, Externalizable {

  protected static final Logger log = Logger.getLogger(DefaultRuleTaskFactory.class);

  /** */
  private static final long serialVersionUID = -6751546625682021618L;

  //    /*
//     * Nested subquery join strategy.
  //     * <p>
  //     * Note: When used on a scale-out index, this results in the use of
  //     * {@link ClientIndexView}s and a LOT of RMI. The {@link #PIPELINE}
  //     * strategy was developed to improve join performance for scale-out.
  //     *
  //     * @see NestedSubqueryWithJoinThreadsTask
  //     */
  //    public static transient final IRuleTaskFactory SUBQUERY = new DefaultRuleTaskFactory(
  //            true/* subquery */);

  /*
   * Pipeline join strategy.
   *
   * <p>Note: This join strategy was designed for scale-out evaluation but does better for local
   * deployments as well. It starts a {@link JoinTask} per index partition on which the join must
   * read while evaluating the rule. This gives it access to the local index objects for each index
   * partition of interest. Intermediate {@link IBindingSet}s are streamed in chunks to the
   * downstream {@link JoinTask}(s). Therefore, all <em>index</em> operations are local and only the
   * intermediate {@link IBindingSet}s and the final {@link ISolution}s are moved using RMI.
   *
   * @see JoinMasterTask
   * @see LocalJoinMasterTask
   * @see DistributedJoinMasterTask
   */
  public static final transient IRuleTaskFactory PIPELINE =
      new DefaultRuleTaskFactory(false /*pipeline*/);

  private boolean subquery;

  /** De-serialization ctor. */
  public DefaultRuleTaskFactory() {}

  public DefaultRuleTaskFactory(boolean subquery) {

    this.subquery = subquery;
    //        this.subquery = false;

  }

  public IStepTask newTask(
      final IRule rule, final IJoinNexus joinNexus, final IBuffer<ISolution[]> buffer) {

    final IIndexManager indexManager = joinNexus.getIndexManager();

    //        if(subquery) {
    //
    //            if (log.isDebugEnabled())
    //                log.debug("local nested subquery joins: indexManager="
    //                        + indexManager.getClass() + ", rule=" + rule);
    //
    //            return new NestedSubqueryWithJoinThreadsTask(rule, joinNexus, buffer);
    //
    //        }

    /*
     * pipeline join.
     */

    if (indexManager instanceof IEmbergraphFederation) {

      final IEmbergraphFederation fed = (IEmbergraphFederation) indexManager;

      if (fed.isScaleOut()) {

        if (log.isDebugEnabled())
          log.debug(
              "scale-out pipeline joins: indexManager="
                  + indexManager.getClass()
                  + ", rule="
                  + rule);

        // scale-out join using a pipeline strategy.
        return new DistributedJoinMasterTask(rule, joinNexus, buffer);

      } else {

        if (log.isDebugEnabled())
          log.debug(
              "local pipeline joins: indexManager=" + indexManager.getClass() + ", rule=" + rule);

        // local joins using a pipeline strategy.
        return new LocalJoinMasterTask(rule, joinNexus, buffer);
      }
    }

    if (log.isDebugEnabled())
      log.debug("local pipeline joins: indexManager=" + indexManager.getClass() + ", rule=" + rule);

    // local joins using a pipeline strategy.
    return new LocalJoinMasterTask(rule, joinNexus, buffer);
  }

  /** The initial version. */
  private static final transient byte VERSION0 = 0;

  /** The current version. */
  private static final transient byte VERSION = VERSION0;

  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {

    final byte version = in.readByte();

    switch (version) {
      case VERSION0:
        break;
      default:
        throw new UnsupportedOperationException("Unknown version: " + version);
    }

    subquery = in.readBoolean();
  }

  public void writeExternal(ObjectOutput out) throws IOException {

    out.writeByte(VERSION);

    out.writeBoolean(subquery);
  }
}
