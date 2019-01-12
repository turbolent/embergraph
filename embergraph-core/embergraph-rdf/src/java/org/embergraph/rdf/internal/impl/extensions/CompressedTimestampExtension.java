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
 * Created on Oct 29, 2015
 */

package org.embergraph.rdf.internal.impl.extensions;

import java.util.Collections;
import java.util.Set;
import org.embergraph.rdf.internal.IDatatypeURIResolver;
import org.embergraph.rdf.internal.IExtension;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.PackedLongIV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;

/*
 * Effective, packed representation of timestamps typed as <code>
 * <http://www.embergraph.org/rdf/datatype#compressedTimestamp></code>. Builds on the {@link
 * PackedLongIV} datatype for representing timestamps, thus supporting the range
 * [0;72057594037927935L].
 *
 * <p>The datatype is recommended for use with timestamps specified in seconds and milliseconds; you
 * should not run into any range issues there at all.
 *
 * <p>Using the datatype for timestamps in microseconds or smaller is discouraged: in the
 * microseconds case the value 72057594037927935 corresponds to Fri, 04 May 2198 23:50:03 GMT, so
 * you couldn't represent dates beyond that time.
 */
@SuppressWarnings("rawtypes")
public class CompressedTimestampExtension<V extends EmbergraphValue> implements IExtension<V> {

  private final EmbergraphURI datatype;

  public static final URI COMPRESSED_TIMESTAMP =
      new URIImpl("http://www.embergraph.org/rdf/datatype#compressedTimestamp");

  public CompressedTimestampExtension(final IDatatypeURIResolver resolver) {
    datatype = resolver.resolve(COMPRESSED_TIMESTAMP);
  }

  public Set<EmbergraphURI> getDatatypes() {

    return Collections.singleton(datatype);
  }

  /** Convert the supplied value into an internal representation as PackedLongIV. */
  @SuppressWarnings("unchecked")
  public LiteralExtensionIV createIV(final Value value) {

    if (value instanceof Literal == false) throw new IllegalArgumentException();

    final Literal lit = (Literal) value;

    final AbstractLiteralIV delegate = new PackedLongIV(Long.parseLong(lit.getLabel()));
    return new LiteralExtensionIV(delegate, datatype.getIV());
  }

  @SuppressWarnings("unchecked")
  public V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf) {

    AbstractLiteralIV delegate = iv.getDelegate();
    if (delegate == null || !(delegate instanceof PackedLongIV)) {
      throw new IllegalArgumentException();
    }

    final PackedLongIV pIv = (PackedLongIV) delegate;
    return (V)
        vf.createLiteral(
            String.valueOf(pIv.getInlineValue()),
            CompressedTimestampExtension.COMPRESSED_TIMESTAMP);
  }
}
