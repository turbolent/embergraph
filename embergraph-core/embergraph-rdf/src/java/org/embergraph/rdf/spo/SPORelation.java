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
 * Created on Jun 21, 2008
 */

package org.embergraph.rdf.spo;

import cutthecrap.utils.striterators.ICloseableIterator;
import cutthecrap.utils.striterators.Resolver;
import cutthecrap.utils.striterators.Striterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.embergraph.bop.BOp;
import org.embergraph.bop.Constant;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IPredicate;
import org.embergraph.bop.IVariableOrConstant;
import org.embergraph.bop.NV;
import org.embergraph.bop.Var;
import org.embergraph.bop.ap.Predicate;
import org.embergraph.btree.BTree;
import org.embergraph.btree.BloomFilterFactory;
import org.embergraph.btree.DefaultTupleSerializer;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.IRangeQuery;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.btree.filter.TupleFilter;
import org.embergraph.btree.isolation.IConflictResolver;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.btree.keys.SuccessorUtil;
import org.embergraph.btree.proc.LongAggregator;
import org.embergraph.btree.raba.codec.EmptyRabaValueCoder;
import org.embergraph.btree.raba.codec.FixedLengthValueRabaCoder;
import org.embergraph.btree.raba.codec.IRabaCoder;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IResourceLock;
import org.embergraph.journal.TemporaryStore;
import org.embergraph.journal.TimestampUtility;
import org.embergraph.rdf.axioms.NoAxioms;
import org.embergraph.rdf.changesets.IChangeLog;
import org.embergraph.rdf.inf.Justification;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.constraints.RangeBOp;
import org.embergraph.rdf.internal.impl.bnode.SidIV;
import org.embergraph.rdf.lexicon.ITermIVFilter;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.sparql.ast.QuadsOperationInTriplesModeException;
import org.embergraph.rdf.sparql.ast.service.history.HistoryIndexTupleSerializer;
import org.embergraph.rdf.spo.JustIndexWriteProc.WriteJustificationsProcConstructor;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.rdf.store.IRawTripleStore;
import org.embergraph.relation.AbstractRelation;
import org.embergraph.relation.accesspath.ArrayAccessPath;
import org.embergraph.relation.accesspath.ElementFilter;
import org.embergraph.relation.accesspath.EmptyAccessPath;
import org.embergraph.relation.accesspath.IAccessPath;
import org.embergraph.relation.accesspath.IElementFilter;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.striterator.ChunkedWrappedIterator;
import org.embergraph.striterator.EmptyChunkedIterator;
import org.embergraph.striterator.IChunkedIterator;
import org.embergraph.striterator.IChunkedOrderedIterator;
import org.embergraph.striterator.IKeyOrder;

/*
 * The {@link SPORelation} handles all things related to the indices representing the triples stored
 * in the database. Statements are first converted to term identifiers using the {@link
 * LexiconRelation} and then inserted into the statement indices in parallel. There is one statement
 * index for each of the three possible access paths for a triple store. The key is formed from the
 * corresponding permutation of the subject, predicate, and object, e.g., {s,p,o}, {p,o,s}, and
 * {o,s,p} for triples or {s,p,o,c}, etc for quads. The statement type (inferred, axiom, or
 * explicit) and the optional statement identifier are stored under the key. All state for a
 * statement is replicated in each of the statement indices.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class SPORelation extends AbstractRelation<ISPO> {

  protected static final transient Logger log = Logger.getLogger(SPORelation.class);

  private final Set<String> indexNames;
  private final List<SPOKeyOrder> keyOrders;

  private final int keyArity;

  /*
   * The arity of the key for the statement indices: <code>3</code> is a triple store, with or
   * without statement identifiers; <code>4</code> is a quad store, which does not support statement
   * identifiers as the 4th position of the (s,p,o,c) is interpreted as context and located in the
   * B+Tree statement index key rather than the value associated with the key.
   */
  public int getKeyArity() {

    return keyArity;
  }

  /*
   * Hard references for the possible statement indices. The index into the array is {@link
   * SPOKeyOrder#index()}.
   */
  private final IIndex[] indices;

  /** Hard reference to the justifications index iff used. */
  private volatile IIndex just;

  /*
   * Constant for the {@link SPORelation} namespace component.
   *
   * <p>Note: To obtain the fully qualified name of an index in the {@link SPORelation} you need to
   * append a "." to the relation's namespace, then this constant, then a "." and then the local
   * name of the index.
   *
   * @see AbstractRelation#getFQN(IKeyOrder)
   */
  public static final String NAME_SPO_RELATION = "spo";

  private static final transient String NAME_JUST = "JUST";

  /*
   * This is used to conditionally enable the logic to retract justifications when the corresponding
   * statements is retracted.
   */
  public final boolean justify;

  /** This is used to conditionally disable all but a single statement index (aka access path). */
  public final boolean oneAccessPath;

  /*
   * <code>true</code> iff the SPO index will maintain a bloom filter.
   *
   * @see Options#BLOOM_FILTER
   */
  protected final boolean bloomFilter;

  /** This is used to conditionally index the {@link IChangeLog}. */
  private final boolean historyService;

  /*
   * When true, SPOs will never be removed from the indices, only downgraded to {@link
   * StatementEnum#History}.
   */
  private final boolean history;

  /*
   * When <code>true</code> the database will support statement identifiers. A statement identifier
   * is a unique 64-bit integer taken from the same space as the term identifiers and which uniquely
   * identifiers a statement in the database regardless of the graph in which that statement
   * appears. The purpose of statement identifiers is to allow statements about statements without
   * recourse to RDF style reification.
   */
  private final boolean statementIdentifiers;

  /*
   * When <code>true</code> the database will support statement identifiers.
   *
   * <p>A statement identifier is a unique 64-bit integer taken from the same space as the term
   * identifiers and which uniquely identifiers a statement in the database regardless of the graph
   * in which that statement appears. The purpose of statement identifiers is to allow statements
   * about statements without recourse to RDF style reification.
   *
   * <p>Only explicit statements will have a statement identifier. Statements made about statements
   * using their statement identifiers will automatically be retracted if a statement they describe
   * is retracted (a micro form of truth maintenance that is always enabled when statement
   * identifiers are enabled).
   */
  public boolean getStatementIdentifiers() {

    return statementIdentifiers;
  }

  public SPORelation(
      final IIndexManager indexManager,
      final String namespace,
      final Long timestamp,
      final Properties properties) {

    this(null /* container */, indexManager, namespace, timestamp, properties);
  }

  public SPORelation(
      final AbstractTripleStore container,
      final IIndexManager indexManager,
      final String namespace,
      final Long timestamp,
      final Properties properties) {

    super(container, indexManager, namespace, timestamp, properties);

    /*
     * Reads off the property for the inference engine that tells us whether
     * or not the justification index is being used. This is used to
     * conditionally enable the logic to retract justifications when the
     * corresponding statements is retracted.
     */

    this.justify =
        Boolean.parseBoolean(
            getProperty(
                AbstractTripleStore.Options.JUSTIFY, AbstractTripleStore.Options.DEFAULT_JUSTIFY));

    this.oneAccessPath =
        Boolean.parseBoolean(
            getProperty(
                AbstractTripleStore.Options.ONE_ACCESS_PATH,
                AbstractTripleStore.Options.DEFAULT_ONE_ACCESS_PATH));

    this.statementIdentifiers =
        Boolean.parseBoolean(
            getProperty(
                AbstractTripleStore.Options.STATEMENT_IDENTIFIERS,
                AbstractTripleStore.Options.DEFAULT_STATEMENT_IDENTIFIERS));

    this.historyService =
        Boolean.parseBoolean(
            getProperty(
                AbstractTripleStore.Options.HISTORY_SERVICE,
                AbstractTripleStore.Options.DEFAULT_HISTORY_SERVICE));

    this.keyArity =
        Boolean.valueOf(
                getProperty(
                    AbstractTripleStore.Options.QUADS, AbstractTripleStore.Options.DEFAULT_QUADS))
            ? 4
            : 3;

    if (statementIdentifiers && keyArity == 4) {

      throw new UnsupportedOperationException(
          AbstractTripleStore.Options.QUADS
              + " does not support the provenance mode ("
              + AbstractTripleStore.Options.STATEMENT_IDENTIFIERS
              + ")");
    }

    this.bloomFilter =
        Boolean.parseBoolean(
            getProperty(
                AbstractTripleStore.Options.BLOOM_FILTER,
                AbstractTripleStore.Options.DEFAULT_BLOOM_FILTER));

    final String historyClass = getProperty(AbstractTripleStore.Options.RDR_HISTORY_CLASS, null);
    this.history = historyClass != null && historyClass.length() > 0;

    // declare the various indices.
    {
      final Set<String> set = new HashSet<>();

      if (keyArity == 3) {

        // three indices for a triple store and the have ids in [0:2].
        this.indices = new IIndex[3];

        if (oneAccessPath) {

          set.add(getFQN(SPOKeyOrder.SPO));

          keyOrders = Collections.unmodifiableList(Arrays.asList(SPOKeyOrder.SPO));

        } else {

          set.add(getFQN(SPOKeyOrder.SPO));

          set.add(getFQN(SPOKeyOrder.POS));

          set.add(getFQN(SPOKeyOrder.OSP));

          keyOrders =
              Collections.unmodifiableList(
                  Arrays.asList(SPOKeyOrder.SPO, SPOKeyOrder.POS, SPOKeyOrder.OSP));
        }

      } else {

        // six indices for a quad store w/ ids in [3:8].
        this.indices = new IIndex[SPOKeyOrder.MAX_INDEX_COUNT];

        if (oneAccessPath) {

          set.add(getFQN(SPOKeyOrder.SPOC));

          keyOrders = Collections.unmodifiableList(Arrays.asList(SPOKeyOrder.SPOC));

        } else {

          final List<SPOKeyOrder> tmp = new ArrayList<>(6);

          for (int i = SPOKeyOrder.FIRST_QUAD_INDEX; i <= SPOKeyOrder.LAST_QUAD_INDEX; i++) {

            set.add(getFQN(SPOKeyOrder.valueOf(i)));

            tmp.add(SPOKeyOrder.valueOf(i));
          }

          keyOrders = Collections.unmodifiableList(tmp);
        }
      }

      /*
       * Note: We need the justifications index in the [indexNames] set
       * since that information is used to request the appropriate index
       * locks when running rules as mutation program in the LDS mode.
       */
      if (justify) {

        set.add(getNamespace() + "." + NAME_JUST);
      }

      this.indexNames = Collections.unmodifiableSet(set);
    }

    // Note: Do not eagerly resolve the indices.

    //        {
    //
    //            final boolean inlineTerms = Boolean.parseBoolean(getProperty(
    //                    AbstractTripleStore.Options.INLINE_TERMS,
    //                    AbstractTripleStore.Options.DEFAULT_INLINE_TERMS));
    //
    //            lexiconConfiguration = new LexiconConfiguration(inlineTerms);
    //
    //        }

  }

  /** Strengthened return type. */
  @Override
  public AbstractTripleStore getContainer() {

    return (AbstractTripleStore) super.getContainer();
  }

  /*
   * @todo This should use GRS row scan in the GRS for the SPORelation namespace. It is only used by
   *     the {@link LocalTripleStore} constructor and a unit test's main() method. This method IS
   *     NOT part of any public API at this time.
   */
  public boolean exists() {

    for (String name : getIndexNames()) {

      if (getIndex(name) == null) return false;
    }

    return true;
  }

  public void create() {

    final IResourceLock resourceLock = acquireExclusiveLock();

    try {

      // create the relation declaration metadata.
      super.create();

      final IIndexManager indexManager = getIndexManager();

      final boolean triples = keyArity == 3;

      if (triples) {

        // triples

        if (oneAccessPath) {

          indexManager.registerIndex(getStatementIndexMetadata(SPOKeyOrder.SPO));

        } else {

          for (int i = SPOKeyOrder.FIRST_TRIPLE_INDEX; i <= SPOKeyOrder.LAST_TRIPLE_INDEX; i++) {

            indexManager.registerIndex(getStatementIndexMetadata(SPOKeyOrder.valueOf(i)));
          }
        }

      } else {

        // quads

        if (oneAccessPath) {

          indexManager.registerIndex(getStatementIndexMetadata(SPOKeyOrder.SPOC));

        } else {

          for (int i = SPOKeyOrder.FIRST_QUAD_INDEX; i <= SPOKeyOrder.LAST_QUAD_INDEX; i++) {

            indexManager.registerIndex(getStatementIndexMetadata(SPOKeyOrder.valueOf(i)));
          }
        }
      }

      if (justify) {

        final String fqn = getNamespace() + "." + NAME_JUST;

        indexManager.registerIndex(getJustIndexMetadata(fqn));
      }

      if (historyService) {

        final SPOKeyOrder keyOrder = triples ? SPOKeyOrder.POS : SPOKeyOrder.PCSO;

        indexManager.registerIndex(getHistoryIndexMetadata(keyOrder));
      }

      //            lookupIndices();

    } finally {

      unlock(resourceLock);
    }
  }

  /*
   * @todo force drop of all indices rather than throwing an exception if an
   * index does not exist?
   */
  @Override
  public void destroy() {

    final IResourceLock resourceLock = acquireExclusiveLock();

    try {

      final IIndexManager indexManager = getIndexManager();

      // clear hard references.
      for (int i = 0; i < indices.length; i++) {

        indices[i] = null;
      }

      // drop indices.
      for (String name : getIndexNames()) {

        indexManager.dropIndex(name);
      }

      //            if (justify) {
      //
      //                indexManager.dropIndex(getNamespace() + "."+ NAME_JUST);
      just = null;
      //
      //            }

      // destroy the relation declaration metadata.
      super.destroy();

    } finally {

      unlock(resourceLock);
    }
  }

  /*
   * Overridden to return the hard reference for the index, which is cached the first time it is
   * resolved. This class does not eagerly resolve the indices to (a) avoid a performance hit when
   * running in a context where the index view is not required; and (b) to avoid exceptions when
   * running as an {@link ITx#UNISOLATED} {@link AbstractTask} where the index was not declared and
   * hence can not be materialized.
   */
  @Override
  public IIndex getIndex(final IKeyOrder<? extends ISPO> keyOrder) {

    final int n = ((SPOKeyOrder) keyOrder).index();

    IIndex ndx = indices[n];

    if (ndx == null) {

      synchronized (indices) {
        if ((ndx = indices[n] = super.getIndex(keyOrder)) == null) {

          throw new IllegalArgumentException(keyOrder.toString());
        }
      }
    }

    return ndx;
  }

  public final SPOKeyOrder getPrimaryKeyOrder() {

    return keyArity == 3 ? SPOKeyOrder.SPO : SPOKeyOrder.SPOC;
  }

  public final IIndex getPrimaryIndex() {

    return getIndex(getPrimaryKeyOrder());
  }

  /*
   * The optional index on which {@link Justification}s are stored.
   *
   * @todo The Justifications index is not a regular index of the SPORelation. In fact, it is a
   *     relation for proof chains and is not really of the SPORelation at all and should probably
   *     be moved onto its own JRelation. The presence of the Justification index on the SPORelation
   *     would cause problems for methods which would like to enumerate the indices, except that we
   *     just silently ignore its presence in those methods (it is not in the index[] for example).
   *     <p>This would cause the justification index namespace to change to be a peer of the
   *     SPORelation namespace.
   */
  public final IIndex getJustificationIndex() {

    if (!justify) return null;

    if (just == null) {

      synchronized (this) {

        // attempt to resolve the index and set the index reference.
        if ((just = super.getIndex(getNamespace() + "." + NAME_JUST)) == null) {

          throw new IllegalStateException();
        }
      }
    }

    return just;
  }

  /*
   * Return an iterator that will visit the distinct (s,p,o) tuples in the source iterator. The
   * context and statement type information will be stripped from the visited {@link ISPO}s. The
   * iterator will be backed by a {@link BTree} on a {@link TemporaryStore} and will use a bloom
   * filter for fast point tests. The {@link BTree} and the source iterator will be closed when the
   * returned iterator is closed.
   *
   * @param src The source iterator.
   * @return The filtered iterator.
   */
  public ICloseableIterator<ISPO> distinctSPOIterator(final ICloseableIterator<ISPO> src) {

    if (!src.hasNext()) return new EmptyChunkedIterator<>(SPOKeyOrder.SPO);

    return new DistinctSPOIterator(this, src);
  }

  /*
   * Return a new unnamed {@link BTree} instance for the {@link SPOKeyOrder#SPO} key order backed by
   * a {@link TemporaryStore}. The index will only store (s,p,o) triples (not quads) and will not
   * store either the SID or {@link StatementEnum}. This is a good choice when you need to impose a
   * "distinct" filter on (s,p,o) triples.
   *
   * @param bloomFilter When <code>true</code>, a bloom filter is enabled for the index. The bloom
   *     filter provides fast correct rejection tests for point lookups up to ~2M triples and then
   *     shuts off automatically. See {@link BloomFilterFactory#DEFAULT} for more details.
   * @return The SPO index.
   * @deprecated Comment out when we drop the {@link DistinctSPOIterator}.
   */
  public BTree getSPOOnlyBTree(final boolean bloomFilter) {

    final TemporaryStore tempStore = getIndexManager().getTempStore();

    final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());

    // leading key compression works great.
    final IRabaCoder leafKeySer = DefaultTupleSerializer.getDefaultLeafKeysCoder();

    // nothing is stored under the values.
    final IRabaCoder leafValSer = EmptyRabaValueCoder.INSTANCE;

    // setup the tuple serializer.
    metadata.setTupleSerializer(
        new SPOTupleSerializer(SPOKeyOrder.SPO, false /* sids */, leafKeySer, leafValSer));

    if (bloomFilter) {

      // optionally enable the bloom filter.
      metadata.setBloomFilterFactory(BloomFilterFactory.DEFAULT);
    }

    final BTree ndx = BTree.create(tempStore, metadata);

    return ndx;
  }

  /*
   * Overrides for the statement indices.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/150" > Choosing the index for
   *     testing fully bound access paths based on index locality</a>
   */
  protected IndexMetadata getStatementIndexMetadata(final SPOKeyOrder keyOrder) {

    final IndexMetadata metadata = newIndexMetadata(getFQN(keyOrder));

    // leading key compression works great.
    final IRabaCoder leafKeySer = DefaultTupleSerializer.getDefaultLeafKeysCoder();

    //        final IRabaCoder leafValSer;
    //        if (!statementIdentifiers) {
    //
    //            /*
    //             * Note: this value coder does not know about statement identifiers.
    //             * Therefore it is turned off if statement identifiers are enabled.
    //             */
    //
    //            leafValSer = new FastRDFValueCoder2();
    ////            leafValSer = SimpleRabaCoder.INSTANCE;
    //
    //        } else {
    //
    //            /*
    //             * The default is canonical huffman coding, which is relatively slow
    //             * and does not achieve very good compression on term identifiers.
    //             */
    //            leafValSer = DefaultTupleSerializer.getDefaultValuesCoder();
    //
    //            /*
    //             * @todo This is much faster than huffman coding, but less space
    //             * efficient. However, it appears that there are some cases where
    //             * SIDs are enabled but only the flag bits are persisted. What
    //             * gives?
    //             */
    ////            leafValSer = new FixedLengthValueRabaCoder(1 + 8);
    //
    //        }

    /*
     * We can just always use the FastRDFValueCoder now that sids are
     * inlined into the statement indices and we don't store a term id
     * for them in the SPO tuple value.
     */
    final IRabaCoder leafValSer = new FastRDFValueCoder2();

    metadata.setTupleSerializer(
        new SPOTupleSerializer(keyOrder, statementIdentifiers, leafKeySer, leafValSer));

    if ((getIndexManager() instanceof IEmbergraphFederation<?>)
        && ((IEmbergraphFederation<?>) getIndexManager()).isScaleOut()
        && keyOrder.getKeyArity() == 4
        && getContainer().isConstrainXXXCShards()
        && keyOrder.getIndexName().endsWith("C")) {

      /*
       * Apply a constraint to an xxxC index such that all quads for the
       * same triple are in the same shard.
       */
      metadata.setSplitHandler(new XXXCShardSplitHandler());
    }

    if (bloomFilter && (keyOrder.equals(SPOKeyOrder.SPO) || keyOrder.equals(SPOKeyOrder.SPOC))) {

      //          * Enable the bloom filter for the SPO index only.
      //          *
      //          * Note: This SPO index is used any time we have an access path that
      //          * is a point test. Therefore this is the only index for which it
      //          * makes sense to maintain a bloom filter.

      /*
       * Enable the bloom filter.
       *
       * Historically, the bloom filter was only enabled in for the SPO
       * index because the SPO index was always used for a point test.
       * Further, by oversight, it was never enabled for a quads mode KB
       * instance. However, in order to improve the locality of reference
       * for point tests, the SPOKeyOrder#getKeyOrder() method was
       * modified to use the index which would be used if we evaluated the
       * original predicate rather than the "as-bound" predicate. This
       * change provides better locality of access since the variables
       * indicate the region of variability in the index while the
       * constants indicate the region of locality. Therefore, fully bound
       * (aka point tests) can now occur on ANY statement index and the
       * bloom filter is now enabled on all statement indices.
       *
       * @see https://sourceforge.net/apps/trac/bigdata/ticket/150
       * (chosing the index for testing fully bound access paths based on
       * index locality)
       */

      /*
       * Note: The maximum error rate (maxP) applies to the mutable BTree
       * only. For scale-out indices, there is one mutable BTree per index
       * partition and a new (empty) BTree is allocated each time the live
       * journal for the index partitions overflows.
       *
       * Note: The bloom filter is disabled once the error rate grows too
       * large. However, in scale-out, we are able to put a perfect fit
       * bloom filter in each index segment. This can significantly reduce
       * the IO costs associated with point tests. This can not be done in
       * the single machine database mode because the error rate of the
       * bloom filter is a function of the size of the bloom filter and
       * the #of entries in the index.
       */

      //            // good performance up to ~2M triples.
      //            final int n = 1000000; // 1M
      //            final double p = 0.01;
      //            final double maxP = 0.20;

      //            // good performance up to ~20M triples.
      //            final int n = 10000000; // 10M
      //            final double p = 0.05;
      //            final double maxP = 0.20;

      //            final BloomFilterFactory factory = new BloomFilterFactory(n, p, maxP);

      final BloomFilterFactory factory = BloomFilterFactory.DEFAULT;

      if (log.isInfoEnabled()) log.info("Enabling bloom filter for SPO index: " + factory);

      metadata.setBloomFilterFactory(factory);
    }

    if (TimestampUtility.isReadWriteTx(getTimestamp())) {

      // enable isolatable indices.
      metadata.setIsolatable(true);

      /*
       * This tests to see whether or not the axiomsClass is NoAxioms. If
       * so, then we understand inference to be disabled and can use a
       * conflict resolver for plain triples, plain quads, or even plain
       * sids (SIDs are assigned by the lexicon using unisolated
       * operations).
       *
       * @todo we should have an explicit "no inference" property. this
       * jumps through hoops since we can not call getAxioms() on the
       * AbstractTripleStore has been created, and SPORelation#create()
       * is invoked from within AbstractTripleStore#create().  When
       * adding that property, update a bunch of unit tests and code
       * which tests on the axioms class or BaseAxioms#isNone().
       */
      if (NoAxioms.class
          .getName()
          .equals(
              getContainer()
                  .getProperties()
                  .getProperty(
                      AbstractTripleStore.Options.AXIOMS_CLASS,
                      AbstractTripleStore.Options.DEFAULT_AXIOMS_CLASS))) {

        metadata.setConflictResolver(new SPOWriteWriteResolver());
      }
    }

    return metadata;
  }

  /*
   * Overrides for the statement indices.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/607" > History Service </a>
   */
  protected IndexMetadata getHistoryIndexMetadata(final SPOKeyOrder keyOrder) {

    final IndexMetadata metadata = newIndexMetadata(getFQN(this, NAME_HISTORY));

    // leading key compression works great.
    final IRabaCoder leafKeySer = DefaultTupleSerializer.getDefaultLeafKeysCoder();

    /*
     * There is one byte per tuple. The low nibble is the flags and
     * statement enum. The high nibble is the ChangeAction.
     */
    final IRabaCoder leafValSer = new FixedLengthValueRabaCoder(1 /* len */);

    metadata.setTupleSerializer(
        new HistoryIndexTupleSerializer(keyOrder, statementIdentifiers, leafKeySer, leafValSer));

    if (TimestampUtility.isReadWriteTx(getTimestamp())) {

      /*
       * Enable isolatable indices.
       *
       * Note: we can not use an unisolated history index since changes
       * could become committed before a given tx commits due to a
       * concurrent tx commit. That would break the semantics of the
       * history index if the tx then fails rather than committing since
       * some of its changes would have become visible and durable anyway.
       */

      metadata.setIsolatable(true);

      /*
       * TODO We might need to add a write-write resolver for the history
       * index.
       */

      //            metadata.setConflictResolver(new HistoryWriteWriteResolver());

    }

    return metadata;
  }

  public static final transient String NAME_HISTORY = "HIST";

  /*
   * Conflict resolver for add/add conflicts and retract/retract conflicts for any of (triple store,
   * triple store with SIDs or quad store) but without inference. For an add/retract conflict, the
   * writes can not be reconciled and the transaction can not be validated. Write-write conflict
   * reconciliation is not supported if inference is enabled. The truth maintenance behavior makes
   * this too complex.
   *
   * @todo It is really TM, not inference, which makes this complicated. However, if we allow
   *     statement type information into the statement index values then we must also deal with that
   *     information in the conflict resolver.
   * @todo In both cases where we can resolve the conflict, the tuple could be withdrawn from the
   *     writeSet since the desired tuple is already in the unisolated index rather than causing it
   *     to be touched on the unisolated index. This would have the advantage of minimizing writes
   *     on the unisolated index.
   */
  private static class SPOWriteWriteResolver implements IConflictResolver {

    /** */
    private static final long serialVersionUID = -1591732801502917983L;

    public SPOWriteWriteResolver() {}

    public boolean resolveConflict(IIndex writeSet, ITuple txTuple, ITuple currentTuple)
        throws Exception {

      /*
       * Note: In fact, retract-retract conflicts SHOULD NOT be resolved
       * because retracts are often used to remove an old property value
       * when a new value will be assigned for that property. For example,
       *
       * tx1: -red, +green
       *
       * tx2: -red, +blue
       *
       * if we resolve the retract-retract conflict, then we will get
       * {green,blue} after the transactions run, rather than either
       * {green} or {blue}.
       */
      //            if (txTuple.isDeletedVersion() && currentTuple.isDeletedVersion()) {
      //
      ////                System.err.println("Resolved retract/retract conflict");
      //
      //                // retract/retract is not a conflict.
      //                return true;
      //
      //            }

      //                System.err.println("Resolved add/add conflict");
      // add/add is not a conflict.
      return !txTuple.isDeletedVersion() && !currentTuple.isDeletedVersion();

      /*
       * Note: We don't need to materialize the SPOs to resolve the
       * conflict, but this is how you would do that.
       */
      // final ISPO txSPO = (ISPO) txTuple.getObject();
      //
      // final ISPO currentSPO = (ISPO) txTuple.getObject();

      // either delete/add or add/delete is a conflict.
    }
  }

  /** Overrides for the {@link IRawTripleStore#getJustificationIndex()}. */
  protected IndexMetadata getJustIndexMetadata(final String name) {

    final IndexMetadata metadata = newIndexMetadata(name);

    metadata.setTupleSerializer(new JustificationTupleSerializer(keyArity));

    return metadata;
  }

  @Override
  public Set<String> getIndexNames() {

    return indexNames;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Iterator<IKeyOrder<ISPO>> getKeyOrders() {

    return (Iterator) keyOrders.iterator();
  }

  /** Return an iterator visiting each {@link IKeyOrder} maintained by this relation. */
  public Iterator<SPOKeyOrder> statementKeyOrderIterator() {

    switch (keyArity) {
      case 3:
        if (oneAccessPath) return SPOKeyOrder.spoOnlyKeyOrderIterator();

        return SPOKeyOrder.tripleStoreKeyOrderIterator();

      case 4:
        if (oneAccessPath) return SPOKeyOrder.spocOnlyKeyOrderIterator();

        return SPOKeyOrder.quadStoreKeyOrderIterator();

      default:
        throw new AssertionError();
    }
  }

  /*
   * Return the access path for a triple pattern.
   *
   * @param s
   * @param p
   * @param o
   * @throws UnsupportedOperationException unless the {@link #getKeyArity()} is <code>3</code>.
   * @deprecated by {@link #getAccessPath(IV, IV, IV, IV)}
   */
  public IAccessPath<ISPO> getAccessPath(final IV s, final IV p, final IV o) {

    if (keyArity != 3) throw new UnsupportedOperationException();

    return getAccessPath(s, p, o, null /* c */, null /* filter */, null /* range */);
  }

  /** Return the access path for a triple or quad pattern. */
  public IAccessPath<ISPO> getAccessPath(final IV s, final IV p, final IV o, final IV c) {

    return getAccessPath(s, p, o, c, null /*filter*/, null /*range*/);
  }

  /** Return the access path for a triple or quad pattern with a range. */
  public IAccessPath<ISPO> getAccessPath(
      final IV s, final IV p, final IV o, final IV c, final RangeBOp range) {

    return getAccessPath(s, p, o, c, null /*filter*/, range);
  }

  /** Return the access path for a triple or quad pattern with a filter. */
  public IAccessPath<ISPO> getAccessPath(
      final IV s, final IV p, final IV o, final IV c, IElementFilter<ISPO> filter) {
    return getAccessPath(s, p, o, c, filter, null);
  }

  /*
   * Return the access path for a triple or quad pattern with an optional filter (core
   * implementation). All arguments are optional. Any bound argument will restrict the returned
   * access path. For a triple pattern, <i>c</i> WILL BE IGNORED as there is no index over the
   * statement identifiers, even when they are enabled. For a quad pattern, any argument MAY be
   * bound.
   *
   * @param s The subject position (optional).
   * @param p The predicate position (optional).
   * @param o The object position (optional).
   * @param c The context position (optional and ignored for a triple store).
   * @param filter The filter (optional).
   * @param range The range (optional).
   * @return The best access path for that triple or quad pattern.
   * @throws UnsupportedOperationException for a triple store without statement identifiers if the
   *     <i>c</i> is non-{@link #NULL}.
   */
  public IAccessPath<ISPO> getAccessPath(
      final IV s,
      final IV p,
      final IV o,
      final IV c,
      IElementFilter<ISPO> filter,
      final RangeBOp range) {

    final IPredicate<ISPO> pred = getPredicate(s, p, o, c, filter, range);

    return getAccessPath(pred);
  }

  /*
   * Return the predicate for a triple or quad pattern filter (core implementation). All arguments
   * are optional. Any bound argument will restrict the returned access path. For a triple pattern,
   * <i>c</i> WILL BE IGNORED as there is no index over the statement identifiers, even when they
   * are enabled. For a quad pattern, any argument MAY be bound.
   *
   * @param s The subject position (optional).
   * @param p The predicate position (optional).
   * @param o The object position (optional).
   * @param c The context position (optional and ignored for a triple store).
   * @return The predicate for that triple or quad pattern.
   * @throws UnsupportedOperationException for a triple store without statement identifiers if the
   *     <i>c</i> is non-{@link #NULL}.
   */
  public IPredicate<ISPO> getPredicate(final IV s, final IV p, final IV o, final IV c) {

    return getPredicate(s, p, o, c, null, null);
  }

  /*
   * Return the predicate for a triple or quad pattern with an optional filter (core
   * implementation). All arguments are optional. Any bound argument will restrict the returned
   * access path. For a triple pattern, <i>c</i> WILL BE IGNORED as there is no index over the
   * statement identifiers, even when they are enabled. For a quad pattern, any argument MAY be
   * bound.
   *
   * @param s The subject position (optional).
   * @param p The predicate position (optional).
   * @param o The object position (optional).
   * @param c The context position (optional and ignored for a triple store).
   * @param filter The filter (optional).
   * @param range The range (optional).
   * @return The predicate for that triple or quad pattern.
   * @throws UnsupportedOperationException for a triple store without statement identifiers if the
   *     <i>c</i> is non-{@link #NULL}.
   */
  public IPredicate<ISPO> getPredicate(
      final IV s,
      final IV p,
      final IV o,
      final IV c,
      IElementFilter<ISPO> filter,
      final RangeBOp range) {

    final IVariableOrConstant<IV> S = (s == null ? Var.var("s") : new Constant<>(s));

    final IVariableOrConstant<IV> P = (p == null ? Var.var("p") : new Constant<>(p));

    final IVariableOrConstant<IV> O = (o == null ? Var.var("o") : new Constant<>(o));

    IVariableOrConstant<IV> C = null;

    switch (keyArity) {
      case 3:
        if (statementIdentifiers) {
          C = (c == null ? Var.var("c") : new Constant<>(c));
        } else if (c != null) {
          /*
           * The 4th position should never become bound for a triple store
           * without statement identifiers.
           */

          throw new RuntimeException(
              new QuadsOperationInTriplesModeException(
                  "Predicate lookup with bound context, but DB is initialized"
                      + " in triples mode. Please do either re-init your database"
                      + " in quads mode or use operations over triples only."));
        }
        break;
      case 4:
        C = (c == null ? Var.var("c") : new Constant<>(c));
        break;
      default:
        throw new AssertionError();
    }

    final Map<String, Object> anns = new LinkedHashMap<>();
    anns.put(IPredicate.Annotations.RELATION_NAME, new String[] {getNamespace()});
    if (range != null) {
      anns.put(IPredicate.Annotations.RANGE, range);
    }

    Predicate<ISPO> pred =
        new SPOPredicate(
            //				keyArity == 4 ?
            (keyArity == 4 || statementIdentifiers) ? new BOp[] {S, P, O, C} : new BOp[] {S, P, O},
            anns);

    if (filter != null) {
      // Layer on an optional filter.
      pred = pred.addIndexLocalFilter(ElementFilter.newInstance(filter));
    }

    return pred;
  }

  //    /*
  //     * Return the {@link IAccessPath} that is most efficient for the specified
  //     * predicate based on an analysis of the bound and unbound positions in the
  //     * predicate.
  //     * <p>
  //     * Note: When statement identifiers are enabled, the only way to bind the
  //     * context position is to already have an {@link SPO} on hand. There is no
  //     * index which can be used to look up an {@link SPO} by its context and the
  //     * context is always a blank node.
  //     * <p>
  //     * Note: This method is a hot spot, especially when the maximum parallelism
  //     * for subqueries is large. A variety of caching techniques are being
  //     * evaluated to address this.
  //     *
  //     * @param pred
  //     *            The predicate.
  //     *
  //     * @return The best access path for that predicate.
  //     */
  //    public IAccessPath<ISPO> getAccessPath(final IPredicate<ISPO> predicate) {
  //
  //        /*
  //         * Note: Query is faster w/o cache on all LUBM queries.
  //         *
  //         * @todo Optimization could reuse a caller's SPOAccessPath instance,
  //         * setting only the changed data on the fromKey/toKey.  That could
  //         * be done with setS(long), setO(long), setP(long) methods.  The
  //         * filter could be modified in the same manner.  That could cut down
  //         * on allocation costs, formatting the from/to keys, etc.
  //         */
  //
  ////        if (predicate.getPartitionId() != -1) {
  //
  ////            /*
  ////             * Note: This handles a read against a local index partition.
  ////             *
  ////             * Note: This does not work here because it has the federation's
  ////             * index manager rather than the data service's index manager. That
  ////             * is because we always resolve relations against the federation
  ////             * since their metadata is stored in the global row store. Maybe
  ////             * this could be changed if we developed the concept of a
  ////             * "relation shard" accessed the metadata via a catalog and which
  ////             * was aware that only one index shard could be resolved locally.
  ////             */
  //
  ////            return getAccessPathForIndexPartition(predicate);
  //
  ////        }
  //
  //        return _getAccessPath(predicate);
  //
  //    }

  //    /*
  //     * Isolates the logic for selecting the {@link SPOKeyOrder} from the
  //     * {@link SPOPredicate} and then delegates to
  //     * {@link #getAccessPath(IKeyOrder, IPredicate)}.
  //     */
  //    final private SPOAccessPath _getAccessPath(final IPredicate<ISPO> predicate) {
  //
  //        final SPOKeyOrder keyOrder = getKeyOrder(predicate);
  //
  //        final SPOAccessPath accessPath = getAccessPath(keyOrder, predicate);
  //
  //        if (log.isDebugEnabled())
  //            log.debug(accessPath.toString());
  //
  //        //            System.err.println("new access path: pred="+predicate);
  //
  //        return accessPath;
  //
  //    }

  /*
   * Implementation chooses a quads or triples index as appropriate.
   *
   * <p>{@inheritDoc}
   *
   * @todo This should recognize when only the primary index is available and then use it for all
   *     access paths.
   */
  public SPOKeyOrder getKeyOrder(final IPredicate<ISPO> predicate) {

    return SPOKeyOrder.getKeyOrder(predicate, keyArity);
  }

  //    /*
  //     * This handles a request for an access path that is restricted to a
  //     * specific index partition.
  //     * <p>
  //     * Note: This path is used with the scale-out JOIN strategy, which
  //     * distributes join tasks onto each index partition from which it needs to
  //     * read. Those tasks constrain the predicate to only read from the index
  //     * partition which is being serviced by that join task.
  //     * <p>
  //     * Note: Since the relation may materialize the index views for its various
  //     * access paths, and since we are restricted to a single index partition and
  //     * (presumably) an index manager that only sees the index partitions local
  //     * to a specific data service, we create an access path view for an index
  //     * partition without forcing the relation to be materialized.
  //     * <p>
  //     * Note: Expanders ARE NOT applied in this code path. Expanders require a
  //     * total view of the relation, which is not available during scale-out
  //     * pipeline joins.
  //     *
  //     * @param indexManager
  //     *            This MUST be the data service local index manager so that the
  //     *            returned access path will read against the local shard.
  //     * @param predicate
  //     *            The predicate. {@link IPredicate#getPartitionId()} MUST return
  //     *            a valid index partition identifier.
  //     *
  //     * @throws IllegalArgumentException
  //     *             if either argument is <code>null</code>.
  //     * @throws IllegalArgumentException
  //     *             unless the {@link IIndexManager} is a <em>local</em> index
  //     *             manager providing direct access to the specified shard.
  //     * @throws IllegalArgumentException
  //     *             unless the predicate identifies a specific shard using
  //     *             {@link IPredicate#getPartitionId()}.
  //     *
  //     * @deprecated {@link AccessPath} is handling this directly based on the
  //     *             {@link IPredicate.Annotations#PARTITION_ID}.
  //     */
  //    @Override
  //    public IAccessPath<ISPO> getAccessPathForIndexPartition(
  //            final IIndexManager indexManager,
  //            final IPredicate<ISPO> predicate
  //            ) {
  //
  //        return getAccessPath(indexManager,getKeyOrder(predicate),predicate);
  //
  ////        /*
  ////         * Note: getIndexManager() _always_ returns the federation's index
  ////         * manager because that is how we materialize an ILocatableResource when
  ////         * we locate it. However, the federation's index manager can not be used
  ////         * here because it addresses the scale-out indices. Instead, the caller
  ////         * must pass in the IIndexManager which has access to the local index
  ////         * objects so we can directly read on the shard.
  ////         */
  //////        final IIndexManager indexManager = getIndexManager();
  //
  ////        if (indexManager == null)
  ////            throw new IllegalArgumentException();
  //
  ////        if (indexManager instanceof IEmbergraphFederation<?>) {
  //
  ////            /*
  ////             * This will happen if you fail to re-create the JoinNexus within
  ////             * the target execution environment.
  ////             *
  ////             * This is disallowed because the predicate specifies an index
  ////             * partition and expects to have access to the local index objects
  ////             * for that index partition. However, the index partition is only
  ////             * available when running inside of the ConcurrencyManager and when
  ////             * using the IndexManager exposed by the ConcurrencyManager to its
  ////             * tasks.
  ////             */
  //
  ////            throw new IllegalArgumentException(
  ////                    "Expecting a local index manager, not: "
  ////                            + indexManager.getClass().toString());
  //
  ////        }
  ////
  ////        if (predicate == null)
  ////            throw new IllegalArgumentException();
  //
  ////        final int partitionId = predicate.getPartitionId();
  //
  ////        if (partitionId == -1) // must be a valid partition identifier.
  ////            throw new IllegalArgumentException();
  //
  ////        /*
  ////         * @todo This condition should probably be an error since the expander
  ////         * will be ignored.
  ////         */
  //////        if (predicate.getSolutionExpander() != null)
  //////            throw new IllegalArgumentException();
  //
  ////        if (predicate.getRelationCount() != 1) {
  //
  ////            /*
  ////             * This is disallowed. The predicate must be reading on a single
  ////             * local index partition, not a view comprised of more than one
  ////             * index partition.
  ////             *
  ////             * @todo In fact, we could allow a view here as long as all parts of
  ////             * the view are local. That could be relevant when the other view
  ////             * component was a shard of a focusStore for parallel decomposition
  ////             * of RDFS closure, etc. The best way to handle such views when the
  ////             * components are not local is to use a UNION of the JOIN. When both
  ////             * parts are local we can do better using a UNION of the
  ////             * IAccessPath.
  ////             */
  ////
  ////            throw new IllegalStateException();
  ////
  ////        }
  ////
  ////        final String namespace = getNamespace();//predicate.getOnlyRelationName();
  //
  ////        /*
  ////         * Find the best access path for that predicate.
  ////         */
  ////        final SPOKeyOrder keyOrder = getKeyOrder(predicate);
  //
  ////        // The name of the desired index partition.
  ////        final String name = DataService.getIndexPartitionName(namespace + "."
  ////                + keyOrder.getIndexName(), predicate.getPartitionId());
  //
  ////        /*
  ////         * Note: whether or not we need both keys and values depends on the
  ////         * specific index/predicate.
  ////         *
  ////         * Note: If the timestamp is a historical read, then the iterator will
  ////         * be read only regardless of whether we specify that flag here or not.
  ////         */
  //////      * Note: We can specify READ_ONLY here since the tail predicates are not
  //////      * mutable for rule execution.
  ////        final int flags = IRangeQuery.KEYS | IRangeQuery.VALS;// | IRangeQuery.READONLY;
  //
  ////        final long timestamp = getTimestamp();//getReadTimestamp();
  ////
  ////        // MUST be a local index view.
  ////        final ILocalBTreeView ndx = (ILocalBTreeView) indexManager
  ////                .getIndex(name, timestamp);
  //
  ////        return newAccessPath(this/* relation */, indexManager, timestamp,
  ////                predicate, keyOrder, ndx, flags, getChunkOfChunksCapacity(),
  ////                getChunkCapacity(), getFullyBufferedReadThreshold());
  //
  //    }

  //    /*
  //     * Core impl.
  //     *
  //     * @param keyOrder
  //     *            The natural order of the selected index (this identifies the
  //     *            index).
  //     * @param predicate
  //     *            The predicate specifying the query constraint on the access
  //     *            path.
  //     *
  //     * @return The access path.
  //     */
  //    public SPOAccessPath getAccessPath(final IIndexManager localIndexManager,
  //            final IKeyOrder<ISPO> keyOrder, final IPredicate<ISPO> predicate) {
  //
  //        if (keyOrder == null)
  //            throw new IllegalArgumentException();
  //
  //        if (predicate == null)
  //            throw new IllegalArgumentException();
  //
  //        final IIndex ndx = getIndex(keyOrder);
  //
  //        if (ndx == null) {
  //
  //            throw new IllegalArgumentException("no index? relation="
  //                    + getNamespace() + ", timestamp=" + getTimestamp()
  //                    + ", keyOrder=" + keyOrder + ", pred=" + predicate
  //                    + ", indexManager=" + getIndexManager());
  //
  //        }
  //
  //        /*
  //         * Note: The PARALLEL flag here is an indication that the iterator may
  //         * run in parallel across the index partitions. This only effects
  //         * scale-out and only for simple triple patterns since the pipeline join
  //         * does something different (it runs inside the index partition using
  //         * the local index, not the client's view of a distributed index).
  //         *
  //         * @todo Introducing the PARALLEL flag here will break semantics when
  //         * the rule is supposed to be "stable" (repeatable order) or when the
  //         * access path is supposed to be fully ordered. What we really need to
  //         * do is take the "stable" flag from the rule and transfer it onto the
  //         * predicates in that rule so we can enforce stable execution for
  //         * scale-out. Of course, that would kill any parallelism for scale-out
  //         * :-) This shows up as an issue when using SLICEs since the rule that
  //         * we execute has to have stable results for someone to page through
  //         * those results using LIMIT/OFFSET.
  //         */
  //        final int flags = IRangeQuery.KEYS
  //                | IRangeQuery.VALS
  //                | (TimestampUtility.isReadOnly(getTimestamp()) ? IRangeQuery.READONLY
  //                        : 0)
  //                | IRangeQuery.PARALLEL
  //                ;
  //
  //        final AbstractTripleStore container = getContainer();
  //
  //        final int chunkOfChunksCapacity = container.getChunkOfChunksCapacity();
  //
  //        final int chunkCapacity = container.getChunkCapacity();
  //
  //        final int fullyBufferedReadThreshold = container.getFullyBufferedReadThreshold();
  //
  //        return newAccessPath(this, getIndexManager(), getTimestamp(),
  //                predicate, keyOrder, ndx, flags, chunkOfChunksCapacity,
  //                chunkCapacity, fullyBufferedReadThreshold);
  //
  //    }

  @Override
  public IAccessPath<ISPO> newAccessPath(
      //            final IRelation<ISPO> relation,
      final IIndexManager localIndexManager,
      //            final long timestamp,
      final IPredicate<ISPO> predicate,
      final IKeyOrder<ISPO> keyOrder
      //            final IIndex ndx,
      //            final int flags,
      //            final int chunkOfChunksCapacity,
      //            final int chunkCapacity,
      //            final int fullyBufferedReadThreshold
      ) {

    if (statementIdentifiers && predicate.arity() == 4) {

      /*
       * Since sids are inlined into the statement indices and the SidIV
       * can materialize the SPO to which it refers directly, we may be
       * able to avoid going to the indices altogether in the case when
       * the C position is bound in the predicate.
       */
      final IVariableOrConstant<IV> sid = predicate.get(3);

      if (sid != null && sid.isConstant() && sid.get() instanceof SidIV) {

        final SidIV sidIV = (SidIV) sid.get();

        final ISPO spo = sidIV.getInlineValue();

        /*
         * We need to check the inline SPO against the predicate to
         * make sure it matches the triple pattern implied by the
         * predicate.  Usually in this case s, p, and o are unbound
         * (reverse lookup from SID to spo), but occasionally there
         * will be a bound term inside the predicate.  In this case
         * we should return an empty access path if the SPO does not
         * match the triple pattern.
         */
        for (int i = 0; i <= 2; i++) {

          final IVariableOrConstant<IV> t = predicate.get(i);

          if (t != null && t.isConstant()) {

            final IV iv = t.get();

            if (!spo.get(i).equals(iv)) {

              return new EmptyAccessPath<>(predicate, SPOKeyOrder.SPO);
            }
          }
        }

        if (log.isDebugEnabled()) {
          log.debug("materializing an inline SID access path: " + spo);
        }

        return new ArrayAccessPath<>(new ISPO[]{spo}, predicate, SPOKeyOrder.SPO);
      }
    }

    final IPredicate<ISPO> historyFiltered;
    /*
     * If we are in history mode and the predicate does not have the
     * include history annotation, attached an index local filter to filter
     * out SPOs where type == StatementEnum.History.
     */
    if (history && !predicate.getProperty(SPOPredicate.Annotations.INCLUDE_HISTORY, false)) {

      historyFiltered =
          ((Predicate<ISPO>) predicate)
              .addIndexLocalFilter(ElementFilter.newInstance(HistorySPOFilter.INSTANCE));

    } else {

      historyFiltered = predicate;
    }

    return new SPOAccessPath(
            this /*relation*/,
            localIndexManager, // timestamp,
            historyFiltered,
            keyOrder
            //                , ndx, flags, chunkOfChunksCapacity,
            //                chunkCapacity, fullyBufferedReadThreshold
            )
        .init();
  }

  //    public long getElementCount(boolean exact) {
  //
  //        final IIndex ndx = getIndex(SPOKeyOrder.SPO);
  //
  //        if (exact) {
  //
  //            return ndx.rangeCountExact(null/* fromKey */, null/* toKey */);
  //
  //        } else {
  //
  //            return ndx.rangeCount(null/* fromKey */, null/* toKey */);
  //
  //        }
  //
  //    }

  /*
   * Efficient scan of the distinct term identifiers that appear in the first position of the keys
   * for the statement index corresponding to the specified {@link IKeyOrder}. For example, using
   * {@link SPOKeyOrder#POS} will give you the term identifiers for the distinct predicates actually
   * in use within statements in the {@link SPORelation}.
   *
   * @param keyOrder The selected index order.
   * @return An iterator visiting the distinct term identifiers.
   */
  public IChunkedIterator<IV> distinctTermScan(final IKeyOrder<ISPO> keyOrder) {

    return distinctTermScan(keyOrder, /* termIdFilter */ null);
  }

  /*
   * Efficient scan of the distinct term identifiers that appear in the first position of the keys
   * for the statement index corresponding to the specified {@link IKeyOrder}. For example, using
   * {@link SPOKeyOrder#POS} will give you the term identifiers for the distinct predicates actually
   * in use within statements in the {@link SPORelation}.
   *
   * @param keyOrder The selected index order.
   * @param filter An optional filter on the visited {@link IV}s.
   * @return An iterator visiting the distinct term identifiers.
   * @todo add the ability to specify {@link IRangeQuery#PARALLEL} here for fast scans across
   *     multiple shards when chunk-wise order is Ok.
   */
  public IChunkedIterator<IV> distinctTermScan(
      final IKeyOrder<ISPO> keyOrder, final ITermIVFilter filter) {

    return distinctTermScan(keyOrder, null /* fromKey */, null /* toKey */, filter);
  }

  /*
   * Efficient scan of the distinct term identifiers that appear in the first position of the keys
   * for the statement index corresponding to the specified {@link IKeyOrder}. For example, using
   * {@link SPOKeyOrder#POS} will give you the term identifiers for the distinct predicates actually
   * in use within statements in the {@link SPORelation}.
   *
   * @param keyOrder The selected index order.
   * @param fromKey The first key for the scan -or- <code>null</code> to start the scan at the head
   *     of the index.
   * @param toKey The last key (exclusive upper bound) for the scan -or- <code>null</code> to scan
   *     until the end of the index.
   * @param termIdFilter An optional filter on the visited {@link IV}s.
   * @return An iterator visiting the distinct term identifiers.
   * @todo add the ability to specify {@link IRangeQuery#PARALLEL} here for fast scans across
   *     multiple shards when chunk-wise order is Ok.
   */
  public IChunkedIterator<IV> distinctTermScan(
      final IKeyOrder<ISPO> keyOrder,
      final byte[] fromKey,
      final byte[] toKey,
      final ITermIVFilter termIdFilter) {

    final DistinctTermAdvancer filter = new DistinctTermAdvancer(keyArity);

    /*
     * Layer in the logic to advance to the tuple that will have the
     * next distinct term identifier in the first position of the key.
     */

    if (termIdFilter != null) {

      /*
       * Layer in a filter for only the desired term types.
       */

      filter.addFilter(
          new TupleFilter<SPO>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isValid(final ITuple<SPO> tuple) {

              final byte[] key = tuple.getKey();

              final IV iv = IVUtility.decode(key);

              return termIdFilter.isValid(iv);
            }
          });
    }

    @SuppressWarnings("unchecked")
    final Iterator<IV> itr =
        new Striterator(
                getIndex(keyOrder)
                    .rangeIterator(
                        fromKey,
                        toKey,
                        0 /* capacity */,
                        IRangeQuery.KEYS | IRangeQuery.CURSOR,
                        filter))
            .addFilter(
                new Resolver() {

                  private static final long serialVersionUID = 1L;

                  /** Resolve SPO key to IV. */
                  @Override
                  protected IV resolve(Object obj) {

                    final byte[] key = ((ITuple) obj).getKey();

                    return IVUtility.decode(key);
                  }
                });

    //        return new ChunkedWrappedIterator<IV>(itr);
    return new ChunkedWrappedIterator<>(itr, IChunkedIterator.DEFAULT_CHUNK_SIZE, IV.class);
  }

  /*
   * Efficient scan of the distinct term identifiers that appear in the first position of the keys
   * for the statement index corresponding to the specified {@link IKeyOrder}. For example, using
   * {@link SPOKeyOrder#POS} will give you the term identifiers for the distinct predicates actually
   * in use within statements in the {@link SPORelation}.
   *
   * @param keyOrder The selected index order.
   * @return An iterator visiting the distinct term identifiers.
   */
  public IChunkedIterator<IV> distinctMultiTermScan(
      final IKeyOrder<ISPO> keyOrder, IV[] knownTerms) {

    return distinctMultiTermScan(keyOrder, knownTerms, /* termIdFilter */ null);
  }

  /*
   * Efficient scan of the distinct term identifiers that appear in the first position of the keys
   * for the statement index corresponding to the specified {@link IKeyOrder}. For example, using
   * {@link SPOKeyOrder#POS} will give you the term identifiers for the distinct predicates actually
   * in use within statements in the {@link SPORelation}.
   *
   * @param keyOrder The selected index order.
   * @param knownTerms An array of term identifiers to be interpreted as bindings using the
   *     <i>keyOrder</i>.
   * @param termIdFilter An optional filter.
   * @return An iterator visiting the distinct term identifiers.
   * @todo add the ability to specify {@link IRangeQuery#PARALLEL} here for fast scans across
   *     multiple shards when chunk-wise order is Ok.
   */
  public IChunkedIterator<IV> distinctMultiTermScan(
      final IKeyOrder<ISPO> keyOrder, final IV[] knownTerms, final ITermIVFilter termIdFilter) {

    final int nterms = knownTerms.length;

    final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

    for (int i = 0; i < knownTerms.length; i++) {
      knownTerms[i].encode(keyBuilder);
    }

    final byte[] fromKey = knownTerms.length == 0 ? null : keyBuilder.getKey();

    final byte[] toKey = fromKey == null ? null : SuccessorUtil.successor(fromKey.clone());

    /*
     * Layer in the logic to advance to the tuple that will have the next
     * distinct term identifier in the first position of the key.
     */
    final DistinctMultiTermAdvancer filter = new DistinctMultiTermAdvancer(getKeyArity(), nterms);

    if (termIdFilter != null) {

      /*
       * Layer in a filter for only the desired term types.
       */

      filter.addFilter(
          new TupleFilter<SPO>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isValid(final ITuple<SPO> tuple) {

              final byte[] key = tuple.getKey();

              final int pos = knownTerms.length;

              final IV iv = IVUtility.decode(key, pos + 1)[pos];

              return termIdFilter.isValid(iv);
            }
          });
    }

    @SuppressWarnings("unchecked")
    final Iterator<IV> itr =
        new Striterator(
                getIndex(keyOrder)
                    .rangeIterator(
                        fromKey,
                        toKey,
                        0 /* capacity */,
                        IRangeQuery.KEYS | IRangeQuery.CURSOR,
                        filter))
            .addFilter(
                new Resolver() {

                  private static final long serialVersionUID = 1L;

                  /** Resolve SPO key to IV. */
                  @Override
                  protected IV resolve(Object obj) {

                    final byte[] key = ((ITuple) obj).getKey();

                    final int pos = knownTerms.length;

                    return IVUtility.decode(key, pos + 1)[pos];
                  }
                });

    return new ChunkedWrappedIterator<>(
        itr,
        ChunkedWrappedIterator.DEFAULT_CHUNK_SIZE, // chunkSize
        IV.class // elementClass
    );
  }

  //    /*
  //     * @todo This implementation was written early on and works for creating new
  //     *       SPOs licensed by inference against a triple store. It does not
  //     *       allow us to specify the statement type, which is always set to
  //     *       [inferred]. It also does not capture the context if one exists, but
  //     *       that could be done by inspection of the arity of the predicate. It
  //     *       might be better to have an explicit "CONSTRUCT" operator rather
  //     *       than having this implicit relationship between the head of a rule
  //     *       and the element created from that rule. For example, that might let
  //     *       us capture the distinction of inferred versus explicit within the
  //     *       CONSTRUCT operator.
  //     */
  //    public SPO newElement(final IPredicate<ISPO> predicate,
  //            final IBindingSet bindingSet) {
  //
  //        if (predicate == null)
  //            throw new IllegalArgumentException();
  //
  //        if (bindingSet == null)
  //            throw new IllegalArgumentException();
  //
  //        final IV s = (IV) predicate.asBound(0, bindingSet);
  //
  //        final IV p = (IV) predicate.asBound(1, bindingSet);
  //
  //        final IV o = (IV) predicate.asBound(2, bindingSet);
  //
  //        final SPO spo = new SPO(s, p, o, StatementEnum.Inferred);
  //
  //        if(log.isDebugEnabled())
  //            log.debug(spo.toString());
  //
  //        return spo;
  //
  //    }

  /*
   * @todo This works for creating new SPOs licensed by inference against a triple store. However,
   *     it does not allow us to specify the statement type, which is always set to [inferred]. It
   *     also does not capture the context if one exists, but that could be done by inspection of
   *     the arity of the predicate. It might be better to have an explicit "CONSTRUCT" operator
   *     rather than having this implicit relationship between the head of a rule and the element
   *     created from that rule. For example, that might let us capture the distinction of inferred
   *     versus explicit within the CONSTRUCT operator.
   */
  @SuppressWarnings("unchecked")
  public SPO newElement(final List<BOp> a, final IBindingSet bindingSet) {

    if (a == null) throw new IllegalArgumentException();

    if (bindingSet == null) throw new IllegalArgumentException();

    final IV s = (IV) ((IVariableOrConstant<?>) a.get(0)).get(bindingSet);

    final IV p = (IV) ((IVariableOrConstant<?>) a.get(1)).get(bindingSet);

    final IV o = (IV) ((IVariableOrConstant<?>) a.get(2)).get(bindingSet);

    final SPO spo = new SPO(s, p, o, StatementEnum.Inferred);

    if (log.isDebugEnabled()) log.debug(spo.toString());

    return spo;
  }

  public Class<ISPO> getElementClass() {

    return ISPO.class;
  }

  /*
   * Inserts {@link SPO}s, writing on the statement indices in parallel.
   *
   * <p>Note: This does NOT write on the justifications index. If justifications are being
   * maintained then the {@link ISolution}s MUST report binding sets and an {@link
   * InsertSolutionBuffer} MUST be used that knows how to write on the justifications index AND
   * delegate writes on the statement indices to this method.
   *
   * <p>Note: This does NOT assign statement identifiers. The {@link SPORelation} does not have
   * direct access to the {@link LexiconRelation} and the latter is responsible for assigning term
   * identifiers. Code that writes explicit statements onto the statement indices MUST use {@link
   * AbstractTripleStore#addStatements(AbstractTripleStore, boolean, IChunkedOrderedIterator,
   * IElementFilter)}, which knows how to generate the statement identifiers. In turn, that method
   * will delegate each "chunk" to this method.
   */
  public long insert(final IChunkedOrderedIterator<ISPO> itr) {

    try {

      long n = 0;

      while (itr.hasNext()) {

        final ISPO[] a = itr.nextChunk();

        n += insert(a, a.length, null /*filter*/);
      }

      return n;

    } finally {

      itr.close();
    }
  }

  /*
   * Deletes {@link SPO}s, writing on the statement indices in parallel.
   *
   * <p>Note: The {@link ISPO#isModified()} flag is set by this method.
   *
   * <p>Note: This does NOT write on the justifications index. If justifications are being
   * maintained then the {@link ISolution}s MUST report binding sets and an {@link
   * InsertSolutionBuffer} MUST be used that knows how to write on the justifications index AND
   * delegate writes on the statement indices to this method.
   *
   * <p>Note: This does NOT perform truth maintenance!
   *
   * <p>Note: This does NOT compute the closure for statement identifiers (statements that need to
   * be deleted because they are about a statement that is being deleted).
   *
   * @see AbstractTripleStore#removeStatements(IChunkedOrderedIterator, boolean)
   * @see SPOAccessPath#removeAll()
   */
  public long delete(final IChunkedOrderedIterator<ISPO> itr) {

    try {

      long n = 0;

      while (itr.hasNext()) {

        final ISPO[] a = itr.nextChunk();

        n += delete(a, a.length);
      }

      return n;

    } finally {

      itr.close();
    }
  }

  /*
   * Inserts {@link SPO}s, writing on the statement indices in parallel.
   *
   * <p>Note: The {@link ISPO#isModified()} flag is set by this method.
   *
   * <p>Note: This does NOT write on the justifications index. If justifications are being
   * maintained then the {@link ISolution}s MUST report binding sets and an {@link
   * InsertSolutionBuffer} MUST be used that knows how to write on the justifications index AND
   * delegate writes on the statement indices to this method.
   *
   * <p>Note: This does NOT perform truth maintenance!
   *
   * <p>Note: This does NOT compute the closure for statement identifiers (statements that need to
   * be deleted because they are about a statement that is being deleted).
   *
   * <p>Note: The statements are inserted into each index in parallel. We clone the statement[] and
   * sort and bulk load each statement index in parallel using a thread pool. All mutation to the
   * statement indices goes through this method.
   *
   * @param a An {@link ISPO}[] of the statements to be written onto the statement indices. For each
   *     {@link ISPO}, the {@link ISPO#isModified()} flag will be set iff the tuple corresponding to
   *     the statement was : (a) inserted; (b) updated (state change, such as to the {@link
   *     StatementEnum} value), or (c) removed.
   * @param numStmts The #of elements of that array that will be written.
   * @param filter An optional filter on the elements to be written.
   * @return The mutation count.
   */
  public long insert(final ISPO[] a, final int numStmts, final IElementFilter<ISPO> filter) {

    if (a == null) throw new IllegalArgumentException();

    if (numStmts > a.length) throw new IllegalArgumentException();

    if (numStmts == 0) return 0L;

    final long begin = System.currentTimeMillis();

    if (log.isDebugEnabled()) {

      log.debug("indexManager=" + getIndexManager());
    }

    // time to sort the statements.
    final AtomicLong sortTime = new AtomicLong(0);

    // time to generate the keys and load the statements into the
    // indices.
    final AtomicLong insertTime = new AtomicLong(0);

    final AtomicLong mutationCount = new AtomicLong(0);

    final List<Callable<Long>> tasks = new ArrayList<>(3);

    /*
     * When true, mutations on the primary index (SPO or SPOC) will be
     * reported. That metadata is used to set the isModified flag IFF the
     * tuple corresponding to the statement in the indices was (a) inserted;
     * (b) modified; or (c) removed.
     */
    final boolean reportMutation = true;

    if (keyArity == 3) {

      tasks.add(
          new SPOIndexWriter(
              this,
              a,
              numStmts,
              false /* clone */,
              SPOKeyOrder.SPO,
              SPOKeyOrder.SPO.isPrimaryIndex(),
              filter,
              sortTime,
              insertTime,
              mutationCount,
              reportMutation));

      if (!oneAccessPath) {

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.POS,
                SPOKeyOrder.POS.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /*reportMutation*/));

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.OSP,
                SPOKeyOrder.OSP.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /*reportMutation*/));
      }

    } else {

      tasks.add(
          new SPOIndexWriter(
              this,
              a,
              numStmts,
              false /* clone */,
              SPOKeyOrder.SPOC,
              SPOKeyOrder.SPOC.isPrimaryIndex(),
              filter,
              sortTime,
              insertTime,
              mutationCount,
              reportMutation));

      if (!oneAccessPath) {

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.POCS,
                SPOKeyOrder.POCS.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.OCSP,
                SPOKeyOrder.OCSP.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.CSPO,
                SPOKeyOrder.CSPO.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.PCSO,
                SPOKeyOrder.PCSO.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexWriter(
                this,
                a,
                numStmts,
                true /* clone */,
                SPOKeyOrder.SOPC,
                SPOKeyOrder.SOPC.isPrimaryIndex(),
                filter,
                sortTime,
                insertTime,
                mutationCount,
                false /* reportMutation */));
      }
    }

    // if(numStmts>1000) {

    // log.info("Writing " + numStmts + " statements...");
    //
    // }

    final List<Future<Long>> futures;
    /*
            final long elapsed_SPO;
            final long elapsed_POS;
            final long elapsed_OSP;
    */
    try {

      futures = getExecutorService().invokeAll(tasks);

      for (int i = 0; i < tasks.size(); i++) {

        //                futures.get(i).get();
        logFuture(futures.get(i));
      }
      /*
                  elapsed_SPO = futures.get(0).get();
                  if (!oneAccessPath) {
                      elapsed_POS = futures.get(1).get();
                      elapsed_OSP = futures.get(2).get();
                  } else {
                      elapsed_POS = 0;
                      elapsed_OSP = 0;
                  }
      */
    } catch (InterruptedException ex) {

      throw new RuntimeException(ex);

    } catch (ExecutionException ex) {

      throw new RuntimeException(ex);
    }

    final long elapsed = System.currentTimeMillis() - begin;

    if (log.isInfoEnabled() && numStmts > 1000) {

      log.info(
          "Wrote "
              + numStmts
              + " statements (mutationCount="
              + mutationCount
              + ") in "
              + elapsed
              + "ms"
              + "; sort="
              + sortTime
              + "ms"
              + ", keyGen+insert="
              + insertTime
              + "ms"
          //                    + "; spo=" + elapsed_SPO + "ms"
          //                    + ", pos=" + elapsed_POS + "ms"
          //                    + ", osp=" + elapsed_OSP + "ms"
          );
    }

    return mutationCount.get();
  }

  private <T> T logFuture(final Future<T> f) throws ExecutionException, InterruptedException {
    try {
      return f.get();
    } catch (InterruptedException e) {
      log.warn(e, e);
      throw e;
    } catch (ExecutionException e) {
      log.error(e, e);
      throw e;
    }
  }

  /*
   * Delete the {@link SPO}s from the statement indices. Any justifications for those statements
   * will also be deleted. The {@link ISPO#isModified()} flag is set by this method if the {@link
   * ISPO} was pre-existing in the database and was therefore deleted by this operation.
   *
   * @param stmts The {@link SPO}s.
   * @param numStmts The #of elements in that array to be processed.
   * @return The #of statements that were removed (mutationCount).
   */
  public long delete(final ISPO[] stmts, final int numStmts) {

    if (stmts == null) throw new IllegalArgumentException();

    if (numStmts < 0 || numStmts > stmts.length) throw new IllegalArgumentException();

    if (numStmts == 0) return 0L;

    final long begin = System.currentTimeMillis();

    /*
     * Downgrade statements to StatementEnum.History instead of actually
     * removing them.
     */
    if (history) {

      final ISPO[] a = new ISPO[numStmts];
      for (int i = 0; i < numStmts; i++) {
        a[i] = new SPO(stmts[i].s(), stmts[i].p(), stmts[i].o(), StatementEnum.History);
      }

      final long nmodified = insert(a, numStmts, null);

      for (int i = 0; i < numStmts; i++) {
        stmts[i].setModified(a[i].getModified());
      }

      return nmodified;
    }

    // The time to sort the data.
    final AtomicLong sortTime = new AtomicLong(0);

    // The time to delete the statements from the indices.
    final AtomicLong writeTime = new AtomicLong(0);

    // The mutation count.
    final AtomicLong mutationCount = new AtomicLong(0);

    final List<Callable<Long>> tasks = new ArrayList<>(3);

    /*
     * When true, mutations on the primary index (SPO or SPOC) will be
     * reported. That metadata is used to set the isModified flag IFF the
     * tuple corresponding to the statement in the indices was (a) inserted;
     * (b) modified; or (c) removed.
     */
    final boolean reportMutation = true;

    if (keyArity == 3) {

      tasks.add(
          new SPOIndexRemover(
              this,
              stmts,
              numStmts,
              SPOKeyOrder.SPO,
              SPOKeyOrder.SPO.isPrimaryIndex(),
              false /* clone */,
              sortTime,
              writeTime,
              mutationCount,
              reportMutation));

      if (!oneAccessPath) {

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.POS,
                SPOKeyOrder.POS.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.OSP,
                SPOKeyOrder.OSP.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));
      }

    } else {

      tasks.add(
          new SPOIndexRemover(
              this,
              stmts,
              numStmts,
              SPOKeyOrder.SPOC,
              SPOKeyOrder.SPOC.isPrimaryIndex(),
              false /* clone */,
              sortTime,
              writeTime,
              mutationCount,
              reportMutation));

      if (!oneAccessPath) {

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.POCS,
                SPOKeyOrder.POCS.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.OCSP,
                SPOKeyOrder.OCSP.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.CSPO,
                SPOKeyOrder.CSPO.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.PCSO,
                SPOKeyOrder.PCSO.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));

        tasks.add(
            new SPOIndexRemover(
                this,
                stmts,
                numStmts,
                SPOKeyOrder.SOPC,
                SPOKeyOrder.SOPC.isPrimaryIndex(),
                true /* clone */,
                sortTime,
                writeTime,
                mutationCount,
                false /* reportMutation */));
      }
    }

    if (justify) {

      /*
       * Also retract the justifications for the statements.
       */

      tasks.add(
          new JustificationRemover(this, stmts, numStmts, true /* clone */, sortTime, writeTime));
    }

    final List<Future<Long>> futures;
    /*
    final long elapsed_SPO;
    final long elapsed_POS;
    final long elapsed_OSP;
    final long elapsed_JST;
    */

    try {

      futures = getExecutorService().invokeAll(tasks);

      for (int i = 0; i < tasks.size(); i++) {

        futures.get(i).get();
      }
      /*
                  elapsed_SPO = futures.get(0).get();

                  if (!oneAccessPath) {

                      elapsed_POS = futures.get(1).get();

                      elapsed_OSP = futures.get(2).get();

                  } else {

                      elapsed_POS = 0;

                      elapsed_OSP = 0;

                  }

                  if (justify) {

                      elapsed_JST = futures.get(3).get();

                  } else {

                      elapsed_JST = 0;

                  }
      */
    } catch (InterruptedException ex) {

      throw new RuntimeException(ex);

    } catch (ExecutionException ex) {

      throw new RuntimeException(ex);
    }

    final long elapsed = System.currentTimeMillis() - begin;

    if (log.isInfoEnabled() && numStmts > 1000) {

      log.info(
          "Removed "
              + numStmts
              + " in "
              + elapsed
              + "ms; sort="
              + sortTime
              + "ms, keyGen+delete="
              + writeTime
              + "ms"
          //                    + "; spo=" + elapsed_SPO + "ms, pos="
          //                    + elapsed_POS + "ms, osp=" + elapsed_OSP
          //                    + "ms, jst=" + elapsed_JST
          );
    }

    return mutationCount.get();
  }

  /*
   * Adds justifications to the store.
   *
   * @param itr The iterator from which we will read the {@link Justification}s to be added. The
   *     iterator is closed by this operation.
   * @return The #of {@link Justification}s written on the justifications index.
   * @todo a lot of the cost of loading data is writing the justifications. SLD/magic sets will
   *     relieve us of the need to write the justifications since we can efficiently prove whether
   *     or not the statements being removed can be entailed from the remaining statements. Any
   *     statement which can still be proven is converted to an inference. Since writing the
   *     justification chains is such a source of latency, SLD/magic sets will translate into an
   *     immediate performance boost for data load.
   */
  public long addJustifications(final IChunkedIterator<Justification> itr) {

    try {

      if (!itr.hasNext()) return 0;

      final long begin = System.currentTimeMillis();

      //            /*
      //             * Note: This capacity estimate is based on N longs per SPO, one
      //             * head, and 2-3 SPOs in the tail. The capacity will be extended
      //             * automatically if necessary.
      //             */
      //
      //            final KeyBuilder keyBuilder = new KeyBuilder(IRawTripleStore.N
      //                    * (1 + 3) * Bytes.SIZEOF_LONG);

      long nwritten = 0;

      final IIndex ndx = getJustificationIndex();

      final JustificationTupleSerializer tupleSer =
          (JustificationTupleSerializer) ndx.getIndexMetadata().getTupleSerializer();

      while (itr.hasNext()) {

        final Justification[] a = itr.nextChunk();

        final int n = a.length;

        // sort into their natural order.
        Arrays.sort(a);

        final byte[][] keys = new byte[n][];

        for (int i = 0; i < n; i++) {

          //                    final Justification jst = a[i];

          keys[i] = tupleSer.serializeKey(a[i]); // jst.getKey(keyBuilder);
        }

        /*
         * sort into their natural order.
         *
         * @todo is it faster to sort the Justification[] or the keys[]?
         * See above for the alternative.
         */
        // Arrays.sort(keys,UnsignedByteArrayComparator.INSTANCE);

        final LongAggregator aggregator = new LongAggregator();

        ndx.submit(
            0 /* fromIndex */,
            n /* toIndex */,
            keys,
            null /* vals */,
            WriteJustificationsProcConstructor.INSTANCE,
            aggregator);

        nwritten += aggregator.getResult();
      }

      final long elapsed = System.currentTimeMillis() - begin;

      if (log.isInfoEnabled())
        log.info("Wrote " + nwritten + " justifications in " + elapsed + " ms");

      return nwritten;

    } finally {

      itr.close();
    }
  }

  /** Dumps the specified index. */
  //    @SuppressWarnings("unchecked")
  public StringBuilder dump(final IKeyOrder<ISPO> keyOrder) {

    final StringBuilder sb = new StringBuilder();

    final IPredicate<ISPO> pred =
        new SPOPredicate(
            keyArity == 4
                ? new BOp[] {
                  Var.var("s"), Var.var("p"), Var.var("o"), Var.var("c"),
                }
                : new BOp[] {
                  Var.var("s"), Var.var("p"), Var.var("o"),
                },
            NV.asMap(
                new NV(IPredicate.Annotations.RELATION_NAME, new String[] {getNamespace()})
                //                        new NV(IPredicate.Annotations.KEY_ORDER,
                //                                keyOrder),
                ));
    //        final IPredicate<ISPO> pred = new SPOPredicate(
    //                new String[] { getNamespace() }, -1, // partitionId
    //                Var.var("s"),
    //                Var.var("p"),
    //                Var.var("o"),
    //                keyArity == 3 ? null : Var.var("c"),
    //                false, // optional
    //                null, // filter,
    //                null // expander
    //        );

    final IChunkedOrderedIterator<ISPO> itr = getAccessPath(keyOrder, pred).iterator();

    try {

      while (itr.hasNext()) {

        sb.append(itr.next());

        sb.append("\n");
      }

    } finally {

      itr.close();
    }

    return sb;
  }

  /*
   * Checks whether one of the associated triple indices uses delete markers.
   *
   * @return true if some index uses delete markers
   */
  public boolean indicesHaveDeleteMarkers() {
    /*
     * actually, either none of the triple indices or all of them uses delete markers, so it
     * suffices to probe the primary index
     */
    return getPrimaryIndex().getIndexMetadata().getDeleteMarkers();
  }

  /*
   * The {@link ILexiconConfiguration} instance, which will determine how terms are encoded and
   * decoded in the key space. private ILexiconConfiguration lexiconConfiguration;
   */

  /*
   * See {@link ILexiconConfiguration#isInline(DTE)}. Delegates to the {@link #lexiconConfiguration}
   * instance. public boolean isInline(DTE dte) { return lexiconConfiguration.isInline(dte); }
   */

  /*
   * See {@link ILexiconConfiguration#isLegacyEncoding()}. Delegates to the {@link
   * #lexiconConfiguration} instance. public boolean isLegacyEncoding() { return
   * lexiconConfiguration.isLegacyEncoding(); }
   */

  /*
   * Return the {@link #lexiconConfiguration} instance. Used to determine how to encode and decode
   * terms in the key space. public ILexiconConfiguration getLexiconConfiguration() { return
   * lexiconConfiguration; }
   */
}
