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

import org.embergraph.bop.IBindingSet;
import org.embergraph.service.fts.FTS.SearchResultType;

/*
 * Metadata about a fulltext search result (against an external service). See {@link FTS}.
 *
 * @author <a href="mailto:ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface IFulltextSearchHit<V extends Comparable<V>> {

  /** The result of the search (values contained in projected columns). */
  String getRes();

  /** The score associated with the search result. */
  Double getScore();

  /** The search snippet associated with the result. */
  String getSnippet();

  /** Get the set of incoming bindings for the search hit */
  IBindingSet getIncomingBindings();

  /** Get the conversion target type for the search hit */
  SearchResultType getSearchResultType();
}
