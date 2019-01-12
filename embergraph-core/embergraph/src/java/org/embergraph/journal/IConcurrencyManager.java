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
 * Created on Feb 19, 2008
 */

package org.embergraph.journal;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.embergraph.counters.ICounterSetAccess;
import org.embergraph.service.IServiceShutdown;

/**
 * Interface for managing concurrent access to resources (indices).
 *
 * @see AbstractTask, Base class for tasks to be executed with concurrency control.
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IConcurrencyManager extends IServiceShutdown, ICounterSetAccess {

  /** The client side of the transaction manager. */
  ILocalTransactionManager getTransactionManager();

  //    /**
  //     * The server side of the transaction manager.
  //     */
  //    public ITransactionManager getTransactionService();

  /** The object used to manage local resources. */
  IResourceManager getResourceManager();

  /**
   * Normal shutdown - running tasks will run to completion, but no new tasks will start.
   *
   * @see #shutdownNow()
   */
  @Override
  void shutdown();

  /**
   * Immediate shutdown - running tasks are cancelled rather than being permitted to complete.
   *
   * @see #shutdown()
   */
  @Override
  void shutdownNow();

  /**
   * Submit a task (asynchronous). Tasks will execute asynchronously in the appropriate thread pool
   * with as much concurrency as possible.
   *
   * <p>Note: Unisolated write tasks will NOT return before the next group commit (exceptions may be
   * thrown if the task fails or the commit fails). The purpose of group commits is to provide
   * higher throughput for writes on the store by only syncing the data to disk periodically rather
   * than after every write. Group commits are scheduled by the {@link #commitService}. The trigger
   * conditions for group commits may be configured using {@link ConcurrencyManager.Options}. If you
   * are using the store in a single threaded context then you may set {@link
   * Options#WRITE_SERVICE_CORE_POOL_SIZE} to ONE (1) which has the effect of triggering commit
   * immediately after each unisolated write. However, note that you can not sync a disk more than ~
   * 30-40 times per second so your throughput in write operations per second will never exceed that
   * for a single-threaded application writing on a hard disk. (Your mileage can vary if you are
   * writing on a transient store or using a durable medium other than disk).
   *
   * <p>Note: The isolated indices used by a read-write transaction are NOT thread-safe. Therefore a
   * partial order is imposed over concurrent tasks for the <strong>same</strong> transaction that
   * seek to read or write on the same index(s). Full concurrency is allowed when different
   * transactions access the same index(s), but write-write conflicts MAY be detected during commit
   * processing.
   *
   * <p>Note: The following exceptions MAY be wrapped by {@link Future#get()} for tasks submitted
   * via this method:
   *
   * <dl>
   *   <dt>{@link ValidationError}
   *   <dd>An unisolated write task was attempting to commit the write set for a transaction but
   *       validation failed. You may retry the entire transaction.
   *   <dt>{@link InterruptedException}
   *   <dd>A task was interrupted during execution and before the task had completed normally. You
   *       MAY retry the task, but note that this exception is also generated when tasks are
   *       cancelled when the journal is being {@link #shutdown()} after the timeout has expired or
   *       {@link #shutdownNow()}. In either of these cases the task will not be accepted by the
   *       journal.
   *   <dt>
   *   <dd>
   * </dl>
   *
   * @param task The task.
   * @return The {@link Future} that may be used to resolve the outcome of the task.
   * @exception RejectedExecutionException if task cannot be scheduled for execution (typically the
   *     queue has a limited capacity and is full)
   * @exception NullPointerException if task is <code>null</code>
   */
  <T> FutureTask<T> submit(AbstractTask<T> task);

  /**
   * Executes the given tasks, returning a list of Futures holding their status and results when all
   * complete. Note that a completed task could have terminated either normally or by throwing an
   * exception. The results of this method are undefined if the given collection is modified while
   * this operation is in progress.
   *
   * <p>Note: Contract is per {@link ExecutorService#invokeAll(Collection)}
   *
   * @param tasks The tasks.
   * @return Their {@link Future}s.
   * @exception InterruptedException if interrupted while waiting, in which case unfinished tasks
   *     are cancelled.
   * @exception NullPointerException if tasks or any of its elements are null
   * @exception RejectedExecutionException if any task cannot be scheduled for execution
   */
  <T> List<Future<T>> invokeAll(Collection<? extends AbstractTask<T>> tasks)
      throws InterruptedException;

  /**
   * Executes the given tasks, returning a list of Futures holding their status and results when all
   * complete or the timeout expires, whichever happens first. Note that a completed task could have
   * terminated either normally or by throwing an exception. The results of this method are
   * undefined if the given collection is modified while this operation is in progress.
   *
   * <p>Note: Contract is based on {@link ExecutorService#invokeAll(Collection, long, TimeUnit)} but
   * only the {@link Future}s of the submitted tasks are returned.
   *
   * @param tasks The tasks.
   * @return The {@link Future}s of all tasks that were {@link #submit(AbstractTask) submitted}
   *     prior to the expiration of the timeout.
   * @exception InterruptedException if interrupted while waiting, in which case unfinished tasks
   *     are cancelled.
   * @exception NullPointerException if tasks or any of its elements are null
   * @exception RejectedExecutionException if any task cannot be scheduled for execution
   */
  <T> List<Future<T>> invokeAll(
      Collection<? extends AbstractTask<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException;

  /** The service on which read-write tasks are executed. */
  WriteExecutorService getWriteService();
}
