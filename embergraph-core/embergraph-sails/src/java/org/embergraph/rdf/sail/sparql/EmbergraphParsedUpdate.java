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
 * Created on Aug 28, 2011
 */

package org.embergraph.rdf.sail.sparql;

import org.embergraph.rdf.sparql.ast.ASTContainer;
import org.openrdf.query.Dataset;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParser;

/**
 * Class extends {@link ParsedUpdate} for API compliance with {@link QueryParser} but DOES NOT
 * support ANY aspect of the {@link QueryParser} API. All data pertaining to the parsed query is
 * reported by {@link #getASTContainer()}. There is NO {@link TupleExpr} associated with the {@link
 * EmbergraphParsedUpdate}. Embergraph uses an entirely different model to represent the parsed
 * query, different optimizers to rewrite the parsed query, and different operations to evaluate the
 * {@link ParsedUpdate}.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 * @openrdf
 */
public class EmbergraphParsedUpdate extends ParsedUpdate {

  private final ASTContainer astContainer;

  /** */
  public EmbergraphParsedUpdate(final ASTContainer astContainer) {

    this.astContainer = astContainer;
  }

  /** Unsupported operation. */
  public EmbergraphParsedUpdate(TupleExpr tupleExpr) {
    throw new UnsupportedOperationException();
  }

  /** Unsupported operation. */
  public EmbergraphParsedUpdate(TupleExpr tupleExpr, Dataset dataset) {
    throw new UnsupportedOperationException();
  }

  /** The {@link ASTContainer}. */
  public ASTContainer getASTContainer() {
    return astContainer;
  }
}
