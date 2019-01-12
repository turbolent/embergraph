package org.embergraph.relation.rule.eval.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.embergraph.bop.IBindingSet;
import org.embergraph.btree.UnisolatedReadWriteIndex;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.relation.accesspath.IAsynchronousIterator;
import org.embergraph.relation.accesspath.IBuffer;
import org.embergraph.relation.rule.IRule;
import org.embergraph.relation.rule.eval.IJoinNexus;
import org.embergraph.relation.rule.eval.ISolution;
import org.embergraph.service.IEmbergraphFederation;

/**
 * Implementation for local join execution on a {@link Journal}.
 * <p>
 * Note: Just like a nested subquery join, when used for mutation this must
 * read and write on the {@link ITx#UNISOLATED} indices and an
 * {@link UnisolatedReadWriteIndex} will be used to serialize exclusive access
 * to the unisolated index for writers while allowing readers concurrent access.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: LocalJoinMasterTask.java 2625 2010-04-16 21:11:44Z mrpersonick
 *          $
 */
public class LocalJoinMasterTask extends JoinMasterTask {

    /**
     * @param rule
     * @param joinNexus
     * @param buffer
     */
    public LocalJoinMasterTask(final IRule rule, final IJoinNexus joinNexus,
            final IBuffer<ISolution[]> buffer) {

        super(rule, joinNexus, buffer);

        if ((joinNexus.getIndexManager() instanceof IEmbergraphFederation<?>)
                && (((IEmbergraphFederation<?>) joinNexus.getIndexManager())
                        .isScaleOut())) {
            
            /*
             * This implementation can not be used with a scale-out
             * federation.
             */
            
            throw new UnsupportedOperationException();
            
        }
        
    }

    /**
     * Applies an initial {@link IBindingSet} to the first join dimension.
     * Intermediate {@link IBindingSet}s will propagate to each join
     * dimension. The final {@link IBindingSet}s will be generated by the
     * last join dimension and written on the {@link #getSolutionBuffer()}.
     * 
     * @return The {@link Future} for the {@link LocalJoinTask} for each
     *         join dimension.
     */
    @Override
    protected List<Future<Void>> start() throws Exception {

        // source for each join dimension.
        final IAsynchronousIterator<IBindingSet[]>[] sources = new IAsynchronousIterator[tailCount];

        // source for the 1st join dimension.
        sources[0] = newBindingSetIterator(joinNexus.newBindingSet(rule));

        // Future for each JoinTask.
        final List<Future<Void>> futures = new ArrayList<Future<Void>>(tailCount); 
        
        // the previous JoinTask and null iff this is the first join dimension.
        LocalJoinTask priorJoinTask = null;

        // for each predicate in the evaluate order.
        for (int orderIndex = 0; orderIndex < tailCount; orderIndex++) {

            // true iff this is the last JOIN in the evaluation order.
            final boolean lastJoin = orderIndex + 1 == tailCount;

            // source for this join dimension.
            final IAsynchronousIterator<IBindingSet[]> src = sources[orderIndex];
            
            assert src != null : "No source: orderIndex=" + orderIndex
                    + ", tailCount=" + tailCount + ", rule=" + rule;
            
            // create the local join task.
            final LocalJoinTask joinTask = new LocalJoinTask(/*indexName, */rule,
                    joinNexus, order, orderIndex, this/* master */,
                    masterUUID, src, getSolutionBuffer(), 
                    ruleState.getRequiredVars());

            if (!lastJoin) {

                // source for the next join dimension.
                sources[orderIndex + 1] = joinTask.syncBuffer.iterator();

            }

			/*
			 * Submit the JoinTask.
			 * 
			 * When the JoinTask for the 1st join dimension executes it will
			 * consume the [initialBindingSet]. That bindingSet will be used to
			 * obtain the first access path and merged with the elements drawn
			 * from that access path. Intermediate bindingSets will be
			 * propagated to the JoinTask for the next predicate in the
			 * evaluation order.
			 * 
			 * Note: This creates the FutureTasks in one pass and sets them on
			 * the various references. It then goes through a second pass to
			 * start the tasks running. This way the [priorJoinTask] (if any)
			 * will always have its sinkFuture set before it begins to execute.
			 */

			final FutureTask<Void> ft = new FutureTask<Void>(joinTask);

			// Save reference to the Future.
			futures.add(ft);

			// Set the Future on the BlockingBuffer.
			if (!lastJoin) {

				joinTask.syncBuffer.setFuture(ft);

			}

			// Set the Future on the JoinTask for the previous join dimension.
			if (priorJoinTask != null) {

				priorJoinTask.setSinkFuture(ft);

			}
            
            priorJoinTask = joinTask;
            
        }            

        // The JoinTasks will be run on this service.
        final ExecutorService executorService = joinNexus.getIndexManager().getExecutorService();
        
        // Submit the JoinTask for execution.
        for(Future<Void> f : futures) {

			executorService.execute((FutureTask<Void>) f);

        }

        return futures;
        
    }
    
}