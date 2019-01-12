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
 * Created on Mar 30, 2005
 */
package org.embergraph.rdf.axioms;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.io.LongPacker;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.model.EmbergraphStatement;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.StatementEnum;
import org.embergraph.rdf.rio.StatementBuffer;
import org.embergraph.rdf.spo.ISPO;
import org.embergraph.rdf.spo.SPO;
import org.embergraph.rdf.spo.SPOKeyOrder;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.openrdf.model.Value;

/*
* A collection of axioms.
 *
 * <p>Axioms are generated by {@link AbstractTripleStore#create()} based on its configured
 * properties. While the implementation class declares axioms in terms of RDF {@link Value}s, the
 * {@link BaseAxioms} only retains the set of {s:p:o} tuples for the term identifiers corresponding
 * those those {@link Value}s. That {s:p:o} tuple array is the serialized state of this class. When
 * an {@link AbstractTripleStore} is reopened, the axioms are de-serialized from a property in the
 * global row store.
 *
 * @author personickm
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class BaseAxioms implements Axioms, Externalizable {

  //    /*
//     * The axioms in SPO order.
  //     */
  //    private transient BTree btree;

  /** The axioms. */
  private Set<SPO> axioms;

  //    /*
//     * Used to format keys for that {@link BTree}.
  //     */
  //    private transient SPOTupleSerializer tupleSer;

  //    /*
//     * Non-<code>null</code> iff the ctor specifies this value.
  //     */
  //    private final transient AbstractTripleStore db;

  /*
   * The namespace of the associated kb instance. This is used to materialize the appropriate {@link
   * EmbergraphValueFactory}.
   */
  private String namespace;

  @Override
  public final String getNamespace() {

    return namespace;
  }

  /*
   * The value factory to be used when creating axioms.
   *
   * @throws IllegalStateException unless the ctor variant was used that specifies the database.
   */
  protected final EmbergraphValueFactory getValueFactory() {

    return EmbergraphValueFactoryImpl.getInstance(namespace);
    //        return db.getValueFactory();

  }

  /** De-serialization constructor. */
  protected BaseAxioms() {

    //        db = null;

  }

  /*
   * Ctor variant used by {@link AbstractTripleStore#create()}.
   *
   * <p>Note: When de-serializing a {@link BaseAxioms} object the zero-arg ctor will be used.
   *
   * @param namespace The namespace for the {@link AbstractTripleStore} instance.
   */
  protected BaseAxioms(final String namespace) {

    if (namespace == null) throw new IllegalArgumentException();

    //        this.db = db;
    this.namespace = namespace;
  }

  /*
   * Uses {@link #addAxioms(Collection)} to collect the declared axioms and then writes the axioms
   * onto the database specified to the {@link BaseAxioms#BaseAxioms(AbstractTripleStore)} ctor.
   *
   * @throws IllegalStateException if that ctor was not used.
   */
  public final void init(final AbstractTripleStore db) {

    // setup [axioms] collection.
    final Set<EmbergraphStatement> axioms = new LinkedHashSet<EmbergraphStatement>(200);

    // obtain collection of axioms to be used.
    addAxioms(axioms);

    this.axioms = writeAxioms(db, axioms);
  }

  /*
   * Adds all axioms declared by this class into <i>axioms</i>.
   *
   * <p>Note: Subclasses MUST extend this method to add their axioms into the <i>axioms</i>
   * collection.
   *
   * @param axioms A collection into which the axioms will be inserted.
   * @throws IllegalArgumentException if the parameter is <code>null</code>.
   */
  protected void addAxioms(final Collection<EmbergraphStatement> axioms) {

    if (axioms == null) throw new IllegalArgumentException();

    // NOP.

  }

  /*
   * Writes the axioms on the database, resolving {@link EmbergraphStatement}s to {@link SPO}s.
   *
   * @return The axioms expressed as {@link SPO}s.
   */
  private Set<SPO> writeAxioms(
      final AbstractTripleStore db, final Collection<EmbergraphStatement> axioms) {

    if (db == null) throw new IllegalArgumentException();

    if (axioms == null) throw new IllegalArgumentException();

    final int naxioms = axioms.size();

    final LinkedHashSet<SPO> ret = new LinkedHashSet<SPO>(naxioms);

    if (naxioms > 0) {

      // Note: min capacity of one handles case with no axioms.
      final int capacity = Math.max(1, naxioms);

      final MyStatementBuffer buffer = new MyStatementBuffer(db, capacity);

      //            final StatementBuffer<SPO> buffer = new StatementBuffer<SPO>(db, capacity);
      //
      //			final IChangeLog changeLog = new IChangeLog() {
      //
      //				@Override
      //				public void changeEvent(final IChangeRecord record) {
      //
      //					final ISPO tmp = record.getStatement();
      //
      //					final SPO spo = new SPO(tmp.s(), tmp.p(), tmp.o());
      //
      //					ret.add( spo );
      //
      //				}
      //
      //				@Override
      //				public void transactionBegin() {
      //				}
      //
      //				@Override
      //				public void transactionPrepare() {
      //				}
      //
      //				@Override
      //				public void transactionCommited(long commitTime) {
      //				}
      //
      //				@Override
      //				public void transactionAborted() {
      //				}
      //
      //				@Override
      //				public void close() {
      //				}
      //			};
      //
      //			buffer.setChangeLog(changeLog);

      final Iterator<EmbergraphStatement> itr = axioms.iterator();

      while (itr.hasNext()) {

        final EmbergraphStatement triple = itr.next();

        assert triple.getStatementType() == StatementEnum.Axiom;

        buffer.add(triple);
      }

      // write on the database.
      buffer.flush();

      // SPO[] exposed by our StatementBuffer subclass.
      final SPO[] stmts = buffer.stmts;

      for (SPO spo : stmts) {

        ret.add(spo);
      }
    }

    // The axioms as SPO objects.
    return ret;
  }

  //    /*
//     * Create the {@link BTree} to hold the axioms.
  //     *
  //     * @param naxioms
  //     *            The #of axioms (used to tune the branching factor).
  //     *
  //     * @throws IllegalStateException
  //     *             if the {@link #btree} exists.
  //     */
  //    private void createBTree(final int naxioms) {
  //
  //        if (btree != null)
  //            throw new IllegalStateException();
  //
  //        // exact fill of the root leaf.
  //        final int branchingFactor = Math.max(Options.MIN_BRANCHING_FACTOR, naxioms );
  //
  //        /*
  //         * Note: This uses a SimpleMemoryRawStore since we never explicitly
  //         * close the BaseAxioms class. Also, all data should be fully
  //         * buffered in the leaf of the btree so the btree will never touch
  //         * the store after it has been populated.
  //         */
  //        final IndexMetadata metadata = new IndexMetadata(UUID.randomUUID());
  //
  //        metadata.setBranchingFactor(branchingFactor);
  //
  //        tupleSer = new SPOTupleSerializer(SPOKeyOrder.SPO, false/* sids */);
  //
  //        metadata.setTupleSerializer(tupleSer);
  //
  //        btree = BTree.createTransient(metadata);
  ////        btree = BTree.create(new SimpleMemoryRawStore(), metadata);
  //
  //    }

  //    /*
//     * Builds an internal B+Tree that is used to quickly identify axioms based
  //     * on {s:p:o} term identifier tuples, and returns the distinct {s:p:o} term
  //     * identifier tuples for the declared axioms.
  //     * <p>
  //     * Note: if the terms for the axioms are already in the lexicon and the
  //     * axioms are already in the database then this will not write on the
  //     * database, but it will still result in the SPO[] containing the axioms to
  //     * be defined in {@link MyStatementBuffer}.
  //     *
  //     * @param axioms
  //     */
  //    private void buildBTree(final Collection<EmbergraphStatement> axioms) {
  //
  //        /*
  //         * Fill the btree with the axioms in SPO order.
  //         *
  //         * Note: This should ALWAYS use the SPO key order even for quads since
  //         * we just want to test on the (s,p,o).
  //         *
  //         * @todo This would be MUCH faster with a hashmap on the SPOs.
  //         *
  //         * @todo There is no need to put the statement type into the in-memory
  //         * axioms. they are axioms after all. That is, we could just have the
  //         * keys and no values.
  //         */
  //
  //        final int naxioms = axioms.size();
  //
  //        createBTree(naxioms/* naxioms */);
  //
  //        for (EmbergraphStatement spo : axioms) {
  //
  //            btree.insert(tupleSer.serializeKey(spo), spo.getStatementType()
  //                    .serialize());
  //
  //        }
  //
  //    }

  //    /*
//     * The initial version. The s, p, and o of each axiom were written out as
  //     * <code>long</code> integers.
  //     */
  //    private static final transient byte VERSION0 = 0;

  /*
   * The serialization format was changed when we introduced the TERMS index (as opposed to the
   * TERM2ID and ID2TERM index). Up to that point, the s, p, and o components were always <code>long
   * </code> termIds assigned by the TERM2ID index. However, the refactor which introduced the TERMS
   * index generalized the {@link IV}s further such that we can no longer rely on the <code>long
   * </code> termId encoding. Therefore, the serialization of the axioms was changed to the length
   * of each {@link SPOKeyOrder#SPO} index key followed by the <code>byte[]</code> comprising that
   * key. This has the effect of using the {@link IV} representation directly within the
   * serialization of the axioms.
   *
   * <p>Note: In the initial version, the s, p, and o of each axiom were written out as <code>long
   * </code> integers. That version is no longer supported.
   *
   * <p>This version also includes the <em>namespace</em> so we can obtain the appropriate {@link
   * EmbergraphValueFactory} instance without requiring a reference to the {@link
   * AbstractTripleStore}.
   */
  private static final transient byte VERSION1 = 1;

  /** The current version. */
  private static final transient byte currentVersion = VERSION1;

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {

    final byte version = in.readByte();

    switch (version) {
        //        case VERSION0:
        //            readVersion0(in);
        //            break;
      case VERSION1:
        readVersion1(in);
        break;
      default:
        throw new UnsupportedOperationException("Unknown version: " + version);
    }
  }

  //    @SuppressWarnings("unchecked")
  //    private void readVersion0(final ObjectInput in) throws IOException {
  //
  //        final long naxioms = LongPacker.unpackLong(in);
  //
  //        if (naxioms < 0 || naxioms > Integer.MAX_VALUE)
  //            throw new IOException();
  //
  //        createBTree((int) naxioms);
  //
  //        for (int i = 0; i < naxioms; i++) {
  //
  //            final IV s = new TermId<EmbergraphURI>(VTE.URI, in.readLong());
  //
  //            final IV p = new TermId<EmbergraphURI>(VTE.URI, in.readLong());
  //
  //            final IV o = new TermId<EmbergraphURI>(VTE.URI, in.readLong());
  //
  //            final SPO spo = new SPO(s, p, o, StatementEnum.Axiom);
  //
  //            btree.insert(tupleSer.serializeKey(spo), spo.getStatementType()
  //                    .serialize());
  //
  //        }
  //    }

  private void readVersion1(final ObjectInput in) throws IOException {

    namespace = in.readUTF();

    final int naxioms = LongPacker.unpackInt(in);

    //        if (naxioms < 0 || naxioms > Integer.MAX_VALUE)
    //            throw new IOException();

    axioms = new LinkedHashSet<SPO>(naxioms);
    //        createBTree((int) naxioms);

    for (int i = 0; i < naxioms; i++) {

      final int nbytes = LongPacker.unpackInt(in);

      final byte[] key = new byte[nbytes];

      in.readFully(key);

      final IV[] ivs = IVUtility.decodeAll(key);

      if (ivs.length != 3) throw new IOException("Expecting 3 IVs, not: " + Arrays.toString(ivs));

      final IV s = ivs[0];

      final IV p = ivs[1];

      final IV o = ivs[2];

      final SPO spo = new SPO(s, p, o, StatementEnum.Axiom);

      //            btree.insert(tupleSer.serializeKey(spo), spo.getStatementType()
      //                    .serialize());
      axioms.add(spo);
    }
  }

  public void writeExternal(final ObjectOutput out) throws IOException {

    //        if (btree == null)
    //            throw new IllegalStateException();

    out.writeByte(currentVersion);

    switch (currentVersion) {
        //        case VERSION0: writeVersion0(out); break;
      case VERSION1:
        writeVersion1(out);
        break;
      default:
        throw new AssertionError();
    }
  }

  //    private void writeVersion0(final ObjectOutput out) throws IOException {
  //
  //        final long naxioms = btree.rangeCount();
  //
  //        LongPacker.packLong(out, naxioms);
  //
  //        @SuppressWarnings("unchecked")
  //        final ITupleIterator<SPO> itr = btree.rangeIterator();
  //
  //        while (itr.hasNext()) {
  //
  //            final SPO spo = itr.next().getObject();
  //
  //            out.writeLong(spo.s().getTermId());
  //
  //            out.writeLong(spo.p().getTermId());
  //
  //            out.writeLong(spo.o().getTermId());
  //
  //        }
  //
  //    }

  private void writeVersion1(final ObjectOutput out) throws IOException {

    out.writeUTF(namespace);

    LongPacker.packLong(out, axioms.size());

    final IKeyBuilder keyBuilder = new KeyBuilder(24 /*initialCapacity*/);

    for (ISPO spo : axioms) {

      final byte[] key = SPOKeyOrder.SPO.encodeKey(keyBuilder, spo);

      if (true) {

        final IV[] ivs = IVUtility.decodeAll(key);

        if (ivs.length != 3)
          throw new IOException("Expecting 3 IVs, not: " + Arrays.toString(ivs) + " for " + spo);

        final IV s = ivs[0];

        final IV p = ivs[1];

        final IV o = ivs[2];

        final SPO spo2 = new SPO(s, p, o, StatementEnum.Axiom);
        if (!spo.equals(spo2)) throw new IOException("Expecting: " + spo + ", not " + spo2);
      }

      LongPacker.packLong(out, key.length);

      out.write(key);
    }
  }

  public final boolean isAxiom(final IV s, final IV p, final IV o) {

    if (axioms == null) throw new IllegalStateException();

    // fast rejection.
    if (s == null || p == null || o == null) {

      return false;
    }

    final SPO spo = new SPO(s, p, o, StatementEnum.Axiom);

    return axioms.contains(spo);

  }

  @Override
  public final int size() {

    if (axioms == null) throw new IllegalStateException();

    return axioms.size();
  }

  @Override
  public final Iterator<SPO> axioms() {

    if (axioms == null) throw new IllegalStateException();

    return Collections.unmodifiableSet(axioms).iterator();
  }

  /*
   * Helper class.
   *
   * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
   */
  private static class MyStatementBuffer extends StatementBuffer<SPO>
      implements StatementBuffer.IWrittenSPOArray {

    /** An array of the axioms in SPO order. */
    private SPO[] stmts;

    /*
     * @param database
     * @param capacity
     */
    public MyStatementBuffer(final AbstractTripleStore database, final int capacity) {

      super(database, capacity);

      didWriteCallback = this;
    }

    /*
     * Overridden to save off a copy of the axioms in SPO order on {@link #stmts} where we can
     * access them afterwards.
     */
    @Override
    public void didWriteSPOs(final SPO[] stmts, final int numStmts) {

      if (this.stmts == null) {

        this.stmts = new SPO[numStmts];

        System.arraycopy(stmts, 0, this.stmts, 0, numStmts);

        Arrays.sort(this.stmts, SPOKeyOrder.SPO.getComparator());
      }

      //            super.didWriteSPOs(stmts, numStmts);

    }
  }
}
