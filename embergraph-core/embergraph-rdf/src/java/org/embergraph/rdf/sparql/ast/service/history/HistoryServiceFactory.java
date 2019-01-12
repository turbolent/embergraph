package org.embergraph.rdf.sparql.ast.service.history;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KVO;
import org.embergraph.journal.IIndexManager;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.changesets.IChangeRecord;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSail.EmbergraphSailConnection;
import org.embergraph.rdf.sparql.ast.eval.CustomServiceFactoryBase;
import org.embergraph.rdf.sparql.ast.service.EmbergraphNativeServiceOptions;
import org.embergraph.rdf.sparql.ast.service.IServiceOptions;
import org.embergraph.rdf.sparql.ast.service.ServiceCall;
import org.embergraph.rdf.sparql.ast.service.ServiceCallCreateParams;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.AbstractRelation;

/*
 * This service tracks KB updates via an {@link IChangeLog} and is responsible for maintaining an
 * ordered index over the assertions that have been added to or removed from a KB instance.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/607">History Service</a>
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public class HistoryServiceFactory extends CustomServiceFactoryBase {

  private static final transient Logger log = Logger.getLogger(HistoryServiceFactory.class);

  private final EmbergraphNativeServiceOptions serviceOptions;

  public HistoryServiceFactory() {

    serviceOptions = new EmbergraphNativeServiceOptions();

    /*
     * TODO Review decision to make this a runFirst service. The rational is
     * that this service can only apply a very limited set of restrictions
     * during query, therefore it will often make sense to run it first.
     * However, the fromTime and toTime could be bound by the query and the
     * service can filter some things more efficiently internally than if we
     * generated a bunch of intermediate solutions for those things.
     */
    serviceOptions.setRunFirst(true);
  }

  @Override
  public IServiceOptions getServiceOptions() {

    return serviceOptions;
  }

  /*
   * TODO Implement: Query should support an index scan of a date range with optional filters on the
   * (s,p,o,c) and add/remove flags. It might make more sense to index in (POS) order rather than
   * SPO order so we can more efficiently scan a specific predicate within some date range using an
   * advancer pattern.
   *
   * <p>The restrictions that this service COULD apply to the index scan are:
   *
   * <dl>
   *   <dt>fromTime
   *   <dd>Inclusive lower bound.
   *   <dt>toTime
   *   <dd>Exclusive upper bound (e.g., the first commit point NOT to be reported).
   *   <dt>P
   *   <dd>The {@link IV} for the predicate (this is the first statement key component in the
   *       history index for both triples and quads mode KBs)
   * </dl>
   *
   * In addition, it could filter on the remaining fields (that is, skip over tuples that fail a
   * filter):
   *
   * <dl>
   *   <dt>S, O [, C]
   *   <dd>The {@link IV} for the subject, object, and (for quads mode, the context).
   *   <dt>action
   *   <dd>The {@link ChangeAction}.
   *   <dt>type
   *   <dd>The {@link StatementTypeEnum}.
   * </dl>
   */
  @Override
  public ServiceCall<?> create(final ServiceCallCreateParams params) {

    throw new UnsupportedOperationException();
  }

  /*
   * Register an {@link IChangeLog} listener that will manage the maintenance of the describe cache.
   */
  @Override
  public void startConnection(final EmbergraphSailConnection conn) {

    //        final Properties properties = conn.getProperties();
    final AbstractTripleStore tripleStore = conn.getTripleStore();

    if (Boolean.valueOf(
        tripleStore.getProperty(
            EmbergraphSail.Options.HISTORY_SERVICE,
            EmbergraphSail.Options.DEFAULT_HISTORY_SERVICE))) {

      conn.addChangeLog(new HistoryChangeLogListener(conn));
    }
  }

  /*
   * Handles maintenance of the history index.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class HistoryChangeLogListener implements IChangeLog {

    /** The vector size for updates. */
    private static final int threshold = 10000;
    /** The connection. */
    private final EmbergraphSailConnection conn;
    /** The KB instance. */
    private final AbstractTripleStore tripleStore;
    /*
     * The head of the index is pruned on update to remove entries that are older than this age
     * (milliseconds).
     */
    private final long minReleaseAge;
    /** The first timestamp that WILL NOT be released. */
    private final long releaseTime;
    /** The timestamp that will be associated with the {@link IChangeLog} events in the index. */
    private volatile long revisionTimestamp;
    /** The set of IVs to be invalidated (lazily instantiated). */
    private Map<ISPO, IChangeRecord> changeSet;
    /** The history index. */
    private IIndex ndx = null;

    HistoryChangeLogListener(final EmbergraphSailConnection conn) {

      this.conn = conn;

      this.tripleStore = conn.getTripleStore();

      this.revisionTimestamp = getRevisionTimestamp(tripleStore);

      this.minReleaseAge =
          Long.valueOf(
              tripleStore.getProperty(
                  EmbergraphSail.Options.HISTORY_SERVICE_MIN_RELEASE_AGE,
                  EmbergraphSail.Options.DEFAULT_HISTORY_SERVICE_MIN_RELEASE_AGE));

      /*
       * TODO We should be able to reach the timestamp service from the
       * index manager. We want to use the globally agreed on clock for
       * the current time when making the decision to prune the head of
       * the index.
       */

      releaseTime = (System.currentTimeMillis() - minReleaseAge) + 1;

      if (log.isInfoEnabled()) {
        log.info("minReleaseAge=" + minReleaseAge + ", releaseTime=" + releaseTime);
      }
    }

    /*
     * Return the revision time that will be used for all changes written onto the history index by
     * this {@link IChangeLog} listener.
     *
     * @see HistoryChangeRecord#getRevisionTime()
     */
    private static long getRevisionTimestamp(final AbstractTripleStore tripleStore) {

      final long revisionTimestamp;

      final IIndexManager indexManager = tripleStore.getIndexManager();

      revisionTimestamp = indexManager.getLastCommitTime() + 1;

      //            if (indexManager instanceof IJournal) {
      //
      //                revisionTimestamp = indexManager.getLastCommitTime() + 1;
      ////                        ((IJournal) indexManager)
      ////                        .getLocalTransactionManager().nextTimestamp();
      //
      //            } else if (indexManager instanceof IEmbergraphFederation) {
      //
      //                try {
      //
      //                    revisionTimestamp = ((IEmbergraphFederation<?>) indexManager)
      //                            .getTransactionService().nextTimestamp();
      //
      //                } catch (IOException e) {
      //
      //                    throw new RuntimeException(e);
      //
      //                }
      //
      //            } else {
      //
      //                throw new AssertionError("indexManager="
      //                        + indexManager.getClass());
      //
      //            }

      return revisionTimestamp;
    }

    @Override
    public void transactionBegin() {

      this.revisionTimestamp = getRevisionTimestamp(tripleStore);
    }

    @Override
    public void transactionPrepare() {

      flush();
    }

    /** Vectors updates against the DESCRIBE cache. */
    @Override
    public void changeEvent(final IChangeRecord record) {

      if (changeSet == null) {

        // Lazy instantiation.
        changeSet = new HashMap<ISPO, IChangeRecord>();

        // Get the history index.
        ndx = getHistoryIndex(tripleStore);

        if (minReleaseAge > 0) {

          pruneHistory();
        }
      }

      final ISPO spo = record.getStatement();

      changeSet.put(spo, record);

      if (changeSet.size() > threshold) {

        flush();
      }
    }

    /*
     * Return the pre-existing history index.
     *
     * @param tripleStore The KB.
     * @return The history index and never <code>null</code>.
     * @throws IllegalStateException if the index was not configured / does not exist.
     */
    private IIndex getHistoryIndex(final AbstractTripleStore tripleStore) {

      final SPORelation spoRelation = tripleStore.getSPORelation();

      final String fqn = AbstractRelation.getFQN(spoRelation, SPORelation.NAME_HISTORY);

      ndx = spoRelation.getIndex(fqn);

      if (ndx == null) throw new IllegalStateException("Index not found: " + fqn);

      return ndx;
    }

    /*
     * Prune the head of the history index.
     *
     * <p>Note: Either this should be done as the first action or you must make a note of the
     * effective release time as the first action and then apply that effective release time later.
     * If you instead compute and apply the effective release time later on, then there is the
     * possibility that you could prune out entries from the current transaction!
     */
    private void pruneHistory() {

      final IKeyBuilder keyBuilder = ndx.getIndexMetadata().getKeyBuilder().reset();

      keyBuilder.append(releaseTime);

      final byte[] toKey = keyBuilder.getKey();

      long n = 0;

      final ITupleIterator<?> itr =
          ndx.rangeIterator(
              null /* fromKey */,
              toKey,
              0 /* capacity */,
              IRangeQuery.REMOVEALL /* flags */,
              null /* filterCtor */);

      while (itr.hasNext()) {

        itr.next();

        n++;
      }

      if (n > 0 && log.isInfoEnabled()) {
        log.info(
            "pruned history: nremoved="
                + n
                + ", minReleaseAge="
                + minReleaseAge
                + ", releaseTime="
                + releaseTime);
      }
    }

    @Override
    public void transactionCommited(long commitTime) {

      flush();
    }

    @Override
    public void transactionAborted() {

      reset();
    }

    /** See {@link IChangeLog#close()}. */
    @Override
    public void close() {
      reset();
    }

    /** Reset the buffer. */
    private void reset() {

      changeSet = null;
    }

    /** Incremental flush. */
    private void flush() {

      if (changeSet != null) {

        final int size = changeSet.size();

        final KVO<HistoryChangeRecord>[] b = new KVO[size];
        {
          // Extract the new change records into an array.
          final IChangeRecord[] a = changeSet.values().toArray(new IChangeRecord[size]);

          final HistoryIndexTupleSerializer tupSer =
              (HistoryIndexTupleSerializer) ndx.getIndexMetadata().getTupleSerializer();

          // Wrap each one with the revision time.
          for (int i = 0; i < size; i++) {

            final IChangeRecord r = a[i];

            // attach the revision time.
            final HistoryChangeRecord s = new HistoryChangeRecord(r, revisionTimestamp);

            final byte[] key = tupSer.serializeKey(s);

            final byte[] val = tupSer.serializeVal(s);

            b[i] = new KVO<HistoryChangeRecord>(key, val, s);
          }
        }

        // Sort to improve the index locality.
        java.util.Arrays.sort(b);

        // Write on the indices.
        for (int i = 0; i < size; i++) {

          final KVO<HistoryChangeRecord> r = b[i];

          ndx.insert(r.key, r.val);
        }

        reset();
      }
    }
  } // class HistoryChangeLogListener
} // class HistoryServiceFactory
