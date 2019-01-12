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
 * Created on Apr 12, 2011
 */

package org.embergraph.rdf.sail.webapp;

import java.io.IOException;
import java.io.OutputStreamWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.embergraph.counters.CounterSet;
import org.embergraph.counters.format.CounterSetFormat;
import org.embergraph.counters.query.CounterSetSelector;
import org.embergraph.counters.query.URLQueryModel;
import org.embergraph.counters.render.IRenderer;
import org.embergraph.counters.render.RendererFactory;
import org.embergraph.journal.IIndexManager;
import org.embergraph.service.IEmbergraphFederation;
import org.embergraph.service.IService;

/*
 * Servlet for exposing performance counters.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 *     <p>TODO The SPARQL layer needs to be separated from the core embergraph layer, with the
 *     EmbergraphContext moving into a servlet package in the embergraph module and the
 *     CountersServlet moving into a servlet package in the org.embergraph.counters package
 *     namespace.
 *     <p>The flot resources need to be retrievable.
 *     <p>The event reporting needs to be hooked via {@link IEventReceivingService}, at least for
 *     scale-out.
 */
public class CountersServlet extends EmbergraphServlet {

  /** */
  private static final long serialVersionUID = 1L;

  private static final transient Logger log = Logger.getLogger(CountersServlet.class);

  /** */
  public CountersServlet() {}

  /*
   * Performance counters
   *
   * <pre>
   * GET /counters
   * </pre>
   */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    try {

      // TODO Hook this how? (NSS does not define an IService right now)
      final IService service = null;

      final IIndexManager indexManager = getIndexManager();

      if (indexManager instanceof IEmbergraphFederation) {

        ((IEmbergraphFederation<?>) indexManager).reattachDynamicCounters();
      }

      final CounterSet counterSet = indexManager.getCounters();

      final CounterSetSelector counterSelector = new CounterSetSelector(counterSet);

      /*
       * Obtain a renderer.
       *
       * @todo if controller state error then send HTTP_BAD_REQUEST
       *
       * @todo Write XSL and stylesheet for interactive browsing of the
       * CounterSet XML?
       */

      // Do conneg.
      final String acceptStr = req.getHeader("Accept");

      final ConnegUtil util = new ConnegUtil(acceptStr);

      final CounterSetFormat format =
          util.getCounterSetFormat(CounterSetFormat.HTML /* fallback */);

      //         final String mimeType = MIME_TEXT_HTML;
      final String mimeType = format.getDefaultMIMEType();

      if (log.isDebugEnabled())
        log.debug("\nAccept=" + acceptStr + ",\nformat=" + format + ", mimeType=" + mimeType);

      final IRenderer renderer;
      {

        // build model of the controller state.
        final URLQueryModel model = URLQueryModel.getInstance(service, req, resp);

        if (log.isDebugEnabled()) log.debug("\nmodel=" + model);

        renderer = RendererFactory.get(model, counterSelector, mimeType);

        if (log.isDebugEnabled()) log.debug("\nrenderer=" + renderer);
      }

      resp.setStatus(HTTP_OK);

      resp.setContentType(mimeType);

      if (format.hasCharset()) {

        // Note: Binary encodings do not specify charset.
        resp.setCharacterEncoding(format.getCharset().name());
      }

      /*
       * Sets the cache behavior -- the data should be good for up to 60
       * seconds unless you change the query parameters. These cache control
       * parameters SHOULD indicate that the response is valid for 60 seconds,
       * that the client must revalidate, and that the response is cachable
       * even if the client was authenticated.
       */
      resp.addHeader("Cache-Control", "max-age=60, must-revalidate, public");
      // to disable caching.
      // r.addHeader("Cache-Control", "no-cache");

      /*
       * Render the counters as specified by the query for the negotiated MIME
       * type.
       */
      {
        final OutputStreamWriter w = new OutputStreamWriter(resp.getOutputStream(), charset);

        // render the view.
        renderer.render(w);

        w.flush();
      }

      if (log.isTraceEnabled()) log.trace("done");

    } catch (Throwable t) {

      EmbergraphRDFServlet.launderThrowable(t, resp, "");
    }
  }
}
