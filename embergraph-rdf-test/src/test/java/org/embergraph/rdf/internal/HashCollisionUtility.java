package org.embergraph.rdf.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;
import org.embergraph.Banner;
import org.embergraph.btree.BTree;
import org.embergraph.btree.DefaultTupleSerializer;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.ITupleIterator;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.keys.DefaultKeyBuilderFactory;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KV;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.SuccessorUtil;
import org.embergraph.btree.raba.codec.CanonicalHuffmanRabaCoder;
import org.embergraph.btree.raba.codec.FrontCodedRabaCoder;
import org.embergraph.io.ByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;
import org.embergraph.io.DirectBufferPool;
import org.embergraph.io.compression.RecordCompressor;
import org.embergraph.journal.BufferMode;
import org.embergraph.journal.Journal;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.internal.impl.literal.PartlyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.uri.PartlyInlineURIIV;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.EmbergraphValueSerializer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.vocab.BaseVocabulary;
import org.embergraph.rwstore.sector.IMemoryManager;
import org.embergraph.rwstore.sector.MemoryManager;
import org.embergraph.util.Bytes;
import org.embergraph.util.BytesUtil;
import org.embergraph.util.concurrent.Latch;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;

/**
 * Utility class to parse some RDF resource(s) and count hash collisions using a variety of hash
 * codes.
 *
 * <p>TODO Various data sets:
 *
 * <pre>
 * /nas/data/lubm/U1/data/University0/
 * /nas/data/bsbm/bsbm_2785/dataset.nt.gz
 * /nas/data/bsbm/bsbm_566496/dataset.nt.gz
 * /data/bsbm3_200m_1MSplits
 *
 * 8B triple bioinformatics data set.
 *
 * BTC data (some very large literals)
 *
 * </pre>
 *
 * TODO order preserving hash codes could be interesting here. Look at 32 and 64 bit variants of the
 * math and at generalized order preserving hash codes. With order preserving hash codes, it makes
 * sense to insert all Unicode terms into TERM2ID such that we have a total order there.
 *
 * <p>TODO benchmark the load time with different hash codes. the cost of the hash computation and
 * the randomness of the distribution will both play a role. The B+Tree will need to be setup with a
 * sufficient [writeRetentionQueue] and we will need to specify [-server -Xmx1G].
 *
 * <p>SHA-256 - no collisions on BSBM 200M. 30G file. time?
 *
 * <p>32-bit hash codes. #collisions=1544132 Elapsed: 16656445ms Journal size: 23841341440 bytes
 * (23G)
 *
 * <p>Now limiting the size of values in a leaf and also increasing the branching factor to 512 (was
 * 32). [The current run is scanning after the initial insert, which involves a little wasted
 * effort. It was also without the -server -Xmx2g, and write retention queue parameters. Finally, it
 * was serializing EmbergraphValue objects, including their IV, rather than RDF Value objects. The
 * code has since been modified to serialize just the EmbergraphValue Also, I've since raised the
 * initial extent from 10M to 200M]. maxCollisions=3, Elapsed: 22579073ms Journal size: 35270950912
 * bytes
 *
 * <p>Now buffering 100k values at a time: 2x faster.
 *
 * <pre>
 * U1:
 * Elapsed: 23379ms
 * NumStatements: 1000313
 * NumDistinctVals: 291259
 * TotalKeyBytes: 1747554
 * TotalValBytes: 60824514
 * MaxCollisions: 1
 * TotalCollisions: 6
 * Journal size: 209715200 bytes
 * name	m	height	nnodes	nleaves	nodeBytes	leafBytes	totalBytes	avgNodeBytes	avgLeafBytes	minNodeBytes	maxNodeBytes	minLeafBytes	maxLeafBytes
 * lex	1024	1	1	474	7913	3662623	3670536	7913	7727	7913	7913	5786	13784
 * </pre>
 *
 * With only a byte (versus short) counter in the key. Oddly, this has no impact on the average leaf
 * size. That suggests that the keys in the leaves are very sparse in terms of the hash code space
 * such that prefix compression is not really doing that much for us.
 *
 * <pre>
 * Elapsed: 23235ms
 * NumStatements: 1000313
 * NumDistinctVals: 291259
 * TotalKeyBytes: 1456295
 * TotalValBytes: 60824514
 * MaxCollisions: 1
 * TotalCollisions: 6
 * Journal size: 209715200 bytes
 * name	m	height	nnodes	nleaves	nodeBytes	leafBytes	totalBytes	avgNodeBytes	avgLeafBytes	minNodeBytes	maxNodeBytes	minLeafBytes	maxLeafBytes
 * lex	1024	1	1	474	7913	3371370	3379283	7913	7112	7913	7913	5274	12774
 * </pre>
 *
 * BSBM 200M: This is the best time and space so far. using a byte counter rather than a short.
 *
 * <pre>
 * Elapsed: 16338357ms
 * NumStatements: 198808848
 * NumDistinctVals: 45647082
 * TotalKeyBytes: 228235410
 * TotalValBytes: 11292849582
 * MaxCollisions: 3
 * TotalCollisions: 244042
 * Journal size: 16591683584 bytes
 * </pre>
 *
 * BSBM 200M: Note: I restarted this run after terminating yourkit so the results should be valid
 * (right?). The main changes are to use stringValue() to test for dateTime, to use the canonical
 * huffman coder for the leaf keys.
 *
 * <pre>
 * Elapsed: 20148506ms
 * NumStatements: 198808848
 * NumDistinctVals: 45647082
 * TotalKeyBytes: 228235410
 * TotalValBytes: 11292849582
 * MaxCollisions: 3
 * TotalCollisions: 244042
 * Journal size: 16591683584 bytes
 * </pre>
 *
 * BSBM 200M: raw records are compress if they are over 64 bytes long.
 *
 * <pre>
 * Elapsed: 18757003ms
 * NumStatements: 198808848
 * NumDistinctVals: 45647082
 * TotalKeyBytes: 228235410
 * TotalValBytes: 7910596818
 * MaxCollisions: 3
 * TotalCollisions: 244042
 * Journal size: 12270108672 bytes
 * </pre>
 *
 * BSBM 200M: literals LT 64 byte labels are assumed inlined into statement indices (except datatype
 * URIs).
 *
 * <pre>
 * Elapsed: 16193915ms
 * NumStatements: 198808848
 * NumDistinctVals: 43273381
 * NumShortLiterals: 2723662
 * TotalKeyBytes: 216366905
 * TotalValBytes: 7807037644
 * MaxCollisions: 3
 * TotalCollisions: 219542
 * Journal size: 11083186176 bytes
 * </pre>
 *
 * BSBM 200M: uris LT 64 byte localNames are assumed inlined into statement indices (plus datatype
 * literals LT 64 bytes).
 *
 * <pre>
 * Elapsed: 5699248ms
 * NumStatements: 198808848
 * NumDistinctVals: 12198222
 * NumShortLiterals: 32779032
 * NumShortURIs: 493520581
 * TotalKeyBytes: 60991110
 * TotalValBytes: 4944223808
 * MaxCollisions: 2
 * TotalCollisions: 17264
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M: one parser thread and one indexer thread.
 *
 * <pre>
 * Elapsed: 3724415ms
 * NumStatements: 198808848
 * NumDistinctVals: 12198222
 * NumShortLiterals: 32779032
 * NumShortBNodes: 0
 * NumShortURIs: 493520581
 * TotalKeyBytes: 60991110
 * TotalValBytes: 4944223808
 * MaxCollisions: 2
 * TotalCollisions: 17264
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * GC OH problem trying to run multiple parsers against BSBM 200M when split into 200 files.
 *
 * <pre>
 * valBufSize := 10000
 *     valQueueCapacity = 100
 *     maxDrain := 50
 *     nparserThreads := 2
 *     parserWorkQueue := 1000
 * </pre>
 *
 * BSBM 200M - this is 3x longer. This run did not have the GC OH problem, but GC had frequent 10%
 * spikes, which is a lot in comparison to our best run.
 *
 * <pre>
 *     valBufSize := 1000
 *     valQueueCapacity = 10
 *     maxDrain := 5
 *     nparserThreads := 4
 *     parserWorkQueue := 100
 *
 * Elapsed: 9589520ms
 * NumStatements: 198805837
 * NumDistinctVals: 12202052
 * NumShortLiterals: 32776100
 * NumShortBNodes: 0
 * NumShortURIs: 493514954
 * TotalKeyBytes: 61010260
 * TotalValBytes: 4945278396
 * MaxCollisions: 2
 * TotalCollisions: 17260
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M: split in 200 files. 69m versus best time so far of 62m. There is only one thread in
 * the pool, but the caller runs policy means that we are actually running two parsers. So, this is
 * not really the same as the best run, which was one parser running in the main thread with the
 * indexer running in another thread.
 *
 * <pre>
 * 	   valBufSize := 10000
 *     valQueueCapacity = 10
 *     maxDrain := 5
 *     nparserThreads := 1
 *     parserWorkQueue := 100
 *
 * Elapsed: 4119775ms
 * NumStatements: 198805837
 * NumDistinctVals: 12202052
 * NumShortLiterals: 32776100
 * NumShortBNodes: 0
 * NumShortURIs: 493514954
 * TotalKeyBytes: 61010260
 * TotalValBytes: 4945278396
 * MaxCollisions: 2
 * TotalCollisions: 17260
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M with 1M statement splits using the memory manager to buffer the data on the native
 * heap. This is the best score so far (compare with 3724415ms with one parser and one indexer
 * thread). For some reason, the #of distinct values and literals is slightly different for these
 * two runs. One other change in this run is that we always gzip the record since we can not
 * deserialize the record unless we know in advance whether or not it is compressed. Previous runs
 * had conditionally compressed based on the original byte[] value length and stored the compressed
 * record iff it was shorter. However, we can only conditionally compress if we use a header or bit
 * flag to indicate that the record is compressed. Peak memory manager use was 262M.
 *
 * <pre>
 * Elapsed: 2863898ms
 * NumStatements: 198805837
 * NumDistinctVals: 12,202,052
 * NumShortLiterals: 61,100,900
 * NumShortBNodes: 0
 * NumShortURIs: 493514954
 * TotalKeyBytes: 61010260
 * TotalValBytes: 4945376779
 * MaxCollisions: 2
 * TotalCollisions: 17260
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M using memory manager (high tide of 351M) and 5 parser threads (plus the main thread).
 * Heap usage is pretty controlled.
 *
 * <pre>
 * Elapsed: 2803451ms
 * NumStatements: 198805837
 * NumDistinctVals: 12202052
 * NumShortLiterals: 61100900
 * NumShortBNodes: 0
 * NumShortURIs: 493514954
 * TotalKeyBytes: 61010260
 * TotalValBytes: 4945376779
 * MaxCollisions: 2
 * TotalCollisions: 17260
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M. Using memory manager and only one parser thread. This does run significantly slower
 * (55m versus 47m with two parser threads). It might not be slower if we also ran against the
 * single source file (this ran against the split files) since each chunk placed onto the queue
 * would then be full, but I doubt that this will make that much difference.
 *
 * <pre>
 * Elapsed: 3300871ms
 * NumStatements: 198805837
 * NumDistinctVals: 12049125
 * NumShortLiterals: 61100900
 * NumShortBNodes: 0
 * NumShortURIs: 493514954
 * TotalKeyBytes: 60245625
 * TotalValBytes: 4877760110
 * MaxCollisions: 2
 * TotalCollisions: 16840
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * BSBM 200M. Using memory manager, one parser thread (the caller), and a single source file. The
 * question is whether we do better with a statement handler that is only flushed incrementally
 * (when full) compared to using 2 parsers and flushing each time we reach the end of a 1M statement
 * source file. Nope. This was 77 minutes. (This was a fair comparison since the source files for
 * the split sources are compressed. So we really do better with two parsers and split files)
 *
 * <pre>
 * /allocationCount=0
 * /bufferCapacity=1000
 * /bufferCount=232
 * /extent=243269632
 * /slotBytes=0
 * /userBytes=0
 * Elapsed: 4605950ms
 * NumStatements: 198808848
 * NumDistinctVals: 12198222
 * NumShortLiterals: 61103832
 * NumShortBNodes: 0
 * NumShortURIs: 493520581
 * TotalKeyBytes: 60991110
 * TotalValBytes: 4944322031
 * MaxCollisions: 2
 * TotalCollisions: 17264
 * Journal size: 7320764416 bytes
 * </pre>
 *
 * TODO Try with only N bytes worth of the SHA hash code, leaving some bits left over for
 * partitioning URIs, Literals, and BNodes (for told bnode mode) and for a counter to break ties
 * when there is a hash collision. We should wind up with an 8-12 byte termId which is collision
 * proof and very well distributed.
 *
 * <p>TODO Add bit flags at the front for {BLOB, URI, Literal, BNode} (BLOB being the odd one out).
 * If we move BLOBs out of the key range of other plain literals, or literals of a given language
 * code or datatype, then we can not do an ordered scan of the literals anymore which is inclusive
 * of the blobs. There is a similar consequence of moving small literals into the statement index.
 *
 * <p>If we inline small unicode values (<32 bytes) and reserve the TERM2ID index for large(r)
 * values then we can approach a situation in which it serves solely for blobs but with a tradeoff
 * in size (of the statement indices) versus indirection.
 *
 * <p>Large value promotion does not really let us handle large blobs (multi-megabytes) in s/o as a
 * 50 4M blobs would fill up a shard. There, I think that we need to give the control over to the
 * application and require it to write on a shared resource (shared file system, S3, etc). The value
 * inserted into the index would then be just the pathname in the shared file system or the URL of
 * the S3 resource. This breaks the ACID decision boundary though as the application has no means
 * available to atomically decide that the resource does not exist and hence create it. Even using a
 * conditional E-Tag on S3 would not work since it would have to have an index over the S3 entities
 * to detect a write-write conflict for the same data under different URLs.
 *
 * @author thompsonbry
 */
public class HashCollisionUtility {

  private static final Logger log = Logger.getLogger(HashCollisionUtility.class);

  /**
   * An index mapping <code>hashCode(Value)+counter : Value</code>. This provides a dictionary for
   * RDF {@link Value}s encountered when loading {@link Statement}s into the database. The counter
   * provides a simple mechanism for reconciling hash collisions.
   */
  private final BTree termsIndex;

  private final LexiconConfiguration<EmbergraphValue> conf;

  private final EmbergraphValueFactory vf;

  /** Counters for things that we track. */
  private static class Counters {

    /** #of statements visited. */
    private final AtomicLong nstmts = new AtomicLong();

    /**
     * The #of {@link URI}s whose <code>localName</code> was short enough that we decided to inline
     * them into the statement indices instead.
     */
    private final AtomicLong nshortURIs = new AtomicLong();

    /**
     * The #of {@link BNode}s whose <code>ID</code> was short enough that we decided to inline them
     * into the statement indices instead (this also counts blank nodes which are inlined because
     * they have integer or UUID IDs).
     */
    private final AtomicLong nshortBNodes = new AtomicLong();

    /**
     * The #of {@link Literal}s which were short enough that we decided to inline them into the
     * statement indices instead.
     */
    private final AtomicLong nshortLiterals = new AtomicLong();

    // private final ConcurrentWeakValueCacheWithBatchedUpdates<Value,
    // EmbergraphValue> valueCache;

    /**
     * The size of the hash collision set for the RDF Value with the most hash collisions observed
     * to date.
     */
    private final AtomicLong maxCollisions = new AtomicLong();

    /** The total #of hash collisions. */
    private final AtomicLong totalCollisions = new AtomicLong();

    // /**
    // * The #of RDF {@link Value}s which were found in the {@link
    // #valueCache},
    // * thereby avoiding a lookup against the index.
    // */
    // private final AtomicLong ncached = new AtomicLong();

    /** The #of distinct RDF {@link Value}s inserted into the index. */
    private final AtomicLong ninserted = new AtomicLong();

    /** The total #of bytes in the generated B+Tree keys (leaves only). */
    private final AtomicLong totalKeyBytes = new AtomicLong();

    /** The total #of bytes in the serialized RDF Values. */
    private final AtomicLong totalValBytes = new AtomicLong();
  } // class Counters

  ////	private interface IHashCode {
  ////		void hashCode(IKeyBuilder keyBuilder,Object o);
  ////	}
  //
  //	private static class Int32HashCode { //implements IHashCode {
  //
  //		public void hashCode(IKeyBuilder keyBuilder, Object o) {
  //
  //			keyBuilder.append(o.hashCode());
  //
  //		}
  //
  //	}
  //
  //	private static class MessageDigestHashCode { //implements IHashCode {
  //
  //		final MessageDigest d;
  //
  //		MessageDigestHashCode() throws NoSuchAlgorithmException {
  //
  //			d = MessageDigest.getInstance("SHA-256"); // 256 bits (32 bytes)
  //
  //		}
  //
  //		public void hashCode(IKeyBuilder keyBuilder, final byte[] b) {
  //
  //			d.reset();
  //			d.digest(b);
  //			keyBuilder.append(d.digest());
  //
  //		}
  //
  //	}

  /** Lock used to coordinate {@link #shutdown()} and the {@link #valueQueue}. */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Latch which is incremented as we accept files to parse and decremented once a parser begins to
   * parse that file.
   */
  private final Latch parserQueueLatch = new Latch(lock);

  /**
   * Latch which is incremented once we begin to parse a file and decremented as the parser task
   * completes.
   */
  private final Latch parserRunLatch = new Latch(lock);

  /** Thread pool used to run the parser. */
  private final ExecutorService parserService;

  /** Thread pool used to run the parser and indexer. */
  private final ExecutorService indexerService;

  /** Class hooks the runnable to provide reporting on the outcome of the {@link FutureTask}. */
  private class ReportingFutureTask<V> extends FutureTask<V> {

    public final File file;

    public ReportingFutureTask(final File file, Callable<V> callable) {

      super(callable);

      this.file = file;

      parserQueueLatch.inc();
    }

    public void run() {

      try {

        parserRunLatch.inc();
        parserQueueLatch.dec();

        super.run();

        parserRunLatch.dec();

      } finally {

        report(this);
      }
    }

    /**
     * Callback is invoked when a {@link ParseFileTask} completes.
     *
     * @param task The future for that task.
     */
    protected void report(final ReportingFutureTask<?> task) {

      try {

        task.get();

        if (log.isDebugEnabled())
          log.debug(
              "Finished parsing: "
                  + task.file
                  + ", queueLatch="
                  + parserQueueLatch
                  + ", runLatch="
                  + parserRunLatch);

      } catch (ExecutionException ex) {

        log.error(ex, ex);

      } catch (InterruptedException e) {

        // propagate the interrupt.
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * A {@link Bucket} has an <code>unsigned byte[]</code> key and an unordered list of <code>long
   * </code> addrs for <code>byte[]</code> values. {@link Bucket} implements {@link Comparable} can
   * can be used to place an array of {@link Bucket}s into ascending key order.
   *
   * <p>TODO This is space efficient for large {@link Value}s, but it would not be efficient for
   * storing binding sets which hash to the same key. In the case of binding sets, the binding sets
   * are normally small. An extensible hash table would conserve space by dynamically determining
   * the #of hash bits in the address, and hence mapping the records onto a smaller #of pages.
   */
  private static class Bucket implements Comparable<Bucket> {

    /** The <code>unsigned byte[]</code> key. */
    public final byte[] key;

    /**
     * The list of addresses for this bucket.
     *
     * <p>TODO Collisions in a bucket are very rare given an int32 hash code, so this should be
     * optimized for the common case with a single address.
     */
    public final List<Long> addrs = new LinkedList<Long>();

    public Bucket(final byte[] key) {

      if (key == null) throw new IllegalArgumentException();

      this.key = key;
    }

    public Bucket(final byte[] key, final long addr) {

      this(key);

      addrs.add(addr);
    }

    /**
     * Add an address to this bucket.
     *
     * @param addr The address.
     */
    public void add(final long addr) {

      addrs.add(addr);
    }

    /** Order {@link Bucket}s into ascending <code>unsigned byte[]</code> key order. */
    public int compareTo(final Bucket o) {

      return BytesUtil.compareBytes(key, o.key);
    }
  }

  /**
   * A chunk of RDF {@link Value}s from the parser which are ready to be inserted into the TERMS
   * index.
   */
  private static class ValueBuffer {

    /** The allocation contexts which can be released once these data have been processed. */
    private final Set<IMemoryManager> contexts = new LinkedHashSet<IMemoryManager>();

    /**
     * The #of distinct records in the addrMap (this is more than the map size if there are hash
     * collisions since some buckets will have more than one entry).
     */
    private final int nvalues;

    /**
     * A map from the <code>unsigned byte[]</code> keys to the collision bucket containing the
     * address of each record for a given <code>unsigned byte[]</code> key.
     */
    private final Map<byte[] /* key */, Bucket> addrMap;

    /**
     * @param contexts The allocation contexts for the records in the addrMap.
     * @param nvalues The #of distinct records in the addrMap (this is more than the map size if
     *     there are hash collisions since some buckets will have more than one entry).
     * @param addrMap A map from the <code>unsigned byte[]</code> keys to the collision bucket
     *     containing the address of each record for a given <code>unsigned byte[]</code> key.
     */
    public ValueBuffer(
        final List<IMemoryManager> contexts, final int nvalues, final Map<byte[], Bucket> addrMap) {

      if (contexts == null) throw new IllegalArgumentException();

      if (addrMap == null) throw new IllegalArgumentException();

      this.contexts.addAll(contexts);

      this.nvalues = nvalues;

      this.addrMap = addrMap;
    }

    /**
     * Clear the address map and the {@link IMemoryManager} allocation contexts against which the
     * data were stored.
     */
    public void clear() {

      addrMap.clear();

      for (IMemoryManager context : contexts) {

        context.clear();
      }
    }

    public long getUserBytes() {

      long nbytes = 0L;

      for (IMemoryManager context : contexts) {

        nbytes += context.getUserBytes();
      }

      return nbytes;
    }
  } // class ValueBuffer

  /** Queue used to hand off {@link ValueBuffer}s from the parser to the indexer. */
  private BlockingQueue<ValueBuffer> valueQueue;

  /** Counters for things that we track. */
  private final Counters c = new Counters();

  /**
   * The upper bound on the size of a {@link ValueBuffer} chunk (currently in slotBytes for the
   * allocations against the {@link MemoryManager}).
   *
   * <p>The size of the chunks, the capacity of the queue, and the number of chunks that may be
   * combined into a single chunk for the indexer may all be used to adjust the parallelism and
   * efficiency of the parsing and indexing. You have to be careful not to let too much data onto
   * the Java heap, but the indexer will do better when it is given a bigger chunk since it can
   * order the data and be more efficient in the index updates.
   */
  final int valBufSize = Bytes.megabyte32 * 10; // 100000;

  /** Capacity of the {@link #valueQueue}. */
  final int valQueueCapacity = 10;

  /**
   * Maximum #of chunks to drain from the {@link #valueQueue} in one go. This bounds the largest
   * chunk that we will index at one go. You can remove the limit by specifying {@link
   * Integer#MAX_VALUE}.
   */
  final int maxDrain = 5;

  /** The size of the read buffer when reading a file. */
  final int fileBufSize = 1024 * 8; // default 8k

  /**
   * How many parser threads to use. There can be only one parser per file, but you can parse more
   * than one file at a time.
   */
  final int nparserThreads = 1;

  /**
   * The size of the work queue for the {@link #parserService}.
   *
   * <p>Note: This should be large enough that we will not wait around forever if the caller is
   * forced to parse a file rather than scan the file system for the next file to be parsed. This
   * hack is introduced by the need to handle a {@link RejectedExecutionException} from the {@link
   * #parserService}. We do that by forcing the parse task to run in the caller's thread. Another
   * choice would be for the caller to catch the {@link RejectedExecutionException}, wait a bit, and
   * then retry.
   */
  final int parserWorkQueueCapacity = 100;

  /**
   * A direct memory heap used to buffer RDF {@link Value}s which will be inserted into the TERMS
   * index. A distinct child {@link IMemoryManager} context is created by the {@link
   * StatementHandler} each time it needs to buffer data. The {@link StatementHandler} monitors the
   * size of the allocation context to decide when it is "big enough" to be transferred onto the
   * {@link #valueQueue}. The indexer eventually obtains the context from the {@link #valueQueue}.
   * Once the indexer is done with a context, it {@link IMemoryManager#clear() clears} the context.
   * The total memory across the allocation contexts is released back to the {@link
   * DirectBufferPool} in {@link #shutdown()} and {@link #shutdownNow()} and no later than when the
   * {@link #mmgr} is finalized.
   */
  final MemoryManager mmgr;

  /** The #of buffers to give to the {@link MemoryManager}. */
  private final int nbuffers = 1000;

  private HashCollisionUtility(final Journal jnl) {

    this.termsIndex = getTermsIndex(jnl);

    /*
     * Setup the parser thread pool. If there is an attempt to run more
     * threads then
     *
     * Note: The pool size is one less than the total #of specified threads
     * since the caller will wind up running tasks rejected by the pool. If
     * the pool would be empty then it is [null] and the caller will run
     * the parser in its own thread.
     *
     * Note: The work queue is bounded so that we do not read any too far in
     * the file system. The #of threads is bounded so that we do not run too
     * many parsers at once. However, running multiple parsers can increase
     * throughput as the parser itself caps out at ~ 68k tps.
     */
    if (nparserThreads > 1) {
      // this.parserService =
      // Executors.newFixedThreadPool(nparserThreads);
      final int corePoolSize = nparserThreads - 1;
      final int maximumPoolSize = nparserThreads - 1;
      final long keepAliveTime = 60;
      final TimeUnit unit = TimeUnit.SECONDS;
      final BlockingQueue<Runnable> workQueue =
          new LinkedBlockingQueue<Runnable>(parserWorkQueueCapacity);
      //			final BlockingQueue<Runnable> workQueue = new SynchronousQueue<Runnable>();
      this.parserService =
          new ThreadPoolExecutor(
              corePoolSize,
              maximumPoolSize,
              keepAliveTime,
              unit,
              workQueue,
              new ThreadPoolExecutor.CallerRunsPolicy());
    } else {
      /*
       * The caller must run the parser in its own thread.
       */
      this.parserService = null;
    }

    // But they all feed the same indexer.
    this.indexerService = Executors.newSingleThreadExecutor();

    // *blocking* queue of ValueBuffers to be indexed
    this.valueQueue = new LinkedBlockingQueue<ValueBuffer>(valQueueCapacity); // lock);

    vf = EmbergraphValueFactoryImpl.getInstance("test");

    final BaseVocabulary vocab;
    try {
      vocab =
          (BaseVocabulary)
              Class.forName(AbstractTripleStore.Options.DEFAULT_VOCABULARY_CLASS)
                  .getDeclaredConstructor(String.class)
                  .newInstance(vf.getNamespace());
      vocab.init();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // factory does not support any extensions.
    final IExtensionFactory xFactory =
        new IExtensionFactory() {

          @Override
          public void init(
              final IDatatypeURIResolver resolver,
              final ILexiconConfiguration<EmbergraphValue> config) {
            // NOP
          }

          @Override
          @SuppressWarnings("rawtypes")
          public Iterator<IExtension<? extends EmbergraphValue>> getExtensions() {
            return Collections.emptyIterator();
          }
        };

    final InlineURIFactory uriFactory = new InlineURIFactory();
    uriFactory.init(vocab);

    /*
     * Note: This inlines everything *except* xsd:dateTime, which
     * substantially reduces the data we will put into the index.
     *
     * @todo Do a special IExtension implementation to handle xsd:dateTime
     * since the DateTimeExtension uses the LexiconRelation to do its work.
     */
    conf =
        new LexiconConfiguration<EmbergraphValue>(
            256, // blobsThreshold
            true, // inlineXSDDatatypeLiterals
            true, // inlineTextLiterals
            64, // maxInlineStringLength
            true, // inlineBNodes
            false, // inlineDateTimes
            TimeZone.getDefault(), // inlineDateTimesTimeZone
            false, // rejectInvalidXSDValues
            false, // rawRecordSupport
            xFactory, // extension factory
            vocab, // predefined vocabulary
            vf,
            uriFactory,
            false, // GeoSpatial support
            null // GeoSpatial config
            );

    //		valueCache = new ConcurrentWeakValueCacheWithBatchedUpdates<Value, EmbergraphValue>(
    //				50000 // hard reference queue capacity
    //				);

    mmgr = new MemoryManager(DirectBufferPool.INSTANCE, nbuffers);
  }

  /** Start the task which will index data as it is parsed. */
  public void start() {

    lock.lock();
    try {

      if (indexerTask != null) throw new IllegalStateException();

      // start indexer.
      indexerTask = new FutureTask<Void>(new IndexerMainTask());

      indexerService.submit(indexerTask);

      // allow parsers to run.
      parsing.set(true);

    } finally {

      lock.unlock();
    }
  }

  /**
   * Future for the task which drains the {@link #valueQueue} and indexes the {@link ValueBuffer}s
   * drained from that queue.
   */
  private FutureTask<Void> indexerTask;

  /** Flag is <code>true</code> while parsers are still running. */
  private final AtomicBoolean parsing = new AtomicBoolean(false);

  /**
   * Poison pill used to indicate that no more objects will be placed onto the {@link #valueQueue}.
   */
  private final ValueBuffer poisonPill =
      new ValueBuffer(new LinkedList<IMemoryManager>(), 0, new LinkedHashMap<byte[], Bucket>());

  /**
   * Normal shutdown. Running parsers will complete and their data will be indexed, but new parsers
   * will not start. This method will block until all data has been indexed.
   *
   * @throws Exception
   */
  public void shutdown() throws Exception {
    log.debug("shutting down...");
    lock.lock();
    try {
      if (log.isDebugEnabled()) log.debug("Waiting on parserQueueLatch: " + parserQueueLatch);
      parserQueueLatch.await();
      if (parserService != null) {
        // no new parsers may start
        parserService.shutdown();
      }
      if (log.isDebugEnabled()) log.debug("Waiting on parserRunLatch: " + parserRunLatch);
      parserRunLatch.await();
      // no parsers should be running.
      parsing.set(false);
      // drop a poison pill on the queue.
      log.debug("Inserting poison pill.");
      valueQueue.put(poisonPill);
      if (indexerTask != null) {
        // wait for the indexer to finish.
        indexerTask.get();
      }
      if (indexerService != null) indexerService.shutdown();
      if (mmgr != null) {
        if (log.isInfoEnabled()) log.info(mmgr.getCounters().toString());
        mmgr.clear();
      }
    } finally {
      lock.unlock();
    }
    log.debug("all done.");
  }

  /**
   * Immediate shutdown. Running tasks will be canceled.
   *
   * @throws Exception
   */
  public void shutdownNow() throws Exception {
    log.debug("shutdownNow");
    parsing.set(false);
    if (parserService != null) parserService.shutdownNow();
    if (indexerService != null) indexerService.shutdownNow();
    if (indexerTask != null) {
      indexerTask.cancel(true /* mayInterruptIfRunning */);
    }
    if (mmgr != null) {
      mmgr.clear();
    }
  }

  /**
   * Task drains the valueQueue and runs an {@link IndexerTask} each time something is drained from
   * that queue.
   *
   * @author thompsonbry
   */
  private class IndexerMainTask implements Callable<Void> {

    public Void call() throws Exception {

      boolean done = false;

      while (!done) {

        try {

          // Blocking take so we know that there is something ready.
          final ValueBuffer first = valueQueue.take();

          // Drain queue, but keep an eye out for that poison pill.
          final LinkedList<ValueBuffer> coll = new LinkedList<ValueBuffer>();

          // The element we already took from the queue.
          coll.add(first);

          // Drain (non-blocking).
          final int ndrained = valueQueue.drainTo(coll, maxDrain) + 1;

          if (log.isInfoEnabled())
            log.info(
                "Drained "
                    + ndrained
                    + " chunks with "
                    + valueQueue.size()
                    + " remaining in the queue.");

          // look for and remove poison pill, noting if found.
          if (coll.remove(poisonPill)) {

            if (log.isDebugEnabled()) log.debug("Found poison pill.");

            done = true;

            // fall through and index what we already have.

          }

          if (!coll.isEmpty()) {

            // combine the buffers into a single chunk.
            final ValueBuffer b = combineChunks(coll);

            if (log.isDebugEnabled())
              log.debug(
                  "Will index "
                      + coll.size()
                      + " chunks having "
                      + b.nvalues
                      + " values in "
                      + b.getUserBytes()
                      + " bytes");

            // Now index that chunk.
            new IndexValueBufferTask(mmgr, b, termsIndex, vf, c).call();
          }

        } catch (Throwable t) {

          log.error(t, t);

          HashCollisionUtility.this.shutdownNow();

          throw new RuntimeException(t);
        }
      } // while(!done)

      log.debug("done.");

      return null;
    }

    /** Combine chunks from the queue into a single chunk. */
    private ValueBuffer combineChunks(final LinkedList<ValueBuffer> coll) {

      final ValueBuffer b;

      if (coll.size() == 1) {

        // There is only one chunk.
        b = coll.getFirst();

      } else {

        // Combine together into a single chunk.
        int nvalues = 0;

        for (ValueBuffer t : coll) nvalues += t.nvalues;

        final List<IMemoryManager> contexts = new LinkedList<IMemoryManager>();
        final LinkedHashMap<byte[], Bucket> addrMap = new LinkedHashMap<byte[], Bucket>();

        //				int off = 0;

        for (ValueBuffer t : coll) {

          contexts.addAll(t.contexts);

          nvalues += t.nvalues;

          for (Bucket bucket : t.addrMap.values()) {

            final Bucket tmp = addrMap.get(bucket.key);

            if (tmp == null) {

              // copy bucket.
              addrMap.put(bucket.key, bucket);

            } else {

              // merge bucket.
              tmp.addrs.addAll(bucket.addrs);
            }
          }

          //					System
          //					.arraycopy(t.keys/* src */, 0/* srcPos */,
          //							keys/* dest */, off/* destPos */,
          //							t.nvalues/* length */);
          //
          //					System
          //					.arraycopy(t.addrs/* src */, 0/* srcPos */,
          //							addrs/* dest */, off/* destPos */,
          //							t.nvalues/* length */);
          //
          //					off += t.nvalues;

        }

        b = new ValueBuffer(contexts, nvalues, addrMap);
      }

      return b;
    }
  } // class IndexerMainTask

  /**
   * Return the index in which we store RDF {@link Value}s.
   *
   * @param jnl The index manager.
   * @return The index.
   */
  /*
  	 * TODO CanonicalHuffmanRabaCoder for U1 drops the average leaf size
  	 *
  	 * @ m=512 from 24k to 16k. Experiment with performance tradeoff
  	 * when compared with gzip of the record.
  	 *
  	 * No apparent impact for U1 on the leaves or nodes for 32 versus 8
  	 * on the front-coded raba.
  	 *
  	 * Dropping maxRecLen from 256 to 64 reduces the leaves from 16k to
  	 * 10k. Dropping it to ZERO (0) reduces the leaves to 5k. This
  	 * suggests that we could to much better if we keep all RDF Values
  	 * out of the index. In standalone, we can give people a TermId
  	 * which is the raw record address. However, in scale-out it needs
  	 * to be the key (to locate the shard) and we will resolve the RDF
  	 * Value using the index on the shard.
  	 *
  	 * Suffix compression would allow us to generalize the counter and
  	 * avoid index space costs when collisions are rare while being able
  	 * to tolerate more collisions (short versus byte).

  U1: m=800, q=8000, ratio=8, maxRecLen=0,
  Elapsed: 41340ms
  NumStatements: 1000313
  NumDistinctVals: 291259
  TotalKeyBytes: 1747554
  TotalValBytes: 60824514
  MaxCollisions: 1
  TotalCollisions: 6
  Journal size: 209715200 bytes
  Average node: 9813
  Average leaf: 6543

  U1: m=800, q=8000, ratio=32, maxRecLen=0,
  Elapsed: 40971ms
  NumStatements: 1000313
  NumDistinctVals: 291259
  TotalKeyBytes: 1747554
  TotalValBytes: 60824514
  MaxCollisions: 1
  TotalCollisions: 6
  Journal size: 209715200 bytes
  Average node: 9821
  Average leaf: 6478

  U1: m=800, q=8000, ratio=64, maxRecLen=0,
  Elapsed: 41629ms
  NumStatements: 1000313
  NumDistinctVals: 291259
  TotalKeyBytes: 1747554
  TotalValBytes: 60824514
  MaxCollisions: 1
  TotalCollisions: 6
  Journal size: 209715200 bytes
  Average node: 9822
  Average leaf: 6467

  U1: m=512, q=8000, ratio=32, maxRecLen=0,
  Elapsed: 44722ms
  NumStatements: 1000313
  NumDistinctVals: 291259
  TotalKeyBytes: 1747554
  TotalValBytes: 60824514
  MaxCollisions: 1
  TotalCollisions: 6
  Journal size: 209715200 bytes
  Average node/leaf: 3969	4149

  U1: m=512, q=8000, ratio=32, maxRecLen=0,
  Elapsed: 40519ms
  NumStatements: 1000313
  NumDistinctVals: 291259
  TotalKeyBytes: 1747554
  TotalValBytes: 60824514
  MaxCollisions: 1
  TotalCollisions: 6
  Journal size: 209715200 bytes
  Average node/leaf, node(min/max), leaf(min/max): 7583	8326	7583	7583	5755	14660

  It would be great if we tracked the node/leaf data live on the RWStore for
  these counters so it could all be reported periodically (via http) or at the
  end in a summary.

  TODO The front compression of the keys is not helping out much since the keys
  are so sparse in the hash code space.  It is a Good Thing that the keys are so
  sparse, but this suggests that we should try a different coder for the leaf keys.
  	 */
  private BTree getTermsIndex(final Journal jnl) {

    final String name = "TERMS";

    BTree ndx = jnl.getIndex(name);

    final int m = 1024;
    final int q = 8000;
    final int ratio = 32;
    final int maxRecLen = 0;
    if (ndx == null) {

      final IndexMetadata md = new IndexMetadata(name, UUID.randomUUID());

      md.setNodeKeySerializer(new FrontCodedRabaCoder(ratio));

      final DefaultTupleSerializer tupleSer =
          new DefaultTupleSerializer(
              new DefaultKeyBuilderFactory(new Properties()),
              /*
               * leaf keys
               */
              //					DefaultFrontCodedRabaCoder.INSTANCE,
              new FrontCodedRabaCoder(ratio),
              //					CanonicalHuffmanRabaCoder.INSTANCE,
              /*
               * leaf values
               */
              CanonicalHuffmanRabaCoder.INSTANCE
              //					new SimpleRabaCoder()
              );

      md.setTupleSerializer(tupleSer);

      // enable raw record support.
      md.setRawRecords(true);

      // set the maximum length of a byte[] value in a leaf.
      md.setMaxRecLen(maxRecLen);

      /*
       * increase the branching factor since leaf size is smaller w/o
       * large records.
       */
      md.setBranchingFactor(m);

      // Note: You need to give sufficient heap for this option!
      md.setWriteRetentionQueueCapacity(q);

      ndx = jnl.registerIndex(name, md);
    }

    return ndx;
  }

  private void parseFileOrDirectory(final File fileOrDir, final RDFFormat fallback)
      throws Exception {

    if (fileOrDir.isDirectory()) {

      final File[] files = fileOrDir.listFiles();

      for (int i = 0; i < files.length; i++) {

        final File f = files[i];

        parseFileOrDirectory(f, fallback);
      }

      return;
    }

    final File f = fileOrDir;

    final String n = f.getName();

    RDFFormat fmt = RDFFormat.forFileName(n, fallback);

    if (fmt == null && n.endsWith(".zip")) {
      fmt = RDFFormat.forFileName(n.substring(0, n.length() - 4), fallback);
    }

    if (fmt == null && n.endsWith(".gz")) {
      fmt = RDFFormat.forFileName(n.substring(0, n.length() - 3), fallback);
    }

    if (fmt == null) {
      log.warn("Ignoring: " + f);
      return;
    }

    final StatementHandler stmtHandler =
        new StatementHandler(valBufSize, c, conf, vf, mmgr, valueQueue, parsing);

    final FutureTask<Void> ft =
        new ReportingFutureTask<Void>(
            f, new ParseFileTask(f, fallback, fileBufSize, vf, stmtHandler));

    if (parserService != null) {
      // run on the thread pool.
      parserService.submit(ft);
    } else {
      // Run in the caller's thread.
      ft.run();
      // Test the Future.
      ft.get();
    }
  }

  /**
   * Task parses a single file.
   *
   * @author thompsonbry
   */
  private static class ParseFileTask implements Callable<Void> {

    private final File file;
    private final RDFFormat fallback;
    private final int fileBufSize;
    private final EmbergraphValueFactory vf;
    private final StatementHandler stmtHandler;

    public ParseFileTask(
        final File file,
        final RDFFormat fallback,
        final int fileBufSize,
        final EmbergraphValueFactory vf,
        final StatementHandler stmtHandler) {

      if (file == null) throw new IllegalArgumentException();

      if (stmtHandler == null) throw new IllegalArgumentException();

      this.file = file;

      this.fallback = fallback;

      this.fileBufSize = fileBufSize;

      this.vf = vf;

      this.stmtHandler = stmtHandler;
    }

    public Void call() throws Exception {

      parseFile(file);

      return null;
    }

    private void parseFile(final File file)
        throws IOException, RDFParseException, RDFHandlerException, NoSuchAlgorithmException,
            InterruptedException {

      if (!file.exists()) throw new RuntimeException("Not found: " + file);

      final RDFFormat format = RDFFormat.forFileName(file.getName(), fallback);

      if (format == null) throw new RuntimeException("Unknown format: " + file);

      if (log.isTraceEnabled()) log.trace("RDFFormat=" + format);

      final RDFParserFactory rdfParserFactory = RDFParserRegistry.getInstance().get(format);

      if (rdfParserFactory == null) throw new RuntimeException("No parser for format: " + format);

      final RDFParser rdfParser = rdfParserFactory.getParser();

      rdfParser.setValueFactory(vf);

      rdfParser.setVerifyData(false);

      rdfParser.setStopAtFirstError(false);

      rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

      rdfParser.setRDFHandler(stmtHandler);

      /*
       * Run the parser, which will cause statements to be inserted.
       */

      if (log.isDebugEnabled()) log.debug("Parsing: " + file);

      InputStream is = new FileInputStream(file);

      try {

        is = new BufferedInputStream(is, fileBufSize);

        final boolean gzip = file.getName().endsWith(".gz");

        if (gzip) is = new GZIPInputStream(is);

        final String baseURI = file.toURI().toString();

        // parse the file
        rdfParser.parse(is, baseURI);

      } finally {

        is.close();
      }
    }
  }

  /** Helper class adds statements to the sail as they are visited by a parser. */
  private static class StatementHandler extends RDFHandlerBase {

    //    	private static final transient Logger log = HashCollisionUtility.log;

    /** Various counters that we track. */
    private final Counters c;

    /** The lexicon configuration. */
    private final LexiconConfiguration<EmbergraphValue> conf;

    /**
     * Blocking queue to which we add {@link ValueBuffer} instances as they are generated by the
     * parser.
     */
    final BlockingQueue<ValueBuffer> valueQueue;

    /**
     * <code>true</code> iff the parser is permitted to run and <code>false</code> if the parser
     * should terminate.
     */
    final AtomicBoolean parsing;

    /** Used to build the keys (just a hash code). */
    private final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

    /** Used to serialize RDF Values as byte[]s. */
    private final DataOutputBuffer out = new DataOutputBuffer();

    /** Used to serialize RDF Values as byte[]s. */
    private final ByteArrayBuffer tbuf = new ByteArrayBuffer();

    /** Used to serialize RDF Values as byte[]s. */
    private final EmbergraphValueSerializer<EmbergraphValue> valSer;

    /**
     * Used to (de-)compress the raw values.
     *
     * <p>Note: This is not thread-safe, even for decompression. You need a pool or thread-local
     * instance to support concurrent reads against the TERMS index.
     */
    private final RecordCompressor compressor = new RecordCompressor(Deflater.BEST_SPEED);

    //		/** Buffer for (serialized) RDF Values. */
    //		private KV[] values;

    /** #of buffered values. */
    private int nvalues = 0;

    /** The memory manager. */
    private final IMemoryManager memoryManager;

    /** The current allocation context. */
    private IMemoryManager context = null;

    /**
     * Map of distinct values in the buffer.
     *
     * <p>TODO In addition to enforcing DISTINCT over the Values in the ValueBuffer, an LRU/LIRS
     * cache would be nice here so we can reuse the frequently resolved (EmbergraphValue => IV)
     * mappings across buffer instances.
     *
     * <p>FIXME We need to provide a canonicalizing mapping for blank nodes.
     *
     * <p>TODO The key should also include the URI,Literal,BNode, etc. prefix bits (or is this
     * necessary any more?).
     */
    private Map<byte[] /*key*/, Bucket> addrMap;

    /** The size of the {@link #values} buffer when it is allocated. */
    private final int valueBufSize;

    public StatementHandler(
        final int valueBufSize,
        final Counters c,
        final LexiconConfiguration<EmbergraphValue> conf,
        final EmbergraphValueFactory vf,
        final IMemoryManager memoryManager,
        final BlockingQueue<ValueBuffer> valueQueue,
        final AtomicBoolean parsing) {

      this.valueBufSize = valueBufSize;

      this.c = c;

      this.conf = conf;

      this.memoryManager = memoryManager;

      this.valueQueue = valueQueue;

      this.parsing = parsing;

      this.valSer = vf.getValueSerializer();
    }

    public void endRDF() {

      if (log.isTraceEnabled()) log.trace("End of source.");

      try {

        flush();

      } catch (InterruptedException e) {

        throw new RuntimeException(e);
      }
    }

    public void handleStatement(final Statement stmt) throws RDFHandlerException {

      if (!parsing.get()) {
        // Either shutdown or never started.
        throw new IllegalStateException();
      }

      try {

        bufferValue((EmbergraphValue) stmt.getSubject());

        bufferValue((EmbergraphValue) stmt.getPredicate());

        bufferValue((EmbergraphValue) stmt.getObject());

        if (stmt.getContext() != null) {

          bufferValue((EmbergraphValue) stmt.getContext());
        }

      } catch (InterruptedException ex) {

        // Interrupted while blocked on the valueQueue
        throw new RDFHandlerException(ex);
      }

      c.nstmts.incrementAndGet();
    }

    /**
     * If the RDF {@link Value} can not be represented inline within the statement indices, then
     * buffer the value for batch resolution against the TERMS index.
     *
     * @param value The RDF {@link Value}.
     * @return A {@link Value}. If the caller's {@link Value} could be represented as an inline
     *     {@link IV}, then the returned value will be a {@link EmbergraphValue} and the inline
     *     {@link IV} will be available from {@link EmbergraphValue#getIV()}. Otherwise the caller's
     *     {@link Value} is returned and the {@link Value} must be resolved against the TERMS index
     *     in order to obtain its {@link IV}.
     * @throws InterruptedException
     *     <p>FIXME Handle {@link BlobIV}, {@link PartlyInlineURIIV}, and {@link
     *     PartlyInlineTypedLiteralIV}. These are three kinds of "non-inline" values. They will have
     *     to be queued for insertion into the TERMS index and Statement which depend on those
     *     non-inline values will have to be deferred until we have resolved those non-inline
     *     values. This is basically the same logic that we already have for StatementBuffer, except
     *     that an asynchronous queue is being used (by this class) to do the resolution of the IV
     *     for large values.
     *     <p>Other kinds of {@link IV}s which could be handled here would be references to large
     *     values stored in the file system, in S3, etc.
     */
    private void bufferValue(final EmbergraphValue value) throws InterruptedException {

      // Not expecting the IV to already be cached.
      assert value.getIV() == null;

      // Attempt to inline this value.
      final IV<?, ?> iv = conf.createInlineIV(value);

      if (iv != null) {

        // This is being inlined.

        switch (iv.getVTE()) {
          case URI:
            c.nshortURIs.incrementAndGet();
            break;
          case BNODE:
            c.nshortBNodes.incrementAndGet();
            break;
          case LITERAL:
            c.nshortLiterals.incrementAndGet();
            break;
          default:
            throw new AssertionError();
        }

        // Verify IV is cached on that Value.
        assert value.getIV() == iv;

        return;
      }

      if (context != null && context.getSlotBytes() >= valueBufSize) {

        // Incremental flush of large values to the TERMS index.
        flush();
      }

      if (context == null) {

        // Lazy allocation of the buffer.
        context = memoryManager.createAllocationContext();

        addrMap = new LinkedHashMap<byte[], Bucket>();
      }

      /*
       * Generate a key (hash code) and value (serialized and compressed)
       * from the EmbergraphValue.
       */
      final KV t = makeKV(value);

      /*
       * Lookup the list of addresses for RDF Values which hash to the
       * same key.
       */
      Bucket bucket = addrMap.get(t.key);

      if (bucket == null) {

        /*
         * No match on that hash code key.
         */

        // lay the record down on the memory manager.
        final long addr = context.allocate(ByteBuffer.wrap(t.val));

        // add new bucket to the map.
        addrMap.put(t.key, bucket = new Bucket(t.key, addr));

        nvalues++;

      } else {

        /*
         * Either a hash collision or the value is already stored at
         * a known address.
         */
        {
          for (Long addr : bucket.addrs) {

            if (context.allocationSize(addr) != t.val.length) {

              // Non-match based on the allocated record size.
              continue;
            }

            /*
             * TODO It would be more efficient to compare the data
             * using the zero-copy get(addr) method.
             */
            final byte[] tmp = context.read(addr);

            if (BytesUtil.bytesEqual(t.val, tmp)) {

              // We've already seen this Value.

              if (log.isDebugEnabled())
                log.debug("Duplicate value in chunk: " + Arrays.toString(t.val));

              /*
               * FIXME This pattern does not really work out for
               * building statements since we lack a reference to
               * the Value which is being inserted into the TERMS
               * index. The StatementBuffer handles this. It keeps
               * the Values in a map and inserts all values into
               * the database. [It should only keep the distinct
               * non-inline values but it currently keeps all
               * distinct values without regard to inlining.]
               */
              return;
            }
          }

          // Fall through - there is no such record on the store.

        }

        // lay the record down on the memory manager.
        bucket.add(context.allocate(ByteBuffer.wrap(t.val)));

        nvalues++;
      }

      return;
    } // bufferValue()

    /**
     * Transfer a non-empty buffer to the {@link #valueQueue}.
     *
     * @throws InterruptedException
     */
    void flush() throws InterruptedException {

      if (nvalues == 0) return;

      if (!parsing.get()) {
        // Either shutdown or never started.
        throw new IllegalStateException();
      }

      if (log.isInfoEnabled())
        log.info(
            "Adding chunk with "
                + nvalues
                + " values and "
                + context.getUserBytes()
                + " bytes to queue.");

      /*
       * Create an object which encapsulates the allocation context (to be
       * cleared when the data have been consumed) and the address map.
       */

      final List<IMemoryManager> contexts = new LinkedList<IMemoryManager>();
      contexts.add(context);

      // put the buffer on the queue (blocking operation).
      valueQueue.put(new ValueBuffer(contexts, nvalues, addrMap));

      // clear reference since we just handed off the data.
      context = null;
      addrMap = null;
      nvalues = 0;

      // clear distinct value set so it does not build for ever.
      //			distinctValues.clear();
      //			addrMap.clear();

    }

    private KV makeKV(final EmbergraphValue r) {

      byte[] val = valSer.serialize(r, out.reset(), tbuf);

      /*
       * FIXME In order support conditional compression  we will have to
       * mark the record with a header to indicate whether or not
       * it is compressed. Without that header we can not
       * deserialize a record resolved via its TermId since we
       * will not know whether or not it is compressed (actually,
       * that could be part of the termId....)
       */
      if (compressor != null) { // && val.length > 64) {

        // compress, reusing [out].
        out.reset();
        compressor.compress(val, out);
      }

      //			if (out.pos() < val.length) // TODO Use compressed version iff smaller.
      {
        val = out.toByteArray();
      }

      /*
       * Note: This is an exclusive lower bound (it does not include the
       * counter).
       *
       * TODO We could format the counter in here as a ZERO (0) since it
       * is a fixed length value and then patch it up later. That would
       * involve less copying.
       */
      final byte[] key = buildKey(r, val).getKey();

      return new KV(key, val);
    } // makeKV()

    private IKeyBuilder buildKey(final Value r, final byte[] val) {

      //		if (true) {

      /*
       * Simple 32-bit hash code based on the byte[] representation of
       * the RDF Value.
       */

      final int hashCode = r.hashCode();

      return keyBuilder.reset().append(hashCode);

      //		} else {
      //
      //			/*
      //			 * Message digest of the serialized representation of the RDF
      //			 * Value.
      //			 *
      //			 * TODO There are methods to copy out the digest (hash code)
      //			 * without memory allocations. getDigestLength() and
      //			 * getDigest(out,start,len).
      //			 */
      //			private final MessageDigest d;
      //
      //			try {
      //
      //				d = MessageDigest.getInstance("SHA-256"); // 256 bits (32 bytes)
      //
      //			} catch (NoSuchAlgorithmException e) {
      //
      //				throw new RuntimeException(e);
      //
      //			}
      //
      //
      //			final byte[] hashCode = d.digest(val);
      //
      //			return keyBuilder.reset().append(hashCode);
      //
      //		}

    } // buildKey
  } // class StatementHandler

  /** Index a {@link ValueBuffer}. */
  private static class IndexValueBufferTask implements Callable<Void> {

    /** The {@link MemoryManager} against which the allocations were made. */
    private final MemoryManager mmgr;

    /** The data to be indexed. */
    private final ValueBuffer vbuf;

    /** The index to write on. */
    private final BTree termsIndex;

    /** Counters for things that we track. */
    private final Counters c;

    /** Used to build the keys. */
    private final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

    //		/** Used to serialize RDF Values as byte[]s. */
    //    	private final DataOutputBuffer out = new DataOutputBuffer();

    /** Used to de-serialize RDF Values (debugging only). */
    private final EmbergraphValueSerializer<EmbergraphValue> valSer;

    /**
     * Used to de-compress the raw values (debugging only).
     *
     * <p>Note: This is not thread-safe, even for decompression. You need a pool or thread-local
     * instance to support concurrent reads against the TERMS index.
     */
    private final RecordCompressor compressor;

    public IndexValueBufferTask(
        final MemoryManager mmgr,
        final ValueBuffer vbuf,
        final BTree termsIndex,
        final EmbergraphValueFactory vf,
        final Counters c) {

      if (mmgr == null) throw new IllegalArgumentException();

      if (vbuf == null) throw new IllegalArgumentException();

      if (termsIndex == null) throw new IllegalArgumentException();

      if (vf == null) throw new IllegalArgumentException();

      if (c == null) throw new IllegalArgumentException();

      this.mmgr = mmgr;
      this.vbuf = vbuf;
      this.termsIndex = termsIndex;
      this.c = c;

      /*
       * Note: debugging only.
       */
      this.valSer = vf.getValueSerializer();
      this.compressor = new RecordCompressor(Deflater.BEST_SPEED);
    }

    public Void call() throws Exception {

      final long begin = System.currentTimeMillis();

      if (log.isInfoEnabled())
        log.info(
            "Indexing " + vbuf.nvalues + " values occupying " + vbuf.getUserBytes() + " bytes");

      /*
       * Place into sorted order by the keys.
       *
       * The Bucket implements Comparable. We extract the buckets, sort
       * them, and then process them.
       */
      final Bucket[] a = vbuf.addrMap.values().toArray(new Bucket[0]);
      Arrays.sort(a);

      // Index the values.
      for (int i = 0; i < a.length; i++) {

        final Bucket b = a[i];

        // The key for that bucket.
        final byte[] baseKey = keyBuilder.reset().append(b.key).getKey();

        // All records for that bucket.
        for (long addr : b.addrs) {

          // Materialize the byte[] from the memory manager.
          final byte[] val = mmgr.read(addr);

          addValue(baseKey, val);
        }
      }

      if (log.isInfoEnabled()) {

        final long elapsed = System.currentTimeMillis() - begin;

        log.info(
            "Indexed "
                + vbuf.nvalues
                + " values occupying "
                + vbuf.getUserBytes()
                + " bytes in "
                + elapsed
                + "ms");
      }

      // release the address map and backing allocation context.
      vbuf.clear();

      return null;
    }

    /**
     * Insert a record into the TERMS index.
     *
     * @param baseKey The base key for the hash code (without the counter suffix).
     * @param val The (serialized and compressed) RDF Value.
     */
    private void addValue(final byte[] baseKey, final byte[] val) {

      /*
       * This is the fixed length hash code prefix. When a collision
       * exists we can either append a counter -or- use more bits from the
       * prefix. An extensible hash index works by progressively
       * increasing the #of bits from the hash code which are used to
       * create a distinction in the index. Records with identical hash
       * values are stored in an (unordered, and possibly chained) bucket.
       * We can approximate this by using N-bits of the hash code for the
       * key and then increasing the #of bits in the key when there is a
       * hash collision. Unless a hash function is used which has
       * sufficient bits available to ensure that there are no collisions,
       * we may be forced eventually to append a counter to impose a
       * distinction among records which are hash identical but whose
       * values differ.
       *
       * In the case of a hash collision, we can determine the records
       * which have already collided using the fast range count between
       * the hash code key and the fixed length successor of that key. We
       * can create a guaranteed distinct key by creating a BigInteger
       * whose values is (#collisions+1) and appending it to the key. This
       * approach will give us keys whose byte length increases slowly as
       * the #of collisions grows (though these might not be the minimum
       * length keys - depending on how we are encoding the BigInteger in
       * the key.)
       *
       * When we have a hash collision, we first need to scan all of the
       * collision records and make sure that none of those records has
       * the same value as the given record. This is done using the fixed
       * length successor of the hash code key as the exclusive upper
       * bound of a key range scan. Each record associated with a tuple in
       * that key range must be compared for equality with the given
       * record to decide whether or not the given record already exists
       * in the index.
       *
       * The fromKey is strictly LT any full key for the hash code of this
       * val but strictly GT any key have a hash code LT the hash code of
       * this val.
       *
       * TODO From [fromKey] and [toKey] could reuse a pair of buffers to
       * reduce heap churn, especially since they are FIXED length keys.
       * The fromKey would have to be formed more intelligently as we do
       * not have a version of SuccessorUtil#successor() which works with
       * a byte offset and length.
       */
      final byte[] fromKey = baseKey;

      // key strictly LT any successor of the hash code of this val.
      final byte[] toKey = SuccessorUtil.successor(fromKey.clone());

      // fast range count. this tells us how many collisions there are.
      // this is an exact collision count since we are not deleting tuples
      // from the TERMS index.
      final long rangeCount = termsIndex.rangeCount(fromKey, toKey);

      if (rangeCount >= Byte.MAX_VALUE) {

        /*
         * Impose a hard limit on the #of hash collisions we will accept
         * in this utility.
         *
         * @todo We do not need to have a hard limit if we use
         * BigInteger for the counter, but the performance will go
         * through the floor if we have to scan 32k entries on a hash
         * collision!
         */

        throw new RuntimeException("Too many hash collisions: ncoll=" + rangeCount);
      }

      // force range count into (signed) byte
      final byte counter = (byte) rangeCount;

      if (rangeCount == 0) {

        /*
         * This is the first time we have observed a Value which
         * generates this hash code, so append a [short] ZERO (0) to
         * generate the actual key and then insert the Value into the
         * index. Since there is nothing in the index for this hash
         * code, no collision is possible and we do not need to test the
         * index for the value before inserting the value into the
         * index.
         */
        final byte[] key = keyBuilder.reset().append(fromKey).appendSigned(counter).getKey();

        if (termsIndex.insert(key, val) != null) {

          throw new AssertionError();
        }

        c.ninserted.incrementAndGet();
        c.totalKeyBytes.addAndGet(key.length);
        c.totalValBytes.addAndGet(val.length);

        return;
      }

      /*
       * iterator over that key range
       *
       * TODO Filter for the value of interest so we can optimize the scan
       * by comparing with the value without causing it to be
       * materialized, especially we should be able to efficiently reject
       * tuples where the byte[] value length is known to differ from the
       * a given length, including when the value is stored as a raw
       * record at which point we are doing a fast rejection based on
       * comparing the byteCount(addr) for the raw record with the target
       * byte count for value that we are seeking in the index.
       *
       * We can visit something iff the desired tuple already exists (same
       * length, and possibly the same data). If we visit nothing then we
       * know that we have to insert a tuple and we know the counter value
       * from the collision count.
       */
      final ITupleIterator<?> itr =
          termsIndex.rangeIterator(
              fromKey, toKey, 0 /* capacity */, IRangeQuery.VALS, null /* filter */);

      boolean found = false;

      while (itr.hasNext()) {

        final ITuple<?> tuple = itr.next();

        // raw bytes
        final byte[] tmp = tuple.getValue();

        if (false) System.out.println(getValue(tmp));

        // Note: Compares the compressed values ;-)
        if (BytesUtil.bytesEqual(val, tmp)) {

          found = true;

          break;
        }
      }

      if (found) {

        // Already in the index.
        return;
      }

      /*
       * Hash collision.
       */

      if (rangeCount > c.maxCollisions.get()) {

        // Raise the maximum collision count.

        c.maxCollisions.set(rangeCount);

        log.warn("MAX COLLISIONS NOW: " + c.maxCollisions.get());
      }

      final byte[] key = keyBuilder.reset().append(fromKey).appendSigned(counter).getKey();

      // Insert into the index.
      if (termsIndex.insert(key, val) != null) {

        throw new AssertionError();
      }

      c.ninserted.incrementAndGet();
      c.totalKeyBytes.addAndGet(key.length);
      c.totalValBytes.addAndGet(val.length);

      c.totalCollisions.incrementAndGet();

      if (rangeCount > 128) { // arbitrary limit to log @ WARN.
        log.warn(
            "Collision: hashCode="
                + BytesUtil.toString(key)
                + ", nstmts="
                + c.nstmts
                + ", nshortLiterals="
                + c.nshortLiterals
                + ", nshortURIs="
                + c.nshortURIs
                + ", ninserted="
                + c.ninserted
                + ", totalCollisions="
                + c.totalCollisions
                + ", maxCollisions="
                + c.maxCollisions
                + ", ncollThisTerm="
                + rangeCount
                + ", resource="
                + getValue(val));
      } else if (log.isDebugEnabled())
        log.debug(
            "Collision: hashCode="
                + BytesUtil.toString(key)
                + ", nstmts="
                + c.nstmts
                + ", nshortLiterals="
                + c.nshortLiterals
                + ", nshortURIs="
                + c.nshortURIs
                + ", ninserted="
                + c.ninserted
                + ", totalCollisions="
                + c.totalCollisions
                + ", maxCollisions="
                + c.maxCollisions
                + ", ncollThisTerm="
                + rangeCount
                + ", resource="
                + getValue(val));
    }

    /**
     * Decompress and deserialize a {@link Value}.
     *
     * @param tmp The serialized and compressed value.
     * @return The {@link Value}.
     */
    private Value getValue(final byte[] tmp) {

      // decompress
      final ByteBuffer b = compressor.decompress(tmp);
      final byte[] c = new byte[b.limit()];
      b.get(c);

      // deserialize.
      return valSer.deserialize(c);
    }
  } // class IndexValueBufferTask

  /**
   * Parse files, inserting {@link Value}s into indices and counting hash collisions.
   *
   * @param args filename(s)
   * @throws IOException
   * @throws RDFHandlerException
   * @throws RDFParseException
   * @throws NoSuchAlgorithmException
   */
  public static void main(final String[] args) throws Exception {

    Banner.banner();

    // check args.
    {
      for (String filename : args) {

        final File file = new File(filename);

        if (!file.exists()) throw new RuntimeException("Not found: " + file);
      }
    }

    final long begin = System.currentTimeMillis();

    final Properties properties = new Properties();

    properties.setProperty(Journal.Options.BUFFER_MODE, BufferMode.DiskRW.toString());

    properties.setProperty(Journal.Options.INITIAL_EXTENT, "" + (Bytes.megabyte * 200));

    //		properties.setProperty(Journal.Options.COLLECT_PLATFORM_STATISTICS,"true");
    //		properties.setProperty(Journal.Options.COLLECT_QUEUE_STATISTICS,"true");
    properties.setProperty(Journal.Options.HTTPD_PORT, "8081");

    // The caller MUST specify the filename using -D on the command line.
    final String journalFile = System.getProperty(Journal.Options.FILE);

    if (journalFile == null) {

      System.err.println("Journal file must be specified: -D" + Journal.Options.FILE);

      System.exit(1);
    }

    properties.setProperty(Journal.Options.FILE, journalFile);

    if (new File(journalFile).exists()) {

      System.err.println("Removing old journal: " + journalFile);

      new File(journalFile).delete();
    }

    final Journal jnl = new Journal(properties);

    final RDFFormat fallback = RDFFormat.N3;

    HashCollisionUtility u = null;
    try {

      u = new HashCollisionUtility(jnl);

      u.start();

      for (String filename : args) {

        u.parseFileOrDirectory(new File(filename), fallback);
      }

      //			// flush anything left in the buffer.
      //			u.stmtHandler.flush();

      // shutdown and block until all data is indexed.
      u.shutdown();

      jnl.commit();

    } catch (Throwable t) {

      u.shutdownNow();

      throw new RuntimeException(t);

    } finally {

      jnl.close();

      final long elapsed = System.currentTimeMillis() - begin;

      System.out.println("Elapsed: " + elapsed + "ms");

      if (u != null) {

        System.out.println("NumStatements: " + u.c.nstmts);

        System.out.println("NumDistinctVals: " + u.c.ninserted);

        System.out.println("NumShortLiterals: " + u.c.nshortLiterals);

        System.out.println("NumShortBNodes: " + u.c.nshortBNodes);

        System.out.println("NumShortURIs: " + u.c.nshortURIs);

        //				System.out.println("NumCacheHit: " + u.ncached);

        System.out.println("TotalKeyBytes: " + u.c.totalKeyBytes);

        System.out.println("TotalValBytes: " + u.c.totalValBytes);

        System.out.println("MaxCollisions: " + u.c.maxCollisions);

        System.out.println("TotalCollisions: " + u.c.totalCollisions);
      }

      if (new File(journalFile).exists()) {

        System.out.println("Journal size: " + new File(journalFile).length() + " bytes");
      }
    }
  }
}
