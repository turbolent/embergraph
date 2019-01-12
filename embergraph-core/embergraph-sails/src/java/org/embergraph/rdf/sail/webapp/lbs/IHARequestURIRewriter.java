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
package org.embergraph.rdf.sail.webapp.lbs;

import javax.servlet.http.HttpServletRequest;
import org.embergraph.rdf.sail.webapp.HALoadBalancerServlet;

/**
 * Interface for rewriting the Request-URI once the load balancer has determined the target host and
 * service to which the request will be proxied.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 */
public interface IHARequestURIRewriter extends IHAPolicyLifeCycle {

  /**
   * Rewrite the <code>originalRequestURI</code> into a <a href=
   * "http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2" >Request-URL</a> for the web
   * application whose servlet context root is given by the <code>proxyToRequestURI</code>.
   *
   * <p>Note: The <code>proxyToRequestURI</code> is include the protocol, host, port, and servlet
   * context path for the target service. It DOES NOT include any information from the original
   * request. The purpose of this method is to modify the <code>originalRequestURI</code> in order
   * to obtain a fully qualified RequestURI for the service to which the request will be proxied.
   * For example:
   *
   * <pre>
   *        full_prefix:                            /embergraph/LBS/leader
   * -or-   full_prefix:                            /embergraph/LBS/read
   * originalRequestURI: http://ha1.example.com:8090/embergraph/LBS/read/sparql
   *  proxyToRequestURI: http://ha3.example.com:8090/embergraph/
   *             return: http://ha2.example.com:8090/embergraph/LBS/read/sparql
   * </pre>
   *
   * <p>Note: this method is only invoked if we will proxy to another service. Therefore, the <code>
   * proxyToRequestURI</code> is never <code>null</code>.
   *
   * @param isLeaderRequest <code>true</code> iff the request is directed to the leader.
   * @param full_prefix The path prefix in the <code>originalRequestURI</code> that which
   *     corresponds to the {@link HALoadBalancerServlet} and which must be removed if the request
   *     is to be forwarded to a local service.
   * @param originalRequestURI The original <a href=
   *     "http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2" >Request-URL</a> from the
   *     <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1" >HTTP
   *     Request-Line</a>
   * @param proxyToRequestURI The RequestURI for the root of the web application for the target
   *     service and never <code>null</code>.
   * @param request The original request.
   * @return The fully qualified <a href=
   *     "http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.2" >Request-URL</a> that will
   *     be used to proxy the http request to the service identified by the <code>proxyToRequestURI
   *     </code>
   * @see ServiceScore#getRequestURI()
   */
  public StringBuilder rewriteURI(
      boolean isLeaderRequest,
      String full_prefix,
      String originalRequestURI,
      String proxyToRequestURI,
      HttpServletRequest request);
}
