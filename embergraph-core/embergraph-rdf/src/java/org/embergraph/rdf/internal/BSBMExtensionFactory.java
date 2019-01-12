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
 * Created on Nov 16, 2011
 */

package org.embergraph.rdf.internal;

import java.util.Collection;
import org.embergraph.rdf.internal.impl.extensions.USDFloatExtension;
import org.embergraph.rdf.model.EmbergraphLiteral;
import org.embergraph.rdf.model.EmbergraphValue;

/*
* Adds inlining for the <code>http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/vocabulary/USD</code>
 * datatype, which is treated as <code>xsd:float</code>.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class BSBMExtensionFactory extends DefaultExtensionFactory {

  @Override
  protected void _init(
      final IDatatypeURIResolver resolver,
      final ILexiconConfiguration<EmbergraphValue> lex,
      final Collection<IExtension<? extends EmbergraphValue>> extensions) {

    // Extension to inline "USD" datatypes.
    extensions.add(new USDFloatExtension<EmbergraphLiteral>(resolver));
  }
}
