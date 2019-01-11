/**

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
 * Created on Oct 3, 2007
 */

package org.embergraph.concurrent;

import java.util.Arrays;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;


/**
 * Class encapsulates handshaking with the {@link LockManager} for an operation
 * requiring exclusive access to one or more resources and that are willing to
 * pre-declare their resource requirements.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * 
 * @param R
 *            The generic type of the object that identifies a resource for the
 *            purposes of the locking system. This is typically the <i>name</i>
 *            of an index.
 * @param T
 *            The generic type of the return value of the delegate
 *            {@link Callable}.
 */
public class LockManagerTask<R extends Comparable<R>,T> implements
        Callable<T> {

    protected static final Logger log = Logger.getLogger(LockManagerTask.class);
    
    protected static final boolean INFO = log.isInfoEnabled();
    
    private final LockManager<R> lockManager;

    private final R[] resource;

    private final Callable<T> target;
    
    private int maxLockTries = 1;

    private long lockTimeout = 0L;

    /**
     * The {@link LockManager}.
     */
    public LockManager<R> getLockManager() {
        
        return lockManager;
        
    }
    
    /**
     * The resource(s) that are pre-declared by the task. {@link #call()} will
     * ensure that the task as a lock on these resources before it invokes
     * {@link #run()} to execution the task.
     */
    public R[] getResource() {
        
        return resource;
        
    }
    
    public int setMaxLockTries(final int newValue) {

        if (newValue < 1)
            throw new IllegalArgumentException();

        int t = this.maxLockTries;

        this.maxLockTries = newValue;

        return t;

    }

    /**
     * The elapsed nanoseconds the task waited to acquire its locks.
     */
    public long getLockLatency() {
        
        return nanoTime_lockLatency;
        
    }
    private long nanoTime_lockLatency;
    
    /**
     * The maximum #of times that the task will attempt to acquire its locks
     * (positive integer).
     */
    public int getMaxLockTries() {

        return maxLockTries;

    }

    public long setLockTimeout(final long newValue) {

        final long t = this.lockTimeout;

        this.lockTimeout = newValue;

        return t;

    }

    /**
     * The timeout (milliseconds) or ZERO (0L) for an infinite timeout.
     */
    public long getLockTimeout() {

        return lockTimeout;

    }

    /**
     * 
     * @param lockManager
     *            The lock manager.
     * 
     * @param resource
     *            The resource(s) to be locked.
     * 
     * @param target
     *            The {@link Runnable} target that will be invoked iff the locks
     *            are successfully acquired.
     */
    public LockManagerTask(final LockManager<R> lockManager,
            final R[] resource, final Callable<T> target) {

        if (lockManager == null)
            throw new NullPointerException();

        if (resource == null)
            throw new NullPointerException();

//        if (resource.length == 0)
//            throw new IllegalArgumentException();
        
        for (int i = 0; i < resource.length; i++) {

            if (resource[i] == null)
                throw new NullPointerException();

        }

        if (target == null)
            throw new NullPointerException();
        
        this.lockManager = lockManager;

        this.resource = resource;
        
        this.target = target;

    }

    /**
     * Attempt to acquire locks on resources required by the task.
     * <p>
     * Up to {@link #getMaxLockTries()} attempts will be made.
     * 
     * @exception DeadlockException
     *                if the locks could not be acquired (last exception
     *                encountered only).
     * @exception TimeoutException
     *                if the locks could not be acquired (last exception
     *                encountered only).
     * @exception InterruptedException
     *                if the current thread is interrupted.
     */
    private void acquireLocks() throws Exception {

        lockManager.nwaiting.incrementAndGet();

        try {

            for (int i = 0; i < maxLockTries; i++) {

                if (Thread.interrupted()) {

                    throw new InterruptedException();

                }

                try {

                    // Request resource lock(s).

                    lockManager.lock(resource, lockTimeout);

                    return;

                } catch (DeadlockException ex) {

                    // Count deadlocks.

                    lockManager.ndeadlock.incrementAndGet();

                } catch (TimeoutException ex) {

                    // Count timeouts.

                    lockManager.ntimeout.incrementAndGet();

                }

                /*
                 * Release any locks granted since we did not get all of the
                 * locks that we were seeking.
                 */

                lockManager.releaseLocks(true/* waiting */);

            }

        } finally {

            lockManager.nwaiting.decrementAndGet();

        }

    }

    /**
     * Acquires pre-declared locks and then runs the operation identified to the
     * constructor.
     * 
     * @return <code>null</code>
     * 
     * @throws Exception
     *             if something goes wrong.
     * @throws InterruptedException
     *             if the current thread is interrupted.
     */
    final public T call() throws Exception {

        final long nanoTime_beforeLock = System.nanoTime();
        
        // start.
        lockManager.didStart(this);

        try {

            /*
             * Acquire pre-declared locks.
             * 
             * Note: in order to refactor this class so that operations do
             * NOT have to predeclare their locks you need to make sure that
             * the handshaking with the {@link LockManager} correctly
             * invokes
             * {@link LockManager#didAbort(Callable, Throwable, boolean)}
             * with the appropriate value for the "waiting" parameter
             * depending on whether or not the transaction is currently
             * waiting. This is more tricky if the operation is able to
             * request additional locks in run() since we need to either
             * carefully differentiate the context or just assume that the
             * operation is waiting unless it has completed successfully.
             */

            acquireLocks();

            if(INFO)
                log.info("Acquired locks");

        } catch (Exception ex) {

            // abort.
            lockManager.didAbort(this, ex, true /*waiting*/);

            // rethrow (do not masquerade the exception).
            throw ex;

        } catch (Throwable t) {

            // abort.
            lockManager.didAbort(this, t, true /*waiting*/);

            // rethrow (masquerade the exception).
            throw new RuntimeException(t);

        } finally {

            /*
             * The amount of time that the task waited to acquire its locks.
             */
            nanoTime_lockLatency = System.nanoTime() - nanoTime_beforeLock;
            
        }

        /*
         * Run the task now that we have the necessary lock(s).
         */
        try {

            if(INFO) log.info(toString() + ": run - start");

            if(Thread.interrupted()) {
                
                throw new InterruptedException();
                
            }
            
            /*
             * Note: "running" means in "call()" for the delegate task.
             */
            final long nrunning = lockManager.nrunning.incrementAndGet();

            // Note: not really atomic and hence only approximate.
            lockManager.maxrunning.set(Math.max(lockManager.maxrunning.get(),
                    nrunning));

            final T ret;
            try {

                ret = target.call();

            } finally {

                // done "running".
                lockManager.nrunning.decrementAndGet();

            }

            if(INFO) log.info(toString() + ": run - end");

            lockManager.didSucceed(this);

            return ret;

        } catch (Throwable t) {

            if (t instanceof HorridTaskDeath) {

                // An expected error.

                lockManager.didAbort(this, t, false /* NOT waiting */);

                throw (HorridTaskDeath) t;

            }

            // An unexpected error.

//            if(log.getLevel().isGreaterOrEqual(Level.ERROR)) {
                
//                log.error("Problem running task: " + this, t);
                
//            }

            lockManager.didAbort(this, t, false /* NOT waiting */);

            if (t instanceof Exception) {

                // Do not masquerade.
                throw (Exception) t;

            }

            // masquerade.
            throw new RuntimeException(t);

        }

    }

    public String toString() {

        return super.toString() + " resources=" + Arrays.toString(resource);

    }

}
