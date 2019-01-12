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

/*
* This factory will create {@link URIExtensionIV}s using {@link InlineURIHandler} delegates.
 * Handler delegates are registered with a namespace prefix that they can handle. These namespace
 * prefixes must be defined in the vocabulary so that they can be properly inlined. The URI to be
 * inlined will then be presented to each handler for conversion. The first registered handler to
 * convert the URI wins. If no handler can handle the URI then no inline URI iv is created.
 */
public interface IInlineURIFactory {

  /** Give the handlers a chance to look up the vocab IV for their namespace prefixes. */
  void init(final Vocabulary vocab);

  /** Create an inline URIExtensionIV for the supplied URI. */
  URIExtensionIV<?> createInlineURIIV(final URI uri);

  /*
   * Inflate the localName portion of an inline URI using its storage delegate.
   *
   * @param namespace the uris's prefix
   * @param delegate the storage delegate
   * @return the inflated localName
   */
  String getLocalNameFromDelegate(
      final URI namespace, final AbstractLiteralIV<EmbergraphLiteral, ?> delegate);
}
