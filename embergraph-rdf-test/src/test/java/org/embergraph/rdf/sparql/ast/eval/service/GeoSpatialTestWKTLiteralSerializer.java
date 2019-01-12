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
 * Created on March 02, 2016
 */
package org.embergraph.rdf.sparql.ast.eval.service;

import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.model.EmbergraphValueFactory;
import org.embergraph.rdf.sparql.ast.DummyConstantNode;
import org.embergraph.service.geospatial.GeoSpatialDefaultLiteralSerializer;
import org.embergraph.service.geospatial.GeoSpatialSearchException;
import org.openrdf.model.impl.URIImpl;

/*
 * Test serializer for WKT literals of the form Point(lat,lon). Note that this is a dummy
 * implementation for tests only, assuming there are no whitespaces contained in the literal, etc.
 *
 * @author msc
 */
public class GeoSpatialTestWKTLiteralSerializer extends GeoSpatialDefaultLiteralSerializer {

  @Override
  public String[] toComponents(String literalString) {
    if (literalString == null) {
      return new String[0];
    }

    final String core = literalString.substring(6, literalString.length() - 1);
    return core.split(",");
  }

  @Override
  public String fromComponents(Object[] components) {

    if (components == null) return "";

    if (components.length != 2)
      throw new GeoSpatialSearchException(
          "Expected component string of lenth 2, but was " + components.length);

    final StringBuffer buf = new StringBuffer();
    buf.append("Point(");
    buf.append(components[0]);
    buf.append(",");
    buf.append(components[1]);
    buf.append(")");

    return buf.toString();
  }

  @Override
  public IV<?, ?> serializeLocation(
      final EmbergraphValueFactory vf, final Object latitude, final Object longitude) {

    final StringBuffer buf = new StringBuffer();
    buf.append("Point(");
    buf.append(latitude);
    buf.append(",");
    buf.append(longitude);
    buf.append(")");

    return DummyConstantNode.toDummyIV(
        vf.createLiteral(
            buf.toString(), new URIImpl("http://www.opengis.net/ont/geosparql#wktLiteral")));
  }
}
