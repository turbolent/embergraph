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

import org.embergraph.rdf.internal.impl.literal.AbstractLiteralIV;
import org.embergraph.rdf.model.EmbergraphLiteral;

/**
 * Utility IV to generate IVs for URIs in the form of http://example.org/value/STRPREFIX1234234513
 * where the localName of the URI is a string prefix followed by an integer value with fixed width.
 *
 * <p>You should extend this class with implementation for specific instances of URIs that follow
 * this form such as: http://rdf.ncbi.nlm.nih.gov/pubchem/compound/CID_1234234 would be created as
 * <code>
 * InlinePrefixedFixedWidthIntegerURIHandler handler = new InlinePrefixedFixedWidthIntegerURIHandler("http://rdf.ncbi.nlm.nih.gov/pubchem/compound/","CID_", 7);
 * </code> This has support for overloading on a single namespace {@link
 * InlineLocalNameIntegerURIHandler}.
 *
 * @author beebs
 */
public class InlinePrefixedFixedWidthIntegerURIHandler extends InlineLocalNameIntegerURIHandler
    implements IPrefixedURIHandler {

  private String prefix = null;
  private int width = 0;

  public InlinePrefixedFixedWidthIntegerURIHandler(
      final String namespace, final String prefix, final int width) {
    super(namespace);
    this.prefix = prefix;
    this.width = width;
  }

  public InlinePrefixedFixedWidthIntegerURIHandler(
      final String namespace, final String prefix, final int width, final int id) {
    super(namespace);
    this.prefix = prefix;
    this.width = width;
    this.packedId = id;
  }

  @Override
  @SuppressWarnings("rawtypes")
  protected AbstractLiteralIV createInlineIV(String localName) {
    if (!localName.startsWith(this.prefix)) {
      return null;
    }

    final String intValue =
        getPackedValueString(localName.substring(this.prefix.length(), localName.length()));

    return super.createInlineIV(intValue);
  }

  @Override
  public String getLocalNameFromDelegate(AbstractLiteralIV<EmbergraphLiteral, ?> delegate) {

    final String intStr = super.getLocalNameFromDelegate(delegate);

    final int intVal = (int) getUnpackedValueFromString(intStr);

    final String localName = this.prefix + String.format("%0" + width + "d", intVal);

    return localName;
  }

  public String getPrefix() {
    return prefix;
  }
}
