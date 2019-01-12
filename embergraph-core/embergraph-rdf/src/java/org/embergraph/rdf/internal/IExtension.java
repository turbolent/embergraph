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

import java.util.Set;
import org.embergraph.rdf.internal.impl.TermId;
import org.embergraph.rdf.internal.impl.literal.LiteralExtensionIV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.vocab.Vocabulary;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

/*
* {@link IExtension}s are responsible for round-tripping between an RDF {@link Value} and an {@link
 * LiteralExtensionIV} for a particular datatype. Because of how {@link LiteralExtensionIV}s are
 * encoded and decoded, the {@link IExtension} will need to have on hand the {@link TermId} for its
 * datatype. This is accomplished via the {@link IDatatypeURIResolver} - the {@link IExtension} will
 * give the resolver the datatype {@link URI} it needs resolved and the resolver will lookup (or
 * create) the {@link TermId}. This relies on the declaration of that {@link URI} as part of the
 * {@link Vocabulary}.
 */
public interface IExtension<V extends EmbergraphValue> {

  /*
   * Return the fully resolved datatype(s) handled by this interface in the form of a {@link
   * EmbergraphURI} with the {@link TermId} already set.
   *
   * @return the datatype
   */
  Set<EmbergraphURI> getDatatypes();

  /*
   * Create an {@link LiteralExtensionIV} from an RDF value.
   *
   * @param value The RDF {@link Value}
   * @return The extension {@link IV} -or- <code>null</code> if the {@link Value} can not be inlined
   *     using this {@link IExtension}.
   */
  LiteralExtensionIV createIV(final Value value);

  /*
   * Create an RDF value from an {@link LiteralExtensionIV}.
   *
   * @param iv The extension {@link IV}
   * @param vf The embergraph value factory
   * @return The RDF {@link Value}
   */
  V asValue(final LiteralExtensionIV iv, final EmbergraphValueFactory vf);
}
