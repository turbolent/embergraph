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

package org.embergraph.rdf.internal.impl.extensions;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.embergraph.rdf.internal.IDatatypeURIResolver;
import org.embergraph.rdf.internal.IExtension;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.XSDIntegerIV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/*
 * This implementation of {@link IExtension} implements inlining for literals that represent the
 * derived numeric types:
 *
 * <ul>
 *   <li>xsd:nonPositiveInteger
 *   <li>xsd:negativeInteger
 *   <li>xsd:nonNegativeInteger
 *   <li>xsd:positiveInteger
 * </ul>
 */
public class DerivedNumericsExtension<V extends EmbergraphValue> implements IExtension<V> {

  private static final transient Logger log = Logger.getLogger(DerivedNumericsExtension.class);

  private final Map<IV, EmbergraphURI> datatypes;

  public DerivedNumericsExtension(final IDatatypeURIResolver resolver) {

    this.datatypes = new LinkedHashMap<>();
    resolve(resolver, XSD.POSITIVE_INTEGER);
    resolve(resolver, XSD.NEGATIVE_INTEGER);
    resolve(resolver, XSD.NON_POSITIVE_INTEGER);
    resolve(resolver, XSD.NON_NEGATIVE_INTEGER);
  }

  private void resolve(final IDatatypeURIResolver resolver, final URI uri) {

    if (log.isDebugEnabled()) {
      log.debug("resolving: " + uri);
    }

    final EmbergraphURI val = resolver.resolve(uri);
    datatypes.put(val.getIV(), val);
  }

  public Set<EmbergraphURI> getDatatypes() {

    return new LinkedHashSet<>(datatypes.values());
  }

  /** Attempts to convert the supplied value into an internal representation using BigInteger. */
  public LiteralExtensionIV createIV(final Value value) {

    if (value instanceof Literal == false) throw new IllegalArgumentException();

    final Literal lit = (Literal) value;

    final URI dt = lit.getDatatype();

    if (dt == null) throw new IllegalArgumentException();

    final String dts = dt.stringValue();

    EmbergraphURI resolvedDT = null;
    for (EmbergraphURI val : datatypes.values()) {
      // Note: URI.stringValue() is efficient....
      if (val.stringValue().equals(dts)) {
        resolvedDT = val;
      }
    }

    if (resolvedDT == null) throw new IllegalArgumentException();

    final String s = lit.getLabel();

    final boolean valid;
    if (dts.equals(XSD.POSITIVE_INTEGER.stringValue())) {
      valid = XMLDatatypeUtil.isValidPositiveInteger(s);
    } else if (dts.equals(XSD.NEGATIVE_INTEGER.stringValue())) {
      valid = XMLDatatypeUtil.isValidNegativeInteger(s);
    } else if (dts.equals(XSD.NON_POSITIVE_INTEGER.stringValue())) {
      valid = XMLDatatypeUtil.isValidNonPositiveInteger(s);
    } else if (dts.equals(XSD.NON_NEGATIVE_INTEGER.stringValue())) {
      valid = XMLDatatypeUtil.isValidNonNegativeInteger(s);
    } else {
      valid = false;
    }

    if (!valid) {
      throw new RuntimeException("could not correctly parse label: " + s + " for datatype: " + dts);
    }

    final BigInteger bi = XMLDatatypeUtil.parseInteger(s);

    final AbstractLiteralIV delegate = new XSDIntegerIV(bi);

    return new LiteralExtensionIV(delegate, resolvedDT.getIV());
  }

  /*
   * Use the BigInteger value of the {@link XSDIntegerIV} delegate to create a datatype literal
   * value with the appropriate datatype.
   */
  public V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf) {

    if (!datatypes.containsKey(iv.getExtensionIV())) {
      throw new IllegalArgumentException("unrecognized datatype");
    }

    final BigInteger bi = iv.getDelegate().integerValue();

    final EmbergraphURI dt = datatypes.get(iv.getExtensionIV());

    final String s = bi.toString();

    return (V) vf.createLiteral(s, dt);
  }
}
