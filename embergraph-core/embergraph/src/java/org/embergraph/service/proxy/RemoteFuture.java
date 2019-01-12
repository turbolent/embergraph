package org.embergraph.service.proxy;

import java.io.IOException;
import java.rmi.Remote;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * {@link Remote} interface declaring the API of {@link Future} but also declaring that each methods
 * throws {@link IOException} in order to be compatible with {@link Remote} and {@link Exporter}. Of
 * course, this means that this interface can not extend {@link Future}!
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public interface RemoteFuture<T> extends Remote {

  boolean cancel(boolean mayInterruptIfRunning) throws IOException;

  T get() throws InterruptedException, ExecutionException, IOException;

  T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException, IOException;

  boolean isCancelled() throws IOException;

  boolean isDone() throws IOException;
}
