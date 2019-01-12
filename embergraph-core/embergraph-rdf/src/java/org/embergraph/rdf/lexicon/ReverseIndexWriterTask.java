package org.embergraph.rdf.lexicon;

import java.util.concurrent.Callable;
import org.embergraph.btree.IIndex;
import org.embergraph.btree.keys.IKeyBuilder;
import org.embergraph.btree.keys.KVO;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.io.ByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;
import org.embergraph.rdf.lexicon.Id2TermWriteProc.Id2TermWriteProcConstructor;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueSerializer;
import org.embergraph.rdf.spo.ISPO;
import org.openrdf.model.BNode;
import org.openrdf.model.Value;

/*
* Add terms to the reverse index, which is the index that we use to lookup the RDF value by its
 * term identifier so that we can externalize {@link ISPO}s as RDF/XML or the like.
 *
 * <p>Note: Every term asserted against the forward mapping [terms] MUST be asserted against the
 * reverse mapping [ids] EVERY time. This is required in order to guarantee that the reverse index
 * remains complete and consistent. Otherwise a client that writes on the terms index and fails
 * before writing on the ids index would cause those terms to remain undefined in the reverse index.
 */
public class ReverseIndexWriterTask implements Callable<Long> {

  private final IIndex idTermIndex;

  private final EmbergraphValueSerializer<EmbergraphValue> ser;

  private final KVO<EmbergraphValue>[] a;

  private final int ndistinct;

  private final boolean storeBlankNodes;

  /*
   * @param idTermIndex The index on which to write the data.
   * @param valueFactory This determines how the {@link Value} objects are serialized on the index.
   * @param a The terms (in sorted order by their term identifiers).
   * @param ndistinct The #of elements in <i>a</i>.
   */
  public ReverseIndexWriterTask(
      final IIndex idTermIndex,
      final EmbergraphValueFactory valueFactory,
      final KVO<EmbergraphValue>[] a,
      final int ndistinct,
      final boolean storeBlankNodes) {

    if (idTermIndex == null) throw new IllegalArgumentException();

    if (valueFactory == null) throw new IllegalArgumentException();

    if (a == null) throw new IllegalArgumentException();

    if (ndistinct < 0 || ndistinct > a.length) throw new IllegalArgumentException();

    this.idTermIndex = idTermIndex;

    this.ser = valueFactory.getValueSerializer();

    this.a = a;

    this.ndistinct = ndistinct;

    this.storeBlankNodes = storeBlankNodes;
  }

  /** @return the elapsed time for this task. */
  public Long call() throws Exception {

    final long _begin = System.currentTimeMillis();

    /*
     * Create a key buffer to hold the keys generated from the term
     * identifiers and then generate those keys.
     *
     * Note: We DO NOT write BNodes on the reverse index.
     */
    final byte[][] keys = new byte[ndistinct][];
    final byte[][] vals = new byte[ndistinct][];
    int nonBNodeCount = 0; // #of non-bnodes.
    {

      // thread-local key builder removes single-threaded constraint.
      final IKeyBuilder keyBuilder = KeyBuilder.newInstance();

      final int initialCapacity = 128;

      // buffer is reused for each serialized term.
      final DataOutputBuffer out = new DataOutputBuffer(initialCapacity);

      // buffer is reused for each serialized term.
      final ByteArrayBuffer tmp = new ByteArrayBuffer(initialCapacity);

      for (int i = 0; i < ndistinct; i++) {

        final EmbergraphValue x = a[i].obj;

        if (!storeBlankNodes && x instanceof BNode) {

          // Blank nodes are not entered into the reverse index.
          continue;
        }

        keys[nonBNodeCount] = x.getIV().encode(keyBuilder.reset()).getKey();

        // Serialize the term.
        vals[nonBNodeCount] = ser.serialize(x, out.reset(), tmp);

        nonBNodeCount++;
      }
    }

    // run the procedure on the index.
    if (nonBNodeCount > 0) {

      idTermIndex.submit(
          0 /* fromIndex */,
          nonBNodeCount /* toIndex */,
          keys,
          vals,
          Id2TermWriteProcConstructor.INSTANCE,
          null /* resultHandler */);
    }

    return System.currentTimeMillis() - _begin;
  }
}
