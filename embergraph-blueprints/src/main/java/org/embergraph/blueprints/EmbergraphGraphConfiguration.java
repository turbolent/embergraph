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
package org.embergraph.blueprints;

import com.tinkerpop.rexster.config.GraphConfiguration;
import com.tinkerpop.rexster.config.GraphConfigurationContext;
import com.tinkerpop.rexster.config.GraphConfigurationException;
import org.apache.commons.configuration.Configuration;

/*
 * Create and configure a EmbergraphGraph for Rexster.
 *
 * @author mikepersonick
 */
public class EmbergraphGraphConfiguration implements GraphConfiguration {

  public interface Options {

    /** Specify the type of embergraph instance to use - embedded or remote. */
    String TYPE = "properties.type";

    /** Specifies that an embedded embergraph instance should be used. */
    String TYPE_EMBEDDED = "embedded";

    /*
     * Specifies that a remote embergraph instance should be used. You MUST also specify one of the
     * following combinations:
     *
     * <dl>
     *   <dt>{@link #HOST} + {@link #PORT}
     *   <dd>The connect to the default namespace on that host.
     *   <dt>{@link #SPARQL_ENDPOINT_URL}
     *   <dd>To connect to a specific namespace using the SPARQL endpoint URL.
     * </dl>
     *
     * @see https://jira.blazegraph.com/browse/BLZG-1374
     */
    String TYPE_REMOTE = "remote";

    /** Journal file for an embedded embergraph instance. */
    String FILE = "properties.file";

    /** Host for a remote embergraph instance. */
    String HOST = "properties.host";

    /** Port for a remote embergraph instance. */
    String PORT = "properties.port";

    /*
     * To connect to a specific namespace using the SPARQL endpoint URL
     *
     * @see https://jira.blazegraph.com/browse/BLZG-1374
     */
    String SPARQL_ENDPOINT_URL = "properties.sparqlEndpointURL";
  }

  /*
   * Configure and return a EmbergraphGraph based on the supplied configuration parameters.
   *
   * @see {@link Options}
   * @see
   *     com.tinkerpop.rexster.config.GraphConfiguration#configureGraphInstance(com.tinkerpop.rexster.config.GraphConfigurationContext)
   */
  @Override
  public EmbergraphGraph configureGraphInstance(final GraphConfigurationContext context)
      throws GraphConfigurationException {

    try {

      return configure(context);

    } catch (Exception ex) {

      throw new GraphConfigurationException(ex);
    }
  }

  protected EmbergraphGraph configure(final GraphConfigurationContext context) throws Exception {

    final Configuration config = context.getProperties();

    if (!config.containsKey(Options.TYPE)) {
      throw new GraphConfigurationException("missing required parameter: " + Options.TYPE);
    }

    final String type = config.getString(Options.TYPE).toLowerCase();

    if (Options.TYPE_EMBEDDED.equals(type)) {

      if (config.containsKey(Options.FILE)) {

        final String journal = config.getString(Options.FILE);

        return EmbergraphGraphFactory.open(journal, true);

      } else {

        return EmbergraphGraphFactory.create();
      }

    } else if (Options.TYPE_REMOTE.equals(type)) {

      if (config.containsKey(Options.SPARQL_ENDPOINT_URL)) {

        final String sparqlEndpointURL = config.getString(Options.SPARQL_ENDPOINT_URL);

        return EmbergraphGraphFactory.connect(sparqlEndpointURL);
      }

      if (!config.containsKey(Options.HOST)) {
        throw new GraphConfigurationException("missing required parameter: " + Options.HOST);
      }

      if (!config.containsKey(Options.PORT)) {
        throw new GraphConfigurationException("missing required parameter: " + Options.PORT);
      }

      final String host = config.getString(Options.HOST);

      final int port = config.getInt(Options.PORT);

      return EmbergraphGraphFactory.connect(host, port);

    } else {

      throw new GraphConfigurationException("unrecognized value for " + Options.TYPE + ": " + type);
    }
  }
}
