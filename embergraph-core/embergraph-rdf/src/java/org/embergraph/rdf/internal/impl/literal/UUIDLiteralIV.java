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
package org.embergraph.rdf.internal.impl.literal;

import java.util.UUID;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.util.Bytes;
import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;

/** Implementation for inline {@link UUID}s (there is no corresponding XML Schema Datatype). */
public class UUIDLiteralIV<V extends EmbergraphLiteral> extends AbstractLiteralIV<V, UUID>
    implements Literal {

  /** */
  private static final long serialVersionUID = 6411134650187983925L;

  private final UUID value;

  public IV<V, UUID> clone(final boolean clearCache) {

    final UUIDLiteralIV<V> tmp = new UUIDLiteralIV<>(value);

    if (!clearCache) {

      tmp.setValue(getValueCache());
    }

    return tmp;
  }

  public UUIDLiteralIV(final UUID value) {

    super(DTE.UUID);

    if (value == null) throw new IllegalArgumentException();

    this.value = value;
  }

  public final UUID getInlineValue() {
    return value;
  }

  @SuppressWarnings("unchecked")
  public V asValue(final LexiconRelation lex) {

    V v = getValueCache();

    if (v == null) {

      final ValueFactory f = lex.getValueFactory();

      v = (V) f.createLiteral(value.toString(), DTE.UUID.getDatatypeURI());

      v.setIV(this);

      setValue(v);
    }

    return v;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o instanceof UUIDLiteralIV<?>) {
      return this.value.equals(((UUIDLiteralIV<?>) o).value);
    }
    return false;
  }

  /** Return the hash code of the {@link UUID}. */
  public int hashCode() {
    return value.hashCode();
  }

  public int byteLength() {
    return 1 + Bytes.SIZEOF_UUID;
  }

  @Override
  public int _compareTo(IV o) {

    return value.compareTo(((UUIDLiteralIV) o).value);
  }
}
