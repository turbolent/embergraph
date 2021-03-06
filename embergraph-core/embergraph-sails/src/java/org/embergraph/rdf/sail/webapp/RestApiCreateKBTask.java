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
package org.embergraph.rdf.sail.webapp;

import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.sail.CreateKBTask;
import org.embergraph.util.NV;

/** Extended to report the correct HTTP response to the client. */
class RestApiCreateKBTask extends AbstractDelegateRestApiTask<Void> {

  public RestApiCreateKBTask(
      final HttpServletRequest req,
      final HttpServletResponse resp,
      final String namespace,
      final Properties properties) {

    super(req, resp, namespace, ITx.UNISOLATED, new CreateKBTask(namespace, properties));
  }

  @Override
  public Void call() throws Exception {

    // Pre-condition check while holding locks.
    {

      // Resolve the namespace.  See BLZG-2023, BLZG-2041.
      // Note: We have a lock on the unisolated view of the namespace using the concurrency manager.
      final boolean exists =
          getIndexManager().getResourceLocator().locate(namespace, ITx.UNISOLATED) != null;

      if (exists) {
        /*
         * The namespace already exists.
         *
         * Note: The response code is defined as 409 (Conflict) since 1.3.2.
         */
        throw new HttpOperationException(
            HttpServletResponse.SC_CONFLICT,
            EmbergraphServlet.MIME_TEXT_PLAIN,
            "EXISTS: " + namespace);
      }
    }

    // Create namespace.
    super.call();

    /*
     * Note: The response code is defined as 201 (Created) since 1.3.2.
     */

    /*
     * Generate the absolute location of the new resource.
     *
     * @see <a href="http://trac.bigdata.com/ticket/1187">CREATE DATA SET does not report Location
     *     header </a>
     */
    final String locationURI;
    {
      // Obtain the reconstructed request URI.
      final StringBuffer sb = req.getRequestURL();
      if (sb.charAt(sb.length() - 1) != '/') {
        // Add trailing '/' iff not present.
        sb.append('/');
      }
      // append the name of the newly created namespace.
      sb.append(namespace);
      /*
       * append /sparql to get the SPARQL end point. This end point may be
       * used for query, update, service description, etc.
       */
      sb.append("/sparql");
      locationURI = sb.toString();
    }

    buildResponse(
        HttpServletResponse.SC_CREATED,
        MultiTenancyServlet.MIME_TEXT_PLAIN,
        "CREATED: " + namespace,
        new NV("Location", locationURI));

    return null;
  }

  @Override
  public final boolean isReadOnly() {
    return false;
  }
}
