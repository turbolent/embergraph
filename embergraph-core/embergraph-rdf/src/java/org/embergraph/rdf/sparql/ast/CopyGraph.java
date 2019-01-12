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
 * Created on Mar 10, 2012
 */

package org.embergraph.rdf.sparql.ast;

import java.util.Map;
import org.embergraph.bop.BOp;

/*
* The COPY operation is a shortcut for inserting all data from an input graph into a destination
 * graph. Data from the input graph is not affected, but data from the destination graph, if any, is
 * removed before insertion.
 *
 * <pre>
 * COPY ( SILENT )? ( ( GRAPH )? IRIref_from | DEFAULT) TO ( ( GRAPH )? IRIref_to | DEFAULT )
 * </pre>
 *
 * @see http://www.w3.org/TR/sparql11-update/#copy
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class CopyGraph extends AbstractFromToGraphManagement {

  /** */
  private static final long serialVersionUID = 1L;

  public CopyGraph() {

    super(UpdateType.Copy);
  }

  /** @param op */
  public CopyGraph(final CopyGraph op) {

    super(op);
  }

  /*
   * @param args
   * @param anns
   */
  public CopyGraph(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);
  }
}
