package org.embergraph.service;

/*
 * Run states for the {@link AbstractTransactionService}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public enum TxServiceRunState {

  /** During startup. */
  Starting(0),
  /** While running (aka open). */
  Running(1),
  /** When shutting down normally. */
  Shutdown(2),
  /** When shutting down immediately. */
  ShutdownNow(3),
  /** When halted. */
  Halted(4);

  TxServiceRunState(int val) {

    this.val = val;
  }

  private final int val;

  public int value() {

    return val;
  }

  public boolean isTransitionLegal(final TxServiceRunState newval) {

    if (this == Starting) {

      if (newval == Running) return true;

      return newval == Halted;

    } else if (this == Running) {

      if (newval == Shutdown) return true;

      return newval == ShutdownNow;

    } else if (this == Shutdown) {

      if (newval == ShutdownNow) return true;

      return newval == Halted;

    } else if (this == ShutdownNow) {

      return newval == Halted;
    }

    return false;
  }
}
