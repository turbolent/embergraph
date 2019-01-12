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
 * Created on July 27, 2015
 */
package org.embergraph.service.geospatial;

import java.util.List;
import java.util.Map;
import org.embergraph.bop.IBindingSet;
import org.embergraph.bop.IConstant;
import org.embergraph.bop.IVariable;
import org.embergraph.rdf.internal.gis.ICoordinate.UNITS;
import org.embergraph.rdf.sparql.ast.TermNode;
import org.embergraph.service.geospatial.GeoSpatial.GeoFunction;
import org.embergraph.service.geospatial.impl.GeoSpatialUtility.PointLatLon;
import org.openrdf.model.URI;

/*
* Interface representing (the configuration of) a geospatial query.
 *
 * <p>See also {@link GeoSpatial} for the vocabulary that can be used to define such a query as
 * SERVICE (or inline).
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface IGeoSpatialQuery {

  /** @return the search function underlying the query */
  GeoFunction getSearchFunction();

  /** @return the datatype of literals we're searching for */
  URI getSearchDatatype();

  /** @return the constant representing the search subject */
  IConstant<?> getSubject();

  /** @return the term node representing the search predicate */
  TermNode getPredicate();

  /** @return the term node representing the search context (named graph) */
  TermNode getContext();

  /** @return the spatial circle center, in case this is a {@link GeoFunction#IN_CIRCLE} query */
  PointLatLon getSpatialCircleCenter();

  /** @return the spatial circle radius, in case this is a {@link GeoFunction#IN_CIRCLE} query */
  Double getSpatialCircleRadius();

  /** @return the boundary box'es south-west border point. */
  PointLatLon getSpatialRectangleSouthWest();

  /** @return the boundary box'es north-east border point. */
  PointLatLon getSpatialRectangleNorthEast();

  /** @return the spatial unit underlying the query */
  UNITS getSpatialUnit();

  /** @return the start timestamp */
  Long getTimeStart();

  /** @return the end timestamp */
  Long getTimeEnd();

  /** @return the coordinate system ID */
  Long getCoordSystem();

  /** @return the custom fields */
  Map<String, LowerAndUpperValue> getCustomFieldsConstraints();

  /** @return the variable to which the location will be bound (if defined) */
  IVariable<?> getLocationVar();

  /** @return the variable to which the time will be bound (if defined) */
  IVariable<?> getTimeVar();

  /** @return the variable to which the latitude will be bound (if defined) */
  IVariable<?> getLatVar();

  /** @return the variable to which the longitude will be bound (if defined) */
  IVariable<?> getLonVar();

  /** @return the variable to which the coordinate system component will be bound (if defined) */
  IVariable<?> getCoordSystemVar();

  /** @return the variable to which the custom fields will be bound (if defined) */
  IVariable<?> getCustomFieldsVar();

  /** @return the variable to which the location+time will be bound (if defined) */
  IVariable<?> getLocationAndTimeVar();

  /** @return the variable to which the literal value will be bound */
  IVariable<?> getLiteralVar();

  /** @return the variable to which the distance value will be bound */
  IVariable<?> getDistanceVar();

  /** @return the incoming bindings to join with */
  IBindingSet getIncomingBindings();

  /*
   * @return a structure containing the lower and upper bound component object defined by this query
   */
  LowerAndUpperBound getLowerAndUpperBound();

  /*
   * Normalizes a GeoSpatial query by converting it into an list of GeoSpatial queries that are
   * normalized, see isNormalized(). The list of queries may be empty if the given query contains
   * unsatisfiable range restrictions (e.g., a timestamp or longitude range from [9;8]. However,
   * note that a latitude range from [10;0] will be interpreted as "everything not in the interval
   * ]0;10[.
   */
  List<IGeoSpatialQuery> normalize();

  /** @return true if the query is normalized. See */
  boolean isNormalized();

  /** @return true if the query is satisfiable */
  boolean isSatisfiable();

  /** @return the datatype configuration associated with the query */
  GeoSpatialDatatypeConfiguration getDatatypeConfig();

  /*
   * Helper class encapsulating both the lower and upper bound as implied by the query, for the
   * given datatype configuration.
   *
   * @author msc
   */
  class LowerAndUpperBound {
    private final Object[] lowerBound;
    private final Object[] upperBound;

    public LowerAndUpperBound(final Object[] lowerBound, final Object[] upperBound) {
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
    }

    public Object[] getLowerBound() {
      return lowerBound;
    }

    public Object[] getUpperBound() {
      return upperBound;
    }
  }

  class LowerAndUpperValue {
    public final Object lowerValue;
    public final Object upperValue;

    public LowerAndUpperValue(final Object lowerValue, final Object upperValue) {
      this.lowerValue = lowerValue;
      this.upperValue = upperValue;
    }
  }
}
