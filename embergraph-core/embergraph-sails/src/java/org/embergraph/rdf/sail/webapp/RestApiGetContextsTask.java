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

import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.webapp.XMLBuilder.Node;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryResult;

/*
 * Task to report the contexts used by a QUADS mode KB instance.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
class RestApiGetContextsTask extends AbstractRestApiTask<Void> {

  public RestApiGetContextsTask(
      final HttpServletRequest req,
      final HttpServletResponse resp,
      final String namespace,
      final long timestamp) {

    super(req, resp, namespace, timestamp);
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public Void call() throws Exception {

    EmbergraphSailRepositoryConnection conn = null;
    try {

      conn = getQueryConnection();

      final StringWriter w = new StringWriter();

      final RepositoryResult<Resource> it = conn.getContextIDs();

      try {

        final XMLBuilder t = new XMLBuilder(w);

        final Node root = t.root("contexts");

        while (it.hasNext()) {

          root.node("context").attr("uri", it.next()).close();
        }

        root.close();

      } finally {

        it.close();
      }

      buildResponse(QueryServlet.HTTP_OK, QueryServlet.MIME_APPLICATION_XML, w.toString());

      return null;

    } finally {

      if (conn != null) {

        conn.close();
      }
    }
  }
}
