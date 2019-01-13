package org.embergraph.service.proxy;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * A helper object that provides the API of {@link Future} but whose methods throw {@link
 * IOException} and are therefore compatible with {@link Remote} and {@link Exporter}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <T>
 */
public class RemoteFutureImpl<T> implements RemoteFuture<T> {

  private final Future<T> future;

  public RemoteFutureImpl(final Future<T> future) {

    if (future == null) throw new IllegalArgumentException();

    this.future = future;
  }

  public boolean cancel(boolean mayInterruptIfRunning) {

    return future.cancel(mayInterruptIfRunning);
  }

  public T get() throws InterruptedException, ExecutionException {

    return future.get();
  }

  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {

    return future.get(timeout, unit);
  }

  public boolean isCancelled() {

    return future.isCancelled();
  }

  public boolean isDone() {

    return future.isDone();
  }
}
