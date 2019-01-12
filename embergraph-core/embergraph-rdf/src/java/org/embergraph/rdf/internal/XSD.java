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

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.XMLSchema;

/*
* Collects various XSD URIs as constants.
 *
 * @author <a href="mailto:mrpersonick@users.sourceforge.net">Mike Personick</a>
 * @see XMLSchema
 */
public class XSD extends XMLSchema {

  /*
   * @todo This really should not be in the XSD namespace since it is not part of the XML Schema
   *     Datatypes specification.
   */
  public static final URI UUID = new URIImpl(NAMESPACE + "uuid");

  /*
   * Not sure if there is a better solution for this. Perhaps XSSLT?
   *
   * <p>http://www.codesynthesis.com/projects/xsstl/
   */
  public static final URI IPV4 = new URIImpl(NAMESPACE + "IPv4Address");
}
