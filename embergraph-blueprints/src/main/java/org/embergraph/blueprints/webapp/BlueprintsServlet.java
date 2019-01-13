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
package org.embergraph.blueprints.webapp;

import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.embergraph.blueprints.EmbergraphGraphBulkLoad;
import org.embergraph.journal.ITx;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.webapp.AbstractRestApiTask;
import org.embergraph.rdf.sail.webapp.BlueprintsServletProxy;
import org.embergraph.rdf.sail.webapp.EmbergraphRDFServlet;
import org.embergraph.rdf.sail.webapp.client.MiniMime;

/** Helper servlet for the blueprints layer. */
public class BlueprintsServlet extends BlueprintsServletProxy {

  /** */
  private static final long serialVersionUID = 1L;

  private static final transient Logger log = Logger.getLogger(BlueprintsServlet.class);

  public static final List<String> mimeTypes = Collections.singletonList("application/graphml+xml");

  public BlueprintsServlet() {}

  /** Post a GraphML file to the blueprints layer. */
  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    final String contentType = req.getContentType();

    if (log.isInfoEnabled()) log.info("Request body: " + contentType);

    final String mimeType = new MiniMime(contentType).getMimeType().toLowerCase();

    if (!mimeTypes.contains(mimeType)) {

      buildAndCommitResponse(
          resp,
          HTTP_BADREQUEST,
          MIME_TEXT_PLAIN,
          "Content-Type not recognized as graph data: " + contentType);

      return;
    }

    try {

      submitApiTask(new BlueprintsPostTask(req, resp, getNamespace(req), ITx.UNISOLATED)).get();

    } catch (Throwable t) {

      EmbergraphRDFServlet.launderThrowable(t, resp, "");
    }
  }

  private static class BlueprintsPostTask extends AbstractRestApiTask<Void> {

    public BlueprintsPostTask(
        HttpServletRequest req, HttpServletResponse resp, String namespace, long timestamp) {

      super(req, resp, namespace, timestamp);
    }

    @Override
    public boolean isReadOnly() {
      return false;
    }

    @Override
    public Void call() throws Exception {

      final long begin = System.currentTimeMillis();

      EmbergraphSailRepositoryConnection conn = null;
      boolean success = false;
      try {

        conn = getConnection();

        final EmbergraphGraphBulkLoad graph = new EmbergraphGraphBulkLoad(conn);

        GraphMLReader.inputGraph(graph, req.getInputStream());

        graph.commit();

        success = true;

        final long nmodified = graph.getMutationCountLastCommit();

        final long elapsed = System.currentTimeMillis() - begin;

        reportModifiedCount(nmodified, elapsed);

        // Done.
        return null;

      } finally {

        if (conn != null) {

          if (!success) conn.rollback();

          conn.close();
        }
      }
    }
  }

  /*
   * Convenience method to access doPost from a public method.
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  public void doPostRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    doPost(req, resp);
  }
}
