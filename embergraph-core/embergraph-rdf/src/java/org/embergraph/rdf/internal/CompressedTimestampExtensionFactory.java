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

package org.embergraph.rdf.internal;

import java.util.Collection;
import org.embergraph.rdf.internal.impl.extensions.CompressedTimestampExtension;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;

/**
 * Extension factory that adds a compressed timestamp literal datatype, namely
 * <http://www.embergraph.org/rdf/datatype#compressedTimestamp>. Integers typed with this datatype
 * should be literals, which are compactly stored in the database.
 *
 * @author <a href="ms@metaphacts.com">Michael Schmidt</a>
 */
public class CompressedTimestampExtensionFactory extends DefaultExtensionFactory {

  @Override
  protected void _init(
      final IDatatypeURIResolver resolver,
      final ILexiconConfiguration<EmbergraphValue> lex,
      final Collection<IExtension<? extends EmbergraphValue>> extensions) {

    extensions.add(new CompressedTimestampExtension<EmbergraphLiteral>(resolver));
  }
}
