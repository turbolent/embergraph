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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.embergraph.btree.IndexMetadata;
import org.embergraph.journal.IIndexManager;
import org.embergraph.journal.IJournal;
import org.embergraph.journal.Journal;
import org.embergraph.journal.Tx;
import org.embergraph.rdf.properties.PropertiesFormat;
import org.embergraph.rdf.properties.PropertiesParser;
import org.embergraph.rdf.properties.PropertiesParserFactory;
import org.embergraph.rdf.properties.PropertiesParserRegistry;
import org.embergraph.rdf.sail.EmbergraphSail;
import org.embergraph.rdf.sail.EmbergraphSailRepositoryConnection;
import org.embergraph.rdf.sail.webapp.client.ConnectOptions;
import org.embergraph.rdf.store.AbstractTripleStore;
import org.embergraph.relation.RelationSchema;
import org.embergraph.service.AbstractFederation;
import org.embergraph.service.AbstractTransactionService;
import org.embergraph.util.InnerCause;
import org.embergraph.util.PropertyUtil;
import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;

/*
 * Mult-tenancy Administration Servlet (management for embergraph namespaces). A embergraph
 * namespace corresponds to a partition in the naming of durable resources. A {@link Journal} or
 * {@link IEmbergraphFederation} may have multiple KB instances, each in their own namespace. This
 * servlet allows you to manage those KB instances using CRUD operations.
 *
 * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/575">NanoSparqlServer Admin API
 *     for Multi-tenant deployments</a>
 * @author thompsonbry
 *     <p>FIXME GROUP COMMIT: The other operations in this class also should use the new REST API
 *     pattern, but are not intrinsically sensitive.
 */
public class MultiTenancyServlet extends EmbergraphRDFServlet {

  /** */
  private static final long serialVersionUID = 1L;

  private static final transient Logger log = Logger.getLogger(MultiTenancyServlet.class);

  /*
   * URL query parameter used to override the servlet init parameter {@link
   * ConfigParams#DESCRIBE_EACH_NAMED_GRAPH}.
   */
  protected static final String DESCRIBE_EACH_NAMED_GRAPH = "describe-each-named-graph";

  /** URL query parameter used to specify that only the default namespace should be described. */
  protected static final String DESCRIBE_DEFAULT_NAMESPACE = "describe-default-namespace";

  /** URL query parameter used to specify that full text index will be created if not exists. */
  private static final String FORCE_INDEX_CREATE_PARAMETER = "force-index-create";

  /** Delegate for the sparql end point expressed by <code>.../namespace/NAMESPACE/sparql</code>. */
  private RESTServlet m_restServlet;

  private static final String namespaceRegex = "[^.]+\\Z";

  public static final Set<String> PROPERTIES_BLACK_LIST =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  Journal.Options.BUFFER_MODE,
                  Journal.Options.FILE,
                  Journal.Options.INITIAL_EXTENT,
                  Journal.Options.MAXIMUM_EXTENT,
                  IndexMetadata.Options.WRITE_RETENTION_QUEUE_CAPACITY,
                  IndexMetadata.Options.BTREE_BRANCHING_FACTOR,
                  RelationSchema.CLASS,
                  AbstractTransactionService.Options.MIN_RELEASE_AGE,
                  RelationSchema.NAMESPACE,
                  RelationSchema.CONTAINER)));

  public MultiTenancyServlet() {}

  /** Overridden to create and initialize the delegate {@link Servlet} instances. */
  @Override
  public void init() throws ServletException {

    super.init();

    m_restServlet = new RESTServlet();

    m_restServlet.init(getServletConfig());
  }

  /** Handle namespace create. */
  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (req.getRequestURI().endsWith("/namespace")) {

      // CREATE NAMESPACE.
      doCreateNamespace(req, resp);

      return;

    } else if (req.getRequestURI().endsWith("/prepareProperties")) {

      // Prepare properties.

      doPrepareProperties(req, resp);

      return;
    }

    final String namespace = getNamespace(req);

    if (req.getRequestURI().endsWith(ConnectOptions.urlEncode(namespace) + "/textIndex")) {

      // CREATE NAMESPACE.
      doRebuildTextIndex(req, resp, namespace);

      return;
    }

    /*
     * Pass through to the SPARQL end point REST API.
     *
     * Note: This also handles CANCEL QUERY, which is a POST.
     */
    m_restServlet.doPost(req, resp);
  }

  /*
   * Delete the KB associated with the effective namespace.
   *
   * @see <a href="https://sourceforge.net/apps/trac/bigdata/ticket/689" > Missing URL encoding in
   *     RemoteRepositoryManager </a>
   */
  @Override
  protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (!isWritable(getServletContext(), req, resp)) {
      // Service must be writable.
      return;
    }

    final String namespace = getNamespace(req);

    if (req.getRequestURI().endsWith("/namespace/" + ConnectOptions.urlEncode(namespace))) {

      // Delete that namespace.
      doDeleteNamespace(req, resp);

      return;
    }

    // Pass through to the SPARQL end point REST API.
    m_restServlet.doDelete(req, resp);
  }

  @Override
  protected void doPut(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (!isWritable(getServletContext(), req, resp)) {
      // Service must be writable.
      return;
    }

    // Pass through to the SPARQL end point REST API.
    m_restServlet.doPut(req, resp);
  }

  /** Handles all read-only namespace oriented administration requests. */
  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (req.getRequestURI().endsWith("/namespace")) {

      // Describe all namespaces.

      doDescribeNamespaces(req, resp);

      return;

    } else if (req.getRequestURI().endsWith("/properties")) {

      // Show properties.

      doShowProperties(req, resp);

      return;
    }

    // Pass through to the SPARQL end point REST API.
    m_restServlet.doGet(req, resp);

  }

  /*
   * Prepare a list of properties for a new namespace.
   *
   * <pre>
   * Request-URI
   * ...
   * Content-Type=...
   * ...
   * PropertySet
   * </pre>
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  private void doPrepareProperties(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (!isWritable(getServletContext(), req, resp)) {
      // Service must be writable.
      return;
    }

    final EmbergraphRDFContext context = getEmbergraphRDFContext();

    final IIndexManager indexManager = context.getIndexManager();

    final long timestamp = getTimestamp(req);

    /*
     * 1. Read the request entity, which must be some kind of Properties
     * object. The EmbergraphSail.Options.NAMESPACE property defaults to "kb".
     * A non-default value SHOULD be specified by the client.
     *
     * 2. Wrap and flatten the base properties for the Journal or
     * Federation. This provides defaults for properties which were not
     * explicitly configured for this KB instance.
     *
     * 3. Add the given properties to the flattened defaults to obtain the
     * effective properties.
     */
    final Properties given, defaults, effectiveProperties;
    {
      final String contentType = req.getContentType();

      if (log.isInfoEnabled()) log.info("Request body: " + contentType);

      final PropertiesFormat format = PropertiesFormat.forMIMEType(contentType);

      if (format == null) {

        buildAndCommitResponse(
            resp,
            HTTP_BADREQUEST,
            MIME_TEXT_PLAIN,
            "Content-Type not recognized as Properties: " + contentType);

        return;
      }

      if (log.isInfoEnabled()) log.info("Format=" + format);

      final PropertiesParserFactory parserFactory =
          PropertiesParserRegistry.getInstance().get(format);

      if (parserFactory == null) {

        buildAndCommitResponse(
            resp,
            HTTP_INTERNALERROR,
            MIME_TEXT_PLAIN,
            "Parser factory not found: Content-Type=" + contentType + ", format=" + format);

        return;
      }

      /*
       * There is a request body, so let's try and parse it.
       */

      final PropertiesParser parser = parserFactory.getParser();

      // The given Properties.
      given = parser.parse(req.getInputStream());

      // check properties
      EmbergraphSail.checkProperties(given);

      // The effective namespace for the new KB.
      final String namespace =
          given.getProperty(
              EmbergraphSail.Options.NAMESPACE, EmbergraphSail.Options.DEFAULT_NAMESPACE);

      try {

        if (!Pattern.matches(namespaceRegex, namespace)) {

          throw new IllegalArgumentException(
              "Namespace should not be empty nor include '.' character");
        }

      } catch (Throwable e) {

        launderThrowable(e, resp, "namespace=" + namespace);

        return;
      }

      /*
       * Get the default Properties.
       */
      if (indexManager instanceof IJournal) {

        final IJournal jnl = (IJournal) indexManager;

        defaults = new Properties(jnl.getProperties());

      } else {

        final AbstractFederation<?> fed = (AbstractFederation<?>) indexManager;

        defaults = fed.getClient().getProperties();
      }

      /*
       * Produce the effective properties.
       */
      {
        effectiveProperties = PropertyUtil.flatCopy(defaults);

        for (Map.Entry<Object, Object> e : given.entrySet()) {

          final String name = (String) e.getKey();

          final Object val = e.getValue();

          if (val != null) {

            // Note: Hashtable does not allow nulls.
            effectiveProperties.put(name, val);
          }
        }

        Set<Object> keySet = new HashSet<>(effectiveProperties.keySet());

        Iterator<Object> it = keySet.iterator();

        while (it.hasNext()) {

          final String name = (String) it.next();

          // remove journal related properties
          if (PROPERTIES_BLACK_LIST.contains(name)) {

            effectiveProperties.remove(name);
          }

          // replace default namespace with a specified one

          String newName =
              name.replaceAll(
                  "(?<=\\.namespace\\.)([^.]+)(?=\\.)", Matcher.quoteReplacement(namespace));

          if (!newName.equals(name)) {

            Object val = effectiveProperties.get(name);

            effectiveProperties.remove(name);

            effectiveProperties.put(newName, val);
          }
        }
      }

      try {

        submitApiTask(
                new AbstractRestApiTask<Void>(req, resp, namespace, timestamp) {

                  @Override
                  public boolean isReadOnly() {
                    return true;
                  }

                  @Override
                  public Void call() throws Exception {

                    sendProperties(req, resp, effectiveProperties);

                    return null;
                  }
                })
            .get();

      } catch (Throwable t) {

        launderThrowable(t, resp, "namespace=" + namespace);
      }
    }
  }

  /*
   * Create a new namespace.
   *
   * <pre>
   * Request-URI
   * ...
   * Content-Type=...
   * ...
   * PropertySet
   * </pre>
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  private void doCreateNamespace(final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    if (!isWritable(getServletContext(), req, resp)) {
      // Service must be writable.
      return;
    }

    /*
     * 1. Read the request entity, which must be some kind of Properties
     * object. The EmbergraphSail.Options.NAMESPACE property defaults to "kb".
     * A non-default value SHOULD be specified by the client.
     *
     * 2. Wrap and flatten the base properties for the Journal or
     * Federation. This provides defaults for properties which were not
     * explicitly configured for this KB instance.
     *
     * 3. Add the given properties to the flattened defaults to obtain the
     * effective properties.
     */
    final Properties props;
    {
      final String contentType = req.getContentType();

      if (log.isInfoEnabled()) log.info("Request body: " + contentType);

      final PropertiesFormat format = PropertiesFormat.forMIMEType(contentType);

      if (format == null) {

        buildAndCommitResponse(
            resp,
            HTTP_BADREQUEST,
            MIME_TEXT_PLAIN,
            "Content-Type not recognized as Properties: " + contentType);

        return;
      }

      if (log.isInfoEnabled()) log.info("Format=" + format);

      final PropertiesParserFactory parserFactory =
          PropertiesParserRegistry.getInstance().get(format);

      if (parserFactory == null) {

        buildAndCommitResponse(
            resp,
            HTTP_INTERNALERROR,
            MIME_TEXT_PLAIN,
            "Parser factory not found: Content-Type=" + contentType + ", format=" + format);

        return;
      }

      /*
       * There is a request body, so let's try and parse it.
       */

      final PropertiesParser parser = parserFactory.getParser();

      // The given Properties.
      props = parser.parse(req.getInputStream());
    }

    // The effective namespace for the new KB.
    final String namespace =
        props.getProperty(
            EmbergraphSail.Options.NAMESPACE, EmbergraphSail.Options.DEFAULT_NAMESPACE);

    try {

      if (Pattern.matches(namespaceRegex, namespace)) {

        submitApiTask(new RestApiCreateKBTask(req, resp, namespace, props)).get();

      } else {

        throw new IllegalArgumentException(
            "Namespace should not be empty nor include '.' character");
      }

    } catch (Throwable e) {

      launderThrowable(e, resp, "namespace=" + namespace);
    }
  }

  /*
   * Delete an existing namespace.
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  private void doDeleteNamespace(final HttpServletRequest req, final HttpServletResponse resp) {

    final String namespace = getNamespace(req);

    try {

      submitApiTask(new RestApiDestroyKBTask(req, resp, namespace)).get();

    } catch (Throwable e) {

      launderThrowable(e, resp, "DELETE NAMESPACE: namespace=" + namespace);
    }
  }

  /*
   * Send the configuration properties for the addressed KB instance.
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  private void doShowProperties(final HttpServletRequest req, final HttpServletResponse resp) {

    final String namespace = getNamespace(req);

    final long timestamp = getTimestamp(req);

    //		// TODO Why is it necessary to protect this operation with a transaction?
    //
    //		// TODO This might no longer be necessary with BLZG-2041 since the code
    //		// now uses correct locking patterns when locating a resource.
    //
    //		final long tx = getEmbergraphRDFContext().newTx(timestamp);

    try {

      submitApiTask(
              new AbstractRestApiTask<Void>(req, resp, namespace, timestamp) {

                @Override
                public boolean isReadOnly() {
                  return true;
                }

                @Override
                public Void call() throws Exception {

                  try {

                    final EmbergraphSailRepositoryConnection conn = getQueryConnection();
                    try {

                      final Properties properties =
                          PropertyUtil.flatCopy(conn.getTripleStore().getProperties());

                      sendProperties(req, resp, properties);

                      return null;

                    } finally {
                      conn.close();
                    }

                  } catch (Throwable t) {
                    if (InnerCause.isInnerCause(t, DatasetNotFoundException.class)) {
                      /*
                       * There is no such triple/quad store instance.
                       */
                      throw new HttpOperationException(
                          HttpServletResponse.SC_NOT_FOUND,
                          EmbergraphServlet.MIME_TEXT_PLAIN,
                          "Not found: namespace=" + namespace);
                    }
                    throw new RuntimeException(t);
                  }
                }
              })
          .get();

    } catch (Throwable t) {

      launderThrowable(t, resp, "namespace=" + namespace);

      //		} finally {
      //
      //		    getEmbergraphRDFContext().abortTx(tx);

    }
  }

  /** Generate a VoID Description for the known namespaces. */
  private void doDescribeNamespaces(final HttpServletRequest req, final HttpServletResponse resp) {

    final long timestamp = getTimestamp(req);

    final boolean describeEachNamedGraph;
    {
      final String s = req.getParameter(DESCRIBE_EACH_NAMED_GRAPH);

      describeEachNamedGraph =
          s != null
              ? Boolean.valueOf(s)
              : getEmbergraphRDFContext().getConfig().describeEachNamedGraph;
    }

    final boolean describeDefaultNamespace;
    {
      final String s = req.getParameter(DESCRIBE_DEFAULT_NAMESPACE);

      describeDefaultNamespace = s != null ? Boolean.valueOf(s) : false;
    }

    /*
     * Protect the entire operation with a transaction, including the describe of each namespace
     * that we discover.
     *
     * @see <a href="http://trac.blazegraph.com/ticket/867">NSS concurrency problem with list
     *     namespaces and create namespace </a>
     */
    final long tx = getEmbergraphRDFContext().newTx(timestamp);

    try {

      final Graph g = new LinkedHashModel();

      if (describeDefaultNamespace) {

        final String namespace = getEmbergraphRDFContext().getConfig().namespace;

        describeNamespaceTx(req, g, namespace, describeEachNamedGraph, tx);

      } else {

        /*
         * The set of registered namespaces for KBs.
         */
        final List<String> namespaces = getEmbergraphRDFContext().getNamespacesTx(tx);

        for (String namespace : namespaces) {

          describeNamespaceTx(req, g, namespace, describeEachNamedGraph, tx);
        }
      }

      sendGraph(req, resp, g);

    } catch (Throwable t) {

      launderThrowable(
          t,
          resp,
          "describeEachNamedGraph="
              + describeEachNamedGraph
              + ", describeDefaultNamespace="
              + describeDefaultNamespace);

    } finally {

      getEmbergraphRDFContext().abortTx(tx);
    }
  }

  /** Describe a namespace into the supplied Graph object. */
  private void describeNamespaceTx(
      final HttpServletRequest req,
      final Graph g,
      final String namespace,
      final boolean describeEachNamedGraph,
      final long tx) {

    // Get a view onto that KB instance for that timestamp.
    final AbstractTripleStore tripleStore = getEmbergraphRDFContext().getTripleStore(namespace, tx);

    if (tripleStore == null) {

      /*
       * There is no such triple/quad store instance (could be a
       * concurrent delete of the namespace).
       */

      return;
    }

    final BNode aDataSet = ValueFactoryImpl.getInstance().createBNode();

    // Figure out the service end point(s).
    final String[] serviceURI = getServiceURIs(getServletContext(), req);

    final VoID v = new VoID(g, tripleStore, serviceURI, aDataSet);

    v.describeDataSet(false /* describeStatistics */, describeEachNamedGraph);
  }

  private void doRebuildTextIndex(
      HttpServletRequest req, HttpServletResponse resp, String namespace) throws IOException {

    boolean forceIndexCreate =
        Boolean.TRUE.toString().equals(req.getParameter(FORCE_INDEX_CREATE_PARAMETER));

    PrintWriter writer = resp.getWriter();

    try {

      AbstractTripleStore store =
          getEmbergraphRDFContext().getTripleStore(namespace, Tx.UNISOLATED);

      store.getLexiconRelation().rebuildTextIndex(forceIndexCreate);

      writer.append("Text index rebuild completed");

    } catch (UnsupportedOperationException e) {

      writer.append(e.getMessage());

      resp.sendError(HTTP_INTERNALERROR, e.getMessage());
    }
  }
}
