package org.embergraph.journal;

/** Simple class to retrieve open journal and thread metrics for debugging purposes. */
public class JournalMetrics {
  final AbstractJournal m_jrnl;
  final ThreadGroup m_grp;
  final int m_startupThreads;

  public JournalMetrics(final AbstractJournal jrnl) {
    m_jrnl = jrnl;
    m_grp = Thread.currentThread().getThreadGroup();
    m_startupThreads = m_grp.activeCount();
  }

  /** @return a debug string with info on open journals and active threads */
  public String getStatus() {

    String bldr = ("Open journals: " + AbstractJournal.nopen.get() + "\n")
        + "Threads - startup: " + m_startupThreads + ", current: " + m_grp.activeCount();
    return bldr;
  }
}
