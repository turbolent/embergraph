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
import org.embergraph.rdf.internal.impl.uri.URIExtensionIV;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.vocab.Vocabulary;
import org.openrdf.model.URI;

/** Do-nothing inline URI factory used in the case where there is no vocabulary defined. */
public class NoInlineURIFactory implements IInlineURIFactory {

  public NoInlineURIFactory() {}

  public void init(final Vocabulary vocab) {}

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public URIExtensionIV createInlineURIIV(URI uri) {
    return null;
  }

  @Override
  public String getLocalNameFromDelegate(
      URI namespace, AbstractLiteralIV<EmbergraphLiteral, ?> delegate) {
    throw new UnsupportedOperationException(
        "Since there can't be inlined URIs this should never be attempted.");
  }
}
