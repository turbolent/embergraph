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
 * Created on March 01, 2016
 */
package org.embergraph.service.geospatial;

import java.io.Serializable;
import org.embergraph.rdf.internal.IV;
import org.embergraph.rdf.internal.gis.ICoordinate.UNITS;
import org.embergraph.rdf.model.EmbergraphValueFactory;

/*
* Interface for serialization and deserialization of GeoSpatial datatypes, defining how a given
 * geospatial datatype is translated from literal string into its component array and back.
 *
 * @author msc
 */
public interface IGeoSpatialLiteralSerializer extends Serializable {

  /*
   * Decomposes a string[] into an array of strings identifying the individual components.
   *
   * @param literalString
   */
  String[] toComponents(final String literalString);

  /*
   * Recomposes the components into a string, should typically use the object's toString() method.
   *
   * @param components
   */
  String fromComponents(final Object[] components);

  /*
   * Serialize a geo-location of latitude and longitude. The two parameters are either Double or
   * Long values, so you should typically use the object's toString() method to get to a valid
   * string representation.
   *
   * @param vf
   * @param latitude
   * @param longitude
   */
  IV<?, ?> serializeLocation(
      final EmbergraphValueFactory vf, final Object latitude, final Object longitude);

  /*
   * Serialize a latitude+longitude+time value contained in a geospatial datatype. The parameters
   * are either Double or Long values, so you should typically use the object's toString() method to
   * get to a valid string representation.
   *
   * @param vf
   * @param latitude
   * @param longitude
   * @param time
   */
  IV<?, ?> serializeLocationAndTime(
      final EmbergraphValueFactory vf,
      final Object latitude,
      final Object longitude,
      final Object time);

  /*
   * Serialize a time value contained in a geospatial datatype. The parameter is either a Double or
   * Long value, so you should typically use the object's toString() method to get to a valid string
   * representation.
   *
   * @param vf
   * @param time
   */
  IV<?, ?> serializeTime(final EmbergraphValueFactory vf, final Object time);

  /*
   * Serialize a latitude value contained in a geospatial datatype. The parameter is either a Double
   * or Long value, so you should typically use the object's toString() method to get to a valid
   * string representation.
   *
   * @param vf
   * @param latitude
   */
  IV<?, ?> serializeLatitude(final EmbergraphValueFactory vf, final Object latitude);

  /*
   * Serialize a longitude value contained in a geospatial datatype. The parameter is either a
   * Double or Long value, so you should typically use the object's toString() method to get to a
   * valid string representation.
   *
   * @param vf
   * @param longitude
   */
  IV<?, ?> serializeLongitude(final EmbergraphValueFactory vf, final Object longitude);

  /*
   * Serialize a coordinate system value contained in a geospatial datatype. The parameter is either
   * a Double or Long value, so you should typically use the object's toString() method to get to a
   * valid string representation.
   *
   * @param vf
   * @param coordinateSystem
   */
  IV<?, ?> serializeCoordSystem(
      final EmbergraphValueFactory vf, final Object coordinateSystem);

  /*
   * Serialize a custom fields value contained in a geospatial datatype. The parameter is either a
   * Double or Long value, so you should typically use the object's toString() method to get to a
   * valid string representation.
   *
   * @param coordSystem
   */
  IV<?, ?> serializeCustomFields(
      final EmbergraphValueFactory vf, final Object... customFields);

  /*
   * Serialize a distance value. The two parameters reflect the distance value (as Double) and the
   * {@link UNITS} in which the value is represented. The {@link UNITS} depend on the units that are
   * specified within the query. Given that the user should be aware of the {@link UNITS}, you may
   * choose to return a numerical value, just ignoring the latter.
   *
   * @param vf
   * @param distance
   * @param units
   */
  IV<?, ?> serializeDistance(
      final EmbergraphValueFactory vf, final Double distance, final UNITS unit);
}
