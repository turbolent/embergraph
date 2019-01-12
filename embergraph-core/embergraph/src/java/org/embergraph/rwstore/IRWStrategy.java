package org.embergraph.rwstore;

import java.util.concurrent.locks.Lock;
import org.embergraph.journal.IBufferStrategy;
import org.embergraph.journal.RWStrategy;
import org.embergraph.rawstore.IAllocationManagerStore;
import org.embergraph.rwstore.sector.MemStrategy;

/**
 * Defines a marker interface to be used to indicate strategies that share RW semantics such as
 * {@link RWStrategy} and {@link MemStrategy}
 *
 * @author Martyn Cutcher
 */
public interface IRWStrategy
    extends IBufferStrategy, IAllocationManager, IAllocationManagerStore, IHistoryManager {

  /** Return the backing {@link IStore}. */
  IStore getStore();

  /**
   * Return <code>true</code> iff the allocation having that address is flagged as committed. The
   * caller must be holding the allocation lock in order for the result to remain valid outside of
   * the method call.
   *
   * @param addr The address.
   * @return <code>true</code> iff the address is currently committed.
   */
  boolean isCommitted(long addr);

  /**
   * Optionally return a {@link Lock} that must be used (when non- <code>null</code>) to make the
   * {@link IBufferStrategy#commit()} / {@link #postCommit()} strategy atomic.
   */
  Lock getCommitLock();

  /**
   * Called post commit to dispose any transient commit state retained to support reset/rollback.
   *
   * <p>Note: It is the responsibility of the commit protocol layers to wind up calling {@link
   * IBufferStrategy#abort()} if there is a failure during the commit protocol.
   */
  void postCommit();
}
