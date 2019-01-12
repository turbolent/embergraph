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

import java.util.Properties;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.remote.EmbergraphSailFactory;

/*
 * Helper class to create EmbergraphGraph instances.
 *
 * @author mikepersonick
 */
public class EmbergraphGraphFactory {

  //    private static final transient Logger log = Logger.getLogger(EmbergraphGraphFactory.class);

  /*
   * Connect to a remote embergraph instance.
   *
   * <p>FIXME This does not parameterize the value of the ContextPath. See {@link
   * EmbergraphStatics#getContextPath()}.
   *
   * @deprecated As of version 1.5.2, you should use one of the connect methods with a
   *     sparqlEndpointURL. See {@linkplain
   *     http://wiki.blazegraph.com/wiki/index.php/NanoSparqlServer#Active_URLs}
   */
  public static EmbergraphGraph connect(final String host, final int port) {

    // Assume the default KB to make the SPARQL Endpoint
    // FIXME:  the /embergraph reference should be parameterized
    return connect("http://" + host + ":" + port + "/embergraph" + "/sparql");
  }

  /*
   * Connect to a remote embergraph instance.
   *
   * @param sparqlEndpointURL The URL of the SPARQL end point. This will be used to read and write
   *     on the graph using the blueprints API.
   */
  public static EmbergraphGraph connect(final String sparqlEndpointURL) {

    // Ticket #1182:  centralize rewriting in the SAIL factory.

    return new EmbergraphGraphClient(EmbergraphSailFactory.connect(sparqlEndpointURL));
  }

  /*
   * Open an existing persistent local embergraph instance. If a journal does not exist at the
   * specified location and the boolean create flag is true a journal will be created at that
   * location.
   */
  public static EmbergraphGraph open(final String file, final boolean create) throws Exception {
    final EmbergraphSail sail = (EmbergraphSail) EmbergraphSailFactory.openSail(file, create);
    sail.initialize();
    return new EmbergraphGraphEmbedded(sail);
  }

  /*
   * Create a persistent local embergraph instance. If a journal does not exist at the specified
   * location, then a journal will be created at that location.
   */
  public static EmbergraphGraph create(final String file) throws Exception {
    final EmbergraphSail sail = (EmbergraphSail) EmbergraphSailFactory.openSail(file, true);
    sail.initialize();
    return new EmbergraphGraphEmbedded(sail);
  }

  /** Create a new local in-memory embergraph instance. */
  public static EmbergraphGraph create() throws Exception {
    return create(EmbergraphRDFFactory.INSTANCE);
  }

  /** Create a new local in-memory embergraph instance with the supplied value factory. */
  public static EmbergraphGraph create(final BlueprintsValueFactory vf) throws Exception {
    return create(vf, new Properties());
  }

  /** Create a new local in-memory embergraph instance with the supplied value factory. */
  public static EmbergraphGraph create(final BlueprintsValueFactory vf, final Properties props)
      throws Exception {
    final EmbergraphSail sail = (EmbergraphSail) EmbergraphSailFactory.createSail();
    sail.initialize();
    return new EmbergraphGraphEmbedded(sail, vf, props);
  }

  //    /*
  //     * Create a new persistent local embergraph instance.
  //     */
  //    public static EmbergraphGraph create(final String file)
  //            throws Exception {
  //        final EmbergraphSail sail = EmbergraphSailFactory.createSail(file);
  //        sail.initialize();
  //        return new EmbergraphGraphEmbedded(sail);
  //    }

}
