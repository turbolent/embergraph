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
package org.embergraph.service.fts;

import java.io.Serializable;
import org.eclipse.jetty.client.HttpClient;
import org.embergraph.bop.IBindingSet;
import org.embergraph.service.fts.FTS.SearchResultType;

/**
 * Abstraction for search interface against external Solr index.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface IFulltextSearch<A extends IFulltextSearchHit> {

  /**
   * Submit a search query against the Solr Index
   *
   * @param query The query.
   * @return The result set.
   */
  FulltextSearchHiterator<A> search(final FulltextSearchQuery query, HttpClient client);

  // public int count(final ExternalSolrSearchQuery query);

  class FulltextSearchQuery implements Serializable {

    private static final long serialVersionUID = -2509557655519603130L;

    final String query;
    final String params;
    final String endpoint;
    final Integer searchTimeout;
    final String searchField;
    final String scoreField;
    final String snippetField;
    final IBindingSet incomingBindings;
    final SearchResultType searchResultType;

    /** Constructor */
    public FulltextSearchQuery(
        final String query,
        final String params,
        final String endpoint,
        final Integer searchTimeout,
        final String searchField,
        final String scoreField,
        final String snippetField,
        final IBindingSet incomingBindings,
        final SearchResultType searchResultType) {

      this.query = query;
      this.params = params;
      this.endpoint = endpoint;
      this.searchTimeout = searchTimeout;
      this.searchField = searchField;
      this.scoreField = scoreField;
      this.snippetField = snippetField;
      this.incomingBindings = incomingBindings;
      this.searchResultType = searchResultType;
    }

    /** @return the query */
    public String getQuery() {
      return query;
    }

    /** @return the query endpoint */
    public String getParams() {
      return params;
    }

    /** @return the query endpoint */
    public String getEndpoint() {
      return endpoint;
    }

    /** @return the search timeout */
    public Integer getSearchTimeout() {
      return searchTimeout;
    }

    /** @return the field that is mapped to the search result */
    public String getSearchField() {
      return searchField;
    }

    /** @return the field that is mapped to the snippet variable */
    public String getSnippetField() {
      return snippetField;
    }

    /** @return the field that is mapped to the score */
    public String getScoreField() {
      return scoreField;
    }

    /** @return the underlying binding set */
    public IBindingSet getIncomingBindings() {
      return incomingBindings;
    }

    /** @return the target type for conversion */
    public SearchResultType getSearchResultType() {
      return searchResultType;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;

      int result = 1;

      result = prime * result + ((query == null) ? 0 : query.hashCode());

      result = prime * result + ((params == null) ? 0 : params.hashCode());

      result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());

      result = prime * result + ((searchTimeout == null) ? 0 : searchTimeout.hashCode());

      result = prime * result + ((searchResultType == null) ? 0 : searchResultType.hashCode());

      return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      FulltextSearchQuery other = (FulltextSearchQuery) obj;

      if ((query == null && other.query != null)
          || (query != null && other.query == null)
          || !query.equals(other.query)) return false;

      if ((params == null && other.params != null)
          || (params != null && other.params == null)
          || !params.equals(other.params)) return false;

      if ((endpoint == null && other.endpoint != null)
          || (endpoint != null && other.endpoint == null)
          || !endpoint.equals(other.endpoint)) return false;

      if ((searchTimeout == null && other.searchTimeout != null)
          || (searchTimeout != null && other.searchTimeout == null)
          || !searchTimeout.equals(other.searchTimeout)) return false;

      return (searchResultType != null || other.searchResultType == null)
          && (searchResultType == null || other.searchResultType != null)
          && searchResultType.equals(other.searchResultType);
    }
  }
}
