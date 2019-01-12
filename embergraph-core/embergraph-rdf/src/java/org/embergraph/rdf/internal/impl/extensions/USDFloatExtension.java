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

import java.util.Collections;
import java.util.Set;
import org.embergraph.rdf.internal.IDatatypeURIResolver;
import org.embergraph.rdf.internal.IExtension;
import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.internal.impl.literal.XSDNumericIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.vocab.decls.BSBMVocabularyDecl;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;

/*
 * Adds inlining for the <code>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/USD</code>
 * datatype, which is treated as <code>xsd:float</code>.
 */
@SuppressWarnings("rawtypes")
public class USDFloatExtension<V extends EmbergraphValue> implements IExtension<V> {

  private final EmbergraphURI datatype;

  public USDFloatExtension(final IDatatypeURIResolver resolver) {

    datatype = resolver.resolve(BSBMVocabularyDecl.USD);
  }

  public Set<EmbergraphURI> getDatatypes() {

    return Collections.singleton(datatype);
  }

  /** Attempts to convert the supplied value into an internal representation using BigInteger. */
  @SuppressWarnings("unchecked")
  public LiteralExtensionIV createIV(final Value value) {

    if (value instanceof Literal == false) throw new IllegalArgumentException();

    final Literal lit = (Literal) value;

    final AbstractLiteralIV delegate =
        new XSDNumericIV<EmbergraphLiteral>(XMLDatatypeUtil.parseFloat(lit.getLabel()));

    return new LiteralExtensionIV(delegate, datatype.getIV());
  }

  @SuppressWarnings("unchecked")
  public V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf) {

    final String s = Float.toString(iv.getDelegate().floatValue());

    return (V) vf.createLiteral(s, datatype);
  }
}
