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
 * Created on June 3rd, 2011
 */
package org.embergraph.rdf.internal.impl.uri;

import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.VTE;
import org.embergraph.rdf.internal.impl.AbstractInlineIV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
 * A fully inlined representation of a URI based on a <code>byte</code> code. The flags byte looks
 * like: <code>VTE=URI, inline=true, extension=false,
 * DTE=XSDByte</code>. It is followed by an <code>unsigned byte</code> value which is the index of
 * the URI in the {@link Vocabulary} class for the triple store.
 *
 * @author thompsonbry
 */
public class VocabURIByteIV<V extends EmbergraphURI> extends AbstractInlineIV<V, Byte>
    implements URI {

  /** */
  private static final long serialVersionUID = -1609505688748169776L;

  private final byte value;

  public byte byteValue() {

    return value;
  }

  /*
   * {@inheritDoc}
   *
   * <p>Note: Always returns <i>this</i>. (The rationale is that the vocabulary {@link IV}s already
   * pin their cached value so there is no point in creating a clone with the cleared cache
   * reference.)
   */
  public IV<V, Byte> clone(final boolean clearCache) {

    return this;
  }

  public VocabURIByteIV(final byte value) {

    super(VTE.URI, DTE.XSDByte);

    this.value = value;
  }

  /*
   * {@inheritDoc}
   *
   * <p>Overridden to return <code>true</code>.
   */
  @Override
  public final boolean isVocabulary() {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public int _compareTo(final IV o) {

    final byte v = KeyBuilder.decodeByte(value);

    final byte v2 = KeyBuilder.decodeByte(((VocabURIByteIV<EmbergraphURI>) o).value);

    return v == v2 ? 0 : v < v2 ? -1 : 1;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o instanceof VocabURIByteIV<?>) {
      return this.value == ((VocabURIByteIV<?>) o).value;
    }
    return false;
  }

  /** Return the hash code of the byte value. */
  public final int hashCode() {

    return (int) value;
  }

  public V asValue(final LexiconRelation lex) throws UnsupportedOperationException {

    V v = getValueCache();

    if (v == null) {

      final EmbergraphValueFactory f = lex.getValueFactory();

      v = (V) lex.getContainer().getVocabulary().asValue(this);

      v.setIV(this);

      setValue(v);
    }

    return v;
  }

  public final int byteLength() {

    return 2 /* flags(1) + byte(2) */;
  }

  public final Byte getInlineValue() {

    return value;
  }

  public String toString() {

    return "Vocab(" + value + ")" + (hasValue() ? "[" + getValue().stringValue() + "]" : "");
  }

  /*
   * Because we only store an index into the vocabulary, we need the materialized URI to answer the
   * URI interface methods.
   */
  @Override
  public boolean needsMaterialization() {

    return true;
  }

  /** Implements {@link Value#stringValue()}. */
  @Override
  public String stringValue() {

    return getValue().stringValue();
  }

  /** Implements {@link URI#getLocalName()}. */
  @Override
  public String getLocalName() {

    return getValue().getLocalName();
  }

  /** Implements {@link URI#getNamespace()}. */
  @Override
  public String getNamespace() {

    return getValue().getNamespace();
  }
}
