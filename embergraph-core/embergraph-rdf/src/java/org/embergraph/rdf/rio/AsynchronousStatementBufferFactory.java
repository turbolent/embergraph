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
 * Created on Jun 25, 2009
 */

package org.embergraph.rdf.rio;

import cutthecrap.utils.striterators.Filter;
import cutthecrap.utils.striterators.Striterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;
import org.embergraph.btree.AsynchronousIndexWriteConfiguration;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KVO;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.proc.IAsyncResultHandler;
import org.embergraph.btree.proc.LongAggregator;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.Instrument;
import org.embergraph.counters.OneShotInstrument;
import org.embergraph.io.ByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.lexicon.AssignTermId;
import org.embergraph.rdf.lexicon.BlobsIndexHelper;
import org.embergraph.rdf.lexicon.BlobsWriteProc;
import org.embergraph.rdf.lexicon.BlobsWriteProc.BlobsWriteProcConstructor;
import org.embergraph.rdf.lexicon.EmbergraphValueCentricFullTextIndex;
import org.embergraph.rdf.lexicon.Id2TermWriteProc.Id2TermWriteProcConstructor;
import org.embergraph.rdf.lexicon.LexiconKeyBuilder;
import org.embergraph.rdf.lexicon.LexiconKeyOrder;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.lexicon.Term2IdTupleSerializer;
import org.embergraph.rdf.lexicon.Term2IdWriteProc;
import org.embergraph.rdf.lexicon.Term2IdWriteProc.Term2IdWriteProcConstructor;
import org.embergraph.rdf.model.EmbergraphBNode;
import org.embergraph.rdf.model.EmbergraphBNodeImpl;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueSerializer;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPOIndexWriteProc;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.spo.SPORelation;
import org.embergraph.rdf.spo.SPOTupleSerializer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.ScaleOutTripleStore;
import org.embergraph.relation.accesspath.IRunnableBuffer;
import org.embergraph.relation.accesspath.UnsynchronizedUnboundedChunkBuffer;
import org.embergraph.search.TextIndexWriteProc;
import org.embergraph.service.AbstractFederation;
import org.embergraph.service.Split;
import org.embergraph.service.ndx.IScaleOutClientIndex;
import org.embergraph.service.ndx.pipeline.DefaultDuplicateRemover;
import org.embergraph.service.ndx.pipeline.KVOC;
import org.embergraph.service.ndx.pipeline.KVOLatch;
import org.embergraph.service.ndx.pipeline.KVOList;
import org.embergraph.striterator.ChunkedWrappedIterator;
import org.embergraph.striterator.IChunkedIterator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.embergraph.striterator.IKeyOrder;
import org.embergraph.util.Bytes;
import org.embergraph.util.DaemonThreadFactory;
import org.embergraph.util.concurrent.Latch;
import org.embergraph.util.concurrent.ShutdownHelper;
import org.embergraph.util.concurrent.ThreadPoolExecutorBaseStatisticsTask;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;

/*
 * Factory object for high-volume RDF data load.
 *
 * <p>The asynchronous statement buffer w/o SIDs is much simpler that w/. If we require that the
 * document is fully buffered in memory, then we can simplify this to just:
 *
 * <pre>
 *
 * Given:
 *
 * value[] - RDF Values observed in the S,P,O, or C positions.
 *
 * statement[] - RDF Statements reported by the parser.
 *
 * Do:
 *
 * value[] =&gt; TERM2ID (Sync RPC, assigning TIDs)
 * value[] =&gt; BLOBS   (Sync RPC, assigning TIDs)
 *
 * value[] =&gt; ID2TERM (Async)
 *
 * value[] =&gt; Text (Async, iff enabled)
 *
 * statement[] =&gt; (SPO,POS,OSP) (Async)
 * </pre>
 *
 * Note: This DOES NOT support truth maintenance. Truth maintenance requires that the term
 * identifiers are resolved against the database's lexicon while the statements are written onto a
 * local (and temporary) triple store. There is no (or at least less) reason to use asynchronous
 * writes against a local store. However, TM could use this to copy the data from the temporary
 * triple store to the database. This should be plugged in transparently in the copyStatements() API
 * for the tripleStore.
 *
 * <p>Note: This DOES NOT support SIDS.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @param <S> The generic type of the statement objects.
 * @param <R> The generic type of the resource identifier (File, URL, etc).
 *     <p>FIXME Modify to support SIDs. We basically need to loop in the {@link
 *     #workflowLatch_bufferTids} workflow state until all SIDs have been assigned. However, the
 *     termination conditions will be a little more complex. During termination, if we have the TIDs
 *     but not yet the SIDs then we need to flush the SID requests rather than allowing them to
 *     timeout. Since SID processing is cyclic, we may have to do this one or more times.
 *     <pre>
 * AsynchronousStatementBufferWithSids:
 *
 * When SIDs are enabled, we must identify the minimum set of statements
 * whose SIDs are referenced by blank nodes in the S, P, O positions of
 * other statements.  Since we can not make that determination until we
 * reach the end of the document, all statements which use blank nodes
 * are placed into the deferredStatements container.
 *
 * Further, and unlike the synchronous StatementBuffer, we must defer
 * writes of grounded statements until we know whether or not their SID
 * appears in a blank node reference by another statement.  We MUST use
 * synchronous RPC to obtain the SIDs for those statements.  This means
 * that the entire document MUST be parsed into memory.  Since we must
 * buffer the entire document in memory when SIDs are enabled (when using
 * asynchronous writes), distinct implementations of the asynchronous
 * statement buffer are used depending on whether or not SIDs are
 * enabled. [Actually, we fully buffer anyway so we can use the same
 * implementation class.]
 *
 * Once the end of the document has been reached, we iteratively divide
 * the parsed statements into three collections.  This process ends once
 * all three collections are empty.
 *
 *    1. groundedStatements : These are statements which are not
 *       referenced by other statements using their SID and which do not
 *       contain references to the SIDs of other statements. The
 *       groundedStatements are written asynchronously since there is no
 *       dependency on their SIDs.
 *
 *    2. referencedStatements : These are statements whose SID has not
 *       been assigned yet and which do not reference other statements
 *       but which are themselves referenced by other statements using a
 *       blank node. These statements are written using synchronous RPC
 *       so that we may obtain their SIDs and thereby convert one or more
 *       deferredStatements to either groundedStatements or
 *       referencedStatements.
 *
 *    3. deferredStatements : These are statements using a blank node to
 *       reference another statement whose SID has not been assigned yet.
 *       These statements MAY also be referenced by other deferred
 *       statements.  However, those references MAY NOT form a cycle.
 *       Deferred statements are moved to either the groundedStatements
 *       or the referencedStatements collection once their blank node
 *       references have been assigned SIDs.
 *
 * Given:
 *
 * value[] - RDF Values observed in the S, P, O, and C positions.
 *
 * unresolvedRefs[] - RDF blank nodes observed in the C position are
 *            entered into this collection.  They are removed
 *            from the collection as they are resolved.
 *
 * statement[] - RDF Statements reported by the parser.
 *
 * Do:
 *
 * // remove blank nodes serving as SIDs from the value[].
 * value[] := value[] - unresolvedRef[];
 *
 * value[] =&gt; TERM2ID (Sync RPC, assigning TIDs)
 *
 * value[] =&gt; ID2TERM (Async)
 *
 * value[] =&gt; Text (Async, iff enabled)
 *
 * // initially, all statements are deferred.
 * deferredStatements := statements;
 *
 * while(!groundedStatements.isEmpty() &amp;&amp; !referencedStatements.isEmpty()
 *    &amp;&amp; !deferredStatements.isEmpty()) {
 *
 *   groundedStatement[] =&gt; TERM2ID (async)
 *
 *   groundedStatement[] := []; // empty.
 *
 *   referencedStatement[] =&gt; TERM2ID (Sync RPC, assigning SIDs)
 *
 *   foreach spo : referencedStatements {
 *
 *     unresolvedRefs.remove( spo.c );
 *
 *   }
 *
 *   referencedStatement[] := []; // empty.
 *
 *   foreach spo : deferredStatement[i] {
 *
 *      if(spo.isGrounded) {
 *
 *         // true iff S.tid, P.tid, and O.tid are bound, implying that
 *         // this statement does not have any unresolved references to
 *         // other statements.
 *
 *     if(unresolvedReferences.contains(spo.c)) {
 *
 *         // will be written synchronously.
 *         referencedStatements.add( spo );
 *
 *     } else {
 *
 *         // will be written asynchronously.
 *         groundedStatement.add( spo );
 *
 *     }
 *
 *      }
 *
 *   }
 *
 * }
 * </pre>
 *
 * @todo evaluate this approach for writing on a local triple store. if there is a performance
 *     benefit then refactor accordingly (requires asynchronous write API for BTree and friends).
 */
public class AsynchronousStatementBufferFactory<S extends EmbergraphStatement, R>
    implements IAsynchronousWriteStatementBufferFactory<S> {

  private static final transient Logger log =
      Logger.getLogger(AsynchronousStatementBufferFactory.class);

  /** The database into which the statements will be written. */
  private final ScaleOutTripleStore tripleStore;

  /** The lexicon. */
  private final LexiconRelation lexiconRelation;

  /** The triples. */
  private final SPORelation spoRelation;

  /** The initial capacity of the canonicalizing mapping for RDF {@link Value}. */
  private final int valuesInitialCapacity;

  /** The initial capacity of the canonicalizing mapping for RDF {@link BNode}s. */
  private final int bnodesInitialCapacity;

  /*
   * The chunk size used by the producer to break the terms and statements into chunks before
   * writing them onto the {@link BlockingBuffer} for the master.
   */
  private final int producerChunkSize;

  /** The default {@link RDFFormat}. */
  private final RDFFormat defaultFormat;

  /*
   * The value that will be used for the graph/context co-ordinate when loading data represented in
   * a triple format into a quad store.
   */
  private final String defaultGraph;

  /** Options for the {@link RDFParser}. */
  private final RDFParserOptions parserOptions;

  /** Delete files after they have been successfully loaded when <code>true</code>. */
  private final boolean deleteAfter;

  /** Delete files after they have been successfully loaded when <code>true</code>. */
  protected boolean isDeleteAfter() {

    return deleteAfter;
  }

  /** The default RDF interchange format that will be used when the format can not be determined. */
  protected RDFFormat getDefaultRDFFormat() {

    return defaultFormat;
  }

  /*
   * When <code>true</code> and the full text index is enabled, then also index datatype literals.
   */
  private final boolean indexDatatypeLiterals;

  /*
   * Asynchronous index write buffers.
   */

  private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer_t2id;
  private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer_id2t;
  private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer_blobs;
  private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer_text;

  /** A map containing an entry for each statement index on which this class will write. */
  private final Map<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> buffer_stmts;

  /*
   * Counts statements written on the database (applied only to the SPO index so we do not double
   * count).
   */
  private final LongAggregator statementResultHandler = new LongAggregator();

  /** Counts tuples written on the full text index. */
  private final LongAggregator textResultHandler = new LongAggregator();

  /*
   * The timestamp set when {@link #notifyStart()} is invoked. This is done when the factory is
   * created.
   */
  private volatile long startTime;

  /*
   * The timestamp set when {@link #notifyEnd()} is invoked. This is done when the factory is {@link
   * #close()}d or when execution is {@link #cancelAll(boolean) cancelled}.
   */
  private long endTime;

  /*
   * Notify that the factory will begin running tasks. This sets the {@link #startTime} used by
   * {@link #getElapsedMillis()} to report the run time of the tasks.
   */
  protected void notifyStart() {

    /*
     * Note: uses the lock to make this atomic since we do this when we
     * accept each document and we already own the lock at that point.
     */
    if (!lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException();

    if (startTime == 0L) {

      endTime = 0L;

      startTime = System.currentTimeMillis();
    }
  }

  /*
   * Notify that the factory is done running tasks (for now). This places a cap on the time reported
   * by {@link #elapsed()}.
   */
  protected void notifyEnd() {

    endTime = System.currentTimeMillis();

    parserService.shutdownNow();

    tidsWriterService.shutdownNow();

    otherWriterService.shutdownNow();

    notifyService.shutdownNow();

    if (serviceStatisticsTask != null) {

      serviceStatisticsTask.cancel();
    }
  }

  /*
   * The elapsed milliseconds, counting only the time between {@link #notifyStart()} and {@link
   * #notifyEnd()}.
   */
  public long getElapsedMillis() {

    if (startTime == 0L) return 0L;

    if (endTime == 0L) {

      return System.currentTimeMillis() - startTime;
    }

    return endTime - startTime;
  }

  /*
   * Cumulative counters. These do not need to be protected by the lock as
   * they do not guard transitions between workflow states.
   */

  /** The #of documents that have been parsed (cumulative total). */
  private final AtomicLong documentsParsedCount = new AtomicLong(0L);

  /** The #of documents whose TIDs have been assigned (cumulative total). */
  private final AtomicLong documentTIDsReadyCount = new AtomicLong(0L);

  /*
   * The #of documents that are waiting on their TIDs (current value). The counter is incremented
   * when a document begins to buffer writes on the TERM2ID/BLOBS indices. The counter is
   * decremented as soon as those writes are restart safe.
   *
   * <p>Note: The {@link #workflowLatch_bufferTids} is only decremented when the document begins to
   * write on the other indices, so {@link #documentTIDsWaitingCount} will be decremented before
   * {@link #workflowLatch_bufferTids}. The two counters will track very closely unless the {@link
   * #otherWriterService} has a backlog.
   *
   * <p>Note: The {@link #workflowLatch_bufferOther} is decremented as soon as the writes on the
   * other indices are restart safe since there is no transition to another workflow state. This is
   * why there is no counter for "documentOtherWaitingCount".
   */
  private final AtomicLong documentTIDsWaitingCount = new AtomicLong(0L);

  /*
   * The #of told triples parsed from documents using this factory and made restart safe on the
   * database. This is incremented each time a document has been made restart safe by the #of
   * distinct told triples parsed from that document.
   *
   * <p>Note: The same triple can occur in more than one document, and documents having duplicate
   * triples may be loaded by distributed clients. The actual #of triples on the database is only
   * available by querying the database.
   */
  private final AtomicLong toldTriplesRestartSafeCount = new AtomicLong();

  /*
   * The #of documents which have been fully processed and are restart-safe on the database
   * (cumulative total).
   */
  private final AtomicLong documentRestartSafeCount = new AtomicLong();

  /** The #of documents for which the {@link BufferOtherWritesTask} failed. */
  private final AtomicLong documentErrorCount = new AtomicLong();

  /*
   * Latches. The latches guard transitions between workflow states and must
   * be protected by the lock.
   */

  /*
   * The {@link #lock} is used to makes the observable state changes for the factory atomic and
   * guards the termination conditions in {@link #close()}. You MUST own the {@link #lock} when
   * incrementing or decrementing any of the {@link Latch}s. The {@link Latch} transitions must be
   * accomplished while you are holding the lock. For example, the transition between <i>parsing</i>
   * and <i>buffering TERM2ID/BLOBS writes</i> requires that we decrement one latch and increment
   * the other while hold the {@link #lock}.
   *
   * <p>The counter associated with each {@link Latch} indicates the total #of documents associated
   * with that workflow state but does not differentiate between documents waiting on the work queue
   * for the corresponding thread pool (e.g., the {@link #parserService}), documents assigned to a
   * worker thread and running in the thread pool, and documents waiting for some state change
   * (e.g., the return from an asynchronous write) before they can be transferred to the next
   * workflow state. However, you can gain additional information about the various thread pools
   * from their counters, including their work queue size, the #of active tasks, etc.
   */
  private final ReentrantLock lock = new ReentrantLock();

  /*
   * A global {@link Latch} guarding all documents which have been accepted for processing and have
   * not yet reached an absorbing state (either an error state or been made restart safe).
   */
  private final Latch workflowLatch_document = new Latch("document", lock);

  /*
   * A {@link Latch} guarding documents which have been accepted for parsing but have not been
   * transferred to the {@link #workflowLatch_bufferTids}.
   *
   * @todo We could add a resolver latch for network IO required to buffer the document locally.
   *     E.g., a read from a DFS or a web page.
   */
  private final Latch workflowLatch_parser = new Latch("parser", lock);

  /*
   * A {@link Latch} guarding documents that have begun to buffering their writes on the
   * TERM2ID/BLOBS indices but have not been transferred to the {@link #workflowLatch_bufferOther}.
   */
  private final Latch workflowLatch_bufferTids = new Latch("bufferTids", lock);

  /*
   * A {@link Latch} guarding documents that have begun to buffer their writes on the other indices
   * but have not yet completed their processing.
   */
  private final Latch workflowLatch_bufferOther = new Latch("bufferOther", lock);

  /*
   * Latches used to guard tasks buffering writes. There is one such latch for
   * TERM2ID/BLOBS and one for the rest of the buffers. During close() we will close
   * the buffers to flush their writes as soon as these latches hit zero.
   *
   * Note: These latches allow us to close the buffers in a timely manner. The
   * other latches guard the workflow state transitions. However, if we do not
   * close the buffers in a timely manner then close() will hang until a chunk
   * or idle timeout (if any) causes the buffers to be flushed!
   */

  /*
   * {@link Latch} guarding tasks until they have buffered their writes on the TERM2ID/BLOBS
   * indices. This latch is decremented as soon as the writes for a given document have been
   * buffered. This is used to close the TERM2ID/BLOBS buffers in a timely manner in {@link
   * #close()}.
   */
  private final Latch guardLatch_term2Id = new Latch("guard_term2Id", lock);

  /*
   * {@link Latch} guarding tasks until they have buffered their writes on the remaining index
   * buffers. This latch is decremented as soon as the writes for a given document have been
   * buffered. This is used to close the other buffers in a timely manner in {@link #close()}.
   */
  private final Latch guardLatch_other = new Latch("guard_other", lock);

  /** {@link Latch} guarding the notify service until all notices have been delivered. */
  private final Latch guardLatch_notify = new Latch("guard_notify", lock);

  /*
   * Parser service pause/resume.
   */

  /*
   * New parser tasks submitted to the {@link #parserService} will block when the {@link
   * #unbufferedStatementCount} is GT this value. This is used to control the RAM demand of the
   * parsed (but not yet buffered) statements. The RAM demand of the buffered statements is
   * controlled by the capacity of the master and sink queues on which those statements are
   * buffered.
   *
   * @todo it is possible that the buffered writes on term2id/blobs could limit throughput when the
   *     parser pool is paused since the decision to pause the parser pool is based on the #of
   *     unbuffered statements overall not just those staged for the term2id/blobs or the other
   *     indices.
   */
  private final long pauseParserPoolStatementThreshold;

  /*
   * The #of statements which have been parsed but not yet written onto the asynchronous index write
   * buffers. This is incremented when all statements for a given document have been parsed by the
   * #of distinct statements in that document. This is decremented when all statements for that
   * document have been placed onto the asynchronous index write buffers, or if processing fails for
   * that document. This is used to prevent new parser threads from overrunning the database when
   * the parsers are faster than the database.
   */
  private final AtomicLong unbufferedStatementCount = new AtomicLong();

  /*
   * The #of RDF {@link Statement}s that have been parsed but which are not yet restart safe on the
   * database. This is incremented when all statements for a given document have been parsed by the
   * #of distinct statements in that document. This is decremented when all writes for that document
   * have been made restart safe on the database, or if processing fails for that document. This may
   * be used as a proxy for the amount of data which is unavailable for garbage collection and thus
   * for the size of the heap entailed by processing.
   */
  private final AtomicLong outstandingStatementCount = new AtomicLong();

  /*
   * In order to prevent runaway demand on RAM, new parser tasks must await this {@link Condition}
   * if the #of parsed but not yet buffered statements is GTE the configured {@link
   * #pauseParserPoolStatementThreshold} threshold.
   */
  private Condition unpaused = lock.newCondition();

  /*
   * The #of threads which are currently paused awaiting the {@link #unpaused} {@link Condition}.
   */
  private AtomicLong pausedThreadCount = new AtomicLong();

  /** The #of times the {@link #parserService} has been paused. */
  private AtomicLong poolPausedCount = new AtomicLong();

  /*
   * Verify counters for latches which must sum atomically to the {@link #workflowLatch_document}.
   */
  private void assertSumOfLatchs() {

    if (!lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException();

    /*
     * Sum the latches for the distinct workflow states for a document
     * across all documents.
     */
    final long n1 =
        workflowLatch_parser.get()
            + workflowLatch_bufferTids.get()
            + workflowLatch_bufferOther.get();

    final long n2 = workflowLatch_document.get();

    if (n1 != n2) {

      throw new AssertionError(
          "Sum of Latches=" + n1 + ", but unfinished=" + n2 + " : " + getCounters().toString());
    }
  }

  /*
   * Bounded thread pool using a bounded work queue to run the parser tasks. If a backlog develops,
   * then the thread pool is <em>paused</em>, and new tasks will not start until the backlog is
   * cleared. This will cause the work queue to fill up, and the threads feeding that work queue to
   * block. This is done to place bounds on the memory demands of the total pipeline.
   */
  private final ParserThreadPoolExecutor parserService;

  /*
   * Bounded thread pool using an unbounded work queue to buffer writes for the TERM2ID/BLOBS
   * indices (these are the indices which assign tids). Tasks are added to the work queue by the
   * parser task in {@link AsynchronousStatementBufferImpl#flush()}.
   */
  private final ThreadPoolExecutor tidsWriterService;

  /*
   * Bounded thread pool using an unbounded work queue to run {@link BufferOtherWritesTask}s. Tasks
   * are added to the work queue by the "TIDs Ready" {@link KVOLatch}. Once the index writes have
   * been buffered, the statement buffer is placed onto the {@link #docsWaitingQueue}. This {@link
   * ExecutorService} MUST be unbounded since tasks will be assigned by {@link KVOLatch#signal()}
   * and that method MUST NOT block.
   */
  private final ThreadPoolExecutor otherWriterService;

  /*
   * Bounded thread pool with an unbounded work queue used process per file success or failure
   * notices.
   */
  private final ThreadPoolExecutor notifyService;

  /** {@link Runnable} collects performance counters on services used by the factory. */
  private final ServiceStatisticsTask serviceStatisticsTask;

  /*
   * Return an estimate of the #of statements written on the indices.
   *
   * <p>This value is aggregated across any {@link IStatementBuffer} obtained from {@link
   * #newStatementBuffer()} for this instance.
   *
   * <p>This value actually reports the #of statements written on the SPO index for the database.
   * Statements are written asynchronously in chunks and the writes MAY proceed at different rates
   * for each of the statement indices. The counter value will be stable once the {@link
   * #awaitAll()} returns normally.
   *
   * @see SPOIndexWriteProc
   */
  public long getStatementCount() {

    return statementResultHandler.getResult().longValue();
  }

  /** The #of documents submitted to the factory which could not be processed due to some error. */
  public long getDocumentErrorCount() {

    return documentErrorCount.get();
  }

  /** The #of documents submitted to the factory which have been processed successfully. */
  public long getDocumentDoneCount() {

    return documentRestartSafeCount.get();
  }

  /*
   * Note: do not invoke this directly. It does not know how to set the resource identifier on the
   * statement buffer impl.
   */
  public IStatementBuffer<S> newStatementBuffer() {

    return newStatementBuffer(null /* resource */);
  }

  protected AsynchronousStatementBufferImpl newStatementBuffer(final R resource) {

    return new AsynchronousStatementBufferImpl(resource);
  }

  /*
   * Submit a resource for processing.
   *
   * @param resource The resource (file or URL, but not a directory).
   * @throws Exception if there is a problem creating the parser task.
   * @throws RejectedExecutionException if the work queue for the parser service is full.
   */
  public void submitOne(final R resource) throws Exception {

    lock.lock();
    try {

      // Note: the parser task will obtain the lock when it runs.
      final Callable<?> task = newParserTask(resource);

      submitOne(resource, task);

    } finally {

      lock.unlock();
    }
  }

  /*
   * Inner method allows the caller to allocate the task once when the caller will retry if there is
   * a {@link RejectedExecutionException}.
   *
   * @param The resource (file or URL, but not a directory).
   * @param The parser task to run.
   * @throws Exception if there is a problem creating the parser task.
   * @throws RejectedExecutionException if the work queue for the parser service is full.
   */
  private void submitOne(final R resource, final Callable<?> task) throws Exception {

    if (resource == null) throw new IllegalArgumentException();

    if (task == null) throw new IllegalArgumentException();

    lock.lock();
    try {

      assertSumOfLatchs();

      notifyStart();

      /*
       * Note: The total processing of the documents will not terminate
       * until this latch has been decremented back to zero.
       */
      workflowLatch_document.inc();

      workflowLatch_parser.inc();

      assertSumOfLatchs();

      try {

        /*
         * Submit resource for parsing.
         *
         * @todo it would be nice to return a Future here that tracked the
         * document through the workflow.
         */

        parserService.submit(task);

      } catch (RejectedExecutionException ex) {

        /*
         * Back out the document since the task was not accepted for
         * execution.
         */

        //                lock.lock();
        //                try {
        assertSumOfLatchs();
        workflowLatch_document.dec();
        workflowLatch_parser.dec();
        assertSumOfLatchs();
        //                } finally {
        //                    lock.unlock();
        //                }

        throw ex;
      }

    } finally {

      lock.unlock();
    }
  }

  /*
   * Submit a resource for processing.
   *
   * @param resource The resource (file or URL, but not a directory).
   * @param retryMillis The number of milliseconds to wait between retries when the parser service
   *     work queue is full. When ZERO (0L), a {@link RejectedExecutionException} will be thrown out
   *     instead.
   * @throws Exception if there is a problem creating the parser task.
   * @throws RejectedExecutionException if the service is shutdown -or- the retryMillis is ZERO(0L).
   */
  public void submitOne(final R resource, final long retryMillis) throws Exception {

    if (resource == null) throw new IllegalArgumentException();

    if (retryMillis < 0) throw new IllegalArgumentException();

    int retryCount = 0;

    final long begin = System.currentTimeMillis();
    long lastLogTime = begin;

    // Note: the parser task will obtain the lock when it runs.
    final Callable<?> task = newParserTask(resource);

    while (true) {

      try {

        // submit resource for processing.
        submitOne(resource, task);

        return;

      } catch (RejectedExecutionException ex) {

        if (parserService.isShutdown()) {

          // Do not retry since service is closed.
          throw ex;
        }

        if (retryMillis == 0L) {

          // Do not retry since if retry interval is 0L.
          throw ex;
        }

        // sleep for the retry interval.
        Thread.sleep(retryMillis);

        retryCount++;

        if (log.isInfoEnabled()) {
          final long now = System.currentTimeMillis();
          final long elapsedSinceLastLogTime = now - lastLogTime;
          if (elapsedSinceLastLogTime > 5000) {
            final long elapsed = now - begin;
            lastLogTime = now;
            log.info(
                "Parser pool blocking: retryCount="
                    + retryCount
                    + ", elapsed="
                    + elapsed
                    + "ms, resource="
                    + resource);
            //                        log.info(getCounters().toString());
          }
        }

        // retry
        continue;

      } catch (InterruptedException ex) {

        throw ex;

      } catch (Exception ex) {

        log.error(resource, ex);
      }
    }
  }

  /*
   * Submit all files in a directory for processing via {@link #submitOne(String)}.
   *
   * @param fileOrDir The file or directory.
   * @param filter An optional filter. Only the files selected by the filter will be processed.
   * @param retryMillis The number of milliseconds to wait between retries when the parser service
   *     work queue is full. When ZERO (0L), a {@link RejectedExecutionException} will be thrown out
   *     instead.
   * @return The #of files that were submitted for processing.
   * @throws Exception
   */
  public int submitAll(final File fileOrDir, final FilenameFilter filter, final long retryMillis)
      throws Exception {

    return new RunnableFileSystemLoader(fileOrDir, filter, retryMillis).call();
  }

  /*
   * Open an buffered input stream reading from the resource. If the resource ends with <code>.gz
   * </code> or <code>.zip</code> then the appropriate decompression will be applied.
   *
   * @param resource The resource identifier.
   * @todo This will only read the first entry from a ZIP file. Archives need to be recognized as
   *     such by the driver and expanded into a sequence of parser calls with the input stream. That
   *     will require a different entry point since we can't close the {@link ZipInputStream} until
   *     we have read all the entries in that file. The {@link ZipInputStream} is likely not thread
   *     safe so the same parser thread would have to consume each of the entries even though they
   *     must also be dealt with as distinct documents. Given all that, reading more than the first
   *     entry might not be worth it.
   */
  protected InputStream getInputStream(final R resource) throws IOException {

    InputStream is;

    if (resource instanceof File) {

      is = new FileInputStream((File) resource);

      final String name = ((File) resource).getName();

      if (name.endsWith(".gz")) {

        is = new GZIPInputStream(is);

      } else if (name.endsWith(".zip")) {

        is = new ZipInputStream(is);
      }

    } else if (resource instanceof URL) {

      is = ((URL) resource).openStream();

    } else {

      throw new UnsupportedOperationException();
    }

    return is;
  }

  /*
   * Return a task to parse the document. The task should allocate an {@link
   * AsynchronousStatementBufferImpl} for the document. When that buffer is flushed, the document
   * will be queued for further processing.
   *
   * @param resource The resource to be parsed.
   * @return The task to execute.
   * @throws Exception
   */
  protected Callable<?> newParserTask(final R resource) throws Exception {

    final String resourceStr = resource.toString();

    if (log.isInfoEnabled()) log.info("resource=" + resourceStr);

    final RDFFormat defaultFormat = getDefaultRDFFormat();
    /* @todo This might be ignorant of .gz and .zip extensions.
     * @todo when resource is URL use reported MimeTYPE also.
     */
    final RDFFormat rdfFormat =
        (defaultFormat == null
            ? RDFFormat.forFileName(resourceStr)
            : RDFFormat.forFileName(resourceStr, defaultFormat));

    if (rdfFormat == null) {

      final String msg = "Could not determine interchange syntax - skipping : file=" + resource;

      log.error(msg);

      throw new RuntimeException(msg);
    }

    // Convert the resource identifier to a URL.
    final String baseURI;
    if (getClass().getResource(resourceStr) != null) {

      baseURI = getClass().getResource(resourceStr).toURI().toString();

    } else {

      baseURI = new File(resourceStr).toURI().toString();
    }

    return new ParserTask(resource, baseURI, rdfFormat);
  }

  /*
   * Tasks either loads a RDF resource or verifies that the told triples found in that resource are
   * present in the database. The difference between data load and data verify is just the behavior
   * of the {@link IStatementBuffer}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  protected class ParserTask implements Callable<Void> {

    /** The resource to be loaded. */
    private final R resource;

    /** The base URL for that resource. */
    private final String baseURL;

    /** The RDF interchange syntax that the file uses. */
    private final RDFFormat rdfFormat;

    /*
     * @param resource The resource to be loaded (a plain file or URL, but not a directory).
     * @param baseURL The base URL for that resource.
     * @param rdfFormat The RDF interchange syntax that the file uses.
     */
    public ParserTask(final R resource, final String baseURL, final RDFFormat rdfFormat) {

      if (resource == null) throw new IllegalArgumentException();

      if (baseURL == null) throw new IllegalArgumentException();

      this.resource = resource;

      this.baseURL = baseURL;

      this.rdfFormat = rdfFormat;
    }

    public Void call() throws Exception {
      // Note: buffer will be pass along from queue to queue.
      final AsynchronousStatementBufferImpl buffer =
          AsynchronousStatementBufferFactory.this.newStatementBuffer(resource);
      try {
        // open reader on the file.
        final InputStream rdfStream = getInputStream(resource);
        try {
          // Obtain a buffered reader on the input stream.
          final Reader reader = new BufferedReader(new InputStreamReader(rdfStream));
          try {
            // run the parser.
            new PresortRioLoader(buffer)
                .loadRdf(
                    reader,
                    baseURL,
                    rdfFormat,
                    defaultGraph == null ? baseURL : defaultGraph,
                    parserOptions);
          } finally {
            reader.close();
          }
        } finally {
          rdfStream.close();
        }
        lock.lock();
        try {
          // done parsing this document.
          documentsParsedCount.incrementAndGet();

          // new workflow state (code lifted from BufferTerm2IdWrites).
          //                    lock.lock();
          //                    try {
          guardLatch_term2Id.inc();
          //                    if(ENABLE_BLOBS)
          //                        guardLatch_term2Id.inc();
          workflowLatch_parser.dec();
          workflowLatch_bufferTids.inc();
          documentTIDsWaitingCount.incrementAndGet();
          assertSumOfLatchs();
          //                    } finally {
          //                        lock.unlock();
          //                    }

          // queue tasks to buffer writes on TERM2ID/BLOBS indices.
          tidsWriterService.submit(new BufferTidWrites(buffer));
          //                    if(ENABLE_BLOBS)
          //                    tidsWriterService
          //                            .submit(new BufferBlobsWrites(buffer));
          // increment #of outstanding statements (parsed but not restart safe).
          outstandingStatementCount.addAndGet(buffer.statementCount);
          // increment #of unbuffered statements.
          unbufferedStatementCount.addAndGet(buffer.statementCount);
        } finally {
          lock.unlock();
        }
      } catch (Throwable ex) {
        // error state.
        lock.lock();
        try {
          workflowLatch_parser.dec();
          documentError(resource, ex);
          throw new Exception(ex);
        } finally {
          lock.unlock();
        }
      }
      // done.
      if (log.isInfoEnabled()) log.info("resource=" + resource + " : " + this);
      return null;
    }
  } // ParserTask

  public String toString() {

    return super.toString() + "::" + getCounters();
  }

  /*
   * @param tripleStore
   * @param producerChunkSize The chunk size used when writing chunks onto the master for the
   *     asynchronous index write API. If this value is on the order of the #of terms or statements
   *     in the parsed documents, then all terms / statements will be written onto the master in one
   *     chunk. The master will split the chunk based on the separator keys for the index partitions
   *     and write splits onto the sink for each index partition. The master and sink configuration
   *     is specified via the {@link IndexMetadata} when the triple store indices are created.
   * @param valuesInitialCapacity The initial capacity of the map of the distinct RDF {@link Value}s
   *     parsed from a single document.
   * @param bnodesInitialCapacity The initial capacity of the map of the distinct RDF {@link BNode}s
   *     parsed from a single document.
   * @param defaultFormat The default {@link RDFFormat} which will be assumed.
   * @param defaultGraph The value that will be used for the graph/context co-ordinate when loading
   *     data represented in a triple format into a quad store. If not given, then the context will
   *     be the resource identifier for the resource being parsed.
   * @param parserOptions Options for the {@link RDFParser}.
   * @param deleteAfter <code>true</code> if the resource should be deleted once the statements from
   *     that resource are restart safe on the target database.
   * @param parserPoolSize The #of worker threads in the thread pool for parsing RDF documents.
   * @param parserQueueCapacity The capacity of the bounded work queue for the service running the
   *     parser tasks.
   * @param term2IdWriterPoolSize The #of worker threads in the thread pool for buffering
   *     asynchronous writes on the TERM2ID/BLOBS indices.
   * @param otherWriterPoolSize The #of worker threads in the thread pool for buffering asynchronous
   *     index writes on the other indices.
   * @param notifyPoolSize The #of worker threads in the thread pool for handling document success
   *     and document error notices.
   * @param pauseParsedPoolStatementThreshold The maximum #of statements which can be parsed but not
   *     yet buffered before requests for new parser tasks are paused [0: {@link Long#MAX_VALUE}].
   *     This allows you to place a constraint on the RAM of the parsers. The RAM demand of the
   *     asynchronous index write buffers is controlled by their master and sink queue capacity and
   *     chunk size.
   */
  public AsynchronousStatementBufferFactory(
      final ScaleOutTripleStore tripleStore,
      final int producerChunkSize,
      final int valuesInitialCapacity,
      final int bnodesInitialCapacity,
      final RDFFormat defaultFormat,
      final String defaultGraph,
      final RDFParserOptions parserOptions,
      final boolean deleteAfter,
      final int parserPoolSize,
      final int parserQueueCapacity,
      final int term2IdWriterPoolSize,
      final int otherWriterPoolSize,
      final int notifyPoolSize,
      final long pauseParsedPoolStatementThreshold) {

    if (tripleStore == null) throw new IllegalArgumentException();
    if (parserOptions == null) throw new IllegalArgumentException();
    if (producerChunkSize <= 0) throw new IllegalArgumentException();
    if (valuesInitialCapacity <= 0) throw new IllegalArgumentException();
    if (bnodesInitialCapacity <= 0) throw new IllegalArgumentException();
    if (pauseParsedPoolStatementThreshold < 0) throw new IllegalArgumentException();

    this.tripleStore = tripleStore;

    this.lexiconRelation = tripleStore.getLexiconRelation();

    this.spoRelation = tripleStore.getSPORelation();

    this.producerChunkSize = producerChunkSize;

    this.valuesInitialCapacity = valuesInitialCapacity;

    this.bnodesInitialCapacity = bnodesInitialCapacity;

    this.defaultFormat = defaultFormat;

    this.defaultGraph = defaultGraph;

    this.parserOptions = parserOptions;

    this.deleteAfter = deleteAfter;

    this.pauseParserPoolStatementThreshold = pauseParsedPoolStatementThreshold;

    if (tripleStore.isStatementIdentifiers()) {

      throw new UnsupportedOperationException("SIDs not supported");
    }

    /*
     * Open the necessary buffers.
     *
     * Note: Lock is required by reopenBuffer_xxx() methods.
     */
    lock.lock();
    try {

      // TERM2ID/ID2TERM
      {
        final AsynchronousIndexWriteConfiguration config =
            tripleStore
                .getLexiconRelation()
                .getTerm2IdIndex()
                .getIndexMetadata()
                .getAsynchronousIndexWriteConfiguration();

        assertLiveness(lexiconRelation.getTerm2IdIndex().getIndexMetadata().getName(), config);

        buffer_t2id =
            ((IScaleOutClientIndex) lexiconRelation.getTerm2IdIndex())
                .newWriteBuffer(
                    new Term2IdWriteProcAsyncResultHandler(false /* readOnly */),
                    new DefaultDuplicateRemover<EmbergraphValue>(true /* testRefs */),
                    new Term2IdWriteProcConstructor(
                        false /* readOnly */,
                        lexiconRelation.isStoreBlankNodes(),
                        lexiconRelation.getTermIdBitsToReverse()));

        buffer_id2t =
            ((IScaleOutClientIndex) lexiconRelation.getId2TermIndex())
                .newWriteBuffer(
                    null /* resultHandler */,
                    new DefaultDuplicateRemover<EmbergraphValue>(true /* testRefs */),
                    Id2TermWriteProcConstructor.INSTANCE);
      }

      // BLOBS
      {
        final AsynchronousIndexWriteConfiguration config =
            tripleStore
                .getLexiconRelation()
                .getBlobsIndex()
                .getIndexMetadata()
                .getAsynchronousIndexWriteConfiguration();

        assertLiveness(lexiconRelation.getBlobsIndex().getIndexMetadata().getName(), config);

        buffer_blobs =
            ((IScaleOutClientIndex) lexiconRelation.getBlobsIndex())
                .newWriteBuffer(
                    new BlobsWriteProcAsyncResultHandler(false /* readOnly */),
                    new DefaultDuplicateRemover<EmbergraphValue>(true /* testRefs */),
                    new BlobsWriteProcConstructor(
                        false /* readOnly */, lexiconRelation.isStoreBlankNodes()));
      }

      // TEXT
      {
        if (lexiconRelation.isTextIndex()) {

          /*
           * FIXME Must hook in once the tids are available so we can
           * tokenize the RDF Literals (Note: only the literals, and
           * only those literals that will be indexed) and write out
           * the tuples on the text index.
           *
           * TODO Unit tests. Must enable the full text index and must
           * verify that both TermIds and BlobIVs were indexed. Inline
           * Unicode IVs also need to be indexed (they are small,
           * unless we change to [s] centric full text indexing).
           */

          final EmbergraphValueCentricFullTextIndex tmp =
              (EmbergraphValueCentricFullTextIndex) lexiconRelation.getSearchEngine();

          buffer_text =
              ((IScaleOutClientIndex) tmp.getIndex())
                  .newWriteBuffer(
                      textResultHandler, // counts tuples written on index
                      new DefaultDuplicateRemover<EmbergraphValue>(true /* testRefs */),
                      TextIndexWriteProc.IndexWriteProcConstructor.NO_OVERWRITE);

          indexDatatypeLiterals =
              Boolean.parseBoolean(
                  lexiconRelation
                      .getProperties()
                      .getProperty(
                          AbstractTripleStore.Options.TEXT_INDEX_DATATYPE_LITERALS,
                          AbstractTripleStore.Options.DEFAULT_TEXT_INDEX_DATATYPE_LITERALS));

        } else {

          buffer_text = null;
          indexDatatypeLiterals = false;
        }
      }

      /*
       * STATEMENT INDICES
       *
       * Allocate and populate map with the SPOKeyOrders that we will be
       * using.
       */
      {
        buffer_stmts =
            new LinkedHashMap<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>>(
                tripleStore.isQuads() ? 6 : 3);

        final Iterator<SPOKeyOrder> itr = tripleStore.getSPORelation().statementKeyOrderIterator();

        while (itr.hasNext()) {

          final SPOKeyOrder keyOrder = itr.next();

          final IRunnableBuffer<KVO<ISPO>[]> buffer =
              ((IScaleOutClientIndex) spoRelation.getIndex(keyOrder))
                  .newWriteBuffer(
                      keyOrder.isPrimaryIndex() ? statementResultHandler : null,
                      new DefaultDuplicateRemover<ISPO>(true /* testRefs */),
                      SPOIndexWriteProc.IndexWriteProcConstructor.INSTANCE);

          buffer_stmts.put(keyOrder, buffer);
        }
      }

    } finally {

      lock.unlock();
    }

    /*
     * Set iff this is a federation based triple store. The various queue
     * statistics are reported only for this case.
     */
    final AbstractFederation<?> fed;
    if (tripleStore.getIndexManager() instanceof AbstractFederation) {

      fed = (AbstractFederation<?>) tripleStore.getIndexManager();

    } else {

      fed = null;
    }

    /*
     * Note: This service must not reject tasks as long as the statement
     * buffer factory is open. It is configured with a bounded workQueue and
     * a bounded thread pool. The #of threads in the pool should build up to
     * the maximumPoolSize and idle threads will be retired, but only after
     * several minutes.
     */
    parserService =
        new ParserThreadPoolExecutor(
            1, // corePoolSize
            parserPoolSize, // maximumPoolSize
            1, // keepAliveTime
            TimeUnit.MINUTES, // keepAlive units.
            new LinkedBlockingQueue<Runnable>(parserQueueCapacity), // workQueue
            new DaemonThreadFactory(getClass().getName() + "_parserService") // threadFactory
            );

    /*
     * Note: This service MUST NOT block or reject tasks as long as the
     * statement buffer factory is open. It is configured with an
     * unbounded workQueue and a bounded thread pool. The #of threads in
     * the pool should build up to the maximumPoolSize and idle threads
     * will be retired, but only after several minutes.
     *
     * Note: Since we are using an unbounded queue, at most corePoolSize
     * threads will be created. Therefore we interpret the caller's argument
     * as both the corePoolSize and the maximumPoolSize.
     */
    tidsWriterService =
        new ThreadPoolExecutor(
            term2IdWriterPoolSize, // corePoolSize
            term2IdWriterPoolSize, // maximumPoolSize
            1, // keepAliveTime
            TimeUnit.MINUTES, // keepAlive units.
            new LinkedBlockingQueue<Runnable>(/* unbounded */ ), // workQueue
            new DaemonThreadFactory(getClass().getName() + "_term2IdWriteService") // threadFactory
            );

    /*
     * Note: This service MUST NOT block or reject tasks as long as the
     * statement buffer factory is open. It is configured with an unbounded
     * workQueue and a bounded thread pool. The #of threads in the pool
     * should build up to the maximumPoolSize and idle threads will be
     * retired, but only after several minutes.
     *
     * Note: Since we are using an unbounded queue, at most corePoolSize
     * threads will be created. Therefore we interpret the caller's argument
     * as both the corePoolSize and the maximumPoolSize.
     */
    otherWriterService =
        new ThreadPoolExecutor(
            otherWriterPoolSize, // corePoolSize
            otherWriterPoolSize, // maximumPoolSize
            1, // keepAliveTime
            TimeUnit.MINUTES, // keepAlive units.
            new LinkedBlockingQueue<Runnable>(/* unbounded */ ), // workQueue
            new DaemonThreadFactory(getClass().getName() + "_otherWriteService") // threadFactory
            );

    /*
     * Note: This service MUST NOT block or reject tasks as long as the
     * statement buffer factory is open. It is configured with an unbounded
     * workQueue and a bounded thread pool. The #of threads in the pool
     * should build up to the maximumPoolSize and idle threads will be
     * retired, but only after several minutes.
     *
     * Note: Since we are using an unbounded queue, at most corePoolSize
     * threads will be created. Therefore we interpret the caller's argument
     * as both the corePoolSize and the maximumPoolSize.
     */
    notifyService =
        new ThreadPoolExecutor(
            notifyPoolSize, // corePoolSize
            notifyPoolSize, // maximumPoolSize
            1, // keepAliveTime
            TimeUnit.MINUTES, // keepAlive units.
            new LinkedBlockingQueue<Runnable>(/* unbounded */ ), // workQueue
            new DaemonThreadFactory(getClass().getName() + "_notifyService") // threadFactory
            );

    /*
     * @todo If sampling should be done for non-federation cases then we
     * need to pass in the ScheduledExecutorService, expose a method to
     * start sampling on the caller's service, or create a
     * ScheduledExecutorService within this factory class.
     */
    serviceStatisticsTask =
        (fed == null ? null : new ServiceStatisticsTask(fed.getScheduledExecutorService()));
  } // ctor

  /*
   * Note: If there is a large sink idle timeout on the TERM2ID index then the sink will not flush
   * itself automatically once its master is no longer pushing data. This situation can occur any
   * time the parser pool is paused. A low sink idle timeout is required for the TERM2ID sink to
   * flush its writes to the database, so the TIDs will be assigned, statements for the parsed
   * documents will be buffered, and new parser threads can begin.
   *
   * @todo This should probably be automatically overridden for this use case. However, the
   *     asynchronous index configuration is not currently passed through with the requests but is
   *     instead global (on the IndexMetadata object for the index on the MDS).
   */
  private static void assertLiveness(
      final String name, final AsynchronousIndexWriteConfiguration config) {

    if (config.getSinkIdleTimeoutNanos() > TimeUnit.SECONDS.toNanos(60)) {

      log.error(
          "Large idle timeout will not preserve liveness: index=" + name + ", config=" + config);
    }
  }

  /** {@link Runnable} samples the services and provides reporting via {@link #getCounters()}. */
  private class ServiceStatisticsTask implements Runnable {

    private final Map<String, ThreadPoolExecutorBaseStatisticsTask> tasks =
        new LinkedHashMap<String, ThreadPoolExecutorBaseStatisticsTask>();

    private final ScheduledFuture<?> serviceStatisticsFuture;

    public ServiceStatisticsTask(final ScheduledExecutorService scheduledService) {

      /*
       * Add scheduled tasks to report the moving average of the queue
       * length, active count, etc. for the various services used by this
       * factory.
       */
      tasks.put("parserService", new ThreadPoolExecutorBaseStatisticsTask(parserService));

      tasks.put(
          "term2IdWriterService", new ThreadPoolExecutorBaseStatisticsTask(tidsWriterService));

      tasks.put("otherWriterService", new ThreadPoolExecutorBaseStatisticsTask(otherWriterService));

      tasks.put("notifyService", new ThreadPoolExecutorBaseStatisticsTask(notifyService));

      // schedule this task to sample performance counters.
      serviceStatisticsFuture =
          scheduledService.scheduleWithFixedDelay(
              this, 0 /* initialDelay */, 1000 /* delay */, TimeUnit.MILLISECONDS);
    }

    protected void finalize() throws Exception {

      cancel();
    }

    public void cancel() {

      serviceStatisticsFuture.cancel(true /* mayInterruptIfRunning */);
    }

    public void run() {

      for (Runnable r : tasks.values()) {

        try {

          r.run();

        } catch (Throwable t) {

          log.error(r, t);
        }
      }
    }

    public CounterSet getCounters() {

      final CounterSet counterSet = new CounterSet();

      for (Map.Entry<String, ThreadPoolExecutorBaseStatisticsTask> e : tasks.entrySet()) {

        counterSet.makePath(e.getKey()).attach(e.getValue().getCounters());
      }

      return counterSet;
    }
  }

  public boolean isAnyDone() {

    /*
     * Note: lock is required to make this test atomic with respect to
     * re-opening of buffers.
     */
    lock.lock();
    try {

      if (buffer_blobs != null) if (buffer_blobs.getFuture().isDone()) return true;

      if (buffer_t2id != null) if (buffer_t2id.getFuture().isDone()) return true;

      if (buffer_id2t.getFuture().isDone()) return true;

      if (buffer_text != null) if (buffer_text.getFuture().isDone()) return true;

      for (Map.Entry<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> e : buffer_stmts.entrySet()) {

        final IRunnableBuffer<KVO<ISPO>[]> buffer = e.getValue();

        if (buffer != null && buffer.getFuture().isDone()) return true;
      }

      if (parserService.isTerminated()) return true;

      if (tidsWriterService.isTerminated()) return true;

      if (otherWriterService.isTerminated()) return true;

      return notifyService != null && notifyService.isTerminated();

    } finally {

      lock.unlock();
    }
  }

  public void cancelAll(final boolean mayInterruptIfRunning) {

    if (log.isInfoEnabled()) log.info("Cancelling futures.");

    if (buffer_blobs != null) buffer_blobs.getFuture().cancel(mayInterruptIfRunning);

    if (buffer_t2id != null) buffer_t2id.getFuture().cancel(mayInterruptIfRunning);

    buffer_id2t.getFuture().cancel(mayInterruptIfRunning);

    if (buffer_text != null) buffer_text.getFuture().cancel(mayInterruptIfRunning);

    for (Map.Entry<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> e : buffer_stmts.entrySet()) {

      final IRunnableBuffer<KVO<ISPO>[]> buffer = e.getValue();

      if (buffer != null) buffer.getFuture().cancel(mayInterruptIfRunning);
    }

    notifyEnd();
  }

  /*
   * Awaits a signal that all documents which have queued writes are finished and then closes the
   * remaining buffers.
   */
  public void close() {

    log.info("");

    try {
      lock.lockInterruptibly();
      try {

        assertSumOfLatchs();

        // not decremented until doc fails parse or is doing TERM2ID writes.
        workflowLatch_parser.await();

        assertSumOfLatchs();

        /*
         * No more tasks will request TIDs, so close the TERM2ID and
         * BLOBS masters. It will flush its writes.
         */
        guardLatch_term2Id.await();
        {
          if (buffer_t2id != null) {
            if (log.isInfoEnabled()) {
              log.info("Closing TERM2ID buffer.");
            }
            buffer_t2id.close();
          }

          if (buffer_blobs != null) {
            if (log.isInfoEnabled()) {
              log.info("Closing BLOBS buffer.");
            }
            buffer_blobs.close();
          }

          workflowLatch_bufferTids.await();
          tidsWriterService.shutdown();
          new ShutdownHelper(tidsWriterService, 10L, TimeUnit.SECONDS) {
            protected void logTimeout() {
              log.warn("Waiting for term2Id write service shutdown.");
            }
          };

          assertSumOfLatchs();
        }

        /*
         * No new index write tasks may start (and all should have
         * terminated by now).
         */
        guardLatch_other.await();
        {
          if (log.isInfoEnabled()) log.info("Closing remaining buffers.");

          buffer_id2t.close();

          if (buffer_text != null) buffer_text.close();

          for (Map.Entry<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> e : buffer_stmts.entrySet()) {

            final IRunnableBuffer<KVO<ISPO>[]> buffer = e.getValue();

            if (buffer != null) buffer.close();
          }

          workflowLatch_bufferOther.await();
          otherWriterService.shutdown();
          new ShutdownHelper(otherWriterService, 10L, TimeUnit.SECONDS) {
            protected void logTimeout() {
              log.warn("Waiting for other write service shutdown.");
            }
          };

          assertSumOfLatchs();
        }

        // wait for the global latch.
        workflowLatch_document.await();

        assertSumOfLatchs();

        if (notifyService != null) {
          // wait until no notifications are pending.
          guardLatch_notify.await();
          // note: shutdown should be immediate since nothing should
          // be pending.
          notifyService.shutdown();
          new ShutdownHelper(notifyService, 10L, TimeUnit.SECONDS) {
            protected void logTimeout() {
              log.warn("Waiting for delete service shutdown.");
            }
          };
        }

      } finally {
        lock.unlock();
        notifyEnd();
      }

    } catch (InterruptedException ex) {

      // @todo should declare this exception in the API.
      throw new RuntimeException(ex);
    }
  }

  public void awaitAll() throws InterruptedException, ExecutionException {

    if (log.isInfoEnabled()) log.info("Start");

    // Close the asynchronous write buffers.
    close();

    // Await futures for the asynchronous write buffers.
    if (log.isInfoEnabled()) log.info("Awaiting futures.");

    if (buffer_blobs != null) buffer_blobs.getFuture().get();

    if (buffer_t2id != null) buffer_t2id.getFuture().get();

    buffer_id2t.getFuture().get();

    if (buffer_text != null) buffer_text.getFuture().get();

    for (Map.Entry<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> e : buffer_stmts.entrySet()) {

      final IRunnableBuffer<KVO<ISPO>[]> buffer = e.getValue();

      if (buffer != null) {

        buffer.getFuture().get();
      }
    }

    if (log.isInfoEnabled()) log.info("Done.");
  }

  /*
   * Invoked after a document has become restart safe. If {@link #newSuccessTask(Object)} returns a
   * {@link Runnable} then that will be executed on the {@link #notifyService}.
   *
   * @param resource The document identifier.
   */
  protected final void documentDone(final R resource) {

    if (!lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException();

    try {

      final Runnable task = newSuccessTask(resource);

      if (task != null) {

        // increment before we submit the task.
        //                lock.lock();
        //                try {
        guardLatch_notify.inc();
        //                } finally {
        //                    lock.unlock();
        //                }
        try {

          // queue up success notice.
          notifyService.submit(
              new Runnable() {
                public void run() {
                  try {
                    task.run();
                  } finally {
                    lock.lock(); // acquire latch w/in task.
                    try {
                      // decrement after the task is done.
                      guardLatch_notify.dec();
                    } finally {
                      lock.unlock();
                    }
                  }
                }
              });

        } catch (RejectedExecutionException ex) {
          // decrement latch since tasks did not run.
          //                    lock.lock();
          //                    try {
          guardLatch_notify.dec();
          //                    } finally {
          //                        lock.unlock();
          //                    }
          // rethrow exception (will be logged below).
          throw ex;
        }
      }

    } catch (Throwable t) {

      // Log @ ERROR and ignore.
      log.error(t, t);
    }
  }

  /*
   * Invoked after a document has failed. If {@link #newFailureTask(Object, Throwable)} returns a
   * {@link Runnable} then that will be executed on the {@link #notifyService}.
   *
   * @param resource The document identifier.
   * @param t The exception.
   */
  protected final void documentError(final R resource, final Throwable t) {

    if (!lock.isHeldByCurrentThread()) throw new IllegalMonitorStateException();

    documentErrorCount.incrementAndGet();

    /*
     * Note: this is responsible for decrementing the #of documents whose
     * processing is not yet complete. This must be done for each task whose
     * future is not watched. However, we MUST NOT do this twice for any
     * given document since that would mess with the counter. That counter
     * is critical as it forms part of the termination condition for the
     * total data load operation.
     */

    workflowLatch_document.dec();

    try {

      final Runnable task = newFailureTask(resource, t);

      if (task != null) {

        // increment before we submit the task.
        guardLatch_notify.inc();

        try {

          // queue up failure notice.
          notifyService.submit(
              new Runnable() {
                public void run() {
                  try {
                    task.run();
                  } finally {
                    lock.lock(); // acquire latch w/in task.
                    try {
                      // decrement after the task is done.
                      guardLatch_notify.dec();
                    } finally {
                      lock.unlock();
                    }
                  }
                }
              });

        } catch (RejectedExecutionException ex) {
          // decrement latch since tasks did not run.
          //                    lock.lock();
          //                    try {
          guardLatch_notify.dec();
          //                    } finally {
          //                        lock.unlock();
          //                    }
          // rethrow exception (will be logged below).
          throw ex;
        }
      }

    } catch (Throwable ex) {

      log.error(ex, ex);
    }
  }

  /*
   * Return the optional task to be executed for a resource which has been successfully processed
   * and whose assertions are now restart safe on the database. The task, if any, will be run on the
   * {@link #notifyService}.
   *
   * <p>The default implementation runs a {@link DeleteTask} IFF <i>deleteAfter</i> was specified as
   * <code>true</code> to the ctor and otherwise returns <code>null</code>. The event is logged @
   * INFO.
   *
   * @param resource The resource.
   * @return The task to run -or- <code>null</code> if no task should be run.
   */
  protected Runnable newSuccessTask(final R resource) {

    if (log.isInfoEnabled()) log.info("resource=" + resource);

    if (deleteAfter) {

      return new DeleteTask(resource);
    }

    return null;
  }

  /*
   * Return the optional task to be executed for a resource for which processing has failed. The
   * task, if any, will be run on the {@link #notifyService}.
   *
   * <p>The default implementation logs a message @ ERROR.
   *
   * @param resource The resource.
   * @param cause The cause.
   * @return The task to run -or- <code>null</code> if no task should be run.
   */
  protected Runnable newFailureTask(final R resource, final Throwable cause) {

    return new Runnable() {

      public void run() {

        log.error(resource, cause);
      }
    };
  }

  /*
   * Task deletes a resource from the local file system.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  protected class DeleteTask implements Runnable {

    private final R resource;

    public DeleteTask(final R resource) {

      if (resource == null) throw new IllegalArgumentException();

      this.resource = resource;
    }

    public void run() {

      deleteResource(resource);
    }
  }

  /*
   * Delete a file whose data have been made restart safe on the database from the local file system
   * (this must be overridden to handle resources which are not {@link File}s).
   *
   * @param resource The resource.
   */
  protected void deleteResource(final R resource) {

    if (resource instanceof File) {

      if (!((File) resource).delete()) {

        log.warn("Could not delete: " + resource);
      }
    }
  }

  public CounterSet getCounters() {

    final CounterSet counterSet = new CounterSet();

    /** The elapsed milliseconds. */
    counterSet.addCounter(
        "elapsedMillis",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(getElapsedMillis());
          }
        });

    /** The #of documents that have been parsed. */
    counterSet.addCounter(
        "documentsParsedCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(documentsParsedCount.get());
          }
        });

    /*
     * The #of documents whose TERM2ID/BLOBS writes have begun to be buffered but are not yet
     * restart-safe on the database.
     */
    counterSet.addCounter(
        "documentTIDsWaitingCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(documentTIDsWaitingCount.get());
          }
        });

    /** The #of documents whose TERM2ID/BLOBS writes are restart-safe on the database. */
    counterSet.addCounter(
        "documentTIDsReadyCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(documentTIDsReadyCount.get());
          }
        });

    /*
     * The #of tuples written on the full text index (this does not count triples that were already
     * present on the index).
     */
    counterSet.addCounter(
        "fullTextTupleWriteCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(textResultHandler.getResult().longValue());
          }
        });

    /*
     * The #of triples written on the SPO index (this does not count triples that were already
     * present on the index).
     */
    counterSet.addCounter(
        "toldTriplesWriteCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(getStatementCount());
          }
        });

    /*
     * The #of told triples parsed from documents using this factory and made restart safe on the
     * database. This is incremented each time a document has been made restart safe by the #of
     * distinct told triples parsed from that document.
     *
     * <p>Note: The same triple can occur in more than one document, and documents having duplicate
     * triples may be loaded by distributed clients. The actual #of triples on the database is only
     * available by querying the database.
     */
    counterSet.addCounter(
        "toldTriplesRestartSafeCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(toldTriplesRestartSafeCount.get());
          }
        });

    /*
     * The told triples per second rate which have been made restart safe by this factory object.
     * When you are loading using multiple clients, then the total told triples per second rate is
     * the aggregation across all of those instances.
     */
    counterSet.addCounter(
        "toldTriplesRestartSafePerSec",
        new Instrument<Long>() {

          @Override
          protected void sample() {

            final long elapsed = getElapsedMillis();

            final double tps =
                (long) (((double) toldTriplesRestartSafeCount.get()) / ((double) elapsed) * 1000d);

            setValue((long) tps);
          }
        });

    /*
     * The #of documents which have been processed by this client and are restart safe on the
     * database by this client.
     */
    counterSet.addCounter(
        "documentRestartSafeCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(documentRestartSafeCount.get());
          }
        });

    /** The #of documents for which the buffer index writes task failed. */
    counterSet.addCounter(
        "documentErrorCount",
        new Instrument<Long>() {
          @Override
          protected void sample() {
            setValue(documentErrorCount.get());
          }
        });

    /*
     * The latches are used to guard the termination conditions for the
     * factory. If they are non-zero the factory can not terminate normally.
     */
    {
      final CounterSet workflowLatchSet = counterSet.makePath("workflowLatch");

      workflowLatchSet.addCounter(
          "parser",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(workflowLatch_parser.get());
            }
          });

      workflowLatchSet.addCounter(
          "bufferTids",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(workflowLatch_bufferTids.get());
            }
          });

      workflowLatchSet.addCounter(
          "bufferOther",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(workflowLatch_bufferOther.get());
            }
          });

      // latch over the total life cycle for a document.
      workflowLatchSet.addCounter(
          "document",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(workflowLatch_document.get());
            }
          });
    } // latches

    /** Latches used to guard the buffers and close them in a timely manner. */
    {
      final CounterSet bufferGuardSet = counterSet.makePath("bufferGuard");

      bufferGuardSet.addCounter(
          "guardTerm2Id",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(guardLatch_term2Id.get());
            }
          });

      bufferGuardSet.addCounter(
          "guardOther",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(guardLatch_other.get());
            }
          });

      bufferGuardSet.addCounter(
          "guardNotify",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(guardLatch_notify.get());
            }
          });
    }

    /*
     * Counters pertaining to the logic which suspects new parser task
     * requests if too many statements are currently buffered.
     */
    {
      final CounterSet pauseSet = counterSet.makePath("pause");

      /*
       * The #of parsed or buffered RDF Statements not yet restart safe
       * (current value).
       */
      pauseSet.addCounter(
          "outstandingStatementCount",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(outstandingStatementCount.get());
            }
          });

      /*
       * The #of parsed but not yet buffered RDF Statements (current
       * value).
       */
      pauseSet.addCounter(
          "unbufferedStatementCount",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(unbufferedStatementCount.get());
            }
          });

      /*
       * The maximum #of statements parsed but not yet buffered before we
       * suspend new parse requests.
       */
      pauseSet.addCounter(
          "pauseParserPoolStatementThreshold",
          new OneShotInstrument<Long>(pauseParserPoolStatementThreshold));

      // The #of suspended parse request threads (current value).
      pauseSet.addCounter(
          "pausedThreadCount",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(pausedThreadCount.get());
            }
          });

      // The #of suspended parse request threads (cumulative).
      pauseSet.addCounter(
          "poolPausedCount",
          new Instrument<Long>() {
            @Override
            protected void sample() {
              setValue(poolPausedCount.get());
            }
          });
    }

    // services
    {
      counterSet.makePath("services").attach(serviceStatisticsTask.getCounters());
    }

    //        if(log.isInfoEnabled())
    //        { // @todo this is just for debugging problems with parser blocking.
    //
    //            final String fqn = tripleStore.getLexiconRelation().getFQN(
    //                    LexiconKeyOrder.TERM2ID);
    //
    //            counterSet.makePath("TERM2ID").attach(
    //                    ((AbstractFederation) tripleStore.getIndexManager())
    //                            .getIndexCounters(fqn).getCounters());
    //
    //        }

    return counterSet;
  }

  /*
   * {@link Runnable} class applies the factory to either a single file or to all files within a
   * directory.
   */
  private class RunnableFileSystemLoader implements Callable<Integer> {

    //        volatile boolean done = false;

    private int count = 0;

    //        private long retryCount = 0L;

    final File fileOrDir;

    final FilenameFilter filter;

    final long retryMillis;

    /*
     * @param fileOrDir The file or directory to be loaded.
     * @param filter An optional filter on files that will be accepted when processing a directory.
     * @param retryMillis The number of milliseconds to wait between retrys when the parser service
     *     work queue is full. When ZERO (0L), a {@link RejectedExecutionException} will be thrown
     *     out instead.
     */
    public RunnableFileSystemLoader(
        final File fileOrDir, final FilenameFilter filter, final long retryMillis) {

      if (fileOrDir == null) throw new IllegalArgumentException();

      if (retryMillis < 0) throw new IllegalArgumentException();

      this.fileOrDir = fileOrDir;

      this.filter = filter; // MAY be null.

      this.retryMillis = retryMillis;
    }

    /*
     * Creates a task using the {@link #taskFactory}, submits it to the {@link #loader} and and
     * waits for the task to complete. Errors are logged, but not thrown.
     *
     * @throws RuntimeException if interrupted.
     */
    public Integer call() throws Exception {

      process2(fileOrDir);

      return count;
    }

    /*
     * Scans file(s) recursively starting with the named file, and, for each file that passes the
     * filter, submits the task.
     *
     * @param file Either a URL, a plain file or directory containing files to be processed.
     * @throws InterruptedException if the thread is interrupted while queuing tasks.
     */
    private void process2(final File file) throws InterruptedException {

      if (file.isHidden()) {

        // ignore hidden files.
        return;
      }

      if (file.isDirectory()) {

        if (log.isInfoEnabled()) log.info("Scanning directory: " + file);

        // filter is optional.
        final File[] files = filter == null ? file.listFiles() : file.listFiles(filter);

        for (final File f : files) {

          process2(f);
        }

      } else {

        /*
         * Processing a standard file.
         */

        if (log.isInfoEnabled()) log.info("Will load: " + file);

        try {

          submitOne((R) file, retryMillis);

          count++;

          return;

        } catch (InterruptedException ex) {

          throw ex;

        } catch (Exception ex) {

          log.error(file, ex);
        }
      }
    }
  }

  /*
   * Class applies the term identifiers assigned by the {@link Term2IdWriteProc} to the {@link
   * EmbergraphValue} references in the {@link KVO} correlated with each {@link Split} of data
   * processed by that procedure.
   *
   * <p>Note: Of necessity, this requires access to the {@link EmbergraphValue}s whose term
   * identifiers are being resolved. This implementation presumes that the array specified to the
   * ctor and the array returned for each chunk that is processed have correlated indices and that
   * the offset into the array is given by {@link Split#fromIndex}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class Term2IdWriteProcAsyncResultHandler
      implements IAsyncResultHandler<
          Term2IdWriteProc.Result, Void, EmbergraphValue, KVO<EmbergraphValue>> {

    private final boolean readOnly;

    /** @param readOnly if readOnly was specified for the {@link Term2IdWriteProc} . */
    public Term2IdWriteProcAsyncResultHandler(final boolean readOnly) {

      this.readOnly = readOnly;
    }

    /*
     * NOP
     *
     * @see #aggregateAsync(KVO[], org.embergraph.rdf.lexicon.Term2IdWriteProc.Result, Split)
     */
    public void aggregate(final Term2IdWriteProc.Result result, final Split split) {}

    /*
     * Copy the assigned / discovered term identifiers onto the corresponding elements of the
     * terms[].
     */
    public void aggregateAsync(
        final KVO<EmbergraphValue>[] chunk,
        final Term2IdWriteProc.Result result,
        final Split split) {

      for (int i = 0; i < chunk.length; i++) {

        @SuppressWarnings("rawtypes")
        final IV iv = result.ivs[i];

        if (iv == null) {

          if (!readOnly) throw new AssertionError();

        } else {

          // assign the term identifier.
          chunk[i].obj.setIV(iv);

          if (chunk[i] instanceof KVOList) {

            final KVOList<EmbergraphValue> tmp = (KVOList<EmbergraphValue>) chunk[i];

            if (!tmp.isDuplicateListEmpty()) {

              // assign the term identifier to the duplicates.
              tmp.map(new AssignTermId(iv));
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("termId=" + iv + ", term=" + chunk[i].obj);
          }
        }
      }
    }

    public Void getResult() {

      return null;
    }
  }

  /*
   * Class applies the term identifiers assigned by the {@link BlobsWriteProc} to the {@link
   * EmbergraphValue} references in the {@link KVO} correlated with each {@link Split} of data
   * processed by that procedure.
   *
   * <p>Note: Of necessity, this requires access to the {@link EmbergraphValue}s whose term
   * identifiers are being resolved. This implementation presumes that the array specified to the
   * ctor and the array returned for each chunk that is processed have correlated indices and that
   * the offset into the array is given by {@link Split#fromIndex}.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class BlobsWriteProcAsyncResultHandler
      implements IAsyncResultHandler<
          BlobsWriteProc.Result, Void, EmbergraphValue, KVO<EmbergraphValue>> {

    private final boolean readOnly;

    /** @param readOnly if readOnly was specified for the {@link BlobsWriteProc} . */
    public BlobsWriteProcAsyncResultHandler(final boolean readOnly) {

      this.readOnly = readOnly;
    }

    /*
     * NOP
     *
     * @see #aggregateAsync(KVO[], org.embergraph.rdf.lexicon.BlobsWriteProc.Result, Split)
     */
    public void aggregate(final BlobsWriteProc.Result result, final Split split) {}

    /*
     * Copy the assigned / discovered term identifiers onto the corresponding elements of the
     * terms[].
     */
    public void aggregateAsync(
        final KVO<EmbergraphValue>[] chunk, final BlobsWriteProc.Result result, final Split split) {

      for (int i = 0; i < chunk.length; i++) {

        final int counter = result.counters[i];

        if (counter == BlobsIndexHelper.NOT_FOUND) {

          if (!readOnly) throw new AssertionError();

        } else {

          // The value whose IV we have discovered/asserted.
          final EmbergraphValue value = chunk[i].obj;

          // Rebuild the IV.
          @SuppressWarnings("rawtypes")
          final BlobIV<?> iv = new BlobIV(VTE.valueOf(value), value.hashCode(), (short) counter);

          // assign the term identifier.
          value.setIV(iv);

          if (chunk[i] instanceof KVOList) {

            final KVOList<EmbergraphValue> tmp = (KVOList<EmbergraphValue>) chunk[i];

            if (!tmp.isDuplicateListEmpty()) {

              // assign the term identifier to the duplicates.
              tmp.map(new AssignTermId(iv));
            }
          }

          if (log.isDebugEnabled()) {
            log.debug("termId=" + iv + ", term=" + chunk[i].obj);
          }
        }
      }
    }

    public Void getResult() {

      return null;
    }
  }

  /*
   * Wrap a {@link EmbergraphValue}[] with a chunked iterator.
   *
   * <p>Note: This resolves inline {@link IV}s and filters them out of the visited {@link
   * EmbergraphValue}s as a side-effect.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  static <V extends EmbergraphValue> IChunkedIterator<V> newValuesIterator(
      final LexiconRelation r, final Iterator<V> itr, final int chunkSize) {

    return new ChunkedWrappedIterator(
        new Striterator(itr)
            .addFilter(
                new Filter() {

                  private static final long serialVersionUID = 1L;

                  @Override
                  public boolean isValid(final Object obj) {
                    /*
                     * Assigns the IV as a side effect iff the RDF Value can
                     * be inlined according to the governing lexicon
                     * configuration and returns true iff the value CAN NOT
                     * be inlined. Thus, inlining is done as a side effect
                     * while the caller sees only those Values which need to
                     * be written onto the TERM2ID/BLOBS index.
                     */
                    return r.getInlineIV((Value) obj) == null;
                  }
                }),
        chunkSize,
        EmbergraphValue.class);
  }

  /*
   * Wrap a {@link EmbergraphValue}[] with a chunked iterator which filters out blank nodes and
   * blobs (neither of which is written onto the reverse index).
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <V extends EmbergraphValue> IChunkedIterator<V> newId2TIterator(
      final LexiconRelation r, final Iterator<V> itr, final int chunkSize) {

    return new ChunkedWrappedIterator(
        new Striterator(itr)
            .addFilter(
                new Filter() {

                  private static final long serialVersionUID = 1L;

                  /*
                   * Filter hides blank nodes since we do not write them onto
                   * the reverse index.
                   *
                   * Filter does not visit blobs since we do not want to write
                   * those onto the reverse index either.
                   */
                  @Override
                  public boolean isValid(final Object obj) {

                    final EmbergraphValue v = (EmbergraphValue) obj;

                    if (v instanceof BNode) return false;

                    return !r.isBlob(v);
                  }
                }),
        chunkSize,
        EmbergraphValue.class);
  }

  /*
   * Return iterator visiting only the {@link EmbergraphLiteral}s that we want to write on the full
   * text index.
   *
   * @param r
   * @param itr
   * @param chunkSize
   * @return
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static <V extends EmbergraphValue> IChunkedIterator<V> newTextIterator(
      final LexiconRelation r,
      final Iterator<V> itr,
      final int chunkSize,
      final boolean indexDatatypeLiterals) {

    return new ChunkedWrappedIterator(
        new Striterator(itr)
            .addFilter(
                new Filter() {

                  private static final long serialVersionUID = 1L;

                  /*
                   * Filter hides blank nodes since we do not write them onto
                   * the TEXT index.
                   */
                  @Override
                  public boolean isValid(final Object obj) {

                    if (!(obj instanceof EmbergraphLiteral)) {
                      // Only index Literals.
                      return false;
                    }

                    final EmbergraphLiteral lit = (EmbergraphLiteral) obj;

                    // Ignore datatype literals.
                    return indexDatatypeLiterals || lit.getDatatype() == null;
                  }
                }),
        chunkSize,
        EmbergraphValue.class);
  }

  /*
   * Asynchronous writes on the TERM2ID index.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @todo something similar for the SIDs
   */
  static class AsyncTerm2IdIndexWriteTask implements Callable<Void> {

    protected static final transient Logger log =
        Logger.getLogger(AsyncTerm2IdIndexWriteTask.class);

    private final KVOLatch latch;

    private final IChunkedIterator<EmbergraphValue> src;

    private final LexiconRelation lexiconRelation;

    private final Term2IdTupleSerializer tupleSerTerm2Id;
    //        private final BlobsTupleSerializer tupleSerBlobs;

    private final IRunnableBuffer<KVO<EmbergraphValue>[]> bufferTerm2Id;
    private final IRunnableBuffer<KVO<EmbergraphValue>[]> bufferBlobs;

    /*
     * @param latch
     * @param r
     * @param src The visits chunks of distinct {@link Value}s.
     * @param bufferTerm2Id
     * @param bufferBlobs
     */
    public AsyncTerm2IdIndexWriteTask(
        final KVOLatch latch,
        final LexiconRelation r,
        final IChunkedIterator<EmbergraphValue> src,
        final IRunnableBuffer<KVO<EmbergraphValue>[]> bufferTerm2Id,
        final IRunnableBuffer<KVO<EmbergraphValue>[]> bufferBlobs) {

      if (latch == null) throw new IllegalArgumentException();

      if (r == null) throw new IllegalArgumentException();

      if (src == null) throw new IllegalArgumentException();

      if (bufferTerm2Id == null && bufferBlobs == null) throw new IllegalArgumentException();

      this.latch = latch;

      this.lexiconRelation = r;

      this.tupleSerTerm2Id =
          bufferTerm2Id == null
              ? null
              : (Term2IdTupleSerializer)
                  r.getIndex(LexiconKeyOrder.TERM2ID).getIndexMetadata().getTupleSerializer();

      this.src = src;

      this.bufferTerm2Id = bufferTerm2Id;

      this.bufferBlobs = bufferBlobs;
    }

    /*
     * Return <code>true</code> if the {@link EmbergraphValue} will be stored against the BLOBS
     * index.
     */
    private boolean isBlob(final EmbergraphValue v) {

      return lexiconRelation.isBlob(v);
    }

    //        /*
    //         * Return <code>true</code> iff the {@link EmbergraphValue} is fully inline
    //         * (in which case the {@link IV} is set as a side-effect on the
    //         * {@link EmbergraphValue}).
    //         */
    //        private boolean isInline(final EmbergraphValue v) {
    //
    //            return lexiconRelation.getInlineIV(v) != null;
    //
    //        }

    /*
     * Reshapes the {@link #src} into {@link KVOC}[]s a chunk at a time and submits each chunk to
     * the write buffer for the TERM2ID index.
     */
    public Void call() throws Exception {

      /*
       * This is a thread-local instance, which is why we defer obtaining
       * this object until call() is executing.
       */
      final LexiconKeyBuilder keyBuilderTerm2Id =
          bufferTerm2Id == null ? null : tupleSerTerm2Id.getLexiconKeyBuilder();

      // BLOBS stuff.
      final EmbergraphValueSerializer<EmbergraphValue> valSer =
          lexiconRelation.getValueFactory().getValueSerializer();
      final BlobsIndexHelper h = new BlobsIndexHelper();
      final IKeyBuilder keyBuilder = h.newKeyBuilder();
      final DataOutputBuffer out = new DataOutputBuffer(512);
      final ByteArrayBuffer tmp = new ByteArrayBuffer(512);

      latch.inc();

      try {

        List<KVOC<EmbergraphValue>> terms = null;
        List<KVOC<EmbergraphValue>> blobs = null;

        while (src.hasNext()) {

          final EmbergraphValue[] chunkIn = src.nextChunk();

          for (EmbergraphValue v : chunkIn) {

            /*
             * Note: The iterator we are visiting has already had
             * the IVs for fully inline Values resolved and set as a
             * side-effect and the inline Values have been filtered
             * out. We will only see non-inline values here, but
             * they may wind up as TermIds or BlobIVs.
             */
            //                        if (isInline(v)) {
            //                            // Immediately resolve the IV via a side-effect.
            //                            System.err.println("inline: "+v);
            //                            continue;
            //                        }

            if (bufferBlobs != null && isBlob(v)) {

              final byte[] key = h.makePrefixKey(keyBuilder.reset(), v);

              final byte[] val = valSer.serialize(v, out.reset(), tmp);

              if (blobs == null) {
                // Lazily allocate.
                blobs = new ArrayList<KVOC<EmbergraphValue>>();
              }
              // Assign a sort key to each Value.
              blobs.add(new KVOC<EmbergraphValue>(key, val, v, latch));
              //                            System.err.println("blob  : "+v);

            } else {

              if (terms == null) {
                // Lazily allocate to chunkSize.
                terms = new ArrayList<KVOC<EmbergraphValue>>(chunkIn.length);
              }
              // Assign a sort key to each Value.
              terms.add(
                  new KVOC<EmbergraphValue>(
                      keyBuilderTerm2Id.value2Key(v), null /* val */, v, latch));
              //                            System.err.println("term  : "+v);

            }
          }

          if (terms != null && !terms.isEmpty()) {

            @SuppressWarnings("unchecked")
            final KVOC<EmbergraphValue>[] a = terms.toArray(new KVOC[terms.size()]);

            // Place in KVO sorted order (by the byte[] keys).
            Arrays.sort(a);

            if (log.isInfoEnabled())
              log.info("Adding chunk to TERM2ID master: chunkSize=" + a.length);

            // add chunk to async write buffer
            bufferTerm2Id.add(a);

            // Clear list.
            terms.clear();
          }

          if (blobs != null && !blobs.isEmpty()) {

            @SuppressWarnings("unchecked")
            final KVOC<EmbergraphValue>[] a = blobs.toArray(new KVOC[blobs.size()]);

            // Place in KVO sorted order (by the byte[] keys).
            Arrays.sort(a);

            if (log.isInfoEnabled())
              log.info("Adding chunk to BLOBS master: chunkSize=" + a.length);

            // add chunk to async write buffer
            bufferBlobs.add(a);

            // Clear list.
            blobs.clear();
          }
        }

      } finally {

        latch.dec();
      }

      // Done.
      return null;
    }
  }

  /*
   * Asynchronous writes on the ID2TERM index.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  static class AsyncId2TermIndexWriteTask implements Callable<Void> {

    protected static final transient Logger log =
        Logger.getLogger(AsyncId2TermIndexWriteTask.class);

    private final KVOLatch latch;

    private final EmbergraphValueFactory valueFactory;

    private final IChunkedIterator<EmbergraphValue> src;

    private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer;

    /*
     * @param src The visits chunks of distinct {@link Value}s with their TIDs assigned. Blank nodes
     *     will automatically be filtered out.
     */
    public AsyncId2TermIndexWriteTask(
        final KVOLatch latch,
        final EmbergraphValueFactory valueFactory,
        final IChunkedIterator<EmbergraphValue> src,
        final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer) {

      if (latch == null) throw new IllegalArgumentException();

      if (valueFactory == null) throw new IllegalArgumentException();

      if (src == null) throw new IllegalArgumentException();

      if (buffer == null) throw new IllegalArgumentException();

      this.latch = latch;

      this.valueFactory = valueFactory;

      this.src = src;

      this.buffer = buffer;
    }

    public Void call() throws Exception {

      // used to serialize the Values for the BTree.
      final EmbergraphValueSerializer<EmbergraphValue> ser = valueFactory.getValueSerializer();

      // thread-local key builder removes single-threaded constraint.
      final IKeyBuilder tmp = KeyBuilder.newInstance(Bytes.SIZEOF_LONG);

      // buffer is reused for each serialized term.
      final DataOutputBuffer out = new DataOutputBuffer();
      final ByteArrayBuffer tbuf = new ByteArrayBuffer();

      latch.inc();

      try {

        while (src.hasNext()) {

          final EmbergraphValue[] chunkIn = src.nextChunk();

          @SuppressWarnings("unchecked")
          final KVOC<EmbergraphValue>[] chunkOut = new KVOC[chunkIn.length];

          int i = 0;

          for (EmbergraphValue v : chunkIn) {

            assert v != null;

            if (v instanceof BNode) {

              // Do not write blank nodes on the reverse index.
              continue;
            }

            if (v.getIV() == null) {

              throw new RuntimeException("No TID: " + v);
            }

            if (v.getIV().isInline()) {

              // Do not write inline values on the reverse index.
              continue;
            }

            final byte[] key = v.getIV().encode(tmp.reset()).getKey();

            // Serialize the term.
            final byte[] val = ser.serialize(v, out.reset(), tbuf);

            /*
             * Note: The EmbergraphValue instance is NOT supplied to
             * the KVO since we do not want it to be retained and
             * since there is no side-effect on the EmbergraphValue for
             * writes on ID2TERM (unlike the writes on TERM2ID).
             */
            chunkOut[i++] = new KVOC<EmbergraphValue>(key, val, null /* v */, latch);
          }

          // make dense.
          final KVO<EmbergraphValue>[] dense = KVO.dense(chunkOut, i);

          /*
           * Put into key order in preparation for writing on the
           * reverse index.
           */
          Arrays.sort(dense);

          // add chunk to asynchronous write buffer
          buffer.add(dense);
        }

      } finally {

        latch.dec();
      }

      // Done.
      return null;
    }
  }

  /*
   * Asynchronous writes on the TEXT index.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  static class AsyncTextIndexWriteTask implements Callable<Void> {

    protected static final transient Logger log = Logger.getLogger(AsyncTextIndexWriteTask.class);

    private final KVOLatch latch;

    private final EmbergraphValueCentricFullTextIndex textIndex;

    private final IChunkedIterator<EmbergraphValue> src;

    private final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer;

    /*
     * @param src The visits chunks of distinct {@link EmbergraphLiteral}s with their TIDs assigned.
     *     Anything which should not be indexed has already been filtered out.
     */
    public AsyncTextIndexWriteTask(
        final KVOLatch latch,
        final EmbergraphValueCentricFullTextIndex textIndex,
        final IChunkedIterator<EmbergraphValue> src,
        final IRunnableBuffer<KVO<EmbergraphValue>[]> buffer) {

      if (latch == null) throw new IllegalArgumentException();

      if (textIndex == null) throw new IllegalArgumentException();

      if (src == null) throw new IllegalArgumentException();

      if (buffer == null) throw new IllegalArgumentException();

      this.latch = latch;

      this.textIndex = textIndex;

      this.src = src;

      this.buffer = buffer;
    }

    /*
     * FIXME This will on the full text index using the {@link EmbergraphValueCentricFullTextIndex}
     * class. That class will wind up doing gathered batch inserts in chunks of up to the capacity
     * set inline in the method below. However, it will use Sync RPC rather than the ASYNC
     * [buffer_text] index write pipeline. While this should be enough to write unit tests for the
     * full text indexing feature, it is not going to scale well.
     *
     * @see EmbergraphValueCentricFullTextIndex
     */
    public Void call() throws Exception {

      latch.inc();

      try {

        /*
         * TODO capacity for the full text index writes.
         */
        final int capacity = 100000;

        textIndex.index(capacity, src);

      } finally {

        latch.dec();
      }

      // Done.
      return null;
    }
  }

  /*
   * Writes the statement chunks onto the specified statement index using the asynchronous write
   * API.
   *
   * <p>Note: This is similar to the {@link SPOIndexWriter}, but the latter uses synchronous RPC.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  static class AsyncSPOIndexWriteTask implements Callable<Void> {

    protected static final transient Logger log = Logger.getLogger(AsyncSPOIndexWriteTask.class);

    private final KVOLatch latch;

    private final IKeyOrder<ISPO> keyOrder;

    /* Note: problem with java 1.6.0_07 and _12 on linux when typed. */
    @SuppressWarnings("rawtypes")
    private final IChunkedOrderedIterator /* <ISPO> */ src;

    private final IRunnableBuffer<KVO<ISPO>[]> writeBuffer;

    private final SPOTupleSerializer tupleSer;

    @SuppressWarnings("rawtypes")
    public AsyncSPOIndexWriteTask(
        final KVOLatch latch,
        final IKeyOrder<ISPO> keyOrder,
        final SPORelation spoRelation,
        /* Note: problem with java 1.6.0_07 and _12 on linux when typed. */
        final IChunkedOrderedIterator /* <ISPO> */ src,
        final IRunnableBuffer<KVO<ISPO>[]> writeBuffer) {

      if (latch == null) throw new IllegalArgumentException();

      if (keyOrder == null) throw new IllegalArgumentException();

      if (writeBuffer == null) throw new IllegalArgumentException();

      this.latch = latch;

      this.keyOrder = keyOrder;

      this.src = src;

      this.writeBuffer = writeBuffer;

      // the tuple serializer for this access path.
      this.tupleSer =
          (SPOTupleSerializer)
              spoRelation.getIndex(keyOrder).getIndexMetadata().getTupleSerializer();
    }

    public Void call() throws Exception {

      long chunksOut = 0;
      long elementsOut = 0;

      latch.inc();

      try {

        while (src.hasNext()) {

          // next chunk, in the specified order.
          @SuppressWarnings("unchecked")
          final ISPO[] chunk = (ISPO[]) src.nextChunk(keyOrder);

          // note: a[] will be dense since nothing is filtered.
          @SuppressWarnings("unchecked")
          final KVOC<ISPO>[] a = new KVOC[chunk.length];

          for (int i = 0; i < chunk.length; i++) {

            final ISPO spo = chunk[i];

            if (spo == null) throw new IllegalArgumentException();

            if (!spo.isFullyBound())
              throw new IllegalArgumentException("Not fully bound: " + spo.toString());

            // generate key for the index.
            final byte[] key = tupleSer.serializeKey(spo);

            // generate value for the index.
            final byte[] val = tupleSer.serializeVal(spo);

            /*
             * Note: The SPO is deliberately not provided to the KVO
             * instance since it is not required (there is nothing
             * being passed back from the write via a side-effect on
             * the EmbergraphStatementImpl) and since it otherwise will
             * force the retention of the RDF Value objects in its
             * s/p/o/c positions.
             */
            a[i] = new KVOC<ISPO>(key, val, null /* spo */, latch);
          }

          // put chunk into sorted order based on assigned keys.
          Arrays.sort(a);

          // write chunk on the buffer.
          writeBuffer.add(a);

          chunksOut++;
          elementsOut += a.length;

          if (log.isDebugEnabled())
            log.debug(
                "Wrote chunk: index="
                    + keyOrder
                    + ", chunksOut="
                    + chunksOut
                    + ", elementsOut="
                    + elementsOut
                    + ", chunkSize="
                    + a.length);

          if (log.isTraceEnabled())
            log.trace("Wrote: index=" + keyOrder + ", chunk=" + Arrays.toString(a));
        }

      } finally {

        latch.dec();
      }

      if (log.isDebugEnabled())
        log.debug(
            "Done: index="
                + keyOrder
                + ", chunksOut="
                + chunksOut
                + ", elementsOut="
                + elementsOut);

      // done.
      return null;
    }
  }

  /*
   * Inner class provides the statement buffer.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   * @param <S>
   * @param <F>
   */
  protected class AsynchronousStatementBufferImpl implements IStatementBuffer<S> {

    /** The document identifier. */
    private final R resource;

    private final AbstractTripleStore database;

    private final EmbergraphValueFactory valueFactory;

    /*
     * A canonicalizing map for RDF {@link Value}s. The use of this map provides a ~40% performance
     * gain.
     */
    private LinkedHashMap<Value, EmbergraphValue> values;

    /*
     * A canonicalizing map for blank nodes. This map MUST be cleared before you begin to add
     * statements to the buffer from a new "source" otherwise it will co-reference blank nodes from
     * distinct sources. The life cycle of the map is the life cycle of the document being loaded,
     * so if you are loading a large document with a lot of blank nodes the map will also become
     * large.
     */
    private final AtomicReference<Map<String, EmbergraphBNode>> bnodes =
        new AtomicReference<Map<String, EmbergraphBNode>>();

    /*
     * The total #of parsed statements so far.
     *
     * <p>{@link IBuffer}
     */
    private int statementCount;

    /** Buffer used to accumulate chunks of statements. */
    private UnsynchronizedUnboundedChunkBuffer<S> statements;

    public final AbstractTripleStore getDatabase() {

      return database;
    }

    /*
     * Returns <code>null</code>.
     *
     * <p>Note: This implementation does not support the concept of a focusStore so it can not be
     * used for truth maintenance.
     */
    public AbstractTripleStore getStatementStore() {

      return null;
    }

    public boolean isEmpty() {

      return statementCount == 0;
    }

    public int size() {

      return statementCount;
    }

    /** Return the identifier for the document. */
    public R getDocumentIdentifier() {

      return resource;
    }

    /** @param resource The document identifier. */
    protected AsynchronousStatementBufferImpl(final R resource) {

      this.resource = resource;

      this.database = AsynchronousStatementBufferFactory.this.tripleStore;

      this.valueFactory = database.getValueFactory();
    }

    /*
     * Note: this implementation always returns ZERO (0).
     *
     * @see ParserTask
     */
    public long flush() {

      return 0L;
    }

    /*
     * Clears all buffered data, including the canonicalizing mapping for blank nodes and deferred
     * provenance statements.
     */
    public void reset() {

      if (log.isInfoEnabled()) log.info("resource=" + getDocumentIdentifier());

      /*
       * Note: clear the reference NOT the contents of the map! This makes
       * it possible for the caller to reuse the same map across multiple
       * StatementBuffer instances.
       */
      bnodes.set(null);

      values = null;

      statements = null;

      statementCount = 0;
    }

    public void setBNodeMap(final Map<String, EmbergraphBNode> bnodes) {

      if (bnodes == null) throw new IllegalArgumentException();

      if (!this.bnodes.compareAndSet(null /* expect */, bnodes /* update */)) {

        throw new IllegalStateException();
      }
    }

    /*
     * Add an "explicit" statement to the buffer (flushes on overflow, no context).
     *
     * @param s
     * @param p
     * @param o
     */
    public void add(Resource s, URI p, Value o) {

      add(s, p, o, null, StatementEnum.Explicit);
    }

    /*
     * Add an "explicit" statement to the buffer (flushes on overflow).
     *
     * @param s
     * @param p
     * @param o
     * @param c
     */
    public void add(Resource s, URI p, Value o, Resource c) {

      add(s, p, o, c, StatementEnum.Explicit);
    }

    /** Add a statement to the buffer (core impl). */
    public void add(
        final Resource s, final URI p, final Value o, final Resource c, final StatementEnum type) {

      // add to the buffer.
      handleStatement(s, p, o, c, type);
    }

    public void add(final S e) {

      add(
          e.getSubject(),
          e.getPredicate(),
          e.getObject(),
          e.getContext(),
          (e instanceof EmbergraphStatement ? e.getStatementType() : null));
    }

    /*
     * Canonicalizing mapping for blank nodes.
     *
     * <p>Note: This map MUST stay in effect while reading from a given source and MUST be cleared
     * (or set to null) before reading from another source.
     */
    private EmbergraphBNode getCanonicalBNode(final EmbergraphBNodeImpl bnode) {

      // the BNode's ID.
      final String id = bnode.getID();

      Map<String, EmbergraphBNode> bnodes = this.bnodes.get();
      if (bnodes == null) {

        /*
         * Allocate a canonicalizing map for blank nodes. Since this
         * will be a private map it does not need to be thread-safe.
         */
        setBNodeMap(new HashMap<String, EmbergraphBNode>(bnodesInitialCapacity));

        // fall through.
        bnodes = this.bnodes.get();

        if (bnodes == null) throw new AssertionError();
      }

      /*
       * Specialized for a concurrent hash map.
       */
      if (bnodes instanceof ConcurrentHashMap) {

        final EmbergraphBNode tmp = bnodes.putIfAbsent(id, bnode);

        if (tmp != null) {

          // already exists in the map.
          return tmp;
        }

        if (log.isTraceEnabled()) log.trace("added: " + bnode);

        // was inserted into the map.
        return bnode;
      }

      /*
       * Synchronized on the map to make the conditional insert atomic.
       */
      synchronized (bnodes) {
        final EmbergraphBNode tmp = bnodes.get(id);

        if (tmp != null) {

          // already exists in the map.
          return tmp;
        }

        // insert this blank node into the map.
        bnodes.put(id, bnode);

        if (log.isTraceEnabled()) log.trace("added: " + bnode);

        // was inserted into the map.
        return bnode;
      } // synchronized
    }

    /*
     * Canonicalizing mapping for a term.
     *
     * <p>Note: Blank nodes are made canonical with the scope of the source from which the data are
     * being read. See {@link #bnodes}. All other kinds of terms are made canonical within the scope
     * of the buffer's current contents in order to keep down the demand on the heap with reading
     * either very large documents or a series of small documents.
     *
     * @param term A term.
     * @return Either the term or the pre-existing term in the buffer with the same data.
     */
    private EmbergraphValue getCanonicalValue(final EmbergraphValue term0) {

      if (term0 == null) {

        // Note: This handles an empty context position.
        return term0;
      }

      final EmbergraphValue term;

      if (term0 instanceof BNode) {

        // impose canonicalizing mapping for blank nodes.
        term = getCanonicalBNode((EmbergraphBNodeImpl) term0);

        /*
         * Fall through.
         *
         * Note: This also records the blank node in the values map so
         * that we can process the values map without having to consider
         * the blank nodes as well.
         */

      } else {

        // not a blank node.
        term = term0;
      }

      if (values == null) {

        /*
         * Create a private (non-thread safe) canonicalizing mapping for
         * RDF Values.
         *
         * Note: A linked hash map is used to make the iterator faster.
         */

        values = new LinkedHashMap<Value, EmbergraphValue>(valuesInitialCapacity);
      }

      /*
       * Impose a canonicalizing mapping on the term.
       */

      final EmbergraphValue tmp = values.get(term);

      if (tmp != null) {

        // already exists.
        return tmp;
      }

      // add to the map.
      if (values.put(term, term) != null) {

        throw new AssertionError();
      }

      if (log.isTraceEnabled()) log.trace("n=" + values.size() + ", added: " + term);

      // return the new term.
      return term;
    }

    /*
     * Adds the values and the statement into the buffer.
     *
     * @param s The subject.
     * @param p The predicate.
     * @param o The object.
     * @param c The context (may be null).
     * @param type The statement type.
     * @throws IndexOutOfBoundsException if the buffer capacity is exceeded.
     * @see #nearCapacity()
     */
    private void handleStatement(
        final Resource s, final URI p, final Value o, final Resource c, final StatementEnum type) {

      _handleStatement(
          (Resource) getCanonicalValue(valueFactory.asValue(s)),
          (URI) getCanonicalValue(valueFactory.asValue(p)),
          getCanonicalValue(valueFactory.asValue(o)),
          (Resource) getCanonicalValue(valueFactory.asValue(c)),
          type);
    }

    /*
     * Form the EmbergraphStatement object using the valueFactory now that we bindings which were
     * (a) allocated by the valueFactory and (b) are canonical for the scope of this document.
     */
    @SuppressWarnings("unchecked")
    private void _handleStatement(
        final Resource s, final URI p, final Value o, final Resource c, final StatementEnum type) {

      final EmbergraphStatement stmt = valueFactory.createStatement(s, p, o, c, type);

      if (statements == null) {

        statements =
            new UnsynchronizedUnboundedChunkBuffer<S>(
                producerChunkSize, (Class<? extends S>) EmbergraphStatement.class);
      }

      statements.add((S) stmt);

      // total #of statements accepted.
      statementCount++;

      if (log.isTraceEnabled()) log.trace("n=" + statementCount + ", added: " + stmt);
    }

    /*
     * Buffers the asynchronous writes on the TERM2ID and BLOBS indices. Those indices will assign
     * tids. If {@link EmbergraphValue} is fully inline, then its {@link IV} is resolved
     * immediately. If the {@link EmbergraphValue} will be stored as a BLOB, then it is written onto
     * the buffer for the BLOBS index. Otherwise it is written onto the buffer for the TERM2ID
     * index.
     */
    private void bufferTidWrites() throws Exception {

      if (log.isInfoEnabled()) {
        final Map<String, EmbergraphBNode> bnodes = this.bnodes.get();
        final int bnodeCount = (bnodes == null ? 0 : bnodes.size());
        log.info(
            "bnodeCount="
                + bnodeCount
                + ", values="
                + values.size()
                + ", statementCount="
                + statementCount);
      }

      if (isAnyDone()) {

        throw new RuntimeException("Factory closed?");
      }

      /*
       * Run task which will queue EmbergraphValue[] chunks onto the TERM2ID
       * async write buffers.
       *
       * Note: This is responsible for assigning the TIDs (term
       * identifiers) to the {@link EmbergraphValue}s. We CAN NOT write on
       * the other indices until we have those TIDs.
       *
       * Note: If there is not enough load being placed the async index
       * write then it can wait up to its idle/chunk timeout. Normally we
       * want to use an infinite chunk timeout so that all chunks written
       * on the index partitions are as full as possible. Therefore, the
       * TERM2ID async writer should use a shorter idle timeout or it can
       * live lock. Ideally, there should be some explicit notice when we
       * are done queuing writes on TERM2ID across all source documents.
       * Even then we can live lock if the input queue is not large
       * enough.
       */

      /*
       * Latch notifies us when all writes for _this_ document on TERM2ID
       * are complete such that we have the assigned term identifiers for
       * all EmbergraphValues appearing in the document. This event is used
       * to transfer the document to another queue.
       */
      final KVOLatch tidsLatch =
          new KVOLatch() {

            public String toString() {

              return super.toString() + " : tidsLatch";
            }

            @Override
            protected void signal() throws InterruptedException {

              super.signal();
              /*
               * Note: There is no requirement for an atomic state
               * transition for these two counters so there is no reason
               * to take the lock here.
               */
              //                    lock.lock();
              //                    try {

              documentTIDsWaitingCount.decrementAndGet();

              documentTIDsReadyCount.incrementAndGet();

              //                    } finally {
              //
              //                        lock.unlock();
              //
              //                    }

              // Note: otherWriterService MUST have unbounded queue.
              otherWriterService.submit(
                  new BufferOtherWritesTask(AsynchronousStatementBufferImpl.this));
            }
          };

      // pre-increment to avoid notice on transient zeros.
      tidsLatch.inc(); // Note: decremented in the finally{} clause.

      try {

        final Callable<Void> task1 =
            new AsyncTerm2IdIndexWriteTask(
                tidsLatch,
                lexiconRelation,
                newValuesIterator(lexiconRelation, values.values().iterator(), producerChunkSize),
                buffer_t2id,
                buffer_blobs);

        // queue chunks onto the write buffer.
        task1.call();

      } finally {

        /*
         * Decrement now that all chunks have been queued for
         * asynchronous writes.
         */

        tidsLatch.dec();
      }

      /*
       * Note: At this point the writes on TERM2ID indices have been buffered.
       */

    }

    //        /*
    //         * Buffers the asynchronous writes on the BLOBS index.
    //         *
    //         * @throws Exception
    //         */
    //        private void bufferBlobsWrites() throws Exception {
    //
    //            if (log.isInfoEnabled()) {
    //                final Map<String, EmbergraphBNode> bnodes = this.bnodes.get();
    //                final int bnodeCount = (bnodes == null ? 0 : bnodes.size());
    //                log.info("bnodeCount=" + bnodeCount + ", values="
    //                        + values.size() + ", statementCount=" + statementCount);
    //            }
    //
    //            if (isAnyDone()) {
    //
    //                throw new RuntimeException("Factory closed?");
    //
    //            }
    //
    //            /*
    //             * Run task which will queue EmbergraphValue[] chunks onto the TERMS
    //             * async write buffer.
    //             *
    //             * Note: This is responsible for assigning the TIDs (term
    //             * identifiers) to the {@link EmbergraphValue}s. We CAN NOT write on
    //             * the other indices until we have those TIDs.
    //             *
    //             * Note: If there is not enough load being placed the async index
    //             * write then it can wait up to its idle/chunk timeout. Normally we
    //             * want to use an infinite chunk timeout so that all chunks written
    //             * on the index partitions are as full as possible. Therefore, the
    //             * TERMS async writer should use a shorter idle timeout or it can
    //             * live lock. Ideally, there should be some explicit notice when we
    //             * are done queuing writes on TERMS across all source documents.
    //             * Even then we can live lock if the input queue is not large
    //             * enough.
    //             */
    //
    //            /*
    //             * Latch notifies us when all writes for _this_ document on TERMS
    //             * are complete such that we have the assigned term identifiers for
    //             * all EmbergraphValues appearing in the document. This event is used
    //             * to transfer the document to another queue.
    //             */
    //            final KVOLatch tidsLatch = new KVOLatch() {
    //
    //                public String toString() {
    //
    //                    return super.toString() + " : tidsLatch";
    //
    //                }
    //
    //                @Override
    //                protected void signal() throws InterruptedException {
    //
    //                    super.signal();
    //                    /*
    //                     * Note: There is no requirement for an atomic state
    //                     * transition for these two counters so there is no reason
    //                     * to take the lock here.
    //                     */
    ////                    lock.lock();
    ////                    try {
    //
    //                        documentTIDsWaitingCount.decrementAndGet();
    //
    //                        documentTIDsReadyCount.incrementAndGet();
    //
    ////                    } finally {
    ////
    ////                        lock.unlock();
    ////
    ////                    }
    //
    //                    // Note: otherWriterService MUST have unbounded queue.
    //                    otherWriterService.submit(new BufferOtherWritesTask(
    //                            AsynchronousStatementBufferImpl.this));
    //
    //                }
    //
    //            };
    //
    //            // pre-increment to avoid notice on transient zeros.
    //            tidsLatch.inc();
    //
    //            try {
    //
    //                final Callable<Void> task = new AsyncBlobsIndexWriteTask(
    //                        tidsLatch, lexiconRelation, newValuesIterator(
    //                                lexiconRelation,
    //                                values.values().iterator(),
    //                                producerChunkSize),
    //                        buffer_blobs);
    //
    //                // queue chunks onto the write buffer.
    //                task.call();
    //
    //            } finally {
    //
    //                /*
    //                 * Decrement now that all chunks have been queued for
    //                 * asynchronous writes.
    //                 */
    //
    //                tidsLatch.dec();
    //
    //            }
    //
    //            /*
    //             * Note: At this point the writes on TERMS have been buffered.
    //             */
    //
    //        }

    /*
     * Buffers write requests for the remaining indices (everything except TERM2ID/BLOBS indices).
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private void bufferOtherWrites() throws InterruptedException, ExecutionException {

      if (log.isDebugEnabled()) {

        log.debug("Writing on remaining indices.");
      }

      /*
       * Setup tasks which can run asynchronously. These tasks have no
       * dependencies. They can each proceed at their own rate. However,
       * we can not return from within this method until they are all
       * done.
       *
       * Note: Each task runs in parallel.
       *
       * Note: Each task uses the asynchronous write API. When the Future
       * for that task is complete all it means is that the data are now
       * buffered on the asynchronous write buffer for the appropriate
       * index. It DOES NOT mean that those writes are complete. However,
       * the [documentStableLatch] DOES indicate when the data is restart
       * safe.
       *
       * Note: These tasks all process iterators. This approach was chosen
       * to isolate the tasks (which queue data for asynchronous writes)
       * from the data structures in this IStatementBuffer implementation.
       * An example of something which WOULD NOT work is if these tasks
       * were inner classes accessing the instance fields on this class
       * since reset() would clear those fields which might cause
       * spontaneous failures within ongoing processing.
       */
      final List<Callable> tasks = new LinkedList<Callable>();

      /*
       * The #of triples parsed from this document. This is added to the
       * total #of restart safe told triples loaded by this client when
       * the latch is triggered. Of course, the actual #of triples on the
       * database is only available by querying the database since the
       * same triple can occur in more than one document, and documents
       * are loaded by distributed clients so there is no way to correct
       * for such duplicate told triples short of querying the database.
       */
      final int toldTriplesThisDocument = statementCount;

      /*
       * Latch is signaled when all data buffered for this document is
       * RESTART SAFE on the database.
       *
       * Note: In order for the latch to have those semantics we have to
       * include it on each KVO object buffered for all remaining indices.
       * The semantics are valid in the presence of duplicate removes IFF
       * they obey the contract for KVOList and link together the
       * duplicates such that the latch is decremented for each distinct
       * KVOC instance, including those which were eliminated as
       * duplicates.
       */
      final KVOLatch documentRestartSafeLatch =
          new KVOLatch() {

            public String toString() {

              return super.toString() + " : documentRestartSafeLatch";
            }

            @Override
            protected void signal() throws InterruptedException {

              super.signal();

              lock.lock();
              try {
                workflowLatch_bufferOther.dec();
                workflowLatch_document.dec();
                assertSumOfLatchs();
                documentRestartSafeCount.incrementAndGet();
                toldTriplesRestartSafeCount.addAndGet(toldTriplesThisDocument);
                outstandingStatementCount.addAndGet(-toldTriplesThisDocument);

                // notify that the document is done.
                documentDone(getDocumentIdentifier());

              } finally {

                lock.unlock();
              }
            }
          };

      tasks.add(
          new AsyncId2TermIndexWriteTask(
              documentRestartSafeLatch,
              valueFactory,
              newId2TIterator(lexiconRelation, values.values().iterator(), producerChunkSize),
              buffer_id2t));

      if (buffer_text != null) {

        tasks.add(
            new AsyncTextIndexWriteTask(
                documentRestartSafeLatch,
                (EmbergraphValueCentricFullTextIndex) lexiconRelation.getSearchEngine(),
                newTextIterator(
                    lexiconRelation,
                    values.values().iterator(),
                    producerChunkSize,
                    indexDatatypeLiterals),
                buffer_text));
      }

      for (Map.Entry<SPOKeyOrder, IRunnableBuffer<KVO<ISPO>[]>> e : buffer_stmts.entrySet()) {

        final SPOKeyOrder keyOrder = e.getKey();

        final IRunnableBuffer<KVO<ISPO>[]> buffer = e.getValue();

        tasks.add(
            new AsyncSPOIndexWriteTask(
                documentRestartSafeLatch,
                keyOrder,
                spoRelation,
                // (IChunkedOrderedIterator<ISPO>)
                statements.iterator(),
                buffer));
      }

      /*
       * Submit all tasks. They will run in parallel. If they complete
       * successfully then all we know is that the data has been buffered
       * for asynchronous writes on the various indices.
       *
       * Note: java 1.6.0_07/12 build problems under linux when typed as
       * <Future> or any other combination that I have tried.
       */
      final List futures;

      /*
       * This latch is incremented _before_ buffering writes, and within
       * each routine that buffers writes, to avoid false triggering. This
       * is done to ensure that the latch will be positive until we exit
       * the try / finally block. We do this around the submit of the
       * tasks and do not decrement the latch until the futures are
       * available so we known that all data is buffered.
       */
      documentRestartSafeLatch.inc();
      try {

        futures = tripleStore.getExecutorService().invokeAll((List) tasks);

      } finally {

        // decrement so that the latch can be triggered.
        documentRestartSafeLatch.dec();
      }

      try {

        /*
         * Make sure that no errors were reported by those tasks.
         */
        for (Object f : futures) {

          ((Future) f).get();
        }

      } finally {

        /*
         * At this point all writes have been buffered. We now discard
         * the buffered data (RDF Values and statements) since it will
         * no longer be used.
         */
        reset();

        lock.lock();
        try {
          if (unbufferedStatementCount.addAndGet(-toldTriplesThisDocument)
              <= pauseParserPoolStatementThreshold) {
            unpaused.signalAll();
          }
        } finally {
          lock.unlock();
        }
      }
    }
  } // StatementBuffer impl.

  /** Task buffers the asynchronous writes on the TERM2ID index. */
  private class BufferTidWrites implements Callable<Void> {

    private final AsynchronousStatementBufferImpl buffer;

    public BufferTidWrites(final AsynchronousStatementBufferImpl buffer) {

      if (buffer == null) throw new IllegalArgumentException();

      this.buffer = buffer;
    }

    public Void call() throws Exception {

      //            // new workflow state.
      //            lock.lock();
      //            try {
      //                guardLatch_term2Id.inc();
      //                workflowLatch_parser.dec();
      //                workflowLatch_bufferTerm2Id.inc();
      //                documentTIDsWaitingCount.incrementAndGet();
      //                assertSumOfLatchs();
      //            } finally {
      //                lock.unlock();
      //            }

      try {

        buffer.bufferTidWrites();

        lock.lock();
        try {
          guardLatch_term2Id.dec();
        } finally {
          lock.unlock();
        }

        return null;

      } catch (Throwable t) {

        lock.lock();
        try {
          guardLatch_term2Id.dec();
          workflowLatch_bufferTids.dec();
          documentTIDsWaitingCount.decrementAndGet();
          documentError(buffer.getDocumentIdentifier(), t);
          outstandingStatementCount.addAndGet(-buffer.statementCount);
          if (unbufferedStatementCount.addAndGet(-buffer.statementCount)
              <= pauseParserPoolStatementThreshold) {
            unpaused.signalAll();
          }
          throw new Exception(t);
        } finally {
          lock.unlock();
        }
      }
    }
  }

  //    /*
  //     * Task buffers the asynchronous writes on the BLOBS index.
  //     */
  //    private class BufferBlobsWrites implements Callable<Void> {
  //
  //        private final AsynchronousStatementBufferImpl buffer;
  //
  //        public BufferBlobsWrites(final AsynchronousStatementBufferImpl buffer) {
  //
  //            if (buffer == null)
  //                throw new IllegalArgumentException();
  //
  //            this.buffer = buffer;
  //
  //        }
  //
  //        public Void call() throws Exception {
  //
  ////            // new workflow state.
  ////            lock.lock();
  ////            try {
  ////                guardLatch_term2Id.inc();
  ////                workflowLatch_parser.dec();
  ////                workflowLatch_bufferTerm2Id.inc();
  ////                documentTIDsWaitingCount.incrementAndGet();
  ////                assertSumOfLatchs();
  ////            } finally {
  ////                lock.unlock();
  ////            }
  //
  //            try {
  //
  //                buffer.bufferBlobsWrites();
  //
  //                lock.lock();
  //                try {
  //                    guardLatch_term2Id.dec();
  //                } finally {
  //                    lock.unlock();
  //                }
  //
  //                return null;
  //
  //            } catch (Throwable t) {
  //
  //                lock.lock();
  //                try {
  //                    guardLatch_term2Id.dec();
  //                    workflowLatch_bufferTerm2Id.dec();
  //                    documentTIDsWaitingCount.decrementAndGet();
  //                    documentError(buffer.getDocumentIdentifier(), t);
  //                    outstandingStatementCount.addAndGet(-buffer.statementCount);
  //                    if (unbufferedStatementCount
  //                            .addAndGet(-buffer.statementCount) <=
  // pauseParserPoolStatementThreshold) {
  //                        unpaused.signalAll();
  //                    }
  //                    throw new Exception(t);
  //                } finally {
  //                    lock.unlock();
  //                }
  //
  //            }
  //
  //        }
  //
  //    }

  /*
   * Task which buffers index writes for the remaining indices (everything other than TERM2ID).
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class BufferOtherWritesTask implements Callable<Void> {

    private final AsynchronousStatementBufferImpl buffer;

    public BufferOtherWritesTask(final AsynchronousStatementBufferImpl buffer) {

      if (buffer == null) throw new IllegalArgumentException();

      this.buffer = buffer;
    }

    public Void call() throws Exception {

      // new workflow state.
      lock.lock();
      try {
        guardLatch_other.inc();
        workflowLatch_bufferTids.dec();
        workflowLatch_bufferOther.inc();
        assertSumOfLatchs();
      } finally {
        lock.unlock();
      }

      try {

        buffer.bufferOtherWrites();

        lock.lock();
        try {
          guardLatch_other.dec();
        } finally {
          lock.unlock();
        }

        return null;

      } catch (Throwable t) {

        lock.lock();
        try {
          guardLatch_other.dec();
          workflowLatch_bufferOther.dec();
          documentError(buffer.getDocumentIdentifier(), t);
          outstandingStatementCount.addAndGet(-buffer.statementCount);
          if (unbufferedStatementCount.addAndGet(-buffer.statementCount)
              <= pauseParserPoolStatementThreshold) {
            unpaused.signalAll();
          }
          throw new Exception(t);
        } finally {
          lock.unlock();
        }
      }
    }
  }

  /*
   * Thread pool with pause/resume semantics based on the amount of buffered state for the outer
   * class.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private class ParserThreadPoolExecutor extends ThreadPoolExecutor {

    /*
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param threadFactory
     */
    public ParserThreadPoolExecutor(
        final int corePoolSize,
        final int maximumPoolSize,
        final long keepAliveTime,
        final TimeUnit unit,
        final BlockingQueue<Runnable> workQueue,
        final ThreadFactory threadFactory) {

      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /** <code>true</code> if worker tasks must wait in {@link #beforeExecute(Thread, Runnable)} */
    private boolean isPaused() {

      return unbufferedStatementCount.get() > pauseParserPoolStatementThreshold;
    }

    /*
     * Overridden to have worker threads pause if {@link #isPaused()} returns true.
     *
     * @param t The thread that will run the task.
     * @param r The {@link Runnable} wrapping the {@link AbstractTask} - this is actually a {@link
     *     FutureTask}. See {@link AbstractExecutorService}.
     */
    protected void beforeExecute(final Thread t, final Runnable r) {

      // Note: [r] is the FutureTask.

      lock.lock();

      try {

        if (isPaused()) {

          pausedThreadCount.incrementAndGet();

          poolPausedCount.incrementAndGet();

          if (log.isInfoEnabled())
            log.info("PAUSE : " + AsynchronousStatementBufferFactory.this.toString());

          while (isPaused()) {
            unpaused.await();
          }
          //                        if (!unpaused.await(60, TimeUnit.SECONDS)) {
          //
          //                            /*
          //                             * Note: This was a trial workaround for a liveness
          //                             * problem.  Unfortunately, it did not fix the
          //                             * problem. [The issue was a deadlock in the global
          //                             * LRU, which has been fixed.]
          //                             */
          //
          //                            log.error("Flushing TERM2ID buffer: "
          //                                            +
          // AbstractStatisticsCollector.fullyQualifiedHostName);
          //
          //                            reopenBuffer_term2Id();
          //
          //                            // fall through : while(isPaused()) will retest.
          //
          //                        }
          //
          //                    }

          pausedThreadCount.decrementAndGet();

          if (log.isInfoEnabled())
            log.info("RESUME: " + AsynchronousStatementBufferFactory.this.toString());
        }

      } catch (InterruptedException ie) {

        t.interrupt();

      } finally {

        lock.unlock();
      }

      super.beforeExecute(t, r);
    }
  }
} // StatementBufferFactory impl.
