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
 * Created on Feb 2, 2010
 */

package org.embergraph.util.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Pattern using a {@link FutureTask} to force synchronization only on tasks
 * waiting for the same computation. This is based on Java Concurrency in
 * Practice, page 108.
 * <p>
 * Concrete implementations MUST provide a means to limit the size of the
 * {@link #cache}. Because the {@link #cache} is unbounded, it will just take on
 * more and more memory as new results are computed. This could be handled by
 * imposing a capacity constraint on the cache, by the use of
 * {@link WeakReference}s if they can be made to stand for the specific
 * computation to be performed, or by the use of a timeout on the cache entries.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
abstract public class Memoizer<A, V> implements Computable<A, V> {

    /**
     * Cache accumulates results.
     */
    protected final ConcurrentMap<A, Future<V>> cache = new ConcurrentHashMap<A, Future<V>>();

    /**
     * The method which computes a result (V) from an argument (A).
     */
    private final Computable<A, V> c;

    public Memoizer(final Computable<A, V> c) {
        this.c = c;
    }

    public V compute(final A arg) throws InterruptedException {
        while (true) {
            Future<V> f = cache.get(arg);
            boolean willRun = false;
            if (f == null) {
                final Callable<V> eval = new Callable<V>() {
                    public V call() throws InterruptedException {
                        return c.compute(arg);
                    }
                };
                final FutureTask<V> ft = new FutureTask<V>(eval);
                f = cache.putIfAbsent(arg, ft);
                if (f == null) {
                    willRun = true; // Note: MUST set before running!
                    f = ft;
                    /*
                     * Note: MAY throw out RuntimeException but WILL set
                     * exception on FutureTask. Thus the thread which invokes
                     * ft.run() will have any uncaught exception tossed out of
                     * ft.run() while other threads will see that exception
                     * wrapped as an ExecutionException when they call f.get().
                     */
                    ft.run(); // call to c.compute() happens here.
                }
            }
            try {
                return f.get();
            } catch (CancellationException e) {
                // remove cancelled task iff still our task.
                cache.remove(arg, f);
			} catch (InterruptedException e) {
				/*
				 * Wrap the exception to indicate whether or not the interrupt
				 * occurred in the thread of the caller that executed the
				 * FutureTask in its thread. This is being done as an aid to
				 * diagnosing situations where f.get() throws out an
				 * InterruptedException.
				 */
				final InterruptedException e2 = new InterruptedException(
						"Interrupted: willRun=" + willRun);
				e2.initCause(e);
				throw e2;
            } catch (ExecutionException e) {
                if (!willRun) {
//                        && InnerCause.isInnerCause(e,
//                                InterruptedException.class)) {
                    /*
                     * Since the task was executed by another thread (ft.run()),
                     * remove the task and retry.
                     * 
                     * Note: Basically, what has happened is that the thread
                     * which got to cache.putIfAbsent() first ran the Computable
                     * and something was thrown out of ft.run(), so the thread
                     * which ran the task needs to propagate the
                     * InterruptedException back to its caller.
                     * 
                     * Typically this situation arises when the thread actually
                     * running the task in ft.run() was interrupted, resulting
                     * in an wrapped InterruptedException or a wrapped
                     * ClosedByInterruptException.
                     * 
                     * However, other threads which concurrently request the
                     * same computation MUST NOT see the InterruptedException
                     * since they were not actually interrupted. Therefore, we
                     * yank out the FutureTask and retry for any thread which
                     * did not run the task itself.
                     * 
                     * If there is a real underlying error, this forces each
                     * thread who is requesting computation to attempt the
                     * computation and report back the error in their own
                     * thread. If the exception is a transient, with the most
                     * common example being an interrupt, then the operation
                     * will succeed for the next thread which attempts ft.run()
                     * and all other threads waiting on f.get() will observe the
                     * successfully computed Future.
                     */ 
                    cache.remove(arg, f);
                    // Retry.
                    continue;
                }
                throw launderThrowable(e.getCause());
            }
        }
    }

    static private RuntimeException launderThrowable(final Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else
            throw new IllegalStateException("Not unchecked", t);
    }

}
