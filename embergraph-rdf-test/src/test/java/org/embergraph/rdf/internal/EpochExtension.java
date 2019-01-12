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

package org.embergraph.rdf.internal;

import java.util.LinkedHashSet;
import java.util.Set;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.store.BD;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.URIImpl;

/*
* This implementation of {@link IExtension} implements inlining for literals that represent time in
 * milliseconds since the epoch. The milliseconds are encoded as an inline long.
 */
public class EpochExtension<V extends EmbergraphValue> implements IExtension<V> {

  /** The datatype URI for the epoch extension. */
  public static final transient URI EPOCH = new URIImpl(BD.NAMESPACE + "Epoch");

  private final EmbergraphURI epoch;

  public EpochExtension(final IDatatypeURIResolver resolver) {

    this.epoch = resolver.resolve(EPOCH);
  }

  public Set<EmbergraphURI> getDatatypes() {

    final Set<EmbergraphURI> datatypes = new LinkedHashSet<EmbergraphURI>();
    datatypes.add(epoch);
    return datatypes;
  }

  /*
   * Attempts to convert the supplied value into an epoch representation. Tests for a literal value
   * with the correct datatype that can be converted to a positive long integer. Encodes the long in
   * a delegate {@link XSDLongIV}, and returns an {@link LiteralExtensionIV} to wrap the native
   * type.
   */
  public LiteralExtensionIV createIV(final Value value) {

    if (value instanceof Literal == false) throw new IllegalArgumentException();

    final Literal lit = (Literal) value;

    final URI dt = lit.getDatatype();

    if (dt == null || !EPOCH.stringValue().equals(dt.stringValue()))
      throw new IllegalArgumentException();

    final String s = value.stringValue();

    final long l = XMLDatatypeUtil.parseLong(s);

    final AbstractLiteralIV delegate = new XSDNumericIV(l);

    return new LiteralExtensionIV(delegate, epoch.getIV());
  }

  /*
   * Use the string value of the {@link LiteralExtensionIV} (which defers to the string value of the
   * native type) to create a literal with the epoch datatype.
   */
  public V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf) {

    return (V) vf.createLiteral(iv.getDelegate().stringValue(), epoch);
  }
}
