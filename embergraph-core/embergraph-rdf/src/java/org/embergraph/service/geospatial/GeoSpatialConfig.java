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
 * Created on Feb 10, 2016
 */
package org.embergraph.service.geospatial;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.embergraph.rdf.internal.impl.extensions.InvalidGeoSpatialDatatypeConfigurationError;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

/*
 * Class providing access to the GeoSpatial index configuration, including datatype definition and
 * default datatype for querying. Initialized and used only if geospatial subsytem is used.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class GeoSpatialConfig implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final String JSON_STR_CONFIG = "config";
  private static final String JSON_STR_URI = "uri";
  private static final String JSON_STR_LITERALSERIALIZER = "literalSerializer";
  private static final String JSON_STR_FIELDS = "fields";

  private static final transient Logger log = Logger.getLogger(GeoSpatialConfig.class);

  /** List containing the configurations for the geospatial datatypes. */
  private List<GeoSpatialDatatypeConfiguration> datatypeConfigs;

  // the default datatype for querying
  private URI defaultDatatype;

  public GeoSpatialConfig(
      final List<String> geoSpatialDatatypeConfigs, final String defaultDatatype) {

    initDatatypes(geoSpatialDatatypeConfigs);

    if (defaultDatatype != null && !defaultDatatype.isEmpty()) {

      try {
        this.defaultDatatype = new URIImpl(defaultDatatype);
      } catch (Exception e) {
        throw new InvalidGeoSpatialDatatypeConfigurationError(
            "Invalid default datatype (" + defaultDatatype + ") does not represent a URI.");
      }

      boolean isRegistered = false;
      for (final GeoSpatialDatatypeConfiguration config : datatypeConfigs) {
        isRegistered |= config.getUri().equals(this.defaultDatatype);
      }

      if (!isRegistered) {
        throw new InvalidGeoSpatialDatatypeConfigurationError(
            "Invalid default datatype ("
                + defaultDatatype
                + ") is not a registered geospatial datatype.");
      }
    } // else: ignore it (no default)
  }

  private void initDatatypes(List<String> geoSpatialDatatypeConfigs) {

    datatypeConfigs = new ArrayList<>();

    if (geoSpatialDatatypeConfigs == null) return; // nothing to be done

    /*
     * We expect a JSON config string of the following format (example):
     *
     * <p>{"config": { "uri": "http://my.custom.datatype2.uri", "literalSerializer":
     * "org.embergraph.service.GeoSpatialLiteralSerializer", "fields": [ { "valueType": "DOUBLE",
     * "multiplier": "100000", "serviceMapping": "LATITUDE" }, { "valueType": "DOUBLE",
     * "multiplier": "100000", "serviceMapping": "LONGITUDE" }, { "valueType": "LONG, "multiplier":
     * "1", "minValue" : "0" , "serviceMapping": "TIME" }, { "valueType": "LONG", "multiplier": "1",
     * "minValue" : "0" , "serviceMapping" : "COORD_SYSTEM" } ] }}
     */
    for (final String configStr : geoSpatialDatatypeConfigs) {

      if (configStr == null || configStr.isEmpty()) continue; // skip

      try {

        // read values from JSON
        final JSONObject json = new JSONObject(configStr);
        final JSONObject topLevelNode = (JSONObject) json.get(JSON_STR_CONFIG);
        final String uri = (String) topLevelNode.get(JSON_STR_URI);
        final String literalSerializer =
            topLevelNode.has(JSON_STR_LITERALSERIALIZER)
                ? (String) topLevelNode.get(JSON_STR_LITERALSERIALIZER)
                : null;
        final JSONArray fields = (JSONArray) topLevelNode.get(JSON_STR_FIELDS);

        // delegate to GeoSpatialDatatypeConfiguration for construction
        datatypeConfigs.add(new GeoSpatialDatatypeConfiguration(uri, literalSerializer, fields));

      } catch (JSONException e) {

        log.warn("Illegal JSON configuration: " + e.getMessage());
        throw new IllegalArgumentException(e); // forward exception
      }

      // validate that there are no duplicate URIs used for the datatypeConfigs
      final Set<URI> uris = new HashSet<>();
      for (GeoSpatialDatatypeConfiguration datatypeConfig : datatypeConfigs) {

        final URI curUri = datatypeConfig.getUri();

        if (uris.contains(curUri)) {
          throw new IllegalArgumentException(
              "Duplicate URI used for geospatial datatype config: " + curUri);
        }

        uris.add(curUri);
      }
    }
  }

  public GeoSpatialDatatypeConfiguration getConfigurationForDatatype(URI datatypeUri) {
    for (final GeoSpatialDatatypeConfiguration cur : datatypeConfigs) {
      if (cur.getUri().equals(datatypeUri)) {
        return cur;
      }
    }

    return null; // not found/registered
  }

  public List<GeoSpatialDatatypeConfiguration> getDatatypeConfigs() {
    return datatypeConfigs;
  }

  public URI getDefaultDatatype() {
    return defaultDatatype;
  }
}
