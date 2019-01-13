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

import java.util.LinkedHashSet;
import java.util.Set;
import org.embergraph.rdf.internal.IDatatypeURIResolver;
import org.embergraph.rdf.internal.IExtension;
import org.embergraph.rdf.internal.XSD;
import org.embergraph.rdf.internal.impl.literal.FullyInlineTypedLiteralIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
 * This implementation of {@link IExtension} supports fully inlined <code>xsd:string</code> values.
 */
public class XSDStringExtension<V extends EmbergraphValue> implements IExtension<V> {

  private final EmbergraphURI xsdStringURI;
  private final int maxInlineStringLength;

  public XSDStringExtension(final IDatatypeURIResolver resolver, final int maxInlineStringLength) {

    if (resolver == null) throw new IllegalArgumentException();

    if (maxInlineStringLength < 0) throw new IllegalArgumentException();

    this.xsdStringURI = resolver.resolve(XSD.STRING);

    this.maxInlineStringLength = maxInlineStringLength;
  }

  public Set<EmbergraphURI> getDatatypes() {

    final Set<EmbergraphURI> datatypes = new LinkedHashSet<>();
    datatypes.add(xsdStringURI);
    return datatypes;
  }

  public LiteralExtensionIV createIV(final Value value) {

    if (value instanceof Literal == false) throw new IllegalArgumentException();

    if (value.stringValue().length() > maxInlineStringLength) {
      // Too large to inline.
      return null;
    }

    final Literal lit = (Literal) value;

    final URI dt = lit.getDatatype();

    // Note: URI.stringValue() is efficient....
    if (dt == null || !XSD.STRING.stringValue().equals(dt.stringValue()))
      throw new IllegalArgumentException();

    final String s = value.stringValue();

    final FullyInlineTypedLiteralIV<EmbergraphLiteral> delegate =
        new FullyInlineTypedLiteralIV<>(
            s, // label
            null, // no language
            null // no datatype
        );

    return new LiteralExtensionIV<EmbergraphLiteral>(delegate, xsdStringURI.getIV());
  }

  public V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf) {

    return (V) vf.createLiteral(iv.getDelegate().stringValue(), xsdStringURI);
  }
}
