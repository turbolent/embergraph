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

import java.util.Iterator;
import org.embergraph.rdf.internal.impl.BlobIV;
import org.embergraph.rdf.model.EmbergraphURI;
import org.embergraph.rdf.model.EmbergraphValue;
import org.openrdf.model.URI;

/**
 * IExtensionFactories are responsible for enumerating what extensions are supported for a
 * particular database configuration. Embergraph comes packaged with a {@link
 * SampleExtensionFactory} that supplies two starter extensions - the {@link EpochExtension} (for
 * representing time since the epoch as a long integer) and the {@link ColorsEnumExtension} (a
 * sample extension for how to represent an enumeration via inline literals).
 */
public interface IExtensionFactory {

  /**
   * This will be called very early in the IExtensionFactory lifecycle so that the {@link BlobIV}s
   * for the {@link IExtension}'s datatype URIs will be on hand when needed. Also gets other
   * relevant configuration information from the lexicon such as whether or not to inline
   * xsd:datetimes and what timezone to use to do so.
   *
   * @param resolver The interface used to resolve an {@link URI} to an {@link EmbergraphURI}.
   * @param config The {@link ILexiconConfiguration}.
   */
  void init(
      final IDatatypeURIResolver resolver, final ILexiconConfiguration<EmbergraphValue> config);

  /** Return the supported extensions. */
  Iterator<IExtension<? extends EmbergraphValue>> getExtensions();
}
