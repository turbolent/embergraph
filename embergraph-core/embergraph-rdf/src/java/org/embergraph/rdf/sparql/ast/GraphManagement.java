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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.embergraph.bop.BOp;
import org.embergraph.bop.IVariable;

/*
 * A Graph Management operation.
 *
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public abstract class GraphManagement extends Update {

  /** */
  private static final long serialVersionUID = 1L;

  /** @param updateType */
  public GraphManagement(final UpdateType updateType) {

    super(updateType);
  }

  /** @param op */
  public GraphManagement(final GraphManagement op) {

    super(op);
  }

  /*
   * @param args
   * @param anns
   */
  public GraphManagement(final BOp[] args, final Map<String, Object> anns) {

    super(args, anns);
  }

  @Override
  public final boolean isSilent() {

    return getProperty(Annotations.SILENT, Annotations.DEFAULT_SILENT);
  }

  @Override
  public final void setSilent(final boolean silent) {

    setProperty(Annotations.SILENT, silent);
  }

  @Override
  public Set<IVariable<?>> getRequiredBound(StaticAnalysis sa) {
    return new HashSet<>();
  }

  @Override
  public Set<IVariable<?>> getDesiredBound(StaticAnalysis sa) {
    return new HashSet<>();
  }
}
