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

import java.math.BigDecimal;
import java.math.BigInteger;
import org.embergraph.btree.keys.KeyBuilder;
import org.embergraph.rdf.internal.DTE;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.lexicon.LexiconRelation;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;

/** Implementation for inline <code>xsd:integer</code>. */
public class XSDIntegerIV<V extends EmbergraphLiteral> extends NumericIV<V, BigInteger>
    implements Literal {

  /** */
  private static final long serialVersionUID = 7530841693216602374L;

  private final BigInteger value;

  private transient int byteLength;

  public IV<V, BigInteger> clone(final boolean clearCache) {

    final XSDIntegerIV<V> tmp = new XSDIntegerIV<V>(value);

    // propagate transient state if available.
    tmp.byteLength = byteLength;

    if (!clearCache) {

      tmp.setValue(getValueCache());
    }

    return tmp;
  }

  public XSDIntegerIV(final BigInteger value) {

    super(DTE.XSDInteger);

    if (value == null) throw new IllegalArgumentException();

    this.value = value;
  }

  public final BigInteger getInlineValue() {

    return value;
  }

  @Override
  public boolean booleanValue() {
    return value.equals(BigInteger.ZERO) ? false : true;
  }

  @Override
  public byte byteValue() {
    return value.byteValue();
  }

  @Override
  public short shortValue() {
    return value.shortValue();
  }

  @Override
  public int intValue() {
    return value.intValue();
  }

  @Override
  public final long longValue() {
    return value.longValue();
  }

  @Override
  public float floatValue() {
    return value.floatValue();
  }

  @Override
  public double doubleValue() {
    return value.doubleValue();
  }

  @Override
  public BigInteger integerValue() {
    return value;
  }

  @Override
  public BigDecimal decimalValue() {
    return new BigDecimal(value);
  }

  @SuppressWarnings("unchecked")
  public V asValue(final LexiconRelation lex) {
    V v = getValueCache();
    if (v == null) {
      final EmbergraphValueFactory f = lex.getValueFactory();
      v = (V) f.createLiteral(value.toString(), DTE.XSDInteger.getDatatypeURI());
      v.setIV(this);
      setValue(v);
    }
    return v;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o instanceof XSDIntegerIV<?>) {
      // Note: I believe that equals() works just fine for BigInteger.
      return this.value.equals(((XSDIntegerIV<?>) o).value);
      //            return this.value.compareTo(((XSDIntegerIV<?>) o).value)==0;
    }
    return false;
  }

  /** Return the hash code of the {@link BigInteger}. */
  public int hashCode() {
    return value.hashCode();
  }

  public int byteLength() {

    if (byteLength == 0) {

      /*
       * Cache the byteLength if not yet set.
       */

      byteLength = 1 /* prefix */ + KeyBuilder.byteLength(value);
    }

    return byteLength;
  }

  @Override
  public int _compareTo(IV o) {

    return value.compareTo(((XSDIntegerIV) o).value);
  }
}
