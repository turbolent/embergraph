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
 * Created on Aug 28, 2015
 */

package org.embergraph.rdf.sparql.ast.explainhints;

import org.embergraph.bop.BOp;

/**
 * Hint to be interpreted by EXPLAIN, containing information to be exposed to the user. Such
 * information could be potential performance bottlenecks identified throughout the query
 * optimization phase, or potential problems due to SPARQL's bottom-up semantics.
 *
 * @author <a href="ms@metaphacts.com">Michael Schmidt</a>
 * @version $Id$
 */
public interface IExplainHint {

  /** Enum datatype specifing the severity of a given explain hint */
  enum ExplainHintSeverity {
    SEVERE, /* could well/likely be a severe problem */
    MODERATE, /* could be a problem, but not critical */
    INFO /* informative explain hint */
  }

  /** Enum datatype categorizing an explain hint */
  enum ExplainHintCategory {
    CORRECTNESS, /* hint regarding the correctness of a query construct */
    PERFORMANCE, /* hint indicating performance related issues */
    OTHER /* anything unclassified */
  }

  /** @return textual representation of the explain hint type */
  String getExplainHintType();

  /** @return severity of an explain hint */
  ExplainHintSeverity getExplainHintSeverity();

  /** @return category of an explain hint */
  ExplainHintCategory getExplainHintCategory();

  /** @return a detailed description, representing the content of the hint */
  String getExplainHintDescription();

  /**
   * @return the node affected by the explain node; note that we attach this node explicitly, since
   *     the node to which the hint is attached is not always the affected node, for instance if the
   *     affected node has been optimized away
   */
  BOp getExplainHintNode();

  /** @return a link to an external help page. */
  String getHelpLink();
}
