package org.embergraph.rdf.sail.remote;

import org.embergraph.rdf.sail.webapp.client.EncodeDecodeValue;
import org.embergraph.rdf.sail.webapp.client.IPreparedQuery;
import org.embergraph.rdf.sail.webapp.client.RemoteRepositoryDecls;
import org.openrdf.model.URI;
import org.openrdf.query.Binding;
import org.openrdf.query.impl.AbstractQuery;

public abstract class AbstractEmbergraphRemoteQuery extends AbstractQuery {

  private String baseURI;

  public AbstractEmbergraphRemoteQuery(String baseURI) {
    super();
    this.baseURI = baseURI;
  }

  /**
   * @see org.openrdf.http.client.HTTPClient#getQueryMethodParameters(QueryLanguage, String, String,
   *     Dataset, boolean, int, Binding...)
   */
  protected void configureConnectOptions(IPreparedQuery q) {
    if (baseURI != null) {
      q.addRequestParam(RemoteRepositoryDecls.BASE_URI, baseURI);
    }
    q.addRequestParam(RemoteRepositoryDecls.INCLUDE_INFERRED, Boolean.toString(includeInferred));
    if (maxQueryTime > 0) {
      q.addRequestParam(
          RemoteRepositoryDecls.MAX_QUERY_TIME_MILLIS, Long.toString(1000L * maxQueryTime));
    }

    if (dataset != null) {
      String[] defaultGraphs = new String[dataset.getDefaultGraphs().size()];
      int i = 0;
      for (URI defaultGraphURI : dataset.getDefaultGraphs()) {
        defaultGraphs[i++] = String.valueOf(defaultGraphURI);
      }

      q.addRequestParam(
          q.isUpdate()
              ? RemoteRepositoryDecls.USING_GRAPH_URI
              : RemoteRepositoryDecls.DEFAULT_GRAPH_URI,
          defaultGraphs);

      String[] namedGraphs = new String[dataset.getNamedGraphs().size()];
      i = 0;
      for (URI namedGraphURI : dataset.getNamedGraphs()) {
        namedGraphs[i++] = String.valueOf(String.valueOf(namedGraphURI));
      }
      q.addRequestParam(
          q.isUpdate()
              ? RemoteRepositoryDecls.USING_NAMED_GRAPH_URI
              : RemoteRepositoryDecls.NAMED_GRAPH_URI,
          namedGraphs);
    }
    for (Binding binding : bindings) {
      String paramName = RemoteRepositoryDecls.BINDING_PREFIX + binding.getName();
      String paramValue = EncodeDecodeValue.encodeValue(binding.getValue());
      q.addRequestParam(paramName, paramValue);
    }
  }
}
