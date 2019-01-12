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
 * Created on Jul 7, 2008
 */

package org.embergraph.rdf.lexicon;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.embergraph.btree.DefaultTupleSerializer;
import org.embergraph.btree.ITuple;
import org.embergraph.btree.keys.ASCIIKeyBuilderFactory;
import org.embergraph.btree.keys.IKeyBuilderFactory;
import org.embergraph.btree.raba.codec.IRabaCoder;
import org.embergraph.btree.raba.codec.SimpleRabaCoder;
import org.embergraph.io.ByteArrayBuffer;
import org.embergraph.io.DataOutputBuffer;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.IVUtility;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.model.EmbergraphValueFactoryImpl;
import org.embergraph.rdf.model.EmbergraphValueSerializer;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.util.Bytes;
import org.openrdf.model.Value;

/*
* Encapsulates key and value formation for the reverse lexicon index.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class Id2TermTupleSerializer extends DefaultTupleSerializer<IV, EmbergraphValue> {

  /** */
  private static final long serialVersionUID = 4841769875819006615L;

  /** The namespace of the owning {@link LexiconRelation}. */
  private String namespace;

  /*
   * A (de-)serialized backed by a {@link EmbergraphValueFactoryImpl} for the {@link #namespace} of
   * the owning {@link LexiconRelation}.
   */
  private transient EmbergraphValueSerializer<EmbergraphValue> valueSer;

  private static final transient int INITIAL_CAPACITY = 128;

  /*
   * Used to serialize RDF {@link Value}s.
   *
   * <p>Note: While this object is not thread-safe, the mutable B+Tree is restricted to a single
   * writer so it does not have to be thread-safe.
   */
  private final transient DataOutputBuffer buf = new DataOutputBuffer(INITIAL_CAPACITY);

  /*
   * Used to serialize RDF {@link Value}s.
   *
   * <p>Note: While this object is not thread-safe, the mutable B+Tree is restricted to a single
   * writer so it does not have to be thread-safe.
   */
  private final transient ByteArrayBuffer tbuf = new ByteArrayBuffer(INITIAL_CAPACITY);

  private transient EmbergraphValueFactory valueFactory;

  /** De-serialization ctor. */
  public Id2TermTupleSerializer() {

    super();
  }

  /*
   * @param keyBuilderFactory A factory that does not support unicode and has an initialCapacity of
   *     {@value Bytes#SIZEOF_LONG}.
   */
  public Id2TermTupleSerializer(final String namespace, final EmbergraphValueFactory valueFactory) {

    this(
        namespace,
        valueFactory,
        // Note: TermId is now 9 bytes (flags + tid)
        new ASCIIKeyBuilderFactory(Bytes.SIZEOF_LONG + 1),
        getDefaultLeafKeysCoder(),
        //                getDefaultValuesCoder() // Canonical Huffman coding.
        SimpleRabaCoder.INSTANCE // Much faster
        );
  }

  public Id2TermTupleSerializer(
      final String namespace,
      final EmbergraphValueFactory valueFactory,
      final IKeyBuilderFactory keyBuilderFactory,
      final IRabaCoder leafKeysCoder,
      final IRabaCoder leafValsCoder) {

    super(keyBuilderFactory, leafKeysCoder, leafValsCoder);

    if (namespace == null) throw new IllegalArgumentException();

    this.namespace = namespace;
    this.valueFactory = valueFactory;
    this.valueSer = this.valueFactory.getValueSerializer();
  }

  /*
   * Generates an unsigned byte[] key from a {@link TermId}.
   *
   * <p>Note: The code that handles efficient batch insertion of terms into the database replicates
   * the logic for encoding the term identifier as an unsigned long integer.
   *
   * @param id The term identifier.
   * @return The id expressed as an unsigned byte[] key of length 8.
   * @see #key2Id()
   */
  public byte[] id2key(final TermId<?> tid) {

    return tid.encode(getKeyBuilder().reset()).getKey();
  }

  /*
   * Decodes the term identifier key to a term identifier.
   *
   * @param key The key for an entry in the id:term index.
   * @return The term identifier.
   */
  public IV deserializeKey(final ITuple tuple) {

    final byte[] key = tuple.getKeyBuffer().array();

    return IVUtility.decode(key);
  }

  /*
   * Return the unsigned byte[] key for a term identifier.
   *
   * @param obj The term identifier as a {@link TermId}.
   */
  public byte[] serializeKey(final Object obj) {

    return id2key((TermId<?>) obj);
  }

  /*
   * Return the <code>byte[]</code> value, which is the serialization of an RDF {@link Value}.
   *
   * @param obj An RDF {@link Value}.
   */
  public byte[] serializeVal(final EmbergraphValue obj) {

    return valueSer.serialize(obj, buf.reset(), tbuf);
  }

  /*
   * De-serializes the {@link ITuple} as a {@link EmbergraphValue}, including the term identifier
   * extracted from the unsigned byte[] key, and sets the appropriate {@link
   * EmbergraphValueFactoryImpl} reference on that object.
   */
  public EmbergraphValue deserialize(final ITuple tuple) {

    final IV<?, ?> iv = deserializeKey(tuple);

    final EmbergraphValue tmp = valueSer.deserialize(tuple.getValueStream(), new StringBuilder());

    tmp.setIV(iv);

    return tmp;
  }

  /*
   *
   *
   * <pre>
   * valueFactoryClass:UTF
   * namespace:UTF
   * </pre>
   */
  private static final transient byte VERSION0 = 0;

  private static final transient byte VERSION = VERSION0;

  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    super.readExternal(in);
    final byte version = in.readByte();
    final String namespace;
    final String valueFactoryClass;
    switch (version) {
      case VERSION0:
        namespace = in.readUTF();
        valueFactoryClass = in.readUTF();
        break;
      default:
        throw new IOException("unknown version=" + version);
    }
    // set the namespace field.
    this.namespace = namespace;
    // resolve the valueSerializer from the value factory class.
    try {
      final Class<?> vfc = Class.forName(valueFactoryClass);
      if (!EmbergraphValueFactory.class.isAssignableFrom(vfc)) {
        throw new RuntimeException(
            AbstractTripleStore.Options.VALUE_FACTORY_CLASS
                + ": Must extend: "
                + EmbergraphValueFactory.class.getName());
      }
      final Method gi = vfc.getMethod("getInstance", String.class);
      this.valueFactory = (EmbergraphValueFactory) gi.invoke(null, namespace);
    } catch (NoSuchMethodException e) {
      throw new IOException(e);
    } catch (InvocationTargetException e) {
      throw new IOException(e);
    } catch (IllegalAccessException e) {
      throw new IOException(e);
    }
    valueSer = this.valueFactory.getValueSerializer();
  }

  public void writeExternal(final ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeByte(VERSION);
    out.writeUTF(namespace);
    out.writeUTF(valueFactory.getClass().getName());
  }
}
