package org.embergraph.rdf.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IJournal;
import org.embergraph.journal.ITx;
import org.embergraph.journal.Journal;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.lexicon.BlobsIndexHelper;
import org.embergraph.rdf.lexicon.LexiconKeyOrder;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.EmbergraphValueSerializer;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.service.IEmbergraphClient;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.util.Bytes;
import org.embergraph.util.BytesUtil;

/*
 * Utility class to dump the TERMS index of a triple store.
 *
 * @author thompsonbry
 */
public class DumpLexicon {

  private static final String REMOTE_ERR_MSG =
      "Remote Lexicon dumping is not supported by this class."
          + "\nPlease use DumpRemoteLexicon in embergraph-jini."
          + "See BLZG-1370 \n";

  private static final String CONFIG_EXT = ".config";
  private static final String PROPERTY_EXT = ".properties";

  protected DumpLexicon() {}

  protected static void usage() {

    System.err.println("usage: (-tuples) <namespace> <filename>");
  }

  /*
   * Open the {@link IIndexManager} identified by the property file.
   *
   * @param propertyFile The property file (for a standalone embergraph instance) or the jini
   *     configuration file (for a embergraph federation). The file must end with either
   *     ".properties" or ".config".
   *     <p>Starting with 1.5.2 the remote dump lexicon capability was moved into the
   *     embergraph-jini artifact. See BLZG-1370.
   * @return The {@link IIndexManager}.
   */
  protected static IIndexManager openIndexManager(final String propertyFile) {

    final File file = new File(propertyFile);

    if (!file.exists()) {

      throw new RuntimeException("Could not find file: " + file);
    }

    if (propertyFile.endsWith(CONFIG_EXT)) {
      // scale-out.
      throw new RuntimeException(REMOTE_ERR_MSG);
    } else if (propertyFile.endsWith(PROPERTY_EXT)) {
      // local journal.
    } else {
      /*
       * Note: This is a hack, but we are recognizing the jini
       * configuration file with a .config extension and the journal
       * properties file with a .properties extension.
       */
      throw new RuntimeException("File must have '.config' or '.properties' extension: " + file);
    }

    final IIndexManager indexManager;
    try {

      /*
       * Note: we only need to specify the FILE when re-opening a journal
       * containing a pre-existing KB.
       */
      final Properties properties = new Properties();
      {
        // Read the properties from the file.
        final InputStream is = new BufferedInputStream(new FileInputStream(propertyFile));
        try {
          properties.load(is);
        } finally {
          is.close();
        }
        if (System.getProperty(EmbergraphSail.Options.FILE) != null) {
          // Override/set from the environment.
          properties.setProperty(
              EmbergraphSail.Options.FILE, System.getProperty(EmbergraphSail.Options.FILE));
        }
      }

      final Journal jnl = new Journal(properties);

      indexManager = jnl;

    } catch (Exception ex) {

      throw new RuntimeException(ex);
    }

    return indexManager;
  }

  /*
   * @param args <code>(-tuples) &lt;namespace&gt; &lt;filename&gt;</code> <br>
   *     where <i>namespace</i> is the namespace of the {@link LexiconRelation}. Use <code>kb.lex
   *     </code> if you have not overridden the namespace of the {@link AbstractTripleStore}. <br>
   *     where <i>filename</i> is the name of the properties or configuration file to be used.
   */
  public static void main(final String[] args) {

    if (args.length < 2) {
      usage();
      System.exit(1);
    }

    boolean showTuples = false;

    int i = 0;

    for (; i < args.length; i++) {

      String arg = args[i];

      if (!arg.startsWith("-")) {

        // End of options.
        break;
      }

      if (arg.equals("-tuples")) {

        showTuples = true;

      } else throw new RuntimeException("Unknown argument: " + arg);
    }

    if (i + 2 != args.length) {
      usage();
      System.exit(1);
    }

    final String namespace = args[i++];

    final String propertyFile = args[i++];

    PrintWriter w = new PrintWriter(System.out);

    IIndexManager indexManager = null;
    try {

      w.println("namespace: " + namespace);

      w.println("filename : " + propertyFile);

      indexManager = openIndexManager(propertyFile);

      final long timestamp = ITx.READ_COMMITTED;

      final AbstractTripleStore tripleStore =
          (AbstractTripleStore) indexManager.getResourceLocator().locate(namespace, timestamp);

      if (tripleStore == null) {

        throw new RuntimeException(
            "Not found: namespace="
                + namespace
                + ", timestamp="
                + TimestampUtility.toString(timestamp));
      }

      final LexiconRelation lex = tripleStore.getLexiconRelation();

      w.println(lex.getLexiconConfiguration());

      w.flush();

      DumpLexicon.dump(tripleStore, w, showTuples);

    } catch (RuntimeException ex) {

      ex.printStackTrace();

      System.err.println("Error: " + ex + " on file: " + propertyFile);

      System.exit(2);

    } finally {

      w.flush();

      w.close();

      if (indexManager != null) {

        if (indexManager instanceof IJournal) {

          if (((IJournal) indexManager).isOpen()) {

            ((IJournal) indexManager).shutdownNow();
          }

        } else {

          IEmbergraphClient<?> client = null;

          try {

            client = ((IEmbergraphFederation<?>) indexManager).getClient();

          } catch (IllegalStateException ex) {
            // Ignore.
          }

          if (client != null) client.disconnect(true /* immediateShutdown */);
        }
      }
    }
  }

  //  /*
  //  * Dumps the lexicon in a variety of ways (test suites only).
  //  */
  // public StringBuilder dumpTerms() {
  //
  //     final StringBuilder sb = new StringBuilder(Bytes.kilobyte32 * 4);
  //
  //     /*
  //      * Dumps the terms in term order.
  //      */
  //     sb.append("---- terms in term order ----\n");
  //     for( Iterator<Value> itr = termIterator(); itr.hasNext(); ) {
  //
  //         final Value val = itr.next();
  //
  //         if (val == null) {
  //             sb.append("NullIV");
  //         } else {
  //             sb.append(val.toString());
  //         }
  //
  //         sb.append("\n");
  //
  //     }
  //
  //     return sb;
  //
  // }

  /*
   * Dumps the lexicon in a variety of ways.
   *
   * @param store
   */
  public static void dump(
      final AbstractTripleStore store, final Writer w, final boolean showBlobs) {

    //        /*
    //         * Note: it is no longer true that all terms are stored in the reverse
    //         * index (BNodes are not). Also, statement identifiers are stored in the
    //         * forward index, so we can't really write the following assertion
    //         * anymore.
    //         */
    //        // Same #of terms in the forward and reverse indices.
    //        assertEquals("#terms", store.getIdTermIndex().rangeCount(null, null),
    //                store.getTermIdIndex().rangeCount(null, null));

    final LexiconRelation r = store.getLexiconRelation();

    try {

      /** Dumps the forward mapping (TERM2ID). */
      {
        w.write(r.getFQN(LexiconKeyOrder.TERM2ID) + " (forward mapping)\n");

        final IIndex ndx = store.getLexiconRelation().getTerm2IdIndex();

        final ITupleIterator<?> itr = ndx.rangeIterator();

        while (itr.hasNext()) {

          final ITuple<?> tuple = itr.next();

          /*
           * The sort key for the term. This is not readily decodable.
           * See LexiconKeyBuilder for specifics.
           */
          final byte[] key = tuple.getKey();

          /*
           * Decode the TermIV.
           */
          final TermId<?> iv = (TermId<?>) IVUtility.decode(tuple.getValue());

          w.write(BytesUtil.toString(key) + ":" + iv + "\n");
        }
      }

      /** Dumps the reverse mapping. */
      {
        w.write(r.getFQN(LexiconKeyOrder.ID2TERM) + " (reverse mapping)\n");

        final IIndex ndx = store.getLexiconRelation().getId2TermIndex();

        @SuppressWarnings("unchecked")
        final ITupleIterator<EmbergraphValue> itr = ndx.rangeIterator();

        while (itr.hasNext()) {

          final ITuple<EmbergraphValue> tuple = itr.next();

          final EmbergraphValue term = tuple.getObject();

          w.write(term.getIV() + ":" + term + " (iv=" + term.getIV() + ")\n");
        }
      }

      // /*
      // * Dumps the term:id index.
      // */
      // for( Iterator<BlobIV> itr =
      // store.getLexiconRelation().termsIndexScan(); itr.hasNext(); ) {
      //
      // System.err.println("term->id : "+itr.next());
      //
      // }

      // /*
      // * Dumps the id:term index.
      // */
      // for( Iterator<Value> itr =
      // store.getLexiconRelation().idTermIndexScan(); itr.hasNext(); ) {
      //
      // System.err.println("id->term : "+itr.next());
      //
      // }

      // /*
      // * Dumps the terms in term order.
      // */
      // for( Iterator<Value> itr =
      // store.getLexiconRelation().termIterator(); itr.hasNext(); ) {
      //
      // System.err.println("termOrder : "+itr.next());
      //
      // }

      /*
       * Dump the BLOBs index.
       */
      w.write(r.getFQN(LexiconKeyOrder.BLOBS) + " (large values)\n");

      dumpBlobs(w, showBlobs /* showEntries */, r.getNamespace(), r.getBlobsIndex());

    } catch (IOException ex) {

      throw new RuntimeException(ex);
    }
  }

  /*
   * Dump the lexicon.
   *
   * @param r The lexicon relation.
   * @return The dump.
   */
  public static Appendable dump(final LexiconRelation r) {

    final StringWriter w =
        new StringWriter(
            100 * Bytes.kilobyte32 // initialCapacity
            );

    w.append(r.getLexiconConfiguration().toString());

    w.append("\n");

    dump(r.getContainer(), w, true /*showEntries*/);

    return w.getBuffer();
  }

  /*
   * Dump the BLOBS index.
   *
   * @param namespace
   * @param ndx
   * @return
   */
  public static Appendable dumpBlobs(final String namespace, final IIndex ndx) {

    final StringWriter w =
        new StringWriter(
            100 * Bytes.kilobyte32 // initialCapacity
            );

    DumpLexicon.dumpBlobs(w, true /*showEntries*/, namespace, ndx);

    return w.getBuffer();
  }

  /*
   * Core implementation for dumping the BLOBS index.
   *
   * @param w Where to write the data.
   * @param showEntries When <code>true</code> the individual entries in the TERMS index will be
   *     reported. When <code>false</code> only metadata about the scanned entries will be reported.
   * @param namespace The namespace of the {@link LexiconRelation}.
   * @param ndx The BLOBS index for that {@link LexiconRelation}.
   */
  public static void dumpBlobs(
      final Writer w, final boolean showEntries, final String namespace, final IIndex ndx) {

    final int BIN_SIZE = 256;

    final int NBINS = (BlobsIndexHelper.MAX_COUNTER + 1) / BIN_SIZE;

    try {

      int maxCollisionCounter = 0;

      /*
       * An array of bins reporting the #of TERMS having the #of collision
       * counters for that bin. The bins are each BIN_SIZE wide. There are
       * NBINS bins. For a given counter value, the bin is selected by
       * floor(counter/binSize).
       *
       * TODO It would be much more useful to use a sparse array so we can
       * report on the distribution at the lower end of the hash collision
       * counter range, which is where most of the collisions will be
       * found.
       */
      final long[] bins = new long[NBINS];

      final EmbergraphValueFactory vf = EmbergraphValueFactoryImpl.getInstance(namespace);

      final EmbergraphValueSerializer<EmbergraphValue> valSer = vf.getValueSerializer();

      // Used to decode the Values.
      final StringBuilder tmp = new StringBuilder();

      w.append("fastRangeCount=" + ndx.rangeCount() + "\n");

      @SuppressWarnings("unchecked")
      final ITupleIterator<BlobIV<?>> itr = ndx.rangeIterator();

      long nvisited = 0L;

      while (itr.hasNext()) {

        final ITuple<BlobIV<?>> tuple = itr.next();

        nvisited++;

        if (tuple.isNull()) {

          if (showEntries) {
            w.append("NullIV: key=");
            w.append(BytesUtil.toString(tuple.getKey()));
            w.append("\n");
          }

        } else {

          final BlobIV<?> iv =
              (BlobIV<?>) IVUtility.decodeFromOffset(tuple.getKeyBuffer().array(), 0 /* offset */);
          // new TermId(tuple.getKey());

          final EmbergraphValue value = valSer.deserialize(tuple.getValueStream(), tmp);

          if (showEntries) {
            w.append(iv.toString());
            w.append(" => ");
            w.append(value.toString());
            w.append("\n");
          }

          final int counter = iv.counter();

          if (counter > maxCollisionCounter) {

            maxCollisionCounter = counter;
          }

          final int bin = (counter / BIN_SIZE);

          bins[bin]++;
        }
      }

      w.append("nvisited=" + nvisited + "\n");

      w.append("binSize=" + BIN_SIZE + "\n");

      w.append("nbins=" + NBINS + "\n");

      // #of non-zero bins.
      int nnzero = 0;

      for (int bin = 0; bin < NBINS; bin++) {

        final long numberInBin = bins[bin];

        if (numberInBin == 0) continue;

        w.append("bins[" + bin + "]=" + numberInBin + "\n");

        nnzero++;
      }

      w.append("numNonZeroBins=" + nnzero + "\n");

      w.append("maxCollisionCounter=" + maxCollisionCounter + "\n");

    } catch (IOException e) {

      throw new RuntimeException(e);
    }
  }
}
