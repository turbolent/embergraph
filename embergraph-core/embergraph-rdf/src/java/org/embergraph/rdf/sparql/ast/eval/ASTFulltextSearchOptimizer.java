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
 * Created on Sep 8, 2011
 */

package org.embergraph.rdf.sparql.ast.eval;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.embergraph.service.fts.FTS;
import org.openrdf.model.URI;

/*
 * Translate {@link FTS#SEARCH} and related magic predicates into a {@link ServiceNode} which will
 * invoke the embergraph search engine.
 *
 * <pre>
 * with {
 *    select ?subj ?score
 *    where {
 *      ?res fts:search "foo" .
 *      ?res fts:endpoint "http://my.solr.endpoint"
 *      ?res fts:relevance ?score .
 *    }
 * } as %searchSet1
 * </pre>
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public class ASTFulltextSearchOptimizer extends ASTSearchOptimizerBase {

  public static final Set<URI> searchUris;

  static {
    final Set<URI> set = new LinkedHashSet<>();

    set.add(FTS.SEARCH);
    set.add(FTS.ENDPOINT);
    set.add(FTS.ENDPOINT_TYPE);
    set.add(FTS.PARAMS);
    set.add(FTS.SEARCH_RESULT_TYPE);
    set.add(FTS.TIMEOUT);
    set.add(FTS.SCORE);
    set.add(FTS.SNIPPET);
    set.add(FTS.SEARCH_FIELD);
    set.add(FTS.SNIPPET_FIELD);
    set.add(FTS.SCORE_FIELD);

    searchUris = Collections.unmodifiableSet(set);
  }

  @Override
  protected Set<URI> getSearchUris() {
    return searchUris;
  }

  @Override
  protected String getNamespace() {
    return FTS.NAMESPACE;
  }

  @Override
  protected URI getSearchPredicate() {
    return FTS.SEARCH;
  }
}
